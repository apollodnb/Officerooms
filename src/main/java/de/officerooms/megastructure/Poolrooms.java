package de.officerooms.megastructure;

import de.officerooms.core.Hashing;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.Room;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;

/**
 * The Poolrooms (0.6.0): eine extrem seltene Megastruktur. Eine Region (160 × 160 Blöcke) ersetzt auf
 * {@link #STOREYS} Geschossen ({@link #H} Blöcke) ihre Büros durch eine liminale Halle aus cremefarbenen Fliesen
 * mit Becken, Säulen, Galerien, Gängen und einem versteckten Unterwasser-Labyrinth.
 *
 * <p>Reines Java: Die Planung hängt nur von Seed und Regionskoordinaten ab, die Blockwahl je Spalte wird
 * im Chunk-Generator abgefragt ({@link #kind}). Die Wandlinien lx = 0 / lz = 0 bleiben Büro (mit deren
 * Türen); innen läuft ein 3 Blöcke dicker Fliesenring, durch den an Türen der Geschosse s0+1 (Hallenboden)
 * und s0+3 (Galerie) Durchgänge führen.</p>
 *
 * <p>Höhen relativ zu yBase = Laufebene von Geschoss s0 (über dessen Quarz-/Wollplatte): 0 Fundament,
 * 1–3 Labyrinth/Becken, 4 Labyrinthdecke, 5 Hallenboden (Wasserspiegel der Becken), 6–25 Halle,
 * 17 Galerieboden, 26 Decke, 27 Abschluss unter der Platte von Geschoss s0 + 5. Hallen- und
 * Galerie-Laufebene (6 und 18) liegen auf der Laufebene der Geschosse s0 + 1 und s0 + 3 (Raster 6).</p>
 */
public final class Poolrooms {

    /** 0.35.0: eight storeys: deep pools under the hall floor, a 32-block hall, the gallery at 12. */
    public static final int STOREYS = 8;
    /** Bandhöhe: Geschosse à 6 Blöcke minus die 2 Plattenschichten von Geschoss s0. */
    public static final int H = STOREYS * 6 - 2;
    public static final int FLOOR = 11;
    public static final int GALLERY = 23;
    public static final int CEIL = 44;
    /** Treppenlauf Hallenboden → Galerie. */
    static final int RUN = GALLERY - FLOOR;
    public static final int LOW_CEIL = FLOOR + 6;
    /** 0.35.0: the underwater labyrinth lies just below the hall floor (its levels 1–4 above this). */
    public static final int MAZE0 = FLOOR - 5;
    static final int W = 160;

    // Spaltenarten
    public static final byte K_OFFICE = 0;
    public static final byte K_RING = 1;
    public static final byte K_OPEN = 2;
    public static final byte K_WALL = 3;
    public static final byte K_PILLAR = 4;

    // Labyrinth
    public static final byte M_NONE = 0;
    public static final byte M_WALL = 1;
    public static final byte M_WATER = 2;
    public static final byte M_AIR_POCKET = 3;
    public static final byte M_ENTRANCE = 4;
    public static final byte M_LANTERN = 5;

    /** Abgefragter Block (abstrakt); der Generator setzt daraus Minecraft-Blöcke. */
    public enum Part {
        AIR, TILE, FLOOR_TILE, WATER, LIGHT_PANEL, LIGHT_INVISIBLE, SEA_LANTERN, RAILING, STAIR_E, STAIR_S, STAIR_W, STAIR_N, OFFICE,
        /** 0.28.1: lifeguard chair post. */
        POST,
        /** 0.35.0: water slides (chute rising toward E/S/W/N), their tube, pool ladders (facing the water). */
        SLIDE_E, SLIDE_S, SLIDE_W, SLIDE_N, TUBE, LADDER_E, LADDER_S, LADDER_W, LADDER_N, RAIL_X, RAIL_Z,
        /** 0.35.1: slide pipes out of the wall (facing = where the mouth points). */
        PIPE_E, PIPE_S, PIPE_W, PIPE_N, PIPE_END_E, PIPE_END_S, PIPE_END_W, PIPE_END_N
    }

    /** 0.28.1 decoration codes (new worlds): bench, lifeguard post, lifeguard seat (+dir), freestanding arch pillar and lintel. */
    static final byte D_BENCH = 1;
    static final byte D_POST = 2;
    static final byte D_SEAT = 3;     // 3..6 = seat facing E, S, W, N
    static final byte D_ARCH = 7;
    static final byte D_LINTEL = 8;
    static final byte D_LADDER = 9;   // 9..12 = pool ladder facing the water E, S, W, N
    static final byte D_RAIL = 13;    // 13 = handrail running along x, 14 = along z

    public record Chest(int lx, int off, int lz, String loot, int facing) {
    }

    public record Lectern(int lx, int lz, int bookIndex, int facing) {
    }

    /** Fertiger Plan einer Fundstelle. */
    public static final class Site {
        public final int rx;
        public final int rz;
        public final int s0;
        public final int yBase;
        final byte[] kind = new byte[W * W];
        final byte[] ceil = new byte[W * W];
        final byte[] pool = new byte[W * W];
        final byte[] maze = new byte[W * W];
        final byte[] archTop = new byte[W * W];
        final byte[] stairK = new byte[W * W];
        final byte[] stairDir = new byte[W * W];
        final boolean[] balcony = new boolean[W * W];
        final boolean[] railing = new boolean[W * W];
        final boolean[] lantern = new boolean[W * W];
        final boolean[] lights = new boolean[W * W];
        /** 0.28.1 (new worlds): decoration code per column and drained pools. */
        final byte[] deco = new byte[W * W];
        final boolean[] drained = new boolean[W * W];
        /** 0.35.0 slides: chute surface height above FLOOR, role (1 chute, 2 tube wall) and ascent direction. */
        final byte[] slideH = new byte[W * W];
        final byte[] slideRole = new byte[W * W];
        final byte[] slideDir = new byte[W * W];
        public boolean modern;
        public final List<Chest> chests = new ArrayList<>();
        public final List<Lectern> lecterns = new ArrayList<>();
        /** 0.35.0: landing spots of the water slides (lx, lz): the slide openings in the offices lead here. */
        public final List<int[]> slideEnds = new ArrayList<>();
        /** Mitte der Halle über dem Labyrintheingang (für Tests). */
        public int entranceX = -1;
        public int entranceZ = -1;
        public int hallX = 80;
        public int hallZ = 80;

