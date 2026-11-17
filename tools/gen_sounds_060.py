#!/usr/bin/env python3
"""
0.6.0: Poolrooms ambience and the voices of the Hound, the Smiler and the Colony.
All sounds are synthesized here (numpy/scipy), no foreign audio material.

Output: src/main/resources/assets/office_rooms/sounds/ambient/pool/*.ogg, entity/hound|smiler|colony/*.ogg
"""
import numpy as np

from gen_sounds import (SR, t_axis, lowpass, highpass, bandpass, normalize, fade, bitcrush, stutter,
                        reverb, harmonic_tone, write_ogg)

rng = np.random.default_rng(20261110)


def noise(sec):
    return rng.standard_normal(int(sec * SR))


def place(buf, x, at):
    i = int(at * SR)
    n = min(len(x), len(buf) - i)
    if n > 0:
        buf[i:i + n] += x[:n]


def plip(freq=900, dur=0.12):
    t = t_axis(dur)
    f = freq * (1 + 1.8 * np.exp(-t * 60))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 35)


# --- Poolrooms ---------------------------------------------------------------------------------

def pool_lap(i):
    sec = 7.0
    t = t_axis(sec)
    y = lowpass(noise(sec), 500) * (0.5 + 0.5 * np.sin(2 * np.pi * (0.35 + 0.1 * i) * t)) * 0.6
    for _ in range(14):
        place(y, plip(rng.uniform(500, 1300), 0.1) * rng.uniform(0.2, 0.5), rng.uniform(0.2, sec - 0.3))
    y = reverb(y, 2.2, 0.55)
    write_ogg(f"ambient/pool/lap{i + 1}.ogg", fade(normalize(y, -10), 1.0, 1.5))


def pool_splash(i):
    sec = 4.0
    t = t_axis(sec)
    y = np.zeros(int(sec * SR))
    burst = bandpass(noise(0.6), 400, 5000) * np.exp(-t_axis(0.6) * 7)
    place(y, burst, 0.1)
    for _ in range(10):
        place(y, plip(rng.uniform(600, 1500), 0.09) * 0.4, rng.uniform(0.3, 1.4))
    y = lowpass(y, 2500)
    y = reverb(y, 3.0, 0.7)
    write_ogg(f"ambient/pool/splash{i + 1}.ogg", fade(normalize(y, -9), 0.01, 1.2))


def pool_drip(i):
    sec = 4.5
    y = np.zeros(int(sec * SR))
    for k in range(3):
        place(y, plip(1100 + 250 * k + 120 * i, 0.14), 0.2 + k * rng.uniform(0.6, 1.1))
    y = reverb(y, 3.5, 0.75)
    write_ogg(f"ambient/pool/drip{i + 1}.ogg", fade(normalize(y, -9), 0.005, 1.0))


def pool_pump():
    sec = 8.0
    t = t_axis(sec)
    y = sum(np.sin(2 * np.pi * 59 * h * t) / h for h in range(1, 6)) * 0.6
    y += lowpass(noise(sec), 300) * 0.4
    y *= 0.8 + 0.2 * np.sin(2 * np.pi * 0.5 * t)
    write_ogg("ambient/pool/pump.ogg", fade(normalize(y, -14), 2.0, 2.5))


def pool_echo_voice():
    """Ein ferner, unverständlicher Ruf in einer gefliesten Halle."""
    sec = 5.0
    t = t_axis(1.2)
    f0 = 210 + 40 * np.sin(np.pi * t / 1.2)
    v = harmonic_tone(f0, 10, ((650, 1.0), (1100, 0.6), (2400, 0.2))) * np.sin(np.pi * t / 1.2)
    y = np.zeros(int(sec * SR))
    place(y, lowpass(v, 1500), 0.3)
    y = reverb(y, 4.0, 0.85)
    write_ogg("ambient/pool/echo.ogg", fade(normalize(y, -12), 0.1, 1.5))


# --- The Hound ---------------------------------------------------------------------------------

