package de.officerooms.block;

import de.officerooms.item.AccessCardItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 0.8.0: Security door. Unbreakable, ignores redstone and opens only for a player holding a Level
 * Access Card whose level is at least the door's {@link #LEVEL}. Used for the vault in The Maze.
 */
public class SecurityDoorBlock extends DoorBlock {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, 3);

    public SecurityDoorBlock(Properties props) {
        super(props, BlockSetType.IRON);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(LEVEL);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int need = state.getValue(LEVEL);
        int have = Math.max(cardLevel(player.getMainHandItem()), cardLevel(player.getOffhandItem()));
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        boolean key = false;
        if (have < need) {
            for (InteractionHand h : InteractionHand.values()) {
                if (player.getItemInHand(h).getItem() instanceof de.officerooms.item.ToolItems.MasterKey) {
                    key = true;
                    have = 3;
                    if (!state.getValue(OPEN) && !player.getAbilities().instabuild) {
                        player.getItemInHand(h).hurtAndBreak(1, player, p -> p.broadcastBreakEvent(h));
                    }
                    break;
                }
            }
        }
        if (have >= need) {
            boolean open = !state.getValue(OPEN);
            setOpen(player, level, state, pos, open);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.8F, open ? 1.6F : 1.2F);
            player.displayClientMessage(Component.literal(open ? "ACCESS GRANTED" : "DOOR LOCKED")
                    .withStyle(ChatFormatting.GREEN), true);
        } else {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 1.0F, 0.5F);
            player.displayClientMessage(Component.literal("ACCESS DENIED - Level " + need + " clearance required")
                    .withStyle(ChatFormatting.RED), true);
        }
        return InteractionResult.CONSUME;
    }

    private static int cardLevel(ItemStack s) {
        return s.getItem() instanceof AccessCardItem c ? c.level : 0;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        // ignores redstone on purpose
    }
}
