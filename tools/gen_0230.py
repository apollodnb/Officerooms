"""0.23.0: assets of the new standard rooms (all textures drawn here, nothing copied)."""
import json, os, random, math
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(230)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def n(a=5): return rnd.randint(-a, a)
def cl(c, a=255): return tuple(max(0, min(255, int(v))) for v in c) + (a,)
def tex(name, fn):
    im = Image.new('RGBA', (16, 16)); px = im.load()
    for y in range(16):
        for x in range(16):
            v = fn(x, y)
            px[x, y] = v if len(v) == 4 else cl(v)
    im.save(f'{A}/textures/block/{name}.png')
def flat(c, a=4): return lambda x, y: tuple(v + n(a) for v in c)

# ---- surfaces -------------------------------------------------------------------------------
stain = {(rnd.randrange(16), rnd.randrange(16)) for _ in range(5)}
def floor_tile(x, y):
    if x % 4 == 0 or y % 4 == 0: return (150 + n(6), 150 + n(6), 146 + n(6))
    b = 224 + n(5); c = (b, b, b - 4)
    if (x, y) in stain or ((x - 1, y) in stain and rnd.random() < .5): c = (196, 186, 150)
    return c
tex('restroom_floor_tile', floor_tile)
def wall_tile(x, y):
    if x % 8 == 0 or y % 4 == 0: return (168 + n(4), 176 + n(4), 168 + n(4))
    g = 214 + n(4) - (6 if y > 11 else 0)
    return (g - 34, g, g - 18)
tex('restroom_wall_tile', wall_tile)
def rug(x, y):
    cx, cy = abs(x - 7.5), abs(y - 7.5)
    if x in (0, 15) or y in (0, 15): return (70 + n(4), 18, 22)
    if x in (1, 14) or y in (1, 14): return (186 + n(6), 148 + n(6), 62)
    d = cx + cy
    if 4.5 < d < 6 or d < 1.5: return (186 + n(6), 148 + n(6), 62)
    if 2.5 < d < 3.5: return (30, 44 + n(3), 70)
    return (118 + n(5), 26 + n(3), 32 + n(3))
tex('executive_rug', rug)
def doormat(x, y):
    if x in (0, 15) or y in (0, 15): return (52 + n(3), 40, 30)
    b = 150 + n(14)
    if y in (6, 9) and 3 <= x <= 12: b -= 60          # faded letters, unreadable
    if 7 <= y <= 8 and x % 3 == 0 and 3 <= x <= 12: b -= 50
    return (b, int(b * .72), int(b * .40))
tex('doormat', doormat)

# ---- materials ------------------------------------------------------------------------------
tex('stall_laminate', lambda x, y: (176 + n(3), 168 + n(3), 152 + n(3)) if 0 < x < 15 else (140, 134, 122))
def door(top):
    def fn(x, y):
        c = (176 + n(3), 168 + n(3), 152 + n(3))
        if x in (0, 15): c = (140, 134, 122)
        if not top and 12 <= x <= 13 and 6 <= y <= 7: c = (200, 200, 205)              # latch
        if top and 11 <= x <= 14 and 11 <= y <= 13:                                     # OCCUPIED indicator
            c = (190, 30, 30) if 12 <= x <= 13 and y == 12 else (60, 60, 64)
        return c
    return fn
tex('stall_door', door(False)); tex('stall_door_top', door(True))
tex('chrome', lambda x, y: (200 + n(8) + (25 if (x + y) % 7 == 0 else 0),) * 3)
tex('white_plastic', flat((228, 228, 224), 3))
tex('dark_plastic', flat((52, 54, 58), 3))
def mirror(x, y):
    if x in (1, 14) or y in (1, 14): return (200, 200, 204)
    b = 120 + y * 3 + n(3)
    c = [b - 20, b - 6, b + 6]
    if (x - y) % 9 in (0, 1) and 3 < x < 12: c = [v + 22 for v in c]                # streak
    if (x - 9) ** 2 + (y - 10) ** 2 < 3: c = [v - 35 for v in c]                      # silvering gone
    return tuple(c)
tex('bathroom_mirror', mirror)
def dryer(x, y):
    c = (230 + n(2),) * 3
    if 9 <= y <= 10 and 5 <= x <= 10 and x % 2 == 0: c = (90, 90, 92)                # vents
    if y == 4 and 7 <= x <= 8: c = (60, 60, 60)
    return c
