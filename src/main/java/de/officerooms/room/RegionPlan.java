package de.officerooms.room;

/**
 * Fertige Planung einer Region: {@link RegionLayout} plus alle Öffnungen (Türen, Durchgänge,
 * Fenster) auf allen Zellkanten, einschließlich der West- und Nordgrenze der Region.
 *
 * <p>Die Grenzlinie zur West- bzw. Nordnachbarregion gehört zu dieser Region (lokales x = 0 bzw.
 * z = 0). Deshalb braucht nur diese Region die Türen auf dieser Linie zu kennen. Um sie zu
 * planen, liest sie die {@link RegionLayout} der beiden Nachbarn (nur deren Grundriss, keine
 * weitere Rekursion).</p>
 */
public final class RegionPlan {

    public final RegionLayout layout;
    final byte[] edges;
    public final int doors;
    public final int windows;
    public final int boundaryDoors;

    public RegionPlan(RegionLayout layout, byte[] edges, int doors, int windows, int boundaryDoors) {
        this.layout = layout;
        this.edges = edges;
        this.doors = doors;
        this.windows = windows;
        this.boundaryDoors = boundaryDoors;
    }

    /** Öffnungscode der West- (axis 0) oder Nordkante (axis 1) der Zelle. */
    public int edge(int s, int ux, int uz, int axis) {
        if (s < 0 || s >= layout.storeys) {
            return 0;
        }
        return edges[layout.edgeKey(s, ux, uz, axis)] & 0xFF;
    }

    public int edgeByKey(int key) {
        return edges[key] & 0xFF;
    }

    public int edgeCount() {
        return edges.length;
    }
}
