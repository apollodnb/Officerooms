"""0.16.0: Partygoer voice and party sounds. Synthesised here (numpy/scipy), no foreign audio material."""
import numpy as np
from gen_sounds import SR, t_axis, lowpass, highpass, bandpass, normalize, fade, reverb, harmonic_tone, write_ogg

rng = np.random.default_rng(20261116)


def noise(sec):
    return rng.standard_normal(int(sec * SR))


def horn():
    """A party blowout horn: a buzzy kazoo tone that rises, wobbles and squeaks off."""
    sec = 0.9
    t = t_axis(sec)
    f0 = 330 + 140 * np.clip(t / 0.12, 0, 1) + 20 * np.sin(2 * np.pi * 7 * t) - 60 * np.clip((t - 0.65) / 0.25, 0, 1)
    saw = 2 * ((np.cumsum(f0) / SR) % 1.0) - 1
    y = bandpass(saw, 400, 3200) + 0.3 * bandpass(noise(sec), 1500, 4000) * np.exp(-t * 6)
    y = np.tanh(3 * normalize(y, 0)) * np.clip(t / 0.02, 0, 1) * np.clip((sec - t) / 0.15, 0, 1)
    write_ogg("entity/partygoer/horn.ogg", fade(normalize(reverb(y, 0.5, 0.2), -2), 0.005, 0.08))


def giggle(variant, sec=1.2, base=520):
    """Short vowel bursts ("hee-hee") with a slightly wrong, too-high voice."""
    t = t_axis(sec)
    y = np.zeros(len(t))
    n = 4 + variant
    for k in range(n):
        at = 0.06 + k * (sec - 0.25) / n
        d = 0.11 + rng.uniform(-0.02, 0.03)
        tt = t_axis(d)
        f0 = base * (1.0 + 0.15 * variant) * (1 + 0.25 * np.sin(np.pi * tt / d)) * (1 - 0.06 * k)
        burst = harmonic_tone(f0, 10, ((900, 1.0), (2400, 0.8), (3400, 0.4))) * np.sin(np.pi * tt / d) ** 2
        burst += 0.25 * bandpass(rng.standard_normal(len(tt)), 2000, 6000) * np.sin(np.pi * tt / d) ** 2
        i0 = int(at * SR)
        y[i0:i0 + len(burst)] += burst[:len(y) - i0]
    return reverb(np.tanh(2 * normalize(y, 0)), 0.9, 0.35)


def squeak():
    sec = 0.4
    t = t_axis(sec)
    f0 = 900 + 700 * np.sin(np.pi * t / sec)
    y = np.sin(2 * np.pi * np.cumsum(f0) / SR) * (0.6 + 0.4 * np.sin(2 * np.pi * 40 * t)) * np.sin(np.pi * t / sec)
    write_ogg("entity/partygoer/hurt.ogg", fade(normalize(y, -3), 0.003, 0.05))


def deflate():
    """A balloon letting its air out, flapping, then silence."""
    sec = 1.6
    t = t_axis(sec)
    f0 = 600 * np.exp(-t * 1.6) + 60
    flap = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / SR))
    y = bandpass(flap, 200, 3000) * np.clip((sec - t) / 0.6, 0, 1) + 0.4 * bandpass(noise(sec), 800, 4000) * np.exp(-t * 2)
    write_ogg("entity/partygoer/death.ogg", fade(normalize(y, -2), 0.003, 0.2))


horn()
for v in range(2):
    write_ogg(f"entity/partygoer/giggle{v + 1}.ogg", fade(normalize(giggle(v), -4), 0.01, 0.2))
write_ogg("entity/partygoer/laugh.ogg", fade(normalize(giggle(3, 2.2, 430), -2), 0.01, 0.3))
squeak()
deflate()
print("partygoer sounds ok")
