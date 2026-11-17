"""Office Rooms 0.18.0: SOUND OVERHAUL. Darker, deeper, layered sounds for every hostile creature (except the Smiler,
whose sounds stay exactly as they are) and new ambient soundscapes. Everything is synthesised here with numpy/scipy;
no foreign audio material. Run: python3 tools/gen_sounds_0180.py"""
import numpy as np
from gen_sounds import SR, t_axis, lowpass, highpass, bandpass, normalize, fade, write_ogg

rng = np.random.default_rng(20261118)


# --- building blocks ------------------------------------------------------------------------------------------
def n(sec):
    return rng.standard_normal(int(sec * SR))


def pad(x, sec):
    return np.concatenate([x, np.zeros(int(sec * SR))])


def verb(x, sec=1.2, wet=0.35, tone=2500):
    """Reverb with a real tail (the input is padded so the tail is not cut off)."""
    x = pad(x, sec)
    ln = int(sec * SR)
    ir = rng.standard_normal(ln) * np.exp(-np.linspace(0, 7, ln))
    ir = lowpass(ir, tone)
    w = np.convolve(x, ir)[:len(x)]
    return (1 - wet) * x + wet * normalize(w, 0) * np.max(np.abs(x))


def sat(x, k=3.0):
    return np.tanh(k * x) / np.tanh(k)


def env(sec, a=0.02, r=0.3, curve=1.0):
    t = t_axis(sec)
    e = np.clip(t / max(a, 1e-4), 0, 1) * np.clip((sec - t) / max(r, 1e-4), 0, 1)
    return e ** curve


def wobble(sec, rate, depth, smooth=40):
    """Slowly wandering random curve (for jitter, roughness, tremolo)."""
    k = max(2, int(sec * rate) + 2)
    pts = rng.standard_normal(k)
    x = np.interp(np.linspace(0, k - 1, int(sec * SR)), np.arange(k), pts)
    return 1 + depth * lowpass(x, smooth) / (np.max(np.abs(x)) + 1e-9)


def voice(f0, formants, harm=24, sub=0.0, jitter=0.01):
    """Glottal-ish source (bright saw) through formant filters; optional subharmonic for guttural growls."""
    f0 = f0 * (1 + jitter * rng.standard_normal(len(f0)).cumsum() / np.sqrt(len(f0)) * 6)
    ph = 2 * np.pi * np.cumsum(f0) / SR
    src = np.zeros(len(f0))
    for h in range(1, harm + 1):
        src += np.sin(h * ph) / h ** 0.9
    if sub:
        src += sub * np.sign(np.sin(ph / 2)) * 0.6
    out = np.zeros(len(f0))
    for fc, g, bw in formants:
        out += g * bandpass(src, max(30, fc - bw), min(fc + bw, SR / 2 - 200))
    return out


def growl(sec, f0=70, rough=28, formants=((350, 1, 160), (900, .6, 250), (2200, .25, 400)), sub=0.6):
    t = t_axis(sec)
    f = f0 * wobble(sec, 3, 0.08)
    v = voice(f, formants, sub=sub, jitter=0.02)
    am = 0.55 + 0.45 * np.abs(np.sin(2 * np.pi * rough * t + 2 * wobble(sec, 6, 1)))   # vocal fry / rattle
    return sat(normalize(v * am, 0), 2.5)


def breath(sec, lo=300, hi=2400, cycles=1.0, rasp=0.0, wet=0.0):
    """Inhale/exhale shaped noise; rasp adds dry crackle, wet adds gurgles."""
    t = t_axis(sec)
    e = np.abs(np.sin(np.pi * cycles * t / sec)) ** 1.5
    x = bandpass(n(sec), lo, hi) * e
    if rasp:
        crack = (rng.random(len(t)) > 1 - 0.004 * rasp) * rng.standard_normal(len(t)) * 3
        x += bandpass(crack, 900, 5000) * e
    if wet:
        g = lowpass(n(sec), 260) * (np.sin(2 * np.pi * 11 * t + wobble(sec, 4, 2)) > 0.4)
        x += wet * g * e
    return x


