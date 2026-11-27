package de.officerooms.client;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.underside.UndersideClientState;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

/** 0.19.0: camera shake for Underside quakes. */
@Mod.EventBusSubscriber(modid = OfficeRoomsIds.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class UndersideClientFx {

    private UndersideClientFx() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!Minecraft.getInstance().isPaused() && UndersideClientState.left > 0) {
            UndersideClientState.left--;
        }
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        DeathmothRenderer.onCamera(e);                            // 0.24.0
        if (UndersideClientState.left <= 0) {
            return;
        }
        double t = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime() + e.getPartialTick();
        float fade = Math.min(1F, UndersideClientState.left / 15F);
        float a = UndersideClientState.strength * fade * 2.2F;
        e.setPitch(e.getPitch() + (float) (Math.sin(t * 2.7) + 0.5 * Math.sin(t * 6.1)) * a);
        e.setYaw(e.getYaw() + (float) (Math.sin(t * 2.1 + 1.3) + 0.5 * Math.sin(t * 5.3)) * a * 0.7F);
        e.setRoll(e.getRoll() + (float) Math.sin(t * 3.3 + 0.7) * a * 0.8F);
    }
}
