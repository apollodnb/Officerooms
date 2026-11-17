package de.officerooms.entity;

import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.dimension.OfficeRoomsDimension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Performance (0.7.0). Die Dimension ist auf ganzer Höhe bebaut; in den geladenen Chunks ticken
 * Dorfbewohner, Golems, Katzen, Frösche und Wächter auf allen Geschossen über und unter dem Spieler.
 *
 * <ul>
 *   <li>Lebewesen weit weg von jedem Spieler (waagrecht &gt; {@code freezeHorizontal}, senkrecht &gt;
 *   {@code freezeVertical}) und ohne Ziel überspringen ihren Tick. In der Nähe eines Spielers laufen sie
 *   unverändert.</li>
 *   <li>Der Vanilla-Spawner ist in dieser Dimension wirkungslos (Biom ohne Spawner), sucht aber in jedem
 *   Chunk nach Plätzen. Er wird für diese Dimension abgeschaltet; die eigenen Spawns ({@link MobSpawner})
 *   sind davon unabhängig.</li>
 * </ul>
 */
public final class TickThrottle {

    private TickThrottle() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(TickThrottle::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(TickThrottle::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(TickThrottle::onLevelLoad);
        MinecraftForge.EVENT_BUS.addListener(TickThrottle::onLevelTick);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e instanceof Player || !(e.level() instanceof ServerLevel level) || level.dimension() != OfficeRoomsDimension.LEVEL_KEY) {
            return;
        }
        if (!OfficeRoomsConfig.FREEZE_FAR_MOBS.get()) {
            return;
        }
        if (e instanceof Mob m && m.getTarget() != null) {
            return; // jagt gerade: nie anhalten
        }
        double h = OfficeRoomsConfig.FREEZE_HORIZONTAL.get();
        double v = OfficeRoomsConfig.FREEZE_VERTICAL.get();
        double h2 = h * h;
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - e.getX();
            double dz = p.getZ() - e.getZ();
            if (dx * dx + dz * dz <= h2 && Math.abs(p.getY() - e.getY()) <= v) {
                return;
            }
        }
        event.setCanceled(true);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().getLevel(OfficeRoomsDimension.LEVEL_KEY);
        if (level != null) {
            level.setSpawnSettings(false, false);
        }
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == OfficeRoomsDimension.LEVEL_KEY) {
            level.setSpawnSettings(false, false);
        }
    }

    /** Schwierigkeitswechsel setzen die Spawn-Einstellungen aller Welten neu: selten nachziehen. */
    private static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.level instanceof ServerLevel level
                && level.dimension() == OfficeRoomsDimension.LEVEL_KEY && level.getGameTime() % 100 == 3) {
            level.setSpawnSettings(false, false);
        }
    }
}
