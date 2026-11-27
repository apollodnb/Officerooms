"""0.44.0: Office Rooms' own office furniture (replaces OfficeCraft). All drawn here, own designs.
Run: python3 tools/gen_0440.py"""
import json, os, random
from PIL import Image
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
M = f'{A}/models/block'
rnd = random.Random(440)


def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)


def n(a=5): return rnd.randint(-a, a)
def cl(c): return tuple(max(0, min(255, int(v))) for v in c[:3]) + ((int(c[3]) if len(c) > 3 else 255),)
def nz(c, a=4): return tuple(v + n(a) for v in c[:3]) + tuple(c[3:])


def tex(name, fn, size=16):
    im = Image.new('RGBA', (size, size)); px = im.load()
    for y in range(size):
        for x in range(size):
            px[x, y] = cl(fn(x, y))
    im.save(f'{A}/textures/block/{name}.png')


# ---- textures -----------------------------------------------------------------------------------------------------
tex('office_desk_top', lambda x, y: nz((214, 210, 200), 3) if 0 < y < 15 else nz((150, 148, 142), 2))      # light laminate, dark edge band
tex('office_desk_panel', lambda x, y: nz((176, 174, 168), 3) if x % 15 else nz((120, 120, 118), 2))
tex('office_metal', lambda x, y: nz((72, 74, 78), 3))
tex('office_chrome', lambda x, y: nz((186, 190, 196), 6))
def drawer(x, y):
    if y in (5, 10) or x in (0, 15) or y in (0, 15): return nz((120, 120, 118), 2)
    if 6 <= x <= 9 and y in (2, 7, 12): return nz((60, 60, 62), 2)                                      # handles
    return nz((186, 184, 178), 3)
tex('office_drawer', drawer)
def screen(x, y):
    if x in (0, 15) or y in (0, 15): return nz((26, 26, 28), 2)
    if 2 <= y <= 3 and 2 <= x <= 13: return (50, 90, 150)                                                # a window title bar
    if 5 <= y <= 12 and 2 <= x <= 13 and (x + y * 3) % 7: return nz((210, 220, 228), 6)                   # a spreadsheet
    return nz((70, 110, 150), 6)
tex('office_screen', screen)
tex('office_keyboard', lambda x, y: (40, 40, 42) if (x % 2 == 0 or y % 3 == 0) else nz((200, 200, 196), 4))
tex('office_plastic_grey', lambda x, y: nz((150, 152, 154), 3))
tex('office_plastic_dark', lambda x, y: nz((44, 44, 48), 3))
tex('office_paper', lambda x, y: nz((236, 236, 230), 2) if y % 3 else nz((176, 184, 200), 4))
tex('office_mug', lambda x, y: nz((236, 234, 226), 3) if y > 3 else nz((110, 70, 40), 4))
for nme, c in (('black', (36, 36, 40)), ('gray', (118, 120, 126)), ('brown', (110, 74, 48))):
    tex(f'office_fabric_{nme}', lambda x, y, c=c: nz(c, 5) if (x + y) % 3 else nz(tuple(v - 12 for v in c), 3))
def partition(x, y):
    if x in (0, 15): return nz((176, 178, 182), 4)                                                     # aluminium post
    return nz((88, 104, 128), 5) if (x * 3 + y) % 5 else nz((78, 92, 116), 4)                           # blue-grey fabric
tex('office_partition', partition)
tex('office_partition_trim', lambda x, y: nz((176, 178, 182), 4))
tex('office_conference_top', lambda x, y: nz((96, 64, 42), 5) if y % 5 else nz((80, 52, 34), 3))
tex('office_conference_edge', lambda x, y: nz((60, 40, 26), 3))
# the whiteboard: one 48 x 32 drawing, cut into six tiles (3 lower, 3 upper)
WB = Image.new('RGBA', (48, 32), (238, 240, 240, 255)); wp = WB.load()
for x in range(48):
    for y in range(32):
        if x in (0, 47) or y in (0, 31): wp[x, y] = (150, 154, 160, 255)
for k, (x0, h) in enumerate(((6, 18), (12, 14), (18, 11), (24, 9), (30, 6), (36, 3))):            # a chart that only goes down
    for x in range(x0, x0 + 4):
        for y in range(27 - h, 27):
            wp[x, y] = (60, 110, 190, 255) if k < 4 else (200, 50, 50, 255)
