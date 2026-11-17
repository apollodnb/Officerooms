package de.officerooms.megastructure;

import de.officerooms.block.CeilingLightBlock;
import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.core.ModSounds;
import de.officerooms.room.OfficePlanner;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

/**
 * 0.31.0: blocks of the abandoned cinema (own designs, drawn in {@code tools/gen_0310.py}) and its decoration: marquee
 * letters, screen numbers, posters and chests.
 */
public final class CinemaBlocks {

    private CinemaBlocks() {
    }

    // --- block classes -----------------------------------------------------------------------------------------------

    /** A decor block whose shape (given for a front facing north) turns with its facing. */
    public static class Turned extends DecorBlock {
        private final VoxelShape[] shapes = new VoxelShape[4];

        public Turned(Properties p, double x0, double y0, double z0, double x1, double y1, double z1) {
            super(p, Shapes.block());
            for (Direction d : Direction.Plane.HORIZONTAL) {
                double[] a = rot(x0, z0, d);
                double[] b = rot(x1, z1, d);
                shapes[d.get2DDataValue()] = Block.box(Math.min(a[0], b[0]), y0, Math.min(a[1], b[1]), Math.max(a[0], b[0]), y1, Math.max(a[1], b[1]));
            }
        }

        private static double[] rot(double x, double z, Direction d) {
            return switch (d) {
                case EAST -> new double[]{16 - z, x};
                case SOUTH -> new double[]{16 - x, 16 - z};
                case WEST -> new double[]{z, 16 - x};
                default -> new double[]{x, z};
            };
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return shapes[s.getValue(FACING).get2DDataValue()];
        }
    }

    /** Marquee letter panel: A–Z, 0–9. */
    public static class Letter extends Turned {
        public static final IntegerProperty GLYPH = IntegerProperty.create("glyph", 0, 35);

        public Letter(Properties p) {
            super(p, 0, 0, 14, 16, 16, 16);
            registerDefaultState(defaultBlockState().setValue(GLYPH, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            super.createBlockStateDefinition(b);
            b.add(GLYPH);
        }

        public static int glyph(char c) {
            return c >= '0' && c <= '9' ? 26 + (c - '0') : Character.toUpperCase(c) - 'A';
        }
    }

    /** Faded movie poster, two blocks tall. */
    public static class Poster extends Turned {
        public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 5);
        public static final BooleanProperty UPPER = BooleanProperty.create("upper");

        public Poster(Properties p) {
            super(p, 1, 0, 15, 15, 16, 16);
            registerDefaultState(defaultBlockState().setValue(VARIANT, 0).setValue(UPPER, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            super.createBlockStateDefinition(b);
            b.add(VARIANT, UPPER);
        }
    }

    /** The projector port in the back wall: a dusty beam drifts toward the screen and the projector rattles. */
    public static class Projector extends Turned {
        public Projector(Properties p) {
            super(p, 3, 3, 14, 13, 13, 16);
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
            Direction f = s.getValue(FACING);
            for (int k = 0; k < 4; k++) {
                double r = 1 + rnd.nextDouble() * 44;
                double drop = r * 0.07;                                  // the beam slopes down to the screen
                level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.5 + f.getStepX() * r + (rnd.nextDouble() - 0.5) * 1.6,
                        pos.getY() + 0.5 - drop + (rnd.nextDouble() - 0.5) * 1.2,
                        pos.getZ() + 0.5 + f.getStepZ() * r + (rnd.nextDouble() - 0.5) * 1.6, 0, 0, 0);
            }
            if (rnd.nextInt(18) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.CINEMA_PROJECTOR.get(), SoundSource.BLOCKS,
                        0.7F, 0.95F + rnd.nextFloat() * 0.1F, false);
            }
        }
    }

    // --- registration ------------------------------------------------------------------------------------------------

    private static Block.Properties props(MapColor c, SoundType st, int light) {
        return Block.Properties.of().mapColor(c).strength(1.5F, 6.0F).sound(st).lightLevel(s -> light);
    }

