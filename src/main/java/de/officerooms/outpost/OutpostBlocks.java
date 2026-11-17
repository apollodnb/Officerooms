package de.officerooms.outpost;

import de.officerooms.block.DecorBlock;
import de.officerooms.block.ModBlocks;
import de.officerooms.block.UndersidePortalBlock;
import de.officerooms.core.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.RegistryObject;
import org.joml.Vector3f;

/** 0.20.0: the equipment of the abandoned research outposts (lever, emergency button, monitors, lights, gateway). */
public final class OutpostBlocks {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static Block.Properties metal(int light) {
        return Block.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> light);
    }

    private static <B extends Block> RegistryObject<B> reg(String name, java.util.function.Supplier<B> b) {
        RegistryObject<B> r = ModBlocks.BLOCKS.register(name, b);
        ModBlocks.DECOR_ITEMS.add(ModBlocks.ITEMS.register(name, () -> new BlockItem(r.get(), new Item.Properties())));
        return r;
    }

    public static final RegistryObject<Block> OUTPOST_WALL = reg("outpost_wall", () -> new Block(Block.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).strength(2.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> OUTPOST_FLOOR = reg("outpost_floor", () -> new Block(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2.0F, 6.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> OUTPOST_CEILING = reg("outpost_ceiling", () -> new Block(Block.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.5F, 6.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> HAZARD_FLOOR = reg("hazard_floor", () -> new Block(Block.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.0F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<PowerLever> POWER_LEVER = reg("power_lever", () -> new PowerLever(metal(0)));
    public static final RegistryObject<EmergencyButton> EMERGENCY_BUTTON = reg("emergency_button", () -> new EmergencyButton(metal(0)));
    public static final RegistryObject<Monitor> OUTPOST_MONITOR = reg("outpost_monitor", () -> new Monitor(metal(0), Block.box(0, 2, 6, 16, 14, 16)));
    public static final RegistryObject<Console> CONTROL_CONSOLE = reg("control_console", () -> new Console(metal(0)));
    public static final RegistryObject<Lamp> WARNING_LIGHT = reg("warning_light", () -> new Lamp(Block.Properties.of().mapColor(MapColor.COLOR_RED).strength(1.0F)
            .sound(SoundType.GLASS).noOcclusion().lightLevel(s -> s.getValue(LIT) ? 11 : 0), Block.box(5, 9, 5, 11, 16, 11), true));
    public static final RegistryObject<Lamp> FACILITY_LIGHT = reg("facility_light", () -> new Lamp(Block.Properties.of().mapColor(MapColor.METAL).strength(1.0F)
            .sound(SoundType.GLASS).noOcclusion().lightLevel(s -> s.getValue(LIT) ? 15 : 0), Block.box(1, 14, 3, 15, 16, 13), false));
    public static final RegistryObject<Frame> GATEWAY_FRAME = reg("gateway_frame", () -> new Frame(Block.Properties.of().mapColor(MapColor.COLOR_GRAY)
            .strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK).lightLevel(s -> s.getValue(ACTIVE) ? 7 : 0)));
    public static final RegistryObject<Gateway> GATEWAY = ModBlocks.BLOCKS.register("dimensional_gateway", () -> new Gateway(Block.Properties.of()
            .mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).noCollission().noOcclusion().sound(SoundType.GLASS).lightLevel(s -> 13)
            .noLootTable().pushReaction(PushReaction.BLOCK)));

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<Controller>> CONTROLLER_BE = ModBlocks.BLOCK_ENTITIES.register("outpost_controller",
            () -> BlockEntityType.Builder.of(Controller::new, POWER_LEVER.get()).build(null));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<MonitorEntity>> MONITOR_BE = ModBlocks.BLOCK_ENTITIES.register("outpost_monitor",
            () -> BlockEntityType.Builder.of(MonitorEntity::new, OUTPOST_MONITOR.get()).build(null));

    private OutpostBlocks() {
    }

    public static void init() {
    }

    static VoxelShape rot(Direction f, double x0, double y0, double z0, double x1, double y1, double z1) {
        // box given for a block facing NORTH (front towards -z); rotate to f
        return switch (f) {
            case SOUTH -> Block.box(16 - x1, y0, 16 - z1, 16 - x0, y1, 16 - z0);
            case EAST -> Block.box(16 - z1, y0, x0, 16 - z0, y1, x1);
            case WEST -> Block.box(z0, y0, 16 - x1, z1, y1, 16 - x0);
            default -> Block.box(x0, y0, z0, x1, y1, z1);
        };
    }

    // --- lever (and the controller of the whole outpost) ----------------------------------------------------
    public static final class PowerLever extends HorizontalDirectionalBlock implements EntityBlock {
        PowerLever(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING, POWERED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext c) {
            return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return rot(s.getValue(FACING), 3, 0, 9, 13, 16, 16);
        }

        @Override
        public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof Controller c) {
                boolean on = !s.getValue(POWERED);
                level.setBlock(pos, s.setValue(POWERED, on), 3);
                level.playSound(null, pos, ModSounds.OUTPOST_LEVER.get(), SoundSource.BLOCKS, 1.5F, on ? 0.9F : 0.75F);
                c.power((ServerLevel) level, on);
                player.displayClientMessage(Component.literal(on ? "MAIN POWER: ON" : "MAIN POWER: OFF"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new Controller(pos, state);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> type) {
            return level.isClientSide || type != CONTROLLER_BE.get() ? null : (l, p, st, be) -> ((Controller) be).tick((ServerLevel) l);
        }
    }

    // --- emergency button -----------------------------------------------------------------------------------
    public static final class EmergencyButton extends Block {
        private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(5, 2, 5, 11, 9, 11), Block.box(2, 9, 2, 14, 13, 14), Block.box(4, 13, 4, 12, 16, 12));

        EmergencyButton(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(POWERED, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(POWERED);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return SHAPE;
        }

        @Override
        public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            ServerLevel sl = (ServerLevel) level;
            sl.setBlock(pos, s.setValue(POWERED, true), 3);
            sl.scheduleTick(pos, this, 12);
            sl.playSound(null, pos, ModSounds.OUTPOST_BUTTON.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
            Controller c = Controller.near(sl, pos);
            player.displayClientMessage(Component.literal(c == null ? "Nothing happens. The button is not connected to anything." : c.press(sl)), true);
            return InteractionResult.CONSUME;
        }

        @Override
        public void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
            level.setBlock(pos, s.setValue(POWERED, false), 3);
        }
    }

    // --- console (desk with a slanted panel) ----------------------------------------------------------------
    public static final class Console extends HorizontalDirectionalBlock {
        Console(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext c) {
            return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return Block.box(0, 0, 0, 16, 14, 16);
        }
    }

    // --- monitors -------------------------------------------------------------------------------------------
    public static final class Monitor extends DecorBlock implements EntityBlock {
        Monitor(Properties p, VoxelShape shape) {
            super(p, shape);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new MonitorEntity(pos, state);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
            if (level.getBlockEntity(pos) instanceof MonitorEntity m && m.phase == Controller.WARNING && r.nextInt(5) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.OUTPOST_BEEP.get(), SoundSource.BLOCKS, 0.35F,
                        0.8F + r.nextFloat() * 0.6F, false);
            }
        }
    }

    public static final class MonitorEntity extends BlockEntity {
        public int phase;
        public long start;

        public MonitorEntity(BlockPos pos, BlockState state) {
            super(MONITOR_BE.get(), pos, state);
        }

        void set(int phase, long start) {
            this.phase = phase;
            this.start = start;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        protected void saveAdditional(CompoundTag t) {
            t.putInt("Phase", phase);
            t.putLong("Start", start);
        }

        @Override
        public void load(CompoundTag t) {
            super.load(t);
            phase = t.getInt("Phase");
            start = t.getLong("Start");
        }

        @Override
        public CompoundTag getUpdateTag() {
            return saveWithoutMetadata();
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }

    // --- lamps ----------------------------------------------------------------------------------------------
    public static final class Lamp extends Block {
        private final VoxelShape shape;
        private final boolean warning;

        Lamp(Properties p, VoxelShape shape, boolean warning) {
            super(p);
            this.shape = shape;
            this.warning = warning;
            registerDefaultState(defaultBlockState().setValue(LIT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(LIT);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return shape;
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
            if (warning && s.getValue(LIT) && r.nextInt(3) == 0) {
                level.addParticle(new DustParticleOptions(new Vector3f(1F, 0.1F, 0.05F), 0.8F), pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.5,
                        pos.getY() + 0.55, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.5, 0, -0.02, 0);
            }
        }
    }

    // --- gateway --------------------------------------------------------------------------------------------
    public static final class Frame extends Block {
        Frame(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(ACTIVE);
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
            if (s.getValue(ACTIVE) && r.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0, 0);
            }
        }
    }

    public static final class Gateway extends Block {
        public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
        private static final VoxelShape X = Block.box(0, 0, 6, 16, 16, 10), Z = Block.box(6, 0, 0, 10, 16, 16);

        Gateway(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.X));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(AXIS);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return s.getValue(AXIS) == Direction.Axis.X ? X : Z;
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (entity instanceof ServerPlayer p && !p.isPassenger()) {
                long now = level.getGameTime();
                if (p.getPersistentData().getLong("office_rooms_gateway_cd") > now) {
                    return;
                }
                p.getPersistentData().putLong("office_rooms_gateway_cd", now + 80);
                level.playSound(null, pos, ModSounds.GATEWAY_OPEN.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
                if (UndersidePortalBlock.sendToOffice(p, pos.getX(), pos.getZ())) {
                    p.displayClientMessage(Component.literal("DESTINATION: UNKNOWN"), true);
                }
            }
        }

        @Override
        public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(new DustParticleOptions(r.nextBoolean() ? new Vector3f(0.4F, 0.95F, 1F) : new Vector3f(0.95F, 0.9F, 0.55F), 1.0F),
                        pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0, 0);
            }
            if (r.nextInt(60) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.GATEWAY_HUM.get(), SoundSource.BLOCKS, 0.6F, 1.0F, false);
            }
        }
    }
}
