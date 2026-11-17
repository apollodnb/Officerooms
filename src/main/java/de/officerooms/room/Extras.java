package de.officerooms.room;

import de.officerooms.core.Hashing;
import de.officerooms.megastructure.Megastructures;
import de.officerooms.room.structure.BlockKind;
import de.officerooms.special.SpecialRooms;
import de.officerooms.special.SpecialType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 0.9.0: small fixed installations that are laid over the office plan of a region.
 *
 * <p><b>Elevator:</b> every region without a megastructure has one elevator shaft: a 5 × 5 column (3 × 3
 * cabin) at the same place on all storeys. A storey is a stop if at least one side opens onto the floor of
 * an ordinary room or corridor; other storeys are passed.</p>
 *
 * <p><b>Secure storage:</b> rare 5 × 5 strongrooms of unbreakable plating inside ordinary rooms, closed by a
 * security door of level 1–3 (Level Access Cards).</p>
 *
 * <p>Both are part of the block plan ({@code BlockSampler}), so furniture, verification and chunk
 * generation all see the same blocks. Pure Java, depends only on seed and region.</p>
 */
public final class Extras {

    public static final int SIZE = 5;

    public record Vault(int s, int x0, int z0, int level) {
        public boolean covers(int lx, int lz) {
            return lx >= x0 && lx < x0 + SIZE && lz >= z0 && lz < z0 + SIZE;
        }
    }

    /**
     * 0.11.0: a hidden room inside an ordinary office. TYPE: 0 maintenance room, 1 supply closet, 2 forgotten
     * break room, 3 abandoned security office, 4 research lab, 5 concealed room. Interior 5 × 4 behind normal
     * office walls; the doorway (south wall, middle) is filled by the decorator: a loose wall panel, a door or a
     * security door. Some hideouts are {@code empty}: only dust and cobwebs.
     */
    public record Hideout(int s, int x0, int z0, int type, boolean empty) {
        /** Size of the small rooms; 0.12.0: security offices and labs are larger ({@link #w()} x {@link #d()}). */
        public static final int W = 7;
        public static final int D = 6;
        public static final int BIG_W = 11;
        public static final int BIG_D = 8;
        public static final String[] NAMES = {"maintenance room", "supply closet", "forgotten break room",
                "abandoned security office", "research lab", "concealed room"};

        public static boolean big(int type) {
            return type == 3 || type == 4;
        }

        public int w() {
            return big(type) ? BIG_W : W;
        }

        public int d() {
            return big(type) ? BIG_D : D;
        }

        public boolean covers(int lx, int lz) {
            return lx >= x0 && lx < x0 + w() && lz >= z0 && lz < z0 + d();
        }

        public int doorX() {
            return x0 + w() / 2;
        }

        public int doorZ() {
            return z0 + d() - 1;
        }
    }

    public static final class Region {
        public final int rx;
        public final int rz;
        /** Region-local origin of the elevator footprint, -1 if the region has none. */
        public int ex0 = -1;
        public int ez0 = -1;
        /** Per storey: bit 0 west, 1 north, 2 east, 3 south door. */
        public byte[] doors = new byte[0];
        /** Per storey: sides where the neighbouring cell belongs to another room (existing doorways stay open). */
        public byte[] foreign = new byte[0];
        public final List<Vault> vaults = new ArrayList<>();
        public final List<Hideout> hideouts = new ArrayList<>();
        private int vMinX = Integer.MAX_VALUE;
        private int vMaxX = Integer.MIN_VALUE;

        Region(int rx, int rz) {
            this.rx = rx;
            this.rz = rz;
        }

        public boolean hasElevator() {
            return ex0 >= 0;
        }

        public boolean inElevator(int lx, int lz) {
            return ex0 >= 0 && lx >= ex0 && lx < ex0 + SIZE && lz >= ez0 && lz < ez0 + SIZE;
        }

        /** 0.26.0: interior cell of the button panel for a door on side 0 W, 1 N, 2 E, 3 S (next to the door). */
        static final int[] PANEL_FX = {1, 3, 3, 1};
        static final int[] PANEL_FZ = {1, 1, 3, 3};

