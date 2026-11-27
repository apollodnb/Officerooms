package de.officerooms.loot;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Registrierung der eigenen Loot-Bedingung. */
public final class ModLoot {

    public static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, OfficeRoomsIds.MOD_ID);

    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> TIER_CHANCE = CONDITIONS.register("tier_chance",
            () -> new LootItemConditionType(new TierChance.JsonSerializer()));

    private ModLoot() {
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }
}
