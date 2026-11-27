package de.officerooms.entity;

import de.officerooms.OfficeRooms;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.room.OfficePlanner;
import de.officerooms.room.RegionLayout;
import de.officerooms.room.Room;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spawnlogik für Wolfspider und Headless Warden, eingebunden in die Raumplanung.
 *
 * <p>Statt zufälliger Blockpositionen zählt der Spawner die tatsächlich geplanten, erreichbaren
 * Räume in der Umgebung jedes Spielers und hält die Zahl der Mobs dort bei
 * {@code Räume / roomsPerMob}. Gespawnt wird nur in der Mitte einer Rasterzelle eines Raums auf
 * Laufebene (garantiert Innenraum, nie Wand oder Massiv), nur bei kollisionsfreiem Platz (also nicht
 * in Möbeln), in geladenen Chunks und mit Mindestabstand zu allen Spielern. Weit entfernte Mobs
 * werden entfernt, damit die Serverlast begrenzt bleibt.</p>
 */
public final class MobSpawner {

    private static final int INTERVAL = 60;
    private static final int WOLF_RADIUS = 48;
    private static final int WOLF_STOREYS = 2;
    private static final int WARDEN_RADIUS = 64;
    private static final int WARDEN_STOREYS = 4;
    private static final double DESPAWN_DISTANCE = 96.0;

