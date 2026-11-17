package de.officerooms.room;

import de.officerooms.atrium.Atrium;
import de.officerooms.room.connection.Openings;
import de.officerooms.room.structure.BlockKind;
import de.officerooms.room.structure.BlockSampler;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests des Raumplaners ohne Minecraft. Prüft die harten Regeln aus PHASE1_AUFTRAG.md auf
 * Blockebene über ganze Regionen und über Regionsgrenzen hinweg.
 */
class PlannerTest {

    private static final int MIN_Y = -64;
    private static final int HEIGHT = 608;

    private static OfficePlanner planner(long seed) {
        return new OfficePlanner(seed, GenSettings.DEFAULT, MIN_Y, HEIGHT);
    }

    @Test
    void verticalGridFitsDimension() {
        OfficePlanner p = planner(1);
        assertEquals(101, p.storeys, "101 Geschosse à 6 Blöcke (Quarz, Wolle, 4 Luft) passen in Y -64..543");
        assertEquals(-63, p.slabY(0));
        assertEquals(-62, p.floorY(0));
        assertTrue(p.slabY(p.storeys) <= MIN_Y + HEIGHT - 1, "Abschlussdecke innerhalb der Bauhöhe");
    }

    @Test
    void deterministicAcrossInstancesAndOrder() {
        OfficePlanner a = planner(42);
        OfficePlanner b = planner(42);
        // b berechnet zuerst andere Regionen: Reihenfolge darf keinen Einfluss haben.
        b.plan(3, -2);
        b.plan(1, 1);
        BlockSampler sa = new BlockSampler(a);
        BlockSampler sb = new BlockSampler(b);
        byte[] ca = new byte[HEIGHT];
        byte[] cb = new byte[HEIGHT];
        for (int x = 140; x < 180; x += 3) {
            for (int z = -20; z < 20; z += 3) {
                sa.column(x, z, ca);
                sb.column(x, z, cb);
                assertArrayEquals(ca, cb, "Spalte " + x + "," + z);
            }
        }
        OfficePlanner c = planner(43);
        BlockSampler sc = new BlockSampler(c);
        int diff = 0;
        for (int x = 0; x < 64; x++) {
            sa.column(x, 7, ca);
            sc.column(x, 7, cb);
            if (!java.util.Arrays.equals(ca, cb)) {
                diff++;
            }
        }
        assertTrue(diff > 10, "anderer Seed erzeugt andere Architektur");
    }

    @Test
    void blockRulesHoldAcrossRegionBoundaries() {
        for (long seed : new long[]{12345L, -7L, 987654321L}) {
            OfficePlanner p = planner(seed);
            PlanVerifier v = new PlanVerifier(p);
            BlockSampler s = new BlockSampler(p);
            // Quader über die Ecke von vier Regionen (x/z = 0 ist Regionsgrenze)
            PlanVerifier.Result r = v.verify(s::kind, -72, -72, 71, 71, MIN_Y, MIN_Y + HEIGHT - 1, false);
            System.out.println("Seed " + seed + ": " + r);
            r.errors.forEach(e -> System.out.println("  " + e));
            assertTrue(r.ok(), "Regeln verletzt für Seed " + seed);
            assertTrue(r.doorBlocks > 1000, "Türen vorhanden");
        }
    }

    @Test
    void windowsOnlyIntoAtriaWholeRegion() {
        OfficePlanner p = planner(2024);
        PlanVerifier v = new PlanVerifier(p);
        BlockSampler s = new BlockSampler(p);
        PlanVerifier.Result r = v.verify(s::kind, 160, 160, 319, 319, MIN_Y, MIN_Y + HEIGHT - 1, false);
        System.out.println("Region 1,1: " + r);
        r.errors.forEach(e -> System.out.println("  " + e));
        assertTrue(r.ok());
        assertTrue(r.glass > 0, "Region hat Atriumfenster");
    }

    @Test
    void roomHeightDistributionMatchesTarget() {
        OfficePlanner p = planner(777);
        long[] bands = new long[4];
        long total = 0;
        for (int rx = 0; rx < 3; rx++) {
            for (int rz = 0; rz < 3; rz++) {
                for (Room r : p.layout(rx, rz).rooms) {
                    if (r.kind != Room.Kind.ROOM || r.cells == 0 || r.sealed) {
                        continue;
                    }
                    total++;
                    bands[r.clear <= 4 ? 0 : r.clear <= 8 ? 1 : r.clear <= 16 ? 2 : 3]++;
                }
            }
        }
        double[] share = new double[4];
        for (int i = 0; i < 4; i++) {
            share[i] = 100.0 * bands[i] / total;
        }
        System.out.printf("Raumhöhen über %d Räume: 4=%.2f%% 5-8=%.2f%% 9-16=%.2f%% >16=%.2f%%%n",
                total, share[0], share[1], share[2], share[3]);
        assertEquals(90.0, share[0], 1.0);
        assertEquals(7.0, share[1], 1.0);
        assertEquals(2.0, share[2], 0.6);
        assertEquals(1.0, share[3], 0.5);
    }

