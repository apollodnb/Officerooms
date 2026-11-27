package de.officerooms.sea;

import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.megastructure.CinemaBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.GlassBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 0.42.0: blocks of the SS Fun =) and of the departure gate in the Office Rooms. All drawn in {@code tools/gen_0420.py}
 * (own designs).
 */
public final class ShipBlocks {

    private ShipBlocks() {
    }

    /** Names of every block here, for the asset generator check and the creative tab. */
    public static final List<String> NAMES = new ArrayList<>();

    // --- block classes -----------------------------------------------------------------------------------------------

    /** Ship lamp: lit or dead. The Mascot turns them off. */
    public static class Lamp extends Block {
        public static final BooleanProperty LIT = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

        public Lamp(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(LIT, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(LIT);
        }
    }

    /** Full block with four picture variants (2 x 2 decals like the =) smile). */
    public static class Variant extends Block {
        public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 3);

        public Variant(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(VARIANT, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(VARIANT);
        }
    }

    /** Wall panel with three picture variants side by side (departure board). */
    public static class Board extends CinemaBlocks.Turned {
        public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);

        public Board(Properties p) {
            super(p, 0, 0, 14, 16, 16, 16);
            registerDefaultState(defaultBlockState().setValue(PART, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            super.createBlockStateDefinition(b);
            b.add(PART);
        }
    }

    /** A vent in the ceiling. Water drips from it; something squeaks inside now and then. */
    public static class Vent extends Block {
        private final boolean broken;

        public Vent(Properties p, boolean broken) {
            super(p);
            this.broken = broken;
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
            if (r.nextInt(broken ? 3 : 12) == 0) {
                level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() - 0.05,
                        pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0, 0);
            }
        }
    }

    /** Wet carpet: drips and the odd splash. */
    public static class Wet extends Block {
        public Wet(Properties p) {
            super(p);
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
            if (r.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.SPLASH, pos.getX() + r.nextDouble(), pos.getY() + 1.02, pos.getZ() + r.nextDouble(), 0, 0, 0);
            }
        }
    }

    /** Right-click block (boarding gate, lifeboat release). */
    public static class Use extends CinemaBlocks.Turned {
        private final java.util.function.BiConsumer<ServerPlayer, BlockPos> action;

        public Use(Properties p, java.util.function.BiConsumer<ServerPlayer, BlockPos> action, double... b) {
            super(p, b[0], b[1], b[2], b[3], b[4], b[5]);
            this.action = action;
        }

