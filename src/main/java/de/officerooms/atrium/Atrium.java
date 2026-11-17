package de.officerooms.atrium;

import de.officerooms.room.Room;

/**
 * Ein geplantes Atrium: rechteckiger, mehrgeschossiger Innenraum mit optionalen Galerien und Brücken.
 *
 * <p>Atrien werden pro Region <b>vor</b> allen anderen Räumen vollständig geplant (Grundfläche,
 * unterstes Geschoss, Höhe, Galerieebenen). Erst danach entstehen die Geschossgrundrisse um sie
 * herum. Deshalb weiß jede Zelle schon bei der Planung, ob hinter ihrer Wand ein Atrium liegt –
 * die Voraussetzung dafür, dass Fenster nie ins Leere zeigen.</p>
 *
 * <p>Atrien halten immer mindestens eine Einheit Abstand zum Regionsrand und ein Geschoss Abstand
 * zur Welt-Ober- und Unterkante. Sie sind dadurch vollständig von Architektur umschlossen.</p>
 */
public final class Atrium {

    public final int index;
    public final String cls;
    /** Grundfläche in Einheiten (regionslokal). */
    public final int ux;
    public final int uz;
    public final int w;
    public final int d;
    /** Unterstes Geschoss und Anzahl Geschosse. */
    public final int s0;
    public final int storeys;
    /** Lichte Höhe in Blöcken. */
    public final int clear;
    /** Pro Geschoss (Index s - s0): Galerie-/Brückenmaske über w*d Zellen oder null. */
    public final byte[][] walk;
    /** Knoten-ID im Verbindungsgraphen pro Geschoss (Boden und Galerien), sonst -1. */
    public final int[] levelNode;
    public Room room;

    public Atrium(int index, String cls, int ux, int uz, int w, int d, int s0, int storeys, int clear) {
        this.index = index;
        this.cls = cls;
        this.ux = ux;
        this.uz = uz;
        this.w = w;
        this.d = d;
        this.s0 = s0;
        this.storeys = storeys;
        this.clear = clear;
        this.walk = new byte[storeys][];
        this.levelNode = new int[storeys];
        java.util.Arrays.fill(levelNode, -1);
    }

    public int top() {
        return s0 + storeys - 1;
    }

    public boolean containsCell(int cx, int cz) {
        return cx >= ux && cx < ux + w && cz >= uz && cz < uz + d;
    }

    /** True, wenn die Zelle in diesem Geschoss eine Galerie- oder Brückenfläche ist. */
    public boolean isWalk(int s, int cx, int cz) {
        int o = s - s0;
        if (o < 0 || o >= storeys || walk[o] == null) {
            return false;
        }
        return walk[o][(cz - uz) * w + (cx - ux)] != 0;
    }

    public boolean hasGallery(int s) {
        int o = s - s0;
        return o > 0 && o < storeys && walk[o] != null;
    }

    public int galleryCount() {
        int n = 0;
        for (byte[] b : walk) {
            if (b != null) {
                n++;
            }
        }
        return n;
    }

    @Override
    public String toString() {
        return "Atrium " + cls + " " + w + "x" + d + " units, storey " + s0 + "+" + storeys + ", height " + clear;
    }
}
