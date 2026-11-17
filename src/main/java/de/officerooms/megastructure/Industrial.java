package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.Room;
import java.util.Optional;

/**
 * 0.15.0: The Industrial Complex, a liminal factory megastructure. One region (160 × 160 blocks) replaces
 * {@link #STOREYS} storeys with five industrial levels (6 blocks each). Every level is a grid of 16 × 16 rooms
 * (concrete walls, grate doors in the middle of most walls) full of pipes: pipe galleries with grated floors you can
 * look through, control rooms, storerooms, small generator rooms and staircases. Two generator halls (2 × 2 rooms)
 * reach through the three lowest levels; they hold four huge generators (some still running) and have grated
 * catwalks along their walls on levels 1 and 2. Office doors open into the complex on every level.
 * Heights within a level: 0 floor, 1–4 walk space, 5 ceiling.
 */
public final class Industrial {

    /** 0.36.0: eight levels in new worlds, five in old ones. */
    public static final int STOREYS = 8;
    public static final int LEGACY_STOREYS = 5;
    public static final int H = STOREYS * 6;
    static final int W = 160;
    static final int C = 16;
    static final int N = W / C;
    /** Generator halls span this many levels. */
    public static final int HALL_LEVELS = 3;

    public static final int EMPTY = 0;
    public static final int PIPES = 1;
    public static final int CONTROL = 2;
    public static final int STORAGE = 3;
    public static final int GEN = 4;
    public static final int STAIR = 5;
    public static final int HALL = 6;
    // 0.36.0 (new worlds)
    public static final int TANKS = 7;
    public static final int FOUNDRY = 8;
    public static final int LOCKERS = 9;

    public enum Part {
        AIR, OFFICE, WALL, FLOOR, CEIL, GRATE, RAIL, PIPE_X, PIPE_Y, PIPE_Z, LAMP, LAMP_BROKEN,
        DOOR_X_LO, DOOR_X_HI, DOOR_X_OPEN_LO, DOOR_X_OPEN_HI, DOOR_Z_LO, DOOR_Z_HI, DOOR_Z_OPEN_LO, DOOR_Z_OPEN_HI,
        STAIR, SUPPORT, CASING, CORE, CORE_DEAD, PANEL, CCTV, CHAIR, LOCKER, CRATE,
        // 0.36.0
        TANK, TANK_TOP, FURNACE, VAT, COAL, LOCKER_E, BENCH
    }

    public static final class Site {
        public final int rx;
        public final int rz;
        public final int s0;
        public final int yBase;
        final long seed;
        /** Cell origins of the two generator halls (2 × 2 cells each). */
        public final int[][] halls;
        final int[][][] type = new int[STOREYS][N][N];
        public final boolean modern;
        public final int storeys;
        public final int h;
        /** Levels the generator halls reach through. */
        public final int hallLevels;

        Site(int rx, int rz, int s0, int yBase, long seed, int[][] halls, boolean modern) {
            this.modern = modern;
            this.storeys = modern ? STOREYS : LEGACY_STOREYS;
            this.h = storeys * 6;
            this.hallLevels = modern ? HALL_LEVELS + 1 : HALL_LEVELS;
            this.rx = rx;
            this.rz = rz;
            this.s0 = s0;
            this.yBase = yBase;
            this.seed = seed;
            this.halls = halls;
        }

        public boolean inBand(int y) {
            return y >= yBase && y < yBase + h;
        }

        public int level(int y) {
            return (y - yBase) / 6;
        }

        public int type(int lv, int cx, int cz) {
            return lv < 0 || lv >= storeys || cx < 0 || cz < 0 || cx >= N || cz >= N ? EMPTY : type[lv][cx][cz];
        }

        /** Index of the hall containing the cell, or -1. */
        public int hallOf(int cx, int cz) {
            for (int i = 0; i < halls.length; i++) {
                if (cx >= halls[i][0] && cx <= halls[i][0] + 1 && cz >= halls[i][1] && cz <= halls[i][1] + 1) {
                    return i;
                }
            }
            return -1;
        }

        double h(int salt, int a, int b) {
            return Hashing.unit(Hashing.hash(seed, rx, rz, salt, a * 4096 + b));
        }

        public int ringSide(int lx, int lz) {
            if (lx == 0 || lz == 0) {
                return -1;
            }
            if (lx <= 2) {
                return 0;
            }
            if (lz <= 2) {
                return 1;
            }
            if (lx >= W - 2) {
                return 2;
            }
            if (lz >= W - 2) {
                return 3;
            }
            return -1;
        }

        /** Door kind in the wall west (vertical = true, at lx = cx·16) or north of a cell: 0 none, 1 gap, 2 closed grate door, 3 open grate door. */
        int door(int lv, boolean vertical, int cx, int cz) {
            double u = h(0x9501 + lv * 2 + (vertical ? 1 : 0), cx, cz);
            if (u >= 0.78) {
                return 0;
            }
            return u < 0.25 ? 1 : u < 0.6 ? 2 : 3;
        }

