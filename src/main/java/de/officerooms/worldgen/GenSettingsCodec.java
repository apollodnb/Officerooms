package de.officerooms.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.officerooms.room.GenSettings;

/**
 * Codec für {@link GenSettings}. Jedes Feld ist optional; fehlende Felder bekommen die
 * Standardwerte aus {@link GenSettings#DEFAULT}. Die Konfiguration steht im Datapack unter
 * {@code data/office_rooms/dimension/office_rooms.json → generator.settings}.
 */
public final class GenSettingsCodec {

    private static final GenSettings D = GenSettings.DEFAULT;

    private GenSettingsCodec() {
    }

    public static final Codec<GenSettings.Grid> GRID = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(4, 8).optionalFieldOf("unit_size", D.grid().unitSize()).forGetter(GenSettings.Grid::unitSize),
            Codec.intRange(16, 64).optionalFieldOf("region_units", D.grid().regionUnits()).forGetter(GenSettings.Grid::regionUnits),
            Codec.intRange(3, 7).optionalFieldOf("base_height", D.grid().baseHeight()).forGetter(GenSettings.Grid::baseHeight),
            Codec.intRange(0, 2000).optionalFieldOf("storeys", D.grid().storeys()).forGetter(GenSettings.Grid::storeys)
    ).apply(i, GenSettings.Grid::new));

    public static final Codec<GenSettings.HeightBand> HEIGHT_BAND = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(3, 512).fieldOf("min").forGetter(GenSettings.HeightBand::min),
            Codec.intRange(3, 512).fieldOf("max").forGetter(GenSettings.HeightBand::max),
            Codec.intRange(0, 100000).fieldOf("weight").forGetter(GenSettings.HeightBand::weight)
    ).apply(i, GenSettings.HeightBand::new));

    public static final Codec<GenSettings.RoomSize> ROOM_SIZE = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(GenSettings.RoomSize::name),
            Codec.intRange(0, 100000).fieldOf("weight").forGetter(GenSettings.RoomSize::weight),
            Codec.intRange(2, 64).fieldOf("min_w").forGetter(GenSettings.RoomSize::minW),
            Codec.intRange(2, 64).fieldOf("max_w").forGetter(GenSettings.RoomSize::maxW),
            Codec.intRange(2, 64).fieldOf("min_d").forGetter(GenSettings.RoomSize::minD),
            Codec.intRange(2, 64).fieldOf("max_d").forGetter(GenSettings.RoomSize::maxD)
    ).apply(i, GenSettings.RoomSize::new));

    public static final Codec<GenSettings.Corridors> CORRIDORS = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 64).optionalFieldOf("seeds_min", D.corridors().seedsMin()).forGetter(GenSettings.Corridors::seedsMin),
            Codec.intRange(0, 64).optionalFieldOf("seeds_max", D.corridors().seedsMax()).forGetter(GenSettings.Corridors::seedsMax),
            Codec.doubleRange(0, 1).optionalFieldOf("max_share", D.corridors().maxShare()).forGetter(GenSettings.Corridors::maxShare),
            Codec.doubleRange(0, 1).optionalFieldOf("wide_chance", D.corridors().wideChance()).forGetter(GenSettings.Corridors::wideChance),
            Codec.intRange(1, 64).optionalFieldOf("segment_min", D.corridors().segmentMin()).forGetter(GenSettings.Corridors::segmentMin),
            Codec.intRange(1, 64).optionalFieldOf("segment_max", D.corridors().segmentMax()).forGetter(GenSettings.Corridors::segmentMax),
            Codec.doubleRange(0, 1).optionalFieldOf("long_chance", D.corridors().longChance()).forGetter(GenSettings.Corridors::longChance),
            Codec.intRange(1, 64).optionalFieldOf("long_max", D.corridors().longMax()).forGetter(GenSettings.Corridors::longMax),
            Codec.doubleRange(0, 1).optionalFieldOf("turn_chance", D.corridors().turnChance()).forGetter(GenSettings.Corridors::turnChance),
            Codec.doubleRange(0, 1).optionalFieldOf("branch_chance", D.corridors().branchChance()).forGetter(GenSettings.Corridors::branchChance),
            Codec.doubleRange(0, 1).optionalFieldOf("side_branch_chance", D.corridors().sideBranchChance()).forGetter(GenSettings.Corridors::sideBranchChance)
    ).apply(i, GenSettings.Corridors::new));

    public static final Codec<GenSettings.AtriumClass> ATRIUM_CLASS = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(GenSettings.AtriumClass::name),
            Codec.intRange(0, 100000).fieldOf("weight").forGetter(GenSettings.AtriumClass::weight),
            Codec.intRange(2, 62).fieldOf("min_w").forGetter(GenSettings.AtriumClass::minW),
            Codec.intRange(2, 62).fieldOf("max_w").forGetter(GenSettings.AtriumClass::maxW),
            Codec.intRange(2, 62).fieldOf("min_d").forGetter(GenSettings.AtriumClass::minD),
            Codec.intRange(2, 62).fieldOf("max_d").forGetter(GenSettings.AtriumClass::maxD),
            Codec.intRange(4, 4000).fieldOf("min_h").forGetter(GenSettings.AtriumClass::minH),
            Codec.intRange(4, 4000).fieldOf("max_h").forGetter(GenSettings.AtriumClass::maxH)
    ).apply(i, GenSettings.AtriumClass::new));

    public static final Codec<GenSettings.Atria> ATRIA = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 1).optionalFieldOf("density", D.atria().density()).forGetter(GenSettings.Atria::density),
            Codec.doubleRange(0, 1).optionalFieldOf("gallery_chance", D.atria().galleryChance()).forGetter(GenSettings.Atria::galleryChance),
            Codec.doubleRange(0, 1).optionalFieldOf("bridge_chance", D.atria().bridgeChance()).forGetter(GenSettings.Atria::bridgeChance),
            ATRIUM_CLASS.listOf().optionalFieldOf("classes", D.atria().classes()).forGetter(GenSettings.Atria::classes)
    ).apply(i, GenSettings.Atria::new));

    public static final Codec<GenSettings.Doors> DOORS = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 100000).optionalFieldOf("normal", D.doors().normal()).forGetter(GenSettings.Doors::normal),
            Codec.intRange(0, 100000).optionalFieldOf("double", D.doors().dbl()).forGetter(GenSettings.Doors::dbl),
            Codec.intRange(0, 100000).optionalFieldOf("wide", D.doors().wide()).forGetter(GenSettings.Doors::wide),
            Codec.intRange(0, 100000).optionalFieldOf("open", D.doors().open()).forGetter(GenSettings.Doors::open),
            Codec.doubleRange(0, 1).optionalFieldOf("extra_chance", D.doors().extraChance()).forGetter(GenSettings.Doors::extraChance),
            Codec.doubleRange(0, 1).optionalFieldOf("second_opening_chance", D.doors().secondOpeningChance()).forGetter(GenSettings.Doors::secondOpeningChance),
            Codec.doubleRange(0, 1).optionalFieldOf("boundary_chance", D.doors().boundaryChance()).forGetter(GenSettings.Doors::boundaryChance),
            Codec.intRange(0, 64).optionalFieldOf("boundary_min_per_storey", D.doors().boundaryMinPerStorey()).forGetter(GenSettings.Doors::boundaryMinPerStorey)
    ).apply(i, GenSettings.Doors::new));

    public static final Codec<GenSettings.Windows> WINDOWS = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 1).optionalFieldOf("room_chance", D.windows().roomChance()).forGetter(GenSettings.Windows::roomChance),
            Codec.intRange(0, 16).optionalFieldOf("max_per_room", D.windows().maxPerRoom()).forGetter(GenSettings.Windows::maxPerRoom),
            Codec.doubleRange(0, 1).optionalFieldOf("office_pair_chance", D.windows().officePairChance()).forGetter(GenSettings.Windows::officePairChance)
    ).apply(i, GenSettings.Windows::new));

    public static final Codec<GenSettings.Weighted> WEIGHTED = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(GenSettings.Weighted::type),
            Codec.intRange(0, 100000).fieldOf("weight").forGetter(GenSettings.Weighted::weight)
    ).apply(i, GenSettings.Weighted::new));

    private static final GenSettings.Furnishing F = D.furnishing();

    public static final Codec<GenSettings.Furnishing> FURNISHING = RecordCodecBuilder.create(i -> i.group(
            WEIGHTED.listOf().optionalFieldOf("small_rooms", F.smallRooms()).forGetter(GenSettings.Furnishing::smallRooms),
            WEIGHTED.listOf().optionalFieldOf("medium_rooms", F.mediumRooms()).forGetter(GenSettings.Furnishing::mediumRooms),
            WEIGHTED.listOf().optionalFieldOf("large_rooms", F.largeRooms()).forGetter(GenSettings.Furnishing::largeRooms),
            WEIGHTED.listOf().optionalFieldOf("room_lighting", F.roomLighting()).forGetter(GenSettings.Furnishing::roomLighting),
            WEIGHTED.listOf().optionalFieldOf("corridor_lighting", F.corridorLighting()).forGetter(GenSettings.Furnishing::corridorLighting),
            Codec.doubleRange(0, 1).optionalFieldOf("atrium_lit_chance", F.atriumLitChance()).forGetter(GenSettings.Furnishing::atriumLitChance),
            Codec.doubleRange(0, 1).optionalFieldOf("water_cooler_chance", F.waterCoolerChance()).forGetter(GenSettings.Furnishing::waterCoolerChance),
            Codec.intRange(3, 32).optionalFieldOf("light_spacing", F.lightSpacing()).forGetter(GenSettings.Furnishing::lightSpacing),
            Codec.intRange(3, 64).optionalFieldOf("dim_light_spacing", F.dimLightSpacing()).forGetter(GenSettings.Furnishing::dimLightSpacing)
    ).apply(i, GenSettings.Furnishing::new));

    private static final GenSettings.Special SP = D.special();

    public static final Codec<GenSettings.Special> SPECIAL = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 1).optionalFieldOf("outpost_chance", SP.outpostChance()).forGetter(GenSettings.Special::outpostChance),
            Codec.intRange(4, 10000).optionalFieldOf("outpost_min_cells", SP.outpostMinCells()).forGetter(GenSettings.Special::outpostMinCells),
            Codec.doubleRange(0, 1).optionalFieldOf("lush_chance", SP.lushChance()).forGetter(GenSettings.Special::lushChance),
            Codec.intRange(1, 10000).optionalFieldOf("lush_min_cells", SP.lushMinCells()).forGetter(GenSettings.Special::lushMinCells),
            Codec.intRange(4, 4000).optionalFieldOf("lush_min_clear", SP.lushMinClear()).forGetter(GenSettings.Special::lushMinClear),
            Codec.doubleRange(0, 1).optionalFieldOf("water_chance", SP.waterChance()).forGetter(GenSettings.Special::waterChance),
            Codec.intRange(1, 10000).optionalFieldOf("water_min_cells", SP.waterMinCells()).forGetter(GenSettings.Special::waterMinCells),
            Codec.intRange(1, 10000).optionalFieldOf("water_max_cells", SP.waterMaxCells()).forGetter(GenSettings.Special::waterMaxCells),
            Codec.doubleRange(0, 1).optionalFieldOf("sculk_chance", SP.sculkChance()).forGetter(GenSettings.Special::sculkChance),
            Codec.intRange(1, 10000).optionalFieldOf("sculk_min_cells", SP.sculkMinCells()).forGetter(GenSettings.Special::sculkMinCells),
            Codec.intRange(0, 64).optionalFieldOf("dark_bottom_storeys", SP.darkBottomStoreys()).forGetter(GenSettings.Special::darkBottomStoreys),
            Codec.doubleRange(0, 1).optionalFieldOf("abandoned_camp_chance", SP.campChance()).forGetter(GenSettings.Special::campChance),
            Codec.doubleRange(0, 1).optionalFieldOf("oasis_region_chance", SP.oasisRegionChance()).forGetter(GenSettings.Special::oasisRegionChance),
            Codec.doubleRange(0, 1).optionalFieldOf("exit_room_chance", SP.exitChance()).forGetter(GenSettings.Special::exitChance),
            Codec.doubleRange(0, 1).optionalFieldOf("poolrooms_region_chance", SP.poolroomsRegionChance()).forGetter(GenSettings.Special::poolroomsRegionChance),
            Codec.intRange(0, 32).optionalFieldOf("hideout_attempts", SP.hideoutAttempts()).forGetter(GenSettings.Special::hideoutAttempts)
    ).apply(i, GenSettings.Special::new));

    public static final Codec<GenSettings> CODEC = RecordCodecBuilder.create(i -> i.group(
            GRID.optionalFieldOf("grid", D.grid()).forGetter(GenSettings::grid),
            HEIGHT_BAND.listOf().optionalFieldOf("room_heights", D.roomHeights()).forGetter(GenSettings::roomHeights),
            ROOM_SIZE.listOf().optionalFieldOf("room_sizes", D.roomSizes()).forGetter(GenSettings::roomSizes),
            Codec.doubleRange(0, 1).optionalFieldOf("composite_chance", D.compositeChance()).forGetter(GenSettings::compositeChance),
            CORRIDORS.optionalFieldOf("corridors", D.corridors()).forGetter(GenSettings::corridors),
            Codec.intRange(0, 32).optionalFieldOf("stairs_per_storey", D.stairsPerStorey()).forGetter(GenSettings::stairsPerStorey),
            ATRIA.optionalFieldOf("atria", D.atria()).forGetter(GenSettings::atria),
            DOORS.optionalFieldOf("doors", D.doors()).forGetter(GenSettings::doors),
            WINDOWS.optionalFieldOf("windows", D.windows()).forGetter(GenSettings::windows),
            FURNISHING.optionalFieldOf("furnishing", D.furnishing()).forGetter(GenSettings::furnishing),
            SPECIAL.optionalFieldOf("special_rooms", D.special()).forGetter(GenSettings::special),
            Codec.intRange(0, 100).optionalFieldOf("content_version", D.contentVersion()).forGetter(GenSettings::contentVersion)
    ).apply(i, GenSettings::new));
}
