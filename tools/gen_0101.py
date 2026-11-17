"""Office Rooms 0.10.1: textures, models, blockstates, lang, loot and recipe for the QoL tools."""
from PIL import Image
import json, os, random
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources')
A = os.path.join(R, 'assets/office_rooms')
rnd = random.Random(101)
def js(path, obj):
    path = os.path.join(R, path); os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=1)
def blank(): return Image.new('RGBA', (16, 16), (0, 0, 0, 0))
def put(im, pts, c):
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), c + (255,) if len(c) == 3 else c)
def line(x0, y0, x1, y1):
    n = max(abs(x1 - x0), abs(y1 - y0)); return [(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)) for i in range(n + 1)]
def thick(pts, w=2):
    out = set()
    for x, y in pts:
        for i in range(w):
            out.add((x + i, y)); out.add((x, y + i))
    return out
def save(im, name, folder='item'):
    p = os.path.join(A, 'textures', folder); os.makedirs(p, exist_ok=True); im.save(os.path.join(p, name + '.png'))

# glowstick: diagonal green tube with caps
im = blank()
for i, (x, y) in enumerate(line(3, 13, 12, 2)):
    put(im, [(x, y), (x + 1, y), (x, y + 1)], (90, 255, 120) if 1 < i < 9 else (40, 40, 44))
    put(im, [(x + 1, y + 1)], (40, 170, 70))
put(im, [(6, 9), (8, 7), (10, 5)], (200, 255, 210))
save(im, 'glowstick')
# bandage: rolled white gauze with a loose end
im = blank()
for y in range(4, 12):
    for x in range(3, 11):
        put(im, [(x, y)], (236, 232, 222) if (x + y) % 3 else (208, 202, 190))
put(im, [(x, 3) for x in range(4, 10)] + [(x, 12) for x in range(4, 10)], (180, 174, 162))
put(im, [(6, 7), (7, 7), (6, 8), (7, 8)], (150, 140, 128))
for x in range(10, 15):
    put(im, [(x, 9 + (x % 2)), (x, 10 + (x % 2))], (232, 226, 214))
put(im, [(12, 10)], (170, 40, 40))
save(im, 'bandage')
# chalk: white stick
im = blank()
for i, (x, y) in enumerate(line(4, 12, 11, 5)):
    put(im, [(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)], (246, 246, 240) if i < 7 else (220, 220, 214))
put(im, [(4, 13), (3, 12), (3, 13)], (200, 200, 196))
save(im, 'chalk')
# industrial flashlight: yellow body, black grip, big lens
im = blank()
for y in range(5, 11):
    for x in range(2, 10):
        put(im, [(x, y)], (230, 190, 40) if y not in (5, 10) else (170, 130, 20))
for x in range(3, 9, 2):
    for y in range(6, 10): put(im, [(x, y)], (30, 30, 32))
for y in range(3, 13):
    for x in range(10, 14):
        put(im, [(x, y)], (50, 50, 54) if x != 13 else (220, 240, 255))
put(im, [(13, 6), (13, 7), (13, 8), (13, 9)], (255, 255, 230))
save(im, 'industrial_flashlight')
# emergency flare: red stick, black cap, white band
im = blank()
for i, (x, y) in enumerate(line(3, 13, 11, 5)):
    c = (210, 40, 30) if i < 6 else (240, 240, 240) if i == 6 else (30, 30, 30)
    put(im, [(x, y), (x + 1, y), (x, y + 1), (x + 1, y + 1)], c)
put(im, [(12, 3), (13, 2), (12, 2)], (255, 160, 40))
save(im, 'emergency_flare')
# noise maker: small alarm box with bells
im = blank()
for y in range(6, 14):
    for x in range(3, 13): put(im, [(x, y)], (110, 116, 124) if x not in (3, 12) and y != 13 else (70, 74, 80))
