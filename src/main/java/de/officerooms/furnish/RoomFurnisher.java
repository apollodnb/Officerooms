package de.officerooms.furnish;

import de.officerooms.atrium.Atrium;
import de.officerooms.core.Hashing;
import de.officerooms.core.Hashing.Rng;
import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.RegionPlan;
import de.officerooms.room.Room;
import de.officerooms.room.connection.Openings;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Plant Einrichtung und Beleuchtung eines einzelnen Raums. Rein deterministisch aus
 * {@code hash(seed, Region, Raumindex)}; arbeitet ausschließlich auf der fertigen Raumplanung
 * (die Architektur wird nicht verändert).
 *
 * <p>Regeln, die für jeden Gegenstand geprüft werden:</p>
 * <ul>
 *     <li>alle belegten Felder liegen im Innenraum dieses Raums (nie in Wänden, nie außerhalb),</li>
 *     <li>kein Feld ist bereits belegt (keine Überschneidungen),</li>
 *     <li>vor jeder Tür/jedem Durchgang bleiben 2 Felder frei, vor Fenstern 1 Feld,</li>
 *     <li>alle Felder liegen im selben Chunk wie der Hauptblock (kein Hineinragen in Nachbarchunks),</li>
 *     <li>der Gegenstand ist niedriger als die lichte Raumhöhe (oberster Block bleibt für Leuchten frei),</li>
 *     <li>nach dem Setzen sind alle Türfelder des Raums weiterhin zu Fuß miteinander verbunden
 *     (Breitensuche); sonst wird der Gegenstand wieder entfernt.</li>
 * </ul>
 */
public final class RoomFurnisher {

    static final int[] DX = {1, 0, -1, 0};
    static final int[] DZ = {0, 1, 0, -1};

    /**
     * Ergebnis: Einrichtungstyp, Lichtvariante, Gegenstände und ein freier Standpunkt (Weltkoordinaten
     * x, y, z, Blickziel x, z) vor einer Tür mit Blick in den Raum, oder null.
     */
    public record Result(RoomType type, String lighting, List<Placement> placements, int[] view) {
        public Result(RoomType type, String lighting, List<Placement> placements) {
            this(type, lighting, placements, null);
        }
    }

    private final OfficePlanner p;
    private final RegionPlan plan;
    private final RegionLayout l;
    private final Room r;
    private final GenSettings.Furnishing f;
    private final int rx;
    private final int rz;
    private final int s;
    private final int unit;
    private final Rng rng;
    /** Eigener Zufallsstrom für Licht: Leuchten hängen nicht von der Möbelplanung ab. */
    private final Rng lightRng;
    private final boolean lightsOnly;
    private final int baseX;
    private final int baseZ;

    // Raster des Raums (regionslokale Blockkoordinaten)
    private int gx0;
    private int gz0;
    private int gw;
    private int gd;
    private boolean[] interior;
    private boolean[] occupied;
    private de.officerooms.room.Extras.Region extras;
    private boolean[] keep;
    private boolean[] noFurniture;
    private int floorY;
    private int topY;
    private final List<Placement> out = new ArrayList<>();
    /** Öffnungen auf dem Raumrand: {ux, uz, Seite, Code} (Seite 0 = Ost, 1 = Süd, 2 = West, 3 = Nord). */
    private final List<int[]> openings = new ArrayList<>();

    /**
     * Raster eines Raums für die Spezialräume (Phase 4): Innenraum, Freihalteflächen vor Türen und
     * Fenstern, Höhen und die Öffnungen auf dem Rand. Koordinaten wie im Einrichtungsplaner
     * (Rasterfeld bx, bz; Welt = base + g0 + b).
     */
    public record Geometry(int baseX, int baseZ, int gx0, int gz0, int gw, int gd, boolean[] interior, boolean[] keep,
                           boolean[] noFurniture, int floorY, int topY, int storey, int unit, List<int[]> openings) {
        public boolean in(int bx, int bz) {
            return bx >= 0 && bz >= 0 && bx < gw && bz < gd && interior[bz * gw + bx];
        }

        public boolean keep(int bx, int bz) {
            return in(bx, bz) && keep[bz * gw + bx];
        }

        public boolean noFurniture(int bx, int bz) {
            return in(bx, bz) && noFurniture[bz * gw + bx];
        }

        public int wx(int bx) {
            return baseX + gx0 + bx;
        }

        public int wz(int bz) {
            return baseZ + gz0 + bz;
        }

        public int bx(int wx) {
            return wx - baseX - gx0;
        }

        public int bz(int wz) {
            return wz - baseZ - gz0;
        }
    }

    public static Geometry geometry(OfficePlanner p, int rx, int rz, Room r) {
        RoomFurnisher f = new RoomFurnisher(p, rx, rz, r, true);
        f.buildGrid();
        return new Geometry(f.baseX, f.baseZ, f.gx0, f.gz0, f.gw, f.gd, f.interior, f.keep, f.noFurniture,
                f.floorY, f.topY, f.s, f.unit, List.copyOf(f.openings));
    }

    private RoomFurnisher(OfficePlanner p, int rx, int rz, Room r, boolean lightsOnly) {
        this.p = p;
        this.rx = rx;
        this.rz = rz;
        this.plan = p.plan(rx, rz);
        this.l = plan.layout;
        this.extras = p.extras.of(rx, rz);
        this.r = r;
        this.f = p.cfg.furnishing();
        this.s = r.kind == Room.Kind.ATRIUM ? r.atrium.s0 : r.bottom;
        this.unit = p.unit;
        this.rng = new Rng(Hashing.hash(p.seed, rx, rz, 0xFA, r.index));
        this.lightRng = new Rng(Hashing.hash(p.seed, rx, rz, 0xFB, r.index));
        this.lightsOnly = lightsOnly;
        this.baseX = rx * p.regionBlocks;
        this.baseZ = rz * p.regionBlocks;
    }

    public static Result furnish(OfficePlanner p, int rx, int rz, Room r) {
        return new RoomFurnisher(p, rx, rz, r, false).run();
    }

    /** Nur die Leuchten (für die Weltgenerierung); identisch zu den Leuchten von {@link #furnish}. */
    public static Result lights(OfficePlanner p, int rx, int rz, Room r) {
        return new RoomFurnisher(p, rx, rz, r, true).run();
    }

