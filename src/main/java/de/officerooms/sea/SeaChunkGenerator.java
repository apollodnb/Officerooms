package de.officerooms.sea;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.officerooms.core.OfficeRoomsIds;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * 0.42.0: generator of the sea dimension of the SS Fun =): water as far as the fog lets you see, a muddy bottom 60
 * blocks down, and every few hundred blocks one of the ships ({@link Ship}).
 */
public final class SeaChunkGenerator extends ChunkGenerator {

    public static final Codec<SeaChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(i, SeaChunkGenerator::new));

    public static final int HEIGHT = 128;

    public SeaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** The world seed: kept from the first chunk the server generates (the generator itself has no seed). */
    public static volatile long seed;

    static List<Ship> shipsIn(long seed, int x0, int z0, int x1, int z1) {
        List<Ship> out = new ArrayList<>(2);
        for (int gx = Math.floorDiv(x0 - Ship.L, Ship.CELL); gx <= Math.floorDiv(x1 + Ship.L, Ship.CELL); gx++) {
            for (int gz = Math.floorDiv(z0 - Ship.W, Ship.CELL); gz <= Math.floorDiv(z1 + Ship.W, Ship.CELL); gz++) {
                Ship s = Ship.of(seed, gx, gz);
                if (s != null && s.ox <= x1 && s.ox + Ship.L > x0 && s.oz <= z1 && s.oz + Ship.W > z0) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    static BlockState seabed(int x, int y, int z) {
        if (y == 0) {
            return Blocks.BEDROCK.defaultBlockState();
        }
        long h = de.officerooms.core.Hashing.hash(0x5EA, x, z);
        if (y == 3) {
            return (h & 7) == 0 ? Blocks.GRAVEL.defaultBlockState() : (h & 7) == 1 ? Blocks.CLAY.defaultBlockState() : Blocks.MUD.defaultBlockState();
        }
        return Blocks.MUD.defaultBlockState();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        long sd = seedOf(randomState);
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        List<Ship> ships = shipsIn(sd, x0, z0, x0 + 15, z0 + 15);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                Ship s = null;
                for (Ship q : ships) {
                    if (q.inBox(x, z)) {
                        s = q;
                    }
                }
                int lx = s == null ? 0 : s.lx(x), lz = s == null ? 0 : s.lz(z);
                for (int y = 0; y < HEIGHT; y++) {
                    BlockState st = s == null ? null : ShipShape.at(s, lx, y, lz);
                    if (st == null) {
                        st = y <= 3 ? seabed(x, y, z) : y < Ship.SEA ? water : null;
                    }
                    if (st != null && !st.isAir()) {
                        chunk.setBlockState(pos.set(x, y, z), st, false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    private static final net.minecraft.resources.ResourceLocation SEED_KEY = new net.minecraft.resources.ResourceLocation(OfficeRoomsIds.MOD_ID, "ss_fun");

    /** The ship layout seed of a world (same value in chunk generation, decoration, structure markers and events). */
    public static long seedOf(RandomState rs) {
        long s = rs.getOrCreateRandomFactory(SEED_KEY).at(0, 0, 0).nextLong();
        seed = s;
        return s;
    }

    public static long seedOf(net.minecraft.server.level.ServerLevel level) {
        return seedOf(level.getChunkSource().randomState());
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        long sd = seedOf(level.getLevel().getChunkSource().randomState());
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        for (Ship s : shipsIn(sd, x0, z0, x0 + 15, z0 + 15)) {
            ShipDecorator.decorate(level, chunk, s);
        }
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager, RandomState randomState, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
    }

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState,
                                 StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager templateManager) {
        // only our own invisible ship markers (for /locate and structure-finding mods)
        ChunkPos cp = chunk.getPos();
        SectionPos sp = SectionPos.bottomOf(chunk);
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
        return HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return Ship.SEA;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return type == Heightmap.Types.OCEAN_FLOOR_WG || type == Heightmap.Types.OCEAN_FLOOR ? 4 : Ship.SEA;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] col = new BlockState[HEIGHT];
        for (int y = 0; y < HEIGHT; y++) {
            col[y] = y <= 3 ? seabed(x, y, z) : y < Ship.SEA ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(0, col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        Ship s = Ship.at(seedOf(randomState), pos.getX(), pos.getZ());
        if (s != null) {
            int d = (pos.getY() - Ship.fy(0)) / Ship.DH;
            info.add("SS Fun =): cell " + s.gx + " " + s.gz + ", deck " + d + ", lx " + s.lx(pos.getX()) + " lz " + s.lz(pos.getZ()));
        }
    }
}
