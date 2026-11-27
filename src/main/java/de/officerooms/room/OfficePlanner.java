package de.officerooms.room;

import de.officerooms.room.connection.OpeningPlanner;

/**
 * Einstieg in die Raumplanung einer Welt (ein Seed, eine Konfiguration, eine Dimensionshöhe).
 *
 * <p>Vertikales Raster: Y = minY ist Bedrock, ab Y0 = minY + 1 folgen Geschosse im Abstand
 * {@link #phys} = {@code pitch} + 1 (Standard 6 = Quarzdecke + grauer Wollboden + 4 Blöcke lichte
 * Höhe). Über dem obersten Geschoss liegt eine durchgehende Decke, darüber ist die Welt massiv.</p>
 *
 * <p>Seit 0.6.1 ist die Geschossplatte zwei Blöcke dick. Die gesamte Planung (Räume, Türen,
 * Fenster, Atrien, Spezialräume) rechnet weiter im Entwurfsraster {@code pitch} (Standard 5);
 * der Rasterer schreibt die Bodenschicht eines Geschosses doppelt (unten Quarz = Decke des
 * Geschosses darunter, oben Wolle = Boden). {@link #floorY(int)} ist die Wollschicht, die
 * Laufebene liegt wie bisher bei floorY + 1. Für Höhen innerhalb mehrgeschossiger Räume gilt
 * {@link #yOf(int, int)}.</p>
 *
 * <p>Horizontales Raster: Regionen von {@code regionUnits × unit} Blöcken (Standard 160), darin
 * Zellen von {@code unit} Blöcken (Standard 4). Die Wandlinie jeder Zelle liegt auf ihrer West-
 * bzw. Nordseite (lokal x bzw. z = 0), Wände sind also immer genau 1 Block dick und werden von
 * beiden Nachbarzellen geteilt.</p>
 */
public final class OfficePlanner {

    public final long seed;
    public final GenSettings cfg;
    public final int minY;
    public final int height;
    public final int unit;
    public final int ru;
    public final int regionBlocks;
    /** Entwurfsraster (Standard 5): Boden + lichte Höhe, so wie Räume, Türen und Atrien geplant sind. */
    public final int pitch;
    /** Physisches Geschossraster (Standard 6): Quarzdecke + Wollboden + lichte Höhe. */
    public final int phys;
    public final int storeys;
    public final int y0;

    /** The Poolrooms (0.6.0): extrem seltene Megastruktur, ersetzt in einzelnen Regionen fünf Geschosse. */
    public final de.officerooms.megastructure.Poolrooms pools = new de.officerooms.megastructure.Poolrooms(this);

    /** 0.8.0: The Neighbourhood, a dark hall with 20 identical houses (as rare as the Poolrooms). */
    public final de.officerooms.megastructure.Neighbourhood hood = new de.officerooms.megastructure.Neighbourhood(this);
    /** 0.8.0: The Maze (Level 0) with its security vault (as rare as the Poolrooms). */
    public final de.officerooms.megastructure.Maze maze = new de.officerooms.megastructure.Maze(this);
    /** 0.14.0: The Abandoned Hotel (basement with vault, lobby, two guest floors). */
    public final de.officerooms.megastructure.Hotel hotel = new de.officerooms.megastructure.Hotel(this);
    /** 0.15.0: The Industrial Complex (five levels of pipe rooms and two generator halls). */
    public final de.officerooms.megastructure.Industrial industrial = new de.officerooms.megastructure.Industrial(this);
    /** 0.16.0: Level Fun with its Partygoers. */
    public final de.officerooms.megastructure.LevelFun fun = new de.officerooms.megastructure.LevelFun(this);
    public final de.officerooms.megastructure.Cinema cinema = new de.officerooms.megastructure.Cinema(this);
    /** 0.43.0 */
    public final de.officerooms.megastructure.Mall mall = new de.officerooms.megastructure.Mall(this);

    /** 0.9.0: elevators and secure storage rooms laid over the plan. */
    public final Extras extras = new Extras(this);

    /** Whether the position lies in the band of any megastructure (own architecture, not the office plan). */
    public boolean megaContains(int x, int y, int z) {
        return pools.contains(x, y, z) || hood.contains(x, y, z) || maze.contains(x, y, z) || hotel.contains(x, y, z) || industrial.contains(x, y, z) || fun.contains(x, y, z) || cinema.contains(x, y, z) || mall.contains(x, y, z);
    }

    private final PlanCache<RegionLayout> layouts = new PlanCache<>(64);
    private final PlanCache<RegionPlan> plans = new PlanCache<>(48);

    public OfficePlanner(long seed, GenSettings cfg, int minY, int height) {
        this.seed = seed;
        this.cfg = cfg;
        this.minY = minY;
        this.height = height;
        this.unit = cfg.unit();
        this.ru = cfg.regionUnits();
        this.regionBlocks = unit * ru;
        this.pitch = cfg.pitch();
        this.phys = pitch + 1;
        this.y0 = minY + 1;
        int max = maxStoreys(cfg, height);
        int want = cfg.grid().storeys();
        this.storeys = want <= 0 ? max : Math.max(5, Math.min(want, max));
    }

    /** Wie viele Geschosse in die Dimensionshöhe passen (Bedrock + Geschosse + Abschlussdecke). */
    public static int maxStoreys(GenSettings cfg, int height) {
        return (height - 2) / (cfg.pitch() + 1);
    }

    private static long key(int rx, int rz) {
        return ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
    }

    public RegionLayout layout(int rx, int rz) {
        return layouts.get(key(rx, rz), () -> LayoutBuilder.build(cfg, seed, rx, rz, storeys));
    }

    public RegionPlan plan(int rx, int rz) {
        return plans.get(key(rx, rz), () -> OpeningPlanner.build(cfg, seed, layout(rx, rz), layout(rx - 1, rz), layout(rx, rz - 1)));
    }

    public int regionOf(int blockCoord) {
        return Math.floorDiv(blockCoord, regionBlocks);
    }

    /** Geschoss eines Y-Werts (kann außerhalb [0, storeys) liegen). */
    public int storeyOf(int y) {
        return Math.floorDiv(y - y0, phys);
    }

    /** Unterste Schicht der Geschossplatte von s: Quarz, von unten die Decke des Geschosses s - 1. */
    public int slabY(int s) {
        return y0 + s * phys;
    }

    /** Y des Bodenblocks (graue Wolle) von Geschoss s. Laufebene ist floorY + 1. */
    public int floorY(int s) {
        return slabY(s) + 1;
    }

    /**
     * Physisches Y zur Entwurfshöhe t über dem Boden von Geschoss s (t = 0 ist der Boden). Jede
     * überschrittene Geschossgrenze innerhalb eines hohen Raums bringt eine zusätzliche Schicht.
     */
    public int yOf(int s, int t) {
        return floorY(s) + t + Math.floorDiv(t, pitch);
    }
}