        /**
         * 0.26.0: side of the first door on storey s (the panel sits beside it), or -1. 0.27.0: never on a wall
         * that another room opens onto (that opening would lead into the panel); then any closed wall, else none.
         */
        public int panelSide(int s) {
            if (s < 0 || s >= doors.length) {
                return -1;
            }
            int f = s < foreign.length ? foreign[s] : 0;
            // the panel cell is a cabin corner: it touches wall d and the next wall (d + 1) % 4
            for (int d = 0; d < 4; d++) {
                if ((doors[s] & (1 << d)) != 0 && (f & (1 << d)) == 0 && (f & (1 << ((d + 1) % 4))) == 0) {
                    return d;
                }
            }
            for (int d = 0; d < 4; d++) {
                if ((f & (1 << d)) == 0 && (f & (1 << ((d + 1) % 4))) == 0) {
                    return d;
                }
            }
            return -1;
        }

        public boolean isStop(int s) {
            return ex0 >= 0 && s >= 0 && s < doors.length && doors[s] != 0;
        }

        /** Next stop from storey {@code s} going {@code dir} (+1 / -1) at least {@code step} storeys away, or -1. */
        public int nextStop(int s, int dir, int step) {
            int last = -1;
            for (int t = s + dir; t >= 0 && t < doors.length; t += dir) {
                if (doors[t] != 0) {
                    last = t;
                    if (Math.abs(t - s) >= step) {
                        return t;
                    }
                }
            }
            return last;
        }

        public Vault vaultAt(int lx, int lz, int s) {
            if (lx < vMinX || lx > vMaxX) {
                return null;
            }
            for (Vault v : vaults) {
                if (v.s == s && v.covers(lx, lz)) {
                    return v;
                }
            }
            return null;
        }

        /** Kind override for a block of the plan; returns {@code base} where nothing is installed. */
        public byte override(int lx, int lz, int s, int pl, byte base) {
            if (inElevator(lx, lz)) {
                if (s >= doors.length) {
                    return base;
                }
                if (pl == 0) {
                    return BlockKind.FLOOR;
                }
                int fx = lx - ex0;
                int fz = lz - ez0;
                boolean shell = fx == 0 || fx == SIZE - 1 || fz == 0 || fz == SIZE - 1;
                if (shell) {
                    int side = fx == 0 && fz == 2 ? 0 : fz == 0 && fx == 2 ? 1 : fx == SIZE - 1 && fz == 2 ? 2
                            : fz == SIZE - 1 && fx == 2 ? 3 : -1;
                    boolean corner = (fx == 0 || fx == SIZE - 1) && (fz == 0 || fz == SIZE - 1);
                    int on = corner ? -1 : fx == 0 ? 0 : fz == 0 ? 1 : fx == SIZE - 1 ? 2 : 3;
                    if (on >= 0 && (foreign[s] & (1 << on)) != 0 && pl >= 2 && BlockKind.isAirLike(base)) {
                        return base; // a doorway of the office plan now opens into the cabin
                    }
                    if (side >= 0 && (doors[s] & (1 << side)) != 0) {
                        if (pl == 1) {
                            return BlockKind.FLOOR_WOOL;
                        }
                        if (pl == 2 || pl == 3) {
                            return BlockKind.AIR;
                        }
                    }
                    return BlockKind.ELEV_WALL;
                }
                if (pl == 1) {
                    return BlockKind.ELEVATOR;
                }
                if (pl == 5 && fx == 2 && fz == 2) {
                    return BlockKind.ELEV_LIGHT;                          // 0.26.0
                }
                int ps = panelSide(s);
                if (pl == 3 && ps >= 0 && fx == PANEL_FX[ps] && fz == PANEL_FZ[ps]) {
                    return (byte) (BlockKind.ELEV_PANEL_E + ps);          // 0.26.0: beside the door, facing into the cabin
                }
                return BlockKind.AIR;
            }
            if (!hideouts.isEmpty() && lx >= vMinX && lx <= vMaxX) {
                for (Hideout h : hideouts) {
                    if (h.s() != s || !h.covers(lx, lz)) {
                        continue;
                    }
                    if (pl <= 1) {
                        return base;                                  // office slab and floor stay; walls reach the slab above
                    }
                    int fx = lx - h.x0();
                    int fz = lz - h.z0();
                    boolean shell = fx == 0 || fx == h.w() - 1 || fz == 0 || fz == h.d() - 1;
                    if (!shell) {
                        return BlockKind.AIR;
                    }
                    return fx == h.w() / 2 && fz == h.d() - 1 && pl <= 3 ? BlockKind.AIR : BlockKind.WALL;
                }
            }
            Vault v = vaultAt(lx, lz, s);
            if (v != null) {
                if (pl == 0) {
                    return base;
                }
                int fx = lx - v.x0;
                int fz = lz - v.z0;
                boolean shell = fx == 0 || fx == SIZE - 1 || fz == 0 || fz == SIZE - 1;
                if (pl == 1 || pl == 5) {
                    return BlockKind.SECURITY;
                }
                if (shell) {
                    return fx == 2 && fz == SIZE - 1 && pl <= 3 ? BlockKind.AIR : BlockKind.SECURITY;
                }
                return BlockKind.AIR;
            }
            return base;
        }

