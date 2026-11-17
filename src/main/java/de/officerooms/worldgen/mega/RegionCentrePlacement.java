package de.officerooms.worldgen.mega;

import com.mojang.serialization.Codec;
import de.officerooms.worldgen.shrine.ShrineStructures;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * 0.41.0: exactly one candidate chunk per 160-block region (10 x 10 chunks): the region's centre chunk. It stays a
 * random_spread placement (spacing 10) so /locate and structure-finding mods such as Explorer's Compass search it.
 */
public final class RegionCentrePlacement extends RandomSpreadStructurePlacement {

    public static final Codec<RegionCentrePlacement> CODEC = Codec.unit(RegionCentrePlacement::new);
    static final int SPACING = 10;

    public RegionCentrePlacement() {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty(), SPACING, SPACING - 1, RandomSpreadType.LINEAR);
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int cx, int cz) {
        return new ChunkPos(Math.floorDiv(cx, SPACING) * SPACING + SPACING / 2, Math.floorDiv(cz, SPACING) * SPACING + SPACING / 2);
    }

    @Override
    public StructurePlacementType<?> type() {
        return ShrineStructures.REGION_CENTRE.get();
    }
}