    private Result run() {
        if (p.pools.overlaps(rx, rz, r) || p.hood.overlaps(rx, rz, r) || p.maze.overlaps(rx, rz, r) || p.hotel.overlaps(rx, rz, r) || p.industrial.overlaps(rx, rz, r) || p.fun.overlaps(rx, rz, r) || p.cinema.overlaps(rx, rz, r)) {
            // im Band der Poolrooms gibt es keine Büroeinrichtung
            return new Result(RoomType.EMPTY, "none", List.of());
        }
        if (r.sealed || r.cells == 0 || r.kind == Room.Kind.STAIRS) {
            return new Result(RoomType.EMPTY, "none", List.of());
        }
        if (r.kind == Room.Kind.ATRIUM) {
            return atrium();
        }
        buildGrid();
        boolean dark = de.officerooms.special.SpecialRooms.darkStorey(p, s);
        if (r.kind == Room.Kind.CORRIDOR) {
            String lighting = dark ? "none" : pick(lightRng, f.corridorLighting(), "dark");
            if (rng.chance(f.waterCoolerChance()) && !lightsOnly) {
                waterCooler();
            }
            if (!dark) {
                corridorLights(lighting);
            }
            if (!lightsOnly && !dark && rng.chance(0.06)) {
                hazardMonitor();
            }
            if (!lightsOnly && p.cfg.modern() && rng.chance(0.006)) {
                onWall(Decor.SLIDE_ENTRANCE, 0);                         // 0.35.0: rare
            }
            return new Result(RoomType.CORRIDOR, lighting, List.copyOf(out));
        }
        de.officerooms.special.SpecialType special = de.officerooms.special.SpecialRooms.of(p, rx, rz, r);
        if (special != de.officerooms.special.SpecialType.NONE) {
            return specialRoom(special, dark);
        }
        List<GenSettings.Weighted> table = r.cells <= 9 ? f.smallRooms() : r.cells <= 30 ? f.mediumRooms() : f.largeRooms();
        RoomType type = RoomType.byId(pick(rng, table, "empty"));
        RoomType extra = extraType();
        if (extra != null) {
            type = extra;
        }
        String lighting = pick(lightRng, f.roomLighting(), "normal");
        if (type == RoomType.SERVER_ROOM) {
            lighting = lightRng.chance(0.5) ? "dark" : "dim";
        } else if (type == RoomType.RESTROOM) {
            lighting = lightRng.chance(0.6) ? "partial" : "dim";
        } else if ((type == RoomType.TRAINING_ROOM || type == RoomType.EXECUTIVE_OFFICE) && lightRng.chance(0.5)) {
            lighting = "dim";
        }
        if (dark) {
            lighting = "none";
        }
        switch (lightsOnly ? RoomType.EMPTY : type) {
            case STANDARD_OFFICE -> standardOffice();
            case OPEN_OFFICE -> openOffice();
            case MEETING_ROOM -> meetingRoom();
            case BREAK_ROOM -> breakRoom();
            case SERVER_ROOM -> serverRoom();
            case ARCHIVE -> archive();
            case TRAINING_ROOM -> trainingRoom();
            case EXECUTIVE_OFFICE -> executiveOffice();
            case RECEPTION -> reception();
            case RESTROOM -> restroom();
            default -> {
            }
        }
        if (!lightsOnly && !dark && type != RoomType.EMPTY && rng.chance(0.15)) {
            hazardMonitor();
        }
        if (!lightsOnly && p.cfg.modern() && rng.chance(0.004)) {
            onWall(Decor.SLIDE_ENTRANCE, 0);                             // 0.35.0: rare
        }
        roomLights(lighting);
        return new Result(type, lighting, List.copyOf(out), view());
    }

    // -------------------------------------------------------------------------------------------
    // 0.23.0: training room, executive office, reception, restroom
    // -------------------------------------------------------------------------------------------

    /**
     * The new standard rooms are rolled with their own random stream, so every room that does not become
     * one of them keeps exactly the layout it had before (also in existing worlds).
     */
    private RoomType extraType() {
        int x = new Rng(Hashing.hash(p.seed, rx, rz, 0xFC, r.index)).nextInt(1000);
        if (r.cells <= 9) {
            return x < 25 ? RoomType.RESTROOM : x < 85 ? RoomType.EXECUTIVE_OFFICE : null;
        }
        if (r.cells <= 30) {
            return x < 20 ? RoomType.RESTROOM : x < 70 ? RoomType.EXECUTIVE_OFFICE : x < 120 ? RoomType.RECEPTION : x < 180 ? RoomType.TRAINING_ROOM : null;
        }
        return x < 60 ? RoomType.RECEPTION : x < 140 ? RoomType.TRAINING_ROOM : null;
    }

    private Spec deco(int bx, int bz, int facing, int dy, int kind) {
        return new Spec(Item.DECOR, bx, bz, facing, dy, kind);
    }

    /** Height for wall decoration (at most {@code want}, below the ceiling lights). */
    private int wallY(int want) {
        return Math.max(1, Math.min(want, r.clear - 2));
    }

    private void floorCover(int bx, int bz, int kind) {
        if (in(bx, bz)) {
            out.add(new Placement(Item.FLOOR_COVER, wx(bx), floorY, wz(bz), 0, kind, null));
        }
    }

    /** One decoration on any free wall (dy = 0 standing, otherwise hanging). */
    private boolean onWall(int kind, int dy) {
        for (int[] w : wallTiles()) {
            if (place(null, deco(w[0], w[1], w[2], dy, kind))) {
                return true;
            }
        }
        return false;
    }

