package de.officerooms.sea;

import static de.officerooms.sea.Ship.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import org.junit.jupiter.api.Test;

/** 0.42.0: every SS Fun =) can be walked from the embarkation lobby to the lifeboats and down to the bilge. */
class ShipLayoutTest {

    static boolean walk(byte c) {
        return c == CO || c == RM || c == LO || c == LANE || c == PR || c == TOP || c == DR;
    }

    @Test
    void lifeboatsAndBilgeReachable() {
        int ships = 0;
        for (long seed : new long[]{7657905887809284474L, 4711L, -99L}) {
            for (int gx = -3; gx <= 3; gx++) {
                Ship s = Ship.of(seed, gx, gx * 2);
                if (s == null) {
                    continue;
                }
                ships++;
                boolean[] seen = new boolean[DECKS * L * W];
                ArrayDeque<int[]> q = new ArrayDeque<>();
                q.add(new int[]{5, 124, C});
                seen[(5 * L + 124) * W + C] = true;
                boolean prom = false, bilge = false, sun = false;
                int count = 0;
                while (!q.isEmpty()) {
                    int[] p = q.poll();
                    count++;
                    int d = p[0], lx = p[1], lz = p[2];
                    prom |= s.code(d, lx, lz) == PR;
                    bilge |= d == 0;
                    sun |= d == 11;
                    int[][] nb = {{d, lx + 1, lz}, {d, lx - 1, lz}, {d, lx, lz + 1}, {d, lx, lz - 1}};
                    for (int[] n : nb) {
                        push(s, seen, q, n);
                    }
                    for (Flight f : s.flights) {                 // up and down the stairs
                        if (lz >= f.z0() && lz <= f.z1()) {
                            int bottom = f.lx0() - f.dir(), top = f.lx0() + 5 * f.dir();
                            if (d == f.d() && (lx == f.lx0() || lx == bottom)) {
                                push(s, seen, q, new int[]{d + 1, top, lz});
                            }
                            if (d == f.d() + 1 && lx == top) {
                                push(s, seen, q, new int[]{f.d(), f.lx0(), lz});
                            }
                        }
                    }
                }
                assertTrue(prom, "promenade not reachable, ship " + gx + " seed " + seed + " (" + count + " cells)");
                assertTrue(bilge, "deck 0 not reachable, ship " + gx);
                assertTrue(sun, "sun deck not reachable, ship " + gx);
            }
        }
        assertTrue(ships >= 6, "too few ships tested: " + ships);
    }

    static void push(Ship s, boolean[] seen, ArrayDeque<int[]> q, int[] n) {
        int d = n[0], lx = n[1], lz = n[2];
        if (d < 0 || d >= DECKS || lx < 0 || lx >= L || lz < 0 || lz >= W) {
            return;
        }
        int i = (d * L + lx) * W + lz;
        if (seen[i] || !walk(s.code(d, lx, lz))) {
            return;
        }
        seen[i] = true;
        q.add(n);
    }

    @Test
    void shipsKeepTheirDistance() {
        for (int gx = -4; gx <= 4; gx++) {
            for (int gz = -4; gz <= 4; gz++) {
                Ship a = Ship.of(4711L, gx, gz), b = Ship.of(4711L, gx + 1, gz);
                if (a != null && b != null) {
                    assertTrue(b.ox - (a.ox + L) >= 400, "ships too close: " + (b.ox - (a.ox + L)));
                }
            }
        }
    }
}
