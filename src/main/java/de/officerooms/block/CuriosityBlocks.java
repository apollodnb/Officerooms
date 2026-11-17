package de.officerooms.block;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.item.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;

/**
 * 0.29.0: blocks of the new special rooms (canteen, mail room, rain room, arcade). Own designs. The vending machine
 * takes an Old Coin and drops something from {@code gameplay/vending}.
 */
public final class CuriosityBlocks {

    private CuriosityBlocks() {
    }

    /** Vending machine (lower half): right-click with an Old Coin. */
    public static class VendingMachine extends DecorBlock {
        public VendingMachine(Properties p) {
            super(p, Block.box(1, 0, 1, 15, 16, 15));
        }

        @Override
        public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            ItemStack held = player.getItemInHand(hand);
            if (!held.is(ModItems.OFFICE_COIN.get())) {
                if (!level.isClientSide) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("It wants a coin."), true);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            if (level instanceof ServerLevel sl) {
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                LootTable table = sl.getServer().getLootData().getLootTable(new ResourceLocation(OfficeRoomsIds.MOD_ID, "gameplay/vending"));
                LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
                Direction f = s.getValue(FACING);
                for (ItemStack out : table.getRandomItems(params)) {
                    ItemEntity e = new ItemEntity(sl, pos.getX() + 0.5 + f.getStepX() * 0.7, pos.getY() + 0.3, pos.getZ() + 0.5 + f.getStepZ() * 0.7, out);
                    e.setDeltaMovement(f.getStepX() * 0.08, 0.05, f.getStepZ() * 0.08);
                    sl.addFreshEntity(e);
                }
                sl.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.7F, 0.6F);
                sl.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    /** Invisible ceiling block of the rain room: water keeps falling from it. */
    public static class RainEmitter extends Block {
        public RainEmitter(Properties p) {
            super(p);
        }

        @Override
        public RenderShape getRenderShape(BlockState s) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return Shapes.empty();
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource rnd) {
            for (int k = 0; k < 3; k++) {
                level.addParticle(ParticleTypes.FALLING_WATER, pos.getX() + rnd.nextDouble(), pos.getY() + 0.9, pos.getZ() + rnd.nextDouble(), 0, 0, 0);
            }
            if (rnd.nextInt(40) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, SoundEvents.WEATHER_RAIN, SoundSource.AMBIENT, 0.25F, 1.0F, false);
            }
        }
    }

    private static Block.Properties props(MapColor c, SoundType st, int light) {
        return Block.Properties.of().mapColor(c).strength(1.5F, 6.0F).sound(st).lightLevel(s -> light).noOcclusion();
    }

    private static RegistryObject<Block> item(String name, RegistryObject<Block> b) {
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties())));
        return b;
    }

    public static final RegistryObject<Block> VENDING = item("vending_machine",
            ModBlocks.BLOCKS.register("vending_machine", () -> new VendingMachine(props(MapColor.COLOR_RED, SoundType.METAL, 0))));
    public static final RegistryObject<Block> VENDING_TOP = item("vending_machine_top",
            ModBlocks.BLOCKS.register("vending_machine_top", () -> new DecorBlock(props(MapColor.COLOR_RED, SoundType.METAL, 8), Block.box(1, 0, 1, 15, 15, 15))));
    public static final RegistryObject<Block> ARCADE = item("arcade_cabinet",
            ModBlocks.BLOCKS.register("arcade_cabinet", () -> new DecorBlock(props(MapColor.COLOR_PURPLE, SoundType.WOOD, 0), Block.box(2, 0, 1, 14, 16, 15))));
    public static final RegistryObject<Block> ARCADE_TOP = item("arcade_cabinet_top",
            ModBlocks.BLOCKS.register("arcade_cabinet_top", () -> new DecorBlock(props(MapColor.COLOR_PURPLE, SoundType.WOOD, 9), Block.box(2, 0, 3, 14, 15, 15))));
    public static final RegistryObject<Block> PARCELS = item("parcel_stack",
            ModBlocks.BLOCKS.register("parcel_stack", () -> new DecorBlock(props(MapColor.WOOD, SoundType.WOOL, 0), Block.box(1, 0, 1, 15, 14, 15))));
    public static final RegistryObject<Block> ARCADE_CARPET = item("arcade_carpet",
            ModBlocks.BLOCKS.register("arcade_carpet", () -> new CarpetBlock(Block.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.3F).sound(SoundType.WOOL))));
    public static final RegistryObject<Block> RAIN = ModBlocks.BLOCKS.register("rain_emitter",
            () -> new RainEmitter(Block.Properties.of().noCollission().noOcclusion().noLootTable().strength(-1.0F, 3600000.0F).replaceable()));

    public static BlockState f(RegistryObject<Block> b, Direction front) {
        return b.get().defaultBlockState().setValue(DecorBlock.FACING, front);
    }

    public static final List<String> NAMES = List.of("vending_machine", "vending_machine_top", "arcade_cabinet", "arcade_cabinet_top", "parcel_stack");

    public static void init() {
    }
}
