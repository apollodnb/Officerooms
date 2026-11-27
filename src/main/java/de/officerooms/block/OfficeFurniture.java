package de.officerooms.block;

import de.officerooms.megastructure.CinemaBlocks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/**
 * 0.44.0: Office Rooms' own office furniture (own designs, drawn in {@code tools/gen_0440.py}). It replaces the optional
 * OfficeCraft furniture everywhere: desks, cubicles, partitions, office chairs, conference tables, whiteboards, water
 * coolers, printers, coffee machines and desk props. All pieces are ordinary blocks (no block entities), so they can
 * be placed during world generation and cost little to render.
 *
 * <p>Furniture ids are the ones the layouts already use ({@code office_desk}, {@code chair_gray}, …). A piece of
 * furniture is laid out around its master block: {@code w} across (counter-clockwise = negative), {@code d} along the
 * facing (the user side), {@code h} upwards.</p>
 */
public final class OfficeFurniture {

    private OfficeFurniture() {
    }

    /** A furniture block with a part number (which piece of a larger set it is). */
    public static class Piece extends CinemaBlocks.Turned {
        public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

        public Piece(Properties p, double... b) {
            super(p, b[0], b[1], b[2], b[3], b[4], b[5]);
            registerDefaultState(defaultBlockState().setValue(PART, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            super.createBlockStateDefinition(b);
            b.add(PART);
        }
    }

    public static final List<String> NAMES = new ArrayList<>();

    private static BlockBehaviour.Properties props(MapColor c, SoundType st, int light) {
        return Block.Properties.of().mapColor(c).strength(1.5F, 4F).sound(st).noOcclusion().lightLevel(s -> light);
    }

    private static RegistryObject<Block> reg(String name, Supplier<Block> b) {
        NAMES.add(name);
        RegistryObject<Block> r = ModBlocks.BLOCKS.register(name, b);
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    private static RegistryObject<Block> piece(String name, MapColor c, SoundType st, int light, double... box) {
        return reg(name, () -> new Piece(props(c, st, light), box));
    }

    public static final RegistryObject<Block> DESK = piece("office_desk", MapColor.COLOR_LIGHT_GRAY, SoundType.WOOD, 0, 0, 0, 0, 16, 16, 16);
    public static final RegistryObject<Block> COMPUTER = piece("office_computer", MapColor.COLOR_BLACK, SoundType.METAL, 3, 2, 0, 3, 14, 11, 13);
    public static final RegistryObject<Block> PHONE = piece("office_phone", MapColor.COLOR_GRAY, SoundType.METAL, 0, 5, 0, 5, 11, 3, 11);
    public static final RegistryObject<Block> PAPERS = piece("office_papers", MapColor.SNOW, SoundType.WOOL, 0, 3, 0, 4, 13, 3, 12);
    public static final RegistryObject<Block> MUG = piece("office_mug", MapColor.SNOW, SoundType.GLASS, 0, 6, 0, 6, 10, 5, 10);
    public static final RegistryObject<Block> CHAIR_BLACK = piece("office_chair_black", MapColor.COLOR_BLACK, SoundType.WOOL, 0, 2, 0, 2, 14, 8, 14);
    public static final RegistryObject<Block> CHAIR_GRAY = piece("office_chair_gray", MapColor.COLOR_GRAY, SoundType.WOOL, 0, 2, 0, 2, 14, 8, 14);
    public static final RegistryObject<Block> CHAIR_BROWN = piece("office_chair_brown", MapColor.COLOR_BROWN, SoundType.WOOL, 0, 2, 0, 2, 14, 8, 14);
    public static final RegistryObject<Block> PARTITION = piece("office_partition", MapColor.COLOR_BLUE, SoundType.WOOL, 0, 0, 0, 6, 16, 16, 10);
    public static final RegistryObject<Block> CONFERENCE_TABLE = piece("office_conference_table", MapColor.COLOR_BROWN, SoundType.WOOD, 0, 0, 0, 0, 16, 16, 16);
    public static final RegistryObject<Block> WHITEBOARD = piece("office_whiteboard", MapColor.SNOW, SoundType.METAL, 0, 0, 0, 6, 16, 16, 10);
    public static final RegistryObject<Block> WATER_COOLER = piece("office_water_cooler", MapColor.SNOW, SoundType.METAL, 0, 3, 0, 3, 13, 16, 13);
    public static final RegistryObject<Block> PRINTER = piece("office_printer", MapColor.COLOR_LIGHT_GRAY, SoundType.METAL, 0, 1, 0, 2, 15, 10, 14);
    public static final RegistryObject<Block> COFFEE_MACHINE = piece("office_coffee_machine", MapColor.COLOR_BLACK, SoundType.METAL, 0, 3, 0, 4, 13, 12, 13);

    public static void init() {
    }

    // --- layouts ----------------------------------------------------------------------------------------------------

    /** Receives one block of a laid-out piece of furniture (air = a free spot that still belongs to it). */
    public interface Sink {
        void put(BlockPos rel, BlockState state);
    }

    static BlockState p(RegistryObject<Block> b, Direction f, int part) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, f).setValue(Piece.PART, part);
    }

