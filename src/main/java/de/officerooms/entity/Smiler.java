package de.officerooms.entity;

import de.officerooms.core.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Smiler (0.6.0): eine Gestalt aus dichter Schwärze mit einem leuchtenden Lächeln, nur an dunklen Orten.
 *
 * <ul>
 *   <li>Pirscht sich langsam an (wird schneller, wenn man wegsieht) und kichert ab und zu.</li>
 *   <li>Berührung tötet sofort (Spieler im Kreativ-/Zuschauermodus sind ausgenommen).</li>
 *   <li>Verschwindet vollständig, wenn ein Spieler eine Fackel in der Hand hält und direkt auf ihn zeigt
 *       (höchstens 10 Blöcke, freie Sicht). Helles Licht vertreibt ihn ebenfalls nach kurzer Zeit.</li>
 *   <li>Unverwundbar gegen gewöhnlichen Schaden.</li>
 * </ul>
 */
public class Smiler extends Monster implements software.bernie.geckolib.animatable.GeoEntity {

    // --- 0.11.0: GeckoLib model and animations (assets from tools/gen_mobs_geo.py) --------------
    private final software.bernie.geckolib.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        GeoAnims.register(this, controllers, 0.02, "giggle", "attack");
    }

    @Override
    public software.bernie.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private int brightTicks;
    private int giggleCooldown = 100;
    private Vec3 lastPos = Vec3.ZERO;
    private int stuckTicks;

    public Smiler(EntityType<? extends Smiler> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.16)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F, 1.0F));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Hält der Spieler eine Fackel in einer Hand? */
    public static boolean holdsTorch(Player p) {
        return isTorch(p.getMainHandItem()) || isTorch(p.getOffhandItem());
    }

    private static boolean isTorch(ItemStack s) {
        return s.is(Items.TORCH) || s.is(Items.SOUL_TORCH) || de.officerooms.item.ToolItems.Flashlight.isOn(s);
    }

    /** Zeigt dieser Spieler mit der Fackel direkt auf den Smiler? */
    boolean torchPointedAt(Player p) {
        if (!holdsTorch(p) || p.isSpectator()) {
            return false;
        }
        Vec3 eye = p.getEyePosition();
        Vec3 center = position().add(0, getBbHeight() * 0.6, 0);
        Vec3 to = center.subtract(eye);
        double dist = to.length();
        if (dist > 10.0) {
            return false;
        }
        if (p.getViewVector(1.0F).dot(to.normalize()) < 0.93) {
            return false;
        }
        return level().clip(new ClipContext(eye, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // Veil of dark smoke. 0.11.0: it rises behind and around the face, never in front of it
            net.minecraft.world.phys.Vec3 back = net.minecraft.world.phys.Vec3.directionFromRotation(0, yBodyRot).scale(-0.55);
            double fy = getY() + 1.6;
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX() + back.x + (random.nextDouble() - 0.5) * 0.9,
                        fy + (random.nextDouble() - 0.5) * 0.9, getZ() + back.z + (random.nextDouble() - 0.5) * 0.9, 0, 0.01, 0);
            }
            if (random.nextInt(8) == 0) {
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + back.x * 1.4, fy - 0.2, getZ() + back.z * 1.4, 0, 0.004, 0);
            }
            return;
        }
        // Fackel: sofort fort
        for (Player p : level().players()) {
            if (torchPointedAt(p)) {
                vanish();
                return;
            }
        }
        // helles Licht vertreibt ihn
        int light = level().getBrightness(LightLayer.BLOCK, blockPosition().above());
        brightTicks = light >= 10 ? brightTicks + 1 : 0;
        if (brightTicks > 60) {
            vanish();
            return;
        }
        LivingEntity t = getTarget();
        if (t instanceof Player p && p.isAlive() && !p.isCreative() && !p.isSpectator()) {
            boolean watched = p.getViewVector(1.0F).dot(position().add(0, 1, 0).subtract(p.getEyePosition()).normalize()) > 0.7;
            double speed = watched ? 0.55 : 1.15;
            if (tickCount % 10 == 0) {
                getNavigation().moveTo(p, speed);
            }
            if (--giggleCooldown <= 0) {
                triggerAnim("move", "giggle");
                playSound(ModSounds.SMILER_GIGGLE.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
                giggleCooldown = 160 + random.nextInt(200);
            }
            if (getBoundingBox().inflate(0.15).intersects(p.getBoundingBox())) {
                touch(p);
                return;
            }
            // hängt an Möbeln fest? Dann taucht er im Dunkeln hinter dem Spieler wieder auf.
            if (position().distanceToSqr(lastPos) < 0.04 && distanceToSqr(p) > 6.0) {
                if (++stuckTicks > 60 && !watched) {
                    blink(p);
                    stuckTicks = 0;
                }
            } else {
                stuckTicks = 0;
            }
            lastPos = position();
        } else if (t != null) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    /** Versetzt sich an eine dunkle, freie Stelle 5–9 Blöcke hinter dem Spieler (außerhalb seines Blickfelds). */
    private void blink(Player p) {
        Vec3 look = p.getViewVector(1.0F).multiply(1, 0, 1).normalize();
        for (int attempt = 0; attempt < 12; attempt++) {
            double a = Math.atan2(-look.z, -look.x) + (random.nextDouble() - 0.5) * 1.6;
            double d = 5 + random.nextDouble() * 4;
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(p.getX() + Math.cos(a) * d, p.getY(), p.getZ() + Math.sin(a) * d);
            if (!level().getBlockState(pos.below()).isSolid()
                    || !level().noCollision(this, getType().getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))
                    || level().getBrightness(LightLayer.BLOCK, pos) > de.officerooms.core.OfficeRoomsConfig.SMILER_MAX_LIGHT.get() + 2) {
                continue;
            }
            moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, getYRot(), 0);
            getNavigation().stop();
            playSound(ModSounds.SMILER_BREATH.get(), 0.8F, 0.8F);
            return;
        }
    }

    private void touch(Player p) {
        triggerAnim("move", "attack");
        playSound(ModSounds.SMILER_KILL.get(), 2.0F, 1.0F);
        p.hurt(damageSources().mobAttack(this), Float.MAX_VALUE);
        vanish();
    }

    /** Verschwindet spurlos (Rauch, leises Geräusch). */
    public void vanish() {
        if (isRemoved()) {
            return;
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 40, 0.4, 0.8, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.6, getZ(), 20, 0.2, 0.1, 0.2, 0.0);
        }
        playSound(ModSounds.SMILER_VANISH.get(), 1.2F, 1.0F);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        return false;
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        vanish();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 300;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SMILER_BREATH.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SMILER_VANISH.get();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return true;
    }
}