def hound_growl(i):
    sec = 2.4
    t = t_axis(sec)
    f0 = 85 + 15 * np.sin(2 * np.pi * 1.3 * t) + 8 * rng.standard_normal(len(t)).cumsum() / SR
    y = harmonic_tone(f0, 25, ((300, 1.0), (700, 0.7), (1500, 0.3)))
    y *= 1 + 0.6 * np.sin(2 * np.pi * 27 * t)
    y += 0.3 * bandpass(noise(sec), 200, 1500)
    y = np.tanh(3 * normalize(y, 0))
    write_ogg(f"entity/hound/growl{i + 1}.ogg", fade(normalize(y, -3), 0.15, 0.4))


def hound_bark(i):
    sec = 0.6
    t = t_axis(sec)
    f0 = 260 * np.exp(-t * 3) + 120
    y = harmonic_tone(f0, 20, ((500, 1.0), (1300, 0.8), (2600, 0.4))) * np.exp(-t * 7)
    y += bandpass(noise(sec), 800, 4000) * np.exp(-t * 12) * 0.5
    y = np.tanh(6 * normalize(y, 0))
    y = reverb(y, 0.6, 0.2)
    write_ogg(f"entity/hound/bark{i + 1}.ogg", fade(normalize(y, -1), 0.002, 0.1))


def hound_hurt():
    t = t_axis(0.45)
    y = harmonic_tone(520 * np.exp(-t * 2.5) + 150, 14, ((700, 1.0), (1800, 0.6))) * np.exp(-t * 6)
    write_ogg("entity/hound/hurt.ogg", fade(normalize(np.tanh(3 * normalize(y, 0)), -2), 0.003, 0.1))


def hound_death():
    sec = 1.6
    t = t_axis(sec)
    y = harmonic_tone(300 * np.exp(-t * 1.5) + 70, 18, ((500, 1.0), (1200, 0.5))) * np.clip((sec - t) / 1.2, 0, 1)
    y += lowpass(noise(sec), 600) * np.exp(-t * 2) * 0.4
    write_ogg("entity/hound/death.ogg", fade(normalize(np.tanh(3 * normalize(y, 0)), -2), 0.003, 0.3))


def hound_sniff():
    sec = 1.2
    y = np.zeros(int(sec * SR))
    for k in range(4):
        s = bandpass(noise(0.12), 1500, 6000) * np.exp(-t_axis(0.12) * 25)
        place(y, s, 0.1 + k * 0.22)
    write_ogg("entity/hound/sniff.ogg", fade(normalize(y, -8), 0.005, 0.1))


# --- The Smiler --------------------------------------------------------------------------------

def smiler_giggle(i):
    sec = 2.0
    y = np.zeros(int(sec * SR))
    n = 6 + i * 2
    for k in range(n):
        d = 0.11
        t = t_axis(d)
        f0 = 520 + 40 * k * (-1) ** k + 80 * i
        g = harmonic_tone(np.full(len(t), float(f0)), 8, ((2600, 1.0), (3400, 0.7))) * np.sin(np.pi * t / d) ** 2
        g += 0.5 * bandpass(noise(d), 3000, 8000) * np.sin(np.pi * t / d)
        place(y, g * (0.6 + 0.4 * np.sin(k)), 0.1 + k * 0.17)
    y = reverb(y, 1.8, 0.6)
    write_ogg(f"entity/smiler/giggle{i + 1}.ogg", fade(normalize(y, -8), 0.02, 0.5))


def smiler_vanish():
    sec = 1.6
    t = t_axis(sec)
    y = highpass(noise(sec), 2500) * np.exp(-t * 3) * 0.6
    y += np.sin(2 * np.pi * (300 - 150 * t) * t) * np.exp(-t * 2.5) * 0.4
    y = reverb(y, 1.2, 0.5)
    write_ogg("entity/smiler/vanish.ogg", fade(normalize(y, -6), 0.01, 0.5))


def smiler_kill():
    sec = 1.4
    t = t_axis(sec)
    y = bandpass(noise(sec), 100, 3000) * np.exp(-t * 6)
    y += np.sin(2 * np.pi * 55 * t) * np.exp(-t * 3)
    t2 = t_axis(0.8)
    g = harmonic_tone(np.full(len(t2), 680.0), 10, ((2800, 1.0),)) * np.sin(np.pi * t2 / 0.8)
    place(y, g * 0.5, 0.5)
    y = np.tanh(4 * normalize(y, 0))
    write_ogg("entity/smiler/kill.ogg", fade(normalize(y, -1), 0.002, 0.3))


