package de.officerooms.worldgen.shrine;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Sehr seltene, halb überwucherte Büroruine in der Oberwelt: ein Bruchstück der Office Rooms, das
 * "durchgefallen" ist. Enthält die Zutaten für das Portal (Sculk-Sensor und Sand) und Warnungen.
 *
 * <p>Wird nur auf trockenem, ebenem Boden erzeugt (Höhenunterschied der Ecken höchstens 3, keine
 * Flüssigkeit an der Oberfläche). Abstand zu Dörfern regelt das Structure-Set.</p>
 */
public final class ShrineStructure extends Structure {

    public static final Codec<ShrineStructure> CODEC = simpleCodec(ShrineStructure::new);
    static final int SIZE = 11;

    public ShrineStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        int x0 = ctx.chunkPos().getMinBlockX() + 2;
        int z0 = ctx.chunkPos().getMinBlockZ() + 2;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int[][] corners = {{0, 0}, {SIZE - 1, 0}, {0, SIZE - 1}, {SIZE - 1, SIZE - 1}, {SIZE / 2, SIZE / 2}};
        for (int[] c : corners) {
            int x = x0 + c[0];
            int z = z0 + c[1];
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            NoiseColumn col = ctx.chunkGenerator().getBaseColumn(x, z, ctx.heightAccessor(), ctx.randomState());
            if (!col.getBlock(h - 1).getFluidState().isEmpty() || !col.getBlock(h).getFluidState().isEmpty()) {
                return Optional.empty();
            }
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        if (max - min > 4 || min <= ctx.chunkGenerator().getSeaLevel()) {
            return Optional.empty();
        }
        // Boden des Fundstücks liegt auf der tiefsten Ecke (Oberseite des obersten festen Blocks)
        int floorY = min - 1;
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(ctx.random());
        BlockPos origin = new BlockPos(x0, floorY, z0);
        return Optional.of(new GenerationStub(origin, b -> b.addPiece(new ShrinePiece(origin, facing, ctx.random().nextLong()))));
    }

    @Override
    public StructureType<?> type() {
        return ShrineStructures.SHRINE.get();
    }
}
