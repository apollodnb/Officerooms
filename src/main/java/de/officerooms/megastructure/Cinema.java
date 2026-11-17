package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.Room;
import java.util.Optional;

/**
 * 0.31.0: the abandoned cinema (new worlds only), a megastructure as rare as the others. Two storeys of a region
 * (160 × 160 blocks, 12 high) become a multiplex nobody has visited in decades.
 *
 * <ul>
 *     <li>A double-height <b>lobby</b> in the middle (checkered tiles that echo): a box office island with ticket
 *     windows and a marquee, velvet-rope queues, two concession stands with popcorn machines, soda fountains, candy
 *     cases and glowing menu boards, faded posters.</li>
 *     <li>Two low <b>corridors</b> along the lobby with numbered doors.</li>
 *     <li><b>Twelve auditoriums</b> (six per side), each with a screen at the corridor end, stadium seating rising
 *     away from it (aisles with stairs and dim step lights), exit signs and a projector port in the back wall. Every
 *     hall is different: a faintly lit empty screen, a screen of static, a dark hall, a wrecked hall with a torn
 *     screen, or one where a figure stands on the screen.</li>
 * </ul>
 * Heights: 0 floor, 1–10 space, 11 ceiling. Office doors of the lower storey open into it through the outer ring.
 *
 * <p>0.37.0: a multiplex of {@link #FLOORS} cinema floors (12 blocks each, {@link #STOREYS} office storeys) on the
 * same footprint. Every floor has its own corridors and twelve auditoriums (screens 1–36). The lobby is an atrium
 * over all floors with galleries (railings) along its walls; freestanding stairs climb from the lobby to the first
 * gallery and from there (via a landing at gallery level) to the second. Office doors open into every cinema floor.</p>
 */
public final class Cinema {

    /** 0.37.0: three cinema floors of 12 blocks (two office storeys each). */
    public static final int FLOORS = 3;
    public static final int FH = 12;
    public static final int STOREYS = FLOORS * 2;
    public static final int H = FLOORS * FH;
    /** Halls per floor. */
    public static final int HALLS = 12;
    static final int W = 160;

    static final int LOBBY_Z0 = 64;
    static final int LOBBY_Z1 = 96;
    static final int BOX_X0 = 70;
    static final int BOX_X1 = 90;
    static final int BOX_Z0 = 70;
    static final int BOX_Z1 = 76;
    static final int STAND_Z = 88;
    static final int[] STANDS = {20, 110};

    public static final int NORMAL = 0;
    public static final int STATIC = 1;
    public static final int DARK = 2;
    public static final int WRECKED = 3;
    public static final int FIGURE = 4;

    public enum Part {
        AIR, OFFICE, WALL, LOBBY_WALL, CARPET, TILE, CEILING, LIGHT, LIGHT_OFF, RISER, STAIR_N, STAIR_S, SEAT_N, SEAT_S,
        SEAT_BROKEN_N, SEAT_BROKEN_S, SCREEN, SCREEN_STATIC, SCREEN_OFF, SCREEN_TORN, FIGURE_LOWER, FIGURE_UPPER, PROJ_N,
        PROJ_S, PROJ_OFF_N, PROJ_OFF_S, AISLE_E, AISLE_W, EXIT_N, EXIT_S, COUNTER_N, COUNTER_S, TICKET_S, POPCORN_N, SODA_N,
        CANDY_N, MENU_N, STANCHION, ROPE, MARQUEE, TRASH_S, TRASH_N, SPILL,
        STAIR_E, STAIR_W, RAIL_X, RAIL_Z, RAIL_ES, RAIL_WS, RAIL_EN, RAIL_WN
    }

    /** Riser level at distance t from the screen: flat front, then a step up every 3 blocks, at most 7. */
    public static int level(int t) {
        return t < 10 ? 0 : Math.min(7, (t - 10) / 3 + 1);
    }

    /** 0.37.0: the galleries along the lobby walls (floors 1 and 2; on floor 0 the ceiling under them). */
    public static boolean gallery(int lx, int lz) {
        return lz <= LOBBY_Z0 + 3 || lz >= LOBBY_Z1 - 3 || lx <= 6 || lx >= 154;
    }

