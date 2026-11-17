package de.officerooms.block;

import de.officerooms.research.ResearchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 0.12.0: exchanges Research Files for permanent upgrades (see {@link ResearchMenu}). */
public class ResearchTableBlock extends DecorBlock {

    public ResearchTableBlock(Properties props, VoxelShape shape) {
        super(props, shape);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ResearchMenu(id, inv, ContainerLevelAccess.create(level, pos)),
                Component.translatable("container.office_rooms.research_table")));
        return InteractionResult.CONSUME;
    }
}
