package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.Room;
import java.util.Optional;

/**
 * 0.14.0: The Abandoned Hotel, a megastructure as rare as the others. One region (160 × 160 blocks) replaces
 * {@link #STOREYS} storeys with four hotel levels (6 blocks each, like office storeys):
 * <ul>
 *   <li>level 0: the basement (bare concrete, pipes, a few lights) with a security vault;</li>
 *   <li>level 1: the lobby, one huge open hall with marble floor, columns, sofas and the reception. Office doors of
 *       storey s0 + 1 open into it;</li>
 *   <li>levels 2 and 3: guest floors. Double-loaded corridors with rooms on both sides, joined by a wide central
 *       aisle that also holds the staircases.</li>
 * </ul>
 * Heights are relative to {@code yBase} (the wool layer of storey s0). Within a level: 0 floor, 1–4 walk space,
 * 5 ceiling. Pure Java: the layout depends only on seed and region and is evaluated per block.
 */
public final class Hotel {

    /** 0.36.0: new worlds get 7 storeys (basement, lobby, 5 guest floors); old worlds keep 4. */
    public static final int STOREYS = 7;
    public static final int LEGACY_STOREYS = 4;
    public static final int H = STOREYS * 6;
    static final int W = 160;

    /** Guest floor grid: rooms 7 wide along x starting at X0, modules of 20 along z starting at Z0. */
    static final int X0 = 3;
    static final int CW = 7;
    static final int Z0 = 3;
    static final int MZ = 20;
    static final int ROWS = 7;
    static final int COLS = 22;
    /** Central aisle (cols 10 and 11) that links all corridors and holds the staircases. */
    static final int AX0 = 73;
    static final int AX1 = 86;
    /** Staircase lanes: level i climbs to level i + 1 along +z from lz = STAIR_Z[i]. */
    static final int LX0 = 84;
    static final int LX1 = 85;
    static final int[] STAIR_Z = {20, 60, 100};
    /** Vault in the basement: 9 × 9. */
    public static final int VS = 9;

    public enum Part {
        AIR, OFFICE, RING, CEILING, CARPET, MARBLE, WAINSCOT, WALLPAPER, LAMP, LAMP_BROKEN,
        DOOR_A_LO, DOOR_A_HI, DOOR_A_OPEN_LO, DOOR_A_OPEN_HI, DOOR_B_LO, DOOR_B_HI, DOOR_B_OPEN_LO, DOOR_B_OPEN_HI,
        BED_A, BED_B, BEDF_A, BEDF_B, NIGHT_A, NIGHT_B, DRESSER, TV,
        STAIR, STAIR_SUPPORT, RAILING, COLUMN, RECEPTION, SOFA_N, SOFA_S, PLANT, RUG,
        B_FLOOR, B_CEIL, B_PILLAR, PIPE, B_LIGHT, CRATE, LAUNDRY, SECURITY,
        // 0.28.0: hotel rework (new worlds)
        CARPET2, PHONE_A, PHONE_B, STATIC_TV, BROKEN_TV, MAGAZINES, COBWEB, BARRICADE, BARRICADE_TOP,
        SCONCE_S, SCONCE_N, ICE_S, CART_N, IRON_A_LO, IRON_A_HI, IRON_B_LO, IRON_B_HI,
        // 0.36.0: labyrinth hotel; groups of four are facings E, S, W, N (see dirBase)
        DOOR_X_LO, DOOR_X_HI, DOOR_X_OPEN_LO, DOOR_X_OPEN_HI, IRON_X_LO, IRON_X_HI,
        TILE, TUB, SINK, SHELF, TABLE, TABLE_TOP, WOOD_FLOOR, PLANT2, WARDROBE,
        STAIR_0, STAIR_1, STAIR_2, STAIR_3, BED_0, BED_1, BED_2, BED_3, BEDF_0, BEDF_1, BEDF_2, BEDF_3,
        NIGHT_0, NIGHT_1, NIGHT_2, NIGHT_3, DRESS_0, DRESS_1, DRESS_2, DRESS_3, TV_0, TV_1, TV_2, TV_3,
        STV_0, STV_1, STV_2, STV_3, BTV_0, BTV_1, BTV_2, BTV_3, PHONE_0, PHONE_1, PHONE_2, PHONE_3,
        SOFA_0, SOFA_1, SOFA_2, SOFA_3, SCONCE_0, SCONCE_1, SCONCE_2, SCONCE_3, CART_0, CART_1, CART_2, CART_3,
        ICE_0, ICE_1, ICE_2, ICE_3, TOILET_0, TOILET_1, TOILET_2, TOILET_3, CHAIR_0, CHAIR_1, CHAIR_2, CHAIR_3
    }

    static final Part[] DIR_BASES = {Part.STAIR_0, Part.BED_0, Part.BEDF_0, Part.NIGHT_0, Part.DRESS_0, Part.TV_0, Part.STV_0,
            Part.BTV_0, Part.PHONE_0, Part.SOFA_0, Part.SCONCE_0, Part.CART_0, Part.ICE_0, Part.TOILET_0, Part.CHAIR_0};