    /** A straight row along a wall: {@code len} specs per tile, longest first. */
    private boolean wallRow(int maxLen, int minLen, java.util.function.BiFunction<int[], Integer, Spec[]> tile) {
        List<int[]> walls = wallTiles();
        for (int len = maxLen; len >= minLen; len--) {
            for (int[] w : walls) {
                int right = (w[2] + 1) % 4;
                List<Spec> g = new ArrayList<>();
                for (int j = 0; j < len; j++) {
                    g.addAll(List.of(tile.apply(new int[]{w[0] + DX[right] * j, w[1] + DZ[right] * j, w[2]}, j)));
                }
                if (place(currentLoot, g.toArray(new Spec[0]))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String currentLoot;

    private void restroom() {
        // tiles on the floor and on every wall column of the room
        for (int bz = 0; bz < gd; bz++) {
            for (int bx = 0; bx < gw; bx++) {
                if (!in(bx, bz)) {
                    continue;
                }
                floorCover(bx, bz, 0);
                for (int d = 0; d < 4; d++) {
                    int ox = bx - DX[d];
                    int oz = bz - DZ[d];
                    if (!in(ox, oz)) {
                        out.add(new Placement(Item.WALL_COVER, wx(ox), floorY, wz(oz), d, r.clear, null));
                    }
                }
            }
        }
        // stalls: partition | toilet | partition | toilet | partition ...; one door may be locked
        boolean stalls = false;
        List<int[]> walls = wallTiles();
        for (int k = 3; k >= 1 && !stalls; k--) {
            for (int[] w : walls) {
                int n = w[2];
                int right = (n + 1) % 4;
                int locked = rng.chance(0.6) ? rng.nextInt(k) : -1;
                List<Spec> g = new ArrayList<>();
                for (int c = 0; c <= 2 * k; c++) {
                    int bx = w[0] + DX[right] * c;
                    int bz = w[1] + DZ[right] * c;
                    if (c % 2 == 0) {
                        for (int row = 0; row < 2; row++) {
                            g.add(deco(bx + DX[n] * row, bz + DZ[n] * row, n, 0, Decor.STALL_PARTITION));
                            g.add(deco(bx + DX[n] * row, bz + DZ[n] * row, n, 1, Decor.STALL_PARTITION_TOP));
                        }
                    } else {
                        g.add(new Spec(Item.TOILET, bx, bz, n, 0, 0));
                        if (c / 2 == locked) {
                            g.add(deco(bx + DX[n], bz + DZ[n], n, 0, Decor.STALL_DOOR_LOCKED));
                            g.add(deco(bx + DX[n], bz + DZ[n], n, 1, Decor.STALL_DOOR_LOCKED_TOP));
                        }
                    }
                }
                if (place(null, g.toArray(new Spec[0]))) {
                    stalls = true;
                    break;
                }
            }
        }
        if (!stalls) {
            onWall(Decor.STALL_DOOR_LOCKED, 0);
        }
        // wash basins with mirrors (the tap drips)
        wallRow(3, 1, (t, j) -> new Spec[]{new Spec(Item.BASIN, t[0], t[1], t[2], 0, 0), deco(t[0], t[1], t[2], 1, Decor.MIRROR)});
        onWall(rng.chance(0.6) ? Decor.HAND_DRYER : Decor.TOWEL_DISPENSER, 1);
        if (rng.chance(0.3)) {
            onWall(Decor.TOWEL_DISPENSER, 1);
        }
    }

    private void trainingRoom() {
        boolean alongX = gw >= gd;
        int n = -1;
        int sx = 0;
        int sz = 0;
        int rows = Math.min(2, r.clear - 2);
        if (rows >= 1) {
            for (int[] w : wallTiles()) {
                boolean shortWall = alongX ? (w[2] == 0 || w[2] == 2) : (w[2] == 1 || w[2] == 3);
                if (!shortWall) {
                    continue;
                }
                int right = (w[2] + 1) % 4;
                List<Spec> g = new ArrayList<>();
                for (int c = -1; c <= 1; c++) {
                    for (int dy = 1; dy <= rows; dy++) {
                        g.add(deco(w[0] + DX[right] * c, w[1] + DZ[right] * c, w[2], dy, dy == rows ? Decor.SCREEN_TOP : Decor.SCREEN));
                    }
                }
                if (place(null, g.toArray(new Spec[0]))) {
                    n = w[2];
                    sx = w[0];
                    sz = w[1];
                    break;
                }
            }
        }
        if (n < 0) {
            meetingRoom();
            return;
        }
        int right = (n + 1) % 4;
        int back = opposite(n);
        // lectern beside the screen, the reading side towards the screen
        for (int c : new int[]{2, -2, 3, -3}) {
            if (place(null, new Spec(Item.LECTERN, sx + DX[right] * c + DX[n], sz + DZ[right] * c + DZ[n], back, 0, 0))) {
                break;
            }
        }
        for (int c : new int[]{-3, 3, -4, 4}) {
            if (place(null, deco(sx + DX[right] * c, sz + DZ[right] * c, n, 0, Decor.FLIPCHART))) {
                break;
            }
        }
        // perfectly aligned rows, all facing the screen ... almost all
        int span = Math.max(gw, gd);
        List<int[]> seats = new ArrayList<>();
        for (int d = 3; d < span; d += 2) {
            for (int c = -span; c <= span; c++) {
                if (c == 2 || c == -3) {
                    continue; // two aisles
                }
                int bx = sx + DX[n] * d + DX[right] * c;
                int bz = sz + DZ[n] * d + DZ[right] * c;
                if (in(bx, bz)) {
                    seats.add(new int[]{bx, bz});
                }
            }
        }
        int odd = rng.chance(0.4) && !seats.isEmpty() ? rng.nextInt(seats.size()) : -1;
        for (int i = 0; i < seats.size(); i++) {
            int[] t = seats.get(i);
            place(null, deco(t[0], t[1], i == odd ? n : back, 0, Decor.STACKING_CHAIR));
        }
        // projector above the rows, lens towards the screen
        int px = sx + DX[n] * 4;
        int pz = sz + DZ[n] * 4;
        if (r.clear >= 4 && lightAllowed(px, pz)) {
            out.add(new Placement(Item.DECOR, wx(px), topY, wz(pz), back, Decor.PROJECTOR, null));
        }
        for (int[] w : wallTiles()) {
            if (w[2] == back && place(null, deco(w[0], w[1], w[2], wallY(2), Decor.WALL_CLOCK))) {
                break;
            }
        }
    }

    private void executiveOffice() {
        String lootId = loot(RoomType.EXECUTIVE_OFFICE);
        int[] c = centerTile();
        if (c != null) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    floorCover(c[0] + dx, c[1] + dz, 1);
                }
            }
        }
        // desk with the chair between desk and wall; sometimes the chair faces the wall
        for (int[] w : wallTiles()) {
            int n = w[2];
            int right = (n + 1) % 4;
            int dx = w[0] + DX[n];
            int dz = w[1] + DZ[n];
            Spec[] g = {
                    deco(w[0], w[1], rng.chance(0.3) ? opposite(n) : n, 0, Decor.EXECUTIVE_CHAIR),
                    new Spec(Item.EXEC_DESK, dx - DX[right], dz - DZ[right], n, 0, 0),
                    new Spec(Item.EXEC_DESK, dx, dz, n, 0, 0),
                    new Spec(Item.EXEC_DESK, dx + DX[right], dz + DZ[right], n, 0, 0),
                    deco(dx + DX[right], dz + DZ[right], n, 1, Decor.DESK_LAMP)};
            if (place(lootId, g)) {
                for (int s2 = -1; s2 <= 1; s2 += 2) {
                    place(null, new Spec(Item.SIDE_CHAIR, dx + DX[n] * 2 + DX[right] * s2, dz + DZ[n] * 2 + DZ[right] * s2, opposite(n), 0, 1));
                }
                break;
            }
        }
        int shelf = rng.nextInt(3);
        wallRow(3, 2, (t, j) -> new Spec[]{new Spec(Item.BOOKSHELF, t[0], t[1], t[2], 0, j == shelf ? 0 : 1)});
        currentLoot = lootId;
        wallRow(2, 1, (t, j) -> new Spec[]{new Spec(Item.CABINET, t[0], t[1], t[2], 0, 0)});
        int colour = rng.nextInt(2);
        wallRow(3, 2, (t, j) -> new Spec[]{new Spec(Item.SOFA, t[0], t[1], t[2], 0, colour)});
        currentLoot = null;
        onWall(Decor.PLANT, 0);
        onWall(Decor.WALL_CLOCK, wallY(2));
    }

    private void reception() {
        for (int bz = 0; bz < gd; bz++) {
            for (int bx = 0; bx < gw; bx++) {
                if (in(bx, bz) && keep[idx(bx, bz)]) {
                    floorCover(bx, bz, 2);
                }
            }
        }
        // counter one tile from the wall, nobody behind it; NOW SERVING above
        boolean counter = false;
        for (int len = 5; len >= 3 && !counter; len--) {
            for (int[] w : wallTiles()) {
                int n = w[2];
                int right = (n + 1) % 4;
                int half = len / 2;
                List<Spec> g = new ArrayList<>();
                for (int j = -half; j < len - half; j++) {
                    g.add(deco(w[0] + DX[n] + DX[right] * j, w[1] + DZ[n] + DZ[right] * j, n, 0, Decor.RECEPTION_DESK));
                }
                g.add(deco(w[0] + DX[n], w[1] + DZ[n], n, 1, Decor.SERVICE_BELL));
                g.add(deco(w[0], w[1], n, 0, Decor.STACKING_CHAIR));
                if (r.clear >= 4) {
                    g.add(deco(w[0] + DX[right], w[1] + DZ[right], n, wallY(2), Decor.QUEUE_DISPLAY));
                }
                if (place(null, g.toArray(new Spec[0]))) {
                    counter = true;
                    break;
                }
            }
        }
        // rows of linked waiting chairs against the walls
        for (int i = 0; i < 2; i++) {
            wallRow(4, 2, (t, j) -> new Spec[]{deco(t[0], t[1], t[2], 0, Decor.WAITING_CHAIR)});
        }
        int[] c = centerTile();
        if (c != null) {
            place(null, new Spec(Item.TABLE, c[0], c[1], 0, 0, 0), deco(c[0], c[1], rng.nextInt(4), 1, Decor.MAGAZINES));
        }
        int colour = 2 + rng.nextInt(2);
        wallRow(2, 2, (t, j) -> new Spec[]{new Spec(Item.SOFA, t[0], t[1], t[2], 0, colour)});
        onWall(Decor.PLANT, 0);
        onWall(Decor.PLANT, 0);
        onWall(Decor.TICKET_DISPENSER, 1);
        onWall(Decor.WALL_CLOCK, wallY(2));
    }

    /** 0.17.0: one HAZARD ZONE monitor against a free wall, sometimes broken. */
    private void hazardMonitor() {
        List<int[]> walls = wallTiles();
        int start = walls.isEmpty() ? 0 : rng.nextInt(walls.size());
        int variant = rng.nextInt(6) == 0 ? 1 : 0;
        for (int i = 0; i < walls.size(); i++) {
            int[] w = walls.get((start + i) % walls.size());
            if (place(Item.MONITOR, w[0], w[1], w[2], null)) {
                if (variant == 1 && !out.isEmpty()) {
                    Placement last = out.get(out.size() - 1);
                    out.set(out.size() - 1, new Placement(last.item(), last.x(), last.y(), last.z(), last.facing(), 1, null));
                }
                return;
            }
        }
    }

    /**
     * Spezialräume (Phase 4). Hier nur Licht und die OfficeCraft-/Refurbished-Möbel, die wie überall
     * in Spielernähe gesetzt werden. Wasser, Pflanzen, Sculk, Betten usw. setzt
     * {@code SpecialDecorator} bei der Generierung.
     */
    private Result specialRoom(de.officerooms.special.SpecialType special, boolean dark) {
        RoomType type = RoomType.of(special);
        String lighting = switch (special) {
            case VILLAGER_OUTPOST, OASIS, CANTEEN, DEPARTURE_GATE -> "normal";
            case COLD_STORAGE, MAIL_ROOM, RAIN_ROOM -> "dim";
            case LAST_MEETING -> "partial";
            case WATER_OFFICE -> "dim";
            case ABANDONED_CAMP -> "partial";
            default -> "none"; // Lush: Licht nur von Leuchtbeeren; Sculk: völlig dunkel
        };
        if (dark) {
            lighting = "none";
        }
        if (!lightsOnly) {
            lootOverride = type;
            switch (special) {
                case WATER_OFFICE -> {
                    if (r.cells >= 16) {
                        openOffice();
                    } else {
                        standardOffice();
                    }
                }
                case SCULK_OFFICE -> standardOffice();
                case LAST_MEETING -> meetingRoom();                // 0.29.1: the same furniture as the meeting rooms
                default -> {
                }
            }
        }
        if (!lighting.equals("none")) {
            roomLights(lighting);
        }
        return new Result(type, lighting, List.copyOf(out), view());
    }

    private static String pick(Rng rng, List<GenSettings.Weighted> list, String fallback) {
        int total = 0;
        for (GenSettings.Weighted w : list) {
            total += Math.max(0, w.weight());
        }
        if (total <= 0) {
            return fallback;
        }
        int x = rng.nextInt(total);
        for (GenSettings.Weighted w : list) {
            x -= Math.max(0, w.weight());
            if (x < 0) {
                return w.type();
            }
        }
        return fallback;
    }

    // -------------------------------------------------------------------------------------------
    // Raster
    // -------------------------------------------------------------------------------------------

    private void buildGrid() {
        int ru = l.ru;
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        List<int[]> cells = new ArrayList<>();
        int[] bb = l.bounds(r, s);
        int bx0 = bb[0] == Integer.MAX_VALUE ? 0 : bb[0];
        int bz0 = bb[1] == Integer.MAX_VALUE ? 0 : bb[1];
        int bx1 = bb[2] == Integer.MIN_VALUE ? ru - 1 : bb[2];
        int bz1 = bb[3] == Integer.MIN_VALUE ? ru - 1 : bb[3];
        for (int z = bz0; z <= bz1; z++) {
            for (int x = bx0; x <= bx1; x++) {
                if (l.room(s, x, z) == r) {
                    cells.add(new int[]{x, z});
                    minX = Math.min(minX, x);
                    minZ = Math.min(minZ, z);
                    maxX = Math.max(maxX, x);
                    maxZ = Math.max(maxZ, z);
                }
            }
        }
        gx0 = minX * unit;
        gz0 = minZ * unit;
        gw = (maxX - minX + 1) * unit;
        gd = (maxZ - minZ + 1) * unit;
        interior = new boolean[gw * gd];
        occupied = new boolean[gw * gd];
        keep = new boolean[gw * gd];
        noFurniture = new boolean[gw * gd];
        for (int bz = 0; bz < gd; bz++) {
            for (int bx = 0; bx < gw; bx++) {
                interior[bz * gw + bx] = isInterior(gx0 + bx, gz0 + bz);
            }
        }
        floorY = p.floorY(s) + 1;
        topY = p.yOf(s, r.clear);
        for (int[] c : cells) {
            markOpenings(c[0], c[1]);
        }
    }

    private Room cellRoom(int ux, int uz) {
        if (ux < 0 || uz < 0 || ux >= l.ru || uz >= l.ru) {
            return null;
        }
        return l.room(s, ux, uz);
    }

    /** Innenraum dieses Raums auf Laufebene (gleiche Regel wie der Rasterer). */
    private boolean isInterior(int lx, int lz) {
        int ux = lx / unit;
        int uz = lz / unit;
        boolean lineX = lx % unit == 0;
        boolean lineZ = lz % unit == 0;
        if (cellRoom(ux, uz) != r) {
            return false;
        }
        if (extras != null && extras.reserved(lx, lz, s)) {
            return false; // 0.9.0: elevator shaft or strongroom
        }
        if (lineX && (ux == 0 || cellRoom(ux - 1, uz) != r)) {
            return false;
        }
        if (lineZ && (uz == 0 || cellRoom(ux, uz - 1) != r)) {
            return false;
        }
        return !(lineX && lineZ) || cellRoom(ux - 1, uz - 1) == r;
    }

    private int edgeCode(int ux, int uz, int side) {
        // side: 0 = Ost, 1 = Süd, 2 = West, 3 = Nord
        switch (side) {
            case 2:
                return plan.edge(s, ux, uz, 0);
            case 3:
                return plan.edge(s, ux, uz, 1);
            case 0:
                return ux + 1 < l.ru ? plan.edge(s, ux + 1, uz, 0) : p.plan(rx + 1, rz).edge(s, 0, uz, 0);
            default:
                return uz + 1 < l.ru ? plan.edge(s, ux, uz + 1, 1) : p.plan(rx, rz + 1).edge(s, ux, 0, 1);
        }
    }

    private void markOpenings(int ux, int uz) {
        for (int side = 0; side < 4; side++) {
            int nx = ux + DX[side];
            int nz = uz + DZ[side];
            if (cellRoom(nx, nz) == r) {
                continue;
            }
            int code = edgeCode(ux, uz, side);
            int t = Openings.type(code);
            if (t == Openings.NONE) {
                continue;
            }
            openings.add(new int[]{ux, uz, side, code});
            for (int pos = 1; pos < unit; pos++) {
                boolean covered = t == Openings.OPEN || Openings.covers(code, pos);
                if (!covered) {
                    continue;
                }
                for (int depth = 1; depth <= (t == Openings.WINDOW ? 1 : 2); depth++) {
                    int lx;
                    int lz;
                    switch (side) {
                        case 2 -> {
                            lx = ux * unit + depth;
                            lz = uz * unit + pos;
                        }
                        case 0 -> {
                            lx = (ux + 1) * unit - depth;
                            lz = uz * unit + pos;
                        }
                        case 3 -> {
                            lx = ux * unit + pos;
                            lz = uz * unit + depth;
                        }
                        default -> {
                            lx = ux * unit + pos;
                            lz = (uz + 1) * unit - depth;
                        }
                    }
                    int i = idx(lx - gx0, lz - gz0);
                    if (i >= 0) {
                        if (t == Openings.WINDOW) {
                            noFurniture[i] = true;
                        } else {
                            keep[i] = true;
                        }
                    }
                }
            }
        }
    }

    private int idx(int bx, int bz) {
        if (bx < 0 || bz < 0 || bx >= gw || bz >= gd) {
            return -1;
        }
        return bz * gw + bx;
    }

    private boolean in(int bx, int bz) {
        int i = idx(bx, bz);
        return i >= 0 && interior[i];
    }

    private int wx(int bx) {
        return baseX + gx0 + bx;
    }

    private int wz(int bz) {
        return baseZ + gz0 + bz;
    }

    // -------------------------------------------------------------------------------------------
    // Platzieren
    // -------------------------------------------------------------------------------------------

    /** Ein geplanter Gegenstand in Rasterkoordinaten (für Gruppen). */
    private record Spec(Item item, int bx, int bz, int facing, int dy, int variant) {
    }

    /** Belegte Felder (dx, dz) relativ zum Hauptblock. */
    public static List<int[]> footprint(Item it, int facing) {
        List<int[]> c = new ArrayList<>();
        int right = (facing + 1) % 4;
        int hw = (it.width - 1) / 2;
        int hd = (it.depth - 1) / 2;
        for (int w = -hw; w <= hw; w++) {
            for (int d = -hd; d <= hd; d++) {
                c.add(new int[]{DX[right] * w + DX[facing] * d, DZ[right] * w + DZ[facing] * d});
            }
        }
        return c;
    }

    private boolean fits(Spec sp, List<int[]> tiles) {
        if (sp.dy() + sp.item().height >= r.clear) {
            return false;
        }
        int chunkX = wx(sp.bx()) >> 4;
        int chunkZ = wz(sp.bz()) >> 4;
        for (int[] c : footprint(sp.item(), sp.facing())) {
            int bx = sp.bx() + c[0];
            int bz = sp.bz() + c[1];
            int i = idx(bx, bz);
            if (i < 0 || !interior[i] || keep[i] || noFurniture[i]) {
                return false;
            }
            if (sp.dy() == 0 && occupied[i]) {
                return false;
            }
            if ((wx(bx) >> 4) != chunkX || (wz(bz) >> 4) != chunkZ) {
                return false;
            }
            tiles.add(new int[]{i, sp.dy()});
        }
        return true;
    }

    /** Setzt alle Gegenstände der Gruppe oder keinen. */
    private boolean place(String loot, Spec... specs) {
        List<int[]> tiles = new ArrayList<>();
        for (Spec sp : specs) {
            if (!fits(sp, tiles)) {
                return false;
            }
        }
        List<Integer> marked = new ArrayList<>();
        for (int[] t : tiles) {
            if (t[1] == 0) {
                if (occupied[t[0]]) {
                    for (int m : marked) {
                        occupied[m] = false;
                    }
                    return false;
                }
                occupied[t[0]] = true;
                marked.add(t[0]);
            }
        }
        if (!pathOk()) {
            for (int m : marked) {
                occupied[m] = false;
            }
            return false;
        }
        for (Spec sp : specs) {
            out.add(new Placement(sp.item(), wx(sp.bx()), floorY + sp.dy(), wz(sp.bz()), sp.facing(), sp.variant(),
                    sp.item().container ? loot : null));
        }
        return true;
    }

    private boolean place(Item it, int bx, int bz, int facing, String loot) {
        return place(loot, new Spec(it, bx, bz, facing, 0, rng.nextInt(4)));
    }

    /** Alle Türfelder müssen über freie Felder miteinander verbunden bleiben. */
    private boolean pathOk() {
        int start = -1;
        int need = 0;
        for (int i = 0; i < keep.length; i++) {
            if (keep[i] && interior[i]) {
                need++;
                if (start < 0) {
                    start = i;
                }
            }
        }
        if (start < 0) {
            return true;
        }
        boolean[] seen = new boolean[interior.length];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(start);
        seen[start] = true;
        int found = 0;
        while (!q.isEmpty()) {
            int i = q.poll();
            if (keep[i]) {
                found++;
            }
            int bx = i % gw;
            int bz = i / gw;
            for (int d = 0; d < 4; d++) {
                int j = idx(bx + DX[d], bz + DZ[d]);
                if (j >= 0 && !seen[j] && interior[j] && !occupied[j]) {
                    seen[j] = true;
                    q.add(j);
                }
            }
        }
        return found == need;
    }

    /** Wandfelder mit Normale (Richtung in den Raum hinein). Ecken werden ausgelassen. */
    private List<int[]> wallTiles() {
        List<int[]> list = new ArrayList<>();
        for (int bz = 0; bz < gd; bz++) {
            for (int bx = 0; bx < gw; bx++) {
                if (!in(bx, bz)) {
                    continue;
                }
                int normal = -1;
                int walls = 0;
                for (int d = 0; d < 4; d++) {
                    if (!in(bx - DX[d], bz - DZ[d])) {
                        walls++;
                        normal = d;
                    }
                }
                if (walls == 1) {
                    list.add(new int[]{bx, bz, normal});
                }
            }
        }
        // deterministisch mischen
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int[] t = list.get(i);
            list.set(i, list.get(j));
            list.set(j, t);
        }
        return list;
    }