        boolean lampLit(int lv, int cx, int cz) {
            return h(0x9510 + lv, cx, cz) < 0.6;
        }

        public boolean running(int lv, int cx, int cz, int unit) {
            return h(0x9520 + lv, cx * 4 + unit, cz) < 0.45;
        }

        public Part part(int lx, int off, int lz, boolean ringDoor) {
            if (lx == 0 || lz == 0) {
                return Part.OFFICE;
            }
            if (off >= h - 1) {
                return Part.CEIL;
            }
            int lv = off / 6;
            int r = off % 6;
            if (ringSide(lx, lz) >= 0) {
                return r >= 1 && r <= 3 && ringDoor ? Part.AIR : r == 0 ? Part.FLOOR : r == 5 ? Part.CEIL : Part.WALL;
            }
            int cx = lx / C;
            int cz = lz / C;
            int ax = lx % C;
            int az = lz % C;
            int hall = hallOf(cx, cz);
            if (hall >= 0 && off < hallLevels * 6) {
                Part w = wall(lv, r, cx, cz, ax, az, hall);
                return w != null ? w : hall(hall, lx, off, lz);
            }
            Part w = wall(lv, r, cx, cz, ax, az, -1);
            if (w != null) {
                return w;
            }
            // the staircase from the level below ends here: an opening with railings
            if (type(lv - 1, cx, cz) == STAIR && lv - 1 >= 0) {
                if (r == 0 && ax >= 4 && ax <= 8 && az >= 7 && az <= 8) {
                    return Part.AIR;
                }
                if (r == 1 && ((ax >= 4 && ax <= 8 && (az == 6 || az == 9)) || (ax == 3 && az >= 7 && az <= 8))) {
                    return Part.RAIL;
                }
            }
            int t = type(lv, cx, cz);
            if (t == STAIR && ax >= 4 && ax <= 9 && az >= 7 && az <= 8 && r >= 1) {
                int k = ax - 4;
                if (k == 5) {
                    return Part.SUPPORT;
                }
                return r == k + 1 ? Part.STAIR : r <= k ? Part.SUPPORT : Part.AIR;
            }
            if (r == 0) {
                return t == PIPES ? Part.GRATE : Part.FLOOR;
            }
            if (r == 5) {
                if (type(lv + 1, cx, cz) == PIPES && lv + 1 < storeys && !(hallOf(cx, cz) >= 0 && lv + 1 < hallLevels)) {
                    return Part.GRATE;
                }
                if (ax == 5 && az == 11) {
                    return lampLit(lv, cx, cz) ? Part.LAMP : Part.LAMP_BROKEN;
                }
                return t == GEN && ax == 8 && az == 8 ? Part.PIPE_Y : Part.CEIL;
            }
            if (r == 4 && (az == 3 || az == 13) && ax >= 1) {
                return Part.PIPE_X;                                         // two main lines run through every room
            }
            if (r == 3 && ax == 1 && az >= 1 && t != STAIR) {
                return Part.PIPE_Z;                                         // and one along the west wall
            }
            if ((ax == 1 || ax == 15) && (az == 1 || az == 15) && t != CONTROL) {
                return Part.PIPE_Y;                                         // risers in the corners
            }
            return switch (t) {
                case PIPES -> pipes(ax, r, az);
                case CONTROL -> control(ax, r, az);
                case STORAGE -> storage(lv, cx, cz, ax, r, az);
                case GEN -> generator(ax, r, az, 8, 8, 1, 3, running(lv, cx, cz, 0));
                case TANKS -> tanks(ax, r, az);
                case FOUNDRY -> foundry(ax, r, az);
                case LOCKERS -> lockers(ax, r, az);
                default -> Part.AIR;
            };
        }

        /** 0.36.0: two round copper tanks. */
        static Part tanks(int ax, int r, int az) {
            for (int tx : new int[]{5, 11}) {
                int dx = ax - tx;
                int dz = az - 8;
                if (dx * dx + dz * dz <= 5) {
                    return r == 4 ? Part.TANK_TOP : Part.TANK;
                }
            }
            return r == 4 && az == 8 && ax >= 7 && ax <= 9 ? Part.PIPE_X : Part.AIR;
        }

        /** 0.36.0: a row of blast furnaces, glowing vats and coal heaps. */
        static Part foundry(int ax, int r, int az) {
            if (az == 2 && ax >= 3 && ax <= 12) {
                return r == 1 ? Part.FURNACE : r == 2 && ax % 3 == 0 ? Part.PIPE_Y : Part.AIR;
            }
            if (r == 1 && az == 9 && (ax == 5 || ax == 10)) {
                return Part.VAT;
            }
            if (ax >= 12 && ax <= 13 && az >= 11 && az <= 13 && (r == 1 || (r == 2 && ax == 13 && az == 13))) {
                return Part.COAL;
            }
            return Part.AIR;
        }

