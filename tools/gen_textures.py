#!/usr/bin/env python3
"""Erzeugt die eigene Wolfspider-Textur (64x32, fast vollständig schwarz)."""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "office_rooms",
                   "textures", "entity", "wolfspider.png")
random.seed(7)
img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
px = img.load()


def fill_box(u, v, w, h, d, base=14, var=6):
    """Füllt die UV-Fläche eines Würfels (Minecraft-Layout) mit fast schwarzem, leicht strukturiertem Fell."""
    for y in range(v, v + d + h):
        for x in range(u, u + 2 * (d + w)):
            if y < v + d and not (u + d <= x < u + d + 2 * w):
                continue
            c = base + random.randint(-var, var)
            if (x + y) % 7 == 0:
                c += 4  # feine Haarstruktur
            px[x, y] = (c, c, c + 1, 255)


fill_box(0, 0, 6, 6, 6)        # Brustteil
fill_box(0, 12, 10, 8, 12)     # Hinterleib
fill_box(24, 0, 16, 2, 2, 10)  # Beine
fill_box(44, 12, 6, 6, 4, 16)  # Wolfskopf
fill_box(44, 22, 3, 3, 4, 12)  # Schnauze
fill_box(24, 4, 2, 2, 1, 18)   # Ohr links
fill_box(30, 4, 2, 2, 1, 18)   # Ohr rechts
# Augen auf der Kopfvorderseite (u+d, v+d) = (48, 16): kaum heller als Schwarz
for ex in (49, 52):
    px[ex, 18] = (38, 38, 40, 255)
img.save(OUT)
print("ok", OUT)


# ---------------------------------------------------------------------------------------------
# Liminal-World-Update: The Gaunt (64x64, verkohlt dunkel mit roten Adern + Leuchtebene) und
# The Observer (64x64, fast schwarz).
# ---------------------------------------------------------------------------------------------
def gaunt_textures():
    base = os.path.join(os.path.dirname(OUT), "gaunt.png")
    glow = os.path.join(os.path.dirname(OUT), "gaunt_glow.png")
    rnd = random.Random(31)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    lum = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    regions = [(0, 0, 28, 14), (28, 0, 54, 16), (0, 16, 8, 36), (8, 16, 20, 32), (20, 16, 24, 20), (24, 16, 40, 22)]
    for (x0, y0, x1, y1) in regions:
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = 18 + rnd.randint(-6, 6)
                img.putpixel((x, y), (c + 4, c, c, 255))
    # rote Risse: Zufallswege durch Rumpf, Arme und Beine
    for (x0, y0, x1, y1) in regions[1:4]:
        for _ in range(3 if x1 - x0 > 10 else 2):
            x, y = rnd.randrange(x0, x1), rnd.randrange(y0, y1)
            for _ in range(10):
                if x0 <= x < x1 and y0 <= y < y1:
                    img.putpixel((x, y), (110, 10, 8, 255))
                    lum.putpixel((x, y), (255, 30, 20, 255))
                x += rnd.choice((-1, 0, 1))
                y += rnd.choice((0, 1, 1))
    # Augenschlitze vorne am Kopf (Kopf: texOffs 0,0, w6 h6 d8 -> Vorderseite x 8..13, y 8..13)
    for (x, y) in [(9, 10), (10, 10), (12, 10), (13, 10)]:
        img.putpixel((x, y), (200, 20, 10, 255))
        lum.putpixel((x, y), (255, 40, 30, 255))
    # Brust: glimmendes Inneres (Rumpf vorne: 28+5 .. 28+13, y 5..16)
    for y in range(8, 13):
        for x in range(35, 39):
            if rnd.random() < 0.6:
                img.putpixel((x, y), (140, 15, 10, 255))
                lum.putpixel((x, y), (255, 50, 30, 220))
    img.save(base)
    lum.save(glow)


def observer_texture():
    path = os.path.join(os.path.dirname(OUT), "observer.png")
    rnd = random.Random(77)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for y in range(0, 40):
        for x in range(0, 40):
            c = 6 + rnd.randint(-3, 3)
            img.putpixel((x, y), (c, c, c + 1, 255))
    img.save(path)


gaunt_textures()
observer_texture()
print("ok gaunt/observer")
