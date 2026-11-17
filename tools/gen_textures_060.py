#!/usr/bin/env python3
"""0.6.0: original textures for The Hound, The Smiler and The Colony (procedural, no foreign assets)."""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "office_rooms", "textures", "entity")
random.seed(60)


def clamp(v):
    return max(0, min(255, int(v)))


def box_regions(u, v, w, h, d):
    """Rechtecke (x0, y0, x1, y1) der sechs Flächen eines Würfels im Minecraft-UV-Layout."""
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


def fill(px, rect, fn):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = fn(x, y)


def fill_box(px, u, v, w, h, d, fn):
    for r in box_regions(u, v, w, h, d).values():
        fill(px, r, fn)


# --- The Hound ----------------------------------------------------------------------------------

def hound():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()

    def fur(x, y):
        c = 46 + random.randint(-10, 10)
        if random.random() < 0.08:
            return (clamp(c + 40), clamp(c + 18), clamp(c + 10), 255)   # kahle Stelle
        if random.random() < 0.05:
            return (clamp(110 + random.randint(-20, 10)), 12, 10, 255)  # Blut
        return (clamp(c + 6), clamp(c), clamp(c - 4), 255)

    def skin(x, y):
        c = random.randint(-8, 8)
        return (clamp(168 + c), clamp(146 + c), clamp(128 + c), 255)

    def hair(x, y):
        c = 30 + random.randint(-8, 8)
        return (clamp(c + 4), clamp(c), clamp(c - 2), 255)

    fill_box(px, 0, 20, 6, 6, 11, fur)    # Rumpf
    fill_box(px, 34, 20, 8, 7, 5, fur)    # Mähne
    fill_box(px, 0, 40, 2, 9, 2, fur)     # Schwanz
    fill_box(px, 10, 40, 2, 9, 2, fur)    # Vorderläufe
    fill_box(px, 18, 40, 2, 9, 2, fur)    # Hinterläufe
    # Kopf: Haut, Haare oben/hinten/an den oberen Seitenrändern
    reg = box_regions(0, 0, 8, 8, 8)
    for name, r in reg.items():
        fill(px, r, skin)
    fill(px, reg["top"], hair)
    fill(px, reg["back"], hair)
    for side in ("left", "right", "front"):
        x0, y0, x1, y1 = reg[side]
        fill(px, (x0, y0, x1, y0 + (2 if side != "front" else 1)), hair)
    fx, fy = reg["front"][0], reg["front"][1]
    # leere Augenhöhlen mit rotem Punkt
    for ex in (1, 5):
        for dx in range(2):
            px[fx + ex + dx, fy + 3] = (16, 6, 6, 255)
            px[fx + ex + dx, fy + 4] = (22, 8, 8, 255)
        px[fx + ex + 1, fy + 3] = (150, 20, 15, 255)
    px[fx + 4, fy + 5] = (120, 90, 80, 255)  # Nase
    # aufgerissener Mund
    for mx in range(2, 6):
        px[fx + mx, fy + 6] = (30, 6, 6, 255)
        px[fx + mx, fy + 7] = (60, 8, 8, 255)
    px[fx + 2, fy + 6] = (200, 190, 170, 255)
    px[fx + 5, fy + 6] = (200, 190, 170, 255)
    # Blut: Spuren aus Augen und Mund, Spritzer auf Wangen und Seiten
    for (sx, sy, n) in ((1, 5, 3), (6, 5, 3), (3, 7, 1), (4, 7, 1)):
        for k in range(n):
            if sy + k < 8:
                px[fx + sx, fy + sy + k] = (clamp(120 + random.randint(-20, 20)), 10, 8, 255)
    for name in ("left", "right", "bottom", "front"):
        x0, y0, x1, y1 = reg[name]
        for _ in range(7 if name != "front" else 4):
            x = random.randint(x0, x1 - 1)
            y = random.randint(max(y0, y0 + 3), y1 - 1)
            px[x, y] = (clamp(130 + random.randint(-30, 20)), 12, 10, 255)
    img.save(os.path.join(OUT, "hound.png"))


