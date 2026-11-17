"""0.12.0: research table, lab/security equipment (textures, models, blockstates), research file, GUI, loot, recipes, lang."""
import json, os, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(120)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def tex(name, frames, anim=None):
    im = Image.new('RGBA', (16, 16 * len(frames)))
    for i, f in enumerate(frames): im.paste(f, (0, 16 * i))
    p = f'{A}/textures/block/{name}.png'; os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
    if anim: w(p + '.mcmeta', {"animation": {"frametime": anim}})
def noise(base, var, alpha=255):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            d = rnd.randint(-var, var); im.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (alpha,))
    return im
def frame(im, col, inset=0):
    d = ImageDraw.Draw(im); d.rectangle([inset, inset, 15 - inset, 15 - inset], outline=col); return im
# --- textures -----------------------------------------------------------------------------------------
casing = frame(noise((60, 62, 66), 5), (40, 41, 44)); tex('equip_casing', [casing])
beige = frame(noise((201, 194, 172), 6), (170, 163, 142)); tex('crt_casing', [beige])
white = frame(noise((214, 216, 218), 5), (180, 182, 186)); tex('lab_white', [white])
def crt(i):
    im = Image.new('RGBA', (16, 16), (10, 22, 14, 255))
    for y in range(16):
        for x in range(16):
            g = 20 + (y % 2) * 10 + rnd.randint(0, 12)
            im.putpixel((x, y), (8, g, 12, 255))
    for row in range(3, 13, 3):                                       # lines of green text
        n = rnd.randint(4, 11)
        for x in range(2, 2 + n):
            if rnd.random() < 0.8: im.putpixel((x, row), (90, 230, 120, 255))
    if i % 2: im.putpixel((2 + rnd.randint(0, 9), 12), (160, 255, 170, 255))
    return frame(im, (25, 25, 25))
tex('crt_screen', [crt(i) for i in range(4)], 8)
def cctv(i):
    im = Image.new('RGBA', (16, 16), (28, 28, 30, 255))
    for qx in (1, 9):
        for qy in (1, 9):
            dark = rnd.random() < 0.25
            for x in range(qx, qx + 6):
                for y in range(qy, qy + 6):
                    v = rnd.randint(10, 40) if dark else rnd.randint(70, 170)
                    im.putpixel((x, y), (v, v, v + 5, 255))
            if not dark: im.putpixel((qx, qy), (230, 30, 30, 255))     # REC dot
    return im
