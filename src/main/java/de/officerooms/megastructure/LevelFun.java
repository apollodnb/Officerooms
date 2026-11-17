package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.Room;
import java.util.Optional;

/**
 * 0.16.0: Level Fun, a megastructure as rare as the others. One storey of a region (160 × 160 blocks) becomes an
 * endless yellow party: a grid of 10 × 10 cells whose walls are only partly built (like Level 0), with party tables
 * and cake, balloon clusters, streamers, confetti, gift boxes and "=)" painted on the walls. Partygoers live here.
 * Heights: 0 floor, 1–4 walk space, 5 ceiling. Office doors of the storey open into it.
 */
public final class LevelFun {

    /** 0.36.0: new worlds get four storeys linked by staircases; old worlds keep one. */
    public static final int STOREYS = 4;
    public static final int H = STOREYS * 6;
    static final int W = 160;
    static final int C = 10;

    public static final int PLAIN = 0;
    public static final int TABLE = 1;
    public static final int BALLOONS = 2;
    public static final int GIFTS = 3;
    public static final int CONFETTI = 4;
    public static final int SMILE = 5;
    public static final int CASTLE = 6;
    // 0.36.0 (new worlds)
    public static final int STAIRS = 7;
    public static final int BALL_PIT = 8;
    public static final int DISCO = 9;
    public static final int BANQUET = 10;
    public static final int DARK = 11;

    public enum Part {
        AIR, OFFICE, WALL, CARPET, CEILING, LIGHT, LIGHT_OFF, CONFETTI, TABLE, CAKE, BALLOON_0, BALLOON_1, BALLOON_2,
        BALLOON_3, BALLOON_4, STREAMER, GIFT, SMILE_E, SMILE_S, INFL_RED, INFL_BLUE, INFL_YELLOW,
        // 0.36.0
        STAIR_S, STAIR_E, STAIR_FILL, RAIL, BALL_R, BALL_B, BALL_Y, BALL_G, DISCO_A, DISCO_B, JUKEBOX
    }

    public static final class Site {
        public final int rx;
        public final int rz;
        public final int s0;
        public final int yBase;
        final long seed;
        public final boolean modern;
        public final int storeys;
        public final int h;

        Site(int rx, int rz, int s0, int yBase, long seed, boolean modern) {
            this.modern = modern;
            this.storeys = modern ? STOREYS : 1;
            this.h = storeys * 6;
            this.rx = rx;
            this.rz = rz;
            this.s0 = s0;
            this.yBase = yBase;
            this.seed = seed;
        }

        public boolean inBand(int y) {
            return y >= yBase && y < yBase + h;
        }

        double h(int salt, int a, int b) {
            return Hashing.unit(Hashing.hash(seed, rx, rz, salt, a * 4096 + b));
        }

