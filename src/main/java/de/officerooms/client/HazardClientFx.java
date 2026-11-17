package de.officerooms.client;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.hazard.HazardState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * 0.17.0: the look of an active HAZARD ZONE on the client: a drifting green haze inside the zone (visible from outside
 * too, so the border is clear), dense green fog while the camera is inside, and a gas hiss now and then. Few particles,
 * only near the player.
 */
@Mod.EventBusSubscriber(modid = OfficeRoomsIds.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HazardClientFx {

    private static final DustParticleOptions GAS = new DustParticleOptions(new Vector3f(0.35F, 0.85F, 0.2F), 2.2F);

    private HazardClientFx() {
    }

    private static boolean here(Minecraft mc) {
        return mc.level != null && mc.player != null && mc.level.dimension() == OfficeRoomsDimension.LEVEL_KEY && HazardState.CLIENT.active();
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase != TickEvent.Phase.END || mc.isPaused() || !here(mc)) {
            return;
        }
        HazardState s = HazardState.CLIENT;
        RandomSource r = mc.level.random;
        Vec3 p = mc.player.position();
        for (int i = 0; i < 6; i++) {
            double x = p.x + (r.nextDouble() - 0.5) * 40;
            double y = p.y + (r.nextDouble() - 0.3) * 8;
            double z = p.z + (r.nextDouble() - 0.5) * 40;
            if (s.contains(x, y, z) && mc.level.getBlockState(net.minecraft.core.BlockPos.containing(x, y, z)).isAir()) {
                mc.level.addParticle(r.nextInt(5) == 0 ? ParticleTypes.SNEEZE : GAS, x, y, z, (r.nextDouble() - 0.5) * 0.02, 0.005, (r.nextDouble() - 0.5) * 0.02);
            }
        }
        if (s.contains(p.x, p.y, p.z) && r.nextInt(160) == 0) {
            mc.level.playLocalSound(p.x, p.y, p.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 0.25F, 0.4F, false);
        }
    }

    private static boolean cameraInside(Minecraft mc) {
        if (!here(mc)) {
            return false;
        }
        Vec3 c = mc.gameRenderer.getMainCamera().getPosition();
        return HazardState.CLIENT.contains(c.x, c.y, c.z);
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (cameraInside(Minecraft.getInstance())) {
            e.setNearPlaneDistance(0.0F);
            e.setFarPlaneDistance(Math.min(e.getFarPlaneDistance(), 22.0F));
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (cameraInside(Minecraft.getInstance())) {
            e.setRed(0.10F);
            e.setGreen(0.22F);
            e.setBlue(0.06F);
        }
    }
}
