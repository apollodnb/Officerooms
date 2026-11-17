"""Office Rooms 0.11.0: tiny toolkit for GeckoLib creature models built in code.

Per-face UVs at DENSITY texture pixels per model unit (finer detail than box UVs), procedural
materials (flesh, chitin, bone, void), glow masks, and keyframe helpers. Everything is generated
here; no foreign assets."""
import json, math, os, random
from PIL import Image

R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources/assets/office_rooms')
FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def clamp(v):
    return max(0, min(255, int(v)))


class Geo:
    def __init__(self, name, density=2, size=256, seed=1):
        self.name, self.D, self.W, self.H = name, density, size, size
        self.rng = random.Random(seed)
        self.tex = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        self.bones, self.index = [], {}
        self.sx = self.sy = self.sh = 0

    # --- layout ------------------------------------------------------------------------------
    def alloc(self, w, h):
        if self.sx + w > self.W:
            self.sx, self.sy, self.sh = 0, self.sy + self.sh, 0
        if self.sy + h > self.H:
            raise RuntimeError(self.name + ': texture full')
        u, v = self.sx, self.sy
        self.sx += w
        self.sh = max(self.sh, h)
        return u, v

    def bone(self, name, parent, pivot, rot=None):
        b = {"name": name, "pivot": [round(p, 3) for p in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        if rot:
            b["rotation"] = rot
        self.bones.append(b)
        self.index[name] = b
        return name

    def cube(self, bone, origin, size, mat, rot=None, pivot=None, inflate=0, face=None, top=None, alpha=255):
        D = self.D
        w, h, d = size
        dims = {'north': (w, h), 'south': (w, h), 'east': (d, h), 'west': (d, h), 'up': (w, d), 'down': (w, d)}
        uv = {}
        for f in FACES:
            pw, ph = max(1, round(dims[f][0] * D)), max(1, round(dims[f][1] * D))
            u, v = self.alloc(pw, ph)
            uv[f] = {"uv": [u, v], "uv_size": [pw, ph]}
            mat(self, (u, v, pw, ph), f, alpha)
            if face and f == 'north':
                face(self, (u, v, pw, ph))
            if top and f == 'up':
                top(self, (u, v, pw, ph))
        c = {"origin": [round(o, 3) for o in origin], "size": [round(s, 3) for s in size], "uv": uv}
        if rot:
            c["rotation"] = rot
            c["pivot"] = [round(p, 3) for p in (pivot or origin)]
        if inflate:
            c["inflate"] = inflate
        self.index[bone]["cubes"].append(c)
        return c

    def line(self, bone, a, b, t, mat, alpha=255):
        """A thin rod from a to b, in a plane x = const or z = const (one rotation axis only).
        Conventions (checked in game): +x rotation tips the top towards -z, +z rotation tips it towards +x."""
        ax, ay, az = a
        bx, by, bz = b
        dx, dy, dz = bx - ax, by - ay, bz - az
        if abs(dx) > 1e-6 and abs(dz) > 1e-6:
            raise ValueError('line must lie in an x or z plane')
        L = math.sqrt(dx * dx + dy * dy + dz * dz)
        rot = None
        if abs(dz) > 1e-6:
            rot = [round(math.degrees(math.atan2(-dz, dy)), 2), 0, 0]
        elif abs(dx) > 1e-6:
            rot = [0, 0, round(math.degrees(math.atan2(dx, dy)), 2)]
        elif dy < 0:
            rot = [180, 0, 0]
        return self.cube(bone, [ax - t / 2, ay, az - t / 2], [t, L, t], mat, rot=rot, pivot=[ax, ay, az], alpha=alpha)

    # --- painting -----------------------------------------------------------------------------
    def px(self, x, y, c, glow=False):
        if 0 <= x < self.W and 0 <= y < self.H:
            (self.glow if glow else self.tex).putpixel((x, y), tuple(clamp(v) for v in c[:3]) + ((c[3] if len(c) > 3 else 255),))

    def save(self, anims):
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{"description": {
            "identifier": "geometry." + self.name, "texture_width": self.W, "texture_height": self.H,
            "visible_bounds_width": 6, "visible_bounds_height": 6, "visible_bounds_offset": [0, 2, 0]}, "bones": self.bones}]}
        for sub in ('geo/entity', 'animations/entity', 'textures/entity'):
            os.makedirs(os.path.join(R, sub), exist_ok=True)
        json.dump(geo, open(os.path.join(R, 'geo/entity', self.name + '.geo.json'), 'w'))
        json.dump({"format_version": "1.8.0", "animations": anims}, open(os.path.join(R, 'animations/entity', self.name + '.animation.json'), 'w'))
        self.tex.save(os.path.join(R, 'textures/entity', self.name + '.png'))
        if self.glow.getbbox():
            self.glow.save(os.path.join(R, 'textures/entity', self.name + '_glowmask.png'))
        return self


