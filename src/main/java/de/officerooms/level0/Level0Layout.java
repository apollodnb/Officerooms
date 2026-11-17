package de.officerooms.level0;

import java.util.ArrayDeque;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 0.21.0: the layout of Level 0, a pure function of (seed, x, z). Four floors of 6 × 6 cells (1-block walls,
 * 5 × 5 inside, 4 blocks high) in regions of 16 × 16 cells. Each floor of a region is a randomised spanning tree
 * plus extra loops, so every free cell is reachable; neighbouring regions are joined by at least one opening on
 * every border (border cells are never blocked). Ladders join the floors. On top come pillar halls, rare
 * Threshold rooms (holes into the floor below) and the very rare, hidden Manila Rooms (with the escape puzzle
 * carved into the solid block reserved under them).
 */
public final class Level0Layout {

    public static final int CELL = 6, R = 16, FLOORS = 4, HEIGHT = 48;
    static final double P_MANILA = 0.22, P_THRESH = 0.22;
    public static final byte FREE = 0, BLOCKED = 1, MANILA = 2, THRESH = 3, UNDER = 4, HALL = 5;
    public static final byte CLOSED = 0, OPEN = 1, DOOR = 2, TINY = 3;

    private Level0Layout() {
    }

    /** 0.33.0: floors 7 apart (was 6): five blocks of walking space instead of four. */
    public static final int PITCH = 7;

    public static int floorY(int k) {
        return 6 + PITCH * k;
    }

    public static int walk(int k) {
        return 7 + PITCH * k;
    }

    public static int ceil(int k) {
        return 12 + PITCH * k;
    }

    /** Floor whose walking space contains y (clamped). */
    public static int floorOf(double y) {
        return Math.max(0, Math.min(FLOORS - 1, (int) Math.floor((y - 7) / (double) PITCH)));
    }

    /** 0.33.0: whether the connection from floor k up at this cell is a carpeted ramp (else a ladder). */
    public static boolean ramp(long seed, int k, int gx, int gz) {
        return u(seed, gx, gz, k, 0x4A3) < 0.65;
    }

    // --- hashing ------------------------------------------------------------------------------------------
    static long h(long seed, long a, long b, long c, long d) {
        long x = seed ^ a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL ^ c * 0x165667B19E3779F9L ^ d * 0xD6E8FEB86659FD93L;
        x ^= x >>> 31;
        x *= 0xBF58476D1CE4E5B9L;
        x ^= x >>> 29;
        x *= 0x94D049BB133111EBL;
        return x ^ (x >>> 32);
    }

    static double u(long seed, long a, long b, long c, long d) {
        return (h(seed, a, b, c, d) >>> 11) * 0x1.0p-53;
    }

    // --- special rooms (cheap, plan-free) --------------------------------------------------------------------
    /** The Manila Room of a region: {floor, mx, mz, side (0 N, 1 E, 2 S, 3 W), entrance cell 0..2} or null. */
    public static int[] manila(long seed, int rx, int rz) {
        if (u(seed, rx, rz, 11, 0) >= P_MANILA) {
            return null;
        }
        return new int[]{(int) (u(seed, rx, rz, 11, 1) * FLOORS), 1 + (int) (u(seed, rx, rz, 11, 2) * 12), 1 + (int) (u(seed, rx, rz, 11, 3) * 12),
                (int) (u(seed, rx, rz, 11, 4) * 4), (int) (u(seed, rx, rz, 11, 5) * 3)};
    }

    static boolean inRect(int cx, int cz, int x0, int z0, int w, int d) {
        return cx >= x0 && cx < x0 + w && cz >= z0 && cz < z0 + d;
    }

    static boolean overlap(int ax, int az, int aw, int ad, int bx, int bz, int bw, int bd) {
        return ax < bx + bw && bx < ax + aw && az < bz + bd && bz < az + ad;
    }

