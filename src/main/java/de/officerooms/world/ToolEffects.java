package de.officerooms.world;

import de.officerooms.block.ModBlocks;
import de.officerooms.block.TempLightBlock;
import de.officerooms.entity.HeadlessWarden;
import de.officerooms.entity.Hunter;
import de.officerooms.item.ThrowableToolItem;
import de.officerooms.item.ToolItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/**
 * 0.10.1: Server side of the QoL tools: flashlight beam, glowstick light, flare (light + repels creatures)
 * and noise maker (draws creatures towards the noise).
 */
public final class ToolEffects {

    private record Source(ThrowableToolItem.Kind kind, ResourceKey<Level> dim, BlockPos pos, long until) {
    }

    private static final Map<UUID, BlockPos> BEAMS = new HashMap<>();
    private static final List<Source> SOURCES = new ArrayList<>();

    private ToolEffects() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ToolEffects::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(ToolEffects::onLevelTick);
    }

    public static boolean flashlightAt(Level level, BlockPos pos) {
        return BEAMS.containsValue(pos);
    }

    private static boolean placeLight(Level level, BlockPos pos, int mode, int ticks) {
        BlockState here = level.getBlockState(pos);
        if (!here.isAir() && !here.is(ModBlocks.TEMP_LIGHT.get())) {
            return false;
        }
        if (here.is(ModBlocks.TEMP_LIGHT.get()) && here.getValue(TempLightBlock.MODE) == 2 && mode != 2) {
            return false;
        }
        level.setBlock(pos, ModBlocks.TEMP_LIGHT.get().defaultBlockState().setValue(TempLightBlock.MODE, mode), 3);
        level.scheduleTick(pos, ModBlocks.TEMP_LIGHT.get(), ticks);
        return true;
    }

    /** A thrown tool landed. */
    public static void land(ThrowableToolItem.Kind kind, Level level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(ModBlocks.TEMP_LIGHT.get())) {
            pos = pos.above();
        }
        switch (kind) {
            case GLOWSTICK -> {
                placeLight(level, pos, 0, 1200);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
            }
            case FLARE -> {
                placeLight(level, pos, 2, 400);
                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.7F);
                SOURCES.add(new Source(kind, level.dimension(), pos, level.getGameTime() + 400));
            }
            case NOISE -> {
                level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 2.0F, 1.0F);
                SOURCES.add(new Source(kind, level.dimension(), pos, level.getGameTime() + 200));
            }
        }
    }

    // --- Flashlight -----------------------------------------------------------------------------

    private static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) {
            return;
        }
        long t = p.level().getGameTime();
        if (t % 3 != 0) {
            return;
        }
        ItemStack s = ToolItems.Flashlight.isOn(p.getMainHandItem()) ? p.getMainHandItem()
                : ToolItems.Flashlight.isOn(p.getOffhandItem()) ? p.getOffhandItem() : null;
        if (s == null || p.isSpectator()) {
            BEAMS.remove(p.getUUID());
            return;
        }
        if (t % 21 == 0) {
            s.setDamageValue(s.getDamageValue() + 1);
            if (s.getDamageValue() >= s.getMaxDamage()) {
                s.getOrCreateTag().putBoolean("On", false);
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("The flashlight flickers and dies.")
                        .withStyle(net.minecraft.ChatFormatting.GRAY), true);
                BEAMS.remove(p.getUUID());
                return;
            }
        }
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(28));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        BlockPos target = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos().relative(hit.getDirection()) : BlockPos.containing(end);
        if (target.equals(BEAMS.get(p.getUUID()))) {
            return;
        }
        BEAMS.put(p.getUUID(), target);
        placeLight(p.level(), target, 1, 10);
        BlockPos near = BlockPos.containing(eye.add(p.getLookAngle().scale(2.5)));   // light the space in front of you too
        if (!near.equals(target)) {
            placeLight(p.level(), near, 1, 8);
        }
    }

    // --- Flare and noise maker ------------------------------------------------------------------

    private static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel level) || SOURCES.isEmpty()) {
            return;
        }
        long t = level.getGameTime();
        Iterator<Source> it = SOURCES.iterator();
        while (it.hasNext()) {
            Source s = it.next();
            if (s.dim != level.dimension()) {
                continue;
            }
            if (t > s.until || !level.isLoaded(s.pos)) {
                it.remove();
                continue;
            }
            Vec3 c = Vec3.atCenterOf(s.pos);
            if (s.kind == ThrowableToolItem.Kind.NOISE) {
                if (t % 5 == 0) {
                    level.playSound(null, s.pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 2.5F, (t / 5) % 2 == 0 ? 1.0F : 0.8F);
                    level.sendParticles(ParticleTypes.NOTE, c.x, c.y + 0.3, c.z, 1, 0.2, 0.1, 0.2, 1);
                }
                if (t % 10 == 0) {
                    for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(s.pos).inflate(24, 8, 24),
                            m -> m.isAlive() && m instanceof Enemy && !(m instanceof Hunter))) {
                        m.setTarget(null);
                        m.getNavigation().moveTo(c.x, c.y, c.z, 1.1);
                    }
                }
            } else if (t % 10 == 0) {
                if (t % 40 == 0) {
                    level.playSound(null, s.pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.6F);
                }
                for (Mob m : level.getEntitiesOfClass(Mob.class, new AABB(s.pos).inflate(9, 4, 9),
                        m -> m.isAlive() && m instanceof Enemy && !(m instanceof Hunter) && !(m instanceof HeadlessWarden))) {
                    Vec3 away = m.position().subtract(c);
                    away = away.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : away.normalize();
                    Vec3 to = m.position().add(away.scale(12));
                    m.setTarget(null);
                    m.getNavigation().moveTo(to.x, m.getY(), to.z, 1.4);
                }
            }
        }
    }
}
