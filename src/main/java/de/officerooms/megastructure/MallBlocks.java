package de.officerooms.megastructure;

import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.core.ModSounds;
import de.officerooms.sea.ShipBlocks;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

/** 0.43.0: blocks of the Dead Mall (own designs, drawn in {@code tools/gen_0430.py}). */
public final class MallBlocks {

    private MallBlocks() {
    }

    public static final List<String> NAMES = new ArrayList<>();

    /** Ceiling speaker: now and then a few bars of tired music, or the chime of an announcement nobody makes. */
    public static class Speaker extends CinemaBlocks.Turned {
        private static long next;

        public Speaker(Properties p) {
            super(p, 3, 13, 3, 13, 16, 13);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return Shapes.empty();
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
            long now = System.currentTimeMillis();
            if (now < next || rnd.nextInt(6) != 0) {
                return;
            }
            next = now + 26000 + rnd.nextInt(30000);                 // one speaker at a time, every half minute or so
            boolean chime = rnd.nextInt(5) == 0;
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, chime ? ModSounds.MALL_CHIME.get() : ModSounds.MALL_MUZAK.get(),
                    SoundSource.AMBIENT, chime ? 1.4F : 1.0F, 0.94F + rnd.nextFloat() * 0.06F, false);
        }
    }

    /** Fountain spout: it still drips. */
    public static class Spout extends Block {
        public Spout(Properties p) {
            super(p);
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
            if (rnd.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.3 + rnd.nextDouble() * 0.4, pos.getY() + 0.05, pos.getZ() + 0.3 + rnd.nextDouble() * 0.4, 0, 0, 0);
            }
        }
    }

    // --- registration ------------------------------------------------------------------------------------------------

    static BlockBehaviour.Properties props(MapColor c, SoundType st, float hard, int light) {
        return Block.Properties.of().mapColor(c).strength(hard, hard * 4).sound(st).lightLevel(s -> light);
    }

    private static RegistryObject<Block> reg(String name, Supplier<Block> b) {
        NAMES.add(name);
        RegistryObject<Block> r = ModBlocks.BLOCKS.register(name, b);
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    private static RegistryObject<Block> full(String name, MapColor c, SoundType st, float hard, int light) {
        return reg(name, () -> new Block(props(c, st, hard, light)));
    }

    private static RegistryObject<Block> turned(String name, MapColor c, SoundType st, int light, double... b) {
        return reg(name, () -> new CinemaBlocks.Turned(props(c, st, 1.5F, light).noOcclusion(), b[0], b[1], b[2], b[3], b[4], b[5]));
    }

    private static RegistryObject<Block> thin(String name, MapColor c, SoundType st, int light, double... b) {
        return reg(name, () -> new ShipBlocks.Thin(props(c, st, 1.0F, light).noOcclusion(), b));
    }

    private static RegistryObject<Block> lamp(String name, MapColor c, int light) {
        return reg(name, () -> new ShipBlocks.Lamp(props(c, SoundType.GLASS, 0.8F, 0).lightLevel(s -> s.getValue(ShipBlocks.Lamp.LIT) ? light : 0)));
    }

    private static RegistryObject<Block> glass(String name, MapColor c) {
        return reg(name, () -> new ShipBlocks.Glass(props(c, SoundType.GLASS, 1.5F, 0).noOcclusion()
                .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)));
    }

    // floors, walls, ceilings
    public static final RegistryObject<Block> TILE = full("mall_tile", MapColor.SAND, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> TILE_ACCENT = full("mall_tile_accent", MapColor.TERRACOTTA_ORANGE, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> TILE_CRACKED = full("mall_tile_cracked", MapColor.SAND, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> SHOP_FLOOR = full("mall_shop_floor", MapColor.QUARTZ, SoundType.STONE, 1.5F, 0);
    public static final RegistryObject<Block> SHOP_CARPET = full("mall_shop_carpet", MapColor.COLOR_PURPLE, SoundType.WOOL, 1F, 0);
    public static final RegistryObject<Block> DEPT_FLOOR = full("mall_dept_floor", MapColor.COLOR_GRAY, SoundType.WOOL, 1F, 0);
    public static final RegistryObject<Block> SERVICE_FLOOR = full("mall_service_floor", MapColor.STONE, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> WALL = full("mall_wall", MapColor.SAND, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> DEPT_WALL = full("mall_dept_wall", MapColor.COLOR_LIGHT_GRAY, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> SERVICE_WALL = full("mall_service_wall", MapColor.STONE, SoundType.STONE, 3F, 0);
    public static final RegistryObject<Block> CEILING = full("mall_ceiling", MapColor.SAND, SoundType.WOOD, 1.5F, 0);
    public static final RegistryObject<Block> SERVICE_CEILING = full("mall_service_ceiling", MapColor.STONE, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> FACADE = full("mall_facade", MapColor.COLOR_BROWN, SoundType.METAL, 2F, 0);
    public static final RegistryObject<Block> FRAME = full("mall_frame", MapColor.COLOR_BROWN, SoundType.METAL, 3F, 0);
    public static final RegistryObject<Block> PILLAR = full("mall_pillar", MapColor.SAND, SoundType.STONE, 3F, 0);
    public static final RegistryObject<Block> SKYLIGHT = full("mall_skylight", MapColor.COLOR_LIGHT_GRAY, SoundType.GLASS, 2F, 9);
    public static final RegistryObject<Block> ROOF_BEAM = full("mall_roof_beam", MapColor.COLOR_GRAY, SoundType.METAL, 3F, 0);
    public static final RegistryObject<Block> SHUTTER = full("mall_shutter", MapColor.COLOR_GRAY, SoundType.METAL, 4F, 0);
    public static final RegistryObject<Block> PLANTER = full("mall_planter", MapColor.TERRACOTTA_BROWN, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> FOUNTAIN_BASIN = full("mall_fountain_basin", MapColor.COLOR_CYAN, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> FOUNTAIN_RIM = full("mall_fountain_rim", MapColor.SAND, SoundType.STONE, 2F, 0);
    public static final RegistryObject<Block> FOUNTAIN_SPOUT = reg("mall_fountain_spout", () -> new Spout(props(MapColor.COLOR_LIGHT_GRAY, SoundType.METAL, 3F, 0)));
    public static final RegistryObject<Block> ESCALATOR_SIDE = full("mall_escalator_side", MapColor.METAL, SoundType.METAL, 3F, 0);
    public static final RegistryObject<Block> ESCALATOR = reg("mall_escalator",
            () -> new StairBlock(() -> ESCALATOR_SIDE.get().defaultBlockState(), props(MapColor.METAL, SoundType.METAL, 3F, 0)));
    public static final RegistryObject<Block> NIGHT_GLASS = full("mall_night_glass", MapColor.COLOR_BLACK, SoundType.GLASS, 3F, 0);
    public static final RegistryObject<Block> STOREFRONT = glass("mall_storefront_glass", MapColor.NONE);
    public static final RegistryObject<Block> LEASE_GLASS = glass("mall_lease_glass", MapColor.SNOW);
    public static final RegistryObject<Block> RAIL = reg("mall_glass_rail", () -> new IronBarsBlock(props(MapColor.NONE, SoundType.GLASS, 1.5F, 0).noOcclusion()));
    public static final RegistryObject<Block> LIGHT = lamp("mall_ceiling_light", MapColor.SNOW, 13);
    public static final RegistryObject<Block> SERVICE_LIGHT = lamp("mall_service_light", MapColor.COLOR_YELLOW, 8);

    // fittings and furniture
    public static final RegistryObject<Block> BENCH = turned("mall_bench", MapColor.WOOD, SoundType.WOOD, 0, 0, 0, 3, 16, 9, 15);
    public static final RegistryObject<Block> PLANT = reg("mall_plant", () -> new ShipBlocks.Thin(props(MapColor.PLANT, SoundType.GRASS, 0.3F, 0).noOcclusion(),
            2, 0, 2, 14, 16, 14));
    public static final RegistryObject<Block> DIRECTORY = turned("mall_directory", MapColor.COLOR_BROWN, SoundType.METAL, 2, 2, 0, 6, 14, 16, 10);
    public static final RegistryObject<Block> DIRECTORY_TOP = turned("mall_directory_top", MapColor.COLOR_BROWN, SoundType.METAL, 4, 2, 0, 6, 14, 14, 10);
    public static final RegistryObject<Block> WET_FLOOR = thin("mall_wet_floor_sign", MapColor.COLOR_YELLOW, SoundType.WOOD, 0, 4, 0, 5, 12, 13, 11);
    public static final RegistryObject<Block> CART = turned("mall_cart", MapColor.METAL, SoundType.METAL, 0, 2, 0, 1, 14, 14, 15);
    public static final RegistryObject<Block> KIDDIE_RIDE = turned("mall_kiddie_ride", MapColor.COLOR_RED, SoundType.METAL, 2, 2, 0, 1, 14, 16, 15);
    public static final RegistryObject<Block> CLOTHES_RACK = turned("mall_clothes_rack", MapColor.COLOR_LIGHT_GRAY, SoundType.METAL, 0, 0, 0, 5, 16, 16, 11);
    public static final RegistryObject<Block> SHELF = turned("mall_shelf", MapColor.COLOR_LIGHT_GRAY, SoundType.METAL, 0, 0, 0, 2, 16, 16, 16);
    public static final RegistryObject<Block> SHOE_SHELF = turned("mall_shoe_shelf", MapColor.WOOD, SoundType.WOOD, 0, 0, 0, 6, 16, 16, 16);
    public static final RegistryObject<Block> COUNTER = turned("mall_counter", MapColor.COLOR_BROWN, SoundType.WOOD, 0, 0, 0, 0, 16, 15, 16);
    public static final RegistryObject<Block> REGISTER = turned("mall_register", MapColor.COLOR_GRAY, SoundType.METAL, 0, 3, 0, 3, 13, 8, 13);
    public static final RegistryObject<Block> TV = turned("mall_tv", MapColor.COLOR_BLACK, SoundType.GLASS, 6, 1, 0, 3, 15, 13, 15);
    public static final RegistryObject<Block> TV_OFF = turned("mall_tv_off", MapColor.COLOR_BLACK, SoundType.GLASS, 0, 1, 0, 3, 15, 13, 15);
    public static final RegistryObject<Block> FOOD_COUNTER = turned("mall_food_counter", MapColor.COLOR_RED, SoundType.METAL, 0, 0, 0, 0, 16, 16, 16);
    public static final RegistryObject<Block> FRYER = turned("mall_fryer", MapColor.METAL, SoundType.METAL, 0, 1, 0, 1, 15, 15, 15);
    public static final RegistryObject<Block> MENU = turned("mall_menu_board", MapColor.COLOR_BLACK, SoundType.WOOD, 6, 0, 0, 14, 16, 16, 16);
    public static final RegistryObject<Block> FOOD_TABLE = turned("mall_food_table", MapColor.COLOR_ORANGE, SoundType.METAL, 0, 1, 0, 1, 15, 14, 15);
    public static final RegistryObject<Block> FOOD_CHAIR = turned("mall_food_chair", MapColor.COLOR_ORANGE, SoundType.METAL, 0, 3, 0, 3, 13, 16, 13);
    public static final RegistryObject<Block> KIOSK = turned("mall_kiosk", MapColor.COLOR_GREEN, SoundType.WOOD, 3, 0, 0, 0, 16, 16, 16);
    public static final RegistryObject<Block> OUT_OF_ORDER = thin("mall_out_of_order", MapColor.COLOR_RED, SoundType.METAL, 0, 6, 0, 6, 10, 16, 10);
    public static final RegistryObject<Block> SPEAKER = reg("mall_speaker", () -> new Speaker(props(MapColor.SNOW, SoundType.METAL, 1F, 0).noOcclusion()));
    public static final RegistryObject<Block> PET_TANK = turned("mall_pet_tank", MapColor.COLOR_CYAN, SoundType.GLASS, 3, 0, 0, 1, 16, 14, 15);
    public static final RegistryObject<Block> SALON_CHAIR = turned("mall_salon_chair", MapColor.COLOR_BLACK, SoundType.METAL, 0, 2, 0, 2, 14, 16, 14);
    public static final RegistryObject<Block> MIRROR = thin("mall_mirror", MapColor.SNOW, SoundType.GLASS, 0, 0, 0, 15, 16, 16, 16);
    public static final RegistryObject<Block> SIGN_LEASE = thin("mall_for_lease_sign", MapColor.SNOW, SoundType.WOOD, 0, 1, 2, 15, 15, 14, 16);
    public static final RegistryObject<Block> EMPLOYEES = thin("mall_employees_only", MapColor.COLOR_GRAY, SoundType.METAL, 0, 1, 4, 15, 15, 12, 16);

    public static void init() {
    }

    // --- states ------------------------------------------------------------------------------------------------------

    public static BlockState f(RegistryObject<Block> b, Direction front) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, front);
    }

    public static BlockState state(Mall.Part part) {
        BlockState lamp = LIGHT.get().defaultBlockState();
        BlockState svc = SERVICE_LIGHT.get().defaultBlockState();
        return switch (part) {
            case AIR, OFFICE -> Blocks.AIR.defaultBlockState();
            case RING, WALL -> WALL.get().defaultBlockState();
            case TILE -> TILE.get().defaultBlockState();
            case TILE_B -> TILE_ACCENT.get().defaultBlockState();
            case TILE_C -> TILE_CRACKED.get().defaultBlockState();
            case SHOP_FLOOR -> SHOP_FLOOR.get().defaultBlockState();
            case SHOP_CARPET -> SHOP_CARPET.get().defaultBlockState();
            case SVC_FLOOR -> SERVICE_FLOOR.get().defaultBlockState();
            case SVC_WALL -> SERVICE_WALL.get().defaultBlockState();
            case CEILING -> CEILING.get().defaultBlockState();
            case SVC_CEILING -> SERVICE_CEILING.get().defaultBlockState();
            case LIGHT -> lamp;
            case LIGHT_OFF -> lamp.setValue(ShipBlocks.Lamp.LIT, false);
            case SVC_LIGHT -> svc;
            case SVC_LIGHT_OFF -> svc.setValue(ShipBlocks.Lamp.LIT, false);
            case SKYLIGHT -> SKYLIGHT.get().defaultBlockState();
            case ROOF_BEAM -> ROOF_BEAM.get().defaultBlockState();
            case FRAME -> FRAME.get().defaultBlockState();
            case FACADE -> FACADE.get().defaultBlockState();
            case GLASS -> STOREFRONT.get().defaultBlockState();
            case LEASE_GLASS -> LEASE_GLASS.get().defaultBlockState();
            case SHUTTER -> SHUTTER.get().defaultBlockState();
            case NIGHT_GLASS -> NIGHT_GLASS.get().defaultBlockState();
            case ESC_N -> ESCALATOR.get().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            case ESC_S -> ESCALATOR.get().defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            case ESC_SIDE -> ESCALATOR_SIDE.get().defaultBlockState();
            case RAIL -> RAIL.get().defaultBlockState();
            case PLANTER -> PLANTER.get().defaultBlockState();
            case PLANT -> f(PLANT, Direction.NORTH);
            case FOUNTAIN_RIM -> FOUNTAIN_RIM.get().defaultBlockState();
            case FOUNTAIN_BASIN -> FOUNTAIN_BASIN.get().defaultBlockState();
            case FOUNTAIN_SPOUT -> FOUNTAIN_SPOUT.get().defaultBlockState();
            case DEPT_FLOOR -> DEPT_FLOOR.get().defaultBlockState();
            case DEPT_WALL -> DEPT_WALL.get().defaultBlockState();
            case PILLAR -> PILLAR.get().defaultBlockState();
        };
    }

    /** Glass railing with its connections (bit 0 north, 1 south, 2 east, 3 west). */
    public static BlockState rail(int mask) {
        return RAIL.get().defaultBlockState().setValue(IronBarsBlock.NORTH, (mask & 1) != 0).setValue(IronBarsBlock.SOUTH, (mask & 2) != 0)
                .setValue(IronBarsBlock.EAST, (mask & 4) != 0).setValue(IronBarsBlock.WEST, (mask & 8) != 0);
    }
}
