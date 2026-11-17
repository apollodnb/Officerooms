package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;
import de.officerooms.special.SpecialRooms;
import de.officerooms.special.SpecialType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The Maze (0.8.0): the classic Level 0 of the Backrooms community, as rare as the Poolrooms.
 *
 * <p>0.34.0: three maze levels on top of each other (four office storeys). Each level is its own labyrinth with
 * Level 0's palette and features (see 0.33.0); carpeted ramps lead up a level, pit fields (a hall whose floor is a
 * grid of deep square pits) drop into a grid of chambers on the level below, holes drop through. 3–5 exits on the
 * lowest level lead back into the offices. The high-security vault sits on a random level at a random spot.</p>
 *
 * <p>Heights relative to {@code yBase} (wool floor layer of storey s0): level L starts at 7·L; per level 0 carpet,
 * 1–5 walk space, 6 drop ceiling with fluorescent panels; 21–23 solid.</p>
 */
public final class Maze {

    public static final int LEVELS = 3;
    public static final int LP = 7;
    public static final int STOREYS = 4;
    public static final int H = 24;
    public static final int CEIL = 6;
    static final int W = 160;
    /** Cells of 6 blocks (1 wall + 5 open), 26 × 26 cells between lx 2 and 158. */
    static final int CELL = 6;
    static final int NC = 26;
    static final int ORIGIN = 2;
    /** Pit field: 4 × 4 cells (24 blocks), pits on a 4-block grid. */
    static final int PF = 4;

    public enum Part {
        AIR, OFFICE, WALLPAPER, CARPET, CEILING, LIGHT_ON, LIGHT_OFF, LIGHT_SIDE, SECURITY_WALL, VAULT_FLOOR,
        FILL, WALL_WORN, WALL_ARROW, WALL_LIGHT, WINDOW, CARPET_STAINED, CARPET_WET, CEIL_STAINED, CEIL_VOID, VENT, FLICKER, HOLE,
        STEP_E, STEP_S, DOOR_LOWER, DOOR_UPPER, FRAME, OUTLET_E, OUTLET_W, OUTLET_S, OUTLET_N, RAMP_E, RAMP_S
    }

