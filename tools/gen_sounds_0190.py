"""Office Rooms 0.19.0: sounds of the Underside and of [Unknown]. Synthesised with the 0.18.0 building blocks."""
import numpy as np
from gen_sounds import SR, t_axis, lowpass, highpass, bandpass, normalize, fade, write_ogg
from gen_sounds_0180 import n, verb, sat, env, wobble, voice, growl, breath, clicks, thud, tear, mix, put, out, rng


def soil(sec, dens=0.01):
    """Crumbling earth: dense low grains."""
    t = t_axis(sec)
    g = (rng.random(len(t)) < dens) * rng.standard_normal(len(t))
    return bandpass(g, 120, 1800) * 3 + 0.5 * lowpass(n(sec), 160)


def rumble(sec, lo=28, hi=90):
    t = t_axis(sec)
    x = lowpass(n(sec), hi) * 6 + 0.6 * np.sin(2 * np.pi * np.cumsum(lo * wobble(sec, 2, 0.2)) / SR)
    return x * wobble(sec, 5, 0.4)


def roar(v):
    sec = 3.2 + 0.5 * v
    t = t_axis(sec)
    f = (55 + 25 * np.clip(t / 0.4, 0, 1) - 20 * np.clip((t - 1.8) / 1.5, 0, 1)) * (1 + 0.1 * v)
    deep = growl(sec, 1, 34, sub=1.0) * 0
    body = voice(f * wobble(sec, 4, 0.06), ((300, 1, 150), (700, .8, 220), (1600, .4, 380)), harm=36, sub=1.0, jitter=0.04)
    upper = voice(f * 4.02 * wobble(sec, 6, 0.05), ((900, 1, 280), (1300, .8, 300), (2900, .5, 450)), harm=26, jitter=0.03)
    e = env(sec, 0.12, 1.4)
    am = 0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 24 * t + 2 * wobble(sec, 6, 1)))
    x = mix(sat(normalize(body * am, 0), 6) * e, 0.45 * sat(normalize(upper, 0), 4) * e * np.clip((t - 0.25) / 0.5, 0, 1),
            0.35 * clicks(sec, 70, 700, 3500) * e, 0.5 * lowpass(sat(normalize(body, 0), 3), 140) * e, deep)
    return verb(x, 3.0, 0.45, 2600)


def hurt(v):
    sec = 0.9
    t = t_axis(sec)
    f = 70 + 30 * np.exp(-t * 4) + 8 * v
    x = mix(sat(normalize(voice(f, ((320, 1, 160), (850, .6, 260)), harm=30, sub=1.0, jitter=0.05), 0), 5) * env(sec, 0.02, 0.5),
            0.5 * clicks(sec, 30, 800, 3000), 0.4 * tear(0.5))
    return verb(lowpass(x, 3000), 1.2, 0.35, 2000)


def death():
    sec = 6.0
    t = t_axis(sec)
    f = 90 * np.exp(-t * 0.35) + 25
    x = mix(sat(normalize(voice(f * wobble(sec, 5, 0.08), ((300, 1, 160), (800, .7, 250), (1900, .35, 400)), harm=36, sub=1.0, jitter=0.06), 0), 6) * env(sec, 0.1, 2.5),
            0.4 * sat(normalize(voice(f * 4 * wobble(sec, 7, 0.06), ((900, 1, 280), (2800, .5, 450)), harm=24, jitter=0.04), 0), 4) * env(sec, 0.3, 3.0),
            0.6 * clicks(sec, 160, 600, 3500) * env(sec, 0.1, 3.5), 0.8 * rumble(sec) * env(sec, 1.0, 2.0))
    for k in range(7):
        x = put(x, 0.8 * thud(0.5, 45 + 5 * k), 0.6 + k * 0.62)
        x = put(x, 0.5 * tear(0.5), 0.5 + k * 0.62)
    return verb(x, 3.5, 0.45, 2200)


