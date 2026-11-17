package de.officerooms.room;

import de.officerooms.atrium.Atrium;
import de.officerooms.atrium.AtriumPlanner;
import de.officerooms.core.Hashing;
import de.officerooms.core.Hashing.Rng;
import de.officerooms.room.connection.ConnectionPlanner;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Erzeugt die {@link RegionLayout} einer Region.
 *
 * <p>Reihenfolge (alles deterministisch aus {@code hash(seed, rx, rz, ...)}):</p>
 * <ol>
 *     <li>Atrien für die ganze Regionssäule ({@link AtriumPlanner}).</li>
 *     <li>Geschoss für Geschoss von unten nach oben:
 *         <ol>
 *             <li>Treppenhäuser (reservieren ihre Zellen auch im Geschoss darüber und markieren
 *             die Zellen vor beiden Treppenenden als „muss begehbar sein“),</li>
 *             <li>Korridornetze, beginnend an den Treppenenden,</li>
 *             <li>Räume als Rechtecke (gewichtete Größenklassen), gelegentlich zusammengesetzt,</li>
 *             <li>Raumhöhen (90 % / 7 % / 2 % / 1 %), hohe Räume reservieren die Geschosse darüber,</li>
 *             <li>Restzellen werden an Nachbarräume angehängt (L-/Sonderformen) oder massiv gefüllt.</li>
 *         </ol>
 *     </li>
 *     <li>Verbindungsplanung ({@link ConnectionPlanner}): Spannbaum + Zusatzkanten, unerreichbare
 *     Räume werden versiegelt (massiv gefüllt).</li>
 * </ol>
 */
public final class LayoutBuilder {

    static final int[] DX = {1, 0, -1, 0};
    static final int[] DZ = {0, 1, 0, -1};

    private final GenSettings cfg;
    private final long seed;
    private final int rx;
    private final int rz;
    private final int storeys;
    private final int ru;
    private final int pitch;
    private final int base;
    private final int[] own;
    private final BitSet mustWalk;
    private final List<Room> rooms = new ArrayList<>();
    private List<Atrium> atria;
    private int pendingTall;
    private int heightTotalWeight;
    private int sizeTotalWeight;

    private LayoutBuilder(GenSettings cfg, long seed, int rx, int rz, int storeys) {
        this.cfg = cfg;
        this.seed = seed;
        this.rx = rx;
        this.rz = rz;
        this.storeys = storeys;
        this.ru = cfg.regionUnits();
        this.pitch = cfg.pitch();
        this.base = cfg.baseHeight();
        this.own = new int[storeys * ru * ru];
        Arrays.fill(own, -1);
        this.mustWalk = new BitSet(own.length);
        for (GenSettings.HeightBand b : cfg.roomHeights()) {
            heightTotalWeight += Math.max(0, b.weight());
        }
        for (GenSettings.RoomSize t : cfg.roomSizes()) {
            sizeTotalWeight += Math.max(0, t.weight());
        }
    }

    public static RegionLayout build(GenSettings cfg, long seed, int rx, int rz, int storeys) {
        return new LayoutBuilder(cfg, seed, rx, rz, storeys).run();
    }

    private RegionLayout run() {
        atria = AtriumPlanner.plan(cfg, new Rng(Hashing.hash(seed, rx, rz, 0xA7)), storeys);
        for (Atrium a : atria) {
            Room r = newRoom(Room.Kind.ATRIUM, a.s0, a.storeys, a.clear);
            r.sizeClass = "atrium_" + a.cls;
            r.atrium = a;
            r.cells = a.w * a.d;
            a.room = r;
            for (int s = a.s0; s <= a.top(); s++) {
                for (int z = a.uz; z < a.uz + a.d; z++) {
                    for (int x = a.ux; x < a.ux + a.w; x++) {
                        own[idx(s, x, z)] = r.index + 1;
                    }
                }
            }
        }

        for (int s = 0; s < storeys; s++) {
            Rng rng = new Rng(Hashing.hash(seed, rx, rz, 0x5701, s));
            placeStairs(s, rng);
            placeCorridors(s, rng);
            placeRooms(s, rng);
            fillLeftovers(s);
        }

        short[] owner = new short[own.length];
        for (int i = 0; i < own.length; i++) {
            owner[i] = (short) Math.max(0, own[i]);
        }

        // Knoten: jeder Raum ist ein Knoten, zusätzlich je Atrium der Boden und jede Galerieebene.
        int nodes = rooms.size();
        List<Integer> nodeRoomList = new ArrayList<>();
        for (Room r : rooms) {
            nodeRoomList.add(r.index);
        }
        for (Atrium a : atria) {
            for (int o = 0; o < a.storeys; o++) {
                if (o == 0 || a.walk[o] != null) {
                    a.levelNode[o] = nodes++;
                    nodeRoomList.add(a.room.index);
                }
            }
        }
        int[] nodeRoom = nodeRoomList.stream().mapToInt(Integer::intValue).toArray();

        RegionLayout layout = new RegionLayout(rx, rz, storeys, ru, owner, rooms, atria, new ArrayList<>(), nodeRoom);
        ConnectionPlanner.planInternal(layout, cfg, seed);
        return layout;
    }

