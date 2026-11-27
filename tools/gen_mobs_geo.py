"""Office Rooms 0.11.0: GeckoLib models, animations and textures for the redesigned creatures
(Gaunt, Headless Warden, Hound, Smiler, Colony, Hunter, Observer). The Wolfspider stays as it is.
Run: python3 tools/gen_mobs_geo.py"""
import math
from geo_lib import Geo, flesh, chitin, bone_mat, void, solid, keys, wave, snaps, ramp

SIDES = (('r', -1), ('l', 1))     # 'r' = model -x (the creature's right), 'l' = +x


def eye(g, x, y, col, glow=True, size=1):
    for a in range(size):
        for b in range(size):
            g.px(x + a, y + b, col, glow=glow)


# =================================================================================================
# THE GAUNT: an arthropod centaur. Upright emaciated torso with floor-length arms on a long,
# segmented lower body that walks on six spider-like legs.
# =================================================================================================
def gaunt():
    g = Geo('gaunt', seed=1010)
    SK = flesh((40, 34, 38), vein=(92, 18, 24))
    CH = chitin((30, 22, 22), hi=(96, 70, 62))
    BN = bone_mat((168, 150, 118))

    def skull_face(g, r):
        u, v, w, h = r
        for j in range(h):
            for i in range(w):
                t = 74 - 4 * j + g.rng.randint(-5, 5)
                g.px(u + i, v + j, (t, t - 8, t - 6))
        for i in range(w):
            g.px(u + i, v, (14, 10, 12)); g.px(u + i, v + 1, (24, 18, 20))
        for cx, low in ((2, 0), (w - 4, 1)):                                    # uneven sockets
            for a in range(2):
                for b in range(3 + low):
                    g.px(u + cx + a, v + 3 + b, (3, 1, 2))
            eye(g, u + cx + (low), v + 4 + low, (255, 50, 30))
        for j in (5, 6):
            g.px(u + w // 2 - 1 + (j % 2), v + j, (12, 6, 8))                  # collapsed nose
        for i in range(w):                                                      # lipless mouth
            g.px(u + i, v + h - 4, (90, 16, 22))
            g.px(u + i, v + h - 3, (222, 208, 166) if i % 2 == 0 else (120, 104, 80))
            g.px(u + i, v + h - 2, (210, 196, 150) if i % 3 else (60, 14, 18))

    def ribs(g, r):
        u, v, w, h = r
        for j in range(2, h, 3):
            for i in range(w):
                if abs(i - w // 2) > 1:
                    g.px(u + i, v + j, (78, 70, 70))
        for j in range(h):
            g.px(u + w // 2, v + j, (12, 8, 10))
        eye(g, u + w // 2, v + h // 2, (255, 40, 24), size=1)

    def spiracles(g, r):
        u, v, w, h = r
        for i in range(2, w - 1, 4):
            eye(g, u + i, v + h // 2, (200, 36, 24))

    g.bone('root', None, [0, 0, 0])
    # lower body
    g.bone('thorax', 'root', [0, 11, -2])
    g.cube('thorax', [-3, 8.5, -5.5], [6, 5.5, 7], SK, face=ribs)
    g.cube('thorax', [-3.4, 13.2, -5], [6.8, 1.2, 6], CH)
    segs = [(2, 5.6, 5.0, 5, 6), (7, 5.0, 4.6, 5, 7), (12, 4.2, 4.0, 5, 8), (17, 3.2, 3.4, 4, 9)]
    parent = 'thorax'
    for k, (z, w, h, d, rx) in enumerate(segs):
        name = 'seg%d' % (k + 1)
        y0 = 8.6 - k * 0.3
        g.bone(name, parent, [0, y0 + h / 2, z], [rx if k else 4, 0, 0])
        g.cube(name, [-w / 2, y0, z], [w, h, d], SK, face=None, top=None)
        g.cube(name, [-w / 2 - 0.3, y0 + h - 0.2, z + 0.3], [w + 0.6, 1.0, d - 0.6], CH, top=spiracles)
        g.cube(name, [-0.5, y0 + h + 0.6, z + d / 2 - 0.5], [1, 1.6, 1], BN, rot=[-30, 0, 0], pivot=[0, y0 + h + 0.6, z + d / 2])
        parent = name
    g.bone('stinger', parent, [0, 8.2, 21], [-35, 0, 0])
    g.cube('stinger', [-0.6, 7.6, 21], [1.2, 1.2, 4], CH)
    g.cube('stinger', [-0.3, 7.9, 25], [0.6, 0.6, 2.5], BN)
    # six legs: thorax (front), seg1 (middle), seg2 (rear)
    for side, s in SIDES:
        for n, (par, z, yaw) in enumerate((('thorax', -3.5, -18), ('seg1', 4.5, 0), ('seg2', 9.5, 18))):
            leg = 'leg_%s%d' % (side, n + 1)
            x0 = s * 2.8
            g.bone(leg, par, [x0, 11, z], [0, yaw * s, 0])
            g.cube(leg, [x0 if s > 0 else x0 - 3.2, 10.6, z - 0.4], [3.2, 0.8, 0.8], CH)          # 0.26.2: thinner
            fem, tib = leg + '_femur', leg + '_tibia'
            g.bone(fem, leg, [s * 6, 11, z], [0, 0, s * 38])
            g.cube(fem, [s * 6 - 0.3, 11, z - 0.3], [0.6, 9, 0.6], CH)
            g.cube(fem, [s * 6 - 0.4, 19.4, z - 0.4], [0.8, 0.8, 0.8], CH)                    # knee knob
            g.bone(tib, fem, [s * 6, 20, z], [0, 0, -s * 51])
            g.cube(tib, [s * 6 - 0.22, 1.2, z - 0.22], [0.45, 18.8, 0.45], CH)
            g.cube(tib, [s * 6 - 0.15, -0.4, z - 0.15], [0.3, 1.8, 0.3], BN)
    # upright torso
    g.bone('waist', 'thorax', [0, 13.5, -3.5], [12, 0, 0])
    g.cube('waist', [-2, 13.5, -5], [4, 6, 3], SK, face=ribs)
    g.bone('chest', 'waist', [0, 19.5, -3.5], [10, 0, 0])
    g.cube('chest', [-4.5, 19.5, -6.5], [9, 7, 6], SK, face=ribs)
    for k in range(3):
        g.cube('chest', [-0.5, 21 + k * 2.2, -0.6], [1, 2, 2.2], BN, rot=[-35, 0, 0], pivot=[0, 21 + k * 2.2, -0.5])
    g.bone('neck', 'chest', [0, 26.5, -4], [22, 0, 0])
    g.cube('neck', [-1, 26, -5], [2, 5, 2], SK)
    g.bone('head', 'neck', [0, 31, -4])
    g.bone('skull', 'head', [0, 31, -4], [-38, 0, 0])
    g.cube('skull', [-3, 30, -10.5], [6, 6, 7.5], SK, face=skull_face)
    g.cube('skull', [-0.6, 35.8, -9], [1.2, 0.8, 6], BN)
    g.bone('jaw', 'skull', [0, 30, -4.5], [16, 0, 0])
    g.cube('jaw', [-2.6, 28.2, -10.2], [5.2, 1.8, 6], SK, face=lambda g, r: [g.px(r[0] + i, r[1], (220, 204, 160) if i % 2 else (110, 94, 70)) for i in range(r[2])])
    for side, s in SIDES:
        a, f, c, nub = 'arm_' + side, 'forearm_' + side, 'claws_' + side, 'nub_' + side
        g.bone(a, 'chest', [s * 5.4, 25.5, -3.5], [-14, 0, -s * 5])
        g.cube(a, [s * 5.4 - 0.6, 15.5, -4.1], [1.2, 10, 1.2], SK)
        g.bone(f, a, [s * 5.4, 15.5, -3.5], [-26, 0, 0])
        g.cube(f, [s * 5.4 - 0.55, 3.5, -4.05], [1.1, 12, 1.1], SK)
        g.bone(c, f, [s * 5.4, 3.5, -3.5], [-14, 0, 0])
        for k in range(3):
            g.cube(c, [s * 5.4 - 0.9 + k * 0.7, -4.2, -3.95], [0.35, 7.7, 0.35], BN, rot=[(k - 1) * 9, 0, 0], pivot=[s * 5.4, 3.5, -3.8])
        g.bone(nub, 'chest', [s * 2.6, 22.5, -6.3], [-40, 0, -s * 14])
        g.cube(nub, [s * 2.6 - 0.45, 18.8, -6.8], [0.9, 3.7, 0.9], SK)
        g.cube(nub, [s * 2.6 - 0.3, 17.6, -6.7], [0.6, 1.2, 0.6], BN)

    A = {}
    # idle: breathing, abdomen pulse, skull snaps, claws curl, one leg at a time taps the floor
    L = 6.0
    idle = {"chest": {"rotation": wave(L, 2.0)}, "waist": {"rotation": wave(L, -1.2)},
            "skull": {"rotation": snaps(L, [(1.9, 0.9, [6, 38, 26]), (4.4, 0.25, [-16, -32, -36])])},
            "jaw": {"rotation": snaps(L, [(2.85, 0.4, [24, 0, 0])])},
            "claws_r": {"rotation": wave(L, 14, 0.5)}, "claws_l": {"rotation": wave(L, 14, 2.1)},
            "nub_r": {"rotation": snaps(L, [(0.5, 0.05, [30, 0, 0]), (3.7, 0.08, [40, 0, 10])])},
            "nub_l": {"rotation": snaps(L, [(1.2, 0.05, [35, 0, 0]), (5.1, 0.08, [30, 0, -10])])},
            "seg1": {"rotation": wave(L, [1.5, 2, 0])}, "seg2": {"rotation": wave(L, [2, 3, 0], 0.8)},
            "seg3": {"rotation": wave(L, [2.5, 4, 0], 1.6)}, "seg4": {"rotation": wave(L, [3, 5, 0], 2.4)},
            "stinger": {"rotation": wave(L, [6, 8, 0], 3.0)}}
    for k, (leg, t0) in enumerate((('leg_r1', 0.6), ('leg_l3', 2.0), ('leg_l1', 3.3), ('leg_r2', 4.6))):
        s = -1 if leg[4] == 'r' else 1
        idle[leg + '_femur'] = {"rotation": snaps(L, [(t0, 0.3, [0, 0, s * 14])], dur=0.12)}
    A["idle"] = {"loop": True, "animation_length": L, "bones": idle}

    def gait(L, lift, swing, lunge):
        bones = {}
        group_a = ('leg_r1', 'leg_l2', 'leg_r3')
        for side, s in SIDES:
            for n in (1, 2, 3):
                leg = 'leg_%s%d' % (side, n)
                ph = 0 if leg in group_a else math.pi
                bones[leg] = {"rotation": wave(L, swing, ph, axis=1)}
                bones[leg + '_femur'] = {"rotation": keys([(L * i / 12, [0, 0, s * lift * max(0, math.sin(2 * math.pi * i / 12 + ph))]) for i in range(13)])}
        for k in range(1, 5):
            bones['seg%d' % k] = {"rotation": wave(L, 3 + k, k * 0.6, axis=1)}
        bones['thorax'] = {"position": wave(L, [0, 0.35, 0], 0, n=12)}
        bones['waist'] = {"rotation": keys([(0, [lunge, 0, 0]), (L, [lunge, 0, 0])])}
        return bones

    w = gait(1.2, 16, 16, 0)
    w.update({"arm_r": {"rotation": wave(1.2, -12, 0.6)}, "arm_l": {"rotation": wave(1.2, 12, 0.6)},
              "skull": {"rotation": snaps(1.2, [(0.6, 0.5, [0, 0, 14])], rest=(0, 0, -10))}})
    A["walk"] = {"loop": True, "animation_length": 1.2, "bones": w}
    r = gait(0.55, 24, 24, 24)
    r.update({"neck": {"rotation": keys([(0, [-24, 0, 0]), (0.55, [-24, 0, 0])])},
              "skull": {"rotation": keys([(0, [-14, 0, -16]), (0.14, [-14, 0, 18]), (0.28, [-14, 0, -12]), (0.41, [-14, 0, 20]), (0.55, [-14, 0, -16])])},
              "jaw": {"rotation": keys([(0, [30, 0, 0]), (0.55, [30, 0, 0])])},
              "arm_r": {"rotation": wave(0.55, [30, 0, 0], 0, base=(-60, 0, 0))}, "arm_l": {"rotation": wave(0.55, [-30, 0, 0], 0, base=(-60, 0, 0))},
              "forearm_r": {"rotation": keys([(0, [-20, 0, 0]), (0.55, [-20, 0, 0])])}, "forearm_l": {"rotation": keys([(0, [-20, 0, 0]), (0.55, [-20, 0, 0])])}})
    A["run"] = {"loop": True, "animation_length": 0.55, "bones": r}
    L = 1.8
    shake = keys([(i * 0.05, [0, (3 if i % 2 else -3) if 6 < i < 30 else 0, 0]) for i in range(int(L / 0.05) + 1)])
    up = lambda v: ramp([(0, [0, 0, 0]), (0.3, v), (1.5, v), (L, [0, 0, 0])])
    A["scream"] = {"loop": False, "animation_length": L, "bones": {
        "thorax": {"rotation": up([-16, 0, 0])}, "waist": {"rotation": up([-20, 0, 0])}, "chest": {"rotation": up([-20, 0, 0])},
        "skull": {"rotation": up([-34, 0, 0])}, "jaw": {"rotation": up([60, 0, 0])},
        "arm_r": {"rotation": up([-20, 0, -62])}, "arm_l": {"rotation": up([-20, 0, 62])},
        "claws_r": {"rotation": up([40, 0, 0])}, "claws_l": {"rotation": up([40, 0, 0])},
        "leg_r1_femur": {"rotation": up([0, 0, -20])}, "leg_l1_femur": {"rotation": up([0, 0, 20])}, "root": {"rotation": shake}}}
    return g.save(A)


# =================================================================================================
# THE HEADLESS WARDEN: hunched sculk giant. Where the head should be, a torn stump ringed with
# bone shards glows from inside. A heart pulses in an open rib cage; tendrils listen on its back.
# =================================================================================================
def warden():
    g = Geo('headless_warden', seed=2020)
    SK = flesh((22, 40, 46), vein=(18, 90, 104), bruise=(12, 22, 30), wet=0.02)
    DK = flesh((14, 22, 26), vein=(20, 110, 120), bruise=(8, 12, 16))
    BN = bone_mat((170, 176, 160))
    GL = solid((60, 230, 235), glow=True)

    def rib_front(g, r):
        u, v, w, h = r
        for j in range(h):
            for i in range(w):
                inner = 2 < i < w - 3 and 3 < j < h - 3
                if inner:
                    g.px(u + i, v + j, (6, 10, 12))
        for j in range(4, h - 3, 3):
            for i in range(2, w - 2):
                if abs(i - w // 2) > 1:
                    g.px(u + i, v + j, (150, 156, 140))
        for k in range(10):
            eye(g, u + 3 + g.rng.randrange(max(1, w - 6)), v + 4 + g.rng.randrange(max(1, h - 8)), (40, 220, 230))

    def stump_top(g, r):
        u, v, w, h = r
        for j in range(h):
            for i in range(w):
                d = math.hypot(i - w / 2 + 0.5, j - h / 2 + 0.5)
                if d < w / 2 - 1:
                    c = (70, 240, 240) if d < w / 5 else (40, 160, 170) if d < w / 3 else (90, 20, 26)
                    g.px(u + i, v + j, c, glow=d < w / 3)
                    g.px(u + i, v + j, c)

    g.bone('root', None, [0, 0, 0])
    g.bone('hips', 'root', [0, 13, 0])
    g.cube('hips', [-5, 11, -3], [10, 4, 6], SK)
    for side, s in SIDES:
        t, sh = 'thigh_' + side, 'shin_' + side
        g.bone(t, 'hips', [s * 3.5, 12, 0], [-8, 0, 0])
        g.cube(t, [s * 3.5 - 2, 6, -2], [4, 7, 4], SK)
        g.bone(sh, t, [s * 3.5, 6, 0], [14, 0, 0])
        g.cube(sh, [s * 3.5 - 2.2, 0.5, -2.4], [4.4, 6, 4.4], DK)
        g.cube(sh, [s * 3.5 - 2.4, 0, -4.4], [4.8, 1.2, 3], DK)
    g.bone('torso', 'hips', [0, 15, 0], [22, 0, 0])
    g.cube('torso', [-7, 15, -4.5], [14, 13, 9], SK, face=rib_front)
    g.cube('torso', [-7.5, 22, -1], [15, 6, 6.5], DK)                       # hunched back
    for k in range(5):
        g.cube('torso', [-0.6 + (k % 2) * 0.4, 18 + k * 2.2, 3.5], [1.2, 2.2, 2], BN, rot=[-40, 0, 0], pivot=[0, 18 + k * 2.2, 4])
    g.bone('heart', 'torso', [0, 21.5, -2.5])
    g.cube('heart', [-1.6, 20, -5.6], [3.2, 3.2, 3], GL)
    g.bone('stump', 'torso', [0, 28, -0.5])
    g.cube('stump', [-3, 27.5, -3.5], [6, 2.2, 6], SK, top=stump_top)
    g.bone('collar', 'stump', [0, 29.5, -0.5])
    for k in range(8):
        a = k * math.pi / 4
        x, z = 2.6 * math.cos(a), -0.5 + 2.6 * math.sin(a)
        rx, rz = round(-math.sin(a) * 25, 1), round(math.cos(a) * 25, 1)
        g.cube('collar', [x - 0.4, 29.2, z - 0.4], [0.8, 2.6 + (k % 3) * 0.8, 0.8], BN, rot=[rx, 0, rz], pivot=[x, 29.2, z])
    for side, s in SIDES:
        a, f, h = 'arm_' + side, 'forearm_' + side, 'hand_' + side
        g.bone(a, 'torso', [s * 7.5, 26, 0], [-18, 0, -s * 6])
        g.cube(a, [s * 7.5 - 2, 16, -2], [4, 10, 4], SK)
        g.bone(f, a, [s * 7.8, 16, 0], [-14, 0, 0])
        g.cube(f, [s * 7.8 - 2.5, 4, -2.5], [5, 12, 5], DK)
        g.bone(h, f, [s * 7.8, 4, -0.5])
        for k in range(3):
            g.cube(h, [s * 7.8 - 2 + k * 1.5, -2.5, -1.8], [1, 6.5, 1], BN, rot=[-10 + k * 6, 0, 0], pivot=[s * 7.8, 4, -1])
        tp = 'tendril_' + side
        g.bone(tp, 'torso', [s * 4, 27.5, 3], [-30, 0, s * 18])
        prev = tp
        for k in range(3):
            n = '%s%d' % (tp, k + 1)
            g.bone(n, prev, [s * 4, 27.5 + k * 5, 3], [-12, 0, s * 6] if k else None)
            g.cube(n, [s * 4 - 0.75 + k * 0.15, 27.5 + k * 5, 2.25], [1.5 - k * 0.3, 5, 1.5 - k * 0.3], DK)
            prev = n
        g.cube(prev, [s * 4 - 0.6, 42.3, 2.4], [1.2, 1.2, 1.2], GL)

    A = {}
    L = 4.0
    beat = ramp([(0, [1, 1, 1]), (0.12, [1.35, 1.35, 1.35]), (0.24, [1, 1, 1]), (0.4, [1.25, 1.25, 1.25]), (0.55, [1, 1, 1]),
                 (2.0, [1, 1, 1]), (2.12, [1.35, 1.35, 1.35]), (2.24, [1, 1, 1]), (2.4, [1.25, 1.25, 1.25]), (2.55, [1, 1, 1]), (L, [1, 1, 1])])
    tend = lambda ph: {"rotation": wave(L, [10, 0, 8], ph)}
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "torso": {"rotation": snaps(L, [(2.8, 0.6, [0, 22, 0])], dur=0.08)}, "heart": {"scale": beat},
        "collar": {"scale": wave(L, [0.08, 0, 0.08], 0, base=(1, 1, 1))},
        "tendril_r1": tend(0), "tendril_r2": tend(0.8), "tendril_r3": tend(1.6),
        "tendril_l1": tend(1.2), "tendril_l2": tend(2.0), "tendril_l3": tend(2.8),
        "arm_r": {"rotation": wave(L, 3)}, "arm_l": {"rotation": wave(L, -3, 1)}}}
    def walk(L, sw, extra=0):
        return {"thigh_r": {"rotation": wave(L, sw)}, "thigh_l": {"rotation": wave(L, -sw)},
                "shin_r": {"rotation": wave(L, sw * 0.6, 1.2)}, "shin_l": {"rotation": wave(L, -sw * 0.6, 1.2)},
                "arm_r": {"rotation": wave(L, -sw * 0.7, 0, base=(-extra, 0, 0))}, "arm_l": {"rotation": wave(L, sw * 0.7, 0, base=(-extra, 0, 0))},
                "torso": {"rotation": wave(L, [0, 0, 5], 0, base=(extra / 2, 0, 0))}, "hips": {"position": wave(L, [0, 0.6, 0], 0, n=12)},
                "heart": {"scale": beat}}
    A["walk"] = {"loop": True, "animation_length": 2.0, "bones": walk(2.0, 18)}
    A["run"] = {"loop": True, "animation_length": 1.0, "bones": walk(1.0, 28, 20)}
    L = 0.9
    A["attack"] = {"loop": False, "animation_length": L, "bones": {
        "arm_r": {"rotation": ramp([(0, [0, 0, 0]), (0.35, [-150, 0, 10]), (0.5, [-20, 0, 0]), (L, [0, 0, 0])])},
        "arm_l": {"rotation": ramp([(0, [0, 0, 0]), (0.35, [-150, 0, -10]), (0.5, [-20, 0, 0]), (L, [0, 0, 0])])},
        "torso": {"rotation": ramp([(0, [0, 0, 0]), (0.35, [-14, 0, 0]), (0.5, [18, 0, 0]), (L, [0, 0, 0])])}}}
    L = 2.0
    shake = keys([(i * 0.05, [(2 if i % 2 else -2) if 5 < i < 34 else 0, 0, 0]) for i in range(int(L / 0.05) + 1)])
    up = lambda v: ramp([(0, [0, 0, 0]), (0.3, v), (1.6, v), (L, [0, 0, 0])])
    A["scream"] = {"loop": False, "animation_length": L, "bones": {
        "torso": {"rotation": up([-30, 0, 0])}, "arm_r": {"rotation": up([-40, 0, -70])}, "arm_l": {"rotation": up([-40, 0, 70])},
        "collar": {"scale": up([1.6, 1.3, 1.6])}, "heart": {"scale": up([1.5, 1.5, 1.5])},
        "tendril_r1": {"rotation": up([-30, 0, -25])}, "tendril_l1": {"rotation": up([-30, 0, 25])}, "root": {"rotation": shake}}}
    return g.save(A)


# =================================================================================================
# THE HOUND: a hairless, starved dog body that carries a pale human head.
# =================================================================================================
def hound():
    g = Geo('hound', seed=3030)
    SK = flesh((122, 108, 104), vein=(110, 50, 60), bruise=(86, 64, 80), wet=0.02)
    FACE = flesh((150, 132, 122), vein=(130, 70, 70), bruise=(110, 86, 90))
    HAIR = void((18, 14, 12), 5)
    BN = bone_mat((190, 176, 150))

    def ribs(g, r):
        u, v, w, h = r
        for i in range(2, w - 2, 3):
            for j in range(1, h - 1):
                g.px(u + i, v + j, (92, 80, 78))

    def human_face(g, r):
        u, v, w, h = r
        for i in range(w):
            for j in range(2):
                g.px(u + i, v + j, (24, 18, 16))                                 # hairline
        for cx in (2, w - 5):
            for a in range(3):
                for b in range(2):
                    g.px(u + cx + a, v + 4 + b, (40, 26, 26))                      # sunken eye sockets
            g.px(u + cx + 1, v + 4, (232, 226, 214)); g.px(u + cx + 1, v + 5, (232, 226, 214))
            eye(g, u + cx + 1, v + 5, (255, 250, 230))                               # the pupils catch the light
            g.px(u + cx + 2, v + 4, (210, 204, 190))
        for j in (6, 7, 8):
            g.px(u + w // 2 - 1, v + j, (120, 96, 88)); g.px(u + w // 2, v + j, (104, 84, 78))
        for i in range(2, w - 2):                                                    # a wide, wrong smile
            g.px(u + i, v + h - 4 + (1 if i in (2, w - 3) else 0), (70, 24, 26))
            g.px(u + i, v + h - 3, (226, 216, 190) if i % 2 else (196, 186, 160))

    g.bone('root', None, [0, 0, 0])
    g.bone('body', 'root', [0, 10, 0])
    # 0.27.1: starved, the spine pushing through the skin; a neck of bare vertebrae
    g.cube('body', [-2, 8.6, -6], [4, 3.6, 12], SK, face=None)
    g.cube('body', [-2.3, 8.8, -6.2], [4.6, 3.2, 5.5], SK, face=ribs)
    g.cube('body', [-1.4, 7.9, 0.5], [2.8, 1.2, 4.5], SK)                       # sunken belly
    for k in range(12):
        h = 0.9 + (0.5 if k % 2 == 0 else 0)
        g.cube('body', [-0.45, 12.1, -5.8 + k * 1.0], [0.9, h, 0.7], BN)          # vertebrae ridge
        if k % 3 == 1:
            g.cube('body', [-1.6, 12.0, -5.8 + k * 1.0], [3.2, 0.35, 0.35], BN)   # transverse spurs
    for k in range(3):                                                           # torn skin hanging from the belly
        g.cube('body', [-1.2 + k * 1.1, 6.2 - k * 0.5, -2 + k * 2.5], [0.5, 2.6 + k * 0.6, 0.25], SK, rot=[6 * (k - 1), 0, 4 * (1 - k)], pivot=[0, 8.6, -2 + k * 2.5])
    g.bone('neck', 'body', [0, 12, -5.5], [-28, 0, 0])
    for k in range(5):                                                           # bare cervical vertebrae
        g.cube('neck', [-0.8, 11.6, -6.6 - k * 1.0], [1.6, 1.4, 0.75], BN)
    g.cube('neck', [-0.35, 11.9, -10.6], [0.7, 0.8, 5.2], SK)                    # cord between them
    g.bone('head', 'neck', [0, 13, -10])
    g.bone('face', 'head', [0, 13, -10], [28, 0, 0])
    g.cube('face', [-3, 10.5, -16], [6, 7.5, 6], FACE, face=human_face)
    g.cube('face', [-3.2, 15.5, -14], [6.4, 2.5, 4.6], HAIR)
    for k in range(4):
        g.cube('face', [-2.5 + k * 1.6, 14 - (k % 2), -10.4], [0.6, 4 + (k % 2) * 1.5, 0.6], HAIR, rot=[30, 0, 0], pivot=[0, 16, -10.5])
    for k in range(6):                                                           # 0.27.1: flesh hanging off the face
        x0 = -2.6 + k * 1.0
        ln = 2.5 + (k * 7 % 5) * 0.9
        g.cube('face', [x0, 10.6 - ln, -15.2 + (k % 3) * 1.4], [0.55, ln, 0.3], SK if k % 2 else FACE,
               rot=[8 + (k % 3) * 6, 0, (k - 2.5) * 5], pivot=[x0 + 0.27, 10.6, -15 + (k % 3) * 1.4])
    g.bone('jaw', 'face', [0, 11, -11])
    g.cube('jaw', [-2.4, 9.5, -15.6], [4.8, 1.4, 5], FACE)
    for k in range(3):                                                           # strips dangling from the jaw
        g.cube('jaw', [-1.8 + k * 1.5, 9.5 - (3 + k), -15 + k], [0.45, 3 + k, 0.25], SK, rot=[10, 0, (k - 1) * 8], pivot=[-1.6 + k * 1.5, 9.5, -14.8 + k])
    for side, s in SIDES:
        for end, z in (('f', -4.5), ('b', 4.5)):
            u_, l_ = 'leg_%s%s' % (end, side), 'paw_%s%s' % (end, side)
            g.bone(u_, 'body', [s * 2.2, 9, z], [8 if end == 'f' else -16, 0, 0])
            g.cube(u_, [s * 2.2 - 0.5, 4, z - 0.5], [1.0, 5, 1.0], SK)                 # 0.27.1: thinner
            g.cube(u_, [s * 2.2 - 0.55, 3.6, z - 0.55], [1.1, 1.1, 1.1], BN)        # knee
            g.bone(l_, u_, [s * 2.2, 4, z], [-6 if end == 'f' else 26, 0, 0])
            g.cube(l_, [s * 2.2 - 0.35, 0.3, z - 0.35], [0.7, 3.8, 0.7], SK)
            g.cube(l_, [s * 2.2 - 0.7, 0, z - 1.8], [1.4, 0.5, 2.0], SK)
            for k in range(3):
                g.cube(l_, [s * 2.2 - 0.8 + k * 0.6, 0, z - 2.4], [0.3, 0.4, 0.8], BN)
    g.bone('tail', 'body', [0, 12, 6], [55, 0, 0])
    for k in range(7):                                                           # 0.27.1: a tail of bare vertebrae
        g.cube('tail', [-0.4 + k * 0.03, 12 + k * 1.1, 5.6], [0.8 - k * 0.07, 0.8, 0.8 - k * 0.07], BN)

    A = {}
    L = 5.0
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "body": {"rotation": wave(L, 1.0)}, "jaw": {"rotation": wave(L, 6, 0, base=(4, 0, 0))},
        "face": {"rotation": snaps(L, [(1.2, 1.1, [0, 10, 42]), (3.6, 0.5, [0, -16, -28])], dur=0.06)},
        "tail": {"rotation": snaps(L, [(0.7, 0.05, [0, 0, 20]), (2.9, 0.05, [0, 0, -24])])}}}
    def gait(L, a):
        return {"leg_fr": {"rotation": wave(L, a)}, "leg_bl": {"rotation": wave(L, a)},
                "leg_fl": {"rotation": wave(L, -a)}, "leg_br": {"rotation": wave(L, -a)},
                "paw_fr": {"rotation": wave(L, a * 0.5, 1.4)}, "paw_bl": {"rotation": wave(L, a * 0.5, 1.4)},
                "paw_fl": {"rotation": wave(L, -a * 0.5, 1.4)}, "paw_br": {"rotation": wave(L, -a * 0.5, 1.4)},
                "body": {"position": wave(L, [0, 0.4, 0], 0, n=12)}, "neck": {"rotation": wave(L, 4, 0.5)},
                "face": {"rotation": snaps(L, [(L * 0.4, L * 0.3, [0, 0, 18])])}, "tail": {"rotation": wave(L, [0, 0, 18])}}
    A["walk"] = {"loop": True, "animation_length": 0.9, "bones": gait(0.9, 24)}
    r = gait(0.45, 40)
    r["neck"] = {"rotation": keys([(0, [18, 0, 0]), (0.45, [18, 0, 0])])}
    r["jaw"] = {"rotation": keys([(0, [24, 0, 0]), (0.45, [24, 0, 0])])}
    A["run"] = {"loop": True, "animation_length": 0.45, "bones": r}
    A["bark"] = {"loop": False, "animation_length": 0.6, "bones": {
        "neck": {"rotation": ramp([(0, [0, 0, 0]), (0.1, [22, 0, 0]), (0.45, [22, 0, 0]), (0.6, [0, 0, 0])])},
        "jaw": {"rotation": ramp([(0, [0, 0, 0]), (0.1, [40, 0, 0]), (0.2, [10, 0, 0]), (0.3, [40, 0, 0]), (0.6, [0, 0, 0])])}}}
    A["attack"] = {"loop": False, "animation_length": 0.5, "bones": {
        "body": {"position": ramp([(0, [0, 0, 0]), (0.15, [0, 0.5, -3]), (0.5, [0, 0, 0])])},
        "jaw": {"rotation": ramp([(0, [0, 0, 0]), (0.12, [45, 0, 0]), (0.25, [0, 0, 0]), (0.5, [0, 0, 0])])}}}
    return g.save(A)


# =================================================================================================
# THE SMILER: no body at all. A face wrapped in darkness, floating at head height: two white eyes
# and a grin far too wide, both glowing.
# =================================================================================================
def smiler():
    g = Geo('smiler', seed=4040)
    CORE = void((5, 4, 7), 4)
    _haze = void((6, 5, 8), 5, edge_alpha=40)

    def SHROUD(g, r, face, alpha):          # dark haze around the face, open at the front so the face stays visible
        _haze(g, r, face, 0 if face == 'north' else alpha)

    def grin_face(g, r):
        u, v, w, h = r
        white = (255, 255, 248)
        for j in range(h):
            for i in range(w):
                g.px(u + i, v + j, (3, 2, 4, 255))
        for cx in (2, w - 6):                                                     # wide, staring eyes
            for a in range(4):
                for b in range(3):
                    if (a, b) in ((0, 0), (3, 0), (0, 2), (3, 2)):
                        continue
                    g.px(u + cx + a, v + 3 + b, white); eye(g, u + cx + a, v + 3 + b, white)
        mid = (w - 1) / 2
        for i in range(1, w - 1):                                                 # the grin, curving up at the ends
            d = abs(i - mid) / mid
            y0 = v + int(round(h - 5 - 5 * d * d))
            rows = 3 if d < 0.75 else 2 if d < 0.95 else 1
            for b in range(rows):
                g.px(u + i, y0 + b, white); eye(g, u + i, y0 + b, white)
            if d < 0.75 and i % 2:                                                  # gaps between the teeth
                g.px(u + i, y0 + 1, (3, 2, 4)); g.px(u + i, y0 + 1, (0, 0, 0, 0), glow=True)

    g.bone('root', None, [0, 0, 0])
    g.bone('float', 'root', [0, 26, 0])
    g.bone('face', 'float', [0, 26, 0])   # not 'head': the whole face turns with the body
    g.cube('face', [-4, 22, -3.5], [8, 8, 7], CORE, face=grin_face)
    # soft black haze around it: wider panels that start behind the face, so they frame it from the front
    g.cube('face', [-5, 21, -2.6], [10, 10, 6], SHROUD, alpha=170)
    g.bone('haze', 'face', [0, 26, 0])
    for k, (x, y, w, h) in enumerate(((-6, 23, 1.4, 5), (4.6, 22, 1.6, 6), (-3, 31, 5, 1.2), (-2, 19.6, 3, 1.4))):
        g.cube('haze', [x, y, -1.8 + k * 0.2], [w, h, 3], CORE)                  # ragged dark edges

    A = {}
    L = 4.0
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "float": {"position": wave(L, [0, 0.8, 0], 0, n=12)},
        "face": {"rotation": ramp([(0, [0, 0, 0]), (1.4, [0, 0, 22]), (2.4, [0, 0, 22]), (2.46, [0, 0, -6]), (L, [0, 0, 0])]),
                 "scale": wave(L, [0.05, 0.02, 0], 0, base=(1, 1, 1))},
        "haze": {"scale": wave(L, [0.08, 0.08, 0.08], 1.0, base=(1, 1, 1))}}}
    def glide(L, lean):
        return {"float": {"position": wave(L, [0, 0.6, 0], 0, n=12)},
                "face": {"rotation": wave(L, [0, 0, 6], 0, base=(lean, 0, 0))},
                "haze": {"scale": wave(L, [0.1, 0.06, 0.1], 0.5, base=(1, 1, 1))}}
    A["walk"] = {"loop": True, "animation_length": 2.0, "bones": glide(2.0, 6)}
    A["run"] = {"loop": True, "animation_length": 1.0, "bones": glide(1.0, 14)}
    A["giggle"] = {"loop": False, "animation_length": 0.8, "bones": {
        "face": {"rotation": keys([(i * 0.05, [0, 0, 12 if i % 2 else -12]) for i in range(17)])}}}
    A["attack"] = {"loop": False, "animation_length": 0.6, "bones": {
        "float": {"position": ramp([(0, [0, 0, 0]), (0.15, [0, 0, -4]), (0.45, [0, 0, -4]), (0.6, [0, 0, 0])])},
        "face": {"scale": ramp([(0, [1, 1, 1]), (0.15, [1.35, 1.35, 1.1]), (0.45, [1.35, 1.35, 1.1]), (0.6, [1, 1, 1])])}}}
    return g.save(A)


# =================================================================================================
# THE COLONY: one creature made of many bodies. A fused mass of torsos crawling on a ring of
# borrowed arms and legs, with heads that look in every direction.
# =================================================================================================
def colony():
    g = Geo('colony', seed=5050)
    SK = flesh((134, 112, 106), vein=(116, 46, 54), bruise=(90, 62, 80), wet=0.025)
    PALE = flesh((150, 130, 122), vein=(120, 60, 64), bruise=(100, 76, 86))
    BN = bone_mat()

    def seam(g, r):
        u, v, w, h = r
        for i in range(0, w, 2):
            g.px(u + i, v + h // 2, (40, 14, 18)); g.px(u + i + 1, v + h // 2 - 1, (60, 30, 30))

    def face(open_mouth):
        def paint(g, r):
            u, v, w, h = r
            for cx in (1, w - 3):
                g.px(u + cx, v + 2, (10, 6, 6)); g.px(u + cx + 1, v + 2, (10, 6, 6)); g.px(u + cx, v + 3, (10, 6, 6))
                eye(g, u + cx + 1, v + 3, (255, 236, 170))
            mh = 3 if open_mouth else 1
            for j in range(mh):
                for i in range(w // 2 - 2, w // 2 + 2):
                    g.px(u + i, v + h - 3 - j, (34, 6, 8))
            if open_mouth:
                eye(g, u + w // 2 - 1, v + h - 4, (120, 30, 20))
        return paint

    g.bone('root', None, [0, 0, 0])
    g.bone('mass', 'root', [0, 8, 0])
    g.cube('mass', [-6, 4, -5], [12, 7, 10], SK, top=seam)
    g.cube('mass', [-4.5, 9, -3.5], [9, 5, 8], SK, rot=[0, 15, 8], pivot=[0, 9, 0], face=seam)
    g.cube('mass', [-5, 6, 1], [8, 5, 6], PALE, rot=[0, -20, -6], pivot=[0, 6, 3])
    g.cube('mass', [-3.5, 8.5, -6.5], [7, 4, 5], PALE, rot=[14, 0, 0], pivot=[0, 8.5, -4])
    for k in range(6):
        g.cube('mass', [-4 + k * 1.6, 13 + (k % 2), -2 + (k % 3)], [0.9, 1.5, 0.9], BN, rot=[(k - 3) * 10, 0, (k - 2) * 12], pivot=[0, 13, 0])
    for k in range(7):                       # 0.26.2: hair-thin filaments
        x0, z0 = -4.5 + k * 1.5, -3 + (k * 37 % 7)
        g.cube('mass', [x0 - 0.12, 12.5, z0 - 0.12], [0.25, 6 + (k * 5 % 5), 0.25], PALE, rot=[(k - 3) * 9, 0, (3 - k) * 7], pivot=[x0, 12.5, z0])
    heads = (('head_main', [0, 9, -6], [-2.5, 7.5, -11.5], None, True),
             ('head_l', [-5, 12, -2], [-8.5, 11.5, -4], [0, 0, 38], False),
             ('head_r', [4.5, 13, 2], [3, 13, 0], [-30, 0, -24], True),
             ('head_b', [0, 11, 5.5], [-2, 10, 5.5], [42, 180, 0], True))
    for name, pv, org, rot, mouth in heads:
        g.bone(name, 'mass', pv, rot)
        g.cube(name, org, [5 if name == 'head_main' else 4, 5 if name == 'head_main' else 4, 5 if name == 'head_main' else 4], PALE, face=face(mouth))
    g.bone('jaw_main', 'head_main', [0, 8, -7])
    g.cube('jaw_main', [-2.2, 6.6, -11.2], [4.4, 1.2, 4.4], PALE)
    ring = (-150, -110, -65, -25, 25, 65, 110, 150)
    for k, ang in enumerate(ring):
        a = math.radians(ang)
        x, z = 5 * math.sin(a), -5 * math.cos(a)
        up_, lo = 'limb%d' % k, 'limb%d_lo' % k
        g.bone(up_, 'mass', [x, 7, z], [0, -ang, 0])
        # 0.26.2: hair-thin stilt limbs with high knees
        g.bone(up_ + '_s', up_, [x, 7, z], [60, 0, 0])
        g.cube(up_ + '_s', [x - 0.3, 7, z - 0.3], [0.6, 9, 0.6], SK)
        g.cube(up_ + '_s', [x - 0.45, 15.4, z - 0.45], [0.9, 0.9, 0.9], PALE)     # knee knob
        g.bone(lo, up_ + '_s', [x, 16, z], [-80, 0, 0])
        g.cube(lo, [x - 0.22, 3.8, z - 0.22], [0.45, 12.2, 0.45], SK)
        if k % 2 == 0:                       # a hand: long thin fingers
            for f in range(4):
                g.cube(lo, [x - 0.6 + f * 0.4, 0.6, z - 0.15], [0.25, 3.4, 0.25], PALE, rot=[(f - 1.5) * 12, 0, 0], pivot=[x, 3.8, z])
        else:                                # a foot: three thin toes
            for f in range(3):
                g.cube(lo, [x - 0.15, 3.6, z - 2.6], [0.3, 0.3, 2.6], PALE, rot=[0, (f - 1) * 22, 0], pivot=[x, 3.8, z])

    A = {}
    L = 4.0
    idle = {"mass": {"scale": wave(L, [0.04, 0.05, 0.04], 0, base=(1, 1, 1))},
            "head_main": {"rotation": snaps(L, [(0.8, 0.9, [0, 28, 0]), (2.7, 0.4, [10, -30, 0])])},
            "head_l": {"rotation": snaps(L, [(1.5, 0.3, [0, 40, 0]), (3.3, 0.6, [-20, 0, 0])])},
            "head_r": {"rotation": snaps(L, [(0.4, 0.5, [0, -35, 0]), (2.2, 0.2, [25, 0, 0])])},
            "head_b": {"rotation": wave(L, [0, 20, 0], 1.0)}, "jaw_main": {"rotation": wave(L, 8, 0, base=(6, 0, 0))}}
    for k in range(8):
        idle['limb%d_s' % k] = {"rotation": snaps(L, [((k * 0.47) % L, 0.15, [74, 0, 0])], rest=(60, 0, 0), dur=0.06)}
    A["idle"] = {"loop": True, "animation_length": L, "bones": idle}
    def crawl(L, lift, swing):
        b = {"mass": {"position": wave(L, [0, 0.5, 0], 0, n=12)}, "head_main": {"rotation": wave(L, [0, 10, 0])}}
        for k in range(8):
            ph = (k % 2) * math.pi + k * 0.3
            b['limb%d' % k] = {"rotation": wave(L, [0, swing, 0], ph, base=(0, 0, 0))}
            # 0.26.2: stepped, jerky lift
            b['limb%d_s' % k] = {"rotation": keys([(L * i / 12, [60 + lift * round(max(0, math.sin(2 * math.pi * i / 12 + ph)) * 3) / 3, 0, 0]) for i in range(13)])}
        return b
    A["walk"] = {"loop": True, "animation_length": 1.2, "bones": crawl(1.2, 14, 14)}
    A["run"] = {"loop": True, "animation_length": 0.6, "bones": crawl(0.6, 20, 20)}
    L = 2.0
    shake = keys([(i * 0.05, [0, (4 if i % 2 else -4) if 4 < i < 34 else 0, 0]) for i in range(int(L / 0.05) + 1)])
    up = lambda v: ramp([(0, [0, 0, 0]), (0.25, v), (1.6, v), (L, [0, 0, 0])])
    A["scream"] = {"loop": False, "animation_length": L, "bones": {
        "mass": {"scale": ramp([(0, [1, 1, 1]), (0.25, [1.12, 1.15, 1.12]), (1.6, [1.12, 1.15, 1.12]), (L, [1, 1, 1])]), "rotation": shake},
        "head_main": {"rotation": up([-35, 0, 0])}, "jaw_main": {"rotation": up([55, 0, 0])},
        "head_l": {"rotation": up([-30, 0, 20])}, "head_r": {"rotation": up([-30, 0, -20])}, "head_b": {"rotation": up([-40, 0, 0])}}}
    return g.save(A)


# =================================================================================================
# THE HUNTER: 3.6 blocks of thin dark lines. A walking outline of a skeleton.
# =================================================================================================
def hunter():
    g = Geo('hunter', seed=6060)
    LN = void((16, 16, 22), 5)
    T = 0.7

    def tiny_eyes(g, r):
        u, v, w, h = r
        for j in range(h):
            for i in range(w):
                g.px(u + i, v + j, (220, 230, 255)); g.px(u + i, v + j, (200, 220, 255), glow=True)

    def box(bone, a, b):
        (x0, y0, z0), (x1, y1, z1) = a, b
        for x in (x0, x1):
            for z in (z0, z1):
                g.line(bone, (x, y0, z), (x, y1, z), T, LN)
        for y in (y0, y1):
            for z in (z0, z1):
                g.cube(bone, [x0, y - T / 2, z - T / 2], [x1 - x0, T, T], LN)
            for x in (x0, x1):
                g.cube(bone, [x - T / 2, y - T / 2, z0], [T, T, z1 - z0], LN)

    g.bone('root', None, [0, 0, 0])
    g.bone('pelvis', 'root', [0, 30, 0])
    g.cube('pelvis', [-3.5, 29.6, -T / 2], [7, T, T], LN)
    g.line('pelvis', (-3.5, 30, 0), (0, 27, 0), T, LN)
    g.line('pelvis', (3.5, 30, 0), (0, 27, 0), T, LN)
    for side, s in SIDES:
        t, sh, ft = 'thigh_' + side, 'shin_' + side, 'foot_' + side
        g.bone(t, 'pelvis', [s * 3, 30, 0], [-6, 0, 0])
        g.line(t, (s * 3, 30, 0), (s * 3, 15, 0), T, LN)
        g.bone(sh, t, [s * 3, 15, 0], [12, 0, 0])
        g.line(sh, (s * 3, 15, 0), (s * 3, 1, 0), T, LN)
        g.bone(ft, sh, [s * 3, 1, 0])
        g.line(ft, (s * 3, 0.4, 0), (s * 3, 0.4, -4), T, LN)
    g.bone('spine', 'pelvis', [0, 30, 0], [8, 0, 0])
    g.line('spine', (0, 30, 0.6), (0, 44, 0.6), T, LN)
    for k in range(4):
        y = 34 + k * 2.6
        w = 4 - abs(k - 1.5) * 0.6
        g.cube('spine', [-w, y, -2.4], [2 * w, T, T], LN)
        g.line('spine', (-w, y, -2.4), (-w, y, 0.6), T, LN)
        g.line('spine', (w, y, -2.4), (w, y, 0.6), T, LN)
    g.cube('spine', [-5, 43.6, -T / 2], [10, T, T], LN)
    g.bone('neck', 'spine', [0, 44, 0.3], [14, 0, 0])
    g.line('neck', (0, 44, 0.3), (0, 47.5, 0.3), T, LN)
    g.bone('head', 'neck', [0, 47.5, 0])
    box('head', (-2.6, 47.5, -3.2), (2.6, 56.5, 2.6))
    g.line('head', (-2.6, 50, -3.2), (0, 47.5, -3.6) if False else (-2.6, 48.2, -3.2), T, LN)
    for x in (-1.3, 1.3):
        g.cube('head', [x - 0.35, 53, -3.45], [0.7, 0.7, 0.4], LN, face=tiny_eyes)
    for side, s in SIDES:
        a, f, h = 'arm_' + side, 'forearm_' + side, 'hand_' + side
        g.bone(a, 'spine', [s * 5, 44, 0], [-4, 0, -s * 6])
        g.line(a, (s * 5, 44, 0), (s * 5, 28, 0), T, LN)
        g.bone(f, a, [s * 5, 28, 0], [-10, 0, 0])
        g.line(f, (s * 5, 28, 0), (s * 5, 13, 0), T, LN)
        g.bone(h, f, [s * 5, 13, 0])
        for k in range(4):
            dz = -1.5 + k
            g.line(h, (s * 5, 13, 0), (s * 5, 6, dz * 1.2), 0.45, LN)

    A = {}
    L = 5.0
    jit = keys([(i * 0.1, [0, 0, (3 if (i * 7) % 5 == 0 else 0) * (1 if i % 2 else -1)]) for i in range(51)])
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "spine": {"rotation": wave(L, [2, 0, 2])}, "head": {"rotation": jit},
        "hand_r": {"rotation": wave(L, 10, 0.4)}, "hand_l": {"rotation": wave(L, 10, 2.2)},
        "arm_r": {"rotation": wave(L, 3)}, "arm_l": {"rotation": wave(L, -3)}}}
    def stride(L, a, lean):
        return {"thigh_r": {"rotation": wave(L, a)}, "thigh_l": {"rotation": wave(L, -a)},
                "shin_r": {"rotation": wave(L, a * 0.5, 1.3)}, "shin_l": {"rotation": wave(L, -a * 0.5, 1.3)},
                "arm_r": {"rotation": wave(L, -a * 0.6, 0, base=(-lean, 0, 0))}, "arm_l": {"rotation": wave(L, a * 0.6, 0, base=(-lean, 0, 0))},
                "spine": {"rotation": keys([(0, [lean, 0, 0]), (L, [lean, 0, 0])])}, "head": {"rotation": jit},
                "pelvis": {"position": wave(L, [0, 0.8, 0], 0, n=12)}}
    A["walk"] = {"loop": True, "animation_length": 1.8, "bones": stride(1.8, 22, 4)}
    A["run"] = {"loop": True, "animation_length": 1.0, "bones": stride(1.0, 34, 16)}
    A["notice"] = {"loop": False, "animation_length": 1.2, "bones": {
        "head": {"rotation": ramp([(0, [0, 0, 0]), (0.05, [0, 40, 18]), (0.5, [0, 40, 18]), (0.55, [0, 0, 0]), (1.2, [0, 0, 0])])},
        "arm_r": {"rotation": ramp([(0, [0, 0, 0]), (0.05, [-30, 0, -20]), (0.9, [-30, 0, -20]), (1.2, [0, 0, 0])])},
        "arm_l": {"rotation": ramp([(0, [0, 0, 0]), (0.05, [-30, 0, 20]), (0.9, [-30, 0, 20]), (1.2, [0, 0, 0])])}}}
    return g.save(A)


# =================================================================================================
# THE OBSERVER: tall, thin and dark. It does not move much. It watches.
# =================================================================================================
def observer():
    g = Geo('observer', seed=7070)
    DK = void((14, 13, 16), 7)

    def eyes(g, r):
        u, v, w, h = r
        for cx in (1, w - 2):
            g.px(u + cx, v + h // 2, (190, 190, 180)); g.px(u + cx, v + h // 2, (120, 120, 112), glow=True)

    g.bone('root', None, [0, 0, 0])
    g.bone('hips', 'root', [0, 20, 0])
    for side, s in SIDES:
        t, sh = 'thigh_' + side, 'shin_' + side
        g.bone(t, 'hips', [s * 1.4, 20, 0])
        g.cube(t, [s * 1.4 - 0.8, 10, -0.8], [1.6, 10, 1.6], DK)
        g.bone(sh, t, [s * 1.4, 10, 0])
        g.cube(sh, [s * 1.4 - 0.7, 0, -0.7], [1.4, 10, 1.4], DK)
    g.bone('torso', 'hips', [0, 20, 0], [8, 0, 0])
    g.cube('torso', [-2.4, 20, -1.4], [4.8, 7, 2.8], DK)
    g.cube('torso', [-3, 26.5, -1.7], [6, 5.5, 3.4], DK)
    g.bone('neck', 'torso', [0, 32, -0.4], [16, 0, 0])
    g.cube('neck', [-0.6, 32, -1], [1.2, 4.5, 1.2], DK)
    g.bone('head', 'neck', [0, 36.5, -0.6])
    g.cube('head', [-2, 36.2, -3.8], [4, 5, 4], DK, face=eyes)
    for side, s in SIDES:
        a, f, h = 'arm_' + side, 'forearm_' + side, 'hand_' + side
        g.bone(a, 'torso', [s * 3.4, 31.5, 0], [0, 0, -s * 3])
        g.cube(a, [s * 3.4 - 0.6, 20.5, -0.6], [1.2, 11, 1.2], DK)
        g.bone(f, a, [s * 3.4, 20.5, 0])
        g.cube(f, [s * 3.4 - 0.5, 9.5, -0.5], [1, 11, 1], DK)
        g.bone(h, f, [s * 3.4, 9.5, 0])
        for k in range(4):
            g.cube(h, [s * 3.4 - 0.75 + k * 0.5, 4.2, -0.3], [0.3, 5.3, 0.3], DK, rot=[(k - 1.5) * 4, 0, 0], pivot=[s * 3.4, 9.5, 0])

    A = {}
    L = 9.0
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "head": {"rotation": ramp([(0, [0, 0, 0]), (3.0, [0, 0, 38]), (5.0, [0, 0, 38]), (5.06, [0, 0, 0]), (6.5, [0, 0, 0]),
                                   (7.6, [0, 42, 0]), (8.3, [0, 42, 0]), (8.35, [0, 0, 0]), (L, [0, 0, 0])])},
        "hand_r": {"rotation": snaps(L, [(2.1, 0.08, [18, 0, 0]), (6.9, 0.08, [12, 0, 0])])},
        "hand_l": {"rotation": snaps(L, [(4.4, 0.08, [16, 0, 0])])}, "torso": {"rotation": wave(L, [1, 0, 0])}}}
    def stride(L, a):
        return {"thigh_r": {"rotation": wave(L, a)}, "thigh_l": {"rotation": wave(L, -a)},
                "shin_r": {"rotation": wave(L, a * 0.4, 1.3)}, "shin_l": {"rotation": wave(L, -a * 0.4, 1.3)},
                "arm_r": {"rotation": wave(L, -a * 0.3)}, "arm_l": {"rotation": wave(L, a * 0.3)}}
    A["walk"] = {"loop": True, "animation_length": 2.4, "bones": stride(2.4, 14)}
    A["run"] = {"loop": True, "animation_length": 1.4, "bones": stride(1.4, 22)}
    return g.save(A)



# =================================================================================================
# 0.32.0: ARANEA MEMBRI (community Backrooms entity, modelled here from its description and the user's
# reference: four thick black legs whose knees rise high above the body (an "M" from the front), a small
# dark body slung low between them and a pale carnivore skull hanging down at the front).
# =================================================================================================
def aranea():
    g = Geo('aranea_membri', density=1, seed=6262)
    HIDE = void((16, 15, 18), 7)
    HIDE2 = void((24, 22, 26), 6)
    SKULL = bone_mat((206, 196, 168))

    def skull_face(g, r):
        u, v, w, h = r
        for cx in (1, w - 4):                                                    # deep, empty eye sockets
            for a in range(3):
                for b in range(3):
                    g.px(u + cx + a, v + 2 + b, (22, 18, 16) if (a, b) != (1, 1) else (6, 4, 4))
        for j in range(5, 8):                                                    # nasal cavity
            g.px(u + w // 2 - 1, v + j, (40, 30, 26)); g.px(u + w // 2, v + j, (30, 22, 20))
        for i in range(1, w - 1):                                                # a row of worn teeth
            g.px(u + i, v + h - 2, (230, 222, 196) if i % 2 else (150, 136, 110))
            g.px(u + i, v + h - 1, (60, 40, 34))
        for k in range(6):                                                       # hairline cracks
            g.px(u + 1 + (k * 5) % (w - 2), v + 1 + (k * 3) % (h - 3), (150, 140, 116))

    g.bone('root', None, [0, 0, 0])
    g.bone('body', 'root', [0, 24, 0])
    g.cube('body', [-5, 20, -7], [10, 8, 14], HIDE)                              # low, lumpy torso
    g.cube('body', [-4, 26.5, -5], [8, 3, 9], HIDE2)                             # hunched back
    g.cube('body', [-3.5, 18.5, -4], [7, 2, 9], HIDE)                            # sagging belly
    for k in range(4):                                                           # ragged shreds hanging off it
        x0 = -4 + k * 2.6
        g.cube('body', [x0, 15.5 - k % 2, -2 + k * 1.5], [0.8, 3.5 + k % 2, 0.6], HIDE, rot=[6 * (k - 1.5), 0, 5 * (1.5 - k)],
               pivot=[x0 + 0.4, 19, -2 + k * 1.5])
    g.bone('neck', 'body', [0, 24, -7], [32, 0, 0])
    g.cube('neck', [-2, 21, -10], [4, 5, 4], HIDE)
    g.bone('head', 'neck', [0, 22, -10])
    g.cube('head', [-3.5, 15.5, -14.5], [7, 8, 5], SKULL, face=skull_face)      # the skull, hanging down
    g.cube('head', [-2.5, 14, -14], [5, 1.6, 4], SKULL)                          # snout tip / jaw
    g.cube('head', [-3.8, 21.5, -11], [7.6, 2.5, 3], HIDE)                       # skin over the back of the skull
    # legs: each built in its own plane (z = hip) along +x, then turned outwards by the bone's yaw
    for name, sx, sz, yaw in (('fl', 1, -1, 38), ('fr', -1, -1, -38), ('bl', 1, 1, -38), ('br', -1, 1, 38)):
        hx, hz = sx * 4.5, sz * 4.5
        up, sh = 'leg_' + name, 'shin_' + name
        g.bone(up, 'body', [hx, 25, hz], [0, yaw, 0])
        kx = hx + sx * 11
        g.line(up, [hx, 25, hz], [kx, 52, hz], 6.8, HIDE)                        # thick upper leg rising to the knee
        g.cube(up, [hx + sx * 4 - 2.3, 33, hz - 2.3], [4.6, 5, 4.6], HIDE2)     # lump
        g.cube(up, [kx - 3.6, 48.5, hz - 3.6], [7.2, 5.6, 7.2], HIDE)          # knee
        g.bone(sh, up, [kx, 51, hz])
        mx = kx + sx * 7
        g.line(sh, [kx, 51, hz], [mx, 22, hz], 6.2, HIDE)                        # long shin, slightly outward
        g.line(sh, [mx, 22, hz], [mx + sx * 3.5, 3, hz], 4.6, HIDE)               # tapering towards the point
        g.line(sh, [mx + sx * 3.5, 3, hz], [mx + sx * 4.2, 0, hz], 2.4, HIDE2)   # sharp tip
        g.cube(sh, [mx - 3.2, 20.5, hz - 3.2], [6.4, 3.5, 6.4], HIDE2)              # ankle knot

    A = {}
    L = 4.0
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "body": {"position": wave(L, [0, 0.5, 0], 0, n=8)},
        "head": {"rotation": snaps(L, [(0.9, 0.9, [0, 28, -14]), (2.7, 0.4, [-20, -18, 10])], dur=0.05)},
        "leg_fl": {"rotation": snaps(L, [(1.8, 0.3, [0, 0, -6])], dur=0.05)}}}
    def gait(L, a, b):
        bones = {}
        for name, ph in (('fl', 0.0), ('br', 0.0), ('fr', math.pi), ('bl', math.pi)):
            sx = 1 if name in ('fl', 'bl') else -1
            bones['leg_' + name] = {"rotation": wave(L, [0, a, -sx * b * 0.5], ph)}
            bones['shin_' + name] = {"rotation": wave(L, [0, 0, sx * b], ph + 1.2)}
        bones["body"] = {"position": wave(L, [0, 0.8, 0], 0, n=12)}
        bones["head"] = {"rotation": snaps(L, [(L * 0.5, L * 0.25, [10, 20, 0])], dur=0.04)}
        return bones
    A["walk"] = {"loop": True, "animation_length": 1.4, "bones": gait(1.4, 12, 10)}
    A["run"] = {"loop": True, "animation_length": 0.55, "bones": gait(0.55, 20, 16)}
    A["screech"] = {"loop": False, "animation_length": 1.2, "bones": {
        "neck": {"rotation": ramp([(0, [0, 0, 0]), (0.15, [-38, 0, 0]), (0.9, [-38, 0, 0]), (1.2, [0, 0, 0])])},
        "head": {"rotation": snaps(1.2, [(0.3, 0.05, [0, 0, 14]), (0.5, 0.05, [0, 0, -14]), (0.7, 0.05, [0, 0, 12])], dur=0.04)},
        "leg_fl": {"rotation": ramp([(0, [0, 0, 0]), (0.2, [0, 0, -18]), (1.2, [0, 0, 0])])},
        "leg_fr": {"rotation": ramp([(0, [0, 0, 0]), (0.2, [0, 0, 18]), (1.2, [0, 0, 0])])}}}
    A["attack"] = {"loop": False, "animation_length": 0.6, "bones": {
        "leg_fl": {"rotation": ramp([(0, [0, 0, 0]), (0.2, [-30, 0, -26]), (0.32, [24, 0, 8]), (0.6, [0, 0, 0])])},
        "leg_fr": {"rotation": ramp([(0, [0, 0, 0]), (0.25, [-30, 0, 26]), (0.37, [24, 0, -8]), (0.6, [0, 0, 0])])},
        "body": {"position": ramp([(0, [0, 0, 0]), (0.3, [0, -2, -3]), (0.6, [0, 0, 0])])}}}
    r = g.save(A)
    import os
    from PIL import Image
    gm = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'office_rooms',
                      'textures', 'entity', 'aranea_membri_glowmask.png')
    if not os.path.exists(gm):                                    # the renderer always draws a glow layer: keep it empty
        Image.new('RGBA', (256, 256), (0, 0, 0, 0)).save(gm)
    return r


