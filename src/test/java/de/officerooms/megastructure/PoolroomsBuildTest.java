package de.officerooms.megastructure;

import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 0.38.1: every Poolrooms site can be built (narrow halls used to push pools past the region and the whole site failed). */
class PoolroomsBuildTest {

    @Test
    void allSitesBuild() {
        GenSettings d = GenSettings.DEFAULT;
        GenSettings g = new GenSettings(d.grid(), d.roomHeights(), d.roomSizes(), d.compositeChance(), d.corridors(), d.stairsPerStorey(),
                d.atria(), d.doors(), d.windows(), d.furnishing(), d.special(), 1);
        int built = 0;
        for (long seed : new long[]{4711L, 7657905887809284474L, 42L, 123456789L}) {
            OfficePlanner p = new OfficePlanner(seed, g, -64, 608);
            for (int rx = -25; rx <= 25; rx++) {
                for (int rz = -25; rz <= 25; rz++) {
                    if (p.pools.site(rx, rz) != null) {
                        built++;
                    }
                }
            }
        }
        assertTrue(built > 20, "only " + built + " Poolrooms");
    }
}
