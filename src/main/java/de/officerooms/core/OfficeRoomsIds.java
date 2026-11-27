package de.officerooms.core;

/**
 * Zentrale Bezeichner des Mods.
 *
 * <p>Alle Resource Locations werden aus {@link #MOD_ID} gebildet. Wenn spaeter
 * {@code ResourceLocation}/{@code ResourceKey}-Konstanten benoetigt werden, sollen sie
 * ueber {@link #id(String)} erzeugt werden, damit der Namespace nur an einer Stelle steht.</p>
 */
public final class OfficeRoomsIds {

    /** Mod-ID, muss mit {@code mods.toml} und dem Datapack-Namespace uebereinstimmen. */
    public static final String MOD_ID = "office_rooms";

    public static final String MOD_NAME = "Office Rooms";

    /** 0.40.0: the real version from the mod list (the old constant was never updated). */
    public static String version() {
        return net.neoforged.fml.ModList.get().getModContainerById(MOD_ID).map(c -> c.getModInfo().getVersion().toString()).orElse("unknown");
    }

    private OfficeRoomsIds() {
    }

    /** Liefert {@code office_rooms:<path>} als Text. */
    public static String id(String path) {
        return MOD_ID + ":" + path;
    }
}
