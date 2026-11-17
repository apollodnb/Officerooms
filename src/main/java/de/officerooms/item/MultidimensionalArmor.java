package de.officerooms.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Rüstungsmaterial „Multidimensional“: etwa 20 % über Netherite.
 *
 * <pre>
 *                     Netherite        Multidimensional
 * Rüstungspunkte      3 / 8 / 6 / 3    4 / 9 / 7 / 4   (Summe 20 → 24)
 * Härte               3,0              3,6
 * Rückstoßresistenz   0,1              0,12
 * Haltbarkeitsfaktor  37               44
 * Verzauberbar        15               18
 * </pre>
 *
 * Texturen: {@code office_rooms:textures/models/armor/multidimensional_layer_1/2.png} (eigene).
 */
public final class MultidimensionalArmor implements ArmorMaterial {

    public static final MultidimensionalArmor INSTANCE = new MultidimensionalArmor();

    private MultidimensionalArmor() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        // Vanilla-Grundwerte je Teil (Helm 11, Brust 16, Hose 15, Stiefel 13) × 44 (Netherite: × 37)
        int base = switch (type) {
            case HELMET -> 11;
            case CHESTPLATE -> 16;
            case LEGGINGS -> 15;
            case BOOTS -> 13;
        };
        return base * 44;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 4;
            case CHESTPLATE -> 9;
            case LEGGINGS -> 7;
            case BOOTS -> 4;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.GLITCHED_INGOT.get());
    }

    @Override
    public String getName() {
        return "office_rooms:multidimensional";
    }

    @Override
    public float getToughness() {
        return 3.6F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.12F;
    }
}
