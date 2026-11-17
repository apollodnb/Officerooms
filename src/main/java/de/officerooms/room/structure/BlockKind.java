package de.officerooms.room.structure;

/**
 * Abstrakte Blockarten des Rasterers. Erst der Chunk-Generator ordnet ihnen Minecraft-Blöcke zu
 * (siehe {@code OfficeChunkGenerator}). So bleibt der Planer ohne Minecraft testbar, und eine
 * spätere Materialvariante ändert nur die Zuordnung.
 */
public final class BlockKind {

    public static final byte AIR = 0;
    /** Bedrock-Boden der Welt. */
    public static final byte BEDROCK = 1;
    /** Geschossdecke/-boden: glatter Quarz. */
    public static final byte FLOOR = 2;
    /** Wand: Quarzblock. */
    public static final byte WALL = 3;
    /** Wandecke/-knoten: Quarzsäule. */
    public static final byte PILLAR = 4;
    /** Massive, nie sichtbare Füllung: weißer Beton. */
    public static final byte FILL = 5;
    /** Abgehängte Decke (Raum niedriger als sein Geschossraster): glatter Quarz. */
    public static final byte CEILING = 6;
    /** Atriumfenster: Glas. */
    public static final byte GLASS = 7;
    /** Galerie-/Brückenbrüstung unten: Quarzblock. */
    public static final byte PARAPET = 8;
    /** Galerie-/Brückenbrüstung oben: glatte Quarzstufe. */
    public static final byte PARAPET_TOP = 9;
    /** Treppenstufen, Aufstiegsrichtung Ost/Süd/West/Nord. */
    public static final byte STAIR_E = 10;
    public static final byte STAIR_S = 11;
    public static final byte STAIR_W = 12;
    public static final byte STAIR_N = 13;

    /** Bodenbelag auf jeder begehbaren Bodenfläche: grauer Teppich (Phase 2). */
    public static final byte CARPET = 14;

    /** Bodenbelag seit 0.6.1: graue Wolle als obere Schicht der Geschossplatte (statt Teppich). */
    public static final byte FLOOR_WOOL = 15;

    /** 0.9.0: elevator floor plate, elevator shaft wall, security plating of a strongroom. */
    public static final byte ELEVATOR = 16;
    public static final byte ELEV_WALL = 17;
    public static final byte SECURITY = 18;

    /** 0.26.0: cabin ceiling light and the button panel on the cabin wall (facing E, S, W, N into the cabin). */
    public static final byte ELEV_LIGHT = 19;
    public static final byte ELEV_PANEL_E = 20;

    public static final int COUNT = 24;

    private BlockKind() {
    }

    /** Blockarten, die auf denselben Minecraft-Block abgebildet werden, auf einen Vertreter reduzieren. */
    public static int canonical(int k) {
        if (k == CEILING) {
            return FLOOR;
        }
        if (k == PARAPET) {
            return WALL;
        }
        return k;
    }

    /** Begehbar: Luft oder Bodenbelag. */
    public static boolean isAirLike(int k) {
        return k == AIR || k == CARPET;
    }

    public static boolean isStair(int k) {
        return k >= STAIR_E && k <= STAIR_N;
    }
}