        Site(int rx, int rz, int s0, int yBase) {
            this.rx = rx;
            this.rz = rz;
            this.s0 = s0;
            this.yBase = yBase;
        }

        static int i(int lx, int lz) {
            return lz * W + lx;
        }

        public boolean inBand(int y) {
            return y >= yBase && y < yBase + H;
        }

        /** Ring-Spalte und auf welcher Seite (0 West, 1 Nord, 2 Ost, 3 Süd), sonst -1. */
        public int ringSide(int lx, int lz) {
            if (kind[i(lx, lz)] != K_RING) {
                return -1;
            }
            if (lx <= 3) {
                return 0;
            }
            if (lz <= 3) {
                return 1;
            }
            if (lx >= W - 3) {
                return 2;
            }
            return 3;
        }

        /**
         * Block an (lx, off, lz). {@code doorOpen} meldet für Ringspalten, ob die angrenzende Bürowand an
         * dieser Höhe eine Türöffnung hat.
         */
        public Part part(int lx, int off, int lz, boolean doorOpen) {
            int i = i(lx, lz);
            byte k = kind[i];
            if (k == K_OFFICE) {
                return Part.OFFICE;
            }
            if (off <= 0 || off >= CEIL) {
                return Part.TILE;
            }
            if (k == K_RING) {
                boolean walkLevel = (off >= FLOOR + 1 && off <= FLOOR + 3) || (off >= GALLERY + 1 && off <= GALLERY + 3);
                return walkLevel && doorOpen ? Part.AIR : Part.TILE;
            }
            // Unter dem Hallenboden: Labyrinth oder Beckenwasser
            if (off < FLOOR) {
                byte m = maze[i];
                if (m != M_NONE) {
                    int mo = off - MAZE0;
                    if (mo <= 0) {
                        return Part.TILE;
                    }
                    if (mo > 4) {
                        return m == M_ENTRANCE ? Part.WATER : Part.TILE;
                    }
                    if (mo == 4) {
                        return m == M_AIR_POCKET ? Part.AIR : m == M_ENTRANCE ? Part.WATER : Part.TILE;
                    }
                    return switch (m) {
                        case M_WALL -> Part.TILE;
                        case M_LANTERN -> mo == 2 ? Part.SEA_LANTERN : Part.TILE;
                        default -> Part.WATER;
                    };
                }
                int d = pool[i];
                if (d > 0 && off > FLOOR - d) {
                    return drained[i] ? Part.AIR : Part.WATER;
                }
                if (d > 0 && off == FLOOR - d && lantern[i]) {
                    return Part.SEA_LANTERN;
                }
                return Part.TILE;
            }
            if (off == FLOOR) {
                if (maze[i] == M_ENTRANCE) {
                    return Part.WATER;
                }
                return pool[i] > 0 ? (drained[i] ? Part.AIR : Part.WATER) : Part.FLOOR_TILE;
            }
            // 0.35.0: water slides (an enclosed tube on legs, open at both ends)
            if (slideRole[i] >= 3) {
                if (off == FLOOR + slideH[i]) {
                    return Part.values()[(slideRole[i] == 4 ? Part.PIPE_END_E : Part.PIPE_E).ordinal() + slideDir[i]];
                }
            } else if (slideRole[i] != 0 && off > FLOOR) {
                int hh = slideH[i];
                int rel = off - FLOOR - hh;
                if (slideRole[i] == 1) {
                    if (rel == 0) {
                        return Part.values()[Part.SLIDE_E.ordinal() + slideDir[i]];
                    }
                    if (rel == 3) {
                        return Part.TUBE;
                    }
                    if (rel > 0 && rel < 3) {
                        return Part.AIR;
                    }
                } else {
                    if (rel >= 0 && rel <= 3 && off <= GALLERY) {
                        return Part.TUBE;
                    }
                    if (rel < 0 && hh % 3 == 1) {
                        return Part.TUBE;                                   // legs
                    }
                }
            }
            byte dc = deco[i];
            if (dc != 0 && off > FLOOR) {
                int h = off - FLOOR;
                if (dc == D_RAIL || dc == D_RAIL + 1) {
                    return h == 1 ? (dc == D_RAIL ? Part.RAIL_X : Part.RAIL_Z) : Part.AIR;
                }
                if (dc >= D_LADDER && dc < D_LADDER + 4) {
                    return h == 1 ? Part.values()[Part.LADDER_E.ordinal() + dc - D_LADDER] : Part.AIR;
                }
                if (dc == D_BENCH) {
                    return h == 1 ? Part.RAILING : Part.AIR;
                }
                if (dc == D_POST) {
                    return h <= 2 ? Part.POST : h == 3 ? Part.TILE : Part.AIR;
                }
                if (dc >= D_SEAT && dc < D_SEAT + 4) {
                    return h <= 2 ? Part.POST : h == 3 ? Part.values()[Part.STAIR_E.ordinal() + dc - D_SEAT] : Part.AIR;
                }
                if (dc == D_ARCH) {
                    return h <= 5 ? Part.TILE : Part.AIR;
                }
                if (dc == D_LINTEL) {
                    return h == 5 ? Part.TILE : Part.AIR;
                }
            }
            // Halle (6..25)
            if (k == K_PILLAR) {
                return Part.TILE;
            }
            if (k == K_WALL) {
                if (balcony[i] && off >= GALLERY + 1 && off <= GALLERY + 3) {
                    return Part.AIR;
                }
                if (balcony[i] && off == GALLERY) {
                    return Part.TILE;
                }
                return off <= archTop[i] ? Part.AIR : Part.TILE;
            }
            int c = ceil[i];
            if (off >= c) {
                return Part.TILE;
            }
            int sk = stairK[i];
            if (sk > 0) {
                int top = FLOOR + sk;
                if (off < top) {
                    return Part.TILE;
                }
                if (off == top) {
                    if (sk >= RUN) {
                        return Part.TILE;
                    }
                    return Part.values()[Part.STAIR_E.ordinal() + stairDir[i]];
                }
            }
            if (balcony[i]) {
                if (off == GALLERY) {
                    return Part.TILE;
                }
                if (off == GALLERY + 1 && railing[i]) {
                    return Part.RAILING;
                }
            }
            if (lights[i]) {
                if (off == c - 1) {
                    return Part.LIGHT_PANEL;
                }
                if (c == CEIL && (off == FLOOR + 3 || off == FLOOR + 9 || off == FLOOR + 17 || off == FLOOR + 25 || off == FLOOR + 31) && !balcony[i]
                        && slideRole[i] == 0) {
                    return Part.LIGHT_INVISIBLE;
                }
            }
            return Part.AIR;
        }
    }