    static BlockPos at(Direction f, int w, int d, int h) {
        Direction left = f.getCounterClockWise();
        return new BlockPos(-left.getStepX() * w + f.getStepX() * d, h, -left.getStepZ() * w + f.getStepZ() * d);
    }

    static int hash(BlockPos master, int salt) {
        long h = master.asLong() * 0x9E3779B97F4A7C15L + salt * 0x632BE59BD9B4E019L;
        h ^= h >>> 29;
        return (int) (h & 0x7FFFFFFF);
    }

    /** Lays out furniture {@code id}. Returns false for unknown ids. {@code master} only seeds the small variations. */
    public static boolean layout(String id, Direction f, BlockPos master, Sink s) {
        switch (id) {
            case "office_desk", "plain_straight_desk", "plain_drawer_desk", "l_shaped_desk", "plain_l_shaped_desk" -> desk(f, master, 0, s);
            case "cubicle_workstation" -> {
                for (int w = -1; w <= 1; w++) {
                    s.put(at(f, w, -1, 0), p(PARTITION, f, 0));
                    s.put(at(f, w, -1, 1), p(PARTITION, f, 1));
                }
                desk(f, master, 0, s);
                for (int w : new int[]{-1, 1}) {
                    Direction side = w < 0 ? f.getClockWise() : f.getCounterClockWise();
                    s.put(at(f, w, 1, 0), p(PARTITION, side, 0));
                    s.put(at(f, w, 1, 1), p(PARTITION, side, 1));
                }
                s.put(at(f, 0, 1, 0), p(chair(hash(master, 3)), f.getOpposite(), 0));
                s.put(at(f, 0, 1, 1), Blocks.AIR.defaultBlockState());
            }
            case "cubicle_partitions", "glass_partition" -> {
                for (int w = -1; w <= 1; w++) {
                    s.put(at(f, w, 0, 0), p(PARTITION, f, 0));
                    s.put(at(f, w, 0, 1), p(PARTITION, f, 1));
                }
            }
            case "chair_black" -> s.put(BlockPos.ZERO, p(CHAIR_BLACK, f, 0));
            case "chair_gray" -> s.put(BlockPos.ZERO, p(CHAIR_GRAY, f, 0));
            case "chair_brown" -> s.put(BlockPos.ZERO, p(CHAIR_BROWN, f, 0));
            case "conference_large" -> conference(f, master, 3, s);
            case "conference_medium" -> conference(f, master, 2, s);
            case "presentation_board" -> {
                for (int k = 0; k < 3; k++) {                       // reads left to right for whoever faces it
                    s.put(at(f, 1 - k, 0, 0), p(WHITEBOARD, f, k));
                    s.put(at(f, 1 - k, 0, 1), p(WHITEBOARD, f, 3 + k));
                }
            }
            case "water_cooler" -> {
                s.put(BlockPos.ZERO, p(WATER_COOLER, f, 0));
                s.put(BlockPos.ZERO.above(), p(WATER_COOLER, f, 1));
            }
            case "vending_machine" -> {
                s.put(BlockPos.ZERO, CuriosityBlocks.VENDING.get().defaultBlockState().setValue(DecorBlock.FACING, f));
                s.put(BlockPos.ZERO.above(), CuriosityBlocks.VENDING_TOP.get().defaultBlockState().setValue(DecorBlock.FACING, f));
            }
            case "printer" -> s.put(BlockPos.ZERO, p(PRINTER, f, 0));
            case "coffee_machine" -> s.put(BlockPos.ZERO, p(COFFEE_MACHINE, f, 0));
            case "desk_lamp" -> s.put(BlockPos.ZERO, RoomDecorBlocks.decor(de.officerooms.furnish.Decor.DESK_LAMP, f));
            case "office_phone" -> s.put(BlockPos.ZERO, p(PHONE, f, 0));
            case "computer_setup" -> s.put(BlockPos.ZERO, p(COMPUTER, f, 0));
            case "paper_stack", "notebook_stack", "scattered_papers" -> s.put(BlockPos.ZERO, p(PAPERS, f, 0));
            case "coffee_mug_white", "coffee_mug_black", "takeaway_coffee_cup" -> s.put(BlockPos.ZERO, p(MUG, f, 0));
            default -> {
                return false;
            }
        }
        return true;
    }

