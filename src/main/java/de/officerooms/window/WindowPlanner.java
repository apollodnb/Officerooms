package de.officerooms.window;

import de.officerooms.atrium.Atrium;
import de.officerooms.core.Hashing;
import de.officerooms.core.Hashing.Rng;
import de.officerooms.room.GenSettings;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;
import de.officerooms.room.connection.Openings;

import java.util.ArrayList;
import java.util.List;

/**
 * Fensterplanung. <b>Fenster entstehen ausschließlich auf Zellkanten, deren eine Seite ein
 * Atrium und deren andere Seite ein erreichbarer Raum oder Korridor ist</b>, sowie (Phase 2,
 * selten) zwischen zwei benachbarten, erreichbaren Büroräumen.
 *
 * <p>Das wird bei der Planung entschieden, nicht nachträglich korrigiert: Kandidaten sind nur
 * Kanten zwischen einer Atriumzelle und einer Raumzelle im selben Geschoss. Weil Atrien nie an
 * den Regionsrand und nie an die Welt-Ober-/Unterkante reichen und ihre gesamte Höhe vorab
 * geplant ist, liegt hinter jedem Fenster garantiert der Atrium-Innenraum.</p>
 *
 * <p>Zusätzliche Prüfungen pro Fenster: das Fenster liegt vollständig unter der (ggf. abgehängten)
 * Decke des Raums und des Atriums, und die Kante trägt keine Tür. Größen: 2×2, 2×3, 3×2, 3×3
 * (Breite × Höhe), Brüstung 1 Block über dem Boden.</p>
 */
public final class WindowPlanner {

    private static final int[][] SIZES = {{2, 2}, {2, 3}, {3, 2}, {3, 3}};

    private WindowPlanner() {
    }

    public static int plan(GenSettings cfg, long seed, RegionLayout layout, byte[] edges) {
        int ru = layout.ru;
        int unit = cfg.unit();
        int pitch = cfg.pitch();
        List<List<int[]>> perRoom = new ArrayList<>();
        for (int i = 0; i < layout.rooms.size(); i++) {
            perRoom.add(null);
        }
        for (int s = 0; s < layout.storeys; s++) {
            for (int z = 0; z < ru; z++) {
                for (int x = 0; x < ru; x++) {
                    if (x > 0) {
                        candidate(layout, perRoom, edges, s, x - 1, z, x, z, 0);
                    }
                    if (z > 0) {
                        candidate(layout, perRoom, edges, s, x, z - 1, x, z, 1);
                    }
                }
            }
        }
        Rng rng = new Rng(Hashing.hash(seed, layout.rx, layout.rz, 0xF0));
        GenSettings.Windows w = cfg.windows();
        int count = 0;
        for (int i = 0; i < perRoom.size(); i++) {
            List<int[]> list = perRoom.get(i);
            if (list == null || !rng.chance(w.roomChance())) {
                continue;
            }
            Room room = layout.rooms.get(i);
            int n = 1 + rng.nextInt(Math.max(1, w.maxPerRoom()));
            for (int t = 0; t < n && !list.isEmpty(); t++) {
                int[] c = list.remove(rng.nextInt(list.size()));
                int key = c[0];
                Atrium a = layout.rooms.get(c[1]).atrium;
                int s = c[2];
                int[] size = SIZES[rng.nextInt(SIZES.length)];
                int ww = Math.min(size[0], unit - 1);
                int wh = size[1];
                // Fensterzeilen ly = 2 .. 1 + wh (Brüstung 1 Block).
                int top = 1 + wh;
                if (top > pitch - 1 || top > room.clear) {
                    wh = 2;
                    top = 3;
                }
                int atriumTop = (s - a.s0) * pitch + top;
                if (top > pitch - 1 || top > room.clear || atriumTop > a.clear) {
                    continue;
                }
                edges[key] = Openings.window(ww, wh, 1 + rng.nextInt(unit - ww));
                count++;
            }
        }
        count += planOfficeWindows(cfg, seed, layout, edges);
        return count;
    }