    private static int opposite(int d) {
        return (d + 2) % 4;
    }

    /** Spezialräume (Wasser, Sculk) verwenden für ihre Schubladen die eigene Loot-Tabelle. */
    private RoomType lootOverride;

    private String loot(RoomType t) {
        return lootOverride != null ? lootOverride.id : t.id;
    }

    // -------------------------------------------------------------------------------------------
    // Einrichtungstypen
    // -------------------------------------------------------------------------------------------

    /** Schreibtisch an der Wand, Stuhl davor, optional Schublade daneben. */
    private boolean deskAtWall(int[] w, RoomType type) {
        int bx = w[0];
        int bz = w[1];
        int n = w[2];
        int chairVariant = rng.nextInt(2);
        if (!place(null, new Spec(Item.DESK, bx, bz, n, 0, 0),
                new Spec(Item.CHAIR, bx + DX[n], bz + DZ[n], opposite(n), 0, chairVariant))) {
            return false;
        }
        int right = (n + 1) % 4;
        int side = rng.chance(0.5) ? 2 : -2;
        if (!place(Item.DRAWER, bx + DX[right] * side, bz + DZ[right] * side, n, loot(type))) {
            place(Item.DRAWER, bx - DX[right] * side, bz - DZ[right] * side, n, loot(type));
        }
        return true;
    }

