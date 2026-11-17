#!/usr/bin/env python3
"""
Liminal-World-Update: sporadische Umgebungsgeräusche und Stimmen der neuen Wesen, synthetisch erzeugt
(numpy/scipy, ohne fremdes Audiomaterial). Nutzt die Hilfsfunktionen aus gen_sounds.py.

Ausgabe: src/main/resources/assets/office_rooms/sounds/ambient/liminal/*.ogg und entity/gaunt|observer/*.ogg
"""
import numpy as np

from gen_sounds import (SR, t_axis, lowpass, highpass, bandpass, normalize, fade, bitcrush, stutter,
                        reverb, harmonic_tone, write_ogg)

rng = np.random.default_rng(20261109)


def noise(sec):
    return rng.standard_normal(int(sec * SR))


def place(buf, x, at):
    i = int(at * SR)
    n = min(len(x), len(buf) - i)
    buf[i:i + n] += x[:n]


def step(dark=1.0):
    t = t_axis(0.18)
    thump = np.sin(2 * np.pi * (70 + 30 * np.exp(-t * 40)) * t) * np.exp(-t * 28)
    scuff = bandpass(noise(0.18), 300, 2500) * np.exp(-t * 45) * 0.5
    return lowpass(thump + scuff, 1800 * dark)


def distant_steps(i):
    sec = 4.5
    y = np.zeros(int(sec * SR))
    at = 0.3
    gap = 0.55 + 0.1 * i
    for k in range(5 + i):
        place(y, step(0.7) * (0.6 + 0.4 * rng.random()), at)
        at += gap + rng.normal(0, 0.03)
    y = reverb(lowpass(y, 1200), 1.6, 0.55)
    write_ogg(f"ambient/liminal/steps{i + 1}.ogg", fade(normalize(y, -4), 0.01, 0.6))


def knock(i):
    sec = 2.6
    y = np.zeros(int(sec * SR))
    t = t_axis(0.12)
    hit = (np.sin(2 * np.pi * 180 * t) + 0.5 * np.sin(2 * np.pi * 410 * t)) * np.exp(-t * 55)
    hit += bandpass(noise(0.12), 500, 3000) * np.exp(-t * 80) * 0.4
    pattern = [0.2, 0.42, 0.64] if i == 0 else [0.2, 0.36, 1.2]
    for p in pattern:
        place(y, hit * (0.8 + 0.2 * rng.random()), p)
    y = reverb(lowpass(y, 2500), 1.2, 0.5)
    write_ogg(f"ambient/liminal/knock{i + 1}.ogg", fade(normalize(y, -4), 0.005, 0.5))


def door(i):
    sec = 2.4
    t = t_axis(sec)
    creak_t = t_axis(0.9)
    f = 220 + 90 * np.sin(2 * np.pi * 1.3 * creak_t) + rng.normal(0, 3, len(creak_t)).cumsum() * 0.02
    creak = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-creak_t * 1.5) * 0.35
    creak = bandpass(np.sign(creak) * np.abs(creak) ** 0.7, 300, 2500)
    y = np.zeros(len(t))
    place(y, creak, 0.1)
    st = t_axis(0.3)
    slam = (np.sin(2 * np.pi * 60 * st) + bandpass(noise(0.3), 80, 900)) * np.exp(-st * 18)
    place(y, slam * 1.2, 1.0 if i == 0 else 0.7)
    y = reverb(lowpass(y, 1600), 1.8, 0.6)
    write_ogg(f"ambient/liminal/door{i + 1}.ogg", fade(normalize(y, -4), 0.01, 0.5))


def neon(i):
    sec = 3.5
    t = t_axis(sec)
    buzz = np.sign(np.sin(2 * np.pi * 120 * t)) * 0.3 + np.sin(2 * np.pi * 240 * t) * 0.3
    buzz = bandpass(buzz, 100, 4000)
    gate = np.ones(len(t))
    for k in range(6 + 3 * i):
        a = rng.uniform(0.2, sec - 0.3)
        gate[int(a * SR):int((a + rng.uniform(0.02, 0.15)) * SR)] = rng.uniform(0.0, 0.2)
    tick = np.zeros(len(t))
    for k in range(4):
        a = rng.uniform(0.2, sec - 0.2)
        place(tick, bandpass(noise(0.01), 2000, 8000) * 0.8, a)
    y = buzz * gate + tick
    write_ogg(f"ambient/liminal/neon{i + 1}.ogg", fade(normalize(y, -8), 0.2, 0.4))


