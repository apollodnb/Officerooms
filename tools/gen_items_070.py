"""Office Rooms 0.7.0: eigene 16x16-Texturen fuer Totem of Mercy, Liquid Pain und die drei Schwerter."""
from PIL import Image
import os
OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/office_rooms/textures/item')

def img(): return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

def sword(name, blade_dark, blade, edge, guard, grip, pommel, gem):
    im = img(); p = im.putpixel
    # Klinge: Band entlang der Gegendiagonale x + y = 15, drei Pixel breit, Spitze oben rechts
    for x in range(5, 16):
        for y in range(0, 11):
            d = (x + y) - 15
            if abs(d) <= 1 and x - y >= -4:
                if x + y >= 14 and x >= 5:
                    p((x, y), edge if d == 1 else blade if d == 0 else blade_dark)
    p((15, 0), edge); p((14, 0), edge); p((15, 1), edge)
    # Parierstange quer zur Klinge
    for (x, y) in [(2, 9), (3, 10), (4, 11), (5, 12), (6, 13)]:
        p((x, y), guard)
    p((4, 11), gem)
    # Griff und Knauf
    for (x, y) in [(3, 12), (2, 13)]:
        p((x, y), grip)
    for (x, y) in [(1, 14), (0, 15), (1, 15), (0, 14)]:
        p((x, y), pommel)
    im.save(os.path.join(OUT, name + '.png'))

sword('witherfang', (28, 24, 32), (58, 52, 64), (150, 110, 190), (40, 34, 44), (70, 50, 40), (30, 30, 30), (170, 60, 220))
sword('leaden_edge', (60, 68, 80), (110, 120, 135), (190, 200, 215), (80, 90, 105), (55, 45, 40), (95, 105, 120), (90, 160, 230))
sword('venomtongue', (30, 70, 30), (60, 140, 50), (170, 230, 90), (40, 60, 35), (80, 60, 35), (50, 80, 40), (210, 240, 60))

# Liquid Pain: Flasche mit dunkelroter, fast schwarzer Fluessigkeit
im = img(); p = im.putpixel
glass = (205, 220, 230, 150); dark = (40, 0, 4); red = (120, 8, 16); hi = (200, 40, 50)
for y in range(7, 15):
    for x in range(4, 12):
        if (y in (7, 14) and x in (4, 11)):
            continue
        edge = x in (4, 11) or y in (7, 14)
        p((x, y), glass if edge else (red if y < 10 else dark))
for y in range(3, 7):
    p((7, y), glass); p((8, y), glass)
p((7, 2), (120, 80, 50)); p((8, 2), (120, 80, 50))  # Korken
p((6, 9), hi); p((6, 10), hi)
# kleines Totenkopf-Zeichen
for (x, y) in [(7, 10), (9, 10), (8, 11), (7, 12), (9, 12)]:
    p((x, y), (225, 215, 200))
im.save(os.path.join(OUT, 'liquid_pain.png'))

# Totem of Mercy: helle Quarz-Figur mit goldenem Ring und tuerkisen Augen
im = img(); p = im.putpixel
q = (232, 226, 214); qd = (180, 172, 160); gold = (235, 190, 60); goldd = (170, 120, 30); eye = (60, 220, 210)
for y in range(3, 14):
    for x in range(5, 11):
        p((x, y), q if x < 9 else qd)
for x in range(6, 10):
    p((x, 2), q)
for x in range(2, 5):
    p((x, 7), q); p((x, 8), qd)
for x in range(11, 14):
    p((x, 7), q); p((x, 8), qd)
for x in range(4, 12):
    p((x, 1), gold) if x in (5, 10) else None
for x in range(6, 10):
    p((x, 0), gold)
p((5, 1), goldd); p((10, 1), goldd)
p((6, 5), eye); p((9, 5), eye)
for x in range(6, 10):
    p((x, 10), gold)
p((7, 11), goldd); p((8, 11), goldd)
for x in range(5, 11):
    p((x, 14), qd)
im.save(os.path.join(OUT, 'totem_of_mercy.png'))
print('ok')