def clicks(sec, count, lo=900, hi=4000, dec=400):
    y = np.zeros(int(sec * SR))
    for _ in range(count):
        at = int(rng.uniform(0, sec - 0.03) * SR)
        d = int(0.02 * SR)
        c = bandpass(n(0.02), lo, hi) * np.exp(-np.linspace(0, dec / 50, d))
        y[at:at + d] += c * rng.uniform(0.4, 1)
    return y


def thud(sec=0.35, f=55, drop=0.5, noise=0.3):
    t = t_axis(sec)
    ff = f * (1 - drop * np.clip(t / sec, 0, 1))
    return np.sin(2 * np.pi * np.cumsum(ff) / SR) * np.exp(-t * 14) + noise * lowpass(n(sec), 400) * np.exp(-t * 25)


def tear(sec=0.7):
    """Wet muscle tearing: dense crunchy grains getting denser, over a low rip."""
    t = t_axis(sec)
    dens = 0.002 + 0.03 * (t / sec) ** 2
    grains = (rng.random(len(t)) < dens) * rng.standard_normal(len(t))
    x = bandpass(grains, 300, 3500) * 3 + 0.6 * lowpass(n(sec), 300) * np.clip(t / sec, 0, 1)
    return sat(x * env(sec, 0.05, 0.15), 2)


def mix(*parts):
    L = max(len(p) for p in parts)
    y = np.zeros(L)
    for p in parts:
        y[:len(p)] += p
    return y


def put(y, x, at):
    i = int(at * SR)
    y[i:i + len(x)] += x[:max(0, len(y) - i)]
    return y


def out(path, x, peak=-1.0, a=0.005, r=0.08):
    write_ogg(path, fade(normalize(x, peak), a, r))


# --- Wolfspider --------------------------------------------------------------------------------------------------
def wolfspider():
    for v in range(3):
        sec = 2.4 + 0.3 * v
        x = mix(0.8 * growl(sec, 62 + 6 * v, 24 + 4 * v) * env(sec, 0.3, 0.6), 0.6 * clicks(sec, 18 + 5 * v, 700, 2600),
                0.35 * breath(sec, 200, 1200, 2, rasp=1))
        out(f"entity/wolfspider/howl{v + 1}.ogg", verb(x, 0.8, 0.25), -3, 0.05, 0.4)
    for v in range(3):
        x = mix(clicks(0.35, 3, 600, 2200, 600), 0.5 * thud(0.25, 90, 0.4, 0.2))
        out(f"entity/wolfspider/step{v + 1}.ogg", x, -8, 0.002, 0.05)
    for v in range(2):
        sec = 0.55
        x = mix(sat(bandpass(n(sec), 250, 1400), 2) * env(sec, 0.01, 0.35), 0.8 * growl(sec, 90 - 10 * v, 34) * env(sec, 0.02, 0.3))
        out(f"entity/wolfspider/hurt{v + 1}.ogg", x, -2)
    sec = 1.8
    x = mix(growl(sec, 70, 20) * np.exp(-t_axis(sec) * 1.4), 0.7 * clicks(sec, 30, 600, 2400) * np.linspace(1, 0, int(sec * SR)),
            0.5 * breath(sec, 150, 700, 1, wet=1.2))
    out("entity/wolfspider/death.ogg", verb(x, 0.9, 0.3), -1, 0.01, 0.4)
    for v in range(2):
        sec = 0.45
        x = mix(sat(bandpass(n(sec), 700, 2600), 3) * env(sec, 0.005, 0.3), thud(0.3, 75, 0.5, 0.4))
        out(f"entity/wolfspider/attack{v + 1}.ogg", x, -2)


