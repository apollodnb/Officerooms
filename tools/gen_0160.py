"""0.16.0: Level Fun blocks (wallpaper, balloons, streamer, confetti, gift box, party table, =) graffiti), Partygoer data."""
import json, os, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(160)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def save(name, im):
    p = f'{A}/textures/block/{name}.png'; os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
def noise(base, var, a=255):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            d = rnd.randint(-var, var); im.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (a,))
    return im
COLS = [(220, 50, 60), (60, 120, 230), (70, 190, 90), (236, 110, 180), (150, 80, 210), (250, 210, 60)]
wp = noise((214, 190, 86), 5)
for x in range(16):
    if x % 8 == 3:
        for y in range(16): wp.putpixel((x, y), (202, 176, 76, 255))
for _ in range(5):
    wp.putpixel((rnd.randrange(16), rnd.randrange(16)), rnd.choice(COLS) + (255,))
save('fun_wallpaper', wp)
for name, c in zip(('red', 'blue', 'green', 'pink', 'purple'), COLS):
    b = noise(c, 6); d = ImageDraw.Draw(b)
    d.rectangle([3, 2, 5, 4], fill=tuple(min(255, v + 80) for v in c)); b.putpixel((4, 3), (255, 255, 255, 255))
    save('balloon_' + name, b)
save('balloon_string', noise((220, 220, 216), 4))
st = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(st)
d.line([0, 0, 15, 0], fill=(240, 240, 240, 255))
for k in range(4):
    c = COLS[k % len(COLS)]; x0 = k * 4
    d.polygon([(x0, 1), (x0 + 3, 1), (x0 + 1, 6)], fill=c + (255,))
save('streamer', st)
cf = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
for _ in range(26): cf.putpixel((rnd.randrange(16), rnd.randrange(16)), rnd.choice(COLS) + (255,))
save('confetti', cf)
gift = noise((80, 170, 220), 5); d = ImageDraw.Draw(gift); d.rectangle([7, 0, 8, 15], fill=(230, 60, 120)); save('gift_box', gift)
gtop = noise((80, 170, 220), 5); d = ImageDraw.Draw(gtop); d.rectangle([7, 0, 8, 15], fill=(230, 60, 120)); d.rectangle([0, 7, 15, 8], fill=(230, 60, 120)); save('gift_box_top', gtop)
cloth = noise((244, 242, 236), 3); d = ImageDraw.Draw(cloth)
for x in range(0, 16, 2): cloth.putpixel((x, 15), (236, 110, 180, 255))
d.line([0, 1, 15, 1], fill=(236, 110, 180)); save('party_table', cloth)
save('party_table_top', noise((246, 244, 240), 3))
gr = Image.new('RGBA', (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(gr)
RED = (176, 20, 24, 235)
d.rectangle([3, 5, 6, 5], fill=RED); d.rectangle([3, 8, 6, 8], fill=RED)                       # "="
for y, x in ((3, 9), (4, 10), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 10), (11, 9)):  # ")"
    gr.putpixel((x, y), RED); gr.putpixel((x + 1, y), RED)
gr.putpixel((5, 12), RED); gr.putpixel((4, 13), RED); gr.putpixel((10, 13), RED)               # drips
save('smile_graffiti', gr)
T = lambda n: O + 'block/' + n
def el(fr, to, tx, faces=('north', 'south', 'east', 'west', 'up', 'down'), over=None):
    return {"from": fr, "to": to, "faces": {f: {"texture": (over or {}).get(f, tx)} for f in faces}}
def decor(name, textures, elements):
    w(f'{A}/models/block/{name}.json', {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": dict(textures, particle=list(textures.values())[0]), "elements": elements})
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + name})
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={f}": ({"model": O + "block/" + name, "y": y} if y else {"model": O + "block/" + name})
                                                    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
for name in ('red', 'blue', 'green', 'pink', 'purple'):
    decor('balloon_' + name, {"b": T('balloon_' + name), "s": T('balloon_string')}, [
        el([7.8, 0, 7.8], [8.2, 8, 8.2], "#s"), el([7, 7.5, 7], [9, 8.5, 9], "#b"), el([4, 8.5, 4], [12, 16, 12], "#b")])
decor('streamer', {"s": T('streamer')}, [el([0, 10, 8], [16, 16, 8], "#s", faces=('north', 'south'))])
decor('confetti', {"c": T('confetti')}, [el([0, 0, 0], [16, 0.25, 16], "#c", faces=('up', 'down'))])
decor('gift_box', {"g": T('gift_box'), "t": T('gift_box_top')}, [el([3, 0, 3], [13, 8, 13], "#g", over={"up": "#t"}),
      el([2.5, 8, 2.5], [13.5, 10, 13.5], "#g", over={"up": "#t"}), el([6, 10, 7.5], [10, 12, 8.5], "#g")])
decor('party_table', {"c": T('party_table'), "t": T('party_table_top')}, [el([0, 0, 0], [16, 15, 16], "#c", over={"up": "#t", "down": "#t"})])
decor('smile_graffiti', {"g": T('smile_graffiti')}, [el([0, 0, 15.9], [16, 16, 16], "#g", faces=('north',))])
w(f'{A}/models/block/fun_wallpaper.json', {"parent": "minecraft:block/cube_all", "textures": {"all": T('fun_wallpaper')}})
w(f'{A}/blockstates/fun_wallpaper.json', {"variants": {"": {"model": O + "block/fun_wallpaper"}}})
w(f'{A}/models/item/fun_wallpaper.json', {"parent": O + "block/fun_wallpaper"})
w(f'{A}/models/item/partygoer_spawn_egg.json', {"parent": "minecraft:item/template_spawn_egg"})
names = ['fun_wallpaper', 'balloon_red', 'balloon_blue', 'balloon_green', 'balloon_pink', 'balloon_purple', 'streamer', 'gift_box', 'party_table', 'smile_graffiti']
for n in names + ['confetti']:
    pools = [] if n == 'confetti' else [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + n}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]
    w(f'{D}/loot_tables/blocks/{n}.json', {"type": "minecraft:block", "pools": pools})
