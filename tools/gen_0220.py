"""0.22.0: fuse box assets (textures drawn here)."""
import json, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(220)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def n(a=5): return rnd.randint(-a, a)
def cl(c): return tuple(max(0, min(255, int(v))) for v in c) + (255,)
def tex(name, fn):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = fn(x, y)
    im.save(f'{A}/textures/block/{name}.png')
def side(x, y):
    b = 128 + n(4)
    if x in (0, 15) or y in (0, 15): b -= 30
    return cl((b, b + 2, b + 6))
def front(broken):
    def fn(x, y):
        c = list(side(x, y)[:3])
        if 1 <= y <= 3 and 3 <= x <= 12: c = (215, 175, 25) if ((x + y) // 2) % 2 == 0 else (28, 28, 28)   # warning strip
        if 6 <= y <= 13 and 3 <= x <= 12:
            if broken:
                c = (36, 34, 34) if (x + y) % 3 else (60, 50, 44)                                    # open, burnt inside
                if (x, y) in ((5, 8), (9, 10), (7, 12)): c = (255, 200, 80)
                if x in (6, 10) and 7 <= y <= 12: c = (150, 40, 30)
            else:
                c = (100, 104, 110) if x % 3 else (70, 72, 78)                                        # rows of breakers
                if x % 3 == 1 and y in (8, 11): c = (40, 200, 70)
        if broken and rnd.random() < 0.12 and y > 4: c = (40, 34, 30)                              # scorch
        return cl(c)
    return fn
tex('fuse_box_side', side); tex('fuse_box_front', front(False)); tex('fuse_box_front_broken', front(True))
M = f'{A}/models/block'
for b, f in (('fuse_box', 'fuse_box_front'), ('fuse_box_broken', 'fuse_box_front_broken')):
    w(f'{M}/{b}.json', {"parent": "minecraft:block/block", "textures": {"s": O + "block/fuse_box_side", "f": O + "block/" + f, "particle": O + "block/fuse_box_side"},
        "elements": [{"from": [3, 1, 11], "to": [13, 15, 16], "faces": {"north": {"texture": "#f"}, "south": {"texture": "#s"}, "east": {"texture": "#s"},
                                                                       "west": {"texture": "#s"}, "up": {"texture": "#s"}, "down": {"texture": "#s"}}}]})
YR = {"north": 0, "east": 90, "south": 180, "west": 270}
w(f'{A}/blockstates/fuse_box.json', {"variants": {f"facing={d},broken={str(br).lower()}": {"model": O + "block/fuse_box" + ("_broken" if br else ""), "y": YR[d]}
                                                   for d in YR for br in (False, True)}})
w(f'{A}/models/item/fuse_box.json', {"parent": O + "block/fuse_box"})
w(f'{D}/loot_tables/blocks/fuse_box.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "fuse_box"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
p = f'{R}/data/minecraft/tags/blocks/mineable/pickaxe.json'; t = json.load(open(p))
if O + 'fuse_box' not in t["values"]: t["values"].append(O + 'fuse_box')
w(p, t)
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp)); L["block.office_rooms.fuse_box"] = "Fuse Box"; w(lp, L)
print("ok")