    /** The first part of a group of four facings (E, S, W, N) that contains {@code p}, or null. */
    public static Part dirBase(Part p) {
        for (Part b : DIR_BASES) {
            if (p.ordinal() >= b.ordinal() && p.ordinal() < b.ordinal() + 4) {
                return b;
            }
        }
        return null;
    }

    /** Rotated variant of a direction group: always pass the _0 base (0.38.1: STAIR_1 + 3 used to give BED_0). */
    static Part dir(Part base, int d) {
        return Part.values()[base.ordinal() + ((d % 4) + 4) % 4];
    }

    /** 0.28.0: kinds of guest rooms (new worlds; old worlds only know STANDARD and EMPTY). */
    public enum RoomKind { STANDARD, TWIN, TRASHED, BARRICADED, STATIC, PHONE, EMPTY }

    public static final class Site {
        public final int rx;
        public final int rz;
        public final int s0;
        public final int yBase;
        final long seed;
        /** Region-local corner of the basement vault and its security level (2 or 3). */
        public final int vx;
        public final int vz;
        public final int vaultLevel;
        /** 0.28.0: hotel rework active (content version 1, new worlds). */
        public final boolean modern;
        /** 0.36.0: storeys and height of this site (7 in new worlds, 4 in old ones). */
        public final int storeys;
        public final int h;
        /** 0.36.0: floor layouts of the lobby (1) and the guest floors (2…), new worlds only. */
        final HotelLayout[] floors;

        Site(int rx, int rz, int s0, int yBase, long seed, int vx, int vz, int vaultLevel, boolean modern) {
            this.modern = modern;
            this.storeys = modern ? STOREYS : LEGACY_STOREYS;
            this.h = storeys * 6;
            this.floors = new HotelLayout[storeys];
            if (modern) {
                java.util.List<int[]> below = java.util.List.of();
                for (int lv = 1; lv < storeys; lv++) {
                    floors[lv] = new HotelLayout(seed, rx, rz, lv, below, lv == 1, lv == storeys - 1);
                    below = floors[lv].up;
                }
            }
            this.rx = rx;
            this.rz = rz;
            this.s0 = s0;
            this.yBase = yBase;
            this.seed = seed;
            this.vx = vx;
            this.vz = vz;
            this.vaultLevel = vaultLevel;
        }

        public boolean inBand(int y) {
            return y >= yBase && y < yBase + h;
        }

