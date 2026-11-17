package de.officerooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 0.26.0: sliding steel door of the elevator cabins. Two leaves with a centre seam; STAGE 0 = closed, 1 and 2 =
 * the leaves slide apart to the left and right (the ride removes the block at the end of the opening). AXIS is
 * the direction the leaves run along. Placed and removed by {@link de.officerooms.world.ElevatorRides}.
 */
public class ElevatorDoorBlock extends Block {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 2);
    private static final int[] GAP = {0, 2, 4};
    private static final VoxelShape[][] SHAPES = new VoxelShape[2][3];

    static {
        for (int st = 0; st < 3; st++) {
            int g = GAP[st];
            SHAPES[0][st] = st == 0 ? Block.box(0, 0, 6.5, 16, 16, 9.5)
                    : Shapes.or(Block.box(0, 0, 6.5, 8 - g * 1.6, 16, 9.5), Block.box(8 + g * 1.6, 0, 6.5, 16, 16, 9.5));
            SHAPES[1][st] = st == 0 ? Block.box(6.5, 0, 0, 9.5, 16, 16)
                    : Shapes.or(Block.box(6.5, 0, 0, 9.5, 16, 8 - g * 1.6), Block.box(6.5, 0, 8 + g * 1.6, 9.5, 16, 16));
        }
    }

    public ElevatorDoorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS, STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPES[s.getValue(AXIS) == Direction.Axis.X ? 0 : 1][s.getValue(STAGE)];
    }
}
