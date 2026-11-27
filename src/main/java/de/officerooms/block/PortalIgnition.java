package de.officerooms.block;

import de.officerooms.OfficeRooms;
import de.officerooms.dimension.OfficeRoomsDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SculkSensorPhase;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Zugang zu den Office Rooms: Sand (oder roter Sand) fällt auf einen Sculk-Sensor. Sobald der Sensor
 * dadurch aktiviert wird, entsteht genau an seiner Position ein Portal (zwei Blöcke hoch: der Sensor und
 * der Block darüber werden zum Portal).
 *
 * <p>Ablauf: Landet ein fallender Sandblock, verschwindet sein Wesen ({@link EntityLeaveLevelEvent}).
 * Liegt darunter ein Sculk-Sensor, wird die Stelle bis zu 2 Sekunden beobachtet. Ist der Sensor in dieser
 * Zeit aktiv (Vanilla-Vibration), wird das Portal gesetzt. Der Sensor ist nur einen halben Block hoch,
 * daher zerfällt der Sand beim Aufprall in Vanilla zu einem Item (im Spiel geprüft) – das Item wird
 * beim Zünden entfernt. Andere
 * Sensor-Mechaniken bleiben unberührt: Ohne Sand passiert nichts, und der Sensor funktioniert normal.</p>
 */
public final class PortalIgnition {

    private record Pending(ServerLevel level, BlockPos sensor, long until) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private PortalIgnition() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PortalIgnition::onLeave);
        NeoForge.EVENT_BUS.addListener(PortalIgnition::onTick);
    }

    private static boolean isSand(BlockState s) {
        return s.is(Blocks.SAND) || s.is(Blocks.RED_SAND) || s.is(Blocks.SUSPICIOUS_SAND);
    }

    private static boolean isSensor(BlockState s) {
        return s.getBlock() instanceof SculkSensorBlock;
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof FallingBlockEntity fb)) {
            return;
        }
        if (level.dimension() == OfficeRoomsDimension.LEVEL_KEY || !isSand(fb.getBlockState())) {
            return;
        }
        // Nur echte Landungen (discard). Beim Entladen von Chunks oder beim Beenden der Welt verlässt
        // fallender Sand die Welt ebenfalls – dann darf hier kein Block gelesen werden, sonst lädt
        // Minecraft mitten im Entladen einen Chunk nach ("Entity is already tracked!").
        if (fb.getRemovalReason() != net.minecraft.world.entity.Entity.RemovalReason.DISCARDED) {
            return;
        }
        BlockPos at = fb.blockPosition();
        for (BlockPos sensor : new BlockPos[]{at, at.below()}) {
            if (!level.isLoaded(sensor)) {
                continue;
            }
            if (isSensor(level.getBlockState(sensor))) {
                synchronized (PENDING) {
                    PENDING.add(new Pending(level, sensor.immutable(), level.getGameTime() + 40));
                }
                return;
            }
        }
    }

    private static void onTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        synchronized (PENDING) {
            if (PENDING.isEmpty()) {
                return;
            }
            Iterator<Pending> it = PENDING.iterator();
            while (it.hasNext()) {
                Pending p = it.next();
                if (p.level() != level) {
                    continue;
                }
                if (!level.isLoaded(p.sensor())) {
                    it.remove();
                    continue;
                }
                BlockState sensor = level.getBlockState(p.sensor());
                if (!isSensor(sensor)) {
                    it.remove();
                    continue;
                }
                if (sensor.getValue(SculkSensorBlock.PHASE) == SculkSensorPhase.ACTIVE) {
                    ignite(level, p.sensor());
                    it.remove();
                } else if (level.getGameTime() > p.until()) {
                    it.remove();
                }
            }
        }
    }

    static void ignite(ServerLevel level, BlockPos sensor) {
        BlockState portal = ModBlocks.OFFICE_PORTAL.get().defaultBlockState();
        level.setBlock(sensor, portal, 3);
        BlockState up = level.getBlockState(sensor.above());
        if (up.canBeReplaced() || isSand(up)) {
            level.setBlock(sensor.above(), portal, 3);
        }
        // Der beim Aufprall zerfallene Sand wird vom Portal "verschluckt"
        for (net.minecraft.world.entity.item.ItemEntity item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(sensor).inflate(1.5), e -> isSand(net.minecraft.world.level.block.Block.byItem(e.getItem().getItem()).defaultBlockState()))) {
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) {
                item.discard();
            }
            break;
        }
        level.playSound(null, sensor, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 0.6F, 0.5F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, sensor.getX() + 0.5, sensor.getY() + 1, sensor.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.02);
        OfficeRooms.LOGGER.info("Office Rooms portal created at {} in {}", sensor, level.dimension().location());
    }
}
