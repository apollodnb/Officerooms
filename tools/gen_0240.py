"""0.24.0: Deathmoth textures (entity 128x64, screen overlay 256x256), lang, spawn egg model. Drawn here."""
import json, os, math, random
from PIL import Image, ImageFilter
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'
rnd = random.Random(240)
def n(a): return rnd.randint(-a, a)
def cl(c, a=255): return tuple(max(0, min(255, int(v))) for v in c) + (a,)

def wing_color(t, s, fore, under):
    """t: 0 root .. 1 tip, s: 0 front .. 1 back edge. Dusty, natural, never bright."""
    base = (96, 82, 64) if fore else (84, 76, 66)
    if under: base = tuple(v + 18 for v in base)
    c = [v + n(7) for v in base]
    for band, w, k in ((0.42, 0.05, -30), (0.74, 0.04, -26), (0.58, 0.03, 14)):
        if abs(t - band + 0.08 * (s - 0.5)) < w: c = [v + k for v in c]
    if fore and not under:
        d = math.hypot((t - 0.6) * 1.4, s - 0.45)                      # dull eye spot (no glow: dark ring, grey-brown centre)
        if d < 0.09: c = [62, 56, 48]
        if d < 0.05: c = [118, 108, 92]
    if (int(t * 30) + int(s * 3)) % 7 == 0 and rnd.random() < 0.5: c = [v - 18 for v in c]   # veins
    if rnd.random() < 0.08: c = [v + 22 for v in c]                    # dust flecks
    if s > 0.9 or t > 0.95: c = [v - 22 for v in c]                    # worn darker fringe
    return c

def draw_wing(px, u0, v0, w, d, fore, under):
    for x in range(w):
        t = (x + 0.5) / w
        if fore:
            zf, zb = 0.30 - 0.30 * t, 0.85 - 0.55 * t ** 1.3
        else:
            zf, zb = 0.20 - 0.10 * t, 1.0 - 0.55 * t ** 1.1
        if t > 0.9: zb -= (t - 0.9) * 3
        for z in range(d):
            s = (z + 0.5) / d
            if zf <= s <= zb:
                ss = (s - zf) / max(0.01, zb - zf)
                px[u0 + x, v0 + z] = cl(wing_color(t, ss, fore, under))
            else:
                px[u0 + x, v0 + z] = (0, 0, 0, 0)

im = Image.new('RGBA', (128, 64), (0, 0, 0, 0)); px = im.load()
# fuzzy body everywhere on the right half
for y in range(64):
    for x in range(56, 128):
        b = 70 + n(14) + (12 if (x * 7 + y * 3) % 5 == 0 else 0)
        px[x, y] = cl((b, b * 0.82, b * 0.62))
# eyes on the head front: dark, matte
for x, y in ((71, 15), (74, 15)): px[x, y] = cl((24, 20, 18))
# forewing 14x12 at texOffs(0,0): top (12,0), bottom (26,0); hindwing 11x10 at texOffs(0,16): top (10,16), bottom (21,16)
draw_wing(px, 12, 0, 14, 12, True, False); draw_wing(px, 26, 0, 14, 12, True, True)
draw_wing(px, 10, 16, 11, 10, False, False); draw_wing(px, 21, 16, 11, 10, False, True)
# feathery antennae plane 3x7 at texOffs(0,32): top (7,32)
for x in range(7, 13):
    for y in range(32, 39):
        px[x, y] = cl((60 + n(10), 50, 38), 255 if (x + y) % 2 == 0 or x in (8, 11) else 0)
os.makedirs(f'{A}/textures/entity', exist_ok=True)
im.save(f'{A}/textures/entity/deathmoth.png')

# screen overlay: wings and hairy body pressed against the camera
S = 256
ov = Image.new('RGBA', (S, S), (0, 0, 0, 0)); op = ov.load()
for y in range(S):
    for x in range(S):
        dx, dy = (x - S / 2) / (S / 2), (y - S / 2) / (S / 2)
        ax = abs(dx)
        body = ax < 0.16 - 0.05 * dy and abs(dy) < 1.0
        fw = 1.02 * max(0.0, 1 - ((dy + 0.30) / 0.72) ** 2) ** 0.5 if dy < 0.32 else 0.0      # forewing lobe
        hw = 0.80 * max(0.0, 1 - ((dy - 0.55) / 0.42) ** 2) ** 0.5 if dy > 0.12 else 0.0      # hindwing lobe
        lim = max(fw, hw) * 0.97 + 0.012 * math.sin(dy * 23)
        if body:
            b = 46 + n(12) + (14 if (x * 5 + y * 3) % 7 == 0 else 0)
            op[x, y] = cl((b, b * 0.82, b * 0.62), 250)
        elif ax < lim:
            t = min(1, max(0, (ax - 0.12) / 0.9)); s = (dy + 1) / 2
            c = wing_color(t, s, dy < 0.25, False)
            edge = max(0.0, min(1.0, (lim - ax) * 10))
            op[x, y] = cl([v * 0.8 for v in c], int(240 * edge))
ov = ov.filter(ImageFilter.GaussianBlur(0.6))
os.makedirs(f'{A}/textures/gui', exist_ok=True)
ov.save(f'{A}/textures/gui/deathmoth_overlay.png')

w = lambda p, o: (os.makedirs(os.path.dirname(p), exist_ok=True), json.dump(o, open(p, 'w'), indent=1))
w(f'{A}/models/item/deathmoth_spawn_egg.json', {"parent": "minecraft:item/template_spawn_egg"})
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({"entity.office_rooms.deathmoth": "Deathmoth", "item.office_rooms.deathmoth_spawn_egg": "Deathmoth Spawn Egg",
          "subtitles.office_rooms.entity.deathmoth.idle": "Something rustles", "subtitles.office_rooms.entity.deathmoth.trigger": "Wings burst open",
          "subtitles.office_rooms.entity.deathmoth.flap": "Heavy wings flap", "subtitles.office_rooms.entity.deathmoth.attack": "Deathmoth screeches",
          "subtitles.office_rooms.entity.deathmoth.hurt": "Deathmoth hurts", "subtitles.office_rooms.entity.deathmoth.death": "Deathmoth dies"})
w(lp, L)
print("ok")
