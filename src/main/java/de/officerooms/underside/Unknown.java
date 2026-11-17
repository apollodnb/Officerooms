package de.officerooms.underside;

import de.officerooms.core.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;

import java.util.ArrayDeque;

/**
 * 0.19.0: [Unknown], the boss of the Underside. A 20-block centipede made of wire-thin black lines, with a skinned
 * ribcage every third segment and the Flayed's scribble head. It swims through the meadow and the upside-down meadow
 * above it, surfaces after a warning (dust + quake), lunges across the gap and now and then crawls on the meadow.
 * Mechanics live here; the boss is never saved (it comes back on the next visit).
 */
public class Unknown extends Monster {

    public static final int SEGMENTS = 11;
    public static final double SPACING = 1.8;

    enum Mode { INTRO, BURROW, TELEGRAPH, LUNGE, CRAWL, DIVE }

    private final UnknownPart[] parts = new UnknownPart[SEGMENTS];
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    private final ServerBossEvent bossBar = new ServerBossEvent(Component.literal("[Unknown]"), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
    private Mode mode = Mode.INTRO;
    private int modeTicks;
    private Vec3 vel = new Vec3(0, 1, 0);
    private Vec3 aim = Vec3.ZERO;
    private Vec3 surface = Vec3.ZERO;
    private boolean onCeiling;
    private boolean attacking;
    private boolean crawlPlanned;
    private int lunges;
    private int nextSound = 60;

    public Unknown(EntityType<? extends Unknown> type, Level level) {
        super(type, level);
        for (int i = 0; i < SEGMENTS; i++) {
            parts[i] = new UnknownPart(this, i, 2.0F, 3.0F);
        }
        noPhysics = true;
        setNoGravity(true);
        xpReward = 300;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600.0).add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.ARMOR, 10.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 128.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    // --- multipart plumbing -----------------------------------------------------------------------------------
    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    public UnknownPart[] parts() {
        return parts;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        for (int i = 0; i < SEGMENTS; i++) {
            parts[i].setId(id + i + 1);
        }
    }

    boolean hurtFromPart(UnknownPart part, DamageSource source, float amount) {
        return hurt(source, part.index % 3 == 1 ? amount : amount * 0.85F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING) || source.getMsgId().equals("inWall")) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity e) {
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public void travel(Vec3 input) {
        // moved directly by customServerAiStep
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(26);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bossBar.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        bossBar.removePlayer(p);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return ModSounds.UNKNOWN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected float getSoundVolume() {
        return 4.0F;
    }

    // --- body -------------------------------------------------------------------------------------------------
    @Override
    public void tick() {
        super.tick();
        if (trail.isEmpty()) {
            for (int k = 0; k < 60; k++) {
                trail.addLast(position().add(0, -0.5 * k, 0));
            }
        }
        if (trail.getFirst().distanceToSqr(position()) > 0.0025) {
            trail.addFirst(position());
        }
        while (trail.size() > 300) {
            trail.removeLast();
        }
        double need = SPACING, acc = 0;
        int i = 0;
        Vec3 a = position();
        for (Vec3 b : trail) {
            double d = a.distanceTo(b);
            while (i < SEGMENTS && d > 1e-6 && acc + d >= need) {
                place(i++, a.add(b.subtract(a).scale((need - acc) / d)));
                need += SPACING;
            }
            acc += d;
            a = b;
        }
        while (i < SEGMENTS) {
            place(i++, a);
        }
    }

    private void place(int i, Vec3 p) {
        UnknownPart part = parts[i];
        part.xo = part.getX();
        part.yo = part.getY();
        part.zo = part.getZ();
        part.xOld = part.xo;
        part.yOld = part.yo;
        part.zOld = part.zo;
        part.setPos(p.x, p.y, p.z);
    }

    // --- behaviour --------------------------------------------------------------------------------------------
    void startIntro(Vec3 dir) {
        mode = Mode.INTRO;
        modeTicks = 0;
        vel = new Vec3(0, 1.0, 0);
        aim = position().add(dir.scale(12)).with(net.minecraft.core.Direction.Axis.Y, UndersideTerrain.ceil(Mth.floor(getX()), Mth.floor(getZ())) + 8);
    }

    private static int floorTop(double x, double z) {
        return UndersideTerrain.floor(Mth.floor(x), Mth.floor(z)) + 1;
    }

    private static int ceilBottom(double x, double z) {
        return UndersideTerrain.ceil(Mth.floor(x), Mth.floor(z));
    }

    private Player prey() {
        Player best = null;
        double bd = 128 * 128;
        for (Player p : level().players()) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative()) {
                double d = p.distanceToSqr(this);
                if (d < bd) {
                    bd = d;
                    best = p;
                }
            }
        }
        return best;
    }