p = f'{R}/data/minecraft/tags/blocks/mineable/axe.json'; t = json.load(open(p))
t["values"] += [O + m for m in ('fun_wallpaper', 'party_table', 'gift_box') if O + m not in t["values"]]; w(p, t)
def it(i, chance, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": i}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": chance}], "entries": [e]}
M = 'minecraft:'
w(f'{D}/loot_tables/chests/level_fun_gift.json', {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [{"type": "minecraft:loot_table", "name": "office_rooms:tiers/uncommon"}]},
    it(M + "cake", .5), it(M + "cookie", .8, 3, 12), it(M + "firework_rocket", .5, 2, 6), it(O + "almond_water", .5, 1, 2), it(O + "glowstick", .5, 2, 5),
    it(O + "noise_maker", .25), it(M + "golden_apple", .12), it(M + "emerald", .3, 1, 4), it(O + "research_file", .15)]})
w(f'{D}/loot_tables/entities/partygoer.json', {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "cookie", "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:killed_by_player"}, {"condition": "minecraft:random_chance", "chance": 0.05}],
     "entries": [{"type": "minecraft:item", "name": O + "research_file"}]}]})
f = f'{A}/sounds.json'; S = json.load(open(f))
for key, files, dist in (("giggle", ["giggle1", "giggle2"], 24), ("horn", ["horn"], 32), ("laugh", ["laugh"], 32), ("hurt", ["hurt"], 16), ("death", ["death"], 16)):
    S["entity.partygoer." + key] = {"category": "hostile", "subtitle": "subtitles.office_rooms.partygoer." + key,
                                    "sounds": [{"name": "office_rooms:entity/partygoer/" + n, "attenuation_distance": dist} for n in files]}
json.dump(S, open(f, 'w'), indent=2)
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({"entity.office_rooms.partygoer": "Partygoer", "item.office_rooms.partygoer_spawn_egg": "Partygoer Spawn Egg",
 "subtitles.office_rooms.partygoer.giggle": "Someone giggles", "subtitles.office_rooms.partygoer.horn": "A party horn",
 "subtitles.office_rooms.partygoer.laugh": "Delighted laughter", "subtitles.office_rooms.partygoer.hurt": "Partygoer squeaks",
 "subtitles.office_rooms.partygoer.death": "Partygoer deflates",
 "block.office_rooms.fun_wallpaper": "Party Wallpaper", "block.office_rooms.balloon_red": "Red Balloon", "block.office_rooms.balloon_blue": "Blue Balloon",
 "block.office_rooms.balloon_green": "Green Balloon", "block.office_rooms.balloon_pink": "Pink Balloon", "block.office_rooms.balloon_purple": "Purple Balloon",
 "block.office_rooms.streamer": "Party Streamer", "block.office_rooms.confetti": "Confetti", "block.office_rooms.gift_box": "Gift Box",
 "block.office_rooms.party_table": "Party Table", "block.office_rooms.smile_graffiti": "=)"})