tex('hand_dryer_front', dryer)
def towel(x, y):
    c = (226 + n(2),) * 3
    if 12 <= y <= 13 and 5 <= x <= 10: c = (176, 148, 108)                           # paper
    if 4 <= y <= 9 and x == 7: c = (110, 112, 116)
    return c
tex('paper_towel_front', towel)
tex('projection_screen', lambda x, y: (226 + n(2), 232 + n(2), 240 + n(2)) if 0 < x < 15 else (225, 224, 218))
def screen_top(x, y):
    if y < 4: return (38 + n(2), 40, 44)
    return (226 + n(2), 232 + n(2), 240 + n(2)) if 0 < x < 15 else (225, 224, 218)
tex('projection_screen_top', screen_top)
def lens(x, y):
    d = (x - 7.5) ** 2 + (y - 7.5) ** 2
    return (20, 26, 40) if d < 10 else (40, 42, 46)
tex('projector_lens', lens)
tex('chair_fabric', lambda x, y: (66 + n(4), 78 + n(4), 98 + n(4)) if (x + y) % 2 else (60 + n(3), 72 + n(3), 92))
def flipchart(x, y):
    c = (242 + n(3), 242 + n(3), 236)
    if y in (6, 9, 12) and 3 <= x <= 3 + (x * 7 + y) % 10: c = (40, 70, 150) if y != 9 else (170, 40, 40)   # marker strokes
    return c
tex('flipchart_paper', flipchart)
def leather(x, y):
    b = 34 + n(3)
    if (x * 3 + y * 5) % 11 == 0: b += 14
    return (b, b - 4, b - 6)
tex('leather', leather)
tex('banker_green', lambda x, y: (34 + y * 3 + n(3), 112 + y * 4 + n(4), 64 + n(3)))
tex('brass', lambda x, y: (186 + n(8), 150 + n(8), 66 + n(5)))
def clock(x, y):
    d = math.hypot(x - 7.5, y - 7.5)
    if d > 5.3: return (30, 30, 32)
    if d > 4.6: return (46, 44, 42)
    c = (236 + n(2), 234, 226)
    if (x, y) in ((7, 3), (12, 7), (7, 12), (3, 7)): c = (90, 90, 90)                # 12 3 6 9
    if (x, y) in ((7, 4), (7, 5), (7, 6), (7, 7)): c = (20, 20, 20)                  # minute hand
    if (x, y) in ((8, 8), (9, 9), (10, 9)): c = (20, 20, 20)                         # hour hand
    if (x, y) in ((6, 9), (5, 10)): c = (180, 30, 30)                                # second hand, stuck
    return c
tex('wall_clock_face', clock)
def reception_front(x, y):
    c = (198 + n(4), 172 + n(4), 132 + n(4))
    if y in (5, 6): c = (186, 188, 192)                                                # steel strip
    if x in (0, 15): c = (150, 126, 92)
    return c
tex('reception_front', reception_front)
tex('dark_wood', lambda x, y: (112 + n(5) + (10 if y % 4 == 0 else 0), 78 + n(4), 52 + n(3)))
tex('teal_fabric', lambda x, y: (38 + n(4), 108 + n(5), 118 + n(5)))
MAG = [(196, 52, 48), (52, 92, 170), (230, 196, 60), (60, 150, 90), (220, 220, 214)]
for i in range(3):
    base = MAG[i]
    def cover(x, y, base=base, i=i):
        c = base
        if 2 + i <= y <= 4 + i and 3 <= x <= 12: c = (240, 240, 236)
        if y > 10 and 4 <= x <= 9: c = MAG[(i + 3) % 5]
        return tuple(v + n(4) for v in c)
    tex(f'magazine_{i}', cover)
tex('plant_pot', lambda x, y: (236 + n(3), 234 + n(3), 228) if y > 1 else (200, 198, 192))
tex('soil', flat((70, 52, 38), 6))
def leaves(x, y):
    h = (x * 7 + y * 13 + (x * y) % 5) % 9
    if h in (0, 4) or (y < 2 and x % 3): return (0, 0, 0, 0)
    g = 92 + n(14)
    dust = 18 if (x + y) % 6 == 0 else 0
    return cl((40 + dust, g + dust, 46 + dust))
