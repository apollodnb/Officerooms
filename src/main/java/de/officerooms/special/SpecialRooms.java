package de.officerooms.special;

import de.officerooms.core.Hashing;
import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;

/**
 * Entscheidet deterministisch, ob ein Büroraum ein Spezialraum ist. Reines Java, ohne Minecraft.
 *
 * <p>Die Wahl hängt nur von Seed, Region und Raumindex ab (eigener Hash-Strom 0x5EC), nicht von der
 * Möbelplanung. Der Rasterer, die Einrichtung, die Dekoration und die Prüfung kommen dadurch immer
 * zum selben Ergebnis.</p>
 */
public final class SpecialRooms {

    private SpecialRooms() {
    }

    public static SpecialType of(OfficePlanner p, int rx, int rz, Room r) {
        if (p.pools.overlaps(rx, rz, r)) {
            return SpecialType.POOLROOMS;
        }
        if (p.hood.overlaps(rx, rz, r)) {
            return SpecialType.NEIGHBOURHOOD;
        }
        if (p.maze.overlaps(rx, rz, r)) {
            return SpecialType.MAZE;
        }
        if (p.hotel.overlaps(rx, rz, r)) {
            return SpecialType.HOTEL;
        }
        if (p.industrial.overlaps(rx, rz, r)) {
            return SpecialType.INDUSTRIAL;
        }
        if (p.fun.overlaps(rx, rz, r)) {
            return SpecialType.LEVEL_FUN;
        }
        if (p.cinema.overlaps(rx, rz, r)) {
            return SpecialType.CINEMA;
        }
        if (r == null || r.kind != Room.Kind.ROOM || r.sealed || r.cells == 0) {
            return SpecialType.NONE;
        }
        GenSettings.Special c = p.cfg.special();
        double u = Hashing.unit(Hashing.hash(p.seed, rx, rz, 0x5EC, r.index));
        // Reihenfolge: seltenster zuerst; die Chancen addieren sich nur bei Räumen, die mehrfach geeignet sind.
        double acc = 0;
        if (r.cells >= c.sculkMinCells()) {
            acc += c.sculkChance();
            if (u < acc) {
                return SpecialType.SCULK_OFFICE;
            }
        }
        if (r.index == oasisIndex(p, rx, rz)) {
            return SpecialType.OASIS;
        }
        if (r.cells >= c.lushMinCells() && r.clear >= c.lushMinClear()) {
            acc += c.lushChance();
            if (u < acc) {
                return SpecialType.LUSH_CAVE;
            }
        }
        if (r.cells >= c.outpostMinCells() && r.storeys == 1) {
            acc += c.outpostChance();
            if (u < acc) {
                return SpecialType.VILLAGER_OUTPOST;
            }
        }
        if (r.cells >= 12 && r.storeys == 1) {
            acc += c.campChance();
            if (u < acc) {
                return SpecialType.ABANDONED_CAMP;
            }
        }
        if (r.cells >= c.waterMinCells() && r.cells <= c.waterMaxCells() && r.storeys == 1) {
            acc += c.waterChance();
            if (u < acc) {
                return SpecialType.WATER_OFFICE;
            }
        }
        // 0.29.0: curiosities, new worlds only (content_version 1), single storey, ordinary entrances
        if (p.cfg.modern() && r.storeys == 1 && r.cells >= 6 && !darkStorey(p, r.bottom)) {
            SpecialType[] t = {SpecialType.ARCADE, SpecialType.RAIN_ROOM, SpecialType.COLD_STORAGE, SpecialType.MAIL_ROOM,
                    SpecialType.CANTEEN, SpecialType.LAST_MEETING};
            int[] minCells = {12, 8, 6, 8, 16, 10};
            for (int k = 0; k < t.length; k++) {
                if (r.cells >= minCells[k]) {
                    acc += CURIO[k];
                    if (u < acc) {
                        return t[k];
                    }
                }
            }
        }
        if (r.bottom == 0 && r.storeys == 1 && r.cells >= 4) {
            acc += c.exitChance();
            if (u < acc) {
                return SpecialType.EXIT_ROOM;
            }
        }
        return SpecialType.NONE;
    }

    /**
     * 0.29.0: chance per suitable room for arcade, rain room, cold storage, mail room, canteen and last meeting (in
     * this order). Defaults here; the common config ([curiosities]) overrides them on load. Changing them in a world
     * that is already explored can leave half rooms at the edge of explored areas.
     */
    public static final double[] CURIO = {0.003, 0.006, 0.008, 0.008, 0.008, 0.01};

    private static final java.util.Map<Long, Integer> OASIS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Oase: höchstens eine pro Region (160 × 160 Blöcke, dadurch immer großer Abstand), mit Chance
     * {@code oasisRegionChance}. Gewählt wird ein hoher Raum (lichte Höhe ≥ 9) mit 30–64 Zellen, dessen
     * Grundriss höchstens 8 × 8 Zellen (32 × 32 Blöcke) groß ist. -1 = keine Oase in dieser Region.
     */
    static int oasisIndex(OfficePlanner p, int rx, int rz) {
        long key = p.seed * 31 + (((long) rx) << 32 ^ (rz & 0xFFFFFFFFL));
        if (OASIS.size() > 4096) {
            OASIS.clear();
        }
        return OASIS.computeIfAbsent(key, k -> {
            if (Hashing.unit(Hashing.hash(p.seed, rx, rz, 0x0A515)) >= p.cfg.special().oasisRegionChance()) {
                return -1;
            }
            RegionLayout l = p.layout(rx, rz);
            java.util.List<Room> cand = new java.util.ArrayList<>();
            for (Room r : l.rooms) {
                if (r.kind != Room.Kind.ROOM || r.sealed || r.clear < 9 || r.cells < 30 || r.cells > 64) {
                    continue;
                }
                int[] b = l.bounds(r, r.bottom);
                if (b[2] - b[0] + 1 > 8 || b[3] - b[1] + 1 > 8) {
                    continue;
                }
                cand.add(r);
            }
            if (cand.isEmpty()) {
                return -1;
            }
            return cand.get((int) (Hashing.unit(Hashing.hash(p.seed, rx, rz, 0x0A516)) * cand.size())).index;
        });
    }

    /** Spezialtyp des Raums an einer Rasterzelle (oder NONE). */
    public static SpecialType at(OfficePlanner p, int rx, int rz, int s, int ux, int uz) {
        RegionLayout l = p.layout(rx, rz);
        if (s < 0 || s >= l.storeys || ux < 0 || uz < 0 || ux >= l.ru || uz >= l.ru) {
            return SpecialType.NONE;
        }
        return of(p, rx, rz, l.room(s, ux, uz));
    }

    /** True, wenn das Geschoss zu den vollständig dunklen untersten Ebenen gehört. */
    public static boolean darkStorey(OfficePlanner p, int s) {
        return s < p.cfg.special().darkBottomStoreys();
    }
}
