package de.officerooms.client;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import de.officerooms.entity.ModEntities;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Nur Client: Dimensionseffekte {@code office_rooms:office_rooms}, Modelle und Renderer der Mobs
 * und das zentrale Audiosystem ({@link ClientAudio}).
 */
@Mod.EventBusSubscriber(modid = OfficeRoomsIds.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ClientAudio.register();
        RoomReverb.register();                                     // 0.31.0
        PoolFx.register();
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(de.officerooms.research.ModMenus.RESEARCH.get(), ResearchScreen::new));
    }

    /** 0.24.0: Deathmoth wings over the screen. */
    @SubscribeEvent
    static void onOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("deathmoth", (gui, g, pt, w, h) -> DeathmothRenderer.renderOverlay(g, pt, w, h));
    }

    @SubscribeEvent
    static void onLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MobRenderers.WOLFSPIDER_LAYER, WolfspiderModel::createBodyLayer);
        event.registerLayerDefinition(MobRenderers.DREAD_WOLFSPIDER_LAYER, DreadWolfspiderModel::createBodyLayer);
        event.registerLayerDefinition(UnknownRenderer.LAYER, UnknownRenderer::createBodyLayer);
        event.registerLayerDefinition(DeathmothRenderer.LAYER, DeathmothRenderer::createBodyLayer);
    }

    @SubscribeEvent
    static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.THROWN_TOOL.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        event.registerEntityRenderer(ModEntities.WOLFSPIDER.get(), MobRenderers.WolfspiderRenderer::new);
        event.registerEntityRenderer(ModEntities.FRIENDLY_WOLFSPIDER.get(), MobRenderers.FriendlyWolfspiderRenderer::new);
        event.registerEntityRenderer(ModEntities.HEADLESS_WARDEN.get(), ctx -> new GeoMobRenderer<>(ctx, "headless_warden", 0.9F, 1.0F, false, false));
        event.registerBlockEntityRenderer(de.officerooms.block.ModBlocks.HAZARD_MONITOR_BE.get(), HazardMonitorRenderer::new);
        event.registerBlockEntityRenderer(de.officerooms.outpost.OutpostBlocks.MONITOR_BE.get(), OutpostMonitorRenderer::new);
        event.registerEntityRenderer(ModEntities.PARTYGOER.get(), ctx -> new GeoMobRenderer<>(ctx, "partygoer", 0.5F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.FLAYED.get(), ctx -> new GeoMobRenderer<>(ctx, "flayed", 0.7F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.UNKNOWN.get(), UnknownRenderer::new);
        event.registerEntityRenderer(ModEntities.DEATHMOTH.get(), DeathmothRenderer::new);
        event.registerEntityRenderer(ModEntities.GAUNT.get(), ctx -> new GeoMobRenderer<>(ctx, "gaunt", 0.6F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.OBSERVER.get(), ctx -> new GeoMobRenderer<>(ctx, "observer", 0.0F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.HOUND.get(), ctx -> new GeoMobRenderer<>(ctx, "hound", 0.5F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.ARANEA.get(), ctx -> new GeoMobRenderer<>(ctx, "aranea_membri", 1.2F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.MASCOT.get(), ctx -> new GeoMobRenderer<>(ctx, "mascot", 0.8F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.SMILER.get(), ctx -> new GeoMobRenderer<>(ctx, "smiler", 0.0F, 1.0F, false, false));
        event.registerEntityRenderer(ModEntities.HUNTER.get(), ctx -> new GeoMobRenderer<>(ctx, "hunter", 0.0F, 1.0F, false, true));
        event.registerEntityRenderer(ModEntities.COLONY.get(), ctx -> new GeoMobRenderer<>(ctx, "colony", 1.3F, 1.4F, false, false));
        // nach OfficeCraft (mods.toml: ordering AFTER), ersetzt dessen Möbel-Renderer durch eine Hülle
        OfficeCraftShaderCompat.register(event);
    }

    @SubscribeEvent
    static void onRegisterEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(new ResourceLocation(OfficeRoomsIds.MOD_ID, "office_rooms"), new OfficeRoomsSkyEffects());
        event.register(new ResourceLocation(OfficeRoomsIds.MOD_ID, "the_sea"), new de.officerooms.sea.SeaClientFx.Effects());
    }

    /** 0.19.0: the upside-down grass is tinted like grass. */
    @SubscribeEvent
    public static void onBlockColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
        event.register((state, getter, pos, tint) -> getter != null && pos != null
                ? net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(getter, pos)
                : net.minecraft.world.level.GrassColor.getDefaultColor(), de.officerooms.block.ModBlocks.UNDERSIDE_GRASS.get());
    }

    @SubscribeEvent
    public static void onItemColors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> net.minecraft.world.level.GrassColor.getDefaultColor(), de.officerooms.block.ModBlocks.UNDERSIDE_GRASS.get());
    }
}