tex('plant_leaves', leaves)
SEG = {'0': 'abcdef'}
def queue(x, y):
    c = (14, 12, 12)
    if x in (2, 13) or y in (5, 11): return (90, 90, 94)
    for ox in (4, 9):                                                                  # "00"
        if ((x in (ox, ox + 2) and 6 <= y <= 10) or (y in (6, 10) and ox <= x <= ox + 2)):
            c = (255, 40, 30)
    return c
tex('queue_display', queue)
tex('ticket_red', lambda x, y: (176 + n(5), 36 + n(3), 34 + n(3)))
tex('ticket_paper', flat((240, 236, 222), 3))

# ---- models ---------------------------------------------------------------------------------
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
def el(f, t, tx, glow=0, **over):
    e = {"from": f, "to": t, "faces": {k: {"texture": "#" + over.get(k, tx)} for k in FACES}}
    if glow:   # Forge per-element light: the face stays bright even in a dark room
        e["forge_data"] = {"block_light": glow, "sky_light": glow}
    return e
MODELS = {
 'restroom_wall_tile': None,
 'stall_partition': ([el([7, 2, 0], [9, 16, 16], 'l'), el([7, 0, 14], [9, 2, 16], 'c')], {'l': 'stall_laminate', 'c': 'chrome'}),
 'stall_partition_top': ([el([7, 0, 0], [9, 12, 16], 'l'), el([6.5, 12, 0], [9.5, 13, 16], 'c')], {'l': 'stall_laminate', 'c': 'chrome'}),
 'stall_door_locked': ([el([0, 2, 0], [16, 16, 2], 'l', north='d', south='d')], {'l': 'stall_laminate', 'd': 'stall_door'}),
 'stall_door_locked_top': ([el([0, 0, 0], [16, 12, 2], 'l', north='d', south='d'), el([0, 12, -0.5], [16, 13, 2.5], 'c')], {'l': 'stall_laminate', 'd': 'stall_door_top', 'c': 'chrome'}),
 'bathroom_mirror': ([el([1, 1, 15], [15, 15, 16], 'c', north='m')], {'c': 'chrome', 'm': 'bathroom_mirror'}),
 'hand_dryer': ([el([4, 3, 11], [12, 12, 16], 'w', north='f'), el([7, 2, 12], [9, 3, 14], 'c')], {'w': 'white_plastic', 'f': 'hand_dryer_front', 'c': 'chrome'}),
 'paper_towel_dispenser': ([el([4, 2, 12], [12, 15, 16], 'w', north='f')], {'w': 'white_plastic', 'f': 'paper_towel_front'}),
 'projection_screen': ([el([0, 0, 15], [16, 16, 16], 'g', glow=11, north='s')], {'g': 'white_plastic', 's': 'projection_screen'}),
 'projection_screen_top': ([el([0, 0, 15], [16, 12, 16], 'g', glow=11, north='s'), el([0, 12, 13], [16, 16, 16], 'k')], {'g': 'white_plastic', 's': 'projection_screen', 'k': 'dark_plastic'}),
 'ceiling_projector': ([el([7.5, 14, 7.5], [8.5, 16, 8.5], 'c'), el([5, 11.5, 4], [11, 14, 12], 'k'), el([6.5, 12, 3], [9.5, 13.5, 4], 'k', glow=10, north='o')], {'c': 'chrome', 'k': 'dark_plastic', 'o': 'projector_lens'}),
 'stacking_chair': ([el([2, 0, 2], [3, 7, 3], 'c'), el([13, 0, 2], [14, 7, 3], 'c'), el([2, 0, 13], [3, 7, 14], 'c'), el([13, 0, 13], [14, 7, 14], 'c'),
                     el([2, 7, 2], [14, 8.5, 14], 'f'), el([2, 8.5, 13], [3, 12, 14], 'c'), el([13, 8.5, 13], [14, 12, 14], 'c'), el([2, 12, 13], [14, 17, 14.5], 'f')],
                    {'c': 'chrome', 'f': 'chair_fabric'}),
 'flipchart': ([el([2, 0, 8], [3, 13, 9], 'c'), el([13, 0, 8], [14, 13, 9], 'c'), el([7.5, 0, 11], [8.5, 12, 12], 'c'),
                el([1, 4, 7], [15, 16, 8], 'k', north='p'), el([1, 15.5, 6.5], [15, 16.5, 8.5], 'c')], {'c': 'chrome', 'k': 'dark_plastic', 'p': 'flipchart_paper'}),
 'executive_chair': ([el([3, 0, 7], [13, 1, 9], 'c'), el([7, 0, 3], [9, 1, 13], 'c'), el([7, 1, 7], [9, 6, 9], 'c'), el([2, 6, 2], [14, 9, 14], 'l'),
                      el([2, 9, 12], [14, 23, 15], 'l'), el([1, 9, 4], [2, 12, 12], 'l'), el([14, 9, 4], [15, 12, 12], 'l')], {'c': 'chrome', 'l': 'leather'}),
 'desk_lamp': ([el([5, 0, 6], [11, 1, 10], 'b'), el([7.5, 1, 7.5], [8.5, 6, 8.5], 'b'), el([3, 6, 5], [13, 9, 11], 'g', glow=12)], {'b': 'brass', 'g': 'banker_green'}),
 'wall_clock': ([el([3, 3, 15], [13, 13, 16], 'k', north='f')], {'k': 'dark_plastic', 'f': 'wall_clock_face'}),
 'reception_desk': ([el([0, 0, 2], [16, 14, 16], 'w', north='f'), el([0, 14, 0], [16, 16, 16], 't')], {'w': 'dark_wood', 'f': 'reception_front', 't': 'dark_wood'}),
 'service_bell': ([el([5, 0, 5], [11, 1, 11], 'w'), el([6, 1, 6], [10, 3.5, 10], 'b'), el([7.5, 3.5, 7.5], [8.5, 4.2, 8.5], 'b')], {'w': 'dark_wood', 'b': 'brass'}),
 'waiting_chair': ([el([1, 0, 3], [2, 7, 13], 'c'), el([14, 0, 3], [15, 7, 13], 'c'), el([1, 7, 2], [15, 9, 14], 'f'), el([1, 9, 12], [15, 17, 14], 'f'),
                    el([0, 7, 4], [1, 12, 12], 'c'), el([15, 7, 4], [16, 12, 12], 'c')], {'c': 'chrome', 'f': 'teal_fabric'}),
 'magazine_stack': ([el([3, 0, 4], [12, 1, 12], 'a'), el([4, 1, 3], [13, 2, 11], 'b'), el([3.5, 2, 4.5], [12.5, 3, 12.5], 'c')],
                    {'a': 'magazine_0', 'b': 'magazine_1', 'c': 'magazine_2'}),
 'queue_display': ([el([2, 4, 14], [14, 11, 16], 'k', glow=13, north='q')], {'k': 'dark_plastic', 'q': 'queue_display'}),
 'ticket_dispenser': ([el([5, 3, 12], [11, 12, 16], 'r'), el([7, 4, 11], [9, 6, 12], 'p')], {'r': 'ticket_red', 'p': 'ticket_paper'}),
}
M = f'{A}/models/block'; YR = {"north": 0, "east": 90, "south": 180, "west": 270}
def model(name, elems, texs, extra=None):
    t = {k: O + 'block/' + v for k, v in texs.items()}; t['particle'] = t[next(iter(texs))]
    m = {"parent": "minecraft:block/block", "textures": t, "elements": elems}
    if extra: m.update(extra)
    w(f'{M}/{name}.json', m)
