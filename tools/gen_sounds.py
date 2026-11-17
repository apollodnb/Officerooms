#!/usr/bin/env python3
"""
Erzeugt alle eigenen Sounds von Office Rooms synthetisch (numpy), ohne fremdes Audiomaterial.

Ausgabe: src/main/resources/assets/office_rooms/sounds/**.ogg (Vorbis, mono, 44.1 kHz).
Aufruf:  python3 tools/gen_sounds.py      (benötigt numpy, scipy und ffmpeg mit libvorbis)

Die Dimensionsmusik (music/office_rooms.ogg) wird NICHT erzeugt; sie ist die vom Auftraggeber
bereitgestellte Aufnahme und wird nur konvertiert (siehe docs).
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, sosfilt

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "office_rooms", "sounds")
rng = np.random.default_rng(20261108)


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def lowpass(x, f, order=4):
    return sosfilt(butter(order, f, "low", fs=SR, output="sos"), x)


def highpass(x, f, order=2):
    return sosfilt(butter(order, f, "high", fs=SR, output="sos"), x)


def bandpass(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def normalize(x, peak_db=-1.0):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * (10 ** (peak_db / 20))


def fade(x, a=0.01, b=0.05):
    n = len(x)
    ia, ib = int(a * SR), int(b * SR)
    env = np.ones(n)
    if ia > 0:
        env[:ia] = np.linspace(0, 1, ia)
    if ib > 0:
        env[-ib:] = np.linspace(1, 0, ib)
    return x * env


def bitcrush(x, bits=6, hold=5):
    """Quantisierung + Sample-and-Hold (digitale Verzerrung)."""
    q = 2 ** (bits - 1)
    y = np.round(x * q) / q
    idx = (np.arange(len(y)) // hold) * hold
    return y[np.minimum(idx, len(y) - 1)]


def stutter(x, count=4, min_ms=30, max_ms=90):
    """Glitch: kurze Abschnitte werden mehrfach wiederholt bzw. ausgeblendet."""
    y = x.copy()
    n = len(y)
    for _ in range(count):
        ln = int(rng.uniform(min_ms, max_ms) / 1000 * SR)
        if n <= 3 * ln:
            break
        start = int(rng.uniform(0.1, 0.8) * (n - 3 * ln))
        seg = y[start:start + ln].copy()
        reps = int(rng.integers(2, 4))
        for r in range(1, reps):
            a = start + r * ln
            y[a:a + ln] = seg[: max(0, min(ln, n - a))]
        if rng.random() < 0.4:
            g = start + reps * ln
            y[g:g + ln // 2] *= 0.05
    return y


def reverb(x, sec=0.8, wet=0.35):
    ir_len = int(sec * SR)
    ir = rng.standard_normal(ir_len) * np.exp(-np.linspace(0, 6, ir_len))
    ir = lowpass(ir, 3000)
    wetsig = np.convolve(x, ir)[: len(x)]
    return (1 - wet) * x + wet * normalize(wetsig, 0) * np.max(np.abs(x))


def resample(x, factor):
    """factor < 1: tiefer und länger."""
    n = int(len(x) / factor)
    return np.interp(np.arange(n) * factor, np.arange(len(x)), x)


def harmonic_tone(f0, harmonics=12, formants=((700, 1.0), (1200, 0.6), (2600, 0.25))):
    phase = 2 * np.pi * np.cumsum(f0) / SR
    y = np.zeros_like(f0)
    for h in range(1, harmonics + 1):
        y += np.sin(h * phase) / h
    out = np.zeros_like(y)
    for fc, g in formants:
        out += g * bandpass(y, fc * 0.7, min(fc * 1.3, SR / 2 - 100))
    return out


def write_ogg(path, x, quality=5):
    full = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wavfile.write(tmp.name, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", tmp.name,
                        "-c:a", "libvorbis", "-q:a", str(quality), "-ac", "1", full], check=True)
    os.unlink(tmp.name)
    print(f"{path}: {len(x) / SR:.2f}s")


# ---------------------------------------------------------------------------------------------
# Lampenbrummen: nahtloser 12-s-Loop. Alle Frequenzen und Schwankungen haben Perioden, die die
# Looplänge ganzzahlig teilen; das Rauschen ist per Frequenzsynthese periodisch.
# ---------------------------------------------------------------------------------------------
def lamp_hum():
    sec = 12.0
    t = t_axis(sec)
    n = len(t)
    hum = (1.0 * np.sin(2 * np.pi * 100 * t)
           + 0.45 * np.sin(2 * np.pi * 200 * t + 0.3)
           + 0.25 * np.sin(2 * np.pi * 300 * t + 1.1)
           + 0.12 * np.sin(2 * np.pi * 50 * t)
           + 0.08 * np.sin(2 * np.pi * 400 * t + 2.0))
    # leichte Schwankungen (0.25 Hz und 1/12 Hz -> ganzzahlig im Loop)
    wobble = 1 + 0.12 * np.sin(2 * np.pi * 0.25 * t) + 0.08 * np.sin(2 * np.pi * (1 / 12) * t + 1.0)
    hum *= wobble
    # periodisches, leises Rauschen (Vorschaltgerät)
    spec = np.zeros(n // 2 + 1, dtype=complex)
    freqs = np.fft.rfftfreq(n, 1 / SR)
    mag = np.where((freqs > 80) & (freqs < 6000), 1 / np.sqrt(freqs + 1), 0)
    spec[:] = mag * np.exp(1j * rng.uniform(0, 2 * np.pi, len(spec)))
    noise = np.fft.irfft(spec, n)
    noise = noise / np.max(np.abs(noise)) * 0.08
    y = hum + noise
    # zwei kurze Störungen (Flackern) weit weg vom Loop-Rand
    for start, dur in ((3.1, 0.35), (8.4, 0.22)):
        a, b = int(start * SR), int((start + dur) * SR)
        seg = np.arange(b - a)
        flicker = 1 + 0.9 * (np.sign(np.sin(2 * np.pi * 23 * seg / SR)) * 0.5 + 0.5) * np.hanning(b - a)
        y[a:b] *= flicker
        y[a:b] += 0.25 * np.hanning(b - a) * np.sign(np.sin(2 * np.pi * 100 * t[a:b])) * 0.3
    y = lowpass(np.concatenate([y, y, y]), 5000)[n:2 * n]  # Filter ohne Randeffekt
    write_ogg("ambient/lamp_hum.ogg", normalize(y, -6.0), quality=4)


# ---------------------------------------------------------------------------------------------
# Wolfspider: tiefes, rückwärts klingendes, verzerrtes und verglitchtes Wolfsjaulen
# ---------------------------------------------------------------------------------------------
def howl(variant):
    sec = 2.4 + 0.4 * variant
    t = t_axis(sec)
    rise, hold = 0.5 + 0.1 * variant, sec * 0.6
    base, peak, end = 360 + 30 * variant, 640 - 20 * variant, 300
    f0 = np.where(t < rise, base + (peak - base) * (t / rise) ** 0.6,
                  np.where(t < hold, peak, peak + (end - peak) * (t - hold) / (sec - hold)))
    f0 = f0 + 14 * np.sin(2 * np.pi * (5 + variant) * t) * np.clip(t / rise, 0, 1)
    y = harmonic_tone(f0, 10)
    y += 0.15 * bandpass(rng.standard_normal(len(t)), 900, 2500) * np.clip(t / rise, 0, 1)
    env = np.clip(t / 0.25, 0, 1) * np.clip((sec - t) / 0.6, 0, 1)
    y *= env
    y = reverb(y, 1.2, 0.4)
    y = resample(y, 0.55 - 0.05 * variant)   # tief
    y = y[::-1]                                # rückwärts
    y = np.tanh(4 * normalize(y, 0))           # verzerrt
    y = bitcrush(y, bits=6, hold=4 + variant)  # digital
    y = stutter(y, count=3 + variant)          # Glitches
    y = lowpass(y, 7000)
    write_ogg(f"entity/wolfspider/howl{variant + 1}.ogg", fade(normalize(y, -2.0), 0.02, 0.15))


def wolfspider_hurt():
    t = t_axis(0.45)
    f0 = 520 - 300 * t / 0.45
    y = harmonic_tone(f0, 8) * np.exp(-t * 6)
    y = np.tanh(5 * normalize(y, 0))
    y = bitcrush(stutter(y, 2, 20, 40), 5, 6)
    write_ogg("entity/wolfspider/hurt.ogg", fade(normalize(y, -3.0), 0.005, 0.05))


def wolfspider_death():
    t = t_axis(1.3)
    f0 = 480 * np.exp(-t * 1.4) + 60
    y = harmonic_tone(f0, 10) * np.clip((1.3 - t) / 1.0, 0, 1)
    y = resample(y, 0.8)[::-1][::-1]
    y = np.tanh(4 * normalize(y, 0))
    y = bitcrush(stutter(y, 4, 25, 70), 5, 8)
    write_ogg("entity/wolfspider/death.ogg", fade(normalize(y, -2.0), 0.005, 0.2))


# ---------------------------------------------------------------------------------------------
# Headless Warden: sehr lauter, digital verzerrter, verglitchter Schrei; Herzschlag
# ---------------------------------------------------------------------------------------------
def scream(variant):
    sec = 2.2 + 0.3 * variant
    t = t_axis(sec)
    f0 = 700 + 1300 * np.sin(np.pi * np.clip(t / sec, 0, 1)) ** 0.7 - 200 * variant
    f0 = f0 + 60 * np.sin(2 * np.pi * 11 * t)
    tone = harmonic_tone(f0, 16, ((900, 1.0), (1800, 0.8), (3200, 0.5)))
    noise = bandpass(rng.standard_normal(len(t)), 1200, 6000) * 0.6
    y = tone + noise
    ring = np.sin(2 * np.pi * (93 + 40 * variant) * t)   # Ringmodulation: "digital"
    y = 0.6 * y + 0.4 * y * ring
    env = np.clip(t / 0.05, 0, 1) * np.clip((sec - t) / 0.5, 0, 1)
    y = np.tanh(8 * normalize(y * env, 0))
    y = bitcrush(y, bits=4, hold=3 + 2 * variant)
    y = stutter(y, count=6, min_ms=25, max_ms=70)
    y = reverb(y, 0.9, 0.3)
    write_ogg(f"entity/headless_warden/scream{variant + 1}.ogg", fade(normalize(y, -0.5), 0.003, 0.2))


def heartbeat():
    sec = 0.75
    t = t_axis(sec)

    def thump(at, f, amp):
        tt = np.clip(t - at, 0, None)
        env = (1 - np.exp(-tt / 0.004)) * np.exp(-tt / 0.11) * (t >= at)
        return amp * np.sin(2 * np.pi * f * tt + 0.5 * np.sin(2 * np.pi * 9 * tt)) * env

    y = thump(0.02, 55, 1.0) + thump(0.30, 46, 0.7)
    y += 0.08 * lowpass(rng.standard_normal(len(t)), 180) * (np.exp(-np.abs(t - 0.03) / 0.02) + np.exp(-np.abs(t - 0.31) / 0.02))
    y = lowpass(np.tanh(1.8 * y), 220)
    write_ogg("entity/headless_warden/heartbeat.ogg", fade(normalize(y, -1.0), 0.002, 0.08))


def warden_hurt():
    t = t_axis(0.6)
    f0 = 140 - 60 * t
    y = harmonic_tone(f0, 20, ((300, 1.0), (700, 0.5), (1500, 0.3))) * np.exp(-t * 4)
    y = bitcrush(np.tanh(6 * normalize(y, 0)), 5, 5)
    write_ogg("entity/headless_warden/hurt.ogg", fade(normalize(y, -2.0), 0.005, 0.05))


def warden_death():
    t = t_axis(2.4)
    f0 = 160 * np.exp(-t * 0.9) + 30
    y = harmonic_tone(f0, 24, ((250, 1.0), (600, 0.6), (1400, 0.3))) * np.clip((2.4 - t) / 1.8, 0, 1)
    y += 0.3 * bandpass(rng.standard_normal(len(t)), 200, 3000) * np.exp(-t * 2)
    y = bitcrush(stutter(np.tanh(6 * normalize(y, 0)), 5, 30, 90), 4, 7)
    write_ogg("entity/headless_warden/death.ogg", fade(normalize(y, -1.0), 0.005, 0.3))


if __name__ == "__main__":
    lamp_hum()
    for v in range(3):
        howl(v)
    wolfspider_hurt()
    wolfspider_death()
    for v in range(2):
        scream(v)
    heartbeat()
    warden_hurt()
    warden_death()
