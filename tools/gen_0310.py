"""0.31.0: assets of the abandoned cinema (all drawn here, own designs, nothing copied)."""
import json, os, random, subprocess
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(310)
T = f'{A}/textures/block'
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def n(a=5): return rnd.randint(-a, a)
def cl(c, a=255): return tuple(max(0, min(255, int(v))) for v in c) + (a,)
def img(wd, ht, fn):
    im = Image.new('RGBA', (wd, ht)); px = im.load()
    for y in range(ht):
        for x in range(wd):
            v = fn(x, y); px[x, y] = v if len(v) == 4 else cl(v)
    return im
def tex(name, fn, folder='block'):
    img(16, 16, fn).save(f'{A}/textures/{folder}/{name}.png')
def flat(c, a=4): return lambda x, y: tuple(v + n(a) for v in c)
def dirt(c, x, y, k=0.12):
    """random grime: darker blotches"""
    return tuple(v * (1 - k * (rnd.random() < 0.18)) for v in c)

# ---- tiny fonts --------------------------------------------------------------------------------------------------
F3 = {'A': '010101111101101', 'E': '111100110100111', 'X': '101101010101101', 'I': '111010010010111', 'T': '111010010010010',
      'S': '011100010001110', 'Y': '101101010010010', 'H': '101101111101101', 'L': '100100100100111', 'O': '010101101101010',
      'K': '101101110101101', 'U': '101101101101111', 'P': '110101110100100', 'V': '101101101101010', '0': '111101101101111',
      'N': '111101101101101', 'W': '101101101111101', '?': '110001010000010'}
F5 = {
 'A': ['01110', '10001', '10001', '11111', '10001', '10001', '10001'], 'B': ['11110', '10001', '10001', '11110', '10001', '10001', '11110'],
 'C': ['01110', '10001', '10000', '10000', '10000', '10001', '01110'], 'D': ['11110', '10001', '10001', '10001', '10001', '10001', '11110'],
 'E': ['11111', '10000', '10000', '11110', '10000', '10000', '11111'], 'F': ['11111', '10000', '10000', '11110', '10000', '10000', '10000'],
 'G': ['01110', '10001', '10000', '10111', '10001', '10001', '01111'], 'H': ['10001', '10001', '10001', '11111', '10001', '10001', '10001'],
 'I': ['01110', '00100', '00100', '00100', '00100', '00100', '01110'], 'J': ['00111', '00010', '00010', '00010', '00010', '10010', '01100'],
 'K': ['10001', '10010', '10100', '11000', '10100', '10010', '10001'], 'L': ['10000', '10000', '10000', '10000', '10000', '10000', '11111'],
 'M': ['10001', '11011', '10101', '10101', '10001', '10001', '10001'], 'N': ['10001', '10001', '11001', '10101', '10011', '10001', '10001'],
 'O': ['01110', '10001', '10001', '10001', '10001', '10001', '01110'], 'P': ['11110', '10001', '10001', '11110', '10000', '10000', '10000'],
 'Q': ['01110', '10001', '10001', '10001', '10101', '10010', '01101'], 'R': ['11110', '10001', '10001', '11110', '10100', '10010', '10001'],
 'S': ['01111', '10000', '10000', '01110', '00001', '00001', '11110'], 'T': ['11111', '00100', '00100', '00100', '00100', '00100', '00100'],
 'U': ['10001', '10001', '10001', '10001', '10001', '10001', '01110'], 'V': ['10001', '10001', '10001', '10001', '10001', '01010', '00100'],
 'W': ['10001', '10001', '10001', '10101', '10101', '10101', '01010'], 'X': ['10001', '10001', '01010', '00100', '01010', '10001', '10001'],
 'Y': ['10001', '10001', '01010', '00100', '00100', '00100', '00100'], 'Z': ['11111', '00001', '00010', '00100', '01000', '10000', '11111'],
 '0': ['01110', '10001', '10011', '10101', '11001', '10001', '01110'], '1': ['00100', '01100', '00100', '00100', '00100', '00100', '01110'],
 '2': ['01110', '10001', '00001', '00010', '00100', '01000', '11111'], '3': ['11110', '00001', '00001', '01110', '00001', '00001', '11110'],
 '4': ['00010', '00110', '01010', '10010', '11111', '00010', '00010'], '5': ['11111', '10000', '11110', '00001', '00001', '10001', '01110'],
 '6': ['00110', '01000', '10000', '11110', '10001', '10001', '01110'], '7': ['11111', '00001', '00010', '00100', '01000', '01000', '01000'],
 '8': ['01110', '10001', '10001', '01110', '10001', '10001', '01110'], '9': ['01110', '10001', '10001', '01111', '00001', '00010', '01100']}