    private void standardOffice() {
        List<int[]> walls = wallTiles();
        int want = rng.chance(0.4) ? 2 : 1;
        int placed = 0;
        int firstNormal = -1;
        // Zweiter Platz bevorzugt an der gegenüberliegenden Wand ("zwei gegenüberliegende Arbeitsplätze").
        for (int pass = 0; pass < 2 && placed < want; pass++) {
            for (int[] w : walls) {
                if (placed >= want) {
                    break;
                }
                if (placed == 1 && pass == 0 && w[2] != opposite(firstNormal)) {
                    continue;
                }
                if (deskAtWall(w, RoomType.STANDARD_OFFICE)) {
                    if (placed == 0) {
                        firstNormal = w[2];
                    }
                    placed++;
                }
            }
        }
        if (rng.chance(0.3)) {
            for (int[] w : walls) {
                if (place(Item.PRINTER, w[0], w[1], w[2], null)) {
                    break;
                }
            }
        }
    }

    /**
     * Performance (Phase 4): Ein Teil der Arbeitsplätze an den Reihenenden im Großraum bleibt leer. Jeder
     * OfficeCraft-Arbeitsplatz wird vom Client jedes Bild einzeln gezeichnet; Lücken sparen
     * Zeichenaufrufe und wirken zugleich verlassen.
     */
    private boolean skipWorkplace(boolean rowEnd) {
        // Nur an Reihenenden: keine auffälligen Lücken mitten in einer Reihe (Update „Liminal World“)
        return rowEnd && rng.chance(WORKPLACE_GAP);
    }

