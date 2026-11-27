package de.officerooms.entity;

import java.util.EnumSet;

import de.officerooms.core.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 0.16.0: the Partygoer of Level Fun (own design). It looks friendly: it watches you, waves and giggles.
 * 0.27.0 (user request): it no longer follows you; it just stands on its spot, turns to you and waves. Come closer
 * than two blocks, or hit it, and it alerts every Partygoer nearby and they all attack. A sprinting player can still
 * get away (about 5.2 blocks per second).
 */
public class Partygoer extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);
    private static final double KEEP = 7.5;
    private static final double TRIGGER = 2.0;

    private int waveCooldown = 60;
    private boolean wasHostile;
    /** 0.24.0: turned hostile because another Partygoer raised the alarm (does not pass it on again). */
    private boolean alerted;
    private static final double ALERT_RADIUS = 48.0;

    public Partygoer(EntityType<? extends Partygoer> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.02, "wave", "attack");
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        // 0.27.0: no approaching, no strolling: it stands and watches
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F, 1.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
                e -> e.distanceToSqr(this) < TRIGGER * TRIGGER));
    }

    /** While calm: walk towards the nearest visible player, but stop at about seven blocks. */
    private final class Approach extends Goal {
        private Player player;
        private int repath;

        Approach() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private Player find() {
            Player p = level().getNearestPlayer(Partygoer.this, 16.0);
            if (p == null || p.isCreative() || p.isSpectator() || !hasLineOfSight(p) || distanceToSqr(p) <= KEEP * KEEP) {
                return null;
            }
            return p;
        }

        @Override
        public boolean canUse() {
            if (getTarget() != null) {
                return false;
            }
            player = find();
            return player != null;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() == null && player != null && player.isAlive() && distanceToSqr(player) > KEEP * KEEP && distanceToSqr(player) < 18 * 18;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(player, 30.0F, 30.0F);
            if (--repath <= 0) {
                repath = 10;
                getNavigation().moveTo(player, 0.55);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
            player = null;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        boolean hostile = getTarget() != null && getTarget().isAlive();
        if (hostile && !wasHostile) {
            playSound(ModSounds.PARTYGOER_LAUGH.get(), 2.0F, 0.95F + random.nextFloat() * 0.1F);
            if (!alerted && getTarget() instanceof Player victim && !victim.isCreative() && !victim.isSpectator()) {
                // 0.24.0: one turns, the whole party turns
                for (Partygoer o : level().getEntitiesOfClass(Partygoer.class, getBoundingBox().inflate(ALERT_RADIUS, 8, ALERT_RADIUS))) {
                    if (o != this && o.isAlive() && (o.getTarget() == null || !o.getTarget().isAlive())) {
                        o.alerted = true;
                        o.setTarget(victim);
                    }
                }
            }
        }
        if (!hostile) {
            alerted = false;
        }
        wasHostile = hostile;
        if (!hostile && --waveCooldown <= 0) {
            Player p = level().getNearestPlayer(this, 16.0);
            if (p != null && !p.isSpectator() && hasLineOfSight(p)) {
                getNavigation().stop();
                triggerAnim("move", "wave");
                playSound(random.nextBoolean() ? ModSounds.PARTYGOER_HORN.get() : ModSounds.PARTYGOER_GIGGLE.get(), 1.4F, 0.9F + random.nextFloat() * 0.2F);
                waveCooldown = 60 + random.nextInt(60);
            } else {
                waveCooldown = 20;
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("move", "attack");
        boolean hit = super.doHurtTarget(target);
        if (hit && random.nextInt(3) == 0) {
            playSound(ModSounds.PARTYGOER_GIGGLE.get(), 1.5F, 1.1F);
        }
        return hit;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 260;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.PARTYGOER_GIGGLE.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PARTYGOER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PARTYGOER_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }
}
