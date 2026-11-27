package de.officerooms.level0;

import de.officerooms.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.joml.Vector3f;

/** 0.21.0: blocks of Level 0 (in Level 0 they cannot be broken; see {@link Level0Events}). */
public final class Level0Blocks {

    private static DeferredHolder<Block, Block> regBlock(String name, java.util.function.Supplier<Block> b) {
        DeferredHolder<Block, Block> r = ModBlocks.BLOCKS.register(name, b);
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    private static DeferredHolder<Block, Block> reg(String name, Block.Properties p) {
        DeferredHolder<Block, Block> r = ModBlocks.BLOCKS.register(name, () -> new Block(p));
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    public static final DeferredHolder<Block, Block> WALLPAPER = reg("level0_wallpaper", Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F, 6.0F).sound(SoundType.WOOL));
    public static final DeferredHolder<Block, Block> CARPET = reg("level0_carpet", Block.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).strength(1.0F).sound(SoundType.WOOL));
    public static final DeferredHolder<Block, Block> CEILING = reg("level0_ceiling", Block.Properties.of().mapColor(MapColor.SAND).strength(1.0F).sound(SoundType.STONE));
    public static final DeferredHolder<Block, Block> LIGHT = reg("level0_light", Block.Properties.of().mapColor(MapColor.SNOW).strength(0.5F).sound(SoundType.GLASS).lightLevel(s -> 15));
    public static final DeferredHolder<Block, Block> LIGHT_OFF = reg("level0_light_off", Block.Properties.of().mapColor(MapColor.SAND).strength(0.5F).sound(SoundType.GLASS));
    public static final DeferredHolder<Block, Block> CEILING_VOID = reg("level0_ceiling_void", Block.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.0F).sound(SoundType.STONE));
    public static final DeferredHolder<Block, Block> FLICKER = ModBlocks.BLOCKS.register("level0_light_flicker", () -> new Flicker(Block.Properties.of().mapColor(MapColor.SNOW)
            .strength(0.5F).sound(SoundType.GLASS).randomTicks().lightLevel(s -> s.getValue(Flicker.LIT) ? 14 : 1)));
    public static final DeferredHolder<Block, Block> WALLPAPER_WORN = reg("level0_wallpaper_worn", Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F, 6.0F).sound(SoundType.WOOL));
    public static final DeferredHolder<Block, Block> WALLPAPER_ARROW = reg("level0_wallpaper_arrow", Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F, 6.0F).sound(SoundType.WOOL));
    /** 0.34.0: a carpeted ramp. Walks like stairs, looks like one continuous slope of the same carpet. */
    public static final DeferredHolder<Block, Block> RAMP = regBlock("level0_ramp", () -> new net.minecraft.world.level.block.StairBlock(
            () -> CARPET.get().defaultBlockState(), Block.Properties.of().mapColor(MapColor.TERRACOTTA_YELLOW).strength(1.0F).sound(SoundType.WOOL).noOcclusion()) {
        @Override
        public net.minecraft.world.level.block.state.BlockState updateShape(net.minecraft.world.level.block.state.BlockState s, net.minecraft.core.Direction d,
                net.minecraft.world.level.block.state.BlockState n, net.minecraft.world.level.LevelAccessor l, net.minecraft.core.BlockPos p, net.minecraft.core.BlockPos np) {
            return super.updateShape(s, d, n, l, p, np).setValue(SHAPE, net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT);
        }

        @Override
        public net.minecraft.world.level.block.state.BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
            net.minecraft.world.level.block.state.BlockState s = super.getStateForPlacement(ctx);
            return s == null ? null : s.setValue(SHAPE, net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT);
        }
    });
    /** 0.33.0: soaked carpet: it squelches and something drips onto it now and then. */
    public static final DeferredHolder<Block, Block> CARPET_WET = regBlock("level0_carpet_wet", () -> new Block(Block.Properties.of()
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(1.0F).sound(SoundType.WET_GRASS)) {
        @Override
        public void animateTick(net.minecraft.world.level.block.state.BlockState s, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
                                net.minecraft.util.RandomSource rnd) {
            if (rnd.nextInt(6) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.SPLASH, pos.getX() + rnd.nextDouble(), pos.getY() + 1.02,
                        pos.getZ() + rnd.nextDouble(), 0, 0, 0);
            }
            if (rnd.nextInt(120) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, net.minecraft.sounds.SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                        net.minecraft.sounds.SoundSource.AMBIENT, 0.5F, 0.9F + rnd.nextFloat() * 0.2F, false);
            }
        }
    });
    public static final DeferredHolder<Block, Block> CARPET_STAINED = reg("level0_carpet_stained", Block.Properties.of().mapColor(MapColor.TERRACOTTA_BROWN).strength(1.0F).sound(SoundType.WOOL));
    public static final DeferredHolder<Block, Block> CEILING_STAINED = reg("level0_ceiling_stained", Block.Properties.of().mapColor(MapColor.SAND).strength(1.0F).sound(SoundType.STONE));
    public static final DeferredHolder<Block, Block> VENT = reg("level0_vent", Block.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.METAL));
    public static final DeferredHolder<Block, Block> OUTLET = ModBlocks.BLOCKS.register("level0_outlet", () -> new Outlet(Block.Properties.of().mapColor(MapColor.SNOW)
            .strength(0.5F).sound(SoundType.STONE).noCollission().noOcclusion()));
    public static final DeferredHolder<Block, Block> MANILA_WALLPAPER = reg("manila_wallpaper", Block.Properties.of().mapColor(MapColor.SAND).strength(1.5F, 6.0F).sound(SoundType.WOOL));
    public static final DeferredHolder<Block, Block> MANILA_CARPET = reg("manila_carpet", Block.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(1.0F).sound(SoundType.WOOL));
    public static final DeferredHolder<Block, Block> EXIT = ModBlocks.BLOCKS.register("level0_exit", () -> new Exit(Block.Properties.of().mapColor(MapColor.COLOR_YELLOW)
            .strength(-1.0F, 3600000.0F).noCollission().noOcclusion().lightLevel(s -> 11).sound(SoundType.GLASS).noLootTable().pushReaction(PushReaction.BLOCK)));

    private Level0Blocks() {
    }

    public static void init() {
    }

    /** 0.21.1: a wall outlet. */
    public static final class Outlet extends net.minecraft.world.level.block.HorizontalDirectionalBlock {
        Outlet(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return switch (s.getValue(FACING)) {
                case SOUTH -> Block.box(5, 2, 0, 11, 8, 1);
                case EAST -> Block.box(0, 2, 5, 1, 8, 11);
                case WEST -> Block.box(15, 2, 5, 16, 8, 11);
                default -> Block.box(5, 2, 15, 11, 8, 16);
            };
        }
    }

    /** 0.21.1: a fluorescent panel that now and then stutters, with a buzz. */
    public static final class Flicker extends Block {
        public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

        Flicker(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(LIT, true));
        }

        @Override
        protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> b) {
            b.add(LIT);
        }

        @Override
        public void randomTick(BlockState s, net.minecraft.server.level.ServerLevel l, BlockPos p, RandomSource r) {
            l.setBlock(p, s.setValue(LIT, false), 2);
            l.scheduleTick(p, this, 2 + r.nextInt(5));
            l.playSound(null, p, de.officerooms.core.ModSounds.AMB_BUZZ.get(), net.minecraft.sounds.SoundSource.AMBIENT, 0.6F, 0.9F + r.nextFloat() * 0.3F);
        }

        @Override
        public void tick(BlockState s, net.minecraft.server.level.ServerLevel l, BlockPos p, RandomSource r) {
            boolean on = !s.getValue(LIT);
            l.setBlock(p, s.setValue(LIT, on), 2);
            if (!on || r.nextFloat() < 0.6F) {
                l.scheduleTick(p, this, 2 + r.nextInt(7));
            }
        }
    }

    /** The way out at the end of the flooded passage. */
    public static final class Exit extends Block implements net.minecraft.world.level.block.LiquidBlockContainer {
        Exit(Properties p) {
            super(p);
        }

        @Override
        public boolean canPlaceLiquid(BlockGetter l, BlockPos p, BlockState s, net.minecraft.world.level.material.Fluid f) {
            return false;                                                   // the water must not wash the way out away
        }

        @Override
        public boolean placeLiquid(net.minecraft.world.level.LevelAccessor l, BlockPos p, BlockState s, net.minecraft.world.level.material.FluidState f) {
            return false;
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return Shapes.block();
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof ServerPlayer p) {
                Level0Events.escape(p);
            }
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
            level.addParticle(new DustParticleOptions(new Vector3f(0.95F, 0.88F, 0.6F), 1.0F), pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(),
                    pos.getZ() + r.nextDouble(), 0, 0.02, 0);
        }
    }
}
