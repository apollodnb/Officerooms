"""0.11.0: loose panel assets, hideout loot tables, lang."""
import json, os
R = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
A = os.path.join(R, 'assets', 'office_rooms'); D = os.path.join(R, 'data', 'office_rooms')
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
w(f'{A}/blockstates/loose_panel.json', {"variants": {"": {"model": "office_rooms:block/loose_panel"}}})
w(f'{A}/models/block/loose_panel.json', {"parent": "minecraft:block/cube_column",
  "textures": {"end": "minecraft:block/quartz_block_top", "side": "minecraft:block/quartz_block_side"}})
w(f'{A}/models/item/loose_panel.json', {"parent": "office_rooms:block/loose_panel"})
def tier(n, rolls): return {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "name": f"office_rooms:tiers/{n}"}]}
def item(i, chance, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": i}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": chance}], "entries": [e]}
U = lambda a, b: {"type": "minecraft:uniform", "min": a, "max": b}
O = 'office_rooms:'
T = {
 "hideout_maintenance": [tier("tools", U(1, 2)), tier("common", U(2, 4)), item(O+"portable_battery", .35), item(O+"industrial_flashlight", .15), item(O+"master_key", .04)],
 "hideout_supply": [tier("supplies", U(3, 6)), item(O+"bandage", .6, 1, 3), item(O+"glowstick", .6, 2, 6), item(O+"almond_water", .7, 1, 3), tier("uncommon", 1)],
 "hideout_supply_common": [tier("supplies", U(1, 3)), tier("common", U(1, 2))],
 "hideout_breakroom": [tier("common", U(2, 4)), item(O+"almond_water", .5, 1, 2), item("minecraft:cake", .1), item("minecraft:cookie", .5, 1, 4)],
 "hideout_security": [tier("supplies", U(1, 3)), item(O+"level_1_access_card", .25), item(O+"level_2_access_card", .10), item(O+"industrial_flashlight", .3), item(O+"noise_maker", .25), item(O+"emergency_flare", .3, 1, 2)],
 "hideout_lab": [tier("uncommon", U(1, 3)), tier("common", U(1, 2)), item(O+"liquid_pain", .2), item(O+"anomaly_detector", .12), item(O+"glitched_ingot", .25, 1, 2), item(O+"level_2_access_card", .08)],
 "hideout_concealed": [tier("rare", U(1, 2)), tier("uncommon", U(1, 2)), item(O+"totem_of_mercy", .08), item(O+"master_key", .12), item(O+"temporal_watch", .06), item(O+"level_3_access_card", .04)],
}
for n, pools in T.items(): w(f'{D}/loot_tables/chests/{n}.json', {"type": "minecraft:chest", "pools": pools})
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp)); L["block.office_rooms.loose_panel"] = "Loose Panel"
json.dump(L, open(lp, 'w'), indent=2, ensure_ascii=False)
tag = f'{R}/data/minecraft/tags/blocks/mineable/axe.json'
t = json.load(open(tag)) if os.path.exists(tag) else {"replace": False, "values": []}
if O+"loose_panel" not in t["values"]: t["values"].append(O+"loose_panel")
w(tag, t); print("ok")
