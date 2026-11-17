package de.officerooms.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Mandelwasser: normales Lebensmittel (Trinkanimation, nur bei Hunger trinkbar), stellt die
 * Hungerleiste vollständig wieder her (20 Punkte) und gibt die Glasflasche zurück. Keine Effekte.
 */
public final class AlmondWaterItem extends Item {

    public AlmondWaterItem(Properties props) {
        super(props);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean creative = entity instanceof Player p && p.getAbilities().instabuild;
        ItemStack rest = super.finishUsingItem(stack, level, entity);
        if (creative) {
            return rest;
        }
        if (rest.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (entity instanceof Player p && !p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
            p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
        return rest;
    }
}
