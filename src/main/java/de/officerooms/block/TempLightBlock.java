package de.officerooms.block;

import de.officerooms.world.ToolEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 0.10.1: Invisible, temporary light source. MODE 0 = glowstick (light 12, green glow), 1 = flashlight beam
 * (light 15, kept alive while a flashlight points at it), 2 = emergency flare (light 15, flames and smoke).
 * It removes itself through a scheduled tick, so nothing is left behind.
 */
public class TempLightBlock extends Block {

    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 2);

    public TempLightBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(MODE, 0));
    }

    public static int lightLevel(BlockState s) {
        return s.getValue(MODE) == 0 ? 12 : 15;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(MODE);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MODE) == 1 && ToolEffects.flashlightAt(level, pos)) {
            level.scheduleTick(pos, this, 10);
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        int m = state.getValue(MODE);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.1;
        double z = pos.getZ() + 0.5;
        if (m == 0 && r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.GLOW, x + (r.nextDouble() - 0.5) * 0.3, y, z + (r.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
        } else if (m == 2) {
            level.addParticle(ParticleTypes.FLAME, x, y + 0.1, z, (r.nextDouble() - 0.5) * 0.04, 0.06, (r.nextDouble() - 0.5) * 0.04);
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 0, 0.05, 0);
            if (r.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
            }
        }
    }
}