for x in range(4, 44): wp[x, 27] = (40, 40, 40, 255)
for y in range(5, 28): wp[4, y] = (40, 40, 40, 255)
for i in range(14): wp[6 + i * 2, 4 + (i % 3)] = (30, 30, 30, 255)                                 # scribbled title
for y in range(4, 10):
    for x in range(40, 46): wp[x, y] = (240, 214, 80, 255)                                         # a sticky note
for k in range(3):
    WB.crop((k * 16, 16, k * 16 + 16, 32)).save(f'{A}/textures/block/office_whiteboard_{k}.png')
    WB.crop((k * 16, 0, k * 16 + 16, 16)).save(f'{A}/textures/block/office_whiteboard_{3 + k}.png')
tex('office_cooler', lambda x, y: nz((228, 230, 232), 3) if not (6 <= x <= 9 and 3 <= y <= 5) else (40, 120, 200) if x < 8 else (200, 50, 50))
tex('office_bottle', lambda x, y: (120, 180, 230, 150) if y > 2 else (90, 150, 210, 200))
def printer(x, y):
    if y < 3: return nz((236, 236, 232), 2)                                                           # paper in the tray
    if 10 <= x <= 13 and 5 <= y <= 6: return (60, 200, 90)
    return nz((196, 196, 192), 3)
tex('office_printer', printer)
tex('office_printer_top', lambda x, y: (40, 40, 42) if 3 <= y <= 4 else nz((176, 176, 172), 3))
def coffee(x, y):
    if 4 <= x <= 11 and 8 <= y <= 14: return (60, 34, 20) if y > 10 else (170, 190, 200)               # the carafe
    if y < 3 and x in (4, 6): return (220, 40, 40)
    return nz((38, 38, 40), 3)
tex('office_coffee', coffee)


# ---- models ---------------------------------------------------------------------------------------------------------
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
YR = {"north": 0, "east": 90, "south": 180, "west": 270}


def el(f, t, tx, glow=0, **over):
    e = {"from": f, "to": t, "faces": {k: {"texture": "#" + over.get(k, tx)} for k in FACES}}
    if glow: e["forge_data"] = {"block_light": glow, "sky_light": glow}
    return e


def model(name, elems, texs, rt=None):
    t = {k: (v if ':' in v else O + 'block/' + v) for k, v in texs.items()}; t['particle'] = t[next(iter(texs))]
    m = {"parent": "minecraft:block/block", "textures": t, "elements": elems}
    if rt: m["render_type"] = rt
    w(f'{M}/{name}.json', m)


