"""
0.7.0: voice of the Hunter (Poolrooms). Synthesized here (numpy/scipy), no foreign audio material.
Output: src/main/resources/assets/office_rooms/sounds/entity/hunter/*.ogg
"""
import numpy as np
from gen_sounds import SR, t_axis, lowpass, highpass, bandpass, normalize, fade, reverb, write_ogg

rng = np.random.default_rng(20261109)


def noise(sec):
    return rng.standard_normal(int(sec * SR))


def wire_hum(sec, base):
    """Elektrisches Summen dünner Drähte: leicht verstimmte Obertöne, langsam schwebend, Knistern."""
    t = t_axis(sec)
    y = np.zeros_like(t)
    for k, a in [(1, 0.5), (2, 0.3), (3, 0.18), (5, 0.1), (7, 0.06)]:
        f = base * k * (1 + 0.004 * np.sin(2 * np.pi * (0.3 + 0.07 * k) * t))
        y += a * np.sin(2 * np.pi * np.cumsum(f) / SR)
    crackle = highpass(noise(sec), 3000) * (rng.random(len(t)) > 0.997) * 2.5
    return y * (0.6 + 0.4 * np.sin(2 * np.pi * 0.5 * t) ** 2) + crackle


# ambient: tiefes, schwebendes Drahtsummen
y = wire_hum(4.0, 55.0)
write_ogg("entity/hunter/ambient.ogg", fade(normalize(reverb(y, 0.35, 0.5), -6), 0.8, 1.2))

# notice: zwei Drahtakkorde, die nach unten gleiten, dazu ein verzerrtes Rauschen
sec = 3.0
t = t_axis(sec)
f = 220 * np.exp(-t * 0.35)
y = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.6 * np.sin(2 * np.pi * np.cumsum(f * 1.498) / SR)
y = np.tanh(2.5 * y) * np.exp(-t * 0.6) + 0.25 * bandpass(noise(sec), 800, 3000) * np.exp(-t * 1.5)
write_ogg("entity/hunter/notice.ogg", fade(normalize(reverb(y, 0.5, 0.6), -3), 0.02, 0.8))

# hurt: kurzes Reißen einer Saite
sec = 0.6
t = t_axis(sec)
y = np.sin(2 * np.pi * 330 * t * (1 - 0.4 * t)) * np.exp(-t * 9) + 0.4 * highpass(noise(sec), 2500) * np.exp(-t * 20)
write_ogg("entity/hunter/hurt.ogg", fade(normalize(y, -3), 0.002, 0.15))

# death: alle Drähte reißen nacheinander, dann verklingt das Summen
sec = 3.5
y = np.zeros(int(sec * SR))
for i in range(7):
    at = 0.05 + i * 0.22 + rng.uniform(0, 0.08)
    d = 0.8
    tt = t_axis(d)
    snap = np.sin(2 * np.pi * rng.uniform(200, 700) * tt * (1 - 0.5 * tt)) * np.exp(-tt * 7)
    i0 = int(at * SR)
    y[i0:i0 + len(snap)] += snap[:len(y) - i0]
y += 0.5 * wire_hum(sec, 55.0) * np.exp(-t_axis(sec) * 1.2)
write_ogg("entity/hunter/death.ogg", fade(normalize(reverb(y, 0.4, 0.6), -3), 0.01, 1.0))
print("hunter sounds ok")