    static final double WORKPLACE_GAP = 0.35;

    private void openOffice() {
        int variant = rng.nextInt(3);
        int facingA = rng.chance(0.5) ? 1 : 3;
        switch (variant) {
            case 0 -> {
                // Cubicle-Raster mit 1 Block breiten Gängen
                int ox = 2 + rng.nextInt(2);
                int oz = 2 + rng.nextInt(2);
                int row = 0;
                for (int bz = oz; bz < gd - 1; bz += 4, row++) {
                    for (int bx = ox; bx < gw - 1; bx += 4) {
                        if (skipWorkplace(bx == ox || bx + 4 >= gw - 1)) {
                            continue;
                        }
                        place(Item.WORKSTATION, bx, bz, row % 2 == 0 ? facingA : opposite(facingA), null);
                    }
                }
            }
            case 1 -> {
                // lange Reihen von Schreibtischen, Stühle davor
                for (int bz = 2; bz < gd - 2; bz += 4) {
                    for (int bx = 2; bx < gw - 2; bx += 3) {
                        if (skipWorkplace(bx == 2 || bx + 3 >= gw - 2)) {
                            continue;
                        }
                        place(null, new Spec(Item.DESK, bx, bz, 1, 0, 0), new Spec(Item.CHAIR, bx, bz + 1, 3, 0, rng.nextInt(2)));
                    }
                }
            }
            default -> {
                // Gruppen: zwei Schreibtische Rücken an Rücken
                for (int bz = 3; bz < gd - 3; bz += 6) {
                    for (int bx = 2; bx < gw - 2; bx += 5) {
                        if (skipWorkplace(bx == 2 || bx + 5 >= gw - 2)) {
                            continue;
                        }
                        place(null,
                                new Spec(Item.DESK, bx, bz, 3, 0, 0), new Spec(Item.CHAIR, bx, bz - 1, 1, 0, rng.nextInt(2)),
                                new Spec(Item.DESK, bx, bz + 1, 1, 0, 0), new Spec(Item.CHAIR, bx, bz + 2, 3, 0, rng.nextInt(2)));
                    }
                }
            }
        }
        List<int[]> walls = wallTiles();
        int drawers = 0;
        boolean cooler = rng.chance(0.5);
        boolean printer = rng.chance(0.5);
        boolean vending = rng.chance(0.2);
        for (int[] w : walls) {
            if (cooler && place(Item.WATER_COOLER, w[0], w[1], w[2], null)) {
                cooler = false;
            } else if (printer && place(Item.PRINTER, w[0], w[1], w[2], null)) {
                printer = false;
            } else if (vending && place(Item.VENDING, w[0], w[1], w[2], null)) {
                vending = false;
            } else if (drawers < 6 && rng.chance(0.15) && place(Item.DRAWER, w[0], w[1], w[2], loot(RoomType.OPEN_OFFICE))) {
                drawers++;
            }
        }
    }

