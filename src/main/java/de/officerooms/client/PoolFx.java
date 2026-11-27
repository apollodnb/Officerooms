package de.officerooms.client;

import de.officerooms.dimension.OfficeRoomsDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 0.28.1: the Poolrooms feel a little more alive: a faint haze drifts over the water and now and then a ring spreads
 * on the still surface, as if something had just touched it. Client only, purely visual.
 */
public final class PoolFx {

    private static final RandomSource RNG = RandomSource.create();
    private static boolean inPool;
    private static int check;

    private PoolFx() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PoolFx::onTick);
    }

    private static void onTick(ClientTickEvent.Post e) {
        if (false) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || mc.level.dimension() != OfficeRoomsDimension.LEVEL_KEY) {
            inPool = false;
            return;
        }
        if (--check <= 0) {
            check = 20;
            inPool = AmbientSounds.poolroomsNearby(mc, mc.player);
        }
        if (!inPool) {
            return;
        }
        BlockPos base = mc.player.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int k = 0; k < 6; k++) {
            int x = base.getX() + RNG.nextInt(25) - 12;
            int z = base.getZ() + RNG.nextInt(25) - 12;
            for (int dy = 1; dy >= -3; dy--) {
                int y = base.getY() + dy;
                if (mc.level.getBlockState(m.set(x, y, z)).is(Blocks.WATER) && mc.level.getBlockState(m.set(x, y + 1, z)).isAir()) {
                    if (RNG.nextInt(3) == 0) {
                        mc.level.addParticle(ParticleTypes.WHITE_ASH, x + RNG.nextDouble(), y + 1.1 + RNG.nextDouble() * 1.5, z + RNG.nextDouble(),
                                0.0, 0.002, 0.0);
                    }
                    if (RNG.nextInt(140) == 0) {
                        for (int r = 0; r < 10; r++) {
                            double a = r * Math.PI / 5;
                            mc.level.addParticle(ParticleTypes.FISHING, x + 0.5 + Math.cos(a) * 0.4, y + 0.95, z + 0.5 + Math.sin(a) * 0.4,
                                    Math.cos(a) * 0.04, 0.0, Math.sin(a) * 0.04);
                        }
                    }
                    break;
                }
            }
        }
    }
}
