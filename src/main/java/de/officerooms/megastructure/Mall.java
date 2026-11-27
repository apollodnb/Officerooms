package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.Room;
import java.util.Optional;

/**
 * 0.43.0: the Dead Mall (new worlds only), a megastructure as rare as the others. Six office storeys of a region
 * (160 × 160 blocks) become a three-floor shopping centre nobody has shopped in for a very long time.
 *
 * <ul>
 *     <li>Two concourses cross in the middle (east–west and north–south, 33 wide). Above them the floors are open
 *     (atriums under skylights) with galleries and glass railings along the voids; a dry fountain sits under the
 *     crossing, the food court under the north atrium, the glass entrance doors (black night behind them) at the south end.</li>
 *     <li>Two pairs of escalators (not running) climb across the east–west atrium: floor 0 → 1 and floor 1 → 2.</li>
 *     <li>Shops line the concourses: shuttered, papered over "for lease", lit and empty, or still full of stock. Each has
 *     a stockroom with a back door to the low <b>service corridors</b>, which also lead to four closed department
 *     stores behind the shops.</li>
 * </ul>
 * Heights per floor: 0 floor, 1–10 space, 11 ceiling. Office doors of the outer ring open into every floor.
 *
 * <p>Region-local coordinates: concourses at {@code 64..96}; quadrant coordinates {@code a} (distance from the
 * north–south concourse) and {@code b} (distance from the east–west concourse) run 0..60 outwards.</p>
 */
public final class Mall {

    public static final int FLOORS = 3;
    public static final int FH = 12;
    public static final int STOREYS = FLOORS * 2;
    public static final int H = FLOORS * FH;
    static final int W = 160;
    /** Concourse band (both directions). */
    public static final int C0 = 64;
    public static final int C1 = 96;
    /** Strips. */
    public static final int EW = 0;
    public static final int NS = 1;

    // shop kinds
    public static final int SHUTTER = 0, LEASE = 1, CLOTHES = 2, SHOES = 3, ELECTRONICS = 4, BOOKS = 5, TOYS = 6, PETS = 7, SALON = 8, DARK = 9,
            FOOD = 10, MANAGEMENT = 11, SECURITY = 12;

    public enum Part {
        AIR, OFFICE, RING, WALL, TILE, TILE_B, TILE_C, SHOP_FLOOR, SHOP_CARPET, SVC_FLOOR, SVC_WALL, CEILING, SVC_CEILING, LIGHT, LIGHT_OFF,
        SVC_LIGHT, SVC_LIGHT_OFF, SKYLIGHT, ROOF_BEAM, FRAME, FACADE, GLASS, LEASE_GLASS, SHUTTER, NIGHT_GLASS, ESC_N, ESC_S, ESC_SIDE, RAIL,
        PLANTER, PLANT, FOUNTAIN_RIM, FOUNTAIN_BASIN, FOUNTAIN_SPOUT, DEPT_FLOOR, DEPT_WALL, PILLAR
    }

    public static boolean concourse(int lx, int lz) {
        return lz >= C0 && lz <= C1 || lx >= C0 && lx <= C1;
    }

    /** Open atrium above floor 0 (the voids under the skylights). */
    public static boolean isVoid(int lx, int lz) {
        if (lz >= 72 && lz <= 88 && (lx >= 12 && lx <= 63 || lx >= 97 && lx <= 148)) {
            return true;
        }
        return lx >= 72 && lx <= 88 && lz >= 12 && lz <= 148;
    }

    static int mirror(int v) {
        return v <= 80 ? v : 160 - v;
    }

    /** Structural columns along the void edges (through all floors). */
    public static boolean pillar(int lx, int lz) {
        int mx = mirror(lx);
        int mz = mirror(lz);
        if ((lz == 71 || lz == 89) && (mx == 16 || mx == 24 || mx == 44 || mx == 56)) {
            return true;
        }
        if ((lx == 71 || lx == 89) && (mz == 20 || mz == 32 || mz == 44 || mz == 56)) {
            return true;
        }
        return (lx == 71 || lx == 89) && (lz == 71 || lz == 89);
    }

    /** A gallery cell at the edge of a void: glass railing on floors 1 and 2. */
    public static boolean railCell(int lx, int lz) {
        if (!concourse(lx, lz) || isVoid(lx, lz) || pillar(lx, lz)) {
            return false;
        }
        return isVoid(lx + 1, lz) || isVoid(lx - 1, lz) || isVoid(lx, lz + 1) || isVoid(lx, lz - 1);
    }