def smiler_ambient():
    sec = 3.0
    t = t_axis(sec)
    y = bandpass(noise(sec), 400, 2500) * (np.clip(np.sin(2 * np.pi * 0.6 * t), 0, 1) ** 2)
    y = reverb(y, 1.5, 0.6)
    write_ogg("entity/smiler/breath.ogg", fade(normalize(y, -12), 0.2, 0.6))


# --- The Colony --------------------------------------------------------------------------------

def colony_scream(i):
    """Mehrstimmiger, nasser Schrei: gurgelnder Anlauf, dann ein ring-moduliertes Kreischen."""
    sec = 2.8
    t = t_axis(sec)
    voices = np.zeros(len(t))
    for v in range(4):
        f0 = (180 + 90 * v + 40 * i) * (1 + 1.6 * np.clip((t - 0.5) / 1.2, 0, 1)) * (1 + 0.04 * np.sin(2 * np.pi * (11 + 3 * v) * t))
        voices += harmonic_tone(f0, 14, ((900 + 300 * v, 1.0), (2100, 0.6), (3600, 0.3))) * (0.8 - 0.12 * v)
    gurgle = lowpass(noise(sec), 400) * (1 + np.sin(2 * np.pi * 17 * t)) * np.clip((0.9 - t) / 0.9, 0, 1)
    y = voices * np.clip((t - 0.3) / 0.4, 0, 1) + 1.2 * gurgle
    y *= np.sin(2 * np.pi * (37 + 9 * i) * t) * 0.5 + 0.7
    y *= np.clip((sec - t) / 0.5, 0, 1)
    y = np.tanh(4 * normalize(y, 0))
    y = stutter(y, 4, 15, 45)
    y = reverb(y, 1.2, 0.35)
    write_ogg(f"entity/colony/scream{i + 1}.ogg", fade(normalize(y, -0.5), 0.01, 0.3))


def colony_chitter(i):
    sec = 1.8
    y = np.zeros(int(sec * SR))
    for k in range(30):
        c = bandpass(noise(0.015), 2000 + 400 * (k % 4), 9000) * np.exp(-t_axis(0.015) * 300)
        place(y, c * rng.uniform(0.4, 1.0), rng.uniform(0.0, sec - 0.05))
    y += lowpass(noise(sec), 200) * 0.15
    write_ogg(f"entity/colony/chitter{i + 1}.ogg", fade(normalize(y, -6), 0.02, 0.2))


def colony_hurt():
    t = t_axis(0.5)
    y = harmonic_tone(700 * np.exp(-t * 3) + 200, 10, ((1200, 1.0), (3000, 0.5))) * np.exp(-t * 6)
    y += lowpass(noise(0.5), 600) * np.exp(-t * 8) * 0.6
    write_ogg("entity/colony/hurt.ogg", fade(normalize(np.tanh(4 * normalize(y, 0)), -2), 0.002, 0.1))


def colony_death():
    sec = 2.5
    t = t_axis(sec)
    y = harmonic_tone(400 * np.exp(-t) + 50, 14, ((800, 1.0), (1900, 0.5))) * np.clip((sec - t) / 2, 0, 1)
    y += lowpass(noise(sec), 300) * np.exp(-t * 1.2) * 0.8
    y = bitcrush(np.tanh(4 * normalize(y, 0)), 6, 3)
    write_ogg("entity/colony/death.ogg", fade(normalize(y, -1), 0.003, 0.4))


def colony_step():
    sec = 0.25
    t = t_axis(sec)
    y = lowpass(noise(sec), 900) * np.exp(-t * 30) + bandpass(noise(sec), 2000, 6000) * np.exp(-t * 60) * 0.3
    write_ogg("entity/colony/step.ogg", fade(normalize(y, -6), 0.002, 0.05))


if __name__ == "__main__":
    for k in range(2):
        pool_lap(k)
        pool_splash(k)
        pool_drip(k)
        hound_growl(k)
        hound_bark(k)
        smiler_giggle(k)
        colony_scream(k)
        colony_chitter(k)
    pool_pump()
    pool_echo_voice()
    hound_hurt()
    hound_death()
    hound_sniff()
    smiler_vanish()
    smiler_kill()
    smiler_ambient()
    colony_hurt()
    colony_death()
    colony_step()
    print("0.6.0 sounds written")