    // -------------------------------------------------------------------------------------------
    // Hilfen
    // -------------------------------------------------------------------------------------------

    private int idx(int s, int x, int z) {
        return (s * ru + z) * ru + x;
    }

    private boolean in(int x, int z) {
        return x >= 0 && z >= 0 && x < ru && z < ru;
    }

    private boolean free(int s, int x, int z) {
        return in(x, z) && own[idx(s, x, z)] == -1;
    }

    /** Frei und nicht als Treppenzugang reserviert. */
    private boolean freeStrict(int s, int x, int z) {
        return free(s, x, z) && !mustWalk.get(idx(s, x, z));
    }

    private Room newRoom(Room.Kind kind, int bottom, int k, int clear) {
        Room r = new Room(rooms.size(), kind, bottom, k, clear);
        rooms.add(r);
        return r;
    }

    private int edgeBetween(int s, int ax, int az, int bx, int bz) {
        if (ax == bx) {
            return RegionLayout.edgeKey(ru, s, ax, Math.max(az, bz), 1);
        }
        return RegionLayout.edgeKey(ru, s, Math.max(ax, bx), az, 0);
    }

    // -------------------------------------------------------------------------------------------
    // Treppenhäuser
    // -------------------------------------------------------------------------------------------

    private void placeStairs(int s, Rng rng) {
        if (s + 1 >= storeys) {
            return;
        }
        int len = cfg.stairCells();
        int target = Math.max(0, cfg.stairsPerStorey());
        int placed = 0;
        for (int attempt = 0; attempt < target * 12 && placed < target; attempt++) {
            int d = rng.nextInt(4);
            int sx = rng.range(1, ru - 2);
            int sz = rng.range(1, ru - 2);
            int lowX = sx - DX[d];
            int lowZ = sz - DZ[d];
            int hiX = sx + DX[d] * len;
            int hiZ = sz + DZ[d] * len;
            if (!in(lowX, lowZ) || !in(hiX, hiZ)) {
                continue;
            }
            boolean ok = free(s, lowX, lowZ) && free(s + 1, hiX, hiZ);
            for (int i = 0; i < len && ok; i++) {
                int cx = sx + DX[d] * i;
                int cz = sz + DZ[d] * i;
                ok = freeStrict(s, cx, cz) && freeStrict(s + 1, cx, cz);
            }
            if (!ok) {
                continue;
            }
            Room r = newRoom(Room.Kind.STAIRS, s, 2, 2 * pitch - 1);
            r.sizeClass = "stairs";
            r.stairX = sx;
            r.stairZ = sz;
            r.stairDir = d;
            r.stairLength = len;
            r.cells = len;
            for (int i = 0; i < len; i++) {
                int cx = sx + DX[d] * i;
                int cz = sz + DZ[d] * i;
                own[idx(s, cx, cz)] = r.index + 1;
                own[idx(s + 1, cx, cz)] = r.index + 1;
            }
            mustWalk.set(idx(s, lowX, lowZ));
            mustWalk.set(idx(s + 1, hiX, hiZ));
            r.lowEdgeKey = edgeBetween(s, lowX, lowZ, sx, sz);
            int lastX = sx + DX[d] * (len - 1);
            int lastZ = sz + DZ[d] * (len - 1);
            r.highEdgeKey = edgeBetween(s + 1, lastX, lastZ, hiX, hiZ);
            placed++;
        }
    }

    // -------------------------------------------------------------------------------------------
    // Korridore
    // -------------------------------------------------------------------------------------------

    private int corridorBudget;