TOP = lambda: el([0, 14, 0], [16, 16, 16], 't', north='e', south='e', east='e', west='e')
MOD = lambda: el([0, 3, 13], [16, 14, 14], 'p')                         # modesty panel at the back
PIECES = {
    'office_desk': {
        0: [TOP(), MOD(), el([0, 0, 1], [1, 14, 15], 'p')],
        1: [TOP(), MOD()],
        2: [TOP(), MOD(), el([10, 0, 1], [16, 14, 14], 'p', north='d')],
        3: [TOP(), MOD(), el([0, 0, 1], [1, 14, 15], 'p'), el([15, 0, 1], [16, 14, 15], 'p')],
        'tex': {'t': 'office_desk_top', 'e': 'office_desk_panel', 'p': 'office_desk_panel', 'd': 'office_drawer'}},
    'office_computer': {0: [el([2, 2, 10], [14, 11, 11], 'b', north='s', glow=6), el([7, 0, 11], [9, 3, 12], 'b'), el([5, 0, 10.5], [11, 0.5, 13], 'b'),
                            el([3, 0, 3], [13, 0.6, 7], 'k'), el([14, 0, 4], [15.5, 0.6, 6], 'b')],
                        'tex': {'b': 'office_plastic_dark', 's': 'office_screen', 'k': 'office_keyboard'}},
    'office_phone': {0: [el([5, 0, 6], [11, 2, 11], 'g'), el([5, 2, 6], [7, 3, 11], 'd')], 'tex': {'g': 'office_plastic_grey', 'd': 'office_plastic_dark'}},
    'office_papers': {0: [el([3, 0, 4], [12, 2, 12], 'p'), el([5, 2, 5], [13, 2.5, 13], 'p')], 'tex': {'p': 'office_paper'}},
    'office_mug': {0: [el([6, 0, 6], [10, 5, 10], 'm'), el([10, 1, 7.5], [11.5, 4, 8.5], 'm')], 'tex': {'m': 'office_mug'}},
    'office_partition': {0: [el([1, 0, 7], [15, 16, 9], 'f'), el([0, 0, 6.5], [1, 16, 9.5], 'a'), el([15, 0, 6.5], [16, 16, 9.5], 'a')],
                         1: [el([1, 0, 7], [15, 9, 9], 'f'), el([0, 0, 6.5], [1, 10, 9.5], 'a'), el([15, 0, 6.5], [16, 10, 9.5], 'a'), el([0, 9, 6.5], [16, 10, 9.5], 'a')],
                         'tex': {'f': 'office_partition', 'a': 'office_partition_trim'}},
    'office_conference_table': {0: [el([0, 13, 0], [16, 16, 16], 't', north='e', south='e', east='e', west='e')],
                                1: [el([0, 13, 0], [16, 16, 16], 't', north='e', south='e', east='e', west='e'), el([6, 0, 6], [10, 13, 10], 'm')],
                                'tex': {'t': 'office_conference_top', 'e': 'office_conference_edge', 'm': 'office_metal'}},
    'office_water_cooler': {0: [el([3, 0, 3], [13, 16, 13], 'c')], 1: [el([4, 0, 4], [12, 10, 12], 'b'), el([6.5, 10, 6.5], [9.5, 12, 9.5], 'b')],
                            'tex': {'c': 'office_cooler', 'b': 'office_bottle'}, 'rt': 'minecraft:translucent'},
    'office_printer': {0: [el([1, 0, 2], [15, 7, 14], 'p', up='t'), el([3, 7, 4], [13, 9, 12], 'p', up='t')], 'tex': {'p': 'office_printer', 't': 'office_printer_top'}},
    'office_coffee_machine': {0: [el([3, 0, 4], [13, 12, 13], 'c')], 'tex': {'c': 'office_coffee'}},
}
for c in ('black', 'gray', 'brown'):
    PIECES[f'office_chair_{c}'] = {0: [el([7, 0, 2], [9, 1, 14], 'm'), el([2, 0, 7], [14, 1, 9], 'm'), el([7, 1, 7], [9, 6, 9], 'm'),
                                       el([2, 6, 2], [14, 8, 14], 'f'), el([2, 8, 12], [14, 21, 14], 'f'), el([1, 8, 4], [2, 11, 11], 'm'),
                                       el([14, 8, 4], [15, 11, 11], 'm')],
                                   'tex': {'f': f'office_fabric_{c}', 'm': 'office_metal'}}
# whiteboard: lower tiles on legs, upper tiles with a frame
wb = {'tex': {}}
for k in range(6):
    t = f'w{k}'
    wb['tex'][t] = f'office_whiteboard_{k}'
    if k < 3:
        e = [el([0, 4, 7.5], [16, 16, 8.5], 'f', north=t, south=t)]
        if k != 1:
            e.append(el([7, 0, 6], [9, 4, 10], 'f'))
    else:
        e = [el([0, 0, 7.5], [16, 14, 8.5], 'f', north=t, south=t), el([0, 14, 7], [16, 15, 9], 'f')]
    wb[k] = e
wb['tex']['f'] = 'office_partition_trim'
PIECES['office_whiteboard'] = wb

for name, spec in PIECES.items():
    parts = sorted(k for k in spec if isinstance(k, int))
    for k in parts:
        model(f'{name}_{k}', spec[k], spec['tex'], rt=spec.get('rt'))
    var = {}
    for d in YR:
        for k in range(6):
            mk = k if k in parts else parts[0]
            v = {"model": O + f"block/{name}_{mk}"}
            if YR[d]: v["y"] = YR[d]
            var[f"facing={d},part={k}"] = v
    w(f'{A}/blockstates/{name}.json', {"variants": var})
    w(f'{A}/models/item/{name}.json', {"parent": O + f"block/{name}_{parts[-1] if name == 'office_desk' else parts[0]}"})
    w(f'{D}/loot_tables/blocks/{name}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + name}],
      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
lang.update({f'block.office_rooms.{k}': v for k, v in {
    'office_desk': 'Office Desk', 'office_computer': 'Office Computer', 'office_phone': 'Desk Phone', 'office_papers': 'Stack of Papers',
    'office_mug': 'Coffee Mug', 'office_chair_black': 'Black Office Chair', 'office_chair_gray': 'Grey Office Chair', 'office_chair_brown': 'Brown Office Chair',
    'office_partition': 'Cubicle Partition', 'office_conference_table': 'Conference Table', 'office_whiteboard': 'Whiteboard',
    'office_water_cooler': 'Water Cooler', 'office_printer': 'Printer', 'office_coffee_machine': 'Coffee Machine'}.items()})
w(L, lang)
print('office furniture:', len(PIECES))
