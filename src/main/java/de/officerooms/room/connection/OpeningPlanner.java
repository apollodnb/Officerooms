package de.officerooms.room.connection;

import de.officerooms.core.Hashing;
import de.officerooms.core.Hashing.Rng;
import de.officerooms.room.GenSettings;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.RegionPlan;
import de.officerooms.room.Room;
import de.officerooms.window.WindowPlanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Setzt geplante Verbindungen in konkrete Öffnungen um und plant die Türen an den Regionsgrenzen.
 *
 * <p>Türtypen (Gewichte konfigurierbar): normale Türöffnung (1 breit), Doppeltüröffnung (2 breit),
 * breite Türöffnung (3 breit), vollständig offener Durchgang (über 2–4 aufeinanderfolgende
 * Kanten, Zwischenpfosten entfallen). Position innerhalb der Wand: zufällige Kante unter allen
 * gemeinsamen Kanten (links, mittig, rechts) und zufälliger Versatz innerhalb der Kante.</p>
 */
public final class OpeningPlanner {

    private OpeningPlanner() {
    }

    public static RegionPlan build(GenSettings cfg, long seed, RegionLayout self, RegionLayout west, RegionLayout north) {
        int ru = self.ru;
        int unit = cfg.unit();
        byte[] edges = new byte[self.storeys * ru * ru * 2];
        Rng rng = new Rng(Hashing.hash(seed, self.rx, self.rz, 0xD0));
        int doors = 0;
        for (Connection c : self.connections) {
            Room a = self.rooms.get(self.nodeRoom[c.nodeA()]);
            Room b = self.rooms.get(self.nodeRoom[c.nodeB()]);
            if (c.stairs()) {
                edges[c.edges()[0]] = Openings.door(Math.min(3, unit - 1), 1);
                doors++;
                continue;
            }
            doors += assign(cfg, rng, edges, c.edges(), a, b, ru, unit, true);
        }

        int boundary = 0;
        if (west != null) {
            boundary += boundaryDoors(cfg, seed, self, west, edges, 0);
        }
        if (north != null) {
            boundary += boundaryDoors(cfg, seed, self, north, edges, 1);
        }

        int windows = WindowPlanner.plan(cfg, seed, self, edges);
        return new RegionPlan(self, edges, doors + boundary, windows, boundary);
    }

    /** Setzt die Öffnung(en) einer Verbindung. Liefert die Anzahl gesetzter Öffnungen. */
    static int assign(GenSettings cfg, Rng rng, byte[] edges, int[] keys, Room a, Room b, int ru, int unit,
                      boolean allowOpen) {
        GenSettings.Doors d = cfg.doors();
        int total = Math.max(1, d.normal() + d.dbl() + d.wide() + (allowOpen ? d.open() : 0));
        int roll = rng.nextInt(total);
        int type;
        if ((roll -= d.normal()) < 0) {
            type = 0;
        } else if ((roll -= d.dbl()) < 0) {
            type = 1;
        } else if ((roll -= d.wide()) < 0) {
            type = 2;
        } else {
            type = 3;
        }
        int pitchInterior = cfg.pitch() - 1;
        if (type == 3) {
            boolean heightOk = a.clear >= pitchInterior && b.clear >= pitchInterior;
            if (heightOk && keys.length >= 2 && open(rng, edges, keys, ru)) {
                return 1;
            }
            type = 2;
        }
        int width = Math.min(unit - 1, type + 1);
        int i = rng.nextInt(keys.length);
        edges[keys[i]] = Openings.door(width, 1 + rng.nextInt(unit - width));
        int n = 1;
        if (keys.length >= 3 && rng.chance(d.secondOpeningChance())) {
            int j = rng.nextInt(keys.length);
            if (Math.abs(j - i) >= 2 && edges[keys[j]] == 0) {
                int w2 = Math.min(unit - 1, rng.range(1, 2));
                edges[keys[j]] = Openings.door(w2, 1 + rng.nextInt(unit - w2));
                n++;
            }
        }
        return n;
    }

