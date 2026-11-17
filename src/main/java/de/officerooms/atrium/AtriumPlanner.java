package de.officerooms.atrium;

import de.officerooms.core.Hashing.Rng;
import de.officerooms.room.GenSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Plant alle Atrien einer Region, bevor irgendein anderer Raum entsteht.
 *
 * <p>Regeln:</p>
 * <ul>
 *     <li>Grundfläche mindestens 1 Einheit vom Regionsrand entfernt (keine Atrien über Regionsgrenzen).</li>
 *     <li>Unterstes Geschoss ≥ 1, oberstes Geschoss ≤ storeys − 2: über und unter jedem Atrium liegt immer
 *     ein Geschoss normaler Architektur, das Atrium ist also nach oben und unten geschlossen.</li>
 *     <li>Zwei Atrien halten horizontal mindestens 1 Einheit und vertikal 1 Geschoss Abstand,
 *     damit zwischen ihnen immer Räume liegen, die Fenster in beide Richtungen haben können.</li>
 *     <li>Die lichte Höhe wird nach oben auf die Geschosszahl gerundet, die Restblöcke werden als
 *     abgehängte Decke gefüllt.</li>
 * </ul>
 */
public final class AtriumPlanner {

    private AtriumPlanner() {
    }

    public static List<Atrium> plan(GenSettings cfg, Rng rng, int storeys) {
        int ru = cfg.regionUnits();
        int pitch = cfg.pitch();
        GenSettings.Atria a = cfg.atria();
        List<Atrium> out = new ArrayList<>();
        if (a.classes().isEmpty() || storeys < 5) {
            return out;
        }
        int totalWeight = 0;
        for (GenSettings.AtriumClass c : a.classes()) {
            totalWeight += Math.max(0, c.weight());
        }
        if (totalWeight <= 0) {
            return out;
        }
        int attempts = (int) Math.round(storeys * a.density());
        for (int i = 0; i < attempts; i++) {
            GenSettings.AtriumClass c = pick(a.classes(), totalWeight, rng);
            int w = rng.range(c.minW(), c.maxW());
            int d = rng.range(c.minD(), c.maxD());
            if (rng.chance(0.5)) {
                int t = w;
                w = d;
                d = t;
            }
            w = Math.max(2, Math.min(w, ru - 2));
            d = Math.max(2, Math.min(d, ru - 2));
            int h = rng.range(Math.max(c.minH(), cfg.baseHeight() + 1), Math.max(c.maxH(), c.minH()));
            int k = (h + 1 + pitch - 1) / pitch;
            int maxK = storeys - 3;
            if (k > maxK) {
                k = maxK;
                h = Math.min(h, k * pitch - 1);
            }
            int s0 = rng.range(1, storeys - 2 - k);
            int ux = rng.range(1, ru - 1 - w);
            int uz = rng.range(1, ru - 1 - d);
            if (overlaps(out, ux, uz, w, d, s0, k)) {
                continue;
            }
            Atrium at = new Atrium(out.size(), c.name(), ux, uz, w, d, s0, k, h);
            planGalleries(at, a, rng, pitch, cfg.baseHeight());
            out.add(at);
        }
        return out;
    }

    private static GenSettings.AtriumClass pick(List<GenSettings.AtriumClass> list, int total, Rng rng) {
        int r = rng.nextInt(total);
        for (GenSettings.AtriumClass c : list) {
            r -= Math.max(0, c.weight());
            if (r < 0) {
                return c;
            }
        }
        return list.get(list.size() - 1);
    }

    private static boolean overlaps(List<Atrium> list, int ux, int uz, int w, int d, int s0, int k) {
        for (Atrium o : list) {
            boolean hx = ux < o.ux + o.w + 1 && o.ux < ux + w + 1;
            boolean hz = uz < o.uz + o.d + 1 && o.uz < uz + d + 1;
            boolean v = s0 < o.s0 + o.storeys + 1 && o.s0 < s0 + k + 1;
            if (hx && hz && v) {
                return true;
            }
        }
        return false;
    }

    /**
     * Galerien: umlaufender Gang (1 Einheit breit) oder zwei gegenüberliegende Gänge mit Brücke,
     * auf einzelnen Zwischengeschossen. Nur bei Atrien, deren Grundfläche ein offenes Inneres lässt.
     */
    private static void planGalleries(Atrium at, GenSettings.Atria a, Rng rng, int pitch, int baseHeight) {
        if (Math.min(at.w, at.d) < 5 || at.storeys < 3) {
            return;
        }
        int step = rng.range(2, 4);
        for (int o = step; o < at.storeys; o += step) {
            // Über dem Galerieboden muss noch ein volles Standardgeschoss frei sein.
            if (o * pitch + baseHeight > at.clear) {
                break;
            }
            if (!rng.chance(a.galleryChance())) {
                continue;
            }
            byte[] m = new byte[at.w * at.d];
            double roll = rng.nextDouble();
            boolean bridge = rng.chance(a.bridgeChance());
            if (roll < 0.7 || !bridge) {
                // umlaufende Galerie
                for (int z = 0; z < at.d; z++) {
                    for (int x = 0; x < at.w; x++) {
                        if (x == 0 || z == 0 || x == at.w - 1 || z == at.d - 1) {
                            m[z * at.w + x] = 1;
                        }
                    }
                }
                if (bridge) {
                    addBridge(at, m, rng);
                }
            } else {
                // zwei gegenüberliegende Galerien + verbindende Brücke
                boolean alongX = rng.chance(0.5);
                for (int z = 0; z < at.d; z++) {
                    for (int x = 0; x < at.w; x++) {
                        boolean side = alongX ? (z == 0 || z == at.d - 1) : (x == 0 || x == at.w - 1);
                        if (side) {
                            m[z * at.w + x] = 1;
                        }
                    }
                }
                if (alongX) {
                    int x = rng.range(1, at.w - 2);
                    for (int z = 0; z < at.d; z++) {
                        m[z * at.w + x] = 1;
                    }
                } else {
                    int z = rng.range(1, at.d - 2);
                    for (int x = 0; x < at.w; x++) {
                        m[z * at.w + x] = 1;
                    }
                }
            }
            at.walk[o] = m;
        }
    }

    private static void addBridge(Atrium at, byte[] m, Rng rng) {
        if (rng.chance(0.5)) {
            int x = rng.range(2, at.w - 3);
            for (int z = 0; z < at.d; z++) {
                m[z * at.w + x] = 1;
            }
        } else {
            int z = rng.range(2, at.d - 3);
            for (int x = 0; x < at.w; x++) {
                m[z * at.w + x] = 1;
            }
        }
    }
}