    private int[] centerTile() {
        int cx = gw / 2;
        int cz = gd / 2;
        if (in(cx, cz)) {
            return new int[]{cx, cz};
        }
        for (int rr = 1; rr < Math.max(gw, gd); rr++) {
            for (int dz = -rr; dz <= rr; dz++) {
                for (int dx = -rr; dx <= rr; dx++) {
                    if (in(cx + dx, cz + dz)) {
                        return new int[]{cx + dx, cz + dz};
                    }
                }
            }
        }
        return null;
    }

    private void meetingRoom() {
        int[] c = centerTile();
        if (c == null) {
            return;
        }
        boolean alongX = gw >= gd;
        int facing = alongX ? 1 : 0; // Breite quer zur Blickrichtung = Längsachse
        int right = (facing + 1) % 4;
        Item table = Item.CONFERENCE_LARGE;
        int[] seats = {-3, -1, 1, 3};
        if (!place(table, c[0], c[1], facing, null)) {
            table = Item.CONFERENCE_MEDIUM;
            seats = new int[]{-2, 0, 2};
            if (!place(table, c[0], c[1], facing, null)) {
                table = null;
            }
        }
        if (table != null) {
            for (int side = -1; side <= 1; side += 2) {
                for (int o : seats) {
                    int bx = c[0] + DX[right] * o + DX[facing] * 2 * side;
                    int bz = c[1] + DZ[right] * o + DZ[facing] * 2 * side;
                    int chairFacing = side > 0 ? opposite(facing) : facing;
                    place(null, new Spec(Item.CHAIR, bx, bz, chairFacing, 0, rng.nextInt(2)));
                }
            }
        } else {
            smallTable(c);
        }
        // Präsentationstafel an einer Stirnwand
        for (int[] w : wallTiles()) {
            boolean shortWall = alongX ? (w[2] == 0 || w[2] == 2) : (w[2] == 1 || w[2] == 3);
            if (shortWall && place(Item.BOARD, w[0], w[1], w[2], null)) {
                break;
            }
        }
    }

    /** Tisch (1–2 Felder) mit Stühlen ringsum. */
    private void smallTable(int[] c) {
        boolean two = rng.chance(0.5) && place(null, new Spec(Item.TABLE, c[0], c[1], 0, 0, 0), new Spec(Item.TABLE, c[0] + 1, c[1], 0, 0, 0));
        if (!two && !place(null, new Spec(Item.TABLE, c[0], c[1], 0, 0, 0))) {
            return;
        }
        int len = two ? 2 : 1;
        for (int i = 0; i < len; i++) {
            place(null, new Spec(Item.SIDE_CHAIR, c[0] + i, c[1] - 1, 1, 0, 0));
            place(null, new Spec(Item.SIDE_CHAIR, c[0] + i, c[1] + 1, 3, 0, 0));
        }
        place(null, new Spec(Item.SIDE_CHAIR, c[0] - 1, c[1], 0, 0, 0));
        place(null, new Spec(Item.SIDE_CHAIR, c[0] + len, c[1], 2, 0, 0));
    }

    private void breakRoom() {
        List<int[]> walls = wallTiles();
        String lootId = loot(RoomType.BREAK_ROOM);
        boolean kitchen = false;
        for (int[] w : walls) {
            int n = w[2];
            int right = (n + 1) % 4;
            // Küchenzeile: Kühlschrank, Arbeitsplatte (Mikrowelle), Spüle, Arbeitsplatte (Kaffeemaschine)
            Spec[] row = {
                    new Spec(Item.FRIDGE, w[0], w[1], n, 0, 0),
                    new Spec(Item.COUNTER, w[0] + DX[right], w[1] + DZ[right], n, 0, 0),
                    new Spec(Item.MICROWAVE, w[0] + DX[right], w[1] + DZ[right], n, 1, 0),
                    new Spec(Item.SINK, w[0] + 2 * DX[right], w[1] + 2 * DZ[right], n, 0, 0),
                    new Spec(Item.COUNTER, w[0] + 3 * DX[right], w[1] + 3 * DZ[right], n, 0, 0),
                    new Spec(Item.COFFEE, w[0] + 3 * DX[right], w[1] + 3 * DZ[right], n, 1, 0)};
            if (place(lootId, row)) {
                kitchen = true;
                break;
            }
        }
        if (!kitchen) {
            for (int[] w : walls) {
                if (place(lootId, new Spec(Item.COUNTER, w[0], w[1], w[2], 0, 0), new Spec(Item.COFFEE, w[0], w[1], w[2], 1, 0))) {
                    break;
                }
            }
        }
        int[] c = centerTile();
        if (c != null) {
            smallTable(c);
        }
        boolean cooler = rng.chance(0.7);
        boolean vending = rng.chance(0.3);
        for (int[] w : walls) {
            if (cooler && place(Item.WATER_COOLER, w[0], w[1], w[2], null)) {
                cooler = false;
            } else if (vending && place(Item.VENDING, w[0], w[1], w[2], null)) {
                vending = false;
            }
        }
    }

    private void serverRoom() {
        // Rack-Reihen mit Gängen; Reihen quer zur längeren Achse
        boolean alongX = gw >= gd;
        int rowStep = 3;
        int a0 = 2;
        int lenA = alongX ? gd : gw;
        int lenB = alongX ? gw : gd;
        for (int a = a0; a < lenA - 1; a += rowStep) {
            for (int b = 2; b < lenB - 2; b++) {
                int bx = alongX ? b : a;
                int bz = alongX ? a : b;
                place(Item.RACK, bx, bz, alongX ? 1 : 0, null);
            }
        }
    }

    private void archive() {
        String lootId = loot(RoomType.ARCHIVE);
        // Schubladenwände mit Lücken (ca. 35 % der Wandfelder), optional eine Mittelreihe.
        for (int[] w : wallTiles()) {
            if (rng.chance(0.35)) {
                place(Item.DRAWER_STACK, w[0], w[1], w[2], lootId);
            }
        }
        if (gd >= 7 && gw >= 7 && rng.chance(0.5)) {
            int bz = gd / 2;
            for (int bx = 2; bx < gw - 2; bx++) {
                place(Item.DRAWER_STACK, bx, bz, 3, lootId);
            }
        }
    }