for name, spec in MODELS.items():
    if spec: model(name, *spec)
# wall tile: tiled face into the room, quartz everywhere else (looks normal from the next room)
w(f'{M}/restroom_wall_tile.json', {"parent": "minecraft:block/cube", "textures": {
    "north": O + "block/restroom_wall_tile", "south": "minecraft:block/quartz_block_side", "east": "minecraft:block/quartz_block_side",
    "west": "minecraft:block/quartz_block_side", "up": "minecraft:block/quartz_block_top", "down": "minecraft:block/quartz_block_bottom",
    "particle": "minecraft:block/quartz_block_side"}})
plane = lambda f, t: {"from": f, "to": t, "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45},
                      "faces": {k: {"texture": "#l", "uv": [0, 0, 16, 16]} for k in ('north', 'south', 'east', 'west')}}
model('office_plant', [el([4, 0, 4], [12, 7, 12], 'p', up='s'), plane([0, 6, 8], [16, 22, 8]), plane([8, 6, 0], [8, 22, 16])],
      {'p': 'plant_pot', 's': 'soil', 'l': 'plant_leaves'}, {"render_type": "minecraft:cutout"})
DECOR = list(MODELS) + ['office_plant']
for name in DECOR:
    w(f'{A}/blockstates/{name}.json', {"variants": {f"facing={d}": {"model": O + "block/" + name, "y": YR[d]} for d in YR}})
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + name})
CARPETS = ['executive_rug', 'doormat']
w(f'{M}/restroom_floor_tile.json', {"parent": "minecraft:block/cube_all", "textures": {"all": O + "block/restroom_floor_tile"}})
w(f'{A}/blockstates/restroom_floor_tile.json', {"variants": {"": {"model": O + "block/restroom_floor_tile"}}})
w(f'{A}/models/item/restroom_floor_tile.json', {"parent": O + "block/restroom_floor_tile"})
for name in CARPETS:
    w(f'{M}/{name}.json', {"parent": "minecraft:block/carpet", "textures": {"wool": O + "block/" + name}})
    w(f'{A}/blockstates/{name}.json', {"variants": {"": {"model": O + "block/" + name}}})
    w(f'{A}/models/item/{name}.json', {"parent": O + "block/" + name})