    /** Railing on the inner edge of a gallery, or null. */
    static Part rail(int lx, int lz) {
        boolean n = lz == LOBBY_Z0 + 3;
        boolean s = lz == LOBBY_Z1 - 3;
        if ((n || s) && (lx == 6 || lx == 154)) {
            return lx == 6 ? (n ? Part.RAIL_ES : Part.RAIL_EN) : (n ? Part.RAIL_WS : Part.RAIL_WN);
        }
        if ((n || s) && lx > 6 && lx < 154) {
            return Part.RAIL_X;
        }
        if ((lx == 6 || lx == 154) && lz > LOBBY_Z0 + 3 && lz < LOBBY_Z1 - 3) {
            return Part.RAIL_Z;
        }
        return null;
    }

    /** Start x of the two flights on each side (west flight climbs east, east flight climbs west); 12 steps each. */
    static final int FLIGHT_W = 30;
    static final int FLIGHT_E = 130;

    /**
     * 0.37.0: the freestanding stairs in the atrium. Floor 0 → 1 along the north gallery (lz 68–69), floor 1 → 2 along
     * the south gallery (lz 91–92). Returns null where there is no stair.
     */
    static Part flight(int lx, int off, int lz) {
        int base;
        int outer;
        if (lz >= LOBBY_Z0 + 4 && lz <= LOBBY_Z0 + 6) {
            base = 0;
            outer = LOBBY_Z0 + 6;
        } else if (lz <= LOBBY_Z1 - 4 && lz >= LOBBY_Z1 - 6) {
            base = FH;
            outer = LOBBY_Z1 - 6;
        } else {
            return null;
        }
        boolean edge = lz == outer;
        boolean south = base == 0;                                     // the outer row lies south of the lower flights
        // 0.37.1: landings with railings at the top of every flight (walking straight on no longer drops you into the atrium)
        boolean wTop = lx >= FLIGHT_W + 12 && lx <= FLIGHT_W + 15;
        boolean eTop = lx <= FLIGHT_E - 12 && lx >= FLIGHT_E - 15;
        if (wTop || eTop) {
            boolean end = lx == FLIGHT_W + 15 || lx == FLIGHT_E - 15;
            return landing(off, base + FH, edge, end, south, wTop);
        }
        // landing at gallery level before the first step of the upper flights
        boolean wFoot = base == FH && lx >= FLIGHT_W - 3 && lx < FLIGHT_W;
        boolean eFoot = base == FH && lx > FLIGHT_E && lx <= FLIGHT_E + 3;
        if (wFoot || eFoot) {
            boolean end = lx == FLIGHT_W - 3 || lx == FLIGHT_E + 3;
            return landing(off, FH, edge, end, false, eFoot);
        }
        if (edge) {
            return null;
        }
        int k;
        boolean east;
        if (lx >= FLIGHT_W && lx <= FLIGHT_W + 11) {
            k = lx - FLIGHT_W;
            east = true;
        } else if (lx <= FLIGHT_E && lx >= FLIGHT_E - 11) {
            k = FLIGHT_E - lx;
            east = false;
        } else {
            return null;
        }
        int step = base + 1 + k;
        if (off == step) {
            return east ? Part.STAIR_E : Part.STAIR_W;
        }
        int bottom = base == 0 ? 1 : FH;
        return off >= bottom && off < step ? Part.RISER : null;
    }

    /** 0.37.1: a landing tile at {@code top} with railings on its outer row and outer end. */
    private static Part landing(int off, int top, boolean edge, boolean end, boolean south, boolean westOfEnd) {
        if (off == top) {
            return Part.TILE;
        }
        if (off != top + 1 || !edge && !end) {
            return null;
        }
        if (edge && end) {
            return south ? (westOfEnd ? Part.RAIL_WN : Part.RAIL_EN) : (westOfEnd ? Part.RAIL_WS : Part.RAIL_ES);
        }
        return edge ? Part.RAIL_X : Part.RAIL_Z;
    }

