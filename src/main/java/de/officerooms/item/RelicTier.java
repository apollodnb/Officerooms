package de.officerooms.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Werkstoff der einzigartigen Schwerter (0.7.0): zwischen Diamant und Netherite, gut verzauberbar. */
public final class RelicTier implements Tier {

    public static final RelicTier INSTANCE = new RelicTier();

    private RelicTier() {
    }

    @Override
    public int getUses() {
        return 1800;
    }

    @Override
    public float getSpeed() {
        return 8.5F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 3.5F;
    }

    @Override
    public int getLevel() {
        return 3;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.DIAMOND);
    }
}
