package de.officerooms.world;

import de.officerooms.dimension.OfficeRoomsDimension;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerSleepInBedEvent;
import net.neoforged.neoforge.event.entity.player.SleepingTimeCheckEvent;
import net.neoforged.bus.api.Event;

/**
 * 0.26.0: beds work in the Office Rooms: sleeping and setting the respawn point.
 *
 * <p>Vanilla only lets players sleep in dimensions marked {@code natural}, but that flag would also make compasses
 * and clocks work and let Nether portals spawn zombified piglins. The dimension therefore stays non-natural and
 * the bed is handled here with the vanilla checks (distance, obstruction, monsters nearby). The fixed time of the
 * dimension counts as night, so players are not woken right away.</p>
 */
public final class OfficeSleep {

    private OfficeSleep() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(OfficeSleep::onTimeCheck);
        NeoForge.EVENT_BUS.addListener(OfficeSleep::onSleep);
    }

    private static boolean office(Player p) {
        return p.level().dimension() == OfficeRoomsDimension.LEVEL_KEY;
    }

    private static void onTimeCheck(SleepingTimeCheckEvent e) {
        if (office(e.getEntity())) {
            e.setResult(Event.Result.ALLOW);
        }
    }

    private static void onSleep(PlayerSleepInBedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !office(p) || p.isSleeping() || !p.isAlive()) {
            return;
        }
        BlockPos pos = e.getPos();
        ServerLevel level = p.serverLevel();
        Vec3 c = Vec3.atBottomCenterOf(pos);
        if (Math.abs(p.getX() - c.x) > 3.5 || Math.abs(p.getY() - c.y) > 2.5 || Math.abs(p.getZ() - c.z) > 3.5) {
            e.setResult(Player.BedSleepingProblem.TOO_FAR_AWAY);
            return;
        }
        if (level.getBlockState(pos.above()).isSuffocating(level, pos.above())) {
            e.setResult(Player.BedSleepingProblem.OBSTRUCTED);
            return;
        }
        p.setRespawnPosition(level.dimension(), pos, p.getYRot(), false, true);
        if (!p.isCreative()) {
            AABB box = new AABB(c.x - 8, c.y - 5, c.z - 8, c.x + 8, c.y + 5, c.z + 8);
            if (!level.getEntitiesOfClass(Monster.class, box, m -> m.isPreventingPlayerRest(p)).isEmpty()) {
                e.setResult(Player.BedSleepingProblem.NOT_SAFE);
                return;
            }
        }
        p.startSleeping(pos);
        p.awardStat(Stats.SLEEP_IN_BED);
        CriteriaTriggers.SLEPT_IN_BED.trigger(p);
        level.updateSleepingPlayerList();
        e.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);   // handled here; vanilla must not continue (no message)
    }
}
