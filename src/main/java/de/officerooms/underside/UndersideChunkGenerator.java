package de.officerooms.underside;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
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

/** 0.19.0: generator of the Underside dimension (see {@link UndersideTerrain}). */
public final class UndersideChunkGenerator extends ChunkGenerator {

    public static final Codec<UndersideChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)
    ).apply(i, UndersideChunkGenerator::new));

    public UndersideChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        BlockState[] col = new BlockState[UndersideTerrain.HEIGHT];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                UndersideTerrain.column(x0 + dx, z0 + dz, col);
                for (int y = 0; y < col.length; y++) {
                    if (!col[y].isAir()) {
                        chunk.setBlockState(pos.set(x0 + dx, y, z0 + dz), col[y], false);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
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
    }

    @Override
    public int getGenDepth() {
        return UndersideTerrain.HEIGHT;
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
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        return UndersideTerrain.floor(x, z) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        BlockState[] col = new BlockState[UndersideTerrain.HEIGHT];
        UndersideTerrain.column(x, z, col);
        return new NoiseColumn(0, col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Underside: floor " + UndersideTerrain.floor(pos.getX(), pos.getZ()) + ", ceiling " + UndersideTerrain.ceil(pos.getX(), pos.getZ()));
    }
}
