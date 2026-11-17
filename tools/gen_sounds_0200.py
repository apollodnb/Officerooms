"""Office Rooms 0.20.0: research outpost sounds (synthesised)."""
import numpy as np
from gen_sounds import SR, t_axis, lowpass, highpass, bandpass, normalize, fade, write_ogg
from gen_sounds_0180 import n, verb, sat, env, wobble, clicks, thud, mix, put, out, rng


def tone(f, sec, shape='sin'):
    t = t_axis(sec)
    ph = 2 * np.pi * np.cumsum(np.broadcast_to(f, t.shape)) / SR
    return np.sin(ph) if shape == 'sin' else np.sign(np.sin(ph)) * 0.6 + 0.4 * np.sin(3 * ph)


def lever():
    x = mix(1.2 * thud(0.5, 70, 0.3, 0.3), 0.8 * highpass(clicks(0.35, 6, 1500, 6000, 200), 800), 0.5 * bandpass(n(0.4), 300, 2000) * env(0.4, 0.005, 0.15))
    return verb(x, 1.0, 0.3, 3000)


def power_on():
    sec = 3.5
    t = t_axis(sec)
    f = 30 + 30 * np.clip(t / 2.5, 0, 1) ** 0.7
    hum = sum(tone(f * k, sec) / k for k in (1, 2, 3, 4))
    x = mix(0.9 * sat(hum, 3) * np.clip(t / 2.0, 0, 1), 1.2 * thud(0.6, 50, 0.4, 0.4), 0.4 * clicks(sec, 30, 1500, 6000, 400) * np.exp(-t))
    return verb(x * env(sec, 0.01, 0.6), 1.5, 0.3, 2500)


def power_off():
    sec = 2.5
    t = t_axis(sec)
    f = 60 * np.exp(-t * 0.9) + 8
    hum = sum(tone(f * k, sec) / k for k in (1, 2, 3))
    return verb(mix(0.9 * sat(hum, 3) * np.exp(-t * 1.2), 1.0 * thud(0.5, 55, 0.3, 0.3)), 1.5, 0.3, 2500)


def alarm():
    sec = 1.9
    t = t_axis(sec)
    f = np.where((t % 0.9) < 0.45, 640, 480)
    x = tone(f, sec, 'sq')
    return verb(lowpass(sat(x, 2), 4000) * env(sec, 0.01, 0.15), 1.4, 0.35, 3000)


def beep(v):
    sec = 0.25
    return verb(tone(1200 + 400 * v, sec, 'sq') * env(sec, 0.003, 0.05) * 0.5, 0.5, 0.2, 5000)


def spark(v):
    sec = 0.5 + 0.2 * v
    t = t_axis(sec)
    x = highpass(clicks(sec, 160 + 60 * v, 2000, 9000, 800), 1500) + 0.5 * bandpass(n(sec), 3000, 9000) * (rng.random(len(t)) < 0.3)
    return x * env(sec, 0.002, 0.25)


def button():
    return verb(mix(1.0 * thud(0.25, 140, 0.15, 0.15), 0.7 * highpass(clicks(0.15, 3, 2000, 7000, 100), 1500)), 0.6, 0.25, 4000)


def charge():
    sec = 8.5
    t = t_axis(sec)
    f = 80 * np.exp(t / sec * np.log(18))
    whine = tone(f, sec) * 0.5 + tone(f * 1.01, sec) * 0.3
    hum = sum(tone(50 * k, sec) / k for k in (1, 2, 3))
    x = mix(whine * np.clip(t / sec, 0, 1) ** 1.5, 0.5 * hum, 0.4 * clicks(sec, 40, 2000, 8000, 600) * (t / sec), 0.3 * bandpass(n(sec), 200, 1200) * wobble(sec, 3, 0.6))
    return verb(sat(x, 2) * env(sec, 0.2, 0.3), 1.8, 0.3, 5000)


def gate_open():
    sec = 4.0
    t = t_axis(sec)
    x = mix(1.6 * thud(1.2, 35, 0.8, 0.8), 0.9 * lowpass(n(sec), 400) * np.exp(-t * 1.2), 0.6 * bandpass(n(sec), 500, 5000) * np.exp(-t * 3),
            0.5 * sum(tone(60 * k * (1 + 0.003 * k), sec) / k for k in (1, 2, 3, 5)) * np.exp(-t * 0.5))
    return verb(sat(x, 2), 3.0, 0.45, 2500)


def hum():
    sec = 3.0
    t = t_axis(sec)
    x = sum(tone(60 * k, sec) / k for k in (1, 2, 3, 4, 6)) * wobble(sec, 0.8, 0.3) + 0.2 * bandpass(n(sec), 2000, 6000) * wobble(sec, 4, 0.8)
    return lowpass(x, 3000) * env(sec, 0.6, 0.8)


if __name__ == '__main__':
    out("block/outpost/lever.ogg", lever())
    out("block/outpost/power_on.ogg", power_on())
    out("block/outpost/power_off.ogg", power_off())
    out("block/outpost/alarm.ogg", alarm(), -3)
    for v in range(2):
        out(f"block/outpost/beep{v + 1}.ogg", beep(v), -8)
    for v in range(3):
        out(f"block/outpost/spark{v + 1}.ogg", spark(v), -4)
    out("block/outpost/button.ogg", button())
    out("block/gateway/charge.ogg", charge(), -2)
    out("block/gateway/open.ogg", gate_open())
    out("block/gateway/hum.ogg", hum(), -6)
    print("ok")