    private MobSpawner() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(MobSpawner::onLevelTick);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != OfficeRoomsDimension.LEVEL_KEY || level.getGameTime() % INTERVAL != 7) {
            return;
        }
        if (!(level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen)) {
            return;
        }
        try {
            despawnFar(level);
            OfficePlanner p = gen.sampler(level.getChunkSource().randomState(), level).planner();
            mazeFatigue(level, p);
            if (!OfficeRoomsConfig.SPAWN_ENABLED.get() || level.getDifficulty() == Difficulty.PEACEFUL) {
                return;
            }
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                // Performance: Raumliste einmal (großer Warden-Umkreis) bestimmen, Wolfspider-Umkreis daraus filtern.
                List<long[]> wardenRooms = spawnableRooms(p, roomsAround(p, player.getBlockX(), player.getBlockY(), player.getBlockZ(),
                        WARDEN_RADIUS, WARDEN_STOREYS));
                int ps = p.storeyOf(player.getBlockY());
                List<long[]> wolfRooms = new ArrayList<>();
                for (long[] r : wardenRooms) {
                    long wx = r[0] * p.regionBlocks + r[4] * p.unit;
                    long wz = r[1] * p.regionBlocks + r[5] * p.unit;
                    if (Math.abs(wx - player.getBlockX()) <= WOLF_RADIUS && Math.abs(wz - player.getBlockZ()) <= WOLF_RADIUS
                            && Math.abs(r[3] - ps) <= WOLF_STOREYS) {
                        wolfRooms.add(r);
                    }
                }
                spawnAround(level, p, player, ModEntities.WOLFSPIDER.get(), ModEntities.FRIENDLY_WOLFSPIDER.get(),
                        OfficeRoomsConfig.WOLFSPIDER_FRIENDLY_CHANCE.get(), wolfRooms, WOLF_RADIUS, WOLF_STOREYS,
                        OfficeRoomsConfig.WOLFSPIDER_ROOMS_PER_MOB.get(), Integer.MAX_VALUE, OfficeRoomsConfig.SPAWN_MIN_DISTANCE.get());
                // The Gaunt: nur in den dunklen untersten Ebenen und nur, wenn der Spieler selbst dort ist
                if (de.officerooms.special.SpecialRooms.darkStorey(p, ps)) {
                    List<long[]> darkRooms = new ArrayList<>();
                    for (long[] r : wolfRooms) {
                        if (de.officerooms.special.SpecialRooms.darkStorey(p, (int) r[3])) {
                            darkRooms.add(r);
                        }
                    }
                    spawnAround(level, p, player, ModEntities.GAUNT.get(), null, 0, darkRooms, WOLF_RADIUS, WOLF_STOREYS,
                            OfficeRoomsConfig.GAUNT_ROOMS_PER_MOB.get(), OfficeRoomsConfig.GAUNT_MAX_NEAR_PLAYER.get(), 24);
                }
                // 0.6.0: Hound und Smiler überall in der Dimension, Colony extrem selten
                if (level.random.nextDouble() < OfficeRoomsConfig.HOUND_SPAWN_CHANCE.get())
                spawnAround(level, p, player, ModEntities.HOUND.get(), null, 0, wolfRooms, WOLF_RADIUS, WOLF_STOREYS,
                        OfficeRoomsConfig.HOUND_ROOMS_PER_MOB.get(), OfficeRoomsConfig.HOUND_MAX_NEAR_PLAYER.get(), 24);
                // 0.24.0: Deathmoths on dark walls (population like the other mobs, never in Maze/Poolrooms)
                DeathmothDirector.populateOffice(level, player,
                        p.megaContains(player.getBlockX(), player.getBlockY(), player.getBlockZ()) ? Math.max(70, wolfRooms.size()) : wolfRooms.size());
                int maxLight = OfficeRoomsConfig.SMILER_MAX_LIGHT.get();
                if (level.random.nextDouble() < OfficeRoomsConfig.SMILER_SPAWN_CHANCE.get())
                spawnAround(level, p, player, ModEntities.SMILER.get(), null, 0, wolfRooms, WOLF_RADIUS, WOLF_STOREYS,
                        OfficeRoomsConfig.SMILER_ROOMS_PER_MOB.get(), OfficeRoomsConfig.SMILER_MAX_NEAR_PLAYER.get(), 16,
                        q -> level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, q) <= maxLight
                                && level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, q.above()) <= maxLight);
                if (level.random.nextDouble() < OfficeRoomsConfig.COLONY_SPAWN_CHANCE.get())
                spawnAround(level, p, player, ModEntities.COLONY.get(), null, 0, wardenRooms, WARDEN_RADIUS, WARDEN_STOREYS,
                        OfficeRoomsConfig.COLONY_ROOMS_PER_MOB.get(), 1, 32);
                // 0.13.0: The Flayed, extremely rare, at most one near a player, never close by
                if (level.random.nextDouble() < OfficeRoomsConfig.FLAYED_SPAWN_CHANCE.get() && !nearMaze(p, player))   // 0.27.0
                spawnAround(level, p, player, ModEntities.FLAYED.get(), null, 0, wardenRooms, WARDEN_RADIUS, WARDEN_STOREYS,
                        40, 1, Math.max(40, OfficeRoomsConfig.SPAWN_MIN_DISTANCE.get()));
                poolrooms(level, p, player);
                neighbourhood(level, p, player);
                maze(level, p, player);
                hotel(level, p, player);
                industrial(level, p, player);
                levelFun(level, p, player);
                // 0.32.0: Aranea Membri, rare in the offices (never in other megastructures), and in the cinemas
                if (level.random.nextDouble() < OfficeRoomsConfig.ARANEA_SPAWN_CHANCE.get()
                        && !p.megaContains(player.getBlockX(), player.getBlockY(), player.getBlockZ()))
                spawnAround(level, p, player, ModEntities.ARANEA.get(), null, 0, wardenRooms, WARDEN_RADIUS, WARDEN_STOREYS, 40, 1, 24);
                cinema(level, p, player);
                mall(level, p, player);
                if (level.random.nextDouble() < OfficeRoomsConfig.WARDEN_SPAWN_CHANCE.get())
                spawnAround(level, p, player, ModEntities.HEADLESS_WARDEN.get(), null, 0, wardenRooms, WARDEN_RADIUS, WARDEN_STOREYS,
                        OfficeRoomsConfig.WARDEN_ROOMS_PER_MOB.get(), OfficeRoomsConfig.WARDEN_MAX_NEAR_PLAYER.get(),
                        Math.max(32, OfficeRoomsConfig.SPAWN_MIN_DISTANCE.get()));
            }
        } catch (RuntimeException e) {
            OfficeRooms.LOGGER.error("Mob-Spawn fehlgeschlagen", e);
        }
    }

    /** Räume (Bodengeschoss im Bereich) um eine Position; jeder Raum einmal. */
    public static List<long[]> roomsAround(OfficePlanner p, int x, int y, int z, int radius, int storeys) {
        int ps = p.storeyOf(y);
        Map<Room, long[]> found = new IdentityHashMap<>();
        for (int bx = x - radius; bx <= x + radius; bx += p.unit) {
            for (int bz = z - radius; bz <= z + radius; bz += p.unit) {
                int rx = p.regionOf(bx);
                int rz = p.regionOf(bz);
                RegionLayout l = p.layout(rx, rz);
                int ux = Math.floorMod(bx, p.regionBlocks) / p.unit;
                int uz = Math.floorMod(bz, p.regionBlocks) / p.unit;
                for (int s = Math.max(0, ps - storeys); s <= Math.min(p.storeys - 1, ps + storeys); s++) {
                    Room r = l.room(s, ux, uz);
                    if (r != null && r.bottom == s && r.isWalkableRoom() && !found.containsKey(r)) {
                        found.put(r, new long[]{rx, rz, r.index, s, ux, uz});
                    }
                }
            }
        }
        return new ArrayList<>(found.values());
    }

    /** Natürliche Spawns nicht in Spezialräumen (Wasser, Dorf, Lush, Sculk haben eigene Regeln). */
    private static List<long[]> spawnableRooms(OfficePlanner p, List<long[]> rooms) {
        List<long[]> out = new ArrayList<>(rooms.size());
        for (long[] r : rooms) {
            Room room = p.layout((int) r[0], (int) r[1]).rooms.get((int) r[2]);
            if (de.officerooms.special.SpecialRooms.of(p, (int) r[0], (int) r[1], room) == de.officerooms.special.SpecialType.NONE) {
                out.add(r);
            }
        }
        return out;
    }

    /**
     * Hält die Zahl der Mobs ({@code type} plus Variante {@code alt}) bei Räume / roomsPerMob. Jeder
     * neue Mob ist mit Chance {@code altChance} die Variante (zufällig verteilt, kein fester Raum).
     */
    private static void spawnAround(ServerLevel level, OfficePlanner p, ServerPlayer player, EntityType<? extends Mob> type,
                                    EntityType<? extends Mob> alt, double altChance, List<long[]> rooms, int radius, int storeys, int roomsPerMob, int cap, int minDistance) {
        spawnAround(level, p, player, type, alt, altChance, rooms, radius, storeys, roomsPerMob, cap, minDistance, null);
    }

    private static void spawnAround(ServerLevel level, OfficePlanner p, ServerPlayer player, EntityType<? extends Mob> type,
                                    EntityType<? extends Mob> alt, double altChance, List<long[]> rooms, int radius, int storeys, int roomsPerMob, int cap, int minDistance,
                                    java.util.function.Predicate<BlockPos> where) {
        int px = player.getBlockX();
        int py = player.getBlockY();
        int pz = player.getBlockZ();
        int ps = p.storeyOf(py);
        double expected = rooms.size() / (double) roomsPerMob;
        int target = (int) expected + (level.random.nextDouble() < expected - (int) expected ? 1 : 0);
        target = Math.min(target, cap);
        AABB box = new AABB(px - radius, p.floorY(ps - storeys), pz - radius, px + radius + 1, p.floorY(ps + storeys + 1), pz + radius + 1);
        int existing = level.getEntities(type, box, e -> e.isAlive()).size()
                + (alt == null ? 0 : level.getEntities(alt, box, e -> e.isAlive() && !(e instanceof FriendlyWolfspider f && f.isTame())).size());
        if (existing >= target || rooms.isEmpty()) {
            return;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            long[] r = rooms.get(level.random.nextInt(rooms.size()));
            // eine zufällige Zelle dieses Raums im Bodengeschoss (Zellmitte = sicherer Innenraum)
            RegionLayout l = p.layout((int) r[0], (int) r[1]);
            Room room = l.rooms.get((int) r[2]);
            int s = (int) r[3];
            int ux = (int) r[4] + level.random.nextInt(5) - 2;
            int uz = (int) r[5] + level.random.nextInt(5) - 2;
            if (ux < 0 || uz < 0 || ux >= l.ru || uz >= l.ru || l.room(s, ux, uz) != room) {
                ux = (int) r[4];
                uz = (int) r[5];
            }
            int x = (int) r[0] * p.regionBlocks + ux * p.unit + p.unit / 2;
            int z = (int) r[1] * p.regionBlocks + uz * p.unit + p.unit / 2;
            int y = p.floorY(s) + 1;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isPositionEntityTicking(pos) || level.getNearestPlayer(x + 0.5, y, z + 0.5, minDistance, false) != null) {
                continue;
            }
            // auf dem Bodenbelag stehen (Teppich hat eine flache Kollisionsform)
            net.minecraft.world.phys.shapes.VoxelShape floor = level.getBlockState(pos).getCollisionShape(level, pos);
            double sy = y + (floor.isEmpty() ? 0.0 : floor.max(net.minecraft.core.Direction.Axis.Y));
            if (sy > y + 0.5 || !level.noCollision(type.getAABB(x + 0.5, sy, z + 0.5)) || !level.getBlockState(pos.below()).isSolid()
                    || level.getBlockState(pos.below()).is(de.officerooms.block.ModBlocks.SECURITY_WALL.get())
                    || level.getBlockState(pos.below()).is(de.officerooms.block.ModBlocks.ELEVATOR.get())
                    || level.getBlockState(pos.below()).is(de.officerooms.block.ModBlocks.ELEVATOR_FLOOR.get())) {
                continue;
            }
            if (where != null && !where.test(pos)) {
                continue;
            }
            EntityType<? extends Mob> pick = alt != null && level.random.nextDouble() < altChance ? alt : type;
            Mob mob = pick.create(level);
            if (mob == null) {
                return;
            }
            mob.moveTo(x + 0.5, sy, z + 0.5, level.random.nextFloat() * 360F, 0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    /**
     * Poolrooms: Die Büroräume des Bands zählen nicht als Räume. Hier werden Hound, Colony und (an dunklen
     * Stellen) Smiler auf trockenem Hallenboden in der Nähe gesetzt – gleiche Seltenheit wie in den Büros,
     * gerechnet mit 16 Räumen je 32 × 32 Blöcke Hallenfläche.
     */
    private static void poolrooms(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.Poolrooms.Site site = p.pools.site(p.regionOf(player.getBlockX()), p.regionOf(player.getBlockZ()));
        if (site == null || !site.inBand(player.getBlockY())) {
            return;
        }
        poolHunter(level, p, site, player);                  // 0.26.2: a Colony hunts here (was: the Hunter)
        double roomsEquivalent = 16.0 * (96.0 * 96.0) / (32.0 * 32.0);
        if (level.random.nextDouble() < OfficeRoomsConfig.HOUND_SPAWN_CHANCE.get())
        tryPool(level, p, site, player, ModEntities.HOUND.get(), roomsEquivalent / OfficeRoomsConfig.HOUND_ROOMS_PER_MOB.get(),
                OfficeRoomsConfig.HOUND_MAX_NEAR_PLAYER.get(), null);
        if (level.random.nextDouble() < OfficeRoomsConfig.COLONY_SPAWN_CHANCE.get())
        tryPool(level, p, site, player, ModEntities.COLONY.get(), roomsEquivalent / OfficeRoomsConfig.COLONY_ROOMS_PER_MOB.get(), 1, null);
        int maxLight = OfficeRoomsConfig.SMILER_MAX_LIGHT.get();
        if (level.random.nextDouble() < OfficeRoomsConfig.SMILER_SPAWN_CHANCE.get())
        tryPool(level, p, site, player, ModEntities.SMILER.get(), roomsEquivalent / OfficeRoomsConfig.SMILER_ROOMS_PER_MOB.get(),
                OfficeRoomsConfig.SMILER_MAX_NEAR_PLAYER.get(),
                q -> level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, q) <= maxLight);
    }

    /**
     * 0.26.2: the Poolrooms hunter is now a Colony (user request). While a player is in the band there is always
     * at least one Colony, 24–70 blocks away on dry hall floor. Hunters left over from older versions are removed.
     */
    private static void poolHunter(ServerLevel level, OfficePlanner p, de.officerooms.megastructure.Poolrooms.Site site, ServerPlayer player) {
        int baseX = site.rx * p.regionBlocks;
        int baseZ = site.rz * p.regionBlocks;
        AABB band = new AABB(baseX, site.yBase, baseZ, baseX + p.regionBlocks, site.yBase + de.officerooms.megastructure.Poolrooms.H, baseZ + p.regionBlocks).inflate(16);
        for (Hunter h : level.getEntities(ModEntities.HUNTER.get(), band, e -> e.isAlive() && !e.isPersistenceRequired())) {
            h.discard();
        }
        if (!level.getEntities(ModEntities.COLONY.get(), band, e -> e.isAlive()).isEmpty()) {
            return;
        }
        EntityType<Colony> type = ModEntities.COLONY.get();
        int y = site.yBase + de.officerooms.megastructure.Poolrooms.FLOOR + 1;
        for (int attempt = 0; attempt < 24; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double d = 24 + level.random.nextDouble() * 46;
            int x = (int) Math.floor(player.getX() + Math.cos(a) * d);
            int z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
            if (x < baseX + 5 || z < baseZ + 5 || x > baseX + p.regionBlocks - 6 || z > baseZ + p.regionBlocks - 6) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isPositionEntityTicking(pos) || level.getNearestPlayer(x + 0.5, y, z + 0.5, 20, false) != null) {
                continue;
            }
            if (!level.getBlockState(pos.below()).isSolid() || !level.getFluidState(pos).isEmpty()
                    || !level.noCollision(type.getAABB(x + 0.5, y, z + 0.5))) {
                continue;
            }
            Colony c = type.create(level);
            if (c == null) {
                return;
            }
            c.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0F);
            c.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            level.addFreshEntity(c);
            return;
        }
    }


    private static void tryPool(ServerLevel level, OfficePlanner p, de.officerooms.megastructure.Poolrooms.Site site, ServerPlayer player,
                                EntityType<? extends Mob> type, double expected, int cap, java.util.function.Predicate<BlockPos> where) {
        int target = Math.min(cap, (int) expected + (level.random.nextDouble() < expected - (int) expected ? 1 : 0));
        AABB box = player.getBoundingBox().inflate(48, 12, 48);
        if (level.getEntities(type, box, e -> e.isAlive()).size() >= target) {
            return;
        }
        int baseX = site.rx * p.regionBlocks;
        int baseZ = site.rz * p.regionBlocks;
        int y = site.yBase + de.officerooms.megastructure.Poolrooms.FLOOR + 1;
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = player.getBlockX() + level.random.nextInt(81) - 40;
            int z = player.getBlockZ() + level.random.nextInt(81) - 40;
            if (x < baseX + 4 || z < baseZ + 4 || x > baseX + p.regionBlocks - 5 || z > baseZ + p.regionBlocks - 5) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isPositionEntityTicking(pos) || level.getNearestPlayer(x + 0.5, y, z + 0.5, 24, false) != null) {
                continue;
            }
            if (!level.getBlockState(pos.below()).isSolid() || !level.getFluidState(pos).isEmpty()
                    || !level.noCollision(type.getAABB(x + 0.5, y, z + 0.5))) {
                continue;
            }
            if (where != null && !where.test(pos)) {
                continue;
            }
            Mob mob = type.create(level);
            if (mob == null) {
                return;
            }
            mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    // --- 0.8.0: Neighbourhood and The Maze ----------------------------------------------------

    /** The Maze: Mining Fatigue III for every player inside the band (refreshed every check). */
    private static void mazeFatigue(ServerLevel level, OfficePlanner p) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            if (p.maze.at(player.getBlockX(), player.getBlockY(), player.getBlockZ()) != null) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, INTERVAL + 40, 2, true, false, true));
            }
        }
    }

    /** Neighbourhood: a few creatures roam the dark streets (the residents of the houses are persistent). */
    private static void neighbourhood(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.Neighbourhood.Site site = p.hood.site(p.regionOf(player.getBlockX()), p.regionOf(player.getBlockZ()));
        if (site == null || !site.inBand(player.getBlockY())) {
            return;
        }
        int y = site.yBase + 1;
        AABB box = player.getBoundingBox().inflate(56, 12, 56);
        int roaming = level.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && !e.isPersistenceRequired()
                && (e instanceof Wolfspider || e instanceof Hound || e instanceof Smiler)).size();
        if (roaming >= 3 || level.random.nextDouble() >= 0.35) {
            return;
        }
        double u = level.random.nextDouble();
        EntityType<? extends Mob> type = u < 0.4 ? ModEntities.WOLFSPIDER.get() : u < 0.7 ? ModEntities.HOUND.get() : ModEntities.SMILER.get();
        spawnInBand(level, p, site.rx, site.rz, y, player, type, 24, 48,
                q -> !level.getBlockState(q.below()).is(net.minecraft.world.level.block.Blocks.OAK_PLANKS));
    }

    /** 0.14.0: the Hotel. Hounds roam the corridors and the lobby (a Smiler now and then). */
    private static void hotel(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.Hotel.Site site = p.hotel.at(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        if (site == null) {
            return;
        }
        int lv = site.level(player.getBlockY());
        int y = site.yBase + lv * 6 + 1;
        AABB box = player.getBoundingBox().inflate(48, 4, 48);
        int roaming = level.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && !e.isPersistenceRequired()
                && (e instanceof Hound || e instanceof Smiler)).size();
        if (roaming >= 4 || level.random.nextDouble() >= 0.45) {
            return;
        }
        EntityType<? extends Mob> type = level.random.nextDouble() < 0.8 ? ModEntities.HOUND.get() : ModEntities.SMILER.get();
        spawnInBand(level, p, site.rx, site.rz, y, player, type, 20, 44, null);
    }

    /** 0.32.0: the Abandoned Cinema: Aranea Membri nests in its ceilings (0.41.1: at most one per cinema floor). */
    private static void cinema(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.Cinema.Site site = p.cinema.at(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        if (site == null || level.random.nextDouble() >= OfficeRoomsConfig.ARANEA_CINEMA_CHANCE.get()) {
            return;
        }
        int baseX = site.rx * p.regionBlocks;
        int baseZ = site.rz * p.regionBlocks;
        // 0.37.1: on the cinema floor of the player (three floors since 0.37.0); 0.41.1: one per floor instead of one per cinema
        int fl = Math.max(0, Math.min(de.officerooms.megastructure.Cinema.FLOORS - 1, (player.getBlockY() - site.yBase) / de.officerooms.megastructure.Cinema.FH));
        int fy = site.yBase + fl * de.officerooms.megastructure.Cinema.FH;
        AABB band = new AABB(baseX, fy, baseZ, baseX + p.regionBlocks, fy + de.officerooms.megastructure.Cinema.FH, baseZ + p.regionBlocks);
        if (level.getEntities(ModEntities.ARANEA.get(), band.inflate(8, 0, 8), e -> e.isAlive()).size() < 2) {     // 0.44.0: two per floor, closer
            spawnInBand(level, p, site.rx, site.rz, site.yBase + fl * de.officerooms.megastructure.Cinema.FH + 1, player, ModEntities.ARANEA.get(), 14, 40, null);
        }
    }

    /** 0.43.0: the Dead Mall: Aranea Membri nests in its ceilings, at most one per mall floor. */
    private static void mall(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.Mall.Site site = p.mall.at(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        if (site == null || level.random.nextDouble() >= OfficeRoomsConfig.ARANEA_MALL_CHANCE.get()) {
            return;
        }
        int baseX = site.rx * p.regionBlocks;
        int baseZ = site.rz * p.regionBlocks;
        int fh = de.officerooms.megastructure.Mall.FH;
        int fl = Math.max(0, Math.min(de.officerooms.megastructure.Mall.FLOORS - 1, (player.getBlockY() - site.yBase) / fh));
        int fy = site.yBase + fl * fh;
        AABB band = new AABB(baseX, fy, baseZ, baseX + p.regionBlocks, fy + fh, baseZ + p.regionBlocks);
        if (level.getEntities(ModEntities.ARANEA.get(), band.inflate(8, 0, 8), e -> e.isAlive()).size() < 2) {     // 0.44.0: two per floor, closer
            spawnInBand(level, p, site.rx, site.rz, fy + 1, player, ModEntities.ARANEA.get(), 14, 40, null);
        }
    }

    /** 0.16.0: Level Fun. Partygoers, always a few of them. */
    private static void levelFun(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.LevelFun.Site site = p.fun.at(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        if (site == null) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(48, 4, 48);
        int n = level.getEntitiesOfClass(Partygoer.class, box, e -> e.isAlive()).size();
        if (n < 6 && level.random.nextDouble() < 0.6) {
            spawnInBand(level, p, site.rx, site.rz, site.yBase + site.level(player.getBlockY()) * 6 + 1, player, ModEntities.PARTYGOER.get(), 18, 44, null);
        }
    }

    /** 0.15.0: the Industrial Complex. One Flayed per complex (it takes a while to appear) and roaming Hounds. */
    private static void industrial(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.Industrial.Site site = p.industrial.at(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        if (site == null) {
            return;
        }
        int y = site.yBase + site.level(player.getBlockY()) * 6 + 1;
        int baseX = site.rx * p.regionBlocks;
        int baseZ = site.rz * p.regionBlocks;
        AABB band = new AABB(baseX, site.yBase, baseZ, baseX + p.regionBlocks, site.yBase + site.h, baseZ + p.regionBlocks);
        if (level.random.nextDouble() < 0.2 && level.getEntities(ModEntities.FLAYED.get(), band.inflate(16, 2, 16), e -> e.isAlive()).isEmpty()) {
            spawnInBand(level, p, site.rx, site.rz, y, player, ModEntities.FLAYED.get(), 30, 64, null);
        }
        AABB box = player.getBoundingBox().inflate(48, 4, 48);
        int roaming = level.getEntitiesOfClass(Mob.class, box, e -> e.isAlive() && !e.isPersistenceRequired() && e instanceof Hound).size();
        if (roaming < 4 && level.random.nextDouble() < 0.4) {
            spawnInBand(level, p, site.rx, site.rz, y, player, ModEntities.HOUND.get(), 18, 44, null);
        }
    }

    /**
     * The Maze: exactly one Flayed and nothing else (0.27.0, user request; the classic creatures are gone). The
     * Flayed is counted only inside the Maze band itself, so a Flayed outside the Maze no longer blocks it.
     */
    private static void maze(ServerLevel level, OfficePlanner p, ServerPlayer player) {
        de.officerooms.megastructure.Maze.Site site = p.maze.at(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        if (site == null) {
            return;
        }
        int baseX = site.rx * p.regionBlocks;
        int baseZ = site.rz * p.regionBlocks;
        int lv = site.levelOf(player.getBlockY());                    // 0.34.0: spawn on the player's maze level
        int y = site.yBase + 1 + lv * de.officerooms.megastructure.Maze.LP;
        java.util.function.Predicate<BlockPos> open = q -> site.openFloor(lv, q.getX() - baseX, q.getZ() - baseZ);
        AABB band = new AABB(baseX, site.yBase, baseZ, baseX + p.regionBlocks, site.yBase + de.officerooms.megastructure.Maze.H, baseZ + p.regionBlocks);
        // other creatures that wandered or spawned into the Maze are removed (not persistent ones)
        for (Mob m : level.getEntitiesOfClass(Mob.class, band, e -> e.isAlive() && !e.isPersistenceRequired() && !(e instanceof Flayed)
                && (e instanceof Wolfspider || e instanceof Hound || e instanceof Smiler || e instanceof Gaunt || e instanceof HeadlessWarden
                || e instanceof Colony || e instanceof Hunter || e instanceof Partygoer || e instanceof Deathmoth))) {
            m.discard();
        }
        if (level.getEntities(ModEntities.FLAYED.get(), band, e -> e.isAlive()).isEmpty()) {
            for (int i = 0; i < 4 && level.getEntities(ModEntities.FLAYED.get(), band, e -> e.isAlive()).isEmpty(); i++) {
                spawnInBand(level, p, site.rx, site.rz, y, player, ModEntities.FLAYED.get(), 24, 72, open);
            }
        }
    }

    /** 0.27.0: true if a Maze lies in the player's region or a neighbouring one, on the player's storeys (±3). */
    private static boolean nearMaze(OfficePlanner p, ServerPlayer player) {
        int rx = p.regionOf(player.getBlockX());
        int rz = p.regionOf(player.getBlockZ());
        int ps = p.storeyOf(player.getBlockY());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                de.officerooms.megastructure.Maze.Site m = p.maze.site(rx + dx, rz + dz);
                if (m != null && ps >= m.s0 - 3 && ps <= m.s0 + de.officerooms.megastructure.Maze.STOREYS + 2) {
                    return true;
                }
            }
        }
        return false;
    }


    /** Spawns one creature on walk level {@code y} of a megastructure band, {@code min}–{@code max} blocks from the player. */
    private static void spawnInBand(ServerLevel level, OfficePlanner p, int rx, int rz, int y, ServerPlayer player,
                                    EntityType<? extends Mob> type, int min, int max, java.util.function.Predicate<BlockPos> where) {
        int baseX = rx * p.regionBlocks;
        int baseZ = rz * p.regionBlocks;
        for (int attempt = 0; attempt < 16; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double d = min + level.random.nextDouble() * (max - min);
            int x = (int) Math.floor(player.getX() + Math.cos(a) * d);
            int z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
            if (x < baseX + 3 || z < baseZ + 3 || x > baseX + p.regionBlocks - 4 || z > baseZ + p.regionBlocks - 4) {
                continue;
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isPositionEntityTicking(pos) || level.getNearestPlayer(x + 0.5, y, z + 0.5, Math.min(min, 20), false) != null) {
                continue;
            }
            if (!level.getBlockState(pos.below()).isSolid() || !level.getFluidState(pos).isEmpty()
                    || !level.noCollision(type.getAABB(x + 0.5, y, z + 0.5))) {
                continue;
            }
            if (where != null && !where.test(pos)) {
                continue;
            }
            Mob mob = type.create(level);
            if (mob == null) {
                return;
            }
            mob.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    private static void despawnFar(ServerLevel level) {
        for (Mob m : level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(Mob.class),
                e -> e instanceof Wolfspider || e instanceof HeadlessWarden || e instanceof FriendlyWolfspider || e instanceof Gaunt
                        || e instanceof Hound || e instanceof Smiler || e instanceof Colony || e instanceof Hunter || e instanceof Flayed || e instanceof Partygoer)) {
            // dauerhafte Mobs (Sculk-Raum-Warden, gezähmte Wolfspider) bleiben
            if (m.isPersistenceRequired() || (m instanceof FriendlyWolfspider f && f.isTame())) {
                continue;
            }
            if (m.getTarget() == null && level.getNearestPlayer(m, DESPAWN_DISTANCE) == null) {
                m.discard();
            }
        }
    }
}
