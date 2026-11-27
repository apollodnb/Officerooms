package de.officerooms.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.officerooms.OfficeRooms;
import de.officerooms.compat.FurnitureBuilder;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.furnish.FurnishingPlanner;
import de.officerooms.furnish.Placement;
import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;
import de.officerooms.room.structure.BlockKind;
import de.officerooms.room.structure.BlockSampler;
import net.minecraft.Util;
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
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Chunk-Generator der Office-Rooms-Dimension.
 *
 * <p>Jeder Chunk fragt für seine 16 × 16 Spalten den {@link BlockSampler} und schreibt die
 * Blöcke direkt in die Chunk-Sektionen (ohne Block-Updates). Die gesamte Architektur wird in
 * {@code fillFromNoise} erzeugt; es gibt keine Features, keine Strukturen, keine Carver und keine
 * Mobs. Weil der Sampler eine reine Funktion der Koordinaten ist, ist das Ergebnis unabhängig
 * von Reihenfolge und Thread der Chunk-Erzeugung.</p>
 */
public final class OfficeChunkGenerator extends ChunkGenerator {

    public static final Codec<OfficeChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            GenSettingsCodec.CODEC.optionalFieldOf("settings", GenSettings.DEFAULT).forGetter(g -> g.settings)
    ).apply(i, i.stable(OfficeChunkGenerator::new)));

    private static final ResourceLocation SEED_KEY = ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, "office_layout");

    private static BlockState[] states;

    private final GenSettings settings;
    private final Map<Long, BlockSampler> samplers = new ConcurrentHashMap<>();
    private final Map<OfficePlanner, FurnishingPlanner> furnishers = new ConcurrentHashMap<>();
    private final Map<OfficePlanner, de.officerooms.special.SpecialDecorator> decorators = new ConcurrentHashMap<>();
    private volatile BlockSampler last;

    public OfficeChunkGenerator(BiomeSource biomeSource, GenSettings settings) {
        super(biomeSource);
        this.settings = settings;
    }

    public GenSettings settings() {
        return settings;
    }

    /** Blockzuordnung der abstrakten Blockarten. */
    public static BlockState[] states() {
        BlockState[] s = states;
        if (s == null) {
            s = new BlockState[BlockKind.COUNT];
            s[BlockKind.AIR] = Blocks.AIR.defaultBlockState();
            s[BlockKind.BEDROCK] = Blocks.BEDROCK.defaultBlockState();
            s[BlockKind.FLOOR] = Blocks.SMOOTH_QUARTZ.defaultBlockState();
            s[BlockKind.WALL] = Blocks.QUARTZ_BLOCK.defaultBlockState();
            s[BlockKind.PILLAR] = Blocks.QUARTZ_PILLAR.defaultBlockState();
            s[BlockKind.FILL] = Blocks.WHITE_CONCRETE.defaultBlockState();
            s[BlockKind.CEILING] = Blocks.SMOOTH_QUARTZ.defaultBlockState();
            s[BlockKind.CARPET] = Blocks.GRAY_CARPET.defaultBlockState();
            s[BlockKind.FLOOR_WOOL] = Blocks.GRAY_WOOL.defaultBlockState();
            s[BlockKind.GLASS] = Blocks.GLASS.defaultBlockState();
            s[BlockKind.PARAPET] = Blocks.QUARTZ_BLOCK.defaultBlockState();
            s[BlockKind.PARAPET_TOP] = Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState();
            Direction[] dirs = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
            for (int d = 0; d < 4; d++) {
                s[BlockKind.STAIR_E + d] = Blocks.QUARTZ_STAIRS.defaultBlockState().setValue(StairBlock.FACING, dirs[d]);
            }
            s[BlockKind.ELEVATOR] = de.officerooms.block.ModBlocks.ELEVATOR_FLOOR.get().defaultBlockState();   // 0.26.0
            s[BlockKind.ELEV_WALL] = de.officerooms.block.ModBlocks.ELEVATOR_WALL.get().defaultBlockState();
            s[BlockKind.ELEV_LIGHT] = de.officerooms.block.ModBlocks.CEILING_PANEL.get().defaultBlockState();
            Direction[] facing = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
            for (int d = 0; d < 4; d++) {
                s[BlockKind.ELEV_PANEL_E + d] = de.officerooms.block.ModBlocks.ELEVATOR_PANEL.get().defaultBlockState()
                        .setValue(de.officerooms.block.ElevatorPanelBlock.FACING, facing[d]);
            }
            s[BlockKind.SECURITY] = de.officerooms.block.ModBlocks.SECURITY_WALL.get().defaultBlockState();
            states = s;
        }
        return s;
    }

    /** Rückwärtszuordnung Blockzustand → Blockart (für die Verifikation im Spiel). */
    public static Map<BlockState, Integer> reverseStates() {
        Map<BlockState, Integer> m = new IdentityHashMap<>();
        BlockState[] s = states();
        for (int k = s.length - 1; k >= 0; k--) {
            m.put(s[k], k);
        }
        // FLOOR und CEILING sind derselbe Block, PARAPET und WALL ebenfalls.
        // Grauer Beton liegt als Bodenbelag unter Möbeln an Stelle des Quarzbodens.
        m.put(Blocks.GRAY_CONCRETE.defaultBlockState(), (int) BlockKind.FLOOR);
        return m;
    }

    /** Sampler für den Weltseed (aus dem RandomState abgeleitet) und die Dimensionshöhe. */
    public BlockSampler sampler(RandomState randomState, LevelHeightAccessor level) {
        long seed = randomState.getOrCreateRandomFactory(SEED_KEY).at(0, 0, 0).nextLong();
        int minY = level.getMinBuildHeight();
        int height = level.getHeight();
        int cap = ContentVersion.cap();                              // 0.43.0: the version stored with the world
        long key = seed ^ ((long) minY << 48) ^ ((long) height << 32) ^ ((long) Math.min(cap, 1000) << 20);
        BlockSampler s = samplers.computeIfAbsent(key, k -> new BlockSampler(new OfficePlanner(seed, ContentVersion.apply(settings), minY, height)));
        last = s;
        return s;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("office_rooms_fill", () -> fill(randomState, chunk)), executor);
    }

    private static final java.util.concurrent.atomic.AtomicLong PROF_LIGHT = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong PROF_SPECIAL = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong PROF_FILL = new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong PROF_N = new java.util.concurrent.atomic.AtomicLong();

    private ChunkAccess fill(RandomState randomState, ChunkAccess chunk) {
        long t0 = System.nanoTime();
        try {
            return fill0(randomState, chunk);
        } finally {
            PROF_FILL.addAndGet(System.nanoTime() - t0);
        }
    }

    private ChunkAccess fill0(RandomState randomState, ChunkAccess chunk) {
        BlockSampler sampler = sampler(randomState, chunk);
        BlockState[] st = states();
        int minY = chunk.getMinBuildHeight();
        int height = chunk.getHeight();
        byte[] col = new byte[height];
        int bx = chunk.getPos().getMinBlockX();
        int bz = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        for (LevelChunkSection s : sections) {
            s.acquire();
        }
        try {
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    sampler.column(bx + dx, bz + dz, col);
                    for (int i = 0; i < height; i++) {
                        byte k = col[i];
                        if (k == BlockKind.AIR) {
                            continue;
                        }
                        int y = minY + i;
                        sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, st[k], false);
                    }
                }
            }
            poolrooms(sampler, chunk, sections, bx, bz);
            neighbourhood(sampler, chunk, sections, bx, bz);
            maze(sampler, chunk, sections, bx, bz);
            hotel(sampler, chunk, sections, bx, bz);
            industrial(sampler, chunk, sections, bx, bz);
            levelFun(sampler, chunk, sections, bx, bz);
            cinema(sampler, chunk, sections, bx, bz);
            mall(sampler, chunk, sections, bx, bz);
        } finally {
            for (LevelChunkSection s : sections) {
                s.release();
            }
        }
        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
        return chunk;
    }

    private static BlockState[] poolStates;

    private static BlockState[] poolStates() {
        BlockState[] ps = poolStates;
        if (ps == null) {
            de.officerooms.megastructure.Poolrooms.Part[] parts = de.officerooms.megastructure.Poolrooms.Part.values();
            ps = new BlockState[parts.length];
            BlockState tile = de.officerooms.block.ModBlocks.POOL_TILE.get().defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.AIR.ordinal()] = Blocks.AIR.defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.TILE.ordinal()] = tile;
            ps[de.officerooms.megastructure.Poolrooms.Part.FLOOR_TILE.ordinal()] = de.officerooms.block.ModBlocks.POOL_FLOOR_TILE.get().defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.WATER.ordinal()] = Blocks.WATER.defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.LIGHT_PANEL.ordinal()] = de.officerooms.block.ModBlocks.CEILING_PANEL.get().defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.LIGHT_INVISIBLE.ordinal()] = Blocks.LIGHT.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 15);
            ps[de.officerooms.megastructure.Poolrooms.Part.SEA_LANTERN.ordinal()] = Blocks.SEA_LANTERN.defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.RAILING.ordinal()] = de.officerooms.block.ModBlocks.POOL_TILE_SLAB.get().defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.POST.ordinal()] = Blocks.BIRCH_FENCE.defaultBlockState();
            Direction[] dirs = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
            for (int d = 0; d < 4; d++) {
                ps[de.officerooms.megastructure.Poolrooms.Part.STAIR_E.ordinal() + d] = de.officerooms.block.ModBlocks.POOL_TILE_STAIRS.get()
                        .defaultBlockState().setValue(StairBlock.FACING, dirs[d]);
                ps[de.officerooms.megastructure.Poolrooms.Part.SLIDE_E.ordinal() + d] = de.officerooms.megastructure.PoolBlocks.SLIDE.get()
                        .defaultBlockState().setValue(StairBlock.FACING, dirs[d]);
                ps[de.officerooms.megastructure.Poolrooms.Part.PIPE_E.ordinal() + d] = de.officerooms.megastructure.PoolBlocks.PIPE.get()
                        .defaultBlockState().setValue(de.officerooms.block.DecorBlock.FACING, dirs[d]);
                ps[de.officerooms.megastructure.Poolrooms.Part.PIPE_END_E.ordinal() + d] = de.officerooms.megastructure.PoolBlocks.PIPE_END.get()
                        .defaultBlockState().setValue(de.officerooms.block.DecorBlock.FACING, dirs[d]);
                ps[de.officerooms.megastructure.Poolrooms.Part.LADDER_E.ordinal() + d] = de.officerooms.megastructure.PoolBlocks.LADDER.get()
                        .defaultBlockState().setValue(de.officerooms.block.DecorBlock.FACING, dirs[d]);
            }
            ps[de.officerooms.megastructure.Poolrooms.Part.TUBE.ordinal()] = de.officerooms.megastructure.PoolBlocks.TUBE.get().defaultBlockState();
            ps[de.officerooms.megastructure.Poolrooms.Part.RAIL_X.ordinal()] = de.officerooms.megastructure.PoolBlocks.RAIL.get().defaultBlockState()
                    .setValue(de.officerooms.block.DecorBlock.FACING, Direction.NORTH);
            ps[de.officerooms.megastructure.Poolrooms.Part.RAIL_Z.ordinal()] = de.officerooms.megastructure.PoolBlocks.RAIL.get().defaultBlockState()
                    .setValue(de.officerooms.block.DecorBlock.FACING, Direction.EAST);
            poolStates = ps;
        }
        return ps;
    }

    /** Überschreibt im Band einer Poolrooms-Fundstelle die Büroarchitektur dieses Chunks. */
    private void poolrooms(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.Poolrooms.Site site = p.pools.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = poolStates();
        de.officerooms.megastructure.Poolrooms.Part[] parts = de.officerooms.megastructure.Poolrooms.Part.values();
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                if (lx == 0 || lz == 0) {
                    continue;
                }
                int side = site.ringSide(lx, lz);
                for (int off = 0; off < de.officerooms.megastructure.Poolrooms.H; off++) {
                    int y = site.yBase + off;
                    boolean door = false;
                    if (side >= 0) {
                        int wx = switch (side) {
                            case 0 -> baseX;
                            case 2 -> baseX + p.regionBlocks;
                            default -> bx + dx;
                        };
                        int wz = switch (side) {
                            case 1 -> baseZ;
                            case 3 -> baseZ + p.regionBlocks;
                            default -> bz + dz;
                        };
                        door = BlockKind.isAirLike(sampler.kind(wx, y, wz));
                    }
                    de.officerooms.megastructure.Poolrooms.Part part = site.part(lx, off, lz, door);
                    if (part == de.officerooms.megastructure.Poolrooms.Part.OFFICE) {
                        continue;
                    }
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, ps[part.ordinal()], false);
                }
            }
        }
    }

    // --- 0.8.0: Neighbourhood and The Maze ----------------------------------------------------

    private static BlockState[] hoodStates;

    private static BlockState[] hoodStates() {
        BlockState[] ps = hoodStates;
        if (ps == null) {
            ps = new BlockState[de.officerooms.megastructure.Neighbourhood.Part.values().length];
            for (de.officerooms.megastructure.Neighbourhood.Part part : de.officerooms.megastructure.Neighbourhood.Part.values()) {
                ps[part.ordinal()] = switch (part) {
                    case AIR, OFFICE -> Blocks.AIR.defaultBlockState();
                    case HALL_WALL -> Blocks.GRAY_CONCRETE.defaultBlockState();
                    case CEILING -> Blocks.BLACK_CONCRETE.defaultBlockState();
                    case STREET -> Blocks.BLACKSTONE.defaultBlockState();
                    case LINE -> Blocks.WHITE_CONCRETE.defaultBlockState();
                    case SIDEWALK -> Blocks.SMOOTH_STONE.defaultBlockState();
                    case LAWN -> Blocks.GRASS_BLOCK.defaultBlockState();
                    case PATH -> Blocks.GRAVEL.defaultBlockState();
                    case FENCE -> Blocks.SPRUCE_FENCE.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.FenceBlock.NORTH, true)
                            .setValue(net.minecraft.world.level.block.FenceBlock.SOUTH, true);
                    case H_FLOOR -> Blocks.OAK_PLANKS.defaultBlockState();
                    case H_WALL -> Blocks.WHITE_TERRACOTTA.defaultBlockState();
                    case H_CORNER -> Blocks.DARK_OAK_LOG.defaultBlockState();
                    case H_ATTIC -> Blocks.SPRUCE_PLANKS.defaultBlockState();
                    case GLASS -> Blocks.GLASS.defaultBlockState();
                    case ROOF -> Blocks.DEEPSLATE_TILES.defaultBlockState();
                    case ROOF_S -> Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
                    case ROOF_N -> Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
                    case CHIMNEY -> Blocks.BRICKS.defaultBlockState();
                    case DOOR_LO -> Blocks.SPRUCE_DOOR.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH);
                    case DOOR_HI -> Blocks.SPRUCE_DOOR.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH)
                            .setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                    case SHELF -> Blocks.BOOKSHELF.defaultBlockState();
                    case SOFA -> Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
                    case TABLE_LEG -> Blocks.DARK_OAK_FENCE.defaultBlockState();
                    case TABLE_TOP -> Blocks.DARK_OAK_PRESSURE_PLATE.defaultBlockState();
                    case TV -> Blocks.BLACK_CONCRETE.defaultBlockState();
                    case TV_STAND -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
                    case CARPET -> Blocks.RED_CARPET.defaultBlockState();
                    case BED -> Blocks.WHITE_WOOL.defaultBlockState();
                    case LAMP -> Blocks.POTTED_DEAD_BUSH.defaultBlockState();
                    case BOARD -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
                    case LANTERN -> Blocks.LANTERN.defaultBlockState();
                    case STAIR_E -> Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
                    case RAIL -> Blocks.OAK_FENCE.defaultBlockState();
                    case POST -> Blocks.DARK_OAK_FENCE.defaultBlockState();
                    case STREET_LAMP -> Blocks.REDSTONE_LAMP.defaultBlockState();
                    case STREET_LAMP_LIT -> Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true);
                    case MAILBOX -> Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, Direction.SOUTH);
                    case SAND -> Blocks.SAND.defaultBlockState();
                    case CHAIN -> Blocks.CHAIN.defaultBlockState();
                    case SEAT -> Blocks.SPRUCE_SLAB.defaultBlockState();
                    case TRUNK -> Blocks.OAK_LOG.defaultBlockState();
                    case LEAVES -> Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
                    case DOOR_OPEN_LO -> Blocks.SPRUCE_DOOR.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH)
                            .setValue(net.minecraft.world.level.block.DoorBlock.OPEN, true);
                    case DOOR_OPEN_HI -> Blocks.SPRUCE_DOOR.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH)
                            .setValue(net.minecraft.world.level.block.DoorBlock.OPEN, true)
                            .setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                    case WARDROBE -> Blocks.SPRUCE_PLANKS.defaultBlockState();
                };
            }
            hoodStates = ps;
        }
        return ps;
    }

    /** Doorway in the office wall line next to a ring column (same rule as the Poolrooms). */
    private static boolean wallDoor(BlockSampler sampler, OfficePlanner p, int side, int baseX, int baseZ, int x, int y, int z) {
        int wx = switch (side) {
            case 0 -> baseX;
            case 2 -> baseX + p.regionBlocks;
            default -> x;
        };
        int wz = switch (side) {
            case 1 -> baseZ;
            case 3 -> baseZ + p.regionBlocks;
            default -> z;
        };
        return BlockKind.isAirLike(sampler.kind(wx, y, wz));
    }

    private void neighbourhood(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.Neighbourhood.Site site = p.hood.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = hoodStates();
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                if (lx == 0 || lz == 0) {
                    continue;
                }
                int side = site.ringSide(lx, lz);
                for (int off = 0; off < site.h; off++) {
                    int y = site.yBase + off;
                    boolean door = side >= 0 && off >= 1 && off <= 3 && wallDoor(sampler, p, side, baseX, baseZ, bx + dx, y, bz + dz);
                    de.officerooms.megastructure.Neighbourhood.Part part = site.part(lx, off, lz, door);
                    if (part == de.officerooms.megastructure.Neighbourhood.Part.OFFICE) {
                        continue;
                    }
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, ps[part.ordinal()], false);
                }
            }
        }
    }

    private static BlockState[] mazeStates;

    private static BlockState outlet(Direction d) {
        return de.officerooms.level0.Level0Blocks.OUTLET.get().defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, d);
    }

    private static BlockState[] mazeStates() {
        BlockState[] ps = mazeStates;
        if (ps == null) {
            ps = new BlockState[de.officerooms.megastructure.Maze.Part.values().length];
            BlockState strip = de.officerooms.block.ModBlocks.CEILING_STRIP.get().defaultBlockState()
                    .setValue(de.officerooms.block.CeilingLightBlock.AXIS, Direction.Axis.X);
            for (de.officerooms.megastructure.Maze.Part part : de.officerooms.megastructure.Maze.Part.values()) {
                ps[part.ordinal()] = switch (part) {
                    case AIR, OFFICE -> Blocks.AIR.defaultBlockState();
                    case WALLPAPER -> de.officerooms.block.ModBlocks.MAZE_WALLPAPER.get().defaultBlockState();
                    case CARPET -> de.officerooms.block.ModBlocks.MAZE_CARPET.get().defaultBlockState();
                    case CEILING -> de.officerooms.block.ModBlocks.MAZE_CEILING.get().defaultBlockState();
                    // 0.33.0: the Maze is built from Level 0's own blocks, lights set into the drop ceiling
                    case LIGHT_ON, LIGHT_SIDE, WALL_LIGHT -> de.officerooms.level0.Level0Blocks.LIGHT.get().defaultBlockState();
                    case LIGHT_OFF -> de.officerooms.level0.Level0Blocks.LIGHT_OFF.get().defaultBlockState();
                    case FLICKER -> de.officerooms.level0.Level0Blocks.FLICKER.get().defaultBlockState();
                    case SECURITY_WALL, VAULT_FLOOR -> de.officerooms.block.ModBlocks.SECURITY_WALL.get().defaultBlockState();
                    case FILL -> de.officerooms.level0.Level0Blocks.WALLPAPER.get().defaultBlockState();
                    case WALL_WORN -> de.officerooms.level0.Level0Blocks.WALLPAPER_WORN.get().defaultBlockState();
                    case WALL_ARROW -> de.officerooms.level0.Level0Blocks.WALLPAPER_ARROW.get().defaultBlockState();
                    case WINDOW -> Blocks.GLASS.defaultBlockState();
                    case CARPET_STAINED -> de.officerooms.level0.Level0Blocks.CARPET_STAINED.get().defaultBlockState();
                    case CARPET_WET -> de.officerooms.level0.Level0Blocks.CARPET_WET.get().defaultBlockState();
                    case CEIL_STAINED -> de.officerooms.level0.Level0Blocks.CEILING_STAINED.get().defaultBlockState();
                    case CEIL_VOID -> de.officerooms.level0.Level0Blocks.CEILING_VOID.get().defaultBlockState();
                    case VENT -> de.officerooms.level0.Level0Blocks.VENT.get().defaultBlockState();
                    case HOLE -> Blocks.AIR.defaultBlockState();
                    case RAMP_E -> de.officerooms.level0.Level0Blocks.RAMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST);
                    case RAMP_S -> de.officerooms.level0.Level0Blocks.RAMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH);
                    case STEP_S -> Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.SOUTH);
                    case STEP_E -> Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST);
                    case DOOR_LOWER -> Blocks.OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.EAST);
                    case DOOR_UPPER -> Blocks.OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.EAST)
                            .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                    case FRAME -> Blocks.STRIPPED_OAK_LOG.defaultBlockState();
                    case OUTLET_E -> outlet(Direction.EAST);
                    case OUTLET_W -> outlet(Direction.WEST);
                    case OUTLET_S -> outlet(Direction.SOUTH);
                    case OUTLET_N -> outlet(Direction.NORTH);
                };
            }
            mazeStates = ps;
        }
        return ps;
    }

    private void maze(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.Maze.Site site = p.maze.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = mazeStates();
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                for (int off = 0; off < de.officerooms.megastructure.Maze.H; off++) {
                    de.officerooms.megastructure.Maze.Part part = site.part(lx, off, lz);
                    if (part == de.officerooms.megastructure.Maze.Part.OFFICE) {
                        continue;
                    }
                    int y = site.yBase + off;
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, ps[part.ordinal()], false);
                    if (part == de.officerooms.megastructure.Maze.Part.HOLE) {             // 0.33.0: through the slab, into the offices below
                        sections[chunk.getSectionIndex(y - 1)].setBlockState(dx, (y - 1) & 15, dz, Blocks.AIR.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    // --- 0.14.0: The Abandoned Hotel ---------------------------------------------------------------

    private static BlockState[] hotelStates;

    private static BlockState[] hotelStates() {
        BlockState[] ps = hotelStates;
        if (ps == null) {
            ps = new BlockState[de.officerooms.megastructure.Hotel.Part.values().length];
            for (de.officerooms.megastructure.Hotel.Part part : de.officerooms.megastructure.Hotel.Part.values()) {
                ps[part.ordinal()] = de.officerooms.megastructure.HotelBlocks.state(part);
            }
            hotelStates = ps;
        }
        return ps;
    }

    private void hotel(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.Hotel.Site site = p.hotel.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = hotelStates();
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                if (lx == 0 || lz == 0) {
                    continue;
                }
                int side = site.ringSide(lx, lz);
                for (int off = 0; off < site.h; off++) {
                    int y = site.yBase + off;
                    boolean door = side >= 0 && off >= 7 && off <= 9 && wallDoor(sampler, p, side, baseX, baseZ, bx + dx, y, bz + dz);
                    de.officerooms.megastructure.Hotel.Part part = site.part(lx, off, lz, door);
                    if (part == de.officerooms.megastructure.Hotel.Part.OFFICE) {
                        continue;
                    }
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, ps[part.ordinal()], false);
                }
            }
        }
    }

    // --- 0.15.0: The Industrial Complex -------------------------------------------------------------

    private static BlockState[] industrialStates;

    private void industrial(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.Industrial.Site site = p.industrial.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = industrialStates;
        if (ps == null) {
            ps = new BlockState[de.officerooms.megastructure.Industrial.Part.values().length];
            for (de.officerooms.megastructure.Industrial.Part part : de.officerooms.megastructure.Industrial.Part.values()) {
                ps[part.ordinal()] = de.officerooms.megastructure.IndustrialBlocks.state(part);
            }
            industrialStates = ps;
        }
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                if (lx == 0 || lz == 0) {
                    continue;
                }
                int side = site.ringSide(lx, lz);
                for (int off = 0; off < site.h; off++) {
                    int y = site.yBase + off;
                    int r = off % 6;
                    boolean door = side >= 0 && r >= 1 && r <= 3 && wallDoor(sampler, p, side, baseX, baseZ, bx + dx, y, bz + dz);
                    de.officerooms.megastructure.Industrial.Part part = site.part(lx, off, lz, door);
                    if (part == de.officerooms.megastructure.Industrial.Part.OFFICE) {
                        continue;
                    }
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, ps[part.ordinal()], false);
                }
            }
        }
    }

    // --- 0.16.0: Level Fun ------------------------------------------------------------------------------

    private static BlockState[] funStates;

    private void levelFun(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.LevelFun.Site site = p.fun.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = funStates;
        if (ps == null) {
            ps = new BlockState[de.officerooms.megastructure.LevelFun.Part.values().length];
            for (de.officerooms.megastructure.LevelFun.Part part : de.officerooms.megastructure.LevelFun.Part.values()) {
                ps[part.ordinal()] = de.officerooms.megastructure.LevelFunBlocks.state(part);
            }
            funStates = ps;
        }
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                if (lx == 0 || lz == 0) {
                    continue;
                }
                int side = site.ringSide(lx, lz);
                for (int off = 0; off < site.h; off++) {
                    int y = site.yBase + off;
                    boolean door = side >= 0 && off % 6 >= 1 && off % 6 <= 3 && wallDoor(sampler, p, side, baseX, baseZ, bx + dx, y, bz + dz);
                    de.officerooms.megastructure.LevelFun.Part part = site.part(lx, off, lz, door);
                    if (part == de.officerooms.megastructure.LevelFun.Part.OFFICE) {
                        continue;
                    }
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, ps[part.ordinal()], false);
                }
            }
        }
    }

    // --- 0.31.0: the abandoned cinema ----------------------------------------------------------------------------

    private static BlockState[] cinemaStates;

    private void cinema(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.Cinema.Site site = p.cinema.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = cinemaStates;
        if (ps == null) {
            ps = new BlockState[de.officerooms.megastructure.Cinema.Part.values().length];
            for (de.officerooms.megastructure.Cinema.Part part : de.officerooms.megastructure.Cinema.Part.values()) {
                ps[part.ordinal()] = de.officerooms.megastructure.CinemaBlocks.state(part);
            }
            cinemaStates = ps;
        }
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                if (lx == 0 || lz == 0) {
                    continue;
                }
                int side = site.ringSide(lx, lz);
                for (int off = 0; off < de.officerooms.megastructure.Cinema.H; off++) {
                    int y = site.yBase + off;
                    boolean door = side >= 0 && off % de.officerooms.megastructure.Cinema.FH >= 1 && off % de.officerooms.megastructure.Cinema.FH <= 3 && wallDoor(sampler, p, side, baseX, baseZ, bx + dx, y, bz + dz);
                    de.officerooms.megastructure.Cinema.Part part = site.part(lx, off, lz, door);
                    if (part == de.officerooms.megastructure.Cinema.Part.OFFICE) {
                        continue;
                    }
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, ps[part.ordinal()], false);
                }
            }
        }
    }

    // --- 0.43.0: the Dead Mall ---------------------------------------------------------------------------------------

    private static BlockState[] mallStates;
    private static BlockState[] mallRails;

    private void mall(BlockSampler sampler, ChunkAccess chunk, LevelChunkSection[] sections, int bx, int bz) {
        OfficePlanner p = sampler.planner();
        int rx = p.regionOf(bx);
        int rz = p.regionOf(bz);
        de.officerooms.megastructure.Mall.Site site = p.mall.site(rx, rz);
        if (site == null) {
            return;
        }
        BlockState[] ps = mallStates;
        if (ps == null) {
            de.officerooms.megastructure.Mall.Part[] parts = de.officerooms.megastructure.Mall.Part.values();
            ps = new BlockState[parts.length];
            for (de.officerooms.megastructure.Mall.Part part : parts) {
                ps[part.ordinal()] = de.officerooms.megastructure.MallBlocks.state(part);
            }
            BlockState[] rails = new BlockState[16];
            for (int m = 0; m < 16; m++) {
                rails[m] = de.officerooms.megastructure.MallBlocks.rail(m);
            }
            mallRails = rails;
            mallStates = ps;
        }
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        int fh = de.officerooms.megastructure.Mall.FH;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int lx = bx + dx - baseX;
                int lz = bz + dz - baseZ;
                if (lx == 0 || lz == 0) {
                    continue;
                }
                int side = site.ringSide(lx, lz);
                for (int off = 0; off < de.officerooms.megastructure.Mall.H; off++) {
                    int y = site.yBase + off;
                    boolean door = side >= 0 && off % fh >= 1 && off % fh <= 3 && wallDoor(sampler, p, side, baseX, baseZ, bx + dx, y, bz + dz);
                    de.officerooms.megastructure.Mall.Part part = site.part(lx, off, lz, door);
                    if (part == de.officerooms.megastructure.Mall.Part.OFFICE) {
                        continue;
                    }
                    BlockState st = ps[part.ordinal()];
                    if (part == de.officerooms.megastructure.Mall.Part.RAIL) {
                        st = mallRails[mallRail(lx, lz)];
                    }
                    sections[chunk.getSectionIndex(y)].setBlockState(dx, y & 15, dz, st, false);
                }
            }
        }
    }

    private static int mallRail(int lx, int lz) {
        int m = 0;
        if (mallJoins(lx, lz - 1)) {
            m |= 1;
        }
        if (mallJoins(lx, lz + 1)) {
            m |= 2;
        }
        if (mallJoins(lx + 1, lz)) {
            m |= 4;
        }
        if (mallJoins(lx - 1, lz)) {
            m |= 8;
        }
        return m;
    }

    private static boolean mallJoins(int lx, int lz) {
        return de.officerooms.megastructure.Mall.railCell(lx, lz) || de.officerooms.megastructure.Mall.pillar(lx, lz);
    }

    public FurnishingPlanner furnishing(BlockSampler sampler) {
        return furnishers.computeIfAbsent(sampler.planner(), FurnishingPlanner::new);
    }

    public de.officerooms.special.SpecialDecorator specials(BlockSampler sampler) {
        return decorators.computeIfAbsent(sampler.planner(), pl -> new de.officerooms.special.SpecialDecorator(pl, sampler, furnishing(sampler)));
    }

    /**
     * Beleuchtung (Phase 2). Läuft in der FEATURES-Stufe, also nach der vollständigen Architektur und
     * vor der Lichtberechnung. Gesetzt werden nur Lichtquellen (mit OfficeCraft unsichtbare
     * Vanilla-Lichtblöcke, sonst Seelaternen); Möbel und Leuchtenmodelle setzt der
     * {@link FurnishingTicker} erst in Spielernähe.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        BlockSampler sampler = sampler(level.getLevel().getChunkSource().randomState(), chunk);
        FurnishingPlanner fp = furnishing(sampler);
        long t0 = System.nanoTime();
        try {
            for (Placement pl : fp.lightsForChunk(chunk.getPos().x, chunk.getPos().z)) {
                if (pl.item().isLight()) {
                    FurnitureBuilder.placeWorldgenLight(level, pl);
                }
            }
        } catch (RuntimeException e) {
            OfficeRooms.LOGGER.error("Furnishing chunk {} failed", chunk.getPos(), e);
        }
        long t1 = System.nanoTime();
        try {
            de.officerooms.megastructure.PoolroomsDecorator.decorate(sampler.planner(), level, chunk);
            de.officerooms.megastructure.MegaDecorator.decorate(sampler.planner(), level, chunk);
        } catch (RuntimeException e) {
            OfficeRooms.LOGGER.error("Poolrooms in Chunk {} fehlgeschlagen", chunk.getPos(), e);
        }
        try {
            specials(sampler).decorate(level, chunk);
        } catch (RuntimeException e) {
            OfficeRooms.LOGGER.error("Special rooms in chunk {} failed", chunk.getPos(), e);
        }
        long t2 = System.nanoTime();
        PROF_LIGHT.addAndGet(t1 - t0);
        PROF_SPECIAL.addAndGet(t2 - t1);
        if (PROF_N.incrementAndGet() % 100 == 0) {
            OfficeRooms.LOGGER.debug("[Profile] 100 chunks: light {} ms, special rooms {} ms, fill {} ms",
                    PROF_LIGHT.getAndSet(0) / 1_000_000, PROF_SPECIAL.getAndSet(0) / 1_000_000, PROF_FILL.getAndSet(0) / 1_000_000);
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // keine Höhlen
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
        // keine Oberfläche
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // keine Mobs
    }

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState,
                                 StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager templateManager) {
        // No vanilla or foreign structures. 0.41.0: only our own invisible megastructure markers (for /locate).
        net.minecraft.world.level.ChunkPos cp = chunk.getPos();
        net.minecraft.core.SectionPos sp = net.minecraft.core.SectionPos.bottomOf(chunk);
        for (var set : structureState.possibleStructureSets()) {
            if (!set.unwrapKey().map(k -> k.location().getNamespace().equals(OfficeRoomsIds.MOD_ID)).orElse(false)
                    || !set.value().placement().isStructureChunk(structureState, cp.x, cp.z)) {
                continue;
            }
            for (var entry : set.value().structures()) {
                var structure = entry.structure().value();
                var start = structure.generate(registryAccess, this, getBiomeSource(), structureState.randomState(), templateManager,
                        structureState.getLevelSeed(), cp, 0, chunk, structure.biomes()::contains);
                if (start.isValid()) {
                    structureManager.setStartForStructure(sp, structure, start, chunk);
                }
            }
        }
    }

    @Override
    public int getGenDepth() {
        return de.officerooms.dimension.OfficeRoomsDimension.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return -64;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        BlockSampler s = sampler(randomState, level);
        byte[] col = new byte[level.getHeight()];
        s.column(x, z, col);
        BlockState[] st = states();
        for (int i = col.length - 1; i >= 0; i--) {
            if (type.isOpaque().test(st[col[i]])) {
                return level.getMinBuildHeight() + i + 1;
            }
        }
        return level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockSampler s = sampler(randomState, level);
        byte[] col = new byte[level.getHeight()];
        s.column(x, z, col);
        BlockState[] st = states();
        BlockState[] out = new BlockState[col.length];
        for (int i = 0; i < col.length; i++) {
            out[i] = st[col[i]];
        }
        return new NoiseColumn(level.getMinBuildHeight(), out);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        BlockSampler s = last;
        if (s == null) {
            return;
        }
        OfficePlanner p = s.planner();
        int rx = p.regionOf(pos.getX());
        int rz = p.regionOf(pos.getZ());
        int st = p.storeyOf(pos.getY());
        info.add("Office Rooms: region " + rx + "," + rz + " storey " + st);
        if (st >= 0 && st < p.storeys) {
            RegionLayout l = p.layout(rx, rz);
            int ux = (pos.getX() - rx * p.regionBlocks) / p.unit;
            int uz = (pos.getZ() - rz * p.regionBlocks) / p.unit;
            Room r = l.rawRoom(st, ux, uz);
            info.add("Room: " + (r == null ? "solid" : r.toString()));
        }
    }
}
