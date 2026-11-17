package de.officerooms.megastructure;

import de.officerooms.room.GenSettings;
import de.officerooms.room.OfficePlanner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Dev tool (0.38.x): world positions of rare cells for in-game tests (seed 4711, new worlds) → build/spots.txt. */
class SpotsDump {

    @Test
    void dump() throws IOException {
        // the settings of the real dimension (they differ from GenSettings.DEFAULT)
        var json = com.google.gson.JsonParser.parseString(Files.readString(Path.of("src/main/resources/data/office_rooms/dimension/office_rooms.json")))
                .getAsJsonObject().getAsJsonObject("generator").get("settings");
        GenSettings g = de.officerooms.worldgen.GenSettingsCodec.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow(false, e -> { });
        // the world derives the planner seed from its RandomState (custom generator → dummy noise settings → Xoroshiro)
        long seed = new net.minecraft.world.level.levelgen.XoroshiroRandomSource(4711L).forkPositional()
                .fromHashOf(new net.minecraft.resources.ResourceLocation("office_rooms", "office_layout")).forkPositional().at(0, 0, 0).nextLong();
        OfficePlanner p = new OfficePlanner(seed, g, -64, 608);
        int R = p.regionBlocks;
        List<String> out = new ArrayList<>();
        int[] got = new int[5];
        for (int ring = 0; ring <= 5; ring++) {
            for (int rx = -ring; rx <= ring; rx++) {
                for (int rz = -ring; rz <= ring; rz++) {
                    if (Math.max(Math.abs(rx), Math.abs(rz)) != ring) {
                        continue;
                    }
                    Megastructures.Kind kind = Megastructures.kindOf(p, rx, rz);
                    LevelFun.Site f = kind == Megastructures.Kind.LEVEL_FUN ? p.fun.site(rx, rz) : null;
                    if (f != null) {
                        for (int lv = 0; lv < f.storeys; lv++) {
                            for (int cx = 1; cx < 15; cx++) {
                                for (int cz = 1; cz < 15; cz++) {
                                    int t = f.cellType(lv, cx, cz);
                                    int k = t == LevelFun.BANQUET ? 0 : t == LevelFun.DARK ? 1 : -1;
                                    if (k >= 0 && got[k]++ < 2) {
                                        out.add((k == 0 ? "banquet " : "dark ") + (rx * R + cx * LevelFun.C + 5) + " " + (f.yBase + lv * 6 + 1) + " " + (rz * R + cz * LevelFun.C + 5));
                                    }
                                }
                            }
                        }
                    }
                    Industrial.Site in = kind == Megastructures.Kind.INDUSTRIAL ? p.industrial.site(rx, rz) : null;
                    if (in != null) {
                        for (int lv = 0; lv < in.storeys; lv++) {
                            for (int cx = 0; cx < 10; cx++) {
                                for (int cz = 0; cz < 10; cz++) {
                                    if (in.type(lv, cx, cz) == Industrial.LOCKERS && got[2]++ < 2) {
                                        out.add("lockers " + (rx * R + cx * Industrial.C + 8) + " " + (in.yBase + lv * 6 + 1) + " " + (rz * R + cz * Industrial.C + 8));
                                    }
                                }
                            }
                        }
                    }
                    Neighbourhood.Site n = kind == Megastructures.Kind.NEIGHBOURHOOD ? p.hood.site(rx, rz) : null;
                    if (n != null) {
                        for (int house = 0; house < Neighbourhood.COLS * Neighbourhood.ROWS; house++) {
                            if (n.park(house) && got[3]++ < 2) {
                                out.add("park " + (rx * R + Neighbourhood.houseX(house) + 8) + " " + (n.yBase + 1) + " " + (rz * R + Neighbourhood.houseZ(house) + 8));
                            }
                        }
                    }
                    Maze.Site mz = kind == Megastructures.Kind.MAZE ? p.maze.site(rx, rz) : null;
                    if (mz != null && got[4] < 9) {
                        for (int[] r : mz.ramps) {
                            if (got[4]++ < 3) {
                                out.add("maze ramp lv " + r[0] + " first step " + (rx * R + Maze.ORIGIN + r[1] * Maze.CELL + 1) + " "
                                        + (mz.yBase + r[0] * Maze.LP + 1) + " " + (rz * R + Maze.ORIGIN + r[2] * Maze.CELL + 1) + " (climbs east)");
                            }
                        }
                        got[4] = 9;
                    }
                    Hotel.Site h = kind == Megastructures.Kind.HOTEL ? p.hotel.site(rx, rz) : null;
                    if (h != null && got[4] < 0) {
                        for (int off = 0; off < h.h; off++) {
                            int c = 0;
                            String first = null;
                            for (int lx = 3; lx < 157; lx++) {
                                for (int lz = 3; lz < 157; lz++) {
                                    if (off >= 6 && off <= 12 && h.part(lx, off, lz, false).name().contains("STAIR")) {
                                        c++;
                                        if (first == null) {
                                            first = (rx * R + lx) + " " + (h.yBase + off) + " " + (rz * R + lz);
                                        }
                                    }
                                }
                            }
                            if (c > 0) {
                                out.add("hotel stairs off " + off + ": " + c + " first " + first);
                            }
                        }
                    }
                }
            }
        }
        for (int rx = -30; rx <= 30; rx++) {
            for (int rz = -30; rz <= 30; rz++) {
                try {
                    p.pools.site(rx, rz);
                } catch (RuntimeException e) {
                    java.io.StringWriter sw = new java.io.StringWriter();
                    (e.getCause() != null ? e.getCause() : e).printStackTrace(new java.io.PrintWriter(sw));
                    out.add("POOL FAIL " + rx + " " + rz + ": " + sw.toString().lines().limit(8).reduce("", (x, y) -> x + "\n" + y));
                }
            }
        }
        out.add(0, "seed " + seed);
        out.add(1, "check: kind of region -1/0 = " + Megastructures.kindOf(p, -1, 0) + " (world: CINEMA)");
        Files.createDirectories(Path.of("build"));
        Files.write(Path.of("build/spots.txt"), out);
    }
}
