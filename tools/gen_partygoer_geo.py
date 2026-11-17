"""Office Rooms 0.16.0: the Partygoer (own design). A tall, slender, glossy yellow figure with a painted smiley face
and a striped party hat. Writes geo, animations, texture and an empty glowmask. Run: python3 tools/gen_partygoer_geo.py"""
import os
from PIL import Image
from geo_lib import Geo, keys, wave, snaps, ramp, _shade

SIDES = (('r', -1), ('l', 1))


def plastic(base=(242, 206, 38)):
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        for j in range(ph):
            for i in range(pw):
                s = _shade(face, j, ph) // 2 + g.rng.randint(-3, 3)
                c = [b + s for b in base]
                if face not in ('up', 'down') and i == 1 and j < ph - 2:
                    c = [min(255, x + 26) for x in c]                     # glossy highlight
                g.px(u + i, v + j, c + [alpha])
    return paint


def stripes(a, b):
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        for j in range(ph):
            for i in range(pw):
                c = a if ((i + j) // 2) % 2 == 0 else b
                g.px(u + i, v + j, [x + g.rng.randint(-6, 6) for x in c] + [alpha])
    return paint


def smiley(g, r):
    u, v, w, h = r
    K = (16, 12, 10, 255)
    for cx in (w // 2 - 4, w // 2 + 2):                                   # two round black eyes
        for a in range(2):
            for b in range(3):
                g.px(u + cx + a, v + 3 + b, K)
    mid = (w - 1) / 2
    for i in range(1, w - 1):                                              # one huge, thin smile
        d = abs(i - mid) / mid
        y = v + int(round(h - 4 - 4 * d * d))
        g.px(u + i, y, K)
        if d < 0.8:
            g.px(u + i, y + 1, K)


Y = plastic()
Y2 = plastic((230, 192, 30))
HAT = stripes((236, 70, 150), (70, 160, 236))
POM = plastic((250, 250, 250))


def partygoer():
    g = Geo('partygoer', seed=6060)
    g.bone('root', None, [0, 0, 0])
    g.bone('hips', 'root', [0, 18, 0])
    g.cube('hips', [-2.2, 17, -1.2], [4.4, 2.5, 2.4], Y2)
    for side, s in SIDES:
        t, sh = 'thigh_' + side, 'shin_' + side
        g.bone(t, 'hips', [s * 1.2, 18, 0])
        g.cube(t, [s * 1.2 - 0.9, 9, -0.9], [1.8, 9, 1.8], Y)
        g.bone(sh, t, [s * 1.2, 9, 0])
        g.cube(sh, [s * 1.2 - 0.8, 0.8, -0.8], [1.6, 8.2, 1.6], Y)
        g.cube(sh, [s * 1.2 - 0.9, 0, -2], [1.8, 0.8, 2.8], Y2)           # flat feet
    g.bone('torso', 'hips', [0, 19, 0])
    g.cube('torso', [-2, 19, -1.1], [4, 5, 2.2], Y)
    g.cube('torso', [-2.6, 24, -1.3], [5.2, 4.5, 2.6], Y)
    g.bone('neck', 'torso', [0, 28.5, 0])
    g.cube('neck', [-0.6, 28.5, -0.6], [1.2, 1.6, 1.2], Y2)
    g.bone('head', 'neck', [0, 30, 0])
    g.cube('head', [-3, 30, -3], [6, 6.5, 6], Y, face=smiley)
    for k, (wd, ht) in enumerate(((4, 1.6), (3, 1.6), (2, 1.6), (1, 1.4))):  # the party hat, slightly tilted
        g.cube('head', [-wd / 2 + 0.4, 36.5 + k * 1.6, -wd / 2], [wd, ht, wd], HAT, rot=[0, 0, -8], pivot=[0, 36.5, 0])
    g.cube('head', [-0.1, 42.6, -0.5], [1, 1, 1], POM, rot=[0, 0, -8], pivot=[0, 36.5, 0])
    for side, s in SIDES:
        a, f, h = 'arm_' + side, 'forearm_' + side, 'hand_' + side
        g.bone(a, 'torso', [s * 3.2, 28, 0])
        g.cube(a, [s * 3.2 - 0.7, 19, -0.7], [1.4, 9, 1.4], Y)
        g.bone(f, a, [s * 3.2, 19, 0])
        g.cube(f, [s * 3.2 - 0.6, 10.5, -0.6], [1.2, 8.5, 1.2], Y)
        g.bone(h, f, [s * 3.2, 10.5, 0])
        g.cube(h, [s * 3.2 - 0.9, 8, -0.9], [1.8, 2.5, 1.8], Y2)

    A = {}
    L = 4.0
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "torso": {"rotation": wave(L, [0, 0, 3])},
        "head": {"rotation": ramp([(0, [0, 0, 0]), (1.4, [0, 0, 16]), (2.6, [0, 0, 16]), (3.2, [0, 0, -6]), (L, [0, 0, 0])])},
        "arm_r": {"rotation": wave(L, [0, 0, 4], 0.5)}, "arm_l": {"rotation": wave(L, [0, 0, -4], 0.5)}}}

    def legs(L, a):
        return {"thigh_r": {"rotation": wave(L, a)}, "thigh_l": {"rotation": wave(L, -a)},
                "shin_r": {"rotation": wave(L, a * 0.5, 1.2, base=(a * 0.4, 0, 0))}, "shin_l": {"rotation": wave(L, a * 0.5, 1.2 + 3.14159, base=(a * 0.4, 0, 0))}}
    W = 1.2                                                                # bouncy, cheerful walk
    A["walk"] = {"loop": True, "animation_length": W, "bones": dict(legs(W, 22), **{
        "root": {"position": keys([(0, [0, 0, 0]), (W / 4, [0, 0.8, 0]), (W / 2, [0, 0, 0]), (3 * W / 4, [0, 0.8, 0]), (W, [0, 0, 0])])},
        "arm_r": {"rotation": wave(W, [-24, 0, 6])}, "arm_l": {"rotation": wave(W, [24, 0, -6])},
        "head": {"rotation": wave(W, [0, 0, 8], 0.0, n=8)}})}
    R = 0.55                                                               # sprinting, arms thrown back and forth
    A["run"] = {"loop": True, "animation_length": R, "bones": dict(legs(R, 44), **{
        "torso": {"rotation": keys([(0, [16, 0, 0]), (R, [16, 0, 0])])},
        "arm_r": {"rotation": wave(R, [-70, 0, 20], 0, n=8)}, "arm_l": {"rotation": wave(R, [70, 0, -20], 0, n=8)},
        "head": {"rotation": wave(R, [0, 0, 14], 1.0, n=8, base=(-14, 0, 0))}})}
    V = 1.6                                                                # a wave: come here
    A["wave"] = {"loop": False, "animation_length": V, "bones": {
        "arm_r": {"rotation": ramp([(0, [0, 0, 0]), (0.25, [0, 0, 150]), (1.35, [0, 0, 150]), (V, [0, 0, 0])])},
        "forearm_r": {"rotation": keys([(0, [0, 0, 0])] + [(0.3 + k * 0.2, [0, 0, 30 if k % 2 else -30]) for k in range(6)] + [(V, [0, 0, 0])])},
        "head": {"rotation": ramp([(0, [0, 0, 0]), (0.3, [0, 0, 22]), (1.3, [0, 0, 22]), (V, [0, 0, 0])])}}}
    T = 0.5                                                                # both arms swing down
    A["attack"] = {"loop": False, "animation_length": T, "bones": {
        "arm_r": {"rotation": ramp([(0, [0, 0, 0]), (0.15, [-150, 0, 10]), (0.3, [-20, 0, 0]), (T, [0, 0, 0])])},
        "arm_l": {"rotation": ramp([(0, [0, 0, 0]), (0.15, [-150, 0, -10]), (0.3, [-20, 0, 0]), (T, [0, 0, 0])])}}}
    g.save(A)
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/office_rooms/textures/entity/partygoer_glowmask.png')
    Image.new('RGBA', (g.W, g.H), (0, 0, 0, 0)).save(p)
    return g


if __name__ == '__main__':
    g = partygoer()
    print('partygoer bones %d cubes %d' % (len(g.bones), sum(len(b['cubes']) for b in g.bones)))
