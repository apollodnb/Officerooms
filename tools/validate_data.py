#!/usr/bin/env python3
"""
Prueft die Datapack-Dateien von Office Rooms gegen die Regeln von Minecraft 1.20.1.

Die Regeln stammen aus den Codecs von Minecraft 1.20.1 (DimensionType, FlatLevelGeneratorSettings,
Biome). Das Skript ersetzt keinen Starttest im Spiel, faengt aber Tipp- und Grenzwertfehler frueh ab.

Aufruf (aus dem Projektverzeichnis):  python3 tools/validate_data.py
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "src" / "main" / "resources" / "data" / "office_rooms"
errors = []


def check(cond, msg):
    print(("  OK   " if cond else "  FEHLER ") + msg)
    if not cond:
        errors.append(msg)


def load(rel):
    path = DATA / rel
    with open(path, encoding="utf-8") as f:
        return json.load(f)


print("dimension_type/office_rooms.json")
dt = load("dimension_type/office_rooms.json")
required = ["ultrawarm", "natural", "coordinate_scale", "has_skylight", "has_ceiling", "ambient_light",
            "monster_spawn_light_level", "monster_spawn_block_light_limit", "piglin_safe", "bed_works",
            "respawn_anchor_works", "has_raids", "min_y", "height", "logical_height", "infiniburn", "effects"]
for k in required:
    check(k in dt, f"Pflichtfeld '{k}' vorhanden")
min_y, height, logical = dt["min_y"], dt["height"], dt["logical_height"]
# Grenzen aus DimensionType (1.20.1): BITS_FOR_Y=12, Y_SIZE=4064, MAX_Y=2031, MIN_Y=-2032
check(min_y % 16 == 0, f"min_y {min_y} ist Vielfaches von 16")
check(height % 16 == 0, f"height {height} ist Vielfaches von 16")
check(-2032 <= min_y <= 2031, f"min_y {min_y} liegt in [-2032, 2031]")
check(16 <= height <= 4064, f"height {height} liegt in [16, 4064]")
check(min_y + height <= 2032, f"min_y + height = {min_y + height} <= 2032")
check(0 <= logical <= height, f"logical_height {logical} <= height {height}")
check(0.00001 <= dt["coordinate_scale"] <= 30000000, "coordinate_scale im gueltigen Bereich")
check(0 <= dt["monster_spawn_block_light_limit"] <= 15, "monster_spawn_block_light_limit in [0, 15]")
check(dt["infiniburn"].startswith("#"), "infiniburn ist ein Block-Tag")
print(f"  INFO Bebaubar von Y {min_y} bis Y {min_y + height - 1} ({height} Bloecke)")

print("dimension/office_rooms.json")
dim = load("dimension/office_rooms.json")
check(dim["type"] == "office_rooms:office_rooms", "verweist auf Dimensionstyp office_rooms:office_rooms")
gen = dim["generator"]
check(gen["type"] == "office_rooms:office_rooms", "Generator office_rooms:office_rooms (eigener Buero-Generator)")
bs = gen["biome_source"]
check(bs["type"] == "minecraft:fixed", "Biomquelle minecraft:fixed")
biome_id = bs["biome"]
ns, path = biome_id.split(":")
check(ns == "office_rooms", f"Biom {biome_id} gehoert zum Mod")
st = gen.get("settings", {})
grid = st.get("grid", {})
unit = grid.get("unit_size", 4)
base = grid.get("base_height", 4)
ru = grid.get("region_units", 40)
check(4 <= unit <= 8, f"grid.unit_size {unit} in [4, 8]")
check(3 <= base <= 7, f"grid.base_height {base} in [3, 7]")
check(16 <= ru <= 64, f"grid.region_units {ru} in [16, 64]")
pitch = base + 1
stair_cells = max(2, (pitch + 4 + unit - 1) // unit)
check(stair_cells * unit >= pitch + 4, f"Treppenhaus passt: {stair_cells} Zellen x {unit} >= {pitch + 4}")
max_storeys = (height - 2) // pitch
want = grid.get("storeys", 0)
storeys = max_storeys if want <= 0 else min(want, max_storeys)
print(f"  INFO Geschossraster {pitch} Bloecke, {storeys} Geschosse (max {max_storeys}), Region {unit * ru} Bloecke")
bands = st.get("room_heights", [])
tw = sum(b["weight"] for b in bands)
if tw:
    for b in bands:
        check(3 <= b["min"] <= b["max"], f"Hoehenband {b['min']}-{b['max']} gueltig")
        print(f"  INFO Raumhoehe {b['min']}-{b['max']}: {100.0 * b['weight'] / tw:.1f} %")
for c in st.get("atria", {}).get("classes", []):
    check(c["max_w"] <= ru - 2 and c["max_d"] <= ru - 2, f"Atrium {c['name']} passt mit Rand in die Region")
    check(c["min_h"] <= c["max_h"], f"Atrium {c['name']} Hoehenbereich gueltig")
    max_h = (storeys - 3) * pitch - 1
    if c["max_h"] > max_h:
        print(f"  INFO Atrium {c['name']}: max_h {c['max_h']} wird auf {max_h} begrenzt (Dimensionshoehe)")

print(f"worldgen/biome/{path}.json")
b = load(f"worldgen/biome/{path}.json")
for k in ["has_precipitation", "temperature", "downfall", "effects", "spawners", "spawn_costs", "carvers", "features"]:
    check(k in b, f"Pflichtfeld '{k}' vorhanden")
for k in ["fog_color", "sky_color", "water_color", "water_fog_color"]:
    check(k in b["effects"] and 0 <= b["effects"][k] <= 0xFFFFFF, f"effects.{k} gueltige RGB-Farbe")
check(b["spawners"] == {} and b["spawn_costs"] == {}, "keine natuerlichen Mob-Spawns")
check(b["features"] == [] and b["carvers"] == {}, "keine Features und Carver")

print("functions")
for fn in ["enter", "leave"]:
    check((DATA / "functions" / f"{fn}.mcfunction").is_file(), f"functions/{fn}.mcfunction vorhanden (1.20.1-Pfad 'functions')")

print()
if errors:
    print(f"{len(errors)} Fehler gefunden.")
    sys.exit(1)
print("Alle Pruefungen bestanden.")
