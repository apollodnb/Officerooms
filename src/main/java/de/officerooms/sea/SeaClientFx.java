package de.officerooms.sea;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** 0.42.0 (client): the sea dimension is drowned in a thick grey-green fog, outside even more than inside the ships. */
@Mod.EventBusSubscriber(modid = de.officerooms.core.OfficeRoomsIds.MOD_ID, value = Dist.CLIENT)
public final class SeaClientFx {

    static final float R = 0.40F, G = 0.47F, B = 0.43F;

    private SeaClientFx() {
    }

    static boolean here() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension() == SeaEvents.LEVEL_KEY;
    }

    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (!here() || e.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean outside = mc.level.canSeeSky(e.getCamera().getBlockPosition());
        e.setNearPlaneDistance(outside ? 0.0F : 4.0F);
        e.setFarPlaneDistance(outside ? 56.0F : 64.0F);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFogColour(ViewportEvent.ComputeFogColor e) {
        if (!here() || e.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        e.setRed(R);
        e.setGreen(G);
        e.setBlue(B);
    }

    /** No sun, no stars, no clouds: only the fog. */
    public static final class Effects extends DimensionSpecialEffects {
        public Effects() {
            super(Float.NaN, false, SkyType.NONE, false, false);
        }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 colour, float brightness) {
            return new Vec3(R, G, B);
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return true;
        }
    }
}
