package de.officerooms.room.connection;

import de.officerooms.atrium.Atrium;
import de.officerooms.core.Hashing;
import de.officerooms.core.Hashing.Rng;
import de.officerooms.room.GenSettings;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Plant, welche Räume einer Region miteinander verbunden werden.
 *
 * <p>Jede Zellkante zwischen zwei verschiedenen Räumen desselben Geschosses ist ein möglicher
 * Durchgang, wenn beide Seiten dort begehbar sind:</p>
 * <ul>
 *     <li>Raum/Korridor: nur im eigenen Bodengeschoss (bei hohen Räumen nie in halber Wandhöhe),</li>
 *     <li>Treppenhaus: nur genau an seinem unteren und oberen Ende,</li>
 *     <li>Atrium: auf dem Atriumboden und an Galerie-/Brückenzellen der Galeriegeschosse.</li>
 * </ul>
 * <p>Aus diesen Kanten entsteht ein Graph. Treppenanschlüsse sind Pflicht; danach baut ein
 * Kruskal-Spannbaum mit gewichteten Zufallsgewichten (Korridore bevorzugt) die Grundverbindung,
 * zusätzliche Kanten erzeugen Schleifen. Die größte Zusammenhangskomponente bleibt offen, alle
 * anderen Räume (ohne Atrien) werden versiegelt, also massiv gefüllt. Dadurch kann keine Tür in
 * einen unerreichbaren oder massiven Bereich führen.</p>
 */
public final class ConnectionPlanner {

    private ConnectionPlanner() {
    }

    /** Knoten der Zelle an dieser Kante oder -1, wenn dort keine Öffnung möglich ist. */
    public static int nodeAt(RegionLayout layout, Room r, int s, int cx, int cz, int edgeKey) {
        if (r == null) {
            return -1;
        }
        switch (r.kind) {
            case ROOM:
            case CORRIDOR:
                return s == r.bottom ? r.index : -1;
            case STAIRS:
                return (edgeKey == r.lowEdgeKey || edgeKey == r.highEdgeKey) ? r.index : -1;
            case ATRIUM: {
                Atrium a = r.atrium;
                if (s == a.s0) {
                    return a.levelNode[0];
                }
                if (a.isWalk(s, cx, cz)) {
                    return a.levelNode[s - a.s0];
                }
                return -1;
            }
            default:
                return -1;
        }
    }

    private static final class IntList {
        int[] a = new int[4];
        int n;

        void add(int v) {
            if (n == a.length) {
                a = Arrays.copyOf(a, n * 2);
            }
            a[n++] = v;
        }

        int[] toSortedArray() {
            int[] r = Arrays.copyOf(a, n);
            Arrays.sort(r);
            return r;
        }
    }

    private static final class Pair {
        final long key;
        final int a;
        final int b;
        final IntList edges = new IntList();
        boolean stairs;
        double weight;

        Pair(long key, int a, int b) {
            this.key = key;
            this.a = a;
            this.b = b;
        }
    }

