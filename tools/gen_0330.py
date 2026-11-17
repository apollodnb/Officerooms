"""0.33.0: one Level 0 palette for Level 0 and The Maze (drawn here): worn mono-yellow wallpaper with a faint
chevron stripe, damp beige-yellow carpet, off-white drop-ceiling tiles, fluorescent panels. Plus the wet carpet."""
import json, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
T = f'{A}/textures/block'
rnd = random.Random(330)
def n(a): return rnd.randint(-a, a)
def cl(c): return tuple(max(0, min(255, int(v))) for v in c) + (255,)
def tex(name, fn):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = cl(fn(x, y))
    im.save(f'{T}/{name}.png')
    return im

WALL = (212, 194, 108)
def wallpaper(x, y, worn=False):
    c = WALL
    if x % 4 == 0:
        c = (198, 180, 96)                                   # stripe seam
    elif x % 4 == 2 and (y + (x // 4) * 2) % 4 in (0, 1) and abs(((y + (x // 4) * 2) % 4) - 0.5) < 1:
        c = (205, 187, 102)                                  # faint chevron tick
    c = tuple(v + n(3) for v in c)
    if worn:
        if (x - 9) ** 2 / 9 + (y - 6) ** 2 / 16 < 1 + rnd.random() * 0.4:
            c = (226, 216, 170)                              # peeled to the paper underneath
        if y > 11:
            c = tuple(v * (0.8 + 0.04 * (15 - y)) for v in c)   # water creeping up from the carpet
    return c
tex('level0_wallpaper', wallpaper)
tex('level0_wallpaper_worn', lambda x, y: wallpaper(x, y, True))
ARROW = {(3, 8), (4, 8), (5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8), (9, 6), (10, 7), (9, 10), (10, 9), (8, 5), (8, 11), (4, 7), (6, 9)}
tex('level0_wallpaper_arrow', lambda x, y: (60, 20, 18) if (x, y) in ARROW or (x, y + 1) in ARROW and rnd.random() < 0.3 else wallpaper(x, y))
def carpet(x, y, base=(176, 156, 92), blot=0.0):
    c = base
    if rnd.random() < 0.22:
        c = tuple(v - 18 for v in base)
    if (x + y * 3) % 7 == 0:
        c = tuple(v + 8 for v in c)
    if blot and (x - 8) ** 2 + (y - 7) ** 2 < 30 * blot + rnd.random() * 8:
        c = tuple(v * 0.7 for v in c)
    return tuple(v + n(4) for v in c)
tex('level0_carpet', carpet)
tex('level0_carpet_stained', lambda x, y: carpet(x, y, blot=1.0))
tex('level0_carpet_wet', lambda x, y: (196, 186, 150) if rnd.random() < 0.03 else carpet(x, y, base=(132, 114, 66)))
def ceiling(x, y, stain=False):
    c = (214, 210, 190)
    if x == 0 or y == 0:
        c = (176, 172, 152)                                  # the T-bar grid of the drop ceiling
    elif rnd.random() < 0.08:
        c = (194, 190, 170)                                  # pinholes of the mineral fibre tile
    if stain:
        r = ((x - 9) ** 2 + (y - 9) ** 2) ** 0.5
        if r < 6:
            c = tuple(v * (0.82 if r > 4.6 else 0.92) for v in (206, 190, 140))
    return tuple(v + n(2) for v in c)
tex('level0_ceiling', ceiling)
tex('level0_ceiling_stained', lambda x, y: ceiling(x, y, True))
def light(x, y, on=True):
    if x in (0, 15) or y in (0, 15):
        return (160, 158, 150)
    if x in (1, 14) or y in (1, 14):
        return (200, 198, 190)
    tube = 4 <= x <= 5 or 10 <= x <= 11
    if on:
        return (255, 254, 240) if tube else (240, 238, 222) if (x + y) % 3 else (226, 224, 208)
    return (150, 150, 144) if tube else (186, 184, 172) if (x + y) % 3 else (176, 174, 162)
tex('level0_light', light)
tex('level0_light_off', lambda x, y: light(x, y, False))
# the Maze shares the palette exactly
for a, b in (('maze_wallpaper', 'level0_wallpaper'), ('maze_carpet', 'level0_carpet'), ('maze_ceiling', 'level0_ceiling')):
    Image.open(f'{T}/{b}.png').save(f'{T}/{a}.png')

def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
w(f'{A}/models/block/level0_carpet_wet.json', {"parent": "minecraft:block/cube_all", "textures": {"all": O + "block/level0_carpet_wet"}})
w(f'{A}/blockstates/level0_carpet_wet.json', {"variants": {"": {"model": O + "block/level0_carpet_wet"}}})
w(f'{A}/models/item/level0_carpet_wet.json', {"parent": O + "block/level0_carpet_wet"})
w(f'{D}/loot_tables/blocks/level0_carpet_wet.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "level0_carpet_wet"}],
  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
lang['block.office_rooms.level0_carpet_wet'] = 'Soaked Carpet'
json.dump(lang, open(L, 'w'), indent=1, ensure_ascii=False)
print('ok')

# ---- 0.34.0: the carpeted ramp (a continuous slope; walks like stairs) ---------------------------------------------
# model for facing east (high side east): a 45° carpet plane plus a stepped body under it (its side shows the wedge)
C = O + 'block/level0_carpet'
def box(f, t):
    return {"from": f, "to": t, "faces": {k: {"texture": "#c"} for k in ('north', 'south', 'east', 'west', 'up', 'down')}}
els = [box([0, 0, 0], [16, 0.5, 16])]
for i in range(1, 8):
    els.append(box([2 * i, 0, 0], [16, 2 * i, 16]))
slope = {"from": [-3.31, 7.9, 0], "to": [19.31, 8.1, 16], "rotation": {"origin": [8, 8, 8], "axis": "z", "angle": 45},
         "faces": {k: {"texture": "#c"} for k in ('north', 'south', 'up', 'down')}}
els.append(slope)
w(f'{A}/models/block/level0_ramp.json', {"parent": "minecraft:block/block", "textures": {"c": C, "particle": C}, "elements": els})
rv = {}
for f, y in (('east', 0), ('south', 90), ('west', 180), ('north', 270)):
    for half in ('bottom', 'top'):
        for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
            v = {"model": O + "block/level0_ramp"}
            if y: v["y"] = y
            if half == 'top': v["x"] = 180
            rv[f"facing={f},half={half},shape={shape}"] = v
w(f'{A}/blockstates/level0_ramp.json', {"variants": rv})
w(f'{A}/models/item/level0_ramp.json', {"parent": O + "block/level0_ramp"})
w(f'{D}/loot_tables/blocks/level0_ramp.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "level0_ramp"}],
  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
lang = json.load(open(L)); lang['block.office_rooms.level0_ramp'] = 'Carpeted Ramp'; json.dump(lang, open(L, 'w'), indent=1, ensure_ascii=False)
print('ramp ok')
