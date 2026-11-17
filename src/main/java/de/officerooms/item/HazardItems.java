package de.officerooms.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 0.17.0: small Hazard Zone items. */
public final class HazardItems {

    private HazardItems() {
    }

    /** Item with a fixed grey tooltip line. */
    public static class Described extends Item {
        private final String key;

        public Described(String key, Properties p) {
            super(p);
            this.key = key;
        }

        @Override
        public void appendHoverText(ItemStack s, Level level, List<Component> lines, TooltipFlag flag) {
            lines.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Gas Filter: right-click to replace the filter of the worn gas mask or visor. */
    public static class GasFilter extends Described {
        public GasFilter(Properties p) {
            super("item.office_rooms.gas_filter.desc", p);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack s = player.getItemInHand(hand);
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!(head.getItem() instanceof HazardHeadgear h) || !h.gasMask || HazardHeadgear.filter(head) == HazardHeadgear.MAX_FILTER) {
                player.displayClientMessage(Component.literal("Wear a gas mask with a used filter to replace it.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(s);
            }
            if (!level.isClientSide) {
                HazardHeadgear.refill(head);
                if (!player.getAbilities().instabuild) {
                    s.shrink(1);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_CHAIN, SoundSource.PLAYERS, 1.0F, 1.3F);
                player.displayClientMessage(Component.literal("New filter fitted.").withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
        }
    }
}
