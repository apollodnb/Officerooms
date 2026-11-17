package de.officerooms.special;

import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialRoomsTest {

    @Test
    void specialRoomsAppearRegularly() {
        OfficePlanner p = new OfficePlanner(12345L, GenSettings.DEFAULT, -64, 608);
        Map<SpecialType, Integer> n = new EnumMap<>(SpecialType.class);
        int rooms = 0;
        int tall = 0;
        int big = 0;
        for (int rx = -2; rx <= 2; rx++) {
            for (int rz = -2; rz <= 2; rz++) {
                RegionLayout l = p.layout(rx, rz);
                for (Room r : l.rooms) {
                    if (r.kind != Room.Kind.ROOM || r.sealed || r.cells == 0) {
                        continue;
                    }
                    rooms++;
                    if (r.clear >= 9 && r.cells >= 12) tall++;
                    if (r.cells >= 24 && r.storeys == 1) big++;
                    n.merge(SpecialRooms.of(p, rx, rz, r), 1, Integer::sum);
                    // deterministisch
                    assertTrue(SpecialRooms.of(p, rx, rz, r) == SpecialRooms.of(p, rx, rz, r));
                }
            }
        }
        System.out.println("[SpecialRooms] Büroräume " + rooms + ", hoch+groß " + tall + ", groß " + big + ": " + n);
        for (SpecialType t : SpecialType.values()) {
            if (t.isMegastructure() || t.ordinal() >= SpecialType.COLD_STORAGE.ordinal()) {
                continue; // megastructures: region-level band; curiosities: new worlds only (see below)
            }
            assertTrue(n.getOrDefault(t, 0) > 0, "kein " + t);
        }
        for (SpecialType t : new SpecialType[]{SpecialType.COLD_STORAGE, SpecialType.ARCADE, SpecialType.CANTEEN}) {
            assertTrue(n.getOrDefault(t, 0) == 0, "old worlds must not get " + t);
        }
    }

    /** 0.29.0: in new worlds (content_version 1) every curiosity room appears; old types stay as frequent as before. */
    @Test
    void curiositiesInNewWorlds() {
        GenSettings d = GenSettings.DEFAULT;
        GenSettings modern = new GenSettings(d.grid(), d.roomHeights(), d.roomSizes(), d.compositeChance(), d.corridors(), d.stairsPerStorey(),
                d.atria(), d.doors(), d.windows(), d.furnishing(), d.special(), 1);
        OfficePlanner p = new OfficePlanner(12345L, modern, -64, 608);
        Map<SpecialType, Integer> n = new EnumMap<>(SpecialType.class);
        for (int rx = -3; rx <= 3; rx++) {
            for (int rz = -3; rz <= 3; rz++) {
                for (Room r : p.layout(rx, rz).rooms) {
                    if (r.kind == Room.Kind.ROOM && !r.sealed && r.cells > 0) {
                        n.merge(SpecialRooms.of(p, rx, rz, r), 1, Integer::sum);
                    }
                }
            }
        }
        System.out.println("[Curiosities] " + n);
        for (SpecialType t : new SpecialType[]{SpecialType.COLD_STORAGE, SpecialType.LAST_MEETING, SpecialType.MAIL_ROOM,
                SpecialType.RAIN_ROOM, SpecialType.CANTEEN, SpecialType.ARCADE}) {
            assertTrue(n.getOrDefault(t, 0) > 0, "kein " + t);
        }
    }

    @Test
    void poolroomsAreRareAndComplete() {
        OfficePlanner p = new OfficePlanner(4711L, GenSettings.DEFAULT, -64, 608);
        int sites = 0;
        int withVault = 0;
        for (int rx = -20; rx < 20; rx++) {
            for (int rz = -20; rz < 20; rz++) {
                de.officerooms.megastructure.Poolrooms.Site a = p.pools.site(rx, rz);
                assertTrue((a == null) == (p.pools.site(rx, rz) == null), "not deterministic");
                if (a == null) {
                    continue;
                }
                sites++;
                if (a.chests.stream().anyMatch(c -> c.loot().contains("vault"))) {
                    withVault++;
                }
            }
        }
        System.out.println("[Poolrooms] " + sites + " of 1600 regions, " + withVault + " with a maze vault");
        assertTrue(sites >= 70 && sites <= 160, "Poolrooms frequency off: " + sites);
        assertTrue(withVault == sites, "every Poolrooms band needs its maze vault");
    }

    @Test
    void bottomFiveStoreysAreCompletelyDark() {
        OfficePlanner p = new OfficePlanner(777L, GenSettings.DEFAULT, -64, 608);
        int darkRooms = 0;
        int litAbove = 0;
        long lights = 0;
        long furniture = 0;
        for (int rx = 0; rx <= 1; rx++) {
            for (int rz = 0; rz <= 1; rz++) {
                RegionLayout l = p.layout(rx, rz);
                for (Room r : l.rooms) {
                    if (r.sealed || r.cells == 0 || r.kind == Room.Kind.STAIRS) {
                        continue;
                    }
                    int bottom = r.kind == Room.Kind.ATRIUM ? r.atrium.s0 : r.bottom;
                    de.officerooms.furnish.RoomFurnisher.Result res = de.officerooms.furnish.RoomFurnisher.furnish(p, rx, rz, r);
                    long nLights = res.placements().stream().filter(pl -> pl.item().isLight()).count();
                    if (bottom < 5) {
                        darkRooms++;
                        assertTrue(nLights == 0, "Licht in dunkler Ebene: " + r);
                        // auch keine Lichtposition unterhalb von Geschoss 5
                    } else if (nLights > 0) {
                        litAbove++;
                    }
                    for (de.officerooms.furnish.Placement pl : res.placements()) {
                        assertTrue(pl.y() >= p.floorY(5) || !pl.item().isLight(), "Licht unter Y " + p.floorY(5) + ": " + pl);
                    }
                    lights += nLights;
                    furniture += res.placements().size() - nLights;
                }
            }
        }
        System.out.println("[Dunkel] Räume in Geschoss 0–4: " + darkRooms + " (ohne Licht), beleuchtete Räume darüber: " + litAbove
                + ", Y-Grenze " + p.floorY(5) + "; Leuchten " + lights + ", Möbel " + furniture);
        assertTrue(darkRooms > 100 && litAbove > 1000);
    }
}
