"""Office Rooms 0.13.0: The Flayed. A stretched, skinless humanoid of BLACK muscle tissue (no red, by design),
far too tall and thin, with dangling arms that almost reach the floor and an elongated head that is mostly mouth.
Run: python3 tools/gen_flayed_geo.py  (writes geo, animations, texture and an empty glowmask)"""
import os
from PIL import Image
from geo_lib import Geo, keys, wave, snaps, ramp

SIDES = (('r', -1), ('l', 1))


def muscle(base=(19, 15, 17)):
    """Black muscle: vertical fibre striations, grey sinew lines, wet glints, deep shadow at the bottom."""
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        vertical = face not in ('up', 'down')
        cols = [g.rng.randint(-5, 7) for _ in range(max(pw, ph))]
        for j in range(ph):
            for i in range(pw):
                k = i if vertical else j
                s = cols[k] + g.rng.randint(-3, 3) + (6 - int(14 * j / max(1, ph - 1)) if vertical else (-8 if face == 'down' else 4))
                c = [b + s for b in base]
                if k % 4 == 0:
                    c = [x - 7 for x in c]                                   # grooves between fibre bundles
                if (k * 7 + j // 3) % 23 == 0:
                    c = [x + 22 for x in c]                                  # pale sinew
                if g.rng.random() < 0.018:
                    c = [x + 48 for x in c]                                  # wet glint
                g.px(u + i, v + j, [max(0, min(255, x)) for x in c] + [alpha])
    return paint


def void_black(g, r, face, alpha):
    u, v, pw, ph = r
    for j in range(ph):
        for i in range(pw):
            g.px(u + i, v + j, [3 + g.rng.randint(0, 3)] * 3 + [alpha])


def mouth(g, r):
    """The front of the head: a ragged, tall black opening rimmed with grey teeth-like splinters."""
    u, v, w, h = r
    for j in range(1, h - 1):
        half = max(1, int((w / 2 - 0.5) * (0.55 + 0.45 * (1 - abs(2 * j / h - 1.1)))))
        for i in range(w // 2 - half, w // 2 + half):
            g.px(u + i, v + j, [2, 2, 3, 255])
        for edge in (w // 2 - half - 1, w // 2 + half):
            if g.rng.random() < 0.55 and 0 <= edge < w:
                g.px(u + edge, v + j, [92, 88, 84, 255])


def scribble(base=(14, 12, 13), with_mouth=False):
    """Black head covered in scratchy, scribbled grey lines (random jagged strokes), optionally with the mouth."""
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        for j in range(ph):
            for i in range(pw):
                g.px(u + i, v + j, [b + g.rng.randint(-3, 4) for b in base] + [alpha])
        for _ in range(max(4, pw * ph // 9)):
            x, y = u + g.rng.randrange(pw), v + g.rng.randrange(ph)
            dx, dy = g.rng.choice((-1, 1)), g.rng.choice((-1, 0, 1))
            shade = g.rng.randint(95, 170)
            for k in range(g.rng.randint(4, 12)):
                if u <= x < u + pw and v <= y < v + ph:
                    g.px(x, y, [shade, shade - 4, shade - 2, alpha])
                if g.rng.random() < 0.35:
                    dx, dy = g.rng.choice((-1, 1)), g.rng.choice((-1, 0, 1))
                x += dx
                y += dy if g.rng.random() < 0.7 else -dy
    return paint


SCR = scribble()


def scribble_mouth(g, r):
    SCR(g, r, 'north', 255)
    mouth(g, r)


M = muscle()
M2 = muscle((30, 25, 27))


def flayed():
    """0.13.0 (rev. 2): wire-thin. Every part is a strand of black muscle a fraction of a block thick; the body is
    an outline of fibres around a spine rather than a solid torso. Long segments are kinked slightly for an organic line."""
    g = Geo('flayed', seed=1313)

    def rod(bone, x, y0, y1, z, t, mat=M, kink=0.0, zk=0.0):
        """A vertical strand from y0 to y1, centred on x/z, split into two slightly kinked halves."""
        h = (y1 - y0) / 2
        g.cube(bone, [x - t / 2, y0, z - t / 2], [t, h + 0.05, t], mat, rot=[zk, 0, kink], pivot=[x, y0 + h, z])
        g.cube(bone, [x - t / 2, y0 + h, z - t / 2], [t, h, t], mat, rot=[-zk, 0, -kink], pivot=[x, y0 + h, z])

    g.bone('root', None, [0, 0, 0])
    g.bone('hips', 'root', [0, 24, 0])
    g.cube('hips', [-2.2, 23.2, -0.35], [4.4, 0.7, 0.7], M2)                    # pelvis bar
    for s in (-1, 1):
        g.cube('hips', [s * 1.2 - 0.2, 23.6, -0.2], [0.4, 3.2, 0.4], M, rot=[0, 0, -s * 28], pivot=[s * 1.2, 23.8, 0])
    for side, s in SIDES:
        t, sh, ft = 'thigh_' + side, 'shin_' + side, 'foot_' + side
        g.bone(t, 'hips', [s * 2, 23.4, 0])
        rod(t, s * 2, 11.5, 23.4, 0, 0.7, M, kink=s * 2.5)
        rod(t, s * 2 + s * 0.35, 12.5, 22.5, -0.3, 0.3, M2, kink=-s * 4)          # loose fibre beside the bone
        g.cube(t, [s * 2 - 0.5, 11, -0.5], [1, 1.1, 1], M2)                         # knee knot
        g.bone(sh, t, [s * 2, 11.5, 0])
        rod(sh, s * 2, 0.8, 11.5, 0, 0.55, M, kink=-s * 2, zk=3)
        rod(sh, s * 2 - s * 0.3, 2, 10, 0.3, 0.25, M2, kink=s * 3)
        g.bone(ft, sh, [s * 2, 1, 0])
        g.cube(ft, [s * 2 - 0.35, 0, -0.6], [0.7, 0.8, 1.2], M2)
        for k in range(3):
            x = s * 2 - 0.5 + k * 0.5
            g.cube(ft, [x - 0.15, 0, -4.2], [0.3, 0.35, 3.8], M, rot=[0, (k - 1) * 12, 0], pivot=[x, 0.2, -0.4])
    g.bone('torso', 'hips', [0, 26, 0], [14, 0, 0])
    rod('torso', 0, 24, 43.5, 0.3, 0.8, M2, zk=4)                                   # spine
    for k, x in enumerate((-0.9, 0.1, 0.9)):                                        # twisted abdominal strands
        rod('torso', x, 25, 34, -0.6, 0.35, M, kink=(k - 1) * 7)
    for k in range(5):                                                              # rib outline: thin curved strands
        y = 33.5 + k * 1.8
        w = 2.6 - abs(k - 1.5) * 0.35
        for s in (-1, 1):
            g.cube('torso', [s * w / 2 - w / 2, y, 0.0], [w, 0.3, 0.3], M, rot=[0, s * 35, s * -8], pivot=[0, y, 0.3])
            g.cube('torso', [s * w - 0.15, y - 1.4, -1.2], [0.3, 1.6, 0.3], M2, rot=[25, 0, 0], pivot=[s * w, y, -0.6])
    for s in (-1, 1):                                                               # chest outline down the sides
        rod('torso', s * 2.5, 32.5, 42, -0.4, 0.35, M, kink=-s * 5)
    g.cube('torso', [-4.6, 42, -0.3], [9.2, 0.55, 0.55], M2)                       # collar bar
    for s in (-1, 1):
        g.cube('torso', [s * 1.4 - 0.15, 37, -1.5], [0.3, 5, 0.3], M, rot=[0, 0, s * 18], pivot=[s * 1.4, 42, -1.4])
    g.bone('neck', 'torso', [0, 43.5, -0.4], [26, 0, 0])
    for x in (-0.35, 0.35):
        rod('neck', x, 43, 50, -0.4, 0.4, M, kink=x * 14)
    g.bone('head', 'neck', [0, 49.5, -0.6], [-18, 0, 0])
    g.cube('head', [-2.3, 50, -1.8], [4.6, 7, 2.4], SCR, face=scribble_mouth)   # wide, flat, scribbled head; the front is a mouth
    g.cube('head', [-2.9, 52.5, -1.4], [0.8, 3.2, 1.6], SCR, rot=[0, 0, 14], pivot=[-2.3, 54, -0.6])   # lopsided side lumps
    g.cube('head', [2.1, 53, -1.4], [0.9, 2.6, 1.6], SCR, rot=[0, 0, -10], pivot=[2.3, 54, -0.6])
    g.cube('head', [-1.6, 56.8, -1.3], [3.0, 1.4, 1.6], SCR, rot=[0, 0, -6], pivot=[0, 57, -0.5])
    g.cube('head', [-1.1, 50.6, -1.88], [2.2, 5.6, 0.1], void_black)
    g.bone('jaw', 'head', [0, 51, -1.0])
    g.cube('jaw', [-1.4, 46.6, -1.7], [2.8, 4.4, 1.2], SCR)
    g.cube('jaw', [-0.9, 47.2, -1.75], [1.8, 3.4, 0.1], void_black)
    for k, x in enumerate((-0.9, 0.0, 0.8)):
        g.cube('jaw', [x, 42.6 - k, -1.3], [0.15, 4 + k, 0.15], M2)                # strands hanging from the jaw
    for side, s in SIDES:
        a, f, h = 'arm_' + side, 'forearm_' + side, 'hand_' + side
        g.bone(a, 'torso', [s * 4.4, 42.3, 0], [-14, 0, -s * 3])
        rod(a, s * 4.4, 27, 42.3, 0, 0.6, M, kink=s * 3)
        rod(a, s * 4.4 + s * 0.3, 28.5, 40, -0.3, 0.25, M2, kink=-s * 5)
        g.cube(a, [s * 4.4 - 0.45, 26.6, -0.45], [0.9, 0.9, 0.9], M2)            # elbow knot
        g.bone(f, a, [s * 4.4, 27, 0])
        rod(f, s * 4.4, 12, 27, 0, 0.5, M, kink=-s * 2, zk=-3)
        rod(f, s * 4.4 - s * 0.25, 13.5, 25.5, 0.25, 0.22, M2, kink=s * 4)
        g.bone(h, f, [s * 4.4, 12, 0])
        g.cube(h, [s * 4.4 - 0.4, 10.6, -0.4], [0.8, 1.6, 0.8], M2)
        for k in range(4):                                                          # very long, hair-thin fingers
            x = s * 4.4 - 0.45 + k * 0.3
            g.cube(h, [x - 0.1, 2.0, -0.1], [0.2, 8.8, 0.2], M, rot=[(k - 1.5) * 7, 0, s * (k - 1.5) * 6], pivot=[x, 10.8, 0])

    A = {}
    L = 7.0
    A["idle"] = {"loop": True, "animation_length": L, "bones": {
        "torso": {"rotation": wave(L, [3, 0, 2])},
        "head": {"rotation": ramp([(0, [0, 0, 0]), (1.8, [0, 0, 24]), (2.9, [0, 0, 24]), (2.95, [10, -30, -6]), (3.6, [10, -30, -6]),
                                   (3.65, [0, 0, 0]), (5.5, [-12, 0, 0]), (6.2, [-12, 0, 0]), (L, [0, 0, 0])])},
        "jaw": {"rotation": snaps(L, [(1.1, 0.25, [-22, 0, 0]), (4.7, 0.6, [-35, 0, 0])], dur=0.08)},
        "hand_r": {"rotation": snaps(L, [(0.8, 0.06, [24, 0, 0]), (2.2, 0.06, [-14, 0, 0]), (6.1, 0.1, [30, 0, 0])])},
        "hand_l": {"rotation": snaps(L, [(3.3, 0.06, [26, 0, 0]), (5.1, 0.06, [-18, 0, 0])])},
        "arm_r": {"rotation": wave(L, [5, 0, 3], 0.4)}, "arm_l": {"rotation": wave(L, [5, 0, -3], 2.1)}}}

    def legs(L, a, knee):
        return {"thigh_r": {"rotation": wave(L, a)}, "thigh_l": {"rotation": wave(L, -a)},
                "shin_r": {"rotation": wave(L, knee, 1.4, base=(knee * 0.6, 0, 0))}, "shin_l": {"rotation": wave(L, knee, 1.4 + 3.14159, base=(knee * 0.6, 0, 0))}}
    W = 3.4                                                                # slow, wrong walk: long limping strides, head jerks
    A["walk"] = {"loop": True, "animation_length": W, "bones": dict(legs(W, 20, 16), **{
        "hips": {"rotation": wave(W, [0, 0, 7], n=8)},
        "torso": {"rotation": wave(W, [4, 0, -9], 0.8, n=8)},
        "arm_r": {"rotation": wave(W, [-8, 0, 6], 0.3)}, "arm_l": {"rotation": wave(W, [8, 0, -6], 0.3)},
        "forearm_r": {"rotation": wave(W, [-10, 0, 0], 1.3)}, "forearm_l": {"rotation": wave(W, [10, 0, 0], 1.3)},
        "head": {"rotation": snaps(W, [(0.7, 0.3, [0, 0, 30]), (2.3, 0.2, [-15, 20, 0])], dur=0.06)},
        "jaw": {"rotation": wave(W, [-6, 0, 0], 0, n=6, base=(-8, 0, 0))}})}
    R = 0.62                                                               # sprint: bent far forward, arms thrashing
    A["run"] = {"loop": True, "animation_length": R, "bones": dict(legs(R, 46, 30), **{
        "torso": {"rotation": wave(R, [6, 0, 5], 0, n=8, base=(22, 0, 0))},
        "neck": {"rotation": keys([(0, [-14, 0, 0]), (R, [-14, 0, 0])])},
        "head": {"rotation": wave(R, [0, 0, 9], 1.0, n=8)},
        "jaw": {"rotation": wave(R, [-10, 0, 0], 0, n=8, base=(-30, 0, 0))},
        "arm_r": {"rotation": wave(R, [55, 0, 10], 0.0, n=8, base=(-20, 0, 0))}, "arm_l": {"rotation": wave(R, [-55, 0, -10], 0.0, n=8, base=(-20, 0, 0))},
        "forearm_r": {"rotation": wave(R, [-30, 0, 0], 1.5, n=8)}, "forearm_l": {"rotation": wave(R, [30, 0, 0], 1.5, n=8)}})}
    S = 1.6                                                                # rears up, mouth gaping, arms spread
    shake = lambda amp: keys([(t / 20, [0, 0, amp * (1 if i % 2 else -1)]) for i, t in enumerate(range(6, 30))])
    A["scream"] = {"loop": False, "animation_length": S, "bones": {
        "torso": {"rotation": ramp([(0, [0, 0, 0]), (0.25, [-24, 0, 0]), (1.3, [-24, 0, 0]), (S, [0, 0, 0])])},
        "neck": {"rotation": ramp([(0, [0, 0, 0]), (0.25, [-22, 0, 0]), (1.3, [-22, 0, 0]), (S, [0, 0, 0])])},
        "head": {"rotation": shake(5)},
        "jaw": {"rotation": ramp([(0, [0, 0, 0]), (0.2, [-60, 0, 0]), (1.3, [-60, 0, 0]), (S, [0, 0, 0])])},
        "arm_r": {"rotation": ramp([(0, [0, 0, 0]), (0.3, [-30, 0, -55]), (1.3, [-30, 0, -55]), (S, [0, 0, 0])])},
        "arm_l": {"rotation": ramp([(0, [0, 0, 0]), (0.3, [-30, 0, 55]), (1.3, [-30, 0, 55]), (S, [0, 0, 0])])},
        "hand_r": {"rotation": shake(12)}, "hand_l": {"rotation": shake(-12)}}}
    F = 3.0                                                                # exhausted for a moment: slumped, swaying
    A["falter"] = {"loop": False, "animation_length": F, "bones": {
        "torso": {"rotation": ramp([(0, [0, 0, 0]), (0.4, [34, 0, 8]), (2.5, [30, 0, -6]), (F, [0, 0, 0])])},
        "head": {"rotation": ramp([(0, [0, 0, 0]), (0.4, [30, 0, 20]), (2.5, [30, 0, 20]), (F, [0, 0, 0])])},
        "jaw": {"rotation": wave(F, [-8, 0, 0], 0, n=6, base=(-14, 0, 0))},
        "arm_r": {"rotation": ramp([(0, [0, 0, 0]), (0.5, [-30, 0, 4]), (2.5, [-30, 0, 4]), (F, [0, 0, 0])])},
        "arm_l": {"rotation": ramp([(0, [0, 0, 0]), (0.5, [-30, 0, -4]), (2.5, [-30, 0, -4]), (F, [0, 0, 0])])},
        "thigh_r": {"rotation": wave(F, 6, 0, n=6)}, "thigh_l": {"rotation": wave(F, -6, 0, n=6)}}}
    g.save(A)
    p = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/office_rooms/textures/entity/flayed_glowmask.png')
    Image.new('RGBA', (g.W, g.H), (0, 0, 0, 0)).save(p)                 # no glowing parts at all
    return g


if __name__ == '__main__':
    g = flayed()
    print('flayed bones %d cubes %d rows %d/%d' % (len(g.bones), sum(len(b['cubes']) for b in g.bones), g.sy + g.sh, g.H))
