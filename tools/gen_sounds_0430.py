"""0.43.0: sounds of the Dead Mall, synthesised here (numpy + ffmpeg, mono ogg). Own compositions, nothing recorded or copied."""
import os, subprocess, tempfile, wave
import numpy as np
from scipy.signal import butter, lfilter
SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'office_rooms', 'sounds')
rng = np.random.default_rng(430)
def t(d): return np.arange(int(SR * d)) / SR
def lp(x, f): b, a = butter(2, f / (SR / 2)); return lfilter(b, a, x)
def hp(x, f): b, a = butter(2, f / (SR / 2), 'high'); return lfilter(b, a, x)
def verb(x, room=0.45, n=9):
    y = x.copy()
    for k in range(1, n + 1):
        d = int(SR * (0.041 * k + 0.017 * (k % 4)))
        y[d:] += x[:-d] * (room ** k)
    return y
def save(path, x, gain=0.85):
    x = x / (np.max(np.abs(x)) + 1e-9) * gain
    full = os.path.join(OUT, path + '.ogg'); os.makedirs(os.path.dirname(full), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
        wv = wave.open(f.name, 'wb'); wv.setnchannels(1); wv.setsampwidth(2); wv.setframerate(SR)
        wv.writeframes((x * 32767).astype(np.int16).tobytes()); wv.close()
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', f.name, '-c:a', 'libvorbis', '-q:a', '3', full], check=True)
    os.unlink(f.name)
def mtof(m): return 440 * 2 ** ((m - 69) / 12)

def ep(freq, d, vel=1.0):
    """soft electric-piano tone: a sine pair with a bell attack and slow decay"""
    tt = t(d)
    e = np.exp(-tt * 1.6) * (1 - np.exp(-tt * 300))
    x = np.sin(2 * np.pi * freq * tt) + 0.25 * np.sin(2 * np.pi * 2 * freq * tt) * np.exp(-tt * 6) + 0.08 * np.sin(2 * np.pi * 7.02 * freq * tt) * np.exp(-tt * 14)
    return x * e * vel

def muzak(seed, bars):
    r = np.random.default_rng(seed)
    bpm = 76; beat = 60 / bpm; total = bars * 4 * beat + 3
    out = np.zeros(int(SR * total))
    prog = [[57, 60, 64, 67], [53, 57, 60, 64], [55, 59, 62, 65], [52, 55, 59, 62]]   # Am7, Fmaj7, G7, Em7 (voiced softly)
    r.shuffle(prog)
    scale = [69, 71, 72, 74, 76, 79, 81]
    for b in range(bars):
        ch = prog[b % 4]
        start = int(SR * b * 4 * beat)
        for k, n in enumerate(ch):                                   # chord, slightly rolled
            s = start + int(SR * 0.03 * k)
            tone = ep(mtof(n), 4 * beat + 0.8, 0.45)
            out[s:s + len(tone)] += tone[:len(out) - s]
        bass = ep(mtof(ch[0] - 12), 2 * beat, 0.6)
        for h in (0, 2):
            s = start + int(SR * h * beat)
            out[s:s + len(bass)] += bass[:len(out) - s]
        for h in range(4):                                           # a lazy melody
            if r.random() < 0.7:
                n = scale[r.integers(len(scale))]
                s = start + int(SR * (h + r.choice([0, 0.5])) * beat)
                tone = ep(mtof(n), beat * 1.5, 0.35)
                out[s:s + len(tone)] += tone[:len(out) - s]
    tt = np.arange(len(out)) / SR                                     # worn tape: wow, hiss, a tinny ceiling speaker
    wow = 1 + 0.004 * np.sin(2 * np.pi * 0.55 * tt) + 0.002 * np.sin(2 * np.pi * 3.1 * tt)
    idx = np.clip(np.cumsum(wow), 0, len(out) - 1).astype(int)
    out = out[idx]
    out = hp(lp(out, 3200), 280) + 0.012 * hp(rng.standard_normal(len(out)), 4000)
    out = verb(out, 0.5, 10)
    fade = np.ones(len(out)); n = int(SR * 2.5); fade[-n:] = np.linspace(1, 0, n); fade[:int(SR * 0.6)] = np.linspace(0, 1, int(SR * 0.6))
    return out * fade

def chime():
    """the three-tone announcement chime, then a breath of static where someone should speak"""
    out = np.zeros(int(SR * 5.5))
    for k, n in enumerate((76, 72, 67)):
        tone = ep(mtof(n), 2.2, 0.9)
        s = int(SR * 0.55 * k)
        out[s:s + len(tone)] += tone
    s = int(SR * 2.6); n = int(SR * 2.0)
    hiss = hp(lp(rng.standard_normal(n), 2500), 400) * 0.08 * np.hanning(n)
    out[s:s + n] += hiss
    return verb(hp(lp(out, 3500), 300), 0.55, 10)

save('block/mall/muzak1', muzak(1, 6))
save('block/mall/muzak2', muzak(2, 6))
save('block/mall/muzak3', muzak(3, 8))
save('block/mall/chime', chime())
print('mall sounds done')