        public int level(int y) {
            return Math.max(0, Math.min(storeys - 1, (y - yBase) / 6));
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

        public int cellType(int cx, int cz) {
            return cellType(0, cx, cz);
        }

        /** Hash salt per storey (storey 0 keeps the old values). */
        int sl(int lv) {
            return lv * 0x40;
        }

        public int cellType(int lv, int cx, int cz) {
            double u = h(0x9601 + sl(lv), cx, cz);
            if (modern) {
                if (lv < storeys - 1 && cx >= 1 && cz >= 1 && cx <= 14 && cz <= 14 && h(0x9620 + sl(lv), cx, cz) < 0.05
                        && (lv == 0 || cellType(lv - 1, cx, cz) != STAIRS)) {
                    return STAIRS;
                }
                return u < 0.24 ? PLAIN : u < 0.36 ? TABLE : u < 0.48 ? BALLOONS : u < 0.55 ? GIFTS : u < 0.63 ? CONFETTI
                        : u < 0.70 ? SMILE : u < 0.76 ? CASTLE : u < 0.83 ? BALL_PIT : u < 0.89 ? DISCO : u < 0.95 ? BANQUET : DARK;
            }
            return u < 0.34 ? PLAIN : u < 0.49 ? TABLE : u < 0.64 ? BALLOONS : u < 0.72 ? GIFTS : u < 0.84 ? CONFETTI : u < 0.93 ? SMILE : CASTLE;
        }

        /** 0.36.0: straight staircase in a STAIRS cell (along z, or along x when turned); o = height above its floor. */
        Part stair(int lv, int cx, int cz, int ax, int az, int o) {
            boolean turned = h(0x9621 + sl(lv), cx, cz) < 0.5;
            int a = turned ? az : ax;                                     // across
            int b = turned ? ax : az;                                     // along the climb
            if (a >= 4 && a <= 5 && b >= 2 && b <= 6) {
                int k = b - 1;                                            // stair block height
                if (o == k) {
                    return turned ? Part.STAIR_E : Part.STAIR_S;
                }
                if (o >= 1 && o < k) {
                    return Part.STAIR_FILL;
                }
                if (o == 5 || o == 6) {
                    return b >= 3 ? Part.AIR : o == 6 ? Part.AIR : null;
                }
            }
            if (o == 7 && ((a == 3 || a == 6) && b >= 1 && b <= 6 || b == 1 && a >= 3 && a <= 6)) {
                return Part.RAIL;
            }
            return null;
        }

        /** Whether the wall west of (vertical) or north of a cell exists at offset {@code a} (0–9) along it. */
        boolean wallAt(int lv, boolean vertical, int cx, int cz, int a) {
            double u = h(0x9602 + (vertical ? 1 : 0) + sl(lv), cx, cz);
            if (u >= 0.55) {
                return false;
            }
            int gap = (int) (h(0x9604 + (vertical ? 1 : 0) + sl(lv), cx, cz) * 7) + 1;   // a 2-wide gap at 1..8
            boolean gapped = u < 0.35;
            return !(gapped && (a == gap || a == gap + 1));
        }

        public Part part(int lx, int y, int lz, boolean ringDoor) {
            if (lx == 0 || lz == 0) {
                return Part.OFFICE;
            }
            int lv = y / 6;
            int off = y % 6;
            if (ringSide(lx, lz) >= 0) {
                return off >= 1 && off <= 3 && ringDoor ? Part.AIR : off == 0 ? Part.CARPET : off == 5 ? Part.CEILING : Part.WALL;
            }
            int cx = lx / C;
            int cz = lz / C;
            int ax = lx % C;
            int az = lz % C;
            int t = cellType(lv, cx, cz);
            if (t == STAIRS) {
                Part st = stair(lv, cx, cz, ax, az, off);
                if (st != null) {
                    return st;
                }
            }
            if (lv > 0 && cellType(lv - 1, cx, cz) == STAIRS) {
                t = PLAIN;                                                // keep the landing above a staircase clear
            }
            if (lv > 0 && off <= 1 && cellType(lv - 1, cx, cz) == STAIRS) {
                Part st = stair(lv - 1, cx, cz, ax, az, off + 6);
                if (st != null) {
                    return st;
                }
            }
            if (off == 0) {
                if (t == DISCO && ax >= 1 && az >= 1) {
                    return (ax + az) % 2 == 0 ? Part.DISCO_A : Part.DISCO_B;
                }
                return Part.CARPET;
            }
            if (off == 5) {
                if (az == 5 && ax >= 3 && ax <= 6) {
                    return t != DARK && h(0x9605 + sl(lv), cx, cz) < 0.8 ? Part.LIGHT : Part.LIGHT_OFF;
                }
                return Part.CEILING;
            }
            if (ax == 0 && az == 0) {
                return Part.WALL;                                          // pillar at every grid corner
            }
            if (ax == 0 && wallAt(lv, true, cx, cz, az)) {
                return Part.WALL;
            }
            if (az == 0 && wallAt(lv, false, cx, cz, ax)) {
                return Part.WALL;
            }
            if (off == 1 && t != BALL_PIT && t != STAIRS && (t == CONFETTI ? h(0x9606 + sl(lv), lx, lz) < 0.3 : h(0x9606 + sl(lv), lx, lz) < 0.04)) {
                return Part.CONFETTI;
            }
            switch (t) {
                case TABLE -> {
                    if (ax >= 4 && ax <= 5 && az >= 4 && az <= 5) {
                        return off == 1 ? Part.TABLE : off == 2 && ax == 4 && az == 4 ? Part.CAKE : Part.AIR;
                    }
                    if (off == 4 && az == 2 && ax >= 2 && ax <= 7) {
                        return Part.STREAMER;
                    }
                    if (off == 1 && ((ax == 3 && az == 6) || (ax == 6 && az == 3))) {
                        return Part.GIFT;
                    }
                }
                case BALLOONS -> {
                    if (off == 3 || off == 2) {
                        for (int[] b : new int[][]{{3, 3}, {4, 6}, {6, 4}, {5, 5}, {7, 7}, {2, 6}}) {
                            if (ax == b[0] && az == b[1] && (off == 3 || (b[0] + b[1]) % 3 == 0)) {
                                return balloon((int) (h(0x9607 + sl(lv), lx * 7 + off, lz) * 5));
                            }
                        }
                    }
                    if (off == 4 && az == 8 && ax >= 1 && ax <= 8) {
                        return Part.STREAMER;
                    }
                }
                case GIFTS -> {
                    if (off == 1 && (ax + az) % 3 == 0 && ax >= 2 && ax <= 7 && az >= 2 && az <= 7 && !(ax == 2 && az == 7)) {
                        return Part.GIFT;
                    }
                }
                case CASTLE -> {
                    Part c = castle(ax, off, az);
                    if (c != null) {
                        return c;
                    }
                }
                case BALL_PIT -> {
                    if (ax >= 2 && ax <= 7 && az >= 2 && az <= 7) {
                        boolean edge = ax == 2 || ax == 7 || az == 2 || az == 7;
                        if (edge && off == 1) {
                            return Part.INFL_RED;
                        }
                        if (!edge && (off == 1 || (off == 2 && h(0x9622 + sl(lv), lx, lz) < 0.35))) {
                            return switch ((int) (h(0x9623 + sl(lv), lx * 3 + off, lz) * 4)) {
                                case 0 -> Part.BALL_R;
                                case 1 -> Part.BALL_B;
                                case 2 -> Part.BALL_Y;
                                default -> Part.BALL_G;
                            };
                        }
                    }
                }
                case DISCO -> {
                    if (off == 1 && ax == 5 && az == 5) {
                        return Part.JUKEBOX;
                    }
                    if (off == 4 && (ax == 2 || ax == 8) && az >= 2 && az <= 8) {
                        return Part.STREAMER;
                    }
                }
                case BANQUET -> {
                    if (az == 5 && ax >= 2 && ax <= 8) {
                        return off == 1 ? Part.TABLE : off == 2 && ax % 3 == 0 ? Part.CAKE : Part.AIR;
                    }
                    if (off == 1 && (az == 4 || az == 6) && ax % 2 == 0 && ax >= 2 && ax <= 8) {
                        return Part.GIFT;
                    }
                }
                case DARK -> {
                    if (off == 2 && ax == 1 && az >= 2 && az <= 8 && az % 3 == 2 && wallAt(lv, true, cx, cz, az)) {
                        return Part.SMILE_E;
                    }
                    if (off == 2 && az == 1 && ax >= 2 && ax <= 8 && ax % 3 == 2 && wallAt(lv, false, cx, cz, ax)) {
                        return Part.SMILE_S;
                    }
                }
                case SMILE -> {
                    if (off >= 2 && off <= 3 && ax == 1 && az >= 3 && az <= 6 && wallAt(lv, true, cx, cz, az)) {
                        return Part.SMILE_E;
                    }
                    if (off >= 2 && off <= 3 && az == 1 && ax >= 3 && ax <= 6 && wallAt(lv, false, cx, cz, ax)) {
                        return Part.SMILE_S;
                    }
                }
                default -> {
                }
            }
            return Part.AIR;
        }

        /** A (not bouncy) bouncy castle: 6 × 6 inflated floor, puffy walls with an opening, four towers. */
        static Part castle(int ax, int off, int az) {
            if (ax < 2 || ax > 7 || az < 2 || az > 7) {
                return null;
            }
            boolean cornerX = ax == 2 || ax == 7;
            boolean cornerZ = az == 2 || az == 7;
            if (cornerX && cornerZ) {
                return off <= 3 ? Part.INFL_BLUE : Part.INFL_YELLOW;            // towers
            }
            if (off == 1) {
                return Part.INFL_YELLOW;                                         // the inflated floor
            }
            if ((cornerX || cornerZ) && off <= 3) {
                if (az == 7 && (ax == 4 || ax == 5)) {
                    return Part.AIR;                                             // the way in
                }
                return off == 3 ? Part.INFL_BLUE : Part.INFL_RED;
            }
            return Part.AIR;
        }

        static Part balloon(int c) {
            return switch (c) {
                case 0 -> Part.BALLOON_0;
                case 1 -> Part.BALLOON_1;
                case 2 -> Part.BALLOON_2;
                case 3 -> Part.BALLOON_3;
                default -> Part.BALLOON_4;
            };
        }
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(16);

    public LevelFun(OfficePlanner p) {
        this.p = p;
    }

    public Site site(int rx, int rz) {
        if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.LEVEL_FUN) {
            return null;
        }
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    private Site build(int rx, int rz) {
        int[] range = Megastructures.storeyRange(p, p.cfg.modern() ? STOREYS : 1);
        if (range[1] <= range[0]) {
            return null;
        }
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9600, 0));
        int s0 = rng.range(range[0], range[1]);
        return new Site(rx, rz, s0, p.floorY(s0), p.seed, p.cfg.modern());
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