    private void placeCorridors(int s, Rng rng) {
        GenSettings.Corridors c = cfg.corridors();
        corridorBudget = (int) (ru * ru * c.maxShare());
        List<int[]> seeds = new ArrayList<>();
        for (int z = 0; z < ru; z++) {
            for (int x = 0; x < ru; x++) {
                if (mustWalk.get(idx(s, x, z)) && own[idx(s, x, z)] == -1) {
                    seeds.add(new int[]{x, z});
                }
            }
        }
        int extra = rng.range(c.seedsMin(), c.seedsMax());
        for (int i = 0; i < extra; i++) {
            seeds.add(new int[]{rng.range(1, ru - 2), rng.range(1, ru - 2)});
        }
        for (int[] sd : seeds) {
            if (corridorBudget <= 0) {
                break;
            }
            if (own[idx(s, sd[0], sd[1])] != -1) {
                continue;
            }
            growCorridor(s, sd[0], sd[1], rng);
        }
    }

    private int segLen(Rng rng) {
        GenSettings.Corridors c = cfg.corridors();
        if (rng.chance(c.longChance())) {
            return rng.range(c.segmentMax(), Math.max(c.segmentMax(), c.longMax()));
        }
        return rng.range(c.segmentMin(), c.segmentMax());
    }

    private void growCorridor(int s, int sx, int sz, Rng rng) {
        GenSettings.Corridors c = cfg.corridors();
        Room cor = newRoom(Room.Kind.CORRIDOR, s, 1, base);
        cor.sizeClass = "corridor";
        int id = cor.index + 1;
        int max = Math.min(corridorBudget, 30 + rng.nextInt(90));
        own[idx(s, sx, sz)] = id;
        cor.cells = 1;

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        int d0 = rng.nextInt(4);
        int w0 = rng.chance(c.wideChance()) ? 2 : 1;
        queue.add(new int[]{sx, sz, d0, w0, segLen(rng)});
        if (rng.chance(0.4)) {
            queue.add(new int[]{sx, sz, (d0 + 2) % 4, 1, segLen(rng)});
        }
        int guard = 0;
        while (!queue.isEmpty() && cor.cells < max && guard++ < 64) {
            int[] seg = queue.poll();
            int curX = seg[0];
            int curZ = seg[1];
            int d = seg[2];
            int w = seg[3];
            int len = seg[4];
            int right = (d + 1) % 4;
            int left = (d + 3) % 4;
            int[] prev = {idx(s, curX, curZ)};
            int rows = 0;
            for (int i = 0; i < len && cor.cells < max; i++) {
                int nx = curX + DX[d];
                int nz = curZ + DZ[d];
                int[] row = new int[w];
                boolean ok = true;
                for (int j = 0; j < w; j++) {
                    int cx = nx + DX[right] * j;
                    int cz = nz + DZ[right] * j;
                    if (!free(s, cx, cz)) {
                        ok = false;
                        break;
                    }
                    row[j] = idx(s, cx, cz);
                }
                if (!ok || !rowAllowed(s, row, prev, id)) {
                    break;
                }
                for (int cell : row) {
                    own[cell] = id;
                }
                cor.cells += w;
                rows++;
                prev = row;
                curX = nx;
                curZ = nz;
                if (w == 1 && rng.chance(c.sideBranchChance())) {
                    queue.add(new int[]{curX, curZ, rng.chance(0.5) ? left : right, 1, segLen(rng)});
                }
            }
            if (rows == 0) {
                continue;
            }
            double roll = rng.nextDouble();
            if (roll < c.turnChance()) {
                int nd = rng.chance(0.5) ? left : right;
                int ax = curX;
                int az = curZ;
                if (w == 2 && nd == right) {
                    ax += DX[right];
                    az += DZ[right];
                }
                queue.add(new int[]{ax, az, nd, rng.chance(c.wideChance() * 0.5) ? 2 : 1, segLen(rng)});
            } else if (roll < c.turnChance() + c.branchChance()) {
                int ax = curX;
                int az = curZ;
                queue.add(new int[]{ax, az, left, 1, segLen(rng)});
                queue.add(new int[]{ax + (w == 2 ? DX[right] : 0), az + (w == 2 ? DZ[right] : 0), right, 1, segLen(rng)});
                if (rng.chance(0.3)) {
                    queue.add(new int[]{ax, az, d, 1, segLen(rng)});
                }
            } else if (roll < c.turnChance() + c.branchChance() + 0.12) {
                queue.add(new int[]{curX, curZ, d, rng.chance(c.wideChance()) ? 2 : 1, segLen(rng)});
            }
        }
        corridorBudget -= cor.cells;
    }

