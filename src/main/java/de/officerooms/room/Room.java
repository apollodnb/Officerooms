package de.officerooms.room;

import de.officerooms.atrium.Atrium;

/**
 * Ein geplanter Raum innerhalb einer Region.
 *
 * <p>Ein Raum belegt eine beliebige (zusammenhängende) Menge von Rasterzellen eines Geschosses
 * und kann sich über mehrere Geschosse erstrecken ({@link #storeys}). Die lichte Höhe
 * {@link #clear} ist unabhängig vom Geschossraster: überzählige Blöcke unter der nächsten
 * Geschossdecke werden als abgehängte Decke gefüllt. So bleibt die 4-Block-Regel exakt,
 * und trotzdem sind Höhen wie 6 oder 13 möglich.</p>
 */
public final class Room {

    public enum Kind {
        /** Büroraum jeder Größe, auch Großraum und zusammengesetzte Grundrisse. */
        ROOM,
        /** Korridornetz (gerade, L, T, Kreuzungen) als ein zusammenhängender Raum. */
        CORRIDOR,
        /** Gerades Treppenhaus über genau ein Geschoss. */
        STAIRS,
        /** Mehrgeschossiges Atrium. */
        ATRIUM
    }

    public final int index;
    public final Kind kind;
    /** Unterstes Geschoss (dort liegt der Boden). */
    public final int bottom;
    /** Anzahl belegter Geschosse. */
    public int storeys;
    /** Lichte Innenhöhe in Blöcken (Bodenoberkante bis Deckenunterkante). */
    public int clear;
    /** Größenklasse aus der Konfiguration, z. B. "office" oder "open_plan". */
    public String sizeClass;
    /** Anzahl Grundrisszellen (im untersten Geschoss). */
    public int cells;
    /** True, wenn der Raum nicht erreichbar wäre und deshalb massiv gefüllt wird. */
    public boolean sealed;
    /** True, wenn der Grundriss aus mehreren Rechtecken zusammengesetzt ist. */
    public boolean composite;

    // Treppenhaus
    public int stairX;
    public int stairZ;
    public int stairDir;
    public int stairLength;
    public int lowEdgeKey = -1;
    public int highEdgeKey = -1;

    // Atrium
    public Atrium atrium;

    public Room(int index, Kind kind, int bottom, int storeys, int clear) {
        this.index = index;
        this.kind = kind;
        this.bottom = bottom;
        this.storeys = storeys;
        this.clear = clear;
    }

    public int top() {
        return bottom + storeys - 1;
    }

    public boolean isWalkableRoom() {
        return kind == Kind.ROOM || kind == Kind.CORRIDOR;
    }

    @Override
    public String toString() {
        return kind + "#" + index + "(" + sizeClass + ", storey " + bottom + ", " + storeys
                + " storey(s), clear height " + clear + ", " + cells + " cells" + (sealed ? ", sealed" : "") + ")";
    }
}
