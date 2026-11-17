package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.Room;
import java.util.Optional;

/**
 * The Neighbourhood (0.8.0): a megastructure as rare as the Poolrooms. One region (160 × 160 blocks)
 * replaces {@link #STOREYS} storeys with a huge, completely unlit hall. Inside stand 20 identical
 * suburban houses on identical lots along empty streets, inspired by the identical row houses of the
 * Backrooms community.
 *
 * <p>Every third house (index % 3 == 2) holds a persistent creature (Smiler, Headless Warden, Hound
 * or even a Colony), every other house holds a loot chest ({@code chests/neighbourhood_house}). A few
 * creatures roam the streets ({@code MobSpawner}).</p>
 *
 * <p>Heights relative to {@code yBase} = the wool floor layer of storey s0: 0 hall floor, 1 walk
 * level (= walk level of storey s0, so office doors line up), 23 ceiling (= slab of storey s0 + 4).
 * The office wall lines lx = 0 / lz = 0 stay office; a 2 block thick ring wall runs inside, opened
 * only where an office door of storey s0 meets it.</p>
 *
 * <p>Pure Java: the layout depends only on seed and region and is evaluated per column.</p>
 */
public final class Neighbourhood {

    /** 0.36.0: new worlds: 6 storeys high with two-storey houses, parks and street lamps; old worlds keep 4. */
    public static final int STOREYS = 6;
    public static final int LEGACY_STOREYS = 4;
    public static final int H = STOREYS * 6;
    static final int W = 160;

    // Lots: 5 columns × 4 rows
    public static final int COLS = 5;
    public static final int ROWS = 4;
    static final int LOT_X0 = 5;
    static final int LOT_W = 30;
    static final int LOT_Z0 = 8;
    static final int LOT_D = 36;
    // House inside the lot
    static final int HX0 = 8;
    static final int HZ0 = 12;
    static final int HW = 13;
    static final int HD = 11;
    /** House-local spot of the chest (against the back wall, next to the shelf). */
    public static final int CHEST_HX = 4;
    public static final int CHEST_HZ = HD - 2;
    /** House-local spot where the resident creature waits. */
    public static final int MOB_HX = 6;
    public static final int MOB_HZ = 4;

    public enum Part {
        AIR, OFFICE, HALL_WALL, CEILING, STREET, LINE, SIDEWALK, LAWN, PATH, FENCE,
        H_FLOOR, H_WALL, H_CORNER, H_ATTIC, GLASS, ROOF, ROOF_S, ROOF_N, CHIMNEY, DOOR_LO, DOOR_HI,
        SHELF, SOFA, TABLE_LEG, TABLE_TOP, TV, TV_STAND, CARPET, BED, LAMP,
        // 0.36.0
        BOARD, LANTERN, STAIR_E, RAIL, POST, STREET_LAMP, STREET_LAMP_LIT, MAILBOX, SAND, CHAIN, SEAT, TRUNK, LEAVES,
        DOOR_OPEN_LO, DOOR_OPEN_HI, WARDROBE
    }

    public enum Resident {
        SMILER, HEADLESS_WARDEN, HOUND, COLONY
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
            this.seed = seed;
            this.modern = modern;
            this.storeys = modern ? STOREYS : LEGACY_STOREYS;
            this.h = storeys * 6;
            this.rx = rx;
            this.rz = rz;
            this.s0 = s0;
            this.yBase = yBase;
        }

        public boolean inBand(int y) {
            return y >= yBase && y < yBase + h;
        }

        /** Ring column and its side (0 west, 1 north, 2 east, 3 south), otherwise -1. */
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

        public Part part(int lx, int off, int lz, boolean doorOpen) {
            if (lx == 0 || lz == 0) {
                return Part.OFFICE;
            }
            if (off >= h - 1) {
                return Part.CEILING;
            }
            if (ringSide(lx, lz) >= 0) {
                return off >= 1 && off <= 3 && doorOpen ? Part.AIR : Part.HALL_WALL;
            }
            int c = Math.floorDiv(lx - LOT_X0, LOT_W);
            int r = Math.floorDiv(lz - LOT_Z0, LOT_D);
            if (c < 0 || c >= COLS || r < 0 || r >= ROWS) {
                return off == 0 ? Part.STREET : Part.AIR;
            }
            int ax = lx - LOT_X0 - c * LOT_W;
            int az = lz - LOT_Z0 - r * LOT_D;
            return modern ? modernLot(r * COLS + c, ax, off, az) : lot(ax, off, az);
        }