        /** 0.36.0: a changing room with rows of lockers and benches. */
        static Part lockers(int ax, int r, int az) {
            if (az >= 3 && az <= 12 && r <= 2 && (ax == 3 || ax == 12) && az != 8) {
                return ax == 3 ? Part.LOCKER_E : Part.LOCKER;
            }
            if (r == 1 && ax == 7 && az >= 4 && az <= 11 && az != 8) {
                return Part.BENCH;
            }
            return Part.AIR;
        }

        /** Walls between rooms (west and north side of each cell), with doors; null inside a room. */
        Part wall(int lv, int r, int cx, int cz, int ax, int az, int hall) {
            boolean wx = ax == 0;
            boolean wz = az == 0;
            if (hall >= 0) {                                                // no walls inside a hall
                wx &= cx == halls[hall][0];
                wz &= cz == halls[hall][1];
            }
            if (!wx && !wz) {
                return null;
            }
            if (r == 0) {
                return hall >= 0 && lv > 0 ? Part.WALL : Part.FLOOR;
            }
            if (r == 5) {
                return hall >= 0 ? Part.WALL : Part.CEIL;
            }
            if (wx && wz) {
                return Part.WALL;
            }
            int d = door(lv, wx, cx, cz);
            if (d != 0 && r <= 2 && (wx ? az == 8 : ax == 8)) {
                if (d == 1) {
                    return Part.AIR;
                }
                boolean open = d == 3;
                if (wx) {
                    return r == 1 ? (open ? Part.DOOR_X_OPEN_LO : Part.DOOR_X_LO) : (open ? Part.DOOR_X_OPEN_HI : Part.DOOR_X_HI);
                }
                return r == 1 ? (open ? Part.DOOR_Z_OPEN_LO : Part.DOOR_Z_LO) : (open ? Part.DOOR_Z_OPEN_HI : Part.DOOR_Z_HI);
            }
            return Part.WALL;
        }

        static Part pipes(int ax, int r, int az) {
            if ((ax == 2 || ax == 13) && (az == 2 || az == 13)) {
                return Part.PIPE_Y;
            }
            if (r == 3 && az == 12 && ax >= 1) {
                return Part.PIPE_X;
            }
            if (r == 4 && ax == 12 && az >= 1) {
                return Part.PIPE_Z;
            }
            if (r == 2 && az == 6 && ax >= 1) {
                return Part.PIPE_X;
            }
            return Part.AIR;
        }

        static Part control(int ax, int r, int az) {
            if (az == 1 && ax >= 3 && ax <= 12) {
                return r == 1 ? Part.PANEL : r == 2 && ax >= 5 && ax <= 10 ? Part.CCTV : Part.AIR;
            }
            if (r == 1 && az == 3 && (ax == 5 || ax == 10)) {
                return Part.CHAIR;
            }
            if (ax == 14 && az >= 4 && az <= 6 && r <= 2) {
                return Part.LOCKER;
            }
            return Part.AIR;
        }

        Part storage(int lv, int cx, int cz, int ax, int r, int az) {
            if (ax % 4 == 2 && az % 4 == 2 && ax >= 2 && ax <= 14 && az >= 2 && az <= 14 && !(ax == 2 && az == 14)) {
                double u = h(0x9530 + lv, cx * 16 + ax, cz * 16 + az);
                if (r == 1 && u < 0.8) {
                    return Part.CRATE;
                }
                if (r == 2 && u < 0.35) {
                    return Part.CRATE;
                }
            }
            return Part.AIR;
        }

        /** A generator block of half size {@code hs} and height {@code ht}, centred on (cx, cz). */
        static Part generator(int ax, int r, int az, int cx, int cz, int hs, int ht, boolean running) {
            int dx = Math.abs(ax - cx);
            int dz = Math.abs(az - cz);
            if (dx > hs || dz > hs) {
                return Part.AIR;
            }
            if (r < ht) {
                return Part.CASING;
            }
            if (r == ht) {
                return dx == 0 && dz == 0 ? (running ? Part.CORE : Part.CORE_DEAD) : dx <= 1 && dz <= 1 ? Part.CASING : Part.AIR;
            }
            return dx == 0 && dz == 0 ? Part.PIPE_Y : Part.AIR;
        }

