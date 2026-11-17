package de.officerooms.furnish;

/**
 * Abstrakte Einrichtungsgegenstände mit fester Belegung. Erst die Kompatibilitätsschicht
 * ({@code de.officerooms.compat.FurnitureBuilder}) ordnet ihnen echte Blöcke zu, je nachdem,
 * welche Möbel-Mods installiert sind (OfficeCraft, Refurbished Furniture oder Vanilla-Ersatz).
 *
 * <p>Belegung: {@code width} quer zur Blickrichtung, {@code depth} entlang der Blickrichtung,
 * beides symmetrisch um den Hauptblock (ungerade Werte), {@code height} Blöcke ab der Laufebene.
 * Die Werte entsprechen den tatsächlich im Spiel ausgelesenen Belegungen der OfficeCraft-Möbel
 * ({@code /officerooms furniture}); Ersatzblöcke belegen höchstens dieselben Zellen.</p>
 */
public enum Item {
    DESK(3, 1, 2, false),
    WORKSTATION(3, 3, 2, false),
    PARTITION(3, 1, 2, false),
    CHAIR(1, 1, 2, false),
    CONFERENCE_LARGE(7, 3, 2, false),
    CONFERENCE_MEDIUM(5, 3, 2, false),
    BOARD(3, 1, 2, false),
    WATER_COOLER(1, 1, 2, false),
    VENDING(1, 1, 2, false),
    PRINTER(1, 1, 1, false),
    /** Kaffeemaschine, steht auf einer Arbeitsplatte (y + 1). */
    COFFEE(1, 1, 1, false),
    /** Schublade (Container mit Loot). */
    DRAWER(1, 1, 1, true),
    /** Schublade mit Schrank darüber (Archiv), beide Container. */
    DRAWER_STACK(1, 1, 2, true),
    COUNTER(1, 1, 1, false),
    SINK(1, 1, 1, false),
    /** Kühlschrank mit Gefrierfach darüber (Container). */
    FRIDGE(1, 1, 2, true),
    /** Mikrowelle, steht auf einer Arbeitsplatte (y + 1). */
    MICROWAVE(1, 1, 1, false),
    TABLE(1, 1, 1, false),
    SIDE_CHAIR(1, 1, 1, false),
    /** Server-Rack, 2 hoch. */
    RACK(1, 1, 2, false),
    /** 0.17.0: HAZARD ZONE monitor standing against a wall (variant 1 = broken). */
    MONITOR(1, 1, 1, false),
    /** 0.23.0: Refurbished toilet / wash basin (vanilla fallback). */
    TOILET(1, 1, 1, false),
    BASIN(1, 1, 1, false),
    /** 0.23.0: Refurbished dark oak desk segment. */
    EXEC_DESK(1, 1, 1, false),
    /** 0.23.0: Refurbished sofa segment (variant = colour). */
    SOFA(1, 1, 1, false),
    /** 0.23.0: Refurbished dark oak storage cabinet (container). */
    CABINET(1, 1, 1, true),
    /** 0.23.0: two-high vanilla bookshelf. */
    BOOKSHELF(1, 1, 2, false),
    /** 0.23.0: lectern with an orientation handout. */
    LECTERN(1, 1, 1, false),
    /** 0.23.0: one Office Rooms decoration block, variant = index in ModBlocks.ROOM_DECOR. */
    DECOR(1, 1, 1, false),
    /** 0.23.0: thin floor layer replacing the carpet (variant 0 tiles, 1 rug, 2 doormat). Never blocks paths. */
    FLOOR_COVER(1, 1, 1, false),
    /** 0.23.0: wall tiles on the quartz wall column at x/z (variant = rows, facing = into the room). */
    WALL_COVER(1, 1, 1, false),
    /** Deckenpanel, an der Decke. */
    LIGHT(1, 1, 1, false),
    /** Lichtleiste (Korridore), an der Decke. */
    LIGHT_BAR(1, 1, 1, false);

    public final int width;
    public final int depth;
    public final int height;
    public final boolean container;

    Item(int width, int depth, int height, boolean container) {
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.container = container;
    }

    /** 0.23.0: surface overlays are placed outside the normal footprint/vacancy rules. */
    public boolean isSurface() {
        return this == FLOOR_COVER || this == WALL_COVER;
    }

    public boolean isLight() {
        return this == LIGHT || this == LIGHT_BAR;
    }
}
