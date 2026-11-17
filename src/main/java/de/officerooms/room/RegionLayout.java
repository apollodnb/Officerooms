package de.officerooms.room;

import de.officerooms.atrium.Atrium;
import de.officerooms.room.connection.Connection;

import java.util.List;

/**
 * Vollständige Raumplanung einer Region (alle Geschosse), ohne die Öffnungen an den Regionsgrenzen.
 *
 * <p>Eine Region ist eine senkrechte Säule von {@code regionUnits × regionUnits} Einheiten
 * (Standard 160 × 160 Blöcke) über die gesamte Weltgeschosshöhe. Sie wird ausschließlich aus
 * {@code hash(seed, rx, rz)} berechnet und hängt von keiner anderen Region ab.</p>
 *
 * <p>Zellwert {@code owner}: 0 = massiv, sonst Raumindex + 1.</p>
 */
public final class RegionLayout {

    public final int rx;
    public final int rz;
    public final int storeys;
    public final int ru;
    final short[] owner;
    public final List<Room> rooms;
    public final List<Atrium> atria;
    /** Interne Verbindungen (Spannbaum + Zusatzkanten), nur zwischen erreichbaren Räumen. */
    public final List<Connection> connections;
    /** Knoten → Raumindex (für Atrium-Ebenen der Index des Atrium-Raums). */
    public final int[] nodeRoom;

    RegionLayout(int rx, int rz, int storeys, int ru, short[] owner, List<Room> rooms, List<Atrium> atria,
                 List<Connection> connections, int[] nodeRoom) {
        this.rx = rx;
        this.rz = rz;
        this.storeys = storeys;
        this.ru = ru;
        this.owner = owner;
        this.rooms = rooms;
        this.atria = atria;
        this.connections = connections;
        this.nodeRoom = nodeRoom;
    }

    /** Rohwert der Zelle: 0 = massiv, sonst Raumindex + 1. Außerhalb des Geschossbereichs 0. */
    public int ownerAt(int s, int ux, int uz) {
        if (s < 0 || s >= storeys) {
            return 0;
        }
        return owner[(s * ru + uz) * ru + ux];
    }

    /** Raum der Zelle oder null (massiv oder versiegelt). */
    public Room room(int s, int ux, int uz) {
        int o = ownerAt(s, ux, uz);
        if (o == 0) {
            return null;
        }
        Room r = rooms.get(o - 1);
        return r.sealed ? null : r;
    }

    /** Raum der Zelle oder null, auch für versiegelte Räume. */
    public Room rawRoom(int s, int ux, int uz) {
        int o = ownerAt(s, ux, uz);
        return o == 0 ? null : rooms.get(o - 1);
    }

    private volatile int[] bounds;

    /**
     * Grundriss-Rechteck jedes Raums im Geschoss {@code s} (Index × 4: minX, minZ, maxX, maxZ), einmal pro
     * Region berechnet. Performance (Phase 4): Einrichtung und Spezialräume müssen dann nicht mehr pro
     * Raum die ganze Regionsebene absuchen.
     */
    public int[] bounds(Room r, int s) {
        int[] b = bounds;
        if (b == null) {
            synchronized (this) {
                b = bounds;
                if (b == null) {
                    b = computeBounds();
                    bounds = b;
                }
            }
        }
        int base = r.index * 4;
        return new int[]{b[base], b[base + 1], b[base + 2], b[base + 3]};
    }

    private int[] computeBounds() {
        int[] b = new int[rooms.size() * 4];
        for (int i = 0; i < rooms.size(); i++) {
            b[i * 4] = Integer.MAX_VALUE;
            b[i * 4 + 1] = Integer.MAX_VALUE;
            b[i * 4 + 2] = Integer.MIN_VALUE;
            b[i * 4 + 3] = Integer.MIN_VALUE;
        }
        for (int s = 0; s < storeys; s++) {
            for (int uz = 0; uz < ru; uz++) {
                for (int ux = 0; ux < ru; ux++) {
                    int o = owner[(s * ru + uz) * ru + ux];
                    if (o == 0) {
                        continue;
                    }
                    Room r = rooms.get(o - 1);
                    // Rechteck im untersten Geschoss des Raums (bzw. erstes Geschoss eines Atriums)
                    int bottom = r.kind == Room.Kind.ATRIUM ? r.atrium.s0 : r.bottom;
                    if (s != bottom) {
                        continue;
                    }
                    int i = (o - 1) * 4;
                    b[i] = Math.min(b[i], ux);
                    b[i + 1] = Math.min(b[i + 1], uz);
                    b[i + 2] = Math.max(b[i + 2], ux);
                    b[i + 3] = Math.max(b[i + 3], uz);
                }
            }
        }
        return b;
    }

    /** Eindeutiger Schlüssel einer Zellkante: Zelle (ux, uz) und ihre West- (axis 0) oder Nordkante (axis 1). */
    public static int edgeKey(int ru, int s, int ux, int uz, int axis) {
        return ((s * ru + uz) * ru + ux) * 2 + axis;
    }

    public int edgeKey(int s, int ux, int uz, int axis) {
        return edgeKey(ru, s, ux, uz, axis);
    }
}
