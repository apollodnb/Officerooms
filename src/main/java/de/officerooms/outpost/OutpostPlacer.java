package de.officerooms.outpost;

import de.officerooms.OfficeRooms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 0.20.0: places research outposts when chunks are generated for the first time: beside some Ancient Cities, beside
 * some Jungle Temples (reached by a ladder shaft), very rarely in deep caves below y 0 and rarely in the Nether.
 * Existing structures (and the Office shrines) are never rebuilt; work is queued and done one outpost per tick.
 */
public final class OutpostPlacer {

    static final double ANCIENT = 0.6, JUNGLE = 0.4, CAVE = 1.0 / 1800, NETHER = 1.0 / 500, SURFACE = 1.0 / 2500, OCEAN = 1.0 / 900;   // 0.39.0: rare (ocean: only deep ocean chunks count)
    private static final ResourceLocation ANCIENT_CITY = ResourceLocation.parse("ancient_city"), JUNGLE_PYRAMID = ResourceLocation.parse("jungle_pyramid");

    record Job(ResourceKey<Level> dim, OutpostBuilder.Theme theme, BoundingBox box, ChunkPos chunk, long seed) {
    }

    private static final ConcurrentLinkedQueue<Job> QUEUE = new ConcurrentLinkedQueue<>();

    private OutpostPlacer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(OutpostPlacer.class);
    }

    static double roll(long seed, int x, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!e.isNewChunk() || !(e.getLevel() instanceof ServerLevel sl)) {
            return;
        }
        ChunkAccess c = e.getChunk();
        ChunkPos cp = c.getPos();
        long seed = sl.getSeed();
        if (sl.dimension() == Level.OVERWORLD) {
            var reg = sl.registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (Map.Entry<Structure, StructureStart> en : c.getAllStarts().entrySet()) {
                ResourceLocation id = reg.getKey(en.getKey());
                StructureStart st = en.getValue();
                if (id == null || !st.isValid()) {
                    continue;
                }
                if (id.equals(ANCIENT_CITY) && roll(seed, cp.x, cp.z, 1) < ANCIENT) {
                    QUEUE.add(new Job(sl.dimension(), OutpostBuilder.Theme.ANCIENT, st.getBoundingBox(), cp, seed));
                } else if (id.equals(JUNGLE_PYRAMID) && roll(seed, cp.x, cp.z, 2) < JUNGLE) {
                    QUEUE.add(new Job(sl.dimension(), OutpostBuilder.Theme.JUNGLE, st.getBoundingBox(), cp, seed));
                }
            }
            if (roll(seed, cp.x, cp.z, 3) < CAVE) {
                QUEUE.add(new Job(sl.dimension(), OutpostBuilder.Theme.CAVE, null, cp, seed));
            }
            if (roll(seed, cp.x, cp.z, 5) < SURFACE) {
                QUEUE.add(new Job(sl.dimension(), OutpostBuilder.Theme.SURFACE, null, cp, seed));
            }
            if (roll(seed, cp.x, cp.z, 6) < OCEAN) {
                QUEUE.add(new Job(sl.dimension(), OutpostBuilder.Theme.OCEAN, null, cp, seed));
            }
        } else if (sl.dimension() == Level.NETHER && roll(seed, cp.x, cp.z, 4) < NETHER) {
            QUEUE.add(new Job(sl.dimension(), OutpostBuilder.Theme.NETHER, null, cp, seed));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        if (QUEUE.isEmpty()) {
            return;
        }
        Job j = QUEUE.poll();
        ServerLevel l = e.getServer().getLevel(j.dim());
        if (l == null) {
            return;
        }
        try {
            BlockPos at = switch (j.theme()) {
                case ANCIENT -> ancient(l, j);
                case JUNGLE -> jungle(l, j);
                case CAVE -> cave(l, j);
                case NETHER -> nether(l, j);
                case SURFACE -> surface(l, j);
                case OCEAN -> ocean(l, j);
                default -> null;
            };
            if (at != null) {
                OfficeRooms.LOGGER.info("Research outpost ({}) placed at {}", j.theme(), at.toShortString());
                OutpostRegistry.add(l, j.theme().name().toLowerCase(java.util.Locale.ROOT), at);
            }
        } catch (RuntimeException ex) {
            OfficeRooms.LOGGER.error("Research outpost could not be placed", ex);
        }
    }

    private static boolean walkable(ServerLevel l, BlockPos p) {
        BlockState below = l.getBlockState(p.below());
        return below.isFaceSturdy(l, p.below(), Direction.UP) && below.getFluidState().isEmpty()
                && l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir() && l.getBlockState(p.above(2)).isAir();
    }

    private static Direction side(long seed, ChunkPos cp) {
        return Direction.from2DDataValue((int) (roll(seed, cp.x, cp.z, 7) * 4));
    }

    /** Beside the city: entrance faces the city, a tunnel runs to the first walkable spot inside it. */
    static BlockPos ancient(ServerLevel l, Job j) {
        BoundingBox b = j.box();
        Direction out = side(j.seed(), j.chunk());
        BlockPos c = b.getCenter();
        int ex = out.getStepX() > 0 ? b.maxX() : out.getStepX() < 0 ? b.minX() : c.getX();
        int ez = out.getStepZ() > 0 ? b.maxZ() : out.getStepZ() < 0 ? b.minZ() : c.getZ();
        BlockPos target = null;
        for (int s = 0; s < 40 && target == null; s++) {
            int x = ex - out.getStepX() * s, z = ez - out.getStepZ() * s;
            for (int y = b.minY(); y < b.minY() + 30; y++) {
                BlockPos p = new BlockPos(x, y, z);
                if (walkable(l, p)) {
                    target = p;
                    break;
                }
            }
        }
        int y = target != null ? target.getY() : b.minY() + 10;
        BlockPos door = new BlockPos(ex + out.getStepX() * 3, y, ez + out.getStepZ() * 3);
        BlockPos lever = OutpostBuilder.build(l, door, out.getOpposite(), OutpostBuilder.Theme.ANCIENT, j.seed() ^ j.chunk().toLong());
        if (target != null) {
            OutpostBuilder.tunnel(l, door.relative(out.getOpposite()), target, OutpostBuilder.Theme.ANCIENT);
        }
        return lever;
    }

    /** Buried next to the temple; a ladder shaft with an iron trapdoor leads down from the jungle floor. */
    static BlockPos jungle(ServerLevel l, Job j) {
        BoundingBox b = j.box();
        Direction out = side(j.seed(), j.chunk());
        BlockPos c = b.getCenter();
        int ex = out.getStepX() > 0 ? b.maxX() : out.getStepX() < 0 ? b.minX() : c.getX();
        int ez = out.getStepZ() > 0 ? b.maxZ() : out.getStepZ() < 0 ? b.minZ() : c.getZ();
        int y = b.minY() - 9;
        BlockPos door = new BlockPos(ex + out.getStepX() * 5, y, ez + out.getStepZ() * 5);
        BlockPos lever = OutpostBuilder.build(l, door, out.getOpposite(), OutpostBuilder.Theme.JUNGLE, j.seed() ^ j.chunk().toLong());
        BlockPos foot = door.relative(out.getOpposite(), 2);
        OutpostBuilder.tunnel(l, door.relative(out.getOpposite()), foot.relative(out.getOpposite()), OutpostBuilder.Theme.JUNGLE);
        OutpostBuilder.shaft(l, foot.relative(out.getOpposite()), out);
        return lever;
    }

    /** Deep in rock below y 0, opening onto a cave passage found in this chunk. */
    static BlockPos cave(ServerLevel l, Job j) {
        ChunkPos cp = j.chunk();
        for (int y = -48; y <= -8; y++) {
            for (int dx = 2; dx < 16; dx += 3) {
                for (int dz = 2; dz < 16; dz += 3) {
                    BlockPos p = new BlockPos(cp.getMinBlockX() + dx, y, cp.getMinBlockZ() + dz);
                    if (!walkable(l, p)) {
                        continue;
                    }
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos probe = p.relative(d, 9).above(2);
                        if (!l.getBlockState(probe).isAir() && !l.getBlockState(p.relative(d, 14).above(2)).isAir()) {
                            BlockPos door = p.relative(d, 3);
                            BlockPos lever = OutpostBuilder.build(l, door, d.getOpposite(), OutpostBuilder.Theme.CAVE, j.seed() ^ cp.toLong());
                            OutpostBuilder.tunnel(l, door.relative(d.getOpposite()), p, OutpostBuilder.Theme.CAVE);
                            return lever;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 0.39.0: a field station standing alone on flat, dry land (never on water, never inside another structure such as a village).
     * Rare: it also gives up when the ground is too uneven.
     */
    /** 0.39.0 (test command): run the natural placement of a surface or ocean station for one chunk now. */
    /** Why the last surface/ocean check failed (test command only). */
    public static String reason = "";

    public static BlockPos tryAt(ServerLevel l, OutpostBuilder.Theme th, ChunkPos cp) {
        Job j = new Job(l.dimension(), th, null, cp, l.getSeed());
        BlockPos at = th == OutpostBuilder.Theme.OCEAN ? ocean(l, j) : th == OutpostBuilder.Theme.SURFACE ? surface(l, j) : null;
        if (at != null) {
            OutpostRegistry.add(l, th.name().toLowerCase(java.util.Locale.ROOT), at);
        }
        return at;
    }

    static BlockPos surface(ServerLevel l, Job j) {
        ChunkPos cp = j.chunk();
        Direction out = side(j.seed(), cp);
        Direction in = out.getOpposite();
        Direction rgt = out.getClockWise();
        BlockPos o0 = new BlockPos(cp.getMinBlockX() + 8, 0, cp.getMinBlockZ() + 8);
        net.minecraft.world.level.levelgen.Heightmap.Types hm = net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES;
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        for (int u = -9; u <= 9; u += 3) {
            for (int v = -6; v <= 14; v += 4) {
                BlockPos c = o0.relative(rgt, u).relative(in, v);
                int y = l.getHeight(hm, c.getX(), c.getZ());
                BlockPos g = new BlockPos(c.getX(), y - 1, c.getZ());
                if (!l.getFluidState(g).isEmpty() || l.getBlockState(g).is(net.minecraft.tags.BlockTags.ICE)) {
                    reason = "water or ice at " + g.toShortString();
                    return null;                                            // water, lava or ice: no station
                }
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
            }
        }
        if (hi - lo > 5 || lo <= l.getSeaLevel()) {
            reason = "ground " + lo + ".." + hi + " (uneven or too low)";
            return null;
        }
        BlockPos door = new BlockPos(o0.getX(), hi, o0.getZ());
        for (int v = 0; v <= 12; v += 6) {
            if (l.structureManager().hasAnyStructureAt(door.relative(in, v))) {
                reason = "structure nearby";
                return null;
            }
        }
        return OutpostBuilder.build(l, door, out, OutpostBuilder.Theme.SURFACE, j.seed() ^ cp.toLong());
    }

    /** 0.39.0: a drilling rig over large, deep ocean: at least 15 blocks of water under the whole rig. */
    static BlockPos ocean(ServerLevel l, Job j) {
        ChunkPos cp = j.chunk();
        BlockPos c = new BlockPos(cp.getMinBlockX() + 8, l.getSeaLevel(), cp.getMinBlockZ() + 8);
        if (!l.getBiome(c).is(net.minecraft.tags.BiomeTags.IS_OCEAN)) {
            reason = "not ocean";
            return null;
        }
        Direction out = side(j.seed(), cp);
        Direction in = out.getOpposite();
        Direction rgt = out.getClockWise();
        for (int u = -12; u <= 12; u += 4) {
            for (int v = -8; v <= 17; v += 5) {
                BlockPos p = c.relative(rgt, u).relative(in, v);
                if (l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, p.getX(), p.getZ()) > l.getSeaLevel() - 15
                        || l.getFluidState(p.below()).isEmpty()) {
                    reason = "floor " + l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, p.getX(), p.getZ()) + ", water below sea level: " + !l.getFluidState(p.below()).isEmpty();
                    return null;                                            // land, shallows or ice nearby
                }
            }
        }
        BlockPos door = c.above(9);
        return OutpostBuilder.build(l, door, out, OutpostBuilder.Theme.OCEAN, j.seed() ^ cp.toLong());
    }

    /** Built into the Nether: on the first broad patch of solid ground with headroom in this chunk. */
    static BlockPos nether(ServerLevel l, Job j) {
        ChunkPos cp = j.chunk();
        for (int y = 36; y <= 100; y++) {
            for (int dx = 3; dx < 14; dx += 5) {
                for (int dz = 3; dz < 14; dz += 5) {
                    BlockPos p = new BlockPos(cp.getMinBlockX() + dx, y, cp.getMinBlockZ() + dz);
                    if (walkable(l, p) && l.getBlockState(p.above(4)).isAir() && l.getBlockState(p.above(6)).isAir()
                            && !l.getBlockState(p.below()).is(net.minecraft.world.level.block.Blocks.LAVA)) {
                        Direction d = side(j.seed(), cp);
                        return OutpostBuilder.build(l, p, d, OutpostBuilder.Theme.NETHER, j.seed() ^ cp.toLong());
                    }
                }
            }
        }
        return null;
    }
}