# --- Hound ---------------------------------------------------------------------------------------------------------
def hound():
    for v in range(3):
        sec = 2.2 + 0.4 * v
        x = mix(growl(sec, 52 + 5 * v, 30, ((300, 1, 140), (750, .7, 200), (1800, .3, 300)), 0.8) * env(sec, 0.25, 0.5),
                0.45 * breath(sec, 250, 1600, 2.5, rasp=0.5, wet=0.4))
        out(f"entity/hound/growl{v + 1}.ogg", verb(x, 0.6, 0.2), -2, 0.05, 0.3)
    for v in range(3):
        sec = 0.9
        t = t_axis(sec)
        f = 150 * np.exp(-t * 2.2) + 55
        bark = voice(f, ((450, 1, 200), (1100, .7, 300), (2600, .3, 500)), sub=0.9) * np.exp(-t * (5 + v))
        x = mix(sat(normalize(bark, 0), 4), thud(0.4, 60, 0.4, 0.5), 0.3 * bandpass(n(sec), 800, 3000) * np.exp(-t * 18))
        out(f"entity/hound/bark{v + 1}.ogg", verb(x, 1.0, 0.3), -0.5)
    for v in range(2):
        sec = 0.7
        t = t_axis(sec)
        f = 210 * np.exp(-t * 1.5) + 70 - 20 * v
        x = mix(sat(normalize(voice(f, ((500, 1, 220), (1300, .5, 300)), sub=0.5), 0), 3) * env(sec, 0.01, 0.4),
                0.4 * growl(sec, 60, 32) * env(sec, 0.05, 0.4))
        out(f"entity/hound/hurt{v + 1}.ogg", x, -2)
    sec = 2.6
    t = t_axis(sec)
    x = mix(sat(normalize(voice(160 * np.exp(-t * 0.6) + 45, ((420, 1, 200), (1000, .5, 300)), sub=0.8), 0), 2.5) * env(sec, 0.02, 1.2),
            0.6 * breath(sec, 150, 800, 1, wet=1))
    out("entity/hound/death.ogg", verb(x, 1.0, 0.3), -1, 0.01, 0.6)
    for v in range(2):
        sec = 0.9
        x = mix(breath(sec, 400, 3000, 3, wet=0.6), 0.3 * breath(sec, 150, 600, 3))
        out(f"entity/hound/sniff{v + 1}.ogg", x, -6, 0.01, 0.1)


# --- Hunter (a wire figure: metal, distorted breath, low voice) ---------------------------------------------------
def hunter():
    for v in range(2):
        sec = 4.0
        t = t_axis(sec)
        b = breath(sec, 180, 1100, 2, rasp=1.5) * (0.7 + 0.3 * np.sin(2 * np.pi * 37 * t))
        low = growl(sec, 42 + 4 * v, 18, ((260, 1, 120), (700, .5, 200)), 1.0) * 0.5
        hum = 0.15 * np.sin(2 * np.pi * 55 * t) * wobble(sec, 1, 0.3)
        x = mix(sat(normalize(b, 0), 2), low * env(sec, 0.6, 0.8), hum)
        if v == 1:                                                             # a distant, distorted scream inside it
            s = t_axis(1.4)
            sc = sat(normalize(voice(380 + 160 * np.sin(np.pi * s / 1.4), ((800, 1, 250), (1500, .6, 300), (2600, .3, 400)), sub=0.4), 0), 5)
            x = put(x, 0.35 * lowpass(sc, 1800) * env(1.4, 0.1, 0.5), 1.8)
        out(f"entity/hunter/ambient{v + 1}.ogg", verb(x, 1.6, 0.35), -4, 0.3, 0.8)
    for v in range(2):
        sec = 2.8
        t = t_axis(sec)
        f = 80 + 260 * np.clip(t / 0.5, 0, 1) - 120 * np.clip((t - 1.6) / 1.2, 0, 1)
        roar = voice(f, ((500, 1, 220), (1100, .8, 300), (2400, .4, 400)), sub=1.0)
        x = mix(sat(normalize(roar, 0), 6) * env(sec, 0.05, 0.8), 0.5 * thud(0.6, 50, 0.3, 0.6), 0.3 * bandpass(n(sec), 1500, 4500) * env(sec, 0.01, 1.5))
        out(f"entity/hunter/notice{v + 1}.ogg", verb(x, 1.4, 0.35), -0.5, 0.01, 0.5)
    for v in range(2):
        sec = 0.7
        t = t_axis(sec)
        metal = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * d) for f, d in ((143, 5), (311, 7), (467, 9)))
        x = mix(0.6 * metal, growl(sec, 55 + 10 * v, 26) * env(sec, 0.01, 0.4))
        out(f"entity/hunter/hurt{v + 1}.ogg", x, -2)
    sec = 3.5
    t = t_axis(sec)
    x = mix(growl(sec, 60, 18) * np.exp(-t * 0.8), 0.5 * sum(np.sin(2 * np.pi * f * t) * np.exp(-t * 1.2) for f in (97, 151, 233)),
            0.4 * clicks(sec, 20, 500, 3000))
    out("entity/hunter/death.ogg", verb(x, 1.5, 0.35), -1, 0.01, 0.8)
    for v in range(2):
        sec = 0.5
        t = t_axis(sec)
        x = mix(thud(0.4, 48, 0.4, 0.5), 0.25 * np.sin(2 * np.pi * (230 + 40 * v) * t) * np.exp(-t * 10))
        out(f"entity/hunter/step{v + 1}.ogg", x, -6, 0.002, 0.08)


