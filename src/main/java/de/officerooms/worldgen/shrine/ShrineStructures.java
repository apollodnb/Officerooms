package de.officerooms.worldgen.shrine;

import com.mojang.serialization.Codec;
import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Overworld-Struktur "Abandoned Office Shrine" (0.6.0): Strukturtyp und Bauteiltyp. Platzierung, Biome und
 * Seltenheit stehen im Datapack ({@code worldgen/structure}, {@code worldgen/structure_set}).
 */
public final class ShrineStructures {

    public static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, OfficeRoomsIds.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, OfficeRoomsIds.MOD_ID);

    public static final RegistryObject<StructureType<ShrineStructure>> SHRINE = TYPES.register("abandoned_office_shrine",
            () -> explicit(ShrineStructure.CODEC));
    public static final RegistryObject<StructurePieceType> SHRINE_PIECE = PIECES.register("abandoned_office_shrine",
            () -> (StructurePieceType.ContextlessType) ShrinePiece::new);
    /** 0.41.0: invisible megastructure markers for /locate and structure-finding mods. */
    public static final RegistryObject<StructureType<de.officerooms.worldgen.mega.MegastructureMarker>> MEGASTRUCTURE = TYPES.register("megastructure",
            () -> explicit(de.officerooms.worldgen.mega.MegastructureMarker.CODEC));
    public static final RegistryObject<StructurePieceType> MARKER_PIECE = PIECES.register("megastructure_marker",
            () -> (StructurePieceType.ContextlessType) de.officerooms.worldgen.mega.MarkerPiece::new);

    public static final DeferredRegister<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, OfficeRoomsIds.MOD_ID);
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<de.officerooms.worldgen.mega.RegionCentrePlacement>> REGION_CENTRE =
            PLACEMENTS.register("region_centre", () -> () -> de.officerooms.worldgen.mega.RegionCentrePlacement.CODEC);
    /** 0.42.0: the ships of the sea dimension. */
    public static final RegistryObject<StructureType<de.officerooms.sea.ShipMarker>> SHIP = TYPES.register("ss_fun",
            () -> explicit(de.officerooms.sea.ShipMarker.CODEC));
    public static final RegistryObject<net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType<de.officerooms.sea.ShipMarker.Placement>> SHIP_PLACEMENT =
            PLACEMENTS.register("ship_cell", () -> () -> de.officerooms.sea.ShipMarker.Placement.CODEC);

    private static <S extends net.minecraft.world.level.levelgen.structure.Structure> StructureType<S> explicit(Codec<S> codec) {
        return () -> codec;
    }

    private ShrineStructures() {
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        PIECES.register(modBus);
        PLACEMENTS.register(modBus);
    }
}
