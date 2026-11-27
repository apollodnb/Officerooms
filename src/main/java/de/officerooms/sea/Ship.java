package de.officerooms.sea;

import de.officerooms.core.Hashing;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 0.42.0: one SS Fun =), a lonely cruise ship in the sea dimension. Pure Java (no Minecraft types), so the layout can be
 * tested on its own.
 *
 * <p>Local coordinates: {@code lx} 0..L-1 from the stern (0) to the bow, {@code lz} 0..W-1 across the ship (centre line
 * at {@link #C}), absolute {@code y}. {@code e = |lz - C|} is the distance from the centre line. Twelve decks of
 * {@link #DH} blocks: floor (o = 0), three blocks of air (o = 1..3), ceiling (o = 4).
 *
 * <ul>
 *     <li>Decks 0–4: crew decks below and at the waterline (white steel, brown carpet, orange lights), the engine hall
 *     over decks 1–3, a flooded bilge on deck 0.</li>
 *     <li>Decks 5–9: passengers. Long corridors with blue carpet, cabins on both sides, three stair cores with twin
 *     staircases, an atrium over decks 5–8, theatre, casino, dining room, kids' club, library, spa, buffet …</li>
 *     <li>Deck 10: the enclosed promenade with the lifeboats, the bridge and suites. Deck 11: the open sun deck.</li>
 *     <li>Only one of the three stair cores goes up past deck 8. The others end at a barricade.</li>
 * </ul>
 */
public final class Ship {

    public static final int L = 300, W = 48, C = 24, KEEL = 36, SEA = 63, DECKS = 12, DH = 5;
    /** Ships sit in cells of 832 x 832 blocks (centre +-48): at least ~430 blocks of open water between two ships. */
    public static final int CELL = 832, JIT = 48;
    /** Start (stern side) of the three stair cores, each {@link #CORE_LEN} long. */
    public static final int[] CORES = {26, 144, 214};
    public static final int CORE_LEN = 12;
    /** End of the superstructure (decks 10 and 11) towards the bow. */
    public static final int SUP0 = 8, SUP1 = L - 62;
    public static final int MID = 150;
    /** Engine hall (decks 1–3). */
    public static final int ENG0 = 42, ENG1 = 104, ENG_E = 12;
    /** Atrium (decks 5–8). */
    public static final int ATR0 = 160, ATR1 = 184;

    // cell codes
    public static final byte O = 0, HU = 1, WA = 2, CO = 3, RM = 4, DR = 5, VO = 7, PR = 8, RA = 9, FD = 10, BL = 11, TOP = 12,
            LO = 13, EL = 14, RL = 15, LANE = 16, ELD = 17;
    // window marks for hull cells
    public static final byte WIN_NONE = 0, WIN_PORT = 1, WIN_BIG = 2;

    public static int fy(int d) {
        return KEEL + 2 + DH * d;
    }

    public enum Kind {
        // passenger cabins
        CABIN, TWIN, MESSY, FLOODED, DARK, BARRICADED, PARTY, STATIC, LUGGAGE, UPSIDE, STRIPPED, SUITE,
        // crew rooms
        CREW_CABIN, STORE, LOCKERS, PIPES, LINEN, TRASH, PUMP, WORKSHOP,
        // venues
        EMBARK, PURSER, SHOPS, CASINO, BAR, LOUNGE, THEATRE, DINING, BUFFET, KIDS, LIBRARY, SPA, GAMES, CAPTAIN, BRIDGE,
        ENGINE, LAUNDRY, CREW_MESS, GALLEY, CREW_OFFICE, MEDICAL, COLD_ROOM, ATRIUM;

        public boolean cabin() {
            return ordinal() <= SUITE.ordinal();
        }

        public boolean crew() {
            return ordinal() >= CREW_CABIN.ordinal() && ordinal() <= WORKSHOP.ordinal();
        }

        public boolean venue() {
            return ordinal() >= EMBARK.ordinal();
        }
    }

    /** A room: interior cells x0..x1, z0..z1 (local, inclusive) on deck d. Door cell (dx, dz) or dx = -1. */
    public static final class Room {
        public final Kind kind;
        public final int d, x0, x1, z0, z1, index;
        public int dx = -1, dz = -1, dirX, dirZ;           // door cell and the direction it opens to (out of the room)
        public final long seed;
        /** Side of the room that faces the hull (+1 / -1 in z), or 0. */
        public int hullSide;

        Room(Kind kind, int d, int x0, int x1, int z0, int z1, int index, long seed) {
            this.kind = kind;
            this.d = d;
            this.x0 = x0;
            this.x1 = x1;
            this.z0 = z0;
            this.z1 = z1;
            this.index = index;
            this.seed = seed;
        }

        public int w() {
            return x1 - x0 + 1;
        }

        public int dep() {
            return z1 - z0 + 1;
        }

        public boolean contains(int lx, int lz) {
            return lx >= x0 && lx <= x1 && lz >= z0 && lz <= z1;
        }
    }

    /** A straight flight of five steps rising from deck d (k = 0 at lx0) in direction dir (+-1 in x), lanes z0..z1. */
    public record Flight(int d, int lx0, int dir, int z0, int z1, boolean steel) {
        /** Step index of column lx, or -1. */
        public int k(int lx) {
            int k = (lx - lx0) * dir;
            return k >= 0 && k <= 4 ? k : -1;
        }
    }

    public final long seed;
    public final int gx, gz, ox, oz;
    public final boolean flip;
    public final int openCore;
    final byte[][] code = new byte[DECKS][L * W];
    final byte[][] aux = new byte[DECKS][L * W];
    final short[][] roomAt = new short[DECKS][L * W];
    public final List<Room> rooms = new ArrayList<>();
    public final List<Flight> flights = new ArrayList<>();
    final boolean[] flightCol = new boolean[L * W];
    /** Interior half-width per deck and lx: cells with e < inner are inside. */
    final int[][] inner = new int[DECKS][L];
    /** Lifeboat stations: lx start, side (+1 / -1). Each boat is 9 long. */
    public final List<int[]> lifeboats = new ArrayList<>();
    private final Hashing.Rng rng;

    // --- placement ---------------------------------------------------------------------------------------------------

    private static final Map<Long, Ship> CACHE = new ConcurrentHashMap<>();
    private static final Map<Long, Boolean> NONE = new ConcurrentHashMap<>();

    /** The ship of cell (gx, gz), or null if this patch of sea is empty. */
    public static Ship of(long seed, int gx, int gz) {
        long key = ((long) gx << 32) ^ (gz & 0xFFFFFFFFL);
        Ship s = CACHE.get(key);
        if (s != null && s.seed == seed) {
            return s;
        }
        if (NONE.containsKey(key ^ seed)) {
            return null;
        }
        long h = Hashing.hash(seed, gx, gz, 0x55F0);
        if (Hashing.unit(h) >= 0.78) {
            NONE.put(key ^ seed, true);
            return null;
        }
        s = new Ship(seed, gx, gz);
        if (CACHE.size() > 24) {
            CACHE.clear();
        }
        CACHE.put(key, s);
        return s;
    }

    /** The ship whose box contains world (x, z), or null. */
    public static Ship at(long seed, int x, int z) {
        Ship s = of(seed, Math.floorDiv(x, CELL), Math.floorDiv(z, CELL));
        return s != null && s.inBox(x, z) ? s : null;
    }

    /** Nearest ship to world (x, z) within a few cells, or null. */
    public static Ship nearest(long seed, int x, int z, int radius) {
        int gx0 = Math.floorDiv(x, CELL), gz0 = Math.floorDiv(z, CELL);
        Ship best = null;
        double bd = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Ship s = of(seed, gx0 + dx, gz0 + dz);
                if (s == null) {
                    continue;
                }
                double d = Math.hypot(s.ox + L / 2.0 - x, s.oz + C - z);
                if (d < bd) {
                    bd = d;
                    best = s;
                }
            }
        }
        return best;
    }

    private Ship(long seed, int gx, int gz) {
        this.seed = seed;
        this.gx = gx;
        this.gz = gz;
        long h = Hashing.hash(seed, gx, gz, 0x5A1);
        this.ox = gx * CELL + CELL / 2 - L / 2 + (int) Math.floorMod(h, 2 * JIT + 1) - JIT;
        this.oz = gz * CELL + CELL / 2 - W / 2 + (int) Math.floorMod(h >>> 20, 2 * JIT + 1) - JIT;
        this.flip = ((h >>> 40) & 1) == 1;
        this.openCore = (int) Math.floorMod(h >>> 44, CORES.length);
        this.rng = new Hashing.Rng(Hashing.hash(seed, gx, gz, 0x5A2));
        build();
    }

    public boolean inBox(int x, int z) {
        return x >= ox && x < ox + L && z >= oz && z < oz + W;
    }

    public int lx(int x) {
        return flip ? ox + L - 1 - x : x - ox;
    }

    public int wx(int lx) {
        return flip ? ox + L - 1 - lx : ox + lx;
    }

    public int lz(int z) {
        return z - oz;
    }

    public int wz(int lz) {
        return oz + lz;
    }

    /** World direction (+1 / -1) of a local x direction. */
    public int wdir(int dir) {
        return flip ? -dir : dir;
    }

    /** Where a boarding passenger arrives: the middle of the embarkation lobby on deck 5 (world x, y, z). */
    public int[] embark() {
        return new int[]{wx(124), fy(5) + 1, wz(C)};
    }

    // --- hull shape --------------------------------------------------------------------------------------------------

    /** Half-width of the hull at lx and height y (cells with e < half are inside the hull). */
    public static int half(int lx, int y) {
        if (lx < 0 || lx >= L) {
            return 0;
        }
        double h;
        if (lx < 16) {
            double t = (16 - lx) / 17.0;
            h = 20 * Math.sqrt(Math.max(0, 1 - t * t));
        } else if (lx > L - 82) {
            double t = (L - 1 - lx) / 81.0;
            h = 20 * Math.pow(Math.max(0, t), 0.62);
        } else {
            h = 20;
        }
        if (y < 54) {
            h -= (54 - y) * 0.55;
        }
        return Math.max(0, (int) Math.floor(h));
    }

    public static int e(int lz) {
        return Math.abs(lz - C);
    }

    public int idx(int lx, int lz) {
        return lx * W + lz;
    }

    public byte code(int d, int lx, int lz) {
        return d < 0 || d >= DECKS || lx < 0 || lx >= L || lz < 0 || lz >= W ? O : code[d][idx(lx, lz)];
    }

    public byte aux(int d, int lx, int lz) {
        return d < 0 || d >= DECKS || lx < 0 || lx >= L || lz < 0 || lz >= W ? 0 : aux[d][idx(lx, lz)];
    }

    public Room room(int d, int lx, int lz) {
        if (d < 0 || d >= DECKS || lx < 0 || lx >= L || lz < 0 || lz >= W) {
            return null;
        }
        int i = roomAt[d][idx(lx, lz)];
        return i < 0 ? null : rooms.get(i);
    }

    public boolean hasFlight(int lx, int lz) {
        return lx >= 0 && lx < L && lz >= 0 && lz < W && flightCol[idx(lx, lz)];
    }

    public int inner(int d, int lx) {
        return lx < 0 || lx >= L ? 0 : inner[d][lx];
    }

    // --- building the plan -------------------------------------------------------------------------------------------

    private void set(int d, int lx, int lz, byte c) {
        if (lx >= 0 && lx < L && lz >= 0 && lz < W) {
            code[d][idx(lx, lz)] = c;
        }
    }

    private void setE(int d, int lx, int s, int e, byte c) {
        set(d, lx, C + s * e, c);
    }

    private byte getE(int d, int lx, int s, int e) {
        return code(d, lx, C + s * e);
    }

    private void build() {
        for (short[] a : roomAt) {
            java.util.Arrays.fill(a, (short) -1);
        }
        for (int d = 0; d < DECKS; d++) {
            int y = fy(d);
            for (int lx = 0; lx < L; lx++) {
                int h0 = half(lx, y);
                int hm = Math.min(h0, Math.min(half(lx - 1, y), half(lx + 1, y)));
                boolean sup = d < 10 || (lx >= SUP0 && lx <= SUP1);
                inner[d][lx] = d == 11 ? 0 : (sup ? Math.max(0, hm - 1) : 0);
                for (int lz = 0; lz < W; lz++) {
                    int e = e(lz);
                    byte c;
                    if (e >= h0) {
                        c = O;
                    } else if (d == 11) {
                        c = sup ? (e >= hm - 1 ? RA : TOP) : O;
                    } else if (d == 10 && !sup) {
                        c = e >= hm - 1 ? RA : TOP;           // open fore and aft deck
                    } else if (e >= hm - 1) {
                        c = HU;
                    } else {
                        c = RM;                               // placeholder: not assigned yet
                    }
                    code[d][idx(lx, lz)] = c;
                }
            }
        }
        for (int d = 0; d <= 4; d++) {
            crewDeck(d);
        }
        for (int d = 5; d <= 9; d++) {
            passengerDeck(d);
        }
        deck10();
        deck11();
        coreFlights();
        fill();
        windows();
        for (Flight f : flights) {
            for (int k = 0; k <= 4; k++) {
                for (int lz = f.z0; lz <= f.z1; lz++) {
                    flightCol[idx(f.lx0 + k * f.dir, lz)] = true;
                }
            }
        }
    }

    private boolean inCore(int lx) {
        for (int c : CORES) {
            if (lx >= c && lx < c + CORE_LEN) {
                return true;
            }
        }
        return false;
    }

    /** Places a venue over lx a..b, the whole width up to the hull, with openings where the corridors meet it. */
    private Room venue(int d, Kind k, int a, int b, int maxE) {
        int idx = rooms.size();
        int z0 = W, z1 = -1;
        for (int lx = a; lx <= b; lx++) {
            int in = Math.min(inner(d, lx), maxE + 1);
            for (int lz = 0; lz < W; lz++) {
                if (e(lz) < in && code(d, lx, lz) != HU && code(d, lx, lz) != O) {
                    set(d, lx, lz, RM);
                    roomAt[d][idx(lx, lz)] = (short) idx;
                    z0 = Math.min(z0, lz);
                    z1 = Math.max(z1, lz);
                }
            }
        }
        Room r = new Room(k, d, a, b, z0, z1, idx, rng.nextLong());
        rooms.add(r);
        // end walls, with openings at the corridor rows
        for (int lx : new int[]{a - 1, b + 1}) {
            for (int lz = 0; lz < W; lz++) {
                int e = e(lz);
                if (e < inner(d, lx) && e <= maxE) {
                    byte cur = code(d, lx, lz);
                    if (cur == CO) {
                        continue;                             // the corridor runs into the venue
                    }
                    set(d, lx, lz, WA);
                    roomAt[d][idx(lx, lz)] = -1;
                }
            }
        }
        if (maxE < 20) {                                      // side walls when the venue is narrower than the ship
            for (int lx = a; lx <= b; lx++) {
                for (int s : new int[]{-1, 1}) {
                    if (maxE + 1 < inner(d, lx) && getE(d, lx, s, maxE + 1) != CO) {
                        setE(d, lx, s, maxE + 1, WA);
                        roomAt[d][idx(lx, C + s * (maxE + 1))] = -1;
                    }
                }
            }
            // two-wide entrances from the corridors running alongside
            int mid = (a + b) / 2;
            for (int s : new int[]{-1, 1}) {
                for (int lx = mid; lx <= mid + 1; lx++) {
                    if (getE(d, lx, s, maxE + 1) == WA && getE(d, lx, s, maxE + 2) == CO) {
                        setE(d, lx, s, maxE + 1, CO);
                    }
                }
            }
        }
        return r;
    }

    private void passengerDeck(int d) {
        // side corridors at e = 8..9, a central wall at e = 0, cabin walls at e = 7 and e = 10
        for (int lx = 0; lx < L; lx++) {
            int in = inner[d][lx];
            for (int s : new int[]{-1, 1}) {
                if (in >= 13) {
                    setE(d, lx, s, 8, CO);
                    setE(d, lx, s, 9, CO);
                    setE(d, lx, s, 7, WA);
                    setE(d, lx, s, 10, WA);
                } else if (in >= 4) {
                    setE(d, lx, s, 1, CO);                      // narrow bow: one central corridor
                    setE(d, lx, s, 2, WA);
                }
            }
            if (in >= 13) {
                set(d, lx, C, WA);
            } else if (in >= 4) {
                set(d, lx, C, CO);
            }
        }
        // cross corridors
        for (int lx = 18 + rng.nextInt(10); lx < L - 70; lx += 26 + rng.nextInt(18)) {
            if (inCore(lx) || inCore(lx + 1)) {
                continue;
            }
            for (int w = 0; w < 2; w++) {
                for (int e = 0; e <= 9; e++) {
                    setE(d, lx + w, 1, e, CO);
                    setE(d, lx + w, -1, e, CO);
                }
            }
        }
        coreCells(d);
        // venues
        switch (d) {
            case 5 -> {
                venue(d, Kind.EMBARK, 112, 136, 20);
                venue(d, Kind.ATRIUM, ATR0, ATR1, 6);
                venue(d, Kind.SHOPS, 188, 208, 6);
                venue(d, Kind.PURSER, 42, 58, 6);
            }
            case 6 -> {
                venue(d, Kind.THEATRE, 232, 266, 20);
                venue(d, Kind.CASINO, 98, 132, 20);
                venue(d, Kind.BAR, 60, 80, 6);
                venue(d, Kind.ATRIUM, ATR0, ATR1, 6);
            }
            case 7 -> {
                venue(d, Kind.DINING, 40, 84, 20);
                venue(d, Kind.THEATRE, 232, 266, 20);
                venue(d, Kind.LOUNGE, 100, 126, 6);
                venue(d, Kind.ATRIUM, ATR0, ATR1, 6);
            }
            case 8 -> {
                venue(d, Kind.KIDS, 98, 126, 20);
                venue(d, Kind.LIBRARY, 58, 76, 6);
                venue(d, Kind.GAMES, 190, 208, 6);
                venue(d, Kind.ATRIUM, ATR0, ATR1, 6);
            }
            default -> {
                venue(d, Kind.BUFFET, 40, 86, 20);
                venue(d, Kind.SPA, 100, 128, 20);
                venue(d, Kind.LOUNGE, 188, 206, 6);
            }
        }
        // the atrium is open over decks 6–8 (galleries at e 6..9 around the void)
        for (int lx = ATR0 + 1; lx < ATR1; lx++) {
            setE(d, lx, 1, 7, CO);
            setE(d, lx, -1, 7, CO);
        }
        if (d >= 6 && d <= 8) {
            for (int lx = ATR0; lx <= ATR1; lx++) {
                for (int lz = 0; lz < W; lz++) {
                    int e = e(lz);
                    if (e <= 5 && lx > ATR0 + 1 && lx < ATR1 - 1) {
                        set(d, lx, lz, VO);
                    } else if (e == 6 && lx > ATR0 && lx < ATR1) {
                        set(d, lx, lz, RL);
                    }
                }
            }
        }
        if (d == 7) {                                       // the theatre is two decks tall
            Room t = null;
            for (Room r : rooms) {
                if (r.d == 7 && r.kind == Kind.THEATRE) {
                    t = r;
                }
            }
            if (t != null) {
                for (int lx = t.x0; lx <= t.x1; lx++) {
                    for (int lz = t.z0; lz <= t.z1; lz++) {
                        if (code(7, lx, lz) == RM && roomAt[7][idx(lx, lz)] == t.index) {
                            set(7, lx, lz, VO);
                        }
                    }
                }
            }
        }
        cabins(d, false);
        hazards(d);
    }

    private void crewDeck(int d) {
        for (int lx = 0; lx < L; lx++) {
            int in = inner[d][lx];
            if (in >= 4 && lx > 6) {
                for (int e = 0; e <= 1; e++) {
                    setE(d, lx, 1, e, CO);
                    setE(d, lx, -1, e, CO);
                }
                setE(d, lx, 1, 2, WA);
                setE(d, lx, -1, 2, WA);
            }
        }
        // narrow side passages, broken into pieces
        for (int s : new int[]{-1, 1}) {
            int lx = 10;
            while (lx < L - 40) {
                int len = 14 + rng.nextInt(40);
                boolean on = rng.nextDouble() < 0.7;
                for (int k = 0; k < len && lx < L - 40; k++, lx++) {
                    if (inner[d][lx] >= 13 && on) {
                        setE(d, lx, s, 9, CO);
                        setE(d, lx, s, 8, WA);
                        setE(d, lx, s, 10, WA);
                    }
                }
            }
        }
        // cross passages (one or two wide), some end in nothing
        for (int lx = 12 + rng.nextInt(8); lx < L - 50; lx += 16 + rng.nextInt(16)) {
            if (inCore(lx) || inCore(lx + 1)) {
                continue;
            }
            int w = rng.chance(0.4) ? 2 : 1;
            for (int s : new int[]{-1, 1}) {
                int reach = rng.chance(0.15) ? 5 : 9;
                for (int k = 0; k < w; k++) {
                    for (int e = 2; e <= reach; e++) {
                        setE(d, lx + k, s, e, CO);
                    }
                }
            }
        }
        coreCells(d);
        switch (d) {
            case 0 -> {
                venue(d, Kind.PUMP, 60, 76, 7);
                venue(d, Kind.PUMP, 180, 194, 7);
            }
            case 1 -> {
                venue(d, Kind.ENGINE, ENG0, ENG1, ENG_E);
                venue(d, Kind.WORKSHOP, 116, 134, 7);
            }
            case 2 -> {
                venue(d, Kind.LAUNDRY, 112, 138, 7);
                venue(d, Kind.STORE, 162, 182, 7);
            }
            case 3 -> {
                venue(d, Kind.CREW_MESS, 110, 136, 7);
                venue(d, Kind.GALLEY, 160, 190, 7);
            }
            default -> {
                venue(d, Kind.CREW_OFFICE, 110, 130, 7);
                venue(d, Kind.MEDICAL, 160, 174, 7);
                venue(d, Kind.COLD_ROOM, 180, 194, 7);
            }
        }
        // the engine hall is open from deck 1 up to deck 3; catwalks along its sides
        if (d == 2 || d == 3) {
            for (int lx = ENG0; lx <= ENG1; lx++) {
                for (int lz = 0; lz < W; lz++) {
                    int e = e(lz);
                    if (e <= ENG_E && e < inner[d][lx]) {
                        boolean walk = e >= ENG_E - 1 || lx <= ENG0 + 1 || lx >= ENG1 - 1;
                        set(d, lx, lz, walk ? LANE : VO);           // LANE here = steel grate catwalk
                        roomAt[d][idx(lx, lz)] = -1;
                        if (!walk && (e == ENG_E - 2 || lx == ENG0 + 2 || lx == ENG1 - 2)) {
                            set(d, lx, lz, RL);
                        }
                    }
                }
                for (int s : new int[]{-1, 1}) {
                    if (ENG_E + 1 < inner[d][lx]) {
                        setE(d, lx, s, ENG_E + 1, WA);
                    }
                }
            }
            for (int lx : new int[]{ENG0 - 1, ENG1 + 1}) {
                for (int e = 0; e <= ENG_E; e++) {
                    for (int s : new int[]{-1, 1}) {
                        if (getE(d, lx, s, e) != CO) {
                            setE(d, lx, s, e, WA);
                        }
                    }
                }
            }
        }
        cabins(d, true);
        if (d == 0) {
            for (Room r : rooms) {
                if (r.d == 0 && r.kind.crew() && rng.chance(0.5)) {
                    // half of the bilge rooms are flooded: marked by aux = 1 on their cells
                    for (int lx = r.x0; lx <= r.x1; lx++) {
                        for (int lz = r.z0; lz <= r.z1; lz++) {
                            if (roomAt[0][idx(lx, lz)] == r.index) {
                                aux[0][idx(lx, lz)] = 1;
                            }
                        }
                    }
                }
            }
        }
        hazards(d);
    }

    private void deck10() {
        int d = 10;
        for (int lx = SUP0; lx <= SUP1; lx++) {
            int in = inner[d][lx];
            if (in < 10) {
                continue;
            }
            for (int s : new int[]{-1, 1}) {
                for (int e = in - 5; e <= in - 1; e++) {
                    setE(d, lx, s, e, PR);
                }
                setE(d, lx, s, in, RA);                       // the hull is open here: a railing
                setE(d, lx, s, in - 6, WA);
                setE(d, lx, s, 0, CO);
                setE(d, lx, s, 1, CO);
                setE(d, lx, s, 2, WA);
            }
        }
        coreCells(d);
        venue(d, Kind.BRIDGE, SUP1 - 10, SUP1 - 1, 20);
        for (int lx = SUP1 - 10; lx <= SUP1; lx++) {
            for (int s : new int[]{-1, 1}) {
                setE(d, lx, s, inner[d][lx], HU);
            }
        }
        venue(d, Kind.CAPTAIN, 192, 206, 9);
        // the promenade is the only way to the lifeboats; open doors from the corridor every ~40 blocks
        for (int lx = 30; lx < SUP1 - 20; lx += 36 + rng.nextInt(12)) {
            if (inCore(lx)) {
                continue;
            }
            for (int s : new int[]{-1, 1}) {
                for (int e = 2; e <= inner[d][lx] - 6; e++) {
                    setE(d, lx, s, e, CO);
                }
            }
        }
        // lifeboats along both sides, and exterior stairs up to the sun deck
        for (int lx = 20; lx + 9 < SUP1 - 18; lx += 19) {
            lifeboats.add(new int[]{lx, 1});
            lifeboats.add(new int[]{lx, -1});
        }
        for (int lx : new int[]{60, 190}) {
            int in = inner[d][lx];
            flights.add(new Flight(10, lx, 1, C + in - 5, C + in - 4, false));
            flights.add(new Flight(10, lx, 1, C - in + 4, C - in + 5, false));
        }
        cabins(d, false);
    }

    private void deck11() {
        // railing around the stair holes on the sun deck (the flights come up from the promenade)
        for (Flight f : flights) {
            if (f.d() != 10) {
                continue;
            }
            for (int k = -1; k <= 3; k++) {
                set(11, f.lx0() + k, f.z0() - 1, RL);
                set(11, f.lx0() + k, f.z1() + 1, RL);
            }
            for (int lz = f.z0(); lz <= f.z1(); lz++) {
                set(11, f.lx0() - 1, lz, RL);
            }
        }
    }

    /** Stair core cells on deck d: lanes at e 3..4 and 6..7, closed elevators at e <= 2, lobbies at both ends. */
    private void coreCells(int d) {
        for (int cx : CORES) {
            for (int lx = cx; lx < cx + CORE_LEN; lx++) {
                for (int lz = 0; lz < W; lz++) {
                    int e = e(lz);
                    if (e >= inner[d][lx]) {
                        continue;
                    }
                    byte cur = code(d, lx, lz);
                    if (d == 10 && (cur == PR || cur == RA || e >= inner[d][lx] - 6)) {
                        continue;
                    }
                    roomAt[d][idx(lx, lz)] = -1;
                    boolean mid = lx >= cx + 3 && lx <= cx + 8;
                    if (e <= 2) {
                        set(d, lx, lz, mid ? EL : LO);
                        if ((lx == cx + 3 || lx == cx + 8) && e <= 1) {
                            set(d, lx, lz, ELD);
                        }
                    } else if (e <= 7) {
                        set(d, lx, lz, mid ? (e == 5 ? RL : LANE) : LO);
                    } else if (e <= 9) {
                        set(d, lx, lz, CO);
                    } else {
                        set(d, lx, lz, LO);                       // window lounge at the side
                    }
                }
            }
        }
    }

    /** The twin staircases: lane A (e 3..4) rises on even decks towards +x, lane B (e 6..7) on odd decks towards -x. */
    private void coreFlights() {
        for (int ci = 0; ci < CORES.length; ci++) {
            int cx = CORES[ci];
            for (int d = 0; d <= 9; d++) {
                boolean open = ci == openCore || d < 8;
                boolean even = d % 2 == 0;
                for (int s : new int[]{-1, 1}) {
                    int za = s > 0 ? C + 3 : C - 4, zb = s > 0 ? C + 6 : C - 7;
                    int z0 = even ? za : zb;
                    if (open) {
                        flights.add(even ? new Flight(d, cx + 3, 1, z0, z0 + 1, d <= 4) : new Flight(d, cx + 8, -1, z0, z0 + 1, d <= 4));
                        if (!even) {                                  // banister in the corridor next to the hole above
                            for (int lx = cx + 5; lx <= cx + 8; lx++) {
                                set(d + 1, lx, s > 0 ? C + 8 : C - 8, RL);
                            }
                        }
                        int cap = even ? cx + 2 : cx + 9;
                        set(d + 1, cap, z0, RL);
                        set(d + 1, cap, z0 + 1, RL);
                    } else if (d == 8) {
                        for (int lx = cx + 3; lx <= cx + 7; lx++) {  // barricade where the stairs would go up
                            set(d, lx, z0, BL);
                            set(d, lx, z0 + 1, BL);
                        }
                    }
                }
            }
        }
    }

    /** Fire doors and barricades in long corridors (some routes are simply closed). */
    private void hazards(int d) {
        for (int lx = 20 + rng.nextInt(20); lx < L - 60; lx += 30 + rng.nextInt(30)) {
            if (inCore(lx) || inCore(lx - 1) || inCore(lx + 1) || (d == 5 && lx > 100 && lx < 150)) {
                continue;
            }
            int s = rng.chance(0.5) ? 1 : -1;
            byte what = rng.chance(0.6) ? FD : BL;
            int[] es = d <= 4 ? new int[]{9} : new int[]{8, 9};
            boolean ok = true;
            for (int e : es) {
                ok &= getE(d, lx, s, e) == CO && getE(d, lx - 1, s, e) == CO && getE(d, lx + 1, s, e) == CO;
            }
            if (ok && rng.chance(0.55)) {
                for (int e : es) {
                    setE(d, lx, s, e, what);
                }
            }
        }
    }

    /** Cabins (or crew rooms) in all cells that are still unassigned. */
    private void cabins(int d, boolean crew) {
        for (int s : new int[]{-1, 1}) {
            int[][] strips = d == 10 ? new int[][]{{3, 99}} : crew ? new int[][]{{3, 7}, {11, 99}} : new int[][]{{1, 6}, {11, 99}, {3, 99}};
            for (int[] st : strips) {
                int lx = 0;
                while (lx < L) {
                    int e0 = st[0];
                    if (!free(d, lx, s, e0)) {
                        lx++;
                        continue;
                    }
                    int wmax = crew ? 4 + rng.nextInt(6) : d == 10 ? 6 + rng.nextInt(3) : 3 + rng.nextInt(3);
                    int w = 0;
                    while (w < wmax && lx + w < L && free(d, lx + w, s, e0)) {
                        w++;
                    }
                    // depth: as far as the strip goes and the cells are free
                    int e1 = e0;
                    while (e1 + 1 <= st[1] && free(d, lx, s, e1 + 1) && free(d, lx + w - 1, s, e1 + 1)) {
                        e1++;
                    }
                    if (w < 2 || e1 - e0 < 1) {
                        for (int k = 0; k < Math.max(1, w); k++) {
                            for (int e = e0; e <= e1; e++) {
                                if (free(d, lx + k, s, e)) {
                                    setE(d, lx + k, s, e, WA);
                                }
                            }
                        }
                        lx += Math.max(1, w);
                        continue;
                    }
                    int z0 = Math.min(C + s * e0, C + s * e1), z1 = Math.max(C + s * e0, C + s * e1);
                    Kind k = crew ? crewKind() : d == 10 ? Kind.SUITE : cabinKind();
                    Room r = new Room(k, d, lx, lx + w - 1, z0, z1, rooms.size(), rng.nextLong());
                    r.hullSide = st[0] >= 11 ? s : 0;
                    rooms.add(r);
                    for (int x = lx; x < lx + w; x++) {
                        for (int e = e0; e <= e1; e++) {
                            setE(d, x, s, e, RM);
                            roomAt[d][idx(x, C + s * e)] = (short) r.index;
                        }
                    }
                    // the wall column after the cabin
                    for (int e = e0; e <= e1; e++) {
                        if (free(d, lx + w, s, e)) {
                            setE(d, lx + w, s, e, WA);
                        }
                    }
                    door(r, s, e0, e1);
                    lx += w + 1;
                }
            }
        }
    }

    private boolean free(int d, int lx, int s, int e) {
        return lx >= 0 && lx < L && e < inner[d][lx] && getE(d, lx, s, e) == RM && roomAt[d][idx(lx, C + s * e)] < 0;
    }

    /** Door in the wall towards the nearest corridor (inner side first, then outer). */
    private void door(Room r, int s, int e0, int e1) {
        int[] tries = rng.chance(0.5) ? new int[]{e0 - 1, e1 + 1} : new int[]{e1 + 1, e0 - 1};
        for (int we : tries) {
            if (we <= 0) {
                continue;
            }
            int ce = we < e0 ? we - 1 : we + 1;
            int start = rng.nextInt(r.w());
            for (int i = 0; i < r.w(); i++) {
                int lx = r.x0 + (start + i) % r.w();
                if (getE(r.d, lx, s, ce) == CO && (getE(r.d, lx, s, we) == WA || getE(r.d, lx, s, we) == HU && false)) {
                    setE(r.d, lx, s, we, DR);
                    int dir = (ce > we ? 1 : -1) * s;               // z direction out of the room
                    aux[r.d][idx(lx, C + s * we)] = (byte) (dir > 0 ? 1 : 3);   // 1 = +z (south), 3 = -z (north)
                    r.dx = lx;
                    r.dz = C + s * we;
                    r.dirZ = dir;
                    return;
                }
            }
        }
    }

    private Kind cabinKind() {
        double u = rng.nextDouble();
        Kind[] k = {Kind.CABIN, Kind.TWIN, Kind.MESSY, Kind.FLOODED, Kind.DARK, Kind.BARRICADED, Kind.PARTY, Kind.STATIC, Kind.LUGGAGE,
                Kind.UPSIDE, Kind.STRIPPED};
        double[] p = {0.30, 0.20, 0.10, 0.05, 0.07, 0.04, 0.05, 0.05, 0.05, 0.03, 0.06};
        double acc = 0;
        for (int i = 0; i < k.length; i++) {
            acc += p[i];
            if (u < acc) {
                return k[i];
            }
        }
        return Kind.CABIN;
    }

    private Kind crewKind() {
        Kind[] k = {Kind.CREW_CABIN, Kind.CREW_CABIN, Kind.CREW_CABIN, Kind.STORE, Kind.STORE, Kind.LOCKERS, Kind.PIPES, Kind.LINEN, Kind.TRASH,
                Kind.WORKSHOP};
        return k[rng.nextInt(k.length)];
    }

    /** Whatever is still unassigned becomes solid wall (behind walls, under tapering bows). */
    private void fill() {
        for (int d = 0; d < DECKS; d++) {
            for (int i = 0; i < L * W; i++) {
                if (code[d][i] == RM && roomAt[d][i] < 0) {
                    code[d][i] = WA;
                }
            }
        }
    }

    /** Windows in the hull: portholes in cabins and crew rooms, big windows in venues and along the promenade. */
    private void windows() {
        for (int d = 0; d <= 10; d++) {
            for (int lx = 0; lx < L; lx++) {
                for (int lz = 0; lz < W; lz++) {
                    if (code[d][idx(lx, lz)] != HU) {
                        continue;
                    }
                    int s = lz > C ? 1 : -1;
                    Room r = room(d, lx, lz - s);
                    if (r == null) {
                        if (code(d, lx, lz - s) == CO && lx % 7 == 3 && d >= 5) {
                            aux[d][idx(lx, lz)] = WIN_PORT;
                        } else if (code(d, lx, lz - s) == LO && d >= 5) {
                            aux[d][idx(lx, lz)] = WIN_BIG;
                        }
                        continue;
                    }
                    if (r.kind.venue()) {
                        aux[d][idx(lx, lz)] = (r.kind == Kind.THEATRE || r.kind == Kind.ENGINE || r.kind == Kind.CASINO || d <= 4)
                                ? (lx % 5 == 2 && d <= 4 ? WIN_PORT : WIN_NONE) : WIN_BIG;
                    } else if (r.kind == Kind.SUITE) {
                        aux[d][idx(lx, lz)] = WIN_BIG;
                    } else if (lx == (r.x0 + r.x1) / 2 && r.kind != Kind.DARK) {
                        aux[d][idx(lx, lz)] = WIN_PORT;
                    }
                }
            }
        }
    }
}
