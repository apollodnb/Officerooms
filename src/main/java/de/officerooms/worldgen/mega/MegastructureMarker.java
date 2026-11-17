package de.officerooms.worldgen.mega;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.officerooms.megastructure.Megastructures;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.shrine.ShrineStructures;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * 0.41.0: invisible marker structure for one megastructure kind in the Office Rooms dimension. It places no blocks;
 * it only makes the megastructures known to the vanilla structure system, so {@code /locate structure}, Explorer's
 * Compass and similar mods can find them. Placement (datapack): random_spread with spacing 10 / separation 9, which
 * puts exactly one candidate on the first chunk of every 160-block region; this structure then checks the region's
 * planned kind and puts its marker piece at the megastructure's entrance point.
 */
public final class MegastructureMarker extends Structure {

    public static final Codec<MegastructureMarker> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind.name().toLowerCase(Locale.ROOT))
    ).apply(i, (st, k) -> new MegastructureMarker(st, Megastructures.Kind.valueOf(k.toUpperCase(Locale.ROOT)))));

    private final Megastructures.Kind kind;

    public MegastructureMarker(StructureSettings settings, Megastructures.Kind kind) {
        super(settings);
        this.kind = kind;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (!(ctx.chunkGenerator() instanceof de.officerooms.worldgen.OfficeChunkGenerator g)) {
            return Optional.empty();
        }
        OfficePlanner p = g.sampler(ctx.randomState(), ctx.heightAccessor()).planner();
        int rx = p.regionOf(ctx.chunkPos().getMinBlockX());
        int rz = p.regionOf(ctx.chunkPos().getMinBlockZ());
        int[] a = Megastructures.anchor(p, kind, rx, rz);
        if (a == null) {
            return Optional.empty();
        }
        BlockPos at = new BlockPos(a[0], a[1], a[2]);
        return Optional.of(new GenerationStub(at, b -> b.addPiece(new MarkerPiece(at))));
    }

    @Override
    public StructureType<?> type() {
        return ShrineStructures.MEGASTRUCTURE.get();
    }
}