    @Test
    void atriaAreEnclosedAndVaried() {
        OfficePlanner p = planner(5);
        int atria = 0;
        int maxClear = 0;
        int galleries = 0;
        Map<String, Integer> classes = new HashMap<>();
        for (int rx = -2; rx < 2; rx++) {
            for (int rz = -2; rz < 2; rz++) {
                RegionLayout l = p.layout(rx, rz);
                for (Atrium a : l.atria) {
                    atria++;
                    maxClear = Math.max(maxClear, a.clear);
                    galleries += a.galleryCount();
                    classes.merge(a.cls, 1, Integer::sum);
                    assertTrue(a.ux >= 1 && a.uz >= 1 && a.ux + a.w <= p.ru - 1 && a.uz + a.d <= p.ru - 1, "Abstand zum Regionsrand");
                    assertTrue(a.s0 >= 1 && a.top() <= p.storeys - 3, "Geschoss über und unter dem Atrium");
                    assertTrue(a.clear <= a.storeys * p.pitch - 1 && a.clear > (a.storeys - 1) * p.pitch - 1, "Höhe passt zur Geschosszahl");
                }
            }
        }
        System.out.println("Atrien in 16 Regionen: " + atria + ", Klassen " + classes + ", max. Höhe " + maxClear + ", Galerien " + galleries);
        assertTrue(atria >= 16, "regelmäßig Atrien");
        assertTrue(maxClear >= 80, "auch sehr hohe Atrien");
        assertTrue(galleries > 0, "Galerien vorhanden");
    }

    /** Alle erreichbaren Räume eines 4×4-Regionsblocks bilden eine einzige Zusammenhangskomponente. */
    @Test
    void globalConnectivityAcrossRegions() {
        OfficePlanner p = planner(31337);
        int n = 4;
        Map<Long, Integer> ids = new HashMap<>();
        int[] parent = new int[2_000_000];
        for (int i = 0; i < parent.length; i++) {
            parent[i] = i;
        }
        java.util.function.BiFunction<Integer, Integer, java.util.function.IntUnaryOperator> idOf = (rx, rz) -> node -> {
            long key = (((long) rx * 1000 + rz) << 32) | node;
            return ids.computeIfAbsent(key, k -> ids.size());
        };
        for (int rx = 0; rx < n; rx++) {
            for (int rz = 0; rz < n; rz++) {
                RegionPlan plan = p.plan(rx, rz);
                RegionLayout l = plan.layout;
                var self = idOf.apply(rx, rz);
                for (var c : l.connections) {
                    union(parent, self.applyAsInt(c.nodeA()), self.applyAsInt(c.nodeB()));
                }
                // Grenztüren auf West- (rx > 0) und Nordlinie (rz > 0)
                for (int axis = 0; axis < 2; axis++) {
                    if ((axis == 0 && rx == 0) || (axis == 1 && rz == 0)) {
                        continue;
                    }
                    RegionLayout other = p.layout(axis == 0 ? rx - 1 : rx, axis == 0 ? rz : rz - 1);
                    var oid = idOf.apply(axis == 0 ? rx - 1 : rx, axis == 0 ? rz : rz - 1);
                    for (int s = 0; s < l.storeys; s++) {
                        for (int i = 0; i < p.ru; i++) {
                            int code = axis == 0 ? plan.edge(s, 0, i, 0) : plan.edge(s, i, 0, 1);
                            if (Openings.type(code) != Openings.DOOR) {
                                continue;
                            }
                            Room a = axis == 0 ? l.room(s, 0, i) : l.room(s, i, 0);
                            Room b = axis == 0 ? other.room(s, p.ru - 1, i) : other.room(s, i, p.ru - 1);
                            assertTrue(a != null && b != null && a.bottom == s && b.bottom == s, "Grenztür zwischen begehbaren Räumen");
                            union(parent, self.applyAsInt(a.index), oid.applyAsInt(b.index));
                        }
                    }
                }
            }
        }
        Integer root = null;
        int rooms = 0;
        int sealed = 0;
        for (int rx = 0; rx < n; rx++) {
            for (int rz = 0; rz < n; rz++) {
                var self = idOf.apply(rx, rz);
                for (Room r : p.layout(rx, rz).rooms) {
                    if (r.cells == 0 || r.kind == Room.Kind.ATRIUM) {
                        continue;
                    }
                    if (r.sealed) {
                        sealed++;
                        continue;
                    }
                    rooms++;
                    int f = find(parent, self.applyAsInt(r.index));
                    if (root == null) {
                        root = f;
                    }
                    assertEquals(root.intValue(), f, "Raum " + r + " in Region " + rx + "," + rz + " nicht erreichbar");
                }
            }
        }
        System.out.println("Zusammenhängend: " + rooms + " Räume/Korridore/Treppen in 16 Regionen, versiegelt " + sealed
                + String.format(" (%.3f %%)", 100.0 * sealed / (rooms + sealed)));
        assertTrue(sealed < 0.01 * (rooms + sealed), "weniger als 1 % versiegelt");
    }

    @Test
    void floorAndCapAreSolid() {
        OfficePlanner p = planner(9);
        BlockSampler s = new BlockSampler(p);
        byte[] col = new byte[HEIGHT];
        for (int x = -50; x < 50; x += 7) {
            for (int z = -50; z < 50; z += 7) {
                s.column(x, z, col);
                assertEquals(BlockKind.BEDROCK, col[0]);
                for (int y = p.slabY(p.storeys); y < MIN_Y + HEIGHT; y++) {
                    assertTrue(col[y - MIN_Y] != BlockKind.AIR, "Abschluss massiv");
                }
            }
        }
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) {
            p[x] = p[p[x]];
            x = p[x];
        }
        return x;
    }

    private static void union(int[] p, int a, int b) {
        int ra = find(p, a);
        int rb = find(p, b);
        if (ra != rb) {
            p[ra] = rb;
        }
    }
}
