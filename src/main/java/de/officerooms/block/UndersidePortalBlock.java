package de.officerooms.block;

import de.officerooms.OfficeRooms;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.Navigator;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** 0.19.0: the two exits that open when [Unknown] dies: back to the Office Rooms, or the Overworld. */
public class UndersidePortalBlock extends Block {

    public static final BooleanProperty OVERWORLD = BooleanProperty.create("overworld");
    private static final VoxelShape SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final String COOLDOWN = "office_rooms_uportal";

    public UndersidePortalBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(OVERWORLD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(OVERWORLD);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof ServerPlayer p) || p.isPassenger()) {
            return;
        }
        long now = level.getGameTime();
        if (p.getPersistentData().getLong(COOLDOWN) > now) {
            return;
        }
        p.getPersistentData().putLong(COOLDOWN, now + 60);
        if (state.getValue(OVERWORLD)) {
            ServerLevel ow = p.server.overworld();
            BlockPos sp = p.getRespawnPosition() != null && p.getRespawnDimension() == Level.OVERWORLD ? p.getRespawnPosition() : ow.getSharedSpawnPos();
            int y = ow.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sp.getX(), sp.getZ());
            p.teleportTo(ow, sp.getX() + 0.5, Math.max(y, sp.getY()), sp.getZ() + 0.5, p.getYRot(), 0F);
            p.displayClientMessage(Component.literal("Daylight. Real daylight."), true);
        } else {
            ServerLevel office = p.server.getLevel(OfficeRoomsDimension.LEVEL_KEY);
            if (office == null || !(office.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
                return;
            }
            OfficePlanner planner = gen.sampler(office.getChunkSource().randomState(), office).planner();
            Navigator.Target t = OfficePortalBlock.arrival(planner, pos.getX(), pos.getZ());
            if (t == null) {
                OfficeRooms.LOGGER.warn("Underside portal: no arrival room found");
                return;
            }
            p.getPersistentData().putLong("office_rooms_portal_cd", office.getGameTime() + 60);
            p.teleportTo(office, t.x(), t.y(), t.z(), t.yaw(), 0F);
            p.displayClientMessage(Component.literal("The hum again. It almost feels like home."), true);
        }
        level.playSound(null, pos, SoundEvents.PORTAL_TRAVEL, SoundSource.BLOCKS, 0.25F, 0.7F);
    }

    /** Sends a player into an office room near (x, z); used by the Underside and research-outpost gateways. */
    public static boolean sendToOffice(ServerPlayer p, int x, int z) {
        ServerLevel office = p.server.getLevel(OfficeRoomsDimension.LEVEL_KEY);
        if (office == null || !(office.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
            return false;
        }
        OfficePlanner planner = gen.sampler(office.getChunkSource().randomState(), office).planner();
        Navigator.Target t = OfficePortalBlock.arrival(planner, x, z);
        if (t == null) {
            OfficeRooms.LOGGER.warn("Gateway: no arrival room found");
            return false;
        }
        p.getPersistentData().putLong("office_rooms_portal_cd", office.getGameTime() + 60);
        p.teleportTo(office, t.x(), t.y(), t.z(), t.yaw(), 0F);
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        boolean ow = state.getValue(OVERWORLD);
        DustParticleOptions d = new DustParticleOptions(ow ? new Vector3f(0.45F, 0.85F, 0.4F) : new Vector3f(0.95F, 0.85F, 0.35F), 1.2F);
        for (int i = 0; i < 2; i++) {
            level.addParticle(d, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
        }
        if (r.nextInt(80) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.3F, ow ? 1.3F : 0.6F, false);
        }
    }
}
