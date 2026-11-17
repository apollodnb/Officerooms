"""0.19.0: Underside assets: artifacts, key, Noclip Sword, [Unknown] texture, upside-down grass, portals, dimension,
biome, loot (artifact chests), recipe, lang, sounds.json. All textures are drawn here."""
import json, os, math, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'; M = 'minecraft:'
rnd = random.Random(190)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def img(path, im):
    os.makedirs(os.path.dirname(path), exist_ok=True); im.save(path)
def item_png(name, draw, model="minecraft:item/generated"):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); draw(ImageDraw.Draw(im), im); img(f'{A}/textures/item/{name}.png', im)
    w(f'{A}/models/item/{name}.json', {"parent": model, "textures": {"layer0": O + "item/" + name}})
BLK = (20, 20, 22)
def pearl(d, im):
    d.ellipse([3, 3, 12, 12], fill=(170, 228, 226), outline=(70, 130, 140)); d.ellipse([5, 5, 10, 10], fill=(200, 245, 240))
    d.point([(6, 5), (5, 6)], fill=(255, 255, 255)); d.point([(10, 11), (11, 10)], fill=(110, 170, 180))
def photo(d, im):
    d.rectangle([2, 3, 13, 13], fill=(226, 216, 190), outline=(150, 140, 115)); d.rectangle([3, 4, 12, 10], fill=(150, 122, 88))
    d.polygon([(4, 10), (4, 7), (7, 5), (10, 7), (10, 10)], fill=(105, 82, 58)); d.rectangle([6, 8, 7, 10], fill=(70, 52, 38))
    for x in (5, 9, 11): d.line([x, 9, x, 10], fill=(60, 45, 34)); d.point((x, 8), fill=(186, 160, 120))
    d.line([3, 12, 9, 12], fill=(170, 160, 135))
def spool(d, im):
    d.rectangle([4, 2, 11, 3], fill=(120, 80, 45)); d.rectangle([4, 12, 11, 13], fill=(120, 80, 45))
    d.rectangle([5, 4, 10, 11], fill=(214, 182, 50))
    for y in range(4, 12, 2): d.line([5, y, 10, y + 1], fill=(170, 138, 30))
    d.line([10, 9, 13, 11], fill=(214, 182, 50)); d.line([13, 11, 14, 14], fill=(214, 182, 50))
def roomkey(d, im):
    brass, dark = (210, 168, 64), (130, 96, 30)
    d.ellipse([2, 2, 7, 7], outline=brass); d.point((4, 4), fill=dark); d.line([6, 6, 12, 12], fill=brass); d.line([7, 6, 13, 12], fill=dark)
    d.line([10, 12, 11, 13], fill=brass); d.line([12, 10, 13, 11], fill=brass)
    d.rectangle([9, 1, 14, 5], fill=(168, 40, 40), outline=(100, 20, 20)); d.point([(10, 3), (12, 3), (13, 3)], fill=(240, 230, 210))
def fuse(d, im):
    d.rectangle([3, 6, 12, 9], fill=(170, 200, 210, 200)); d.rectangle([1, 5, 3, 10], fill=(150, 150, 155)); d.rectangle([12, 5, 14, 10], fill=(150, 150, 155))
    d.line([4, 7, 6, 8, 8, 7, 10, 8, 11, 7], fill=(255, 150, 40)); d.point([(6, 8), (8, 7)], fill=(255, 220, 120))
def badge(d, im):
    d.ellipse([2, 2, 13, 13], fill=(245, 205, 40), outline=(170, 130, 10)); d.point([(6, 6), (9, 6)], fill=BLK)
    d.line([4, 8, 6, 11, 9, 11, 11, 8], fill=BLK); d.point([(4, 7), (11, 7)], fill=BLK); d.line([7, 14, 8, 14], fill=(160, 160, 160))
def ukey(d, im):
    d.ellipse([1, 1, 7, 7], outline=(28, 28, 30)); d.ellipse([2, 2, 6, 6], outline=(70, 140, 60)); d.point((4, 4), fill=(140, 230, 210))
    d.line([6, 6, 13, 13], fill=(28, 28, 30)); d.line([7, 6, 14, 13], fill=(55, 55, 60))
    d.line([10, 13, 11, 14], fill=(28, 28, 30)); d.line([12, 11, 13, 12], fill=(28, 28, 30)); d.point([(9, 9), (11, 11)], fill=(140, 230, 210))
def sword(d, im):
    for i in range(10):
        x, y = 4 + i, 11 - i
        d.point((x - 1, y), fill=(0, 220, 230)); d.point((x + 1, y), fill=(230, 40, 200)); d.point((x, y), fill=(18, 18, 22))
        d.point((x, y - 1), fill=(48, 48, 56))
    d.line([2, 10, 5, 13], fill=(90, 60, 140)); d.line([1, 14, 3, 12], fill=(40, 30, 60)); d.point((1, 14), fill=(0, 220, 230))
    for (x, y) in ((7, 4), (12, 6), (9, 2)): d.point((x, y), fill=(0, 220, 230, 160))