# --- materials ---------------------------------------------------------------------------------

def _shade(face, j, ph):
    if face == 'up':
        return 14
    if face == 'down':
        return -34
    return 12 - 34 * (j / max(1, ph - 1))          # light from above, dark towards the bottom


def flesh(base, vein=(70, 14, 20), bruise=(60, 34, 52), wet=0.015):
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        rng = g.rng
        for j in range(ph):
            for i in range(pw):
                s = _shade(face, j, ph) + rng.randint(-7, 7)
                if i == 0 or j == 0 or i == pw - 1 or j == ph - 1:
                    s -= 12
                c = [b + s for b in base]
                if (i * 5 + j * 3 + (i * j) % 7) % 13 == 0:
                    c = [x - 10 for x in c]                                  # pores and wrinkles
                if rng.random() < wet:
                    c = [x + 38 for x in c]                                  # moist highlight
                g.px(u + i, v + j, c + [alpha])
        for _ in range(max(1, pw * ph // 90)):                              # bruises
            bx, by = u + rng.randrange(pw), v + rng.randrange(ph)
            for k in range(rng.randint(2, 5)):
                g.px(bx + rng.randint(-1, 1), by + rng.randint(-1, 1), list(bruise) + [alpha])
        for _ in range(max(1, pw * ph // 140)):                             # veins: short random walks
            x, y = u + rng.randrange(pw), v + rng.randrange(ph)
            for k in range(rng.randint(3, 9)):
                if u <= x < u + pw and v <= y < v + ph:
                    g.px(x, y, list(vein) + [alpha])
                x += rng.choice((-1, 0, 1))
                y += rng.choice((0, 1, 1))
    return paint


def chitin(base, hi=(120, 100, 86)):
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        rng = g.rng
        for j in range(ph):
            for i in range(pw):
                s = _shade(face, j, ph) // 2 + rng.randint(-4, 4)
                c = [b + s for b in base]
                if face != 'down' and j == 1:
                    c = list(hi)                                             # glossy rim
                if (j + i // 3) % 5 == 0 and face not in ('up', 'down'):
                    c = [x - 12 for x in c]                                  # growth bands
                if rng.random() < 0.02:
                    c = [x + 30 for x in c]
                g.px(u + i, v + j, c + [alpha])
    return paint


def bone_mat(base=(176, 162, 128)):
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        for j in range(ph):
            for i in range(pw):
                t = j / max(1, ph - 1)
                c = [b * (0.55 + 0.45 * (1 - t)) + g.rng.randint(-8, 8) for b in base]
                if g.rng.random() < 0.05:
                    c = [x * 0.6 for x in c]                                 # cracks and dirt
                g.px(u + i, v + j, c + [alpha])
    return paint


def void(base=(10, 10, 13), spread=6, edge_alpha=None):
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        for j in range(ph):
            for i in range(pw):
                c = [b + g.rng.randint(-spread, spread) for b in base]
                a = alpha
                if edge_alpha is not None and (i == 0 or j == 0 or i == pw - 1 or j == ph - 1):
                    a = edge_alpha
                g.px(u + i, v + j, c + [a])
    return paint


def solid(col, glow=False):
    def paint(g, r, face, alpha):
        u, v, pw, ph = r
        for j in range(ph):
            for i in range(pw):
                c = [x + g.rng.randint(-10, 10) for x in col]
                g.px(u + i, v + j, c + [alpha])
                if glow:
                    g.px(u + i, v + j, c + [255], glow=True)
    return paint


# --- animation helpers -------------------------------------------------------------------------

def keys(pairs):
    return {("%.3f" % t): [round(x, 2) for x in v] for t, v in pairs}


def wave(L, amp, phase=0.0, axis=0, n=12, base=(0, 0, 0)):
    """amp may be a number (one axis) or a 3-vector."""
    out = []
    for i in range(n + 1):
        s = math.sin(2 * math.pi * i / n + phase)
        if isinstance(amp, (list, tuple)):
            v = [base[k] + amp[k] * s for k in range(3)]
        else:
            v = list(base)
            v[axis] += amp * s
        out.append((L * i / n, v))
    return keys(out)


def snaps(L, events, rest=(0, 0, 0), dur=0.05):
    """Sudden jerks: events = [(start, hold, vector)]. Snaps in, holds, snaps back."""
    pts = [(0, rest)]
    for t, hold, vec in events:
        pts += [(t, rest), (t + dur, vec), (t + dur + hold, vec), (t + 2 * dur + hold, rest)]
    pts.append((L, rest))
    pts.sort(key=lambda p: p[0])
    return keys(pts)


def ramp(points):
    return keys(points)
