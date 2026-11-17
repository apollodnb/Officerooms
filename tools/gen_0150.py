"""0.15.0: Industrial Complex blocks, grate door, electronic component, loot tables, lang."""
import json, os, random, math, zipfile
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(150)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def save(name, frames, anim=None, kind='block'):
    im = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames): im.paste(f, (0, 16 * i))
    p = f'{A}/textures/{kind}/{name}.png'; os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
    if anim: w(p + '.mcmeta', {"animation": {"frametime": anim}})
def noise(base, var, a=255):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            d = rnd.randint(-var, var); im.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (a,))
    return im
def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), tuple(c) + ((255,) if len(c) == 3 else ()))
wall = noise((104, 110, 104), 6); d = ImageDraw.Draw(wall); d.rectangle([0, 0, 15, 15], outline=(78, 84, 78)); d.line([0, 7, 15, 7], fill=(86, 92, 86))
for x, y in ((2, 2), (13, 2), (2, 12), (13, 12)): put(wall, x, y, (150, 154, 146))
for _ in range(10):
    x, y = rnd.randrange(16), rnd.randrange(8, 16); put(wall, x, y, (88, 82, 70))
save('industrial_wall', [wall])
fl = noise((70, 72, 72), 6)
for _ in range(6): put(fl, rnd.randrange(16), rnd.randrange(16), (50, 52, 52))
d = ImageDraw.Draw(fl); d.line([0, 0, 15, 0], fill=(58, 60, 60)); d.line([0, 0, 0, 15], fill=(58, 60, 60))
save('industrial_floor', [fl])
ce = noise((88, 90, 92), 4); d = ImageDraw.Draw(ce); d.line([0, 0, 15, 0], fill=(64, 66, 68)); d.line([0, 8, 15, 8], fill=(64, 66, 68))
save('industrial_ceiling', [ce])
gr = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for x in range(16):
    for y in range(16):
        if x % 4 == 0 or y % 4 == 0: gr.putpixel((x, y), (80 + rnd.randint(-10, 10), 80, 76, 255))