def metal(i):
    sec = 3.0
    t = t_axis(sec)
    freqs = [310, 467, 732, 1190, 1630] if i == 0 else [190, 287, 520, 880]
    y = sum(np.sin(2 * np.pi * f * t + rng.random() * 6) * np.exp(-t * (1.2 + k * 0.6)) / (k + 1) for k, f in enumerate(freqs))
    y += bandpass(noise(sec), 1000, 6000) * np.exp(-t * 30) * 0.4
    y = reverb(y, 2.0, 0.6)
    write_ogg(f"ambient/liminal/metal{i + 1}.ogg", fade(normalize(y, -5), 0.003, 0.8))


def creak(i):
    sec = 1.8
    t = t_axis(sec)
    f = 140 + 60 * np.sin(2 * np.pi * (0.5 + 0.3 * i) * t) + 40 * rng.standard_normal(len(t)).cumsum() / SR
    y = np.sin(2 * np.pi * np.cumsum(f) / SR)
    y = bandpass(np.tanh(3 * y) * (0.3 + 0.7 * np.abs(np.sin(2 * np.pi * 9 * t))), 200, 3000)
    y *= np.sin(np.pi * t / sec)
    y = reverb(y, 1.0, 0.4)
    write_ogg(f"ambient/liminal/creak{i + 1}.ogg", fade(normalize(y, -6), 0.05, 0.3))


def voices(i):
    """Unverständliche, entfernte Stimmen: formantgefilterte Grundtöne mit Silbenrhythmus."""
    sec = 5.0
    t = t_axis(sec)
    y = np.zeros(len(t))
    for speaker in range(2):
        f0 = (115 if speaker == 0 else 190) * (1 + 0.08 * np.sin(2 * np.pi * 0.7 * t + speaker))
        tone = harmonic_tone(f0, 20, ((rng.uniform(500, 800), 1.0), (rng.uniform(1000, 1600), 0.5), (2500, 0.2)))
        syll = np.clip(np.sin(2 * np.pi * rng.uniform(3.0, 4.5) * t + rng.random() * 6), 0, 1) ** 2
        phrase = np.zeros(len(t))
        a = 0.3 + speaker * 1.4
        while a < sec - 0.8:
            d = rng.uniform(0.6, 1.4)
            phrase[int(a * SR):int((a + d) * SR)] = 1
            a += d + rng.uniform(0.6, 1.5)
        y += tone * syll * lowpass(phrase, 8)
    y = reverb(lowpass(y, 900 + 300 * i), 2.2, 0.7)
    write_ogg(f"ambient/liminal/voices{i + 1}.ogg", fade(normalize(y, -6), 0.3, 0.8))


def whisper(i):
    sec = 2.8
    t = t_axis(sec)
    y = np.zeros(len(t))
    a = 0.2
    while a < sec - 0.4:
        d = rng.uniform(0.15, 0.4)
        n = noise(d)
        tt = t_axis(d)
        lo, hi = rng.uniform(1500, 2500), rng.uniform(4000, 7000)
        place(y, bandpass(n, lo, hi) * np.sin(np.pi * tt / d) ** 2, a)
        a += d + rng.uniform(0.03, 0.2)
    y = reverb(y, 1.2, 0.35)
    write_ogg(f"ambient/liminal/whisper{i + 1}.ogg", fade(normalize(y, -8), 0.1, 0.4))


def wind(i):
    sec = 7.0
    t = t_axis(sec)
    env = 0.5 + 0.5 * np.sin(2 * np.pi * (0.12 + 0.05 * i) * t - np.pi / 2)
    y = lowpass(noise(sec), 500 + 200 * i) * env
    whistle = np.sin(2 * np.pi * np.cumsum(600 + 80 * np.sin(2 * np.pi * 0.2 * t)) / SR) * env ** 2 * 0.05
    y = y + whistle
    write_ogg(f"ambient/liminal/wind{i + 1}.ogg", fade(normalize(y, -10), 1.5, 2.0))


def behind(i):
    """Etwas bewegt sich direkt hinter dem Spieler: Stoff-Rascheln, ein vorsichtiger Schritt."""
    sec = 1.6
    t = t_axis(sec)
    rustle = bandpass(noise(sec), 800, 5000) * np.clip(np.sin(np.pi * t / 0.9), 0, 1) ** 2 * 0.4
    y = rustle.copy()
    place(y, step(1.0) * 0.8, 0.9 + 0.1 * i)
    write_ogg(f"ambient/liminal/behind{i + 1}.ogg", fade(normalize(y, -6), 0.02, 0.2))


