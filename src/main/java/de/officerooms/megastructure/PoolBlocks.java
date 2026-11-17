package de.officerooms.megastructure;

import de.officerooms.block.ModBlocks;
import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/** 0.35.0: water slides (slippery chute, tube) and pool ladders for the Poolrooms (own designs, gen_0350.py). */
public final class PoolBlocks {

    private PoolBlocks() {
    }

    private static RegistryObject<Block> reg(String name, Supplier<Block> b) {
        RegistryObject<Block> r = ModBlocks.BLOCKS.register(name, b);
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    /** The chute: a smooth slope as slippery as ice, always straight. */
    public static final RegistryObject<Block> SLIDE = reg("pool_slide", () -> new StairBlock(() -> ModBlocks.POOL_TILE.get().defaultBlockState(),
            Block.Properties.of().mapColor(MapColor.SNOW).strength(1.5F).sound(SoundType.STONE).friction(0.989F).noOcclusion()) {
        @Override
        public BlockState updateShape(BlockState s, net.minecraft.core.Direction d, BlockState n, net.minecraft.world.level.LevelAccessor l,
                                      net.minecraft.core.BlockPos p, net.minecraft.core.BlockPos np) {
            return super.updateShape(s, d, n, l, p, np).setValue(SHAPE, StairsShape.STRAIGHT);
        }
    });
    public static final RegistryObject<Block> TUBE = reg("pool_slide_tube",
            () -> new Block(Block.Properties.of().mapColor(MapColor.SNOW).strength(1.5F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> LADDER = reg("pool_ladder",
            () -> new CinemaBlocks.Turned(Block.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(),
                    3, 0, 10, 13, 16, 16));

    /** Stainless tube handrail, one block long (runs along x when facing north). */
    public static final RegistryObject<Block> RAIL = reg("pool_handrail",
            () -> new CinemaBlocks.Turned(Block.Properties.of().mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion(),
                    0, 0, 7, 16, 15, 9));

    /** 0.35.1: a round slide pipe (straight) and its open mouth with water dribbling out (front = north). */
    public static final RegistryObject<Block> PIPE = reg("pool_slide_pipe",
            () -> new CinemaBlocks.Turned(Block.Properties.of().mapColor(MapColor.SNOW).strength(1.5F).sound(SoundType.STONE).noOcclusion(),
                    1, 1, 0, 15, 15, 16));
    public static final RegistryObject<Block> PIPE_END = reg("pool_slide_pipe_end", () -> new CinemaBlocks.Turned(
            Block.Properties.of().mapColor(MapColor.SNOW).strength(1.5F).sound(SoundType.STONE).noOcclusion(), 1, 1, 0, 15, 15, 16) {
        @Override
        public void animateTick(BlockState s, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, net.minecraft.util.RandomSource rnd) {
            net.minecraft.core.Direction f = s.getValue(FACING);
            double x = pos.getX() + 0.5 + f.getStepX() * 0.45 + (rnd.nextDouble() - 0.5) * 0.4;
            double z = pos.getZ() + 0.5 + f.getStepZ() * 0.45 + (rnd.nextDouble() - 0.5) * 0.4;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, x, pos.getY() + 0.12, z, 0, 0, 0);
            if (rnd.nextInt(40) == 0) {
                level.playLocalSound(x, pos.getY(), z, net.minecraft.sounds.SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON,
                        net.minecraft.sounds.SoundSource.AMBIENT, 0.4F, 0.8F + rnd.nextFloat() * 0.3F, false);
            }
        }
    });

    public static void init() {
    }
}
