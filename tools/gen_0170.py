"""0.17.0: HAZARD ZONE assets: gear items + armour layers, filter, crystal, detector, monitor, loot, recipes, lang."""
import json, os, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(170)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def img(path, im):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)
def item_png(name, draw):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); draw(ImageDraw.Draw(im), im); img(f'{A}/textures/item/{name}.png', im)
    w(f'{A}/models/item/{name}.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "item/" + name}})
GREY, DARK, GREEN, YEL, BLK = (120, 124, 120), (50, 52, 50), (90, 220, 80), (232, 196, 40), (24, 24, 24)
def mask(d, im):
    d.ellipse([2, 2, 13, 14], fill=GREY, outline=DARK); d.ellipse([3, 4, 6, 7], fill=(150, 200, 210), outline=BLK); d.ellipse([9, 4, 12, 7], fill=(150, 200, 210), outline=BLK)
    d.rectangle([6, 10, 9, 14], fill=(90, 100, 70), outline=BLK); d.line([7, 11, 7, 13], fill=(60, 66, 50))
def goggles(d, im):
    d.line([0, 8, 15, 8], fill=BLK, width=2); d.rectangle([2, 5, 6, 10], fill=DARK, outline=BLK); d.rectangle([9, 5, 13, 10], fill=DARK, outline=BLK)
    d.ellipse([3, 6, 5, 9], fill=GREEN); d.ellipse([10, 6, 12, 9], fill=GREEN)
def visor(d, im):
    d.ellipse([1, 1, 14, 13], fill=YEL, outline=(150, 120, 20)); d.rectangle([3, 5, 12, 8], fill=(40, 120, 40), outline=BLK); d.line([4, 6, 11, 6], fill=GREEN)
    d.rectangle([6, 11, 9, 14], fill=(90, 100, 70), outline=BLK)
def filt(d, im):
    d.rectangle([4, 2, 11, 13], fill=(96, 110, 64), outline=BLK); d.rectangle([4, 6, 11, 8], fill=YEL); d.rectangle([6, 0, 9, 2], fill=DARK)
def crystal(d, im):
    d.polygon([(8, 0), (12, 6), (10, 15), (6, 15), (4, 6)], fill=(70, 210, 60), outline=(20, 90, 20)); d.line([8, 2, 8, 13], fill=(170, 255, 150))
def detector(d, im):
    d.rectangle([3, 1, 12, 15], fill=YEL, outline=(120, 96, 20)); d.rectangle([4, 3, 11, 9], fill=(10, 30, 12), outline=BLK)
    d.polygon([(7, 4), (9, 4), (8, 7)], fill=(255, 60, 40)); d.ellipse([6, 11, 9, 14], fill=BLK); d.point((10, 12), fill=(255, 60, 40))
for n, f in (('gas_mask', mask), ('night_vision_goggles', goggles), ('hazard_visor', visor), ('gas_filter', filt), ('toxic_crystal', crystal), ('special_event_detector', detector)):
    item_png(n, f)
# armour layers (64 × 32, head box at u 0..32, v 0..16; front face 8..16 × 8..16)
def armour(name, kind):
    im = Image.new('RGBA', (64, 32), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
    for x0 in (0, 8, 16, 24):                                   # a strap all around the head
        d.line([x0, 11, x0 + 7, 11], fill=BLK)
    d.line([8, 2, 15, 2], fill=BLK)                              # strap over the top
    if kind in ('mask', 'visor'):
        d.rectangle([8, 12, 15, 15], fill=GREY); d.rectangle([10, 13, 13, 15], fill=(90, 100, 70)); d.line([11, 14, 12, 14], fill=DARK)
    if kind == 'mask':
        d.rectangle([8, 10, 15, 12], fill=GREY); d.rectangle([9, 10, 10, 11], fill=(150, 200, 210)); d.rectangle([13, 10, 14, 11], fill=(150, 200, 210))
    if kind == 'goggles':
        d.rectangle([9, 10, 10, 11], fill=GREEN); d.rectangle([13, 10, 14, 11], fill=GREEN); d.point((11, 11), fill=DARK); d.point((12, 11), fill=DARK)
    if kind == 'visor':
        d.rectangle([8, 0, 15, 7], fill=YEL); d.rectangle([0, 8, 7, 10], fill=YEL); d.rectangle([16, 8, 31, 10], fill=YEL)
        d.rectangle([8, 8, 15, 9], fill=YEL); d.rectangle([8, 10, 15, 11], fill=(40, 140, 40))
    img(f'{A}/textures/models/armor/{name}_layer_1.png', im)
armour('gas_mask', 'mask'); armour('night_vision_goggles', 'goggles'); armour('hazard_visor', 'visor')
# monitor
def scr(broken):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            v = 8 + (y % 2) * 4 + rnd.randint(0, 3); im.putpixel((x, y), (v, v + 6, v, 255))
    d = ImageDraw.Draw(im); d.rectangle([0, 0, 15, 15], outline=(36, 38, 40))
    if broken:
        d.line([3, 2, 8, 9], fill=(150, 160, 160)); d.line([8, 9, 13, 6], fill=(150, 160, 160)); d.line([8, 9, 6, 14], fill=(150, 160, 160))
    return im
img(f'{A}/textures/block/hazard_screen.png', scr(False)); img(f'{A}/textures/block/hazard_screen_broken.png', scr(True))
T = lambda n: O + 'block/' + n
def el(fr, to, tx, faces=('north', 'south', 'east', 'west', 'up', 'down'), over=None):
    return {"from": fr, "to": to, "faces": {f: {"texture": (over or {}).get(f, tx)} for f in faces}}
for name, s in (('hazard_monitor', 'hazard_screen'), ('hazard_monitor_broken', 'hazard_screen_broken')):
    w(f'{A}/models/block/{name}.json', {"parent": "minecraft:block/block", "textures": {"c": T('equip_casing'), "s": T(s), "particle": T('equip_casing')},
        "elements": [el([0, 2, 6], [16, 14, 16], "#c", over={"north": "#s"}), el([6, 0, 9], [10, 2, 13], "#c")]})
var = {}
for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for b in ("false", "true"):
        m = {"model": O + "block/" + ("hazard_monitor_broken" if b == "true" else "hazard_monitor")}
        if y: m["y"] = y
        var[f"broken={b},facing={f}"] = m
w(f'{A}/blockstates/hazard_monitor.json', {"variants": var})
w(f'{A}/models/item/hazard_monitor.json', {"parent": O + "block/hazard_monitor"})
w(f'{D}/loot_tables/blocks/hazard_monitor.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "hazard_monitor"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
p = f'{R}/data/minecraft/tags/blocks/mineable/pickaxe.json'; t = json.load(open(p))
if O + 'hazard_monitor' not in t["values"]: t["values"].append(O + 'hazard_monitor')
w(p, t)
# recipes
M = 'minecraft:'
def shaped(name, pattern, key, count=1):
    w(f'{D}/recipes/{name}.json', {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
                                   "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": O + name, "count": count}})
shaped('night_vision_goggles', ["ICI", "GLG"], {"I": M + "iron_ingot", "C": O + "electronic_component", "G": M + "glass_pane", "L": M + "leather"})
shaped('gas_filter', ["N", "C", "P"], {"N": M + "iron_nugget", "C": M + "charcoal", "P": M + "paper"}, 2)
shaped('hazard_monitor', ["IGI", "ICI"], {"I": M + "iron_ingot", "G": M + "glass_pane", "C": O + "electronic_component"})
w(f'{D}/recipes/hazard_visor.json', {"type": "minecraft:crafting_shapeless", "category": "equipment",
    "ingredients": [{"item": O + "gas_mask"}, {"item": O + "night_vision_goggles"}, {"item": O + "toxic_crystal"}], "result": {"item": O + "hazard_visor"}})
# loot
def tier(n, rolls): return {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "name": f"office_rooms:tiers/{n}"}]}
def it(i, chance, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": i}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": chance}], "entries": [e]}
U = lambda a, b: {"type": "minecraft:uniform", "min": a, "max": b}
w(f'{D}/loot_tables/chests/hazard_cache.json', {"type": "minecraft:chest", "pools": [tier("rare", U(1, 2)), tier("uncommon", U(1, 2)),
    it(O + "research_file", .5, 1, 2), it(O + "electronic_component", .6, 1, 4), it(O + "gas_filter", .5, 1, 3), it(O + "toxic_crystal", .35),
    it(O + "glowstick", .4, 2, 5), it(O + "special_event_detector", .05)]})
