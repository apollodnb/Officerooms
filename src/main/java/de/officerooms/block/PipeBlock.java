package de.officerooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 0.14.0: a rusty pipe along x, y or z (hotel basement, industrial complex). */
public class PipeBlock extends RotatedPillarBlock {

    private static final VoxelShape X = Block.box(0, 4, 4, 16, 12, 12);
    private static final VoxelShape Y = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape Z = Block.box(4, 4, 0, 12, 12, 16);

    public PipeBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(AXIS)) {
            case X -> X;
            case Z -> Z;
            default -> Y;
        };
    }
}
