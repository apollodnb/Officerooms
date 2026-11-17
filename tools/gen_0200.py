"""0.20.0: research outpost assets (all textures drawn here)."""
import json, os, math, random
from PIL import Image, ImageDraw
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
rnd = random.Random(200)
def w(p, o):
    os.makedirs(os.path.dirname(p), exist_ok=True); json.dump(o, open(p, 'w'), indent=1)
def tex(name, fn, size=16, frames=1):
    im = Image.new('RGBA', (size, size * frames), (0, 0, 0, 0)); px = im.load()
    for f in range(frames):
        for y in range(size):
            for x in range(size):
                px[x, y + f * size] = fn(x, y, f)
    p = f'{A}/textures/block/{name}.png'; os.makedirs(os.path.dirname(p), exist_ok=True); im.save(p)
    if frames > 1: json.dump({"animation": {"frametime": 2}}, open(p + '.mcmeta', 'w'))
def n(a=8): return rnd.randint(-a, a)
def cl(c): return tuple(max(0, min(255, v)) for v in c) + (255,)
# --- surfaces
def wall(x, y, f):
    b = 214 + n(5)
    if y in (0, 15) or x in (0, 15): b -= 28
    if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): b -= 50
    if y > 10 and rnd.random() < 0.25: b -= 18
    return cl((b, b - 4 + n(2), b - 16 + n(2)))
tex('outpost_wall', wall)
def floor(x, y, f):
    b = 150 + n(6)
    if x % 8 == 0 or y % 8 == 0: b -= 45
    if rnd.random() < 0.05: b -= 30
    return cl((b, b + 2, b + 4))
tex('outpost_floor', floor)
def ceil_(x, y, f):
    b = 176 + n(4)
    if x in (0, 15) or y in (0, 15): b -= 40
    if (x * 7 + y * 3) % 11 == 0: b -= 22
    if rnd.random() < 0.04: b -= 35
    return cl((b, b, b - 6))
tex('outpost_ceiling', ceil_)
def hazard(x, y, f):
    yel = ((x + y) // 4) % 2 == 0
    c = (222 + n(6), 180 + n(6), 30) if yel else (32 + n(4), 30 + n(4), 30)
    if rnd.random() < 0.08: c = (110, 100, 80)
    return cl(c)
tex('hazard_floor', hazard)
def metal(x, y, f, base=(70, 72, 78)):
    b = n(4); c = (base[0] + b, base[1] + b, base[2] + b)
    if x in (0, 15) or y in (0, 15): c = tuple(v - 18 for v in c)
    if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)): c = (150, 150, 155)
    return cl(c)
tex('outpost_metal', metal)
tex('outpost_metal_light', lambda x, y, f: metal(x, y, f, (150, 152, 158)))
def frame(on):
    def fn(x, y, f):
        c = metal(x, y, f, (44, 46, 52))[:3]
        if x in (4, 11) and 2 < y < 13: c = (90, 235, 255) if on else (30, 70, 80)
        if y in (4, 11) and 2 < x < 13: c = (90, 235, 255) if on else (30, 70, 80)
        if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 3: c = (230, 250, 255) if on else (50, 60, 66)
        if x in (0, 15) or y in (0, 15): c = (200, 160, 30) if (x + y) % 4 < 2 else (30, 30, 30)
        return cl(c)
    return fn
tex('gateway_frame', frame(False)); tex('gateway_frame_on', frame(True))
def gate(x, y, f):
    ph = f / 16 * 2 * math.pi; dx, dy = x - 7.5, y - 7.5; r = math.hypot(dx, dy); a = math.atan2(dy, dx)
    v = 0.5 + 0.5 * math.sin(r * 1.1 - ph * 2 + math.sin(a * 2 + ph))
    s = rnd.random()
    if s < 0.06: return (255, 255, 255, 230)
    if s < 0.1: return (240, 225, 140, 220)
    return (int(40 + 120 * v), int(150 + 90 * v), int(200 + 55 * v), 205)
