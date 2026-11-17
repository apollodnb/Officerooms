package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 0.36.0: the guest floors of the reworked hotel (new worlds). A grid of 15 × 15 cells (10 blocks, walls shared)
 * per floor: narrow corridors (3 wide) wind through it like a maze, with bends and dead ends; every other cell is a
 * small room with a purpose (guest rooms, twin rooms, bathrooms, linen closets, lounges, ...), each reachable through
 * a door from a corridor or from the next room. L-shaped staircases in random spots climb one floor at a time.
 */
public final class HotelLayout {

    public static final int NC = 15;
    public static final int CS = 10;
    public static final int ORG = 5;
    static final int MC = 7;

    public static final byte SOLID = 0, CORR = 1, ROOM = 2;
    /** Directions: 0 E (+x), 1 S (+z), 2 W, 3 N. */
    public static final int[] DX = {1, 0, -1, 0};
    public static final int[] DZ = {0, 1, 0, -1};

    public enum Kind { GUEST, TWIN, BATH, LINEN, LOUNGE, DINING, TRASHED, STATIC, PHONE, BARRICADED, EMPTY }

    public final byte[][] type = new byte[NC][NC];
    /** Corridor connections (bit per direction). */
    public final byte[][] conn = new byte[NC][NC];
    /** Door direction of a room (0–3). */
    public final byte[][] door = new byte[NC][NC];
    public final Kind[][] kind = new Kind[NC][NC];
    /** Staircase cells going up from this floor, and the ones arriving from the floor below. */
    public final List<int[]> up = new ArrayList<>();
    public final List<int[]> down = new ArrayList<>();