for x, y in [(5, 8), (7, 8), (9, 8), (6, 10), (8, 10), (10, 10)]: put(im, [(x, y)], (30, 30, 34))
put(im, [(3, 4), (4, 4), (4, 3), (5, 3), (3, 5)], (220, 180, 50)); put(im, [(12, 4), (11, 4), (11, 3), (10, 3), (12, 5)], (220, 180, 50))
put(im, [(11, 12), (12, 12)], (230, 60, 50))
save(im, 'noise_maker')
# master key: ornate brass key
im = blank()
ring = [(x, y) for x in range(1, 7) for y in range(1, 7) if 4 <= (x - 3.5) ** 2 + (y - 3.5) ** 2 <= 8]
put(im, ring, (214, 170, 60)); put(im, [(3, 3), (4, 4)], (120, 30, 30))
for i, (x, y) in enumerate(line(6, 6, 13, 13)):
    put(im, [(x, y), (x + 1, y)], (196, 150, 50))
put(im, [(10, 12), (11, 13), (12, 11), (13, 12), (9, 13)], (170, 120, 30))
save(im, 'master_key')
# portable battery
im = blank()
for y in range(4, 14):
    for x in range(4, 12): put(im, [(x, y)], (40, 44, 50) if x in (4, 11) or y == 13 else (60, 66, 76))
for y in range(5, 9):
    for x in range(5, 11): put(im, [(x, y)], (70, 200, 110))
put(im, [(6, 2), (7, 2), (8, 2), (9, 2), (6, 3), (7, 3), (8, 3), (9, 3)], (170, 170, 176))
put(im, [(7, 10), (7, 11), (7, 12), (6, 11), (8, 11)], (240, 240, 240))
save(im, 'portable_battery')

# chalk mark textures (as seen by the viewer)
C = (238, 238, 232, 230)
marks = {
    'cross': thick(line(3, 3, 12, 12)) | thick(line(12, 3, 3, 12)),
    'right': thick(line(2, 7, 12, 7)) | thick(line(12, 7, 8, 3)) | thick(line(12, 7, 8, 11)),
    'left': thick(line(13, 7, 3, 7)) | thick(line(3, 7, 7, 3)) | thick(line(3, 7, 7, 11)),
    'up': thick(line(7, 13, 7, 3)) | thick(line(7, 3, 3, 7)) | thick(line(7, 3, 11, 7)),
    'down': thick(line(7, 2, 7, 12)) | thick(line(7, 12, 3, 8)) | thick(line(7, 12, 11, 8)),
}
for name, pts in marks.items():
    im = blank()
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16 and rnd.random() > 0.12:
            im.putpixel((x, y), (C[0] - rnd.randint(0, 20), C[1] - rnd.randint(0, 20), C[2] - rnd.randint(0, 20), C[3]))
    save(im, 'chalk_' + name, 'block')
    js('assets/office_rooms/models/block/chalk_mark_%s.json' % name, {"render_type": "minecraft:cutout", "ambientocclusion": False,
       "textures": {"particle": "office_rooms:block/chalk_" + name, "mark": "office_rooms:block/chalk_" + name},
       "elements": [{"from": [0, 0, 15.95], "to": [16, 16, 15.95], "shade": False,
                     "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#mark"}}}]})
rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
js('assets/office_rooms/blockstates/chalk_mark.json', {"variants": {"facing=%s,mark=%s" % (f, m): dict({"model": "office_rooms:block/chalk_mark_" + m}, **({"y": r} if r else {}))
                                                                     for f, r in rot.items() for m in marks}})
js('assets/office_rooms/blockstates/temp_light.json', {"variants": {"": {"model": "minecraft:block/air"}}})
items = ['glowstick', 'bandage', 'chalk', 'industrial_flashlight', 'emergency_flare', 'noise_maker', 'master_key', 'portable_battery']
for it in items:
    js('assets/office_rooms/models/item/%s.json' % it, {"parent": "minecraft:item/" + ("handheld" if it in ('chalk', 'industrial_flashlight', 'master_key') else "generated"),
                                                        "textures": {"layer0": "office_rooms:item/" + it}})