    /** Openings in the railings where the escalators arrive and leave. */
    static boolean railGap(int f, int lx, int lz) {
        int mx = mirror(lx);
        if (f == 1 && lz == 89) {
            return mx >= 30 && mx <= 31 || mx >= 34 && mx <= 35;
        }
        return f == 2 && lz == 71 && mx >= 34 && mx <= 35;
    }

    /**
     * The two escalators on each side of the east–west atrium (x mirrored at 80). Floor 0 → 1: steps at x 30–31,
     * rising south from z 77 to the south gallery. Floor 1 → 2: steps at x 34–35, from the south gallery of floor 1
     * rising north to a landing at z 72–76 and the north gallery of floor 2. Returns null where there is no escalator.
     */
    static Part escalator(int lx, int off, int lz) {
        int mx = mirror(lx);
        if (mx >= 29 && mx <= 32 && lz >= 77 && lz <= 88) {
            int step = 1 + (lz - 77);
            if (mx == 29 || mx == 32) {
                return off >= 1 && off <= step + 1 ? Part.ESC_SIDE : null;
            }
            if (off == step) {
                return Part.ESC_S;
            }
            return off >= 1 && off < step ? Part.ESC_SIDE : null;
        }
        if (mx >= 33 && mx <= 36 && lz >= 72 && lz <= 88) {
            int step = lz >= 77 ? FH + 1 + (88 - lz) : 2 * FH;
            boolean landing = lz < 77;
            if (mx == 33 || mx == 36) {
                return off >= FH + 1 && off <= step + 1 && (off >= 2 * FH || !landing) ? Part.ESC_SIDE : null;
            }
            if (off == step) {
                return landing ? Part.TILE : Part.ESC_N;
            }
            return !landing && off >= FH + 1 && off < step ? Part.ESC_SIDE : null;
        }
        return null;
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

        public double h(int salt, int a, int b) {
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

        public static int uid(int f, int q, int strip, int idx) {
            return f * 1000 + q * 100 + strip * 10 + idx;
        }

        /** Kind of shop unit. Floor 0 north of the crossing: the food court stalls. */
        public int unitKind(int f, int q, int strip, int idx) {
            if (strip == NS && f == 0 && q < 2) {
                return FOOD;
            }
            if (f == 1 && q == 0 && strip == NS && idx == 0) {
                return MANAGEMENT;
            }
            if (f == 0 && q == 3 && strip == NS && idx == 0) {
                return SECURITY;
            }
            double u = h(0xA110, uid(f, q, strip, idx), 0);
            if (u < 0.22) {
                return SHUTTER;
            }
            if (u < 0.34) {
                return LEASE;
            }
            if (u < 0.48) {
                return CLOTHES;
            }
            if (u < 0.56) {
                return SHOES;
            }
            if (u < 0.65) {
                return ELECTRONICS;
            }
            if (u < 0.73) {
                return BOOKS;
            }
            if (u < 0.80) {
                return TOYS;
            }
            if (u < 0.85) {
                return PETS;
            }
            if (u < 0.90) {
                return SALON;
            }
            return DARK;
        }

        private double litChance(int kind) {
            return switch (kind) {
                case SHUTTER, LEASE, DARK -> 0.0;
                case FOOD -> 0.45;
                default -> 0.7;
            };
        }

        public Part part(int lx, int off, int lz, boolean ringDoor) {
            if (lx == 0 || lz == 0) {
                return Part.OFFICE;
            }
            int f = off / FH;
            int o = off % FH;
            if (ringSide(lx, lz) >= 0) {
                return o >= 1 && o <= 3 && ringDoor ? Part.AIR : o == 0 ? Part.TILE : o == FH - 1 ? Part.CEILING : Part.RING;
            }
            if (Mall.concourse(lx, lz)) {
                return concourse(lx, off, lz, f, o);
            }
            boolean west = lx < C0;
            boolean north = lz < C0;
            int a = west ? C0 - 1 - lx : lx - C1 - 1;
            int b = north ? C0 - 1 - lz : lz - C1 - 1;
            int q = (west ? 0 : 1) + (north ? 0 : 2);
            if (b <= 17) {
                return unit(f, o, q, EW, a / 12, b, a % 12, 11, a);
            }
            if (a <= 17 && b >= 21) {
                return unit(f, o, q, NS, (b - 21) / 13, a, (b - 21) % 13, 12, b);
            }
            if (a >= 21 && b >= 21) {
                return anchor(o, a, b);
            }
            return service(o, a, b);
        }

        private Part unit(int f, int o, int q, int strip, int idx, int d, int w, int width, int along) {
            if (o == FH - 1) {
                return Part.CEILING;
            }
            int kind = unitKind(f, q, strip, idx);
            int uid = uid(f, q, strip, idx);
            boolean wall = w == 0 || along >= 60;
            if (d == 17) {                                             // back wall, door to the service corridor
                if (!wall && w == width - 1 && o >= 1 && o <= 3) {
                    return Part.AIR;
                }
                return o == 0 ? floor(kind) : Part.WALL;
            }
            if (wall) {
                if (strip == EW && along == 0 && d >= 2 && d <= 15 && o >= 1 && o <= 4 && glazed(kind)) {
                    return Part.GLASS;                                  // corner shops are glazed on both sides
                }
                if (d == 0) {
                    return o == 0 ? Part.TILE : o <= 4 ? Part.FRAME : Part.FACADE;
                }
                return o == 0 ? floor(kind) : Part.WALL;
            }
            if (d == 0) {
                return front(kind, o, w, width);
            }
            if (o == 0) {
                return floor(kind);
            }
            if (o >= 7) {
                return Part.CEILING;
            }
            if (o == 6) {
                if (w % 4 == 2 && d % 4 == 2) {
                    return h(0xA112, uid, w * 32 + d) < litChance(kind) ? Part.LIGHT : Part.LIGHT_OFF;
                }
                return Part.CEILING;
            }
            if (d == 13 && kind != FOOD) {                             // stockroom partition
                return w == width - 1 && o <= 3 ? Part.AIR : Part.WALL;
            }
            return Part.AIR;
        }

        private static boolean glazed(int kind) {
            return kind != SHUTTER && kind != LEASE && kind != MANAGEMENT && kind != SECURITY && kind != FOOD;
        }

        private static Part floor(int kind) {
            return switch (kind) {
                case CLOTHES, SHOES, SALON, MANAGEMENT, BOOKS -> Part.SHOP_CARPET;
                default -> Part.SHOP_FLOOR;
            };
        }

        private static Part front(int kind, int o, int w, int width) {
            if (o == 0) {
                return Part.TILE;
            }
            if (o >= 5) {
                return Part.FACADE;
            }
            boolean door = w >= width / 2 && w <= width / 2 + 1 + width % 2;
            return switch (kind) {
                case SHUTTER -> Part.SHUTTER;
                case LEASE -> Part.LEASE_GLASS;
                case FOOD, DARK -> o == 4 ? Part.FACADE : Part.AIR;
                case MANAGEMENT, SECURITY -> door && o <= 3 ? Part.AIR : Part.WALL;
                default -> door ? (o <= 3 ? Part.AIR : Part.FRAME) : Part.GLASS;
            };
        }

        private Part anchor(int o, int a, int b) {
            if (o == FH - 1) {
                return Part.CEILING;
            }
            if (a == 21 || b == 21 || a >= 60 || b >= 60) {
                if (o >= 1 && o <= 3 && (a == 21 && b >= 38 && b <= 40 || b == 21 && a >= 38 && a <= 40)) {
                    return Part.AIR;
                }
                return o == 0 ? Part.DEPT_FLOOR : Part.DEPT_WALL;
            }
            if (o == 0) {
                return Part.DEPT_FLOOR;
            }
            if (o == 10) {
                return Part.CEILING;
            }
            if (o == 9) {
                if ((a - 22) % 6 == 3 && (b - 22) % 6 == 3) {
                    return h(0xA113, a, b) < 0.25 ? Part.LIGHT : Part.LIGHT_OFF;   // the department stores are mostly dark
                }
                return Part.CEILING;
            }
            if ((a - 22) % 10 == 5 && (b - 22) % 10 == 5) {
                return Part.PILLAR;
            }
            return Part.AIR;
        }

        private Part service(int o, int a, int b) {
            if (o == FH - 1) {
                return Part.CEILING;
            }
            if (a == 0) {                                              // "employees only" passage from the concourse
                if (o >= 1 && o <= 3 && b >= 19) {
                    return Part.AIR;
                }
                return o == 0 ? Part.SVC_FLOOR : o >= 5 ? Part.FACADE : Part.SVC_WALL;
            }
            if (o == 0) {
                return Part.SVC_FLOOR;
            }
            if (o <= 4) {
                return Part.AIR;
            }
            if (o == 5) {
                boolean lamp = b <= 20 ? a % 9 == 4 && b == 19 : b % 9 == 4 && a == 19;
                if (lamp) {
                    return h(0xA114, a, b) < 0.5 ? Part.SVC_LIGHT : Part.SVC_LIGHT_OFF;
                }
            }
            return Part.SVC_CEILING;
        }

        private Part ceilingLight(int lx, int lz, int off) {
            if (lx % 6 == 3 && lz % 6 == 3) {
                return h(0xA115, lx, lz * 64 + off) < 0.68 ? Part.LIGHT : Part.LIGHT_OFF;
            }
            return Part.CEILING;
        }

        private Part floorTile(int lx, int lz) {
            int dx = lx - 80;
            int dz = lz - 80;
            if (dx * dx + dz * dz <= 49) {
                return Part.FOUNTAIN_BASIN;
            }
            if (h(0xA116, lx, lz) < 0.03) {
                return Part.TILE_C;
            }
            return ((lx >> 2) + (lz >> 2) & 1) == 0 ? Part.TILE : Part.TILE_B;
        }

        private Part concourse(int lx, int off, int lz, int f, int o) {
            boolean vd = isVoid(lx, lz);
            if (off == H - 1) {
                return vd ? (lx % 6 == 0 || lz % 6 == 0 ? Part.ROOF_BEAM : Part.SKYLIGHT) : ceilingLight(lx, lz, off);
            }
            Part e = escalator(lx, off, lz);
            if (e != null) {
                return e;
            }
            if (pillar(lx, lz) && o >= 1 && o <= FH - 2) {
                return Part.PILLAR;
            }
            if (vd) {
                if (f == 0 && o == 0) {
                    return floorTile(lx, lz);
                }
                return f == 0 ? floorFeature(lx, o, lz) : Part.AIR;
            }
            if (o == FH - 1) {
                return ceilingLight(lx, lz, off);
            }
            if (o == 0) {
                return f == 0 ? floorTile(lx, lz) : Part.TILE;
            }
            if (f >= 1) {
                return o == 1 && railCell(lx, lz) && !railGap(f, lx, lz) ? Part.RAIL : Part.AIR;
            }
            if (lz == W - 3) {                                          // the entrance doors at the south end
                if (lx >= 73 && lx <= 87) {
                    return o <= 4 ? (lx % 4 == 1 ? Part.FRAME : Part.NIGHT_GLASS) : Part.FACADE;
                }
                return Part.WALL;
            }
            return floorFeature(lx, o, lz);
        }

        /** Fixed fittings on floor 0: the fountain and the planters. */
        private Part floorFeature(int lx, int o, int lz) {
            int dx = lx - 80;
            int dz = lz - 80;
            int r2 = dx * dx + dz * dz;
            if (r2 <= 49) {
                if (r2 <= 1) {
                    return o <= 3 ? Part.FOUNTAIN_SPOUT : Part.AIR;
                }
                return o == 1 && r2 > 30 ? Part.FOUNTAIN_RIM : Part.AIR;
            }
            if (o <= 2 && planter(lx, lz)) {
                boolean mid = lz == 80 && (mirror(lx) == 20 || mirror(lx) == 50) || lx == 80 && (lz == 110 || lz == 130);
                return o == 1 ? Part.PLANTER : mid ? Part.PLANT : Part.AIR;
            }
            return Part.AIR;
        }
    }

    /** 3 × 3 planters along the middle of the concourses. */
    public static boolean planter(int lx, int lz) {
        int mx = mirror(lx);
        if (lz >= 79 && lz <= 81 && (mx >= 19 && mx <= 21 || mx >= 49 && mx <= 51)) {
            return true;
        }
        return lx >= 79 && lx <= 81 && (lz >= 109 && lz <= 111 || lz >= 129 && lz <= 131);
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(16);

    public Mall(OfficePlanner p) {
        this.p = p;
    }

    public Site site(int rx, int rz) {
        if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.MALL) {
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
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0xA100, 0));
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
