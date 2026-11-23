package de.officerooms.entity;

import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.dimension.OfficeRoomsDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Lässt den Observer extrem selten und unvorhersehbar erscheinen (Liminal-World-Update).
 *
 * <ul>
 *     <li>Alle 20 s pro Spieler ein Wurf mit {@code observerChance} (Standard 2 %, also im Mittel etwa
 *     alle 17 Minuten eine Begegnung), nie zwei Observer gleichzeitig beim selben Spieler.</li>
 *     <li>Position: entlang der vier Rasterachsen (Flure verlaufen achsparallel). Gesucht wird ein Platz,
 *     den der Spieler mit freier Sicht sehen kann (auch durch Glas), bevorzugt dunkel und weit weg.</li>
 *     <li>Niemals vor dem Spieler: nur in Richtungen, in die er gerade nicht schaut (über 70° zur
 *     Blickrichtung), und mindestens 18 Blöcke entfernt; dreht er sich um, steht sie am Ende des Flurs.</li>
 *     <li>30 %: kurzes Auftauchen in 9–16 Blöcken (zwischen Möbeln, in einer Tür) für 3–5 Sekunden.</li>
 * </ul>
 */
public final class ObserverDirector {

    private static final int INTERVAL = 400;

    private ObserverDirector() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ObserverDirector::onTick);
    }

    private static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != OfficeRoomsDimension.LEVEL_KEY || level.getGameTime() % INTERVAL != 13
                || !OfficeRoomsConfig.mobsAllowed(level)) {   // 0.41.2
            return;
        }
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || level.random.nextDouble() >= OfficeRoomsConfig.OBSERVER_CHANCE.get()) {
                continue;
            }
            if (!level.getEntitiesOfClass(Observer.class, p.getBoundingBox().inflate(96)).isEmpty()) {
                continue;
            }
            trySpawn(level, p, level.random.nextDouble() < 0.3);
        }
    }

    /** Versucht eine Erscheinung beim Spieler. Liefert true bei Erfolg. */
    public static boolean trySpawn(ServerLevel level, ServerPlayer p, boolean peek) {
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        List<BlockPos> candidates = new ArrayList<>();
        List<BlockPos> dark = new ArrayList<>();
        int minD = peek ? 9 : 18;
        int maxD = peek ? 16 : 44;
        BlockPos start = p.blockPosition();
        for (Direction d : dirs) {
            Vec3 dv = new Vec3(d.getStepX(), 0, d.getStepZ());
            if (look.dot(dv) > Math.cos(Math.toRadians(70))) {
                continue; // nicht in Blickrichtung erscheinen
            }
            for (int k = 1; k <= maxD; k++) {
                BlockPos q = start.relative(d, k);
                if (!seeThrough(level, q) || !seeThrough(level, q.above())) {
                    break;
                }
                if (k < minD || !standable(level, q)) {
                    continue;
                }
                Vec3 eye = Vec3.atBottomCenterOf(q).add(0, 2.3, 0);
                if (level.clip(new ClipContext(p.getEyePosition(), eye, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType()
                        != HitResult.Type.MISS) {
                    continue;
                }
                candidates.add(q);
                if (level.getBrightness(LightLayer.BLOCK, q) <= 7) {
                    dark.add(q);
                }
            }
        }
        List<BlockPos> pool = !dark.isEmpty() ? dark : candidates;
        if (pool.isEmpty()) {
            return false;
        }
        BlockPos at = pool.get(level.random.nextInt(pool.size()));
        if (!peek) {
            // am Ende des Flurs: weitester Kandidat in dieser Liste
            for (BlockPos c : pool) {
                if (c.distSqr(start) > at.distSqr(start)) {
                    at = c;
                }
            }
        }
        Observer o = ModEntities.OBSERVER.get().create(level);
        if (o == null) {
            return false;
        }
        double y = at.getY() + Math.max(0, level.getBlockState(at).getCollisionShape(level, at).max(Direction.Axis.Y));
        o.moveTo(at.getX() + 0.5, y, at.getZ() + 0.5, 0, 0);
        o.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, p.getEyePosition());
        boolean deep = at.getY() < -38;
        o.setup(peek ? 60 + level.random.nextInt(40) : 400 + level.random.nextInt(800), peek ? 6 : 14, deep);
        if (!level.noCollision(o, o.getBoundingBox())) {
            return false;
        }
        level.addFreshEntity(o);
        return true;
    }

    private static boolean seeThrough(ServerLevel level, BlockPos q) {
        BlockState s = level.getBlockState(q);
        return s.getCollisionShape(level, q).isEmpty() || s.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock
                || s.is(net.minecraft.tags.BlockTags.IMPERMEABLE) || s.getVisualShape(level, q, net.minecraft.world.phys.shapes.CollisionContext.empty()).isEmpty();
    }

    private static boolean standable(ServerLevel level, BlockPos q) {
        BlockState feet = level.getBlockState(q);
        boolean feetOk = feet.getCollisionShape(level, q).isEmpty() || feet.getBlock() instanceof net.minecraft.world.level.block.CarpetBlock;
        return feetOk && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()
                && level.getBlockState(q.above(2)).getCollisionShape(level, q.above(2)).isEmpty()
                && level.getBlockState(q.below()).isFaceSturdy(level, q.below(), Direction.UP)
                && level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new AABB(q)).isEmpty();
    }
}