    private void waterCooler() {
        for (int[] w : wallTiles()) {
            if (place(Item.WATER_COOLER, w[0], w[1], w[2], null)) {
                return;
            }
        }
    }

    /** Freies Türvorfeld (2. Reihe) als Standpunkt, Blick auf die Raummitte. */
    private int[] view() {
        int[] c = centerTile();
        if (c == null) {
            return null;
        }
        int best = -1;
        int bestD = -1;
        for (int i = 0; i < keep.length; i++) {
            if (keep[i] && interior[i] && !occupied[i]) {
                int bx = i % gw;
                int bz = i / gw;
                int d = Math.abs(bx - c[0]) + Math.abs(bz - c[1]);
                if (d > bestD) {
                    bestD = d;
                    best = i;
                }
            }
        }
        if (best < 0) {
            return null;
        }
        return new int[]{wx(best % gw), floorY, wz(best / gw), wx(c[0]), wz(c[1])};
    }

    // -------------------------------------------------------------------------------------------
    // Beleuchtung
    // -------------------------------------------------------------------------------------------

    /** Leuchten nie auf Tür- und Fenstervorfeldern (offene Durchgänge und 3 hohe Fenster reichen bis unter die Decke). */
    private boolean lightAllowed(int bx, int bz) {
        int i = idx(bx, bz);
        return i >= 0 && interior[i] && !keep[i] && !noFurniture[i];
    }

    private void light(Item it, int bx, int bz, int facing, boolean lit) {
        if (!lightAllowed(bx, bz)) {
            return;
        }
        out.add(new Placement(it, wx(bx), topY, wz(bz), facing, lit ? 1 : 0, null));
    }

    /**
     * Raumbeleuchtung: normal (Raster lightSpacing), dim (Raster dimLightSpacing), partial (Raster
     * lightSpacing, 40 % ausgefallen), dark (eine einzelne Leuchte, manchmal keine).
     */
    private void roomLights(String lighting) {
        if (r.clear < 3 || lighting.equals("none")) {
            return;
        }
        if (lighting.equals("dark")) {
            if (lightRng.chance(0.7)) {
                int[] c = randomInterior();
                if (c != null) {
                    light(Item.LIGHT, c[0], c[1], 0, true);
                }
            }
            return;
        }
        int spacing = lighting.equals("dim") ? f.dimLightSpacing() : f.lightSpacing();
        int ox = Math.min(gw - 1, spacing / 2);
        int oz = Math.min(gd - 1, spacing / 2);
        int count = 0;
        for (int bz = oz; bz < gd; bz += spacing) {
            for (int bx = ox; bx < gw; bx += spacing) {
                if (lightAllowed(bx, bz)) {
                    boolean lit = !lighting.equals("partial") || lightRng.chance(0.6);
                    light(Item.LIGHT, bx, bz, 0, lit);
                    count++;
                }
            }
        }
        if (count == 0) {
            int[] c = centerTile();
            if (c != null) {
                light(Item.LIGHT, c[0], c[1], 0, true);
                if (!lightAllowed(c[0], c[1])) {
                    int[] alt = randomInterior();
                    if (alt != null) {
                        light(Item.LIGHT, alt[0], alt[1], 0, true);
                    }
                }
            }
        }
    }

    private int[] randomInterior() {
        List<int[]> all = new ArrayList<>();
        for (int bz = 0; bz < gd; bz++) {
            for (int bx = 0; bx < gw; bx++) {
                if (lightAllowed(bx, bz)) {
                    all.add(new int[]{bx, bz});
                }
            }
        }
        return all.isEmpty() ? null : all.get(lightRng.nextInt(all.size()));
    }

    /** Korridore: Lichtleisten in Zellmitte, hell = jede Zelle, dunkel = jede dritte, teils ausgefallen. */
    private void corridorLights(String lighting) {
        boolean bright = lighting.equals("bright");
        int n = 0;
        int ux0 = gx0 / unit;
        int uz0 = gz0 / unit;
        for (int uz = uz0; uz < uz0 + gd / unit; uz++) {
            for (int ux = ux0; ux < ux0 + gw / unit; ux++) {
                if (l.room(s, ux, uz) != r) {
                    continue;
                }
                n++;
                if (bright ? (ux + uz) % 2 != 0 : (ux + 2 * uz) % 3 != 0) {
                    continue;
                }
                int bx = ux * unit + unit / 2 - gx0;
                int bz = uz * unit + unit / 2 - gz0;
                boolean alongX = cellRoom(ux - 1, uz) == r || cellRoom(ux + 1, uz) == r;
                boolean lit = bright ? lightRng.chance(0.95) : lightRng.chance(0.55);
                light(Item.LIGHT_BAR, bx, bz, alongX ? 3 : 0, lit);
            }
        }
    }

    /** Atrien: Deckenpanels oben und Leuchten unter den Galerien; nur ein Teil der Atrien ist beleuchtet. */
    private Result atrium() {
        Atrium a = r.atrium;
        boolean litAtrium = rng.chance(f.atriumLitChance());
        if (!litAtrium || de.officerooms.special.SpecialRooms.darkStorey(p, a.s0)) {
            return new Result(RoomType.ATRIUM, "dark", List.of());
        }
        int top = p.yOf(a.s0, a.clear);
        for (int cz = 0; cz < a.d; cz++) {
            for (int cx = 0; cx < a.w; cx++) {
                int ux = a.ux + cx;
                int uz = a.uz + cz;
                int lx = ux * unit + unit / 2;
                int lz = uz * unit + unit / 2;
                if ((cx % 2 == 0) && (cz % 2 == 0) && rng.chance(0.7)) {
                    out.add(new Placement(Item.LIGHT, baseX + lx, top, baseZ + lz, 0, 1, null));
                }
            }
        }
        for (int o = 1; o < a.storeys; o++) {
            if (a.walk[o] == null) {
                continue;
            }
            boolean galleryLit = rng.chance(0.7);
            int g = a.s0 + o;
            int y = p.slabY(g) - 1;
            for (int cz = 0; cz < a.d; cz++) {
                for (int cx = 0; cx < a.w; cx++) {
                    if (a.walk[o][cz * a.w + cx] == 0 || (cx + cz) % 2 != 0) {
                        continue;
                    }
                    int lx = (a.ux + cx) * unit + unit / 2;
                    int lz = (a.uz + cz) * unit + unit / 2;
                    out.add(new Placement(Item.LIGHT, baseX + lx, y, baseZ + lz, 0, galleryLit && rng.chance(0.8) ? 1 : 0, null));
                }
            }
        }
        return new Result(RoomType.ATRIUM, "lit", List.copyOf(out));
    }
}
