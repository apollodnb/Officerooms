package de.officerooms.client;

import de.officerooms.core.OfficeRoomsIds;
import de.officerooms.dimension.OfficeRoomsDimension;
import de.officerooms.hazard.BlackoutState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 0.22.0: lights flicker and die when you are inside a blackout; they flicker back when the power returns. */
@Mod.EventBusSubscriber(modid = OfficeRoomsIds.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class BlackoutClient {

    private static float level = 1F;
    private static boolean wasIn;
    private static int flicker;

    private BlackoutClient() {
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase != TickEvent.Phase.END || mc.player == null || mc.level == null || mc.isPaused()) {
            return;
        }
        boolean in = mc.level.dimension() == OfficeRoomsDimension.LEVEL_KEY && BlackoutState.CLIENT.inside(mc.player.getX(), mc.player.getY(), mc.player.getZ());
        if (in != wasIn) {
            flicker = 30;                                                     // 1.5 s of stutter at every change
            wasIn = in;
        }
        float target = in && !mc.player.hasEffect(MobEffects.NIGHT_VISION) ? 0.1F : 1F;
        level += (target - level) * 0.12F;
        float out = level;
        if (flicker > 0) {
            flicker--;
            if (mc.level.random.nextInt(3) == 0) {
                out = mc.level.random.nextBoolean() ? 1F : 0.1F;
            }
        }
        BlackoutState.dim = out;
    }
}
