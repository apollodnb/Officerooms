package de.officerooms.world;

import de.officerooms.block.ModBlocks;
import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.room.Extras;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

/**
 * 0.9.0: Riding elevators. Jumping on an elevator plate rides up, sneaking rides down. In the generated
 * shafts of the Office Rooms a ride goes to the next stop (a storey with a door) at least "floors per ride"
 * storeys away; elsewhere it goes to the next plate straight above or below (up to 64 blocks).
 */
public final class Elevators {

    private static final String STEP_KEY = OfficeRoomsIds.MOD_ID + ".elevator_step";
    private static final String COOLDOWN_KEY = OfficeRoomsIds.MOD_ID + ".elevator_cd";
    private static final int[] STEPS = {1, 5, 10, 25};

    private Elevators() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Elevators::onJump);
        NeoForge.EVENT_BUS.addListener(Elevators::onPlayerTick);
    }

    private static int step(ServerPlayer player) {
        int s = player.getPersistentData().getInt(STEP_KEY);
        return s <= 0 ? 1 : s;
    }

    public static void cycleStep(ServerPlayer player) {
        int cur = step(player);
        int next = STEPS[0];
        for (int i = 0; i < STEPS.length; i++) {
            if (STEPS[i] == cur) {
                next = STEPS[(i + 1) % STEPS.length];
            }
        }
        player.getPersistentData().putInt(STEP_KEY, next);
        player.displayClientMessage(Component.literal("Elevator: " + next + (next == 1 ? " floor" : " floors")
                + " per ride  (jump = up, sneak = down)").withStyle(ChatFormatting.AQUA), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    private static BlockPos plateUnder(ServerPlayer player) {
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        return player.level().getBlockState(below).is(ModBlocks.ELEVATOR.get()) ? below : null;
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.isSpectator()) {
            BlockPos plate = plateUnder(player);
            if (plate != null) {
                ride(player, plate, 1);
            }
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isShiftKeyDown() && player.onGround() && !player.isSpectator()) {
            BlockPos plate = plateUnder(player);
            if (plate != null) {
                ride(player, plate, -1);
            }
        }
    }

    private static void ride(ServerPlayer player, BlockPos plate, int dir) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now < player.getPersistentData().getLong(COOLDOWN_KEY)) {
            return;
        }
        int step = step(player);
        int targetY = Integer.MIN_VALUE;
        String label = null;
        if (level.dimension() == OfficeRoomsDimension.LEVEL_KEY && level.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen) {
            OfficePlanner p = gen.sampler(level.getChunkSource().randomState(), level).planner();
            int rx = p.regionOf(plate.getX());
            int rz = p.regionOf(plate.getZ());
            Extras.Region ex = p.extras.of(rx, rz);
            if (ex != null && ex.inElevator(plate.getX() - rx * p.regionBlocks, plate.getZ() - rz * p.regionBlocks)
                    && plate.getY() == p.floorY(p.storeyOf(plate.getY()))) {
                // 0.26.0: generated shafts are ridden with the cabin panel (ElevatorRides), not by jumping/sneaking
                player.displayClientMessage(Component.literal("Right-click the floor plate to use the panel.").withStyle(ChatFormatting.GRAY), true);
                player.getPersistentData().putLong(COOLDOWN_KEY, now + 40);
                if (now >= 0) {
                    return;
                }
                int s = p.storeyOf(plate.getY());
                int t = ex.nextStop(s, dir, step);
                if (t < 0) {
                    player.displayClientMessage(Component.literal(dir > 0 ? "Top stop." : "Bottom stop.").withStyle(ChatFormatting.GRAY), true);
                    player.getPersistentData().putLong(COOLDOWN_KEY, now + 10);
                    return;
                }
                targetY = p.floorY(t) + 1;
                label = "Floor " + t;
            }
        }
        if (targetY == Integer.MIN_VALUE) {
            // free-standing plates: next plate straight above / below
            int found = 0;
            for (int d = 2; d <= 64 * step && found < step; d++) {
                BlockPos q = plate.offset(0, dir * d, 0);
                if (level.isOutsideBuildHeight(q) || d > 64 && found == 0) {
                    break;
                }
                if (level.getBlockState(q).is(ModBlocks.ELEVATOR.get())
                        && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                        && level.getBlockState(q.above(2)).getCollisionShape(level, q.above(2)).isEmpty()) {
                    targetY = q.getY() + 1;
                    found++;
                }
            }
            if (targetY == Integer.MIN_VALUE) {
                return;
            }
        }
        player.getPersistentData().putLong(COOLDOWN_KEY, now + 12);
        level.playSound(null, plate.above(), SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5F, 0.7F);
        player.teleportTo(level, plate.getX() + 0.5, targetY, plate.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.setDeltaMovement(0, 0, 0);
        player.fallDistance = 0;
        level.playSound(null, plate.getX() + 0.5, targetY, plate.getZ() + 0.5, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.7F, dir > 0 ? 1.5F : 1.0F);
        if (label != null) {
            player.displayClientMessage(Component.literal(label).withStyle(ChatFormatting.AQUA), true);
        }
    }
}