    private void steer(Vec3 to, double speed, double turn) {
        Vec3 want = to.subtract(position());
        if (want.lengthSqr() < 1e-4) {
            return;
        }
        // 0.25.0: close to the target it turns straight towards it (it used to circle around the aim point,
        // which looked like it was stuck)
        double t = want.length() < speed * 6 ? 1.0 : turn;
        Vec3 dir = vel.lengthSqr() < 1e-6 ? want.normalize() : vel.normalize().lerp(want.normalize(), t).normalize();
        vel = dir.scale(speed);
    }

    private void enterBurrow(boolean ceiling) {
        mode = Mode.BURROW;
        modeTicks = 0;
        onCeiling = ceiling;
        attacking = lunges % 3 == 2 || random.nextFloat() < 0.4F;
        Player p = prey();
        Vec3 c = p != null ? p.position() : position();
        if (attacking) {
            aim = c;
        } else {
            double a = random.nextDouble() * Math.PI * 2, r = 9 + random.nextDouble() * 8;
            aim = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossBar.setProgress(getHealth() / getMaxHealth());
        if (isDeadOrDying()) {
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        modeTicks++;
        Player prey = prey();
        switch (mode) {
            case INTRO -> {
                steer(aim, 0.95, 0.07);
                if (modeTicks == 2) {
                    sound(ModSounds.UNKNOWN_EMERGE.get(), 5.0F);
                }
                if (modeTicks == 14) {
                    sound(ModSounds.UNKNOWN_ROAR.get(), 6.0F);
                }
                if (getY() > ceilBottom(getX(), getZ()) + 5 || modeTicks > 80) {
                    lunges = 1;
                    enterBurrow(true);
                }
            }
            case BURROW -> {
                if (attacking && prey != null) {
                    aim = prey.position();
                }
                double depth = onCeiling ? ceilBottom(aim.x, aim.z) + 6 : floorTop(aim.x, aim.z) - 6;
                steer(new Vec3(aim.x, depth, aim.z), 0.6, 0.18);
                Vec3 h = position().subtract(aim.x, getY(), aim.z);
                if (modeTicks % 4 == 0) {
                    dust(sl, getX(), getZ(), 4, 0.6);
                }
                if (modeTicks % 30 == 0) {
                    sound(ModSounds.UNKNOWN_BURROW.get(), 2.5F);
                }
                if (h.horizontalDistance() < 2.0 || modeTicks > 160) {
                    mode = Mode.TELEGRAPH;
                    modeTicks = 0;
                    surface = new Vec3(getX(), onCeiling ? ceilBottom(getX(), getZ()) : floorTop(getX(), getZ()), getZ());
                    vel = Vec3.ZERO;
                    UndersideEvents.quake(sl, surface, 28, attacking ? 30 : 20, attacking ? 0.7F : 0.4F);
                    sl.playSound(null, surface.x, surface.y, surface.z, ModSounds.UNDERSIDE_QUAKE.get(), SoundSource.HOSTILE, 3.0F, 0.9F);
                }
            }
            case TELEGRAPH -> {
                vel = Vec3.ZERO;
                if (modeTicks % 2 == 0) {
                    dust(sl, surface.x, surface.z, 10, 0.4 + modeTicks / 30.0);
                }
                if (modeTicks >= (attacking ? 32 : 22)) {
                    mode = Mode.LUNGE;
                    modeTicks = 0;
                    Vec3 d;
                    if (prey != null && attacking) {
                        d = prey.position().subtract(surface).multiply(1, 0, 1);
                        d = d.lengthSqr() < 0.5 ? new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5) : d;
                    } else {
                        d = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
                    }
                    d = d.normalize();
                    Vec3 exit = surface.add(d.scale(10));
                    aim = new Vec3(exit.x, onCeiling ? floorTop(exit.x, exit.z) - 7 : ceilBottom(exit.x, exit.z) + 7, exit.z);
                    vel = new Vec3(0, onCeiling ? -1.2 : 1.2, 0);
                    crawlPlanned = onCeiling && random.nextFloat() < 0.45F;
                    sound(ModSounds.UNKNOWN_EMERGE.get(), 5.0F);
                    if (attacking || random.nextFloat() < 0.5F) {
                        sound(ModSounds.UNKNOWN_ROAR.get(), 6.0F);
                    }
                    burst(sl, surface);
                }
            }
            case LUNGE -> {
                steer(aim, 1.15, 0.09);
                boolean down = onCeiling;
                if (down && crawlPlanned && getY() <= floorTop(getX(), getZ()) + 2.5 && modeTicks > 6) {
                    mode = Mode.CRAWL;
                    modeTicks = 0;
                    burst(sl, new Vec3(getX(), floorTop(getX(), getZ()), getZ()));
                } else if (modeTicks > 100 || (down ? getY() < floorTop(getX(), getZ()) - 4 : getY() > ceilBottom(getX(), getZ()) + 4)) {
                    lunges++;
                    enterBurrow(!onCeiling);
                }
            }
            case CRAWL -> {
                Vec3 t = prey != null ? prey.position() : position().add(vel.multiply(5, 0, 5));
                steer(new Vec3(t.x, floorTop(getX(), getZ()) + 0.1, t.z), 0.45, 0.07);
                vel = new Vec3(vel.x, Mth.clamp(floorTop(getX(), getZ()) + 0.1 - getY(), -0.3, 0.3), vel.z);
                if (modeTicks % 3 == 0) {
                    dust(sl, getX(), getZ(), 3, 0.8);
                }
                if (modeTicks > 130) {
                    mode = Mode.DIVE;
                    modeTicks = 0;
                    Vec3 f = vel.multiply(1, 0, 1).normalize().scale(7);
                    aim = position().add(f.x, -9, f.z);
                    sound(ModSounds.UNKNOWN_ROAR.get(), 5.0F);
                }
            }
            case DIVE -> {
                steer(aim, 0.75, 0.15);
                if (getY() < floorTop(getX(), getZ()) - 4 || modeTicks > 60) {
                    lunges++;
                    enterBurrow(false);
                }
            }
        }
        Vec3 np = position().add(vel);
        setPos(np.x, np.y, np.z);
        setDeltaMovement(vel);
        if (vel.lengthSqr() > 1e-4) {
            setYRot((float) (Mth.atan2(-vel.x, vel.z) * Mth.RAD_TO_DEG));
            setXRot((float) (-Mth.atan2(vel.y, vel.horizontalDistance()) * Mth.RAD_TO_DEG));
            yHeadRot = getYRot();
            yBodyRot = getYRot();
        }
        hitPlayers();
        if (--nextSound <= 0) {
            nextSound = 90 + random.nextInt(120);
            boolean buried = mode == Mode.BURROW || mode == Mode.TELEGRAPH;
            sound(buried ? ModSounds.UNKNOWN_BURROW.get() : ModSounds.UNKNOWN_ROAR.get(), buried ? 2.5F : 5.0F);
        }
    }

    private void sound(SoundEvent e, float vol) {
        level().playSound(null, getX(), getY(), getZ(), e, SoundSource.HOSTILE, vol, 0.9F + random.nextFloat() * 0.2F);
    }

    private void hitPlayers() {
        AABB all = getBoundingBox();
        for (UnknownPart p : parts) {
            all = all.minmax(p.getBoundingBox());
        }
        for (Player pl : level().getEntitiesOfClass(Player.class, all.inflate(1))) {
            if (pl.isSpectator() || pl.isCreative()) {
                continue;
            }
            boolean touch = getBoundingBox().inflate(0.3).intersects(pl.getBoundingBox());
            for (int i = 0; i < SEGMENTS && !touch; i++) {
                touch = parts[i].getBoundingBox().inflate(0.2).intersects(pl.getBoundingBox());
            }
            if (touch && pl.invulnerableTime <= 0) {
                if (pl.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
                    Vec3 k = pl.position().subtract(position()).multiply(1, 0, 1).normalize();
                    pl.knockback(1.4, -k.x, -k.z);
                }
            }
        }
    }

    private void dust(ServerLevel sl, double x, double z, int n, double spread) {
        double y = onCeiling ? ceilBottom(x, z) - 0.05 : floorTop(x, z) + 0.1;
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, (onCeiling ? Blocks.ROOTED_DIRT : Blocks.DIRT).defaultBlockState()),
                x, y, z, n, spread, 0.05, spread, 0.15);
        if (!onCeiling && random.nextInt(3) == 0) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GRASS_BLOCK.defaultBlockState()), x, y, z, n / 2 + 1, spread, 0.05, spread, 0.15);
        }
    }

    private void burst(ServerLevel sl, Vec3 at) {
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), at.x, at.y, at.z, 80, 1.6, 0.4, 1.6, 0.4);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 20, 1.2, 0.3, 1.2, 0.03);
    }

    // --- death ------------------------------------------------------------------------------------------------
    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel sl) {
            if (deathTime == 1) {
                sound(ModSounds.UNKNOWN_DEATH.get(), 8.0F);
            }
            if (deathTime % 3 == 0) {
                int idx = SEGMENTS - deathTime / 3;
                Vec3 p = idx >= 0 && idx < SEGMENTS ? parts[idx].position() : position();
                sl.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y + 1.5, p.z, 2, 0.6, 0.6, 0.6, 0);
                sl.sendParticles(ParticleTypes.SQUID_INK, p.x, p.y + 1.5, p.z, 25, 0.8, 0.8, 0.8, 0.05);
            }
            if (deathTime == 40) {
                dropExperience();
                UndersideEvents.defeated(sl, this);
            }
        }
        if (deathTime >= 40 && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        vel = Vec3.ZERO;
    }

    public BlockPos groundBelow() {
        return BlockPos.containing(getX(), floorTop(getX(), getZ()), getZ());
    }
}
