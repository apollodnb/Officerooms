"""0.43.0: assets of the Dead Mall (all drawn here, own designs, nothing copied).
Run from anywhere: python3 tools/gen_0430.py"""
import json, math, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
M = f'{A}/models/block'
rnd = random.Random(430)
os.makedirs(f'{A}/textures/block', exist_ok=True)


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)


def n(a=5): return rnd.randint(-a, a)
def cl(c): return tuple(max(0, min(255, int(v))) for v in c[:3]) + ((int(c[3]) if len(c) > 3 else 255),)
def mix(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))
def nz(c, a=4): return tuple(v + n(a) for v in c[:3]) + tuple(c[3:])


def tex(name, fn, size=16):
    im = Image.new('RGBA', (size, size)); px = im.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = cl(fn(x, y))
    im.save(f'{A}/textures/block/{name}.png')


# ---- a 5x7 font for the 32 px signs ------------------------------------------------------------------------------
F5 = {'A': '01110 10001 10001 11111 10001 10001 10001', 'D': '11110 10001 10001 10001 10001 10001 11110',
      'E': '11111 10000 10000 11110 10000 10000 11111', 'F': '11111 10000 10000 11110 10000 10000 10000',
      'L': '10000 10000 10000 10000 10000 10000 11111', 'N': '10001 11001 10101 10101 10011 10001 10001',
      'O': '01110 10001 10001 10001 10001 10001 01110', 'R': '11110 10001 10001 11110 10100 10010 10001',
      'S': '01111 10000 10000 01110 00001 00001 11110', 'T': '11111 00100 00100 00100 00100 00100 00100',
      'U': '10001 10001 10001 10001 10001 10001 01110', 'Y': '10001 10001 01010 00100 00100 00100 00100',
      'P': '11110 10001 10001 11110 10000 10000 10000', 'H': '10001 10001 10001 11111 10001 10001 10001',
      'C': '01110 10001 10000 10000 10000 10001 01110', 'I': '01110 00100 00100 00100 00100 00100 01110',
      'W': '10001 10001 10001 10101 10101 10101 01010', 'M': '10001 11011 10101 10101 10001 10001 10001',
      'X': '10001 10001 01010 00100 01010 10001 10001', 'G': '01110 10001 10000 10111 10001 10001 01111'}


def text_mask(lines, size=32, top=None):
    """set of (x, y) pixels of centred text lines"""
    pts = set(); hgt = len(lines) * 9 - 2
    y0 = top if top is not None else (size - hgt) // 2
    for li, s in enumerate(lines):
        wd = len(s) * 6 - 1; x0 = (size - wd) // 2
        for i, ch in enumerate(s):
            g = F5.get(ch)
            if not g:
                continue
            for r, row in enumerate(g.split()):
                for k, b in enumerate(row):
                    if b == '1':
                        pts.add((x0 + i * 6 + k, y0 + li * 9 + r))
    return pts


# ---- textures ------------------------------------------------------------------------------------------------------
def tile(base, grout=(150, 140, 122)):
    def f(x, y):
        if x % 8 == 0 or y % 8 == 0:
            return nz(grout, 3)
        c = nz(base, 4)
        if (x % 8 == 1 or y % 8 == 1):
            c = tuple(v + 8 for v in c)                       # a faint glaze edge
        return c
    return f


tex('mall_tile', tile((214, 198, 168)))
tex('mall_tile_accent', tile((168, 96, 66), (120, 84, 64)))
CRACK = {(3, 2), (4, 3), (4, 4), (5, 5), (6, 5), (7, 6), (9, 6), (10, 7), (10, 8), (11, 9), (12, 9), (13, 10), (6, 6), (6, 7), (5, 8), (5, 9)}
tex('mall_tile_cracked', lambda x, y: (96, 86, 70) if (x, y) in CRACK else tile((206, 190, 160))(x, y))
tex('mall_shop_floor', lambda x, y: nz((200, 198, 190), 3) if rnd.random() > 0.08 else nz((120, 124, 130), 6))
CARPET = [(52, 34, 70), (210, 80, 140), (40, 170, 170), (230, 200, 80)]
def carpet(x, y):
    h = (x * 7 + y * 13 + (x * y) % 5) % 23
    if h == 0: return nz(CARPET[1], 8)
    if h == 7: return nz(CARPET[2], 8)
    if h == 15 and (x + y) % 2: return nz(CARPET[3], 8)
    return nz(CARPET[0], 5)