save('grate', [gr])
cas = noise((92, 96, 98), 5); d = ImageDraw.Draw(cas); d.rectangle([0, 0, 15, 15], outline=(60, 62, 64))
for y in range(3, 11, 2): d.line([3, y, 12, y], fill=(40, 42, 44))
for x in range(16):
    for y in (13, 14): put(cas, x, y, (210, 170, 30) if ((x + y) // 2) % 2 else (30, 30, 30))
save('generator_casing', [cas])
def core(i):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            r = math.hypot(x - 7.5, y - 7.5); v = 0.5 + 0.5 * math.sin(r * 0.9 - i * 1.2)
            im.putpixel((x, y), (int(200 + 55 * v), int(90 + 110 * v), int(20 + 40 * v), 255))
    d = ImageDraw.Draw(im); d.rectangle([0, 0, 15, 15], outline=(60, 62, 64)); d.rectangle([1, 1, 14, 14], outline=(90, 94, 96))
    return im
save('generator_core', [core(i) for i in range(6)], 3)
dead = noise((40, 38, 36), 5); d = ImageDraw.Draw(dead); d.rectangle([0, 0, 15, 15], outline=(60, 62, 64)); d.rectangle([1, 1, 14, 14], outline=(90, 94, 96))
d.line([4, 3, 9, 11], fill=(22, 20, 20)); save('generator_core_dead', [dead])
cage = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for x in range(16):
    for y in range(16):
        if x % 5 == 0 or y in (0, 15): cage.putpixel((x, y), (40, 40, 38, 255))
save('cage_lamp_cage', [cage])
bulb = noise((255, 226, 150), 6); save('cage_lamp_bulb', [bulb])
bulbd = noise((70, 66, 58), 5); save('cage_lamp_bulb_dead', [bulbd])
def doorpart(top):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(70, 72, 70)); d.rectangle([1, 0 if top else 1, 14, 15 if top else 14], outline=(96, 98, 94))
    for x in range(3, 14, 3): d.line([x, 1, x, 14], fill=(84, 86, 82))
    for y in ((4, 11) if top else (3, 12)): d.line([1, y, 14, y], fill=(84, 86, 82))
    if not top: d.rectangle([12, 1, 13, 3], fill=(150, 130, 60))
    return im
save('grate_door_top', [doorpart(True)]); save('grate_door_bottom', [doorpart(False)])
it = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(it)
d.rectangle([3, 4, 12, 11], fill=(30, 110, 50), outline=(20, 70, 30))
for x in (4, 6, 8, 10): d.line([x, 2, x, 3], fill=(200, 190, 120)); d.line([x, 12, x, 13], fill=(200, 190, 120))
d.rectangle([6, 6, 9, 9], fill=(30, 30, 34)); put(it, 5, 5, (220, 60, 50)); put(it, 10, 10, (230, 210, 80))
save('electronic_component', [it], kind='item')
w(f'{A}/models/item/electronic_component.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "item/electronic_component"}})
# --- models / blockstates -----------------------------------------------------------------------------------
T = lambda n: O + 'block/' + n
def item(n, parent=None): w(f'{A}/models/item/{n}.json', {"parent": parent or O + "block/" + n})
def cube(n, cutout=False):
    m = {"parent": "minecraft:block/cube_all", "textures": {"all": T(n)}}
    if cutout: m["render_type"] = "minecraft:cutout"
    w(f'{A}/models/block/{n}.json', m); w(f'{A}/blockstates/{n}.json', {"variants": {"": {"model": O + "block/" + n}}}); item(n)
for n in ('industrial_wall', 'industrial_floor', 'industrial_ceiling', 'generator_casing', 'generator_core', 'generator_core_dead'): cube(n)
cube('grate', True)
def el(fr, to, tx, faces=('north', 'south', 'east', 'west', 'up', 'down'), glow=False):
    e = {"from": fr, "to": to, "faces": {f: {"texture": tx} for f in faces}}
    if glow: e["forge_data"] = {"block_light": 15, "sky_light": 15}
    return e
for name, b, g in (('cage_lamp', 'cage_lamp_bulb', True), ('cage_lamp_broken', 'cage_lamp_bulb_dead', False)):
    w(f'{A}/models/block/{name}.json', {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"c": T('cage_lamp_cage'), "b": T(b), "m": T('generator_casing'), "particle": T('generator_casing')},
        "elements": [el([5, 15, 5], [11, 16, 11], "#m"), el([6.5, 10.5, 6.5], [9.5, 14, 9.5], "#b", glow=g), el([5, 9, 5], [11, 15, 11], "#c")]})
    item(name)
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={f}": {"model": O + "block/" + name} for f in ("north", "east", "south", "west")}})
for part in ('bottom_left', 'bottom_left_open', 'bottom_right', 'bottom_right_open', 'top_left', 'top_left_open', 'top_right', 'top_right_open'):
    w(f'{A}/models/block/grate_door_{part}.json', {"parent": "minecraft:block/door_" + part, "render_type": "minecraft:cutout",
        "textures": {"bottom": T('grate_door_bottom'), "top": T('grate_door_top')}})
jar = '/root/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar'
bs = json.loads(zipfile.ZipFile(jar).read('assets/minecraft/blockstates/iron_door.json').decode().replace('minecraft:block/iron_door', O + 'block/grate_door'))
w(f'{A}/blockstates/grate_door.json', bs)
gi = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(gi); d.rectangle([4, 0, 11, 15], outline=(70, 72, 70))
for y in range(2, 15, 3): d.line([5, y, 10, y], fill=(96, 98, 94))
save('grate_door', [gi], kind='item')
w(f'{A}/models/item/grate_door.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "item/grate_door"}})
# --- data -----------------------------------------------------------------------------------------------------
names = ['industrial_wall', 'industrial_floor', 'industrial_ceiling', 'grate', 'generator_casing', 'generator_core', 'generator_core_dead', 'cage_lamp', 'cage_lamp_broken']
for n in names:
    w(f'{D}/loot_tables/blocks/{n}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + n}],
                                                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
w(f'{D}/loot_tables/blocks/grate_door.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "grate_door",
    "conditions": [{"condition": "minecraft:block_state_property", "block": O + "grate_door", "properties": {"half": "lower"}}]}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
p = f'{R}/data/minecraft/tags/blocks/mineable/pickaxe.json'; t = json.load(open(p))
t["values"] += [O + m for m in names + ['grate_door'] if O + m not in t["values"]]; w(p, t)
for tag in ('doors',):
    p = f'{R}/data/minecraft/tags/blocks/{tag}.json'; t = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    if O + 'grate_door' not in t["values"]: t["values"].append(O + 'grate_door')
    w(p, t)
def tier(n, rolls): return {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "name": f"office_rooms:tiers/{n}"}]}
def it2(i, chance, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": i}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": chance}], "entries": [e]}
U = lambda a, b: {"type": "minecraft:uniform", "min": a, "max": b}
M = 'minecraft:'
T3 = {
 "industrial_control": [tier("uncommon", U(1, 2)), tier("common", U(1, 3)), it2(O + "electronic_component", .35, 1, 2), it2(O + "level_1_access_card", .25),
                        it2(O + "level_2_access_card", .08), it2(O + "research_file", .10), it2(O + "industrial_flashlight", .2), it2(O + "portable_battery", .3)],
 "industrial_storage": [tier("supplies", U(2, 4)), tier("tools", U(0, 2)), it2(M + "iron_ingot", .6, 2, 6), it2(M + "copper_ingot", .6, 3, 9),
                        it2(M + "redstone", .5, 2, 8), it2(O + "electronic_component", .2), it2(O + "emergency_flare", .3, 1, 2)],
 "industrial_generator": [tier("rare", U(1, 2)), it2(O + "electronic_component", .7, 1, 3), it2(O + "research_file", .25, 1, 2), it2(O + "glitched_ingot", .3, 1, 2),
                          it2(M + "redstone_block", .4, 1, 3), it2(O + "portable_battery", .5)],
}
for n, pools in T3.items(): w(f'{D}/loot_tables/chests/{n}.json', {"type": "minecraft:chest", "pools": pools})
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({"block.office_rooms.industrial_wall": "Industrial Wall", "block.office_rooms.industrial_floor": "Industrial Floor",
 "block.office_rooms.industrial_ceiling": "Industrial Ceiling", "block.office_rooms.grate": "Metal Grate", "block.office_rooms.generator_casing": "Generator Casing",
 "block.office_rooms.generator_core": "Generator Core", "block.office_rooms.generator_core_dead": "Dead Generator Core", "block.office_rooms.cage_lamp": "Cage Lamp",
 "block.office_rooms.cage_lamp_broken": "Broken Cage Lamp", "block.office_rooms.grate_door": "Grate Door", "item.office_rooms.electronic_component": "Electronic Component"})
json.dump(L, open(lp, 'w'), indent=2, ensure_ascii=False); print("ok")