def drip(i):
    sec = 3.0
    y = np.zeros(int(sec * SR))
    for k in range(3 + i):
        tt = t_axis(0.15)
        f = 1200 + 900 * np.exp(-tt * 40) + rng.uniform(-200, 200)
        place(y, np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt * 35), rng.uniform(0.1, sec - 0.2))
    y = reverb(y, 1.5, 0.5)
    write_ogg(f"ambient/liminal/drip{i + 1}.ogg", fade(normalize(y, -8), 0.005, 0.5))


def rustle(i):
    sec = 3.0
    t = t_axis(sec)
    env = np.clip(np.sin(np.pi * t / sec), 0, 1) * (0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 2.3 * t)))
    y = bandpass(noise(sec), 1500, 7000) * env
    write_ogg(f"ambient/liminal/rustle{i + 1}.ogg", fade(normalize(y, -10), 0.2, 0.5))


def rumble(i):
    sec = 6.0
    t = t_axis(sec)
    y = lowpass(noise(sec), 90) * np.sin(np.pi * t / sec)
    y += np.sin(2 * np.pi * 38 * t) * np.sin(np.pi * t / sec) * 0.3
    write_ogg(f"ambient/liminal/rumble{i + 1}.ogg", fade(normalize(y, -6), 0.5, 1.0))


# --- The Gaunt (dunkle Ebenen) ------------------------------------------------------------------

def gaunt_scream(i):
    sec = 2.2
    t = t_axis(sec)
    f0 = (420 + 280 * np.sin(np.pi * t / sec)) * (1 + 0.05 * np.sin(2 * np.pi * 23 * t))
    y = harmonic_tone(f0, 18, ((900, 1.0), (2200, 0.8), (3400, 0.5)))
    y += 0.6 * bandpass(noise(sec), 1500, 7000)
    y *= np.clip(t / 0.05, 0, 1) * np.clip((sec - t) / 0.6, 0, 1)
    y = np.tanh(5 * normalize(y, 0))
    y = bitcrush(stutter(y, 3, 20, 60), 6, 3)
    y = reverb(y, 1.0, 0.3)
    write_ogg(f"entity/gaunt/scream{i + 1}.ogg", fade(normalize(y, -0.5), 0.003, 0.3))


def gaunt_breath():
    sec = 2.5
    t = t_axis(sec)
    env = np.clip(np.sin(2 * np.pi * 0.8 * t), 0, 1) ** 1.5
    y = bandpass(noise(sec), 200, 1800) * env + 0.3 * np.sin(2 * np.pi * 75 * t) * env
    write_ogg("entity/gaunt/breath.ogg", fade(normalize(y, -4), 0.05, 0.3))


def gaunt_hurt():
    t = t_axis(0.5)
    y = harmonic_tone(600 * np.exp(-t * 2), 12, ((1100, 1.0), (2600, 0.5))) * np.exp(-t * 6)
    y = np.tanh(4 * normalize(y, 0))
    write_ogg("entity/gaunt/hurt.ogg", fade(normalize(y, -2), 0.003, 0.1))


def gaunt_death():
    t = t_axis(2.0)
    y = harmonic_tone(500 * np.exp(-t * 1.2) + 60, 16, ((800, 1.0), (2000, 0.6))) * np.clip((2 - t) / 1.5, 0, 1)
    y = bitcrush(np.tanh(5 * normalize(y, 0)), 5, 5)
    write_ogg("entity/gaunt/death.ogg", fade(normalize(y, -1), 0.003, 0.3))


# --- The Observer -------------------------------------------------------------------------------

def observer_vanish():
    """Kaum hörbares Ausatmen mit einem tiefen Unterdruck-Puls."""
    sec = 1.5
    t = t_axis(sec)
    y = bandpass(noise(sec), 300, 2000) * np.exp(-t * 3) * 0.5
    y += np.sin(2 * np.pi * 45 * t) * np.exp(-t * 4) * 0.6
    y = reverb(y, 1.5, 0.6)
    write_ogg("entity/observer/vanish.ogg", fade(normalize(y, -10), 0.01, 0.4))


if __name__ == "__main__":
    for k in range(2):
        distant_steps(k)
        knock(k)
        door(k)
        neon(k)
        metal(k)
        creak(k)
        voices(k)
        whisper(k)
        wind(k)
        behind(k)
        drip(k)
        rustle(k)
        rumble(k)
        gaunt_scream(k)
    gaunt_breath()
    gaunt_hurt()
    gaunt_death()
    observer_vanish()