    private final OfficePlanner p;
    private final PlanCache<Optional<Site>> cache = new PlanCache<>(32);

    public Poolrooms(OfficePlanner p) {
        this.p = p;
    }

    /** Fundstelle der Region oder null. */
    public Site site(int rx, int rz) {
        // Schneller Pfad ohne Cache-Sperre: fast alle Regionen haben keine Poolrooms
        if (p.regionBlocks != W || p.phys != 6
                || Hashing.unit(Hashing.hash(p.seed, rx, rz, 0x9001, 0)) >= p.cfg.special().poolroomsRegionChance()) {
            return null;
        }
        long key = ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
        return cache.get(key, () -> Optional.ofNullable(build(rx, rz))).orElse(null);
    }

    /** Ob ein Raum der Region (ganz oder teilweise) im Poolrooms-Band liegt. */
    public boolean overlaps(int rx, int rz, Room r) {
        if (r == null) {
            return false;
        }
        Site s = site(rx, rz);
        if (s == null) {
            return false;
        }
        int b = r.kind == Room.Kind.ATRIUM && r.atrium != null ? r.atrium.s0 : r.bottom;
        int t = r.kind == Room.Kind.ATRIUM && r.atrium != null ? r.atrium.top() : r.top();
        return t >= s.s0 && b <= s.s0 + STOREYS - 1;
    }

    /** Ob die Weltposition im Band einer Fundstelle liegt (und nicht auf deren Bürowandlinie). */
    public boolean contains(int x, int y, int z) {
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        Site s = site(rx, rz);
        if (s == null || !s.inBand(y)) {
            return false;
        }
        int lx = x - rx * p.regionBlocks;
        int lz = z - rz * p.regionBlocks;
        return lx != 0 && lz != 0;
    }

    private Site build(int rx, int rz) {
        if (p.regionBlocks != W || p.phys != 6) {
            return null;
        }
        double chance = p.cfg.special().poolroomsRegionChance();
        if (Hashing.unit(Hashing.hash(p.seed, rx, rz, 0x9001, 0)) >= chance) {
            return null;
        }
        int lo = Math.max(p.cfg.special().darkBottomStoreys() + 4, 8);
        int hi = p.storeys - STOREYS - 8;
        if (hi <= lo) {
            return null;
        }
        Hashing.Rng rng = new Hashing.Rng(Hashing.hash(p.seed, rx, rz, 0x9002, 0));
        int s0 = rng.range(lo, hi);
        Site s = new Site(rx, rz, s0, p.floorY(s0) + 1);
        s.modern = p.cfg.modern();
        new Builder(s, rng).run();
        return s;
    }

    // ---------------------------------------------------------------------------------------------

    private static final class Builder {
        final Site s;
        final Hashing.Rng rng;
        final List<int[]> leaves = new ArrayList<>();
        final List<int[]> corridors = new ArrayList<>();
        final List<int[]> archList = new ArrayList<>();

        Builder(Site s, Hashing.Rng rng) {
            this.s = s;
            this.rng = rng;
        }

