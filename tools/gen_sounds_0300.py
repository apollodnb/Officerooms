#!/usr/bin/env python3
"""Office Rooms 0.30.0 - builds mob screams, hurt sounds and music from the author's Ableton recordings.

Usage: python3 gen_sounds_0300.py <dir with flayed.wav hound.wav colony.wav music2.wav music3.wav level_fun.wav>

Mob sounds are written as MONO Ogg Vorbis (so Minecraft attenuates them by distance), music as stereo.
Idle/ambient sounds are not touched.
"""
import os
import subprocess
import sys

import numpy as np

SRC = sys.argv[1]
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "office_rooms", "sounds")
SR = 22050


def load(name):
    raw = subprocess.run(["sox", os.path.join(SRC, name), "-t", "f32", "-r", str(SR), "-c", "1", "-"],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype=np.float32)


def loudest(sig, a, b, length):
    """Start time of the loudest window of `length` seconds between a and b."""
    w = int(length * SR)
    seg = sig[int(a * SR):int(b * SR)] ** 2
    if len(seg) <= w:
        return a
    c = np.convolve(seg, np.ones(w), "valid")
    return a + int(np.argmax(c)) / SR


def sox(src, dst, start, dur, *effects, fade=(0.01, 0.12), norm=-0.3, stereo=False, quality=5):
    dst = os.path.join(OUT, dst + ".ogg")
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    cmd = ["sox", "-V1", os.path.join(SRC, src), "-C", str(quality)]
    if not stereo:
        cmd += ["-c", "1"]
    cmd += [dst]
    if start is not None:
        cmd += ["trim", str(start), str(dur)]
    if not stereo:
        cmd += ["remix", "-"]
    cmd += ["gain", "-4"]
    if fade:
        cmd += ["fade", "t", str(fade[0]), "-0", str(fade[1])]
    if not stereo:
        # 0.30.1: much louder - compress the dynamics first, effects after, then normalise close to full scale
        cmd += ["compand", "0.01,0.25", "6:-60,-60,-36,-14,-20,-6,-8,-2,0,-1", "-3", "-90", "0.02"]
    cmd += list(effects)
    if norm is not None:
        cmd += ["norm", str(norm)]
    if not stereo:
        cmd += ["rate", "44100"]
    subprocess.run(cmd, check=True)
    print("wrote", os.path.relpath(dst, OUT))


def main():
    fl, ho, co = load("flayed.wav"), load("hound.wav"), load("colony.wav")

    # --- 0.30.1: the full recordings, looped while Flayed / Hound hunt ---------------------------
    sox("flayed.wav", "entity/flayed/hunt_loop", None, None, fade=(0.02, 0.05))
    sox("hound.wav", "entity/hound/hunt_loop", None, None, fade=(0.02, 0.05))

    # --- Trigger screams -------------------------------------------------------------------------
    for i, (a, b) in enumerate([(0.25, 5.9), (6.3, 15.8), (16.2, 17.2)], 1):
        sox("flayed.wav", f"entity/flayed/scream{i}", a, b - a, fade=(0.02, 0.35))
    hound_bursts = [(0.0, 1.9), (2.5, 4.6), (5.0, 7.6), (8.25, 9.8)]
    for i, (a, b) in enumerate(hound_bursts, 1):
        sox("hound.wav", f"entity/hound/bark{i}", a, b - a, fade=(0.01, 0.25))
    colony_calls = [(0.75, 2.75), (3.25, 4.75), (5.25, 6.75), (7.25, 8.75), (9.5, 11.25), (12.0, 13.5), (14.0, 15.3)]
    for i, (a, b) in enumerate(colony_calls, 1):
        sox("colony.wav", f"entity/colony/scream{i}", a, b - a, fade=(0.01, 0.25))

    # --- Hurt sounds of the three: the harshest moment of their own voice, short and choked -------
    def hurt(src, sig, ranges, dst, length, *fx):
        for i, (a, b) in enumerate(ranges, 1):
            t = loudest(sig, a, b, length)
            sox(src, f"{dst}{i}", t, length, *fx, fade=(0.005, length * 0.45))

    hurt("flayed.wav", fl, [(0.25, 5.9), (6.3, 11), (11, 15.8)], "entity/flayed/hurt", 0.7, "pitch", "150", "overdrive", "6")
    hurt("hound.wav", ho, [(0.0, 1.9), (2.5, 4.6), (5.0, 7.6)], "entity/hound/hurt", 0.5, "pitch", "200", "overdrive", "5")
    hurt("colony.wav", co, [(0.75, 2.75), (5.25, 6.75), (9.5, 11.25)], "entity/colony/hurt", 0.6, "pitch", "120", "overdrive", "5")

    # --- Creepier hurt sounds for every other mob, built from the same recordings ----------------
    # wolfspider: colony clicks, high and dry, reversed tail
    hurt("colony.wav", co, [(3.25, 4.75), (7.25, 8.75), (12.0, 13.5)], "entity/wolfspider/hurt", 0.4,
         "pitch", "700", "reverse", "fade", "t", "0.15", "reverse")
    # headless warden: Flayed scream an octave down, huge and hollow
    hurt("flayed.wav", fl, [(0.25, 5.9), (6.3, 15.8)], "entity/headless_warden/hurt", 0.9,
         "pitch", "-900", "overdrive", "8", "reverb", "60", "50", "90")
    # gaunt: thin, breathy, slightly higher Flayed shriek with a short echo
    hurt("flayed.wav", fl, [(11, 15.8), (16.2, 17.2)], "entity/gaunt/hurt", 0.55,
         "pitch", "450", "highpass", "500", "echo", "0.8", "0.6", "60", "0.4")
    # hunter: hound burst dropped low, guttural
    hurt("hound.wav", ho, [(5.0, 7.6), (8.25, 9.8)], "entity/hunter/hurt", 0.6,
         "pitch", "-500", "overdrive", "10", "lowpass", "2500")
    # partygoer: colony call pitched up into a strangled giggle-shriek
    hurt("colony.wav", co, [(0.75, 2.75), (14.0, 15.3)], "entity/partygoer/hurt", 0.45,
         "pitch", "900", "tremolo", "18", "70")
    # deathmoth: tiny, insectoid flutter scream
    t = loudest(co, 9.5, 11.25, 0.35)
    sox("colony.wav", "entity/deathmoth/hurt", t, 0.35, "pitch", "1200", "tremolo", "40", "90",
        fade=(0.005, 0.15))
    # the burrowing Unknown: reversed Flayed scream dropped low, swells into the hit
    hurt("flayed.wav", fl, [(0.25, 5.9), (6.3, 15.8)], "entity/unknown/hurt", 0.8,
         "reverse", "pitch", "-700", "overdrive", "6", "reverb", "40")

    # --- Music (stereo, streamed) -------------------------------------------------------------
    sox("music2.wav", "music/office_rooms2", None, None, fade=(0.5, 2.0), norm=-0.3, stereo=True, quality=4)
    sox("music3.wav", "music/office_rooms3", None, None, fade=(0.5, 2.0), norm=-0.3, stereo=True, quality=4)
    sox("level_fun.wav", "music/level_fun", None, None, fade=None, norm=-0.3, stereo=True, quality=4)


if __name__ == "__main__":
    main()