tex('cctv_front', [cctv(i) for i in range(6)], 3)
def panel():
    im = noise((48, 50, 54), 4); d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 9, 6], fill=(14, 40, 20))
    for i, c in enumerate([(200, 40, 40), (40, 180, 60), (220, 190, 40), (50, 110, 220)]):
        im.putpixel((11 + (i % 2) * 2, 3 + (i // 2) * 2), c + (255,))
    for x in range(2, 14, 2):
        for y in (9, 12): im.putpixel((x, y), (150, 150, 155, 255))
    return frame(im, (30, 31, 34))
tex('control_top', [panel()])
def rack(i):
    im = Image.new('RGBA', (16, 16), (22, 22, 24, 255)); d = ImageDraw.Draw(im)
    for y in range(1, 15, 3):
        d.rectangle([1, y, 14, y + 1], fill=(45, 46, 50))
        for x in (3, 5, 7):
            on = rnd.random() < 0.6
            im.putpixel((x, y), ((60, 230, 90) if x != 7 else (240, 170, 40)) + (255,) if on else (30, 50, 32, 255))
    return frame(im, (60, 62, 66))
tex('rack_front', [rack(i) for i in range(4)], 6)
def cabinet():
    im = white.copy(); d = ImageDraw.Draw(im)
    d.rectangle([2, 2, 13, 13], fill=(150, 180, 185, 255))
    d.line([2, 7, 13, 7], fill=(200, 202, 205)); d.line([7, 2, 7, 13], fill=(200, 202, 205))
    for x, y, c in [(3, 5, (180, 60, 60)), (5, 5, (60, 140, 70)), (9, 5, (200, 180, 70)), (11, 5, (90, 90, 200)), (4, 11, (120, 70, 160)), (10, 11, (60, 150, 150))]:
        im.putpixel((x, y), c + (255,)); im.putpixel((x, y + 1), c + (255,))
    return im
tex('cabinet_front', [cabinet()])
black = frame(noise((30, 30, 33), 4), (20, 20, 22)); tex('lab_black', [black])
glass = Image.new('RGBA', (16, 16), (200, 230, 235, 50)); frame(glass, (220, 240, 245, 150)); tex('lab_glass', [glass])
def fluid(col, blob):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            v = rnd.randint(-15, 15); im.putpixel((x, y), (col[0] + v // 2, col[1] + v, col[2] + v // 2, 150))
    if blob:
        for x in range(5, 11):
            for y in range(4, 13):
                if (x - 8) ** 2 / 9 + (y - 8) ** 2 / 20 < 1: im.putpixel((x, y), (25, 30, 25, 230))
    for _ in range(4): im.putpixel((rnd.randint(1, 14), rnd.randint(1, 14)), (210, 255, 210, 200))
    return im
tex('specimen_fluid', [fluid((90, 170, 80), True)])
tex('tank_fluid', [fluid((70, 160, 140), True)])
def core(i):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            v = int(127 + 127 * __import__('math').sin((x + y + i * 4) / 2.5))
            im.putpixel((x, y), (120 + v // 3, 40 + v // 4, 200 + v // 5 if 200 + v // 5 < 256 else 255, 255))
    return im
tex('machine_core', [core(i) for i in range(4)], 4)
def hazard():
    im = casing.copy()
    for x in range(16):
        for y in range(10, 16):
            im.putpixel((x, y), (230, 190, 30, 255) if ((x + y) // 3) % 2 else (25, 25, 25, 255))
    return im
tex('machine_base', [hazard()])
desk = frame(noise((96, 66, 42), 7), (70, 48, 30)); tex('research_desk', [desk])
paper = noise((232, 228, 214), 4)
for y in range(3, 14, 2):
    for x in range(2, rnd.randint(8, 14)): paper.putpixel((x, y), (90, 90, 100, 255))
tex('research_paper', [paper])
folder = noise((205, 170, 100), 6); ImageDraw.Draw(folder).rectangle([0, 6, 15, 8], fill=(170, 30, 30)); tex('research_folder', [folder])
lamp = noise((50, 90, 60), 5); ImageDraw.Draw(lamp).rectangle([0, 12, 15, 15], fill=(250, 230, 160)); tex('research_lamp', [lamp])
# item: research file
it = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(it)
d.rectangle([2, 3, 14, 13], fill=(205, 170, 100), outline=(140, 105, 50)); d.rectangle([2, 2, 7, 3], fill=(190, 155, 85))
d.rectangle([2, 7, 14, 9], fill=(170, 30, 30)); d.line([4, 11, 11, 11], fill=(110, 80, 40))
os.makedirs(f'{A}/textures/item', exist_ok=True); it.save(f'{A}/textures/item/research_file.png')
w(f'{A}/models/item/research_file.json', {"parent": "minecraft:item/generated", "textures": {"layer0": O + "item/research_file"}})
# GUI
g = Image.new('RGBA', (256, 256), (0, 0, 0, 0)); d = ImageDraw.Draw(g)
d.rectangle([0, 0, 175, 165], fill=(198, 198, 198)); d.line([0, 0, 174, 0], fill=(255, 255, 255)); d.line([0, 0, 0, 164], fill=(255, 255, 255))
d.line([1, 165, 175, 165], fill=(85, 85, 85)); d.line([175, 1, 175, 165], fill=(85, 85, 85))
def slot(x, y):
    d.rectangle([x, y, x + 17, y + 17], fill=(139, 139, 139)); d.line([x, y, x + 16, y], fill=(55, 55, 55)); d.line([x, y, x, y + 16], fill=(55, 55, 55))
    d.line([x + 1, y + 17, x + 17, y + 17], fill=(255, 255, 255)); d.line([x + 17, y + 1, x + 17, y + 17], fill=(255, 255, 255))
slot(16, 47)
for r in range(3):
    for c in range(9): slot(7 + c * 18, 83 + r * 18)
for c in range(9): slot(7 + c * 18, 141)
os.makedirs(f'{A}/textures/gui', exist_ok=True); g.save(f'{A}/textures/gui/research_table.png')
# --- models -------------------------------------------------------------------------------------------
def el(fr, to, tx, faces=('north', 'south', 'east', 'west', 'up', 'down'), rot=None, glow=False, over=None):
    e = {"from": fr, "to": to, "faces": {f: {"texture": (over or {}).get(f, tx)} for f in faces}}
    if rot: e["rotation"] = rot
    if glow: e["forge_data"] = {"block_light": 15, "sky_light": 15}
    return e
def model(name, textures, elements, translucent=False):
    m = {"parent": "minecraft:block/block", "textures": dict(textures, particle=list(textures.values())[0]), "elements": elements}
    if translucent: m["render_type"] = "minecraft:translucent"
    else: m["render_type"] = "minecraft:cutout"
    w(f'{A}/models/block/{name}.json', m)
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + name})
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={f}": ({"model": O + "block/" + name, "y": y} if y else {"model": O + "block/" + name})
                                                    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
T = lambda n: O + 'block/' + n
model('crt_monitor', {"c": T('crt_casing'), "s": T('crt_screen')}, [
    el([2, 0, 4], [14, 11, 14], "#c"), el([4, 1, 14], [12, 9, 16], "#c"),
    el([3, 1, 3.9], [13, 10, 4], "#s", faces=('north',), glow=True)])
model('cctv_bank', {"c": T('equip_casing'), "s": T('cctv_front')}, [
    el([0, 0, 0], [16, 16, 16], "#c"), el([0, 0, -0.01], [16, 16, 0], "#s", faces=('north',), glow=True)])
model('control_panel', {"c": T('equip_casing'), "t": T('control_top')}, [
    el([0, 0, 1], [16, 7, 16], "#c"),
    el([0, 6, 2], [16, 9, 16], "#c", over={"up": "#t"}, rot={"origin": [8, 9, 16], "axis": "x", "angle": 22.5})])
model('server_rack', {"c": T('equip_casing'), "r": T('rack_front')}, [
    el([1, 0, 1], [15, 16, 15], "#c", over={"north": "#r"}), el([1.5, 0.5, 0.99], [14.5, 15.5, 1], "#r", faces=('north',), glow=True)])
model('lab_cabinet', {"c": T('lab_white'), "f": T('cabinet_front')}, [el([0, 0, 0], [16, 16, 16], "#c", over={"north": "#f"})])
model('microscope', {"w": T('lab_white'), "b": T('lab_black')}, [
    el([4, 0, 5], [12, 1, 12], "#w"), el([7, 1, 10], [9, 10, 12], "#w"), el([5, 4, 5], [11, 5, 10], "#b"),
    el([7, 9, 7], [9, 10, 10], "#w"), el([7, 5, 6], [9, 12, 8], "#b"), el([6.5, 12, 6.5], [9.5, 13, 8.5], "#b")])
model('specimen_jar', {"g": T('lab_glass'), "f": T('specimen_fluid'), "c": T('equip_casing')}, [
    el([5.5, 0.5, 5.5], [10.5, 8, 10.5], "#f", glow=True), el([5, 0, 5], [11, 9, 11], "#g"), el([5, 9, 5], [11, 10, 11], "#c")], True)
model('containment_tank', {"g": T('lab_glass'), "f": T('tank_fluid')}, [
    el([2, 0, 2], [14, 16, 14], "#f", faces=('north', 'south', 'east', 'west'), glow=True), el([1, 0, 1], [15, 16, 15], "#g", faces=('north', 'south', 'east', 'west'))], True)
model('experiment_machine', {"m": T('machine_base'), "k": T('machine_core'), "c": T('equip_casing')}, [
    el([1, 0, 1], [15, 3, 15], "#m"), el([4, 3, 4], [12, 13, 12], "#k", glow=True),
    el([3, 5, 3], [13, 6, 13], "#c"), el([3, 10, 3], [13, 11, 13], "#c"), el([5, 13, 5], [11, 16, 11], "#c")])
model('research_table', {"d": T('research_desk'), "p": T('research_paper'), "f": T('research_folder'), "l": T('research_lamp')}, [
    el([0, 12, 0], [16, 15, 16], "#d"), el([1, 0, 1], [3, 12, 3], "#d"), el([13, 0, 1], [15, 12, 3], "#d"),
    el([1, 0, 13], [3, 12, 15], "#d"), el([13, 0, 13], [15, 12, 15], "#d"),
    el([2, 15, 4], [8, 15.2, 10], "#p", faces=('up',)), el([9, 15, 8], [14, 17, 13], "#f"),
    el([11, 15, 2], [13, 15.5, 4], "#l"), el([11.5, 15.5, 2.5], [12.5, 20, 3.5], "#l"), el([10, 19, 2], [14, 21, 6], "#l")])
# --- data ---------------------------------------------------------------------------------------------
names = ['research_table', 'crt_monitor', 'cctv_bank', 'control_panel', 'server_rack', 'lab_cabinet', 'microscope', 'specimen_jar', 'containment_tank', 'experiment_machine']
for n in names:
    w(f'{D}/loot_tables/blocks/{n}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + n}],
                                                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag, members in (('pickaxe', names[1:]), ('axe', ['research_table'])):
    p = f'{R}/data/minecraft/tags/blocks/mineable/{tag}.json'; t = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    t["values"] += [O + m for m in members if O + m not in t["values"]]; w(p, t)
w(f'{D}/tags/items/reforgeable.json', {"replace": False, "values": []})
def shaped(name, pattern, key, count=1):
    w(f'{D}/recipes/{name}.json', {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                   "key": {k: {"item": v} for k, v in key.items()}, "result": {"item": O + name, "count": count}})
M = 'minecraft:'
shaped('research_table', ["PBP", "ICI"], {"P": M + "paper", "B": M + "book", "I": M + "iron_ingot", "C": M + "crafting_table"})
shaped('crt_monitor', ["III", "IGR", "III"], {"I": M + "iron_ingot", "G": M + "glass_pane", "R": M + "redstone"})
shaped('cctv_bank', ["MM", "MM"], {"M": O + "crt_monitor"}, 2)
shaped('control_panel', ["BRB", "III"], {"B": M + "stone_button", "R": M + "redstone", "I": M + "iron_ingot"}, 2)
shaped('server_rack', ["IRI", "IPI", "IRI"], {"I": M + "iron_ingot", "R": M + "redstone", "P": M + "repeater"})
shaped('lab_cabinet', ["IGI", "IBI", "III"], {"I": M + "iron_ingot", "G": M + "glass_pane", "B": M + "glass_bottle"})
shaped('microscope', ["G", "I", "S"], {"G": M + "glass_pane", "I": M + "iron_ingot", "S": M + "smooth_stone_slab"})
shaped('specimen_jar', ["S", "B"], {"S": M + "slime_ball", "B": M + "glass_bottle"})
shaped('containment_tank', ["IGI", "GSG", "IGI"], {"I": M + "iron_ingot", "G": M + "glass", "S": M + "slime_ball"})
shaped('experiment_machine', ["IEI", "ARA", "III"], {"I": M + "iron_ingot", "E": M + "ender_eye", "A": M + "amethyst_shard", "R": M + "redstone_block"})
# research files in loot
FILES = {"hideout_lab": (.45, 2), "hideout_security": (.18, 1), "hideout_concealed": (.30, 1), "hideout_maintenance": (.06, 1), "hideout_supply": (.04, 1),
         "maze_vault": (.35, 2), "secure_storage_1": (.08, 1), "secure_storage_2": (.18, 1), "secure_storage_3": (.35, 2), "poolrooms_vault": (.30, 1),
         "archive": (.03, 1), "standard_office": (.01, 1), "server_room": (.04, 1), "neighbourhood_house": (.03, 1), "abandoned_camp": (.05, 1),
         "sculk_special": (.15, 1), "oasis": (.20, 1), "outpost": (.03, 1)}
for n, (ch, mx) in FILES.items():
    p = f'{D}/loot_tables/chests/{n}.json'; t = json.load(open(p))
    t["pools"] = [q for q in t["pools"] if not any(e.get("name") == O + "research_file" for e in q.get("entries", []))]
    e = {"type": "minecraft:item", "name": O + "research_file"}
    if mx > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": mx}}]
    t["pools"].append({"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": ch}], "entries": [e]}); w(p, t)
# lang
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({
 "block.office_rooms.research_table": "Research Table", "block.office_rooms.crt_monitor": "CRT Monitor", "block.office_rooms.cctv_bank": "CCTV Monitor Bank",
 "block.office_rooms.control_panel": "Control Panel", "block.office_rooms.server_rack": "Server Rack", "block.office_rooms.lab_cabinet": "Lab Cabinet",
 "block.office_rooms.microscope": "Microscope", "block.office_rooms.specimen_jar": "Specimen Jar", "block.office_rooms.containment_tank": "Containment Tank",
 "block.office_rooms.experiment_machine": "Resonance Apparatus",
 "item.office_rooms.research_file": "Research File", "item.office_rooms.research_file.desc": "Classified findings. Exchange them at a Research Table.",
 "container.office_rooms.research_table": "Research Table",
 "gui.office_rooms.heart_research": "Heart Research (%s)", "gui.office_rooms.heart_max": "Heart Research complete",
 "gui.office_rooms.heart_tip": "Costs Research Files. Permanently adds one heart of maximum health. Researched: %s / %s",
 "gui.office_rooms.reforge": "Reforge Weapon (%s)", "gui.office_rooms.reforge_max": "Fully reforged",
 "gui.office_rooms.reforge_tip": "Costs Research Files. Place a weapon in the slot. Each reforge adds +3 damage, up to 3 times. Enchantments are kept.",
 "gui.office_rooms.hearts": "❤ %s/%s", "gui.office_rooms.files": "Files: %s",
 "gui.office_rooms.place_weapon": "Place a weapon in the slot.", "gui.office_rooms.not_reforged": "This weapon is not reforged yet.",
 "gui.office_rooms.reforged": "Reforged %s: +%s damage",
 "tooltip.office_rooms.reforged": "Reforged %s (+%s damage)", "tooltip.office_rooms.reforged_ranged": "Reforged %s (+%s projectile damage)"})
json.dump(L, open(lp, 'w'), indent=2, ensure_ascii=False); print("ok")
