package de.officerooms.item;

import de.officerooms.entity.ThrownTool;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 0.10.1: Glowstick, Emergency Flare and Noise Maker: thrown, they act where they land. */
public class ThrowableToolItem extends Item {

    public enum Kind { GLOWSTICK, FLARE, NOISE }

    public final Kind kind;

    public ThrowableToolItem(Kind kind, Properties props) {
        super(props);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.6F);
        if (!level.isClientSide) {
            ThrownTool t = new ThrownTool(level, player);
            ItemStack one = stack.copy();
            one.setCount(1);
            t.setItem(one);
            t.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, kind == Kind.GLOWSTICK ? 1.1F : 0.9F, 1.0F);
            level.addFreshEntity(t);
        }
        player.getCooldowns().addCooldown(this, kind == Kind.GLOWSTICK ? 10 : 40);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
