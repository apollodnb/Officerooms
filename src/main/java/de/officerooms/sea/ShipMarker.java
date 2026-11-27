package de.officerooms.sea;

import com.mojang.serialization.Codec;
import de.officerooms.worldgen.mega.MarkerPiece;
import de.officerooms.worldgen.shrine.ShrineStructures;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * 0.42.0: invisible structure marker at the embarkation lobby of every SS Fun =), so {@code /locate structure
 * office_rooms:ss_fun} and structure-finding mods find the ships. It places no blocks.
 */
public final class ShipMarker extends Structure {

    public static final Codec<ShipMarker> CODEC = simpleCodec(ShipMarker::new);

    public ShipMarker(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (!(ctx.chunkGenerator() instanceof SeaChunkGenerator)) {
            return Optional.empty();
        }
        long seed = SeaChunkGenerator.seedOf(ctx.randomState());
        ChunkPos cp = ctx.chunkPos();
        Ship s = Ship.of(seed, Math.floorDiv(cp.getMinBlockX(), Ship.CELL), Math.floorDiv(cp.getMinBlockZ(), Ship.CELL));
        if (s == null) {
            return Optional.empty();
        }
        int[] e = s.embark();
        if ((e[0] >> 4) != cp.x || (e[2] >> 4) != cp.z) {
            return Optional.empty();
        }
        BlockPos at = new BlockPos(e[0], e[1], e[2]);
        return Optional.of(new GenerationStub(at, b -> b.addPiece(new MarkerPiece(at))));
    }

    @Override
    public StructureType<?> type() {
        return ShrineStructures.SHIP.get();
    }

    /** One candidate chunk per ship cell: the chunk of its embarkation lobby (or the cell centre if the cell is empty). */
    public static final class Placement extends RandomSpreadStructurePlacement {
        public static final Codec<Placement> CODEC = Codec.unit(Placement::new);
        static final int SPACING = Ship.CELL / 16;

        public Placement() {
            super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty(), SPACING, SPACING - 1, RandomSpreadType.LINEAR);
        }

        @Override
        public ChunkPos getPotentialStructureChunk(long levelSeed, int cx, int cz) {
            int gx = Math.floorDiv(cx, SPACING), gz = Math.floorDiv(cz, SPACING);
            Ship s = SeaChunkGenerator.seed == 0 ? null : Ship.of(SeaChunkGenerator.seed, gx, gz);
            if (s != null) {
                int[] e = s.embark();
                return new ChunkPos(e[0] >> 4, e[2] >> 4);
            }
            return new ChunkPos(gx * SPACING + SPACING / 2, gz * SPACING + SPACING / 2);
        }

        @Override
        public StructurePlacementType<?> type() {
            return ShrineStructures.SHIP_PLACEMENT.get();
        }
    }
}
