"""Office Rooms 0.24.0: Deathmoth sounds (synthesised)."""
import json, os
import numpy as np
from gen_sounds import SR, t_axis, lowpass, highpass, bandpass, normalize, fade, write_ogg
from gen_sounds_0180 import n, verb, sat, env, wobble, clicks, thud, mix, out, rng
from gen_sounds_0200 import tone


def flutter(sec, rate, lo=200, hi=2200, jitter=0.15):
    """Wing beats: dusty noise bursts at about `rate` per second."""
    t = t_axis(sec)
    r = rate * (1 + jitter * (wobble(sec, 1.3, 1.0) - 0.5))
    ph = 2 * np.pi * np.cumsum(r) / SR
    beat = np.clip(np.sin(ph), 0, 1) ** 3
    return bandpass(n(sec), lo, hi) * beat


def idle(v):
    sec = 0.9 + 0.3 * v
    x = mix(0.7 * flutter(sec, 9 + 3 * v, 300, 1800), 0.25 * lowpass(tone(70 + 9 * v, sec, 'sq'), 400) * wobble(sec, 3, 0.8))
    return verb(x * env(sec, 0.15, 0.3), 0.9, 0.3, 2000)


def trigger():
    sec = 1.1
    t = t_axis(sec)
    x = mix(1.2 * flutter(sec, 26, 150, 3500) * np.exp(-t * 0.8), 0.6 * thud(0.25, 90, 0.4, 0.6),
            0.4 * bandpass(n(sec), 2500, 7000) * np.exp(-t * 6))
    return verb(sat(x, 2.0), 0.8, 0.25, 4000)


def flap(v):
    sec = 0.55
    return lowpass(flutter(sec, 18 + 4 * v, 150, 2600), 3000) * env(sec, 0.02, 0.15)


def attack(v):
    sec = 1.6
    t = t_axis(sec)
    f = 1700 + 900 * np.sin(2 * np.pi * (2.3 + v) * t) * np.exp(-t * 0.6)
    screech = bandpass(sat(tone(f, sec, 'sq'), 3), 900, 6000)
    buzz = sat(sum(tone((95 + 12 * v) * k, sec) / k for k in (1, 2, 3, 5)), 2.5)
    x = mix(0.55 * screech * env(sec, 0.01, 0.4), 0.8 * buzz * wobble(sec, 31, 0.7), 0.9 * flutter(sec, 32, 200, 4000))
    return verb(x, 0.6, 0.2, 5000)


def hurt():
    sec = 0.45
    t = t_axis(sec)
    x = mix(bandpass(sat(tone(2300 - 1500 * t, sec, 'sq'), 3), 800, 5000) * np.exp(-t * 7), flutter(sec, 30, 200, 3000))
    return x


def death():
    sec = 1.4
    t = t_axis(sec)
    x = mix(0.7 * bandpass(sat(tone(1900 * np.exp(-t * 1.5) + 200, sec, 'sq'), 3), 400, 5000) * np.exp(-t * 2.5),
            0.8 * flutter(sec, 22, 150, 2500) * np.exp(-t * 2.2), 0.5 * thud(0.4, 60, 0.3, 0.5))
    return verb(x, 1.0, 0.3, 3000)


if __name__ == '__main__':
    for v in range(3):
        out(f"entity/deathmoth/idle{v + 1}.ogg", idle(v), -14)
    out("entity/deathmoth/trigger.ogg", trigger(), -1)
    for v in range(2):
        out(f"entity/deathmoth/flap{v + 1}.ogg", flap(v), -6)
        out(f"entity/deathmoth/attack{v + 1}.ogg", attack(v), -2)
    out("entity/deathmoth/hurt.ogg", hurt(), -3)
    out("entity/deathmoth/death.ogg", death(), -2)
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'office_rooms', 'sounds.json')
    d = json.load(open(p))
    O = 'office_rooms:entity/deathmoth/'
    def ev(name, files, dist, sub, vol=1.0):
        d[f'entity.deathmoth.{name}'] = {"category": "hostile", "sounds": [{"name": O + f, "attenuation_distance": dist, "volume": vol} for f in files],
                                        "subtitle": f"subtitles.office_rooms.entity.deathmoth.{sub}"}
    ev('idle', ['idle1', 'idle2', 'idle3'], 8, 'idle', 0.6)
    ev('trigger', ['trigger'], 24, 'trigger')
    ev('flap', ['flap1', 'flap2'], 16, 'flap', 0.8)
    ev('attack', ['attack1', 'attack2'], 16, 'attack')
    ev('hurt', ['hurt'], 16, 'hurt')
    ev('death', ['death'], 16, 'death')
    json.dump(d, open(p, 'w'), indent=1)
    print("ok")