# --- The Smiler ---------------------------------------------------------------------------------

def smiler():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    px = img.load()

    def shadow(alpha_lo, alpha_hi):
        def f(x, y):
            c = random.randint(0, 9)
            return (c, c, c + 1, random.randint(alpha_lo, alpha_hi))
        return f

    fill_box(px, 0, 0, 10, 10, 9, shadow(235, 255))     # Kopf (Kern, fast deckend)
    fill_box(px, 0, 20, 10, 20, 6, shadow(200, 245))    # Körper
    fill_box(px, 40, 20, 12, 12, 8, shadow(120, 200))   # Schleier (durchscheinend)
    fill_box(px, 0, 48, 3, 9, 1, shadow(60, 170))       # Fetzen
    img.save(os.path.join(OUT, "smiler.png"))
    # Leuchtebene: Lächeln und Augen auf der Vorderseite des Kopfes
    glow = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    gp = glow.load()
    fx, fy = box_regions(0, 0, 10, 10, 9)["front"][:2]
    for ex in (2, 6):
        gp[fx + ex, fy + 3] = (235, 235, 220, 255)
        gp[fx + ex + 1, fy + 3] = (200, 200, 185, 255)
    # breiter, gezackter Halbmond aus Zähnen
    smile = {0: (5, 6), 1: (5, 7), 2: (6, 7), 3: (6, 8), 4: (6, 8), 5: (6, 8), 6: (6, 8), 7: (6, 7), 8: (5, 7), 9: (5, 6)}
    for x, (y0, y1) in smile.items():
        for y in range(y0, y1 + 1):
            tooth = (x + y) % 2 == 0 or y == y0
            gp[fx + x, fy + y] = (250, 250, 236, 255) if tooth else (110, 110, 100, 255)
    glow.save(os.path.join(OUT, "smiler_glow.png"))


# --- The Colony ---------------------------------------------------------------------------------

def colony():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    px = img.load()
    for y in range(64):
        for x in range(128):
            c = random.randint(-12, 12)
            r, g, b = 186 + c, 182 + c, 150 + c
            if random.random() < 0.06:
                r, g, b = 140 + c, 104 + c, 82 + c   # Fäulnis
            if random.random() < 0.04:
                r, g, b = r + 30, g + 30, b + 26     # nasser Glanz
            px[x, y] = (clamp(r), clamp(g), clamp(b), 255)
    # dunkle Adern als Zufallspfade
    for _ in range(60):
        x, y = random.randint(0, 127), random.randint(0, 63)
        for _ in range(random.randint(4, 14)):
            px[x % 128, y % 64] = (clamp(104 + random.randint(-10, 10)), clamp(98 + random.randint(-10, 10)), 74, 255)
            x += random.choice((-1, 0, 1))
            y += random.choice((-1, 0, 1))
    img.save(os.path.join(OUT, "colony.png"))
    glow = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    gp = glow.load()
    regions = [box_regions(0, 0, 14, 6, 16), box_regions(0, 24, 6, 5, 6), box_regions(24, 24, 5, 4, 5),
               box_regions(44, 24, 4, 6, 4)]
    for reg in regions:
        for name, (x0, y0, x1, y1) in reg.items():
            if name == "bottom":
                continue
            n = max(1, (x1 - x0) * (y1 - y0) // 14)
            for _ in range(n):
                x = random.randint(x0, x1 - 1)
                y = random.randint(y0, y1 - 1)
                gp[x, y] = (236, 228, 120, 255)
                if random.random() < 0.3 and x + 1 < x1:
                    gp[x + 1, y] = (190, 184, 90, 255)
    glow.save(os.path.join(OUT, "colony_glow.png"))


if __name__ == "__main__":
    hound()
    smiler()
    colony()
    print("0.6.0 textures written")
