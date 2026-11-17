package de.officerooms.entity;

import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.megastructure.Megastructures;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 0.24.0: Deathmoth spawning. Like the other mobs it spawns by population, not on a timer aimed at a player,
 * and only in the Office Rooms dimension (the Overworld has no Office Rooms mobs).
 *
 * <ul>
 *     <li> {@link #populateOffice} keeps about one Deathmoth per {@code roomsPerMob} rooms around
 *     each player (at most {@code maxNearPlayer}), spread over dark walls anywhere in the area: offices, hotel,
 *     industrial halls, the neighbourhood, Level Fun.</li>
 *     <li>Never in a Maze or Poolrooms region (the whole region column), never in Level 0, the Underside, the Nether
 *     or the End. These rules apply to every natural spawn path.</li>
 * </ul>
 */
public final class DeathmothDirector {

    private DeathmothDirector() {
    }

    public static void register() {
        // spawning is driven by MobSpawner (Office Rooms dimension only)
    }

    /** Direction from an adjacent sturdy wall into {@code air}, or null. */
    public static Direction wallNormal(net.minecraft.world.level.LevelReader level, BlockPos air) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos w = air.relative(d);
            if (level.getBlockState(w).isFaceSturdy(level, w, d.getOpposite())) {
                return d.getOpposite();
            }
        }
        return null;
    }

    /** Office Rooms: keeps the Deathmoth population around one player. Called by MobSpawner every 3 s. */
    public static void populateOffice(ServerLevel level, ServerPlayer player, int rooms) {
        if (!OfficeRoomsConfig.DEATHMOTH_ENABLED.get() || player.isCreative() || forbidden(level, player.blockPosition())) {
            return;
        }
        double expected = rooms / (double) OfficeRoomsConfig.DEATHMOTH_ROOMS_PER_MOB.get();
        int target = (int) expected + (level.random.nextDouble() < expected - (int) expected ? 1 : 0);
        target = Math.min(target, OfficeRoomsConfig.DEATHMOTH_MAX_NEAR_PLAYER.get());
        int existing = level.getEntitiesOfClass(Deathmoth.class, player.getBoundingBox().inflate(48, 14, 48)).size();
        if (existing < target) {
            trySpawn(level, player, 16, 46);
        }
    }

    /** Forbidden places: Maze and Poolrooms regions of the Office Rooms. */
    public static boolean forbidden(ServerLevel level, BlockPos pos) {
        if (level.dimension() != OfficeRoomsDimension.LEVEL_KEY) {
            return false;
        }
        if (!(level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
            return false;
        }
        OfficePlanner p = gen.sampler(level.getChunkSource().randomState(), level).planner();
        if (p == null) {
            return false;
        }
        Megastructures.Kind k = Megastructures.kindOf(p, p.regionOf(pos.getX()), p.regionOf(pos.getZ()));
        return k == Megastructures.Kind.MAZE || k == Megastructures.Kind.POOLROOMS
                || p.maze.contains(pos.getX(), pos.getY(), pos.getZ()) || p.pools.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    /** Tries to place one Deathmoth near the player. Returns true on success. */
    public static boolean trySpawn(ServerLevel level, ServerPlayer p) {
        return trySpawn(level, p, 9, 26);
    }

    public static boolean trySpawn(ServerLevel level, ServerPlayer p, int minD, int maxD) {
        if (level.dimension() != OfficeRoomsDimension.LEVEL_KEY) {
            return false;                                         // Office Rooms only
        }
        if (forbidden(level, p.blockPosition())) {
            return false;
        }
        var rnd = level.random;
        BlockPos best = null;
        Direction bestN = null;
        int bestLight = 99;
        Vec3 eye = p.getEyePosition();
        for (int i = 0; i < 96; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double dist = minD + rnd.nextDouble() * (maxD - minD);
            int dy = maxD > 30 ? rnd.nextInt(25) - 12 : rnd.nextInt(7) - 2;
            BlockPos a = BlockPos.containing(p.getX() + Math.cos(ang) * dist, p.getY() + dy, p.getZ() + Math.sin(ang) * dist);
            if (!level.isLoaded(a)) {
                continue;
            }
            BlockState s = level.getBlockState(a);
            if (!s.getCollisionShape(level, a).isEmpty() || !s.getFluidState().isEmpty()) {
                continue;
            }
            Direction n = wallNormal(level, a);
            if (n == null || !floorBelow(level, a)) {
                continue;
            }
            Vec3 c = Vec3.atCenterOf(a);
            if (level.clip(new ClipContext(eye, c, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS) {
                continue;                                         // the player could see it appear
            }
            if (forbidden(level, a)) {
                continue;
            }
            int light = level.getMaxLocalRawBrightness(a);
            if (light < bestLight) {
                best = a;
                bestN = n;
                bestLight = light;
                if (light <= 3) {
                    break;
                }
            }
        }
        // dark spots preferred; bright ones only rarely
        // the darkest wall found; lit places (rooms with working ceiling panels) only sometimes
        if (best == null || (bestLight > 7 && (bestLight > 12 || rnd.nextInt(2) != 0))) {
            return false;
        }
        Deathmoth m = ModEntities.DEATHMOTH.get().create(level);
        if (m == null) {
            return false;
        }
        m.attachTo(best, bestN, 0);
        if (!level.noCollision(m, m.getBoundingBox())) {
            return false;
        }
        level.addFreshEntity(m);
        return true;
    }

    /** Solid ground 1–4 blocks below (walls of rooms and caves, not the middle of a huge shaft). */
    private static boolean floorBelow(ServerLevel level, BlockPos a) {
        for (int k = 1; k <= 4; k++) {
            BlockPos q = a.below(k);
            if (!level.getBlockState(q).getCollisionShape(level, q).isEmpty()) {
                return k >= 1;
            }
        }
        return false;
    }
}
