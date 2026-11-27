package de.officerooms.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Sound-Events (Phase 3). Dateien und Varianten stehen in {@code assets/office_rooms/sounds.json}. */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, OfficeRoomsIds.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC = reg("music.office_rooms");
    /** 0.30.0: own music while the player is in Level Fun. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_LEVEL_FUN = reg("music.level_fun");
    /** 0.30.1: the full recordings, looped on the client while a Flayed / Hound hunts. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_HUNT_LOOP = reg("entity.flayed.hunt_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUND_HUNT_LOOP = reg("entity.hound.hunt_loop");
    /** 0.31.0: the projector still rattling in an empty auditorium. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CINEMA_PROJECTOR = reg("block.cinema.projector");
    /** 0.43.0: the Dead Mall. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MALL_MUZAK = reg("block.mall.muzak");
    public static final DeferredHolder<SoundEvent, SoundEvent> MALL_CHIME = reg("block.mall.chime");
    /** 0.32.0: Aranea Membri. */
    /** 0.35.0: riding the slide from an office into the Poolrooms. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SLIDE_RUSH = reg("block.slide.rush");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLIDE_SPLASH = reg("block.slide.splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARANEA_SCRATCH = reg("entity.aranea_membri.scratch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARANEA_SCREECH = reg("entity.aranea_membri.screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARANEA_AMBIENT = reg("entity.aranea_membri.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARANEA_HURT = reg("entity.aranea_membri.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARANEA_DEATH = reg("entity.aranea_membri.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> ARANEA_STEP = reg("entity.aranea_membri.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> MASCOT_SQUEAK = reg("entity.mascot.squeak");
    public static final DeferredHolder<SoundEvent, SoundEvent> MASCOT_GIGGLE = reg("entity.mascot.giggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MASCOT_HONK = reg("entity.mascot.honk");
    public static final DeferredHolder<SoundEvent, SoundEvent> MASCOT_HURT = reg("entity.mascot.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MASCOT_DEATH = reg("entity.mascot.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MASCOT_STEP = reg("entity.mascot.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIP_HORN = reg("ambient.ship.horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHIP_AMBIENT = reg("ambient.ship.hull");
    public static final DeferredHolder<SoundEvent, SoundEvent> LIFEBOAT_DROP = reg("block.ship_lifeboat.drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMP_HUM = reg("ambient.lamp_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLFSPIDER_HOWL = reg("entity.wolfspider.howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLFSPIDER_HURT = reg("entity.wolfspider.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLFSPIDER_DEATH = reg("entity.wolfspider.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARDEN_SCREAM = reg("entity.headless_warden.scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARDEN_HEARTBEAT = reg("entity.headless_warden.heartbeat");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARDEN_HURT = reg("entity.headless_warden.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARDEN_DEATH = reg("entity.headless_warden.death");

    // Liminal-World-Update: sporadische Umgebungsgeräusche und neue Wesen
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_STEPS = reg("ambient.liminal.steps");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_KNOCK = reg("ambient.liminal.knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_DOOR = reg("ambient.liminal.door");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_NEON = reg("ambient.liminal.neon");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_METAL = reg("ambient.liminal.metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_CREAK = reg("ambient.liminal.creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_VOICES = reg("ambient.liminal.voices");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_WHISPER = reg("ambient.liminal.whisper");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_WIND = reg("ambient.liminal.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_BEHIND = reg("ambient.liminal.behind");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_DRIP = reg("ambient.liminal.drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_RUSTLE = reg("ambient.liminal.rustle");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_RUMBLE = reg("ambient.liminal.rumble");
    // 0.6.0: Poolrooms-Atmosphäre und neue Wesen
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_POOL_LAP = reg("ambient.pool.lap");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_POOL_SPLASH = reg("ambient.pool.splash");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_POOL_DRIP = reg("ambient.pool.drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_POOL_PUMP = reg("ambient.pool.pump");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_POOL_ECHO = reg("ambient.pool.echo");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUND_GROWL = reg("entity.hound.growl");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUND_BARK = reg("entity.hound.bark");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUND_HURT = reg("entity.hound.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUND_DEATH = reg("entity.hound.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOUND_SNIFF = reg("entity.hound.sniff");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMILER_GIGGLE = reg("entity.smiler.giggle");
    // 0.7.0: The Hunter
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTER_AMBIENT = reg("entity.hunter.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTER_NOTICE = reg("entity.hunter.notice");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTER_HURT = reg("entity.hunter.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTER_DEATH = reg("entity.hunter.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMILER_VANISH = reg("entity.smiler.vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMILER_KILL = reg("entity.smiler.kill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMILER_BREATH = reg("entity.smiler.breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_SCREAM = reg("entity.colony.scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_CHITTER = reg("entity.colony.chitter");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_HURT = reg("entity.colony.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_DEATH = reg("entity.colony.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> COLONY_STEP = reg("entity.colony.step");
    // 0.18.0: sound overhaul
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLFSPIDER_STEP = reg("entity.wolfspider.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOLFSPIDER_ATTACK = reg("entity.wolfspider.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNTER_STEP = reg("entity.hunter.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUNT_STEP = reg("entity.gaunt.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_TEAR = reg("entity.flayed.tear");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_BUZZ = reg("ambient.liminal.buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_VENT = reg("ambient.liminal.vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_PIPE = reg("ambient.liminal.pipe");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_DISTANT = reg("ambient.liminal.distant");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_MACHINE = reg("ambient.industrial.machine");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_STEAM = reg("ambient.industrial.steam");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_LAB = reg("ambient.lab.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_HOTEL = reg("ambient.hotel.far");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_FUN_PARTY = reg("ambient.fun.party");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_FUN_MUSICBOX = reg("ambient.fun.musicbox");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMB_POOL_DISTANT = reg("ambient.pool.distant");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTYGOER_GIGGLE = reg("entity.partygoer.giggle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTYGOER_HORN = reg("entity.partygoer.horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTYGOER_LAUGH = reg("entity.partygoer.laugh");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTYGOER_HURT = reg("entity.partygoer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARTYGOER_DEATH = reg("entity.partygoer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_SCREAM = reg("entity.flayed.scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_AMBIENT = reg("entity.flayed.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_HURT = reg("entity.flayed.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_DEATH = reg("entity.flayed.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_FALTER = reg("entity.flayed.falter");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAYED_STEP = reg("entity.flayed.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUNT_SCREAM = reg("entity.gaunt.scream");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUNT_BREATH = reg("entity.gaunt.breath");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUNT_HURT = reg("entity.gaunt.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAUNT_DEATH = reg("entity.gaunt.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> OBSERVER_VANISH = reg("entity.observer.vanish");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATHMOTH_IDLE = reg("entity.deathmoth.idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATHMOTH_TRIGGER = reg("entity.deathmoth.trigger");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATHMOTH_FLAP = reg("entity.deathmoth.flap");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATHMOTH_ATTACK = reg("entity.deathmoth.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATHMOTH_HURT = reg("entity.deathmoth.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEATHMOTH_DEATH = reg("entity.deathmoth.death");

    private ModSounds() {
    }

    // 0.19.0: the Underside
    public static final DeferredHolder<SoundEvent, SoundEvent> UNKNOWN_ROAR = reg("entity.unknown.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNKNOWN_HURT = reg("entity.unknown.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNKNOWN_DEATH = reg("entity.unknown.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNKNOWN_BURROW = reg("entity.unknown.burrow");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNKNOWN_EMERGE = reg("entity.unknown.emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNDERSIDE_QUAKE = reg("ambient.underside.quake");
    public static final DeferredHolder<SoundEvent, SoundEvent> UNDERSIDE_WIND = reg("ambient.underside.wind");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEDROCK_CRACK = reg("item.underside_key.use");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOCLIP_ON = reg("item.noclip_sword.on");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOCLIP_OFF = reg("item.noclip_sword.off");

    // 0.20.0: research outposts
    public static final DeferredHolder<SoundEvent, SoundEvent> OUTPOST_LEVER = reg("block.outpost.lever");
    public static final DeferredHolder<SoundEvent, SoundEvent> OUTPOST_POWER_ON = reg("block.outpost.power_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> OUTPOST_POWER_OFF = reg("block.outpost.power_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> OUTPOST_ALARM = reg("block.outpost.alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> OUTPOST_BEEP = reg("block.outpost.beep");
    public static final DeferredHolder<SoundEvent, SoundEvent> OUTPOST_SPARK = reg("block.outpost.spark");
    public static final DeferredHolder<SoundEvent, SoundEvent> OUTPOST_BUTTON = reg("block.outpost.button");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_CHARGE = reg("block.gateway.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_OPEN = reg("block.gateway.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> GATEWAY_HUM = reg("block.gateway.hum");

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(OfficeRoomsIds.MOD_ID, name)));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }
}
