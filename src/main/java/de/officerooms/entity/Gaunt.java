package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import org.joml.Vector3f;

/**
 * The Gaunt: ein eigenes Wesen der dunklen untersten Ebenen (Liminal-World-Update), von der Stimmung
 * angelehnt an Waldkreaturen aus Horrorfilmen: etwa so groß wie ein Spieler, ausgezehrt, mit langen
 * Gliedern, glimmend rotem Inneren. Extrem schnell und aggressiv, sieht den Spieler und jagt ihn,
 * schreit grausam beim Entdecken. Spawnt nur in Geschoss 0–4, selten, höchstens eines pro Spieler.
 */
public class Gaunt extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    // --- 0.11.0: GeckoLib model and animations (assets from tools/gen_mobs_geo.py) --------------
    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.012, "scream");
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }



    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(Gaunt.class, EntityDataSerializers.BOOLEAN);
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1.0F, 0.08F, 0.05F), 0.8F);

    private LivingEntity lastTarget;
    private int screamCooldown;

    public Gaunt(EntityType<? extends Gaunt> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    void applyConfig() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.GAUNT_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.GAUNT_DAMAGE.get());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(OfficeRoomsConfig.GAUNT_SPEED.get());
        getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(OfficeRoomsConfig.GAUNT_FOLLOW_RANGE.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_HUNTING, false);
    }

    public boolean isHunting() {
        return entityData.get(DATA_HUNTING);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData data, CompoundTag tag) {
        applyConfig();
        setHealth(getMaxHealth());
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        applyConfig();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.7, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        // 0.6.2: wittert Spieler im Nahbereich auch durch Wände (in den dunklen Ebenen gibt es kaum Sichtlinien)
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                e -> e.distanceToSqr(this) <= Math.pow(OfficeRoomsConfig.GAUNT_SENSE_RANGE.get(), 2)));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                level().addParticle(EMBER, getRandomX(0.4), getY() + 0.6 + random.nextDouble() * 1.2, getRandomZ(0.4), 0, 0, 0);
            }
            return;
        }
        LivingEntity t = getTarget();
        boolean hunting = t != null && t.isAlive();
        entityData.set(DATA_HUNTING, hunting);
        if (screamCooldown > 0) {
            screamCooldown--;
        }
        if (hunting && t != lastTarget && screamCooldown == 0) {
            // grausamer Schrei, sobald es jemanden entdeckt
            playSound(ModSounds.GAUNT_SCREAM.get(), 3.0F, 0.9F + random.nextFloat() * 0.2F);
            triggerAnim("move", "scream");
            screamCooldown = 160;
            callPack(t);
        }
        lastTarget = hunting ? t : null;
    }

    /** Der Schrei ruft alle Gaunts im Umkreis zur Jagd auf dasselbe Opfer – auch ohne Sichtkontakt. */
    private void callPack(LivingEntity prey) {
        int r = OfficeRoomsConfig.GAUNT_ALERT_RADIUS.get();
        if (r <= 0) {
            return;
        }
        for (Gaunt g : level().getEntitiesOfClass(Gaunt.class, getBoundingBox().inflate(r), g -> g != this && g.isAlive())) {
            if (g.getTarget() == null || !g.getTarget().isAlive()) {
                g.setTarget(prey);
                g.lastTarget = prey; // kein Kettenschrei
                g.screamCooldown = Math.max(g.screamCooldown, 60);
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && random.nextInt(4) == 0 && screamCooldown < 100) {
            playSound(ModSounds.GAUNT_SCREAM.get(), 2.0F, 1.1F);
        }
        return hit;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GAUNT_BREATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GAUNT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GAUNT_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.GAUNT_STEP.get(), 0.5F, 0.8F + random.nextFloat() * 0.3F);
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }
}