w(f'{D}/loot_tables/chests/hazard_core.json', {"type": "minecraft:chest", "pools": [tier("rare", U(2, 3)), tier("special", 1),
    it(O + "research_file", .9, 2, 4), it(O + "toxic_crystal", .8, 1, 2), it(O + "electronic_component", .8, 2, 5), it(O + "glitched_ingot", .4, 1, 3),
    it(O + "totem_of_mercy", .15), it(O + "level_3_access_card", .2), it(O + "master_key", .15), it(M + "diamond", .5, 1, 4), it(O + "gas_filter", .6, 2, 3)]})
ADD = {
 "gas_mask": {"hideout_lab": .12, "hideout_security": .12, "hideout_maintenance": .10, "hideout_supply": .08, "secure_storage_1": .08, "secure_storage_2": .10,
              "secure_storage_3": .12, "industrial_storage": .10, "industrial_control": .08, "standard_office": .01, "archive": .02, "break_room": .02,
              "open_office": .01, "server_room": .03},
 "gas_filter": {"hideout_supply": .3, "hideout_maintenance": .2, "industrial_storage": .3, "secure_storage_supplies": .25, "hotel_room": .05},
 "special_event_detector": {"hideout_lab": .10, "hideout_security": .12, "secure_storage_2": .08, "secure_storage_3": .12, "maze_vault": .10,
                            "hotel_vault": .15, "industrial_control": .08, "poolrooms_vault": .08},
}
for item, tables in ADD.items():
    for n, ch in tables.items():
        p = f'{D}/loot_tables/chests/{n}.json'; t = json.load(open(p))
        t["pools"] = [q for q in t["pools"] if not any(e.get("name") == O + item for e in q.get("entries", []))]
        t["pools"].append(it(O + item, ch)); w(p, t)
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({"item.office_rooms.gas_mask": "Gas Mask", "item.office_rooms.night_vision_goggles": "Night Vision Goggles", "item.office_rooms.hazard_visor": "Hazard Visor",
 "item.office_rooms.gas_filter": "Gas Filter", "item.office_rooms.gas_filter.desc": "Right-click while wearing a gas mask to replace its filter.",
 "item.office_rooms.toxic_crystal": "Toxic Crystal", "item.office_rooms.toxic_crystal.desc": "Only found in Hazard Zones. Fuses a gas mask and goggles into a Hazard Visor.",
 "item.office_rooms.special_event_detector": "Special Event Detector",
 "item.office_rooms.special_event_detector.desc": "Hold it to locate an announced or active Hazard Zone.",
 "block.office_rooms.hazard_monitor": "Hazard Monitor"})
json.dump(L, open(lp, 'w'), indent=2, ensure_ascii=False); print("ok")