        /** 0.36.1: is the column part of a playground lot? */
        public boolean inPark(int lx, int lz) {
            int c = Math.floorDiv(lx - LOT_X0, LOT_W);
            int r = Math.floorDiv(lz - LOT_Z0, LOT_D);
            return c >= 0 && c < COLS && r >= 0 && r < ROWS && park(r * COLS + c);
        }

        /** 0.36.1: the post under a mailbox (not a street lamp or swing post). */
        public boolean mailboxPost(int lx, int off, int lz) {
            int c = Math.floorDiv(lx - LOT_X0, LOT_W);
            int r = Math.floorDiv(lz - LOT_Z0, LOT_D);
            return off == 1 && lx - LOT_X0 - c * LOT_W == HX0 + 8 && lz - LOT_Z0 - r * LOT_D == HZ0 - 3;
        }

        double hh(int salt, int house) {
            return Hashing.unit(Hashing.hash(seed, rx, rz, 0x9140 + salt, house));
        }

        /** 0.36.0: some of the houses with a resident are an empty playground instead (the resident waits there). */
        public boolean park(int house) {
            return modern && house != 0 && hasResident(house) && hh(0, house) < 0.4;
        }

        /** 0.36.0: lots in new worlds: two-storey houses with small differences, parks, street lamps, mailboxes. */
        Part modernLot(int house, int ax, int off, int az) {
            if (az == 7 && ax == 2) {                                       // street lamp
                if (off >= 1 && off <= 4) {
                    return Part.POST;
                }
                if (off == 5) {
                    return hh(1, house) < 0.2 ? Part.STREET_LAMP_LIT : Part.STREET_LAMP;
                }
            }
            if (park(house)) {
                return parkLot(ax, off, az);
            }
            if (ax == HX0 + 8 && az == HZ0 - 3 && (off == 1 || off == 2)) {
                return off == 1 ? Part.POST : Part.MAILBOX;
            }
            int hx = ax - HX0;
            int hz = az - HZ0;
            if (hx >= -1 && hx <= HW && hz >= 0 && hz < HD) {
                Part p = house2(house, hx, off, hz);
                if (p != null) {
                    return p;
                }
            }
            Part p = lot(ax, Math.min(off, 1), az);
            return off > 1 && p != Part.AIR ? Part.AIR : p;
        }

        Part parkLot(int ax, int off, int az) {
            if (az <= 7) {
                return lot(ax, off, az);
            }
            if (off == 0) {
                return ax >= 5 && ax <= 9 && az >= 13 && az <= 17 ? Part.SAND : Part.LAWN;
            }
            if (off == 1 && ax == 0) {
                return Part.FENCE;
            }
            if ((ax == 16 || ax == 20) && az == 16 && off <= 4) {           // swing frame
                return Part.POST;
            }
            if (off == 4 && az == 16 && ax > 16 && ax < 20) {
                return Part.RAIL;
            }
            if (az == 16 && (ax == 17 || ax == 19) && (off == 2 || off == 3)) {
                return Part.CHAIN;
            }
            if (az == 16 && (ax == 17 || ax == 19) && off == 1) {
                return Part.SEAT;
            }
            if (off == 1 && az == 24 && ax >= 4 && ax <= 7) {
                return Part.SOFA;                                           // bench
            }
            int dx = ax - 23;
            int dz = az - 27;
            if (dx == 0 && dz == 0 && off <= 4) {
                return Part.TRUNK;
            }
            if (off >= 4 && off <= 7 && dx * dx + dz * dz + (off - 5) * (off - 5) * 2 <= 7) {
                return Part.LEAVES;
            }
            return Part.AIR;
        }

