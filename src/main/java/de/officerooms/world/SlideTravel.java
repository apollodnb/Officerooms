package de.officerooms.world;

import de.officerooms.core.ModSounds;
import de.officerooms.hazard.HazardNet;
import de.officerooms.megastructure.Poolrooms;
import de.officerooms.room.OfficePlanner;
import de.officerooms.worldgen.OfficeChunkGenerator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * 0.35.0: the slide openings in the offices. Climb in (right click) and you slide: the screen goes dark, the tube
 * rumbles and shakes, and after a few seconds you shoot out of a water slide into a landing pool of the nearest
 * Poolrooms. The two are not physically connected.
 */
public final class SlideTravel {

    private static final int RIDE = 60;
    private static final Map<UUID, int[]> RIDING = new HashMap<>();
    private static final Map<UUID, Vec3> TARGET = new HashMap<>();

    private SlideTravel() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(SlideTravel::onTick);
    }

    /** Called when a player uses a slide opening (server side). */
    public static void start(ServerPlayer player, net.minecraft.core.BlockPos slide) {
        if (RIDING.containsKey(player.getUUID()) || !(player.level() instanceof ServerLevel sl)) {
            return;
        }
        OfficePlanner p = sl.getChunkSource().getGenerator() instanceof OfficeChunkGenerator gen
                ? gen.sampler(sl.getChunkSource().randomState(), sl).planner() : null;
        // the destination Poolrooms depend only on the slide opening (same for everyone), the exit tube is random
        Vec3 target = p == null ? null : nearestLanding(p, slide.getX(), slide.getZ(), player.getRandom().nextInt(1 << 16));
        if (target == null) {
            player.displayClientMessage(Component.literal("The tube is blocked. Somewhere below, water drips."), true);
            return;
        }
        RIDING.put(player.getUUID(), new int[]{RIDE});
        TARGET.put(player.getUUID(), target);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, RIDE + 30, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RIDE, 6, false, false));
        sl.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SLIDE_RUSH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        HazardNet.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new HazardNet.Quake(RIDE, 0.55F));
    }

    private static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || RIDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, int[]>> it = RIDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, int[]> en = it.next();
            ServerPlayer pl = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (pl == null) {
                it.remove();
                TARGET.remove(en.getKey());
                continue;
            }
            if (--en.getValue()[0] > 0) {
                pl.setDeltaMovement(Vec3.ZERO);
                continue;
            }
            it.remove();
            Vec3 t = TARGET.remove(en.getKey());
            if (t != null && pl.level() instanceof ServerLevel sl) {
                pl.teleportTo(sl, t.x, t.y, t.z, pl.getYRot(), 20.0F);
                de.officerooms.quest.Milestones.grant(pl, "slide_ride");   // 0.38.0
                pl.setDeltaMovement(0, -0.6, 0);
                pl.fallDistance = 0;
                sl.playSound(null, t.x, t.y, t.z, ModSounds.SLIDE_SPLASH.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
                pl.removeEffect(MobEffects.BLINDNESS);
            }
        }
    }

    /** End of a water slide (above its landing pool) in the nearest Poolrooms, or the middle of its biggest hall. */
    static Vec3 nearestLanding(OfficePlanner p, int x, int z, int pick) {
        int rx0 = p.regionOf(x);
        int rz0 = p.regionOf(z);
        for (int r = 0; r <= 24; r++) {
            Vec3 best = null;
            double bd = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    Poolrooms.Site s = p.pools.site(rx0 + dx, rz0 + dz);
                    if (s == null) {
                        continue;
                    }
                    int bx = (rx0 + dx) * p.regionBlocks;
                    int bz = (rz0 + dz) * p.regionBlocks;
                    Vec3 v;
                    if (!s.slideEnds.isEmpty()) {
                        int[] end = s.slideEnds.get(Math.floorMod(pick, s.slideEnds.size()));
                        v = new Vec3(bx + end[0] + 0.5, s.yBase + Poolrooms.FLOOR + 2.2, bz + end[1] + 0.5);
                    } else {
                        v = new Vec3(bx + s.hallX + 0.5, s.yBase + Poolrooms.FLOOR + 1, bz + s.hallZ + 0.5);
                    }
                    // compare region centres, so every exit of one Poolrooms counts the same
                    double cx = bx + p.regionBlocks / 2.0;
                    double cz = bz + p.regionBlocks / 2.0;
                    double d = (cx - x) * (cx - x) + (cz - z) * (cz - z) + (dx * 7 + dz) * 1e-6;
                    if (d < bd) {
                        bd = d;
                        best = v;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }
}
