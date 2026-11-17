package de.officerooms.room.structure;

import de.officerooms.atrium.Atrium;
import de.officerooms.room.Extras;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.RegionPlan;
import de.officerooms.room.Room;
import de.officerooms.room.connection.Openings;

/**
 * Rasterer: berechnet für jede Weltkoordinate die {@link BlockKind} aus der Regionsplanung.
 *
 * <p>Die Funktion ist rein (keine Zustände, keine Nachbarchunks). Jeder Chunk ruft sie für seine
 * eigenen Spalten auf und erhält dadurch exakt die Blöcke, die zu den Plänen aller Nachbarn
 * passen. Große Räume und Atrien über mehrere Chunks werden so ohne Überschneidung, ohne
 * doppelte Platzierung und ohne Abschneiden erzeugt.</p>
 *
 * <p>Geometrie einer Zelle (Standard unit = 4): lokale Position 0 ist die Wandlinie, 1..3 ist
 * Innenraum. Eine Linie zwischen zwei Zellen desselben Raums ist Innenraum (so verschmelzen
 * Module zu Großräumen ohne Zwischenwände), zwischen verschiedenen Räumen ist sie Wand.</p>
 */
public final class BlockSampler {

    private static final Room FOREIGN = new Room(-1, Room.Kind.ROOM, -1, 0, 0);

    private final OfficePlanner p;
    private final int unit;
    private final int pitch;
    private final int phys;
    private final int doorHeight;

    public BlockSampler(OfficePlanner planner) {
        this.p = planner;
        this.unit = planner.unit;
        this.pitch = planner.pitch;
        this.phys = planner.phys;
        this.doorHeight = Math.max(2, Math.min(3, planner.pitch - 2));
    }

    public OfficePlanner planner() {
        return p;
    }

    /** Füllt {@code out[y - minY]} für die ganze Spalte (x, z). */
    public void column(int x, int z, byte[] out) {
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        RegionPlan plan = p.plan(rx, rz);
        int lx = x - rx * p.regionBlocks;
        int lz = z - rz * p.regionBlocks;
        int n = Math.min(out.length, p.height);
        for (int i = 0; i < n; i++) {
            out[i] = kindLocal(plan, lx, lz, p.minY + i);
        }
        Extras.Region ex = p.extras.of(rx, rz);
        if (ex != null && (ex.inElevator(lx, lz) || !ex.vaults.isEmpty() || !ex.hideouts.isEmpty())) {
            for (int i = 1; i < n; i++) {
                int rel = p.minY + i - p.y0;
                if (rel >= 0) {
                    out[i] = ex.override(lx, lz, rel / phys, rel % phys, out[i]);
                }
            }
        }
    }

    public byte kind(int x, int y, int z) {
        if (y < p.minY || y >= p.minY + p.height) {
            return BlockKind.AIR;
        }
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        RegionPlan plan = p.plan(rx, rz);
        int lx = x - rx * p.regionBlocks;
        int lz = z - rz * p.regionBlocks;
        byte k = kindLocal(plan, lx, lz, y);
        Extras.Region ex = p.extras.of(rx, rz);
        if (ex != null && y > p.minY && y >= p.y0) {
            int rel = y - p.y0;
            k = ex.override(lx, lz, rel / phys, rel % phys, k);
        }
        return k;
    }

    private byte kindLocal(RegionPlan plan, int lx, int lz, int y) {
        if (y == p.minY) {
            return BlockKind.BEDROCK;
        }
        int rel = y - p.y0;
        int s = rel / phys;
        int pl = rel % phys;
        if (s >= p.storeys) {
            return (s == p.storeys && pl <= 1) ? BlockKind.FLOOR : BlockKind.FILL;
        }
        // Physische Schicht pl -> Entwurfsschicht: 0 und 1 sind beide die Bodenschicht (ly 0),
        // darüber pl = ly + 1. Treppen rechnen direkt physisch (eine Stufe mehr).
        if (pl == 0) {
            return kindIn(plan, lx, lz, s, 0, 0);
        }
        if (pl == 1) {
            byte k = kindIn(plan, lx, lz, s, 0, 1);
            if (k == BlockKind.FLOOR && BlockKind.isAirLike(kindIn(plan, lx, lz, s, 1, 2))) {
                return BlockKind.FLOOR_WOOL; // begehbare Fläche: graue Wolle statt Teppich
            }
            return k;
        }
        return kindIn(plan, lx, lz, s, pl - 1, pl);
    }

