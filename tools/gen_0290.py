"""0.29.0: assets of the curiosity rooms (all drawn here, nothing copied)."""
import json, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(290)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def n(a=5): return rnd.randint(-a, a)
def cl(c, a=255): return tuple(max(0, min(255, int(v))) for v in c) + (a,)
def tex(name, fn, folder='block'):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            v = fn(x, y); px[x, y] = v if len(v) == 4 else cl(v)
    im.save(f'{A}/textures/{folder}/{name}.png')
def flat(c, a=4): return lambda x, y: tuple(v + n(a) for v in c)

# vending machine: faded red body, glass window with rows of goods, coin slot
tex('vending_side', flat((150, 40, 38), 5))
def vend_front(x, y):
    if 1 <= x <= 10 and 1 <= y <= 14:
        if y % 4 == 0: return (70, 74, 80)                       # shelf
        if 2 <= x <= 9 and x % 2 == 0: return [(200, 170, 60), (90, 150, 200), (210, 90, 70), (120, 180, 90)][(y // 4) % 4]
        return (34, 40, 46)
    if x >= 12 and 5 <= y <= 6: return (24, 24, 26)                 # coin slot
    if x >= 12 and 10 <= y <= 12: return (40, 40, 44)              # tray
    return (160 + n(4), 44 + n(3), 40 + n(3))
tex('vending_front', vend_front)
def vend_top(x, y):
    if 2 <= y <= 9 and 1 <= x <= 14: return (240, 226, 190) if (x + y) % 5 else (220, 120, 70)   # lit sign
    return (150 + n(4), 40, 38)
tex('vending_top_front', vend_top)
# arcade cabinet: dark purple, abstract lit screen, control panel
tex('arcade_side', lambda x, y: (56 + n(3), 30 + n(3), 74 + n(4)) if not (3 <= y <= 12 and x in (1, 14)) else (180, 60, 200))
def arcade_panel(x, y):
    if y < 4: return (30, 26, 36)
    if (x, y) in ((4, 7), (11, 6), (12, 8), (10, 9)): return [(230, 60, 60), (60, 200, 230), (240, 220, 60), (120, 230, 90)][(x + y) % 4]
    if 3 <= x <= 5 and 5 <= y <= 9: return (20, 20, 22)
    return (56 + n(3), 30 + n(3), 74 + n(3))
tex('arcade_panel', arcade_panel)
def arcade_screen(x, y):
    if x in (0, 15) or y in (0, 15): return (28, 24, 34)
    c = (8, 10, 22)
    if (x - 8) ** 2 + (y - 9) ** 2 < 6: c = (240, 220, 80)
    if y == 12 and 2 <= x <= 13: c = (60, 200, 230)
    if (x * 7 + y * 3) % 23 == 0: c = (200, 80, 220)
    if 3 <= x <= 6 and y == 3: c = (230, 230, 230)
    return c
tex('arcade_screen', arcade_screen)
# parcels: brown cardboard with tape
tex('parcel', lambda x, y: (196, 160, 112) if x in (7, 8) else (150 + n(5), 112 + n(4), 72 + n(3)))
tex('parcel_top', lambda x, y: (196, 160, 112) if x in (7, 8) or y in (7, 8) else (156 + n(5), 118 + n(4), 76 + n(3)))
# arcade carpet: dark blue with scattered geometric shapes (own pattern)
def arcade_carpet(x, y):
    c = (18 + n(2), 20 + n(2), 52 + n(3))
    if (x - 4) ** 2 + (y - 4) ** 2 in (4, 5): c = (220, 70, 180)
    if x == 11 and 9 <= y <= 13 or y == 11 and 9 <= x <= 13: c = (70, 210, 230)
    if (x + 2 * y) % 16 == 0 and x > 6: c = (240, 210, 60)
    return c
tex('arcade_carpet', arcade_carpet)
# old coin (item)
def coin(x, y):
    r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** .5
    if r > 6.2: return (0, 0, 0, 0)
    if r > 5.2: return (120, 92, 40)
    if r < 2.2: return (150, 118, 52) if (x + y) % 2 else (196, 160, 80)
    return (186 + n(6), 150 + n(6), 70 + n(4))
tex('office_coin', coin, 'item')

FACES = ('north', 'south', 'east', 'west', 'up', 'down')
def el(f, t, tx, glow=0, **over):
    e = {"from": f, "to": t, "faces": {k: {"texture": "#" + over.get(k, tx)} for k in FACES}}
    if glow: e["forge_data"] = {"block_light": glow, "sky_light": glow}
    return e
MODELS = {
 'vending_machine': ([el([1, 0, 1], [15, 16, 15], 's', north='f')], {'s': 'vending_side', 'f': 'vending_front'}),
 'vending_machine_top': ([el([1, 0, 1], [15, 15, 15], 's', north='t', glow=10)], {'s': 'vending_side', 't': 'vending_top_front'}),
 'arcade_cabinet': ([el([2, 0, 1], [14, 12, 15], 'c'), el([2, 12, 0], [14, 16, 9], 'c', up='p', north='c')], {'c': 'arcade_side', 'p': 'arcade_panel'}),
 'arcade_cabinet_top': ([el([2, 0, 3], [14, 15, 15], 'c'), el([3, 2, 2.5], [13, 13, 3], 'x', glow=12)], {'c': 'arcade_side', 'x': 'arcade_screen'}),
 'parcel_stack': ([el([1, 0, 1], [9, 7, 9], 'p', up='t'), el([8, 0, 6], [15, 6, 15], 'p', up='t'), el([3, 7, 3], [10, 13, 10], 'p', up='t')],
                  {'p': 'parcel', 't': 'parcel_top'}),
}
M = f'{A}/models/block'; YR = {"north": 0, "east": 90, "south": 180, "west": 270}
for name, (elems, texs) in MODELS.items():
    t = {k: O + 'block/' + v for k, v in texs.items()}; t['particle'] = t[next(iter(texs))]
    w(f'{M}/{name}.json', {"parent": "minecraft:block/block", "textures": t, "elements": elems})
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={d}": {"model": O + "block/" + name, "y": YR[d]} for d in YR}})
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + name})
w(f'{M}/arcade_carpet.json', {"parent": "minecraft:block/carpet", "textures": {"wool": O + "block/arcade_carpet"}})
w(f'{A}/blockstates/arcade_carpet.json', {"variants": {"": {"model": O + "block/arcade_carpet"}}})
w(f'{A}/models/item/arcade_carpet.json', {"parent": O + "block/arcade_carpet"})
w(f'{M}/rain_emitter.json', {"textures": {"particle": "minecraft:block/water_still"}, "elements": []})
w(f'{A}/blockstates/rain_emitter.json', {"variants": {"": {"model": O + "block/rain_emitter"}}})
w(f'{A}/models/item/office_coin.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "item/office_coin"}})
for name in list(MODELS) + ['arcade_carpet']:
    w(f'{D}/loot_tables/blocks/{name}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + name}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
p = f'{R}/data/minecraft/tags/blocks/mineable/pickaxe.json'; t = json.load(open(p))
for nm in ['vending_machine', 'vending_machine_top']:
    if O + nm not in t["values"]: t["values"].append(O + nm)
w(p, t)
p = f'{R}/data/minecraft/tags/blocks/mineable/axe.json'; t = json.load(open(p))
for nm in ['arcade_cabinet', 'arcade_cabinet_top', 'parcel_stack']:
    if O + nm not in t["values"]: t["values"].append(O + nm)
w(p, t)
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
lang.update({'block.office_rooms.vending_machine': 'Vending Machine', 'block.office_rooms.vending_machine_top': 'Vending Machine (Top)',
             'block.office_rooms.arcade_cabinet': 'Arcade Cabinet', 'block.office_rooms.arcade_cabinet_top': 'Arcade Cabinet (Screen)',
             'block.office_rooms.parcel_stack': 'Parcels', 'block.office_rooms.arcade_carpet': 'Arcade Carpet',
             'block.office_rooms.rain_emitter': 'Rain', 'item.office_rooms.office_coin': 'Old Coin'})
w(L, lang)
print('ok')