tex('mall_shop_carpet', carpet)
tex('mall_dept_floor', lambda x, y: nz((112, 112, 118) if (x % 4 and y % 4) else (98, 98, 104), 3))
tex('mall_service_floor', lambda x, y: nz((120, 118, 112), 6) if rnd.random() > 0.05 else nz((84, 80, 70), 4))
tex('mall_wall', lambda x, y: nz((222, 210, 184), 3) if rnd.random() > 0.1 else nz((208, 196, 170), 2))
tex('mall_dept_wall', lambda x, y: nz((176, 176, 172), 3) if x % 8 else nz((150, 150, 146), 2))
def cinder(x, y):
    row = y // 8; xs = (x + (4 if row % 2 else 0)) % 16
    if y % 8 == 7 or xs == 15: return nz((150, 156, 146), 3)
    return nz((186, 192, 180), 5)
tex('mall_service_wall', cinder)
def ceiling(x, y):
    if x == 0 or y == 0: return nz((176, 172, 160), 2)
    c = nz((228, 224, 212), 3)
    return tuple(v - 30 for v in c) if (x * 3 + y * 5) % 11 == 0 else c
tex('mall_ceiling', ceiling)
tex('mall_service_ceiling', lambda x, y: nz((138, 136, 132), 5))
tex('mall_facade', lambda x, y: nz((74, 56, 40), 3) if y % 4 else nz((104, 82, 58), 3))
tex('mall_frame', lambda x, y: nz((70, 54, 38), 2) if 3 < x < 12 else nz((52, 40, 28), 2))
def pillar(x, y):
    if 6 <= y <= 7: return nz((176, 140, 70), 4)               # brass band
    return tile((224, 214, 188), (186, 176, 154))(x, y)
tex('mall_pillar', pillar)
def skylight(x, y):
    if x == 0 or y == 0 or x == 8 or y == 8: return nz((70, 74, 80), 2)
    t = (x + y) / 30
    return nz(mix((150, 162, 172), (190, 196, 200), t), 3)
