package de.officerooms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 0.23.0: furniture and surfaces of the new standard rooms (training room, executive office, reception,
 * restroom). The order of {@link #DECOR} matches {@link de.officerooms.furnish.Decor}.
 */
public final class RoomDecorBlocks {

    enum Fx { NONE, DRIP, TICK, BELL, PHONE, STATIC, HUM, SLIDE }

    /** Facing decoration with a shape that turns with the block (models face north). */
    public static class Piece extends DecorBlock {
        private final VoxelShape[] shapes = new VoxelShape[4];
        private final Fx fx;

        Piece(Properties p, double[] b, Fx fx) {
            super(p, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
            this.fx = fx;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
                double[] r = switch (d) {
                    case SOUTH -> new double[]{16 - x1, 16 - z1, 16 - x0, 16 - z0};
                    case EAST -> new double[]{16 - z1, x0, 16 - z0, x1};
                    case WEST -> new double[]{z0, 16 - x1, z1, 16 - x0};
                    default -> new double[]{x0, z0, x1, z1};
                };
                shapes[d.get2DDataValue()] = Block.box(r[0], b[1], r[1], r[2], b[4], r[3]);
            }
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            return shapes[s.getValue(FACING).get2DDataValue()];
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
            if (fx == Fx.DRIP) {
                // the tap below the mirror never quite closes
                Direction f = s.getValue(FACING);
                double x = pos.getX() + 0.5 + f.getStepX() * 0.3;
                double z = pos.getZ() + 0.5 + f.getStepZ() * 0.3;
                if (rnd.nextInt(3) == 0) {
                    level.addParticle(ParticleTypes.DRIPPING_WATER, x, pos.getY() - 0.05, z, 0, 0, 0);
                }
                if (rnd.nextInt(14) == 0) {
                    level.playLocalSound(x, pos.getY() - 0.4, z, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.5F, 0.9F + rnd.nextFloat() * 0.2F, false);
                }
            } else if (fx == Fx.TICK && rnd.nextInt(2) == 0) {
                // it ticks; the hands never move
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.08F, 1.9F, false);
            } else if (fx == Fx.PHONE && rnd.nextInt(700) == 0) {
                // 0.28.0: an old phone that rings for nobody
                for (int k = 0; k < 2; k++) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.6F, 1.6F + k * 0.12F, false);
                }
            } else if (fx == Fx.STATIC) {
                // 0.28.0: the TV hisses quietly and flickers
                if (rnd.nextInt(6) == 0) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.12F, 1.8F, false);
                }
            } else if (fx == Fx.HUM && rnd.nextInt(60) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.18F, 1.7F, false);
            } else if (fx == Fx.BELL && rnd.nextInt(900) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.35F, 2.0F, false);
            }
        }

        @Override
        public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (fx == Fx.SLIDE) {                                     // 0.35.0: climb into the slide
                if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    de.officerooms.world.SlideTravel.start(sp, pos);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (fx != Fx.BELL) {
                return InteractionResult.PASS;
            }
            level.playSound(player, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.0F, 2.0F);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static final List<RegistryObject<Block>> DECOR = new ArrayList<>();

    private static Block.Properties props(MapColor c, SoundType st, int light, boolean solid) {
        Block.Properties p = Block.Properties.of().mapColor(c).strength(1.0F, 3.0F).sound(st).lightLevel(s -> light);
        return solid ? p : p.noOcclusion();
    }

    private static RegistryObject<Block> item(String name, RegistryObject<Block> b) {
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
        return b;
    }

    private static void piece(String name, MapColor c, SoundType st, int light, Fx fx, double... b) {
        boolean solid = b[0] == 0 && b[1] == 0 && b[2] == 0 && b[3] == 16 && b[4] == 16 && b[5] == 16 && light == 0;
        DECOR.add(item(name, ModBlocks.BLOCKS.register(name, () -> new Piece(props(c, st, light, solid), b, fx))));
    }

    private static RegistryObject<Block> carpet(String name, MapColor c, SoundType st) {
        return item(name, ModBlocks.BLOCKS.register(name, () -> new CarpetBlock(Block.Properties.of().mapColor(c).strength(0.3F).sound(st))));
    }

    public static final RegistryObject<Block> FLOOR_TILE = item("restroom_floor_tile", ModBlocks.BLOCKS.register("restroom_floor_tile",
            () -> new Block(Block.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5F, 6.0F).sound(SoundType.STONE))));
    public static final RegistryObject<Block> RUG = carpet("executive_rug", MapColor.COLOR_RED, SoundType.WOOL);
    public static final RegistryObject<Block> DOORMAT = carpet("doormat", MapColor.WOOD, SoundType.WOOL);

    static {
        MapColor w = MapColor.QUARTZ, m = MapColor.METAL;
        piece("restroom_wall_tile", MapColor.COLOR_LIGHT_GREEN, SoundType.STONE, 0, Fx.NONE, 0, 0, 0, 16, 16, 16);
        piece("stall_partition", MapColor.SAND, SoundType.METAL, 0, Fx.NONE, 7, 2, 0, 9, 16, 16);
        piece("stall_partition_top", MapColor.SAND, SoundType.METAL, 0, Fx.NONE, 7, 0, 0, 9, 13, 16);
        piece("stall_door_locked", MapColor.SAND, SoundType.METAL, 0, Fx.NONE, 0, 2, 0, 16, 16, 2);
        piece("stall_door_locked_top", MapColor.SAND, SoundType.METAL, 0, Fx.NONE, 0, 0, 0, 16, 12, 2);
        piece("bathroom_mirror", m, SoundType.GLASS, 0, Fx.DRIP, 1, 1, 15, 15, 15, 16);
        piece("hand_dryer", w, SoundType.METAL, 0, Fx.NONE, 4, 2, 11, 12, 12, 16);
        piece("paper_towel_dispenser", w, SoundType.METAL, 0, Fx.NONE, 4, 2, 12, 12, 15, 16);
        piece("projection_screen", MapColor.SNOW, SoundType.WOOL, 4, Fx.NONE, 0, 0, 15, 16, 16, 16);
        piece("projection_screen_top", MapColor.SNOW, SoundType.WOOL, 4, Fx.NONE, 0, 0, 13, 16, 16, 16);
        piece("ceiling_projector", MapColor.COLOR_GRAY, SoundType.METAL, 3, Fx.NONE, 5, 11, 3, 11, 16, 12);
        piece("stacking_chair", MapColor.COLOR_BLUE, SoundType.METAL, 0, Fx.NONE, 2, 0, 2, 14, 16, 14);
        piece("flipchart", MapColor.SNOW, SoundType.WOOD, 0, Fx.NONE, 1, 0, 6, 15, 16, 12);
        piece("executive_chair", MapColor.COLOR_BLACK, SoundType.WOOL, 0, Fx.NONE, 2, 0, 2, 14, 16, 15);
        piece("desk_lamp", MapColor.COLOR_GREEN, SoundType.GLASS, 10, Fx.NONE, 3, 0, 5, 13, 9, 11);
        piece("wall_clock", MapColor.SNOW, SoundType.WOOD, 0, Fx.TICK, 3, 3, 15, 13, 13, 16);
        piece("reception_desk", MapColor.WOOD, SoundType.WOOD, 0, Fx.NONE, 0, 0, 0, 16, 16, 16);
        piece("service_bell", MapColor.GOLD, SoundType.METAL, 0, Fx.BELL, 5, 0, 5, 11, 4, 11);
        piece("waiting_chair", MapColor.COLOR_CYAN, SoundType.METAL, 0, Fx.NONE, 1, 0, 2, 15, 16, 14);
        piece("magazine_stack", MapColor.COLOR_RED, SoundType.WOOL, 0, Fx.NONE, 3, 0, 3, 13, 3, 13);
        piece("office_plant", MapColor.PLANT, SoundType.AZALEA_LEAVES, 0, Fx.NONE, 3, 0, 3, 13, 16, 13);
        piece("queue_display", MapColor.COLOR_BLACK, SoundType.METAL, 5, Fx.NONE, 2, 4, 14, 14, 11, 16);
        piece("ticket_dispenser", MapColor.COLOR_RED, SoundType.METAL, 0, Fx.NONE, 5, 3, 12, 11, 12, 16);
        // 0.28.0: hotel rework
        piece("hotel_phone", MapColor.SAND, SoundType.WOOD, 0, Fx.PHONE, 4, 0, 5, 12, 5, 12);
        piece("static_tv", MapColor.COLOR_BLACK, SoundType.METAL, 6, Fx.STATIC, 2, 0, 2, 14, 12, 15);
        piece("ice_machine", MapColor.METAL, SoundType.METAL, 2, Fx.HUM, 1, 0, 2, 15, 16, 16);
        piece("housekeeping_cart", MapColor.COLOR_GRAY, SoundType.METAL, 0, Fx.NONE, 1, 0, 3, 15, 13, 13);
        piece("hotel_sconce", MapColor.GOLD, SoundType.GLASS, 9, Fx.NONE, 5, 4, 11, 11, 13, 16);
        // 0.35.0: the opening of a water slide in an office wall
        piece("slide_entrance", MapColor.SNOW, SoundType.STONE, 3, Fx.SLIDE, 0, 0, 11, 16, 16, 16);
    }

    public static BlockState decor(int index, Direction front) {
        return DECOR.get(index).get().defaultBlockState().setValue(DecorBlock.FACING, front);
    }

    public static boolean isCover(BlockState s) {
        return s.is(FLOOR_TILE.get()) || s.is(RUG.get()) || s.is(DOORMAT.get());
    }

    public static void init() {
    }

    private RoomDecorBlocks() {
    }
}
