package de.officerooms.room;

import de.officerooms.room.structure.BlockKind;
import de.officerooms.room.structure.BlockSampler;

import java.util.ArrayList;
import java.util.List;

/**
 * Prüft einen Blockquader auf die harten Regeln von Phase 1. Wird von den Unit-Tests (mit dem
 * Rasterer als Blockquelle) und im Spiel von {@code /officerooms verify} (mit den tatsächlich
 * generierten Weltblöcken) verwendet.
 *
 * <ul>
 *     <li>Jeder Glasblock sitzt in einer Wand zwischen einem erreichbaren Raum und einem Atrium,
 *     beide Seiten sind Luft.</li>
 *     <li>Jede Öffnung in einer Wand zwischen zwei verschiedenen Räumen führt auf beiden Seiten in
 *     Luft eines erreichbaren Raums (keine Tür in eine Wand, ins Massiv oder ins Leere).</li>
 *     <li>Keine Luft in massiven oder versiegelten Zellen, keine Luft in der Abschlussdecke.</li>
 *     <li>Optional: tatsächliche Blöcke stimmen mit der Planung überein.</li>
 * </ul>
 */
public final class PlanVerifier {

    /** Blockquelle: liefert die {@link BlockKind} an einer Weltposition. */
    @FunctionalInterface
    public interface KindSource {
        int kind(int x, int y, int z);
    }

    /** Blockquelle meldet einen Einrichtungsblock (Möbel, Leuchte), der in geplanter Luft steht. */
    public static final int FURNISHING = -1;
    /** Blockquelle meldet Wasser. Erlaubt nur im Bereich eines Wasserbüros (Phase 4). */
    public static final int WATER = -3;

    public static final class Result {
        /** Einrichtungsblöcke in geplanter Luft (Möbel, Leuchten). */
        public long furnishing;
        public long blocks;
        public long glass;
        /** Davon Glas zwischen zwei Büroräumen. */
        public long officeGlass;
        public long doorBlocks;
        public long mismatches;
        /** Blöcke in Spezialräumen (Wände, Boden, Decke und Innenraum): dort wird nur auf Wasserlecks geprüft. */
        public long special;
        public long water;
        public final List<String> errors = new ArrayList<>();

        void error(String msg) {
            if (errors.size() < 50) {
                errors.add(msg);
            }
        }

        public boolean ok() {
            return errors.isEmpty() && mismatches == 0;
        }

        @Override
        public String toString() {
            return "blocks " + blocks + ", glass " + glass + " (office windows " + officeGlass + "), door air " + doorBlocks + ", furniture blocks " + furnishing + ", special room blocks " + special + " (water " + water + "), deviations " + mismatches
                    + ", errors " + errors.size();
        }
    }

    private final OfficePlanner p;
    private final BlockSampler sampler;

    public PlanVerifier(OfficePlanner planner) {
        this.p = planner;
        this.sampler = new BlockSampler(planner);
    }

    /** Raum an einer Innenraum-Position (nicht auf einer Wandlinie), oder null. */
    public Room roomAt(int x, int y, int z) {
        int s = p.storeyOf(y);
        if (s < 0 || s >= p.storeys) {
            return null;
        }
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        RegionLayout l = p.layout(rx, rz);
        int lx = x - rx * p.regionBlocks;
        int lz = z - rz * p.regionBlocks;
        return l.room(s, lx / p.unit, lz / p.unit);
    }

    /**
     * Spezialraum, zu dem der Block gehört: Innenraum, Wände (Linien am Rand), Boden und Decke.
     * Spezialräume verändern Wände/Boden/Decke (Sculk, Moos) und verschließen Öffnungen.
     */
    public de.officerooms.special.SpecialType specialZone(int x, int y, int z) {
        int rx0 = p.regionOf(x);
        int rz0 = p.regionOf(z);
        int rel = y - p.y0;
        if (rel < 0) {
            return de.officerooms.special.SpecialType.NONE;
        }
        int s = rel / p.phys;
        int ly = rel % p.phys; // 0 und 1: Geschossplatte (gehört auch zum Raum darunter)
        boolean lx = onLineX(x);
        boolean lz = onLineX(z);
        for (int ds = 0; ds <= (ly <= 1 ? 1 : 0); ds++) {
            int st = s - ds;
            if (st < 0 || st >= p.storeys) {
                continue;
            }
            for (int ox = 0; ox <= (lx ? 1 : 0); ox++) {
                for (int oz = 0; oz <= (lz ? 1 : 0); oz++) {
                    int cx = x - ox;
                    int cz = z - oz;
                    int rx = p.regionOf(cx);
                    int rz = p.regionOf(cz);
                    RegionLayout l = p.layout(rx, rz);
                    Room r = l.room(st, Math.floorMod(cx, p.regionBlocks) / p.unit, Math.floorMod(cz, p.regionBlocks) / p.unit);
                    if (r == null || r.kind != Room.Kind.ROOM) {
                        continue;
                    }
                    de.officerooms.special.SpecialType t = de.officerooms.special.SpecialRooms.of(p, rx, rz, r);
                    if (t != de.officerooms.special.SpecialType.NONE) {
                        return t;
                    }
                }
            }
        }
        return de.officerooms.special.SpecialType.NONE;
    }

