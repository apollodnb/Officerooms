package de.officerooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Deckenleuchte von Office Rooms (Phase 4, Performance). Ein flaches Panel bzw. ein
 * LED-Streifen direkt unter der Decke, normales Blockmodell: Es wird mit dem Chunk gerendert und
 * kostet pro Bild nichts. Bis 0.3.0 waren die Leuchten OfficeCraft-Möbel, die der Client jedes Bild
 * einzeln zeichnet; bei hunderten Leuchten in Sichtweite war das der größte FPS-Posten.
 *
 * <p>LIT = leuchtet (Lichtstufe 15), sonst ausgefallene Lampe. Bei Streifen leuchtet nur das
 * Mittelstück ({@link #CENTER}), damit drei Segmente nicht dreifach Licht berechnen.</p>
 */
public final class CeilingLightBlock extends Block implements SimpleWaterloggedBlock {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty CENTER = BooleanProperty.create("center");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** Wasserbüro: Leuchten stehen im Wasser, ohne dass Wasser sie wegspült. */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final VoxelShape PANEL = Block.box(2, 15, 2, 14, 16, 14);
    private static final VoxelShape STRIP_X = Block.box(0, 15, 5, 16, 16, 11);
    private static final VoxelShape STRIP_Z = Block.box(5, 15, 0, 11, 16, 16);

    private final boolean strip;

    public CeilingLightBlock(Properties props, boolean strip) {
        super(props);
        this.strip = strip;
        registerDefaultState(stateDefinition.any().setValue(LIT, true).setValue(CENTER, true).setValue(AXIS, Direction.Axis.X).setValue(WATERLOGGED, false));
    }

    public static int lightLevel(BlockState s) {
        return s.getValue(LIT) && s.getValue(CENTER) ? 15 : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT, CENTER, AXIS, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        boolean water = ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(AXIS, ctx.getHorizontalDirection().getAxis()).setValue(WATERLOGGED, water);
    }

    @Override
    public FluidState getFluidState(BlockState s) {
        return s.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(s);
    }

    @Override
    public BlockState updateShape(BlockState s, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (s.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(s, dir, other, level, pos, otherPos);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (!strip) {
            return PANEL;
        }
        return s.getValue(AXIS) == Direction.Axis.X ? STRIP_X : STRIP_Z;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }
}
