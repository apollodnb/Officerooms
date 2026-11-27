package de.officerooms.worldgen;

import de.officerooms.OfficeRooms;
import de.officerooms.compat.FurnitureBuilder;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.furnish.FurnishingPlanner;
import de.officerooms.furnish.Placement;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.structure.BlockSampler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Richtet die Umgebung jedes Spielers ein (Möbel und Leuchtenmodelle).
 *
 * <p>Warum nicht schon bei der Generierung: Die Dimension hat 204 Geschosse. Voll eingerichtet
 * hätte jede Chunk-Säule rund 4 000 BlockEntities (OfficeCraft-Möbel bestehen aus vielen
 * Teilblöcken). Das sprengt Speicher und Netzwerk (im Test: OutOfMemoryError). Deshalb setzt die
 * Generierung nur das <i>Licht</i> (unsichtbare Lichtblöcke), und Möbel entstehen in einem Umkreis
 * von {@link #RADIUS} Chunks und ±{@link #STOREYS} Geschossen um jeden Spieler, die nächsten zuerst.</p>
 *
 * <p>Die Planung ist deterministisch; gesetzt wird nur in noch freie Blöcke, und jede Einheit
 * (Chunk, Geschoss) wird in {@link FurnishedData} vermerkt. Dadurch entsteht nie etwas doppelt.</p>
 */
public final class FurnishingTicker {

    /** Horizontaler Umkreis in Chunks. */
    public static final int RADIUS = 3;
    /** Geschosse über und unter dem Spieler. */
    public static final int STOREYS = 3;
    /** Einheiten (Chunk × Geschoss) pro Durchlauf. */
    public static final int BUDGET = 10;
    /** Durchlauf alle n Ticks. */
    public static final int INTERVAL = 4;

    private static final int[][] OFFSETS;

    static {
        List<int[]> list = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                list.add(new int[]{dx, dz});
            }
        }
        list.sort((a, b) -> Integer.compare(a[0] * a[0] + a[1] * a[1], b[0] * b[0] + b[1] * b[1]));
        OFFSETS = list.toArray(new int[0][]);
    }

    /**
     * Performance (Phase 4): Position (Chunk, Geschoss) je Spieler, an der seine Umgebung zuletzt
     * vollständig eingerichtet war. Solange er dort bleibt, wird nichts erneut geprüft.
     */
    private static final java.util.Map<java.util.UUID, Long> CLEAN = new java.util.concurrent.ConcurrentHashMap<>();

    private FurnishingTicker() {
    }

    /** The chunk and its 8 neighbours are fully loaded (no synchronous chunk loading while placing). */
    private static boolean loaded3x3(ServerLevel level, int cx, int cz) {
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (level.getChunkSource().getChunkNow(cx + dx, cz + dz) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FurnishingTicker::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != OfficeRoomsDimension.LEVEL_KEY || level.getGameTime() % INTERVAL != 0
                || level.players().isEmpty()) {
            return;
        }
        if (!(level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
            return;
        }
        try {
            run(level, gen);
        } catch (RuntimeException e) {
            OfficeRooms.LOGGER.error("Furnishing near a player failed", e);
        }
    }

    private static void run(ServerLevel level, OfficeChunkGenerator gen) {
        BlockSampler sampler = gen.sampler(level.getChunkSource().randomState(), level);
        OfficePlanner p = sampler.planner();
        FurnishingPlanner fp = gen.furnishing(sampler);
        FurnishedData data = FurnishedData.get(level);
        int budget = BUDGET;
        for (ServerPlayer player : level.players()) {
            int pcx = player.getBlockX() >> 4;
            int pcz = player.getBlockZ() >> 4;
            int ps = p.storeyOf(player.getBlockY());
            long here = FurnishedData.key(pcx, pcz, ps);
            Long clean = CLEAN.get(player.getUUID());
            if (clean != null && clean == here) {
                continue;
            }
            boolean complete = true;
            for (int dv = 0; dv <= 2 * STOREYS; dv++) {
                int s = ps + ((dv & 1) == 0 ? dv / 2 : -(dv + 1) / 2);
                if (s < 0 || s >= p.storeys) {
                    continue;
                }
                for (int[] o : OFFSETS) {
                    int cx = pcx + o[0];
                    int cz = pcz + o[1];
                    long key = FurnishedData.key(cx, cz, s);
                    if (data.isDone(key)) {
                        continue;
                    }
                    if (!loaded3x3(level, cx, cz)) {
                        // 0.23.1: OfficeCraft part blocks notify their neighbours; with a neighbour chunk
                        // not loaded yet that made the server thread wait for chunk generation (seconds).
                        complete = false;
                        continue;
                    }
                    for (Placement pl : fp.forChunkStorey(cx, cz, s)) {
                        FurnitureBuilder.placeNearPlayer(level, pl);
                    }
                    data.markDone(key);
                    if (--budget <= 0) {
                        return;
                    }
                }
            }
            if (complete) {
                CLEAN.put(player.getUUID(), here);
            } else {
                CLEAN.remove(player.getUUID());
            }
        }
    }

}