    /** Whether the railing of floor f is open (top or foot of a flight). */
    static boolean railGap(int f, int lx, int lz) {
        boolean w = false;
        boolean e = false;
        if (f == 1 && lz == LOBBY_Z0 + 3 || f == 2 && lz == LOBBY_Z1 - 3) {
            w = lx >= FLIGHT_W + 8 && lx <= FLIGHT_W + 14;               // top of a flight and its landing
            e = lx <= FLIGHT_E - 8 && lx >= FLIGHT_E - 14;
        } else if (f == 1 && lz == LOBBY_Z1 - 3) {
            w = lx >= FLIGHT_W - 3 && lx < FLIGHT_W;                     // to the landings of the upper flights
            e = lx > FLIGHT_E && lx <= FLIGHT_E + 3;
        }
        return w || e;
    }

    static boolean inStand(int lx) {
        for (int sx : STANDS) {
            if (lx >= sx && lx <= sx + 30) {
                return true;
            }
        }
        return false;
    }

    public static final class Site {
        public final int rx;
        public final int rz;
        public final int s0;
        public final int yBase;
        final long seed;

        Site(int rx, int rz, int s0, int yBase, long seed) {
            this.rx = rx;
            this.rz = rz;
            this.s0 = s0;
            this.yBase = yBase;
            this.seed = seed;
        }