    private static RegistryObject<Block> reg(String name, Supplier<Block> b) {
        RegistryObject<Block> r = ModBlocks.BLOCKS.register(name, b);
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    private static RegistryObject<Block> full(String name, MapColor c, SoundType st, int light) {
        return reg(name, () -> new Block(props(c, st, light)));
    }

    private static RegistryObject<Block> turned(String name, MapColor c, SoundType st, int light, double... b) {
        return reg(name, () -> new Turned(props(c, st, light).noOcclusion(), b[0], b[1], b[2], b[3], b[4], b[5]));
    }

    public static final RegistryObject<Block> WALL = full("cinema_wall", MapColor.COLOR_RED, SoundType.WOOL, 0);
    public static final RegistryObject<Block> WALLPAPER = full("cinema_wallpaper", MapColor.TERRACOTTA_RED, SoundType.WOOD, 0);
    public static final RegistryObject<Block> CARPET = full("cinema_carpet", MapColor.COLOR_PURPLE, SoundType.WOOL, 0);
    public static final RegistryObject<Block> TILE = full("cinema_tile", MapColor.QUARTZ, SoundType.STONE, 0);
    public static final RegistryObject<Block> CEILING = full("cinema_ceiling", MapColor.COLOR_BLACK, SoundType.STONE, 0);
    public static final RegistryObject<Block> RISER = full("cinema_riser", MapColor.COLOR_PURPLE, SoundType.WOOL, 0);
    public static final RegistryObject<Block> STAIRS = reg("cinema_riser_stairs",
            () -> new StairBlock(() -> RISER.get().defaultBlockState(), props(MapColor.COLOR_PURPLE, SoundType.WOOL, 0)));
    public static final RegistryObject<Block> SCREEN = full("cinema_screen", MapColor.SNOW, SoundType.WOOL, 6);
    public static final RegistryObject<Block> SCREEN_STATIC = full("cinema_screen_static", MapColor.SNOW, SoundType.WOOL, 8);
    public static final RegistryObject<Block> SCREEN_OFF = full("cinema_screen_off", MapColor.COLOR_BLACK, SoundType.WOOL, 0);
    public static final RegistryObject<Block> SCREEN_TORN = full("cinema_screen_torn", MapColor.COLOR_GRAY, SoundType.WOOL, 2);
    public static final RegistryObject<Block> FIGURE_LOWER = full("cinema_screen_figure_lower", MapColor.SNOW, SoundType.WOOL, 6);
    public static final RegistryObject<Block> FIGURE_UPPER = full("cinema_screen_figure_upper", MapColor.SNOW, SoundType.WOOL, 6);
    public static final RegistryObject<Block> SEAT = turned("cinema_seat", MapColor.COLOR_RED, SoundType.WOOL, 0, 1, 0, 2, 15, 16, 15);
    public static final RegistryObject<Block> SEAT_BROKEN = turned("cinema_seat_broken", MapColor.COLOR_RED, SoundType.WOOL, 0, 1, 0, 2, 15, 12, 15);
    public static final RegistryObject<Block> AISLE_LIGHT = turned("cinema_aisle_light", MapColor.COLOR_YELLOW, SoundType.METAL, 5, 5, 1, 14, 11, 4, 16);
    public static final RegistryObject<Block> EXIT = turned("cinema_exit_sign", MapColor.COLOR_GREEN, SoundType.METAL, 7, 2, 4, 14, 14, 11, 16);
    public static final RegistryObject<Block> PROJECTOR = reg("cinema_projector",
            () -> new Projector(props(MapColor.COLOR_GRAY, SoundType.METAL, 11).noOcclusion()));
    public static final RegistryObject<Block> PROJECTOR_OFF = turned("cinema_projector_off", MapColor.COLOR_GRAY, SoundType.METAL, 0, 3, 3, 14, 13, 13, 16);
    public static final RegistryObject<Block> COUNTER = turned("cinema_counter", MapColor.COLOR_RED, SoundType.WOOD, 0, 0, 0, 0, 16, 16, 16);
    public static final RegistryObject<Block> TICKET_WINDOW = turned("cinema_ticket_window", MapColor.NONE, SoundType.GLASS, 0, 0, 0, 7, 16, 16, 9);
    public static final RegistryObject<Block> POPCORN = turned("cinema_popcorn_machine", MapColor.COLOR_RED, SoundType.GLASS, 6, 2, 0, 2, 14, 16, 14);
    public static final RegistryObject<Block> SODA = turned("cinema_soda_fountain", MapColor.COLOR_LIGHT_GRAY, SoundType.METAL, 0, 2, 0, 3, 14, 13, 15);
    public static final RegistryObject<Block> CANDY = turned("cinema_candy_case", MapColor.COLOR_RED, SoundType.GLASS, 4, 0, 0, 0, 16, 16, 16);
    public static final RegistryObject<Block> MENU = turned("cinema_menu_board", MapColor.COLOR_BLACK, SoundType.WOOD, 5, 0, 0, 15, 16, 16, 16);
    public static final RegistryObject<Block> STANCHION = reg("cinema_stanchion",
            () -> new DecorBlock(props(MapColor.GOLD, SoundType.METAL, 0).noOcclusion(), Block.box(6, 0, 6, 10, 15, 10)));
    public static final RegistryObject<Block> ROPE = reg("cinema_velvet_rope",
            () -> new DecorBlock(props(MapColor.COLOR_RED, SoundType.WOOL, 0).noOcclusion(), Block.box(0, 9, 7, 16, 12, 9)));
    public static final RegistryObject<Block> MARQUEE = full("cinema_marquee", MapColor.GOLD, SoundType.GLASS, 10);
    public static final RegistryObject<Block> TRASH = turned("cinema_trash_bin", MapColor.COLOR_GRAY, SoundType.METAL, 0, 4, 0, 4, 12, 13, 12);
    public static final RegistryObject<Block> SPILL = reg("cinema_spilled_popcorn",
            () -> new CarpetBlock(Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.1F).sound(SoundType.WOOL).noOcclusion()));
    public static final RegistryObject<Block> LETTER = reg("cinema_letter", () -> new Letter(props(MapColor.COLOR_RED, SoundType.GLASS, 8).noOcclusion()));
    public static final RegistryObject<Block> POSTER = reg("cinema_poster", () -> new Poster(props(MapColor.COLOR_BLACK, SoundType.WOOD, 0).noOcclusion()));