    /** Threshold room on floor k (1..3): {tx, tz} of a 5 × 4 cell rectangle, or null. */
    static int[] thresh(long seed, int k, int rx, int rz) {
        if (k < 1 || k >= FLOORS || u(seed, rx, rz, 12, k) >= P_THRESH) {
            return null;
        }
        int tx = 1 + (int) (u(seed, rx, rz, 13, k) * 10), tz = 1 + (int) (u(seed, rx, rz, 14, k) * 11);
        int[] m = manila(seed, rx, rz);
        if (m != null && m[0] >= k - 2 && m[0] <= k + 1 && overlap(tx, tz, 5, 4, m[1], m[2], 3, 3)) {
            return null;
        }
        return new int[]{tx, tz};
    }

    static byte baseType(long seed, int k, int rx, int rz, int cx, int cz) {
        int[] m = manila(seed, rx, rz);
        if (m != null && inRect(cx, cz, m[1], m[2], 3, 3)) {
            if (m[0] == k) {
                return MANILA;
            }
            if (m[0] == k + 1) {
                return BLOCKED;
            }
        }
        int[] t = thresh(seed, k, rx, rz);
        if (t != null && inRect(cx, cz, t[0], t[1], 5, 4)) {
            return THRESH;
        }
        int[] t2 = thresh(seed, k + 1, rx, rz);
        if (t2 != null && inRect(cx, cz, t2[0], t2[1], 5, 4)) {
            return UNDER;
        }
        return FREE;
    }

    /** Cells (index cx + cz·16) with a ladder from floor k up to k + 1. */
    static int[] ladders(long seed, int k, int rx, int rz) {
        if (k < 0 || k >= FLOORS - 1) {
            return new int[0];
        }
        int[] out = new int[3];
        int n = 0;
        for (int a = 0; a < 40 && n < 3; a++) {
            int cx = (int) (u(seed, rx, rz, 20 + k, a) * R), cz = (int) (u(seed, rx, rz, 30 + k, a) * R);
            if (baseType(seed, k, rx, rz, cx, cz) == FREE && baseType(seed, k + 1, rx, rz, cx, cz) == FREE) {
                out[n++] = cx + cz * R;
            }
        }
        return java.util.Arrays.copyOf(out, n);
    }

    static byte openType(double v) {
        return v < 0.45 ? OPEN : v < 0.85 ? DOOR : TINY;
    }

    /** Edge between region cell (15, i) of (rx, rz) and (0, i) of (rx + 1, rz). */
    static byte borderE(long seed, int k, int rx, int rz, int i) {
        int best = 0;
        double bv = 2;
        for (int j = 0; j < R; j++) {
            double v = u(seed, rx, rz, 40 + k, j);
            if (v < bv) {
                bv = v;
                best = j;
            }
        }
        double v = u(seed, rx, rz, 40 + k, i);
        return v < 0.25 || i == best ? openType(u(seed, rx, rz, 50 + k, i)) : CLOSED;
    }

    /** Edge between region cell (i, 15) of (rx, rz) and (i, 0) of (rx, rz + 1). */
    static byte borderS(long seed, int k, int rx, int rz, int i) {
        int best = 0;
        double bv = 2;
        for (int j = 0; j < R; j++) {
            double v = u(seed, rx, rz, 60 + k, j);
            if (v < bv) {
                bv = v;
                best = j;
            }
        }
        double v = u(seed, rx, rz, 60 + k, i);
        return v < 0.25 || i == best ? openType(u(seed, rx, rz, 70 + k, i)) : CLOSED;
    }

    // --- plans --------------------------------------------------------------------------------------------
    public static final class Plan {
        public final byte[] type = new byte[R * R], east = new byte[R * R], south = new byte[R * R], furn = new byte[R * R];
        public final boolean[] pillar = new boolean[R * R], ladderUp = new boolean[R * R], ladderExit = new boolean[R * R];
        /** 0 dead, 1 lit, 2 flickering. */
        public final byte[] light = new byte[R * R];
        public int[] manila;
    }

    private static final ConcurrentHashMap<Long, Plan> CACHE = new ConcurrentHashMap<>();

