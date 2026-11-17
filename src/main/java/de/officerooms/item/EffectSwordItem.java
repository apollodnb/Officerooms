package de.officerooms.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Einzigartige Schwerter (0.7.0): normale, verzauberbare Schwerter, deren Treffer zusätzlich einen
 * Effekt auf das Ziel legen (Wither, Langsamkeit oder Gift).
 */
public final class EffectSwordItem extends SwordItem {

    private final Supplier<MobEffect> effect;
    private final int ticks;
    private final int amplifier;
    private final String descKey;

    public EffectSwordItem(Tier tier, int damage, float speed, Properties props, Supplier<MobEffect> effect, int ticks, int amplifier, String descKey) {
        super(tier, damage, speed, props);
        this.effect = effect;
        this.ticks = ticks;
        this.amplifier = amplifier;
        this.descKey = descKey;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(effect.get(), ticks, amplifier), attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
