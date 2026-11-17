package de.officerooms.block;

import de.officerooms.core.ModSounds;
import de.officerooms.hazard.BlackoutEvents;
import de.officerooms.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 0.22.0: the fuse box of a blackout area. Sparks while broken; three Electronic Components fix it. */
public class FuseBoxBlock extends HorizontalDirectionalBlock {

    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    public static final int COST = 3;

    public FuseBoxBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(BROKEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, BROKEN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case SOUTH -> Block.box(3, 1, 0, 13, 15, 5);
            case EAST -> Block.box(0, 1, 3, 5, 15, 13);
            case WEST -> Block.box(11, 1, 3, 16, 15, 13);
            default -> Block.box(3, 1, 11, 13, 15, 16);
        };
    }

    @Override
    public InteractionResult use(BlockState s, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!s.getValue(BROKEN)) {
            player.displayClientMessage(Component.literal("The fuse box hums quietly."), true);
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(hand);
        boolean creative = player.getAbilities().instabuild;
        if (!creative && (!held.is(ModItems.ELECTRONIC_COMPONENT.get()) || held.getCount() < COST)) {
            player.displayClientMessage(Component.literal("Burnt out. You need " + COST + " Electronic Components in your hand to fix it."), true);
            return InteractionResult.CONSUME;
        }
        if (!creative) {
            held.shrink(COST);
        }
        level.setBlock(pos, s.setValue(BROKEN, false), 3);
        ((ServerLevel) level).sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.3);
        level.playSound(null, pos, ModSounds.OUTPOST_LEVER.get(), SoundSource.BLOCKS, 1.2F, 1.1F);
        if (player instanceof ServerPlayer sp && BlackoutEvents.repaired((ServerLevel) level, pos, sp)) {
            reward((ServerLevel) level, pos, s.getValue(FACING), sp);
        }
        return InteractionResult.CONSUME;
    }

    /** 0.22.1: the maintenance kit that pops out of a repaired fuse box (only when it ended a blackout). */
    private static void reward(ServerLevel l, BlockPos pos, Direction f, ServerPlayer by) {
        de.officerooms.quest.Milestones.grant(by, "fuse_box_repaired");
        var table = l.getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("office_rooms", "gameplay/fuse_box_reward"));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(l)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, by)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.6, z = pos.getZ() + 0.5 + f.getStepZ() * 0.6;
        for (ItemStack st : table.getRandomItems(params)) {
            var it = new net.minecraft.world.entity.item.ItemEntity(l, x, pos.getY() + 0.5, z, st);
            it.setDeltaMovement(f.getStepX() * 0.15 + (l.random.nextDouble() - 0.5) * 0.08, 0.2, f.getStepZ() * 0.15 + (l.random.nextDouble() - 0.5) * 0.08);
            it.setDefaultPickUpDelay();
            l.addFreshEntity(it);
        }
        net.minecraft.world.entity.ExperienceOrb.award(l, new net.minecraft.world.phys.Vec3(x, pos.getY() + 0.5, z), 15 + l.random.nextInt(11));
        by.displayClientMessage(Component.literal("A maintenance kit drops out of the fuse box."), false);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (!s.getValue(BROKEN)) {
            return;
        }
        Direction f = s.getValue(FACING);
        double x = pos.getX() + 0.5 + f.getStepX() * 0.2, z = pos.getZ() + 0.5 + f.getStepZ() * 0.2;
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + (r.nextDouble() - 0.5) * 0.5, pos.getY() + 0.3 + r.nextDouble() * 0.5, z + (r.nextDouble() - 0.5) * 0.5,
                    (r.nextDouble() - 0.5) * 0.2, r.nextDouble() * 0.15, (r.nextDouble() - 0.5) * 0.2);
        }
        if (r.nextInt(3) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.OUTPOST_SPARK.get(), SoundSource.BLOCKS, 0.9F, 0.8F + r.nextFloat() * 0.5F, false);
        }
    }
}