# --- Colony ----------------------------------------------------------------------------------------------------------
def colony():
    for v in range(3):
        sec = 3.2 + 0.4 * v
        t = t_axis(sec)
        x = np.zeros(int(sec * SR))
        for k, (f0, at) in enumerate(((110, 0), (147, 0.15), (83, 0.3), (196, 0.45))):  # four throats, out of tune
            d = sec - at
            s = t_axis(d)
            f = (f0 * (1 + 0.04 * v)) * (1 + 0.3 * np.clip(s / 0.6, 0, 1)) * wobble(d, 4, 0.05)
            vo = sat(normalize(voice(f, ((600 - 60 * k, 1, 220), (1400, .6, 300), (2600, .3, 400)), sub=0.7), 0), 3)
            x = put(x, vo * env(d, 0.1, 0.9) * (0.8 - 0.12 * k), at)
        x = mix(x, 0.6 * growl(sec, 45, 22) * env(sec, 0.3, 1.0))
        out(f"entity/colony/scream{v + 1}.ogg", verb(x, 2.0, 0.4), -0.5, 0.02, 0.8)
    for v in range(3):
        sec = 2.4
        x = mix(0.7 * growl(sec, 48 + 8 * v, 15, ((280, 1, 140), (650, .6, 200)), 1.0) * env(sec, 0.4, 0.8),
                0.5 * breath(sec, 200, 1400, 1.5, wet=1.2), 0.4 * clicks(sec, 10, 300, 1500))
        out(f"entity/colony/chitter{v + 1}.ogg", verb(x, 1.0, 0.3), -3, 0.1, 0.5)
    for v in range(2):
        sec = 0.8
        x = mix(growl(sec, 75 - 10 * v, 30) * env(sec, 0.01, 0.5), 0.4 * tear(0.5))
        out(f"entity/colony/hurt{v + 1}.ogg", x, -2)
    sec = 4.0
    t = t_axis(sec)
    x = mix(growl(sec, 70, 14) * np.exp(-t * 0.6), 0.6 * breath(sec, 120, 700, 2, wet=1.5), 0.4 * tear(1.2))
    out("entity/colony/death.ogg", verb(x, 1.8, 0.35), -1, 0.01, 1.0)
    for v in range(2):
        x = mix(thud(0.45, 42 + 6 * v, 0.4, 0.8), 0.3 * breath(0.45, 200, 900, 1, wet=1.5))
        out(f"entity/colony/step{v + 1}.ogg", x, -5, 0.002, 0.1)