    /**
     * Phase 2: seltene Fenster zwischen zwei unmittelbar benachbarten Büroräumen (beide Raumart ROOM,
     * beide erreichbar, gleiches Bodengeschoss). Pro Raumpaar höchstens ein Fenster, nur auf Kanten
     * ohne Tür. Hinter dem Fenster liegt dadurch immer der tatsächliche Nachbarraum.
     */
    private static int planOfficeWindows(GenSettings cfg, long seed, RegionLayout layout, byte[] edges) {
        double chance = cfg.windows().officePairChance();
        if (chance <= 0) {
            return 0;
        }
        int ru = layout.ru;
        int unit = cfg.unit();
        int pitch = cfg.pitch();
        java.util.Map<Long, List<Integer>> pairs = new java.util.LinkedHashMap<>();
        for (int s = 0; s < layout.storeys; s++) {
            for (int z = 0; z < ru; z++) {
                for (int x = 0; x < ru; x++) {
                    for (int axis = 0; axis < 2; axis++) {
                        if ((axis == 0 && x == 0) || (axis == 1 && z == 0)) {
                            continue;
                        }
                        Room a = axis == 0 ? layout.room(s, x - 1, z) : layout.room(s, x, z - 1);
                        Room b = layout.room(s, x, z);
                        if (a == null || b == null || a == b || a.kind != Room.Kind.ROOM || b.kind != Room.Kind.ROOM
                                || a.bottom != s || b.bottom != s) {
                            continue;
                        }
                        int key = layout.edgeKey(s, x, z, axis);
                        if (edges[key] != 0) {
                            continue;
                        }
                        long pk = ((long) Math.min(a.index, b.index) << 32) | Math.max(a.index, b.index);
                        pairs.computeIfAbsent(pk, k -> new ArrayList<>()).add(key);
                    }
                }
            }
        }
        int count = 0;
        for (java.util.Map.Entry<Long, List<Integer>> e : pairs.entrySet()) {
            long h = Hashing.hash(seed, layout.rx, layout.rz, 0xF1, e.getKey());
            if (Hashing.unit(h) >= chance) {
                continue;
            }
            Rng rng = new Rng(h);
            List<Integer> keys = e.getValue();
            int key = keys.get(rng.nextInt(keys.size()));
            Room a = layout.rooms.get((int) (e.getKey() >>> 32));
            Room b = layout.rooms.get((int) (e.getKey() & 0xFFFFFFFFL));
            int[] size = SIZES[rng.nextInt(SIZES.length)];
            int ww = Math.min(size[0], unit - 1);
            int wh = size[1];
            int top = 1 + wh;
            if (top > pitch - 1 || top > a.clear || top > b.clear) {
                wh = 2;
                top = 3;
                if (top > a.clear || top > b.clear) {
                    continue;
                }
            }
            edges[key] = Openings.window(ww, wh, 1 + rng.nextInt(unit - ww));
            count++;
        }
        return count;
    }

    private static void candidate(RegionLayout layout, List<List<int[]>> perRoom, byte[] edges, int s,
                                  int ax, int az, int bx, int bz, int axis) {
        Room a = layout.room(s, ax, az);
        Room b = layout.room(s, bx, bz);
        if (a == null || b == null || a == b) {
            return;
        }
        Room atrium;
        Room room;
        if (a.kind == Room.Kind.ATRIUM && b.isWalkableRoom()) {
            atrium = a;
            room = b;
        } else if (b.kind == Room.Kind.ATRIUM && a.isWalkableRoom()) {
            atrium = b;
            room = a;
        } else {
            return;
        }
        if (room.bottom != s) {
            return;
        }
        int key = layout.edgeKey(s, bx, bz, axis);
        if (edges[key] != 0) {
            return;
        }
        List<int[]> list = perRoom.get(room.index);
        if (list == null) {
            list = new ArrayList<>();
            perRoom.set(room.index, list);
        }
        list.add(new int[]{key, atrium.index, s});
    }
}
