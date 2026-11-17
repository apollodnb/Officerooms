package de.officerooms.megastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import org.junit.jupiter.api.Test;

/** 0.31.0: the abandoned cinema exists only in new worlds, is as rare as the others and every hall can be entered. */
class CinemaTest {

    private static OfficePlanner modern() {
        GenSettings d = GenSettings.DEFAULT;
        GenSettings g = new GenSettings(d.grid(), d.roomHeights(), d.roomSizes(), d.compositeChance(), d.corridors(), d.stairsPerStorey(),
                d.atria(), d.doors(), d.windows(), d.furnishing(), d.special(), 1);
        return new OfficePlanner(4711L, g, -64, 608);
    }

    @Test
    void onlyInNewWorldsAndRare() {
        OfficePlanner old = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        OfficePlanner p = modern();
        int n = 0;
        int c = 0;
        for (int rx = -30; rx < 30; rx++) {
            for (int rz = -30; rz < 30; rz++) {
                n++;
                assertNull(old.cinema.site(rx, rz));
                if (p.cinema.site(rx, rz) != null) {
                    c++;
                    assertNull(p.fun.site(rx, rz));
                    assertNull(p.hotel.site(rx, rz));
                }
            }
        }
        System.out.println("[Cinema] " + c + " of " + n + " regions");
        assertTrue(c >= 0.03 * n && c <= 0.10 * n, "frequency off: " + c);
    }

    @Test
    void hallsHaveScreenSeatsAndDoors() {
        OfficePlanner p = modern();
        Cinema.Site s = null;
        for (int rx = -30; rx < 30 && s == null; rx++) {
            for (int rz = -30; rz < 30 && s == null; rz++) {
                s = p.cinema.site(rx, rz);
            }
        }
        assertTrue(s != null);
        for (int f = 0; f < Cinema.FLOORS; f++) {
            int F = f * Cinema.FH;
            for (int i = 0; i < 6; i++) {
                int x0 = 3 + 26 * i;
                // doors from the corridors into both halls
                assertEquals(Cinema.Part.AIR, s.part(x0 + 1, F + 1, 57, false));
                assertEquals(Cinema.Part.AIR, s.part(x0 + 1, F + 2, 103, false));
                // the screen sits right behind the door wall, seats rise behind it
                Cinema.Part sc = s.part(x0 + 12, F + 6, 56, false);
                assertTrue(sc.name().startsWith("SCREEN") || sc == Cinema.Part.AIR || sc.name().startsWith("FIGURE"), sc.name());
                int seats = 0;
                for (int t = 11; t <= 50; t++) {
                    Cinema.Part a = s.part(x0 + 10, F + Cinema.level(t) + 1, 56 - t, false);
                    if (a.name().startsWith("SEAT")) {
                        seats++;
                    }
                }
                assertTrue(seats >= 8, "too few seat rows in hall " + (f * 12 + i) + ": " + seats);
                // the walkway above each riser is free to stand on (2 blocks of air)
                for (int t = 0; t <= 53; t++) {
                    int lv = Cinema.level(t);
                    assertEquals(Cinema.Part.AIR, s.part(x0 + 2, F + lv + 2, 56 - t, false), "aisle blocked at t=" + t);
                }
            }
            assertEquals(Cinema.Part.AIR, s.part(16, F + 2, 63, false));
            assertEquals(Cinema.Part.AIR, s.part(16, F + 2, 97, false));
        }
        // the lobby connects to the corridors
        assertEquals(Cinema.Part.AIR, s.part(16, 2, 63, false));
        assertEquals(Cinema.Part.AIR, s.part(16, 2, 97, false));
    }

    /** 0.37.0: three floors on the same footprint, connected by the atrium stairs. */
    @Test
    void floorsAreConnectedByStairs() {
        OfficePlanner p = modern();
        Cinema.Site s = null;
        for (int rx = -30; rx < 30 && s == null; rx++) {
            for (int rz = -30; rz < 30 && s == null; rz++) {
                s = p.cinema.site(rx, rz);
            }
        }
        assertTrue(s != null);
        assertEquals(6, Cinema.STOREYS);
        int[][] flights = {{68, 0, 67}, {91, 12, 93}};   // stair row, base, gallery row at the top
        for (int[] fl : flights) {
            for (int side = 0; side < 2; side++) {
                for (int k = 0; k < 12; k++) {
                    int lx = side == 0 ? 30 + k : 130 - k;
                    int step = fl[1] + 1 + k;
                    for (int lz = fl[0]; lz <= fl[0] + 1; lz++) {
                        Cinema.Part st = s.part(lx, step, lz, false);
                        assertTrue(st == Cinema.Part.STAIR_E || st == Cinema.Part.STAIR_W, "no step at " + lx + "/" + step + "/" + lz + ": " + st);
                        assertEquals(Cinema.Part.AIR, s.part(lx, step + 1, lz, false), "head room " + lx);
                        assertEquals(Cinema.Part.AIR, s.part(lx, step + 2, lz, false), "head room " + lx);
                    }
                }
                // the top step meets the gallery floor of the next cinema floor, and the railing is open there
                int top = side == 0 ? 41 : 119;
                int g = fl[1] + Cinema.FH;
                assertEquals(Cinema.Part.TILE, s.part(top, g, fl[2], false));
                assertEquals(Cinema.Part.AIR, s.part(top, g + 1, fl[2], false));
                assertEquals(Cinema.Part.AIR, s.part(top, g + 2, fl[2], false));
            }
        }
        // the upper flights start at the floor 1 gallery: railing open at their foot, closed elsewhere
        assertEquals(Cinema.Part.AIR, s.part(28, 13, 93, false));
        assertEquals(Cinema.Part.TILE, s.part(28, 12, 91, false));
        assertEquals(Cinema.Part.TILE, s.part(132, 12, 92, false));
        assertEquals(Cinema.Part.AIR, s.part(28, 13, 91, false));
        assertEquals(Cinema.Part.RAIL_X, s.part(31, 13, 93, false));
        assertEquals(Cinema.Part.RAIL_X, s.part(80, 13, 93, false));
        assertEquals(Cinema.Part.RAIL_X, s.part(80, 25, 67, false));
        assertEquals(Cinema.Part.RAIL_Z, s.part(6, 13, 80, false));
        // 0.37.1: landings with railings at the top of the flights and at the foot of the upper flights
        assertEquals(Cinema.Part.TILE, s.part(43, 12, 69, false));
        assertEquals(Cinema.Part.TILE, s.part(117, 24, 91, false));
        assertEquals(Cinema.Part.RAIL_X, s.part(43, 13, 70, false));
        assertEquals(Cinema.Part.RAIL_WN, s.part(45, 13, 70, false));
        assertEquals(Cinema.Part.RAIL_ES, s.part(115, 25, 90, false));
        assertEquals(Cinema.Part.RAIL_Z, s.part(27, 13, 92, false));
        assertEquals(Cinema.Part.RAIL_X, s.part(28, 13, 90, false));
        assertEquals(Cinema.Part.AIR, s.part(44, 13, 67, false));
        assertEquals(Cinema.Part.AIR, s.part(35, 13, 70, false));
        // the atrium is open over the box office up to the roof
        assertEquals(Cinema.Part.AIR, s.part(80, 20, 80, false));
        assertEquals(Cinema.Part.CEILING, s.part(81, Cinema.H - 1, 80, false));
    }
}
