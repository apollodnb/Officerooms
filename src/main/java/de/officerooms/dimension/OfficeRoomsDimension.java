package de.officerooms.dimension;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.worldgen.OfficeWorldgen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Bezeichner und Eckdaten der Office-Rooms-Dimension.
 *
 * <p><b>Registrierung:</b> In Minecraft 1.20.1 sind {@code dimension} (LevelStem),
 * {@code dimension_type} und {@code worldgen/biome} dynamische Registries. Sie werden
 * nicht per Java-Code registriert, sondern beim Laden der Welt aus den Datapack-Dateien
 * gelesen, die Forge aus jeder Mod-JAR bereitstellt:</p>
 * <ul>
 *     <li>{@code data/office_rooms/dimension/office_rooms.json}: Dimension + Chunk-Generator</li>
 *     <li>{@code data/office_rooms/dimension_type/office_rooms.json}: Bauhoehe, Licht, Effekte</li>
 *     <li>{@code data/office_rooms/worldgen/biome/office_void.json}: leeres Biom ohne Spawns</li>
 * </ul>
 *
 * <p>Die Konstanten hier muessen mit diesen Dateien uebereinstimmen. Sie dienen als einzige
 * Java-Quelle fuer IDs (z. B. fuer spaetere {@code ResourceKey<Level>}-Konstanten,
 * Portale oder Befehle).</p>
 */
public final class OfficeRoomsDimension {

    /** {@code office_rooms:office_rooms}, Schluessel der Dimension (Level). */
    public static final String DIMENSION_ID = OfficeRoomsIds.id("office_rooms");

    /** {@code office_rooms:office_rooms}, Schluessel des Dimensionstyps. */
    public static final String DIMENSION_TYPE_ID = OfficeRoomsIds.id("office_rooms");

    /** {@code office_rooms:office_void}, einziges Biom der Dimension. */
    public static final String BIOME_ID = OfficeRoomsIds.id("office_void");

    /** Schlüssel der Dimension als Level-Key (für Teleport/Befehle). */
    public static final ResourceKey<Level> LEVEL_KEY =
            ResourceKey.create(Registries.DIMENSION, new ResourceLocation(OfficeRoomsIds.MOD_ID, "office_rooms"));

    /** Unterste Ebene (Bedrock-Boden). Muss ein Vielfaches von 16 sein. */
    public static final int MIN_Y = -64;

    /** Gesamthoehe in Bloecken. Muss ein Vielfaches von 16 sein, maximal 4064. */
    public static final int HEIGHT = 608;

    /** Hoechste bebaubare Ebene: MIN_Y + HEIGHT - 1 = 543 (0.6.1: 60 % der früheren 1024 Blöcke). */
    public static final int MAX_BUILD_Y = MIN_Y + HEIGHT - 1;


    private OfficeRoomsDimension() {
    }

    /**
     * Haengt Dimensions-bezogene Registrierungen an den Mod-Event-Bus: den Codec des
     * Office-Rooms-Chunk-Generators ({@code office_rooms:office_rooms}).
     */
    public static void register(IEventBus modEventBus) {
        OfficeWorldgen.register(modEventBus);
    }
}
