package de.officerooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 0.10.1: A chalk mark on a wall: an arrow (left, right, up, down) or a cross. FACING = side the mark faces. */
public class ChalkMarkBlock extends HorizontalDirectionalBlock {

    public enum Mark implements StringRepresentable {
        CROSS, LEFT, RIGHT, UP, DOWN;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Mark> MARK = EnumProperty.create("mark", Mark.class);
    private static final VoxelShape N = Block.box(1, 1, 15, 15, 15, 16);
    private static final VoxelShape S = Block.box(1, 1, 0, 15, 15, 1);
    private static final VoxelShape W = Block.box(15, 1, 1, 16, 15, 15);
    private static final VoxelShape E = Block.box(0, 1, 1, 1, 15, 15);

    public ChalkMarkBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(MARK, Mark.CROSS));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, MARK);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> S;
            case WEST -> W;
            case EAST -> E;
            default -> N;
        };
    }

    @Override
    public boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        Direction f = s.getValue(FACING);
        BlockPos wall = pos.relative(f.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, f);
    }

    @Override
    public BlockState updateShape(BlockState s, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return dir == s.getValue(FACING).getOpposite() && !canSurvive(s, level, pos) ? Blocks.AIR.defaultBlockState() : s;
    }
}