    /** Offener Durchgang über einen Lauf zusammenhängender, kollinearer Kanten. */
    private static boolean open(Rng rng, byte[] edges, int[] keys, int ru) {
        // Kanten nach (Geschoss, Achse, Linie, Position) sortieren und Läufe suchen.
        long[] sorted = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            int k = keys[i];
            int axis = k & 1;
            int cell = k >> 1;
            int s = cell / (ru * ru);
            int c = cell % (ru * ru);
            int ux = c % ru;
            int uz = c / ru;
            int line = axis == 0 ? ux : uz;
            int pos = axis == 0 ? uz : ux;
            sorted[i] = ((long) s << 32) | ((long) axis << 31) | ((long) line << 24) | ((long) pos << 16) | i;
        }
        Arrays.sort(sorted);
        List<int[]> runs = new ArrayList<>();
        int start = 0;
        for (int i = 1; i <= sorted.length; i++) {
            boolean cont = i < sorted.length
                    && (sorted[i] >>> 24) == (sorted[i - 1] >>> 24)
                    && ((sorted[i] >> 16) & 0xFF) == ((sorted[i - 1] >> 16) & 0xFF) + 1;
            if (!cont) {
                if (i - start >= 2) {
                    runs.add(new int[]{start, i - start});
                }
                start = i;
            }
        }
        if (runs.isEmpty()) {
            return false;
        }
        int[] run = runs.get(rng.nextInt(runs.size()));
        int len = Math.min(run[1], 2 + rng.nextInt(3));
        int off = rng.nextInt(run[1] - len + 1);
        for (int i = 0; i < len; i++) {
            int keyIndex = (int) (sorted[run[0] + off + i] & 0xFFFF);
            edges[keys[keyIndex]] = (byte) Openings.OPEN;
        }
        return true;
    }

    /**
     * Türen auf der eigenen West- (axis 0) oder Nordgrenze. Kandidaten sind nur erreichbare Räume
     * und Korridore, die auf beiden Seiten ihr Bodengeschoss auf dieser Höhe haben.
     */
    private static int boundaryDoors(GenSettings cfg, long seed, RegionLayout self, RegionLayout other, byte[] edges, int axis) {
        int ru = self.ru;
        int unit = cfg.unit();
        GenSettings.Doors d = cfg.doors();
        Rng rng = new Rng(Hashing.hash(seed, self.rx, self.rz, 0xB0 + axis));
        int count = 0;
        for (int s = 0; s < self.storeys; s++) {
            Map<Long, List<Integer>> pairs = new LinkedHashMap<>();
            Map<Long, Room[]> rooms = new LinkedHashMap<>();
            for (int i = 0; i < ru; i++) {
                Room mine = axis == 0 ? self.room(s, 0, i) : self.room(s, i, 0);
                Room theirs = axis == 0 ? other.room(s, ru - 1, i) : other.room(s, i, ru - 1);
                if (mine == null || theirs == null || !mine.isWalkableRoom() || !theirs.isWalkableRoom()
                        || mine.bottom != s || theirs.bottom != s) {
                    continue;
                }
                long pk = ((long) theirs.index << 32) | mine.index;
                pairs.computeIfAbsent(pk, k -> new ArrayList<>())
                        .add(axis == 0 ? self.edgeKey(s, 0, i, 0) : self.edgeKey(s, i, 0, 1));
                rooms.putIfAbsent(pk, new Room[]{theirs, mine});
            }
            if (pairs.isEmpty()) {
                continue;
            }
            List<long[]> ranked = new ArrayList<>();
            for (long pk : pairs.keySet()) {
                long h = Hashing.hash(seed, self.rx, self.rz, 0xB8 + axis, pk ^ ((long) s << 50));
                ranked.add(new long[]{h >>> 1, pk});
            }
            ranked.sort((x, y) -> Long.compare(x[0], y[0]));
            int min = Math.max(0, d.boundaryMinPerStorey());
            for (int r = 0; r < ranked.size(); r++) {
                double u = (ranked.get(r)[0]) / (double) Long.MAX_VALUE;
                if (r >= min && u >= d.boundaryChance()) {
                    continue;
                }
                long pk = ranked.get(r)[1];
                int[] keys = pairs.get(pk).stream().mapToInt(Integer::intValue).toArray();
                Room[] rr = rooms.get(pk);
                count += assign(cfg, rng, edges, keys, rr[0], rr[1], ru, unit, false);
            }
        }
        return count;
    }
}
