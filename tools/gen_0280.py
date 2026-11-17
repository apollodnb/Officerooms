"""0.28.0: assets of the hotel rework (all drawn here, nothing copied)."""
import json, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(280)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def n(a=5): return rnd.randint(-a, a)
def cl(c, a=255): return tuple(max(0, min(255, int(v))) for v in c) + (a,)
def tex(name, fn):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            v = fn(x, y); px[x, y] = v if len(v) == 4 else cl(v)
    im.save(f'{A}/textures/block/{name}.png')
def flat(c, a=4): return lambda x, y: tuple(v + n(a) for v in c)

# carpet of the upper guest floor: faded teal with a small diamond pattern
def teal_carpet(x, y):
    d = (x + y) % 8 == 0 or (x - y) % 8 == 0
    return (40 + n(4), 84 + n(4), 82 + n(4)) if not d else (150 + n(5), 128 + n(5), 70 + n(4))
tex('hotel_carpet_teal', teal_carpet)
tex('phone_body', flat((178, 160, 128), 4))
def phone_dial(x, y):
    r = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** .5
    return (230, 226, 210) if r < 2 else (60 + n(3), 56, 50) if r < 6 else (178 + n(4), 160 + n(4), 128 + n(3))
tex('phone_dial', phone_dial)
def static(x, y):
    v = rnd.randint(40, 235)
    if 0 in (x, y) or 15 in (x, y): return (30, 30, 32)
    return (v, v, v + 6)
tex('static_screen', static)
tex('tv_case', flat((54, 46, 40), 4))
def ice_front(x, y):
    if y < 3: return (190 + n(3), 196, 200)
    if 4 <= y <= 9 and 3 <= x <= 12: return (90 + n(6), 120 + n(6), 140 + n(6))
    if y == 12 and 5 <= x <= 10: return (40, 40, 44)
    return (206 + n(4), 210 + n(4), 214 + n(3))
tex('ice_machine_front', ice_front)
tex('ice_machine_side', flat((196, 200, 204), 4))
tex('cart_frame', flat((70, 72, 78), 4))
def towels(x, y):
    return (226 + n(4), 224 + n(4), 214 + n(4)) if y % 4 else (196, 192, 182)
tex('cart_linen', towels)
tex('cart_bag', flat((56, 44, 70), 5))
def shade(x, y):
    return (236 + n(3), 200 + n(4), 140 + n(4)) if 2 < y < 14 else (196, 160, 110)
tex('sconce_shade', shade)
tex('brass2', flat((168, 136, 70), 6))

FACES = ('north', 'south', 'east', 'west', 'up', 'down')
def el(f, t, tx, glow=0, **over):
    e = {"from": f, "to": t, "faces": {k: {"texture": "#" + over.get(k, tx)} for k in FACES}}
    if glow: e["forge_data"] = {"block_light": glow, "sky_light": glow}
    return e
MODELS = {
 'hotel_phone': ([el([4, 0, 5], [12, 3, 12], 'b', up='d'), el([3.5, 3, 6], [12.5, 4.5, 8], 'b')], {'b': 'phone_body', 'd': 'phone_dial'}),
 'static_tv': ([el([2, 0, 3], [14, 12, 15], 'c', north='s'), el([2.5, 1, 2.6], [13.5, 11, 3], 's', glow=9)], {'c': 'tv_case', 's': 'static_screen'}),
 'ice_machine': ([el([1, 0, 2], [15, 16, 16], 'm', north='f')], {'m': 'ice_machine_side', 'f': 'ice_machine_front'}),
 'housekeeping_cart': ([el([1, 1, 3], [15, 2, 13], 'k'), el([1, 8, 3], [15, 9, 13], 'k'), el([1, 0, 3], [2, 12, 4], 'k'), el([14, 0, 3], [15, 12, 4], 'k'),
                        el([1, 0, 12], [2, 12, 13], 'k'), el([14, 0, 12], [15, 12, 13], 'k'), el([2, 2, 4], [9, 8, 12], 'l'), el([9, 2, 4], [14, 13, 12], 'g'),
                        el([2, 9, 4], [9, 12, 12], 'l')], {'k': 'cart_frame', 'l': 'cart_linen', 'g': 'cart_bag'}),
 'hotel_sconce': ([el([7, 4, 14], [9, 9, 16], 'b'), el([5, 8, 11], [11, 13, 15], 's', glow=12)], {'b': 'brass2', 's': 'sconce_shade'}),
}
M = f'{A}/models/block'; YR = {"north": 0, "east": 90, "south": 180, "west": 270}
for name, (elems, texs) in MODELS.items():
    t = {k: O + 'block/' + v for k, v in texs.items()}; t['particle'] = t[next(iter(texs))]
    w(f'{M}/{name}.json', {"parent": "minecraft:block/block", "textures": t, "elements": elems})
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={d}": {"model": O + "block/" + name, "y": YR[d]} for d in YR}})
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + name})
w(f'{M}/hotel_carpet_teal.json', {"parent": "minecraft:block/cube_all", "textures": {"all": O + "block/hotel_carpet_teal"}})
w(f'{A}/blockstates/hotel_carpet_teal.json', {"variants": {"": {"model": O + "block/hotel_carpet_teal"}}})
w(f'{A}/models/item/hotel_carpet_teal.json', {"parent": O + "block/hotel_carpet_teal"})
for name in list(MODELS) + ['hotel_carpet_teal']:
    w(f'{D}/loot_tables/blocks/{name}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + name}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
p = f'{R}/data/minecraft/tags/blocks/mineable/pickaxe.json'; t = json.load(open(p))
for nm in ['static_tv', 'ice_machine', 'housekeeping_cart', 'hotel_sconce', 'hotel_phone']:
    if O + nm not in t["values"]: t["values"].append(O + nm)
w(p, t)
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
lang.update({'block.office_rooms.hotel_phone': 'Hotel Phone', 'block.office_rooms.static_tv': 'Old TV', 'block.office_rooms.ice_machine': 'Ice Machine',
             'block.office_rooms.housekeeping_cart': 'Housekeeping Cart', 'block.office_rooms.hotel_sconce': 'Wall Sconce',
             'block.office_rooms.hotel_carpet_teal': 'Hotel Carpet (Teal)'})
w(L, lang)
print('ok')
