package de.officerooms.furnish;

import de.officerooms.room.OfficePlanner;
import de.officerooms.room.PlanCache;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Liefert die Einrichtung eines Chunks. Die Einrichtung wird pro Raum geplant
 * ({@link RoomFurnisher}) und gecacht; ein Chunk setzt nur die Gegenstände, deren Hauptblock in
 * ihm liegt. Weil der Planer garantiert, dass jeder Gegenstand vollständig im Chunk seines
 * Hauptblocks liegt, entstehen keine Teilmöbel an Chunkgrenzen und nichts wird doppelt gesetzt.
 */
public final class FurnishingPlanner {

    private final OfficePlanner p;
    private final PlanCache<RoomFurnisher.Result> cache = new PlanCache<>(60000);
    private final PlanCache<RoomFurnisher.Result> lightCache = new PlanCache<>(60000);
    private final PlanCache<Map<Integer, List<Placement>>> byStorey = new PlanCache<>(512);

    public FurnishingPlanner(OfficePlanner p) {
        this.p = p;
    }

    public OfficePlanner planner() {
        return p;
    }

    public RoomFurnisher.Result room(int rx, int rz, Room r) {
        return cache.get(roomKey(rx, rz, r), () -> RoomFurnisher.furnish(p, rx, rz, r));
    }

    private static long roomKey(int rx, int rz, Room r) {
        return (((long) rx & 0xFFFFF) << 44) | (((long) rz & 0xFFFFF) << 24) | (r.index & 0xFFFFFF);
    }

    /** Nur Leuchten eines Raums (schnell, ohne Möbelplanung). */
    public RoomFurnisher.Result roomLights(int rx, int rz, Room r) {
        return lightCache.get(roomKey(rx, rz, r), () -> RoomFurnisher.lights(p, rx, rz, r));
    }

    /** Gegenstände des Chunks, deren Hauptblock im Geschoss s liegt. */
    public List<Placement> forChunkStorey(int cx, int cz, int s) {
        long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        Map<Integer, List<Placement>> m = byStorey.get(key, () -> {
            Map<Integer, List<Placement>> r = new java.util.HashMap<>();
            for (Placement pl : forChunk(cx, cz)) {
                r.computeIfAbsent(p.storeyOf(pl.y()), k -> new ArrayList<>()).add(pl);
            }
            return r;
        });
        return m.getOrDefault(s, List.of());
    }

    /** Alle Gegenstände, deren Hauptblock im Chunk (cx, cz) liegt. */
    public List<Placement> forChunk(int cx, int cz) {
        return forChunk(cx, cz, false);
    }

    /** Nur die Leuchten des Chunks (Weltgenerierung). */
    public List<Placement> lightsForChunk(int cx, int cz) {
        return forChunk(cx, cz, true);
    }

    private List<Placement> forChunk(int cx, int cz, boolean lightsOnly) {
        int x0 = cx << 4;
        int z0 = cz << 4;
        int rx = p.regionOf(x0);
        int rz = p.regionOf(z0);
        RegionLayout l = p.layout(rx, rz);
        int ux0 = (x0 - rx * p.regionBlocks) / p.unit;
        int uz0 = (z0 - rz * p.regionBlocks) / p.unit;
        int ux1 = Math.min(l.ru - 1, (x0 + 15 - rx * p.regionBlocks) / p.unit);
        int uz1 = Math.min(l.ru - 1, (z0 + 15 - rz * p.regionBlocks) / p.unit);
        Map<Room, Boolean> rooms = new IdentityHashMap<>();
        for (int s = 0; s < l.storeys; s++) {
            for (int uz = uz0; uz <= uz1; uz++) {
                for (int ux = ux0; ux <= ux1; ux++) {
                    Room r = l.room(s, ux, uz);
                    if (r == null || r.kind == Room.Kind.STAIRS) {
                        continue;
                    }
                    int bottom = r.kind == Room.Kind.ATRIUM ? r.atrium.s0 : r.bottom;
                    if (bottom == s) {
                        rooms.put(r, Boolean.TRUE);
                    }
                }
            }
        }
        List<Placement> out = new ArrayList<>();
        for (Room r : rooms.keySet()) {
            for (Placement pl : (lightsOnly ? roomLights(rx, rz, r) : room(rx, rz, r)).placements()) {
                if ((pl.x() >> 4) == cx && (pl.z() >> 4) == cz) {
                    out.add(pl);
                }
            }
        }
        return out;
    }
}
