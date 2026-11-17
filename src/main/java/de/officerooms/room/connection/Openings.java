package de.officerooms.room.connection;

/**
 * Kodierung einer Öffnung auf einer Zellkante (1 Byte pro Kante).
 *
 * <pre>
 * 0x00              keine Öffnung (geschlossene Wand, falls die Räume verschieden sind)
 * 0x40 | w | p      Türöffnung: Breite w (Bits 3–4: w-1), Startposition p (Bits 0–2: p-1)
 * 0xC0              vollständig offener Durchgang (ganze Kantenbreite, ganze Geschosshöhe)
 * 0x80 | h | w | p  Fenster: Höhe h (Bit 5: h-2), Breite w, Startposition p
 * </pre>
 *
 * <p>Positionen zählen entlang der Kante von 1 bis unit-1 (Position 0 und unit sind Wandpfosten).</p>
 */
public final class Openings {

    public static final int NONE = 0;
    public static final int TYPE_MASK = 0xC0;
    public static final int DOOR = 0x40;
    public static final int WINDOW = 0x80;
    public static final int OPEN = 0xC0;

    private Openings() {
    }

    public static byte door(int width, int start) {
        return (byte) (DOOR | ((width - 1) << 3) | (start - 1));
    }

    public static byte window(int width, int height, int start) {
        return (byte) (WINDOW | ((height - 2) << 5) | ((width - 1) << 3) | (start - 1));
    }

    public static int type(int code) {
        return code & TYPE_MASK;
    }

    public static int width(int code) {
        return ((code >> 3) & 3) + 1;
    }

    public static int start(int code) {
        return (code & 7) + 1;
    }

    public static int windowHeight(int code) {
        return ((code >> 5) & 1) + 2;
    }

    public static boolean covers(int code, int pos) {
        int st = start(code);
        return pos >= st && pos < st + width(code);
    }
}