        @Override
        public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                action.accept(sp, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Hull block that ships use for windows of all kinds: glass that culls against itself. */
    public static class Glass extends GlassBlock {
        public Glass(Properties p) {
            super(p);
        }
    }

    /** A thin wall-mounted decor block: barely any collision (rails, rings, signs). */
    public static class Thin extends CinemaBlocks.Turned {
        public Thin(Properties p, double... b) {
            super(p, b[0], b[1], b[2], b[3], b[4], b[5]);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return Shapes.empty();
        }
    }

    // --- registration ------------------------------------------------------------------------------------------------

    static BlockBehaviour.Properties props(MapColor c, SoundType st, float hard, int light) {
        return Block.Properties.of().mapColor(c).strength(hard, hard * 4).sound(st).lightLevel(s -> light);
    }

    private static DeferredHolder<Block, Block> reg(String name, Supplier<Block> b) {
        NAMES.add(name);
        DeferredHolder<Block, Block> r = ModBlocks.BLOCKS.register(name, b);
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    private static DeferredHolder<Block, Block> full(String name, MapColor c, SoundType st, float hard, int light) {
        return reg(name, () -> new Block(props(c, st, hard, light)));
    }

    private static DeferredHolder<Block, Block> turned(String name, MapColor c, SoundType st, int light, double... b) {
        return reg(name, () -> new CinemaBlocks.Turned(props(c, st, 1.5F, light).noOcclusion(), b[0], b[1], b[2], b[3], b[4], b[5]));
    }

    private static DeferredHolder<Block, Block> thin(String name, MapColor c, SoundType st, int light, double... b) {
        return reg(name, () -> new Thin(props(c, st, 1.0F, light).noOcclusion(), b));
    }

    private static DeferredHolder<Block, Block> lamp(String name, MapColor c, int light) {
        return reg(name, () -> new Lamp(props(c, SoundType.GLASS, 0.8F, 0).lightLevel(s -> s.getValue(Lamp.LIT) ? light : 0)));
    }

    // hull and structure (the outer hull is very hard: breaking out is possible, but slow)
    public static final DeferredHolder<Block, Block> HULL = full("ship_hull", MapColor.SNOW, SoundType.METAL, 40F, 0);
    public static final DeferredHolder<Block, Block> HULL_DARK = full("ship_hull_dark", MapColor.COLOR_BLUE, SoundType.METAL, 40F, 0);
    public static final DeferredHolder<Block, Block> HULL_RUST = full("ship_hull_rust", MapColor.TERRACOTTA_WHITE, SoundType.METAL, 40F, 0);
    public static final DeferredHolder<Block, Block> SMILE = reg("ship_smile", () -> new Variant(props(MapColor.SNOW, SoundType.METAL, 40F, 0)));
    public static final DeferredHolder<Block, Block> FUNNEL = full("ship_funnel", MapColor.COLOR_RED, SoundType.METAL, 20F, 0);
    public static final DeferredHolder<Block, Block> FUNNEL_TOP = full("ship_funnel_top", MapColor.COLOR_BLACK, SoundType.METAL, 20F, 0);
    public static final DeferredHolder<Block, Block> FUNNEL_SMILE = reg("ship_funnel_smile", () -> new Variant(props(MapColor.COLOR_RED, SoundType.METAL, 20F, 0)));
    public static final DeferredHolder<Block, Block> PORTHOLE = reg("ship_porthole", () -> new Glass(props(MapColor.SNOW, SoundType.GLASS, 3F, 0).noOcclusion()
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final DeferredHolder<Block, Block> WINDOW = reg("ship_window", () -> new Glass(props(MapColor.NONE, SoundType.GLASS, 3F, 0).noOcclusion()
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    // passenger interior
    public static final DeferredHolder<Block, Block> WALL_PANEL = full("ship_wall_panel", MapColor.SAND, SoundType.WOOD, 2F, 0);
    public static final DeferredHolder<Block, Block> WAINSCOT = full("ship_wainscot", MapColor.WOOD, SoundType.WOOD, 2F, 0);
    public static final DeferredHolder<Block, Block> WOOD_PANEL = full("ship_wood_panel", MapColor.COLOR_ORANGE, SoundType.WOOD, 2F, 0);
    public static final DeferredHolder<Block, Block> TEAL_TRIM = full("ship_teal_trim", MapColor.COLOR_CYAN, SoundType.WOOD, 2F, 0);
    public static final DeferredHolder<Block, Block> CEILING = full("ship_ceiling", MapColor.SAND, SoundType.WOOD, 2F, 0);
    public static final DeferredHolder<Block, Block> GOLD_CEILING = full("ship_gold_ceiling", MapColor.GOLD, SoundType.METAL, 2F, 7);
    public static final DeferredHolder<Block, Block> LIGHT = lamp("ship_ceiling_light", MapColor.SNOW, 13);
    public static final DeferredHolder<Block, Block> CREW_LIGHT = lamp("ship_crew_light", MapColor.COLOR_ORANGE, 9);
    public static final DeferredHolder<Block, Block> CARPET = full("ship_carpet", MapColor.COLOR_BLUE, SoundType.WOOL, 1F, 0);
    public static final DeferredHolder<Block, Block> CARPET_STARS = full("ship_carpet_stars", MapColor.COLOR_BLUE, SoundType.WOOL, 1F, 0);
    public static final DeferredHolder<Block, Block> CARPET_TEAL = full("ship_carpet_teal", MapColor.COLOR_CYAN, SoundType.WOOL, 1F, 0);
    public static final DeferredHolder<Block, Block> CARPET_RED = full("ship_carpet_red", MapColor.COLOR_RED, SoundType.WOOL, 1F, 0);
    public static final DeferredHolder<Block, Block> CARPET_BROWN = full("ship_carpet_brown", MapColor.COLOR_BROWN, SoundType.WOOL, 1F, 0);
    public static final DeferredHolder<Block, Block> CARPET_WET = reg("ship_carpet_wet", () -> new Wet(props(MapColor.COLOR_BLUE, SoundType.WOOL, 1F, 0)));
    public static final DeferredHolder<Block, Block> MARBLE = full("ship_marble", MapColor.TERRACOTTA_BROWN, SoundType.STONE, 3F, 0);
    public static final DeferredHolder<Block, Block> TEAK = full("ship_teak", MapColor.WOOD, SoundType.WOOD, 2F, 0);
    public static final DeferredHolder<Block, Block> POOL_TILE = full("ship_pool_tile", MapColor.COLOR_LIGHT_BLUE, SoundType.STONE, 3F, 0);
    public static final DeferredHolder<Block, Block> BRASS_COLUMN = full("ship_brass_column", MapColor.GOLD, SoundType.METAL, 3F, 0);
    public static final DeferredHolder<Block, Block> CURTAIN = full("ship_curtain", MapColor.COLOR_RED, SoundType.WOOL, 1F, 0);
    public static final DeferredHolder<Block, Block> CARPET_STAIRS = reg("ship_carpet_stairs",
            () -> new StairBlock(() -> CARPET.get().defaultBlockState(), props(MapColor.COLOR_BLUE, SoundType.WOOL, 1F, 0)));
    public static final DeferredHolder<Block, Block> TEAL_STAIRS = reg("ship_teal_stairs",
            () -> new StairBlock(() -> CARPET_TEAL.get().defaultBlockState(), props(MapColor.COLOR_CYAN, SoundType.WOOL, 1F, 0)));
    // crew areas
    public static final DeferredHolder<Block, Block> STEEL_WALL = full("ship_steel_wall", MapColor.SNOW, SoundType.METAL, 4F, 0);
    public static final DeferredHolder<Block, Block> STEEL_FLOOR = full("ship_steel_floor", MapColor.COLOR_GRAY, SoundType.METAL, 4F, 0);
    public static final DeferredHolder<Block, Block> STEEL_FRAME = full("ship_steel_frame", MapColor.SNOW, SoundType.METAL, 4F, 0);
    public static final DeferredHolder<Block, Block> GRATE = reg("ship_grate", () -> new Glass(props(MapColor.COLOR_GRAY, SoundType.METAL, 4F, 0).noOcclusion()
            .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    public static final DeferredHolder<Block, Block> STEEL_STAIRS = reg("ship_steel_stairs",
            () -> new StairBlock(() -> STEEL_FLOOR.get().defaultBlockState(), props(MapColor.COLOR_GRAY, SoundType.METAL, 4F, 0)));
    public static final DeferredHolder<Block, Block> PIPES = full("ship_pipes", MapColor.COLOR_GRAY, SoundType.METAL, 4F, 0);
    public static final DeferredHolder<Block, Block> ENGINE = full("ship_engine", MapColor.COLOR_GRAY, SoundType.METAL, 6F, 0);
    public static final DeferredHolder<Block, Block> ENGINE_TOP = full("ship_engine_top", MapColor.COLOR_GRAY, SoundType.METAL, 6F, 0);
    public static final DeferredHolder<Block, Block> VENT = reg("ship_vent", () -> new Vent(props(MapColor.COLOR_GRAY, SoundType.METAL, 2F, 0).noOcclusion(), false));
    public static final DeferredHolder<Block, Block> VENT_BROKEN = reg("ship_vent_broken", () -> new Vent(props(MapColor.COLOR_BLACK, SoundType.METAL, 2F, 0).noOcclusion()
            .noCollission(), true));
    // doors and rails
    public static final DeferredHolder<Block, Block> CABIN_DOOR = reg("ship_cabin_door",
            () -> new DoorBlock(props(MapColor.SNOW, SoundType.WOOD, 2F, 0).noOcclusion(), BlockSetType.OAK));
    public static final DeferredHolder<Block, Block> CREW_DOOR = reg("ship_crew_door",
            () -> new DoorBlock(props(MapColor.SNOW, SoundType.METAL, 3F, 0).noOcclusion(), BlockSetType.OAK));
    public static final DeferredHolder<Block, Block> FIRE_DOOR = reg("ship_fire_door",
            () -> new DoorBlock(props(MapColor.COLOR_GRAY, SoundType.METAL, 8F, 0).noOcclusion(), BlockSetType.IRON));
    public static final DeferredHolder<Block, Block> RAILING = reg("ship_railing", () -> new FenceBlock(props(MapColor.SNOW, SoundType.METAL, 3F, 0)));
    public static final DeferredHolder<Block, Block> BRASS_RAILING = reg("ship_brass_railing", () -> new FenceBlock(props(MapColor.GOLD, SoundType.METAL, 3F, 0)));
    // decor (front = the side facing into the room; wall-mounted parts sit at the back of the block)
    public static final DeferredHolder<Block, Block> HANDRAIL = thin("ship_handrail", MapColor.WOOD, SoundType.WOOD, 0, 0, 7, 13, 16, 10, 16);
    public static final DeferredHolder<Block, Block> LIFE_RING = thin("ship_life_ring", MapColor.COLOR_ORANGE, SoundType.WOOL, 0, 1, 1, 14, 15, 15, 16);
    public static final DeferredHolder<Block, Block> EXIT_SIGN = thin("ship_exit_sign", MapColor.COLOR_GREEN, SoundType.GLASS, 8, 1, 4, 14, 15, 12, 16);
    public static final DeferredHolder<Block, Block> SCONCE = thin("ship_sconce", MapColor.GOLD, SoundType.METAL, 11, 5, 3, 11, 11, 13, 16);
    public static final DeferredHolder<Block, Block> FIRE_HOSE = turned("ship_fire_hose", MapColor.COLOR_RED, SoundType.METAL, 0, 1, 0, 9, 15, 14, 16);
    public static final DeferredHolder<Block, Block> DECK_CHAIR = turned("ship_deck_chair", MapColor.SNOW, SoundType.WOOD, 0, 2, 0, 0, 14, 8, 16);
    public static final DeferredHolder<Block, Block> SLOT_MACHINE = turned("ship_slot_machine", MapColor.COLOR_RED, SoundType.METAL, 0, 1, 0, 2, 15, 16, 15);
    public static final DeferredHolder<Block, Block> SLOT_MACHINE_TOP = turned("ship_slot_machine_top", MapColor.COLOR_RED, SoundType.METAL, 9, 1, 0, 3, 15, 12, 15);
    public static final DeferredHolder<Block, Block> BUFFET = turned("ship_buffet_counter", MapColor.SNOW, SoundType.METAL, 0, 0, 0, 0, 16, 16, 16);
    public static final DeferredHolder<Block, Block> WASHER = turned("ship_washer", MapColor.SNOW, SoundType.METAL, 0, 0, 0, 1, 16, 16, 16);
    public static final DeferredHolder<Block, Block> ELEVATOR_DOOR = turned("ship_elevator_door", MapColor.GOLD, SoundType.METAL, 0, 0, 0, 12, 16, 16, 16);
    public static final DeferredHolder<Block, Block> SUITCASE = turned("ship_suitcase", MapColor.COLOR_BLUE, SoundType.WOOL, 0, 3, 0, 5, 13, 11, 11);
    public static final DeferredHolder<Block, Block> CONSOLE = turned("ship_console", MapColor.COLOR_GRAY, SoundType.METAL, 4, 0, 0, 2, 16, 12, 16);
    public static final DeferredHolder<Block, Block> POSTER = thin("ship_poster", MapColor.COLOR_YELLOW, SoundType.WOOL, 0, 1, 0, 15, 15, 16, 16);
    // lifeboats: the way off the ship
    public static final DeferredHolder<Block, Block> LIFEBOAT = full("ship_lifeboat", MapColor.COLOR_ORANGE, SoundType.WOOD, 3F, 0);
    public static final DeferredHolder<Block, Block> LIFEBOAT_CANOPY = full("ship_lifeboat_canopy", MapColor.SNOW, SoundType.WOOD, 3F, 0);
    public static final DeferredHolder<Block, Block> LIFEBOAT_RELEASE = reg("ship_lifeboat_release", () -> new Use(props(MapColor.COLOR_RED, SoundType.METAL, 3F, 2)
            .noOcclusion(), SeaEvents::launch, 3, 0, 10, 13, 14, 16));
    // departure gate in the Office Rooms
    public static final DeferredHolder<Block, Block> TERMINAL_CARPET = full("terminal_carpet", MapColor.COLOR_GRAY, SoundType.WOOL, 1F, 0);
    public static final DeferredHolder<Block, Block> TERMINAL_WINDOW = full("terminal_window", MapColor.COLOR_LIGHT_GREEN, SoundType.GLASS, 3F, 3);
    public static final DeferredHolder<Block, Block> DEPARTURE_BOARD = reg("departure_board", () -> new Board(props(MapColor.COLOR_BLACK, SoundType.GLASS, 2F, 9).noOcclusion()));
    public static final DeferredHolder<Block, Block> BOARDING_GATE = reg("boarding_gate", () -> new Use(props(MapColor.COLOR_GRAY, SoundType.METAL, 6F, 0)
            .noOcclusion(), SeaEvents::board, 0, 0, 13, 16, 16, 16));
    public static final DeferredHolder<Block, Block> BOARDING_GATE_TOP = reg("boarding_gate_top", () -> new Use(props(MapColor.COLOR_GRAY, SoundType.METAL, 6F, 6)
            .noOcclusion(), SeaEvents::board, 0, 0, 13, 16, 16, 16));
    public static final DeferredHolder<Block, Block> GATE_DESK = turned("gate_desk", MapColor.COLOR_GRAY, SoundType.METAL, 0, 0, 0, 0, 16, 16, 16);
    public static final DeferredHolder<Block, Block> GATE_SEATS = turned("gate_seats", MapColor.COLOR_BLUE, SoundType.METAL, 0, 0, 0, 2, 16, 9, 16);

    public static BlockState f(DeferredHolder<Block, Block> b, Direction front) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, front);
    }

    public static void init() {
    }
}
