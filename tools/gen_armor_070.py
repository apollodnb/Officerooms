"""
Office Rooms 0.7.1: Multidimensional-Rüstung mit Form statt vollflächiger Kästen.

Vanilla-Rüstung bekommt ihre Form über durchsichtige Pixel in der Textur (das Modell ist ein leicht
vergrößerter Spielerkörper). Bisher war jede Fläche gefüllt: der Spieler steckte in einer Box,
auch das Gesicht war bedeckt. Hier eigene Zeichnung in der bisherigen Farbpalette:

- Helm: Silhouette eines Netherite-Helms (Stirnband, Wangen- und Kieferschutz, offenes Gesicht),
  Kamm auf dem Scheitel.
- Brustpanzer: Brustplatte mit Mittelgrat, Schulterplatten und Armschienen; Arme dazwischen frei.
- Hose (layer_2): Gürtel, Oberschenkelplatten, Knieschutz; Schienbeine frei (dort sitzen die Stiefel).
- Stiefel (layer_1, Beinbereich): unterer Teil mit Kappe.

UV-Layout wie Vanilla (64 × 32). Alle Pixel eigene Zeichnung.
"""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/office_rooms/textures/models/armor')

DARK = (14, 30, 96, 255)
BASE = (20, 40, 120, 255)
PLATE = (32, 62, 170, 255)
EDGE = (64, 104, 214, 255)
GLINT = (110, 240, 255, 255)
VIOLET = (190, 90, 255, 255)
CLEAR = (0, 0, 0, 0)

rng = random.Random(70)


class Box:
    """Ein Quader im Vanilla-UV-Layout: u, v = linke obere Ecke, Breite w, Höhe h, Tiefe d."""

    def __init__(self, u, v, w, h, d):
        self.u, self.v, self.w, self.h, self.d = u, v, w, h, d

    def faces(self):
        u, v, w, h, d = self.u, self.v, self.w, self.h, self.d
        return {
            'top': (u + d, v, w, d),
            'bottom': (u + d + w, v, w, d),
            'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h),
            'left': (u + d + w, v + d, d, h),
            'back': (u + d + w + d, v + d, w, h),
        }


