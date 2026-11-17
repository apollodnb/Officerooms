package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Observer: große, schlanke, dunkle Gestalt (Liminal-World-Update). Sie bewegt sich nicht, greift
 * nicht an und verfolgt niemanden. Sie steht weit entfernt (Ende eines Flurs, hinter einem Fenster) oder
 * taucht kurz zwischen Möbeln auf und verschwindet lautlos, sobald man näher kommt, sie zu lange ansieht,
 * sie berührt oder ihre Zeit abgelaufen ist. Erzeugt nur vom {@link ObserverDirector}.
 *
 * <p>Tiefe Variante (unterste dunkle Ebenen): Verschwindet sie, weil man ihr zu nahe kam, wird es kurz
 * dunkel (Effekt „Dunkelheit“) und hinter dem Spieler flüstert etwas.</p>
 */
public class Observer extends PathfinderMob implements software.bernie.geckolib.animatable.GeoEntity {

    // --- 0.11.0: GeckoLib model and animations (assets from tools/gen_mobs_geo.py) --------------
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.02);
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private int life;
    private int stare;
    private boolean deep;
    private double vanishDistance = 14;

    public Observer(EntityType<? extends Observer> type, Level level) {
        super(type, level);
        setInvulnerable(true);
        setSilent(true);
        setPersistenceRequired();
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** Lebensdauer in Ticks, Verschwinde-Abstand, tiefe Variante. */
    public void setup(int lifeTicks, double vanishDist, boolean deepVariant) {
        this.life = lifeTicks;
        this.vanishDistance = vanishDist;
        this.deep = deepVariant;
    }

    public boolean isDeep() {
        return deep;
    }

    @Override
    protected void registerGoals() {
        // keine KI: sie steht nur da
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && source.getEntity() instanceof Player) {
            vanish(false, "hit");
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        Player p = level().getNearestPlayer(this, 64);
        if (p != null) {
            getLookControl().setLookAt(p, 10F, 10F);
            yBodyRot = yHeadRot;
        }
        if (--life <= 0 || p == null) {
            vanish(false, p == null ? "noplayer" : "life");
            return;
        }
        double d = p.distanceTo(this);
        if (d < vanishDistance) {
            vanish(true, "near " + (int) d);
            return;
        }
        // Zu langes direktes Ansehen: nach 2–4 Sekunden ist sie fort
        Vec3 eye = p.getEyePosition();
        Vec3 to = getEyePosition().subtract(eye).normalize();
        if (p.getViewVector(1F).dot(to) > 0.995 && hasLineOfSight(p)) {
            if (++stare > 40 + random.nextInt(40)) {
                vanish(false, "stare");
            }
        } else {
            stare = Math.max(0, stare - 1);
        }
    }

    private void vanish(boolean approached, String why) {
        if (isRemoved()) {
            return;
        }
        de.officerooms.OfficeRooms.LOGGER.debug("Observer vanishes ({}) at {}", why, blockPosition());
        Player p = level().getNearestPlayer(this, 64);
        if (random.nextInt(3) == 0) {
            playSound(ModSounds.OBSERVER_VANISH.get(), 0.4F, 0.8F);
        }
        if (deep && approached && p != null && level() instanceof ServerLevel) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
            Vec3 behind = p.position().subtract(p.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
            level().playSound(null, behind.x, behind.y + 1.4, behind.z, ModSounds.AMB_WHISPER.get(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.6F, 0.9F);
        }
        discard();
    }

    /** Sichtlinie (Glas zählt als durchsichtig). */
    @Override
    public boolean hasLineOfSight(net.minecraft.world.entity.Entity e) {
        Vec3 a = e.getEyePosition();
        Vec3 b = getEyePosition();
        return level().clip(new ClipContext(a, b, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        // nie dauerhaft: beim Speichern verschwindet sie
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
