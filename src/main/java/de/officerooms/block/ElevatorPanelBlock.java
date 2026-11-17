package de.officerooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 0.26.0: button panel on the cabin wall (the click area is larger than the panel itself). FACING points into the cabin. Right-click opens the floor panel. */
public class ElevatorPanelBlock extends HorizontalDirectionalBlock {

    private static final VoxelShape N = Block.box(3, 0, 13, 13, 16, 16);
    private static final VoxelShape S = Block.box(3, 0, 0, 13, 16, 3);
    private static final VoxelShape W = Block.box(13, 0, 3, 16, 16, 13);
    private static final VoxelShape E = Block.box(0, 0, 3, 3, 16, 13);

    public ElevatorPanelBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> S;
            case WEST -> W;
            case EAST -> E;
            default -> N;
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            de.officerooms.world.ElevatorRides.openPanel(sp, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