def fill(img, rect, mask=None):
    """Fläche füllen; mask(x, y, w, h) -> Farbe oder None (durchsichtig). Ohne Maske: Plattenmuster."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            c = mask(x, y, w, h) if mask else plate(x, y, w, h)
            img.putpixel((x0 + x, y0 + y), c if c else CLEAR)


def plate(x, y, w, h):
    """Grundmuster einer Platte: Rand heller, innen Blau mit vereinzelten Glanzpunkten."""
    if x == 0 or y == 0 or x == w - 1 or y == h - 1:
        return EDGE if (x + y) % 3 else PLATE
    r = rng.random()
    if r < 0.05:
        return GLINT
    if r < 0.08:
        return VIOLET
    return BASE if (x + 2 * y) % 5 else DARK


def helmet(img):
    f = Box(0, 0, 8, 8, 8).faces()
    fill(img, f['top'], lambda x, y, w, h: GLINT if x in (3, 4) and 1 <= y <= 6 else plate(x, y, w, h))  # Kamm
    fill(img, f['bottom'], lambda *a: None)

    def front(x, y, w, h):
        if y <= 2:                                   # Stirnband über die ganze Breite
            return EDGE if y == 2 else plate(x, y, w, h)
        if x in (0, 7):                              # Wangenschutz
            return PLATE
        if y == 7 and x in (1, 6):                   # Kieferschutz
            return PLATE
        if y == 3 and x in (1, 6):
            return BASE
        return None                                  # Gesicht frei
    fill(img, f['front'], front)

    def side(x, y, w, h, front_left):
        fx = x if front_left else w - 1 - x          # fx = 0 ist die Gesichtsseite
        if y >= 6 and fx >= 5:                       # Nacken hinten etwas offen
            return None
        if y == 7 and 2 <= fx <= 4:
            return None
        return plate(x, y, w, h)
    fill(img, f['right'], lambda x, y, w, h: side(x, y, w, h, False))
    fill(img, f['left'], lambda x, y, w, h: side(x, y, w, h, True))
    fill(img, f['back'], lambda x, y, w, h: None if y == 7 and 2 <= x <= 5 else plate(x, y, w, h))


def chest(img):
    f = Box(16, 16, 8, 12, 4).faces()
    fill(img, f['top'])
    fill(img, f['bottom'], lambda *a: None)

    def front(x, y, w, h):
        if y <= 7:                                   # Brustplatte
            if x in (3, 4) and y >= 1:
                return GLINT if y in (2, 5) else EDGE  # Mittelgrat
            return plate(x, y, w, h)
        if y <= 9:                                   # Bauchschienen, mittig schmaler
            return PLATE if x in (0, 1, 6, 7) or y == 8 else None
        return None                                  # Bauch/Hüfte frei
    fill(img, f['front'], front)
    fill(img, f['back'], lambda x, y, w, h: plate(x, y, w, h) if y <= 8 else (PLATE if x in (0, 7) and y == 9 else None))
    side = lambda x, y, w, h: plate(x, y, w, h) if y <= 8 else None
    fill(img, f['right'], side)
    fill(img, f['left'], side)

    a = Box(40, 16, 4, 12, 4).faces()                # Arme (links wird gespiegelt)
    fill(img, a['top'])
    fill(img, a['bottom'], lambda *a_: None)

    def arm(x, y, w, h):
        if y <= 3:                                   # Schulterplatte
            return EDGE if y == 3 else plate(x, y, w, h)
        if 7 <= y <= 9:                              # Armschiene
            return GLINT if (y == 8 and x == 1) else PLATE
        return None
    for k in ('right', 'front', 'left', 'back'):
        fill(img, a[k], arm)


def boots(img):
    f = Box(0, 16, 4, 12, 4).faces()
    fill(img, f['top'], lambda *a: None)
    fill(img, f['bottom'], lambda x, y, w, h: DARK)

    def boot(x, y, w, h):
        if y < 7:
            return None
        if y == 7:
            return EDGE                              # Stulpe
        return plate(x, y, w, h)
    for k in ('right', 'left', 'back'):
        fill(img, f[k], boot)
    fill(img, f['front'], lambda x, y, w, h: GLINT if y == 10 and x in (1, 2) else boot(x, y, w, h))  # Kappe


def leggings(img):
    b = Box(16, 16, 8, 12, 4).faces()
    for k in ('top', 'bottom'):
        fill(img, b[k], lambda *a: None)

    def belt(x, y, w, h):
        if y < 9:
            return None
        if y == 9:
            return EDGE
        if y == 10 and x in (3, 4):
            return VIOLET                            # Schnalle
        return PLATE
    for k in ('right', 'front', 'left', 'back'):
        fill(img, b[k], belt)

    l = Box(0, 16, 4, 12, 4).faces()
    fill(img, l['top'], lambda *a: None)
    fill(img, l['bottom'], lambda *a: None)

    def thigh(x, y, w, h):
        if y <= 4:                                   # Oberschenkelplatte
            return plate(x, y, w, h)
        if y == 5:
            return EDGE
        return None
    for k in ('right', 'left', 'back'):
        fill(img, l[k], thigh)

    def thigh_front(x, y, w, h):
        if 6 <= y <= 7:                              # Knieschutz
            return GLINT if (y == 6 and x in (1, 2)) else PLATE
        return thigh(x, y, w, h)
    fill(img, l['front'], thigh_front)


def main():
    l1 = Image.new('RGBA', (64, 32), CLEAR)
    helmet(l1)
    chest(l1)
    boots(l1)
    l1.save(os.path.join(OUT, 'multidimensional_layer_1.png'))
    l2 = Image.new('RGBA', (64, 32), CLEAR)
    leggings(l2)
    l2.save(os.path.join(OUT, 'multidimensional_layer_2.png'))
    print('armor layers ok')


if __name__ == '__main__':
    main()