        void run() {
            for (int lz = 0; lz < W; lz++) {
                for (int lx = 0; lx < W; lx++) {
                    int i = Site.i(lx, lz);
                    if (lx == 0 || lz == 0) {
                        s.kind[i] = K_OFFICE;
                    } else if (lx <= 3 || lz <= 3 || lx >= W - 3 || lz >= W - 3) {
                        s.kind[i] = K_RING;
                    } else {
                        s.kind[i] = K_OPEN;
                        s.ceil[i] = CEIL;
                    }
                }
            }
            // Galerie rundum (3 breit) mit Brüstung an der Innenkante
            for (int lz = 4; lz < W - 3; lz++) {
                for (int lx = 4; lx < W - 3; lx++) {
                    int dEdge = Math.min(Math.min(lx - 4, lz - 4), Math.min(W - 4 - lx, W - 4 - lz));
                    if (dEdge <= 2) {
                        s.balcony[Site.i(lx, lz)] = true;
                        s.railing[Site.i(lx, lz)] = dEdge == 2;
                    }
                }
            }
            split(4, 4, W - 3, W - 3, 0);
            int[] mazeLeaf = pickMazeLeaf();
            for (int[] c : corridors) {
                corridor(c);
            }
            for (int[] l : leaves) {
                if (l == mazeLeaf) {
                    continue;
                }
                hall(l);
            }
            stairs();
            if (mazeLeaf != null) {
                maze(mazeLeaf);
            }
            lightsGrid();
            if (s.modern) {
                modernize(mazeLeaf);
            }
            // Mitte der größten Halle als Ankunftspunkt für /officerooms goto poolrooms
            int[] best = null;
            for (int[] l : leaves) {
                if (l != mazeLeaf && (best == null || area(l) > area(best))) {
                    best = l;
                }
            }
            if (best != null) {
                s.hallX = (best[0] + best[2]) / 2;
                s.hallZ = (best[1] + best[3]) / 2;
                // Ankunftsspalte trocken halten
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int i = Site.i(s.hallX + dx, s.hallZ + dz);
                        s.pool[i] = 0;
                        if (s.kind[i] == K_PILLAR) {
                            s.kind[i] = K_OPEN;
                        }
                    }
                }
            }
        }

        static int area(int[] r) {
            return (r[2] - r[0]) * (r[3] - r[1]);
        }

        /** BSP-Teilung von [x0,x1) × [z0,z1); Trennwände liegen auf einer Linie. */
        void split(int x0, int z0, int x1, int z1, int depth) {
            int w = x1 - x0;
            int d = z1 - z0;
            boolean canX = w >= 56;                                          // 0.35.0: fewer, much bigger halls
            boolean canZ = d >= 56;
            boolean force = Math.max(w, d) > 110;
            if ((!canX && !canZ) || (!force && depth >= 2 && rng.chance(0.35))) {
                leaves.add(new int[]{x0, z0, x1, z1});
                return;
            }
            boolean alongX = canX && (!canZ || (w >= d ? rng.chance(0.7) : rng.chance(0.3)));
            int len = alongX ? w : d;
            // Wandlinie auf einer 4er-Rasterlinie, mindestens 16 vom Rand
            int at = 16 + 4 * rng.nextInt(Math.max(1, (len - 32) / 4 + 1));
            boolean corridor = len >= 56 && rng.chance(0.35);
            if (alongX) {
                int sx = x0 + at;
                if (corridor) {
                    wallLine(true, sx, z0, z1);
                    wallLine(true, sx + 8, z0, z1);
                    corridors.add(new int[]{sx + 1, z0, sx + 8, z1});
                    split(x0, z0, sx, z1, depth + 1);
                    split(sx + 9, z0, x1, z1, depth + 1);
                } else {
                    wallLine(true, sx, z0, z1);
                    split(x0, z0, sx, z1, depth + 1);
                    split(sx + 1, z0, x1, z1, depth + 1);
                }
            } else {
                int sz = z0 + at;
                if (corridor) {
                    wallLine(false, sz, x0, x1);
                    wallLine(false, sz + 8, x0, x1);
                    corridors.add(new int[]{x0, sz + 1, x1, sz + 8});
                    split(x0, z0, x1, sz, depth + 1);
                    split(x0, sz + 9, x1, z1, depth + 1);
                } else {
                    wallLine(false, sz, x0, x1);
                    split(x0, z0, x1, sz, depth + 1);
                    split(x0, sz + 1, x1, z1, depth + 1);
                }
            }
        }

        /** Trennwand mit Bögen (mindestens einer). vertical = Linie x = c (läuft in z). */
        void wallLine(boolean vertical, int c, int a0, int a1) {
            int len = a1 - a0;
            List<int[]> arches = new ArrayList<>();
            int pos = a0 + 3 + rng.nextInt(6);
            while (pos < a1 - 4) {
                int width = rng.range(3, 7);
                int top = FLOOR + rng.range(4, 11);
                arches.add(new int[]{pos, Math.min(a1 - 2, pos + width), top});
                pos += width + rng.range(6, 16);
            }
            if (arches.isEmpty()) {
                int mid = a0 + len / 2;
                arches.add(new int[]{mid - 2, mid + 2, FLOOR + 5});
            }
            for (int a = a0; a < a1; a++) {
                int lx = vertical ? c : a;
                int lz = vertical ? a : c;
                int i = Site.i(lx, lz);
                if (s.kind[i] == K_RING || s.kind[i] == K_OFFICE) {
                    continue;
                }
                s.kind[i] = K_WALL;
                s.archTop[i] = 0;
                for (int[] arch : arches) {
                    if (a >= arch[0] && a < arch[1]) {
                        int top = arch[2];
                        if (s.modern) {                                       // 0.35.0: round arches
                            double r = (arch[1] - arch[0]) / 2.0;
                            double x = a + 0.5 - (arch[0] + r);
                            top = (int) Math.round(arch[2] - r + Math.sqrt(Math.max(0, r * r - x * x)));
                        }
                        s.archTop[i] = (byte) top;
                    }
                }
            }
            for (int[] arch : arches) {
                archList.add(new int[]{vertical ? 1 : 0, c, arch[0], arch[1]});
            }
        }

        int[] pickMazeLeaf() {
            int[] best = null;
            for (int[] l : leaves) {
                int w = l[2] - l[0];
                int d = l[3] - l[1];
                if (w < 28 || d < 28) {
                    continue;
                }
                // die größte geeignete Halle: das Labyrinth soll eine echte Herausforderung sein (begrenzt auf 64 × 64)
                if ((w <= 68 && d <= 68) && (best == null || area(l) > area(best))) {
                    best = l;
                }
            }
            if (best == null) {
                // 0.8.3: rare layouts without a 28–68 hall still get their labyrinth (and vault): the hall
                // closest to 64 × 64 that fits at least a 4 × 4 labyrinth
                for (int[] l : leaves) {
                    int w = l[2] - l[0];
                    int d = l[3] - l[1];
                    if (w < 21 || d < 21) {
                        continue;
                    }
                    if (best == null || Math.abs(area(l) - 64 * 64) < Math.abs(area(best) - 64 * 64)) {
                        best = l;
                    }
                }
            }
            if (best != null) {
                leaves.remove(best);
                leaves.add(best);
            }
            return best;
        }

        void corridor(int[] c) {
            boolean channel = rng.chance(0.5);
            boolean alongZ = (c[3] - c[1]) > (c[2] - c[0]);
            int mid = alongZ ? (c[0] + c[2]) / 2 : (c[1] + c[3]) / 2;
            for (int lz = c[1]; lz < c[3]; lz++) {
                for (int lx = c[0]; lx < c[2]; lx++) {
                    int i = Site.i(lx, lz);
                    if (s.kind[i] != K_OPEN) {
                        continue;
                    }
                    s.ceil[i] = (byte) (s.balcony[i] ? CEIL : LOW_CEIL);
                    if (channel && (alongZ ? lx == mid : lz == mid) && !s.balcony[i]) {
                        s.pool[i] = 1;
                    }
                }
            }
        }

        void hall(int[] l) {
            double r = rng.nextDouble();
            boolean flooded = r < (s.modern ? 0.22 : 0.12);
            boolean pools = r >= (s.modern ? 0.22 : 0.12) && r < (s.modern ? 0.92 : 0.72);
            boolean pillars = r >= 0.55 && r < 0.9 || flooded && rng.chance(0.5);
            if (flooded) {
                for (int lz = l[1] + 1; lz < l[3] - 1; lz++) {
                    for (int lx = l[0] + 1; lx < l[2] - 1; lx++) {
                        s.pool[Site.i(lx, lz)] = 1;
                    }
                }
            }
            if (pools) {
                int n = 1 + rng.nextInt(3);
                // 0.35.0 (new worlds): pools sized to the (now huge) halls
                int maxW = s.modern ? Math.max(22, (l[2] - l[0]) * 3 / 5) : 22;
                int maxD = s.modern ? Math.max(22, (l[3] - l[1]) * 3 / 5) : 22;
                for (int k = 0; k < n * 4 && n > 0; k++) {
                    int w = rng.range(s.modern ? 8 : 3, Math.max(s.modern ? 9 : 3, Math.min(maxW, l[2] - l[0] - 6)));
                    int d = rng.range(s.modern ? 8 : 3, Math.max(s.modern ? 9 : 3, Math.min(maxD, l[3] - l[1] - 6)));
                    int x = rng.range(l[0] + 3, l[2] - 3 - w);
                    int z = rng.range(l[1] + 3, l[3] - 3 - d);
                    if (x < l[0] + 3 || z < l[1] + 3) {
                        continue;
                    }
                    int depth = rng.range(1, 3);
                    if (x + w > l[2] - 3 || z + d > l[3] - 3) {
                        continue;                     // 0.38.1: too big for a narrow hall (ran past the region before)
                    }
                    for (int lz = z; lz < z + d; lz++) {
                        for (int lx = x; lx < x + w; lx++) {
                            int i = Site.i(lx, lz);
                            s.pool[i] = (byte) Math.max(s.pool[i], depth);
                            s.lantern[i] = depth >= 2 && Math.floorMod(lx * 7 + lz * 13, 9) == 0;
                        }
                    }
                    n--;
                }
            }
            if (pillars) {
                int ox = Math.floorMod(rng.nextInt(8), 8);
                for (int lz = l[1] + 4; lz < l[3] - 4; lz++) {
                    for (int lx = l[0] + 4; lx < l[2] - 4; lx++) {
                        if (Math.floorMod(lx - l[0] - ox, 8) < 2 && Math.floorMod(lz - l[1] - ox, 8) < 2) {
                            int i = Site.i(lx, lz);
                            if (!s.balcony[i]) {
                                s.kind[i] = K_PILLAR;
                                s.pool[i] = 0;
                            }
                        }
                    }
                }
            }
            // Bademeister-Ecke: Truhe und manchmal ein Lesepult mit Logbuch
            if (rng.chance(0.35)) {
                int cx = l[0] + 4;
                int cz = l[1] + 4;
                if (dry(cx, cz) && dry(cx + 1, cz)) {
                    s.chests.add(new Chest(cx, FLOOR + 1, cz, "poolrooms", 3));
                    if (rng.chance(0.6)) {
                        s.lecterns.add(new Lectern(cx + 1, cz, rng.nextInt(16), 1));
                    }
                }
            }
        }

        /**
         * 0.28.1 (new worlds only, own random stream so old layouts stay identical): deeper water in the middle of
         * pools with a shallow step at the edge, drained pools, children's pools, tiled benches along the walls,
         * lifeguard chairs, freestanding arches leading nowhere and more underwater light.
         */
        void modernize(int[] mazeLeaf) {
            Hashing.Rng m = new Hashing.Rng(Hashing.hash(s.rx * 31L + s.yBase, s.rx, s.rz, 0x9F01, 0));
            for (int[] l : leaves) {
                if (l == mazeLeaf) {
                    continue;
                }
                // children's pools: small, shallow, square
                int kids = m.chance(0.45) ? 1 + m.nextInt(2) : 0;
                for (int k = 0; k < kids; k++) {
                    int w = m.range(3, 6);
                    int x = m.range(l[0] + 4, Math.max(l[0] + 5, l[2] - 4 - w));
                    int z = m.range(l[1] + 4, Math.max(l[1] + 5, l[3] - 4 - w));
                    boolean free = true;
                    for (int lz = z - 1; lz <= z + w && free; lz++) {
                        for (int lx = x - 1; lx <= x + w && free; lx++) {
                            free = dry(lx, lz);
                        }
                    }
                    if (free) {
                        for (int lz = z; lz < z + w; lz++) {
                            for (int lx = x; lx < x + w; lx++) {
                                s.pool[Site.i(lx, lz)] = 1;
                            }
                        }
                    }
                }
                // a drained pool now and then (whole hall's pools)
                boolean drain = m.chance(0.18);
                for (int lz = l[1]; lz < l[3]; lz++) {
                    for (int lx = l[0]; lx < l[2]; lx++) {
                        int i = Site.i(lx, lz);
                        if (s.pool[i] > 0 && s.maze[i] == M_NONE && drain && s.kind[i] == K_OPEN) {
                            s.drained[i] = true;
                        }
                    }
                }
                // a lifeguard chair beside the largest water of the hall
                if (m.chance(0.55)) {
                    for (int t = 0; t < 40; t++) {
                        int lx = m.range(l[0] + 3, l[2] - 3);
                        int lz = m.range(l[1] + 3, l[3] - 3);
                        int dir = -1;
                        int[][] dd = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
                        for (int d = 0; d < 4 && dir < 0; d++) {
                            int j = Site.i(lx + dd[d][0] * 2, lz + dd[d][1] * 2);
                            if (s.pool[j] > 0 && !s.drained[j]) {
                                dir = d;
                            }
                        }
                        if (dir >= 0 && dry(lx, lz) && s.deco[Site.i(lx, lz)] == 0) {
                            s.deco[Site.i(lx, lz)] = (byte) (D_SEAT + (dir + 2) % 4);   // backrest away from the water
                            break;
                        }
                    }
                }
                // a freestanding arch leading nowhere
                if (m.chance(0.4)) {
                    for (int t = 0; t < 30; t++) {
                        int lx = m.range(l[0] + 4, l[2] - 8);
                        int lz = m.range(l[1] + 4, l[3] - 4);
                        boolean ok = true;
                        for (int dx = 0; dx <= 3 && ok; dx++) {
                            ok = dry(lx + dx, lz) && s.deco[Site.i(lx + dx, lz)] == 0 && s.ceil[Site.i(lx + dx, lz)] >= FLOOR + 7;
                        }
                        if (ok) {
                            s.deco[Site.i(lx, lz)] = D_ARCH;
                            s.deco[Site.i(lx + 3, lz)] = D_ARCH;
                            s.deco[Site.i(lx + 1, lz)] = D_LINTEL;
                            s.deco[Site.i(lx + 2, lz)] = D_LINTEL;
                            break;
                        }
                    }
                }
            }
            // 0.35.0: water arches: some wall arches stand in water, a channel runs through them
            for (int[] ar : archList) {
                if (!m.chance(0.35)) {
                    continue;
                }
                boolean vertical = ar[0] == 1;
                for (int a = ar[2]; a < ar[3]; a++) {
                    for (int d = -6; d <= 6; d++) {
                        int lx = vertical ? ar[1] + d : a;
                        int lz = vertical ? a : ar[1] + d;
                        if (lx < 5 || lz < 5 || lx >= W - 5 || lz >= W - 5) {
                            continue;
                        }
                        int i = Site.i(lx, lz);
                        if ((s.kind[i] == K_OPEN || (d == 0 && s.kind[i] == K_WALL)) && s.maze[i] == M_NONE && s.stairK[i] == 0 && !s.balcony[i]) {
                            s.pool[i] = (byte) Math.max(s.pool[i], 2);
                            s.deco[i] = 0;
                        }
                    }
                }
            }
            // depth: a shallow step at every edge, then deeper and deeper toward the middle; every hall has its own
            // maximum (diving halls reach ten blocks)
            byte[] maxD = new byte[W * W];
            for (int[] l : leaves) {
                byte md = (byte) (m.chance(0.5) ? m.range(6, FLOOR) : m.range(3, 5));
                for (int lz = l[1]; lz < l[3]; lz++) {
                    for (int lx = l[0]; lx < l[2]; lx++) {
                        maxD[Site.i(lx, lz)] = md;
                    }
                }
            }
            int[] dist = new int[W * W];
            ArrayDeque<Integer> q = new ArrayDeque<>();
            for (int i = 0; i < W * W; i++) {
                if (s.pool[i] == 0 || s.maze[i] != M_NONE) {
                    dist[i] = 0;
                    q.add(i);
                } else {
                    dist[i] = Integer.MAX_VALUE;
                }
            }
            while (!q.isEmpty()) {
                int i = q.poll();
                int lx = i % W;
                int lz = i / W;
                int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
                for (int[] dd : nb) {
                    int x = lx + dd[0];
                    int z = lz + dd[1];
                    if (x < 0 || z < 0 || x >= W || z >= W) {
                        continue;
                    }
                    int j = Site.i(x, z);
                    if (dist[j] > dist[i] + 1) {
                        dist[j] = dist[i] + 1;
                        q.add(j);
                    }
                }
            }
            byte[] nd = s.pool.clone();
            for (int lz = 4; lz < W - 3; lz++) {
                for (int lx = 4; lx < W - 3; lx++) {
                    int i = Site.i(lx, lz);
                    if (s.pool[i] == 0 || s.maze[i] != M_NONE) {
                        continue;
                    }
                    int md = maxD[i] == 0 ? 4 : maxD[i];
                    int depth = Math.min(md, dist[i] <= 1 ? 1 : 1 + (dist[i] - 1) * 2);
                    if (s.pool[i] == 1 && !s.drained[i]) {
                        depth = Math.min(depth, 2);                           // channels and children's pools stay shallow
                    }
                    nd[i] = (byte) Math.max(1, Math.min(FLOOR - 1, depth));
                    s.lantern[i] = depth >= 3 && Math.floorMod(lx * 5 + lz * 3, 7) == 0;
                }
            }
            System.arraycopy(nd, 0, s.pool, 0, nd.length);
            // arcades standing in the deep water: pillars from the bottom, a lintel over every gap
            for (int[] l : leaves) {
                if (l == mazeLeaf || !m.chance(0.6)) {
                    continue;
                }
                int bestZ = -1;
                int bestRun = 0;
                int bestX = 0;
                for (int lz = l[1] + 2; lz < l[3] - 2; lz++) {
                    int run = 0;
                    for (int lx = l[0] + 1; lx < l[2] - 1; lx++) {
                        run = s.pool[Site.i(lx, lz)] >= 3 && s.kind[Site.i(lx, lz)] == K_OPEN ? run + 1 : 0;
                        if (run > bestRun) {
                            bestRun = run;
                            bestZ = lz;
                            bestX = lx - run + 1;
                        }
                    }
                }
                if (bestRun >= 9) {
                    for (int k = 1; k < bestRun - 1; k++) {
                        int i = Site.i(bestX + k, bestZ);
                        if ((k - 1) % 4 == 0) {
                            s.pool[i] = 0;                                    // the pillar stands on the pool floor
                            s.kind[i] = K_PILLAR;
                        } else {
                            s.deco[i] = D_LINTEL;
                        }
                    }
                }
            }
            // pool ladders where dry floor meets deeper water
            for (int lz = 5; lz < W - 5; lz++) {
                for (int lx = 5; lx < W - 5; lx++) {
                    int i = Site.i(lx, lz);
                    if (!dry(lx, lz) || s.deco[i] != 0 || Math.floorMod(lx * 11 + lz * 7, 23) != 0) {
                        continue;
                    }
                    int[][] dd = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
                    for (int d = 0; d < 4; d++) {
                        int j = Site.i(lx + dd[d][0], lz + dd[d][1]);
                        int j2 = Site.i(lx + dd[d][0] * 2, lz + dd[d][1] * 2);
                        if (s.pool[j] >= 1 && s.pool[j2] >= 3 && !s.drained[j]) {
                            s.deco[i] = (byte) (D_LADDER + d);
                            break;
                        }
                    }
                }
            }
            // water slides: an enclosed tube from the gallery down into a deep landing pool
            this.m = m;
            int slides = 1 + m.nextInt(3);
            for (int n = 0; n < slides; n++) {
                int tubes = 1 + m.nextInt(3);
                for (int t = 0; t < 20; t++) {
                    if (trySlide(m.nextInt(4), m.range(12, W - 20), tubes)) {
                        break;
                    }
                }
            }
            // stainless handrails, the classic Poolrooms kind: along pool edges, and some just standing on the tiles
            for (int lz = 5; lz < W - 5; lz++) {
                for (int lx = 5; lx < W - 5; lx++) {
                    int i = Site.i(lx, lz);
                    if (!dry(lx, lz) || s.deco[i] != 0) {
                        continue;
                    }
                    boolean poolEW = s.pool[Site.i(lx + 1, lz)] > 0 || s.pool[Site.i(lx - 1, lz)] > 0;
                    boolean poolNS = s.pool[Site.i(lx, lz + 1)] > 0 || s.pool[Site.i(lx, lz - 1)] > 0;
                    if (poolEW == poolNS) {
                        continue;
                    }
                    int along = poolEW ? lz : lx;
                    int perp = poolEW ? lx : lz;
                    if (Hashing.unit(Hashing.hash(s.rx, s.rz, along / 5, perp, 0x9F21)) < 0.22) {
                        s.deco[i] = (byte) (poolEW ? D_RAIL + 1 : D_RAIL);      // runs along z: facing east; along x: facing north
                    }
                }
            }
            for (int[] l : leaves) {
                if (l == mazeLeaf || !m.chance(0.5)) {
                    continue;
                }
                for (int t = 0; t < 30; t++) {
                    boolean alongX = m.chance(0.5);
                    int len = m.range(3, 8);
                    int lx = m.range(l[0] + 3, l[2] - 3 - (alongX ? len : 0));
                    int lz = m.range(l[1] + 3, l[3] - 3 - (alongX ? 0 : len));
                    boolean ok = true;
                    for (int k = 0; k < len && ok; k++) {
                        int x = alongX ? lx + k : lx;
                        int z = alongX ? lz : lz + k;
                        ok = dry(x, z) && s.deco[Site.i(x, z)] == 0;
                    }
                    if (ok) {
                        for (int k = 0; k < len; k++) {
                            s.deco[Site.i(alongX ? lx + k : lx, alongX ? lz : lz + k)] = (byte) (alongX ? D_RAIL : D_RAIL + 1);
                        }
                        break;
                    }
                }
            }
            // tiled benches along the hall walls (dry floor next to a wall, every few blocks)
            for (int lz = 5; lz < W - 5; lz++) {
                for (int lx = 5; lx < W - 5; lx++) {
                    int i = Site.i(lx, lz);
                    if (!dry(lx, lz) || s.deco[i] != 0) {
                        continue;
                    }
                    boolean byWall = s.kind[Site.i(lx + 1, lz)] == K_WALL || s.kind[Site.i(lx - 1, lz)] == K_WALL
                            || s.kind[Site.i(lx, lz + 1)] == K_WALL || s.kind[Site.i(lx, lz - 1)] == K_WALL;
                    if (byWall && Math.floorMod(lx + lz * 3, 9) < 3 && s.archTop[i] == 0) {
                        s.deco[i] = D_BENCH;
                    }
                }
            }
        }

        boolean dry(int lx, int lz) {
            int i = Site.i(lx, lz);
            return s.kind[i] == K_OPEN && s.pool[i] == 0 && s.stairK[i] == 0 && !s.balcony[i] && s.slideRole[i] == 0;
        }

        /** Je Seite eine Treppe zur Galerie (parallel zur Wand, 2 breit, 10 Stufen). */
        void stairs() {
            for (int side = 0; side < 4; side++) {
                for (int attempt = 0; attempt < 12; attempt++) {
                    int start = rng.range(12, W - 16 - RUN);
                    if (tryStair(side, start)) {
                        break;
                    }
                }
            }
        }

        boolean tryStair(int side, int start) {
            // Läufe in +a-Richtung; Spalten b = 7..8 Abstand vom Ring
            List<int[]> cells = new ArrayList<>();
            for (int k = 1; k <= RUN; k++) {
                for (int b = 7; b <= 8; b++) {
                    int a = start + k;
                    int lx;
                    int lz;
                    switch (side) {
                        case 0 -> { lx = b; lz = a; }
                        case 1 -> { lx = a; lz = b; }
                        case 2 -> { lx = W - 1 - b; lz = a; }
                        default -> { lx = a; lz = W - 1 - b; }
                    }
                    int i = Site.i(lx, lz);
                    if (s.kind[i] != K_OPEN || s.ceil[i] != CEIL) {
                        return false;
                    }
                    cells.add(new int[]{lx, lz, k});
                }
            }
            // Aufstiegsrichtung: entlang +a → Ost (Seite 1/3) oder Süd (Seite 0/2)
            int dir = (side == 1 || side == 3) ? 0 : 1;
            for (int[] c : cells) {
                int i = Site.i(c[0], c[1]);
                s.stairK[i] = (byte) c[2];
                s.stairDir[i] = (byte) dir;
                s.pool[i] = 0;
                s.lights[i] = false;
            }
            // Brüstungslücke an der Ankunft
            for (int k = RUN - 2; k <= RUN + 1; k++) {
                int a = start + k;
                int lx;
                int lz;
                switch (side) {
                    case 0 -> { lx = 6; lz = a; }
                    case 1 -> { lx = a; lz = 6; }
                    case 2 -> { lx = W - 7; lz = a; }
                    default -> { lx = a; lz = W - 7; }
                }
                s.railing[Site.i(lx, lz)] = false;
            }
            // Platz neben der Treppe frei von Becken halten
            for (int[] c : cells) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int lx = c[0] + dx;
                        int lz = c[1] + dz;
                        if (lx > 3 && lz > 3 && lx < W - 3 && lz < W - 3) {
                            s.pool[Site.i(lx, lz)] = 0;
                            if (s.kind[Site.i(lx, lz)] == K_PILLAR) {
                                s.kind[Site.i(lx, lz)] = K_OPEN;
                            }
                        }
                    }
                }
            }
            return true;
        }

        /**
         * 0.35.0: one to three tubes side by side that stick out of the outer wall a few blocks up, bend down and end
         * over a deep landing pool (like the classic pictures). The slide openings in the offices lead here.
         */
        boolean trySlide(int side, int a0, int tubes) {
            // 0.35.1: real pipes. 1–3 round tubes side by side come straight out of the outer wall two blocks above
            // the water, the open mouth facing into the hall; a deep pool lies under and in front of them.
            int a1 = a0 + 2 * (tubes - 1);
            for (int a = a0 - 3; a <= a1 + 3; a++) {
                for (int b = 4; b <= 14; b++) {
                    int[] c = at(side, a, b);
                    int i = Site.i(c[0], c[1]);
                    if (s.kind[i] != K_OPEN || s.ceil[i] != CEIL || s.stairK[i] != 0 || s.slideRole[i] != 0 || s.maze[i] != M_NONE) {
                        return false;
                    }
                }
            }
            int dir = switch (side) {                                         // where the mouth points: into the hall
                case 0 -> 0;
                case 1 -> 1;
                case 2 -> 2;
                default -> 3;
            };
            for (int t = 0; t < tubes; t++) {
                int ac = a0 + 2 * t;
                int len = 2 + m.nextInt(2);
                for (int b = 4; b < 4 + len; b++) {
                    int[] c = at(side, ac, b);
                    int i = Site.i(c[0], c[1]);
                    s.slideRole[i] = (byte) (b == 3 + len ? 4 : 3);
                    s.slideH[i] = 2;
                    s.slideDir[i] = (byte) dir;
                    s.deco[i] = 0;
                    s.lights[i] = false;
                }
                int[] end = at(side, ac, 5 + len);
                s.slideEnds.add(new int[]{end[0], end[1]});
            }
            for (int a = a0 - 3; a <= a1 + 3; a++) {                          // the pool under and in front of them
                for (int b = 4; b <= 14; b++) {
                    int[] c = at(side, a, b);
                    int i = Site.i(c[0], c[1]);
                    boolean edge = a == a0 - 3 || a == a1 + 3 || b == 14;
                    s.pool[i] = (byte) Math.max(s.pool[i], edge ? 2 : 5);
                    s.deco[i] = 0;
                }
            }
            return true;
        }

        Hashing.Rng m;

        int[] at(int side, int a, int b) {
            return switch (side) {
                case 0 -> new int[]{b, a};
                case 1 -> new int[]{a, b};
                case 2 -> new int[]{W - 1 - b, a};
                default -> new int[]{a, W - 1 - b};
            };
        }

        void lightsGrid() {
            for (int lz = 4; lz < W - 3; lz++) {
                for (int lx = 4; lx < W - 3; lx++) {
                    int i = Site.i(lx, lz);
                    if (s.kind[i] != K_OPEN || s.stairK[i] != 0) {
                        continue;
                    }
                    // Poolrooms sind hell: dichtes Raster; die Halle über dem Labyrinth bleibt gedämpft
                    int step = inMazeHall(lx, lz) ? 12 : 6;
                    if (Math.floorMod(lx - 4, step) == 3 && Math.floorMod(lz - 4, step) == 3) {
                        s.lights[i] = true;
                    }
                }
            }
        }

        int[] mazeHall;

        boolean inMazeHall(int lx, int lz) {
            return mazeHall != null && lx >= mazeHall[0] && lx < mazeHall[2] && lz >= mazeHall[1] && lz < mazeHall[3];
        }

        /** Unterwasser-Labyrinth unter einer trockenen Halle (Perfektes Labyrinth, Zellen 4 × 4). */
        void maze(int[] l) {
            mazeHall = l;
            int ax0 = l[0] + 2;
            int az0 = l[1] + 2;
            int nx = (l[2] - 2 - ax0 - 1) / 4;
            int nz = (l[3] - 2 - az0 - 1) / 4;
            if (nx < 4 || nz < 4) {
                return;
            }
            boolean[] eastOpen = new boolean[nx * nz];
            boolean[] southOpen = new boolean[nx * nz];
            boolean[] seen = new boolean[nx * nz];
            ArrayDeque<int[]> stack = new ArrayDeque<>();
            stack.push(new int[]{0, 0});
            seen[0] = true;
            int[] dx = {1, 0, -1, 0};
            int[] dz = {0, 1, 0, -1};
            while (!stack.isEmpty()) {
                int[] c = stack.peek();
                int[] order = {0, 1, 2, 3};
                for (int k = 3; k > 0; k--) {
                    int j = rng.nextInt(k + 1);
                    int t = order[k];
                    order[k] = order[j];
                    order[j] = t;
                }
                boolean moved = false;
                for (int o : order) {
                    int x = c[0] + dx[o];
                    int z = c[1] + dz[o];
                    if (x < 0 || z < 0 || x >= nx || z >= nz || seen[z * nx + x]) {
                        continue;
                    }
                    switch (o) {
                        case 0 -> eastOpen[c[1] * nx + c[0]] = true;
                        case 1 -> southOpen[c[1] * nx + c[0]] = true;
                        case 2 -> eastOpen[z * nx + x] = true;
                        default -> southOpen[z * nx + x] = true;
                    }
                    seen[z * nx + x] = true;
                    stack.push(new int[]{x, z});
                    moved = true;
                    break;
                }
                if (!moved) {
                    stack.pop();
                }
            }
            // Weg vom Start (0,0) zur entferntesten Zelle (BFS)
            int[] dist = new int[nx * nz];
            int[] prev = new int[nx * nz];
            java.util.Arrays.fill(dist, -1);
            ArrayDeque<Integer> q = new ArrayDeque<>();
            q.add(0);
            dist[0] = 0;
            int far = 0;
            while (!q.isEmpty()) {
                int c = q.poll();
                int x = c % nx;
                int z = c / nx;
                if (dist[c] > dist[far]) {
                    far = c;
                }
                int[][] nb = {
                        {x + 1, z, x < nx - 1 && eastOpen[c] ? 1 : 0},
                        {x - 1, z, x > 0 && eastOpen[c - 1] ? 1 : 0},
                        {x, z + 1, z < nz - 1 && southOpen[c] ? 1 : 0},
                        {x, z - 1, z > 0 && southOpen[c - nx] ? 1 : 0}};
                for (int[] n : nb) {
                    if (n[2] == 0) {
                        continue;
                    }
                    int ni = n[1] * nx + n[0];
                    if (dist[ni] < 0) {
                        dist[ni] = dist[c] + 1;
                        prev[ni] = c;
                        q.add(ni);
                    }
                }
            }
            List<Integer> path = new ArrayList<>();
            for (int c = far; c != 0; c = prev[c]) {
                path.add(0, c);
            }
            java.util.Set<Integer> pockets = new java.util.HashSet<>();
            for (int k = 6; k < path.size() - 2; k += 7) {
                pockets.add(path.get(k));
            }
            // Spalten setzen
            for (int az = 0; az <= nz * 4; az++) {
                for (int ax = 0; ax <= nx * 4; ax++) {
                    int lx = ax0 + ax;
                    int lz = az0 + az;
                    int i = Site.i(lx, lz);
                    int cx = Math.min(ax / 4, nx - 1);
                    int cz = Math.min(az / 4, nz - 1);
                    int ox = ax % 4;
                    int oz = az % 4;
                    boolean water;
                    if (ax == nx * 4 || az == nz * 4) {
                        water = false;
                    } else if (ox != 0 && oz != 0) {
                        water = true;
                    } else if (ox == 0 && oz != 0) {
                        water = cx > 0 && eastOpen[cz * nx + cx - 1];
                    } else if (oz == 0 && ox != 0) {
                        water = cz > 0 && southOpen[(cz - 1) * nx + cx];
                    } else {
                        water = false;
                    }
                    byte m;
                    if (!water) {
                        m = Math.floorMod(lx * 31 + lz * 17, 23) == 0 ? M_LANTERN : M_WALL;
                    } else if (ox != 0 && oz != 0 && pockets.contains(cz * nx + cx)) {
                        m = M_AIR_POCKET;
                    } else {
                        m = M_WATER;
                    }
                    s.maze[i] = m;
                    s.pool[i] = 0;
                    if (s.kind[i] == K_PILLAR) {
                        s.kind[i] = K_OPEN;
                    }
                }
            }
            // Unauffälliger Einstieg: 1 × 2 Becken über der Startzelle
            int ex = ax0 + 1;
            int ez = az0 + 1;
            s.maze[Site.i(ex, ez)] = M_ENTRANCE;
            s.maze[Site.i(ex + 1, ez)] = M_ENTRANCE;
            s.entranceX = ex;
            s.entranceZ = ez;
            // Schatz am Ende
            int fx = ax0 + (far % nx) * 4;
            int fz = az0 + (far / nx) * 4;
            s.chests.add(new Chest(fx + 1, MAZE0 + 1, fz + 1, "poolrooms_vault_artifact", 1));
            s.chests.add(new Chest(fx + 3, MAZE0 + 1, fz + 1, "poolrooms_vault", 1));
            s.maze[Site.i(fx + 2, fz + 3)] = M_LANTERN;
        }
    }
}