def mascot():
    """0.42.0: the Mascot of the SS Fun =): a soaked, swollen kids' club costume with a face that is only =)."""
    g = Geo('mascot', density=2, seed=4242)

    from geo_lib import _shade

    def fur(base, wet=(70, 66, 40)):
        def paint(g, r, face, alpha):
            u, v, pw, ph = r
            rng = g.rng
            for j in range(ph):
                for i in range(pw):
                    t = j / max(1, ph - 1)
                    s = _shade(face, j, ph) // 2 + rng.randint(-10, 10)
                    c = [b + s for b in base]
                    soak = max(0.0, t - 0.35) * 1.3 if face not in ('up',) else 0.0
                    c = [c[k] * (1 - soak) + wet[k] * soak for k in range(3)]      # soaked from below
                    if (i + j * 3) % 5 == 0 and rng.random() < 0.5:
                        c = [x - 18 for x in c]                                    # matted tufts
                    if rng.random() < 0.012:
                        c = [20, 14, 14]                                           # holes in the fabric
                    g.px(u + i, v + j, c + [alpha])
            for _ in range(max(1, pw * ph // 160)):                                # torn seams, dark inside
                x, y = u + rng.randrange(pw), v + rng.randrange(ph)
                for k in range(rng.randint(3, 7)):
                    g.px(x + k, y + (k % 2), [40, 18, 20, alpha])
        return paint

    FUR = fur((196, 170, 88))
    PALE = fur((232, 216, 168), wet=(120, 110, 80))
    NAVY = solid((34, 44, 86))
    WHITE = solid((226, 224, 214))
    SHOE = solid((150, 36, 34))

    def face(g, r):
        u, v, w, h = r
        for y0 in (h // 3, h // 3 + 4):                                            # "=": two black slits
            for i in range(w // 6, w // 6 + w // 3):
                for k in range(2):
                    g.px(u + i, v + y0 + k, (12, 10, 12))
        cx, cy = u + w * 0.30, v + h * 0.52                                        # ")": stitched smile
        for a in range(-55, 56, 3):
            t = math.radians(a)
            for rr in (w * 0.52, w * 0.52 + 1):
                g.px(int(cx + rr * math.cos(t)), int(cy + rr * math.sin(t)), (12, 10, 12))
        for a in range(-45, 46, 15):
            t = math.radians(a)
            for rr in (w * 0.45, w * 0.62):
                g.px(int(cx + rr * math.cos(t)), int(cy + rr * math.sin(t)), (60, 40, 36))
        for k in range(4):                                                         # water running from the eyes
            g.px(u + w // 6 + 2, v + h // 3 + 6 + k, (90, 96, 92))

    g.bone('root', None, [0, 0, 0])
    g.bone('body', 'root', [0, 10, 0])
    g.cube('body', [-6.5, 9, -4.5], [13, 9, 9], FUR, inflate=0.3)                 # swollen, sagging belly
    g.cube('body', [-5.5, 7.5, -3.5], [11, 2, 7], FUR)
    g.cube('body', [-3.5, 11, -4.9], [7, 5, 1], PALE)                              # belly patch
    g.bone('chest', 'body', [0, 18, 0])
    g.cube('chest', [-5.5, 18, -4], [11, 7, 8], FUR)
    g.cube('chest', [-6, 23.5, -4.5], [12, 2, 9], NAVY)                            # sailor collar
    g.cube('chest', [-5, 22.5, -4.7], [10, 1, 1], WHITE)
    g.bone('head', 'chest', [0, 25, 0], [6, 0, 4])                                 # it never holds its head straight
    g.cube('head', [-7, 25, -6.5], [14, 12, 13], FUR, inflate=0.4)                 # far too big
    g.cube('head', [-5, 26, -7.2], [10, 8, 1], PALE, face=face)                    # the face: =)
    g.cube('head', [-6.5, 36.5, -2.5], [4, 4, 3], FUR, rot=[0, 0, 18], pivot=[-4.5, 37, -1])   # ear
    g.cube('head', [3.5, 33, -2.5], [3, 4, 3], FUR, rot=[0, 0, -70], pivot=[5, 36, -1])        # the torn ear, hanging
    g.cube('head', [-3.5, 37.2, -3.5], [7, 2, 7], WHITE, rot=[-8, 0, -12], pivot=[0, 37, 0])   # sailor hat
    g.cube('head', [-3.6, 37.0, -3.6], [7.2, 0.8, 7.2], NAVY, rot=[-8, 0, -12], pivot=[0, 37, 0])
    for name, sx in (('arm_l', 1), ('arm_r', -1)):
        g.bone(name, 'chest', [sx * 6, 24, 0])
        g.cube(name, [sx * 6 - 1.6, 14, -1.6], [3.2, 10, 3.2], FUR)                 # too long
        g.bone(name + '_lo', name, [sx * 6, 14.5, 0])
        g.cube(name + '_lo', [sx * 6 - 1.5, 3.5, -1.5], [3, 11, 3], FUR)
        g.cube(name + '_lo', [sx * 6 - 2.2, 0.5, -2.5], [4.4, 3.5, 5], PALE)        # mittens nearly on the floor
    for name, sx in (('leg_l', 1), ('leg_r', -1)):
        g.bone(name, 'root', [sx * 3, 9, 0])
        g.cube(name, [sx * 3 - 2.2, 1.5, -2.2], [4.4, 8, 4.4], FUR)
        g.cube(name, [sx * 3 - 2.6, 0, -4.2], [5.2, 2, 7], SHOE)                     # big clown shoes

    A = {}
    A["idle"] = {"loop": True, "animation_length": 5.0, "bones": {
        "body": {"rotation": wave(5.0, [2, 0, 3], 0, n=10)},
        "head": {"rotation": snaps(5.0, [(1.2, 1.1, [0, 0, 24]), (3.6, 0.4, [-14, 30, -10])], dur=0.06)},
        "arm_l": {"rotation": wave(5.0, [6, 0, 3], 0.6, n=10)},
        "arm_r": {"rotation": wave(5.0, [6, 0, -3], 2.2, n=10)}}}
    def walk(L, a):
        return {"leg_l": {"rotation": wave(L, [a, 0, 0], 0)}, "leg_r": {"rotation": wave(L, [a, 0, 0], math.pi)},
                "arm_l": {"rotation": wave(L, [a * 0.7, 0, 4], math.pi)}, "arm_r": {"rotation": wave(L, [a * 0.7, 0, -4], 0)},
                "body": {"position": wave(L, [0, 0.7, 0], 0, n=12), "rotation": wave(L, [0, 0, 4], 0.5, n=12)},
                "head": {"rotation": snaps(L, [(L * 0.4, L * 0.2, [10, 0, 18])], dur=0.05)}}
    A["walk"] = {"loop": True, "animation_length": 1.2, "bones": walk(1.2, 24)}
    run = {"body": {"rotation": keys([(0, [62, 0, 0]), (0.5, [62, 0, 0])]), "position": wave(0.5, [0, 1.2, 0], 0, n=8, base=(0, -3, 0))},
           "head": {"rotation": keys([(0, [-58, 0, 12]), (0.5, [-58, 0, 12])])},
           "arm_l": {"rotation": wave(0.5, [50, 0, 0], 0, base=(-70, 0, 0))}, "arm_r": {"rotation": wave(0.5, [50, 0, 0], math.pi, base=(-70, 0, 0))},
           "leg_l": {"rotation": wave(0.5, [45, 0, 0], math.pi)}, "leg_r": {"rotation": wave(0.5, [45, 0, 0], 0)}}
    A["run"] = {"loop": True, "animation_length": 0.5, "bones": run}
    A["wave"] = {"loop": True, "animation_length": 2.4, "bones": {
        "arm_r": {"rotation": wave(2.4, [0, 0, 22], 0, n=8, base=(0, 0, -150))},
        "arm_r_lo": {"rotation": wave(2.4, [0, 0, 16], 1.0, n=8)},
        "head": {"rotation": wave(2.4, [0, 0, 14], 0.4, n=8, base=(0, 0, 16))},
        "body": {"rotation": wave(2.4, [0, 0, 2], 0, n=8)}}}
    A["hug"] = {"loop": True, "animation_length": 0.8, "bones": {
        "arm_l": {"rotation": wave(0.8, [0, 14, 0], 0, n=8, base=(-90, 30, 0))},
        "arm_r": {"rotation": wave(0.8, [0, -14, 0], 0, n=8, base=(-90, -30, 0))},
        "head": {"rotation": snaps(0.8, [(0.2, 0.15, [16, 0, 26])], dur=0.04)},
        "body": {"rotation": wave(0.8, [6, 0, 0], 0, n=8, base=(10, 0, 0))}}}
    A["drop"] = {"loop": False, "animation_length": 0.8, "bones": {
        "body": {"position": ramp([(0, [0, 14, 0]), (0.15, [0, -4, 0]), (0.4, [0, -1, 0]), (0.8, [0, 0, 0])]),
                 "rotation": ramp([(0, [0, 0, 180]), (0.15, [30, 0, 10]), (0.8, [0, 0, 0])])},
        "head": {"rotation": snaps(0.8, [(0.35, 0.2, [0, 0, 40])], dur=0.04)}}}
    return g.save(A)


if __name__ == '__main__':
    for f in (gaunt, warden, hound, smiler, colony, hunter, observer, aranea):
        g = f()
        print('%-16s bones %3d  cubes %3d  texture rows used %d/%d' % (g.name, len(g.bones), sum(len(b['cubes']) for b in g.bones), g.sy + g.sh, g.H))
