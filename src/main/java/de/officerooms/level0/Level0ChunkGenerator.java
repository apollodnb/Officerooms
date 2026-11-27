package de.officerooms.level0;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static de.officerooms.level0.Level0Layout.*;

/** 0.21.0: generator of Level 0 (blocks are written in buildSurface, where the world seed is known). */
public final class Level0ChunkGenerator extends ChunkGenerator {

    public static final Codec<Level0ChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(i, Level0ChunkGenerator::new));
    private static final ResourceLocation MANILA_LOOT = ResourceLocation.fromNamespaceAndPath("office_rooms", "chests/manila_room");

    public Level0ChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor e, Blender b, RandomState rs, StructureManager sm, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager sm, RandomState rs, ChunkAccess chunk) {
        long seed = region.getSeed();
        BlockState[] col = new BlockState[HEIGHT];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                column(seed, x0 + dx, z0 + dz, col);
                for (int y = 0; y < HEIGHT; y++) {
                    if (!col[y].isAir()) {
                        chunk.setBlockState(pos.set(x0 + dx, y, z0 + dz), col[y], false);
                    }
                }
            }
        }
    }

    /** Fills one column; also used by {@link #getBaseColumn}. */
    static void column(long seed, int x, int z, BlockState[] col) {
        BlockState air = Blocks.AIR.defaultBlockState(), solid = Level0Blocks.WALLPAPER.get().defaultBlockState();
        BlockState carpet = Level0Blocks.CARPET.get().defaultBlockState(), ceiling = Level0Blocks.CEILING.get().defaultBlockState();
        java.util.Arrays.fill(col, solid);
        col[0] = Blocks.BEDROCK.defaultBlockState();
        col[HEIGHT - 1] = Blocks.BEDROCK.defaultBlockState();
        col[HEIGHT - 2] = Blocks.BEDROCK.defaultBlockState();
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL), lx = Math.floorMod(x, CELL), lz = Math.floorMod(z, CELL);
        int rx = Math.floorDiv(gx, R), rz = Math.floorDiv(gz, R), cx = Math.floorMod(gx, R), cz = Math.floorMod(gz, R), c = cx + cz * R;
        for (int k = 0; k < FLOORS; k++) {
            Plan p = plan(seed, k, rx, rz);
            byte t = p.type[c];
            int w = walk(k);
            boolean rampBelow = k > 0 && p.ladderExit[c] && ramp(seed, k - 1, gx, gz);
            col[floorY(k)] = p.ladderExit[c] && !rampBelow && lx == 1 && lz == 2 ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH) : carpet;
            col[ceil(k)] = ceilingAt(seed, k, gx, gz, lx, lz, x, z, p.light[c], ceiling);
            if (u(seed, x, z, k, 0xCA4) < 0.06) {
                col[floorY(k)] = Level0Blocks.CARPET_STAINED.get().defaultBlockState();
            }
            if (rampBelow && ((lx == 4 && lz == 1) || (lx == 5 && lz == 1) || (lx == 5 && lz == 2))) {
                col[floorY(k)] = air;                                       // 0.33.0: the ramp from below comes up through here
            }
            if (rampBelow && lx == 5 && lz == 3) {                          // 0.34.0: its last step lies in this floor
                col[floorY(k)] = Level0Blocks.RAMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH);
            }
            if (t == BLOCKED) {
                continue;                                                   // solid (reserved for the Manila puzzle above)
            }
            boolean wall;
            if (lx == 0 && lz == 0) {
                wall = p.pillar[c] || edgeE(seed, k, gx - 1, gz) != OPEN || edgeE(seed, k, gx - 1, gz - 1) != OPEN
                        || edgeS(seed, k, gx, gz - 1) != OPEN || edgeS(seed, k, gx - 1, gz - 1) != OPEN;
            } else if (lx == 0) {
                wall = !passable(edgeE(seed, k, gx - 1, gz), lz);
            } else if (lz == 0) {
                wall = !passable(edgeS(seed, k, gx, gz - 1), lx);
            } else {
                wall = false;
            }
            for (int y = w; y < ceil(k); y++) {
                col[y] = wall ? solid : air;
            }
            if (t == UNDER && (lx == 0 || lz == 0)) {
                // 0.34.0: under the pit field: the pits are shafts, joined at the bottom by low openings
                boolean door = (lx == 0 && lz == 3) || (lz == 0 && lx == 3);
                for (int y = w; y < ceil(k); y++) {
                    col[y] = door && y < w + 2 ? air : solid;
                }
                col[ceil(k)] = solid;                                       // the shaft walls run up without a ceiling seam
                continue;
            }
            if (wall) {
                double wv = u(seed, x, z, k, 0x3A11);                       // worn patches and spray-painted arrows
                if (wv < 0.05) {
                    col[w + 1 + (int) (wv * 40) % 2] = Level0Blocks.WALLPAPER_WORN.get().defaultBlockState();
                } else if (wv < 0.056) {
                    col[w + 1] = Level0Blocks.WALLPAPER_ARROW.get().defaultBlockState();
                } else if (wv < 0.06) {
                    col[w + 3] = Level0Blocks.LIGHT.get().defaultBlockState();          // 0.33.0: a light set into the wall
                } else if (wv < 0.063 && !(lx == 0 && lz == 0)) {
                    col[w + 1] = Blocks.GLASS.defaultBlockState();                     // a window into the next room
                    col[w + 2] = Blocks.GLASS.defaultBlockState();
                }
                continue;
            }
            outlet(seed, k, gx, gz, lx, lz, w, col);
            odd(seed, k, gx, gz, lx, lz, w, p.furn[c], p.light[c], col);
            // 0.25.1: the oversized log-and-plank tables (furn 1 and 2) are gone: they looked stacked, not liminal
            // ladders up to the next floor
            if (p.ladderUp[c] && ramp(seed, k, gx, gz)) {
                rampUp(k, lx, lz, col);                                     // 0.33.0: carpeted stairs up to the next floor
            } else if (p.ladderUp[c] && lx == 1 && lz == 1) {
                for (int y = w; y < ceil(k); y++) {
                    col[y] = solid;
                }
            }
            if (p.ladderUp[c] && !ramp(seed, k, gx, gz) && lx == 1 && lz == 2) {
                for (int y = w; y <= floorY(k + 1); y++) {
                    col[y] = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
                }
            }
            // 0.34.0: the pit field (was: Threshold): every cell is one deep square pit, only narrow walkways remain
            if (t == THRESH && lx != 0 && lz != 0) {
                col[floorY(k)] = air;
                col[ceil(k - 1)] = air;
            }
        }
        manilaColumn(seed, x, z, col);
    }

    /** 0.33.0: most connections between floors are carpeted stairs now (the classic ramp to another storey). */
    static boolean ramp(long seed, int k, int gx, int gz) {
        return Level0Layout.ramp(seed, k, gx, gz);
    }

    /** Seven steps along the north wall and up the east wall of the cell, through the ceiling into the floor above. */
    private static void rampUp(int k, int lx, int lz, BlockState[] col) {
        int[][] path = {{1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {5, 2}, {5, 3}};
        int w = walk(k);
        if ((lz == 0 || lz == 2) && lx >= 3 && lx <= 5 && !(lx == 5 && lz == 2)) {
            col[ceil(k)] = Blocks.AIR.defaultBlockState();                  // 0.38.1: ceiling beside the steep part (players drift sideways)
        }
        for (int i = 0; i < path.length; i++) {
            if (path[i][0] != lx || path[i][1] != lz) {
                continue;
            }
            Direction up = i < 5 ? Direction.EAST : Direction.SOUTH;   // 0.38.1: the corner step faces east (you step onto it from the west)
            for (int y = w; y < w + i; y++) {
                col[y] = Level0Blocks.CARPET.get().defaultBlockState();
            }
            if (i < 6) {
                col[w + i] = Level0Blocks.RAMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, up);
            }
            // 0.38.1: headroom from the third step on, one block higher: stepping up, the head is still over the previous step
            for (int y = w + i + 1; y <= w + i + 3 && y <= floorY(k + 1) && i >= 2; y++) {
                if (y >= ceil(k)) {
                    col[y] = Blocks.AIR.defaultBlockState();                // headroom through the ceiling
                }
            }
        }
    }

    static BlockState lightState(byte kind) {
        return switch (kind) {
            case 1 -> Level0Blocks.LIGHT.get().defaultBlockState();
            case 2 -> Level0Blocks.FLICKER.get().defaultBlockState();
            default -> Level0Blocks.LIGHT_OFF.get().defaultBlockState();
        };
    }

    /** 0.21.1: inconsistently placed lights (centre, offset, strips, pairs), dark zones, stains, vents, missing tiles. */
    static BlockState ceilingAt(long seed, int k, int gx, int gz, int lx, int lz, int x, int z, byte light, BlockState ceiling) {
        boolean dark = u(seed, Math.floorDiv(gx, 3), Math.floorDiv(gz, 3), k, 0xDA4) < 0.09;
        int pat = (int) (u(seed, gx, gz, k, 0x1A7) * 6);
        boolean here = switch (pat) {
            case 0, 1 -> lx == 3 && lz == 3;
            case 2 -> lz == 3 && (lx == 2 || lx == 4);
            case 3 -> lx == 3 && (lz == 2 || lz == 4);
            case 4 -> (lx == 1 && lz == 2) || (lx == 4 && lz == 4);
            default -> lx == 2 && lz == 4;
        };
        if (here) {
            return dark ? Level0Blocks.LIGHT_OFF.get().defaultBlockState() : lightState(light);
        }
        double v = u(seed, x, z, k, 0x5EED);
        if (v < 0.02) {
            return Level0Blocks.CEILING_VOID.get().defaultBlockState();
        }
        if (v < 0.035) {
            return Level0Blocks.VENT.get().defaultBlockState();
        }
        if (v < 0.07) {
            return Level0Blocks.CEILING_STAINED.get().defaultBlockState();
        }
        return ceiling;
    }

    /** 0.21.1: electrical outlets low on the walls, here and there. */
    private static void outlet(long seed, int k, int gx, int gz, int lx, int lz, int w, BlockState[] col) {
        if (col[w].isAir() && u(seed, gx * 7L + lx, gz * 7L + lz, k, 0x0E7) < 0.035) {
            Direction face = null;
            if (lx == 1 && !passable(edgeE(seed, k, gx - 1, gz), lz)) {
                face = Direction.EAST;
            } else if (lx == 5 && !passable(edgeE(seed, k, gx, gz), lz)) {
                face = Direction.WEST;
            } else if (lz == 1 && !passable(edgeS(seed, k, gx, gz - 1), lx)) {
                face = Direction.SOUTH;
            } else if (lz == 5 && !passable(edgeS(seed, k, gx, gz), lx)) {
                face = Direction.NORTH;
            }
            if (face != null) {
                col[w] = Level0Blocks.OUTLET.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, face);
            }
        }
    }

    private static BlockState stair(Direction d, boolean top) {
        return Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, d)
                .setValue(net.minecraft.world.level.block.StairBlock.HALF, top ? net.minecraft.world.level.block.state.properties.Half.TOP : net.minecraft.world.level.block.state.properties.Half.BOTTOM);
    }

    /** 0.21.1: odd furniture and paradoxical architecture inside a cell. */
    private static void odd(long seed, int k, int gx, int gz, int lx, int lz, int w, byte kind, byte light, BlockState[] col) {
        if (kind < 3) {
            return;
        }
        int v = (int) (u(seed, gx, gz, k, 0x0DD) * 4);
        Direction d = Direction.from2DDataValue(v);
        BlockState plank = Blocks.OAK_PLANKS.defaultBlockState();
        switch (kind) {
            case 3 -> {                                                     // chairs facing the wall, a lonely little table
                if (lx == 1 && lz == 1) col[w] = stair(Direction.NORTH, false);
                if (lx == 5 && lz == 5 && v % 2 == 0) col[w] = stair(Direction.EAST, false);
                if (lx == 5 && lz == 1 && v >= 2) col[w] = stair(Direction.SOUTH, false);
                if (lx == 1 && lz == 5) {
                    col[w] = Blocks.OAK_FENCE.defaultBlockState();
                    col[w + 1] = Blocks.OAK_PRESSURE_PLATE.defaultBlockState();
                }
            }
            case 4 -> {                                                     // 0.21.2: one chair in the middle of the room, facing nothing
                if (lx == 3 && lz == 3) col[w] = stair(d, false);
            }
            case 5 -> {                                                     // a staircase that ends in the ceiling
                if (lz == 1 && lx >= 1 && lx <= 4) {
                    for (int y = w; y < w + lx - 1; y++) col[y] = plank;
                    col[w + lx - 1] = stair(Direction.EAST, false);
                }
            }
            case 6 -> {                                                     // a door standing alone, leading nowhere
                if (lx == 3 && (lz == 2 || lz == 4)) {
                    for (int y = w; y <= w + 2; y++) col[y] = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
                }
                if (lx == 3 && lz == 3) {
                    col[w] = Blocks.OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.EAST)
                            .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
                    col[w + 1] = Blocks.OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.EAST)
                            .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                    col[w + 2] = Blocks.STRIPPED_OAK_LOG.defaultBlockState();
                }
            }
            case 7 -> {                                                     // a wall hanging from the ceiling, never reaching the floor
                if ((v % 2 == 0 ? lz : lx) == 3) {
                    for (int y = w + 2; y < ceil(k); y++) col[y] = Level0Blocks.WALLPAPER.get().defaultBlockState();
                }
            }
            case 8 -> {                                                     // the ceiling suddenly comes down to two blocks
                col[w + 2] = lx == 3 && lz == 3 ? lightState(light) : Level0Blocks.CEILING.get().defaultBlockState();
                for (int y = w + 3; y < ceil(k); y++) col[y] = Level0Blocks.CEILING.get().defaultBlockState();
            }
            case 9 -> {                                                     // one bookshelf, one chair facing it
                if (lx == 2 && lz == 5) {
                    col[w] = Blocks.BOOKSHELF.defaultBlockState();
                    col[w + 1] = Blocks.BOOKSHELF.defaultBlockState();
                }
                if (lx == 2 && lz == 4) col[w] = stair(Direction.NORTH, false);
            }
            case 10 -> {                                                    // 0.33.0: a lone pillar of wallpaper, off-centre
                if (lx == 2 + v % 2 && lz == 3) {
                    for (int y = w; y < ceil(k); y++) col[y] = Level0Blocks.WALLPAPER.get().defaultBlockState();
                }
            }
            case 11 -> {                                                    // a hole in the floor: down a floor, or into the dark
                boolean hole = (lx == 3 && lz == 3) || (v >= 2 && lx == 4 && lz == 3);
                if (hole) {
                    col[floorY(k)] = Blocks.AIR.defaultBlockState();
                    if (k > 0) {
                        col[ceil(k - 1)] = Blocks.AIR.defaultBlockState();
                    } else {
                        for (int y = 1; y < floorY(0); y++) col[y] = Blocks.AIR.defaultBlockState();   // a pit with no visible bottom
                    }
                }
            }
            case 12 -> {                                                    // half the room is one step higher, for no reason
                if ((v % 2 == 0 ? lz : lx) >= 3) col[w] = Level0Blocks.CARPET.get().defaultBlockState();
            }
            case 13 -> {                                                    // a sealed closet in the middle of the room
                if (lx >= 2 && lx <= 4 && lz >= 2 && lz <= 4 && !(lx == 3 && lz == 3)) {
                    for (int y = w; y < ceil(k); y++) col[y] = Level0Blocks.WALLPAPER.get().defaultBlockState();
                }
                if (lx == 3 && lz == 3) {
                    for (int y = w; y < ceil(k); y++) col[y] = Blocks.AIR.defaultBlockState();
                    col[w] = Level0Blocks.OUTLET.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, d);
                }
            }
            case 14 -> {                                                    // a soaked patch of carpet (the moisture has a source somewhere)
                if ((lx - 3) * (lx - 3) + (lz - 3) * (lz - 3) <= 2 + v) col[floorY(k)] = Level0Blocks.CARPET_WET.get().defaultBlockState();
            }
            case 15 -> {                                                    // a light switch that switches nothing
                if (lx == 1 && lz == 3 && col[w + 1].isAir()) {
                    col[w + 1] = Blocks.LEVER.defaultBlockState().setValue(net.minecraft.world.level.block.LeverBlock.FACE,
                            net.minecraft.world.level.block.state.properties.AttachFace.WALL).setValue(net.minecraft.world.level.block.LeverBlock.FACING, Direction.EAST);
                }
            }
            case 16 -> {                                                    // a wall across the room that stops one block short of the ceiling
                if ((v % 2 == 0 ? lx : lz) == 3) {
                    for (int y = w; y < ceil(k) - 1; y++) col[y] = Level0Blocks.WALLPAPER.get().defaultBlockState();
                }
            }
            default -> {
            }
        }
    }

    /** Manila Room lining, floor, hidden table, and the water passage puzzle below it. */
    private static void manilaColumn(long seed, int x, int z, BlockState[] col) {
        int gx = Math.floorDiv(x, CELL), gz = Math.floorDiv(z, CELL);
        int[] m = manila(seed, Math.floorDiv(gx, R), Math.floorDiv(gz, R));
        if (m == null) {
            return;
        }
        int ox = (Math.floorDiv(gx, R) * R + m[1]) * CELL, oz = (Math.floorDiv(gz, R) * R + m[2]) * CELL;
        int bx = x - ox, bz = z - oz;
        if (bx < 1 || bx > 17 || bz < 1 || bz > 17) {
            return;
        }
        int k = m[0], w = walk(k);
        BlockState air = Blocks.AIR.defaultBlockState(), lining = Level0Blocks.MANILA_WALLPAPER.get().defaultBlockState();
        col[floorY(k)] = Level0Blocks.MANILA_CARPET.get().defaultBlockState();
        boolean edge = bx == 1 || bx == 17 || bz == 1 || bz == 17;
        int e = m[4] * 6 + 3;                                                // entrance gap position along the side
        boolean gap = switch (m[3]) {
            case 0 -> bz == 1 && bx == e;
            case 1 -> bx == 17 && bz == e;
            case 2 -> bz == 17 && bx == e;
            default -> bx == 1 && bz == e;
        };
        for (int y = w; y < ceil(k); y++) {
            col[y] = edge && !(gap && y < w + 2) ? lining : air;
        }
        if (edge) {
            return;
        }
        // big table hiding the 1 × 1 hole in the corner
        if (bx >= 2 && bx <= 5 && bz >= 2 && bz <= 5) {
            if ((bx == 2 || bx == 5) && (bz == 2 || bz == 5)) {
                col[w] = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
                col[w + 1] = Blocks.STRIPPED_BIRCH_LOG.defaultBlockState();
            }
            col[w + 2] = Blocks.BIRCH_PLANKS.defaultBlockState();
        }
        if (bx == 3 && bz == 3) {                                            // the shaft: 2 deep
            col[w - 1] = air;
            col[w - 2] = air;
        }
        if (bz == 3 && bx >= 4 && bx <= 8) {                                 // 1 × 1 passage: only swimmable
            col[w - 2] = air;
        }
        if (bx >= 9 && bx <= 11 && bz >= 2 && bz <= 4) {                     // exit chamber
            for (int y = w - 4; y <= w - 2; y++) {
                col[y] = air;
            }
            col[w - 5] = Level0Blocks.MANILA_CARPET.get().defaultBlockState();
            if (bx == 11 && bz == 3) {
                col[w - 4] = Level0Blocks.EXIT.get().defaultBlockState();
                col[w - 3] = Level0Blocks.EXIT.get().defaultBlockState();
            }
        }
    }

    // --- 0.21.2: optional furniture from MrCrayfish's Refurbished Furniture (used only if that mod is installed) ---
    private static final String RF = "refurbished_furniture";

    /** A Refurbished Furniture block by id, turned to {@code facing}, or null if the mod or the block is missing. */
    static BlockState rf(String id, Direction facing) {
        net.minecraft.world.level.block.Block b = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(RF, id));
        if (b == null || b == Blocks.AIR) {
            return null;
        }
        BlockState s = b.defaultBlockState();
        for (net.minecraft.world.level.block.state.properties.Property<?> p : s.getProperties()) {
            if (p.getName().equals("facing") && p instanceof net.minecraft.world.level.block.state.properties.DirectionProperty dp && dp.getPossibleValues().contains(facing)) {
                s = s.setValue(dp, facing);
            }
        }
        return s;
    }

    private static void put(WorldGenLevel level, BlockPos p, BlockState s) {
        if (s != null) {
            level.setBlock(p, s, 2);
        }
    }

    /** Replaces the vanilla stand-ins of the odd furniture in this chunk with Refurbished Furniture pieces. */
    private static void refurbish(WorldGenLevel level, long seed, int x0, int z0) {
        for (int k = 0; k < FLOORS; k++) {
            int w = walk(k);
            for (int gx = Math.floorDiv(x0, CELL); gx <= Math.floorDiv(x0 + 15, CELL); gx++) {
                for (int gz = Math.floorDiv(z0, CELL); gz <= Math.floorDiv(z0 + 15, CELL); gz++) {
                    Plan p = plan(seed, k, Math.floorDiv(gx, R), Math.floorDiv(gz, R));
                    byte kind = p.furn[Math.floorMod(gx, R) + Math.floorMod(gz, R) * R];
                    if (kind != 3 && kind != 4 && kind != 9) {
                        continue;
                    }
                    int v = (int) (u(seed, gx, gz, k, 0x0DD) * 4);
                    int bx = gx * CELL, bz = gz * CELL;
                    java.util.function.BiConsumer<int[], BlockState> at = (l, s) -> {
                        BlockPos q = new BlockPos(bx + l[0], w + l[1], bz + l[2]);
                        if (q.getX() >= x0 && q.getX() < x0 + 16 && q.getZ() >= z0 && q.getZ() < z0 + 16) {
                            put(level, q, s);
                        }
                    };
                    switch (kind) {
                        case 3 -> {
                            at.accept(new int[]{1, 0, 1}, rf("oak_chair", Direction.NORTH));
                            if (v % 2 == 0) at.accept(new int[]{5, 0, 5}, rf("oak_chair", Direction.EAST));
                            if (v >= 2) at.accept(new int[]{5, 0, 1}, rf("oak_chair", Direction.SOUTH));
                            BlockState table = rf("oak_table", Direction.NORTH);
                            if (table != null) {
                                at.accept(new int[]{1, 0, 5}, table);
                                at.accept(new int[]{1, 1, 5}, Blocks.AIR.defaultBlockState());
                            }
                        }
                        case 4 -> {
                            BlockState sofa = rf(v % 2 == 0 ? "yellow_sofa" : "light_gray_sofa", Direction.from2DDataValue(v));
                            at.accept(new int[]{3, 0, 3}, sofa != null ? sofa : rf("oak_chair", Direction.from2DDataValue(v)));
                        }
                        default -> {
                            at.accept(new int[]{2, 0, 4}, rf("oak_chair", Direction.NORTH));
                            at.accept(new int[]{2, 0, 5}, rf("oak_drawer", Direction.SOUTH));
                            at.accept(new int[]{2, 1, 5}, rf("oak_storage_cabinet", Direction.SOUTH));
                        }
                    }
                }
            }
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager sm) {
        long seed = level.getSeed();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        if (net.neoforged.fml.ModList.get().isLoaded(RF)) {
            refurbish(level, seed, x0, z0);
        }
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (int[] corner : new int[][]{{x0, z0}, {x0 + 15, z0}, {x0, z0 + 15}, {x0 + 15, z0 + 15}}) {
            int rx = Math.floorDiv(Math.floorDiv(corner[0], CELL), R), rz = Math.floorDiv(Math.floorDiv(corner[1], CELL), R);
            if (!done.add(((long) rx << 32) ^ (rz & 0xffffffffL))) {
                continue;
            }
            int[] m = manila(seed, rx, rz);
            if (m == null) {
                continue;
            }
            int cxp = (rx * R + m[1]) * CELL + 9, czp = (rz * R + m[2]) * CELL + 9;
            if (cxp >= x0 && cxp < x0 + 16 && czp >= z0 && czp < z0 + 16) {
                BlockPos p = new BlockPos(cxp, walk(m[0]), czp);
                level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.from2DDataValue(m[3])), 2);
                if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity ch) {
                    ch.setLootTable(MANILA_LOOT, seed ^ p.asLong());
                }
            }
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion r, long s, RandomState rs, BiomeManager bm, StructureManager sm, ChunkAccess c, GenerationStep.Carving st) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public void createStructures(RegistryAccess ra, ChunkGeneratorStructureState ss, StructureManager sm, ChunkAccess c, StructureTemplateManager tm) {
    }

    @Override
    public int getGenDepth() {
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState rs) {
        return HEIGHT;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState rs) {
        BlockState[] col = new BlockState[HEIGHT];
        java.util.Arrays.fill(col, Level0Blocks.WALLPAPER.get().defaultBlockState());
        return new NoiseColumn(0, col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState rs, BlockPos pos) {
        info.add("Level 0: floor " + floorOf(pos.getY()));
    }
}
