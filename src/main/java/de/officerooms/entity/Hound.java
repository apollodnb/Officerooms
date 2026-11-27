package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import de.officerooms.core.OfficeRoomsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The Hound (0.6.0): wolfsartiger Körper mit einem blutigen Menschenkopf. Sehr hoher Schaden.
 *
 * <p>Pilzsuppe: Hält ein Spieler eine Pilzsuppe in der Haupt- oder Nebenhand, greift der Hound ihn nicht
 * an. Er verliert ihn sofort als Ziel (auch mitten im Angriff und auch, wenn er vom Spieler getroffen
 * wird), schnüffelt und entfernt sich. Die Prüfung läuft jeden Tick, Angriffe gegen solche Spieler werden
 * zusätzlich in {@link #doHurtTarget} verworfen.</p>
 */
public class Hound extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    // --- 0.11.0: GeckoLib model and animations (assets from tools/gen_mobs_geo.py) --------------
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.02, "bark", "attack");
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final EntityDataAccessor<Boolean> DATA_ANGRY = SynchedEntityData.defineId(Hound.class, EntityDataSerializers.BOOLEAN);

    private LivingEntity lastTarget;
    private int calmCooldown;
    private Player calmFrom;

    public Hound(EntityType<? extends Hound> type, Level level) {
        super(type, level);
        this.xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)   // 0.27.1: faster (was 0.30)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    /** Hält der Spieler Pilzsuppe in einer Hand? */
    public static boolean holdsStew(Player p) {
        return p.getMainHandItem().is(Items.MUSHROOM_STEW) || p.getOffhandItem().is(Items.MUSHROOM_STEW);
    }

    void applyConfig() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(OfficeRoomsConfig.HOUND_HEALTH.get());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(OfficeRoomsConfig.HOUND_DAMAGE.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_ANGRY, false);
    }

    public boolean isAngry() {
        return entityData.get(DATA_ANGRY);
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
        goalSelector.addGoal(2, new LeaveGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && !holdsStew(p)));
    }

    @Override
    public void setTarget(LivingEntity target) {
        if (target instanceof Player p && holdsStew(p)) {
            calm(p);
            return;
        }
        super.setTarget(target);
    }

    private void calm(Player p) {
        super.setTarget(null);
        setAggressive(false);
        getNavigation().stop();
        if (calmFrom != p || calmCooldown <= 0) {
            playSound(ModSounds.HOUND_SNIFF.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        }
        calmFrom = p;
        calmCooldown = 60;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (isAngry()) {
                net.neoforged.fml.DistExecutor.unsafeRunWhenOn(net.neoforged.api.distmarker.Dist.CLIENT, () -> () ->
                        de.officerooms.client.MobLoops.hunting(this, ModSounds.HOUND_HUNT_LOOP.get(), Hound::isAngry));
            }
            return;
        }
        if (calmCooldown > 0) {
            calmCooldown--;
        }
        LivingEntity t = getTarget();
        if (t instanceof Player p && holdsStew(p)) {
            calm(p);
            t = null;
        }
        boolean angry = t != null && t.isAlive();
        entityData.set(DATA_ANGRY, angry);
        if (angry && t != lastTarget) {
            triggerAnim("move", "bark");                             // 0.30.1: the voice is the client-side hunt loop
        }
        lastTarget = angry ? t : null;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (target instanceof Player p && holdsStew(p)) {
            calm(p);
            return false;
        }
        triggerAnim("move", "attack");
        return super.doHurtTarget(target);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        // auch nach einem Treffer bleibt er friedlich, solange der Spieler die Suppe hält
        if (source.getEntity() instanceof Player p && holdsStew(p)) {
            calm(p);
        }
        return r;
    }

    /** Entfernt sich vom Spieler mit der Suppe. */
    private final class LeaveGoal extends Goal {
        private Vec3 dest;

        LeaveGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (calmCooldown <= 0 || calmFrom == null || !calmFrom.isAlive() || distanceToSqr(calmFrom) > 14 * 14) {
                return false;
            }
            dest = DefaultRandomPos.getPosAway(Hound.this, 12, 4, calmFrom.position());
            return dest != null;
        }

        @Override
        public void start() {
            getNavigation().moveTo(dest.x, dest.y, dest.z, 0.9);
        }

        @Override
        public boolean canContinueToUse() {
            return calmCooldown > 0 && !getNavigation().isDone();
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isAngry() ? null : ModSounds.HOUND_GROWL.get();          // 0.30.1: no growl on top of the hunt loop
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HOUND_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HOUND_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(net.minecraft.sounds.SoundEvents.WOLF_STEP, 0.2F, 0.8F);
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }
}
