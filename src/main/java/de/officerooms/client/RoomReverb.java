package de.officerooms.client;

import com.mojang.blaze3d.audio.Channel;
import de.officerooms.OfficeRooms;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.core.OfficeRoomsIds;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.client.event.sound.PlayStreamingSourceEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

/**
 * 0.31.0: reverb by room size. In the Office Rooms dimensions the client measures the space around the player
 * (26 rays, up to 48 blocks) a few times per second: small furnished offices with carpet stay dry and muffled,
 * long corridors, atriums, tiled pools and cinema halls ring out with a long, bright tail.
 *
 * <p>Uses the OpenAL EFX extension (one shared EAX reverb on an auxiliary send). Every world sound that starts while
 * the player is in the dimension gets the send; music and listener-relative sounds (hum, UI) stay dry. If EFX is
 * missing or anything goes wrong, the effect switches itself off for the session and sounds play as before.</p>
 */
public final class RoomReverb {

    private static final int RAYS = 26;
    private static final double RANGE = 48.0;
    private static final Vec3[] DIRS = new Vec3[RAYS];

    static {
        int k = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        DIRS[k++] = new Vec3(x, y, z).normalize();
                    }
                }
            }
        }
    }

    // client thread: measured and smoothed room values
    private static float size = 4F;      // mean free distance (blocks)
    private static float soft = 0.5F;    // share of soft surfaces (wool, carpet)
    private static int ray;
    private static final float[] DIST = new float[RAYS];
    private static final boolean[] SOFT = new boolean[RAYS];

    // shared with the sound thread
    private static volatile boolean active;
    private static volatile float decay = 1F;
    private static volatile float wet;
    private static volatile float hf = 0.5F;
    private static volatile float reflDelay = 0.01F;

    // sound thread only
    private static boolean init;
    private static boolean failed;
    private static boolean eax;
    private static int slot;
    private static int effect;
    private static float appliedDecay = -1F;
    private static float appliedWet = -1F;
    private static float appliedHf = -1F;
    private static Field sourceField;

    private RoomReverb() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(RoomReverb::onTick);
        MinecraftForge.EVENT_BUS.addListener(RoomReverb::onPlay);
        MinecraftForge.EVENT_BUS.addListener(RoomReverb::onStream);
    }

    // --- measuring (client thread) -------------------------------------------------------------------------------

    private static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        boolean here = mc.level != null && mc.player != null && OfficeRoomsConfig.ROOM_REVERB.get()
                && mc.level.dimension().location().getNamespace().equals(OfficeRoomsIds.MOD_ID);
        if (!here) {
            active = false;
            return;
        }
        // 6 rays per tick: the whole sphere is refreshed every ~5 ticks without a frame spike
        Vec3 eye = mc.player.getEyePosition();
        for (int n = 0; n < 6; n++) {
            int i = ray;
            ray = (ray + 1) % RAYS;
            Vec3 end = eye.add(DIRS[i].scale(RANGE));
            BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() == HitResult.Type.MISS) {
                DIST[i] = (float) RANGE;
                SOFT[i] = false;
            } else {
                DIST[i] = (float) hit.getLocation().distanceTo(eye);
                SoundType st = mc.level.getBlockState(hit.getBlockPos()).getSoundType();
                SOFT[i] = st == SoundType.WOOL || st == SoundType.MOSS_CARPET || st == SoundType.MOSS;
            }
        }
        float sum = 0F;
        int softN = 0;
        for (int i = 0; i < RAYS; i++) {
            sum += DIST[i];
            if (SOFT[i]) {
                softN++;
            }
        }
        float mean = sum / RAYS;
        float s = softN / (float) RAYS;
        size += (mean - size) * 0.15F;            // smooth: walking through a door fades the room in
        soft += (s - soft) * 0.15F;
        float strength = OfficeRoomsConfig.ROOM_REVERB_STRENGTH.get().floatValue();
        decay = Mth.clamp(0.35F + size * 0.13F, 0.35F, 7.0F) * (1.0F - soft * 0.45F);
        wet = Mth.clamp(0.08F + size / (float) RANGE * 0.62F, 0.05F, 0.7F) * (1.0F - soft * 0.55F) * strength;
        hf = Mth.clamp(0.95F - soft * 0.7F, 0.15F, 0.95F);
        reflDelay = Mth.clamp(size / 343F, 0.003F, 0.25F);
        active = strength > 0F;
    }

    // --- applying (sound thread) -----------------------------------------------------------------------------------

    private static void onPlay(PlaySoundSourceEvent event) {
        apply(event.getSound(), event.getChannel());
    }

    private static void onStream(PlayStreamingSourceEvent event) {
        apply(event.getSound(), event.getChannel());
    }

    private static void apply(SoundInstance sound, Channel channel) {
        if (failed || sound == null || channel == null) {
            return;
        }
        try {
            int src = source(channel);
            boolean dry = !active || sound.getSource() == SoundSource.MUSIC || sound.getSource() == SoundSource.MASTER
                    || sound.isRelative() || sound.getAttenuation() == SoundInstance.Attenuation.NONE;
            if (!init) {
                if (dry) {
                    return;
                }
                setup();
                if (failed) {
                    return;
                }
            }
            if (dry) {
                org.lwjgl.openal.AL11.alSource3i(src, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, 0, EXTEfx.AL_FILTER_NULL);
                return;
            }
            update();
            org.lwjgl.openal.AL11.alSource3i(src, EXTEfx.AL_AUXILIARY_SEND_FILTER, slot, 0, EXTEfx.AL_FILTER_NULL);
        } catch (Throwable t) {
            failed = true;
            OfficeRooms.LOGGER.warn("Office Rooms reverb disabled: {}", t.toString());
        }
    }

    private static int source(Channel channel) throws IllegalAccessException {
        if (sourceField == null) {
            sourceField = ObfuscationReflectionHelper.findField(Channel.class, "f_83642_");
        }
        return sourceField.getInt(channel);
    }

    private static void setup() {
        init = true;
        long ctx = ALC10.alcGetCurrentContext();
        long dev = ctx == 0 ? 0 : ALC10.alcGetContextsDevice(ctx);
        if (dev == 0 || !ALC10.alcIsExtensionPresent(dev, "ALC_EXT_EFX")) {
            failed = true;
            OfficeRooms.LOGGER.info("Office Rooms reverb: OpenAL EFX not available, room reverb off");
            return;
        }
        AL10.alGetError();
        slot = EXTEfx.alGenAuxiliaryEffectSlots();
        effect = EXTEfx.alGenEffects();
        EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_EAXREVERB);
        eax = AL10.alGetError() == AL10.AL_NO_ERROR;
        if (!eax) {
            EXTEfx.alEffecti(effect, EXTEfx.AL_EFFECT_TYPE, EXTEfx.AL_EFFECT_REVERB);
        }
        EXTEfx.alAuxiliaryEffectSlotf(slot, EXTEfx.AL_EFFECTSLOT_GAIN, 1.0F);
        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_AUXILIARY_SEND_AUTO, AL10.AL_TRUE);
        int err = AL10.alGetError();
        if (err != AL10.AL_NO_ERROR) {
            failed = true;
            OfficeRooms.LOGGER.warn("Office Rooms reverb: OpenAL error {} during setup, room reverb off", err);
            return;
        }
        OfficeRooms.LOGGER.info("Office Rooms reverb ready ({})", eax ? "EAX reverb" : "standard reverb");
    }

    /** Pushes the latest room values into the effect when they changed noticeably (also re-colours playing sounds). */
    private static void update() {
        float d = decay;
        float g = wet;
        float h = hf;
        if (Math.abs(d - appliedDecay) < 0.05F && Math.abs(g - appliedWet) < 0.02F && Math.abs(h - appliedHf) < 0.03F) {
            return;
        }
        appliedDecay = d;
        appliedWet = g;
        appliedHf = h;
        float late = Mth.clamp(reflDelay * 0.6F, 0.004F, 0.09F);
        if (eax) {
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DENSITY, 1.0F);
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DIFFUSION, 0.9F);
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAIN, g);
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_GAINHF, h);
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_TIME, Mth.clamp(d, 0.1F, 20F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_DECAY_HFRATIO, Mth.clamp(0.4F + h * 0.8F, 0.1F, 2F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_REFLECTIONS_GAIN, Mth.clamp(0.4F + g * 1.2F, 0F, 3.16F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_REFLECTIONS_DELAY, Mth.clamp(reflDelay, 0F, 0.3F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LATE_REVERB_GAIN, Mth.clamp(0.8F + g * 1.6F, 0F, 10F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_EAXREVERB_LATE_REVERB_DELAY, late);
            EXTEfx.alEffecti(effect, EXTEfx.AL_EAXREVERB_DECAY_HFLIMIT, AL10.AL_TRUE);
        } else {
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DENSITY, 1.0F);
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DIFFUSION, 0.9F);
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_GAIN, g);
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_GAINHF, h);
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DECAY_TIME, Mth.clamp(d, 0.1F, 20F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_DECAY_HFRATIO, Mth.clamp(0.4F + h * 0.8F, 0.1F, 2F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_REFLECTIONS_GAIN, Mth.clamp(0.4F + g * 1.2F, 0F, 3.16F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_REFLECTIONS_DELAY, Mth.clamp(reflDelay, 0F, 0.3F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_LATE_REVERB_GAIN, Mth.clamp(0.8F + g * 1.6F, 0F, 10F));
            EXTEfx.alEffectf(effect, EXTEfx.AL_REVERB_LATE_REVERB_DELAY, late);
        }
        EXTEfx.alAuxiliaryEffectSloti(slot, EXTEfx.AL_EFFECTSLOT_EFFECT, effect);
    }
}