    public static Plan plan(long seed, int k, int rx, int rz) {
        long key = h(seed, k, rx, rz, 99);
        Plan p = CACHE.get(key);
        if (p == null) {
            if (CACHE.size() > 4096) {
                CACHE.clear();
            }
            p = build(seed, k, rx, rz);
            CACHE.put(key, p);
        }
        return p;
    }

    private static boolean walkable(byte t) {
        return t != BLOCKED && t != MANILA;
    }

    private static void openRect(Plan p, int x0, int z0, int w, int d) {
        for (int cx = x0; cx < x0 + w; cx++) {
            for (int cz = z0; cz < z0 + d; cz++) {
                if (cx < x0 + w - 1) {
                    p.east[cx + cz * R] = OPEN;
                }
                if (cz < z0 + d - 1) {
                    p.south[cx + cz * R] = OPEN;
                }
            }
        }
    }

    private static Plan build(long seed, int k, int rx, int rz) {
        Plan p = new Plan();
        Random r = new Random(h(seed, k, rx, rz, 7));
        for (int cx = 0; cx < R; cx++) {
            for (int cz = 0; cz < R; cz++) {
                p.type[cx + cz * R] = baseType(seed, k, rx, rz, cx, cz);
            }
        }
        // spanning tree (iterative DFS) over walkable cells
        boolean[] seen = new boolean[R * R];
        ArrayDeque<Integer> st = new ArrayDeque<>();
        st.push(0);
        seen[0] = true;
        int[] dx = {1, -1, 0, 0}, dz = {0, 0, 1, -1};
        while (!st.isEmpty()) {
            int c = st.peek();
            int cx = c % R, cz = c / R;
            int[] order = {0, 1, 2, 3};
            for (int i = 3; i > 0; i--) {
                int j = r.nextInt(i + 1), t = order[i];
                order[i] = order[j];
                order[j] = t;
            }
            boolean moved = false;
            for (int d : order) {
                int nx = cx + dx[d], nz = cz + dz[d];
                if (nx < 0 || nz < 0 || nx >= R || nz >= R) {
                    continue;
                }
                int n = nx + nz * R;
                if (seen[n] || !walkable(p.type[n])) {
                    continue;
                }
                byte t = openType(r.nextDouble());
                switch (d) {
                    case 0 -> p.east[c] = t;
                    case 1 -> p.east[n] = t;
                    case 2 -> p.south[c] = t;
                    default -> p.south[n] = t;
                }
                seen[n] = true;
                st.push(n);
                moved = true;
                break;
            }
            if (!moved) {
                st.pop();
            }
        }
        // extra loops
        for (int cx = 0; cx < R; cx++) {
            for (int cz = 0; cz < R; cz++) {
                int c = cx + cz * R;
                if (!walkable(p.type[c])) {
                    continue;
                }
                if (cx < R - 1 && p.east[c] == CLOSED && walkable(p.type[c + 1]) && r.nextDouble() < 0.16) {
                    p.east[c] = openType(r.nextDouble());
                }
                if (cz < R - 1 && p.south[c] == CLOSED && walkable(p.type[c + R]) && r.nextDouble() < 0.16) {
                    p.south[c] = openType(r.nextDouble());
                }
            }
        }
        // pillar halls
        for (int i = 0; i < 3; i++) {
            int w = 2 + r.nextInt(3), d = 2 + r.nextInt(3), x0 = r.nextInt(R - w + 1), z0 = r.nextInt(R - d + 1);
            boolean ok = true;
            for (int cx = x0; cx < x0 + w && ok; cx++) {
                for (int cz = z0; cz < z0 + d && ok; cz++) {
                    ok = p.type[cx + cz * R] == FREE;
                }
            }
            if (!ok) {
                continue;
            }
            openRect(p, x0, z0, w, d);
            for (int cx = x0; cx < x0 + w; cx++) {
                for (int cz = z0; cz < z0 + d; cz++) {
                    p.type[cx + cz * R] = HALL;
                    if (cx > x0 && cz > z0) {
                        p.pillar[cx + cz * R] = r.nextDouble() < 0.7;
                    }
                }
            }
        }
        int[] t = thresh(seed, k, rx, rz);
        if (t != null) {
            openRect(p, t[0], t[1], 5, 4);
        }
        int[] t2 = thresh(seed, k + 1, rx, rz);
        if (t2 != null) {
            openRect(p, t2[0], t2[1], 5, 4);
            for (int cx = t2[0] + 1; cx < t2[0] + 5; cx++) {
                for (int cz = t2[1] + 1; cz < t2[1] + 4; cz++) {
                    p.pillar[cx + cz * R] = r.nextBoolean();
                }
            }
        }
        int[] m = manila(seed, rx, rz);
        if (m != null && m[0] == k) {
            p.manila = m;
            openRect(p, m[1], m[2], 3, 3);
            int e = m[4];
            switch (m[3]) {
                case 0 -> p.south[(m[1] + e) + (m[2] - 1) * R] = TINY;
                case 1 -> p.east[(m[1] + 2) + (m[2] + e) * R] = TINY;
                case 2 -> p.south[(m[1] + e) + (m[2] + 2) * R] = TINY;
                default -> p.east[(m[1] - 1) + (m[2] + e) * R] = TINY;
            }
        }
        for (int c : ladders(seed, k, rx, rz)) {
            p.ladderUp[c] = true;
        }
        for (int c : ladders(seed, k - 1, rx, rz)) {
            p.ladderExit[c] = true;
        }
        for (int c = 0; c < R * R; c++) {
            double lv = r.nextDouble();
            p.light[c] = (byte) (lv < 0.66 ? 1 : lv < 0.72 ? 2 : 0);
            byte ty = p.type[c];
            if (p.ladderUp[c] || p.ladderExit[c] || (ty != FREE && ty != HALL)) {
                continue;
            }
            double v = r.nextDouble();
            if (v < 0.045) {
                p.furn[c] = 1;
            } else if (ty == HALL && v < 0.10) {
                p.furn[c] = 2;
            } else {
                // 0.21.1: odd wooden furniture and paradoxical architecture (never blocking the way through the cell)
                double o = r.nextDouble();
                p.furn[c] = (byte) (o < 0.05 ? 3 : o < 0.065 ? 4 : o < 0.085 ? 5 : o < 0.105 ? 6 : o < 0.13 ? 7 : o < 0.15 ? 8 : o < 0.165 ? 9
                        // 0.33.0: more Level 0: pillars, holes, pointless steps, sealed closets, puddles, light switches
                        : o < 0.20 ? 10 : o < 0.235 ? 11 : o < 0.26 ? 12 : o < 0.28 ? 13 : o < 0.32 ? 14 : o < 0.34 ? 15 : o < 0.36 ? 16 : 0);
            }
        }
        return p;
    }

