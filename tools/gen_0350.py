"""0.35.0: Poolrooms - small mosaic tiles, water slides (chute, tube), pool ladders. Own designs."""
import json, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
T = f'{A}/textures/block'
rnd = random.Random(350)
def n(a): return rnd.randint(-a, a)
def tex(name, fn):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            c = fn(x, y); px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (255,)
    im.save(f'{T}/{name}.png')
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
# small square mosaic, 4 px tiles with thin grout, slight variation per tile
def mosaic(base, grout):
    shade = {(i, j): rnd.randint(-6, 4) for i in range(4) for j in range(4)}
    def fn(x, y):
        if x % 4 == 3 or y % 4 == 3:
            return tuple(v + n(2) for v in grout)
        s = shade[(x // 4, y // 4)]
        return tuple(v + s + n(1) for v in base)
    return fn
tex('pool_tile', mosaic((232, 230, 218), (196, 194, 182)))
tex('pool_floor_tile', mosaic((224, 220, 204), (186, 182, 166)))
tex('pool_slide', lambda x, y: (248, 246, 236) if (x + y) % 9 else (236, 234, 224))
tex('pool_slide_tube', lambda x, y: (238, 232, 214) if y % 8 else (222, 216, 196))
tex('pool_ladder', lambda x, y: (196, 200, 204) if (x + y) % 3 else (150, 156, 162))
C = lambda t: {k: {"texture": "#" + t} for k in ('north', 'south', 'east', 'west', 'up', 'down')}
def box(f, t, tx='c'):
    return {"from": f, "to": t, "faces": C(tx)}
# slide: the same continuous slope as the Level 0 ramp
els = [box([0, 0, 0], [16, 0.5, 16])] + [box([2 * i, 0, 0], [16, 2 * i, 16]) for i in range(1, 8)]
els.append({"from": [-3.31, 7.9, 0], "to": [19.31, 8.1, 16], "rotation": {"origin": [8, 8, 8], "axis": "z", "angle": 45},
            "faces": {k: {"texture": "#c"} for k in ('north', 'south', 'up', 'down')}})
w(f'{A}/models/block/pool_slide.json', {"parent": "minecraft:block/block", "textures": {"c": O + "block/pool_slide", "particle": O + "block/pool_slide"}, "elements": els})
sv = {}
for f, y in (('east', 0), ('south', 90), ('west', 180), ('north', 270)):
    for half in ('bottom', 'top'):
        for shape in ('straight', 'inner_left', 'inner_right', 'outer_left', 'outer_right'):
            v = {"model": O + "block/pool_slide"}
            if y: v["y"] = y
            if half == 'top': v["x"] = 180
            sv[f"facing={f},half={half},shape={shape}"] = v
w(f'{A}/blockstates/pool_slide.json', {"variants": sv})
w(f'{A}/models/block/pool_slide_tube.json', {"parent": "minecraft:block/cube_all", "textures": {"all": O + "block/pool_slide_tube"}})
w(f'{A}/blockstates/pool_slide_tube.json', {"variants": {"": {"model": O + "block/pool_slide_tube"}}})
# pool ladder: two steel rails rising from the deck and bending over into the water (front = north)
lad = []
for x in (3, 12):
    lad += [box([x, 0, 12], [x + 1, 13, 13]), box([x, 12, 3], [x + 1, 13, 13]), box([x, 4, 2], [x + 1, 13, 3])]
lad += [box([3, 6, 2], [13, 7, 3]), box([3, 9, 2], [13, 10, 3])]          # two rungs in the water
w(f'{A}/models/block/pool_ladder.json', {"parent": "minecraft:block/block", "textures": {"c": O + "block/pool_ladder", "particle": O + "block/pool_ladder"}, "elements": lad})
YR = {"north": 0, "east": 90, "south": 180, "west": 270}
w(f'{A}/blockstates/pool_ladder.json', {"variants": {f"facing={d}": {"model": O + "block/pool_ladder", "y": r} for d, r in YR.items()}})
for nm in ('pool_slide', 'pool_slide_tube', 'pool_ladder'):
    w(f'{A}/models/item/{nm}.json', {"parent": O + "block/" + nm})
    w(f'{D}/loot_tables/blocks/{nm}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + nm}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
lang.update({'block.office_rooms.pool_slide': 'Water Slide', 'block.office_rooms.pool_slide_tube': 'Slide Tube', 'block.office_rooms.pool_ladder': 'Pool Ladder'})
json.dump(lang, open(L, 'w'), indent=1, ensure_ascii=False)
print('ok')

# ---- slide opening in an office wall (model front = north, back against the wall at south) -------------------------
tex('slide_entrance_plastic', lambda x, y: (240, 236, 222) if (x + y) % 7 else (226, 222, 206))
tex('slide_entrance_dark', lambda x, y: (12, 20, 26) if rnd.random() > 0.04 else (60, 110, 130))
ring = [box([0, 0, 9], [16, 3, 16], 'p'), box([0, 13, 9], [16, 16, 16], 'p'), box([0, 3, 9], [3, 13, 16], 'p'), box([13, 3, 9], [16, 13, 16], 'p'),
        box([3, 3, 9], [4, 4, 16], 'p'), box([12, 3, 9], [13, 4, 16], 'p'), box([3, 12, 9], [4, 13, 16], 'p'), box([12, 12, 9], [13, 13, 16], 'p'),
        box([3, 3, 15], [13, 13, 16], 'd'), box([2, 0, 6], [14, 1, 9], 'p')]                 # dark tube inside, a lip below
w(f'{A}/models/block/slide_entrance.json', {"parent": "minecraft:block/block", "textures": {"p": O + "block/slide_entrance_plastic",
  "d": O + "block/slide_entrance_dark", "particle": O + "block/slide_entrance_plastic"}, "elements": ring})
w(f'{A}/blockstates/slide_entrance.json', {"variants": {f"facing={d}": {"model": O + "block/slide_entrance", "y": r} for d, r in YR.items()}})
w(f'{A}/models/item/slide_entrance.json', {"parent": O + "block/slide_entrance"})
w(f'{D}/loot_tables/blocks/slide_entrance.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "slide_entrance"}],
  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
lang = json.load(open(L)); lang['block.office_rooms.slide_entrance'] = 'Slide Opening'
lang['subtitles.office_rooms.slide.rush'] = 'Rushing through a tube'; lang['subtitles.office_rooms.slide.splash'] = 'Splash'
json.dump(lang, open(L, 'w'), indent=1, ensure_ascii=False)
# sounds: the ride (a rumbling, watery rush) and the splash at the end
import subprocess
S = f'{A}/sounds/block/slide'; os.makedirs(S, exist_ok=True)
def sx(a): subprocess.run(['sox', '-V1'] + a, check=True)
sx(['-n', '-r', '44100', '-c', '1', '/tmp/sl_a.wav', 'synth', '3.0', 'brownnoise', 'tremolo', '7', '40', 'bandpass', '600', '500'])
sx(['-n', '-r', '44100', '-c', '1', '/tmp/sl_b.wav', 'synth', '3.0', 'pinknoise', 'tremolo', '13', '60', 'highpass', '2500', 'gain', '-10'])
sx(['-m', '/tmp/sl_a.wav', '/tmp/sl_b.wav', '-C', '5', f'{S}/rush.ogg', 'fade', 'q', '0.4', '-0', '0.6', 'reverb', '40', 'norm', '-1'])
sx(['-n', '-r', '44100', '-c', '1', '-C', '5', f'{S}/splash.ogg', 'synth', '1.2', 'whitenoise', 'lowpass', '3000', 'fade', 'p', '0.005', '-0', '1.1', 'reverb', '50', 'norm', '-1'])
SJ = f'{A}/sounds.json'; sj = json.load(open(SJ))
sj['block.slide.rush'] = {"category": "player", "subtitle": "subtitles.office_rooms.slide.rush", "sounds": [O + "block/slide/rush"]}
sj['block.slide.splash'] = {"category": "player", "subtitle": "subtitles.office_rooms.slide.splash", "sounds": [O + "block/slide/splash"]}
json.dump(sj, open(SJ, 'w'), indent=2)
print('slide entrance ok')

# ---- stainless tube handrail (runs along x when facing north) ----------------------------------------------------
tex('pool_handrail', lambda x, y: (214, 218, 222) if y % 4 else (170, 176, 182))
hr = [box([0, 13, 7.25], [16, 14.5, 8.75]), box([1.25, 0, 7.25], [2.75, 13, 8.75]), box([13.25, 0, 7.25], [14.75, 13, 8.75]),
      box([0.5, 0, 6.5], [3.5, 0.5, 9.5]), box([12.5, 0, 6.5], [15.5, 0.5, 9.5])]
w(f'{A}/models/block/pool_handrail.json', {"parent": "minecraft:block/block", "textures": {"c": O + "block/pool_handrail", "particle": O + "block/pool_handrail"}, "elements": hr})
w(f'{A}/blockstates/pool_handrail.json', {"variants": {f"facing={d}": {"model": O + "block/pool_handrail", "y": r} for d, r in YR.items()}})
w(f'{A}/models/item/pool_handrail.json', {"parent": O + "block/pool_handrail"})
w(f'{D}/loot_tables/blocks/pool_handrail.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "pool_handrail"}],
  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
lang = json.load(open(L)); lang['block.office_rooms.pool_handrail'] = 'Pool Handrail'; json.dump(lang, open(L, 'w'), indent=1, ensure_ascii=False)
print('handrail ok')

# ---- 0.35.1: round slide pipes out of the wall (front/mouth = north, wall side = south) -----------------------------
tex('pool_slide_pipe_inner', lambda x, y: (150, 140, 120) if (x + y) % 5 else (132, 122, 104))
def ring(z0, z1, ztop=None, mouth=False):
    zt = z0 if ztop is None else ztop
    def e(f, t, rot=None, tx='c'):
        el = {"from": f, "to": t, "faces": {k: {"texture": "#" + tx} for k in ('north', 'south', 'east', 'west', 'up', 'down')}}
        if rot is not None:
            el["rotation"] = {"origin": [8, 8, 8], "axis": "z", "angle": rot}
        return el
    els = [e([4, 13, zt], [12, 15, 16]), e([4, 1, z0], [12, 3, 16]),                 # top, bottom
           e([4, 13, zt], [12, 15, 16], 45), e([4, 13, zt], [12, 15, 16], -45),       # upper diagonals
           e([4, 1, z0], [12, 3, 16], 45), e([4, 1, z0], [12, 3, 16], -45)]           # lower diagonals
    if mouth:                                                                          # sides cut back like a slanted mouth
        els += [e([1, 4, z0 + 2], [3, 8, 16]), e([1, 8, z0 + 5], [3, 12, 16]), e([13, 4, z0 + 2], [15, 8, 16]), e([13, 8, z0 + 5], [15, 12, 16])]
    else:
        els += [e([1, 4, z0], [3, 12, 16]), e([13, 4, z0], [15, 12, 16])]
    return els
w(f'{A}/models/block/pool_slide_pipe.json', {"parent": "minecraft:block/block", "textures": {"c": O + "block/pool_slide_tube", "particle": O + "block/pool_slide_tube"},
  "elements": ring(0, 16)})
w(f'{A}/models/block/pool_slide_pipe_end.json', {"parent": "minecraft:block/block", "textures": {"c": O + "block/pool_slide_tube", "particle": O + "block/pool_slide_tube"},
  "elements": ring(0, 16, ztop=7, mouth=True)})
for nm in ('pool_slide_pipe', 'pool_slide_pipe_end'):
    w(f'{A}/blockstates/{nm}.json', {"variants": {f"facing={d}": {"model": O + "block/" + nm, "y": r} for d, r in YR.items()}})
    w(f'{A}/models/item/{nm}.json', {"parent": O + "block/" + nm})
    w(f'{D}/loot_tables/blocks/{nm}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + nm}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
lang = json.load(open(L)); lang['block.office_rooms.pool_slide_pipe'] = 'Slide Pipe'; lang['block.office_rooms.pool_slide_pipe_end'] = 'Slide Pipe End'
json.dump(lang, open(L, 'w'), indent=1, ensure_ascii=False)
# the office slide opening: flat against the wall, a round dark hole in a white collar
ring2 = [box([0, 0, 13], [16, 3, 16], 'p'), box([0, 13, 13], [16, 16, 16], 'p'), box([0, 3, 13], [3, 13, 16], 'p'), box([13, 3, 13], [16, 13, 16], 'p'),
         box([3, 3, 13], [5, 5, 16], 'p'), box([11, 3, 13], [13, 5, 16], 'p'), box([3, 11, 13], [5, 13, 16], 'p'), box([11, 11, 13], [13, 13, 16], 'p'),
         box([3, 3, 15.8], [13, 13, 16], 'd'), box([2, 0, 11], [14, 1, 13], 'p')]
w(f'{A}/models/block/slide_entrance.json', {"parent": "minecraft:block/block", "textures": {"p": O + "block/slide_entrance_plastic",
  "d": O + "block/slide_entrance_dark", "particle": O + "block/slide_entrance_plastic"}, "elements": ring2})
print('pipes ok')