tex('mall_skylight', skylight)
tex('mall_roof_beam', lambda x, y: nz((72, 74, 78), 3) if 2 < y < 13 else nz((56, 58, 60), 2))
tex('mall_shutter', lambda x, y: nz((150, 152, 154), 3) if y % 3 else nz((96, 98, 100), 3))
tex('mall_planter', lambda x, y: nz((156, 84, 52), 4) if (y % 4 and (x + (y // 4) * 4) % 8) else nz((120, 104, 92), 3))
tex('mall_planter_top', lambda x, y: nz((156, 84, 52), 3) if x in (0, 15) or y in (0, 15) else nz((70, 52, 36), 6))
def basin(x, y):
    if rnd.random() < 0.06: return nz((214, 182, 72), 10)        # coins
    if rnd.random() < 0.03: return nz((180, 184, 190), 8)
    return tile((70, 122, 128), (52, 92, 96))(x, y)
tex('mall_fountain_basin', basin)
tex('mall_fountain_rim', lambda x, y: nz((214, 206, 186), 4) if y > 2 else nz((236, 230, 214), 3))
tex('mall_fountain_spout', lambda x, y: nz((150, 136, 98), 6) if x % 5 else nz((118, 104, 70), 4))
tex('mall_escalator_side', lambda x, y: nz((168, 172, 176), 2) if y > 1 else nz((24, 24, 26), 2))
tex('mall_escalator_top', lambda x, y: nz((230, 190, 40), 4) if y < 2 else nz((120, 124, 128), 3) if x % 2 else nz((84, 86, 90), 2))
def night(x, y):
    if x in (0, 15) or y in (0, 15): return nz((50, 50, 54), 2)
    if y == 8: return nz((150, 152, 156), 4)                     # push bar
    if x == 7 or x == 8: return nz((30, 30, 34), 2)
    if (x + 2 * y) % 13 == 0: return (18, 20, 26)
    return nz((6, 7, 10), 1)
tex('mall_night_glass', night)
tex('mall_storefront_glass', lambda x, y: (60, 54, 40, 255) if y == 15 else (210, 226, 232, 46 if (x + y) % 9 else 80))
def lease(x, y):
    if 4 <= x <= 11 and y in (1, 2): return (210, 204, 160, 255)  # tape
    if (x * 5 + y * 3) % 17 == 0: return (150, 128, 90, 255)
    return nz((182, 154, 112), 6) + (255,)
tex('mall_lease_glass', lease)
tex('mall_glass_rail', lambda x, y: (168, 140, 72, 255) if y < 2 else (200, 220, 226, 60 if x % 7 else 90))
tex('mall_glass_rail_top', lambda x, y: (168, 140, 72, 255))
tex('mall_ceiling_light', lambda x, y: (200, 196, 182) if x in (0, 15) or y in (0, 15) or x == 8 or y == 8 else nz((252, 248, 230), 2))
tex('mall_ceiling_light_off', lambda x, y: (170, 166, 156) if x in (0, 15) or y in (0, 15) or x == 8 or y == 8 else nz((150, 150, 144), 3))
tex('mall_service_light', lambda x, y: (60, 60, 60) if (x % 4 == 0 or y % 4 == 0) else nz((246, 214, 140), 4))
tex('mall_service_light_off', lambda x, y: (60, 60, 60) if (x % 4 == 0 or y % 4 == 0) else nz((110, 100, 80), 4))
# decor textures
tex('mall_wood', lambda x, y: nz((150, 104, 62), 6) if y % 4 else nz((110, 74, 42), 4))
tex('mall_metal', lambda x, y: nz((70, 72, 76), 3))
tex('mall_chrome', lambda x, y: nz((196, 200, 206), 6))
tex('mall_plastic_orange', lambda x, y: nz((222, 128, 40), 4))
tex('mall_plastic_red', lambda x, y: nz((196, 44, 40), 4))
tex('mall_plastic_black', lambda x, y: nz((30, 30, 32), 3))
tex('mall_laminate', lambda x, y: nz((226, 220, 206), 3) if y < 3 else nz((150, 110, 74), 5))
def plant(x, y):
    cx = abs(x - 8)
    leaf = (y < 12 and abs(cx - (12 - y) // 2) <= 1) or (y >= 11 and cx <= 0)
    if not leaf: return (0, 0, 0, 0)
    return nz((96, 120, 70) if y < 11 else (110, 84, 50), 10) + (255,)
tex('mall_plant', plant)
def directory(x, y):
    if x in (0, 15) or y in (0, 15): return nz((60, 46, 32), 2)
    if (x, y) in {(10, 6), (11, 6), (10, 7), (11, 7)}: return (230, 30, 30)    # you are here (it is not)
    blocks = [((2, 2, 6, 5), (90, 140, 200)), ((8, 2, 13, 4), (210, 170, 80)), ((2, 8, 5, 13), (120, 180, 120)), ((7, 10, 13, 13), (190, 120, 170))]
    for (x0, y0, x1, y1), c in blocks:
        if x0 <= x <= x1 and y0 <= y <= y1: return nz(c, 6)
    return nz((222, 218, 206), 3)
tex('mall_directory', directory)
tex('mall_directory_top', lambda x, y: nz((60, 46, 32), 2) if x in (0, 15) or y in (0, 15) else nz((190, 50, 46), 4) if y < 6 else directory((x + 5) % 15 + 1, (y + 7) % 14 + 1))
def wet(x, y):
    if 6 <= x <= 9 and 3 <= y <= 4: return (20, 20, 20)
    if x == 8 and 5 <= y <= 9: return (20, 20, 20)
    if y == 10 and 5 <= x <= 11: return (20, 20, 20)
    return nz((236, 200, 30), 6)
tex('mall_wet_floor', wet)
tex('mall_cart_wire', lambda x, y: (176, 180, 186, 255) if x % 3 == 0 or y % 3 == 0 else (0, 0, 0, 0))
tex('mall_kiddie', lambda x, y: nz((200, 40, 40), 6) if y > 4 else nz((240, 220, 90), 6))
def rack(x, y):
    colours = [(160, 40, 50), (40, 70, 140), (220, 200, 160), (40, 40, 44), (90, 130, 80), (200, 120, 160)]
    return nz(colours[(x // 2 + y // 9) % len(colours)], 8)
tex('mall_clothes', rack)
tex('mall_shelf', lambda x, y: nz((200, 202, 204), 3) if y % 5 else nz((120, 122, 126), 3))
tex('mall_boxes', lambda x, y: nz((190, 150, 100), 6) if (x // 5 + y // 5) % 2 else nz((220, 220, 214), 4))
def shoes(x, y):
    if y % 5 == 4: return nz((120, 84, 50), 4)
    return nz([(40, 40, 44), (190, 60, 50), (230, 230, 230), (90, 60, 40)][(x // 4 + y // 5) % 4], 8)
tex('mall_shoes', shoes)
tex('mall_register', lambda x, y: nz((60, 200, 90), 4) if (3 <= x <= 12 and 2 <= y <= 5) else nz((90, 92, 96), 4))
def tv(x, y):
    if x in (0, 15) or y in (0, 1, 14, 15): return nz((34, 32, 30), 3)
    v = rnd.randint(60, 230); return (v, v, v)
tex('mall_tv', tv)
tex('mall_tv_off', lambda x, y: nz((34, 32, 30), 3) if x in (0, 15) or y in (0, 1, 14, 15) else nz((20, 26, 24), 3))
tex('mall_food_counter', lambda x, y: nz((200, 204, 210), 4) if y < 4 else nz((180, 40, 36), 5) if y < 13 else nz((60, 60, 64), 3))
tex('mall_fryer', lambda x, y: nz((40, 34, 20), 4) if (2 <= x <= 13 and 2 <= y <= 13 and y > 4) else nz((180, 184, 188), 4))
def menu(x, y):
    if x in (0, 15) or y in (0, 15): return nz((40, 40, 40), 2)
    if y % 3 == 1 and 2 <= x <= 13 and (x * 7 + y) % 5: return nz([(240, 200, 60), (230, 230, 230), (230, 90, 60)][y % 3], 10)
    return nz((14, 14, 16), 2)
tex('mall_menu', menu)
tex('mall_kiosk', lambda x, y: (nz((40, 110, 70), 4) if y > 5 else nz((236, 236, 230), 3) if (x // 3) % 2 else nz((40, 110, 70), 4)))
tex('mall_speaker', lambda x, y: (60, 60, 60) if ((x - 7.5) ** 2 + (y - 7.5) ** 2 < 30 and (x + y) % 2) else nz((228, 228, 222), 3))
tex('mall_tank', lambda x, y: (40, 44, 48, 255) if x in (0, 15) or y in (0, 15) else (70, 110, 70, 150) if y > 4 else (150, 180, 180, 90))
tex('mall_mirror', lambda x, y: nz((60, 48, 34), 2) if x in (0, 15) or y in (0, 15) else nz(mix((200, 214, 220), (150, 164, 176), (x + y) / 30), 2))
def sign(name, lines, bg, fg, border):
    pts = text_mask(lines)
    tex(name, lambda x, y: border if x < 2 or y < 2 or x > 29 or y > 29 else fg if (x, y) in pts else nz(bg, 3), 32)
sign('mall_for_lease', ['FOR', 'LEASE'], (236, 236, 232), (190, 30, 30), (190, 30, 30))
sign('mall_employees', ['STAFF', 'ONLY'], (70, 74, 80), (236, 236, 236), (230, 190, 40))
sign('mall_out_of_order', ['OUT', 'OF', 'ORDER'], (236, 236, 232), (20, 20, 20), (200, 30, 30))

# ---- models ----------------------------------------------------------------------------------------------------------
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
YR = {"north": 0, "east": 90, "south": 180, "west": 270}


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


def cube(name, texture=None, glow=0, rt=None, top=None):
    texture = texture or name
    over = {'up': 't'} if top else {}
    texs = {'a': texture, **({'t': top} if top else {})}
    model(name, [el([0, 0, 0], [16, 16, 16], 'a', glow=glow, **over)], texs, rt=rt)
    w(f'{A}/blockstates/{name}.json', {"variants": {"": {"model": O + "block/" + name}}})
    item_model(name)


def facing_state(name, models=None):
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={d}": {"model": O + "block/" + (models or name), "y": YR[d]} for d in YR}})


FULL = ['mall_tile', 'mall_tile_accent', 'mall_tile_cracked', 'mall_shop_floor', 'mall_shop_carpet', 'mall_dept_floor', 'mall_service_floor', 'mall_wall',
        'mall_dept_wall', 'mall_service_wall', 'mall_ceiling', 'mall_service_ceiling', 'mall_facade', 'mall_frame', 'mall_pillar', 'mall_roof_beam',
        'mall_shutter', 'mall_fountain_basin', 'mall_fountain_rim', 'mall_escalator_side', 'mall_night_glass']
for nme in FULL:
    cube(nme)
cube('mall_skylight', glow=9)
cube('mall_planter', top='mall_planter_top')
cube('mall_fountain_spout')
cube('mall_storefront_glass', rt='minecraft:translucent')
cube('mall_lease_glass', rt='minecraft:cutout')
for nme in ('mall_ceiling_light', 'mall_service_light'):
    model(nme, [el([0, 0, 0], [16, 16, 16], 'a', glow=15)], {'a': nme})
    model(nme + '_off', [el([0, 0, 0], [16, 16, 16], 'a')], {'a': nme + '_off'})
    w(f'{A}/blockstates/{nme}.json', {"variants": {"lit=true": {"model": O + "block/" + nme}, "lit=false": {"model": O + "block/" + nme + "_off"}}})
    item_model(nme)
# escalator steps (stairs)
for kind, parent in (('', 'stairs'), ('_inner', 'inner_stairs'), ('_outer', 'outer_stairs')):
    w(f'{M}/mall_escalator{kind}.json', {"parent": f"minecraft:block/{parent}", "textures": {"bottom": O + "block/mall_escalator_side",
      "top": O + "block/mall_escalator_top", "side": O + "block/mall_escalator_side"}})
var = {}
for f in ('east', 'north', 'south', 'west'):
    base = {'east': 0, 'south': 90, 'west': 180, 'north': 270}[f]
    for half in ('bottom', 'top'):
        for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
            mdl = O + 'block/mall_escalator' + ('' if shape == 'straight' else '_inner' if 'inner' in shape else '_outer')
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
w(f'{A}/blockstates/mall_escalator.json', {"variants": var})
item_model('mall_escalator')
# glass railing (a pane with a brass top)
PT = {'pane': O + 'block/mall_glass_rail', 'edge': O + 'block/mall_glass_rail_top', 'particle': O + 'block/mall_glass_rail'}
for part, parent in (('post', 'template_glass_pane_post'), ('side', 'template_glass_pane_side'), ('side_alt', 'template_glass_pane_side_alt'),
                     ('noside', 'template_glass_pane_noside'), ('noside_alt', 'template_glass_pane_noside_alt')):
    w(f'{M}/mall_glass_rail_{part}.json', {"parent": f"minecraft:block/{parent}", "textures": PT, "render_type": "minecraft:translucent"})
mp = [{"apply": {"model": O + "block/mall_glass_rail_post"}}]
for d, mdl, y in (('north', 'side', 0), ('east', 'side', 90), ('south', 'side_alt', 0), ('west', 'side_alt', 90)):
    a = {"model": O + f"block/mall_glass_rail_{mdl}"}
    if y: a["y"] = y
    mp.append({"when": {d: "true"}, "apply": a})
for d, mdl, y in (('north', 'noside', 0), ('east', 'noside_alt', 0), ('south', 'noside_alt', 90), ('west', 'noside', 270)):
    a = {"model": O + f"block/mall_glass_rail_{mdl}"}
    if y: a["y"] = y
    mp.append({"when": {d: "false"}, "apply": a})
w(f'{A}/blockstates/mall_glass_rail.json', {"multipart": mp})
w(f'{A}/models/item/mall_glass_rail.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "block/mall_glass_rail"}, "render_type": "minecraft:translucent"})

# turned decor, front = north: (elements, textures, render type)
TURNED = {
 'mall_bench': ([el([0, 4, 2], [16, 6, 13], 'w'), el([0, 6, 13], [16, 14, 15], 'w'), el([1, 0, 4], [3, 4, 12], 'm'), el([13, 0, 4], [15, 4, 12], 'm')],
                {'w': 'mall_wood', 'm': 'mall_metal'}, None),
 'mall_plant': (None, None, None),
 'mall_directory': ([el([2, 0, 7], [14, 16, 9], 'f', north='d', south='d')], {'f': 'mall_metal', 'd': 'mall_directory'}, None),
 'mall_directory_top': ([el([2, 0, 7], [14, 14, 9], 'f', north='d', south='d')], {'f': 'mall_metal', 'd': 'mall_directory_top'}, None),
 'mall_wet_floor_sign': ([el([4, 0, 6], [12, 13, 7], 'y'), el([4, 0, 9], [12, 13, 10], 'y'), el([4, 12, 6], [12, 13, 10], 'y')], {'y': 'mall_wet_floor'}, None),
 'mall_cart': ([el([2, 5, 1], [14, 14, 15], 'c'), el([3, 1, 2], [13, 5, 14], 'c'), el([2, 0, 2], [4, 2, 4], 'b'), el([12, 0, 2], [14, 2, 4], 'b'),
                el([2, 0, 12], [4, 2, 14], 'b'), el([12, 0, 12], [14, 2, 14], 'b'), el([2, 14, 14], [14, 15, 15], 'r')],
               {'c': 'mall_cart_wire', 'b': 'mall_plastic_black', 'r': 'mall_plastic_red'}, 'minecraft:cutout'),
 'mall_kiddie_ride': ([el([2, 3, 2], [14, 9, 14], 'k'), el([4, 9, 6], [12, 13, 12], 'k'), el([3, 0, 3], [13, 3, 13], 'm'), el([6, 9, 13], [10, 16, 15], 'y')],
                      {'k': 'mall_kiddie', 'm': 'mall_metal', 'y': 'mall_plastic_orange'}, None),
 'mall_clothes_rack': ([el([7, 0, 7], [9, 15, 9], 'c'), el([0, 14, 7.5], [16, 15, 8.5], 'c'), el([1, 3, 5], [15, 14, 11], 'k')],
                       {'c': 'mall_chrome', 'k': 'mall_clothes'}, None),
 'mall_shelf': ([el([0, 0, 2], [16, 16, 16], 's', north='b')], {'s': 'mall_shelf', 'b': 'mall_boxes'}, None),
 'mall_shoe_shelf': ([el([0, 0, 6], [16, 16, 16], 'w', north='s')], {'w': 'mall_wood', 's': 'mall_shoes'}, None),
 'mall_counter': ([el([0, 0, 0], [16, 15, 16], 'l')], {'l': 'mall_laminate'}, None),
 'mall_register': ([el([3, 0, 4], [13, 5, 13], 'r'), el([4, 5, 9], [12, 9, 11], 'r', north='g')], {'r': 'mall_metal', 'g': 'mall_register'}, None),
 'mall_tv': ([el([1, 0, 3], [15, 13, 15], 'b', north='s', glow=0), el([2, 2, 2.9], [14, 12, 3], 's', glow=10)], {'b': 'mall_plastic_black', 's': 'mall_tv'}, None),
 'mall_tv_off': ([el([1, 0, 3], [15, 13, 15], 'b', north='s')], {'b': 'mall_plastic_black', 's': 'mall_tv_off'}, None),
 'mall_food_counter': ([el([0, 0, 0], [16, 16, 16], 'c', up='t')], {'c': 'mall_food_counter', 't': 'mall_chrome'}, None),
 'mall_fryer': ([el([1, 0, 1], [15, 15, 15], 'c', up='f')], {'c': 'mall_chrome', 'f': 'mall_fryer'}, None),
 'mall_menu_board': ([el([0, 0, 14], [16, 16, 16], 'b', north='m', glow=6)], {'b': 'mall_plastic_black', 'm': 'mall_menu'}, None),
 'mall_food_table': ([el([1, 13, 1], [15, 14, 15], 'o'), el([7, 0, 7], [9, 13, 9], 'm'), el([4, 0, 4], [12, 1, 12], 'm')], {'o': 'mall_plastic_orange', 'm': 'mall_metal'}, None),
 'mall_food_chair': ([el([3, 7, 3], [13, 8, 13], 'o'), el([3, 8, 12], [13, 16, 13], 'o'), el([3, 0, 3], [4, 7, 4], 'm'), el([12, 0, 3], [13, 7, 4], 'm'),
                      el([3, 0, 12], [4, 7, 13], 'm'), el([12, 0, 12], [13, 7, 13], 'm')], {'o': 'mall_plastic_orange', 'm': 'mall_metal'}, None),
 'mall_kiosk': ([el([0, 0, 0], [16, 12, 16], 'k'), el([0, 12, 0], [16, 13, 16], 'w'), el([0, 15, -1], [16, 16, 17], 'k')], {'k': 'mall_kiosk', 'w': 'mall_wood'}, None),
 'mall_out_of_order': ([el([7, 0, 7], [9, 8, 9], 'm'), el([3, 0, 3], [13, 1, 13], 'm'), el([3, 8, 7.5], [13, 16, 8.5], 'm', north='s', south='s')],
                       {'m': 'mall_metal', 's': 'mall_out_of_order'}, None),
 'mall_speaker': ([el([3, 14, 3], [13, 16, 13], 's')], {'s': 'mall_speaker'}, None),
 'mall_pet_tank': ([el([0, 0, 1], [16, 14, 15], 't')], {'t': 'mall_tank'}, 'minecraft:translucent'),
 'mall_salon_chair': ([el([3, 6, 3], [13, 9, 13], 'b'), el([3, 9, 12], [13, 16, 14], 'b'), el([6, 0, 6], [10, 6, 10], 'c'), el([4, 0, 4], [12, 1, 12], 'c'),
                       el([2, 9, 4], [3, 11, 12], 'c'), el([13, 9, 4], [14, 11, 12], 'c')], {'b': 'mall_plastic_black', 'c': 'mall_chrome'}, None),
 'mall_mirror': ([el([0, 0, 15], [16, 16, 16], 'm')], {'m': 'mall_mirror'}, None),
 'mall_for_lease_sign': ([el([1, 2, 15], [15, 14, 16], 's')], {'s': 'mall_for_lease'}, None),
 'mall_employees_only': ([el([1, 4, 15], [15, 12, 16], 's')], {'s': 'mall_employees'}, None),
}
for nme, (elems, texs, rt) in TURNED.items():
    if nme == 'mall_plant':
        model(nme, None, {'cross': 'mall_plant'}, rt='minecraft:cutout', parent='minecraft:block/cross')
    else:
        model(nme, elems, texs, rt=rt)
    facing_state(nme)
    if nme == 'mall_plant':
        w(f'{A}/models/item/{nme}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "block/mall_plant"}})
    else:
        item_model(nme)

ALL = FULL + ['mall_skylight', 'mall_planter', 'mall_fountain_spout', 'mall_storefront_glass', 'mall_lease_glass', 'mall_ceiling_light', 'mall_service_light',
              'mall_escalator', 'mall_glass_rail'] + list(TURNED)

# ---- loot ------------------------------------------------------------------------------------------------------------
for nme in ALL:
    w(f'{D}/loot_tables/blocks/{nme}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + nme}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def it(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name if ':' in name else O + name, "weight": weight}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


def table(name, rolls, entries):
    w(f'{D}/loot_tables/chests/{name}.json', {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]}, "entries": entries}]})


table('mall_shop', (2, 5), [it('office_coin', 6, 2, 8), it('minecraft:paper', 4, 1, 4), it('minecraft:gold_nugget', 3, 2, 6), it('minecraft:name_tag', 1),
      it('minecraft:clock', 1), it('glowstick', 2), it('almond_water', 2)])
table('mall_stock', (2, 6), [it('minecraft:white_wool', 3, 1, 4), it('minecraft:leather', 3, 1, 3), it('minecraft:string', 3, 2, 6), it('minecraft:paper', 3, 2, 6),
      it('minecraft:leather_boots', 1), it('minecraft:leather_helmet', 1), it('minecraft:book', 2), it('minecraft:candle', 2, 1, 3), it('office_coin', 2, 1, 4),
      it('portable_battery', 1), it('research_file', 1)])
table('mall_electronics', (2, 5), [it('electronic_component', 5, 1, 3), it('portable_battery', 3), it('minecraft:redstone', 3, 2, 6), it('minecraft:copper_ingot', 3, 1, 4),
      it('anomaly_detector', 1), it('industrial_flashlight', 1), it('minecraft:glowstone_dust', 2, 1, 4)])
table('mall_toys', (2, 5), [it('noise_maker', 3), it('chalk', 3, 1, 3), it('minecraft:snowball', 3, 2, 8), it('minecraft:firework_rocket', 2, 1, 3),
      it('minecraft:red_dye', 2), it('minecraft:lime_dye', 2), it('glowstick', 3, 1, 4), it('minecraft:music_disc_13', 1)])
table('mall_food', (2, 5), [it('minecraft:bread', 4, 1, 3), it('minecraft:cookie', 4, 2, 8), it('minecraft:baked_potato', 3, 1, 3), it('minecraft:cooked_chicken', 3, 1, 2),
      it('minecraft:pumpkin_pie', 2), it('almond_water', 4, 1, 2), it('minecraft:honey_bottle', 1), it('minecraft:sugar', 2, 1, 4)])
table('mall_office', (3, 6), [it('research_file', 4, 1, 2), it('minecraft:paper', 4, 2, 6), it('office_coin', 4, 3, 10), it('master_key', 1),
      it('level_2_access_card', 1), it('minecraft:writable_book', 2), it('minecraft:map', 1)])
table('mall_security', (3, 6), [it('industrial_flashlight', 3), it('portable_battery', 4, 1, 2), it('bandage', 3, 1, 3), it('emergency_flare', 3, 1, 2),
      it('minecraft:iron_ingot', 3, 1, 3), it('master_key', 1), it('level_1_access_card', 2), it('anomaly_detector', 1)])
table('mall_dept', (3, 7), [it('minecraft:leather_chestplate', 2), it('minecraft:leather_leggings', 2), it('minecraft:white_wool', 3, 2, 6),
      it('minecraft:gold_ingot', 2, 1, 3), it('minecraft:emerald', 1, 1, 2), it('office_coin', 4, 2, 8), it('minecraft:diamond', 1), it('research_file', 2)])
table('mall_kiosk', (2, 4), [it('office_coin', 5, 1, 6), it('minecraft:cookie', 3, 1, 4), it('minecraft:paper', 2, 1, 3), it('almond_water', 2), it('glowstick', 2)])

# ---- world data ------------------------------------------------------------------------------------------------------
w(f'{D}/worldgen/structure/dead_mall.json', {"type": "office_rooms:megastructure", "kind": "mall", "biomes": "#office_rooms:has_structure/megastructure",
  "step": "surface_structures", "spawn_overrides": {}, "terrain_adaptation": "none"})
w(f'{D}/worldgen/structure_set/dead_mall.json', {"structures": [{"structure": "office_rooms:dead_mall", "weight": 1}], "placement": {"type": "office_rooms:region_centre"}})
for m in ('discover_mall', 'mall_dept'):
    w(f'{D}/advancements/milestones/{m}.json', {"criteria": {"done": {"trigger": "minecraft:impossible"}}})
dim = f'{D}/dimension/office_rooms.json'; dj = json.load(open(dim))
dj['generator']['settings']['content_version'] = 3
w(dim, dj)

# ---- lang ------------------------------------------------------------------------------------------------------------
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
NAMES = {'mall_tile': 'Mall Tiles', 'mall_tile_accent': 'Terracotta Mall Tiles', 'mall_tile_cracked': 'Cracked Mall Tiles', 'mall_shop_floor': 'Shop Vinyl',
 'mall_shop_carpet': 'Arcade Carpet', 'mall_dept_floor': 'Department Store Carpet', 'mall_service_floor': 'Service Corridor Concrete', 'mall_wall': 'Mall Stucco',
 'mall_dept_wall': 'Department Store Wall', 'mall_service_wall': 'Painted Cinder Block', 'mall_ceiling': 'Mall Ceiling Tile', 'mall_service_ceiling': 'Service Ceiling',
 'mall_facade': 'Bronze Shop Facade', 'mall_frame': 'Bronze Storefront Frame', 'mall_pillar': 'Mall Column', 'mall_skylight': 'Skylight',
 'mall_roof_beam': 'Skylight Beam', 'mall_shutter': 'Roller Shutter', 'mall_planter': 'Planter', 'mall_fountain_basin': 'Fountain Basin',
 'mall_fountain_rim': 'Fountain Rim', 'mall_fountain_spout': 'Fountain Spout', 'mall_escalator_side': 'Escalator Panel', 'mall_escalator': 'Escalator Steps',
 'mall_night_glass': 'Main Entrance Door', 'mall_storefront_glass': 'Storefront Glass', 'mall_lease_glass': 'Papered-Over Window',
 'mall_glass_rail': 'Glass Railing', 'mall_ceiling_light': 'Mall Ceiling Light', 'mall_service_light': 'Caged Service Light', 'mall_bench': 'Mall Bench',
 'mall_plant': 'Plastic Palm', 'mall_directory': 'Mall Directory', 'mall_directory_top': 'Mall Directory Map', 'mall_wet_floor_sign': 'Wet Floor Sign',
 'mall_cart': 'Shopping Cart', 'mall_kiddie_ride': 'Coin-Operated Ride', 'mall_clothes_rack': 'Clothes Rack', 'mall_shelf': 'Store Shelf',
 'mall_shoe_shelf': 'Shoe Wall', 'mall_counter': 'Sales Counter', 'mall_register': 'Cash Register', 'mall_tv': 'Television (Static)', 'mall_tv_off': 'Television',
 'mall_food_counter': 'Food Stall Counter', 'mall_fryer': 'Deep Fryer', 'mall_menu_board': 'Menu Board', 'mall_food_table': 'Food Court Table',
 'mall_food_chair': 'Food Court Chair', 'mall_kiosk': 'Kiosk Cart', 'mall_out_of_order': 'Out of Order Sign', 'mall_speaker': 'Ceiling Speaker',
 'mall_pet_tank': 'Murky Aquarium', 'mall_salon_chair': 'Salon Chair', 'mall_mirror': 'Mirror', 'mall_for_lease_sign': 'For Lease Sign',
 'mall_employees_only': 'Staff Only Sign'}
for k, v in NAMES.items():
    lang[f'block.office_rooms.{k}'] = v
lang['subtitles.office_rooms.mall.muzak'] = 'Music from a ceiling speaker'
lang['subtitles.office_rooms.mall.chime'] = 'An announcement chime'
w(L, lang)
missing = [x for x in ALL if x not in NAMES]
assert not missing, missing

# ---- sounds.json (audio: tools/gen_sounds_0430.py) -------------------------------------------------------------------
S = f'{A}/sounds.json'; snd = json.load(open(S))
snd['block.mall.muzak'] = {"category": "ambient", "subtitle": "subtitles.office_rooms.mall.muzak",
                           "sounds": [{"name": O + f"block/mall/muzak{k}", "attenuation_distance": 28, "stream": True} for k in (1, 2, 3)]}
snd['block.mall.chime'] = {"category": "ambient", "subtitle": "subtitles.office_rooms.mall.chime",
                           "sounds": [{"name": O + "block/mall/chime", "attenuation_distance": 48}]}
w(S, snd)
print('mall blocks with assets:', len(ALL))