# --- Gaunt ------------------------------------------------------------------------------------------------------------
def gaunt():
    for v in range(3):
        sec = 3.0 + 0.3 * v
        x = mix(breath(sec, 600, 3600, 2, rasp=3 + v), 0.5 * growl(sec, 46 + 4 * v, 40, ((300, 1, 140), (800, .4, 200))) * env(sec, 0.6, 0.6))
        out(f"entity/gaunt/breath{v + 1}.ogg", verb(x, 0.7, 0.2), -4, 0.2, 0.4)
    for v in range(3):
        sec = 1.8 + 0.3 * v
        t = t_axis(sec)
        f = 140 + 320 * np.clip(t / 0.12, 0, 1) * np.exp(-t * 0.8) + 30 * v
        x = mix(sat(normalize(voice(f, ((700, 1, 250), (1300, .7, 300), (2500, .4, 400)), sub=0.8), 0), 7) * env(sec, 0.005, 0.6),
                0.5 * bandpass(n(sec), 1500, 5000) * env(sec, 0.005, 0.4) * (rng.random(int(sec * SR)) > 0.5))
        out(f"entity/gaunt/scream{v + 1}.ogg", verb(x, 1.3, 0.3), -0.5, 0.002, 0.4)
    for v in range(2):
        sec = 0.6
        x = mix(0.5 * breath(sec, 250, 1300, 1, rasp=2), growl(sec, 62 - 8 * v, 36, ((300, 1, 140), (750, .6, 200), (1500, .2, 300)), 0.9) * env(sec, 0.01, 0.35))
        out(f"entity/gaunt/hurt{v + 1}.ogg", lowpass(x, 2200), -2)
    sec = 2.6
    t = t_axis(sec)
    x = mix(growl(sec, 64, 30) * np.exp(-t * 1.0), breath(sec, 500, 3000, 1, rasp=4) * np.exp(-t * 0.8), 0.4 * clicks(sec, 14, 1000, 4000))
    out("entity/gaunt/death.ogg", verb(x, 1.2, 0.3), -1, 0.01, 0.6)
    for v in range(2):
        x = mix(clicks(0.3, 4, 1200, 4500, 700), 0.3 * bandpass(n(0.3), 300, 1200) * env(0.3, 0.02, 0.2), 0.3 * thud(0.25, 80, 0.3, 0.1))
        out(f"entity/gaunt/step{v + 1}.ogg", x, -8, 0.002, 0.05)


# --- The Flayed: the most frightening sound in the dimension ----------------------------------------------------------
def flayed_scream(v):
    sec = 4.4 + 0.4 * v
    t = t_axis(sec)
    shapes = [  # pitch contours: rising rage, broken sob, a long howl that cracks, a stuttering shriek
        lambda: 260 + 520 * np.clip(t / 0.4, 0, 1) ** 1.4 - 140 * np.clip((t - 2.6) / 1.6, 0, 1),
        lambda: 330 + 380 * np.clip(t / 0.3, 0, 1) + 90 * np.sin(2 * np.pi * 2.2 * t) * np.clip(t - 1.0, 0, 1),
        lambda: 300 + 450 * np.clip(t / 0.5, 0, 1) + 220 * (t > 2.1) - 260 * np.clip((t - 3.2) / 1.2, 0, 1),
        lambda: 360 + 420 * np.clip(t / 0.25, 0, 1) * (0.85 + 0.15 * np.sign(np.sin(2 * np.pi * 7 * t))),
    ]
    f0 = shapes[v]() * wobble(sec, 6, 0.04) * (1 + 0.03 * np.sin(2 * np.pi * (5.5 + v) * t) * np.clip(t - 0.6, 0, 1))
    human = voice(f0, ((850, 1, 260), (1250, .9, 300), (2850, .6, 420), (3900, .3, 500)), harm=30, jitter=0.03)
    gut = voice(f0 * 0.25, ((380, 1, 180), (820, .8, 260), (2000, .35, 400)), harm=30, sub=1.0, jitter=0.04)
    shriek = bandpass(n(sec), 1800, 4200) * (0.6 + 0.4 * np.sin(2 * np.pi * 31 * t)) + 0.4 * np.sin(2 * np.pi * np.cumsum(f0 * 3.01) / SR)
    e = env(sec, 0.06, 1.3)
    rage = np.clip((t - 0.3) / 0.6, 0, 1)
    x = mix(sat(normalize(human, 0), 5) * e, 0.9 * sat(normalize(gut, 0), 6) * e * rage, 0.35 * shriek * e * rage)
    x = put(x, 0.7 * tear(0.6), 0.0)                                                       # it tears itself open first
    x = mix(x, 0.4 * lowpass(sat(normalize(gut, 0), 3), 220) * e)                          # chest-shaking low end
    return verb(x, 2.8, 0.42, 3200)


