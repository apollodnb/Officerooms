package de.officerooms.megastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.Test;

class HotelLayoutTest {

    @Test
    void everyRoomAndStairReachable() {
        for (long seed = 1; seed <= 40; seed++) {
            List<int[]> below = List.of();
            for (int lv = 1; lv < 7; lv++) {
                HotelLayout f = new HotelLayout(seed, 3, -2, lv, below, lv == 1, lv == 6);
                assertEquals(lv == 6 ? 0 : 2, f.up.size(), "stairs up on level " + lv);
                if (lv >= 2) {
                    boolean[][] seen = new boolean[HotelLayout.NC][HotelLayout.NC];
                    ArrayDeque<int[]> q = new ArrayDeque<>();
                    int[] start = f.down.get(0);
                    q.add(start);
                    seen[start[0]][start[1]] = true;
                    while (!q.isEmpty()) {
                        int[] c = q.poll();
                        for (int d = 0; d < 4; d++) {
                            int nx = c[0] + HotelLayout.DX[d];
                            int nz = c[1] + HotelLayout.DZ[d];
                            if (nx < 0 || nz < 0 || nx >= HotelLayout.NC || nz >= HotelLayout.NC || seen[nx][nz]) {
                                continue;
                            }
                            if (f.opening(c[0], c[1], d, 5) || f.doorBetween(c[0], c[1], d)) {
                                seen[nx][nz] = true;
                                q.add(new int[]{nx, nz});
                            }
                        }
                    }
                    for (int x = 0; x < HotelLayout.NC; x++) {
                        for (int z = 0; z < HotelLayout.NC; z++) {
                            assertTrue(seen[x][z], "seed " + seed + " level " + lv + " cell " + x + "," + z + " unreachable");
                        }
                    }
                    for (int[] u : f.up) {
                        assertEquals(HotelLayout.CORR, f.type[u[0]][u[1]]);
                    }
                }
                below = f.up;
            }
        }
    }
}