    /** Odd cell kinds (0.33.0). */
    static final int PILLAR = 1, LOW = 2, HANG = 3, STEPS = 4, DOOR = 5, SEALED = 6, PIT = 7, WET = 8, RAISED = 9, PLATFORM = 10;
    /** Ramp path inside a cell (cell-local ax, az), seven steps up one level. */
    static final int[][] RAMP = {{1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {5, 2}, {5, 3}};

    public record Exit(int lx, int lz, boolean west) {
    }

    public static final class Vault {
        /** Region-local door position (lower half at walk level) and facing (0 E, 1 S, 2 W, 3 N). */
        public final int doorX;
        public final int doorZ;
        public final int facing = 1;
        /** Security level of the door (1–3). */
        public final int level;
        /** 0.34.0: maze level (0–2) the vault stands on. */
        public final int floor;
        public final int chestX;
        public final int chestZ;
        /** Wall sign outside the door. */
        public final int signX;
        public final int signZ;
        final int cx;
        final int cz;
        final int x0;
        final int z0;
        final int x1;
        final int z1;

        Vault(int vx, int vz, int level, int floor) {
            this.cx = vx;
            this.cz = vz;
            this.x0 = ORIGIN + vx * CELL;
            this.z0 = ORIGIN + vz * CELL;
            this.x1 = x0 + 2 * CELL;
            this.z1 = z0 + 2 * CELL;
            this.doorX = x0 + 3;
            this.doorZ = z0;
            this.level = level;
            this.floor = floor;
            this.chestX = x0 + CELL;
            this.chestZ = z0 + CELL;
            this.signX = doorX + 1;
            this.signZ = doorZ - 1;
        }

        public boolean inside(int lx, int lz) {
            return lx > x0 && lx < x1 && lz > z0 && lz < z1;
        }

        boolean shell(int lx, int lz) {
            return lx >= x0 && lx <= x1 && lz >= z0 && lz <= z1;
        }

        /** Absolute offset of the vault's walk level above yBase. */
        public int walkOff() {
            return floor * LP + 1;
        }
    }

    public static final class Site {
        public final int rx;
        public final int rz;
        public final int s0;
        public final int yBase;
        final boolean[][] wall = new boolean[LEVELS][W * W];
        final byte[][] light = new byte[LEVELS][W * W];
        /** Pit field corner cell per level (on that level the floor is pits; the level below holds the chambers), or null. */
        final int[][] pits = new int[LEVELS][];
        /** Ramp cells per level (going up to the next level). */
        final List<int[]> ramps = new ArrayList<>();
        public final List<Exit> exits = new ArrayList<>();
        public Vault vault;
        long seed;

        Site(int rx, int rz, int s0, int yBase) {
            this.rx = rx;
            this.rz = rz;
            this.s0 = s0;
            this.yBase = yBase;
        }

        static int i(int lx, int lz) {
            return lz * W + lx;
        }

        public boolean inBand(int y) {
            return y >= yBase && y < yBase + H;
        }

        public boolean isExit(int lx, int lz) {
            for (Exit e : exits) {
                if (e.lx == lx && e.lz == lz) {
                    return true;
                }
            }
            return false;
        }

        /** Maze level of an absolute y (clamped). */
        public int levelOf(int y) {
            return Math.max(0, Math.min(LEVELS - 1, (y - yBase) / LP));
        }

        /** Open floor (walkable, not the vault) on the lowest level. */
        public boolean openFloor(int lx, int lz) {
            return openFloor(0, lx, lz);
        }

        public boolean openFloor(int lv, int lx, int lz) {
            return lx > 0 && lz > 0 && lx < W && lz < W && !wall[lv][i(lx, lz)]
                    && (vault == null || vault.floor != lv || !vault.shell(lx, lz)) && !pitHole(lv, lx, lz) && !chamberWall(lv, lx, lz);
        }

        double h(int salt, int a, int b) {
            return Hashing.unit(Hashing.hash(seed, rx, rz, salt, a * 4096 + b));
        }

        static boolean inPF(int[] pf, int lx, int lz) {
            if (pf == null) {
                return false;
            }
            int x0 = ORIGIN + pf[0] * CELL;
            int z0 = ORIGIN + pf[1] * CELL;
            return lx >= x0 && lx <= x0 + PF * CELL && lz >= z0 && lz <= z0 + PF * CELL;
        }

        /** A pit (open floor) on level lv's pit field. */
        boolean pitHole(int lv, int lx, int lz) {
            int[] pf = pits[lv];
            if (!inPF(pf, lx, lz)) {
                return false;
            }
            int a = lx - (ORIGIN + pf[0] * CELL);
            int b = lz - (ORIGIN + pf[1] * CELL);
            return a % 4 != 0 && b % 4 != 0;
        }

        /** The walls of the chamber grid on level lv (below the pit field of level lv + 1). */
        boolean chamberWall(int lv, int lx, int lz) {
            if (lv + 1 >= LEVELS || !inPF(pits[lv + 1], lx, lz)) {
                return false;
            }
            int[] pf = pits[lv + 1];
            int a = lx - (ORIGIN + pf[0] * CELL);
            int b = lz - (ORIGIN + pf[1] * CELL);
            return a % 4 == 0 || b % 4 == 0;
        }

        boolean chamberDoor(int lv, int lx, int lz) {
            int[] pf = pits[lv + 1];
            int a = lx - (ORIGIN + pf[0] * CELL);
            int b = lz - (ORIGIN + pf[1] * CELL);
            return (a % 4 == 0 && b % 4 == 2) || (b % 4 == 0 && a % 4 == 2);
        }

        /** Step index (0–6) of a ramp going up from level lv at this block, or -1. */
        int rampStep(int lv, int lx, int lz) {
            for (int[] r : ramps) {
                if (r[0] != lv) {
                    continue;
                }
                int ax = lx - (ORIGIN + r[1] * CELL);
                int az = lz - (ORIGIN + r[2] * CELL);
                for (int k = 0; k < RAMP.length; k++) {
                    if (RAMP[k][0] == ax && RAMP[k][1] == az) {
                        return k;
                    }
                }
            }
            return -1;
        }

        /** Odd kind of a maze cell (0 = plain); never in or next to the vault, pit fields or ramps. */
        int odd(int lv, int cx, int cz) {
            if (vault != null && vault.floor == lv && cx >= vault.cx - 1 && cx <= vault.cx + 2 && cz >= vault.cz - 1 && cz <= vault.cz + 2) {
                return 0;
            }
            for (int[] r : ramps) {
                if ((r[0] == lv || r[0] + 1 == lv) && r[1] == cx && r[2] == cz) {
                    return 0;
                }
            }
            for (int d = 0; d <= 1; d++) {
                int[] pf = lv + d < LEVELS ? pits[lv + d] : null;
                if (pf != null && cx >= pf[0] - 1 && cx <= pf[0] + PF && cz >= pf[1] - 1 && cz <= pf[1] + PF) {
                    return 0;
                }
            }
            double u = h(0x9301 + lv * 31, cx, cz);
            return u < 0.05 ? PILLAR : u < 0.09 ? LOW : u < 0.13 ? HANG : u < 0.155 ? STEPS : u < 0.175 ? DOOR : u < 0.19 ? SEALED
                    : u < 0.215 ? PIT : u < 0.27 ? WET : u < 0.29 ? RAISED : u < 0.33 ? PLATFORM : 0;
        }

        public Part part(int lx, int off, int lz) {
            if (lx == 0 || lz == 0) {
                if (isExit(lx, lz) && off >= 1 && off <= 3) {
                    return Part.AIR;
                }
                return Part.OFFICE;
            }
            if (off >= LEVELS * LP) {
                return Part.FILL;
            }
            int lv = off / LP;
            int loff = off % LP;
            if (vault != null && vault.floor == lv && vault.shell(lx, lz)) {
                if (loff == 0 || loff >= CEIL) {
                    return Part.VAULT_FLOOR;
                }
                if (!vault.inside(lx, lz)) {
                    if (lx == vault.doorX && lz == vault.doorZ && (loff == 1 || loff == 2)) {
                        return Part.AIR; // the security door is placed by the decorator
                    }
                    return Part.SECURITY_WALL;
                }
                if (loff == CEIL - 1 && light[lv][i(lx, lz)] != 0) {
                    return light[lv][i(lx, lz)] == 3 ? Part.LIGHT_SIDE : Part.LIGHT_ON;
                }
                return Part.AIR;
            }
            return mazePart(lv, lx, loff, lz);
        }

        private Part mazePart(int lv, int lx, int loff, int lz) {
            // ramps: up from this level, and the opening they come through from the level below
            int up = rampStep(lv, lx, lz);
            if (up >= 0) {
                if (loff >= 1 && loff <= up) {
                    return Part.CARPET;
                }
                if (loff == up + 1 && up < 6) {
                    return up < 5 ? Part.RAMP_E : Part.RAMP_S;              // 0.38.1: corner step faces east
                }
                if (loff == CEIL && up >= 2) {                                  // 0.38.1: from the third step on (was: fourth)
                    return up == 6 ? Part.CARPET : Part.AIR;                         // headroom through the ceiling
                }
                if (loff >= 1 && loff < CEIL) {
                    return Part.AIR;
                }
            }
            if (loff == CEIL) {                                                 // 0.38.1: ceiling beside the steep part of a ramp
                int n1 = rampStep(lv, lx, lz - 1);
                int n2 = rampStep(lv, lx, lz + 1);
                if (n1 >= 2 && n1 <= 4 || n2 >= 2 && n2 <= 4) {
                    return Part.AIR;
                }
            }
            if (lv > 0 && loff == 0) {
                int below = rampStep(lv - 1, lx, lz);
                if (below >= 3 && below <= 5) {
                    return Part.AIR;
                }
                if (below == 6) {
                    return Part.RAMP_S;                                               // the last step lies in this floor
                }
            }
            // pit field on this level, chamber grid below it
            if (pitHole(lv, lx, lz) && loff == 0) {
                return Part.AIR;
            }
            if (lv + 1 < LEVELS && inPF(pits[lv + 1], lx, lz)) {
                if (loff == 0) {
                    return Part.CARPET;
                }
                if (chamberWall(lv, lx, lz)) {
                    return chamberDoor(lv, lx, lz) && loff <= 2 ? Part.AIR : Part.WALLPAPER;
                }
                return Part.AIR;                                                      // open up into the pit above
            }
            boolean isWall = wall[lv][i(lx, lz)];
            if (inPF(pits[lv], lx, lz)) {
                int a = lx - (ORIGIN + pits[lv][0] * CELL);
                int b = lz - (ORIGIN + pits[lv][1] * CELL);
                if (a > 0 && a < PF * CELL && b > 0 && b < PF * CELL) {
                    isWall = false;                                                   // no pillars inside the pit field
                }
            }
            if (inPF(pits[lv], lx, lz) && !isWall) {
                if (loff == 0) {
                    return Part.CARPET;                                               // the narrow walkways between the pits
                }
                if (loff == CEIL) {
                    int a = lx - (ORIGIN + pits[lv][0] * CELL);
                    int b = lz - (ORIGIN + pits[lv][1] * CELL);
                    return a % 8 == 6 && b % 8 == 6 ? Part.LIGHT_ON : Part.CEILING;
                }
                return Part.AIR;
            }
            int ox = lx - ORIGIN;
            int oz = lz - ORIGIN;
            boolean inGrid = ox > 0 && oz > 0 && ox < NC * CELL && oz < NC * CELL;
            int cx = inGrid ? ox / CELL : -1;
            int cz = inGrid ? oz / CELL : -1;
            int ax = inGrid ? ox % CELL : -1;
            int az = inGrid ? oz % CELL : -1;
            int kind = inGrid && ax > 0 && az > 0 ? odd(lv, cx, cz) : 0;
            if (loff == 0) {
                if (kind == PIT && ax == 3 && (az == 3 || (h(0x9302, cx + lv * 64, cz) < 0.5 && az == 2))) {
                    return Part.HOLE;
                }
                if (kind == WET && (ax - 3) * (ax - 3) + (az - 3) * (az - 3) <= 3) {
                    return Part.CARPET_WET;
                }
                double v = h(0x9303 + lv, lx, lz);
                return v < 0.05 ? Part.CARPET_STAINED : v < 0.058 ? Part.CARPET_WET : Part.CARPET;
            }
            if (loff == CEIL) {
                if (lv + 1 < LEVELS) {
                    // a hole in the floor of the level above opens this ceiling too
                    int kindUp = inGrid && ax > 0 && az > 0 ? odd(lv + 1, cx, cz) : 0;
                    if (kindUp == PIT && ax == 3 && (az == 3 || (h(0x9302, cx + (lv + 1) * 64, cz) < 0.5 && az == 2))) {
                        return Part.AIR;
                    }
                }
                if (!isWall) {
                    byte l = light[lv][i(lx, lz)];
                    if (l == 1) {
                        return h(0x9304, lx, lz + lv * 200) < 0.06 ? Part.FLICKER : Part.LIGHT_ON;
                    }
                    if (l == 2) {
                        return Part.LIGHT_OFF;
                    }
                    if (l == 3) {
                        return Part.LIGHT_SIDE;
                    }
                }
                double v = h(0x9305 + lv, lx, lz);
                return v < 0.015 ? Part.CEIL_VOID : v < 0.03 ? Part.VENT : v < 0.07 ? Part.CEIL_STAINED : Part.CEILING;
            }
            if (isWall) {
                double v = h(0x9306 + lv, lx * 8 + loff, lz);
                if (loff == 3 && v < 0.012) {
                    return Part.WALL_LIGHT;
                }
                if ((loff == 2 || loff == 3) && h(0x9307 + lv, lx, lz) < 0.006) {
                    return Part.WINDOW;
                }
                return v < 0.05 ? Part.WALL_WORN : v < 0.055 && loff == 2 ? Part.WALL_ARROW : Part.WALLPAPER;
            }
            switch (kind) {
                case PILLAR -> {
                    if (ax == 3 && az == 3) {
                        return Part.WALLPAPER;
                    }
                }
                case LOW -> {
                    if (loff >= 4) {
                        return loff == 4 && ax == 3 && az == 3 ? Part.LIGHT_ON : Part.CEILING;
                    }
                }
                case HANG -> {
                    if (loff >= 3 && (h(0x9308, cx + lv * 64, cz) < 0.5 ? az : ax) == 3) {
                        return Part.WALLPAPER;
                    }
                }
                case STEPS -> {
                    if (az == 1 && loff < ax) {
                        return loff == ax - 1 ? Part.STEP_E : Part.WALLPAPER;
                    }
                }
                case DOOR -> {
                    if (ax == 3 && (az == 2 || az == 4) && loff <= 3) {
                        return Part.FRAME;
                    }
                    if (ax == 3 && az == 3) {
                        return loff == 1 ? Part.DOOR_LOWER : loff == 2 ? Part.DOOR_UPPER : loff == 3 ? Part.FRAME : Part.AIR;
                    }
                }
                case SEALED -> {
                    if (ax >= 2 && ax <= 4 && az >= 2 && az <= 4 && !(ax == 3 && az == 3)) {
                        return Part.WALLPAPER;
                    }
                }
                case PLATFORM -> {
                    if (az == 2 && loff == 1) {
                        return Part.RAMP_S;
                    }
                    if (az == 3) {
                        return loff == 1 ? Part.CARPET : loff == 2 ? Part.RAMP_S : Part.AIR;
                    }
                    if (az >= 4 && loff <= 2) {
                        return Part.CARPET;
                    }
                }
                case RAISED -> {
                    if (loff == 1 && az >= 3) {
                        return Part.CARPET;
                    }
                }
                default -> {
                }
            }
            if (loff == 1 && h(0x9309 + lv, lx, lz) < 0.012) {
                boolean[] wl = wall[lv];
                if (lx > 0 && wl[i(lx - 1, lz)]) return Part.OUTLET_E;
                if (lx < W - 1 && wl[i(lx + 1, lz)]) return Part.OUTLET_W;
                if (lz > 0 && wl[i(lx, lz - 1)]) return Part.OUTLET_S;
                if (lz < W - 1 && wl[i(lx, lz + 1)]) return Part.OUTLET_N;
            }
            return Part.AIR;
        }
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(16);

    public Maze(OfficePlanner p) {
        this.p = p;
    }

    public Site site(int rx, int rz) {
        if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.MAZE) {
            return null;
        }
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    public boolean overlaps(int rx, int rz, Room r) {
        if (r == null) {
            return false;
        }
        Site s = site(rx, rz);
        return s != null && Megastructures.overlapsBand(r, s.s0, STOREYS);
    }

    /** Whether the position lies in the maze band (including the carved exits in the office wall line). */
    public boolean contains(int x, int y, int z) {
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        Site s = site(rx, rz);
        if (s == null || !s.inBand(y)) {
            return false;
        }
        int lx = x - rx * p.regionBlocks;
        int lz = z - rz * p.regionBlocks;
        if (lx == 0 || lz == 0) {
            return s.isExit(lx, lz);
        }
        return true;
    }

    /** Site whose band contains the position (for the mining fatigue check), otherwise null. */
    public Site at(int x, int y, int z) {
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        Site s = site(rx, rz);
        return s != null && s.inBand(y) ? s : null;
    }

    private static boolean overlapsCells(int ax, int az, int aw, int bx, int bz, int bw) {
        return ax < bx + bw && bx < ax + aw && az < bz + bw && bz < az + aw;
    }

    private Site build(int rx, int rz) {
        int[] range = Megastructures.storeyRange(p, STOREYS);
        if (range[1] <= range[0]) {
            return null;
        }
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9202, 0));
        int s0 = rng.range(range[0], range[1]);
        Site s = new Site(rx, rz, s0, p.floorY(s0));
        s.seed = p.seed;

        // the vault: a random level, a random spot
        int vf = rng.nextInt(LEVELS);
        int vx = rng.range(2, NC - 4);
        int vz = rng.range(2, NC - 4);
        double lvv = rng.nextDouble();
        s.vault = new Vault(vx, vz, lvv < 0.6 ? 1 : lvv < 0.9 ? 2 : 3, vf);
        // pit fields on the upper levels (the level below gets the chamber grid)
        for (int lv = 1; lv < LEVELS; lv++) {
            if (!rng.chance(0.8)) {
                continue;
            }
            for (int a = 0; a < 20; a++) {
                int px = rng.range(2, NC - PF - 2);
                int pz = rng.range(2, NC - PF - 2);
                boolean clash = (vf == lv || vf == lv - 1) && overlapsCells(px - 1, pz - 1, PF + 2, vx, vz, 2);
                if (lv == 2 && s.pits[1] != null && overlapsCells(px - 1, pz - 1, PF + 2, s.pits[1][0], s.pits[1][1], PF)) {
                    clash = true;                                              // level 1 can't be pits and chambers at once
                }
                if (!clash) {
                    s.pits[lv] = new int[]{px, pz};
                    break;
                }
            }
        }
        // ramps up: three per level, in cells free on both levels
        for (int lv = 0; lv < LEVELS - 1; lv++) {
            int n = 0;
            for (int a = 0; a < 60 && n < 3; a++) {
                int cx = rng.range(1, NC - 2);
                int cz = rng.range(1, NC - 2);
                boolean bad = false;
                for (int d = 0; d <= 1 && !bad; d++) {
                    int l = lv + d;
                    if (s.vault.floor == l && overlapsCells(cx - 1, cz - 1, 3, vx, vz, 2)) {
                        bad = true;
                    }
                    for (int e = 0; e <= 1 && !bad; e++) {
                        int[] pf = l + e < LEVELS ? s.pits[l + e] : null;
                        if (pf != null && overlapsCells(cx - 1, cz - 1, 3, pf[0], pf[1], PF)) {
                            bad = true;
                        }
                    }
                }
                for (int[] r : s.ramps) {
                    if (Math.abs(r[1] - cx) + Math.abs(r[2] - cz) < 6) {
                        bad = true;
                    }
                }
                if (!bad) {
                    s.ramps.add(new int[]{lv, cx, cz});
                    n++;
                }
            }
        }
        for (int lv = 0; lv < LEVELS; lv++) {
            buildLevel(s, lv, new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9210 + lv, 0)));
        }
        exits(s, rng);
        return s;
    }

    /** One labyrinth (spanning tree plus loops and halls) for a level; the vault and the chamber grid are left out. */
    private void buildLevel(Site s, int lv, Hashing.Rng rng) {
        boolean[][] blocked = new boolean[NC][NC];
        if (s.vault.floor == lv) {
            for (int a = 0; a < 2; a++) {
                for (int b = 0; b < 2; b++) {
                    blocked[s.vault.cx + a][s.vault.cz + b] = true;
                }
            }
        }
        int[] under = lv + 1 < LEVELS ? s.pits[lv + 1] : null;     // chambers here
        if (under != null) {
            for (int a = 0; a < PF; a++) {
                for (int b = 0; b < PF; b++) {
                    blocked[under[0] + a][under[1] + b] = true;
                }
            }
        }
        boolean[][] hWall = new boolean[NC][NC];
        boolean[][] vWall = new boolean[NC][NC];
        for (boolean[] a : hWall) {
            java.util.Arrays.fill(a, true);
        }
        for (boolean[] a : vWall) {
            java.util.Arrays.fill(a, true);
        }
        boolean[][] seen = new boolean[NC][NC];
        for (int a = 0; a < NC; a++) {
            for (int b = 0; b < NC; b++) {
                seen[a][b] = blocked[a][b];
            }
        }
        int[] stack = new int[NC * NC];
        int sp = 0;
        stack[sp++] = 0;
        seen[0][0] = true;
        int[] dx = {1, 0, -1, 0};
        int[] dz = {0, 1, 0, -1};
        while (sp > 0) {
            int cur = stack[sp - 1];
            int cx = cur % NC;
            int cz = cur / NC;
            int[] opts = new int[4];
            int n = 0;
            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d];
                int nz = cz + dz[d];
                if (nx >= 0 && nz >= 0 && nx < NC && nz < NC && !seen[nx][nz]) {
                    opts[n++] = d;
                }
            }
            if (n == 0) {
                sp--;
                continue;
            }
            int d = opts[rng.nextInt(n)];
            int nx = cx + dx[d];
            int nz = cz + dz[d];
            openWall(hWall, vWall, cx, cz, d);
            seen[nx][nz] = true;
            stack[sp++] = nz * NC + nx;
        }
        for (int cz = 0; cz < NC; cz++) {
            for (int cx = 0; cx < NC; cx++) {
                if (blocked[cx][cz]) {
                    continue;
                }
                if (cx + 1 < NC && !blocked[cx + 1][cz] && rng.chance(0.30)) {
                    hWall[cx][cz] = false;
                }
                if (cz + 1 < NC && !blocked[cx][cz + 1] && rng.chance(0.30)) {
                    vWall[cx][cz] = false;
                }
            }
        }
        int halls = rng.range(3, 5);
        for (int h = 0; h < halls; h++) {
            int hx = rng.range(0, NC - 3);
            int hz = rng.range(0, NC - 3);
            for (int a = 0; a < 3; a++) {
                for (int b = 0; b < 3; b++) {
                    int cx = hx + a;
                    int cz = hz + b;
                    if (blocked[cx][cz]) {
                        continue;
                    }
                    if (a < 2 && !blocked[cx + 1][cz]) {
                        hWall[cx][cz] = false;
                    }
                    if (b < 2 && !blocked[cx][cz + 1]) {
                        vWall[cx][cz] = false;
                    }
                }
            }
        }
        int[] pf = s.pits[lv];                                     // the pit field is one open hall
        if (pf != null) {
            for (int a = 0; a < PF; a++) {
                for (int b = 0; b < PF; b++) {
                    if (a < PF - 1) {
                        hWall[pf[0] + a][pf[1] + b] = false;
                    }
                    if (b < PF - 1) {
                        vWall[pf[0] + a][pf[1] + b] = false;
                    }
                }
            }
        }
        boolean[] w = s.wall[lv];
        for (int lz = 1; lz < W; lz++) {
            for (int lx = 1; lx < W; lx++) {
                w[Site.i(lx, lz)] = rasterWall(hWall, vWall, lx, lz, p.seed + lv * 7919L, s);
            }
        }
        byte[] l = s.light[lv];
        for (int cz = 0; cz < NC; cz++) {
            for (int cx = 0; cx < NC; cx++) {
                int mx = ORIGIN + cx * CELL + 3;
                int mz = ORIGIN + cz * CELL + 3;
                byte centre = rng.chance(0.12) ? (byte) 2 : (byte) 1;
                l[Site.i(mx, mz)] = centre;
                l[Site.i(mx - 1, mz)] = 3;
                l[Site.i(mx + 1, mz)] = 3;
            }
        }
    }

    private static void openWall(boolean[][] hWall, boolean[][] vWall, int cx, int cz, int d) {
        switch (d) {
            case 0 -> hWall[cx][cz] = false;
            case 1 -> vWall[cx][cz] = false;
            case 2 -> hWall[cx - 1][cz] = false;
            default -> vWall[cx][cz - 1] = false;
        }
    }

    private static boolean rasterWall(boolean[][] hWall, boolean[][] vWall, int lx, int lz, long seed, Site s) {
        if (lx <= ORIGIN || lz <= ORIGIN || lx >= ORIGIN + NC * CELL || lz >= ORIGIN + NC * CELL) {
            return true; // outer boundary (lx 1..2 and 158..159)
        }
        int ox = lx - ORIGIN;
        int oz = lz - ORIGIN;
        boolean lineX = ox % CELL == 0;
        boolean lineZ = oz % CELL == 0;
        if (!lineX && !lineZ) {
            return false;
        }
        int cx = ox / CELL;
        int cz = oz / CELL;
        if (lineX && lineZ) {
            int kx = ox / CELL;
            int kz = oz / CELL;
            boolean n = kz - 1 >= 0 && hWall[kx - 1][kz - 1];
            boolean so = kz < NC && hWall[kx - 1][kz];
            boolean w = kx - 1 >= 0 && vWall[kx - 1][kz - 1];
            boolean e = kx < NC && vWall[kx][kz - 1];
            if (n || so || w || e) {
                return true;
            }
            long h = Hashing.hash(seed, s.rx, s.rz, lz * W + lx, 0x9203);
            return Hashing.unit(h) < 0.45;
        }
        if (lineX) {
            return hWall[cx - 1][cz];
        }
        return vWall[cx][cz - 1];
    }

    private void exits(Site s, Hashing.Rng rng) {
        List<Exit> cands = new ArrayList<>();
        int ru = p.ru;
        int unit = p.unit;
        for (int side = 0; side < 2; side++) {
            boolean west = side == 0;
            int nrx = west ? s.rx - 1 : s.rx;
            int nrz = west ? s.rz : s.rz - 1;
            // 0.8.3: a neighbouring megastructure only blocks the rooms inside its own storey band (checked
            // per room below via SpecialRooms). Exits only look west/north, so there is no planning cycle.
            RegionLayout nl = p.layout(nrx, nrz);
            for (int u = 2; u < ru - 2; u++) {
                Room r = west ? nl.room(s.s0, ru - 1, u) : nl.room(s.s0, u, ru - 1);
                if (r == null || r.sealed || r.bottom > s.s0 || r.top() < s.s0
                        || (r.kind != Room.Kind.ROOM && r.kind != Room.Kind.CORRIDOR && r.kind != Room.Kind.ATRIUM)) {
                    continue;
                }
                SpecialType st = SpecialRooms.of(p, nrx, nrz, r);
                if (st.sealsOpenings() || st == SpecialType.EXIT_ROOM || st.isMegastructure()) {
                    continue;
                }
                // a block of the cell interior that also hits an open maze row
                int along = -1;
                for (int k = 1; k <= 3; k++) {
                    int t = u * unit + k;
                    if ((t - ORIGIN) % CELL != 0 && t > ORIGIN && t < W - 2) {
                        along = t;
                        break;
                    }
                }
                if (along < 0) {
                    continue;
                }
                cands.add(west ? new Exit(0, along, true) : new Exit(along, 0, false));
            }
        }
        // prefer spread-out exits: shuffle, then take while keeping a distance
        for (int i = cands.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Exit t = cands.get(i);
            cands.set(i, cands.get(j));
            cands.set(j, t);
        }
        int want = rng.range(3, 5);
        for (Exit e : cands) {
            if (s.exits.size() >= want) {
                break;
            }
            boolean far = true;
            for (Exit o : s.exits) {
                if (Math.abs(o.lx - e.lx) + Math.abs(o.lz - e.lz) < 24) {
                    far = false;
                    break;
                }
            }
            if (far) {
                s.exits.add(e);
            }
        }
        // 0.33.0: very rarely both neighbours offer no room at all (e.g. two megastructures): cut plain openings
        if (cands.isEmpty()) {
            for (int k = 0; k < 3; k++) {
                int t = ORIGIN + 3 + (k * 7 + 4) * CELL;
                cands.add(k % 2 == 0 ? new Exit(0, t, true) : new Exit(t, 0, false));
            }
        }
        // 0.33.0: with the two-storey band some sites have few candidates: allow closer exits rather than fewer
        for (Exit e : cands) {
            if (s.exits.size() >= 3) {
                break;
            }
            boolean ok = !s.exits.contains(e);
            for (Exit o : s.exits) {
                if (Math.abs(o.lx - e.lx) + Math.abs(o.lz - e.lz) < 8) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                s.exits.add(e);
            }
        }
        for (Exit e : cands) {
            if (s.exits.size() >= want) {
                break;
            }
            if (!s.exits.contains(e)) {
                s.exits.add(e);
            }
        }
        // carve the passage through the maze boundary (lx/lz 1..2) up to the first open cell row
        for (Exit e : s.exits) {
            for (int d = 1; d <= ORIGIN; d++) {
                int lx = e.west ? d : e.lx;
                int lz = e.west ? e.lz : d;
                s.wall[0][Site.i(lx, lz)] = false;
            }
        }
    }

}