lang = {
    "item.office_rooms.glowstick": "Glowstick",
    "item.office_rooms.glowstick.desc": "Throw it: a dim green light that lasts about a minute.",
    "item.office_rooms.bandage": "Bandage",
    "item.office_rooms.bandage.desc": "Takes 2 seconds to apply, then heals slowly. You cannot run while applying it.",
    "item.office_rooms.chalk": "Chalk",
    "item.office_rooms.chalk.desc": "Mark walls. Click the left, right, top or bottom of a wall for an arrow, the middle for a cross.",
    "item.office_rooms.industrial_flashlight": "Industrial Flashlight",
    "item.office_rooms.industrial_flashlight.desc": "Right-click to switch on. A strong beam - but the charge runs out.",
    "item.office_rooms.emergency_flare": "Emergency Flare",
    "item.office_rooms.emergency_flare.desc": "Throw it: blinding red light for 20 seconds. Most creatures back away from it.",
    "item.office_rooms.noise_maker": "Noise Maker",
    "item.office_rooms.noise_maker.desc": "Throw it: rings for 10 seconds and draws creatures to it.",
    "item.office_rooms.master_key": "Master Key",
    "item.office_rooms.master_key.desc": "Opens any security door. It will not last forever.",
    "item.office_rooms.portable_battery": "Portable Battery",
    "item.office_rooms.portable_battery.desc": "Charges a flashlight in your other hand. Empty? Drain a lit ceiling light with it.",
    "entity.office_rooms.thrown_tool": "Thrown Tool",
    "block.office_rooms.chalk_mark": "Chalk Mark",
    "block.office_rooms.temp_light": "Light",
}
for f in ('en_us', 'de_de'):
    path = os.path.join(A, 'lang', f + '.json'); d = json.load(open(path)); d.update(lang); json.dump(d, open(path, 'w'), indent=2, ensure_ascii=False)

# loot: dedicated tools table, linked from the existing tiers
def item(name, w, cnt=None):
    e = {"type": "minecraft:item", "name": "office_rooms:" + name, "weight": w}
    if cnt: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": cnt[0], "max": cnt[1]}}]
    return e
js('data/office_rooms/loot_tables/tiers/tools.json', {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [
    item('glowstick', 30, (2, 5)), item('bandage', 24, (1, 3)), item('chalk', 16), item('emergency_flare', 8, (1, 2)),
    item('noise_maker', 8), item('portable_battery', 7), item('industrial_flashlight', 4)]}]})
T = os.path.join(R, 'data/office_rooms/loot_tables/tiers/')
def link(tier, w, extra=()):
    p = T + tier + '.json'; d = json.load(open(p)); e = d['pools'][0]['entries']
    if not any(x.get('name') == 'office_rooms:tiers/tools' for x in e):
        e.append({"type": "minecraft:loot_table", "name": "office_rooms:tiers/tools", "weight": w})
    for x in extra:
        if not any(y.get('name') == x['name'] for y in e): e.append(x)
    json.dump(d, open(p, 'w'), indent=2)
link('supplies', 19); link('common', 10); link('uncommon', 9); link('rare', 6)
link('special', 0, [item('master_key', 1)])
d = json.load(open(T + 'special.json')); d['pools'][0]['entries'] = [x for x in d['pools'][0]['entries'] if x.get('name') != 'office_rooms:tiers/tools']; json.dump(d, open(T + 'special.json', 'w'), indent=2)
C_ = os.path.join(R, 'data/office_rooms/loot_tables/chests/')
for f, c in (('maze_vault', 0.08), ('secure_storage_3', 0.06), ('poolrooms_vault', 0.03)):
    d = json.load(open(C_ + f + '.json'))
    if not any(e.get('name') == 'office_rooms:master_key' for p in d['pools'] for e in p['entries']):
        d['pools'].append({"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": c}],
                           "entries": [{"type": "minecraft:item", "name": "office_rooms:master_key"}]})
    json.dump(d, open(C_ + f + '.json', 'w'), indent=2)
js('data/office_rooms/recipes/bandage.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [{"item": "minecraft:paper"}, {"item": "minecraft:paper"}, {"item": "minecraft:white_wool"}],
    "result": {"item": "office_rooms:bandage", "count": 3}})
print('ok')
