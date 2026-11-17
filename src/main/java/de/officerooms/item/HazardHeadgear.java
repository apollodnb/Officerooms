package de.officerooms.item;

import java.util.List;

import de.officerooms.core.OfficeRoomsIds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * 0.17.0: head gear. The Gas Mask protects against Hazard Zone gas while its filter lasts (10 minutes inside the gas;
 * replace with a Gas Filter). The Night Vision Goggles give night vision while their battery lasts (20 minutes;
 * recharge with a Portable Battery). The Hazard Visor does both. Charge and filter are kept in the item's NBT, the
 * items never break.
 */
public class HazardHeadgear extends ArmorItem {

    public static final int MAX_FILTER = 600;
    public static final int MAX_CHARGE = 1200;

    public final boolean gasMask;
    public final boolean nightVision;

    public HazardHeadgear(String name, boolean gasMask, boolean nightVision, Properties props) {
        super(new Material(name), Type.HELMET, props.stacksTo(1));
        this.gasMask = gasMask;
        this.nightVision = nightVision;
    }

    public static int filter(ItemStack s) {
        return MAX_FILTER - (s.hasTag() ? s.getTag().getInt("FilterUsed") : 0);
    }

    public static void useFilter(ItemStack s) {
        s.getOrCreateTag().putInt("FilterUsed", Math.min(MAX_FILTER, MAX_FILTER - filter(s) + 1));
    }

    public static void refill(ItemStack s) {
        s.getOrCreateTag().putInt("FilterUsed", 0);
    }

    public static int charge(ItemStack s) {
        return MAX_CHARGE - (s.hasTag() ? s.getTag().getInt("ChargeUsed") : 0);
    }

    public static void useCharge(ItemStack s) {
        s.getOrCreateTag().putInt("ChargeUsed", Math.min(MAX_CHARGE, MAX_CHARGE - charge(s) + 1));
    }

    public static void recharge(ItemStack s) {
        s.getOrCreateTag().putInt("ChargeUsed", 0);
    }

    /** The worn goggles/visor if they need charging, else EMPTY. */
    public static ItemStack chargeable(Player p) {
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        return head.getItem() instanceof HazardHeadgear h && h.nightVision && charge(head) < MAX_CHARGE ? head : ItemStack.EMPTY;
    }

    @Override
    public boolean isBarVisible(ItemStack s) {
        return gasMask ? filter(s) < MAX_FILTER : charge(s) < MAX_CHARGE;
    }

    @Override
    public int getBarWidth(ItemStack s) {
        return Math.round(13.0F * (gasMask ? filter(s) / (float) MAX_FILTER : charge(s) / (float) MAX_CHARGE));
    }

    @Override
    public int getBarColor(ItemStack s) {
        return gasMask ? 0x58C24A : 0x4AB8E8;
    }

    @Override
    public void appendHoverText(ItemStack s, Level level, List<Component> lines, TooltipFlag flag) {
        if (gasMask) {
            lines.add(Component.literal("Filter: " + filter(s) * 100 / MAX_FILTER + "% (lasts 10 min in toxic gas)").withStyle(ChatFormatting.GREEN));
        }
        if (nightVision) {
            lines.add(Component.literal("Battery: " + charge(s) * 100 / MAX_CHARGE + "% (20 min of night vision)").withStyle(ChatFormatting.AQUA));
        }
    }

    /** Armour material per item (own texture), 1 armour point, never breaks. */
    private record Material(String name) implements ArmorMaterial {
        @Override
        public int getDurabilityForType(Type type) {
            return 0;
        }

        @Override
        public int getDefenseForType(Type type) {
            return 1;
        }

        @Override
        public int getEnchantmentValue() {
            return 8;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }

        @Override
        public String getName() {
            return OfficeRoomsIds.MOD_ID + ":" + name;
        }

        @Override
        public float getToughness() {
            return 0;
        }

        @Override
        public float getKnockbackResistance() {
            return 0;
        }
    }
}