def text3(px, s, x0, y0, col):
    for i, ch in enumerate(s):
        g = F3.get(ch)
        if not g:
            continue
        for k, b in enumerate(g):
            if b == '1':
                px[x0 + i * 4 + k % 3, y0 + k // 3] = col

# ---- surfaces ------------------------------------------------------------------------------------------------------
def wall(x, y):  # quilted fabric panel, dull red, button every 8 px
    base = (92, 22, 30) if (x // 4) % 2 else (84, 19, 27)
    if (x % 8 == 4 and y % 8 == 4):
        base = (40, 10, 14)
    if x % 8 == 0:
        base = (60, 14, 20)
    return dirt(tuple(v + n(3) for v in base), x, y, 0.2)
tex('cinema_wall', wall)
def wallpaper_old(x, y):  # (replaced in 0.35.2 by a burgundy art-deco cinema wallpaper, drawn directly)
    cx, cy = x % 8, y % 8
    r = ((cx - 4) ** 2 + (cy - 8) ** 2) ** 0.5
    c = (34, 58, 60)
    if 3.2 < r < 4.2 or (cy > 4 and abs(cx - 4) == (8 - cy) // 2):
        c = (128, 104, 58)
    if rnd.random() < 0.04:
        c = (150, 140, 112)  # bare plaster
    return dirt(tuple(v + n(4) for v in c), x, y, 0.25)
# tex('cinema_wallpaper', wallpaper_old)  # 0.35.2: see NEW_IN_0.35.2
def carpet(x, y):  # dark purple carpet with faded retro squiggles
    c = (38, 22, 52)
    if (x + 2 * y) % 11 == 0 or (2 * x - y) % 13 == 0:
        c = (92, 70, 30)
    if (x * 3 + y) % 17 == 0:
        c = (40, 80, 92)
    return dirt(tuple(v + n(4) for v in c), x, y, 0.3)
tex('cinema_carpet', carpet)
tex('cinema_riser', lambda x, y: dirt(tuple(v + n(3) for v in ((28, 16, 40) if y > 1 else (60, 50, 30))), x, y, 0.2))
def tile(x, y):
    c = (196, 186, 160) if ((x // 8) + (y // 8)) % 2 == 0 else (30, 28, 28)
    if x % 8 == 0 or y % 8 == 0:
        c = (110, 104, 92)
    return dirt(tuple(v + n(5) for v in c), x, y, 0.15)
tex('cinema_tile', tile)
tex('cinema_ceiling', lambda x, y: (22, 22, 24) if (x % 4 == 1 and y % 4 == 2) else tuple(v + n(2) for v in (36, 35, 38)))

# screens: bright (rendered full-bright), grain and a seam
tex('cinema_screen', lambda x, y: tuple(v + n(6) for v in ((196, 196, 188) if x != 0 else (170, 170, 164))))
tex('cinema_screen_off', lambda x, y: tuple(v + n(3) for v in ((14, 14, 16) if (x + y) % 9 else (30, 30, 34))))
def torn(x, y):
    c = (120, 118, 112)
    if rnd.random() < 0.06 or (x + y == 17 and 4 < x < 13):
        c = (20, 18, 16)
    return dirt(tuple(v + n(6) for v in c), x, y, 0.3)
tex('cinema_screen_torn', torn)
# static: 8 animated frames
st = Image.new('RGBA', (16, 128)); spx = st.load()
for f in range(8):
    for y in range(16):
        for x in range(16):
            v = rnd.randint(40, 235)
            if (y + f * 3) % 7 == 0:
                v = min(255, v + 30)
            spx[x, y + f * 16] = (v, v, v, 255)
st.save(f'{T}/cinema_screen_static.png')
json.dump({"animation": {"frametime": 1}}, open(f'{T}/cinema_screen_static.png.mcmeta', 'w'))
# a thin dark figure standing on the screen (2 blocks)
def figure(upper):
    def fn(x, y):
        c = (196 + n(6), 196 + n(6), 188 + n(6))
        if upper:
            head = (x - 8) ** 2 / 4 + (y - 11) ** 2 / 9 < 1.0
            neck = 7 <= x <= 8 and y >= 13
            shoulders = y >= 15 and 4 <= x <= 11
            if head or neck or shoulders:
                return (10, 10, 12)
        else:
            body = 5 <= x <= 10 and y <= 6
            legs = (x in (6, 9) and y > 6)
            arms = (x in (3, 12) and y <= 9)
            if body or legs or arms:
                return (10, 10, 12)
        return c
    return fn
tex('cinema_screen_figure_lower', figure(False))
tex('cinema_screen_figure_upper', figure(True))

# seats
tex('cinema_seat_fabric', lambda x, y: dirt(tuple(v + n(5) for v in ((120, 18, 26) if y % 4 else (96, 14, 20))), x, y, 0.25))
tex('cinema_seat_torn', lambda x, y: (200, 186, 150) if rnd.random() < 0.25 else dirt(tuple(v + n(5) for v in (104, 16, 24)), x, y, 0.35))
tex('cinema_seat_frame', flat((40, 38, 42), 4))
# small lights and signs
tex('cinema_aisle_light', lambda x, y: (255, 200, 120) if 4 <= x <= 11 and 6 <= y <= 9 else (34, 32, 30))
def exit_sign():
    im = img(16, 16, lambda x, y: (24, 60, 32) if 1 <= y <= 9 else (40, 40, 40)); px = im.load()
    text3(px, 'EXIT', 0, 3, (180, 255, 190, 255))
    im.save(f'{T}/cinema_exit_sign.png')
exit_sign()
tex('cinema_projector', lambda x, y: (255, 248, 220) if (x - 8) ** 2 + (y - 8) ** 2 < 10 else (26, 26, 28) if (x - 8) ** 2 + (y - 8) ** 2 < 30 else (60, 58, 56))
tex('cinema_projector_off', lambda x, y: (40, 44, 48) if (x - 8) ** 2 + (y - 8) ** 2 < 10 else (26, 26, 28) if (x - 8) ** 2 + (y - 8) ** 2 < 30 else (60, 58, 56))
# lobby furniture
tex('cinema_counter_front', lambda x, y: (150, 120, 60) if y in (1, 14) else dirt(tuple(v + n(4) for v in ((110, 22, 30) if x % 5 else (80, 16, 22))), x, y, 0.2))
tex('cinema_counter_top', lambda x, y: tuple(v + n(4) for v in (150, 150, 152)))
tex('cinema_counter_side', lambda x, y: dirt(tuple(v + n(4) for v in (90, 20, 26)), x, y, 0.2))
tex('cinema_ticket_glass', lambda x, y: (120, 120, 120, 255) if x in (0, 15) or y in (0, 15) else (30, 30, 30, 0) if (x - 8) ** 2 + (y - 11) ** 2 < 5 else (170, 200, 205, 70 + n(10)))
def popcorn(x, y):
    if y < 3 or y > 12:
        return (150, 24, 28)
    if x in (0, 15):
        return (150, 24, 28)
    if y > 6:
        return (236, 214, 150) if rnd.random() < 0.8 else (250, 240, 200)
    return (190, 210, 214, 140)
tex('cinema_popcorn', popcorn)
tex('cinema_soda', lambda x, y: ((180, 40, 30) if x < 5 else (30, 90, 160) if x < 11 else (60, 150, 60)) if 3 <= y <= 6 else (40, 40, 44) if y in (8, 9) and x % 5 == 2 else tuple(v + n(5) for v in (168, 170, 174)))
tex('cinema_candy', lambda x, y: (120, 120, 120) if x in (0, 15) or y in (0, 15) else (rnd.choice([(200, 40, 60), (240, 200, 40), (60, 170, 80), (90, 60, 160), (240, 240, 240)]) if y % 5 in (2, 3) else (60, 70, 74)))
def menu(x, y):
    if x in (0, 15) or y in (0, 15):
        return (60, 50, 40)
    if y % 3 == 2 and 2 <= x <= 13 and not (x in (9, 10)) and rnd.random() < 0.85:
        return (230, 226, 200)
    return (16, 16, 18)
tex('cinema_menu', menu)
tex('cinema_brass', lambda x, y: tuple(v + n(8) for v in (170, 136, 60)))
tex('cinema_velvet', lambda x, y: tuple(v + n(6) for v in (130, 14, 26)))
def marquee(x, y):
    if x % 4 == 2 and y % 4 == 2:
        return (255, 210, 120) if rnd.random() < 0.85 else (70, 50, 30)
    return tuple(v + n(3) for v in (70, 14, 18))
tex('cinema_marquee', marquee)
tex('cinema_trash', lambda x, y: (236, 214, 150) if y < 3 and rnd.random() < 0.6 else tuple(v + n(6) for v in ((84, 86, 88) if x % 3 else (64, 66, 68))))
tex('cinema_spilled_popcorn', lambda x, y: (240, 222, 160, 255) if rnd.random() < 0.18 else (0, 0, 0, 0))
# marquee letters: warm bulbs on dark red, 5x7 glyphs scaled into the 16 px tile
GL = [chr(ord('A') + i) for i in range(26)] + [str(i) for i in range(10)]
for gi, ch in enumerate(GL):
    g = F5[ch]
    def fn(x, y, g=g):
        gx, gy = (x - 3) // 2, (y - 1) // 2
        if 3 <= x < 13 and 1 <= y < 15 and 0 <= gx < 5 and 0 <= gy < 7 and g[gy][gx] == '1':
            return (255, 214, 130) if (x + y) % 2 == 0 else (230, 170, 90)
        return tuple(v + n(2) for v in (54, 10, 14))
    tex(f'cinema_letter_{gi}', fn)
# posters 16x32, split into upper/lower
POSTERS = [('HALL', (150, 130, 60)), ('STAY', (30, 40, 70)), ('OPEN', (70, 20, 20)), ('EXIT', (20, 60, 30)), ('LV0', (170, 150, 70)), ('LOOK', (40, 40, 44))]
for pi, (title, col) in enumerate(POSTERS):
    im = Image.new('RGBA', (16, 32)); px = im.load()
    for y in range(32):
        for x in range(16):
            c = col
            if pi == 0 and y > 8:   # corridor in perspective
                d = abs(x - 7.5)
                c = (190, 170, 80) if d > (y - 8) / 4 else (60, 50, 20) if d < 1.5 and y < 20 else (210, 190, 100)
            if pi == 1 and 12 <= y <= 28 and 5 <= x <= 10:   # one door, ajar, black inside
                c = (12, 10, 10) if x > 7 else (110, 80, 50)
            if pi == 2 and y in (16, 17) and x in (4, 5, 10, 11):   # eyes in the dark
                c = (240, 240, 230)
            if pi == 3 and 14 <= y <= 18 and 3 <= x <= 12:        # an exit sign far away
                c = (60, 200, 90)
            if pi == 4 and y > 8:                                # yellow wallpaper
                c = (180, 160, 70) if (x + y) % 4 else (150, 130, 60)
            if pi == 5 and y > 8:                                # ceiling tiles, one missing, something inside
                c = (200, 200, 190) if x % 5 and y % 5 else (120, 120, 112)
                if 6 <= x <= 10 and 14 <= y <= 19:
                    c = (6, 6, 6) if not ((x, y) in ((7, 16), (9, 16))) else (230, 230, 200)
            fade = 0.75 + 0.25 * rnd.random()
            c = tuple(v * fade for v in c)
            if x in (0, 15) or y in (0, 31):
                c = (200, 190, 170)
            px[x, y] = cl(c)
    text3(px, title, 1, 2, (235, 228, 210, 255))
    if pi == 5:
        text3(px, 'UP', 5, 26, (235, 228, 210, 255))
    im.crop((0, 0, 16, 16)).save(f'{T}/cinema_poster_{pi}_upper.png')
    im.crop((0, 16, 16, 32)).save(f'{T}/cinema_poster_{pi}_lower.png')
# item
tex('ticket_stub', lambda x, y: (0, 0, 0, 0) if not (2 <= y <= 12 and 1 <= x <= 14) or (x in (1, 14) and y % 3 == 0)
    else (190, 50, 50) if y in (2, 12) else (230, 212, 170) if not (y == 7 and 3 <= x <= 12) else (120, 30, 30), 'item')

# ---- models --------------------------------------------------------------------------------------------------------
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
def el(f, t, tx, glow=0, **over):
    e = {"from": f, "to": t, "faces": {k: {"texture": "#" + over.get(k, tx)} for k in FACES}}
    if glow: e["forge_data"] = {"block_light": glow, "sky_light": glow}
    return e
M = f'{A}/models/block'; YR = {"north": 0, "east": 90, "south": 180, "west": 270}
def model(name, elems, texs, rt=None):
    t = {k: O + 'block/' + v for k, v in texs.items()}; t['particle'] = t[next(iter(texs))]
    m = {"parent": "minecraft:block/block", "textures": t, "elements": elems}
    if rt: m["render_type"] = rt
    w(f'{M}/{name}.json', m)
def facing_state(name, model_name=None, extra=''):
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={d}{extra}": {"model": O + "block/" + (model_name or name), "y": YR[d]} for d in YR}})
def item_model(name, model_name=None):
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + (model_name or name)})
def cube(name, texture=None, glow=0):
    texture = texture or name
    model(name, [el([0, 0, 0], [16, 16, 16], 'a', glow=glow)], {'a': texture})
    w(f'{A}/blockstates/{name}.json', {"variants": {"": {"model": O + "block/" + name}}})
    item_model(name)

FULL = {'cinema_wall': 0, 'cinema_wallpaper': 0, 'cinema_carpet': 0, 'cinema_tile': 0, 'cinema_ceiling': 0, 'cinema_riser': 0,
        'cinema_screen': 15, 'cinema_screen_off': 0, 'cinema_screen_torn': 0, 'cinema_screen_static': 15,
        'cinema_screen_figure_lower': 15, 'cinema_screen_figure_upper': 15, 'cinema_marquee': 15}
for nm, g in FULL.items():
    cube(nm, glow=g)
# stairs (vanilla template)
for kind, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
    w(f'{M}/cinema_riser_stairs{kind}.json', {"parent": f"minecraft:block/{parent}", "textures": {
        "bottom": O + "block/cinema_carpet", "top": O + "block/cinema_carpet", "side": O + "block/cinema_riser"}})
var = {}
for f in ('east', 'north', 'south', 'west'):
    base = {'east': 0, 'south': 90, 'west': 180, 'north': 270}[f]
    for half in ('bottom', 'top'):
        for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
            mdl = O + 'block/cinema_riser_stairs' + ('' if shape == 'straight' else '_inner' if 'inner' in shape else '_outer')
            y = base
            if shape in ('inner_left', 'outer_left'):
                y = (y - 90) % 360
            v = {"model": mdl, "uvlock": True}
            if half == 'top':
                v["x"] = 180
                if shape in ('inner_left', 'outer_left'):
                    y = (y + 90) % 360
                elif shape in ('inner_right', 'outer_right'):
                    y = (y + 90) % 360
            if y: v["y"] = y
            if not v.get("y") and not v.get("x"): v.pop("uvlock")
            var[f"facing={f},half={half},shape={shape}"] = v
w(f'{A}/blockstates/cinema_riser_stairs.json', {"variants": var})
item_model('cinema_riser_stairs')

TURNED = {
 'cinema_seat': ([el([1, 0, 3], [15, 5, 12], 'fr'), el([2, 5, 2], [14, 8, 12], 'fa'), el([2, 5, 12], [14, 16, 15], 'fa'),
                  el([0, 5, 3], [2, 10, 13], 'fr'), el([14, 5, 3], [16, 10, 13], 'fr')], {'fa': 'cinema_seat_fabric', 'fr': 'cinema_seat_frame'}, None),
 'cinema_seat_broken': ([el([1, 0, 3], [15, 5, 12], 'fr'), el([2, 5, 2], [14, 7, 12], 'fa'), el([2, 5, 12], [9, 12, 15], 'fa'),
                         el([0, 5, 3], [2, 10, 13], 'fr')], {'fa': 'cinema_seat_torn', 'fr': 'cinema_seat_frame'}, None),
 'cinema_aisle_light': ([el([5, 1, 14], [11, 4, 16], 'l', glow=12)], {'l': 'cinema_aisle_light'}, None),
 'cinema_exit_sign': ([el([2, 4, 14], [14, 11, 16], 'b', north='e', glow=14)], {'e': 'cinema_exit_sign', 'b': 'cinema_seat_frame'}, None),
 'cinema_projector': ([el([3, 3, 14], [13, 13, 16], 'p', glow=15)], {'p': 'cinema_projector'}, None),
 'cinema_projector_off': ([el([3, 3, 14], [13, 13, 16], 'p')], {'p': 'cinema_projector_off'}, None),
 'cinema_counter': ([el([0, 0, 0], [16, 16, 16], 's', north='f', up='t')], {'f': 'cinema_counter_front', 's': 'cinema_counter_side', 't': 'cinema_counter_top'}, None),
 'cinema_ticket_window': ([el([0, 0, 7], [16, 16, 9], 'g')], {'g': 'cinema_ticket_glass'}, 'minecraft:translucent'),
 'cinema_popcorn_machine': ([el([2, 0, 2], [14, 16, 14], 'p', glow=10)], {'p': 'cinema_popcorn'}, 'minecraft:cutout'),
 'cinema_soda_fountain': ([el([2, 0, 3], [14, 13, 15], 's')], {'s': 'cinema_soda'}, None),
 'cinema_candy_case': ([el([0, 0, 0], [16, 16, 16], 's', north='c', up='t')], {'c': 'cinema_candy', 's': 'cinema_counter_side', 't': 'cinema_counter_top'}, None),
 'cinema_menu_board': ([el([0, 0, 15], [16, 16, 16], 'm', glow=10)], {'m': 'cinema_menu'}, None),
 'cinema_trash_bin': ([el([4, 0, 4], [12, 13, 12], 't')], {'t': 'cinema_trash'}, None),
 'cinema_stanchion': ([el([7, 0, 7], [9, 15, 9], 'b'), el([5, 0, 5], [11, 1, 11], 'b'), el([6, 14, 6], [10, 16, 10], 'b')], {'b': 'cinema_brass'}, None),
 'cinema_velvet_rope': ([el([0, 9, 7], [16, 11, 9], 'v'), el([0, 9, 7], [16, 11, 9], 'v')], {'v': 'cinema_velvet'}, None),
}
for name, (elems, texs, rt) in TURNED.items():
    model(name, elems, texs, rt)
    facing_state(name)
    item_model(name)
w(f'{M}/cinema_spilled_popcorn.json', {"parent": "minecraft:block/carpet", "textures": {"wool": O + "block/cinema_spilled_popcorn"}, "render_type": "minecraft:cutout"})
w(f'{A}/blockstates/cinema_spilled_popcorn.json', {"variants": {"": {"model": O + "block/cinema_spilled_popcorn"}}})
item_model('cinema_spilled_popcorn')
# letters
lv = {}
for gi in range(36):
    model(f'cinema_letter_{gi}', [el([0, 0, 14], [16, 16, 16], 'b', north='l', glow=14)], {'l': f'cinema_letter_{gi}', 'b': 'cinema_seat_frame'})
    for d in YR:
        lv[f"facing={d},glyph={gi}"] = {"model": O + f"block/cinema_letter_{gi}", "y": YR[d]}
w(f'{A}/blockstates/cinema_letter.json', {"variants": lv})
item_model('cinema_letter', 'cinema_letter_0')
# posters
pv = {}
for pi in range(6):
    for half in ('upper', 'lower'):
        model(f'cinema_poster_{pi}_{half}', [el([1, 0, 15], [15, 16, 16], 'p')], {'p': f'cinema_poster_{pi}_{half}'})
        for d in YR:
            pv[f"facing={d},upper={'true' if half == 'upper' else 'false'},variant={pi}"] = {"model": O + f"block/cinema_poster_{pi}_{half}", "y": YR[d]}
w(f'{A}/blockstates/cinema_poster.json', {"variants": pv})
item_model('cinema_poster', 'cinema_poster_0_upper')
w(f'{A}/models/item/ticket_stub.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "item/ticket_stub"}})

# ---- loot ----------------------------------------------------------------------------------------------------------
ALL = list(FULL) + list(TURNED) + ['cinema_riser_stairs', 'cinema_spilled_popcorn', 'cinema_letter', 'cinema_poster']
for name in ALL:
    w(f'{D}/loot_tables/blocks/{name}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + name}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
def it(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ':' in name else O + name, "weight": weight}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e
def table(name, rolls, entries):
    w(f'{D}/loot_tables/chests/{name}.json', {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}]})
table('cinema_box_office', (3, 6), [it('office_coin', 6, 2, 6), it('ticket_stub', 6, 1, 4), it('minecraft:paper', 3, 2, 6), it('master_key', 1),
                                    it('research_file', 1), it('glowstick', 2, 1, 2)])
table('cinema_concessions', (3, 6), [it('minecraft:cookie', 4, 2, 6), it('minecraft:sugar', 3, 2, 5), it('minecraft:honey_bottle', 2), it('minecraft:sweet_berries', 3, 2, 6),
                                     it('almond_water', 3, 1, 2), it('office_coin', 3, 1, 3), it('cinema_spilled_popcorn', 2, 2, 6)])
table('cinema_lost_found', (2, 5), [it('ticket_stub', 5, 1, 3), it('office_coin', 4, 1, 4), it('minecraft:string', 2, 1, 3), it('minecraft:leather', 1), it('glowstick', 2),
                                    it('industrial_flashlight', 1), it('portable_battery', 1), it('almond_water', 2), it('research_file', 1), it('minecraft:candle', 2, 1, 2)])
w(f'{D}/advancements/milestones/discover_cinema.json', {"criteria": {"done": {"trigger": "minecraft:impossible"}}})

# ---- sound: projector rattle ---------------------------------------------------------------------------------------
snd = f'{A}/sounds/block/cinema'; os.makedirs(snd, exist_ok=True)
for i, (rate, dur) in enumerate(((24, 2.4), (22, 2.0)), 1):
    a, b = f'/tmp/proj_a{i}.wav', f'/tmp/proj_b{i}.wav'
    subprocess.run(['sox', '-V1', '-n', '-r', '44100', '-c', '1', a, 'synth', str(dur), 'brownnoise', 'tremolo', str(rate), '95', 'bandpass', '1600', '900'], check=True)
    subprocess.run(['sox', '-V1', '-n', '-r', '44100', '-c', '1', b, 'synth', str(dur), 'sine', '55', 'vol', '0.25'], check=True)
    subprocess.run(['sox', '-V1', '-m', a, b, '-C', '5', f'{snd}/projector{i}.ogg', 'fade', 't', '0.2', '-0', '0.5', 'norm', '-3'], check=True)
S = f'{A}/sounds.json'; sj = json.load(open(S))
sj['block.cinema.projector'] = {"category": "block", "subtitle": "subtitles.office_rooms.cinema.projector",
                                "sounds": [{"name": O + "block/cinema/projector1", "attenuation_distance": 24}, {"name": O + "block/cinema/projector2", "attenuation_distance": 24}]}
json.dump(sj, open(S, 'w'), indent=2)

# ---- lang ----------------------------------------------------------------------------------------------------------
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
NAMES = {'cinema_wall': 'Cinema Wall Panel', 'cinema_wallpaper': 'Lobby Wallpaper', 'cinema_carpet': 'Cinema Carpet', 'cinema_tile': 'Lobby Tiles',
         'cinema_ceiling': 'Acoustic Ceiling', 'cinema_riser': 'Seating Riser', 'cinema_riser_stairs': 'Aisle Steps', 'cinema_screen': 'Cinema Screen',
         'cinema_screen_static': 'Cinema Screen (Static)', 'cinema_screen_off': 'Cinema Screen (Off)', 'cinema_screen_torn': 'Torn Cinema Screen',
         'cinema_screen_figure_lower': 'Cinema Screen (Figure)', 'cinema_screen_figure_upper': 'Cinema Screen (Figure, Top)', 'cinema_seat': 'Cinema Seat',
         'cinema_seat_broken': 'Broken Cinema Seat', 'cinema_aisle_light': 'Aisle Light', 'cinema_exit_sign': 'Exit Sign', 'cinema_projector': 'Projector Port',
         'cinema_projector_off': 'Dead Projector Port', 'cinema_counter': 'Concession Counter', 'cinema_ticket_window': 'Ticket Window',
         'cinema_popcorn_machine': 'Popcorn Machine', 'cinema_soda_fountain': 'Soda Fountain', 'cinema_candy_case': 'Candy Case', 'cinema_menu_board': 'Menu Board',
         'cinema_stanchion': 'Stanchion', 'cinema_velvet_rope': 'Velvet Rope', 'cinema_marquee': 'Marquee Lights', 'cinema_trash_bin': 'Popcorn Bin',
         'cinema_spilled_popcorn': 'Spilled Popcorn', 'cinema_letter': 'Marquee Letter', 'cinema_poster': 'Faded Poster'}
for k, v in NAMES.items():
    lang[f'block.office_rooms.{k}'] = v
lang['item.office_rooms.ticket_stub'] = 'Ticket Stub'
lang['subtitles.office_rooms.cinema.projector'] = 'Projector rattles'
w(L, lang)
print('ok', len(ALL), 'blocks')
