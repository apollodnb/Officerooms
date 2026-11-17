package de.officerooms.item;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Eigene Items (Phase 4): Mandelwasser, Glitched Ingot, Multidimensional Rod und die
 * Multidimensional-Ausrüstung (etwa 20 % stärker als Netherite).
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, OfficeRoomsIds.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OfficeRoomsIds.MOD_ID);

    /** Mandelwasser: füllt die Hungerleiste vollständig (20 Punkte), keine weiteren Effekte. */
    public static final RegistryObject<Item> ALMOND_WATER = ITEMS.register("almond_water",
            () -> new AlmondWaterItem(new Item.Properties().stacksTo(16)
                    .food(new FoodProperties.Builder().nutrition(20).saturationMod(0.6F).build())));

    public static final RegistryObject<Item> GLITCHED_INGOT = ITEMS.register("glitched_ingot",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> MULTIDIMENSIONAL_ROD = ITEMS.register("multidimensional_rod",
            () -> new Item(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // --- 0.7.0 ---------------------------------------------------------------------------------

    /** Extrem selten: verhindert einen Tod in den Office Rooms und bringt den Spieler in die Overworld. */
    public static final RegistryObject<Item> TOTEM_OF_MERCY = ITEMS.register("totem_of_mercy",
            () -> new TotemOfMercyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    /** Tötet beim Trinken sofort. */
    public static final RegistryObject<Item> LIQUID_PAIN = ITEMS.register("liquid_pain",
            () -> new LiquidPainItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** Einzigartige Schwerter: Wither, Langsamkeit, Gift. */
    public static final RegistryObject<Item> WITHERFANG = ITEMS.register("witherfang",
            () -> new EffectSwordItem(RelicTier.INSTANCE, 3, -2.4F, new Item.Properties().rarity(Rarity.RARE).fireResistant(),
                    () -> net.minecraft.world.effect.MobEffects.WITHER, 100, 1, "item.office_rooms.witherfang.desc"));
    public static final RegistryObject<Item> LEADEN_EDGE = ITEMS.register("leaden_edge",
            () -> new EffectSwordItem(RelicTier.INSTANCE, 3, -2.4F, new Item.Properties().rarity(Rarity.RARE),
                    () -> net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 2, "item.office_rooms.leaden_edge.desc"));
    public static final RegistryObject<Item> VENOMTONGUE = ITEMS.register("venomtongue",
            () -> new EffectSwordItem(RelicTier.INSTANCE, 3, -2.4F, new Item.Properties().rarity(Rarity.RARE),
                    () -> net.minecraft.world.effect.MobEffects.POISON, 100, 1, "item.office_rooms.venomtongue.desc"));

    // --- 0.10.1: QoL tools -----------------------------------------------------------------------
    public static final RegistryObject<Item> GLOWSTICK = ITEMS.register("glowstick",
            () -> new ThrowableToolItem(ThrowableToolItem.Kind.GLOWSTICK, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> EMERGENCY_FLARE = ITEMS.register("emergency_flare",
            () -> new ThrowableToolItem(ThrowableToolItem.Kind.FLARE, new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> NOISE_MAKER = ITEMS.register("noise_maker",
            () -> new ThrowableToolItem(ThrowableToolItem.Kind.NOISE, new Item.Properties().stacksTo(4).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BANDAGE = ITEMS.register("bandage",
            () -> new ToolItems.Bandage(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> CHALK = ITEMS.register("chalk",
            () -> new ToolItems.Chalk(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> INDUSTRIAL_FLASHLIGHT = ITEMS.register("industrial_flashlight",
            () -> new ToolItems.Flashlight(new Item.Properties().durability(480).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PORTABLE_BATTERY = ITEMS.register("portable_battery",
            () -> new ToolItems.Battery(new Item.Properties().durability(3).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> MASTER_KEY = ITEMS.register("master_key",
            () -> new ToolItems.MasterKey(new Item.Properties().durability(3).rarity(Rarity.EPIC)));

    // --- 0.8.0 ---------------------------------------------------------------------------------

    /** Slows nearby creatures for 4 seconds; very long cooldown. Rare office loot only. */
    public static final RegistryObject<Item> TEMPORAL_WATCH = ITEMS.register("temporal_watch",
            () -> new TemporalWatchItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** Signal grows the closer a creature comes; never tells which one. */
    public static final RegistryObject<Item> ANOMALY_DETECTOR = ITEMS.register("anomaly_detector",
            () -> new AnomalyDetectorItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    /** Level Access Cards: open security doors up to their level. */
    public static final RegistryObject<Item> ACCESS_CARD_1 = ITEMS.register("level_1_access_card",
            () -> new AccessCardItem(1, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ACCESS_CARD_2 = ITEMS.register("level_2_access_card",
            () -> new AccessCardItem(2, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> ACCESS_CARD_3 = ITEMS.register("level_3_access_card",
            () -> new AccessCardItem(3, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** 0.12.0: rare exploration loot for the Research Table. */
    public static final RegistryObject<Item> RESEARCH_FILE = ITEMS.register("research_file",
            () -> new ResearchFileItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));

    /** 0.29.0: currency of the vending machines (common loot). */
    public static final RegistryObject<Item> OFFICE_COIN = ITEMS.register("office_coin", () -> new Item(new Item.Properties()));
    /** 0.31.0: a torn ticket stub from the abandoned cinema (flavour and loot). */
    public static final RegistryObject<Item> TICKET_STUB = ITEMS.register("ticket_stub", () -> new Item(new Item.Properties()));

    /** 0.15.0: rare loot from the Industrial Complex. */
    public static final RegistryObject<Item> ELECTRONIC_COMPONENT = ITEMS.register("electronic_component",
            () -> new Item(new Item.Properties().stacksTo(64).rarity(Rarity.UNCOMMON)));

    // --- 0.17.0: Hazard Zone gear -------------------------------------------------------------------------
    public static final RegistryObject<Item> GAS_MASK = ITEMS.register("gas_mask",
            () -> new HazardHeadgear("gas_mask", true, false, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> NIGHT_VISION_GOGGLES = ITEMS.register("night_vision_goggles",
            () -> new HazardHeadgear("night_vision_goggles", false, true, new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> HAZARD_VISOR = ITEMS.register("hazard_visor",
            () -> new HazardHeadgear("hazard_visor", true, true, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> GAS_FILTER = ITEMS.register("gas_filter",
            () -> new HazardItems.GasFilter(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> TOXIC_CRYSTAL = ITEMS.register("toxic_crystal",
            () -> new HazardItems.Described("item.office_rooms.toxic_crystal.desc", new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> EVENT_DETECTOR = ITEMS.register("special_event_detector",
            () -> new HazardItems.Described("item.office_rooms.special_event_detector.desc", new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    private static Item.Properties gear() {
        return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
    }

    // Werkzeuge: Angriffsschaden = 1 + Modifikator + Stufenbonus (4,6). Netherite: Schwert 8, Spitzhacke 6,
    // Axt 10, Schaufel 6,5. Multidimensional: Schwert 9,6, Spitzhacke 7,6, Axt 12, Schaufel 7,8, Hacke 1,6.
    public static final RegistryObject<Item> MD_SWORD = ITEMS.register("multidimensional_sword",
            () -> new SwordItem(MultidimensionalTier.TIER, 4, -2.4F, gear()));
    public static final RegistryObject<Item> MD_PICKAXE = ITEMS.register("multidimensional_pickaxe",
            () -> new PickaxeItem(MultidimensionalTier.TIER, 2, -2.8F, gear()));
    public static final RegistryObject<Item> MD_AXE = ITEMS.register("multidimensional_axe",
            () -> new AxeItem(MultidimensionalTier.TIER, 6.4F, -3.0F, gear()));
    public static final RegistryObject<Item> MD_SHOVEL = ITEMS.register("multidimensional_shovel",
            () -> new ShovelItem(MultidimensionalTier.TIER, 2.2F, -3.0F, gear()));
    public static final RegistryObject<Item> MD_HOE = ITEMS.register("multidimensional_hoe",
            () -> new HoeItem(MultidimensionalTier.TIER, -4, 0.0F, gear()));

    public static final RegistryObject<Item> MD_HELMET = ITEMS.register("multidimensional_helmet",
            () -> new ArmorItem(MultidimensionalArmor.INSTANCE, ArmorItem.Type.HELMET, gear()));
    public static final RegistryObject<Item> MD_CHESTPLATE = ITEMS.register("multidimensional_chestplate",
            () -> new ArmorItem(MultidimensionalArmor.INSTANCE, ArmorItem.Type.CHESTPLATE, gear()));
    public static final RegistryObject<Item> MD_LEGGINGS = ITEMS.register("multidimensional_leggings",
            () -> new ArmorItem(MultidimensionalArmor.INSTANCE, ArmorItem.Type.LEGGINGS, gear()));
    public static final RegistryObject<Item> MD_BOOTS = ITEMS.register("multidimensional_boots",
            () -> new ArmorItem(MultidimensionalArmor.INSTANCE, ArmorItem.Type.BOOTS, gear()));

    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
            new net.minecraft.resources.ResourceLocation(OfficeRoomsIds.MOD_ID, "main"));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.office_rooms"))
            .icon(() -> new ItemStack(GLITCHED_INGOT.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> it : ITEMS.getEntries()) {
                    out.accept(it.get());
                }
                out.accept(de.officerooms.block.ModBlocks.CEILING_PANEL_ITEM.get());
                out.accept(de.officerooms.block.ModBlocks.CEILING_STRIP_ITEM.get());
                out.accept(de.officerooms.entity.ModEntities.WOLFSPIDER_EGG.get());
                out.accept(de.officerooms.entity.ModEntities.FRIENDLY_WOLFSPIDER_EGG.get());
                out.accept(de.officerooms.entity.ModEntities.HEADLESS_WARDEN_EGG.get());
                out.accept(de.officerooms.entity.ModEntities.GAUNT_EGG.get());
                out.accept(de.officerooms.block.ModBlocks.MAZE_WALLPAPER_ITEM.get());
                out.accept(de.officerooms.block.ModBlocks.ELEVATOR_ITEM.get());
                out.accept(de.officerooms.block.ModBlocks.MAZE_CARPET_ITEM.get());
                out.accept(de.officerooms.block.ModBlocks.MAZE_CEILING_ITEM.get());
                out.accept(de.officerooms.block.ModBlocks.RESEARCH_TABLE_ITEM.get());
                for (RegistryObject<Item> it : de.officerooms.block.ModBlocks.DECOR_ITEMS) {
                    out.accept(it.get());
                }
                out.accept(de.officerooms.block.ModBlocks.PIPE_ITEM.get());
                out.accept(de.officerooms.block.ModBlocks.GRATE_DOOR_ITEM.get());
                out.accept(de.officerooms.block.ModBlocks.HAZARD_MONITOR_ITEM.get());
            })
            .build());

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
