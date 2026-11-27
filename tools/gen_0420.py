"""0.42.0: assets of the SS Fun =) and the departure gate (all drawn here, own designs, nothing copied).
Run from anywhere: python3 tools/gen_0420.py"""
import json, math, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(420)
T = f'{A}/textures/block'
os.makedirs(T, exist_ok=True); os.makedirs(f'{A}/textures/item', exist_ok=True)


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)


def n(a=5): return rnd.randint(-a, a)
def cl(c, a=255): return tuple(max(0, min(255, int(v))) for v in c[:3]) + ((c[3] if len(c) > 3 else a),)
def mix(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def img(wd, ht, fn):
    im = Image.new('RGBA', (wd, ht)); px = im.load()
    for y in range(ht):
        for x in range(wd):
            px[x, y] = cl(fn(x, y))
    return im


def tex(name, fn, folder='block', size=(16, 16)):
    im = img(size[0], size[1], fn); im.save(f'{A}/textures/{folder}/{name}.png'); return im


def grime(c, k=0.1, p=0.15):
    return tuple(v * (1 - k) for v in c) if rnd.random() < p else c


# ---- fonts ---------------------------------------------------------------------------------------------------------
F5 = {
 'A': ['01110', '10001', '10001', '11111', '10001', '10001', '10001'], 'B': ['11110', '10001', '10001', '11110', '10001', '10001', '11110'],
 'D': ['11110', '10001', '10001', '10001', '10001', '10001', '11110'], 'E': ['11111', '10000', '10000', '11110', '10000', '10000', '11111'],
 'F': ['11111', '10000', '10000', '11110', '10000', '10000', '10000'], 'G': ['01110', '10001', '10000', '10111', '10001', '10001', '01111'],
 'I': ['01110', '00100', '00100', '00100', '00100', '00100', '01110'], 'N': ['10001', '10001', '11001', '10101', '10011', '10001', '10001'],
 'O': ['01110', '10001', '10001', '10001', '10001', '10001', '01110'], 'R': ['11110', '10001', '10001', '11110', '10100', '10010', '10001'],
 'S': ['01111', '10000', '10000', '01110', '00001', '00001', '11110'], 'T': ['11111', '00100', '00100', '00100', '00100', '00100', '00100'],
 'U': ['10001', '10001', '10001', '10001', '10001', '10001', '01110'], 'X': ['10001', '10001', '01010', '00100', '01010', '10001', '10001'],
 'E2': [], '2': ['01110', '10001', '00001', '00010', '00100', '01000', '11111'], '6': ['00110', '01000', '10000', '11110', '10001', '10001', '01110'],
 '=': ['00000', '00000', '11111', '00000', '11111', '00000', '00000'], ')': ['01000', '00100', '00010', '00010', '00010', '00100', '01000'],
 ' ': ['00000'] * 7, '!': ['00100', '00100', '00100', '00100', '00100', '00000', '00100'], 'K': ['10001', '10010', '10100', '11000', '10100', '10010', '10001'],
 'P': ['11110', '10001', '10001', '11110', '10000', '10000', '10000'], 'C': ['01110', '10001', '10000', '10000', '10000', '10001', '01110'],
 'L': ['10000', '10000', '10000', '10000', '10000', '10000', '11111'], 'M': ['10001', '11011', '10101', '10101', '10001', '10001', '10001'],
 'Y': ['10001', '10001', '01010', '00100', '00100', '00100', '00100'], 'H': ['10001', '10001', '10001', '11111', '10001', '10001', '10001']}


def text5(px, s, x0, y0, col, adv=6):
    for i, ch in enumerate(s):
        g = F5.get(ch)
        for r, row in enumerate(g or []):
            for k, b in enumerate(row):
                if b == '1' and 0 <= x0 + i * adv + k < 1000:
                    try:
                        px[x0 + i * adv + k, y0 + r] = col
                    except IndexError:
                        pass


# ---- hull ----------------------------------------------------------------------------------------------------------
WHITE = (226, 226, 218); NAVY = (30, 42, 72)


def hull(x, y):
    c = WHITE
    if y == 7 or x == 0:
        c = (196, 196, 188)                                     # plate seams
    if (x, y) in ((2, 2), (13, 2), (2, 12), (13, 12)):
        c = (180, 180, 172)                                     # rivets
    return grime(tuple(v + n(3) for v in c), 0.06, 0.2)
tex('ship_hull', hull)
tex('ship_hull_dark', lambda x, y: tuple(v + n(3) for v in ((22, 32, 58) if y == 7 or x == 0 else NAVY)))


def rust(x, y):
    c = hull(x, y)
    streak = (x * 7 + 3) % 5 == 0 or (x * 3) % 7 == 1
    if streak and rnd.random() < 0.9 - y * 0.04:
        c = mix(c, (150, 78, 34), 0.55 + rnd.random() * 0.3)
    return c
tex('ship_hull_rust', rust)


def smile32(bg, fg):
    """=) on a 32 x 32 field: two eye slits on the left, a stitched smile on the right."""
    im = Image.new('RGBA', (32, 32), cl(bg)); px = im.load()
    for y in range(32):
        for x in range(32):
            px[x, y] = cl(tuple(v + n(3) for v in bg))
    for (y0, y1) in ((9, 13), (19, 23)):                       # "="
        for y in range(y0, y1):
            for x in range(3, 12):
                px[x, y] = cl(fg)
    for a in range(-52, 53, 2):                                 # ")"
        t = math.radians(a)
        for r in (17.0, 18.0, 19.0, 19.8):
            x, y = int(5 + r * math.cos(t)), int(16 + r * math.sin(t))
            if 0 <= x < 32 and 0 <= y < 32:
                px[x, y] = cl(fg)
    for a in range(-45, 46, 15):                                # stitches across the smile
        t = math.radians(a)
        for r in (15.6, 21.2):
            x, y = int(5 + r * math.cos(t)), int(16 + r * math.sin(t))
            if 0 <= x < 32 and 0 <= y < 32:
                px[x, y] = cl(mix(fg, bg, 0.25))
    return im


def split4(im, name):
    for v, (cx, cy) in enumerate(((0, 0), (16, 0), (0, 16), (16, 16))):
        im.crop((cx, cy, cx + 16, cy + 16)).save(f'{T}/{name}_{v}.png')
split4(smile32(WHITE, NAVY), 'ship_smile')
split4(smile32((168, 32, 30), (236, 232, 220)), 'ship_funnel_smile')
tex('ship_funnel', lambda x, y: tuple(v + n(3) for v in ((140, 24, 22) if y % 8 == 0 else (168, 32, 30))))
tex('ship_funnel_top', lambda x, y: tuple(v + n(5) for v in ((26, 24, 24) if y > 3 else (60, 50, 46))))


def porthole(x, y):
    d = math.hypot(x - 7.5, y - 7.5)
    if d > 7.6:
        return (226, 226, 218, 255)
    if d > 5.8:
        return (150, 132, 80, 255) if (x + y) % 5 else (120, 104, 60, 255)   # brass ring with bolts
    g = (110, 140, 132)
    return cl(tuple(v + n(4) for v in g), 110 if d < 4 else 140)
tex('ship_porthole', porthole)
tex('ship_window', lambda x, y: (235, 235, 228, 255) if x in (0, 15) or y in (0, 15) else cl((108, 132, 126), 95 if (x - y) % 9 else 130))

# ---- passenger interior ----------------------------------------------------------------------------------------------
tex('ship_wall_panel', lambda x, y: tuple(v + n(2) for v in ((188, 174, 146) if x == 0 else (208, 194, 166) if (x * 3 + y // 5) % 11 else (200, 186, 158))))


def wainscot(x, y):
    if y <= 1:
        return (96, 58, 32)                                     # rail along the top
    c = (124, 78, 44) if (y // 3) % 2 else (112, 70, 38)
    if x == 0:
        c = (84, 50, 26)
    return tuple(v + n(4) for v in c)
tex('ship_wainscot', wainscot)
tex('ship_wood_panel', lambda x, y: tuple(v + n(5) for v in ((176, 98, 44) if (x + (y // 4) % 2) % 4 else (156, 84, 36))))
tex('ship_teal_trim', lambda x, y: (190, 158, 70) if y in (3, 12) else tuple(v + n(3) for v in (34, 112, 108)))
tex('ship_ceiling', lambda x, y: tuple(v + n(2) for v in ((206, 196, 172) if x == 0 or y == 0 else (226, 216, 192))))


def gold(x, y):
    edge = min(x, y, 15 - x, 15 - y)
    c = (150, 112, 40) if edge == 0 else (178, 138, 56) if edge == 1 else (212, 170, 78)
    if edge > 2 and (x * 5 + y * 3) % 7 == 0:
        c = (232, 196, 110)                                     # hammered shimmer
    return tuple(v + n(4) for v in c)
tex('ship_gold_ceiling', gold)


def lamp(lit):
    def f(x, y):
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 4.5:
            return (255, 246, 214) if lit else (150, 146, 136)
        if d < 5.8:
            return (210, 196, 150) if lit else (120, 116, 106)
        return tuple(v + n(2) for v in (226, 216, 192))
    return f
tex('ship_ceiling_light', lamp(True)); tex('ship_ceiling_light_off', lamp(False))


def crew_lamp(lit):
    def f(x, y):
        if 2 <= x <= 13 and 5 <= y <= 10:
            return (255, 150, 60) if lit else (96, 70, 50)
        return tuple(v + n(2) for v in (220, 220, 214))
    return f
tex('ship_crew_light', crew_lamp(True)); tex('ship_crew_light_off', crew_lamp(False))


def carpet(base, line, kind):
    def f(x, y):
        c = base
        if kind == 'waves' and (y - int(2.2 * math.sin((x + 3) * 0.78))) % 8 == 0:
            c = line
        if kind == 'waves' and (y - int(2.2 * math.sin((x + 9) * 0.78)) - 4) % 8 == 0 and x % 3:
            c = mix(base, line, 0.45)
        if kind == 'stars':
            sx, sy = x % 8, y % 8
            if (sx == 3 and 1 <= sy <= 5) or (sy == 3 and 1 <= sx <= 5) or (abs(sx - 3) == 1 and abs(sy - 3) == 1):
                c = line
            if y in (0, 15):
                c = mix(line, base, 0.3)
        if kind == 'swirl':
            for cx, cy in ((4, 4), (12, 12)):
                d = math.hypot(x - cx, y - cy)
                if 2.4 < d < 3.4:
                    c = line
            if (x + y) % 16 == 8 and x % 4 == 0:
                c = (196, 110, 80)
        if kind == 'diamond' and ((x + y) % 8 == 0 or (x - y) % 8 == 0):
            c = line
        return tuple(v + n(5) for v in c)
    return f
tex('ship_carpet', carpet((30, 48, 98), (196, 158, 70), 'waves'))
tex('ship_carpet_stars', carpet((24, 38, 82), (204, 168, 76), 'stars'))
tex('ship_carpet_teal', carpet((52, 112, 96), (206, 172, 84), 'swirl'))
tex('ship_carpet_red', carpet((112, 24, 30), (188, 146, 62), 'diamond'))
tex('ship_carpet_brown', lambda x, y: tuple(v + n(7) for v in (150, 106, 58)))
tex('ship_carpet_wet', lambda x, y: (70, 92, 140) if rnd.random() < 0.05 else tuple(v + n(4) for v in (20, 32, 66)))


def marble(x, y):
    q = ((x + y) // 8 + (x - y + 16) // 8) % 2
    c = (112, 66, 40) if q else (216, 190, 150)
    if (x + y) % 8 == 0 or (x - y) % 8 == 0:
        c = (196, 156, 72)
    return tuple(v + n(5) for v in c)
tex('ship_marble', marble)
tex('ship_teak', lambda x, y: (40, 30, 22) if y % 4 == 3 else tuple(v + n(5) for v in ((156, 114, 72) if (x + (y // 4) * 5) % 16 else (132, 96, 58))))
tex('ship_pool_tile', lambda x, y: (176, 210, 210) if x % 4 == 0 or y % 4 == 0 else tuple(v + n(5) for v in (64, 170, 182)))
tex("ship_brass_column", lambda x, y: tuple(v + n(4) for v in ((206, 162, 72) if (y // 4) % 2 else (124, 70, 34))))
tex('ship_curtain', lambda x, y: tuple(v + n(3) for v in ((120, 16, 22) if x % 4 in (1, 2) else (84, 10, 16))))

# ---- crew areas ------------------------------------------------------------------------------------------------------
def steel_wall(x, y):
    c = (226, 228, 224)
    if x in (7, 8):
        c = (212, 214, 210)                                     # stiffener
    if y % 8 == 3 and x % 4 == 1:
        c = (196, 198, 194)                                     # rivets
    return grime(tuple(v + n(2) for v in c), 0.08, 0.1)
tex('ship_steel_wall', steel_wall)
tex('ship_steel_floor', lambda x, y: tuple(v + n(4) for v in ((122, 128, 128) if (x + y) % 4 == 0 and (x - y) % 4 == 0 else (98, 104, 104))))


def frame(x, y):
    c = (224, 224, 218)
    if ((x - 7.5) / 5.6) ** 2 + ((y - 7.5) / 3.1) ** 2 < 1:
        c = (64, 64, 60)                                        # one oval cut-out per plate
    if y in (0, 15):
        c = (204, 204, 198)
    if x == 0:
        c = (196, 196, 190)
    return tuple(v + n(2) for v in c)
tex('ship_steel_frame', frame)
tex('ship_grate', lambda x, y: (112, 116, 116, 255) if x % 4 == 0 or y % 4 == 0 else (0, 0, 0, 0))


def pipes(x, y):
    band = y // 4
    cols = [(120, 124, 128), (60, 92, 130), (88, 120, 80), (150, 60, 50)]
    c = cols[band]
    if y % 4 == 0:
        c = (50, 50, 52)
    elif y % 4 == 1:
        c = mix(c, (255, 255, 255), 0.25)
    if x % 8 == 5:
        c = (180, 180, 170)                                     # clamps
    return tuple(v + n(3) for v in c)
tex('ship_pipes', pipes)
tex('ship_engine', lambda x, y: (150, 120, 40) if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)) else
    tuple(v + n(4) for v in ((70, 74, 78) if x in (0, 15) or y in (0, 15) else (92, 98, 104))))
tex('ship_engine_top', lambda x, y: tuple(v + n(4) for v in ((60, 62, 64) if math.hypot(x - 7.5, y - 7.5) < 5 else (110, 116, 120))))
tex('ship_vent', lambda x, y: (34, 34, 36) if y % 3 == 1 and 1 < x < 14 else tuple(v + n(2) for v in (170, 172, 170)))
tex('ship_vent_broken', lambda x, y: (6, 6, 8) if 2 < x < 13 and 2 < y < 13 else (150, 150, 146) if (x + y) % 3 else (90, 90, 88))

# ---- doors (bottom / top halves) ------------------------------------------------------------------------------------
def door(top, base, kind):
    def f(x, y):
        c = base
        if x in (0, 15) or (y == 15 and not top) or (y == 0 and top):
            c = mix(base, (0, 0, 0), 0.25)
        if kind == 'cabin':
            if x in (2, 13) and y % 3 == 1:
                c = mix(base, (0, 0, 0), 0.18)                  # rivet rows
            if top and math.hypot(x - 7.5, y - 7) < 2.6:
                c = (40, 48, 60) if math.hypot(x - 7.5, y - 7) < 1.8 else (150, 130, 80)   # round number plate
            if not top and x == 12 and 1 <= y <= 3:
                c = (170, 170, 168)                             # handle
        if kind == 'crew':
            if top and 4 <= x <= 11 and 3 <= y <= 9:
                c = (90, 116, 116) if not (x in (4, 11) or y in (3, 9)) else (160, 160, 156)
            if not top and x == 12 and 0 <= y <= 2:
                c = (90, 90, 90)
        if kind == 'fire':
            if not top and 8 <= y <= 10:
                c = (170, 30, 26)
            if top and 6 <= y <= 8:
                c = (220, 190, 40)
            if not top and x == 12 and 0 <= y <= 2:
                c = (40, 40, 40)
        return tuple(v + n(2) for v in c)
    return f
for name, base, kind in (('ship_cabin_door', (226, 220, 204), 'cabin'), ('ship_crew_door', (214, 216, 212), 'crew'), ('ship_fire_door', (120, 124, 124), 'fire')):
    tex(name + '_bottom', door(False, base, kind)); tex(name + '_top', door(True, base, kind))
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    b = Image.open(f'{T}/{name}_bottom.png'); t = Image.open(f'{T}/{name}_top.png')
    im.paste(t.resize((8, 8)), (4, 0)); im.paste(b.resize((8, 8)), (4, 8)); im.save(f'{A}/textures/item/{name}.png')

# ---- small textures for decor models ----------------------------------------------------------------------------------
def flat(name, c, a=4): tex(name, lambda x, y: tuple(v + n(a) for v in c))
flat('ship_rail_white', (232, 232, 228), 2); flat('ship_rail_brass', (196, 152, 62), 6); flat('ship_rail_wood', (130, 72, 34), 6)
tex('ship_life_ring', lambda x, y: (0, 0, 0, 0) if not (3.0 < math.hypot(x - 7.5, y - 7.5) < 7.2) else
    ((236, 232, 224, 255) if (int(math.degrees(math.atan2(y - 7.5, x - 7.5)) + 180) // 45) % 2 else (230, 96, 32, 255)))


def exit_sign(x, y):
    c = (20, 150, 70)
    man = {(5, 4), (5, 5), (4, 6), (5, 6), (6, 6), (5, 7), (4, 8), (6, 8), (3, 9), (7, 9)}
    if (x, y) in man or (y == 6 and 9 <= x <= 12) or (x == 11 and y in (5, 7)):
        c = (235, 255, 235)
    return c
tex('ship_exit_sign', exit_sign)
tex('ship_sconce', lambda x, y: (255, 230, 170) if 5 <= x <= 10 and 3 <= y <= 9 else tuple(v + n(5) for v in (190, 150, 64)))
tex('ship_fire_hose', lambda x, y: (60, 30, 30) if 3 < math.hypot(x - 7.5, y - 7.5) < 5 else (170, 28, 24) if x in (0, 15) or y in (0, 15)
    else (200, 40, 34) if not (2 <= x <= 13 and 2 <= y <= 13) else (150, 30, 26))
flat('ship_plastic_white', (232, 232, 226), 3); flat('ship_cabinet_red', (176, 30, 34), 6); flat('ship_cabinet_gold', (200, 160, 70), 6)


def reels(x, y):
    if y < 3 or y > 12:
        return (176, 30, 34)
    if x in (0, 5, 10, 15):
        return (40, 30, 20)
    return (250, 244, 220) if (x % 5, y) not in ((2, 6), (2, 7), (3, 6), (2, 9)) else (200, 30, 30)
tex('ship_slot_screen', reels)
flat('ship_steel', (176, 180, 182), 4); tex('ship_glass_guard', lambda x, y: (220, 240, 240, 80) if x not in (0, 15) else (200, 200, 200, 200))
tex('ship_washer_front', lambda x, y: (40, 50, 60) if math.hypot(x - 7.5, y - 8.5) < 3.5 else (170, 170, 166) if math.hypot(x - 7.5, y - 8.5) < 5
    else (226, 226, 222) if y > 2 else (140, 150, 160))
tex('ship_elevator_door', lambda x, y: (120, 90, 40) if x in (7, 8) else tuple(v + n(4) for v in ((206, 166, 82) if y % 6 else (180, 140, 60))))
flat('ship_suitcase', (40, 56, 96), 4); flat('ship_strap', (120, 80, 40), 4)
tex('ship_console_top', lambda x, y: (20, 30, 30) if 2 <= x <= 13 and 1 <= y <= 6 else ((60, 220, 120) if (x * 3 + y) % 7 == 0 else (220, 60, 40) if (x + y * 5) % 11 == 0 else (90, 96, 100)))
flat('ship_console', (80, 86, 92), 4)


def poster(x, y):
    c = (232, 196, 70)
    if x in (0, 15) or y in (0, 15):
        c = (200, 60, 50)
    if 3 <= x <= 12 and 2 <= y <= 10 and math.hypot(x - 7.5, y - 6) < 4.8:
        c = (250, 236, 180)                                     # the mascot's face
        if y in (4, 6) and 4 <= x <= 6:
            c = (30, 30, 40)                                    # =
        if 9 <= x <= 10 and 3 <= y <= 8 and abs(y - 5.5) > (x - 9) * 1.5:
            c = (30, 30, 40)                                    # )
    if y >= 12 and 2 <= x <= 13 and (x + y) % 2 == 0:
        c = (200, 60, 50)
    return c
tex('ship_poster', poster)
flat('ship_lifeboat', (236, 110, 30), 6); tex('ship_lifeboat_canopy', lambda x, y: (236, 110, 30) if y > 11 else tuple(v + n(3) for v in (236, 234, 226)))
tex('ship_release', lambda x, y: (220, 200, 40) if y < 3 else (190, 30, 28) if not (6 <= x <= 9) else (40, 40, 40))
flat('terminal_carpet_base', (70, 82, 96), 5)
tex('terminal_carpet', lambda x, y: (98, 110, 126) if (x % 4 == 0 and y % 4 == 0) else tuple(v + n(5) for v in (66, 78, 92)))
def term_window(x, y):
    if x in (0, 15) or y in (0, 15):
        return (34, 36, 38)                                     # mullion
    c = mix((150, 172, 162), (98, 122, 114), y / 15)
    if y == 10:
        c = mix(c, (70, 92, 86), 0.5)                           # the horizon, barely
    if 5 <= x <= 11 and 7 <= y <= 9 and not (x == 5 and y == 7):
        c = mix(c, (92, 108, 102), 0.45)                        # something out there in the fog
    if x == 8 and 4 <= y <= 6:
        c = mix(c, (92, 108, 102), 0.35)
    return tuple(v + n(2) for v in c)
tex('terminal_window', term_window)
tex('gate_desk', lambda x, y: (40, 60, 110) if y > 4 else (180, 186, 190))
tex('gate_seat', lambda x, y: tuple(v + n(4) for v in (40, 70, 140)))


def board():
    im = Image.new('RGBA', (48, 16), (12, 12, 14, 255)); px = im.load()
    text5(px, 'SS FUN=)', 0, 1, (255, 180, 40, 255)); text5(px, 'BOARDING', 0, 9, (110, 255, 140, 255))
    for k in range(3):
        im.crop((k * 16, 0, k * 16 + 16, 16)).save(f'{T}/departure_board_{k}.png')
board()


def gate_top(x, y):
    im = Image.new('RGBA', (16, 16), (60, 64, 70, 255))
    return (60, 64, 70)
im = Image.new('RGBA', (16, 16), (52, 56, 62, 255)); px = im.load()
text5(px, '26', 2, 4, (255, 200, 60, 255), adv=7); im.save(f'{T}/boarding_gate_top.png')
tex('boarding_gate', lambda x, y: (52, 56, 62) if x in (0, 1, 14, 15) else (120, 150, 150, 255) if 3 <= y <= 12 and x not in (7, 8) else (90, 96, 104))

# ---- models ----------------------------------------------------------------------------------------------------------
M = f'{A}/models/block'; YR = {"north": 0, "east": 90, "south": 180, "west": 270}
FACES = ('north', 'south', 'east', 'west', 'up', 'down')


def el(f, t, tx, glow=0, **over):
    e = {"from": f, "to": t, "faces": {k: {"texture": "#" + over.get(k, tx)} for k in FACES}}
    if glow: e["forge_data"] = {"block_light": glow, "sky_light": glow}
    return e


def model(name, elems, texs, rt=None, parent="minecraft:block/block"):
    t = {k: (v if ':' in v else O + 'block/' + v) for k, v in texs.items()}; t['particle'] = t[next(iter(texs))]
    m = {"parent": parent, "textures": t}
    if elems is not None: m["elements"] = elems
    if rt: m["render_type"] = rt
    w(f'{M}/{name}.json', m)


def item_model(name, model_name=None):
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + (model_name or name)})


def cube(name, texture=None, glow=0, rt=None):
    texture = texture or name
    model(name, [el([0, 0, 0], [16, 16, 16], 'a', glow=glow)], {'a': texture}, rt=rt)
    w(f'{A}/blockstates/{name}.json', {"variants": {"": {"model": O + "block/" + name}}})
    item_model(name)


def facing_state(name, models=None, extra=None):
    var = {}
    for d in YR:
        if extra:
            for k, mdl in extra.items():
                var[f"facing={d},{k}"] = {"model": O + "block/" + mdl, "y": YR[d]}
        else:
            var[f"facing={d}"] = {"model": O + "block/" + (models or name), "y": YR[d]}
    w(f'{A}/blockstates/{name}.json', {"variants": var})


FULL = ['ship_hull', 'ship_hull_dark', 'ship_hull_rust', 'ship_funnel', 'ship_funnel_top', 'ship_wall_panel', 'ship_wainscot', 'ship_wood_panel',
        'ship_teal_trim', 'ship_ceiling', 'ship_carpet', 'ship_carpet_stars', 'ship_carpet_teal', 'ship_carpet_red', 'ship_carpet_brown',
        'ship_carpet_wet', 'ship_marble', 'ship_teak', 'ship_pool_tile', 'ship_brass_column', 'ship_curtain', 'ship_steel_wall', 'ship_steel_floor',
        'ship_steel_frame', 'ship_pipes', 'ship_engine', 'ship_lifeboat', 'ship_lifeboat_canopy', 'terminal_carpet', 'ship_vent', 'ship_vent_broken']
for nme in FULL:
    cube(nme)
cube('ship_engine_top')
model('ship_engine_top', [el([0, 0, 0], [16, 16, 16], 's', up='t')], {'s': 'ship_engine', 't': 'ship_engine_top'})
cube('ship_gold_ceiling', glow=7)
cube('terminal_window', glow=6)
cube('ship_porthole', rt='minecraft:translucent'); cube('ship_window', rt='minecraft:translucent'); cube('ship_grate', rt='minecraft:cutout')
# lamps
for nme in ('ship_ceiling_light', 'ship_crew_light'):
    model(nme, [el([0, 0, 0], [16, 16, 16], 'a', glow=15)], {'a': nme})
    model(nme + '_off', [el([0, 0, 0], [16, 16, 16], 'a')], {'a': nme + '_off'})
    w(f'{A}/blockstates/{nme}.json', {"variants": {"lit=true": {"model": O + "block/" + nme}, "lit=false": {"model": O + "block/" + nme + "_off"}}})
    item_model(nme)
# decals
for nme in ('ship_smile', 'ship_funnel_smile'):
    for v in range(4):
        model(f'{nme}_{v}', [el([0, 0, 0], [16, 16, 16], 'a')], {'a': f'{nme}_{v}'})
    w(f'{A}/blockstates/{nme}.json', {"variants": {f"variant={v}": {"model": O + f"block/{nme}_{v}"} for v in range(4)}})
    item_model(nme, nme + '_0')
# stairs
for nme, top, side in (('ship_carpet_stairs', 'ship_carpet', 'ship_wood_panel'), ('ship_teal_stairs', 'ship_carpet_teal', 'ship_wood_panel'),
                       ('ship_steel_stairs', 'ship_steel_floor', 'ship_steel_floor')):
    for kind, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
        w(f'{M}/{nme}{kind}.json', {"parent": f"minecraft:block/{parent}", "textures": {"bottom": O + "block/" + side, "top": O + "block/" + top, "side": O + "block/" + side}})
    var = {}
    for f in ('east', 'north', 'south', 'west'):
        base = {'east': 0, 'south': 90, 'west': 180, 'north': 270}[f]
        for half in ('bottom', 'top'):
            for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
                mdl = O + 'block/' + nme + ('' if shape == 'straight' else '_inner' if 'inner' in shape else '_outer')
                y = base
                if shape in ('inner_left', 'outer_left'):
                    y = (y - 90) % 360
                v = {"model": mdl, "uvlock": True}
                if half == 'top':
                    v["x"] = 180
                    if shape != 'straight':
                        y = (y + 90) % 360
                if y: v["y"] = y
                var[f"facing={f},half={half},shape={shape}"] = v
    w(f'{A}/blockstates/{nme}.json', {"variants": var})
    item_model(nme)
# doors
DOOR_Y = {('east', False, 'left'): 0, ('east', True, 'left'): 90, ('east', False, 'right'): 0, ('east', True, 'right'): 270,
          ('north', False, 'left'): 270, ('north', True, 'left'): 0, ('north', False, 'right'): 270, ('north', True, 'right'): 180,
          ('south', False, 'left'): 90, ('south', True, 'left'): 180, ('south', False, 'right'): 90, ('south', True, 'right'): 0,
          ('west', False, 'left'): 180, ('west', True, 'left'): 270, ('west', False, 'right'): 180, ('west', True, 'right'): 90}
for nme in ('ship_cabin_door', 'ship_crew_door', 'ship_fire_door'):
    for half in ('bottom', 'top'):
        for hinge in ('left', 'right'):
            for op in ('', '_open'):
                w(f'{M}/{nme}_{half}_{hinge}{op}.json', {"parent": f"minecraft:block/door_{half}_{hinge}{op}",
                  "textures": {"bottom": O + f"block/{nme}_bottom", "top": O + f"block/{nme}_top"}, "render_type": "minecraft:cutout"})
    var = {}
    for (f, op, hinge), y in DOOR_Y.items():
        for half, mh in (('lower', 'bottom'), ('upper', 'top')):
            v = {"model": O + f"block/{nme}_{mh}_{hinge}{'_open' if op else ''}"}
            if y: v["y"] = y
            var[f"facing={f},half={half},hinge={hinge},open={'true' if op else 'false'}"] = v
    w(f'{A}/blockstates/{nme}.json', {"variants": var})
    w(f'{A}/models/item/{nme}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "item/" + nme}})
# fences (railings)
for nme, t in (('ship_railing', 'ship_rail_white'), ('ship_brass_railing', 'ship_rail_brass')):
    model(nme + '_post', None, {'texture': t}, parent="minecraft:block/fence_post")
    model(nme + '_side', None, {'texture': t}, parent="minecraft:block/fence_side")
    model(nme + '_inventory', None, {'texture': t}, parent="minecraft:block/fence_inventory")
    mp = [{"apply": {"model": O + f"block/{nme}_post"}}]
    for d, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270)):
        a = {"model": O + f"block/{nme}_side", "uvlock": True}
        if y: a["y"] = y
        mp.append({"when": {d: "true"}, "apply": a})
    w(f'{A}/blockstates/{nme}.json', {"multipart": mp})
    item_model(nme, nme + '_inventory')

# turned decor: (elements, textures, render type, light)
TURNED = {
 'ship_handrail': ([el([0, 8, 13], [16, 10, 15], 'w'), el([2, 6, 15], [3, 9, 16], 'w'), el([13, 6, 15], [14, 9, 16], 'w')], {'w': 'ship_rail_wood'}, None),
 'ship_life_ring': ([el([1, 1, 15], [15, 15, 16], 'r')], {'r': 'ship_life_ring'}, 'minecraft:cutout'),
 'ship_exit_sign': ([el([1, 4, 14], [15, 12, 16], 's', glow=10, south='s')], {'s': 'ship_exit_sign'}, None),
 'ship_sconce': ([el([6, 4, 14], [10, 12, 16], 'b'), el([5, 8, 11], [11, 13, 14], 'l', glow=13)], {'b': 'ship_cabinet_gold', 'l': 'ship_sconce'}, None),
 'ship_fire_hose': ([el([1, 0, 9], [15, 14, 16], 'f', north='h')], {'f': 'ship_cabinet_red', 'h': 'ship_fire_hose'}, None),
 'ship_deck_chair': ([el([2, 3, 0], [14, 5, 12], 'p'), el([2, 5, 11], [14, 13, 13], 'p'), el([2, 0, 1], [3, 3, 2], 'p'), el([13, 0, 1], [14, 3, 2], 'p'),
                      el([2, 0, 10], [3, 3, 11], 'p'), el([13, 0, 10], [14, 3, 11], 'p')], {'p': 'ship_plastic_white'}, None),
 'ship_slot_machine': ([el([1, 0, 2], [15, 16, 15], 'c', north='g'), el([2, 10, 0], [14, 11, 2], 'g')], {'c': 'ship_cabinet_red', 'g': 'ship_cabinet_gold'}, None),
 'ship_slot_machine_top': ([el([1, 0, 3], [15, 12, 15], 'c', north='s', glow=0), el([2, 2, 2.9], [14, 11, 3], 's', glow=12), el([12, 6, 0], [13, 12, 3], 'g')],
                           {'c': 'ship_cabinet_red', 's': 'ship_slot_screen', 'g': 'ship_cabinet_gold'}, None),
 'ship_buffet_counter': ([el([0, 0, 0], [16, 11, 16], 's'), el([0, 11, 6], [16, 16, 7], 'g')], {'s': 'ship_steel', 'g': 'ship_glass_guard'}, 'minecraft:translucent'),
 'ship_washer': ([el([1, 0, 1], [15, 16, 16], 'b', north='f')], {'b': 'ship_plastic_white', 'f': 'ship_washer_front'}, None),
 'ship_elevator_door': ([el([0, 0, 12], [16, 16, 16], 'd')], {'d': 'ship_elevator_door'}, None),
 'ship_suitcase': ([el([3, 0, 5], [13, 11, 11], 's'), el([7, 11, 7], [9, 12, 9], 't'), el([3, 5, 4.9], [13, 6, 11.1], 't')], {'s': 'ship_suitcase', 't': 'ship_strap'}, None),
 'ship_console': ([el([0, 0, 2], [16, 10, 16], 'b'), el([0, 10, 4], [16, 12, 16], 'b', up='t')], {'b': 'ship_console', 't': 'ship_console_top'}, None),
 'ship_poster': ([el([1, 0, 15], [15, 16, 16], 'p')], {'p': 'ship_poster'}, None),
 'ship_lifeboat_release': ([el([3, 0, 10], [13, 14, 16], 'r'), el([7, 6, 6], [9, 8, 10], 'h'), el([6, 2, 5], [10, 8, 6], 'h')], {'r': 'ship_release', 'h': 'ship_cabinet_red'}, None),
 'boarding_gate': ([el([0, 0, 13], [16, 16, 16], 'g')], {'g': 'boarding_gate'}, None),
 'boarding_gate_top': ([el([0, 0, 13], [16, 16, 16], 'g', glow=8)], {'g': 'boarding_gate_top'}, None),
 'gate_desk': ([el([0, 0, 0], [16, 16, 16], 'd')], {'d': 'gate_desk'}, None),
 'gate_seats': ([el([0, 0, 2], [16, 1, 14], 's'), el([1, 4, 2], [7, 6, 12], 's'), el([9, 4, 2], [15, 6, 12], 's'), el([1, 6, 12], [7, 13, 14], 's'),
                 el([9, 6, 12], [15, 13, 14], 's'), el([7, 1, 7], [9, 4, 9], 's')], {'s': 'gate_seat'}, None),
}
for nme, (elems, texs, rt) in TURNED.items():
    model(nme, elems, texs, rt=rt)
    facing_state(nme)
    item_model(nme)
# departure board: three parts
for k in range(3):
    model(f'departure_board_{k}', [el([0, 0, 14], [16, 16, 16], 'b', glow=12)], {'b': f'departure_board_{k}'})
facing_state('departure_board', extra={f"part={k}": f"departure_board_{k}" for k in range(3)})
item_model('departure_board', 'departure_board_1')

ALL = (FULL + ['ship_engine_top', 'ship_gold_ceiling', 'terminal_window', 'ship_porthole', 'ship_window', 'ship_grate', 'ship_ceiling_light', 'ship_crew_light',
       'ship_smile', 'ship_funnel_smile', 'ship_carpet_stairs', 'ship_teal_stairs', 'ship_steel_stairs', 'ship_railing', 'ship_brass_railing', 'departure_board']
       + list(TURNED))

# ---- loot ------------------------------------------------------------------------------------------------------------
for nme in ALL:
    w(f'{D}/loot_tables/blocks/{nme}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + nme}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for nme in ('ship_cabin_door', 'ship_crew_door', 'ship_fire_door'):
    w(f'{D}/loot_tables/blocks/{nme}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + nme,
      "conditions": [{"condition": "minecraft:block_state_property", "block": O + nme, "properties": {"half": "lower"}}]}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def it(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ':' in name else O + name, "weight": weight}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def table(name, rolls, entries):
    w(f'{D}/loot_tables/chests/{name}.json', {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}]})
table('ship_cabin', (2, 5), [it('office_coin', 5, 1, 4), it('minecraft:paper', 4, 1, 3), it('minecraft:book', 2), it('almond_water', 3), it('minecraft:bread', 3, 1, 2),
      it('minecraft:leather', 2), it('bandage', 2), it('glowstick', 2), it('minecraft:clock', 1), it('minecraft:compass', 1), it('research_file', 1),
      it('minecraft:white_wool', 2, 1, 3), it('minecraft:name_tag', 1)])
table('ship_cabin_good', (3, 6), [it('office_coin', 5, 2, 6), it('research_file', 3), it('minecraft:gold_ingot', 3, 1, 3), it('minecraft:diamond', 1),
      it('almond_water', 3, 1, 2), it('bandage', 3), it('master_key', 1), it('portable_battery', 2), it('industrial_flashlight', 1), it('totem_of_mercy', 1)])
table('ship_crew', (2, 5), [it('minecraft:iron_ingot', 4, 1, 3), it('electronic_component', 3, 1, 2), it('bandage', 3), it('almond_water', 3), it('portable_battery', 2),
      it('minecraft:paper', 3, 1, 4), it('office_coin', 3, 1, 3), it('emergency_flare', 2), it('gas_filter', 2), it('research_file', 1)])
table('ship_stores', (3, 7), [it('minecraft:bread', 4, 1, 4), it('minecraft:dried_kelp', 4, 2, 6), it('minecraft:cooked_cod', 3, 1, 3), it('almond_water', 4, 1, 2),
      it('minecraft:rope' if False else 'minecraft:string', 3, 2, 5), it('minecraft:iron_nugget', 3, 3, 9), it('minecraft:coal', 3, 1, 4),
      it('minecraft:white_wool', 2, 1, 4), it('emergency_flare', 1), it('electronic_component', 1)])
table('ship_purser', (3, 6), [it('office_coin', 6, 3, 10), it('ticket_stub', 4, 1, 3), it('minecraft:paper', 4, 2, 6), it('research_file', 3, 1, 2),
      it('master_key', 1), it('minecraft:gold_nugget', 3, 2, 8), it('minecraft:writable_book', 2)])
table('ship_casino', (4, 8), [it('office_coin', 10, 4, 16), it('minecraft:gold_nugget', 6, 3, 12), it('minecraft:gold_ingot', 3, 1, 4), it('minecraft:emerald', 2, 1, 3),
      it('minecraft:diamond', 1)])
table('ship_galley', (3, 6), [it('minecraft:bread', 4, 1, 4), it('minecraft:cake', 1), it('minecraft:cooked_salmon', 3, 1, 3), it('minecraft:cookie', 3, 2, 8),
      it('minecraft:apple', 3, 1, 3), it('almond_water', 3, 1, 3), it('minecraft:pumpkin_pie', 2), it('minecraft:honey_bottle', 1)])
table('ship_kids', (2, 5), [it('minecraft:cookie', 4, 1, 6), it('office_coin', 3, 1, 4), it('minecraft:paper', 3, 1, 3), it('chalk', 3, 1, 3), it('noise_maker', 2),
      it('glowstick', 3, 1, 3), it('minecraft:red_dye', 2), it('minecraft:yellow_dye', 2), it('minecraft:cake', 1)])
table('ship_captain', (3, 6), [it('research_file', 4, 1, 3), it('minecraft:map', 3), it('minecraft:compass', 3), it('minecraft:spyglass', 2), it('master_key', 2),
      it('minecraft:gold_ingot', 3, 1, 4), it('office_coin', 3, 3, 10), it('temporal_watch', 1), it('anomaly_detector', 1)])
table('ship_medical', (2, 5), [it('bandage', 6, 1, 4), it('almond_water', 4, 1, 3), it('minecraft:golden_apple', 1), it('minecraft:glass_bottle', 3, 1, 3),
      it('minecraft:paper', 2, 1, 3), it('liquid_pain', 1)])
table('departure_gate', (2, 4), [it('ticket_stub', 4, 1, 2), it('office_coin', 4, 1, 4), it('minecraft:paper', 3, 1, 3), it('almond_water', 3), it('minecraft:compass', 1)])

# ---- world data ------------------------------------------------------------------------------------------------------
w(f'{D}/dimension_type/the_sea.json', {"ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
  "ambient_light": 0.0, "fixed_time": 12700, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0, "piglin_safe": False, "bed_works": False,
  "respawn_anchor_works": False, "has_raids": False, "min_y": 0, "height": 128, "logical_height": 128, "infiniburn": "#minecraft:infiniburn_overworld",
  "effects": "office_rooms:the_sea"})
w(f'{D}/dimension/the_sea.json', {"type": "office_rooms:the_sea", "generator": {"type": "office_rooms:the_sea",
  "biome_source": {"type": "minecraft:fixed", "biome": "office_rooms:the_sea"}}})
w(f'{D}/worldgen/biome/the_sea.json', {"has_precipitation": False, "temperature": 0.5, "downfall": 0.4, "effects": {"fog_color": 0x66786E, "sky_color": 0x66786E,
  "water_color": 0x2E4A44, "water_fog_color": 0x1A2A28, "ambient_sound": "office_rooms:ambient.ship.hull"}, "spawners": {}, "spawn_costs": {}, "carvers": {},
  "features": []})
w(f'{D}/tags/worldgen/biome/has_structure/ss_fun.json', {"replace": False, "values": ["office_rooms:the_sea"]})
w(f'{D}/worldgen/structure/ss_fun.json', {"type": "office_rooms:ss_fun", "biomes": "#office_rooms:has_structure/ss_fun", "step": "surface_structures",
  "spawn_overrides": {}, "terrain_adaptation": "none"})
w(f'{D}/worldgen/structure_set/ss_fun.json', {"structures": [{"structure": "office_rooms:ss_fun", "weight": 1}], "placement": {"type": "office_rooms:ship_cell"}})
for m in ('ship_boarded', 'ship_escaped', 'room_departure_gate', 'mascot_kill'):
    w(f'{D}/advancements/milestones/{m}.json', {"criteria": {"done": {"trigger": "minecraft:impossible"}}})

# ---- lang ------------------------------------------------------------------------------------------------------------
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
NAMES = {'ship_hull': 'Ship Hull', 'ship_hull_dark': 'Dark Ship Hull', 'ship_hull_rust': 'Rusty Ship Hull', 'ship_smile': 'Hull Logo =)',
 'ship_funnel': 'Funnel Plating', 'ship_funnel_top': 'Sooty Funnel Plating', 'ship_funnel_smile': 'Funnel Logo =)', 'ship_porthole': 'Porthole',
 'ship_window': 'Ship Window', 'ship_wall_panel': 'Cabin Wall Panel', 'ship_wainscot': 'Wood Wainscot', 'ship_wood_panel': 'Stairwell Wood Panel',
 'ship_teal_trim': 'Teal Trim', 'ship_ceiling': 'Ship Ceiling', 'ship_gold_ceiling': 'Gold Ceiling Panel', 'ship_ceiling_light': 'Ship Ceiling Light',
 'ship_crew_light': 'Crew Corridor Light', 'ship_carpet': 'Wave Carpet', 'ship_carpet_stars': 'Star Carpet', 'ship_carpet_teal': 'Swirl Carpet',
 'ship_carpet_red': 'Red Carpet', 'ship_carpet_brown': 'Service Carpet', 'ship_carpet_wet': 'Soaked Carpet', 'ship_marble': 'Atrium Marble',
 'ship_teak': 'Teak Deck', 'ship_pool_tile': 'Ship Pool Tiles', 'ship_brass_column': 'Brass Column', 'ship_curtain': 'Velvet Curtain',
 'ship_carpet_stairs': 'Wave Carpet Stairs', 'ship_teal_stairs': 'Swirl Carpet Stairs', 'ship_steel_wall': 'Crew Steel Wall',
 'ship_steel_floor': 'Steel Deck Plate', 'ship_steel_frame': 'Steel Ceiling Frame', 'ship_grate': 'Steel Grating', 'ship_steel_stairs': 'Steel Stairs',
 'ship_pipes': 'Pipe Bundle', 'ship_engine': 'Ship Engine', 'ship_engine_top': 'Engine Cylinder Head', 'ship_vent': 'Ceiling Vent',
 'ship_vent_broken': 'Broken Vent', 'ship_cabin_door': 'Cabin Door', 'ship_crew_door': 'Crew Door', 'ship_fire_door': 'Fire Door',
 'ship_railing': 'Ship Railing', 'ship_brass_railing': 'Brass Railing', 'ship_handrail': 'Handrail', 'ship_life_ring': 'Life Ring',
 'ship_exit_sign': 'Emergency Exit Sign', 'ship_sconce': 'Brass Sconce', 'ship_fire_hose': 'Fire Hose Cabinet', 'ship_deck_chair': 'Deck Chair',
 'ship_slot_machine': 'Slot Machine', 'ship_slot_machine_top': 'Slot Machine Top', 'ship_buffet_counter': 'Buffet Counter', 'ship_washer': 'Washing Machine',
 'ship_elevator_door': 'Elevator Doors', 'ship_suitcase': 'Abandoned Suitcase', 'ship_console': 'Bridge Console', 'ship_poster': 'Kids\' Club Poster',
 'ship_lifeboat': 'Lifeboat Hull', 'ship_lifeboat_canopy': 'Lifeboat Canopy', 'ship_lifeboat_release': 'Lifeboat Release',
 'terminal_carpet': 'Terminal Carpet', 'terminal_window': 'Terminal Window', 'departure_board': 'Departure Board', 'boarding_gate': 'Boarding Gate',
 'boarding_gate_top': 'Gate Sign', 'gate_desk': 'Gate Desk', 'gate_seats': 'Gate Seats'}
for k, v in NAMES.items():
    lang[f'block.office_rooms.{k}'] = v
lang['entity.office_rooms.mascot'] = 'The Mascot'
lang['item.office_rooms.mascot_spawn_egg'] = 'The Mascot Spawn Egg'
w(f'{A}/models/item/mascot_spawn_egg.json', {"parent": "minecraft:item/template_spawn_egg"})
SUB = {'mascot.squeak': 'Something squeaks', 'mascot.giggle': 'Muffled giggling', 'mascot.honk': 'A honk from the ceiling', 'mascot.hurt': 'The Mascot squelches',
       'mascot.death': 'The Mascot deflates', 'mascot.step': 'Wet footsteps', 'ship.horn': 'A ship\'s horn', 'ship.hull': 'The hull creaks',
       'ship_lifeboat.drop': 'Lifeboat lowered'}
for k, v in SUB.items():
    lang[f'subtitles.office_rooms.{k}'] = v
w(L, lang)

# ---- sounds.json entries (the audio itself: tools/gen_sounds_0420.py) -----------------------------------------------
S = f'{A}/sounds.json'; snd = json.load(open(S))
def ev(key, cat, files, sub, dist=24, stream=False):
    e = {"category": cat, "subtitle": "subtitles.office_rooms." + sub, "sounds": [{"name": O + f, "attenuation_distance": dist, **({"stream": True} if stream else {})} for f in files]}
    snd[key] = e
ev('entity.mascot.squeak', 'hostile', ['entity/mascot/squeak1', 'entity/mascot/squeak2', 'entity/mascot/squeak3'], 'mascot.squeak', 20)
ev('entity.mascot.giggle', 'hostile', ['entity/mascot/giggle1', 'entity/mascot/giggle2'], 'mascot.giggle', 32)
ev('entity.mascot.honk', 'hostile', ['entity/mascot/honk'], 'mascot.honk', 48)
ev('entity.mascot.hurt', 'hostile', ['entity/mascot/hurt1', 'entity/mascot/hurt2'], 'mascot.hurt', 16)
ev('entity.mascot.death', 'hostile', ['entity/mascot/death'], 'mascot.death', 24)
ev('entity.mascot.step', 'hostile', ['entity/mascot/step1', 'entity/mascot/step2'], 'mascot.step', 12)
ev('ambient.ship.horn', 'ambient', ['ambient/ship/horn'], 'ship.horn', 96)
ev('ambient.ship.hull', 'ambient', ['ambient/ship/hull'], 'ship.hull', 16, stream=True)
ev('block.ship_lifeboat.drop', 'blocks', ['block/ship/lifeboat'], 'ship_lifeboat.drop', 32)
w(S, snd)
print('blocks with assets:', len(ALL) + 3)
