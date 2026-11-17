package de.officerooms.worldgen.mega;

import de.officerooms.worldgen.shrine.ShrineStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.ChunkPos;

/** 0.41.0: 1x1x1 piece that places nothing; it only gives the marker structure a position. */
public final class MarkerPiece extends StructurePiece {

    public MarkerPiece(BlockPos at) {
        super(ShrineStructures.MARKER_PIECE.get(), 0, new BoundingBox(at));
    }

    public MarkerPiece(CompoundTag tag) {
        super(ShrineStructures.MARKER_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator gen, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
    }
}
