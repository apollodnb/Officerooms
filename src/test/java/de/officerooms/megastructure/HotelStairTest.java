package de.officerooms.megastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 0.38.1: the L-shaped hotel stairs climb the full storey (six steps for six blocks), in every rotation. */
class HotelStairTest {

    @Test
    void sixStepsInEveryRotation() {
        for (int rot = 0; rot < 4; rot++) {
            int steps = 0;
            for (int o = 1; o <= 6; o++) {
                boolean found = false;
                for (int a = 1; a < 10; a++) {
                    for (int b = 1; b < 10; b++) {
                        Hotel.Part p = Hotel.Site.lStair(a, b, o, rot);
                        found |= p != null && p.name().startsWith("STAIR_") && !p.name().equals("STAIR_SUPPORT");
                    }
                }
                if (found) {
                    steps++;
                }
            }
            assertTrue(steps == 6, "rotation " + rot + ": steps on " + steps + " of 6 heights");
        }
    }
}
