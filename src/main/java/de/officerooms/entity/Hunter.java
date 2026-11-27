package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
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
 * The Hunter (0.7.0): ein riesiges Wesen aus dünnen, leuchtenden Linien, das nur in den Poolrooms
 * lebt. Bei jedem Besuch der Poolrooms ist mindestens einer unterwegs. Er wittert Spieler auch ohne
 * Sicht und verfolgt sie unermüdlich, ist aber langsamer als ein sprintender Spieler.
 */
public class Hunter extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    // --- 0.11.0: GeckoLib model and animations (assets from tools/gen_mobs_geo.py) --------------
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.03, "notice");
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    /** Wittert Spieler in diesem Umkreis auch ohne Sichtkontakt. */
    public static final double SENSE = 96.0;
    private int humCooldown;

    public Hunter(EntityType<? extends Hunter> type, Level level) {
        super(type, level);
        this.xpReward = 30;
        this.setMaxUpStep(1.1F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e.distanceToSqr(this) <= SENSE * SENSE));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        LivingEntity t = getTarget();
        if (humCooldown > 0) {
            humCooldown--;
        } else if (t != null && t.isAlive()) {
            triggerAnim("move", "notice");
            playSound(ModSounds.HUNTER_NOTICE.get(), 2.5F, 0.85F + random.nextFloat() * 0.2F);
            humCooldown = 200 + random.nextInt(120);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false; // MobSpawner.despawnFar räumt ihn weg, sobald niemand mehr in der Nähe ist
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HUNTER_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(ModSounds.HUNTER_STEP.get(), 0.6F, 0.85F + random.nextFloat() * 0.3F);                // 0.18.0
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HUNTER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HUNTER_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    @Override
    public boolean canBeLeashed(Player player) {
        return false;
    }

    @Override
    public int getMaxHeadXRot() {
        return 30;
    }

    static boolean isHunter(Mob m) {
        return m instanceof Hunter;
    }
}