    byte kindIn(RegionPlan plan, int lx, int lz, int s, int ly, int pl) {
        RegionLayout l = plan.layout;
        int ux = lx / unit;
        int ox = lx % unit;
        int uz = lz / unit;
        int oz = lz % unit;
        if (ox != 0 && oz != 0) {
            Room r = l.room(s, ux, uz);
            if (r == null) {
                return BlockKind.FILL;
            }
            return interior(r, s, ly, pl, lx, lz, ux, uz, ux, uz, ux, uz, ux, uz);
        }
        if (ox == 0 && oz != 0) {
            Room a = ux > 0 ? l.room(s, ux - 1, uz) : FOREIGN;
            Room b = l.room(s, ux, uz);
            if (a == b) {
                return a == null ? BlockKind.FILL : interior(a, s, ly, pl, lx, lz, ux - 1, uz, ux, uz, ux, uz, ux, uz);
            }
            return wallLine(plan.edge(s, ux, uz, 0), oz, ly);
        }
        if (ox != 0) {
            Room a = uz > 0 ? l.room(s, ux, uz - 1) : FOREIGN;
            Room b = l.room(s, ux, uz);
            if (a == b) {
                return a == null ? BlockKind.FILL : interior(a, s, ly, pl, lx, lz, ux, uz - 1, ux, uz, ux, uz, ux, uz);
            }
            return wallLine(plan.edge(s, ux, uz, 1), ox, ly);
        }
        // Wandknoten (Pfosten) zwischen vier Zellen
        Room c00 = (ux > 0 && uz > 0) ? l.room(s, ux - 1, uz - 1) : FOREIGN;
        Room c10 = uz > 0 ? l.room(s, ux, uz - 1) : FOREIGN;
        Room c01 = ux > 0 ? l.room(s, ux - 1, uz) : FOREIGN;
        Room c11 = l.room(s, ux, uz);
        if (c00 == c10 && c10 == c01 && c01 == c11) {
            return c00 == null ? BlockKind.FILL
                    : interior(c00, s, ly, pl, lx, lz, ux - 1, uz - 1, ux, uz - 1, ux - 1, uz, ux, uz);
        }
        if (c00 == null && c10 == null && c01 == null && c11 == null) {
            return BlockKind.FILL;
        }
        if (ly == 0) {
            return BlockKind.FLOOR;
        }
        if (ux > 0 && uz > 0) {
            if (c00 == c01 && c10 == c11
                    && Openings.type(plan.edge(s, ux, uz - 1, 0)) == Openings.OPEN
                    && Openings.type(plan.edge(s, ux, uz, 0)) == Openings.OPEN) {
                return BlockKind.AIR;
            }
            if (c00 == c10 && c01 == c11
                    && Openings.type(plan.edge(s, ux - 1, uz, 1)) == Openings.OPEN
                    && Openings.type(plan.edge(s, ux, uz, 1)) == Openings.OPEN) {
                return BlockKind.AIR;
            }
        }
        boolean straight = (c00 == c01 && c10 == c11) || (c00 == c10 && c01 == c11);
        return straight ? BlockKind.WALL : BlockKind.PILLAR;
    }

    private byte wallLine(int code, int pos, int ly) {
        if (ly == 0) {
            return BlockKind.FLOOR;
        }
        int t = Openings.type(code);
        if (t == Openings.DOOR && Openings.covers(code, pos) && ly <= doorHeight) {
            return BlockKind.AIR;
        }
        if (t == Openings.OPEN) {
            return BlockKind.AIR;
        }
        if (t == Openings.WINDOW && Openings.covers(code, pos) && ly >= 2 && ly <= 1 + Openings.windowHeight(code)) {
            return BlockKind.GLASS;
        }
        return BlockKind.WALL;
    }

    private byte interior(Room r, int s, int ly, int pl, int lx, int lz,
                          int ax, int az, int bx, int bz, int cx, int cz, int dx, int dz) {
        switch (r.kind) {
            case STAIRS:
                return stairs(r, s, pl, lx, lz);
            case ATRIUM: {
                Atrium a = r.atrium;
                int t = (s - a.s0) * pitch + ly;
                if (t == 0) {
                    return BlockKind.FLOOR;
                }
                if (t > a.clear) {
                    return BlockKind.CEILING;
                }
                if (ly <= 2 && a.hasGallery(s)) {
                    boolean walk = a.isWalk(s, ax, az) || a.isWalk(s, bx, bz) || a.isWalk(s, cx, cz) || a.isWalk(s, dx, dz);
                    boolean open = !a.isWalk(s, ax, az) || !a.isWalk(s, bx, bz) || !a.isWalk(s, cx, cz) || !a.isWalk(s, dx, dz);
                    if (ly == 0) {
                        return walk ? BlockKind.FLOOR : BlockKind.AIR;
                    }
                    if (walk && open) {
                        return ly == 1 ? BlockKind.PARAPET : BlockKind.PARAPET_TOP;
                    }
                }
                return BlockKind.AIR;
            }
            default: {
                int t = (s - r.bottom) * pitch + ly;
                if (t == 0) {
                    return BlockKind.FLOOR;
                }
                return t > r.clear ? BlockKind.CEILING : BlockKind.AIR;
            }
        }
    }

    /**
     * Gerades Treppenhaus über ein Geschoss, physisch gerechnet (Geschossraster phys = 6:
     * Quarz, Wolle, 4 Blöcke Luft). Achskoordinate a (1 .. Länge-1) vom unteren Ende:
     * a = 1..2 unterer Absatz, a = 3..phys+1 Stufen (phys-1 Stück), danach oberer Absatz auf
     * Höhe des nächsten Geschossbodens. pl ist die physische Schicht im Geschoss s.
     */
    private byte stairs(Room r, int s, int pl, int lx, int lz) {
        int a;
        switch (r.stairDir) {
            case 0 -> a = lx - r.stairX * unit;
            case 2 -> a = (r.stairX + 1) * unit - lx;
            case 1 -> a = lz - r.stairZ * unit;
            default -> a = (r.stairZ + 1) * unit - lz;
        }
        int t = (s - r.bottom) * phys + pl;
        if (a <= 2) {
            return t == 0 ? BlockKind.FLOOR : t == 1 ? BlockKind.FLOOR_WOOL : BlockKind.AIR;
        }
        if (a <= phys + 1) {
            int step = a - 1; // erste Stufe auf Laufhöhe 2, oberste auf phys
            if (t == step) {
                return (byte) (BlockKind.STAIR_E + r.stairDir);
            }
            return t < step ? BlockKind.WALL : BlockKind.AIR;
        }
        if (t == phys) {
            return BlockKind.FLOOR;
        }
        if (t == phys + 1) {
            return BlockKind.FLOOR_WOOL;
        }
        return t < phys ? BlockKind.WALL : BlockKind.AIR;
    }
}
