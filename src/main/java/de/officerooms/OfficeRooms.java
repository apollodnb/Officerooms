package de.officerooms;

import de.officerooms.command.OfficeRoomsCommands;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Einstiegspunkt des Mods Office Rooms.
 *
 * <p>Dimension, Dimensionstyp und Biom kommen aus den Datapack-Registries
 * ({@code data/office_rooms/...}). Seit 0.1.0 verweist die Dimension auf den eigenen
 * Chunk-Generator {@code office_rooms:office_rooms}, der die prozedurale Büroarchitektur erzeugt.
 * Diese Klasse verbindet nur die Module mit den Event-Bussen.</p>
 */
@Mod(OfficeRoomsIds.MOD_ID)
public final class OfficeRooms {

    public static final Logger LOGGER = LoggerFactory.getLogger(OfficeRoomsIds.MOD_NAME);

    public OfficeRooms() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Module registrieren: Chunk-Generator-Codec (Mod-Bus) und Befehle (Forge-Bus).
        OfficeRoomsDimension.register(modEventBus);
        OfficeRoomsCommands.register();
        de.officerooms.core.ModSounds.register(modEventBus);
        de.officerooms.block.ModBlocks.register(modEventBus);
        de.officerooms.item.UndersideItems.init();
        de.officerooms.outpost.OutpostBlocks.init();
        de.officerooms.level0.Level0Blocks.init();
        de.officerooms.megastructure.CinemaBlocks.init();
        de.officerooms.megastructure.PoolBlocks.init();
        de.officerooms.block.RoomDecorBlocks.init();
        de.officerooms.block.CuriosityBlocks.init();
        de.officerooms.item.ModItems.register(modEventBus);
        de.officerooms.research.ModMenus.register(modEventBus);
        de.officerooms.research.ResearchUpgrades.register();
        de.officerooms.hazard.HazardNet.register();
        de.officerooms.hazard.HazardEvents.register();
        de.officerooms.underside.UndersideEvents.register();
        de.officerooms.outpost.OutpostPlacer.register();
        de.officerooms.level0.Level0Events.register();
        de.officerooms.hazard.BlackoutEvents.register();
        de.officerooms.loot.ModLoot.register(modEventBus);
        de.officerooms.entity.ModEntities.register(modEventBus);
        de.officerooms.worldgen.shrine.ShrineStructures.register(modEventBus);
        de.officerooms.entity.NoiseSystem.register();
        de.officerooms.entity.MobSpawner.register();
        de.officerooms.entity.TickThrottle.register();
        de.officerooms.world.Atmosphere.register();
        de.officerooms.world.Elevators.register();
        de.officerooms.world.ElevatorRides.register();
        de.officerooms.world.SlideTravel.register();
        de.officerooms.world.OfficeSleep.register();
        de.officerooms.world.ToolEffects.register();
        de.officerooms.item.TotemOfMercyItem.register();
        de.officerooms.entity.ObserverDirector.register();
        de.officerooms.entity.DeathmothDirector.register();
        de.officerooms.quest.Milestones.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON,
                de.officerooms.core.OfficeRoomsConfig.COMMON_SPEC);
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
                de.officerooms.core.OfficeRoomsConfig.CLIENT_SPEC);
        de.officerooms.worldgen.FurnishingTicker.register();
        de.officerooms.block.PortalIgnition.register();

        modEventBus.addListener(this::onCommonSetup);
        // 0.6.2: neue Spawn-Balance einmalig in bestehende Config-Dateien übernehmen
        modEventBus.addListener((net.minecraftforge.fml.event.config.ModConfigEvent.Loading e) -> {
            if (e.getConfig().getSpec() == de.officerooms.core.OfficeRoomsConfig.COMMON_SPEC) {
                de.officerooms.core.OfficeRoomsConfig.applyBalance();
                de.officerooms.core.OfficeRoomsConfig.applyCurio();
            }
        });

        LOGGER.info("{} {} is loading.", OfficeRoomsIds.MOD_NAME, OfficeRoomsIds.version());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Office-Rooms-Dimension: {} (Dimensionstyp {}, Bauhoehe Y {} bis {}).",
                OfficeRoomsDimension.DIMENSION_ID,
                OfficeRoomsDimension.DIMENSION_TYPE_ID,
                OfficeRoomsDimension.MIN_Y,
                OfficeRoomsDimension.MAX_BUILD_Y);
        // OfficeCraft-Formcaches einmalig auf dem Haupt-Thread füllen (Thread-Sicherheitsfehler im Mod umgehen)
        event.enqueueWork(de.officerooms.compat.OfficeCraftCompat::warmUpCaches);
        event.enqueueWork(de.officerooms.item.MultidimensionalTier::register);
        de.officerooms.quest.QuestInstaller.install();   // 0.40.0: before any server loads its quest book
    }
}
