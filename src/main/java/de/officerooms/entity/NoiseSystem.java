package de.officerooms.entity;

import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Geräuscherkennung für den Headless Warden – getrennt von KI und Audio.
 *
 * <p>Quelle sind die Vanilla-Spielereignisse ({@link GameEvent}), die Forge über
 * {@link VanillaGameEvent} für jedes Ereignis in jeder Welt meldet, plus Sprünge
 * ({@link LivingEvent.LivingJumpEvent}, dafür gibt es kein Vanilla-Ereignis). Es werden keine
 * Sculk-Blöcke oder Vanilla-Vibrationsempfänger verwendet.</p>
 *
 * <p>Jedes Ereignis bekommt eine Hörweite; lautere Aktionen reichen weiter. Gezählt werden nur
 * Ereignisse, die ein Spieler (oder sein Geschoss) auslöst. Die Hörweite wird <b>nicht</b> durch
 * Wände oder geschlossene Türen begrenzt (keine Verdeckungsprüfung) – der Warden hört durch Wände.</p>
 *
 * <p>Schleichen: Ereignisse aus dem Vanilla-Tag {@code ignore_vibrations_sneaking} (Schritte, Landen,
 * Schwimmen, Gleiten …) werden beim Schleichen ignoriert – genau wie bei Sculk-Sensoren. Die übrigen
 * Geräusche eines schleichenden Spielers zählen mit halber Reichweite.</p>
 */
public final class NoiseSystem {

    private static final Map<GameEvent, Double> RADIUS = new IdentityHashMap<>();

    static {
        RADIUS.put(GameEvent.STEP.value(), 10.0);              // Gehen (Rennen: siehe SPRINT_STEP)
        RADIUS.put(GameEvent.HIT_GROUND.value(), 14.0);        // Landen
        RADIUS.put(GameEvent.SPLASH.value(), 12.0);
        RADIUS.put(GameEvent.SWIM.value(), 10.0);
        RADIUS.put(GameEvent.FLAP.value(), 8.0);
        RADIUS.put(GameEvent.ELYTRA_GLIDE.value(), 10.0);
        RADIUS.put(GameEvent.BLOCK_OPEN.value(), 18.0);        // Türen, Falltüren, Zauntore
        RADIUS.put(GameEvent.BLOCK_CLOSE.value(), 18.0);
        RADIUS.put(GameEvent.BLOCK_DESTROY.value(), 20.0);     // Blöcke abbauen
        RADIUS.put(GameEvent.BLOCK_PLACE.value(), 14.0);       // Blöcke setzen
        RADIUS.put(GameEvent.BLOCK_CHANGE.value(), 10.0);
        RADIUS.put(GameEvent.BLOCK_ACTIVATE.value(), 12.0);
        RADIUS.put(GameEvent.BLOCK_DEACTIVATE.value(), 12.0);
        RADIUS.put(GameEvent.BLOCK_ATTACH.value(), 8.0);
        RADIUS.put(GameEvent.BLOCK_DETACH.value(), 8.0);
        RADIUS.put(GameEvent.CONTAINER_OPEN.value(), 14.0);    // Schubladen, Truhen
        RADIUS.put(GameEvent.CONTAINER_CLOSE.value(), 14.0);
        RADIUS.put(GameEvent.ENTITY_DAMAGE.value(), 16.0);     // Treffer
        RADIUS.put(GameEvent.PROJECTILE_SHOOT.value(), 22.0);
        RADIUS.put(GameEvent.PROJECTILE_LAND.value(), 12.0);
        RADIUS.put(GameEvent.PRIME_FUSE.value(), 20.0);
        RADIUS.put(GameEvent.EXPLODE.value(), 64.0);
        RADIUS.put(GameEvent.EAT.value(), 8.0);
        RADIUS.put(GameEvent.DRINK.value(), 8.0);
        RADIUS.put(GameEvent.ITEM_INTERACT_FINISH.value(), 8.0);
        RADIUS.put(GameEvent.EQUIP.value(), 6.0);
        RADIUS.put(GameEvent.SHEAR.value(), 10.0);
        RADIUS.put(GameEvent.INSTRUMENT_PLAY.value(), 48.0);
        RADIUS.put(GameEvent.NOTE_BLOCK_PLAY.value(), 32.0);
        RADIUS.put(GameEvent.JUKEBOX_PLAY.value(), 48.0);
        RADIUS.put(GameEvent.ENTITY_PLACE.value(), 10.0);
        RADIUS.put(GameEvent.TELEPORT.value(), 16.0);
        RADIUS.put(GameEvent.FLUID_PLACE.value(), 10.0);
        RADIUS.put(GameEvent.FLUID_PICKUP.value(), 10.0);
    }

    static final double SPRINT_STEP = 18.0;
    static final double JUMP = 12.0;

    private NoiseSystem() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NoiseSystem::onGameEvent);
        NeoForge.EVENT_BUS.addListener(NoiseSystem::onJump);
    }

    private static Player playerOf(Entity e) {
        if (e instanceof Player p) {
            return p;
        }
        if (e instanceof Projectile proj && proj.getOwner() instanceof Player p) {
            return p;
        }
        return null;
    }

    private static void onGameEvent(VanillaGameEvent event) {
        Level level = event.getLevel();
        // Performance: Schritte aller Wesen in allen Dimensionen kommen hier an; früh aussteigen.
        if (level.isClientSide || level.dimension() != de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY) {
            return;
        }
        Player player = playerOf(event.getCause());
        if (player == null) {
            return;
        }
        net.minecraft.core.Holder<GameEvent> geh = event.getVanillaEvent();
        GameEvent ge = geh.value();
        // Wie Vanilla-Vibrationsempfänger: vorsichtiges Gehen (Schleichen) dämpft Schritte, Landen usw. vollständig.
        if (player.isSteppingCarefully() && geh.is(GameEventTags.IGNORE_VIBRATIONS_SNEAKING)) {
            return;
        }
        Double radius = RADIUS.get(ge);
        if (radius == null) {
            return;
        }
        double r = radius;
        if (ge == GameEvent.STEP.value() && player.isSprinting()) {
            r = SPRINT_STEP;
        }
        if (player.isCrouching()) {
            r *= 0.5;
        }
        emit(level, event.getEventPosition(), player, r);
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide && !p.isCrouching()
                && p.level().dimension() == de.officerooms.dimension.OfficeRoomsDimension.LEVEL_KEY) {
            emit(p.level(), p.position(), p, JUMP);
        }
    }

    /** Verteilt ein Geräusch an alle Headless Warden in Hörweite (ohne Verdeckung durch Wände). */
    public static int emit(Level level, Vec3 pos, Player source, double radius) {
        double r = radius * OfficeRoomsConfig.WARDEN_HEARING.get();
        int n = 0;
        for (HeadlessWarden w : level.getEntitiesOfClass(HeadlessWarden.class, new AABB(pos, pos).inflate(r))) {
            if (w.isAlive() && w.position().distanceToSqr(pos) <= r * r) {
                w.hear(pos, source, r);
                n++;
            }
        }
        return n;
    }
}