        Part hall(int i, int lx, int off, int lz) {
            int hx = lx - halls[i][0] * C;
            int hz = lz - halls[i][1] * C;
            int top = hallLevels * 6 - 1;
            if (off == 0) {
                return Part.FLOOR;
            }
            if (off == top) {
                return hx % 8 == 4 && hz % 8 == 4 ? (h(0x9540, i, hx * 32 + hz) < 0.6 ? Part.LAMP : Part.LAMP_BROKEN) : Part.CEIL;
            }
            boolean edge = hx <= 2 || hz <= 2 || hx >= 30 || hz >= 30;
            if (off % 6 == 0) {
                return edge ? Part.GRATE : Part.AIR;                        // catwalks on levels 1 and 2
            }
            if (off > 6 && off % 6 == 1 && !edge && (hx == 3 || hz == 3 || hx == 29 || hz == 29)) {
                return Part.RAIL;
            }
            if (off == top - 2 && hz == 16 && hx >= 3 && hx <= 29) {
                return Part.PIPE_X;
            }
            int[][] units = {{8, 8}, {8, 23}, {23, 8}, {23, 23}};
            for (int u = 0; u < 4; u++) {
                int dx = Math.abs(hx - units[u][0]);
                int dz = Math.abs(hz - units[u][1]);
                if (dx > 2 || dz > 2) {
                    continue;
                }
                if (off <= 4) {
                    return Part.CASING;
                }
                if (off == 5) {
                    return dx == 0 && dz == 0 ? (running(0, halls[i][0], halls[i][1], u + i * 4) ? Part.CORE : Part.CORE_DEAD)
                            : dx <= 1 && dz <= 1 ? Part.CASING : Part.AIR;
                }
                if ((dx == 0 && dz == 0) || (dx == 2 && dz == 2 && (hx + hz) % 2 == 0)) {
                    return off < top ? Part.PIPE_Y : Part.CEIL;
                }
                return Part.AIR;
            }
            return Part.AIR;
        }
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(16);

    public Industrial(OfficePlanner p) {
        this.p = p;
    }

    public Site site(int rx, int rz) {
        if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.INDUSTRIAL) {
            return null;
        }
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    private Site build(int rx, int rz) {
        boolean modern = p.cfg.modern();
        int n = modern ? STOREYS : LEGACY_STOREYS;
        int[] range = Megastructures.storeyRange(p, n);
        if (range[1] <= range[0]) {
            return null;
        }
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9502, 0));
        int s0 = rng.range(range[0], range[1]);
        int[] a = {rng.range(1, 3), rng.range(1, 7)};
        int[] b = {rng.range(5, 7), rng.range(1, 7)};
        Site s = new Site(rx, rz, s0, p.floorY(s0), p.seed, new int[][]{a, b}, modern);
        for (int lv = 0; lv < n; lv++) {
            for (int cx = 0; cx < N; cx++) {
                for (int cz = 0; cz < N; cz++) {
                    if (s.hallOf(cx, cz) >= 0 && lv < s.hallLevels) {
                        s.type[lv][cx][cz] = HALL;
                        continue;
                    }
                    double u = rng.nextDouble();
                    if (modern) {
                        s.type[lv][cx][cz] = u < 0.24 ? PIPES : u < 0.36 ? CONTROL : u < 0.50 ? STORAGE : u < 0.61 ? GEN
                                : u < 0.71 ? TANKS : u < 0.80 ? FOUNDRY : u < 0.88 ? LOCKERS : EMPTY;
                    } else {
                        s.type[lv][cx][cz] = u < 0.30 ? PIPES : u < 0.47 ? CONTROL : u < 0.66 ? STORAGE : u < 0.80 ? GEN : EMPTY;
                    }
                }
            }
        }
        // three staircases per level, never in a hall cell and never right above the staircase below
        for (int lv = 0; lv < n - 1; lv++) {
            int placed = 0;
            for (int tries = 0; tries < 60 && placed < 3; tries++) {
                int cx = rng.range(1, N - 2);
                int cz = rng.range(1, N - 2);
                if (s.hallOf(cx, cz) >= 0 || s.type[lv][cx][cz] == STAIR || (lv > 0 && s.type[lv - 1][cx][cz] == STAIR)) {
                    continue;
                }
                s.type[lv][cx][cz] = STAIR;
                s.type[lv + 1][cx][cz] = EMPTY;                           // keep a clear landing around the opening
                placed++;
            }
        }
        return s;
    }

    public boolean overlaps(int rx, int rz, Room r) {
        if (r == null) {
            return false;
        }
        Site s = site(rx, rz);
        return s != null && Megastructures.overlapsBand(r, s.s0, s.storeys);
    }

    public boolean contains(int x, int y, int z) {
        return at(x, y, z) != null;
    }

    public Site at(int x, int y, int z) {
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        Site s = site(rx, rz);
        if (s == null || !s.inBand(y)) {
            return null;
        }
        return x - rx * p.regionBlocks != 0 && z - rz * p.regionBlocks != 0 ? s : null;
    }
}