tex('dimensional_gateway', gate, frames=16)
def stripes(x, y, f): return cl((215, 175, 25) if ((x + y) // 3) % 2 == 0 else (28, 28, 28))
tex('hazard_stripes', stripes)
def red(lit):
    def fn(x, y, f):
        d = math.hypot(x - 6, y - 5)
        b = 250 - int(d * 8) if lit else 150 - int(d * 6)
        return cl((b, int(b * 0.12), int(b * 0.1)))
    return fn
tex('emergency_red', red(True)); tex('warning_light', red(False)); tex('warning_light_on', red(True))
def panel(lit):
    def fn(x, y, f):
        if x in (0, 15) or y in (0, 15): return cl((90, 90, 95))
        return cl((250, 248, 228) if lit else (150, 150, 145))
    return fn
tex('facility_light', panel(False)); tex('facility_light_on', panel(True))
def console_top(x, y, f):
    c = metal(x, y, f, (58, 60, 66))[:3]
    if 2 <= y <= 4 and 2 <= x <= 13: c = (20, 40, 30) if x % 3 else (60, 220, 90)
    if y in (7, 8) and x % 3 == 1: c = [(220, 40, 40), (240, 200, 40), (60, 160, 240)][x % 9 // 3]
    if 11 <= y <= 13 and x in (3, 4, 11, 12): c = (200, 200, 205)
    if (x - 8) ** 2 + (y - 12) ** 2 < 4: c = (25, 25, 25)
    return cl(c)
tex('console_panel', console_top)
def console_front(x, y, f):
    c = metal(x, y, f, (82, 84, 90))[:3]
    if y in (3, 4) and 3 <= x <= 12: c = (40, 40, 44)
    if y == 12 and x % 4 == 1: c = (240, 180, 40)
    return cl(c)
tex('console_front', console_front)
def plate(x, y, f):
    if x in (0, 15) or y in (0, 15): return cl((30, 30, 30))
    if y < 4: return cl((215, 175, 25) if ((x + y) // 2) % 2 == 0 else (28, 28, 28))
    return metal(x, y, f, (96, 98, 104))
tex('lever_plate', plate)
tex('lever_grip', lambda x, y, f: cl((190 + n(10), 30, 25)))
# --- models
M = f'{A}/models/block'; I = f'{A}/models/item'; B = f'{A}/blockstates'
def cube(name, t):
    w(f'{M}/{name}.json', {"parent": "minecraft:block/cube_all", "textures": {"all": O + "block/" + t}})
for n_ in ('outpost_wall', 'outpost_floor', 'outpost_ceiling', 'hazard_floor'):
    cube(n_, n_); w(f'{B}/{n_}.json', {"variants": {"": {"model": O + "block/" + n_}}}); w(f'{I}/{n_}.json', {"parent": O + "block/" + n_})
cube('gateway_frame', 'gateway_frame'); cube('gateway_frame_on', 'gateway_frame_on')
w(f'{B}/gateway_frame.json', {"variants": {"active=false": {"model": O + "block/gateway_frame"}, "active=true": {"model": O + "block/gateway_frame_on"}}})
w(f'{I}/gateway_frame.json', {"parent": O + "block/gateway_frame"})
def faces(t, ids=("north", "south", "east", "west", "up", "down")): return {k: {"texture": t} for k in ids}
def el(a, b, t, rot=None):
    e = {"from": a, "to": b, "faces": faces(t)}
    if rot: e["rotation"] = rot
    return e
YR = {"north": 0, "east": 90, "south": 180, "west": 270}
# lever
for on in (False, True):
    ang = -45 if on else 45
    rot = {"origin": [8, 8.5, 12], "axis": "x", "angle": ang}
    w(f'{M}/power_lever{"_on" if on else ""}.json', {"textures": {"plate": O + "block/lever_plate", "m": O + "block/outpost_metal", "g": O + "block/lever_grip", "particle": O + "block/outpost_metal"},
        "elements": [el([3, 0, 14], [13, 16, 16], "#plate"), el([5, 6, 11], [11, 11, 14], "#m"),
                     el([7, 7.5, 3], [9, 9.5, 12], "#m", rot), el([5.5, 7, 1], [10.5, 10, 5], "#g", rot)]})
w(f'{B}/power_lever.json', {"variants": {f"facing={d},powered={str(p).lower()}": {"model": O + "block/power_lever" + ("_on" if p else ""), "y": YR[d]}
                                          for d in YR for p in (False, True)}})
w(f'{I}/power_lever.json', {"parent": O + "block/power_lever"})
# emergency button
for pr in (False, True):
    top = 14.5 if pr else 16
    w(f'{M}/emergency_button{"_pressed" if pr else ""}.json', {"textures": {"m": O + "block/outpost_metal", "s": O + "block/hazard_stripes", "r": O + "block/emergency_red", "particle": O + "block/emergency_red"},
        "elements": [el([1, 0, 1], [15, 2, 15], "#m"), el([5, 2, 5], [11, 9, 11], "#m"), el([2, 9, 2], [14, 13, 14], "#s"), el([4, 13, 4], [12, top, 12], "#r")]})
w(f'{B}/emergency_button.json', {"variants": {"powered=false": {"model": O + "block/emergency_button"}, "powered=true": {"model": O + "block/emergency_button_pressed"}}})
w(f'{I}/emergency_button.json', {"parent": O + "block/emergency_button"})
# console
w(f'{M}/control_console.json', {"textures": {"m": O + "block/outpost_metal", "f": O + "block/console_front", "p": O + "block/console_panel", "particle": O + "block/outpost_metal"},
    "elements": [{"from": [0, 0, 0], "to": [16, 10, 16], "faces": {"north": {"texture": "#f"}, "south": {"texture": "#m"}, "east": {"texture": "#m"}, "west": {"texture": "#m"}, "up": {"texture": "#m"}, "down": {"texture": "#m"}}},
                 {"from": [0, 10, 1], "to": [16, 12, 15], "rotation": {"origin": [8, 10, 8], "axis": "x", "angle": -22.5},
                  "faces": {"up": {"texture": "#p"}, "north": {"texture": "#m"}, "south": {"texture": "#m"}, "east": {"texture": "#m"}, "west": {"texture": "#m"}, "down": {"texture": "#m"}}}]})
w(f'{B}/control_console.json', {"variants": {f"facing={d}": {"model": O + "block/control_console", "y": YR[d]} for d in YR}})
w(f'{I}/control_console.json', {"parent": O + "block/control_console"})
# monitor: same body as the hazard monitor
hm = json.load(open(f'{B}/hazard_monitor.json'))
w(f'{B}/outpost_monitor.json', {"variants": {k.replace(",broken=false", "").replace("broken=false,", ""): v for k, v in hm["variants"].items() if "broken=true" not in k}})
w(f'{I}/outpost_monitor.json', {"parent": O + "block/hazard_monitor"})
# lamps
for lit in (False, True):
    s = "_on" if lit else ""
    w(f'{M}/warning_light{s}.json', {"textures": {"m": O + "block/outpost_metal", "r": O + "block/warning_light" + s, "particle": O + "block/warning_light" + s},
        "elements": [el([5, 15, 5], [11, 16, 11], "#m"), el([7, 13, 7], [9, 15, 9], "#m"), el([5.5, 9, 5.5], [10.5, 13, 10.5], "#r")]})
    w(f'{M}/facility_light{s}.json', {"textures": {"m": O + "block/outpost_metal_light", "l": O + "block/facility_light" + s, "particle": O + "block/facility_light" + s},
        "elements": [{"from": [1, 14, 3], "to": [15, 16, 13], "faces": {"down": {"texture": "#l"}, "up": {"texture": "#m"}, "north": {"texture": "#m"}, "south": {"texture": "#m"}, "east": {"texture": "#m"}, "west": {"texture": "#m"}}}]})
for b_ in ('warning_light', 'facility_light'):
    w(f'{B}/{b_}.json', {"variants": {"lit=false": {"model": O + "block/" + b_}, "lit=true": {"model": O + "block/" + b_ + "_on"}}})
    w(f'{I}/{b_}.json', {"parent": O + "block/" + b_ + "_on"})
# gateway
w(f'{M}/dimensional_gateway.json', {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": {"p": O + "block/dimensional_gateway", "particle": O + "block/dimensional_gateway"},
    "elements": [{"from": [0, 0, 6], "to": [16, 16, 10], "faces": {"north": {"texture": "#p"}, "south": {"texture": "#p"}}}]})
w(f'{B}/dimensional_gateway.json', {"variants": {"axis=x": {"model": O + "block/dimensional_gateway"}, "axis=z": {"model": O + "block/dimensional_gateway", "y": 90}}})
# loot
items = ['outpost_wall', 'outpost_floor', 'outpost_ceiling', 'hazard_floor', 'power_lever', 'emergency_button', 'outpost_monitor', 'control_console', 'warning_light', 'facility_light', 'gateway_frame']
for it in items:
    w(f'{D}/loot_tables/blocks/{it}.json', {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + it}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
p = f'{R}/data/minecraft/tags/blocks/mineable/pickaxe.json'
t = json.load(open(p)) if os.path.exists(p) else {"replace": False, "values": []}
for it in items:
    if O + it not in t["values"]: t["values"].append(O + it)
w(p, t)
def opt(item, ch, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": item}
    if hi > 1: e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": ch}], "entries": [e]}
w(f'{D}/loot_tables/chests/research_outpost.json', {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [{"type": "minecraft:loot_table", "name": O + "tiers/uncommon"}]},
    {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [{"type": "minecraft:loot_table", "name": O + "tiers/common"}]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.15}], "entries": [{"type": "minecraft:loot_table", "name": O + "tiers/rare"}]},
    opt(O + "research_file", 0.85, 1, 3), opt(O + "electronic_component", 0.9, 2, 5), opt(O + "portable_battery", 0.4), opt(O + "gas_filter", 0.3, 1, 2),
    opt(O + "glitched_ingot", 0.3, 1, 3), opt(O + "anomaly_detector", 0.12), opt(O + "night_vision_goggles", 0.06), opt(O + "almond_water", 0.5, 1, 3),
    opt("minecraft:diamond", 0.25, 1, 2), opt("minecraft:redstone", 0.6, 4, 12), opt("minecraft:copper_ingot", 0.5, 3, 8)]})
# lang + sounds
lp = f'{A}/lang/en_us.json'; L = json.load(open(lp))
L.update({"block.office_rooms.outpost_wall": "Lab Wall Panel", "block.office_rooms.outpost_floor": "Lab Floor Tiles", "block.office_rooms.outpost_ceiling": "Lab Ceiling Tiles", "block.office_rooms.hazard_floor": "Hazard Floor",
          "block.office_rooms.power_lever": "Main Power Lever", "block.office_rooms.emergency_button": "Emergency Activation Button", "block.office_rooms.outpost_monitor": "Gateway Monitor",
          "block.office_rooms.control_console": "Control Console", "block.office_rooms.warning_light": "Warning Light", "block.office_rooms.facility_light": "Facility Light",
          "block.office_rooms.gateway_frame": "Gateway Frame", "block.office_rooms.dimensional_gateway": "Dimensional Gateway",
          "subtitles.office_rooms.block.outpost.lever": "Heavy lever thrown", "subtitles.office_rooms.block.outpost.power_on": "Facility powers up",
          "subtitles.office_rooms.block.outpost.power_off": "Facility powers down", "subtitles.office_rooms.block.outpost.alarm": "Alarm blares",
          "subtitles.office_rooms.block.outpost.beep": "Console beeps", "subtitles.office_rooms.block.outpost.spark": "Electricity crackles",
          "subtitles.office_rooms.block.outpost.button": "Emergency button pressed", "subtitles.office_rooms.block.gateway.charge": "Gateway charges",
          "subtitles.office_rooms.block.gateway.open": "Gateway tears open", "subtitles.office_rooms.block.gateway.hum": "Gateway hums"})
w(lp, L)
sp = f'{A}/sounds.json'; S = json.load(open(sp))
for k, files, dist in (("block.outpost.lever", ["block/outpost/lever"], 24), ("block.outpost.power_on", ["block/outpost/power_on"], 48), ("block.outpost.power_off", ["block/outpost/power_off"], 48),
                       ("block.outpost.alarm", ["block/outpost/alarm"], 64), ("block.outpost.beep", ["block/outpost/beep1", "block/outpost/beep2"], 16),
                       ("block.outpost.spark", ["block/outpost/spark1", "block/outpost/spark2", "block/outpost/spark3"], 24), ("block.outpost.button", ["block/outpost/button"], 16),
                       ("block.gateway.charge", ["block/gateway/charge"], 64), ("block.gateway.open", ["block/gateway/open"], 96), ("block.gateway.hum", ["block/gateway/hum"], 24)):
    S[k] = {"sounds": [{"name": O + f_, "attenuation_distance": dist} for f_ in files], "subtitle": "subtitles.office_rooms." + k}
w(sp, S)
print("ok")
