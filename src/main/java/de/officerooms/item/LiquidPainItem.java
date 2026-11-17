package de.officerooms.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Liquid Pain (0.7.0): sieht aus wie ein Getränk und tötet beim Trinken sofort. Eigener Schadenstyp
 * {@code office_rooms:liquid_pain} (umgeht Rüstung, Effekte, Unverwundbarkeit und jeden Totem).
 */
public final class LiquidPainItem extends Item {

    public static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE,
            new ResourceLocation("office_rooms", "liquid_pain"));

    public LiquidPainItem(Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel server) {
            DamageSource src = new DamageSource(server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE));
            if (!(entity instanceof Player p && p.getAbilities().instabuild)) {
                stack.shrink(1);
            }
            entity.hurt(src, Float.MAX_VALUE);
            if (entity.isAlive()) {
                entity.kill(); // falls ein anderer Mod den Schaden abfängt
            }
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.office_rooms.liquid_pain.desc").withStyle(ChatFormatting.DARK_RED));
    }
}
