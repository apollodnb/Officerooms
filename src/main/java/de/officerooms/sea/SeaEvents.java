package de.officerooms.sea;

import de.officerooms.core.Hashing;
import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 0.42.0: getting on and off the SS Fun =), and the Mascot's spawning.
 *
 * <ul>
 *     <li>A departure gate in the Office Rooms (rare special room) boards you onto one of the ships. Which one depends on
 *     the gate.</li>
 *     <li>The red handle beside a lifeboat on the promenade (deck 10) lowers it into the fog: you come back somewhere in the
 *     Office Rooms, far from the gate. The first trip off a ship brings a small reward.</li>
 * </ul>
 */
public final class SeaEvents {

    public static final ResourceKey<Level> LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "the_sea"));
    static final String FROM_X = OfficeRoomsIds.MOD_ID + ".gate_x", FROM_Z = OfficeRoomsIds.MOD_ID + ".gate_z";
    static final String ABOARD = OfficeRoomsIds.MOD_ID + ".aboard";

    private SeaEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SeaEvents::onLevelTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> {
            ServerLevel sea = e.getServer().getLevel(LEVEL_KEY);
            if (sea != null) {
                SeaChunkGenerator.seedOf(sea);                      // /locate needs the ship seed before the first sea chunk
            }
        });
    }

    /** The ship that a gate at this position boards onto. */
    public static Ship shipFor(long seed, int x, int z) {
        long h = Hashing.hash(seed, x >> 4, z >> 4, 0x6A7E);
        int gx = (int) Math.floorMod(h, 401) - 200, gz = (int) Math.floorMod(h >>> 24, 401) - 200;
        for (int k = 0; k < 40; k++) {
            Ship s = Ship.of(seed, gx + k, gz);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public static void board(ServerPlayer p, BlockPos gate) {
        ServerLevel sea = p.server.getLevel(LEVEL_KEY);
        if (sea == null || p.isOnPortalCooldown()) {
            return;
        }
        Ship s = shipFor(SeaChunkGenerator.seedOf(sea), gate.getX(), gate.getZ());
        if (s == null) {
            return;
        }
        if (p.level().dimension() == OfficeRoomsDimension.LEVEL_KEY) {
            p.getPersistentData().putInt(FROM_X, gate.getX());
            p.getPersistentData().putInt(FROM_Z, gate.getZ());
        }
        p.getPersistentData().putBoolean(ABOARD, true);
        p.setPortalCooldown();
        int[] e = s.embark();
        p.level().playSound(null, gate, ModSounds.SHIP_HORN.get(), SoundSource.AMBIENT, 1.5F, 0.9F);
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
        p.teleportTo(sea, e[0] + 0.5, e[1], e[2] + 0.5, s.flip ? 90F : 270F, 0F);
        sea.playSound(null, e[0], e[1], e[2], ModSounds.SHIP_HORN.get(), SoundSource.AMBIENT, 2.0F, 0.8F);
        p.displayClientMessage(Component.literal("Now boarding: SS Fun =)").withStyle(ChatFormatting.GOLD), true);
        de.officerooms.quest.Milestones.grant(p, "ship_boarded");
    }

    /** The lifeboat handle: off the ship and back to the Office Rooms. */
    public static void launch(ServerPlayer p, BlockPos pos) {
        if (p.level().dimension() != LEVEL_KEY || p.isOnPortalCooldown()) {
            p.displayClientMessage(Component.literal("The handle does not move."), true);
            return;
        }
        int fx = p.getPersistentData().getInt(FROM_X), fz = p.getPersistentData().getInt(FROM_Z);
        p.level().playSound(null, pos, ModSounds.LIFEBOAT_DROP.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        boolean sent = false;
        for (int a = 0; a < 6 && !sent; a++) {
            double ang = p.getRandom().nextDouble() * Math.PI * 2, d = 600 + p.getRandom().nextDouble() * 1800;
            sent = de.officerooms.block.UndersidePortalBlock.sendToOffice(p, fx + (int) (Math.cos(ang) * d), fz + (int) (Math.sin(ang) * d));
        }
        if (!sent) {
            p.displayClientMessage(Component.literal("The lifeboat is stuck."), true);
            return;
        }
        p.setPortalCooldown();
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        de.officerooms.quest.Milestones.grant(p, "ship_escaped");
        if (p.getPersistentData().getBoolean(ABOARD)) {
            p.getPersistentData().remove(ABOARD);
            ItemStack reward = new ItemStack(de.officerooms.item.ModItems.RESEARCH_FILE.get(), 2);
            if (!p.getInventory().add(reward)) {
                p.drop(reward, false);
            }
            p.displayClientMessage(Component.literal("The lifeboat drifted through the fog for a long time. You kept the life jacket's emergency kit: 2 × Research File."), false);
        } else {
            p.displayClientMessage(Component.literal("The lifeboat drifted through the fog for a long time."), true);
        }
    }

    // --- the Mascot --------------------------------------------------------------------------------------------------

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != LEVEL_KEY
                || level.getGameTime() % 100 != 31) {
            return;
        }
        if (!OfficeRoomsConfig.mobsAllowed(level)) {
            return;
        }
        long seed = SeaChunkGenerator.seedOf(level);
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative()) {
                continue;
            }
            Ship s = Ship.at(seed, p.getBlockX(), p.getBlockZ());
            int d = (p.getBlockY() - Ship.fy(0)) / Ship.DH;
            if (s == null || d < 0 || d > 10 || level.random.nextDouble() >= OfficeRoomsConfig.MASCOT_CHANCE.get()) {
                continue;
            }
            List<Mascot> near = level.getEntitiesOfClass(Mascot.class, new AABB(p.blockPosition()).inflate(80, 24, 80));
            if (near.size() >= OfficeRoomsConfig.MASCOT_MAX.get()) {
                continue;
            }
            if (level.random.nextDouble() < 0.3) {
                spawnWaving(level, s, p, d);
            } else {
                spawnInVent(level, p, d);
            }
        }
    }

    /** Test command: a Mascot in a vent near the player. */
    public static boolean spawnMascotNear(ServerLevel level, ServerPlayer p, int d) {
        return spawnInVent(level, p, d);
    }

    /** Test command (0.43.0): a waving Mascot down the corridor the player looks along. */
    public static boolean spawnMascotWaving(ServerLevel level, ServerPlayer p, int d) {
        Ship s = Ship.at(SeaChunkGenerator.seedOf(level), p.getBlockX(), p.getBlockZ());
        return s != null && spawnWaving(level, s, p, d);
    }

    static boolean spawnInVent(ServerLevel level, ServerPlayer p, int d) {
        int cy = Ship.fy(d) + 4;
        java.util.List<BlockPos> vents = new java.util.ArrayList<>();
        for (int dx = -28; dx <= 28; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                BlockPos v = new BlockPos(p.getBlockX() + dx, cy, p.getBlockZ() + dz);
                if (dx * dx + dz * dz >= 100 && level.isLoaded(v) && level.getBlockState(v).is(ShipBlocks.VENT.get())) {
                    vents.add(v);
                }
            }
        }
        if (!vents.isEmpty()) {
            BlockPos v = vents.get(level.random.nextInt(vents.size()));
            int x = v.getX(), z = v.getZ();
            Mascot m = de.officerooms.entity.ModEntities.MASCOT.get().create(level);
            if (m == null) {
                return false;
            }
            m.moveTo(x + 0.5, Ship.fy(d) + 1, z + 0.5, level.random.nextFloat() * 360, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(v), MobSpawnType.EVENT, null, null);
            m.setMode(Mascot.LURK);
            level.addFreshEntity(m);
            return true;
        }
        return false;
    }

    static boolean spawnWaving(ServerLevel level, Ship s, ServerPlayer p, int d) {
        // somewhere down the corridor the player is looking along
        net.minecraft.world.phys.Vec3 look = p.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) {
            return false;
        }
        look = look.normalize();
        for (int dist = 34; dist >= 18; dist -= 2) {
            BlockPos q = BlockPos.containing(p.getX() + look.x * dist, Ship.fy(d) + 1, p.getZ() + look.z * dist);
            if (!level.isLoaded(q) || !level.getBlockState(q).isAir() || !level.getBlockState(q.above()).isAir()
                    || !level.getBlockState(q.below()).isSolid()) {
                continue;
            }
            Mascot m = de.officerooms.entity.ModEntities.MASCOT.get().create(level);
            if (m == null) {
                return false;
            }
            m.moveTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, 0, 0);
            if (!p.hasLineOfSight(m)) {
                continue;
            }
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(q), MobSpawnType.EVENT, null, null);
            m.setMode(Mascot.WAVE);
            level.addFreshEntity(m);
            return true;
        }
        return false;
    }
}
