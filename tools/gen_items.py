#!/usr/bin/env python3
"""Erzeugt die eigenen Item- und Rüstungstexturen von Office Rooms (Phase 4).

Alle Motive sind hier im Code gezeichnet (Linien und Flächen, automatische Umrandung) und mit einem
dunkelblauen „Glitch“-Effekt versehen: einzelne Pixel in Cyan/Magenta, eine um 1 Pixel versetzte Zeile.
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "office_rooms", "textures")
ITEM = os.path.join(ROOT, "item")
ARMOR = os.path.join(ROOT, "models", "armor")
os.makedirs(ITEM, exist_ok=True)
os.makedirs(ARMOR, exist_ok=True)

C = {
    "D": (8, 14, 46, 255),      # Umriss
    "B": (24, 52, 140, 255),    # Blau
    "L": (66, 110, 220, 255),   # Hellblau (Kante)
    "H": (140, 175, 255, 255),  # Glanz
    "R": (18, 30, 80, 255),     # Stab
    "C": (110, 240, 255, 255),  # Glitch Cyan
    "M": (190, 90, 255, 255),   # Glitch Magenta
    "G": (150, 150, 155, 255),  # Eisengriff
    "g": (95, 95, 100, 255),
}


def line(a, b):
    (x0, y0), (x1, y1) = a, b
    n = max(abs(x1 - x0), abs(y1 - y0))
    return [(round(x0 + (x1 - x0) * t / n), round(y0 + (y1 - y0) * t / n)) for t in range(n + 1)] if n else [a]


class Sprite:
    def __init__(self, seed):
        self.px = {}
        self.rng = random.Random(seed)

    def put(self, pts, c):
        for p in pts:
            if 0 <= p[0] < 16 and 0 <= p[1] < 16:
                self.px[p] = c

    def outline(self):
        out = {}
        for (x, y) in list(self.px):
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (x + dx, y + dy)
                if q not in self.px and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    out[q] = "D"
        self.px.update(out)

    def glitch(self, n=3, shift_row=None, colors=("B", "L")):
        cand = [p for p, c in self.px.items() if c in colors]
        for p in self.rng.sample(cand, min(n, len(cand))):
            self.px[p] = self.rng.choice("CM")
        if shift_row is not None:
            row = {p: c for p, c in self.px.items() if p[1] == shift_row}
            for p in row:
                del self.px[p]
            for (x, y), c in row.items():
                if x + 1 < 16:
                    self.px[(x + 1, y)] = c
            # Farbsaum der Verschiebung
            xs = [x for (x, y) in row]
            if xs:
                self.px[(min(xs), shift_row)] = "C"

    def save(self, name):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for (x, y), c in self.px.items():
            img.putpixel((x, y), C[c])
        img.save(os.path.join(ITEM, name + ".png"))


def handle(s, a, b, iron=True):
    pts = line(a, b)
    pts2 = line((a[0] + 1, a[1]), (b[0] + 1, b[1]))
    half = len(pts) // 2
    s.put(pts[:half] + pts2[:half], "G")
    s.put([(x, y) for (x, y) in pts[:half]], "g")
    s.put(pts[half:] + pts2[half:], "R")


# Schwert: Klinge von oben rechts nach unten links, Parierstange, Stab als Griff
s = Sprite(1)
s.put(line((13, 2), (5, 10)), "L")
s.put(line((12, 2), (4, 10)), "B")
s.put(line((13, 3), (6, 10)), "B")
s.put([(14, 1), (13, 1)], "H")
s.put(line((2, 8), (6, 12)), "R")
s.put(line((4, 11), (2, 13)), "R")
s.put([(1, 14)], "C")
s.outline()
s.glitch(3, shift_row=6)
s.save("multidimensional_sword")

# Spitzhacke
s = Sprite(2)
handle(s, (2, 13), (9, 6))
head = [(4, 3), (5, 2), (6, 2), (7, 1), (8, 1), (9, 1), (10, 2), (11, 3), (12, 4), (13, 5), (13, 6), (14, 7), (14, 8), (13, 9)]
s.put(head, "B")
s.put([(6, 3), (7, 2), (8, 2), (9, 2), (10, 3), (11, 4), (12, 5), (12, 6), (13, 7)], "L")
s.put([(3, 4), (13, 10)], "B")
s.outline()
s.glitch(3, shift_row=3)
s.save("multidimensional_pickaxe")

# Axt
s = Sprite(3)
handle(s, (2, 13), (10, 5))
s.put([(8, 2), (9, 1), (10, 1), (11, 2), (12, 3), (13, 4), (13, 5), (12, 6), (11, 6)], "B")
s.put([(9, 2), (10, 2), (11, 3), (12, 4), (12, 5), (10, 3), (11, 4), (11, 5), (10, 4), (9, 3)], "L")
s.put([(8, 3), (8, 4), (9, 4)], "B")
s.outline()
s.glitch(2, shift_row=4)
s.save("multidimensional_axe")

# Schaufel
s = Sprite(4)
handle(s, (2, 13), (9, 6))
s.put([(10, 2), (11, 2), (12, 3), (13, 4), (13, 5), (12, 6), (11, 6), (10, 5), (9, 4), (9, 3)], "B")
s.put([(10, 3), (11, 3), (12, 4), (11, 4), (10, 4), (12, 5), (11, 5)], "L")
s.outline()
s.glitch(2, shift_row=4)
s.save("multidimensional_shovel")

# Hacke
s = Sprite(5)
handle(s, (2, 13), (10, 5))
s.put([(7, 3), (8, 2), (9, 2), (10, 2), (11, 3), (12, 4)], "B")
s.put([(8, 3), (9, 3), (10, 3), (11, 4)], "L")
s.outline()
s.glitch(2, shift_row=3)
s.save("multidimensional_hoe")

# Stab
s = Sprite(6)
s.put(line((3, 12), (12, 3)), "R")
s.put(line((4, 12), (12, 4)), "B")
s.put([(12, 2), (13, 3), (13, 2), (2, 13), (3, 13)], "C")
s.put([(7, 8), (9, 6)], "M")
s.outline()
s.glitch(1, shift_row=7, colors=("B",))
s.save("multidimensional_rod")

# Barren
s = Sprite(7)
for y in range(6, 11):
    x0 = 3 + (10 - y) // 2
    s.put([(x, y) for x in range(x0, 13 - (y - 6) // 3)], "B")
s.put([(x, 6) for x in range(5, 12)], "L")
s.put([(6, 7), (7, 7)], "H")
s.outline()
s.glitch(4, shift_row=8)
s.save("glitched_ingot")

# Rüstungs-Icons
s = Sprite(8)  # Helm
s.put([(x, y) for y in range(4, 10) for x in range(4, 12) if not (y >= 7 and 6 <= x <= 9)], "B")
s.put([(x, 4) for x in range(5, 11)], "L")
s.outline()
s.glitch(3, shift_row=6)
s.save("multidimensional_helmet")

s = Sprite(9)  # Brustpanzer
pts = [(x, y) for y in range(3, 14) for x in range(2, 14)
       if not (y <= 5 and 6 <= x <= 9) and not (y >= 6 and (x <= 3 or x >= 12))]
s.put(pts, "B")
s.put([(x, 3) for x in (2, 3, 4, 5, 10, 11, 12, 13)], "L")
s.outline()
s.glitch(4, shift_row=9)
s.save("multidimensional_chestplate")

s = Sprite(10)  # Hose
pts = [(x, y) for y in range(2, 15) for x in range(4, 12) if not (y >= 6 and 7 <= x <= 8)]
s.put(pts, "B")
s.put([(x, 2) for x in range(4, 12)], "L")
s.outline()
s.glitch(3, shift_row=10)
s.save("multidimensional_leggings")

s = Sprite(11)  # Stiefel
pts = [(x, y) for y in range(7, 13) for x in list(range(2, 6)) + list(range(10, 14))]
pts += [(1, 12), (9, 12), (1, 11), (9, 11)]
s.put(pts, "B")
s.put([(2, 7), (3, 7), (10, 7), (11, 7)], "L")
s.outline()
s.glitch(2, shift_row=10)
s.save("multidimensional_boots")

# Mandelwasser: Flasche mit milchig-heller Flüssigkeit
s = Sprite(12)
glass = (200, 220, 230, 255)
C["W"] = glass
C["A"] = (236, 226, 205, 255)
C["a"] = (214, 200, 172, 255)
C["K"] = (150, 120, 90, 255)
s.put([(7, 2), (8, 2)], "K")
s.put([(7, 3), (8, 3), (7, 4), (8, 4)], "W")
body = [(x, y) for y in range(5, 14) for x in range(4, 12) if not (y == 5 and x in (4, 11)) and not (y == 13 and x in (4, 11))]
s.put(body, "W")
s.put([(x, y) for y in range(8, 13) for x in range(5, 11)], "A")
s.put([(5, 12), (6, 12), (9, 11), (10, 12)], "a")
s.put([(6, 6), (6, 7)], "W")
s.outline()
s.save("almond_water")

# Rüstungsschichten 64×32 (Vanilla-Layout): dunkelblau, Scanlines, Glitchpixel
rng = random.Random(42)


def armor_layer(name, regions):
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for (x0, y0, x1, y1) in regions:
        for y in range(y0, y1):
            shift = 1 if y % 7 == 3 else 0
            for x in range(x0, x1):
                base = (20, 40, 120) if (y % 2 == 0) else (14, 30, 96)
                if (x + shift) % 4 == 0:
                    base = (32, 62, 170)
                c = base + (255,)
                r = rng.random()
                if r < 0.025:
                    c = C["C"]
                elif r < 0.04:
                    c = C["M"]
                img.putpixel((x, y), c)
    img.save(os.path.join(ARMOR, name + ".png"))


# Kopf (0,0)-(32,16), Körper (16,16)-(40,32), Arme (40,16)-(56,32), Beine (0,16)-(16,32)
# armor_layer("multidimensional_layer_1", [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32)])  # 0.7.1: ersetzt durch gen_armor_070.py (Rüstung mit Form)
# armor_layer("multidimensional_layer_2", [(16, 16, 40, 32), (0, 16, 16, 32)])  # 0.7.1: ersetzt durch gen_armor_070.py (Rüstung mit Form)
print("ok")