for n, f in (("chlorine_pearl", pearl), ("faded_photograph", photo), ("thread_of_the_maze", spool), ("room_key_000", roomkey),
             ("humming_fuse", fuse), ("party_smile_badge", badge), ("underside_key", ukey)):
    item_png(n, f)
item_png("noclip_sword", sword, "minecraft:item/handheld")
# boss texture: near-black with faint dark-red flecks; bone-white eye patch at (116,120)
im = Image.new('RGBA', (128, 128))
px = im.load()
for y in range(128):
    for x in range(128):
        v = rnd.randint(8, 22); px[x, y] = (v + (14 if rnd.random() < 0.04 else 0), v, v, 255)
for y in range(118, 128):
    for x in range(114, 128):
        px[x, y] = (226, 218, 196, 255)
img(f'{A}/textures/entity/unknown.png', im)
# upside-down grass
T = M + 'block/'
side = {"uv": [0, 0, 16, 16], "rotation": 180}
w(f'{A}/models/block/underside_grass.json', {"parent": "minecraft:block/block",
    "textures": {"particle": T + "dirt", "bottom": T + "grass_block_top", "top": T + "dirt", "side": T + "grass_block_side", "overlay": T + "grass_block_side_overlay"},
    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
        "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down", "tintindex": 0}, "up": {"texture": "#top", "cullface": "up"},
        **{f: dict(side, texture="#side", cullface=f) for f in ("north", "south", "east", "west")}}},
        {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {f: dict(side, texture="#overlay", cullface=f, tintindex=0) for f in ("north", "south", "east", "west")}}]})
w(f'{A}/blockstates/underside_grass.json', {"variants": {"": {"model": O + "block/underside_grass"}}})
w(f'{A}/models/item/underside_grass.json', {"parent": O + "block/underside_grass"})
w(f'{D}/loot_tables/blocks/underside_grass.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "dirt"}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
p = f'{R}/data/minecraft/tags/blocks/mineable/shovel.json'
t = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
if O + 'underside_grass' not in t["values"]: t["values"].append(O + 'underside_grass')
w(p, t)
# portals: animated swirls
def swirl(name, c1, c2):
    fr = 16; im = Image.new('RGBA', (16, 16 * fr)); px = im.load()
    for f in range(fr):
        ph = f / fr * 2 * math.pi
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5; r = math.hypot(dx, dy); a = math.atan2(dy, dx)
                v = 0.5 + 0.5 * math.sin(3 * a + r * 0.9 - ph * 2) * math.cos(r * 0.5 - ph)
                v = max(0, min(1, v + rnd.uniform(-0.08, 0.08)))
                px[x, y + 16 * f] = tuple(int(c1[i] + (c2[i] - c1[i]) * v) for i in range(3)) + (200,)
    img(f'{A}/textures/block/{name}.png', im)
    json.dump({"animation": {"frametime": 2}}, open(f'{A}/textures/block/{name}.png.mcmeta', 'w'))
    w(f'{A}/models/block/{name}.json', {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": {"p": O + "block/" + name, "particle": O + "block/" + name},
        "elements": [{"from": [0, 0, 6], "to": [16, 16, 10], "faces": {"north": {"texture": "#p"}, "south": {"texture": "#p"}}}]})
swirl("underside_portal_office", (120, 96, 30), (250, 228, 140))
swirl("underside_portal_overworld", (30, 90, 40), (150, 215, 250))
w(f'{A}/blockstates/underside_portal.json', {"variants": {"overworld=false": {"model": O + "block/underside_portal_office"},
                                                          "overworld=true": {"model": O + "block/underside_portal_overworld"}}})
# dimension
w(f'{D}/dimension/underside.json', {"type": O + "underside", "generator": {"type": O + "underside", "biome_source": {"type": "minecraft:fixed", "biome": O + "underside_meadow"}}})
w(f'{D}/dimension_type/underside.json', {"ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": False, "has_ceiling": True,
    "ambient_light": 0.45, "fixed_time": 6000, "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0, "piglin_safe": False,
    "bed_works": False, "respawn_anchor_works": False, "has_raids": False, "min_y": 0, "height": 128, "logical_height": 128,
    "infiniburn": "#minecraft:infiniburn_overworld", "effects": "minecraft:overworld"})
w(f'{D}/worldgen/biome/underside_meadow.json', {"has_precipitation": False, "temperature": 0.7, "downfall": 0.5,
    "effects": {"fog_color": 0xAEB89C, "sky_color": 0xAEB89C, "water_color": 0x3F76E4, "water_fog_color": 0x050533, "grass_color": 0x86A35C,
                "foliage_color": 0x6E8F45, "ambient_sound": O + "ambient.underside.wind"},
    "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})
# artifact chests + recipe
ART = {"poolrooms_vault": "chlorine_pearl", "neighbourhood_house": "faded_photograph", "maze_vault": "thread_of_the_maze",
       "hotel_vault": "room_key_000", "industrial_generator": "humming_fuse", "level_fun_gift": "party_smile_badge"}
for table, art in ART.items():
    base = json.load(open(f'{D}/loot_tables/chests/{table}.json'))
    base["pools"].append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + art}]})
    w(f'{D}/loot_tables/chests/{table}_artifact.json', base)