def flayed():
    for v in range(4):
        out(f"entity/flayed/scream{v + 1}.ogg", flayed_scream(v), -0.2, 0.002, 1.0)
    for v in range(2):
        sec = 3.2
        x = mix(breath(sec, 180, 1500, 2, rasp=1, wet=1.6), 0.4 * growl(sec, 40 + 6 * v, 20, ((260, 1, 120), (600, .5, 200)), 1.0) * env(sec, 0.6, 0.8),
                0.25 * tear(0.5) if v == 1 else np.zeros(1))
        out(f"entity/flayed/ambient{v + 1}.ogg", verb(x, 1.0, 0.25), -5, 0.2, 0.5)
    for v in range(2):
        out(f"entity/flayed/tear{v + 1}.ogg", verb(mix(tear(0.8 + 0.2 * v), 0.5 * thud(0.5, 45, 0.4, 0.6)), 0.6, 0.2), -1)
    for v in range(2):
        sec = 0.9
        t = t_axis(sec)
        f = 240 * np.exp(-t * 1.2) + 60
        x = mix(sat(normalize(voice(f, ((600, 1, 240), (1200, .6, 300)), sub=1.0), 0), 5) * env(sec, 0.01, 0.5), 0.5 * tear(0.4))
        out(f"entity/flayed/hurt{v + 1}.ogg", x, -1.5)
    sec = 5.0
    t = t_axis(sec)
    f = 520 * np.exp(-t * 0.5) + 50
    x = mix(sat(normalize(voice(f, ((800, 1, 260), (1300, .8, 300), (2600, .4, 400)), sub=0.9), 0), 4) * env(sec, 0.02, 2.5),
            0.6 * breath(sec, 120, 600, 1.5, wet=2) * np.clip((t - 2) / 1, 0, 1), 0.5 * tear(1.2))
    out("entity/flayed/death.ogg", verb(x, 2.4, 0.4), -0.5, 0.01, 1.2)
    sec = 2.6
    t = t_axis(sec)
    pant = np.abs(np.sin(2 * np.pi * 1.7 * t)) ** 3
    x = mix(breath(sec, 180, 1400, 1, wet=1.4) * pant * 2, 0.4 * growl(sec, 38, 16) * pant)
    out("entity/flayed/falter.ogg", verb(x, 0.8, 0.2), -3, 0.05, 0.3)
    for v in range(2):
        out(f"entity/flayed/step{v + 1}.ogg", mix(thud(0.45, 40 + 5 * v, 0.4, 0.9), 0.3 * breath(0.45, 300, 1500, 1, wet=2)), -5, 0.002, 0.1)


# --- Headless Warden and Partygoer: only the too-high hurt/death sounds -------------------------------------------------
def others():
    for v in range(2):
        sec = 0.8
        x = mix(growl(sec, 48 - 6 * v, 22, ((260, 1, 120), (700, .5, 200)), 1.0) * env(sec, 0.01, 0.5), 0.4 * thud(0.5, 40, 0.3, 0.6))
        out(f"entity/headless_warden/hurt{v + 1}.ogg", x, -1.5)
    sec = 3.5
    t = t_axis(sec)
    x = mix(growl(sec, 52, 14) * np.exp(-t * 0.7), 0.5 * breath(sec, 100, 600, 1, wet=1.2), 0.4 * thud(0.8, 32, 0.5, 0.8))
    out("entity/headless_warden/death.ogg", verb(x, 1.8, 0.35), -1, 0.01, 0.8)
    for v in range(2):                                           # a rubbery, deflating groan instead of a squeak
        sec = 0.6
        t = t_axis(sec)
        f = 190 * np.exp(-t * 1.5) + 70 + 15 * v
        x = sat(normalize(voice(f, ((520, 1, 220), (1100, .5, 260)), sub=0.3), 0), 2.5) * env(sec, 0.01, 0.35)
        out(f"entity/partygoer/hurt{v + 1}.ogg", mix(x, 0.3 * bandpass(n(sec), 300, 1500) * env(sec, 0.01, 0.3)), -2.5)


