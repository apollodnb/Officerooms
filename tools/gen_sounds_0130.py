"""0.13.0: voice of The Flayed. Synthesised here (numpy/scipy), no foreign audio material.
Output: src/main/resources/assets/office_rooms/sounds/entity/flayed/*.ogg"""
import numpy as np
from gen_sounds import SR, t_axis, lowpass, highpass, bandpass, normalize, fade, reverb, harmonic_tone, write_ogg

rng = np.random.default_rng(20261111)


def noise(sec):
    return rng.standard_normal(int(sec * SR))


def scream(variant):
    """An enormous, stretched human-like shriek that cracks into a second, lower voice and distorts."""
    sec = 3.4 + 0.4 * variant
    t = t_axis(sec)
    rise = np.clip(t / 0.35, 0, 1)
    f0 = 260 + 900 * rise ** 1.5 - 120 * np.clip((t - 2.2) / 1.2, 0, 1) + 90 * variant
    f0 = f0 * (1 + 0.035 * np.sin(2 * np.pi * (6.5 + variant) * t)) * (1 + 0.012 * rng.standard_normal(len(t)).cumsum() / np.sqrt(len(t)))
    high = harmonic_tone(f0, 18, ((850, 1.0), (1250, 0.9), (2900, 0.6), (4200, 0.3)))
    low = harmonic_tone(f0 * 0.503, 14, ((500, 1.0), (900, 0.7), (2400, 0.3)))     # the second, wrong voice underneath
    rasp = bandpass(noise(sec), 900, 5500) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * 37 * t)))
    y = high + 0.8 * low * np.clip((t - 0.25) / 0.3, 0, 1) + 0.55 * rasp
    y = y * (0.75 + 0.25 * np.sign(np.sin(2 * np.pi * (17 + 4 * variant) * t)))      # vocal-fry flutter
    env = np.clip(t / 0.04, 0, 1) * np.clip((sec - t) / 0.7, 0, 1)
    y = np.tanh(9 * normalize(y * env, 0))                                            # hard distortion
    y = y + 0.35 * lowpass(np.tanh(4 * normalize(low * env, 0)), 400)                # chest-shaking low end
    y = reverb(y, 1.4, 0.38)
    write_ogg(f"entity/flayed/scream{variant + 1}.ogg", fade(normalize(y, -0.3), 0.002, 0.4))


def ambient(variant):
    """Wet clicking and a slow, rattling inhale."""
    sec = 3.0
    t = t_axis(sec)
    y = np.zeros(len(t))
    for k in range(rng.integers(5, 10)):
        at = int(rng.uniform(0.1, sec - 0.2) * SR)
        d = int(0.04 * SR)
        click = bandpass(noise(0.04), 600, 3500) * np.exp(-np.linspace(0, 9, d))
        y[at:at + d] += click * rng.uniform(0.4, 1.0)
    breath = bandpass(noise(sec), 180, 1400) * np.sin(np.pi * np.clip((t - 0.8) / 1.8, 0, 1)) ** 2
    breath *= 0.6 + 0.4 * (np.sin(2 * np.pi * 28 * t) > 0)                            # rattle
    y += 0.7 * breath
    write_ogg(f"entity/flayed/ambient{variant + 1}.ogg", fade(normalize(reverb(y, 0.6, 0.3), -8), 0.05, 0.3))


def hurt():
    sec = 0.7
    t = t_axis(sec)
    f0 = 700 * np.exp(-t * 1.5)
    y = np.tanh(6 * normalize(harmonic_tone(f0, 14, ((900, 1), (2500, 0.6))) + 0.4 * bandpass(noise(sec), 1000, 5000), 0)) * np.exp(-t * 4)
    write_ogg("entity/flayed/hurt.ogg", fade(normalize(y, -2), 0.002, 0.1))


def death():
    sec = 4.0
    t = t_axis(sec)
    f0 = 520 * np.exp(-t * 0.7) + 40
    y = harmonic_tone(f0, 16, ((800, 1), (1300, 0.7), (2800, 0.4)))
    gurgle = lowpass(noise(sec), 500) * (np.sin(2 * np.pi * 9 * t) > 0.3)
    y = np.tanh(5 * normalize(y + 0.8 * gurgle * np.clip((t - 1.2) / 1.0, 0, 1), 0)) * np.clip((sec - t) / 2.0, 0, 1)
    write_ogg("entity/flayed/death.ogg", fade(normalize(reverb(y, 1.2, 0.35), -1), 0.005, 0.5))


def falter():
    """Ragged, exhausted panting when it slows down for a moment."""
    sec = 2.6
    t = t_axis(sec)
    pant = np.abs(np.sin(2 * np.pi * 1.9 * t)) ** 3
    y = bandpass(noise(sec), 250, 2200) * pant + 0.3 * harmonic_tone(np.full(len(t), 110.0), 8, ((400, 1), (900, 0.5))) * pant
    write_ogg("entity/flayed/falter.ogg", fade(normalize(reverb(y, 0.5, 0.25), -5), 0.05, 0.3))


def step():
    sec = 0.35
    t = t_axis(sec)
    y = lowpass(noise(sec), 300) * np.exp(-t * 18) + 0.4 * bandpass(noise(sec), 800, 2500) * np.exp(-t * 30)
    write_ogg("entity/flayed/step.ogg", fade(normalize(y, -6), 0.001, 0.05))


for v in range(2):
    scream(v)
    ambient(v)
hurt()
death()
falter()
step()
print("flayed sounds ok")