    private boolean onLineX(int x) {
        return Math.floorMod(x, p.unit) == 0;
    }

    public Result verify(KindSource world, int x0, int z0, int x1, int z1, int yMin, int yMax, boolean compare) {
        Result res = new Result();
        int maxY = p.minY + p.height - 1;
        int capY = p.slabY(p.storeys);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    int k = world.kind(x, y, z);
                    res.blocks++;
                    if (p.megaContains(x, y, z)) {
                        // Poolrooms: eigene Architektur, nicht mit dem Büroplan vergleichbar
                        res.special++;
                        continue;
                    }
                    de.officerooms.special.SpecialType sp = specialZone(x, y, z);
                    if (k == WATER) {
                        res.water++;
                        if (sp != de.officerooms.special.SpecialType.WATER_OFFICE && sp != de.officerooms.special.SpecialType.VILLAGER_OUTPOST
                                && sp != de.officerooms.special.SpecialType.OASIS) {
                            res.error("Water outside a water office at " + x + " " + y + " " + z);
                        }
                    }
                    if (sp != de.officerooms.special.SpecialType.NONE) {
                        res.special++;
                        continue;
                    }
                    if (compare) {
                        int e = sampler.kind(x, y, z);
                        if (k == FURNISHING && BlockKind.isAirLike(e)) {
                            res.furnishing++;
                        } else if (BlockKind.canonical(e) != BlockKind.canonical(k)) {
                            res.mismatches++;
                            if (res.mismatches <= 5) {
                                res.error("Deviation at " + x + " " + y + " " + z + ": Welt " + k + ", Plan " + e);
                            }
                        }
                    }
                    if (y >= capY && BlockKind.isAirLike(k)) {
                        res.error("Air in the top ceiling at " + x + " " + y + " " + z);
                    }
                    if (y == maxY && BlockKind.isAirLike(k)) {
                        res.error("Air at the build limit at " + x + " " + y + " " + z);
                    }
                    if (k == BlockKind.GLASS) {
                        res.glass++;
                        checkGlass(world, res, x, y, z);
                    } else if (BlockKind.isAirLike(k)) {
                        boolean lx = onLineX(x);
                        boolean lz = onLineX(z);
                        if (lx != lz) {
                            checkLineAir(world, res, x, y, z, lx);
                        } else if (!lx) {
                            if (roomAt(x, y, z) == null) {
                                res.error("Air in a solid/sealed cell at " + x + " " + y + " " + z);
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private void checkGlass(KindSource world, Result res, int x, int y, int z) {
        boolean lx = onLineX(x);
        boolean lz = onLineX(z);
        if (lx == lz) {
            res.error("Glass not on a wall line at " + x + " " + y + " " + z);
            return;
        }
        int ax = lx ? x - 1 : x;
        int az = lx ? z : z - 1;
        int bx = lx ? x + 1 : x;
        int bz = lx ? z : z + 1;
        Room a = roomAt(ax, y, az);
        Room b = roomAt(bx, y, bz);
        boolean atriumA = a != null && a.kind == Room.Kind.ATRIUM;
        boolean atriumB = b != null && b.kind == Room.Kind.ATRIUM;
        boolean roomA = a != null && a.isWalkableRoom();
        boolean roomB = b != null && b.isWalkableRoom();
        boolean officePair = a != null && b != null && a != b && a.kind == Room.Kind.ROOM && b.kind == Room.Kind.ROOM;
        if (officePair) {
            res.officeGlass++;
        }
        if (!((atriumA && roomB) || (atriumB && roomA) || officePair)) {
            res.error("Window not between a room and an atrium or two offices at " + x + " " + y + " " + z + " (" + a + " | " + b + ")");
        }
        if (!BlockKind.isAirLike(world.kind(ax, y, az)) || !BlockKind.isAirLike(world.kind(bx, y, bz))) {
            res.error("Window without air on both sides at " + x + " " + y + " " + z);
        }
    }

    private void checkLineAir(KindSource world, Result res, int x, int y, int z, boolean lineX) {
        int ax = lineX ? x - 1 : x;
        int az = lineX ? z : z - 1;
        int bx = lineX ? x + 1 : x;
        int bz = lineX ? z : z + 1;
        Room a = roomAt(ax, y, az);
        Room b = roomAt(bx, y, bz);
        if (a != null && a == b) {
            return; // Linie innerhalb eines Raums
        }
        res.doorBlocks++;
        if (a == null || b == null) {
            res.error("Opening into a solid/sealed area at " + x + " " + y + " " + z);
            return;
        }
        if (!BlockKind.isAirLike(world.kind(ax, y, az)) || !BlockKind.isAirLike(world.kind(bx, y, bz))) {
            if (p.megaContains(ax, y, az) || p.megaContains(bx, y, bz)) {
                res.special++; // Bürotür am Rand der Poolrooms, vom Fliesenring verschlossen (gewollt)
                return;
            }
            res.error("Opening leads into a wall or furniture at " + x + " " + y + " " + z + " (" + a + " | " + b + ")");
        }
    }
}
