package de.officerooms.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 0.8.0: The Temporal Watch. Slows every hostile creature within {@link #RADIUS} blocks to a crawl for
 * 4 seconds. Very long cooldown. Only found in rare office loot.
 */
public class TemporalWatchItem extends Item {

    public static final double RADIUS = 8.0;
    public static final int DURATION = 80;
    public static final int COOLDOWN = 2400;

    public TemporalWatchItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            int n = 0;
            for (Mob m : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(RADIUS),
                    e -> e.isAlive() && (e instanceof Enemy || e instanceof de.officerooms.entity.Observer)
                            && e.distanceToSqr(player) <= RADIUS * RADIUS)) {
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DURATION, 5, false, true));
                m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DURATION, 1, false, true));
                m.getNavigation().stop();
                m.setDeltaMovement(m.getDeltaMovement().multiply(0.1, 1.0, 0.1));
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, m.getX(), m.getY(0.5), m.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
                n++;
            }
            sl.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.0), player.getZ(), 80, RADIUS / 2, 0.5, RADIUS / 2, 0.5);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            level.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 0.7F, 1.8F);
            player.displayClientMessage(Component.literal(n == 0 ? "Time slows... but nothing is near."
                    : "Time slows for " + n + (n == 1 ? " creature." : " creatures.")).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.office_rooms.temporal_watch.desc").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.office_rooms.temporal_watch.cooldown").withStyle(ChatFormatting.DARK_GRAY));
    }
}