def burrow(v):
    sec = 1.8 + 0.4 * v
    x = mix(rumble(sec, 32 + 6 * v, 110), 0.8 * soil(sec, 0.012 + 0.004 * v), 0.3 * clicks(sec, 20, 400, 1500, 200))
    return lowpass(x, 1600) * env(sec, 0.4, 0.8)


def emerge(v):
    sec = 2.4
    t = t_axis(sec)
    x = mix(1.4 * thud(0.9, 40 + 8 * v, 0.6, 0.6), 1.2 * soil(sec, 0.05) * np.exp(-t * 1.6), 0.7 * rumble(sec, 30, 120) * env(sec, 0.01, 1.6),
            0.6 * bandpass(n(sec), 200, 2500) * np.exp(-t * 3) * np.clip(t / 0.08, 0, 1))
    return verb(x, 2.0, 0.35, 1800)


def quake(v):
    sec = 5.0 + v
    t = t_axis(sec)
    x = mix(1.3 * rumble(sec, 24 + 4 * v, 70), 0.5 * soil(sec, 0.006) * np.clip(t / sec * 1.5, 0, 1), 0.3 * clicks(sec, 25, 300, 1200, 150))
    return lowpass(x, 900) * env(sec, 1.6, 1.4)


def wind():
    sec = 26.0
    t = t_axis(sec)
    a = bandpass(n(sec), 200, 900) * wobble(sec, 0.4, 0.7) + 0.6 * bandpass(n(sec), 600, 2200) * wobble(sec, 0.6, 0.8) * 0.4
    hum = 0.08 * np.sin(2 * np.pi * 52 * t) * wobble(sec, 0.3, 0.5)
    x = normalize(a, -14) + hum
    k = int(3 * SR)                            # crossfade the tail into the head so it loops cleanly
    head, tail = x[:k], x[-k:]
    r = np.linspace(0, 1, k)
    x = x[k:].copy()
    x[-k:] = tail * (1 - r) + head * r
    return x


def key_use():
    sec = 3.0
    t = t_axis(sec)
    x = mix(1.5 * thud(0.8, 38, 0.5, 0.8), 0.9 * soil(sec, 0.02) * np.exp(-t * 1.2), 0.8 * rumble(sec, 26, 80) * env(sec, 0.05, 2.0),
            0.5 * highpass(clicks(0.6, 40, 1500, 6000, 600), 1200))
    return verb(x, 2.5, 0.4, 1500)


def glitch(up):
    sec = 0.9 if up else 0.6
    t = t_axis(sec)
    f = (300 + 1500 * (t / sec)) if up else (1500 - 1200 * (t / sec))
    tone = np.sign(np.sin(2 * np.pi * np.cumsum(f) / SR)) * 0.3 + np.sin(2 * np.pi * np.cumsum(f * 1.5) / SR) * 0.3
    crush = np.repeat(rng.standard_normal(len(t) // 40 + 1), 40)[:len(t)] * 0.4
    gate = (np.sin(2 * np.pi * 23 * t) > -0.2)
    return lowpass((tone + crush) * gate * env(sec, 0.01, 0.25), 6000)


if __name__ == '__main__':
    for v in range(3):
        out(f"entity/unknown/roar{v + 1}.ogg", roar(v))
        out(f"entity/unknown/burrow{v + 1}.ogg", burrow(v), -3)
    for v in range(2):
        out(f"entity/unknown/hurt{v + 1}.ogg", hurt(v))
        out(f"entity/unknown/emerge{v + 1}.ogg", emerge(v))
        out(f"ambient/underside/quake{v + 1}.ogg", quake(v), -2)
    out("entity/unknown/death.ogg", death())
    write_ogg("ambient/underside/wind.ogg", wind())
    out("item/underside_key_use.ogg", key_use())
    out("item/noclip_on.ogg", glitch(True), -4)
    out("item/noclip_off.ogg", glitch(False), -5)
    print("ok")
