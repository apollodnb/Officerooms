"""Office Rooms 0.8.0: textures, models, blockstates, block loot and lang entries for the Maze blocks,
the security door and the new items (Temporal Watch, Anomaly Detector, Level Access Cards)."""
from PIL import Image
import json, os, random
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src/main/resources')
A = os.path.join(R, 'assets/office_rooms')
def save(im, *p):
    path = os.path.join(A, 'textures', *p) + '.png'; os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)
def js(path, obj):
    path = os.path.join(R, path); os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, 'w'), indent=1)
rnd = random.Random(80)
def jit(c, a=6):
    return tuple(max(0, min(255, v + rnd.randint(-a, a))) for v in c[:3]) + ((c[3],) if len(c) > 3 else (255,))

# --- Maze wallpaper: mono-yellow with faint vertical stripes and a chevron pattern ---------------
im = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        base = (196, 178, 96)
        if x % 4 == 0: base = (182, 164, 84)
        if (x % 4 == 2) and ((y + x // 4 * 2) % 4 == 0): base = (172, 154, 78)
        if y == 15: base = (160, 142, 70)
        im.putpixel((x, y), jit(base, 5))
save(im, 'block', 'maze_wallpaper')
# --- Maze carpet: damp, mottled beige-brown ------------------------------------------------------
im = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = (150, 128, 82)
        if rnd.random() < 0.18: c = (132, 112, 70)
        if rnd.random() < 0.06: c = (118, 98, 60)
        im.putpixel((x, y), jit(c, 6))
save(im, 'block', 'maze_carpet')
# --- Maze ceiling: off-white acoustic tile with a grid line -------------------------------------
im = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = (214, 206, 176)
        if x == 0 or y == 0: c = (170, 162, 132)
        elif rnd.random() < 0.10: c = (196, 188, 158)
        im.putpixel((x, y), jit(c, 3))
save(im, 'block', 'maze_ceiling')
# --- Security wall: dark steel plate with rivets and hazard stripe at the bottom ----------------
im = Image.new('RGBA', (16, 16))
for y in range(16):
    for x in range(16):
        c = (58, 62, 70)
        if x in (0, 15) or y in (0, 15): c = (40, 43, 50)
        if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): c = (120, 126, 136)
        if y in (7, 8): c = (48, 52, 60)
        im.putpixel((x, y), jit(c, 3))
save(im, 'block', 'security_wall')
# --- Security door (two halves) -----------------------------------------------------------------
def door(top):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = (70, 76, 86)
            if x in (0, 15) or (top and y == 0) or (not top and y == 15): c = (36, 38, 44)
            elif x in (1, 14): c = (54, 58, 66)
            if not top and y >= 12 and 1 < x < 14:  # hazard stripes
                c = (220, 180, 30) if ((x + y) // 2) % 2 == 0 else (30, 30, 30)
            if top and 4 <= x <= 11 and 3 <= y <= 7:  # small window
                c = (90, 140, 160) if x > 4 and y > 3 else (60, 90, 110)
            if not top and 11 <= x <= 13 and 3 <= y <= 7:  # card reader
                c = (20, 20, 24) if y != 4 else (60, 220, 90)
            im.putpixel((x, y), jit(c, 2))
    return im
save(door(True), 'block', 'security_door_top')
save(door(False), 'block', 'security_door_bottom')

# --- Items --------------------------------------------------------------------------------------
def blank(): return Image.new('RGBA', (16, 16), (0, 0, 0, 0))
# Temporal Watch: golden pocket watch with violet glow
im = blank(); p = im.putpixel
gold = (230, 186, 60); goldd = (160, 112, 28); face = (236, 230, 250); vio = (150, 90, 230)
for y in range(16):
    for x in range(16):
        d = ((x - 7.5) ** 2 + (y - 8.5) ** 2) ** 0.5
        if d <= 6.4:
            p((x, y), gold if d > 5.2 else goldd if d > 4.6 else face)
for (x, y) in [(7, 0), (8, 0), (6, 1), (9, 1), (7, 2), (8, 2)]: p((x, y), gold)
for y in range(5, 9): p((7, y), (40, 30, 60))
for x in range(8, 11): p((x, 8), (40, 30, 60))
for (x, y) in [(4, 6), (11, 11), (4, 11), (11, 6)]: p((x, y), vio)
p((7, 4), vio); p((12, 8), vio); p((3, 8), vio); p((7, 13), vio)
save(im, 'item', 'temporal_watch')
# Anomaly Detector: handheld device with a screen and an antenna
im = blank(); p = im.putpixel
body = (64, 70, 60); bodyd = (40, 44, 38); scr = (30, 60, 30); wave = (110, 240, 110); red = (230, 50, 40)
for y in range(4, 15):
    for x in range(4, 12): p((x, y), body if 4 < x < 11 and y < 14 else bodyd)
for y in range(5, 10):
    for x in range(5, 11): p((x, y), scr)
for (x, y) in [(5, 8), (6, 7), (7, 8), (8, 6), (9, 8), (10, 7)]: p((x, y), wave)
p((6, 11), red); p((9, 11), (200, 200, 200)); p((6, 12), (90, 96, 86)); p((9, 12), (90, 96, 86))
for y in range(0, 4): p((10, y), (150, 150, 150))
p((10, 0), red)
save(im, 'item', 'anomaly_detector')
# Access cards: plastic card with stripe colour per level
for lvl, col in ((1, (60, 170, 230)), (2, (240, 170, 40)), (3, (220, 50, 60))):
    im = blank(); p = im.putpixel
    for y in range(4, 13):
        for x in range(1, 15):
            edge = x in (1, 14) or y in (4, 12)
            p((x, y), (60, 60, 66) if edge else (226, 228, 232))
    for x in range(2, 14):
        p((x, 6), col); p((x, 7), col)
    for (x, y) in [(3, 9), (4, 9), (3, 10), (4, 10)]: p((x, y), (210, 180, 70))  # chip
    for i in range(lvl):
        p((12 - i * 2, 10), col)
    for x in range(7, 11): p((x, 9), (150, 150, 160))
    save(im, 'item', 'level_%d_access_card' % lvl)

# --- Models, blockstates, block loot ------------------------------------------------------------
for b in ('maze_wallpaper', 'maze_carpet', 'maze_ceiling', 'security_wall'):
    js('assets/office_rooms/blockstates/%s.json' % b, {"variants": {"": {"model": "office_rooms:block/" + b}}})
    js('assets/office_rooms/models/block/%s.json' % b, {"parent": "minecraft:block/cube_all", "textures": {"all": "office_rooms:block/" + b}})
for b in ('maze_wallpaper', 'maze_carpet', 'maze_ceiling'):
    js('assets/office_rooms/models/item/%s.json' % b, {"parent": "office_rooms:block/" + b})
    js('data/office_rooms/loot_tables/blocks/%s.json' % b, {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": "office_rooms:" + b}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# door models (vanilla door parents)
for part in ('bottom_left', 'bottom_left_open', 'bottom_right', 'bottom_right_open', 'top_left', 'top_left_open', 'top_right', 'top_right_open'):
    js('assets/office_rooms/models/block/security_door_%s.json' % part, {"parent": "minecraft:block/door_" + part,
        "render_type": "minecraft:cutout",
        "textures": {"bottom": "office_rooms:block/security_door_bottom", "top": "office_rooms:block/security_door_top"}})
variants = {}
rot = {'east': 0, 'south': 90, 'west': 180, 'north': 270}
for facing, r in rot.items():
    for half in ('lower', 'upper'):
        for hinge in ('left', 'right'):
            for op in ('false', 'true'):
                hp = 'bottom' if half == 'lower' else 'top'
                if op == 'false':
                    m = '%s_%s' % (hp, hinge); y = r
                else:
                    m = '%s_%s_open' % (hp, hinge); y = (r + (90 if hinge == 'left' else 270)) % 360
                v = {"model": "office_rooms:block/security_door_" + m}
                if y: v["y"] = y
                variants['facing=%s,half=%s,hinge=%s,open=%s' % (facing, half, hinge, op)] = v
js('assets/office_rooms/blockstates/security_door.json', {"variants": variants})
for it in ('temporal_watch', 'anomaly_detector', 'level_1_access_card', 'level_2_access_card', 'level_3_access_card'):
    js('assets/office_rooms/models/item/%s.json' % it, {"parent": "minecraft:item/generated", "textures": {"layer0": "office_rooms:item/" + it}})

# --- Lang -----------------------------------------------------------------------------------------
lang = {
    "block.office_rooms.maze_wallpaper": "Maze Wallpaper",
    "block.office_rooms.maze_carpet": "Damp Carpet",
    "block.office_rooms.maze_ceiling": "Maze Ceiling Tile",
    "block.office_rooms.security_wall": "Security Wall",
    "block.office_rooms.security_door": "Security Door",
    "item.office_rooms.temporal_watch": "The Temporal Watch",
    "item.office_rooms.temporal_watch.desc": "Slows every creature within 8 blocks for 4 seconds.",
    "item.office_rooms.temporal_watch.cooldown": "Cooldown: 2 minutes",
    "item.office_rooms.anomaly_detector": "Anomaly Detector",
    "item.office_rooms.anomaly_detector.desc": "Hold it: the closer a creature, the stronger the signal. It never says what it is.",
    "item.office_rooms.level_1_access_card": "Level 1 Access Card",
    "item.office_rooms.level_2_access_card": "Level 2 Access Card",
    "item.office_rooms.level_3_access_card": "Level 3 Access Card",
}
for f in ('en_us', 'de_de'):
    path = os.path.join(A, 'lang', f + '.json')
    d = json.load(open(path)); d.update(lang)
    json.dump(d, open(path, 'w'), indent=2, ensure_ascii=False)
print('ok')