w(f'{D}/recipes/underside_key.json', {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [{"item": O + a} for a in list(ART.values()) + ["ticket_stub"]], "result": {"item": O + "underside_key"}})
# lang
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({
    "item.office_rooms.chlorine_pearl": "Chlorine Pearl", "item.office_rooms.chlorine_pearl.desc": "From the end of the Poolrooms maze. Smells of chlorine and of something older.",
    "item.office_rooms.faded_photograph": "Faded Photograph", "item.office_rooms.faded_photograph.desc": "A family in front of a house in the Neighbourhood. Nobody in it has a face.",
    "item.office_rooms.thread_of_the_maze": "Thread of the Maze", "item.office_rooms.thread_of_the_maze.desc": "Unwound in the vault of the Maze. It still pulls somewhere.",
    "item.office_rooms.room_key_000": "Room Key 000", "item.office_rooms.room_key_000.desc": "From the vault of the Abandoned Hotel. There is no room 000.",
    "item.office_rooms.humming_fuse": "Humming Fuse", "item.office_rooms.humming_fuse.desc": "From a generator hall of the Industrial Complex. It hums without power.",
    "item.office_rooms.party_smile_badge": "Party Smile Badge", "item.office_rooms.party_smile_badge.desc": "A present from Level Fun. The smile is a little too wide.",
    "item.office_rooms.underside_key": "Underside Key", "item.office_rooms.underside_key.desc": "Use it on the bedrock beneath the lowest floor of the Office Rooms.",
    "item.office_rooms.noclip_sword": "Noclip Sword", "item.office_rooms.noclip_sword.desc": "Right-click: walk through walls (not floors) for 10 s. 30 s cooldown.",
    "entity.office_rooms.unknown": "[Unknown]", "block.office_rooms.underside_grass": "Upside-down Grass", "block.office_rooms.underside_portal": "Underside Portal",
    "subtitles.office_rooms.entity.unknown.roar": "[Unknown] roars", "subtitles.office_rooms.entity.unknown.hurt": "[Unknown] hurts",
    "subtitles.office_rooms.entity.unknown.death": "[Unknown] collapses", "subtitles.office_rooms.entity.unknown.burrow": "Something digs beneath you",
    "subtitles.office_rooms.entity.unknown.emerge": "The ground bursts open", "subtitles.office_rooms.ambient.underside.quake": "The ground shakes",
    "subtitles.office_rooms.item.underside_key.use": "Bedrock cracks", "subtitles.office_rooms.item.noclip_sword.on": "Reality glitches",
    "subtitles.office_rooms.item.noclip_sword.off": "Reality settles"})
w(lp, L)
# sounds.json
sp = f'{A}/sounds.json'; S = json.load(open(sp))
def ev(key, files, dist=None, sub=True, stream=False):
    e = {"sounds": []}
    for f in files:
        s = {"name": O + f}
        if dist: s["attenuation_distance"] = dist
        if stream: s["stream"] = True
        e["sounds"].append(s)
    if sub: e["subtitle"] = "subtitles.office_rooms." + key
    S[key] = e
ev("entity.unknown.roar", [f"entity/unknown/roar{i}" for i in (1, 2, 3)], 96)
ev("entity.unknown.hurt", [f"entity/unknown/hurt{i}" for i in (1, 2)], 48)
ev("entity.unknown.death", ["entity/unknown/death"], 128)
ev("entity.unknown.burrow", [f"entity/unknown/burrow{i}" for i in (1, 2, 3)], 40)
ev("entity.unknown.emerge", [f"entity/unknown/emerge{i}" for i in (1, 2)], 80)
ev("ambient.underside.quake", [f"ambient/underside/quake{i}" for i in (1, 2)], 96)
ev("ambient.underside.wind", ["ambient/underside/wind"], None, False, True)
ev("item.underside_key.use", ["item/underside_key_use"], 48)
ev("item.noclip_sword.on", ["item/noclip_on"], 16)
ev("item.noclip_sword.off", ["item/noclip_off"], 16)
w(sp, S)
print("ok")
