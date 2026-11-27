"""0.42.0: sounds of the SS Fun =) and the Mascot, synthesised here (numpy + ffmpeg, mono ogg). Nothing recorded or copied."""
import os, subprocess, tempfile
import numpy as np
from scipy.signal import butter, lfilter
SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'office_rooms', 'sounds')
rng = np.random.default_rng(420)


def t(d): return np.arange(int(SR * d)) / SR
def lp(x, f): b, a = butter(2, f / (SR / 2)); return lfilter(b, a, x)
def hp(x, f): b, a = butter(2, f / (SR / 2), 'high'); return lfilter(b, a, x)
def bp(x, lo, hi): b, a = butter(2, [lo / (SR / 2), hi / (SR / 2)], 'band'); return lfilter(b, a, x)
def env(n, a=0.01, r=0.1):
    e = np.ones(n); ai, ri = int(a * SR), int(r * SR)
    e[:ai] = np.linspace(0, 1, ai); e[-ri:] *= np.linspace(1, 0, ri); return e
def osc(freq):
    ph = 2 * np.pi * np.cumsum(freq) / SR; return np.sin(ph)
def verb(x, room=0.35, n=6):
    y = x.copy()
    for k in range(1, n + 1):
        d = int(SR * (0.023 * k + 0.011 * (k % 3)))
        y[d:] += x[:-d] * (room ** k)
    return y
def save(path, x, gain=0.9):
    x = x / (np.max(np.abs(x)) + 1e-9) * gain
    full = os.path.join(OUT, path + '.ogg'); os.makedirs(os.path.dirname(full), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
        import wave
        wv = wave.open(f.name, 'wb'); wv.setnchannels(1); wv.setsampwidth(2); wv.setframerate(SR)
        wv.writeframes((x * 32767).astype(np.int16).tobytes()); wv.close()
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', f.name, '-c:a', 'libvorbis', '-q:a', '4', full], check=True)
    os.unlink(f.name)


def squeak(p):
    d = 0.32; tt = t(d)
    f = p * (900 + 600 * np.sin(np.pi * tt / d) ** 0.7 - 300 * tt / d)
    x = osc(f) + 0.4 * osc(2 * f) + 0.15 * rng.standard_normal(len(tt))
    x = bp(x, 500, 4000) * env(len(tt), 0.02, 0.08)
    gurgle = lp(rng.standard_normal(len(tt)), 300) * 0.6 * env(len(tt), 0.05, 0.1)
    return verb(x + gurgle, 0.25)
for i, p in enumerate((1.0, 0.82, 1.18)):
    save(f'entity/mascot/squeak{i + 1}', squeak(p))


def giggle(base, n):
    parts = []
    for k in range(n):
        d = 0.11 + 0.03 * rng.random(); tt = t(d)
        f0 = base * (1.15 - 0.05 * k) * (1 + 0.03 * np.sin(2 * np.pi * 9 * tt))
        x = osc(f0) + 0.5 * osc(2 * f0) + 0.3 * osc(3 * f0)
        x = bp(x, 400, 2400) + 0.2 * bp(rng.standard_normal(len(tt)), 1500, 3000)
        parts.append(x * env(len(tt), 0.01, 0.05)); parts.append(np.zeros(int(SR * (0.05 + 0.04 * rng.random()))))
    x = np.concatenate(parts)
    return verb(lp(x, 1800), 0.45, 8)                         # muffled, as if through wet fabric
save('entity/mascot/giggle1', giggle(340, 5)); save('entity/mascot/giggle2', giggle(300, 7))

tt = t(0.75)
honk = np.sign(osc(330 * np.ones(len(tt)))) * 0.5 + np.sign(osc(415 * np.ones(len(tt)))) * 0.4
honk = np.tanh(3 * lp(honk, 2500)) * env(len(tt), 0.01, 0.2)
save('entity/mascot/honk', verb(honk, 0.5, 8))
for i in range(2):
    tt = t(0.35)
    q = squeak(0.7 + 0.2 * i); m = min(len(tt), len(q))
    x = lp(rng.standard_normal(m), 700 + 300 * i) * env(m, 0.005, 0.2) + 0.5 * q[:m]
    save(f'entity/mascot/hurt{i + 1}', x)
tt = t(2.2)
f = 1100 * np.exp(-tt * 1.6) + 120
death = (osc(f) + 0.3 * osc(2.01 * f)) * env(len(tt), 0.02, 0.6) + 0.5 * hp(rng.standard_normal(len(tt)), 3000) * np.exp(-tt * 1.2)
save('entity/mascot/death', verb(death, 0.4))
for i in range(2):
    tt = t(0.22)
    x = lp(rng.standard_normal(len(tt)), 450 + 150 * i) * np.exp(-tt * 18) + 0.3 * bp(rng.standard_normal(len(tt)), 2000, 6000) * np.exp(-tt * 30)
    save(f'entity/mascot/step{i + 1}', x)

tt = t(4.2)
sw = np.minimum(1, tt / 0.6) * np.minimum(1, (4.2 - tt) / 1.2)
f1 = 72 * (1 + 0.004 * np.sin(2 * np.pi * 0.7 * tt))
horn = (2 * ((f1 * tt) % 1) - 1) + 0.7 * (2 * ((1.5 * f1 * tt) % 1) - 1)
horn = lp(horn, 600) * sw
save('ambient/ship/horn', verb(horn, 0.55, 10))

d = 28.0; tt = t(d)
waves = lp(np.cumsum(rng.standard_normal(len(tt))) * 0.02, 180)
waves = (waves - waves.mean()) * (0.6 + 0.4 * np.sin(2 * np.pi * tt / 7.0))
creak = np.zeros(len(tt))
for k in range(6):
    s = int(SR * rng.uniform(1, d - 3)); L = int(SR * rng.uniform(0.6, 1.6)); ts = np.arange(L) / SR
    f = rng.uniform(170, 320) + 60 * np.sin(2 * np.pi * rng.uniform(0.5, 1.5) * ts)
    c = bp(osc(f) * (1 + 0.6 * rng.standard_normal(L)), 120, 900) * np.sin(np.pi * ts / ts[-1]) ** 2
    creak[s:s + L] += c * rng.uniform(0.15, 0.35)
amb = waves / (np.max(np.abs(waves)) + 1e-9) * 0.7 + creak
fade = int(SR * 1.0); amb[:fade] *= np.linspace(0, 1, fade); amb[-fade:] *= np.linspace(1, 0, fade)
save('ambient/ship/hull', verb(amb, 0.3), 0.6)

tt = t(2.8)
clicks = np.zeros(len(tt))
for k in range(60):
    s = int(SR * (0.05 + k * 0.03 + 0.01 * rng.random()))
    if s < len(tt) - 400:
        clicks[s:s + 300] += hp(rng.standard_normal(300), 2500) * np.exp(-np.arange(300) / 60) * (1 - k / 70)
splash = np.zeros(len(tt)); s = int(SR * 2.0)
splash[s:] = lp(rng.standard_normal(len(tt) - s), 2500) * np.exp(-np.arange(len(tt) - s) / SR * 4)
save('block/ship/lifeboat', verb(clicks + 1.5 * splash, 0.35))
print('ok')
