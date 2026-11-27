package de.officerooms.megastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

/** 0.43.0: everything in the Dead Mall can be walked to from the main entrance (escalators, galleries, shops, service corridors). */
class MallLayoutTest {

    static Mall.Part[][][] grid(Mall.Site s) {
        Mall.Part[][][] g = new Mall.Part[160][Mall.H][160];
        for (int x = 1; x < 160; x++) {
            for (int o = 0; o < Mall.H; o++) {
                for (int z = 1; z < 160; z++) {
                    g[x][o][z] = s.part(x, o, z, false);
                }
            }
        }
        return g;
    }

    static boolean open(Mall.Part p) {
        return p == Mall.Part.AIR || p == Mall.Part.PLANT;
    }

    static boolean stand(Mall.Part[][][] g, int x, int o, int z) {
        if (x < 1 || z < 1 || x > 159 || z > 159 || o < 1 || o + 1 >= Mall.H) {
            return false;
        }
        Mall.Part below = g[x][o - 1][z];
        return open(g[x][o][z]) && open(g[x][o + 1][z]) && below != null && !open(below) && below != Mall.Part.OFFICE && below != Mall.Part.RAIL;
    }

    @Test
    void everythingReachableFromTheEntrance() {
        for (long seed : new long[]{4711L, -3L, 99L}) {
            Mall.Site s = new Mall.Site(2, 3, 10, 64, seed);
            Mall.Part[][][] g = grid(s);
            boolean[][][] seen = new boolean[160][Mall.H][160];
            ArrayDeque<int[]> q = new ArrayDeque<>();
            q.add(new int[]{80, 1, 150});
            assertTrue(stand(g, 80, 1, 150), "entrance floor");
            seen[80][1][150] = true;
            while (!q.isEmpty()) {
                int[] c = q.poll();
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = c[0] + d[0];
                    int nz = c[2] + d[1];
                    for (int dy = 1; dy >= -3; dy--) {                 // one step up, or a short drop
                        int no = c[1] + dy;
                        if (dy == 1 && !open(g[c[0]][Math.min(Mall.H - 1, c[1] + 2)][c[2]])) {
                            continue;
                        }
                        if (stand(g, nx, no, nz)) {
                            if (!seen[nx][no][nz]) {
                                seen[nx][no][nz] = true;
                                q.add(new int[]{nx, no, nz});
                            }
                            break;
                        }
                    }
                }
            }
            assertTrue(seen[8][2 * Mall.FH + 1][66], "top gallery, seed " + seed);
            assertTrue(seen[150][Mall.FH + 1][94], "first floor gallery, seed " + seed);
            assertTrue(seen[63 - 30][Mall.FH + 1][63 - 30], "department store floor 1, seed " + seed);
            assertTrue(seen[97 + 30][2 * Mall.FH + 1][97 + 30], "department store floor 2, seed " + seed);
            assertTrue(seen[63 - 5][1][63 - 30], "food stall, seed " + seed);
            assertTrue(seen[63 - 19][2 * Mall.FH + 1][63 - 50], "service corridor floor 2, seed " + seed);
        }
    }
}