    /** Builds the floor; {@code downCells} are the stair cells arriving from below (kept as corridor halls). */
    HotelLayout(long seed, int rx, int rz, int lv, List<int[]> downCells, boolean lobby, boolean top) {
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(seed, rx, rz, 0x94A0 + lv, 0));
        down.addAll(downCells);
        // stairs up from here: two macro cells, never where the stairs from below arrive
        for (int t = 0; t < 60 && up.size() < (top ? 0 : 2); t++) {
            int cx = 2 * rng.nextInt(MC) + 1;
            int cz = 2 * rng.nextInt(MC) + 1;
            boolean bad = false;
            for (int[] d : downCells) {
                bad |= Math.abs(d[0] - cx) + Math.abs(d[1] - cz) < 4;
            }
            for (int[] u : up) {
                bad |= Math.abs(u[0] - cx) + Math.abs(u[1] - cz) < 6;
            }
            if (lobby) {                                                     // keep clear of the reception and the basement stairs
                bad |= cx >= 3 && cx <= 9 && cz >= 4 && cz <= 9;
                bad |= cx >= 5 && cx <= 9 && cz <= 3;
            }
            if (!bad) {
                up.add(new int[]{cx, cz, rng.nextInt(4)});
            }
        }
        if (lobby) {
            return;
        }
        // winding corridors: a maze over 7 × 7 macro cells, the cells between them are the corridor pieces
        boolean[][] seen = new boolean[MC][MC];
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        int si = rng.nextInt(MC);
        int sj = rng.nextInt(MC);
        stack.push(new int[]{si, sj});
        seen[si][sj] = true;
        type[2 * si + 1][2 * sj + 1] = CORR;
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            int[] opts = new int[4];
            int n = 0;
            for (int d = 0; d < 4; d++) {
                int ni = c[0] + DX[d];
                int nj = c[1] + DZ[d];
                if (ni >= 0 && nj >= 0 && ni < MC && nj < MC && !seen[ni][nj]) {
                    opts[n++] = d;
                }
            }
            if (n == 0) {
                stack.pop();
                continue;
            }
            int d = opts[rng.nextInt(n)];
            link(c[0], c[1], d);
            seen[c[0] + DX[d]][c[1] + DZ[d]] = true;
            stack.push(new int[]{c[0] + DX[d], c[1] + DZ[d]});
        }
        for (int i = 0; i < MC; i++) {                                       // a few loops so it is not a pure tree
            for (int j = 0; j < MC; j++) {
                if (i + 1 < MC && rng.chance(0.12)) {
                    link(i, j, 0);
                }
                if (j + 1 < MC && rng.chance(0.12)) {
                    link(i, j, 1);
                }
            }
        }
        // every other cell is a room; doors are handed out outward from the corridors so each room can be reached
        for (int x = 0; x < NC; x++) {
            for (int z = 0; z < NC; z++) {
                if (type[x][z] != CORR) {
                    type[x][z] = ROOM;
                    double u = rng.nextDouble();
                    kind[x][z] = u < 0.34 ? Kind.GUEST : u < 0.45 ? Kind.TWIN : u < 0.53 ? Kind.BATH : u < 0.60 ? Kind.LINEN
                            : u < 0.67 ? Kind.LOUNGE : u < 0.72 ? Kind.DINING : u < 0.80 ? Kind.TRASHED : u < 0.85 ? Kind.STATIC
                            : u < 0.89 ? Kind.PHONE : u < 0.95 ? Kind.BARRICADED : Kind.EMPTY;
                }
            }
        }
        boolean[][] reached = new boolean[NC][NC];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < NC; x++) {
            for (int z = 0; z < NC; z++) {
                if (type[x][z] == CORR) {
                    reached[x][z] = true;
                    q.add(new int[]{x, z});
                }
            }
        }
        // shuffle-ish: rooms prefer a door onto a corridor; otherwise they open into a reached neighbour room
        for (int pass = 0; pass < 2; pass++) {
            ArrayDeque<int[]> nq = new ArrayDeque<>(q);
            while (!nq.isEmpty()) {
                int[] c = nq.poll();
                int start = rng.nextInt(4);
                for (int k = 0; k < 4; k++) {
                    int d = (start + k) % 4;
                    int x = c[0] + DX[d];
                    int z = c[1] + DZ[d];
                    if (x < 0 || z < 0 || x >= NC || z >= NC || reached[x][z] || type[x][z] != ROOM) {
                        continue;
                    }
                    if (pass == 0 && type[c[0]][c[1]] != CORR) {
                        continue;
                    }
                    reached[x][z] = true;
                    door[x][z] = (byte) ((d + 2) % 4);                       // the room's door points back at c
                    nq.add(new int[]{x, z});
                }
            }
            q.clear();
            for (int x = 0; x < NC; x++) {
                for (int z = 0; z < NC; z++) {
                    if (reached[x][z]) {
                        q.add(new int[]{x, z});
                    }
                }
            }
        }
    }

    private void link(int i, int j, int d) {
        int ax = 2 * i + 1;
        int az = 2 * j + 1;
        int cx = ax + DX[d];
        int cz = az + DZ[d];
        int bx = ax + 2 * DX[d];
        int bz = az + 2 * DZ[d];
        type[ax][az] = CORR;
        type[cx][cz] = CORR;
        type[bx][bz] = CORR;
        conn[ax][az] |= (byte) (1 << d);
        conn[cx][cz] |= (byte) (1 << d) | (1 << ((d + 2) % 4));
        conn[bx][bz] |= (byte) (1 << ((d + 2) % 4));
    }

    public boolean isUp(int cx, int cz) {
        for (int[] u : up) {
            if (u[0] == cx && u[1] == cz) {
                return true;
            }
        }
        return false;
    }

    /** Rotation (0–3) of the staircase in this cell going up, or -1. */
    public int upRot(int cx, int cz) {
        for (int[] u : up) {
            if (u[0] == cx && u[1] == cz) {
                return u[2];
            }
        }
        return -1;
    }

    public int downRot(int cx, int cz) {
        for (int[] u : down) {
            if (u[0] == cx && u[1] == cz) {
                return u[2];
            }
        }
        return -1;
    }

    /** Directions in which a corridor cell has a path: its corridor links plus the doors of rooms opening onto it. */
    public int arms(int cx, int cz) {
        int m = conn[cx][cz];
        for (int d = 0; d < 4; d++) {
            int nx = cx + DX[d];
            int nz = cz + DZ[d];
            if (nx >= 0 && nz >= 0 && nx < NC && nz < NC && type[nx][nz] == ROOM && door[nx][nz] == (d + 2) % 4) {
                m |= 1 << d;
            }
        }
        return m;
    }

    public boolean isDown(int cx, int cz) {
        for (int[] u : down) {
            if (u[0] == cx && u[1] == cz) {
                return true;
            }
        }
        return false;
    }

    /** Whether the cell is an open hall (stair cell) rather than a narrow corridor. */
    public boolean hall(int cx, int cz) {
        return isUp(cx, cz) || isDown(cx, cz);
    }

    /** Is the wall between cell (cx, cz) and its neighbour in direction d open (corridor) at this offset along it? */
    public boolean opening(int cx, int cz, int d, int along) {
        int nx = cx + DX[d];
        int nz = cz + DZ[d];
        if (nx < 0 || nz < 0 || nx >= NC || nz >= NC) {
            return false;
        }
        return type[cx][cz] == CORR && type[nx][nz] == CORR && (conn[cx][cz] & (1 << d)) != 0 && along >= 4 && along <= 6;
    }

    /** Does a room door sit in the wall between (cx, cz) and its neighbour in direction d? */
    public boolean doorBetween(int cx, int cz, int d) {
        int nx = cx + DX[d];
        int nz = cz + DZ[d];
        if (nx < 0 || nz < 0 || nx >= NC || nz >= NC) {
            return false;
        }
        return (type[cx][cz] == ROOM && door[cx][cz] == d) || (type[nx][nz] == ROOM && door[nx][nz] == (d + 2) % 4);
    }
}
