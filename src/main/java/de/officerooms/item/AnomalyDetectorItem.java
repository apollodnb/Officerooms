package de.officerooms.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 0.8.0: Anomaly Detector. While held, it picks up the nearest creature within {@link #RANGE} blocks.
 * The closer it is, the stronger the signal on the action bar and the faster the beeping. It never
 * tells which creature it is.
 */
public class AnomalyDetectorItem extends Item {

    public static final double RANGE = 32.0;
    private static final int BARS = 12;

    public AnomalyDetectorItem(Properties props) {
        super(props);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        boolean held = selected || player.getOffhandItem() == stack;
        if (!held) {
            return;
        }
        long t = level.getGameTime();
        if (t % 5 != 0) {
            return;
        }
        double best = Double.MAX_VALUE;
        for (Mob m : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RANGE),
                e -> e.isAlive() && (e instanceof Enemy || e instanceof de.officerooms.entity.Observer))) {
            best = Math.min(best, m.distanceToSqr(player));
        }
        double d = Math.sqrt(best);
        double strength = d > RANGE ? 0.0 : 1.0 - d / RANGE;
        int bars = (int) Math.ceil(strength * BARS);
        ChatFormatting col = strength > 0.75 ? ChatFormatting.RED : strength > 0.45 ? ChatFormatting.GOLD
                : strength > 0.0 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY;
        MutableComponent msg = Component.literal("ANOMALY ").withStyle(ChatFormatting.GRAY);
        msg.append(Component.literal("|".repeat(bars)).withStyle(col, ChatFormatting.BOLD));
        msg.append(Component.literal("|".repeat(BARS - bars)).withStyle(ChatFormatting.DARK_GRAY));
        msg.append(Component.literal(strength > 0 ? "  SIGNAL " + Math.round(strength * 100) + "%" : "  no signal")
                .withStyle(strength > 0 ? col : ChatFormatting.DARK_GRAY));
        player.displayClientMessage(msg, true);
        if (strength > 0) {
            // beep interval 40 ticks (far) down to 5 ticks (very close)
            int interval = Math.max(5, (int) Math.round((1.0 - strength) * 8) * 5);
            if (t % interval == 0) {
                player.playNotifySound(SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.6F,
                        (float) (0.7 + strength * 1.1));
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.office_rooms.anomaly_detector.desc").withStyle(ChatFormatting.GRAY));
    }
}