        public boolean inBand(int y) {
            return y >= yBase && y < yBase + H;
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

        /** Kind of auditorium (per floor 0–5 north wing, 6–11 south wing; floor f adds 12 · f). */
        public int hallKind(int i) {
            double u = h(0x9C01, i, 0);
            return u < 0.3 ? NORMAL : u < 0.5 ? STATIC : u < 0.7 ? DARK : u < 0.85 ? WRECKED : FIGURE;
        }

        /** Auditorium index at a block (or -1 outside the auditoriums). */
        public int hallAt(int lx, int lz) {
            return hallAt(lx, 0, lz);
        }

        /** 0.37.0: auditorium index 0–35 at a block of the band (or -1). */
        public int hallAt(int lx, int off, int lz) {
            int f = off / FH;
            if (lx < 4 || lx > 157 || (lx - 3) % 26 == 0) {
                return -1;
            }
            int i = (lx - 3) / 26;
            if (lz >= 3 && lz <= 56) {
                return f * HALLS + i;
            }
            if (lz >= 104 && lz <= 157) {
                return f * HALLS + i + 6;
            }
            return -1;
        }

        public Part part(int lx, int off, int lz, boolean ringDoor) {
            if (lx == 0 || lz == 0) {
                return Part.OFFICE;
            }
            int f = off / FH;
            int o = off % FH;
            if (ringSide(lx, lz) >= 0) {
                return o >= 1 && o <= 3 && ringDoor ? Part.AIR : o == 0 ? Part.TILE : o == FH - 1 ? Part.CEILING : Part.WALL;
            }
            if (lz <= 56) {
                return hall(lx, o, lz, true, f);
            }
            if (lz >= 104) {
                return hall(lx, o, lz, false, f);
            }
            if (lz == 57 || lz == 103) {
                return hallWall(lx, o);
            }
            if (lz <= 62 || lz >= 98) {
                return corridor(lx, o, lz);
            }
            if (lz == 63 || lz == 97) {
                return lobbyWall(lx, o);
            }
            return lobby(lx, off, lz);
        }

        private Part hall(int lx, int off, int lz, boolean north, int f) {
            int m = (lx - 3) % 26;
            if (m == 0) {
                return off == FH - 1 ? Part.CEILING : Part.WALL;
            }
            int i = (lx - 3) / 26;
            int w = i == 5 ? 24 : 25;                                  // last hall is one narrower (ring)
            int ax = m;
            int t = north ? 56 - lz : lz - 104;
            int kind = hallKind(f * HALLS + (north ? i : i + 6));
            int lv = level(t);
            boolean aisle = ax <= 3 || ax >= w - 2;
            if (off == 0) {
                return Part.CARPET;
            }
            if (off == FH - 1) {
                return Part.CEILING;
            }
            if (off < lv) {
                return Part.RISER;
            }
            if (off == lv) {
                boolean step = t >= 10 && (t - 10) % 3 == 0 && t <= 28;
                return aisle && step ? (north ? Part.STAIR_N : Part.STAIR_S) : Part.CARPET;
            }
            // the screen on the wall at the corridor end
            if (t == 0 && off >= 3 && off <= 9 && ax >= 4 && ax <= w - 3) {
                if (kind == FIGURE && ax == w / 2 + 1 && (off == 4 || off == 5)) {
                    return off == 4 ? Part.FIGURE_LOWER : Part.FIGURE_UPPER;
                }
                return switch (kind) {
                    case STATIC -> Part.SCREEN_STATIC;
                    case DARK -> Part.SCREEN_OFF;
                    case WRECKED -> h(0x9C02, lx, off) < 0.18 ? Part.AIR : Part.SCREEN_TORN;
                    default -> Part.SCREEN;
                };
            }
            if (t == 0 && off == 5 && (ax == 1 || ax == w)) {
                return north ? Part.EXIT_N : Part.EXIT_S;            // over the two doors, facing into the hall
            }
            if (t == 53 && off == 9 && ax == w / 2 + 1) {
                boolean lit = kind != DARK && kind != WRECKED;
                return north ? (lit ? Part.PROJ_S : Part.PROJ_OFF_S) : (lit ? Part.PROJ_N : Part.PROJ_OFF_N);
            }
            if (off == lv + 1) {
                boolean row = t >= 11 && t <= 50 && (t - 10) % 3 == 1;
                if (row && !aisle) {
                    double u = h(0x9C03, lx, lz);
                    double gone = kind == WRECKED ? 0.25 : 0.05;
                    double broken = kind == WRECKED ? 0.55 : 0.14;
                    if (u < gone) {
                        return Part.AIR;
                    }
                    return u < broken ? (north ? Part.SEAT_BROKEN_S : Part.SEAT_BROKEN_N) : (north ? Part.SEAT_S : Part.SEAT_N);
                }
                if ((ax == 1 || ax == w) && kind != DARK && (row || t == 3 || t == 7)) {
                    return ax == 1 ? Part.AISLE_E : Part.AISLE_W;
                }
                if (h(0x9C04, lx, lz) < (kind == WRECKED ? 0.08 : 0.012)) {
                    return Part.SPILL;
                }
            }
            return Part.AIR;
        }

        private Part hallWall(int lx, int off) {
            int m = (lx - 3) % 26;
            int w = (lx - 3) / 26 == 5 ? 24 : 25;
            if (m != 0 && off >= 1 && off <= 3 && (m <= 2 || m >= w - 1)) {
                return Part.AIR;                                       // the two doors at the front corners
            }
            return off == FH - 1 ? Part.CEILING : Part.WALL;
        }

        private Part corridor(int lx, int off, int lz) {
            if (off == 0) {
                return Part.CARPET;
            }
            if (off <= 5) {
                return Part.AIR;
            }
            if (off == 6) {
                if (lx % 8 == 4 && (lz == 60 || lz == 100)) {
                    return h(0x9C05, lx, lz) < 0.55 ? Part.LIGHT : Part.LIGHT_OFF;
                }
                return Part.CEILING;
            }
            return off == FH - 1 ? Part.CEILING : Part.WALL;
        }

        private Part lobbyWall(int lx, int off) {
            int m = (lx - 3) % 26;
            if (off >= 1 && off <= 4 && m >= 12 && m <= 15) {
                return Part.AIR;
            }
            return off == 0 ? Part.TILE : off == FH - 1 ? Part.CEILING : Part.LOBBY_WALL;
        }

        private Part lobby(int lx, int off, int lz) {
            if (off == 0) {
                return Part.TILE;
            }
            if (off == H - 1) {
                if (lx % 10 == 5 && (lz - LOBBY_Z0) % 8 == 4) {
                    return h(0x9C06, lx, lz) < 0.55 ? Part.LIGHT : Part.LIGHT_OFF;
                }
                return Part.CEILING;
            }
            Part st = flight(lx, off, lz);
            if (st != null) {
                return st;
            }
            int f = off / FH;
            int o = off % FH;
            boolean gal = gallery(lx, lz);
            if (o == FH - 1) {
                if (!gal) {
                    return Part.AIR;                                   // the open atrium
                }
                boolean spot = (lz == LOBBY_Z0 + 1 || lz == LOBBY_Z1 - 1) && lx % 10 == 5 || (lx == 4 || lx == 156) && lz % 10 == 5;
                return spot ? (h(0x9C08, lx, lz + off) < 0.5 ? Part.LIGHT : Part.LIGHT_OFF) : Part.CEILING;
            }
            if (f >= 1) {
                if (!gal) {
                    return Part.AIR;
                }
                if (o == 0) {
                    return Part.TILE;
                }
                if (o == 1) {
                    Part r = rail(lx, lz);
                    if (r != null && !railGap(f, lx, lz)) {
                        return r;
                    }
                    if (lz == LOBBY_Z0 + 1 && (lx - 3) % 26 == 8) {
                        return Part.TRASH_S;
                    }
                }
                return Part.AIR;
            }
            // box office island
            if (lx >= BOX_X0 && lx <= BOX_X1 && lz >= BOX_Z0 && lz <= BOX_Z1) {
                boolean edge = lx == BOX_X0 || lx == BOX_X1 || lz == BOX_Z0 || lz == BOX_Z1;
                if (off == 7) {
                    return edge ? Part.MARQUEE : Part.AIR;
                }
                if (off > 7) {
                    return Part.AIR;
                }
                if (off == 6) {
                    return Part.LOBBY_WALL;                            // roof of the booth
                }
                if (lz == BOX_Z1 && lx > BOX_X0 && lx < BOX_X1) {
                    return off == 1 ? Part.COUNTER_S : off <= 3 ? (lx % 3 == 0 ? Part.LOBBY_WALL : Part.TICKET_S) : Part.LOBBY_WALL;
                }
                return edge ? Part.LOBBY_WALL : Part.AIR;
            }
            // velvet-rope queues in front of it
            if ((lz == 80 || lz == 83) && lx >= 72 && lx <= 88 && off == 1) {
                return lx % 4 == 0 ? Part.STANCHION : Part.ROPE;
            }
            // concession stands
            for (int sx : STANDS) {
                if (lx >= sx && lx <= sx + 30 && lz >= STAND_Z) {
                    int a = lx - sx;
                    if (a == 0 || a == 30) {
                        return off <= 5 ? Part.LOBBY_WALL : Part.AIR;
                    }
                    if (lz == STAND_Z) {
                        if (a == 1) {
                            return Part.AIR;                           // way behind the counter
                        }
                        if (off == 1) {
                            return a % 10 == 5 ? Part.CANDY_N : Part.COUNTER_N;
                        }
                        if (off == 2) {
                            return a % 10 == 3 ? Part.POPCORN_N : a % 10 == 7 ? Part.SODA_N : Part.AIR;
                        }
                        if (off == 7) {
                            return Part.MARQUEE;
                        }
                        return Part.AIR;
                    }
                    if (lz == LOBBY_Z1 - 1 && off == 1 && a % 4 != 1) {
                        return Part.COUNTER_N;                         // back counter
                    }
                    if (lz == LOBBY_Z1 && (off == 4 || off == 5) && a % 6 >= 2 && a % 6 <= 4) {
                        return Part.MENU_N;
                    }
                    return Part.AIR;
                }
            }
            if (off == 1) {
                int m = (lx - 3) % 26;
                if (m == 8 && (lz == LOBBY_Z0 + 1 || lz == LOBBY_Z1 - 1)) {
                    return lz == LOBBY_Z0 + 1 ? Part.TRASH_S : Part.TRASH_N;
                }
                if (lz >= 78 && lz <= STAND_Z - 1 && h(0x9C07, lx, lz) < 0.03) {
                    return Part.SPILL;
                }
            }
            return Part.AIR;
        }
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(16);

    public Cinema(OfficePlanner p) {
        this.p = p;
    }

    public Site site(int rx, int rz) {
        if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.CINEMA) {
            return null;
        }
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    private Site build(int rx, int rz) {
        int[] range = Megastructures.storeyRange(p, STOREYS);
        if (range[1] <= range[0]) {
            return null;
        }
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9C00, 0));
        int s0 = rng.range(range[0], range[1]);
        return new Site(rx, rz, s0, p.floorY(s0), p.seed);
    }

    public boolean overlaps(int rx, int rz, Room r) {
        if (r == null) {
            return false;
        }
        Site s = site(rx, rz);
        return s != null && Megastructures.overlapsBand(r, s.s0, STOREYS);
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
