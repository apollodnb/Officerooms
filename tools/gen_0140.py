"""0.14.0: Abandoned Hotel blocks (textures, models, blockstates, items), pipe, hotel loot tables, lang."""
import json, os, random, math
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(140)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def save(name, im):
    p = f'{A}/textures/block/{name}.png'; os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
def noise(base, var):
    im = Image.new('RGBA', (16, 16))
    for x in range(16):
        for y in range(16):
            d = rnd.randint(-var, var); im.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))
    return im
def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16: im.putpixel((x, y), tuple(c) + (255,))
# wallpaper: faded burgundy damask with a repeating gold motif and water stains
wp = noise((112, 34, 42), 5)
for x in range(16):
    for y in range(16):
        u, v = (x + 4) % 8 - 4, (y + 4) % 8 - 4
        if abs(u) + abs(v) == 3 or (u == 0 and abs(v) <= 1) or (v == 0 and abs(u) <= 1):
            put(wp, x, y, (150, 108, 64) if (x // 8 + y // 8) % 2 == 0 else (140, 98, 58))
for y in range(9, 16):
    for x in range(3, 7):
        if rnd.random() < 0.35:
            c = wp.getpixel((x, y)); put(wp, x, y, [int(c[i] * 0.85) for i in range(3)])
save('hotel_wallpaper', wp)
ws = noise((72, 46, 30), 4); d = ImageDraw.Draw(ws)
d.rectangle([0, 0, 15, 1], fill=(104, 70, 44)); d.rectangle([2, 4, 13, 14], outline=(52, 33, 22)); d.line([3, 5, 12, 5], fill=(92, 62, 40))
save('hotel_wainscot', ws)
cp = noise((124, 28, 34), 5)
for x in range(16):
    for y in range(16):
        u, v = x % 8, y % 8
        if abs(u - 4) + abs(v - 4) == 4: put(cp, x, y, (178, 136, 62))
        elif u == 4 and v == 4: put(cp, x, y, (60, 20, 30))
        elif abs(u - 4) + abs(v - 4) == 2: put(cp, x, y, (98, 22, 30))
save('hotel_carpet', cp)
mb = noise((226, 219, 202), 3)
for _ in range(5):
    x, y = rnd.randrange(16), rnd.randrange(16)
    for k in range(rnd.randint(6, 14)):
        put(mb, x % 16, y % 16, (168, 162, 152)); x += rnd.choice((-1, 0, 1, 1)); y += rnd.choice((0, 1))
save('hotel_marble', mb)
cl = noise((214, 206, 186), 3); d = ImageDraw.Draw(cl); d.rectangle([0, 0, 15, 15], outline=(196, 188, 168))
for _ in range(3): put(cl, rnd.randrange(16), rnd.randrange(16), (170, 158, 128))
save('hotel_ceiling', cl)
brass = noise((150, 118, 56), 8); save('hotel_brass', brass)
glow = Image.new('RGBA', (16, 16)); d = ImageDraw.Draw(glow)
for x in range(16):
    for y in range(16):
        r = math.hypot(x - 7.5, y - 7.5); v = max(0, 1 - r / 8)
        glow.putpixel((x, y), (int(220 + 35 * v), int(190 + 50 * v), int(120 + 60 * v), 255))
save('hotel_lamp_glow', glow)
dead = noise((70, 66, 58), 6); d = ImageDraw.Draw(dead); d.line([2, 3, 9, 12], fill=(30, 28, 26)); d.line([9, 12, 13, 8], fill=(30, 28, 26))
save('hotel_lamp_dead', dead)
sheet = noise((232, 228, 216), 4)
for y in (4, 9, 13): ImageDraw.Draw(sheet).line([0, y, 15, y], fill=(214, 208, 194))
save('hotel_sheet', sheet)
blanket = noise((128, 30, 40), 5); ImageDraw.Draw(blanket).rectangle([0, 0, 15, 2], fill=(232, 228, 216))
for x in range(0, 16, 4): ImageDraw.Draw(blanket).line([x, 3, x, 15], fill=(110, 24, 34))
save('hotel_blanket', blanket)
wood = noise((84, 54, 34), 5)
for y in range(0, 16, 3): ImageDraw.Draw(wood).line([0, y, 15, y], fill=(72, 46, 28))
save('hotel_wood', wood)
drawer = wood.copy(); d = ImageDraw.Draw(drawer)
for y0 in (1, 6, 11):
    d.rectangle([1, y0, 14, y0 + 3], outline=(52, 33, 20)); put(drawer, 7, y0 + 1, (190, 156, 80)); put(drawer, 8, y0 + 1, (190, 156, 80))
save('hotel_drawer', drawer)
desk = wood.copy(); d = ImageDraw.Draw(desk); d.rectangle([1, 2, 14, 13], outline=(150, 118, 56)); d.rectangle([3, 4, 12, 11], outline=(60, 38, 24))
save('hotel_desk_front', desk)
shade = noise((236, 222, 186), 4); save('hotel_shade', shade)
pipe = noise((96, 84, 74), 6)
for _ in range(14):
    x, y = rnd.randrange(16), rnd.randrange(16); put(pipe, x, y, (136, 72, 40)); put(pipe, (x + 1) % 16, y, (120, 64, 36))
for y in (0, 15): ImageDraw.Draw(pipe).line([0, y, 15, y], fill=(70, 60, 54))
save('pipe', pipe)
pend = noise((80, 70, 62), 4); d = ImageDraw.Draw(pend); d.ellipse([3, 3, 12, 12], fill=(30, 26, 24)); d.ellipse([3, 3, 12, 12], outline=(110, 96, 84))
save('pipe_end', pend)
# --- models ---------------------------------------------------------------------------------------------
def el(fr, to, tx, faces=('north', 'south', 'east', 'west', 'up', 'down'), glow=False, over=None):
    e = {"from": fr, "to": to, "faces": {f: {"texture": (over or {}).get(f, tx)} for f in faces}}
    if glow: e["forge_data"] = {"block_light": 15, "sky_light": 15}
    return e
T = lambda n: O + 'block/' + n
def item(n, parent=None): w(f'{A}/models/item/{n}.json', {"parent": parent or O + "block/" + n})
def cube(n):
    w(f'{A}/models/block/{n}.json', {"parent": "minecraft:block/cube_all", "textures": {"all": T(n)}})
    w(f'{A}/blockstates/{n}.json', {"variants": {"": {"model": O + "block/" + n}}}); item(n)
for n in ('hotel_wallpaper', 'hotel_wainscot', 'hotel_carpet', 'hotel_marble', 'hotel_ceiling'): cube(n)
def decor(name, textures, elements):
    w(f'{A}/models/block/{name}.json', {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": dict(textures, particle=list(textures.values())[0]), "elements": elements})
    item(name)
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={f}": ({"model": O + "block/" + name, "y": y} if y else {"model": O + "block/" + name})
                                                    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
decor('hotel_lamp', {"b": T('hotel_brass'), "g": T('hotel_lamp_glow')}, [el([3, 15, 3], [13, 16, 13], "#b"), el([4, 12, 4], [12, 15, 12], "#b", over={"down": "#g"}),
      el([4.5, 11.9, 4.5], [11.5, 12, 11.5], "#g", faces=('down',), glow=True)])
decor('hotel_lamp_broken', {"b": T('hotel_brass'), "g": T('hotel_lamp_dead')}, [el([3, 15, 3], [13, 16, 13], "#b"), el([4, 12, 4], [12, 15, 12], "#b", over={"down": "#g"})])
decor('hotel_bed', {"w": T('hotel_wood'), "s": T('hotel_sheet'), "k": T('hotel_blanket')}, [
    el([0, 0, 0], [16, 6, 16], "#w"), el([0, 6, 0], [16, 9, 16], "#s", over={"up": "#s"}),
    el([2, 9, 2], [14, 11, 6], "#s"), el([0, 0, 0], [16, 16, 2], "#w"), el([0, 8.5, 9], [16, 9.2, 16], "#k", faces=('up', 'east', 'west', 'south'))])
decor('hotel_bed_foot', {"w": T('hotel_wood'), "k": T('hotel_blanket')}, [
    el([0, 0, 0], [16, 6, 16], "#w"), el([0, 6, 0], [16, 9.2, 16], "#k"), el([0, 0, 14], [16, 10, 16], "#w")])
decor('hotel_nightstand', {"d": T('hotel_drawer'), "w": T('hotel_wood'), "b": T('hotel_brass'), "s": T('hotel_shade')}, [
    el([2, 0, 2], [14, 10, 14], "#w", over={"north": "#d"}), el([6, 10, 6], [10, 11, 10], "#b"), el([7.5, 11, 7.5], [8.5, 13, 8.5], "#b"),
    el([5, 13, 5], [11, 17, 11], "#s", glow=True)])
decor('hotel_dresser', {"d": T('hotel_drawer'), "w": T('hotel_wood')}, [el([0, 0, 1], [16, 14, 15], "#w", over={"north": "#d"})])
decor('hotel_reception_desk', {"f": T('hotel_desk_front'), "w": T('hotel_wood'), "m": T('hotel_marble')}, [
    el([0, 0, 1], [16, 14, 16], "#w", over={"north": "#f"}), el([0, 14, 0], [16, 16, 16], "#m")])
w(f'{A}/models/block/pipe.json', {"parent": "minecraft:block/block", "textures": {"s": T('pipe'), "e": T('pipe_end'), "particle": T('pipe')}, "elements": [
    el([4, 0, 4], [12, 16, 12], "#s", over={"up": "#e", "down": "#e"}), el([3, 0, 3], [13, 2, 13], "#s", over={"up": "#e", "down": "#e"}),
    el([3, 14, 3], [13, 16, 13], "#s", over={"up": "#e", "down": "#e"})]})
w(f'{A}/blockstates/pipe.json', {"variants": {"axis=y": {"model": O + "block/pipe"}, "axis=z": {"model": O + "block/pipe", "x": 90},
                                                "axis=x": {"model": O + "block/pipe", "x": 90, "y": 90}}})
item('pipe')
# --- data -------------------------------------------------------------------------------------------------
names = ['hotel_wallpaper', 'hotel_wainscot', 'hotel_carpet', 'hotel_marble', 'hotel_ceiling', 'hotel_lamp', 'hotel_lamp_broken', 'hotel_bed',
         'hotel_bed_foot', 'hotel_nightstand', 'hotel_dresser', 'hotel_reception_desk', 'pipe']
for n in names:
    w(f'{D}/loot_tables/blocks/{n}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + n}],
                                                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag, members in (('pickaxe', ['hotel_marble', 'hotel_ceiling', 'hotel_lamp', 'hotel_lamp_broken', 'pipe']),
                     ('axe', ['hotel_wallpaper', 'hotel_wainscot', 'hotel_bed', 'hotel_bed_foot', 'hotel_nightstand', 'hotel_dresser', 'hotel_reception_desk'])):
    p = f'{R}/data/minecraft/tags/blocks/mineable/{tag}.json'; t = json.load(open(p))
    t["values"] += [O + m for m in members if O + m not in t["values"]]; w(p, t)
def tier(n, rolls): return {"rolls": rolls, "entries": [{"type": "minecraft:loot_table", "name": f"office_rooms:tiers/{n}"}]}
def it(i, chance, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": i}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": chance}], "entries": [e]}
U = lambda a, b: {"type": "minecraft:uniform", "min": a, "max": b}
M = 'minecraft:'
T2 = {
 "hotel_room": [tier("common", U(2, 4)), tier("supplies", U(0, 2)), it(O + "almond_water", .4), it(M + "paper", .5, 1, 4), it(M + "book", .2),
                it(O + "glowstick", .3, 1, 3), it(O + "research_file", .03)],
 "hotel_reception": [tier("uncommon", U(1, 3)), tier("common", U(1, 3)), it(O + "level_1_access_card", .35), it(O + "level_2_access_card", .12),
                     it(M + "paper", .8, 2, 8), it(M + "book", .5, 1, 2), it(O + "research_file", .1)],
 "hotel_vault": [tier("rare", U(1, 3)), tier("uncommon", U(1, 2)), it(O + "research_file", .8, 1, 3), it(O + "level_3_access_card", .15),
                 it(O + "glitched_ingot", .4, 1, 3), it(O + "totem_of_mercy", .1), it(O + "master_key", .15), it(M + "gold_ingot", .6, 2, 6), it(M + "diamond", .35, 1, 3)],
 "hotel_safe_box": [tier("uncommon", U(1, 2)), it(M + "gold_nugget", .6, 3, 9), it(M + "emerald", .3, 1, 3), it(O + "research_file", .15)],
}
for n, pools in T2.items(): w(f'{D}/loot_tables/chests/{n}.json', {"type": "minecraft:chest", "pools": pools})
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({"block.office_rooms.hotel_wallpaper": "Hotel Wallpaper", "block.office_rooms.hotel_wainscot": "Hotel Wainscot",
 "block.office_rooms.hotel_carpet": "Hotel Carpet", "block.office_rooms.hotel_marble": "Hotel Marble", "block.office_rooms.hotel_ceiling": "Hotel Ceiling",
 "block.office_rooms.hotel_lamp": "Hotel Ceiling Lamp", "block.office_rooms.hotel_lamp_broken": "Broken Hotel Ceiling Lamp",
 "block.office_rooms.hotel_bed": "Hotel Bed (Head)", "block.office_rooms.hotel_bed_foot": "Hotel Bed (Foot)", "block.office_rooms.hotel_nightstand": "Hotel Nightstand",
 "block.office_rooms.hotel_dresser": "Hotel Dresser", "block.office_rooms.hotel_reception_desk": "Reception Desk", "block.office_rooms.pipe": "Rusty Pipe"})
json.dump(L, open(lp, 'w'), indent=2, ensure_ascii=False); print("ok")