    static RegistryObject<Block> chair(int h) {
        return switch (h % 3) {
            case 0 -> CHAIR_BLACK;
            case 1 -> CHAIR_GRAY;
            default -> CHAIR_BROWN;
        };
    }

    /** Three-wide desk with a drawer pedestal on the right, a computer and a few things on top. */
    static void desk(Direction f, BlockPos master, int d, Sink s) {
        s.put(at(f, -1, d, 0), p(DESK, f, 0));
        s.put(at(f, 0, d, 0), p(DESK, f, 1));
        s.put(at(f, 1, d, 0), p(DESK, f, 2));
        s.put(at(f, 0, d, 1), p(COMPUTER, f, 0));
        BlockState left = switch (hash(master, 1) % 5) {
            case 0 -> p(PHONE, f, 0);
            case 1 -> p(PAPERS, f, 0);
            case 2 -> RoomDecorBlocks.decor(de.officerooms.furnish.Decor.DESK_LAMP, f);
            default -> Blocks.AIR.defaultBlockState();
        };
        BlockState right = switch (hash(master, 2) % 4) {
            case 0 -> p(MUG, f, 0);
            case 1 -> p(PAPERS, f.getClockWise(), 0);
            default -> Blocks.AIR.defaultBlockState();
        };
        s.put(at(f, -1, d, 1), left);
        s.put(at(f, 1, d, 1), right);
    }

    /** Conference table, 2·half+1 long and 3 deep, legs at the corners, papers here and there. */
    static void conference(Direction f, BlockPos master, int half, Sink s) {
        for (int w = -half; w <= half; w++) {
            for (int d = -1; d <= 1; d++) {
                boolean corner = Math.abs(w) == half && Math.abs(d) == 1;
                s.put(at(f, w, d, 0), p(CONFERENCE_TABLE, f, corner ? 1 : 0));
                int h = hash(master, 10 + w * 3 + d) % 9;
                s.put(at(f, w, d, 1), d != 0 && h == 0 ? p(PAPERS, d > 0 ? f : f.getOpposite(), 0)
                        : d != 0 && h == 1 ? p(MUG, f, 0) : Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Occupied cells relative to the master block. */
    public static List<BlockPos> cells(String id, Direction facing) {
        List<BlockPos> out = new ArrayList<>();
        if (!layout(id, facing, BlockPos.ZERO, (rel, st) -> out.add(rel))) {
            return List.of();
        }
        if (!out.contains(BlockPos.ZERO)) {
            out.add(BlockPos.ZERO);
        }
        return out;
    }

    /** Places furniture {@code id}; the caller guarantees that all cells are free and inside the current chunk. */
    public static boolean place(WorldGenLevel level, BlockPos master, String id, Direction facing, int flags) {
        return layout(id, facing, master, (rel, st) -> {
            if (!st.isAir()) {
                level.setBlock(master.offset(rel), st, flags);
            }
        });
    }
}