json.dump(L, open(lp, 'w'), indent=2, ensure_ascii=False); print("ok")

# --- 0.16.1: red play carpet (curving roads, little cars) and inflatable bouncy-castle blocks ------------------
import math
S = 32
pc = Image.new('RGBA', (S, S))
for x in range(S):
    for y in range(S):
        v = math.sin((x + 2 * y) / 3.1) * 6 + rnd.randint(-6, 6)
        pc.putpixel((x, y), (int(170 + v), int(30 + v / 3), int(32 + v / 3), 255))
for x in range(S):                                                         # darker swirls
    for y in range(S):
        if abs(math.sin(2 * math.pi * (x + y * 0.5) / S) * 6 + 16 - (y + x * 0.3) % S) < 0.8:
            r, g, b, a = pc.getpixel((x, y)); pc.putpixel((x, y), (r - 40, g - 10, b - 10, 255))
def road(fn):
    pts = []
    for i in range(400):
        t = i / 400
        pts.append(fn(t))
    for k, (x, y) in enumerate(pts):
        xi, yi = int(x) % S, int(y) % S
        for dx in (-1, 0, 1):                                              # dark asphalt-red band
            r, g, b, a = pc.getpixel(((xi + dx) % S, yi)); pc.putpixel(((xi + dx) % S, yi), (max(0, r - 50), g, b, 255))
        if (k // 18) % 2 == 0:
            pc.putpixel((xi, yi), (236, 196, 50, 255))                    # yellow dashes
road(lambda t: (t * S, 8 + 4 * math.sin(2 * math.pi * t)))
road(lambda t: (20 + 5 * math.sin(2 * math.pi * t), t * S))
for cx, cy, c in ((5, 20, (60, 110, 220)), (24, 4, (60, 110, 220)), (12, 27, (240, 200, 60)), (27, 18, (90, 170, 230))):
    for dx in range(4):
        for dy in range(2):
            pc.putpixel(((cx + dx) % S, (cy + dy) % S), c + (255,))
    pc.putpixel(((cx + 1) % S, (cy + 2) % S), (30, 30, 30, 255)); pc.putpixel(((cx + 3) % S, (cy + 2) % S), (30, 30, 30, 255))
    pc.putpixel(((cx + 1) % S, cy % S), (200, 230, 255, 255))
pc.save(f'{A}/textures/block/play_carpet.png')
for name, base in (('inflatable_red', (226, 52, 58)), ('inflatable_blue', (56, 112, 226)), ('inflatable_yellow', (244, 206, 52))):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            v = int(28 * math.cos(math.pi * ((x % 8) - 3.5) / 8))           # puffy air chambers
            im.putpixel((x, y), tuple(max(0, min(255, c + v - 14 + rnd.randint(-3, 3))) for c in base) + (255,))
    for y in range(16):
        im.putpixel((0, y), tuple(int(c * 0.7) for c in base) + (255,)); im.putpixel((8, y), tuple(int(c * 0.7) for c in base) + (255,))
    im.save(f'{A}/textures/block/{name}.png')
for n in ('play_carpet', 'inflatable_red', 'inflatable_blue', 'inflatable_yellow'):
    w(f'{A}/models/block/{n}.json', {"parent": "minecraft:block/cube_all", "textures": {"all": T(n)}})
    w(f'{A}/blockstates/{n}.json', {"variants": {"": {"model": O + "block/" + n}}})
    w(f'{A}/models/item/{n}.json', {"parent": O + "block/" + n})
    w(f'{D}/loot_tables/blocks/{n}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + n}],
                                                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
L = json.load(open(lp))
L.update({"block.office_rooms.play_carpet": "Play Carpet", "block.office_rooms.inflatable_red": "Red Inflatable",
          "block.office_rooms.inflatable_blue": "Blue Inflatable", "block.office_rooms.inflatable_yellow": "Yellow Inflatable"})
json.dump(L, open(lp, 'w'), indent=2, ensure_ascii=False); print("ok 0.16.1")