    // --- global edge queries --------------------------------------------------------------------------------
    /** Edge between global cells (gx, gz) and (gx + 1, gz). */
    public static byte edgeE(long seed, int k, int gx, int gz) {
        int rx = Math.floorDiv(gx, R), rz = Math.floorDiv(gz, R), cx = Math.floorMod(gx, R), cz = Math.floorMod(gz, R);
        return cx == R - 1 ? borderE(seed, k, rx, rz, cz) : plan(seed, k, rx, rz).east[cx + cz * R];
    }

    /** Edge between global cells (gx, gz) and (gx, gz + 1). */
    public static byte edgeS(long seed, int k, int gx, int gz) {
        int rx = Math.floorDiv(gx, R), rz = Math.floorDiv(gz, R), cx = Math.floorMod(gx, R), cz = Math.floorMod(gz, R);
        return cz == R - 1 ? borderS(seed, k, rx, rz, cx) : plan(seed, k, rx, rz).south[cx + cz * R];
    }

    public static byte typeAt(long seed, int k, int gx, int gz) {
        return plan(seed, k, Math.floorDiv(gx, R), Math.floorDiv(gz, R)).type[Math.floorMod(gx, R) + Math.floorMod(gz, R) * R];
    }

    static boolean passable(byte e, int pos) {
        return switch (e) {
            case OPEN -> true;
            case DOOR -> pos == 2 || pos == 3;
            case TINY -> pos == 3;
            default -> false;
        };
    }
}
