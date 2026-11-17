package de.officerooms.item;

import de.officerooms.block.ChalkMarkBlock;
import de.officerooms.block.CeilingLightBlock;
import de.officerooms.block.ModBlocks;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** 0.10.1: Bandage, Chalk, Industrial Flashlight, Portable Battery and Master Key. */
public final class ToolItems {

    private ToolItems() {
    }

    static void desc(Item item, List<Component> tip) {
        tip.add(Component.translatable(item.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }

    /** Takes 2 seconds to apply (no sprinting meanwhile), then heals slowly. */
    public static class Bandage extends Item {
        public Bandage(Properties p) {
            super(p);
        }

        @Override
        public int getUseDuration(ItemStack s) {
            return 40;
        }

        @Override
        public UseAnim getUseAnimation(ItemStack s) {
            return UseAnim.BOW;
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (player.getHealth() >= player.getMaxHealth()) {
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
            if (!level.isClientSide) {
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 0));
                level.playSound(null, e.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            if (!(e instanceof Player p && p.getAbilities().instabuild)) {
                stack.shrink(1);
            }
            return stack;
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            desc(this, tip);
        }
    }

    /** Draws arrows or a cross on walls; where on the wall you click decides the arrow. */
    public static class Chalk extends Item {
        public Chalk(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Direction face = ctx.getClickedFace();
            if (face.getAxis() == Direction.Axis.Y) {
                return InteractionResult.PASS;
            }
            Level level = ctx.getLevel();
            BlockPos clicked = ctx.getClickedPos();
            BlockState hitState = level.getBlockState(clicked);
            if (hitState.is(ModBlocks.CHALK_MARK.get())) {   // drawing over an existing mark replaces it
                face = hitState.getValue(ChalkMarkBlock.FACING);
                clicked = clicked.relative(face.getOpposite());
            }
            BlockPos pos = clicked.relative(face);
            BlockState here = level.getBlockState(pos);
            if (!here.isAir() && !here.is(ModBlocks.CHALK_MARK.get())) {
                return InteractionResult.FAIL;
            }
            Vec3 hit = ctx.getClickLocation().subtract(Vec3.atLowerCornerOf(clicked));
            Direction right = face.getOpposite().getClockWise();
            double r = (hit.x - 0.5) * right.getStepX() + (hit.z - 0.5) * right.getStepZ() + 0.5;
            double v = hit.y;
            ChalkMarkBlock.Mark mark = v > 0.68 ? ChalkMarkBlock.Mark.UP : v < 0.32 ? ChalkMarkBlock.Mark.DOWN
                    : r > 0.68 ? ChalkMarkBlock.Mark.RIGHT : r < 0.32 ? ChalkMarkBlock.Mark.LEFT : ChalkMarkBlock.Mark.CROSS;
            BlockState st = ModBlocks.CHALK_MARK.get().defaultBlockState().setValue(ChalkMarkBlock.FACING, face).setValue(ChalkMarkBlock.MARK, mark);
            if (!st.canSurvive(level, pos)) {
                return InteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                level.setBlock(pos, st, 3);
                level.playSound(null, pos, SoundEvents.CALCITE_HIT, SoundSource.BLOCKS, 0.6F, 1.6F);
                if (ctx.getPlayer() != null) {
                    ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), pl -> pl.broadcastBreakEvent(ctx.getHand()));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            desc(this, tip);
        }
    }

    /** Powerful beam of light; right-click toggles; drains its charge while on. */
    public static class Flashlight extends Item {
        public Flashlight(Properties p) {
            super(p);
        }

        public static boolean isOn(ItemStack s) {
            return s.getItem() instanceof Flashlight && s.hasTag() && s.getTag().getBoolean("On") && s.getDamageValue() < s.getMaxDamage();
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack s = player.getItemInHand(hand);
            if (s.getDamageValue() >= s.getMaxDamage()) {
                player.displayClientMessage(Component.literal("The flashlight is dead. Charge it with a Portable Battery.").withStyle(ChatFormatting.GRAY), true);
                s.getOrCreateTag().putBoolean("On", false);
                return InteractionResultHolder.fail(s);
            }
            boolean on = !s.getOrCreateTag().getBoolean("On");
            s.getOrCreateTag().putBoolean("On", on);
            level.playSound(null, player.blockPosition(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.5F, on ? 1.6F : 1.2F);
            return InteractionResultHolder.sidedSuccess(s, level.isClientSide);
        }

        @Override
        public boolean isFoil(ItemStack s) {
            return isOn(s);
        }

        @Override
        public boolean shouldCauseReequipAnimation(ItemStack a, ItemStack b, boolean slotChanged) {
            return slotChanged;
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            desc(this, tip);
            int pct = Math.round(100F * (s.getMaxDamage() - s.getDamageValue()) / s.getMaxDamage());
            tip.add(Component.literal("Charge: " + pct + " %").withStyle(pct > 20 ? ChatFormatting.AQUA : ChatFormatting.RED));
        }
    }

    /** Recharges a flashlight in the other hand; recharge it by draining a lit ceiling light. */
    public static class Battery extends Item {
        public Battery(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockState st = level.getBlockState(ctx.getClickedPos());
            ItemStack s = ctx.getItemInHand();
            if (!(st.getBlock() instanceof CeilingLightBlock) || !st.getValue(CeilingLightBlock.LIT) || s.getDamageValue() == 0) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide) {
                level.setBlock(ctx.getClickedPos(), st.setValue(CeilingLightBlock.LIT, false), 3);
                s.setDamageValue(0);
                level.playSound(null, ctx.getClickedPos(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.8F, 1.6F);
                if (ctx.getPlayer() != null) {
                    ctx.getPlayer().displayClientMessage(Component.literal("You drain the light. The battery is full.").withStyle(ChatFormatting.AQUA), true);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack bat = player.getItemInHand(hand);
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            ItemStack goggles = HazardHeadgear.chargeable(player);
            if (!(other.getItem() instanceof Flashlight) && !goggles.isEmpty()) {          // 0.17.0: charge worn goggles
                if (bat.getDamageValue() >= bat.getMaxDamage()) {
                    player.displayClientMessage(Component.literal("The battery is empty. Drain a ceiling light to recharge it.").withStyle(ChatFormatting.GRAY), true);
                    return InteractionResultHolder.fail(bat);
                }
                if (!level.isClientSide) {
                    HazardHeadgear.recharge(goggles);
                    bat.setDamageValue(bat.getDamageValue() + 1);
                    level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.8F);
                    player.displayClientMessage(Component.literal("Goggles charged.").withStyle(ChatFormatting.AQUA), true);
                }
                return InteractionResultHolder.sidedSuccess(bat, level.isClientSide);
            }
            if (!(other.getItem() instanceof Flashlight) || other.getDamageValue() == 0) {
                player.displayClientMessage(Component.literal("Hold a flashlight in the other hand to charge it.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(bat);
            }
            if (bat.getDamageValue() >= bat.getMaxDamage()) {
                player.displayClientMessage(Component.literal("The battery is empty. Drain a ceiling light to recharge it.").withStyle(ChatFormatting.GRAY), true);
                return InteractionResultHolder.fail(bat);
            }
            if (!level.isClientSide) {
                other.setDamageValue(0);
                bat.setDamageValue(bat.getDamageValue() + 1);
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6F, 1.8F);
                player.displayClientMessage(Component.literal("Flashlight charged.").withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.sidedSuccess(bat, level.isClientSide);
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            desc(this, tip);
            tip.add(Component.literal("Charges left: " + (s.getMaxDamage() - s.getDamageValue()) + " / " + s.getMaxDamage()).withStyle(ChatFormatting.AQUA));
        }
    }

    /** Opens every security door; wears out after a few uses. */
    public static class MasterKey extends Item {
        public MasterKey(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack s, Level l, List<Component> tip, TooltipFlag f) {
            desc(this, tip);
            tip.add(Component.literal("Uses left: " + (s.getMaxDamage() - s.getDamageValue())).withStyle(ChatFormatting.GOLD));
        }
    }
}
