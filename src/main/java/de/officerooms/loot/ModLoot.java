package de.officerooms.loot;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Registrierung der eigenen Loot-Bedingung. */
public final class ModLoot {

    public static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, OfficeRoomsIds.MOD_ID);

    public static final RegistryObject<LootItemConditionType> TIER_CHANCE = CONDITIONS.register("tier_chance",
            () -> new LootItemConditionType(new TierChance.JsonSerializer()));

    private ModLoot() {
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