ALL = DECOR + CARPETS + ['restroom_floor_tile']
for name in ALL:
    w(f'{D}/loot_tables/blocks/{name}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + name}],
                                                                               "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag, names in (('pickaxe', ['restroom_wall_tile', 'stall_partition', 'stall_partition_top', 'stall_door_locked', 'stall_door_locked_top', 'hand_dryer',
                                'paper_towel_dispenser', 'ceiling_projector', 'stacking_chair', 'service_bell', 'waiting_chair', 'queue_display',
                                'ticket_dispenser', 'bathroom_mirror', 'restroom_floor_tile']),
                   ('axe', ['flipchart', 'reception_desk', 'wall_clock', 'executive_chair'])):
    p = f'{R}/data/minecraft/tags/blocks/mineable/{tag}.json'
    t = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
    for nm in names:
        if O + nm not in t["values"]: t["values"].append(O + nm)
    w(p, t)
NAMES = {'restroom_wall_tile': 'Restroom Wall Tiles', 'stall_partition': 'Stall Partition', 'stall_partition_top': 'Stall Partition (Top)',
         'stall_door_locked': 'Locked Stall Door', 'stall_door_locked_top': 'Locked Stall Door (Top)', 'bathroom_mirror': 'Restroom Mirror',
         'hand_dryer': 'Hand Dryer', 'paper_towel_dispenser': 'Paper Towel Dispenser', 'projection_screen': 'Projection Screen',
         'projection_screen_top': 'Projection Screen (Top)', 'ceiling_projector': 'Ceiling Projector', 'stacking_chair': 'Stacking Chair',
         'flipchart': 'Flipchart', 'executive_chair': 'Executive Chair', 'desk_lamp': 'Desk Lamp', 'wall_clock': 'Wall Clock',
         'reception_desk': 'Reception Desk', 'service_bell': 'Service Bell', 'waiting_chair': 'Waiting Room Chair', 'magazine_stack': 'Old Magazines',
         'office_plant': 'Office Plant', 'queue_display': 'Now Serving Display', 'ticket_dispenser': 'Ticket Dispenser',
         'restroom_floor_tile': 'Restroom Floor Tiles', 'executive_rug': 'Executive Rug', 'doormat': 'Doormat'}
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
for k, v in NAMES.items(): L[f'block.office_rooms.{k}'] = v
w(lp, L)
# chest loot tables of the new rooms (same tier pools as the other office rooms)
src = json.load(open(f'{D}/loot_tables/chests/archive.json'))
for room in ('executive_office', 'training_room', 'reception', 'restroom'):
    w(f'{D}/loot_tables/chests/{room}.json', json.loads(json.dumps(src).replace('"archive"', f'"{room}"')))
print(len(ALL), 'blocks ok')