        /** Hotel level (0 basement, 1 lobby, 2–3 guest floors) of a world y inside the band. */
        public int level(int y) {
            return (y - yBase) / 6;
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

        double roomHash(int level, int row, int col, int band) {
            return Hashing.unit(Hashing.hash(seed, rx, rz, 0x9401 + level, row * 64 + col * 2 + band));
        }

        /** Room is empty (stripped bare). */
        public boolean emptyRoom(int level, int row, int col, int band) {
            return roomHash(level, row, col, band) < 0.12;
        }

        /** Room holds a loot chest (decorated in the FEATURES stage). */
        public boolean chestRoom(int level, int row, int col, int band) {
            double u = roomHash(level, row, col, band);
            return u >= 0.12 && u < 0.30;
        }

        boolean doorOpen(int level, int row, int col, int band) {
            if (roomKind(level, row, col, band) == RoomKind.BARRICADED) {
                return false;
            }
            double u = roomHash(level, row, col, band);
            return u < 0.12 || u > 0.72;
        }

        /** 0.28.0: the kind of a guest room. */
        public RoomKind roomKind(int level, int row, int col, int band) {
            if (emptyRoom(level, row, col, band)) {
                return RoomKind.EMPTY;
            }
            if (!modern) {
                return RoomKind.STANDARD;
            }
            double t = Hashing.unit(Hashing.hash(seed, rx, rz, 0x9481 + level, row * 64 + col * 2 + band));
            return t < 0.22 ? RoomKind.TWIN : t < 0.34 ? RoomKind.TRASHED : t < 0.40 ? RoomKind.BARRICADED
                    : t < 0.46 ? RoomKind.STATIC : t < 0.51 ? RoomKind.PHONE : RoomKind.STANDARD;
        }

        double corridorHash(int level, int row, int col, int salt) {
            return Hashing.unit(Hashing.hash(seed, rx, rz, 0x9490 + level * 8 + salt, row * 64 + col));
        }

        public Part part(int lx, int off, int lz, boolean doorOpen) {
            if (lx == 0 || lz == 0) {
                return Part.OFFICE;
            }
            if (off >= h - 1) {
                return Part.CEILING;
            }
            if (ringSide(lx, lz) >= 0) {
                return off >= 7 && off <= 9 && doorOpen ? Part.AIR : Part.RING;
            }
            Part st = stairs(lx, off, lz, modern);
            if (st != null) {
                return st;
            }
            int lv = off / 6;
            int r = off % 6;
            if (modern && lv >= 1) {
                Part ls = lStairs(lv, lx, r, lz);
                if (ls != null) {
                    return ls;
                }
                if (lv >= 2) {
                    return labyrinth(lv, lx, r, lz);
                }
            }
            return switch (lv) {
                case 0 -> basement(lx, r, lz);
                case 1 -> lobby(lx, r, lz);
                default -> guest(lv, lx, r, lz);
            };
        }

        /** Staircase lanes and the railings around their openings, or null. */
        static Part stairs(int lx, int off, int lz, boolean modern) {
            for (int i = 0; i < (modern ? 1 : STAIR_Z.length); i++) {
                int f = i * 6;
                int o = off - f;
                if (o < 1 || o > 7) {
                    continue;
                }
                int k = lz - STAIR_Z[i];
                if (lx >= LX0 && lx <= LX1 && k >= 0 && k <= 5) {
                    if (k == 5) {
                        return o == 6 ? null : o < 6 ? Part.STAIR_SUPPORT : null;
                    }
                    if (o == k + 1) {
                        return Part.STAIR;
                    }
                    return o <= k ? Part.STAIR_SUPPORT : Part.AIR;
                }
                if (o == 7 && ((k >= 0 && k <= 4 && (lx == LX0 - 1 || lx == LX1 + 1)) || (k == -1 && lx >= LX0 && lx <= LX1))) {
                    return Part.RAILING;
                }
            }
            return null;
        }

        Part basement(int lx, int r, int lz) {
            boolean inVault = lx >= vx && lx < vx + VS && lz >= vz && lz < vz + VS;
            if (inVault) {
                if (r == 0 || r == 5) {
                    return Part.SECURITY;
                }
                boolean shell = lx == vx || lx == vx + VS - 1 || lz == vz || lz == vz + VS - 1;
                if (!shell) {
                    return Part.AIR;
                }
                return lx == vx + VS / 2 && lz == vz + VS - 1 && r <= 2 ? Part.AIR : Part.SECURITY;
            }
            if (r == 0) {
                return Part.B_FLOOR;
            }
            if (r == 5) {
                return lx % 12 == 0 && lz % 12 == 4 && Hashing.unit(Hashing.hash(seed, lx, lz, 0x9410, 0)) < 0.35 ? Part.B_LIGHT : Part.B_CEIL;
            }
            if (lx % 12 == 6 && lz % 12 == 6) {
                return Part.B_PILLAR;
            }
            if (r == 4 && (lz % 12 == 0 || lz % 12 == 1) && lx % 12 != 6) {
                return Part.PIPE;
            }
            if (r <= 2) {
                double u = Hashing.unit(Hashing.hash(seed, lx, lz, 0x9411, 0));
                if (u < 0.012 && (lx < AX0 - 2 || lx > AX1 + 2)) {
                    return r == 1 || u < 0.006 ? Part.CRATE : Part.AIR;
                }
                if (u > 0.994 && r == 1 && (lx < AX0 - 2 || lx > AX1 + 2)) {
                    return Part.LAUNDRY;
                }
            }
            return Part.AIR;
        }

        Part lobby(int lx, int r, int lz) {
            if (r == 0) {
                return Part.MARBLE;
            }
            if (modern && r < 5 && floors[1] != null) {
                int cx = Math.floorDiv(lx - HotelLayout.ORG, HotelLayout.CS);
                int cz = Math.floorDiv(lz - HotelLayout.ORG, HotelLayout.CS);
                if (floors[1].isUp(cx, cz)) {
                    return Part.AIR;                                         // 0.36.0: staircase up, keep it clear
                }
            }
            if (r == 5) {
                if (lx % 8 == 4 && lz % 8 == 4) {
                    return Hashing.unit(Hashing.hash(seed, lx, lz, 0x9412, 0)) < 0.25 ? Part.LAMP_BROKEN : Part.LAMP;
                }
                return Part.CEILING;
            }
            if (lx % 16 >= 8 && lx % 16 <= 9 && lz % 16 >= 8 && lz % 16 <= 9 && !(lx >= LX0 - 1 && lx <= LX1 + 1)) {
                return Part.COLUMN;
            }
            if (lx >= 62 && lx <= 81 && lz == 76) {                              // wall behind the reception
                return r == 1 ? Part.WAINSCOT : Part.WALLPAPER;
            }
            if (r == 1 && lx >= 64 && lx <= 79 && lz == 72) {
                return Part.RECEPTION;
            }
            if (r == 1 && lx >= 40 && lx <= 119 && lz >= 56 && lz <= 66 && (lx < 62 || lx > 81 || lz < 60)) {
                return Part.RUG;
            }
            for (int[] g : new int[][]{{24, 30}, {120, 30}, {24, 124}, {120, 124}, {44, 96}, {104, 96}}) {   // sofa groups
                int ax = lx - g[0];
                int az = lz - g[1];
                if (r == 1 && ax >= 0 && ax <= 3) {
                    if (az == 0) {
                        return Part.SOFA_S;
                    }
                    if (az == 4) {
                        return Part.SOFA_N;
                    }
                }
                if (r == 1 && az == 2 && (ax == -1 || ax == 4)) {
                    return Part.PLANT;
                }
            }
            return Part.AIR;
        }

        Part guest(int lv, int lx, int r, int lz) {
            boolean aisle = lx >= AX0 && lx <= AX1;
            int row = Math.floorDiv(lz - Z0, MZ);
            Part carpet = modern && lv == 3 ? Part.CARPET2 : Part.CARPET;
            if (aisle && lx == AX0 && row >= 0 && row < ROWS && r >= 1 && r <= 4) {
                int az0 = lz - Z0 - row * MZ;
                if (az0 < 8 || az0 > 11) {
                    return r == 1 ? Part.WAINSCOT : Part.WALLPAPER;          // 0.28.0: east wall of the rooms beside the aisle
                }
            }
            if (aisle || row >= ROWS) {
                if (!aisle && r >= 1 && r <= 4) {
                    return r == 1 ? Part.WAINSCOT : Part.WALLPAPER;          // solid back of the building
                }
                if (r == 0) {
                    return carpet;
                }
                if (r == 5) {
                    return aisle && lx == (AX0 + AX1) / 2 && lz % 7 == 3 ? lamp(lx, lz, lv) : Part.CEILING;
                }
                return Part.AIR;
            }
            int az = lz - Z0 - row * MZ;
            if (az >= 8 && az <= 11) {                                       // corridor
                if (r == 0) {
                    return carpet;
                }
                if (r == 5) {
                    return az == 9 && (lx - X0) % CW == 3 ? lamp(lx, lz, lv) : Part.CEILING;
                }
                if (modern) {
                    int c = (lx - X0) / CW;
                    int ax = (lx - X0) % CW;
                    if (r == 2 && ax == 0 && (az == 8 || az == 11)) {
                        return az == 8 ? Part.SCONCE_S : Part.SCONCE_N;      // warm, dim wall lights between the doors
                    }
                    if (r == 1 && ax == 5 && az == 8 && corridorHash(lv, row, c, 1) < 0.07) {
                        return Part.ICE_S;
                    }
                    if (r == 1 && ax == 5 && az == 11 && corridorHash(lv, row, c, 2) < 0.07) {
                        return Part.CART_N;
                    }
                }
                return Part.AIR;
            }
            int band = az < 8 ? 0 : 1;
            int rz = band == 0 ? az : MZ - 1 - az;
            int col = (lx - X0) / CW;
            int axx = (lx - X0) % CW;
            if (r == 0) {
                return carpet;
            }
            if (r == 5) {
                return Part.CEILING;
            }
            RoomKind kind = roomKind(lv, row, col, band);
            boolean wall = rz == 0 || rz == 7 || axx == 0;
            if (wall) {
                if (rz == 7 && axx == 3 && r <= 2 && kind == RoomKind.BARRICADED) {
                    return band == 0 ? (r == 1 ? Part.IRON_A_LO : Part.IRON_A_HI) : (r == 1 ? Part.IRON_B_LO : Part.IRON_B_HI);
                }
                if (rz == 7 && axx == 3 && r <= 2) {
                    boolean open = doorOpen(lv, row, col, band);
                    if (band == 0) {
                        return open ? (r == 1 ? Part.DOOR_A_OPEN_LO : Part.DOOR_A_OPEN_HI) : (r == 1 ? Part.DOOR_A_LO : Part.DOOR_A_HI);
                    }
                    return open ? (r == 1 ? Part.DOOR_B_OPEN_LO : Part.DOOR_B_OPEN_HI) : (r == 1 ? Part.DOOR_B_LO : Part.DOOR_B_HI);
                }
                return r == 1 ? Part.WAINSCOT : Part.WALLPAPER;
            }
            if (kind == RoomKind.EMPTY) {
                return Part.AIR;
            }
            if (modern && kind != RoomKind.STANDARD) {
                Part m = modernRoom(kind, band, r, rz, axx);
                if (m != null) {
                    return m;
                }
            }
            if (r == 1) {
                if (rz == 1 && (axx == 2 || axx == 3)) {
                    return band == 0 ? Part.BED_A : Part.BED_B;
                }
                if (rz == 2 && (axx == 2 || axx == 3)) {
                    return band == 0 ? Part.BEDF_A : Part.BEDF_B;
                }
                if (rz == 1 && (axx == 1 || axx == 4)) {
                    return band == 0 ? Part.NIGHT_A : Part.NIGHT_B;
                }
                if (rz == 4 && axx == 6) {
                    return Part.DRESSER;
                }
            }
            if (r == 2 && rz == 4 && axx == 6) {
                return Part.TV;
            }
            return Part.AIR;
        }

        // --- 0.36.0: the labyrinth hotel (new worlds) -------------------------------------------------------

        static final int[] DX = {1, 0, -1, 0};
        static final int[] DZ = {0, 1, 0, -1};

        /** L-shaped staircase (canonical frame: first flight east, then south) climbing from floor lv to lv + 1. */
        Part lStairs(int lv, int lx, int r, int lz) {
            int gx = lx - HotelLayout.ORG;
            int gz = lz - HotelLayout.ORG;
            if (gx <= 0 || gz <= 0 || gx >= HotelLayout.NC * HotelLayout.CS || gz >= HotelLayout.NC * HotelLayout.CS) {
                return null;
            }
            int cx = gx / HotelLayout.CS;
            int cz = gz / HotelLayout.CS;
            int a = gx - cx * HotelLayout.CS;
            int b = gz - cz * HotelLayout.CS;
            if (a == 0 || b == 0) {
                return null;
            }
            HotelLayout f = floors[lv];
            int rot = f.upRot(cx, cz);
            if (rot >= 0) {
                Part p = lStair(a, b, r, rot);
                if (p != null) {
                    return p;
                }
            }
            rot = f.downRot(cx, cz);
            if (rot >= 0 && r <= 1) {
                return lStair(a, b, r + 6, rot);
            }
            return null;
        }

        static Part lStair(int a, int b, int o, int rot) {
            int x = a - 5;
            int z = b - 5;
            for (int i = 0; i < rot; i++) {                                  // world → canonical frame
                int t = x;
                x = z;
                z = -t;
            }
            int ca = x + 5;
            int cb = z + 5;
            if (cb >= 2 && cb <= 3 && ca >= 2 && ca <= 4) {                  // first flight
                int k = ca - 2;
                if (o == k + 1) {
                    return dir(Part.STAIR_0, rot);
                }
                if (o >= 1 && o <= k) {
                    return Part.STAIR_SUPPORT;
                }
            }
            if (cb >= 2 && cb <= 3 && ca >= 5 && ca <= 6 && o >= 1 && o <= 3) {
                return Part.STAIR_SUPPORT;                                   // landing
            }
            if (ca >= 5 && ca <= 6 && cb >= 4 && cb <= 5) {                  // second flight
                int k = cb - 4;
                if (o == k + 4) {
                    return dir(Part.STAIR_0, rot + 1);
                }
                if (o >= 1 && o <= k + 3) {
                    return Part.STAIR_SUPPORT;
                }
            }
            if (o == 6 && ca >= 5 && ca <= 6 && cb == 6) {
                return dir(Part.STAIR_0, rot + 1);                               // 0.38.1: the sixth step lies in the floor above
            }
            boolean hole = ca >= 2 && ca <= 6 && cb >= 2 && cb <= 5;
            if (hole && (o == 5 || o == 6)) {
                return Part.AIR;
            }
            if (o == 7) {
                boolean rail = (cb == 1 && ca >= 1 && ca <= 7) || ((ca == 1 || ca == 7) && cb >= 1 && cb <= 6) || (cb == 6 && ca <= 4);
                if (rail) {
                    return Part.RAILING;
                }
            }
            return null;
        }

        int[] cellOf(int lx, int lz) {
            int gx = lx - HotelLayout.ORG;
            int gz = lz - HotelLayout.ORG;
            int n = HotelLayout.NC * HotelLayout.CS;
            if (gx < 0 || gz < 0 || gx > n || gz > n) {
                return null;
            }
            int cx = Math.min(gx / HotelLayout.CS, HotelLayout.NC - 1);
            int cz = Math.min(gz / HotelLayout.CS, HotelLayout.NC - 1);
            return new int[]{cx, cz, gx - cx * HotelLayout.CS, gz - cz * HotelLayout.CS};
        }

        double cellHash(int lv, int cx, int cz, int salt) {
            return Hashing.unit(Hashing.hash(seed, rx, rz, 0x94B0 + lv * 16 + salt, cx * 64 + cz));
        }

        public boolean roomDoorOpen(int lv, int cx, int cz) {
            HotelLayout f = floors[lv];
            return f.kind[cx][cz] != HotelLayout.Kind.BARRICADED && cellHash(lv, cx, cz, 1) < 0.55;
        }

        public boolean roomChest(int lv, int cx, int cz) {
            HotelLayout.Kind k = floors[lv].kind[cx][cz];
            return k == HotelLayout.Kind.BARRICADED || (k != HotelLayout.Kind.EMPTY && cellHash(lv, cx, cz, 2) < 0.2);
        }

        public HotelLayout floor(int lv) {
            return floors[lv];
        }

        Part wall(int r) {
            return r == 1 ? Part.WAINSCOT : Part.WALLPAPER;
        }

        /** Guest floors of the labyrinth hotel. */
        Part labyrinth(int lv, int lx, int r, int lz) {
            HotelLayout f = floors[lv];
            Part carpet = lv % 2 == 1 ? Part.CARPET2 : Part.CARPET;
            int[] c = cellOf(lx, lz);
            if (r == 5) {
                if (c != null && c[2] == 5 && c[3] == 5) {
                    byte t = f.type[c[0]][c[1]];
                    HotelLayout.Kind k = f.kind[c[0]][c[1]];
                    if (t == HotelLayout.CORR || (k != HotelLayout.Kind.EMPTY && k != HotelLayout.Kind.LINEN)) {
                        return lamp(lx, lz, lv);
                    }
                }
                return Part.CEILING;
            }
            if (c == null) {
                return r == 0 ? carpet : wall(r);
            }
            int cx = c[0];
            int cz = c[1];
            int a = c[2];
            int b = c[3];
            boolean xw = a == 0 || a == HotelLayout.CS;
            boolean zw = b == 0 || b == HotelLayout.CS;
            if (xw || zw) {
                if (r == 0) {
                    return carpet;
                }
                if (xw && zw) {
                    return wall(r);
                }
                int d = xw ? (a == 0 ? 2 : 0) : (b == 0 ? 3 : 1);
                int along = xw ? b : a;
                if (f.opening(cx, cz, d, along)) {
                    return Part.AIR;
                }
                if (along == 5 && r <= 2 && f.doorBetween(cx, cz, d)) {
                    int ox = cx;
                    int oz = cz;
                    if (!(f.type[cx][cz] == HotelLayout.ROOM && f.door[cx][cz] == d)) {
                        ox += DX[d];
                        oz += DZ[d];
                    }
                    boolean x = d == 0 || d == 2;
                    if (f.kind[ox][oz] == HotelLayout.Kind.BARRICADED) {
                        return x ? (r == 1 ? Part.IRON_X_LO : Part.IRON_X_HI) : (r == 1 ? Part.IRON_A_LO : Part.IRON_A_HI);
                    }
                    boolean open = roomDoorOpen(lv, ox, oz);
                    if (x) {
                        return open ? (r == 1 ? Part.DOOR_X_OPEN_LO : Part.DOOR_X_OPEN_HI) : (r == 1 ? Part.DOOR_X_LO : Part.DOOR_X_HI);
                    }
                    return open ? (r == 1 ? Part.DOOR_A_OPEN_LO : Part.DOOR_A_OPEN_HI) : (r == 1 ? Part.DOOR_A_LO : Part.DOOR_A_HI);
                }
                return wall(r);
            }
            if (f.type[cx][cz] == HotelLayout.CORR) {
                if (r == 0) {
                    return carpet;
                }
                if (f.hall(cx, cz)) {
                    return Part.AIR;
                }
                int arms = f.arms(cx, cz);
                boolean centre = a >= 4 && a <= 6 && b >= 4 && b <= 6;
                boolean path;
                int link = f.conn[cx][cz];
                if ((link == 5 || link == 10) && !f.hall(cx, cz) && cellHash(lv, cx, cz, 11) < 0.6) {
                    // a kink: the corridor swerves around a solid block instead of running straight through
                    boolean far = cellHash(lv, cx, cz, 12) < 0.5;
                    boolean ew = link == 5;
                    int along = ew ? a : b;
                    int across = ew ? b : a;
                    boolean lane = far ? across >= 7 : across <= 3;
                    int laneSide = ew ? (far ? 1 : 3) : (far ? 0 : 2);   // direction of the detour
                    int stubs = arms & ~link;
                    boolean otherStub = (stubs & ~(1 << laneSide)) != 0;
                    path = (across >= 4 && across <= 6 && (along <= 3 || along >= 7)) || (lane && along >= 1 && along <= 9)
                            || (otherStub && centre)
                            || ((stubs & 1) != 0 && a >= 7 && b >= 4 && b <= 6) || ((stubs & 4) != 0 && a <= 3 && b >= 4 && b <= 6)
                            || ((stubs & 2) != 0 && b >= 7 && a >= 4 && a <= 6) || ((stubs & 8) != 0 && b <= 3 && a >= 4 && a <= 6);
                    if (!path) {
                        return wall(r);
                    }
                    if (r == 2 && along == 5 && (far ? across == 9 : across == 1) && (stubs & (1 << laneSide)) == 0) {
                        return dir(Part.SCONCE_0, laneSide + 2);
                    }
                    return Part.AIR;
                }
                path = centre
                        || ((arms & 1) != 0 && a >= 7 && b >= 4 && b <= 6)
                        || ((arms & 4) != 0 && a <= 3 && b >= 4 && b <= 6)
                        || ((arms & 2) != 0 && b >= 7 && a >= 4 && a <= 6)
                        || ((arms & 8) != 0 && b <= 3 && a >= 4 && a <= 6);
                if (!path) {
                    return wall(r);
                }
                for (int d = 0; d < 4; d++) {                                // sconces on the blind sides of the bends
                    if ((arms & (1 << d)) == 0 && a == 5 + DX[d] && b == 5 + DZ[d]) {
                        if (r == 2 && cellHash(lv, cx, cz, 3 + d) < 0.6) {
                            return dir(Part.SCONCE_0, d + 2);
                        }
                        if (r == 1 && Integer.bitCount(arms) == 1 && cellHash(lv, cx, cz, 8) < 0.35) {
                            return cellHash(lv, cx, cz, 9) < 0.5 ? dir(Part.ICE_0, d + 2) : dir(Part.CART_0, d + 2);
                        }
                    }
                }
                return Part.AIR;
            }
            return room(lv, f, cx, cz, a, b, r, carpet);
        }

        /** Interior of a labyrinth room. (u, v): u across the room (door at u = 5), v depth (1 at the door, 9 far wall). */
        Part room(int lv, HotelLayout f, int cx, int cz, int a, int b, int r, Part carpet) {
            HotelLayout.Kind k = f.kind[cx][cz];
            int dd = f.door[cx][cz];
            int s = (dd + 1) % 4;
            int far = (dd + 2) % 4;
            int ns = (dd + 3) % 4;
            int v = 5 - ((a - 5) * DX[dd] + (b - 5) * DZ[dd]);
            int u = 5 + (a - 5) * DX[s] + (b - 5) * DZ[s];
            if (r == 0) {
                return switch (k) {
                    case BATH -> Part.TILE;
                    case LINEN, DINING -> Part.WOOD_FLOOR;
                    default -> carpet;
                };
            }
            switch (k) {
                case EMPTY -> {
                    return r == 4 && (u == 1 || u == 9) && (v == 1 || v == 9) && cellHash(lv, cx, cz, 10) < 0.6 ? Part.COBWEB : Part.AIR;
                }
                case BATH -> {
                    if (r == 1 && v == 9 && (u == 2 || u == 3)) {
                        return Part.TUB;
                    }
                    if (r == 1 && v == 9 && u == 8) {
                        return dir(Part.TOILET_0, far);
                    }
                    if (r == 1 && u == 9 && v == 5) {
                        return Part.SINK;
                    }
                    if (r == 1 && u == 9 && v == 6) {
                        return Part.LAUNDRY;                                 // towels
                    }
                    return Part.AIR;
                }
                case LINEN -> {
                    if (v == 9 && u >= 2 && u <= 8 && r <= 3) {
                        return Part.SHELF;
                    }
                    if (r == 1 && ((u == 2 && (v == 3 || v == 4)) || (u == 3 && v == 3))) {
                        return Part.LAUNDRY;
                    }
                    if (r == 2 && u == 2 && v == 3) {
                        return Part.LAUNDRY;
                    }
                    if (u == 8 && v >= 6 && v <= 7 && r <= (v == 7 ? 2 : 1)) {
                        return Part.CRATE;
                    }
                    if (r == 1 && u == 7 && v == 3) {
                        return dir(Part.CART_0, ns);
                    }
                    return Part.AIR;
                }
                case LOUNGE -> {
                    if (v == 9 && u >= 2 && u <= 8 && r <= 2) {
                        return Part.SHELF;
                    }
                    if (r == 1) {
                        if (v == 8 && u >= 4 && u <= 6) {
                            return dir(Part.SOFA_0, dd);
                        }
                        if (u == 2 && (v == 5 || v == 6)) {
                            return dir(Part.SOFA_0, s);
                        }
                        if (u == 8 && (v == 5 || v == 6)) {
                            return dir(Part.SOFA_0, ns);
                        }
                        if ((u == 1 || u == 9) && v == 8) {
                            return Part.PLANT2;
                        }
                        if (u >= 3 && u <= 7 && v >= 4 && v <= 7) {
                            return Part.RUG;
                        }
                    }
                    return Part.AIR;
                }
                case DINING -> {
                    boolean tu = u == 3 || u == 7;
                    boolean tv = v == 3 || v == 7;
                    if (tu && tv) {
                        return r == 1 ? Part.TABLE : r == 2 ? Part.TABLE_TOP : Part.AIR;
                    }
                    if (r == 1 && tv && (u == 2 || u == 6)) {
                        return dir(Part.CHAIR_0, ns);
                    }
                    if (r == 1 && tv && (u == 4 || u == 8)) {
                        return dir(Part.CHAIR_0, s);
                    }
                    if (r == 1 && v == 9 && u == 5) {
                        return dir(Part.CART_0, dd);
                    }
                    return Part.AIR;
                }
                default -> {
                }
            }
            // bedrooms: guest, twin, trashed, static, phone, barricaded
            boolean twin = k == HotelLayout.Kind.TWIN;
            boolean trashed = k == HotelLayout.Kind.TRASHED;
            if (r == 1) {
                if (twin ? (v == 9 && (u == 3 || u == 7)) : (v == 9 && u == 5)) {
                    return dir(Part.BED_0, far);
                }
                if (twin ? (v == 8 && (u == 3 || u == 7)) : (v == 8 && u == 5)) {
                    return trashed ? Part.AIR : dir(Part.BEDF_0, far);
                }
                if (twin ? (v == 9 && u == 5) : (v == 9 && (u == 4 || u == 6))) {
                    return dir(Part.NIGHT_0, dd);
                }
                if (u == 9 && v == 5) {
                    return trashed ? Part.AIR : dir(Part.DRESS_0, ns);
                }
                if (u == 9 && v == 9) {
                    return Part.PLANT2;
                }
                if (u == 1 && (v == 6 || v == 7)) {
                    return Part.WARDROBE;                                    // wardrobe against the side wall
                }
                if (u == 8 && v == 2 && !twin) {
                    return dir(Part.SOFA_0, ns);                             // armchair by the door
                }
                if (u == 2 && v == 2 && cellHash(lv, cx, cz, 13) < 0.5) {
                    return Part.CRATE;                                       // somebody's luggage
                }
                if (k == HotelLayout.Kind.BARRICADED && u == 5 && v == 1) {
                    return Part.BARRICADE;
                }
                if (trashed && ((u == 3 && v == 5) || (u == 7 && v == 3) || (u == 2 && v == 7))) {
                    return Part.MAGAZINES;
                }
                if (!twin && u >= 4 && u <= 6 && v >= 5 && v <= 6) {
                    return Part.RUG;
                }
            }
            if (r == 2 && u == 1 && (v == 6 || v == 7)) {
                return Part.WARDROBE;
            }
            if (r == 2) {
                if (u == 9 && v == 5) {
                    return k == HotelLayout.Kind.STATIC ? dir(Part.STV_0, ns) : trashed ? dir(Part.BTV_0, ns) : dir(Part.TV_0, ns);
                }
                if (k == HotelLayout.Kind.PHONE && v == 9 && u == (twin ? 5 : 4)) {
                    return dir(Part.PHONE_0, dd);
                }
                if (k == HotelLayout.Kind.BARRICADED && u == 5 && v == 1) {
                    return Part.BARRICADE_TOP;
                }
            }
            if (r == 4 && trashed && (u == 1 || u == 9) && (v == 1 || v == 9)) {
                return Part.COBWEB;
            }
            return Part.AIR;
        }

        /** World offset (relative to the region) of room-local (u, v) in a labyrinth room: {lx, lz}. */
        public int[] roomPos(HotelLayout f, int cx, int cz, int u, int v) {
            int dd = f.door[cx][cz];
            int s = (dd + 1) % 4;
            int a = 5 + DX[dd] * (5 - v) + DX[s] * (u - 5);
            int b = 5 + DZ[dd] * (5 - v) + DZ[s] * (u - 5);
            return new int[]{HotelLayout.ORG + cx * HotelLayout.CS + a, HotelLayout.ORG + cz * HotelLayout.CS + b};
        }

        Part lamp(int lx, int lz, int lv) {
            double off = modern && lv >= 2 ? 0.55 : 0.3;                    // 0.28.0: dimmer guest floors, sconces carry the light
            return Hashing.unit(Hashing.hash(seed, lx, lz, 0x9413 + lv, 0)) < off ? Part.LAMP_BROKEN : Part.LAMP;
        }

        /**
         * 0.28.0: interior of the reworked room kinds; null = fall back to the standard furniture. Room-local:
         * rz 0 far wall … 7 door wall, axx 1–6.
         */
        Part modernRoom(RoomKind kind, int band, int r, int rz, int axx) {
            boolean a = band == 0;
            switch (kind) {
                case TWIN -> {
                    if (r == 1 && rz == 1 && (axx == 1 || axx == 5)) {
                        return a ? Part.BED_A : Part.BED_B;
                    }
                    if (r == 1 && rz == 2 && (axx == 1 || axx == 5)) {
                        return a ? Part.BEDF_A : Part.BEDF_B;
                    }
                    if (r == 1 && rz == 1 && axx == 3) {
                        return a ? Part.NIGHT_A : Part.NIGHT_B;
                    }
                    if (r == 1 && rz <= 2 && (axx == 2 || axx == 4)) {
                        return Part.AIR;
                    }
                    return null;
                }
                case TRASHED -> {
                    if (r == 1 && rz == 2 && (axx == 2 || axx == 3)) {
                        return Part.AIR;                                     // the mattress is gone, only the head of the bed is left
                    }
                    if (r == 2 && rz == 4 && axx == 6) {
                        return Part.BROKEN_TV;
                    }
                    if (r == 1 && rz == 4 && axx == 6) {
                        return Part.AIR;                                     // dresser missing
                    }
                    if (r == 1 && ((rz == 5 && axx == 2) || (rz == 3 && axx == 5) || (rz == 6 && axx == 5))) {
                        return Part.MAGAZINES;
                    }
                    if (r == 4 && (rz == 1 || rz == 6) && (axx == 1 || axx == 6)) {
                        return Part.COBWEB;
                    }
                    return null;
                }
                case BARRICADED -> {
                    if (axx == 3 && rz == 6 && r <= 2) {
                        return r == 1 ? Part.BARRICADE : Part.BARRICADE_TOP; // something pushed against the door from inside
                    }
                    return null;
                }
                case STATIC -> {
                    return r == 2 && rz == 4 && axx == 6 ? Part.STATIC_TV : null;
                }
                case PHONE -> {
                    return r == 2 && rz == 1 && axx == 1 ? (a ? Part.PHONE_A : Part.PHONE_B) : null;
                }
                default -> {
                    return null;
                }
            }
        }
    }