# --- Ambience -------------------------------------------------------------------------------------------------------------
def ambience():
    for v in range(3):                                               # deep electrical buzz with flicker and crackle
        sec = 3.5
        t = t_axis(sec)
        hum = sum(a * np.sin(2 * np.pi * f * t) for f, a in ((50, 1), (100, .7), (150, .35), (250, .2), (350, .1)))
        flick = np.resize(np.where(rng.random(int(sec * 40) + 1) < 0.15 + 0.1 * v, 0.2, 1.0).repeat(int(SR / 40)), len(t))
        crack = (rng.random(len(t)) > 0.9993) * rng.standard_normal(len(t)) * 6
        x = sat(hum * lowpass(flick.astype(float), 60), 2) + bandpass(crack, 1500, 7000)
        out(f"ambient/liminal/buzz{v + 1}.ogg", x * env(sec, 0.4, 0.8), -6, 0.1, 0.4)
    for v in range(2):                                               # ventilation: a long breath of air through a duct
        sec = 6.0
        t = t_axis(sec)
        x = bandpass(n(sec), 120, 900) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.17 * t + v)) + 0.2 * np.sin(2 * np.pi * (63 + 7 * v) * t)
        out(f"ambient/liminal/vent{v + 1}.ogg", verb(x * env(sec, 1.5, 2.0), 1.0, 0.3), -8, 0.5, 1.0)
    for v in range(3):                                               # pipes: knocks running along, a metal groan
        sec = 3.0
        t = t_axis(sec)
        x = np.zeros(int(sec * SR))
        for k in range(4 + v):
            tt = t_axis(0.5)
            k_ = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * d) for f, d in ((180 + 30 * k, 12), (410 + 50 * k, 18), (760, 25)))
            x = put(x, k_ * 0.8 ** k, 0.15 + k * (0.25 + 0.05 * v))
        groan = np.sin(2 * np.pi * np.cumsum(90 + 40 * np.sin(2 * np.pi * 0.4 * t)) / SR) * env(sec, 0.6, 1.0) * 0.4
        out(f"ambient/liminal/pipe{v + 1}.ogg", verb(mix(x, lowpass(sat(groan, 3), 900)), 1.8, 0.45), -4, 0.01, 0.8)
    for v in range(3):                                               # something far away: screams, howls, a roar
        sec = 3.0
        if v == 0:
            x = flayed_scream(1)[:int(4 * SR)]
        elif v == 1:
            x = growl(sec, 55, 25) * env(sec, 0.4, 1.2)
        else:
            t = t_axis(sec)
            x = sat(normalize(voice(200 + 200 * np.sin(np.pi * t / sec), ((700, 1, 250), (1400, .6, 300)), sub=0.8), 0), 4) * env(sec, 0.3, 1.2)
        x = lowpass(x, 900 if v != 0 else 1400)
        out(f"ambient/liminal/distant{v + 1}.ogg", verb(x, 3.0, 0.6, 1500), -10, 0.3, 1.5)
    for v in range(2):                                               # generator halls: heavy machine hum
        sec = 4.0
        t = t_axis(sec)
        x = sum(a * np.sin(2 * np.pi * f * t + rng.uniform(0, 6)) for f, a in ((38, 1), (76, .8), (114, .4), (228, .15), (1220, .03)))
        x = x * (0.85 + 0.15 * np.sin(2 * np.pi * (2.5 + v) * t)) + 0.15 * bandpass(n(sec), 300, 2000) * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * t))
        out(f"ambient/industrial/machine{v + 1}.ogg", sat(x, 1.6) * env(sec, 0.8, 1.0), -4, 0.3, 0.6)
    sec = 2.0
    out("ambient/industrial/steam.ogg", verb(highpass(n(sec), 1500) * env(sec, 0.05, 1.2) * 0.8, 1.0, 0.3), -6)
    for v in range(3):                                               # labs and security rooms: server fans, beeps, relays
        sec = 4.0
        t = t_axis(sec)
        fan = bandpass(n(sec), 300, 2500) * 0.3 + 0.2 * np.sin(2 * np.pi * (120 + 15 * v) * t)
        x = fan.copy()
        for k in range(2 + v):
            bt = t_axis(0.12)
            x = put(x, 0.5 * np.sin(2 * np.pi * (1600 - 300 * k) * bt) * env(0.12, 0.005, 0.03), 0.5 + k * 1.1)
        x = put(x, clicks(0.2, 2, 1500, 4000), 2.7)
        out(f"ambient/lab/hum{v + 1}.ogg", x * env(sec, 0.6, 0.8), -9, 0.3, 0.6)
    for v in range(3):                                               # hotel: distant door, an elevator chime, muffled TV
        sec = 3.0
        t = t_axis(sec)
        if v == 0:
            x = lowpass(thud(0.6, 70, 0.3, 0.8), 600)
        elif v == 1:
            x = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * 1.6) for f in (880, 1108.7)) * 0.4
            x = x * (t < 2.2)
        else:
            x = lowpass(bandpass(n(sec), 300, 3000) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 3.1 * t))), 1200) * env(sec, 0.1, 0.4)
        out(f"ambient/hotel/far{v + 1}.ogg", verb(x, 2.2, 0.55, 2000), -9, 0.01, 0.8)
    # Level Fun: a cheerful little tune from a broken speaker, slowly going out of tune, and a music box gone wrong
    melody = [0, 4, 7, 12, 7, 4, 5, 9, 12, 9, 5, 2, 7, 11, 14, 11, 12, 7, 4, 0]
    for v in range(2):
        beat = 0.28 + 0.03 * v
        sec = beat * len(melody) + 1.0
        x = np.zeros(int(sec * SR))
        for k, st in enumerate(melody):
            f = 523.25 * 2 ** (st / 12) * (1 - 0.004 * k * (1 + v))                 # drifting flat
            tt = t_axis(beat * 0.95)
            note = (np.sign(np.sin(2 * np.pi * f * tt)) * 0.3 + np.sin(2 * np.pi * f * tt)) * np.exp(-tt * 4)
            x = put(x, note, k * beat)
        t = t_axis(sec)
        x = x * (0.75 + 0.25 * np.sin(2 * np.pi * 0.7 * t))                            # tape wobble
        x = lowpass(highpass(x, 400), 2600) + 0.05 * bandpass(n(sec), 2000, 6000)
        out(f"ambient/fun/party{v + 1}.ogg", verb(x, 1.6, 0.45), -8, 0.05, 0.8)
    sec = 4.0
    x = np.zeros(int(sec * SR))
    for k, st in enumerate([12, 11, 7, 6, 4, 3, 0, -1]):
        f = 1046.5 * 2 ** (st / 12)
        tt = t_axis(0.5)
        x = put(x, np.sin(2 * np.pi * f * tt) * np.exp(-tt * 6) + 0.3 * np.sin(2 * np.pi * f * 2.76 * tt) * np.exp(-tt * 9), k * 0.48 * (1 + 0.03 * k))
    out("ambient/fun/musicbox.ogg", verb(x, 2.0, 0.5), -10, 0.01, 0.8)
    sec = 5.0                                                          # the Poolrooms: a deep groan over the water
    t = t_axis(sec)
    x = lowpass(growl(sec, 36, 10, ((220, 1, 100), (500, .4, 150)), 1.0), 500) * env(sec, 1.5, 2.0)
    out("ambient/pool/distant.ogg", verb(x, 3.5, 0.65, 1200), -9, 0.5, 1.5)


if __name__ == '__main__':
    wolfspider()
    hound()
    hunter()
    colony()
    gaunt()
    flayed()
    others()
    ambience()
    print("0.18.0 sounds ok")
