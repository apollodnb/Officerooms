package de.officerooms.block;

import de.officerooms.OfficeRooms;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.room.Navigator;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Portal zwischen Overworld (bzw. jeder anderen Dimension) und den Office Rooms.
 *
 * <ul>
 *     <li>Außerhalb der Office Rooms führt es hinein: in einen Raum in der Mitte der Dimension über der
 *     Position des Portals. Der Ausgangspunkt wird am Spieler gespeichert.</li>
 *     <li>In den Office Rooms (Rückkehrportale auf der untersten Ebene) führt es in die Overworld:
 *     an den gespeicherten Ausgangspunkt, sonst an den Weltspawn.</li>
 *     <li>Nur Spieler reisen; nach einer Reise 3 Sekunden Sperre, damit man nicht zurückspringt.</li>
 * </ul>
 */
public final class OfficePortalBlock extends Block {

    private static final String RETURN = "office_rooms_return";
    private static final String COOLDOWN = "office_rooms_portal_cd";

    public OfficePortalBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.isPassenger() || player.isVehicle()) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        long now = level.getGameTime();
        if (data.getLong(COOLDOWN) > now && data.getString(COOLDOWN + "_dim").equals(level.dimension().location().toString())) {
            return;
        }
        ServerLevel target;
        if (level.dimension() == OfficeRoomsDimension.LEVEL_KEY) {
            target = player.server.overworld();
            double x;
            double y;
            double z;
            if (data.contains(RETURN)) {
                CompoundTag r = data.getCompound(RETURN);
                ServerLevel back = player.server.getLevel(net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION, new net.minecraft.resources.ResourceLocation(r.getString("dim"))));
                if (back != null && back != level) {
                    target = back;
                }
                x = r.getDouble("x");
                y = r.getDouble("y");
                z = r.getDouble("z");
            } else {
                BlockPos sp = target.getSharedSpawnPos();
                x = sp.getX() + 0.5;
                z = sp.getZ() + 0.5;
                y = target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ());
            }
            setCooldown(player, target, 60);
            player.teleportTo(target, x, y, z, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.literal("You wake up somewhere familiar."), true);
        } else {
            target = player.server.getLevel(OfficeRoomsDimension.LEVEL_KEY);
            if (target == null || !(target.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
                return;
            }
            OfficePlanner p = gen.sampler(target.getChunkSource().randomState(), target).planner();
            Navigator.Target t = arrival(p, pos.getX(), pos.getZ());
            if (t == null) {
                OfficeRooms.LOGGER.warn("Portal: no arrival room found");
                return;
            }
            // Rückkehrpunkt: zwei Blöcke neben dem Portal, nicht im Portal selbst
            CompoundTag r = new CompoundTag();
            r.putString("dim", level.dimension().location().toString());
            BlockPos safe = safeSpotNear((ServerLevel) level, pos);
            r.putDouble("x", safe.getX() + 0.5);
            r.putDouble("y", safe.getY());
            r.putDouble("z", safe.getZ() + 0.5);
            data.put(RETURN, r);
            setCooldown(player, target, 60);
            player.teleportTo(target, t.x(), t.y(), t.z(), t.yaw(), 0F);
            player.displayClientMessage(Component.literal("The hum of fluorescent lights. You are not where you were."), true);
        }
        level.playSound(null, pos, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.25F, 0.6F);
    }

    /** Ankunftsraum nahe (x, z) in der mittleren Ebene, kein Spezialraum (z. B. kein Wasserbüro). */
    static Navigator.Target arrival(OfficePlanner p, int x, int z) {
        for (int k = 0; k < 12; k++) {
            Navigator.Target t = Navigator.findRoom(p, x + k * 37, z - k * 23, p.storeys / 2);
            if (t == null) {
                continue;
            }
            int tx = (int) Math.floor(t.x());
            int tz = (int) Math.floor(t.z());
            int rx = p.regionOf(tx);
            int rz = p.regionOf(tz);
            de.officerooms.room.Room r = p.layout(rx, rz).room(p.storeyOf((int) t.y()),
                    Math.floorMod(tx, p.regionBlocks) / p.unit, Math.floorMod(tz, p.regionBlocks) / p.unit);
            if (de.officerooms.special.SpecialRooms.of(p, rx, rz, r) == de.officerooms.special.SpecialType.NONE) {
                return t;
            }
        }
        return Navigator.findRoom(p, x, z, p.storeys / 2);
    }

    private static void setCooldown(ServerPlayer player, ServerLevel target, int ticks) {
        player.getPersistentData().putLong(COOLDOWN, target.getGameTime() + ticks);
        player.getPersistentData().putString(COOLDOWN + "_dim", target.dimension().location().toString());
    }

    /** Freier Platz (2 Blöcke Luft über festem Boden) in der Nähe des Portals, nicht das Portal selbst. */
    private static BlockPos safeSpotNear(ServerLevel level, BlockPos pos) {
        for (int r = 2; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    for (int dy = -1; dy <= 2; dy++) {
                        BlockPos q = pos.offset(dx, dy, dz);
                        if (level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()
                                && level.getBlockState(q.below()).isFaceSturdy(level, q.below(), net.minecraft.core.Direction.UP)) {
                            return q;
                        }
                    }
                }
            }
        }
        return pos.above(2);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rnd) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.SCULK_SOUL, pos.getX() + rnd.nextDouble(), pos.getY() + rnd.nextDouble(),
                    pos.getZ() + rnd.nextDouble(), 0, 0.02, 0);
        }
        if (rnd.nextInt(60) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS, 0.2F, 0.5F + rnd.nextFloat() * 0.2F, false);
        }
    }
}