        /** Area the furnisher must keep clear: the installations and the strip in front of a vault door. */
        public boolean reserved(int lx, int lz, int s) {
            if (inElevator(lx, lz)) {
                return true;
            }
            if (lx < vMinX || lx > vMaxX) {
                return false;
            }
            for (Vault v : vaults) {
                if (v.s == s && lx >= v.x0 - 1 && lx <= v.x0 + SIZE && lz >= v.z0 - 1 && lz <= v.z0 + SIZE) {
                    return true;
                }
            }
            for (Hideout h : hideouts) {
                if (h.s() == s && lx >= h.x0() - 1 && lx <= h.x0() + h.w() && lz >= h.z0() - 1 && lz <= h.z0() + h.d() + 1) {
                    return true;
                }
            }
            return false;
        }
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Region>> cache = new PlanCache<>(64);

    public Extras(OfficePlanner p) {
        this.p = p;
    }

    /** Installations of a region, or null if it has none (megastructure regions, non-default grids). */
    public Region of(int rx, int rz) {
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    private static boolean plain(OfficePlanner p, int rx, int rz, Room r, int s) {
        return r != null && r.bottom == s && !r.sealed && (r.kind == Room.Kind.ROOM || r.kind == Room.Kind.CORRIDOR)
                && SpecialRooms.of(p, rx, rz, r) == SpecialType.NONE;
    }

    private Region build(int rx, int rz) {
        if (p.unit != 4 || p.phys != 6 || p.ru < 16 || Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.NONE) {
            return null;
        }
        Region out = new Region(rx, rz);
        RegionLayout l = p.layout(rx, rz);
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9301, 0));
        // Elevator: a cell that is never part of a staircase (and preferably never part of an atrium)
        int[] pick = null;
        for (int pass = 0; pass < 2 && pick == null; pass++) {
            for (int i = 0; i < 40 && pick == null; i++) {
                int ux = rng.range(3, p.ru - 4);
                int uz = rng.range(3, p.ru - 4);
                boolean ok = true;
                for (int s = 0; s < p.storeys && ok; s++) {
                    Room r = l.room(s, ux, uz);
                    if (r != null && (r.kind == Room.Kind.STAIRS || (pass == 0 && r.kind == Room.Kind.ATRIUM))) {
                        ok = false;
                    }
                }
                if (ok) {
                    pick = new int[]{ux, uz};
                }
            }
        }
        if (pick != null) {
            int ux = pick[0];
            int uz = pick[1];
            byte[] doors = new byte[p.storeys];
            byte[] foreign = new byte[p.storeys];
            int[][] nb = {{-1, 0}, {0, -1}, {1, 0}, {0, 1}};
            int stops = 0;
            for (int s = 0; s < p.storeys; s++) {
                int m = 0;
                Room own = l.room(s, ux, uz);
                for (int side = 0; side < 4; side++) {
                    if (l.room(s, ux + nb[side][0], uz + nb[side][1]) != own) {
                        foreign[s] |= (byte) (1 << side);
                    }
                    if (plain(p, rx, rz, l.room(s, ux + nb[side][0], uz + nb[side][1]), s)) {
                        m |= 1 << side;
                    }
                }
                doors[s] = (byte) m;
                stops += m != 0 ? 1 : 0;
            }
            if (stops >= 2) {
                out.ex0 = ux * p.unit;
                out.ez0 = uz * p.unit;
                out.doors = doors;
                out.foreign = foreign;
            }
        }
        // Secure storage rooms
        Hashing.Rng vr = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9302, 0));
        for (int i = 0; i < 3; i++) {
            int s = vr.range(p.cfg.special().darkBottomStoreys(), p.storeys - 2);
            int ux = vr.range(2, p.ru - 4);
            int uz = vr.range(2, p.ru - 4);
            double lv = vr.nextDouble();
            Room r = l.room(s, ux, uz);
            if (!plain(p, rx, rz, r, s) || r.kind != Room.Kind.ROOM || r.top() != s || r.cells < 12) {
                continue;
            }
            boolean same = l.room(s, ux + 1, uz) == r && l.room(s, ux, uz + 1) == r && l.room(s, ux + 1, uz + 1) == r;
            if (!same) {
                continue;
            }
            int x0 = ux * p.unit + 2;
            int z0 = uz * p.unit + 2;
            if (out.ex0 >= 0 && Math.abs(x0 - out.ex0) < 10 && Math.abs(z0 - out.ez0) < 10) {
                continue;
            }
            boolean clash = false;
            for (Vault o : out.vaults) {
                if (o.s == s && Math.abs(o.x0 - x0) < 10 && Math.abs(o.z0 - z0) < 10) {
                    clash = true;
                }
            }
            if (clash) {
                continue;
            }
            out.vaults.add(new Vault(s, x0, z0, lv < 0.6 ? 1 : lv < 0.9 ? 2 : 3));
            out.vMinX = Math.min(out.vMinX, x0);
            out.vMaxX = Math.max(out.vMaxX, x0 + SIZE - 1);
        }
        // 0.11.0: hidden rooms. Six attempts per region (0.27.0: hideout_attempts, 12 in new worlds); each needs
        // 3 × 3 cells of one ordinary single-storey room. The first six attempts stay identical in old worlds.
        Hashing.Rng hr = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9303, 0));
        int[] weights = {25, 20, 18, 15, 12, 10};
        for (int i = 0; i < p.cfg.special().hideoutAttempts(); i++) {
            int s = hr.range(p.cfg.special().darkBottomStoreys(), p.storeys - 2);
            int ux = hr.range(2, p.ru - 5);
            int uz = hr.range(2, p.ru - 5);
            int roll = hr.nextInt(100);
            boolean empty = hr.chance(0.15);
            int type = 0;
            for (int acc = weights[0]; roll >= acc && type < weights.length - 1; acc += weights[++type]) {
            }
            int cw = (Hideout.big(type) ? Hideout.BIG_W : Hideout.W) + 2;
            int cd = (Hideout.big(type) ? Hideout.BIG_D : Hideout.D) + 3;
            int nx = (cw + p.unit - 1) / p.unit;          // cells the room plus its margin needs
            int nz = (cd + p.unit - 1) / p.unit;
            Room r = l.room(s, ux, uz);
            if (ux + nx >= p.ru || uz + nz >= p.ru || !plain(p, rx, rz, r, s) || r.kind != Room.Kind.ROOM || r.top() != s || r.cells < nx * nz) {
                continue;
            }
            boolean same = true;
            for (int a = 0; a < nx && same; a++) {
                for (int b = 0; b < nz && same; b++) {
                    same = l.room(s, ux + a, uz + b) == r;
                }
            }
            if (!same) {
                continue;
            }
            int x0 = ux * p.unit + 2;
            int z0 = uz * p.unit + 2;
            if (out.ex0 >= 0 && Math.abs(x0 - out.ex0) < 14 && Math.abs(z0 - out.ez0) < 14) {
                continue;
            }
            boolean clash = false;
            for (Vault o : out.vaults) {
                clash |= o.s == s && Math.abs(o.x0 - x0) < 14 && Math.abs(o.z0 - z0) < 14;
            }
            for (Hideout o : out.hideouts) {
                clash |= o.s() == s && Math.abs(o.x0() - x0) < 14 && Math.abs(o.z0() - z0) < 14;
            }
            if (clash) {
                continue;
            }
            out.hideouts.add(new Hideout(s, x0, z0, type, empty && type != 5));
            out.vMinX = Math.min(out.vMinX, x0 - 1);
            out.vMaxX = Math.max(out.vMaxX, x0 + cw);
        }
        return out.ex0 < 0 && out.vaults.isEmpty() && out.hideouts.isEmpty() ? null : out;
    }
}
