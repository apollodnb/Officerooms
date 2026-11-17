"""0.21.0: Level 0 assets (all textures drawn here) and data."""
import json, os, math, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(210)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def n(a=6): return rnd.randint(-a, a)
def cl(c): return tuple(max(0, min(255, int(v))) for v in c) + (255,)
def tex(name, fn, frames=1):
    im = Image.new('RGBA', (16, 16 * frames)); px = im.load()
    for f in range(frames):
        for y in range(16):
            for x in range(16):
                px[x, y + 16 * f] = fn(x, y, f)
    p = f'{A}/textures/block/{name}.png'; im.save(p)
    if frames > 1: json.dump({"animation": {"frametime": 3}}, open(p + '.mcmeta', 'w'))
def wallpaper(x, y, f):
    b = (236, 222, 132)
    chev = (x % 4) == (abs((y % 8) - 4) // 2 + (0 if (x // 4) % 2 else 1)) % 4
    d = -14 if chev else 0
    if rnd.random() < 0.06: d -= 10
    return cl((b[0] + d + n(3), b[1] + d + n(3), b[2] + d // 2 + n(3)))
tex('level0_wallpaper', wallpaper)
def carpet(x, y, f):
    b = 170 + n(7)
    if rnd.random() < 0.08: b -= 22
    return cl((b * 1.12, b * 1.0, b * 0.58))
tex('level0_carpet', carpet)
def ceiling(x, y, f):
    b = 222 + n(3)
    if x in (0, 15) or y in (0, 15): b -= 30
    if rnd.random() < 0.12: b -= 25
    return cl((b, b - 4, b - 26))
tex('level0_ceiling', ceiling)
def light(on):
    def fn(x, y, f):
        if x in (0, 15) or y in (0, 15): return cl((200, 196, 170))
        if not on: return cl((176 + n(3), 174, 158))
        return cl((255, 253, 236) if 3 <= y <= 12 else (246, 242, 214))
    return fn
tex('level0_light', light(True)); tex('level0_light_off', light(False))
def manila(x, y, f):
    b = (228, 208, 158); d = -9 if x % 3 == 0 else 0
    return cl((b[0] + d + n(3), b[1] + d + n(3), b[2] + d + n(3)))
tex('manila_wallpaper', manila)
tex('manila_carpet', lambda x, y, f: cl((196 + n(5), 176 + n(5), 136 + n(5))))
def exitf(x, y, f):
    ph = f / 12 * 2 * math.pi; r = math.hypot(x - 7.5, y - 7.5); a = math.atan2(y - 7.5, x - 7.5)
    v = 0.5 + 0.5 * math.sin(r * 1.2 - ph * 2 + a)
    return (int(200 + 55 * v), int(185 + 60 * v), int(130 + 110 * v), 215)
tex('level0_exit', exitf, 12)
def void(x, y, f):
    if x in (0, 15) or y in (0, 15): return cl((120, 116, 96))
    c = 10 + n(4)
    if (x == 5 and y > 3) or (y == 9 and 5 <= x <= 12): c = 42
    return cl((c, c, c + 2))
tex('level0_ceiling_void', void)
def worn(x, y, f):
    if 3 <= x <= 12 and 4 <= y <= 13 and (x - 7) ** 2 / 25 + (y - 9) ** 2 / 30 < 1 + rnd.random() * 0.3:
        return cl((206 + n(5), 200 + n(5), 184 + n(5)))
    return wallpaper(x, y, f)
tex('level0_wallpaper_worn', worn)
def arrow(x, y, f):
    shaft = 7 <= y <= 8 and 2 <= x <= 10
    head = 10 <= x <= 13 and abs(y - 7.5) <= (13 - x) + 0.5
    if (shaft or head) and rnd.random() < 0.9:
        return cl((170 + n(15), 30, 25))
    return wallpaper(x, y, f)
tex('level0_wallpaper_arrow', arrow)
def stained(x, y, f):
    c = carpet(x, y, f)
    d = (x - 8) ** 2 + (y - 8) ** 2
    k = 0.72 if d < 30 + rnd.random() * 10 else 0.86 if d < 50 else 1.0
    return cl((c[0] * k, c[1] * k, c[2] * k))
tex('level0_carpet_stained', stained)
def cstain(x, y, f):
    c = ceiling(x, y, f)
    d = (x - 6) ** 2 + (y - 9) ** 2
    return cl((c[0] - 30, c[1] - 36, c[2] - 50)) if 18 < d < 34 else c if d >= 18 else cl((c[0] - 14, c[1] - 18, c[2] - 26))
tex('level0_ceiling_stained', cstain)
def vent(x, y, f):
    if x in (0, 15) or y in (0, 15): return cl((180, 178, 170))
    return cl((60, 60, 62) if y % 3 == 0 else (150, 150, 146))
tex('level0_vent', vent)
def outlet(x, y, f):
    if x in (5, 10) and y in (5, 6): return cl((40, 38, 34))
    if x in (5, 10) and y in (10, 11): return cl((40, 38, 34))
    return cl((236 + n(3), 232 + n(3), 214 + n(3)))
tex('level0_outlet', outlet)
M = f'{A}/models/block'
for b in ('level0_wallpaper_worn', 'level0_wallpaper_arrow', 'level0_carpet_stained', 'level0_ceiling_stained', 'level0_vent', 'level0_ceiling_void', 'level0_wallpaper', 'level0_carpet', 'level0_ceiling', 'level0_light', 'level0_light_off', 'manila_wallpaper', 'manila_carpet'):
    w(f'{M}/{b}.json', {"parent": "minecraft:block/cube_all", "textures": {"all": O + "block/" + b}})
    w(f'{A}/blockstates/{b}.json', {"variants": {"": {"model": O + "block/" + b}}})
    w(f'{A}/models/item/{b}.json', {"parent": O + "block/" + b})
    w(f'{D}/loot_tables/blocks/{b}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + b}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
w(f'{A}/blockstates/level0_light_flicker.json', {"variants": {"lit=true": {"model": O + "block/level0_light"}, "lit=false": {"model": O + "block/level0_light_off"}}})
w(f'{D}/loot_tables/blocks/level0_light_flicker.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "level0_light"}]}]})
w(f'{M}/level0_outlet.json', {"parent": "minecraft:block/block", "textures": {"t": O + "block/level0_outlet", "particle": O + "block/level0_outlet"},
    "elements": [{"from": [5, 2, 15], "to": [11, 8, 16], "faces": {k: {"texture": "#t"} for k in ("north", "south", "east", "west", "up", "down")}}]})
w(f'{A}/blockstates/level0_outlet.json', {"variants": {f"facing={d}": {"model": O + "block/level0_outlet", "y": r} for d, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
w(f'{D}/loot_tables/blocks/level0_outlet.json', {"type": "minecraft:block", "pools": []})
w(f'{M}/level0_exit.json', {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": O + "block/level0_exit"}})
w(f'{A}/blockstates/level0_exit.json', {"variants": {"": {"model": O + "block/level0_exit"}}})
p = f'{R}/data/minecraft/tags/blocks/mineable/pickaxe.json'; t = json.load(open(p))
for b in ('level0_ceiling', 'level0_light', 'level0_light_off'):
    if O + b not in t["values"]: t["values"].append(O + b)
w(p, t)
w(f'{D}/loot_tables/chests/manila_room.json', {"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:water_bucket"}]}]})
w(f'{D}/dimension/level_0.json', {"type": O + "level_0", "generator": {"type": O + "level_0", "biome_source": {"type": "minecraft:fixed", "biome": O + "level_0"}}})
w(f'{D}/dimension_type/level_0.json', {"ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": True,
    "ambient_light": 0.12, "fixed_time": 6000, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0, "piglin_safe": False,
    "bed_works": False, "respawn_anchor_works": False, "has_raids": False, "min_y": 0, "height": 48, "logical_height": 48,
    "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld"})
w(f'{D}/worldgen/biome/level_0.json', {"has_precipitation": False, "temperature": 0.8, "downfall": 0.4,
    "effects": {"fog_color": 0xC9BE84, "sky_color": 0xC9BE84, "water_color": 0x5A7E9A, "water_fog_color": 0x2B3F4E, "ambient_sound": O + "ambient.lamp_hum"},
    "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({"block.office_rooms.level0_wallpaper": "Level 0 Wallpaper", "block.office_rooms.level0_carpet": "Damp Carpet", "block.office_rooms.level0_ceiling": "Level 0 Ceiling Tile",
          "block.office_rooms.level0_light": "Fluorescent Panel", "block.office_rooms.level0_light_off": "Dead Fluorescent Panel", "block.office_rooms.level0_light_flicker": "Flickering Fluorescent Panel", "block.office_rooms.level0_ceiling_void": "Missing Ceiling Tile", "block.office_rooms.level0_wallpaper_worn": "Peeling Wallpaper", "block.office_rooms.level0_wallpaper_arrow": "Wallpaper with Spray-Painted Arrow", "block.office_rooms.level0_carpet_stained": "Soaked Carpet", "block.office_rooms.level0_ceiling_stained": "Water-Stained Ceiling Tile", "block.office_rooms.level0_vent": "Ceiling Vent", "block.office_rooms.level0_outlet": "Wall Outlet", "block.office_rooms.manila_wallpaper": "Manila Wallpaper",
          "block.office_rooms.manila_carpet": "Manila Carpet", "block.office_rooms.level0_exit": "Way Out"})
w(lp, L)
print("ok")
