package de.officerooms.megastructure;

import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 0.41.0: /locate markers - marker for every planned megastructure. */
class MarkerAnchorTest {

    @Test
    void everyMegastructureGetsAMarker() {
        GenSettings d = GenSettings.DEFAULT;
        GenSettings g = new GenSettings(d.grid(), d.roomHeights(), d.roomSizes(), d.compositeChance(), d.corridors(), d.stairsPerStorey(),
                d.atria(), d.doors(), d.windows(), d.furnishing(), d.special(), 3);
        OfficePlanner p = new OfficePlanner(7657905887809284474L, g, -64, 608);
        Map<Megastructures.Kind, Integer> n = new EnumMap<>(Megastructures.Kind.class);
        for (int rx = -20; rx <= 20; rx++) {
            for (int rz = -20; rz <= 20; rz++) {
                Megastructures.Kind k = Megastructures.kindOf(p, rx, rz);
                for (Megastructures.Kind q : Megastructures.Kind.values()) {
                    int[] a = Megastructures.anchor(p, q, rx, rz);
                    if (q != k) {
                        assertEquals(null, a, q + " marker in a " + k + " region");
                    } else if (a != null) {
                        assertTrue(p.regionOf(a[0]) == rx && p.regionOf(a[2]) == rz, "marker outside its region");
                        n.merge(q, 1, Integer::sum);
                    }
                }
            }
        }
        for (Megastructures.Kind q : Megastructures.Kind.values()) {
            if (q != Megastructures.Kind.NONE) {
                assertTrue(n.getOrDefault(q, 0) > 0, "no " + q + " marker in 41x41 regions: " + n);
            }
        }
        System.out.println("markers: " + n);
    }
}