    // --- room helpers for the decorator (region-local coordinates) -----------------------------------------

    /** Region-local x of the door of room column {@code col}, or -1 for the aisle columns. */
    public static int doorX(int col) {
        int x0 = X0 + col * CW;
        return x0 >= AX0 && x0 <= AX1 ? -1 : x0 + 3;
    }

    /** Region-local z of the door of a room (band 0 north of the corridor, band 1 south). */
    public static int doorZ(int row, int band) {
        return Z0 + row * MZ + (band == 0 ? 7 : MZ - 8);
    }

    public static int cols() {
        return COLS;
    }

    public static int rows() {
        return ROWS;
    }

    public static int reception() {
        return 72;
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(16);

    public Hotel(OfficePlanner p) {
        this.p = p;
    }

    public Site site(int rx, int rz) {
        if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.HOTEL) {
            return null;
        }
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    private Site build(int rx, int rz) {
        int[] range = Megastructures.storeyRange(p, p.cfg.modern() ? STOREYS : LEGACY_STOREYS);
        if (range[1] <= range[0]) {
            return null;
        }
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9402, 0));
        int s0 = rng.range(range[0], range[1]);
        int corner = rng.nextInt(4);
        int vx = corner % 2 == 0 ? 12 : W - 12 - VS;
        int vz = corner / 2 == 0 ? 12 : W - 12 - VS;
        int level = rng.nextInt(2) == 0 ? 2 : 3;
        return new Site(rx, rz, s0, p.floorY(s0), p.seed, vx, vz, level, p.cfg.modern());
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

    /** The site whose band contains the position (not on the shared office wall lines), else null. */
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