        /** Two-storey house: ground floor 1–4, upper floor 5 (floor) / 6–9, attic 10, gable roof from 11. */
        Part house2(int house, int hx, int off, int hz) {
            boolean inside = hx >= 0 && hx < HW;
            int mid = HD / 2;
            int roofOff = 11 + Math.min(hz, HD - 1 - hz);
            if (hx == 10 && hz == 7 && off >= 10 && off <= 18) {
                return Part.CHIMNEY;
            }
            if (off == roofOff) {
                return hz < mid ? Part.ROOF_S : hz > mid ? Part.ROOF_N : Part.ROOF;
            }
            if (!inside) {
                return off == 0 ? null : off < roofOff ? null : null;
            }
            if (off == 0) {
                return Part.H_FLOOR;
            }
            boolean wallX = hx == 0 || hx == HW - 1;
            boolean wallZ = hz == 0 || hz == HD - 1;
            boolean boarded = hh(2, house) < 0.25;
            if (off == 5) {
                if (!wallX && !wallZ && hz == HD - 2 && hx >= 6 && hx <= 8) {
                    return Part.AIR;                                        // stairwell
                }
                return Part.H_ATTIC;
            }
            if (off == 10) {
                return Part.H_ATTIC;
            }
            if (off > 10) {
                if (off < roofOff && wallX) {
                    return Part.H_WALL;
                }
                return off < roofOff ? Part.AIR : null;
            }
            int fl = off <= 4 ? 0 : 1;
            int o = off <= 4 ? off : off - 5;                               // 1–4 on both floors
            if (wallX && wallZ) {
                return Part.H_CORNER;
            }
            if (wallZ) {
                if (fl == 0 && hz == 0 && hx == 6 && o <= 2) {
                    boolean open = hh(3, house) < 0.3;
                    return o == 1 ? (open ? Part.DOOR_OPEN_LO : Part.DOOR_LO) : (open ? Part.DOOR_OPEN_HI : Part.DOOR_HI);
                }
                if ((o == 2 || o == 3) && (hx == 2 || hx == 3 || hx == 9 || hx == 10)) {
                    return boarded ? Part.BOARD : Part.GLASS;
                }
                return Part.H_WALL;
            }
            if (wallX) {
                if ((o == 2 || o == 3) && (hz == 3 || hz == 4 || hz == 6 || hz == 7)) {
                    return boarded ? Part.BOARD : Part.GLASS;
                }
                return Part.H_WALL;
            }
            if (fl == 0) {
                if (hz == HD - 2 && hx >= 5 && hx <= 8) {                   // the stairs up
                    int k = hx - 4;
                    return o == k ? Part.STAIR_E : o < k ? Part.H_ATTIC : Part.AIR;
                }
                if (hx == 1 && hz == 1 && o == 2 && hh(4, house) < 0.15) {
                    return Part.LANTERN;                                    // someone left a light on
                }
                Part p = house(hx, off, hz);
                return p == Part.SHELF && hx > 3 ? Part.AIR : p;
            }
            // upper floor
            if (o == 1 && hz == HD - 3 && hx >= 6 && hx <= 8) {
                return Part.RAIL;
            }
            if (o == 1 && hz == HD - 2 && hx == 5) {
                return Part.RAIL;
            }
            if (o == 1 && hz == 1 && (hx == 2 || hx == 3 || hx == 9 || hx == 10)) {
                return Part.BED;
            }
            if (hx == 11 && hz >= 4 && hz <= 6 && o <= 2) {
                return Part.WARDROBE;
            }
            if (o == 1 && hx == 1 && hz == 7) {
                return Part.TABLE_LEG;
            }
            if (o == 2 && hx == 1 && hz == 7) {
                return hh(5, house) < 0.2 ? Part.LANTERN : Part.LAMP;
            }
            if (o == 1 && hx >= 4 && hx <= 8 && hz >= 3 && hz <= 6) {
                return Part.CARPET;
            }
            return Part.AIR;
        }
    }

    /** Block of a lot at lot-local (ax, off, az); identical for all 20 lots. */
    static Part lot(int ax, int off, int az) {
        if (az <= 5) {
            if (off != 0) {
                return Part.AIR;
            }
            return az == 3 && (ax % 8) < 4 ? Part.LINE : Part.STREET;
        }
        if (az <= 7) {
            return off == 0 ? Part.SIDEWALK : Part.AIR;
        }
        int hx = ax - HX0;
        int hz = az - HZ0;
        if (hx >= -1 && hx <= HW && hz >= 0 && hz < HD) {
            Part h = house(hx, off, hz);
            if (h != null) {
                return h;
            }
        }
        if (off == 0) {
            if (ax == HX0 + 6 && az >= 8 && az < HZ0) {
                return Part.PATH;
            }
            return Part.LAWN;
        }
        if (off == 1 && ax == 0) {
            return Part.FENCE;
        }
        return Part.AIR;
    }

    /** House-local block, or null for "lot surface" (lawn/air around the house). */
    static Part house(int hx, int off, int hz) {
        boolean inside = hx >= 0 && hx < HW;
        // Roof: gable along x, rising from the front (hz 0) and back (hz 10) to the ridge (hz 5)
        int mid = HD / 2;
        int roofOff = 6 + Math.min(hz, HD - 1 - hz);
        if (hx == 10 && hz == 7 && off >= 5 && off <= 13) {
            return Part.CHIMNEY;
        }
        if (off == roofOff) {
            return hz < mid ? Part.ROOF_S : hz > mid ? Part.ROOF_N : Part.ROOF;
        }
        if (!inside) {
            return null;
        }
        if (off == 0) {
            return Part.H_FLOOR;
        }
        if (off == 5) {
            return Part.H_ATTIC;
        }
        boolean wallX = hx == 0 || hx == HW - 1;
        boolean wallZ = hz == 0 || hz == HD - 1;
        if (off > 5) {
            if (off < roofOff && wallX) {
                return Part.H_WALL; // gable
            }
            return off < roofOff ? Part.AIR : null;
        }
        // Walls (off 1..4)
        if (wallX && wallZ) {
            return Part.H_CORNER;
        }
        if (wallZ) {
            if (hz == 0 && hx == 6) {
                return off == 1 ? Part.DOOR_LO : off == 2 ? Part.DOOR_HI : Part.H_WALL;
            }
            if ((off == 2 || off == 3) && (hx == 2 || hx == 3 || hx == 9 || hx == 10)) {
                return Part.GLASS;
            }
            return Part.H_WALL;
        }
        if (wallX) {
            if ((off == 2 || off == 3) && (hz == 3 || hz == 4 || hz == 6 || hz == 7)) {
                return Part.GLASS;
            }
            return Part.H_WALL;
        }
        // Furnishing (identical in every house)
        if (hz == HD - 2 && hx >= 1 && hx <= 3 && off <= 3) {
            return Part.SHELF;
        }
        if (hx == 9 && hz == HD - 2) {
            return off == 1 ? Part.TV_STAND : off == 2 ? Part.TV : Part.AIR;
        }
        if (hz == 6 && hx >= 8 && hx <= 10 && off == 1) {
            return Part.SOFA;
        }
        if (hx == 3 && hz == 4) {
            return off == 1 ? Part.TABLE_LEG : off == 2 ? Part.TABLE_TOP : Part.AIR;
        }
        if (hx == 11 && hz == 1) {
            return off == 1 ? Part.BED : Part.AIR;
        }
        if (off == 1 && hx >= 7 && hx <= 11 && hz >= 7 && hz <= 8 && !(hx == 9 && hz == HD - 2)) {
            return Part.CARPET;
        }
        if (hx == 1 && hz == 1 && off <= 2) {
            return off == 1 ? Part.TABLE_LEG : Part.LAMP;
        }
        return Part.AIR;
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(16);

    public Neighbourhood(OfficePlanner p) {
        this.p = p;
    }

    public Site site(int rx, int rz) {
        if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.NEIGHBOURHOOD) {
            return null;
        }
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    private Site build(int rx, int rz) {
        boolean modern = p.cfg.modern();
        int[] range = Megastructures.storeyRange(p, modern ? STOREYS : LEGACY_STOREYS);
        if (range[1] <= range[0]) {
            return null;
        }
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9102, 0));
        int s0 = rng.range(range[0], range[1]);
        return new Site(rx, rz, s0, p.floorY(s0), p.seed, modern);
    }

    public boolean overlaps(int rx, int rz, Room r) {
        if (r == null) {
            return false;
        }
        Site s = site(rx, rz);
        return s != null && Megastructures.overlapsBand(r, s.s0, s.storeys);
    }

    public boolean contains(int x, int y, int z) {
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        Site s = site(rx, rz);
        if (s == null || !s.inBand(y)) {
            return false;
        }
        return x - rx * p.regionBlocks != 0 && z - rz * p.regionBlocks != 0;
    }

    /** World-independent house helpers. */
    public static int houseCount() {
        return COLS * ROWS;
    }

    public static boolean hasResident(int house) {
        return house % 3 == 2;
    }

    /** Region-local x of the house's local origin. */
    public static int houseX(int house) {
        return LOT_X0 + (house % COLS) * LOT_W + HX0;
    }

    public static int houseZ(int house) {
        return LOT_Z0 + (house / COLS) * LOT_D + HZ0;
    }

    public Resident resident(Site s, int house) {
        double u = Hashing.unit(Hashing.hash(p.seed, s.rx, s.rz, 0x9105, house));
        if (u < 0.35) {
            return Resident.SMILER;
        }
        if (u < 0.55) {
            return Resident.HEADLESS_WARDEN;
        }
        if (u < 0.90) {
            return Resident.HOUND;
        }
        return Resident.COLONY;
    }

    /** Region-local street spots (walk level) for roaming creatures. */
    public static int[] streetSpot(Hashing.Rng rng) {
        int r = rng.nextInt(ROWS + 1);
        int lz = r < ROWS ? LOT_Z0 + r * LOT_D + 2 : LOT_Z0 + ROWS * LOT_D + 2;
        int lx = rng.range(6, W - 6);
        return new int[]{lx, lz};
    }
}