    /** Eine neue Korridorreihe darf nur an die vorige Reihe grenzen, nie seitlich an andere Teile desselben Netzes. */
    private boolean rowAllowed(int s, int[] row, int[] prev, int id) {
        for (int cell : row) {
            int local = cell - s * ru * ru;
            int x = local % ru;
            int z = local / ru;
            for (int k = 0; k < 4; k++) {
                int nx = x + DX[k];
                int nz = z + DZ[k];
                if (!in(nx, nz)) {
                    continue;
                }
                int n = idx(s, nx, nz);
                if (own[n] == id && !contains(prev, n) && !contains(row, n)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean contains(int[] a, int v) {
        for (int x : a) {
            if (x == v) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------------------------
    // Räume
    // -------------------------------------------------------------------------------------------

    private GenSettings.RoomSize pickSize(Rng rng) {
        int r = rng.nextInt(Math.max(1, sizeTotalWeight));
        for (GenSettings.RoomSize t : cfg.roomSizes()) {
            r -= Math.max(0, t.weight());
            if (r < 0) {
                return t;
            }
        }
        return cfg.roomSizes().get(0);
    }

    private int drawHeight(Rng rng) {
        if (pendingTall > 0) {
            int h = pendingTall;
            pendingTall = 0;
            return h;
        }
        int r = rng.nextInt(Math.max(1, heightTotalWeight));
        for (GenSettings.HeightBand b : cfg.roomHeights()) {
            r -= Math.max(0, b.weight());
            if (r < 0) {
                return Math.max(3, rng.range(b.min(), b.max()));
            }
        }
        return base;
    }

    private void placeRooms(int s, Rng rng) {
        if (cfg.roomSizes().isEmpty()) {
            return;
        }
        for (int z = 0; z < ru; z++) {
            for (int x = 0; x < ru; x++) {
                if (!free(s, x, z)) {
                    continue;
                }
                GenSettings.RoomSize t = pickSize(rng);
                int w = rng.range(t.minW(), t.maxW());
                int d = rng.range(t.minD(), t.maxD());
                if (rng.chance(0.5)) {
                    int tmp = w;
                    w = d;
                    d = tmp;
                }
                int fw = 0;
                while (fw < w && free(s, x + fw, z)) {
                    fw++;
                }
                if (fw < 2) {
                    continue;
                }
                int fd = 0;
                while (fd < d && rowFree(s, x, z + fd, fw)) {
                    fd++;
                }
                if (fd < 2) {
                    continue;
                }
                Room room = newRoom(Room.Kind.ROOM, s, 1, base);
                room.sizeClass = (fw * fd >= 0.75 * w * d) ? t.name() : classify(fw, fd);
                List<Integer> cells = new ArrayList<>();
                for (int zz = z; zz < z + fd; zz++) {
                    for (int xx = x; xx < x + fw; xx++) {
                        own[idx(s, xx, zz)] = room.index + 1;
                        cells.add(idx(s, xx, zz));
                    }
                }
                if (rng.chance(cfg.compositeChance())) {
                    attachWing(s, rng, room, x, z, fw, fd, cells);
                }
                room.cells = cells.size();
                assignHeight(s, rng, room, cells);
            }
        }
    }

    private boolean rowFree(int s, int x, int z, int w) {
        for (int i = 0; i < w; i++) {
            if (!free(s, x + i, z)) {
                return false;
            }
        }
        return true;
    }

    /** Hängt ein zweites Rechteck an: L-, T- oder versetzte Grundrisse. */
    private void attachWing(int s, Rng rng, Room room, int x, int z, int w, int d, List<Integer> cells) {
        int ww = rng.range(2, 4);
        int wd = rng.range(2, 4);
        int wx;
        int wz;
        if (rng.chance(0.5)) {
            // nach Süden
            wx = x + rng.range(-1, Math.max(-1, w - ww + 1));
            wz = z + d;
        } else {
            // nach Osten
            wx = x + w;
            wz = z + rng.range(-1, Math.max(-1, d - wd + 1));
            int t = ww;
            ww = wd;
            wd = t;
        }
        for (int zz = wz; zz < wz + wd; zz++) {
            for (int xx = wx; xx < wx + ww; xx++) {
                if (!free(s, xx, zz)) {
                    return;
                }
            }
        }
        // Der Flügel muss am Hauptrechteck anliegen (mindestens eine gemeinsame Kante).
        boolean touches = false;
        for (int zz = wz; zz < wz + wd && !touches; zz++) {
            for (int xx = wx; xx < wx + ww && !touches; xx++) {
                for (int k = 0; k < 4; k++) {
                    int nx = xx + DX[k];
                    int nz = zz + DZ[k];
                    if (in(nx, nz) && own[idx(s, nx, nz)] == room.index + 1) {
                        touches = true;
                        break;
                    }
                }
            }
        }
        if (!touches) {
            return;
        }
        for (int zz = wz; zz < wz + wd; zz++) {
            for (int xx = wx; xx < wx + ww; xx++) {
                own[idx(s, xx, zz)] = room.index + 1;
                cells.add(idx(s, xx, zz));
            }
        }
        room.composite = true;
    }

    private static String classify(int w, int d) {
        int a = w * d;
        if (a <= 9) {
            return "small";
        }
        if (a <= 30) {
            return "office";
        }
        if (a <= 64) {
            return "open_plan";
        }
        return "large_open";
    }

    /**
     * Raumhöhe würfeln. Hohe Räume belegen zusätzliche Geschosse; ist darüber kein Platz, wird
     * der Raum 4 hoch und die gewürfelte Höhe an den nächsten Raum weitergereicht („Schuld“),
     * damit die konfigurierte Verteilung trotz Platzkonflikten erhalten bleibt.
     */
    private void assignHeight(int s, Rng rng, Room room, List<Integer> cells) {
        int h = drawHeight(rng);
        int k = (h + 1 + pitch - 1) / pitch;
        if (k <= 1) {
            room.clear = h;
            return;
        }
        boolean ok = s + k - 1 < storeys;
        int layer = ru * ru;
        for (int up = 1; up < k && ok; up++) {
            for (int cell : cells) {
                int c = cell + up * layer;
                if (own[c] != -1 || mustWalk.get(c)) {
                    ok = false;
                    break;
                }
            }
        }
        if (!ok) {
            if (pendingTall == 0) {
                pendingTall = h;
            }
            room.clear = base;
            return;
        }
        for (int up = 1; up < k; up++) {
            for (int cell : cells) {
                own[cell + up * layer] = room.index + 1;
            }
        }
        room.storeys = k;
        room.clear = h;
    }

    // -------------------------------------------------------------------------------------------
    // Restzellen
    // -------------------------------------------------------------------------------------------

    private void fillLeftovers(int s) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int z = 0; z < ru; z++) {
                for (int x = 0; x < ru; x++) {
                    int i = idx(s, x, z);
                    if (own[i] != -1) {
                        continue;
                    }
                    Room best = null;
                    for (int k = 0; k < 4; k++) {
                        int nx = x + DX[k];
                        int nz = z + DZ[k];
                        if (!in(nx, nz)) {
                            continue;
                        }
                        int o = own[idx(s, nx, nz)];
                        if (o <= 0) {
                            continue;
                        }
                        Room r = rooms.get(o - 1);
                        if (r.kind == Room.Kind.ROOM && r.bottom == s && r.storeys == 1
                                && (best == null || r.cells < best.cells)) {
                            best = r;
                        }
                    }
                    if (best != null) {
                        own[i] = best.index + 1;
                        best.cells++;
                        best.composite = true;
                        changed = true;
                    }
                }
            }
        }
        for (int z = 0; z < ru; z++) {
            for (int x = 0; x < ru; x++) {
                int i = idx(s, x, z);
                if (own[i] != -1) {
                    continue;
                }
                if (mustWalk.get(i)) {
                    // Treppenzugang ohne Nachbarraum: kleiner Vorraum aus allen angrenzenden Restzellen.
                    Room r = newRoom(Room.Kind.ROOM, s, 1, base);
                    r.sizeClass = "vestibule";
                    ArrayDeque<int[]> q = new ArrayDeque<>();
                    q.add(new int[]{x, z});
                    own[i] = r.index + 1;
                    while (!q.isEmpty()) {
                        int[] p = q.poll();
                        r.cells++;
                        for (int k = 0; k < 4; k++) {
                            int nx = p[0] + DX[k];
                            int nz = p[1] + DZ[k];
                            if (free(s, nx, nz)) {
                                own[idx(s, nx, nz)] = r.index + 1;
                                q.add(new int[]{nx, nz});
                            }
                        }
                    }
                } else {
                    own[i] = 0;
                }
            }
        }
    }
}
