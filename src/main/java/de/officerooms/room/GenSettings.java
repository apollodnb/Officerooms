package de.officerooms.room;

import java.util.List;

/**
 * Zentrale, datengetriebene Konfiguration der Office-Rooms-Generierung.
 *
 * <p>Die Werte stehen im Generator-Abschnitt von {@code data/office_rooms/dimension/office_rooms.json}
 * ({@code "settings": {...}}) und werden über {@code GenSettingsCodec} gelesen. Fehlende Felder
 * bekommen die Standardwerte aus {@link #DEFAULT}. Die Klasse selbst kennt keine Minecraft-Typen,
 * damit der Planer ohne Spiel getestet werden kann.</p>
 *
 * <p>Alle Größen in „Einheiten“ beziehen sich auf {@link Grid#unitSize()} (Standard 4 Blöcke,
 * inklusive einer Wandlinie). Ein Modul von 16 × 16 Blöcken entspricht 4 × 4 Einheiten.</p>
 */
public record GenSettings(
        Grid grid,
        List<HeightBand> roomHeights,
        List<RoomSize> roomSizes,
        double compositeChance,
        Corridors corridors,
        int stairsPerStorey,
        Atria atria,
        Doors doors,
        Windows windows,
        Furnishing furnishing,
        Special special,
        int contentVersion) {

    /**
     * 0.28.0: content version of the world. 0 = worlds created before 0.28.0 (old hotel, old poolrooms, no new
     * special rooms, so explored areas stay consistent); 1 = new worlds (set in the dimension JSON).
     */
    public boolean modern() {
        return contentVersion >= 1;
    }

    /**
     * Raster.
     *
     * @param unitSize     Blöcke pro Rastereinheit (eine Wandlinie + Innenraum), 4–8. Ein Modul
     *                     von 16 × 16 Blöcken sind bei unit 4 genau 4 × 4 Einheiten.
     * @param regionUnits  Einheiten pro Planungsregion und Achse (Standard 40 → 160 Blöcke)
     * @param baseHeight   Lichte Höhe eines Standardgeschosses (Standard 4). Geschossraster = baseHeight + 1.
     * @param storeys      Anzahl Geschosse, 0 = so viele, wie in die Dimension passen
     */
    public record Grid(int unitSize, int regionUnits, int baseHeight, int storeys) {
    }

    /** Raumhöhenband: lichte Höhe in [min, max], Gewicht relativ zu den anderen Bändern. */
    public record HeightBand(int min, int max, int weight) {
    }

    /** Raumgröße in Einheiten (Breite × Tiefe, vor zufälliger Drehung). */
    public record RoomSize(String name, int weight, int minW, int maxW, int minD, int maxD) {
    }

    public record Corridors(int seedsMin, int seedsMax, double maxShare, double wideChance,
                            int segmentMin, int segmentMax, double longChance, int longMax,
                            double turnChance, double branchChance, double sideBranchChance) {
    }

    /** Atriumklasse: Grundfläche in Einheiten, lichte Höhe in Blöcken. */
    public record AtriumClass(String name, int weight, int minW, int maxW, int minD, int maxD,
                              int minH, int maxH) {
    }

    public record Atria(double density, double galleryChance, double bridgeChance, List<AtriumClass> classes) {
    }

    public record Doors(int normal, int dbl, int wide, int open, double extraChance,
                        double secondOpeningChance, double boundaryChance, int boundaryMinPerStorey) {
    }

    /**
     * Fenster.
     *
     * @param roomChance       Anteil der Räume an einem Atrium, die Atriumfenster bekommen
     * @param maxPerRoom       höchstens so viele Atriumfenster pro Raum
     * @param officePairChance Wahrscheinlichkeit pro Paar benachbarter Büroräume für ein Fenster
     *                         zwischen ihnen (selten, Phase 2)
     */
    public record Windows(double roomChance, int maxPerRoom, double officePairChance) {
    }

    /** Gewichtete Auswahl eines Einrichtungstyps. */
    public record Weighted(String type, int weight) {
    }

    /**
     * Einrichtung und Beleuchtung (Phase 2).
     *
     * @param smallRooms   Einrichtungstypen für kleine Räume (bis 9 Zellen)
     * @param mediumRooms  Einrichtungstypen für mittlere Räume (bis 30 Zellen)
     * @param largeRooms   Einrichtungstypen für große Räume
     * @param roomLighting Beleuchtungsvarianten für Räume (normal, dim, partial, dark)
     * @param corridorLighting Beleuchtungsvarianten für Korridore (bright, dark)
     * @param atriumLitChance  Anteil der Atrien mit beleuchteter Decke/Galerien
     * @param waterCoolerChance Wahrscheinlichkeit je Korridornetz für einen Wasserspender
     * @param lightSpacing     Abstand der Deckenleuchten in normal beleuchteten Räumen (Blöcke)
     * @param dimLightSpacing  Abstand in schwach beleuchteten Räumen und dunklen Korridoren
     */
    public record Furnishing(List<Weighted> smallRooms, List<Weighted> mediumRooms, List<Weighted> largeRooms,
                             List<Weighted> roomLighting, List<Weighted> corridorLighting,
                             double atriumLitChance, double waterCoolerChance,
                             int lightSpacing, int dimLightSpacing) {
    }

    /**
     * Spezialräume (Phase 4). Jede Chance gilt pro geeignetem Büroraum. Geeignet: Mindestzahl an
     * Grundrisszellen, Lush zusätzlich Mindest-Lichthöhe, Water höchstens ein Geschoss hoch.
     * {@code darkBottomStoreys}: so viele unterste Geschosse haben keinerlei künstliches Licht.
     * {@code hideoutAttempts} (0.27.0): Versuche pro Region für versteckte Räume. Standard 6 (alte Welten ohne das
     * Feld bleiben unverändert), neue Welten 12 über die Dimension-JSON.
     */
    public record Special(double outpostChance, int outpostMinCells,
                          double lushChance, int lushMinCells, int lushMinClear,
                          double waterChance, int waterMinCells, int waterMaxCells,
                          double sculkChance, int sculkMinCells,
                          int darkBottomStoreys,
                          double campChance, double oasisRegionChance, double exitChance,
                          double poolroomsRegionChance,
                          int hideoutAttempts) {
    }

    public static final GenSettings DEFAULT = new GenSettings(
            new Grid(4, 40, 4, 0),
            List.of(
                    new HeightBand(4, 4, 90),
                    new HeightBand(5, 8, 7),
                    new HeightBand(9, 16, 2),
                    new HeightBand(17, 28, 1)),
            List.of(
                    new RoomSize("small_square", 20, 2, 3, 2, 3),
                    new RoomSize("small_rect", 18, 2, 3, 3, 5),
                    new RoomSize("office", 20, 4, 4, 4, 4),
                    new RoomSize("office_varied", 14, 3, 5, 3, 6),
                    new RoomSize("rect_office", 10, 8, 8, 4, 4),
                    new RoomSize("open_plan", 8, 8, 8, 8, 8),
                    new RoomSize("large_open", 6, 12, 12, 8, 8),
                    new RoomSize("huge_open", 3, 16, 16, 12, 12),
                    new RoomSize("vast_open", 1, 16, 20, 12, 16)),
            0.15,
            new Corridors(1, 3, 0.30, 0.2, 3, 10, 0.12, 24, 0.45, 0.25, 0.05),
            3,
            new Atria(0.1, 0.6, 0.45, List.of(
                    new AtriumClass("small", 30, 3, 5, 3, 5, 12, 20),
                    new AtriumClass("medium", 25, 7, 9, 7, 9, 20, 40),
                    new AtriumClass("large", 15, 11, 13, 11, 13, 40, 80),
                    new AtriumClass("shaft", 12, 2, 3, 3, 6, 40, 120),
                    new AtriumClass("hall", 8, 12, 20, 8, 12, 20, 40),
                    new AtriumClass("huge", 7, 15, 17, 15, 17, 80, 160),
                    new AtriumClass("mega", 3, 24, 30, 24, 30, 160, 300))),
            new Doors(45, 25, 15, 15, 0.25, 0.15, 0.12, 2),
            new Windows(0.6, 2, 0.02),
            new Furnishing(
                    List.of(new Weighted("empty", 35), new Weighted("standard_office", 38), new Weighted("archive", 10),
                            new Weighted("break_room", 9), new Weighted("server_room", 8)),
                    List.of(new Weighted("empty", 22), new Weighted("standard_office", 30), new Weighted("meeting_room", 18),
                            new Weighted("break_room", 12), new Weighted("archive", 10), new Weighted("server_room", 8)),
                    List.of(new Weighted("empty", 15), new Weighted("open_office", 60), new Weighted("meeting_room", 10),
                            new Weighted("break_room", 8), new Weighted("archive", 7)),
                    List.of(new Weighted("normal", 45), new Weighted("dim", 25), new Weighted("partial", 20), new Weighted("dark", 10)),
                    List.of(new Weighted("bright", 40), new Weighted("dark", 60)),
                    0.55, 0.12, 7, 11),
            new Special(0.03, 24, 0.15, 12, 9, 0.004, 6, 40, 0.0008, 9, 5, 0.004, 0.06, 0.08, 0.07, 6),
            0);

    // ---------------------------------------------------------------------------------------
    // Abgeleitete, abgesicherte Werte
    // ---------------------------------------------------------------------------------------

    public int unit() {
        return clamp(grid.unitSize(), 4, 8);
    }

    public int regionUnits() {
        return clamp(grid.regionUnits(), 16, 64);
    }

    public int baseHeight() {
        // Treppen brauchen pitch + 4 <= stairCells * unit; mit 3 Zellen und unit 4 also baseHeight <= 7.
        return clamp(grid.baseHeight(), 3, 7);
    }

    /** Geschossraster: 1 Block Decke/Boden + lichte Standardhöhe. */
    public int pitch() {
        return baseHeight() + 1;
    }

    /** Länge eines Treppenhauses in Einheiten (gerade Treppe über genau ein Geschoss). */
    public int stairCells() {
        int u = unit();
        return Math.max(2, (pitch() + 4 + u - 1) / u);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