    public static void init() {
    }

    // --- states ------------------------------------------------------------------------------------------------------

    private static BlockState f(RegistryObject<Block> b, Direction d) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, d);
    }

    public static BlockState state(Cinema.Part part) {
        BlockState strip = ModBlocks.CEILING_STRIP.get().defaultBlockState().setValue(CeilingLightBlock.AXIS, Direction.Axis.X);
        return switch (part) {
            case AIR, OFFICE -> Blocks.AIR.defaultBlockState();
            case WALL -> WALL.get().defaultBlockState();
            case LOBBY_WALL -> WALLPAPER.get().defaultBlockState();
            case CARPET -> CARPET.get().defaultBlockState();
            case TILE -> TILE.get().defaultBlockState();
            case CEILING -> CEILING.get().defaultBlockState();
            case LIGHT -> strip;
            case LIGHT_OFF -> strip.setValue(CeilingLightBlock.LIT, false);
            case RISER -> RISER.get().defaultBlockState();
            case STAIR_N -> STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            case STAIR_S -> STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            case SEAT_N -> f(SEAT, Direction.NORTH);
            case SEAT_S -> f(SEAT, Direction.SOUTH);
            case SEAT_BROKEN_N -> f(SEAT_BROKEN, Direction.NORTH);
            case SEAT_BROKEN_S -> f(SEAT_BROKEN, Direction.SOUTH);
            case SCREEN -> SCREEN.get().defaultBlockState();
            case SCREEN_STATIC -> SCREEN_STATIC.get().defaultBlockState();
            case SCREEN_OFF -> SCREEN_OFF.get().defaultBlockState();
            case SCREEN_TORN -> SCREEN_TORN.get().defaultBlockState();
            case FIGURE_LOWER -> FIGURE_LOWER.get().defaultBlockState();
            case FIGURE_UPPER -> FIGURE_UPPER.get().defaultBlockState();
            case PROJ_N -> f(PROJECTOR, Direction.NORTH);
            case PROJ_S -> f(PROJECTOR, Direction.SOUTH);
            case PROJ_OFF_N -> f(PROJECTOR_OFF, Direction.NORTH);
            case PROJ_OFF_S -> f(PROJECTOR_OFF, Direction.SOUTH);
            case AISLE_E -> f(AISLE_LIGHT, Direction.EAST);
            case AISLE_W -> f(AISLE_LIGHT, Direction.WEST);
            case EXIT_N -> f(EXIT, Direction.NORTH);
            case EXIT_S -> f(EXIT, Direction.SOUTH);
            case COUNTER_N -> f(COUNTER, Direction.NORTH);
            case COUNTER_S -> f(COUNTER, Direction.SOUTH);
            case TICKET_S -> f(TICKET_WINDOW, Direction.SOUTH);
            case POPCORN_N -> f(POPCORN, Direction.NORTH);
            case SODA_N -> f(SODA, Direction.NORTH);
            case CANDY_N -> f(CANDY, Direction.NORTH);
            case MENU_N -> f(MENU, Direction.NORTH);
            case STANCHION -> f(STANCHION, Direction.NORTH);
            case ROPE -> f(ROPE, Direction.NORTH);
            case MARQUEE -> MARQUEE.get().defaultBlockState();
            case TRASH_S -> f(TRASH, Direction.SOUTH);
            case TRASH_N -> f(TRASH, Direction.NORTH);
            case SPILL -> SPILL.get().defaultBlockState();
            case STAIR_E -> STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.EAST);
            case STAIR_W -> STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.WEST);
            case RAIL_X -> rail(false, false, true, true);
            case RAIL_Z -> rail(true, true, false, false);
            case RAIL_ES -> rail(false, true, true, false);
            case RAIL_WS -> rail(false, true, false, true);
            case RAIL_EN -> rail(true, false, true, false);
            case RAIL_WN -> rail(true, false, false, true);
        };
    }

    /** 0.37.0: gallery railing (dark oak fence with fixed connections). */
    private static BlockState rail(boolean n, boolean s, boolean e, boolean w) {
        return Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(net.minecraft.world.level.block.FenceBlock.NORTH, n)
                .setValue(net.minecraft.world.level.block.FenceBlock.SOUTH, s).setValue(net.minecraft.world.level.block.FenceBlock.EAST, e)
                .setValue(net.minecraft.world.level.block.FenceBlock.WEST, w);
    }

    // --- decoration (signs, posters, chests) ----------------------------------------------------------------------

    private static void set(WorldGenLevel level, ChunkAccess chunk, BlockPos pos, BlockState st) {
        if ((pos.getX() >> 4) == chunk.getPos().x && (pos.getZ() >> 4) == chunk.getPos().z) {
            level.setBlock(pos, st, 2);
        }
    }

    private static void text(WorldGenLevel level, ChunkAccess chunk, String s, int x, int y, int z, Direction face) {
        // reading direction: facing south the letters run west -> east (+x), facing north east -> west
        int step = face == Direction.SOUTH ? 1 : -1;
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == ' ') {
                continue;
            }
            set(level, chunk, new BlockPos(x + k * step, y, z),
                    LETTER.get().defaultBlockState().setValue(DecorBlock.FACING, face).setValue(Letter.GLYPH, Letter.glyph(c)));
        }
    }

    public static void decorate(OfficePlanner p, Cinema.Site s, WorldGenLevel level, ChunkAccess chunk, int baseX, int baseZ) {
        int y = s.yBase;
        int cx0 = chunk.getPos().getMinBlockX();
        int cz0 = chunk.getPos().getMinBlockZ();
        if (cx0 > baseX + 159 || cx0 + 15 < baseX || cz0 > baseZ + 159 || cz0 + 15 < baseZ) {
            return;
        }
        // marquee lettering
        text(level, chunk, "TICKETS", baseX + 77, y + 6, baseZ + Cinema.BOX_Z1 + 1, Direction.SOUTH);
        text(level, chunk, "NOW SHOWING", baseX + 75, y + 8, baseZ + Cinema.LOBBY_Z0, Direction.SOUTH);
        for (int sx : Cinema.STANDS) {
            text(level, chunk, "CONCESSIONS", baseX + sx + 25, y + 6, baseZ + Cinema.STAND_Z - 1, Direction.NORTH);
        }
        for (int f = 0; f < Cinema.FLOORS; f++) {
            int yf = y + f * Cinema.FH;
            // screen numbers over the corridor side of each auditorium (0.37.0: 1–36 over three floors)
            for (int i = 0; i < 6; i++) {
                int x0 = 3 + 26 * i;
                String n1 = Integer.toString(f * Cinema.HALLS + i + 1);
                String n2 = Integer.toString(f * Cinema.HALLS + i + 7);
                text(level, chunk, n1, baseX + x0 + 13, yf + 4, baseZ + 58, Direction.SOUTH);
                text(level, chunk, n2, baseX + x0 + 13 + n2.length() - 1, yf + 4, baseZ + 102, Direction.NORTH);
            }
            if (f > 0) {
                int a = f * Cinema.HALLS + 1;
                text(level, chunk, "SCREENS " + a + " TO " + (a + 11), baseX + 66, yf + 7, baseZ + Cinema.LOBBY_Z0, Direction.SOUTH);
            }
            // posters along both lobby walls
            for (int lx = 3; lx < 157; lx++) {
                int m = (lx - 3) % 26;
                if (m != 5 && m != 21) {
                    continue;
                }
                int v = (int) (s.h(0x9C51, lx, f) * 6);
                set(level, chunk, new BlockPos(baseX + lx, yf + 2, baseZ + Cinema.LOBBY_Z0), poster(Direction.SOUTH, v, false));
                set(level, chunk, new BlockPos(baseX + lx, yf + 3, baseZ + Cinema.LOBBY_Z0), poster(Direction.SOUTH, v, true));
                if (f > 0 || !Cinema.inStand(lx)) {
                    int w = (int) (s.h(0x9C52, lx, f) * 6);
                    set(level, chunk, new BlockPos(baseX + lx, yf + 2, baseZ + Cinema.LOBBY_Z1), poster(Direction.NORTH, w, false));
                    set(level, chunk, new BlockPos(baseX + lx, yf + 3, baseZ + Cinema.LOBBY_Z1), poster(Direction.NORTH, w, true));
                }
            }
        }
        // chests: box office till, concession stock rooms, lost and found at the back of some auditoriums
        chest(p, level, chunk, new BlockPos(baseX + 80, y + 1, baseZ + Cinema.BOX_Z0 + 1), Direction.SOUTH, "cinema_box_office");
        for (int sx : Cinema.STANDS) {
            chest(p, level, chunk, new BlockPos(baseX + sx + 15, y + 1, baseZ + Cinema.LOBBY_Z1), Direction.NORTH, "cinema_concessions");
        }
        for (int j = 0; j < Cinema.FLOORS * Cinema.HALLS; j++) {
            int i = j % Cinema.HALLS;
            if (s.h(0x9C53, j, 0) < 0.45) {
                boolean north = i < 6;
                int x0 = 3 + 26 * (i % 6);
                int t = 52;
                int lz = north ? 56 - t : 104 + t;
                chest(p, level, chunk, new BlockPos(baseX + x0 + 12, y + (j / Cinema.HALLS) * Cinema.FH + Cinema.level(t) + 1, baseZ + lz),
                        north ? Direction.SOUTH : Direction.NORTH, "cinema_lost_found");
            }
        }
    }

    private static BlockState poster(Direction d, int v, boolean upper) {
        return POSTER.get().defaultBlockState().setValue(DecorBlock.FACING, d).setValue(Poster.VARIANT, v).setValue(Poster.UPPER, upper);
    }

    private static void chest(OfficePlanner p, WorldGenLevel level, ChunkAccess chunk, BlockPos pos, Direction d, String table) {
        if ((pos.getX() >> 4) == chunk.getPos().x && (pos.getZ() >> 4) == chunk.getPos().z) {
            MegaDecorator.lootChest(p, level, pos, d, table);
        }
    }
}
