"""Office Rooms 0.9.0: elevator plate assets and secure storage loot tables."""
from PIL import Image
import json, os, random
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources')
A = os.path.join(R, 'assets/office_rooms')
def js(path, obj):
    path = os.path.join(R, path); os.makedirs(os.path.dirname(path), exist_ok=True); json.dump(obj, open(path, 'w'), indent=1)
rnd = random.Random(90)
def tex(top):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = (96, 102, 112)
            if x in (0, 15) or y in (0, 15): c = (58, 62, 70)
            if top:
                if (x + y) % 4 == 0 and 1 < x < 14 and 1 < y < 14: c = (120, 126, 136)   # tread pattern
                # up / down arrows
                up = [(7, 3), (8, 3), (6, 4), (9, 4), (5, 5), (10, 5), (7, 4), (8, 4), (7, 5), (8, 5), (7, 6), (8, 6)]
                dn = [(x_, 15 - y_) for x_, y_ in up]
                if (x, y) in up: c = (90, 220, 255)
                if (x, y) in dn: c = (255, 190, 70)
            elif y in (7, 8): c = (230, 200, 60) if (x // 2) % 2 == 0 else (30, 30, 34)
            im.putpixel((x, y), tuple(max(0, min(255, v + rnd.randint(-3, 3))) for v in c) + (255,))
    return im
os.makedirs(os.path.join(A, 'textures/block'), exist_ok=True)
tex(True).save(os.path.join(A, 'textures/block/elevator_top.png'))
tex(False).save(os.path.join(A, 'textures/block/elevator_side.png'))
js('assets/office_rooms/blockstates/elevator.json', {"variants": {"": {"model": "office_rooms:block/elevator"}}})
js('assets/office_rooms/models/block/elevator.json', {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "top": "office_rooms:block/elevator_top", "bottom": "office_rooms:block/elevator_side", "side": "office_rooms:block/elevator_side"}})
js('assets/office_rooms/models/item/elevator.json', {"parent": "office_rooms:block/elevator"})
js('data/office_rooms/loot_tables/blocks/elevator.json', {"type": "minecraft:block", "pools": [{"rolls": 1,
    "entries": [{"type": "minecraft:item", "name": "office_rooms:elevator"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
js('data/office_rooms/recipes/elevator.json', {"type": "minecraft:crafting_shaped", "category": "redstone",
    "pattern": ["IPI", "IEI", "IRI"], "key": {"I": {"item": "minecraft:iron_ingot"}, "P": {"item": "minecraft:piston"},
    "E": {"item": "minecraft:ender_pearl"}, "R": {"item": "minecraft:redstone"}}, "result": {"item": "office_rooms:elevator", "count": 2}})
p = os.path.join(R, 'data/minecraft/tags/blocks/mineable/pickaxe.json'); d = json.load(open(p))
if 'office_rooms:elevator' not in d['values']: d['values'].append('office_rooms:elevator'); json.dump(d, open(p, 'w'), indent=1)
for f in ('en_us', 'de_de'):
    path = os.path.join(A, 'lang', f + '.json'); d = json.load(open(path))
    d["block.office_rooms.elevator"] = "Elevator Plate"
    json.dump(d, open(path, 'w'), indent=2, ensure_ascii=False)

# --- loot ------------------------------------------------------------------------------------
def item(name, w=1, cnt=None):
    e = {"type": "minecraft:item", "name": name, "weight": w}
    if cnt: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": cnt[0], "max": cnt[1]}}]
    return e
def tier(t, rolls=1, chance=None):
    pool = {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "name": "office_rooms:tiers/" + t}]}
    if chance is not None: pool["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    return pool
def chance(name, c):
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": c}], "entries": [item(name)]}
L = 'data/office_rooms/loot_tables/chests/'
js(L + 'secure_storage_supplies.json', {"type": "minecraft:chest", "pools": [tier('supplies', {"type": "minecraft:uniform", "min": 3, "max": 5}),
    tier('common', 2), chance('office_rooms:almond_water', 0.8)]})
js(L + 'secure_storage_1.json', {"type": "minecraft:chest", "pools": [tier('uncommon', 3), tier('rare', 1), tier('rare', 1, 0.5),
    tier('stories', 1, 0.5), chance('office_rooms:anomaly_detector', 0.25), chance('office_rooms:level_2_access_card', 0.20)]})
js(L + 'secure_storage_2.json', {"type": "minecraft:chest", "pools": [tier('uncommon', 2), tier('rare', 2), tier('special', 1),
    tier('special', 1, 0.5), tier('stories', 1, 0.5), chance('office_rooms:temporal_watch', 0.12),
    chance('office_rooms:level_3_access_card', 0.20)]})
js(L + 'secure_storage_3.json', {"type": "minecraft:chest", "pools": [tier('rare', 3), tier('special', 2), tier('multidimensional', 1),
    {"rolls": 1, "entries": [item('minecraft:netherite_ingot', 2, (1, 2)), item('minecraft:diamond', 3, (2, 5)),
                             item('office_rooms:glitched_ingot', 2), item('minecraft:enchanted_golden_apple', 1)]},
    chance('office_rooms:temporal_watch', 0.25), chance('office_rooms:totem_of_mercy', 0.10)]})
print('ok')
