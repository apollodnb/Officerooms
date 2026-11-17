package de.officerooms.core;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Zentrale Konfiguration für Mobs und Audio (Phase 3).
 *
 * <ul>
 *     <li>{@code config/office_rooms-common.toml}: Spawnraten, Schaden, Wither, Geschwindigkeit, Hörweiten.</li>
 *     <li>{@code config/office_rooms-client.toml}: Lautstärken von Lampenbrummen und Herzschlag.</li>
 * </ul>
 * <p>Die Generierung (Räume, Einrichtung, Licht) wird weiterhin im Datapack konfiguriert
 * ({@code data/office_rooms/dimension/office_rooms.json}).</p>
 */
public final class OfficeRoomsConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static ForgeConfigSpec.BooleanValue MEGA_HINTS;
    public static ForgeConfigSpec.IntValue MEGA_HINT_STOREYS;
    public static ForgeConfigSpec.BooleanValue LIGHT_FLICKER;
    public static ForgeConfigSpec.IntValue LIGHT_FLICKER_RADIUS;

    public enum ShaderCompat { AUTO, ALWAYS, OFF }

    public static ForgeConfigSpec.EnumValue<ShaderCompat> OFFICECRAFT_SHADER_COMPAT;
    /** 0.9.0: level of detail of OfficeCraft furniture drawn through the shader compatibility renderer. */
    public enum ShaderDetail { FULL, BALANCED, SIMPLE }
    public static ForgeConfigSpec.EnumValue<ShaderDetail> OFFICECRAFT_SHADER_DETAIL;
    public static ForgeConfigSpec.IntValue OFFICECRAFT_SHADER_FULL_DISTANCE;
    public static ForgeConfigSpec.IntValue OFFICECRAFT_SHADER_RENDER_DISTANCE;
    public static ForgeConfigSpec.BooleanValue OFFICECRAFT_SHADER_SHADOWS;

    // Spawns
    public static final ForgeConfigSpec.BooleanValue SPAWN_ENABLED;
    public static final ForgeConfigSpec.IntValue WOLFSPIDER_ROOMS_PER_MOB;
    public static final ForgeConfigSpec.IntValue WARDEN_ROOMS_PER_MOB;
    public static final ForgeConfigSpec.IntValue WARDEN_MAX_NEAR_PLAYER;
    public static final ForgeConfigSpec.IntValue SPAWN_MIN_DISTANCE;

    // Wolfspider
    public static final ForgeConfigSpec.DoubleValue WOLFSPIDER_HEALTH;
    public static final ForgeConfigSpec.DoubleValue WOLFSPIDER_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue WOLFSPIDER_SPEED;
    public static final ForgeConfigSpec.IntValue WOLFSPIDER_WITHER_SECONDS;
    public static final ForgeConfigSpec.IntValue WOLFSPIDER_WITHER_LEVEL;

    // Headless Warden
    public static final ForgeConfigSpec.DoubleValue WARDEN_HEALTH;
    public static final ForgeConfigSpec.DoubleValue WARDEN_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue WARDEN_SPEED;
    public static final ForgeConfigSpec.DoubleValue WARDEN_CHASE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WARDEN_HEARING;
    public static final ForgeConfigSpec.IntValue WARDEN_FORGET_SECONDS;
    public static final ForgeConfigSpec.DoubleValue WOLFSPIDER_FRIENDLY_CHANCE;
    public static final ForgeConfigSpec.DoubleValue WARDEN_INGOT_CHANCE;
    public static final ForgeConfigSpec.DoubleValue GAUNT_HEALTH;
    public static final ForgeConfigSpec.DoubleValue GAUNT_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue GAUNT_SPEED;
    public static final ForgeConfigSpec.IntValue GAUNT_ROOMS_PER_MOB;
    public static final ForgeConfigSpec.IntValue GAUNT_MAX_NEAR_PLAYER;
    public static final ForgeConfigSpec.IntValue GAUNT_ALERT_RADIUS;
    public static final ForgeConfigSpec.DoubleValue GAUNT_FOLLOW_RANGE;
    public static final ForgeConfigSpec.IntValue GAUNT_SENSE_RANGE;
    public static final ForgeConfigSpec.IntValue BALANCE_VERSION;
    public static final ForgeConfigSpec.DoubleValue WARDEN_SPAWN_CHANCE;
    public static final ForgeConfigSpec.BooleanValue FREEZE_FAR_MOBS;
    public static final ForgeConfigSpec.DoubleValue FREEZE_HORIZONTAL;
    public static final ForgeConfigSpec.DoubleValue FREEZE_VERTICAL;
    public static final ForgeConfigSpec.DoubleValue OBSERVER_CHANCE;
    // 0.6.0
    public static final ForgeConfigSpec.DoubleValue HOUND_HEALTH;
    public static final ForgeConfigSpec.DoubleValue HOUND_DAMAGE;
    public static final ForgeConfigSpec.IntValue HOUND_ROOMS_PER_MOB;
    public static final ForgeConfigSpec.IntValue HOUND_MAX_NEAR_PLAYER;
    public static final ForgeConfigSpec.IntValue SMILER_ROOMS_PER_MOB;
    public static final ForgeConfigSpec.IntValue SMILER_MAX_NEAR_PLAYER;
    public static final ForgeConfigSpec.IntValue SMILER_MAX_LIGHT;
    public static final ForgeConfigSpec.DoubleValue COLONY_HEALTH;
    public static final ForgeConfigSpec.DoubleValue COLONY_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue COLONY_SPEED;
    public static final ForgeConfigSpec.IntValue COLONY_ROOMS_PER_MOB;
    public static final ForgeConfigSpec.IntValue COLONY_CHASE_SECONDS;
    public static final ForgeConfigSpec.IntValue COLONY_FALTER_SECONDS;
    public static final ForgeConfigSpec.DoubleValue COLONY_SPAWN_CHANCE;
    public static final ForgeConfigSpec.BooleanValue BLACKOUT_ENABLED;
    public static final ForgeConfigSpec.IntValue BLACKOUT_FIRST_DELAY;
    public static final ForgeConfigSpec.IntValue BLACKOUT_PERIOD;
    public static final ForgeConfigSpec.IntValue BLACKOUT_WARNING;
    public static final ForgeConfigSpec.IntValue BLACKOUT_ACTIVE;
    public static final ForgeConfigSpec.IntValue BLACKOUT_RADIUS;
    public static final ForgeConfigSpec.IntValue BLACKOUT_STOREYS;
    public static final ForgeConfigSpec.IntValue BLACKOUT_GAUNTS;
    public static final ForgeConfigSpec.BooleanValue HAZARD_ENABLED;
    public static final ForgeConfigSpec.IntValue HAZARD_FIRST_DELAY;
    public static final ForgeConfigSpec.IntValue HAZARD_PERIOD;
    public static final ForgeConfigSpec.IntValue HAZARD_WARNING;
    public static final ForgeConfigSpec.IntValue HAZARD_ACTIVE;
    public static final ForgeConfigSpec.IntValue HAZARD_RADIUS;
    public static final ForgeConfigSpec.IntValue HAZARD_SEARCH_REGIONS;
    public static final ForgeConfigSpec.IntValue HAZARD_CHESTS;
    // 0.24.0: Deathmoth
    public static final ForgeConfigSpec.BooleanValue DEATHMOTH_ENABLED;
    public static final ForgeConfigSpec.IntValue DEATHMOTH_ROOMS_PER_MOB;
    public static final ForgeConfigSpec.IntValue DEATHMOTH_MAX_NEAR_PLAYER;
    public static final ForgeConfigSpec.DoubleValue DEATHMOTH_HEALTH;
    public static final ForgeConfigSpec.DoubleValue DEATHMOTH_BITE;
    public static final ForgeConfigSpec.DoubleValue DEATHMOTH_DRAIN;
    public static final ForgeConfigSpec.DoubleValue DEATHMOTH_ATTACH_SECONDS;
    public static final ForgeConfigSpec.IntValue DEATHMOTH_CHASE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue DEATHMOTH_RANGE;
    public static final ForgeConfigSpec.DoubleValue FLAYED_HEALTH;
    public static final ForgeConfigSpec.DoubleValue FLAYED_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ARANEA_HEALTH;
    public static final ForgeConfigSpec.DoubleValue ARANEA_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue ARANEA_SPAWN_CHANCE;
    public static final ForgeConfigSpec.DoubleValue ARANEA_CINEMA_CHANCE;
    public static final ForgeConfigSpec.BooleanValue INSTALL_QUESTS;
    public static final ForgeConfigSpec.DoubleValue FLAYED_CHASE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue FLAYED_CHASE_SECONDS;
    public static final ForgeConfigSpec.IntValue FLAYED_FALTER_SECONDS;
    public static final ForgeConfigSpec.DoubleValue FLAYED_SPAWN_CHANCE;
    public static final ForgeConfigSpec.DoubleValue HOUND_SPAWN_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SMILER_SPAWN_CHANCE;

    /** Qualitätsstufen der Loot-Tabellen (Reihenfolge der Werte in {@code [loot]}). */
    public static final java.util.List<String> LOOT_TIERS = java.util.List.of("common", "uncommon", "rare", "special", "multidimensional", "wood",
            "stories", "supplies");
    /** Standardchancen je Container und Stufe: common, uncommon, rare, special, multidimensional, wood. */
    private static final java.util.Map<String, java.util.List<Double>> LOOT_DEFAULTS = new java.util.LinkedHashMap<>();
    private static final java.util.Map<String, ForgeConfigSpec.ConfigValue<java.util.List<? extends Double>>> LOOT = new java.util.HashMap<>();

    /** 0.6.2: Spawn-Balance. Ältere Config-Dateien behalten sonst ihre alten Werte. */
    public static final int CURRENT_BALANCE = 6;
    /** 0.29.0: curiosity room chances (copied into SpecialRooms.CURIO on load). */
    public static final ForgeConfigSpec.DoubleValue[] CURIO = new ForgeConfigSpec.DoubleValue[6];

    public static void applyCurio() {
        for (int k = 0; k < CURIO.length; k++) {
            if (CURIO[k] != null) {
                de.officerooms.special.SpecialRooms.CURIO[k] = CURIO[k].get();
            }
        }
    }

    public static void applyBalance() {
        if (BALANCE_VERSION.get() >= CURRENT_BALANCE) {
            return;
        }
        if (BALANCE_VERSION.get() == 5) {
            // 0.41.1: Aranea Membri lurks more often in cinemas (only if still at the old default)
            if (Math.abs(ARANEA_CINEMA_CHANCE.get() - 0.04) < 1e-9) {
                ARANEA_CINEMA_CHANCE.set(ARANEA_CINEMA_CHANCE.getDefault());
            }
            BALANCE_VERSION.set(CURRENT_BALANCE);
            BALANCE_VERSION.save();
            de.officerooms.OfficeRooms.LOGGER.info("Office Rooms: balance 6 applied to the config.");
            return;
        }
        if (BALANCE_VERSION.get() == 4) {
            // 0.27.0: Gaunt and Colony are tougher (only if still at the old defaults)
            if (Math.abs(GAUNT_HEALTH.get() - 40.0) < 1e-9) {
                GAUNT_HEALTH.set(GAUNT_HEALTH.getDefault());
            }
            if (Math.abs(COLONY_HEALTH.get() - 50.0) < 1e-9) {
                COLONY_HEALTH.set(COLONY_HEALTH.getDefault());
            }
            BALANCE_VERSION.set(5);
            de.officerooms.OfficeRooms.LOGGER.info("Office Rooms: balance 5 applied to the config.");
            applyBalance();
            return;
        }
        if (BALANCE_VERSION.get() == 3) {
            // 0.25.0: bigger blackouts (only if still at the old defaults)
            if (BLACKOUT_RADIUS.get() == 48) {
                BLACKOUT_RADIUS.set(BLACKOUT_RADIUS.getDefault());
            }
            if (BLACKOUT_STOREYS.get() == 3) {
                BLACKOUT_STOREYS.set(BLACKOUT_STOREYS.getDefault());
            }
            BALANCE_VERSION.set(4);
            applyBalance();
            return;
        }
        if (BALANCE_VERSION.get() == 2) {
            // 0.24.0: only values still at their old defaults move (hounds a bit more often, wolfspiders rarer)
            if (WOLFSPIDER_ROOMS_PER_MOB.get() == 40) {
                WOLFSPIDER_ROOMS_PER_MOB.set(WOLFSPIDER_ROOMS_PER_MOB.getDefault());
            }
            if (HOUND_ROOMS_PER_MOB.get() == 60) {
                HOUND_ROOMS_PER_MOB.set(HOUND_ROOMS_PER_MOB.getDefault());
            }
            if (Math.abs(HOUND_SPAWN_CHANCE.get() - 0.15) < 1e-9) {
                HOUND_SPAWN_CHANCE.set(HOUND_SPAWN_CHANCE.getDefault());
            }
            BALANCE_VERSION.set(3);
            de.officerooms.OfficeRooms.LOGGER.info("Office Rooms: spawn balance 3 applied to the config.");
            applyBalance();
            return;
        }
        for (ForgeConfigSpec.ConfigValue<?> v : java.util.List.<ForgeConfigSpec.ConfigValue<?>>of(
                GAUNT_ROOMS_PER_MOB, GAUNT_MAX_NEAR_PLAYER, GAUNT_ALERT_RADIUS, GAUNT_FOLLOW_RANGE, GAUNT_SENSE_RANGE,
                HOUND_ROOMS_PER_MOB, HOUND_MAX_NEAR_PLAYER, HOUND_SPAWN_CHANCE,
                SMILER_ROOMS_PER_MOB, SMILER_MAX_NEAR_PLAYER, SMILER_MAX_LIGHT, SMILER_SPAWN_CHANCE, WARDEN_SPAWN_CHANCE)) {
            reset(v);
        }
        BALANCE_VERSION.set(CURRENT_BALANCE);
        BALANCE_VERSION.save();
        de.officerooms.OfficeRooms.LOGGER.info("Office Rooms: spawn balance {} applied to the config.", CURRENT_BALANCE);
    }

    private static <T> void reset(ForgeConfigSpec.ConfigValue<T> v) {
        v.set(v.getDefault());
    }

    static {
        // Normale Räume: nie Spezialstufe
        LOOT_DEFAULTS.put("standard_office", java.util.List.of(0.25, 0.04, 0.008, 0.0, 0.0, 0.0, 0.03, 0.0));
        LOOT_DEFAULTS.put("open_office", java.util.List.of(0.25, 0.04, 0.008, 0.0, 0.0, 0.0, 0.03, 0.0));
        LOOT_DEFAULTS.put("meeting_room", java.util.List.of(0.20, 0.04, 0.008, 0.0, 0.0, 0.0, 0.03, 0.0));
        LOOT_DEFAULTS.put("break_room", java.util.List.of(0.40, 0.03, 0.005, 0.0, 0.0, 0.0, 0.02, 0.03));
        LOOT_DEFAULTS.put("server_room", java.util.List.of(0.20, 0.07, 0.01, 0.0, 0.0, 0.0, 0.04, 0.02));
        LOOT_DEFAULTS.put("archive", java.util.List.of(0.20, 0.04, 0.015, 0.0, 0.0, 0.0, 0.10, 0.0));
        // 0.23.0: new standard rooms (only the executive office has containers by default)
        LOOT_DEFAULTS.put("executive_office", java.util.List.of(0.45, 0.12, 0.03, 0.0, 0.0, 0.0, 0.12, 0.0));
        LOOT_DEFAULTS.put("training_room", java.util.List.of(0.25, 0.04, 0.008, 0.0, 0.0, 0.0, 0.05, 0.0));
        LOOT_DEFAULTS.put("reception", java.util.List.of(0.30, 0.04, 0.008, 0.0, 0.0, 0.0, 0.05, 0.02));
        LOOT_DEFAULTS.put("restroom", java.util.List.of(0.20, 0.02, 0.005, 0.0, 0.0, 0.0, 0.02, 0.0));
        LOOT_DEFAULTS.put("corridor", java.util.List.of(0.20, 0.03, 0.005, 0.0, 0.0, 0.0, 0.02, 0.02));
        // Spezialräume
        LOOT_DEFAULTS.put("water_office", java.util.List.of(0.30, 0.10, 0.03, 0.006, 0.0, 0.0, 0.06, 0.0));
        LOOT_DEFAULTS.put("lush_cave", java.util.List.of(0.60, 0.15, 0.04, 0.02, 0.0, 0.6, 0.15, 0.1));
        LOOT_DEFAULTS.put("sculk_office", java.util.List.of(0.35, 0.12, 0.05, 0.015, 0.0, 0.0, 0.10, 0.0));
        LOOT_DEFAULTS.put("abandoned_camp", java.util.List.of(1.0, 0.6, 0.3, 0.06, 0.0, 0.5, 0.9, 1.0));
        LOOT_DEFAULTS.put("outpost", java.util.List.of(0.8, 0.2, 0.05, 0.0, 0.0, 0.3, 0.25, 0.6));
        LOOT_DEFAULTS.put("oasis", java.util.List.of(1.0, 1.0, 0.8, 0.6, 0.0, 0.3, 0.4, 0.0));
        LOOT_DEFAULTS.put("sculk_special", java.util.List.of(1.0, 0.85, 0.65, 0.40, 0.04, 0.0, 0.5, 0.0));
        LOOT_DEFAULTS.put("neighbourhood_house", java.util.List.of(1.0, 0.45, 0.15, 0.03, 0.005, 0.0, 0.6, 0.7));
    }

    /** Chance einer Loot-Stufe in einem Container dieses Raumtyps (0, wenn nicht eingetragen). */
    public static double lootChance(String room, String tier) {
        ForgeConfigSpec.ConfigValue<java.util.List<? extends Double>> v = LOOT.get(room);
        int i = LOOT_TIERS.indexOf(tier);
        if (v == null || i < 0) {
            return 0.0;
        }
        java.util.List<? extends Double> l;
        try {
            l = v.get();
        } catch (IllegalStateException notLoaded) {
            l = LOOT_DEFAULTS.get(room);
        }
        if (i >= l.size()) {
            // ältere Konfiguration ohne diese Stufe: Standardwert verwenden
            java.util.List<Double> def = LOOT_DEFAULTS.get(room);
            return def != null && i < def.size() ? def.get(i) : 0.0;
        }
        return Math.max(0.0, Math.min(1.0, l.get(i)));
    }

    // Client
    public static final ForgeConfigSpec.DoubleValue HUM_VOLUME;
    public static final ForgeConfigSpec.DoubleValue MUSIC_VOLUME;
    public static final ForgeConfigSpec.BooleanValue ROOM_REVERB;
    public static final ForgeConfigSpec.DoubleValue ROOM_REVERB_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue HEARTBEAT_VOLUME;
    public static final ForgeConfigSpec.IntValue HEARTBEAT_RANGE;
    public static final ForgeConfigSpec.IntValue MUSIC_INTERVAL_MINUTES;
    public static final ForgeConfigSpec.DoubleValue AMBIENT_VOLUME;
    public static final ForgeConfigSpec.IntValue AMBIENT_MIN_SECONDS;
    public static final ForgeConfigSpec.IntValue AMBIENT_MAX_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Atmosphere (0.9.0)").push("atmosphere");
        MEGA_HINTS = b.comment("Sounds of a megastructure (water, neon hum, barking) seep through the storeys above and below it")
                .define("megastructureHints", true);
        MEGA_HINT_STOREYS = b.comment("How many storeys above and below a megastructure its sounds can be heard")
                .defineInRange("megastructureHintStoreys", 8, 1, 30);
        LIGHT_FLICKER = b.comment("Ceiling lights close to a creature flicker as a warning").define("lightFlicker", true);
        LIGHT_FLICKER_RADIUS = b.comment("Creatures within this many blocks of a player make the lights around them flicker")
                .defineInRange("lightFlickerRadius", 14, 4, 32);
        b.pop();
        b.push("spawning");
        SPAWN_ENABLED = b.comment("Let creatures spawn in the Office Rooms dimension").define("enabled", true);
        WOLFSPIDER_ROOMS_PER_MOB = b.comment("On average one Wolfspider per this many rooms near a player")
                .defineInRange("wolfspiderRoomsPerMob", 70, 1, 100000);
        WOLFSPIDER_FRIENDLY_CHANCE = b.comment("Share of spawned Wolfspiders that are the friendly variant (0.05 = 5 %)")
                .defineInRange("friendlyWolfspiderChance", 0.05, 0.0, 1.0);
        WARDEN_ROOMS_PER_MOB = b.comment("On average one Headless Warden per this many rooms near a player")
                .defineInRange("wardenRoomsPerMob", 1500, 1, 1000000);
        WARDEN_MAX_NEAR_PLAYER = b.comment("At most this many Headless Wardens near one player at the same time")
                .defineInRange("wardenMaxNearPlayer", 1, 0, 16);
        WARDEN_SPAWN_CHANCE = b.comment("Chance per spawn check (every 3 seconds, per player) that a roaming Headless Warden may appear. 0.01 = roughly every 5 minutes. Sculk office guardians are not affected.")
                .defineInRange("wardenSpawnChance", 0.01, 0.0, 1.0);
        SPAWN_MIN_DISTANCE = b.comment("Minimum distance to the nearest player when spawning (blocks)")
                .defineInRange("minDistance", 20, 4, 128);
        FREEZE_FAR_MOBS = b.comment("Performance: creatures far away from every player (and not hunting) pause until a player comes near")
                .define("freezeFarMobs", true);
        FREEZE_HORIZONTAL = b.comment("Horizontal distance (blocks) from the nearest player beyond which creatures pause").defineInRange("freezeHorizontal", 64.0, 16.0, 512.0);
        FREEZE_VERTICAL = b.comment("Vertical distance (blocks) from the nearest player beyond which creatures pause (one storey = 6 blocks)").defineInRange("freezeVertical", 16.0, 6.0, 512.0);
        BALANCE_VERSION = b.comment("Internal: spawn balance already applied to this file. Set to 0 to reset all spawn values to the current defaults.")
                .defineInRange("balanceVersion", 0, 0, 1000);
        b.pop();

        b.push("wolfspider");
        WOLFSPIDER_HEALTH = b.defineInRange("health", 20.0, 1.0, 1024.0);
        WOLFSPIDER_DAMAGE = b.comment("Normal melee damage").defineInRange("damage", 4.0, 0.0, 1024.0);
        WOLFSPIDER_SPEED = b.defineInRange("speed", 0.32, 0.05, 2.0);
        WOLFSPIDER_WITHER_SECONDS = b.comment("Wither effect duration per hit in seconds (0 = off)")
                .defineInRange("witherSeconds", 6, 0, 600);
        WOLFSPIDER_WITHER_LEVEL = b.comment("Wither effect level (0 = Wither I)")
                .defineInRange("witherLevel", 0, 0, 9);
        b.pop();

        b.push("headless_warden");
        WARDEN_HEALTH = b.defineInRange("health", 160.0, 1.0, 4096.0);
        WARDEN_DAMAGE = b.comment("Damage per hit").defineInRange("damage", 30.0, 0.0, 4096.0);
        WARDEN_SPEED = b.comment("Base speed (vanilla Warden: 0.3)").defineInRange("speed", 0.42, 0.05, 2.0);
        WARDEN_CHASE_MULTIPLIER = b.comment("Speed multiplier while chasing")
                .defineInRange("chaseMultiplier", 1.6, 1.0, 4.0);
        WARDEN_HEARING = b.comment("Multiplier for all hearing ranges (1.0 = default)").defineInRange("hearingMultiplier", 1.0, 0.1, 5.0);
        WARDEN_FORGET_SECONDS = b.comment("The chase ends when the player stays quiet for this many seconds")
                .defineInRange("forgetSeconds", 4, 1, 120);
        WARDEN_INGOT_CHANCE = b.comment("Chance that a killed Headless Warden drops a Glitched Ingot (rolled once per death)")
                .defineInRange("glitchedIngotChance", 0.5, 0.0, 1.0);
        b.pop();
        b.push("gaunt");
        GAUNT_HEALTH = b.defineInRange("health", 60.0, 1.0, 4096.0);
        GAUNT_DAMAGE = b.defineInRange("damage", 9.0, 0.0, 4096.0);
        GAUNT_SPEED = b.comment("Base speed (it runs at 1.7x this while hunting)").defineInRange("speed", 0.38, 0.05, 2.0);
        GAUNT_ROOMS_PER_MOB = b.comment("Only in the five lowest (dark) levels: on average one Gaunt per this many rooms near a player")
                .defineInRange("roomsPerMob", 30, 1, 1000000);
        GAUNT_MAX_NEAR_PLAYER = b.comment("At most this many Gaunts near one player").defineInRange("maxNearPlayer", 6, 0, 16);
        GAUNT_ALERT_RADIUS = b.comment("When a Gaunt screams, every Gaunt within this many blocks joins the hunt (0 = off)").defineInRange("alertRadius", 40, 0, 128);
        GAUNT_SENSE_RANGE = b.comment("Gaunts sense players within this many blocks even through walls (0 = only by sight)").defineInRange("senseRange", 24, 0, 64);
        GAUNT_FOLLOW_RANGE = b.comment("How far a Gaunt keeps chasing its prey (blocks)").defineInRange("followRange", 64.0, 8.0, 128.0);
        b.pop();

        b.push("observer");
        OBSERVER_CHANCE = b.comment("Chance per player every 20 seconds that The Observer appears somewhere (0.02 = about every 17 minutes)")
                .defineInRange("chance", 0.02, 0.0, 1.0);
        b.pop();

        b.comment("The Hound: wolf body, human head. Leaves you alone while you hold mushroom stew.").push("hound");
        HOUND_HEALTH = b.defineInRange("health", 30.0, 1.0, 4096.0);
        HOUND_DAMAGE = b.defineInRange("damage", 12.0, 0.0, 4096.0);
        HOUND_ROOMS_PER_MOB = b.comment("On average one Hound per this many rooms near a player (whole dimension)").defineInRange("roomsPerMob", 45, 1, 100000);
        HOUND_MAX_NEAR_PLAYER = b.comment("At most this many Hounds near one player").defineInRange("maxNearPlayer", 2, 0, 16);
        HOUND_SPAWN_CHANCE = b.comment("Chance per spawn check (every 3 seconds, per player) that a Hound may appear. 0.2 = roughly every 15 seconds while below the limit").defineInRange("spawnChance", 0.2, 0.0, 1.0);
        b.pop();

        b.comment("The Smiler: stalks in the dark, kills on touch, vanishes when you hold a torch up to it.").push("smiler");
        SMILER_ROOMS_PER_MOB = b.comment("On average one Smiler per this many rooms near a player (only in dark spots)").defineInRange("roomsPerMob", 60, 1, 100000);
        SMILER_MAX_NEAR_PLAYER = b.comment("At most this many Smilers near one player").defineInRange("maxNearPlayer", 2, 0, 16);
        SMILER_MAX_LIGHT = b.comment("Only spawns where the block light is at most this (it fades away in light 10 or brighter)").defineInRange("maxLight", 7, 0, 15);
        SMILER_SPAWN_CHANCE = b.comment("Chance per spawn check (every 3 seconds, per player) that a Smiler may appear. 0.2 = roughly every 15 seconds while below the limit").defineInRange("spawnChance", 0.2, 0.0, 1.0);
        b.pop();

        b.comment("The Colony: extremely rare, hunts relentlessly, falters after a while of chasing.").push("colony");
        COLONY_HEALTH = b.defineInRange("health", 80.0, 1.0, 4096.0);
        COLONY_DAMAGE = b.defineInRange("damage", 14.0, 0.0, 4096.0);
        COLONY_SPEED = b.comment("Base speed (it chases at 1.6x this)").defineInRange("speed", 0.17, 0.05, 2.0);
        COLONY_ROOMS_PER_MOB = b.comment("On average one Colony per this many rooms near a player").defineInRange("roomsPerMob", 2500, 1, 1000000);
        COLONY_CHASE_SECONDS = b.comment("Seconds of continuous pursuit before it falters").defineInRange("chaseSeconds", 10, 1, 600);
        COLONY_FALTER_SECONDS = b.comment("Seconds it stays slow when it falters").defineInRange("falterSeconds", 3, 1, 60);
        COLONY_SPAWN_CHANCE = b.comment("Chance per spawn check (every 3 seconds, per player) that a Colony may appear. 0.0025 = roughly once per 20 minutes of exploring").defineInRange("spawnChance", 0.0025, 0.0, 1.0);
        b.pop();

        b.comment("HAZARD ZONE: a recurring toxic event in a random megastructure near the players (times in minutes).").push("hazard_zone");
        HAZARD_ENABLED = b.define("enabled", true);
        HAZARD_FIRST_DELAY = b.comment("Minutes after the first start of the world (with this mod) until the first warning").defineInRange("firstWarningAfter", 20, 0, 10000);
        HAZARD_PERIOD = b.comment("A new event cycle every N minutes (warning + active + cooldown)").defineInRange("cycleMinutes", 100, 10, 10000);
        HAZARD_WARNING = b.comment("Warning phase on the monitors before the zone activates").defineInRange("warningMinutes", 15, 1, 600);
        HAZARD_ACTIVE = b.comment("How long the zone stays active").defineInRange("activeMinutes", 45, 1, 600);
        HAZARD_RADIUS = b.comment("Horizontal half-size of the zone in blocks").defineInRange("radius", 44, 8, 76);
        HAZARD_SEARCH_REGIONS = b.comment("Search radius (in 160-block regions) for a megastructure around a random player").defineInRange("searchRegions", 8, 1, 40);
        HAZARD_CHESTS = b.comment("Hazard loot chests per event").defineInRange("chests", 8, 0, 40);
        b.pop();

        b.comment("BLACKOUT: the power fails in an area around a player; announced on the monitors (times in minutes).").push("blackout");
        BLACKOUT_ENABLED = b.define("enabled", true);
        BLACKOUT_FIRST_DELAY = b.comment("Minutes after the first start until the first announcement").defineInRange("firstWarningAfter", 30, 0, 10000);
        BLACKOUT_PERIOD = b.comment("Minutes from the end of one blackout to the next announcement").defineInRange("cycleMinutes", 60, 5, 10000);
        BLACKOUT_WARNING = b.comment("Announcement on the monitors before the lights go out").defineInRange("warningMinutes", 3, 1, 120);
        BLACKOUT_ACTIVE = b.comment("Minutes until the backup power comes on if nobody repairs the fuse box").defineInRange("maxMinutes", 20, 1, 600);
        BLACKOUT_RADIUS = b.comment("Horizontal half-size of the area in blocks").defineInRange("radius", 80, 8, 200);
        BLACKOUT_STOREYS = b.comment("Storeys above and below the chosen player's storey").defineInRange("storeys", 4, 0, 20);
        BLACKOUT_GAUNTS = b.comment("At most this many Gaunts come up in the dark at the same time").defineInRange("gaunts", 3, 0, 20);
        b.pop();

        b.comment("Deathmoth (0.24.0): sits on dark walls and ambushes. Never in the Maze, the Poolrooms, Level 0 or the Underside.").push("deathmoth");
        DEATHMOTH_ENABLED = b.define("enabled", true);
        DEATHMOTH_ROOMS_PER_MOB = b.comment("Office Rooms: on average one Deathmoth per this many rooms around a player, sitting on dark walls").defineInRange("roomsPerMob", 35, 1, 100000);
        DEATHMOTH_MAX_NEAR_PLAYER = b.comment("Office Rooms: at most this many Deathmoths around one player").defineInRange("maxNearPlayer", 3, 0, 16);
        DEATHMOTH_HEALTH = b.defineInRange("health", 16.0, 1.0, 1024.0);
        DEATHMOTH_BITE = b.comment("Damage when it reaches the face").defineInRange("biteDamage", 2.0, 0.0, 100.0);
        DEATHMOTH_DRAIN = b.comment("Damage every 0.6 seconds while it clings to the face").defineInRange("drainDamage", 1.5, 0.0, 100.0);
        DEATHMOTH_ATTACH_SECONDS = b.comment("Seconds it clings to the face before it lets go (hitting it ends this early)").defineInRange("attachSeconds", 3.5, 0.5, 30.0);
        DEATHMOTH_CHASE_SECONDS = b.comment("Seconds it flies after a player before giving up").defineInRange("chaseSeconds", 10, 1, 120);
        DEATHMOTH_RANGE = b.comment("Detection range in blocks (needs line of sight beyond 2.5 blocks)").defineInRange("detectionRange", 6.0, 1.0, 32.0);
        b.pop();

        b.comment("The Flayed: extremely rare. Screams when it sees you, sprints after you, falters after a while.").push("flayed");
        FLAYED_HEALTH = b.defineInRange("health", 80.0, 1.0, 4096.0);
        FLAYED_DAMAGE = b.defineInRange("damage", 18.0, 0.0, 4096.0);
        FLAYED_CHASE_MULTIPLIER = b.comment("Speed multiplier while chasing (1.75 = about 7.5 blocks per second; a sprinting player does 5.6)").defineInRange("chaseMultiplier", 1.75, 0.5, 5.0);
        FLAYED_CHASE_SECONDS = b.comment("Seconds of continuous pursuit before it falters").defineInRange("chaseSeconds", 10, 1, 600);
        FLAYED_FALTER_SECONDS = b.comment("Seconds it stays slow when it falters").defineInRange("falterSeconds", 3, 1, 60);
        FLAYED_SPAWN_CHANCE = b.comment("Chance per spawn check (every 3 seconds, per player) that a Flayed may appear. 0.0012 = roughly once per 40 minutes of exploring").defineInRange("spawnChance", 0.0012, 0.0, 1.0);
        b.pop();

        b.comment("0.32.0: Aranea Membri. Nests in the ceiling, drops on you when you walk underneath.").push("aranea_membri");
        ARANEA_HEALTH = b.defineInRange("health", 70.0, 1.0, 4096.0);
        ARANEA_DAMAGE = b.defineInRange("damage", 9.0, 0.0, 4096.0);
        ARANEA_SPAWN_CHANCE = b.comment("Chance per spawn check (every 3 seconds, per player) in the normal Office Rooms. 0.002 = rare")
                .defineInRange("spawnChance", 0.002, 0.0, 1.0);
        ARANEA_CINEMA_CHANCE = b.comment("Chance per spawn check while a player is inside an Abandoned Cinema (at most one per cinema floor)")
                .defineInRange("cinemaChance", 0.12, 0.0, 1.0);
        b.pop();

        b.comment("0.40.0: FTB Quests support").push("quests");
        INSTALL_QUESTS = b.comment("When FTB Quests is installed, add the Office Rooms chapters to the quest book on start (config/ftbquests/quests).",
                        "Existing chapters of a modpack are never touched; only the office_rooms_* files and the Office Rooms chapter group",
                        "are added, and updated when a new version of the mod brings new quests. Set to false to manage the quests yourself.")
                .define("installQuests", true);
        b.pop();

        b.comment("0.29.0: chance per suitable room for the new special rooms (new worlds only). Order: arcade, rain room,",
                "cold storage, mail room, canteen, last meeting. Changing these in an explored world can leave half rooms.").push("curiosities");
        String[] cn = {"arcade", "rain_room", "cold_storage", "mail_room", "canteen", "last_meeting"};
        for (int k = 0; k < cn.length; k++) {
            CURIO[k] = b.defineInRange(cn[k], de.officerooms.special.SpecialRooms.CURIO[k], 0.0, 0.2);
        }
        b.pop();

        b.comment("Loot: chance per container opening for each quality tier, in this order",
                "[common, uncommon, rare, special, multidimensional, wood, stories, supplies]. Contents: data/office_rooms/loot_tables/tiers/*.json")
                .push("loot");
        for (java.util.Map.Entry<String, java.util.List<Double>> e : LOOT_DEFAULTS.entrySet()) {
            LOOT.put(e.getKey(), b.defineList(e.getKey(), e.getValue(), o -> o instanceof Double d && d >= 0 && d <= 1));
        }
        b.pop();
        COMMON_SPEC = b.build();

        ForgeConfigSpec.Builder c = new ForgeConfigSpec.Builder();
        c.push("audio");
        // 0.16.2: louder hum, much quieter music (new keys so existing configs pick up the new defaults)
        HUM_VOLUME = c.comment("Volume of the lamp hum (on top of the 'Ambient/Environment' slider)")
                .defineInRange("lampHumLevel", 0.85, 0.0, 1.0);
        MUSIC_VOLUME = c.comment("Volume of the Office Rooms music (on top of the 'Music' slider)")
                .defineInRange("musicVolume", 0.8, 0.0, 1.0); // 0.30.1: new key, much louder default (was musicLevel 0.2)
        ROOM_REVERB = c.comment("0.31.0: reverb by room size in the Office Rooms dimensions (needs OpenAL EFX; switches itself off if missing)")
                .define("roomReverb", true);
        ROOM_REVERB_STRENGTH = c.comment("Strength of the room reverb (0 = dry, 1 = default, up to 2)")
                .defineInRange("roomReverbStrength", 1.0, 0.0, 2.0);
        HEARTBEAT_VOLUME = c.comment("Maximum volume of the heartbeat near a Headless Warden")
                .defineInRange("heartbeatVolume", 1.0, 0.0, 1.0);
        HEARTBEAT_RANGE = c.comment("The heartbeat becomes audible from this distance (blocks)")
                .defineInRange("heartbeatRange", 40, 4, 128);
        MUSIC_INTERVAL_MINUTES = c.comment("Main music: one random track on entering, then about one every N minutes (randomised +-30%) while you stay")
                .defineInRange("musicIntervalMinutes", 30, 1, 600);
        AMBIENT_VOLUME = c.comment("Volume of sporadic ambient sounds (footsteps, doors, whispers...)")
                .defineInRange("ambientVolume", 0.6, 0.0, 1.0);
        AMBIENT_MIN_SECONDS = c.comment("Minimum pause between two ambient sounds").defineInRange("ambientMinSeconds", 45, 5, 3600);
        AMBIENT_MAX_SECONDS = c.comment("Maximum pause between two ambient sounds").defineInRange("ambientMaxSeconds", 150, 10, 7200);
        c.pop();
        c.comment("Compatibility").push("compat");
        OFFICECRAFT_SHADER_COMPAT = c.comment("OfficeCraft furniture is invisible with Oculus/Iris shader packs because OfficeCraft draws it with its own shader.",
                        "auto = Office Rooms redraws the furniture in a shader-friendly way only while a shader pack is active (experimental)",
                        "always = always use the shader-friendly drawing, off = always use OfficeCraft's own drawing")
                .defineEnum("officeCraftShaderCompat", ShaderCompat.AUTO);
        OFFICECRAFT_SHADER_DETAIL = c.comment("Detail of OfficeCraft furniture while the shader compatibility renderer is active.",
                        "FULL = every triangle (slow with shader packs), BALANCED = full detail only close to the camera, simplified shapes further away,",
                        "SIMPLE = always simplified shapes (fastest).")
                .defineEnum("officeCraftShaderDetail", ShaderDetail.BALANCED);
        OFFICECRAFT_SHADER_FULL_DISTANCE = c.comment("BALANCED: furniture closer than this many blocks is drawn in full detail.")
                .defineInRange("officeCraftShaderFullDetailDistance", 8, 0, 64);
        OFFICECRAFT_SHADER_RENDER_DISTANCE = c.comment("Furniture further away than this many blocks is not drawn by the compatibility renderer.")
                .defineInRange("officeCraftShaderRenderDistance", 48, 8, 128);
        OFFICECRAFT_SHADER_SHADOWS = c.comment("Whether OfficeCraft furniture casts shadows with shader packs (false = much faster).")
                .define("officeCraftShaderShadows", false);
        c.pop();
        CLIENT_SPEC = c.build();
    }

    private OfficeRoomsConfig() {
    }
}
