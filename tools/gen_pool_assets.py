#!/usr/bin/env python3
"""Poolrooms (0.6.0): textures, blockstates, models and loot tables for the pool tile blocks.

Original textures (cream ceramic tiles), generated procedurally. Run from the project root.
"""
import json
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(ROOT, "assets", "office_rooms")
D = os.path.join(ROOT, "data")


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, "w"), indent=1)


def tile_texture(name, base, grout, size, seed):
    random.seed(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    shades = {}
    for y in range(16):
        for x in range(16):
            if x % size == 0 or y % size == 0:
                g = random.randint(-3, 3)
                px[x, y] = tuple(max(0, min(255, c + g)) for c in grout) + (255,)
                continue
            key = (x // size, y // size)
            if key not in shades:
                shades[key] = random.randint(-5, 5)
            s = shades[key] + random.randint(-2, 2)
            c = [max(0, min(255, v + s)) for v in base]
            # leichter Glanz oben links in jeder Fliese
            if x % size == 1 and y % size == 1:
                c = [min(255, v + 12) for v in c]
            px[x, y] = tuple(c) + (255,)
    img.save(os.path.join(A, "textures", "block", name + ".png"))


def stairs_states(block, model):
    """Same rotations as vanilla stair blockstates."""
    variants = {}
    rot = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing in ("east", "north", "south", "west"):
        base = rot[facing]
        for half in ("bottom", "top"):
            for shape in ("inner_left", "inner_right", "outer_left", "outer_right", "straight"):
                m = model + ("_inner" if shape.startswith("inner") else "_outer" if shape.startswith("outer") else "")
                left = shape.endswith("_left")
                right = shape.endswith("_right")
                if half == "bottom":
                    x = 0
                    y = (base - 90) % 360 if left else base
                else:
                    x = 180
                    y = (base + 90) % 360 if right else base
                v = {"model": m}
                if x:
                    v["x"] = x
                if y:
                    v["y"] = y
                if x or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    return {"variants": variants}


def main():
    tile_texture("pool_tile", (236, 224, 186), (205, 192, 152), 4, 11)
    tile_texture("pool_floor_tile", (240, 234, 212), (196, 190, 168), 4, 12)
    for b in ("pool_tile", "pool_floor_tile"):
        w(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"office_rooms:block/{b}"}}})
        w(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"office_rooms:block/{b}"}})
        w(f"{A}/models/item/{b}.json", {"parent": f"office_rooms:block/{b}"})
        w(f"{D}/office_rooms/loot_tables/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"office_rooms:{b}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    t = {"bottom": "office_rooms:block/pool_tile", "top": "office_rooms:block/pool_tile", "side": "office_rooms:block/pool_tile"}
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(f"{A}/models/block/pool_tile_stairs{suffix}.json", {"parent": f"minecraft:block/{parent}", "textures": t})
    w(f"{A}/blockstates/pool_tile_stairs.json", stairs_states("pool_tile_stairs", "office_rooms:block/pool_tile_stairs"))
    w(f"{A}/models/item/pool_tile_stairs.json", {"parent": "office_rooms:block/pool_tile_stairs"})
    w(f"{A}/models/block/pool_tile_slab.json", {"parent": "minecraft:block/slab", "textures": t})
    w(f"{A}/models/block/pool_tile_slab_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    w(f"{A}/blockstates/pool_tile_slab.json", {"variants": {
        "type=bottom": {"model": "office_rooms:block/pool_tile_slab"},
        "type=top": {"model": "office_rooms:block/pool_tile_slab_top"},
        "type=double": {"model": "office_rooms:block/pool_tile"}}})
    w(f"{A}/models/item/pool_tile_slab.json", {"parent": "office_rooms:block/pool_tile_slab"})
    w(f"{D}/office_rooms/loot_tables/blocks/pool_tile_stairs.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "office_rooms:pool_tile_stairs"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    w(f"{D}/office_rooms/loot_tables/blocks/pool_tile_slab.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "office_rooms:pool_tile_slab", "functions": [
            {"function": "minecraft:set_count", "count": 2, "conditions": [{"condition": "minecraft:block_state_property",
                                                                            "block": "office_rooms:pool_tile_slab", "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}]})
    for tag, vals in (("mineable/pickaxe", ["pool_tile", "pool_floor_tile", "pool_tile_stairs", "pool_tile_slab"]),
                      ("stairs", ["pool_tile_stairs"]), ("slabs", ["pool_tile_slab"])):
        path = f"{D}/minecraft/tags/blocks/{tag}.json"
        cur = json.load(open(path)) if os.path.exists(path) else {"replace": False, "values": []}
        for v in vals:
            if "office_rooms:" + v not in cur["values"]:
                cur["values"].append("office_rooms:" + v)
        w(path, cur)
    w(f"{D}/minecraft/tags/items/stairs.json", {"replace": False, "values": ["office_rooms:pool_tile_stairs"]})
    w(f"{D}/minecraft/tags/items/slabs.json", {"replace": False, "values": ["office_rooms:pool_tile_slab"]})


if __name__ == "__main__":
    main()
    print("pool assets written")
