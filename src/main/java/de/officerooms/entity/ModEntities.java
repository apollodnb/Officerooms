package de.officerooms.entity;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registrierung der Mobs (Phase 3) und ihrer Spawn-Eier (für Tests im Kreativmodus). */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, OfficeRoomsIds.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, OfficeRoomsIds.MOD_ID);

    /** 0,9 × 0,7 Blöcke: passt durch 1 Block breite Türöffnungen. */
    public static final DeferredHolder<EntityType<?>, EntityType<Wolfspider>> WOLFSPIDER = ENTITIES.register("wolfspider",
            () -> EntityType.Builder.of(Wolfspider::new, MobCategory.MONSTER).sized(0.95F, 0.95F).clientTrackingRange(10)
                    .build(OfficeRoomsIds.MOD_ID + ":wolfspider"));

    /** 0,9 × 2,2 Blöcke: kopflos, passt durch 3 Blöcke hohe Türöffnungen. */
    public static final DeferredHolder<EntityType<?>, EntityType<HeadlessWarden>> HEADLESS_WARDEN = ENTITIES.register("headless_warden",
            () -> EntityType.Builder.of(HeadlessWarden::new, MobCategory.MONSTER).sized(0.9F, 2.2F).clientTrackingRange(16)
                    .fireImmune().build(OfficeRoomsIds.MOD_ID + ":headless_warden"));

    /** Freundliche Variante (Phase 4): zähmbar, gleiche Größe. */
    public static final DeferredHolder<EntityType<?>, EntityType<FriendlyWolfspider>> FRIENDLY_WOLFSPIDER = ENTITIES.register("friendly_wolfspider",
            () -> EntityType.Builder.of(FriendlyWolfspider::new, MobCategory.CREATURE).sized(0.9F, 0.7F).clientTrackingRange(10)
                    .build(OfficeRoomsIds.MOD_ID + ":friendly_wolfspider"));

    /** The Gaunt: etwa Spielergröße (0,8 × 1,9), nur in den dunklen untersten Ebenen. */
    public static final DeferredHolder<EntityType<?>, EntityType<Gaunt>> GAUNT = ENTITIES.register("gaunt",
            () -> EntityType.Builder.of(Gaunt::new, MobCategory.MONSTER).sized(0.8F, 1.9F).clientTrackingRange(12)
                    .build(OfficeRoomsIds.MOD_ID + ":gaunt"));

    /** 0.13.0: The Flayed. Hitbox 0.7 x 2.9 so it still fits through office doors; the model is taller. */
    public static final DeferredHolder<EntityType<?>, EntityType<Flayed>> FLAYED = ENTITIES.register("flayed",
            () -> EntityType.Builder.of(Flayed::new, MobCategory.MONSTER).sized(0.7F, 2.9F).clientTrackingRange(12)
                    .build(OfficeRoomsIds.MOD_ID + ":flayed"));

    /** 0.19.0: [Unknown], boss of the Underside (head hitbox; the body segments are part entities). */
    public static final DeferredHolder<EntityType<?>, EntityType<de.officerooms.underside.Unknown>> UNKNOWN = ENTITIES.register("unknown",
            () -> EntityType.Builder.<de.officerooms.underside.Unknown>of(de.officerooms.underside.Unknown::new, MobCategory.MONSTER).sized(2.6F, 3.6F)
                    .clientTrackingRange(16).updateInterval(1).fireImmune().build(OfficeRoomsIds.MOD_ID + ":unknown"));

    /** 0.16.0: the Partygoer of Level Fun. */
    public static final DeferredHolder<EntityType<?>, EntityType<Partygoer>> PARTYGOER = ENTITIES.register("partygoer",
            () -> EntityType.Builder.of(Partygoer::new, MobCategory.MONSTER).sized(0.6F, 2.4F).clientTrackingRange(10)
                    .build(OfficeRoomsIds.MOD_ID + ":partygoer"));

    /** The Observer: groß und schlank (0,6 × 2,6), wird weit sichtbar übertragen. */
    public static final DeferredHolder<EntityType<?>, EntityType<Observer>> OBSERVER = ENTITIES.register("observer",
            () -> EntityType.Builder.of(Observer::new, MobCategory.MISC).sized(0.6F, 2.6F).clientTrackingRange(16)
                    .fireImmune().noSummon().build(OfficeRoomsIds.MOD_ID + ":observer"));

    /** 0.24.0: The Deathmoth (wingspan about 2 blocks; small hitbox so it fits through doors). */
    public static final DeferredHolder<EntityType<?>, EntityType<Deathmoth>> DEATHMOTH = ENTITIES.register("deathmoth",
            () -> EntityType.Builder.of(Deathmoth::new, MobCategory.MONSTER).sized(0.7F, 0.5F).clientTrackingRange(10)
                    .build(OfficeRoomsIds.MOD_ID + ":deathmoth"));

    /** 0.6.0: The Hound (wolfsgroß), The Smiler (Schattengestalt), The Colony (flache, breite Masse). */
    public static final DeferredHolder<EntityType<?>, EntityType<Hound>> HOUND = ENTITIES.register("hound",
            () -> EntityType.Builder.of(Hound::new, MobCategory.MONSTER).sized(0.8F, 1.0F).clientTrackingRange(10)
                    .build(OfficeRoomsIds.MOD_ID + ":hound"));
    public static final DeferredHolder<EntityType<?>, EntityType<Smiler>> SMILER = ENTITIES.register("smiler",
            () -> EntityType.Builder.of(Smiler::new, MobCategory.MONSTER).sized(0.8F, 2.0F).clientTrackingRange(10)
                    .fireImmune().build(OfficeRoomsIds.MOD_ID + ":smiler"));
    public static final DeferredHolder<EntityType<?>, EntityType<Colony>> COLONY = ENTITIES.register("colony",
            () -> EntityType.Builder.of(Colony::new, MobCategory.MONSTER).sized(2.0F, 1.4F).clientTrackingRange(12)
                    .build(OfficeRoomsIds.MOD_ID + ":colony"));

    /** 0.7.0: The Hunter – riesiges Linienwesen der Poolrooms. */
    /** 0.10.1: thrown glowstick, flare or noise maker. */
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownTool>> THROWN_TOOL = ENTITIES.register("thrown_tool",
            () -> EntityType.Builder.<ThrownTool>of(ThrownTool::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4)
                    .updateInterval(10).build(OfficeRoomsIds.MOD_ID + ":thrown_tool"));

    public static final DeferredHolder<EntityType<?>, EntityType<Hunter>> HUNTER = ENTITIES.register("hunter",
            () -> EntityType.Builder.of(Hunter::new, MobCategory.MONSTER).sized(1.0F, 3.6F).clientTrackingRange(12)
                    .fireImmune().build(OfficeRoomsIds.MOD_ID + ":hunter"));

    public static final DeferredHolder<Item, Item> HUNTER_EGG = ITEMS.register("hunter_spawn_egg",
            () -> new DeferredSpawnEggItem(HUNTER, 0x0A1418, 0xBFF4FF, new Item.Properties()));

    /** 0.32.0: Aranea Membri. Hitbox smaller than the model (the legs reach far out) so it fits through doors. */
    public static final DeferredHolder<EntityType<?>, EntityType<AraneaMembri>> ARANEA = ENTITIES.register("aranea_membri",
            () -> EntityType.Builder.of(AraneaMembri::new, MobCategory.MONSTER).sized(1.2F, 1.9F).clientTrackingRange(10)
                    .build("aranea_membri"));
    public static final DeferredHolder<Item, Item> ARANEA_EGG = ITEMS.register("aranea_membri_spawn_egg",
            () -> new DeferredSpawnEggItem(ARANEA, 0x101012, 0xCFC4A6, new Item.Properties()));

    /** 0.42.0: the Mascot of the SS Fun =). */
    public static final DeferredHolder<EntityType<?>, EntityType<de.officerooms.sea.Mascot>> MASCOT = ENTITIES.register("mascot",
            () -> EntityType.Builder.of(de.officerooms.sea.Mascot::new, MobCategory.MONSTER).sized(0.9F, 2.1F).clientTrackingRange(10)
                    .build("mascot"));
    public static final DeferredHolder<Item, Item> MASCOT_EGG = ITEMS.register("mascot_spawn_egg",
            () -> new DeferredSpawnEggItem(MASCOT, 0xC9B45A, 0x2A3A5C, new Item.Properties()));
    public static final DeferredHolder<Item, Item> HOUND_EGG = ITEMS.register("hound_spawn_egg",
            () -> new DeferredSpawnEggItem(HOUND, 0x2A1A14, 0x8A1010, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SMILER_EGG = ITEMS.register("smiler_spawn_egg",
            () -> new DeferredSpawnEggItem(SMILER, 0x050505, 0xF0F0E0, new Item.Properties()));
    public static final DeferredHolder<Item, Item> COLONY_EGG = ITEMS.register("colony_spawn_egg",
            () -> new DeferredSpawnEggItem(COLONY, 0xB8B79A, 0xE8E070, new Item.Properties()));

    public static final DeferredHolder<Item, Item> FLAYED_EGG = ITEMS.register("flayed_spawn_egg",
            () -> new DeferredSpawnEggItem(FLAYED, 0x0B0A0C, 0x3A3438, new Item.Properties()));
    public static final DeferredHolder<Item, Item> PARTYGOER_EGG = ITEMS.register("partygoer_spawn_egg",
            () -> new DeferredSpawnEggItem(PARTYGOER, 0xF2CE26, 0xEC4696, new Item.Properties()));
    public static final DeferredHolder<Item, Item> GAUNT_EGG = ITEMS.register("gaunt_spawn_egg",
            () -> new DeferredSpawnEggItem(GAUNT, 0x120808, 0xD01010, new Item.Properties()));

    public static final DeferredHolder<Item, Item> FRIENDLY_WOLFSPIDER_EGG = ITEMS.register("friendly_wolfspider_spawn_egg",
            () -> new DeferredSpawnEggItem(FRIENDLY_WOLFSPIDER, 0x0D0D0D, 0xF2F2F2, new Item.Properties()));

    public static final DeferredHolder<Item, Item> DEATHMOTH_EGG = ITEMS.register("deathmoth_spawn_egg",
            () -> new DeferredSpawnEggItem(DEATHMOTH, 0x4A3E31, 0x2A241D, new Item.Properties()));
    public static final DeferredHolder<Item, Item> WOLFSPIDER_EGG = ITEMS.register("wolfspider_spawn_egg",
            () -> new DeferredSpawnEggItem(WOLFSPIDER, 0x0D0D0D, 0x2A2A2A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> HEADLESS_WARDEN_EGG = ITEMS.register("headless_warden_spawn_egg",
            () -> new DeferredSpawnEggItem(HEADLESS_WARDEN, 0x0E2A33, 0x39D6E0, new Item.Properties()));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(ModEntities::onAttributes);
        modBus.addListener(ModEntities::onCreativeTab);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(WOLFSPIDER.get(), Wolfspider.createAttributes().build());
        event.put(HEADLESS_WARDEN.get(), HeadlessWarden.createAttributes().build());
        event.put(FRIENDLY_WOLFSPIDER.get(), FriendlyWolfspider.createAttributes().build());
        event.put(GAUNT.get(), Gaunt.createAttributes().build());
        event.put(FLAYED.get(), Flayed.createAttributes().build());
        event.put(UNKNOWN.get(), de.officerooms.underside.Unknown.createAttributes().build());
        event.put(PARTYGOER.get(), Partygoer.createAttributes().build());
        event.put(HOUND.get(), Hound.createAttributes().build());
        event.put(ARANEA.get(), AraneaMembri.createAttributes().build());
        event.put(MASCOT.get(), de.officerooms.sea.Mascot.createAttributes().build());
        event.put(SMILER.get(), Smiler.createAttributes().build());
        event.put(COLONY.get(), Colony.createAttributes().build());
        event.put(HUNTER.get(), Hunter.createAttributes().build());
        event.put(OBSERVER.get(), Observer.createAttributes().build());
        event.put(DEATHMOTH.get(), Deathmoth.createAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(WOLFSPIDER_EGG);
            event.accept(DEATHMOTH_EGG);
            event.accept(HEADLESS_WARDEN_EGG);
            event.accept(FRIENDLY_WOLFSPIDER_EGG);
            event.accept(GAUNT_EGG);
            event.accept(HOUND_EGG);
            event.accept(ARANEA_EGG);
            event.accept(MASCOT_EGG);
            event.accept(SMILER_EGG);
            event.accept(HUNTER_EGG);
            event.accept(COLONY_EGG);
            event.accept(FLAYED_EGG);
            event.accept(PARTYGOER_EGG);
        }
    }
}
