package de.officerooms.megastructure;

import java.io.PrintWriter;
import org.junit.jupiter.api.Test;

/** Writes a plan of guest floors (one char per block) to build/hotel_map.txt; a dev aid, not an assertion. */
class HotelMapDump {

    @Test
    void dump() throws Exception {
        Hotel.Site s = new Hotel.Site(1, 1, 10, 0, 4711L, 12, 12, 2, true);
        try (PrintWriter w = new PrintWriter("build/hotel_map.txt")) {
            for (int lv = 1; lv <= 3; lv++) {
                w.println("LEVEL " + lv);
                for (int lz = 1; lz < 160; lz++) {
                    StringBuilder b = new StringBuilder();
                    for (int lx = 1; lx < 160; lx++) {
                        Hotel.Part p = s.part(lx, lv * 6 + 1, lz, false);
                        Hotel.Part p2 = s.part(lx, lv * 6 + 2, lz, false);
                        String n = p.name();
                        char c = p == Hotel.Part.AIR ? (p2 == Hotel.Part.AIR ? '.' : 'o')
                                : n.startsWith("WAINSCOT") || n.startsWith("RING") ? '#'
                                : n.contains("DOOR") ? 'D' : n.contains("IRON") ? 'X' : n.startsWith("STAIR") ? 'S'
                                : n.startsWith("RAIL") ? '+' : n.startsWith("BED") ? 'b' : 'f';
                        b.append(c);
                    }
                    w.println(b);
                }
            }
        }
    }
}
