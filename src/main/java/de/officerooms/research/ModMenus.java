package de.officerooms.research;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, OfficeRoomsIds.MOD_ID);

    public static final RegistryObject<MenuType<ResearchMenu>> RESEARCH = MENUS.register("research_table",
            () -> new MenuType<>(ResearchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}
