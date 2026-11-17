package de.officerooms.megastructure;

import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 0.8.0: Neighbourhood and The Maze: rarity, exclusivity, layout and maze connectivity. */
class MegastructuresTest {

    @Test
    void megastructuresAreAsRareAsPoolroomsAndExclusive() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int pools = 0;
        int hoods = 0;
        int mazes = 0;
        int n = 0;
        for (int rx = -30; rx < 30; rx++) {
            for (int rz = -30; rz < 30; rz++) {
                n++;
                boolean a = p.pools.site(rx, rz) != null;
                boolean b = p.hood.site(rx, rz) != null;
                boolean c = p.maze.site(rx, rz) != null;
                assertTrue((a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) <= 1, "two megastructures in region " + rx + "," + rz);
                pools += a ? 1 : 0;
                hoods += b ? 1 : 0;
                mazes += c ? 1 : 0;
            }
        }
        System.out.println("[Megastructures] " + n + " regions: Poolrooms " + pools + ", Neighbourhood " + hoods + ", Maze " + mazes);
        for (int k : new int[]{pools, hoods, mazes}) {
            assertTrue(k >= 0.04 * n && k <= 0.10 * n, "frequency off: " + k + " of " + n);
        }
    }

    @Test
    void everyMazeHasThreeToFiveExits() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int mazes = 0;
        int[] hist = new int[6];
        for (int rx = -30; rx < 30; rx++) {
            for (int rz = -30; rz < 30; rz++) {
                Maze.Site s = p.maze.site(rx, rz);
                if (s == null) {
                    continue;
                }
                mazes++;
                hist[Math.min(5, s.exits.size())]++;
            }
        }
        System.out.println("[Maze] " + mazes + " mazes, exits 0..5: " + java.util.Arrays.toString(hist));
        assertEquals(0, hist[0] + hist[1] + hist[2], "mazes with fewer than 3 exits");
    }

    @Test
    void elevatorsAndSecureStorage() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int regions = 0;
        int elevators = 0;
        int stops = 0;
        int vaults = 0;
        for (int rx = -8; rx < 8; rx++) {
            for (int rz = -8; rz < 8; rz++) {
                regions++;
                de.officerooms.room.Extras.Region ex = p.extras.of(rx, rz);
                if (Megastructures.kindOf(p, rx, rz) != Megastructures.Kind.NONE) {
                    assertTrue(ex == null, "megastructure regions have no installations");
                    continue;
                }
                if (ex == null) {
                    continue;
                }
                vaults += ex.vaults.size();
                if (ex.hasElevator()) {
                    elevators++;
                    int n = 0;
                    for (int s = 0; s < p.storeys; s++) {
                        n += ex.isStop(s) ? 1 : 0;
                    }
                    stops += n;
                    assertTrue(n >= 2);
                    int first = ex.nextStop(-1, 1, 1);
                    assertTrue(ex.nextStop(first, 1, 10) - first >= 1);
                }
            }
        }
        System.out.println("[Extras] " + regions + " regions: elevators " + elevators + " (avg stops " + (stops / Math.max(1, elevators))
                + "), secure storage rooms " + vaults);
        assertTrue(elevators > regions / 2);
        assertTrue(vaults > 0);
    }

    @Test
    void neighbourhoodHasTwentyHousesEveryThirdInhabited() {
        assertEquals(20, Neighbourhood.houseCount());
        int residents = 0;
        for (int h = 0; h < 20; h++) {
            residents += Neighbourhood.hasResident(h) ? 1 : 0;
            int x = Neighbourhood.houseX(h);
            int z = Neighbourhood.houseZ(h);
            assertTrue(x > 2 && x + Neighbourhood.HW < 158 && z > 2 && z + Neighbourhood.HD < 158, "house outside the hall");
            // chest / creature spot is inside the house and free
            assertEquals(Neighbourhood.Part.AIR, Neighbourhood.house(Neighbourhood.CHEST_HX, 1, Neighbourhood.CHEST_HZ));
            assertEquals(Neighbourhood.Part.AIR, Neighbourhood.house(Neighbourhood.MOB_HX, 1, Neighbourhood.MOB_HZ));
            assertEquals(Neighbourhood.Part.AIR, Neighbourhood.house(Neighbourhood.MOB_HX, 2, Neighbourhood.MOB_HZ));
        }
        assertEquals(6, residents);
        // no light source anywhere in the hall
        for (Neighbourhood.Part part : Neighbourhood.Part.values()) {
            assertTrue(!part.name().contains("LIGHT"));
        }
    }

    @Test
    void mazeHasExitsAndReachableVault() {
        mazeChecks();
    }

    /** Flood fill over open floor of one maze level, from its first open cell in scan order. */
    private static boolean[] flood(Maze.Site s, int lv) {
        boolean[] seen = new boolean[160 * 160];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        int total = 0;
        for (int z = 1; z < 160; z++) {
            for (int x = 1; x < 160; x++) {
                if (s.openFloor(lv, x, z)) {
                    total++;
                    if (q.isEmpty() && !seen[z * 160 + x]) {
                        seen[z * 160 + x] = true;
                        q.add(new int[]{x, z});
                    }
                }
            }
        }
        int reached = 0;
        while (!q.isEmpty()) {
            int[] c = q.poll();
            reached++;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int x = c[0] + d[0];
                int z = c[1] + d[1];
                if (x <= 0 || z <= 0 || x >= 160 || z >= 160 || seen[z * 160 + x] || !s.openFloor(lv, x, z)) {
                    continue;
                }
                seen[z * 160 + x] = true;
                q.add(new int[]{x, z});
            }
        }
        assertTrue(reached > total * 0.97, "level " + lv + " split: " + reached + " of " + total);
        return seen;
    }

    private void mazeChecks() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int checked = 0;
        for (int rx = -40; rx < 40 && checked < 12; rx++) {
            for (int rz = -40; rz < 40 && checked < 12; rz++) {
                Maze.Site s = p.maze.site(rx, rz);
                if (s == null) {
                    continue;
                }
                checked++;
                assertTrue(s.exits.size() >= 3 && s.exits.size() <= 5, "exits: " + s.exits.size());
                Maze.Vault v = s.vault;
                // vault shell closed except for the door
                int openings = 0;
                for (int lx = v.x0; lx <= v.x1; lx++) {
                    for (int lz = v.z0; lz <= v.z1; lz++) {
                        boolean edge = lx == v.x0 || lx == v.x1 || lz == v.z0 || lz == v.z1;
                        if (edge && s.part(lx, v.walkOff(), lz) == Maze.Part.AIR) {
                            openings++;
                        }
                    }
                }
                assertEquals(1, openings, "vault must have exactly one door");
                assertEquals(Maze.Part.VAULT_FLOOR, s.part(v.chestX, v.walkOff() - 1, v.chestZ));
                assertEquals(Maze.Part.VAULT_FLOOR, s.part(v.chestX, v.walkOff() - 1 + Maze.CEIL, v.chestZ));
                // 0.34.0: every level is one connected labyrinth; the vault door is reachable on its own level
                for (int lv = 0; lv < Maze.LEVELS; lv++) {
                    boolean[] sn = flood(s, lv);
                    if (lv == v.floor) {
                        assertTrue(sn[(v.doorZ - 1) * 160 + v.doorX], "vault door not reachable on level " + lv);
                    }
                }
                // flood fill from the first exit
                Maze.Exit e0 = s.exits.get(0);
                boolean[] seen = new boolean[160 * 160];
                ArrayDeque<int[]> q = new ArrayDeque<>();
                int sx = e0.west() ? 1 : e0.lx();
                int sz = e0.west() ? e0.lz() : 1;
                q.add(new int[]{sx, sz});
                seen[sz * 160 + sx] = true;
                int reached = 0;
                while (!q.isEmpty()) {
                    int[] c = q.poll();
                    reached++;
                    int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    for (int[] d : nb) {
                        int x = c[0] + d[0];
                        int z = c[1] + d[1];
                        if (x <= 0 || z <= 0 || x >= 160 || z >= 160 || seen[z * 160 + x] || !s.openFloor(x, z)) {
                            continue;
                        }
                        seen[z * 160 + x] = true;
                        q.add(new int[]{x, z});
                    }
                }

                for (Maze.Exit e : s.exits) {
                    int x = e.west() ? 1 : e.lx();
                    int z = e.west() ? e.lz() : 1;
                    assertTrue(seen[z * 160 + x], "exit not connected: " + e);
                }
                int open = 0;
                for (int lx = 1; lx < 160; lx++) {
                    for (int lz = 1; lz < 160; lz++) {
                        open += s.openFloor(lx, lz) ? 1 : 0;
                    }
                }
                System.out.println("[Maze] region " + rx + "," + rz + " storey " + s.s0 + ": exits " + s.exits.size()
                        + ", vault level " + v.level + ", reachable " + reached + "/" + open);
                assertTrue(reached >= open * 0.97, "maze badly fragmented");
            }
        }
        assertTrue(checked >= 3, "too few mazes to check");
    }

    @Test
    void hiddenRooms() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int total = 0;
        int empty = 0;
        int[] types = new int[6];
        for (int rx = -8; rx < 8; rx++) {
            for (int rz = -8; rz < 8; rz++) {
                de.officerooms.room.Extras.Region ex = p.extras.of(rx, rz);
                if (ex == null) {
                    continue;
                }
                java.util.List<de.officerooms.room.Extras.Hideout> hs = ex.hideouts;
                for (int i = 0; i < hs.size(); i++) {
                    de.officerooms.room.Extras.Hideout h = hs.get(i);
                    total++;
                    types[h.type()]++;
                    empty += h.empty() ? 1 : 0;
                    assertTrue(h.s() >= 0 && h.s() < p.storeys);
                    assertTrue(h.x0() >= 0 && h.x0() + h.w() <= p.regionBlocks && h.z0() >= 0 && h.z0() + h.d() <= p.regionBlocks);
                    for (de.officerooms.room.Extras.Vault v : ex.vaults) {
                        assertTrue(v.s() != h.s() || Math.max(Math.abs(v.x0() - h.x0()), Math.abs(v.z0() - h.z0())) >= 14, "hideout clashes with vault");
                    }
                    for (int j = i + 1; j < hs.size(); j++) {
                        de.officerooms.room.Extras.Hideout o = hs.get(j);
                        assertTrue(o.s() != h.s() || Math.max(Math.abs(o.x0() - h.x0()), Math.abs(o.z0() - h.z0())) >= 14, "hideouts overlap");
                    }
                    // doorway is open, walls closed
                    assertTrue(h.covers(h.doorX(), h.doorZ()));
                }
            }
        }
        System.out.println("[Hideouts] " + total + " in 256 regions, empty " + empty + ", by type " + java.util.Arrays.toString(types));
        assertTrue(total > 0);
        assertTrue(total < 256 * 6);
    }

    @Test
    void hotel() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int sites = 0;
        for (int rx = -12; rx < 12; rx++) {
            for (int rz = -12; rz < 12; rz++) {
                de.officerooms.megastructure.Hotel.Site s = p.hotel.site(rx, rz);
                if (s == null) {
                    continue;
                }
                sites++;
                assertTrue(p.extras.of(rx, rz) == null, "no elevators or hidden rooms inside the hotel region");
                // every staircase: a step per height, free headroom, landing on the next floor
                int[] sz = {20, 60, 100};
                for (int i = 0; i < 3; i++) {
                    for (int k = 0; k < 5; k++) {
                        assertEquals(de.officerooms.megastructure.Hotel.Part.STAIR, s.part(84, i * 6 + 1 + k, sz[i] + k, false));
                        assertEquals(de.officerooms.megastructure.Hotel.Part.AIR, s.part(84, i * 6 + 2 + k, sz[i] + k, false));
                        assertEquals(de.officerooms.megastructure.Hotel.Part.AIR, s.part(84, i * 6 + 3 + k, sz[i] + k, false));
                    }
                }
                // vault: closed shell with one doorway in the south wall
                int open = 0;
                for (int a = 0; a < de.officerooms.megastructure.Hotel.VS; a++) {
                    for (int[] e : new int[][]{{s.vx + a, s.vz}, {s.vx + a, s.vz + 8}, {s.vx, s.vz + a}, {s.vx + 8, s.vz + a}}) {
                        open += s.part(e[0], 1, e[1], false) == de.officerooms.megastructure.Hotel.Part.AIR ? 1 : 0;
                    }
                }
                assertEquals(1, open, "vault doorway");
                assertEquals(de.officerooms.megastructure.Hotel.Part.MARBLE, s.part(40, 6, 40, false));
                assertEquals(de.officerooms.megastructure.Hotel.Part.RING, s.part(1, 8, 50, false));
                assertEquals(de.officerooms.megastructure.Hotel.Part.AIR, s.part(1, 8, 50, true));
            }
        }
        System.out.println("[Hotel] " + sites + " hotels in 576 regions");
        assertTrue(sites > 0);
    }

    @Test
    void industrial() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int sites = 0;
        for (int rx = -12; rx < 12; rx++) {
            for (int rz = -12; rz < 12; rz++) {
                de.officerooms.megastructure.Industrial.Site s = p.industrial.site(rx, rz);
                if (s == null) {
                    continue;
                }
                sites++;
                assertTrue(p.extras.of(rx, rz) == null);
                assertTrue(p.hotel.site(rx, rz) == null && p.maze.site(rx, rz) == null);
                int stairs = 0;
                for (int lv = 0; lv < 4; lv++) {
                    for (int cx = 0; cx < 10; cx++) {
                        for (int cz = 0; cz < 10; cz++) {
                            if (s.type(lv, cx, cz) != de.officerooms.megastructure.Industrial.STAIR) {
                                continue;
                            }
                            stairs++;
                            for (int k = 0; k < 5; k++) {           // steps, headroom and the opening above
                                int lx = cx * 16 + 4 + k;
                                assertEquals(de.officerooms.megastructure.Industrial.Part.STAIR, s.part(lx, lv * 6 + 1 + k, cz * 16 + 7, false));
                                assertEquals(de.officerooms.megastructure.Industrial.Part.AIR, s.part(lx, lv * 6 + 2 + k, cz * 16 + 7, false));
                                assertEquals(de.officerooms.megastructure.Industrial.Part.AIR, s.part(lx, lv * 6 + 3 + k, cz * 16 + 7, false));
                            }
                        }
                    }
                }
                assertEquals(12, stairs, "three staircases per level");
                int[] h = s.halls[0];                                // hall: open from the floor to the catwalk level
                assertEquals(de.officerooms.megastructure.Industrial.Part.AIR, s.part(h[0] * 16 + 16, 9, h[1] * 16 + 3 + 2, false));
                assertEquals(de.officerooms.megastructure.Industrial.Part.GRATE, s.part(h[0] * 16 + 1, 6, h[1] * 16 + 16, false));
            }
        }
        System.out.println("[Industrial] " + sites + " complexes in 576 regions");
        assertTrue(sites > 0);
    }

    @Test
    void levelFun() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int sites = 0;
        int[] kinds = new int[7];
        for (int rx = -12; rx < 12; rx++) {
            for (int rz = -12; rz < 12; rz++) {
                de.officerooms.megastructure.LevelFun.Site s = p.fun.site(rx, rz);
                if (s == null) {
                    continue;
                }
                sites++;
                assertTrue(p.extras.of(rx, rz) == null && p.industrial.site(rx, rz) == null && p.hotel.site(rx, rz) == null);
                for (int cx = 0; cx < 16; cx++) {
                    for (int cz = 0; cz < 16; cz++) {
                        kinds[s.cellType(cx, cz)]++;
                    }
                }
                int open = 0;                                       // most of the floor is walkable
                for (int lx = 3; lx < 157; lx++) {
                    for (int lz = 3; lz < 157; lz++) {
                        de.officerooms.megastructure.LevelFun.Part a = s.part(lx, 1, lz, false);
                        open += a == de.officerooms.megastructure.LevelFun.Part.AIR || a == de.officerooms.megastructure.LevelFun.Part.CONFETTI ? 1 : 0;
                    }
                }
                assertTrue(open > 154 * 154 * 0.7, "open floor " + open);
            }
        }
        System.out.println("[LevelFun] " + sites + " in 576 regions, cell types " + java.util.Arrays.toString(kinds));
        assertTrue(sites > 0);
        for (int k : kinds) {
            assertTrue(k > 0);
        }
    }
}
