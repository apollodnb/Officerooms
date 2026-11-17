package de.officerooms.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/**
 * Loot-Bedingung {@code office_rooms:tier_chance}: Ein Pool (eine Qualitätsstufe) wird mit der Chance
 * gewürfelt, die in {@code office_rooms-common.toml} unter {@code [loot]} für Raumtyp und Stufe
 * eingetragen ist. Damit stehen alle Loot-Wahrscheinlichkeiten zentral an einer Stelle.
 */
public record TierChance(String room, String tier) implements LootItemCondition {

    @Override
    public LootItemConditionType getType() {
        return ModLoot.TIER_CHANCE.get();
    }

    @Override
    public boolean test(LootContext ctx) {
        return ctx.getRandom().nextDouble() < OfficeRoomsConfig.lootChance(room, tier);
    }

    public static final class JsonSerializer implements Serializer<TierChance> {
        @Override
        public void serialize(JsonObject json, TierChance c, JsonSerializationContext ctx) {
            json.addProperty("room", c.room);
            json.addProperty("tier", c.tier);
        }

        @Override
        public TierChance deserialize(JsonObject json, JsonDeserializationContext ctx) {
            return new TierChance(GsonHelper.getAsString(json, "room"), GsonHelper.getAsString(json, "tier"));
        }
    }
}
