package de.officerooms.client;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

/**
 * 0.30.1: hunting voices. While a Flayed or Hound hunts, its full recording loops on the client, follows the mob
 * and stops as soon as it gives up or disappears. One instance per mob, never stacked.
 */
public final class MobLoops {

    private static final Map<Entity, Loop> ACTIVE = new WeakHashMap<>();

    private MobLoops() {
    }

    /** Called every client tick while the mob hunts; starts the loop if it is not already running. */
    public static <T extends Entity> void hunting(T mob, SoundEvent sound, Predicate<T> stillHunting) {
        Loop l = ACTIVE.get(mob);
        if (l != null && !l.isStopped() && Minecraft.getInstance().getSoundManager().isActive(l)) {
            return;
        }
        if (l != null && !l.isStopped() && l.age < 20) {
            return; // just queued, the sound engine has not picked it up yet
        }
        @SuppressWarnings("unchecked")
        Loop n = new Loop(mob, sound, e -> stillHunting.test((T) e));
        ACTIVE.put(mob, n);
        Minecraft.getInstance().getSoundManager().play(n);
    }

    private static final class Loop extends AbstractTickableSoundInstance {
        private final Entity mob;
        private final Predicate<Entity> hunting;
        int age;

        Loop(Entity mob, SoundEvent sound, Predicate<Entity> hunting) {
            super(sound, SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
            this.mob = mob;
            this.hunting = hunting;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.x = mob.getX();
            this.y = mob.getY();
            this.z = mob.getZ();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            age++;
            if (mob.isRemoved() || !mob.isAlive() || !hunting.test(mob)) {
                stop();
                return;
            }
            x = mob.getX();
            y = mob.getY() + mob.getBbHeight() * 0.8;
            z = mob.getZ();
        }
    }
}
