package de.officerooms.block;

import de.officerooms.hazard.HazardState;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 0.17.0: a monitor that shows the HAZARD ZONE status (normal display, warning with countdown, active zone). Some
 * monitors in the world are broken and only show corrupted data. The text is drawn by {@code HazardMonitorRenderer}.
 */
public class HazardMonitorBlock extends DecorBlock implements EntityBlock {

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    public HazardMonitorBlock(Properties props, VoxelShape shape) {
        super(props, shape);
        registerDefaultState(defaultBlockState().setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(BROKEN);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    /** Alarm beeps near working monitors while a warning or a zone is active (client only). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        int ph = HazardState.CLIENT.phase;
        if (ph != HazardState.IDLE && !state.getValue(BROKEN) && r.nextInt(6) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS,
                    0.5F, ph == HazardState.ACTIVE ? 0.7F : 1.6F, false);
        }
    }

    public static class Entity extends BlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(ModBlocks.HAZARD_MONITOR_BE.get(), pos, state);
        }
    }

    public static BlockEntityType<Entity> type() {
        return ModBlocks.HAZARD_MONITOR_BE.get();
    }
}