    public static void planInternal(RegionLayout layout, GenSettings cfg, long seed) {
        int ru = layout.ru;
        Map<Long, Pair> pairs = new HashMap<>();
        List<Pair> order = new ArrayList<>();
        for (int s = 0; s < layout.storeys; s++) {
            for (int z = 0; z < ru; z++) {
                for (int x = 0; x < ru; x++) {
                    Room b = layout.rawRoom(s, x, z);
                    if (b == null) {
                        continue;
                    }
                    if (x > 0) {
                        consider(layout, pairs, order, s, x - 1, z, x, z, 0, b);
                    }
                    if (z > 0) {
                        consider(layout, pairs, order, s, x, z - 1, x, z, 1, b);
                    }
                }
            }
        }

        int nodes = layout.nodeRoom.length;
        int[] parent = new int[nodes];
        for (int i = 0; i < nodes; i++) {
            parent[i] = i;
        }

        GenSettings.Doors doors = cfg.doors();
        List<Pair> chosen = new ArrayList<>();
        List<Pair> rest = new ArrayList<>();
        for (Pair p : order) {
            if (p.stairs) {
                union(parent, p.a, p.b);
                chosen.add(p);
            } else {
                double f = typeFactor(layout, p);
                p.weight = Hashing.unit(Hashing.hash(seed, layout.rx, layout.rz, 0xC0, p.key)) * f;
                rest.add(p);
            }
        }
        rest.sort(Comparator.comparingDouble((Pair p) -> p.weight).thenComparingLong(p -> p.key));
        Rng rng = new Rng(Hashing.hash(seed, layout.rx, layout.rz, 0xC1));
        for (Pair p : rest) {
            if (find(parent, p.a) != find(parent, p.b)) {
                union(parent, p.a, p.b);
                chosen.add(p);
            } else if (rng.chance(Math.min(0.95, doors.extraChance() * extraFactor(layout, p)))) {
                chosen.add(p);
            }
        }

        // Größte Komponente (nach Grundrisszellen) bleibt offen.
        long[] size = new long[nodes];
        for (Room r : layout.rooms) {
            if (r.kind != Room.Kind.ATRIUM) {
                size[find(parent, r.index)] += r.cells;
            }
        }
        int main = 0;
        for (int i = 1; i < nodes; i++) {
            if (size[i] > size[main]) {
                main = i;
            }
        }
        for (Room r : layout.rooms) {
            if (r.kind != Room.Kind.ATRIUM && find(parent, r.index) != main) {
                r.sealed = true;
            }
        }
        for (Pair p : chosen) {
            if (find(parent, p.a) == main) {
                layout.connections.add(new Connection(p.a, p.b, p.edges.toSortedArray(), p.stairs));
            }
        }
    }

    private static void consider(RegionLayout layout, Map<Long, Pair> pairs, List<Pair> order, int s,
                                 int ax, int az, int bx, int bz, int axis, Room b) {
        Room a = layout.rawRoom(s, ax, az);
        if (a == null || a == b) {
            return;
        }
        int key = layout.edgeKey(s, bx, bz, axis);
        int na = nodeAt(layout, a, s, ax, az, key);
        int nb = nodeAt(layout, b, s, bx, bz, key);
        if (na < 0 || nb < 0 || na == nb) {
            return;
        }
        int lo = Math.min(na, nb);
        int hi = Math.max(na, nb);
        long pk = ((long) lo << 32) | hi;
        Pair p = pairs.get(pk);
        if (p == null) {
            p = new Pair(pk, lo, hi);
            pairs.put(pk, p);
            order.add(p);
        }
        p.edges.add(key);
        if ((a.kind == Room.Kind.STAIRS && (key == a.lowEdgeKey || key == a.highEdgeKey))
                || (b.kind == Room.Kind.STAIRS && (key == b.lowEdgeKey || key == b.highEdgeKey))) {
            p.stairs = true;
        }
    }

    private static Room.Kind kindOf(RegionLayout layout, int node) {
        return layout.rooms.get(layout.nodeRoom[node]).kind;
    }

    private static double typeFactor(RegionLayout layout, Pair p) {
        Room.Kind a = kindOf(layout, p.a);
        Room.Kind b = kindOf(layout, p.b);
        if (a == Room.Kind.ATRIUM || b == Room.Kind.ATRIUM) {
            return 0.5;
        }
        if (a == Room.Kind.CORRIDOR && b == Room.Kind.CORRIDOR) {
            return 0.2;
        }
        if (a == Room.Kind.CORRIDOR || b == Room.Kind.CORRIDOR) {
            return 0.35;
        }
        return 1.0;
    }

    private static double extraFactor(RegionLayout layout, Pair p) {
        Room.Kind a = kindOf(layout, p.a);
        Room.Kind b = kindOf(layout, p.b);
        if (a == Room.Kind.ATRIUM || b == Room.Kind.ATRIUM) {
            return 2.0;
        }
        if (a == Room.Kind.CORRIDOR && b == Room.Kind.CORRIDOR) {
            return 2.5;
        }
        if (a == Room.Kind.CORRIDOR || b == Room.Kind.CORRIDOR) {
            return 1.4;
        }
        return 0.6;
    }

    static int find(int[] p, int x) {
        while (p[x] != x) {
            p[x] = p[p[x]];
            x = p[x];
        }
        return x;
    }

    static void union(int[] p, int a, int b) {
        int ra = find(p, a);
        int rb = find(p, b);
        if (ra != rb) {
            if (ra < rb) {
                p[rb] = ra;
            } else {
                p[ra] = rb;
            }
        }
    }
}
