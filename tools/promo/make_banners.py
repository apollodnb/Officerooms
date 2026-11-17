import random, sys, os
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance, ImageChops
D = '/home/claude/banners'
TITLE = f'{D}/fonts/BebasNeue-Regular.ttf'
MONO = f'{D}/fonts/IBMPlexMono-Medium.ttf'
CREAM = (242, 235, 217)
ACCENT = (233, 205, 110)
random.seed(4711)

def cover(img, w, h, fx=0.5, fy=0.5, zoom=1.0):
    iw, ih = img.size
    s = max(w / iw, h / ih) * zoom
    img = img.resize((int(iw * s) + 1, int(ih * s) + 1), Image.LANCZOS)
    iw, ih = img.size
    x = int((iw - w) * fx); y = int((ih - h) * fy)
    return img.crop((x, y, x + w, y + h))

def grade(img, side='left', strength=0.78, bright=1.0):
    w, h = img.size
    img = ImageEnhance.Brightness(img).enhance(bright)
    img = ImageEnhance.Color(img).enhance(0.85)
    img = ImageEnhance.Contrast(img).enhance(1.08)
    warm = Image.new('RGB', (w, h), (58, 44, 18))
    img = Image.blend(img, warm, 0.16)
    # darkening gradient behind the text
    g = Image.new('L', (w, h))
    gd = ImageDraw.Draw(g)
    for x in range(w):
        t = x / w if side == 'left' else (abs(x - w / 2) / (w / 2) if side == 'center' else 1 - x / w)
        a = strength * (1 - t) ** 1.8 + 0.04 if side != 'center' else 0.5 - 0.3 * t
        gd.line([(x, 0), (x, h)], fill=int(255 * min(1, a)))
    img = Image.composite(Image.new('RGB', (w, h), (10, 8, 5)), img, g)
    # vignette
    v = Image.new('L', (w, h), 0)
    vd = ImageDraw.Draw(v)
    for i in range(40):
        vd.rectangle([i * w / 160, i * h / 160, w - i * w / 160, h - i * h / 160], outline=None, fill=int(255 * (1 - i / 40) ** 2 * 0.0))
    vig = Image.radial_gradient('L').resize((w, h))
    img = Image.composite(Image.new('RGB', (w, h), (0, 0, 0)), img, vig.point(lambda p: int(p * 0.38)))
    # scanlines + grain
    sl = Image.new('L', (w, h), 0)
    sd = ImageDraw.Draw(sl)
    for y in range(0, h, 3):
        sd.line([(0, y), (w, y)], fill=22)
    img = Image.composite(Image.new('RGB', (w, h), (0, 0, 0)), img, sl)
    noise = Image.effect_noise((w, h), 26).convert('RGB')
    img = ImageChops.blend(img, ImageChops.overlay(img, noise), 0.22)
    return img

def spaced(draw, xy, text, font, fill, spacing, shadow=True):
    x, y = xy
    for ch in text:
        if shadow:
            draw.text((x + 3, y + 3), ch, font=font, fill=(0, 0, 0))
        draw.text((x, y), ch, font=font, fill=fill)
        x += draw.textlength(ch, font=font) + spacing
    return x

def width(draw, text, font, spacing):
    return sum(draw.textlength(c, font=font) + spacing for c in text) - spacing

def glow_text(img, xy, text, font, fill, spacing, radius=10):
    w, h = img.size
    layer = Image.new('L', (w, h), 0)
    d = ImageDraw.Draw(layer)
    x, y = xy
    for ch in text:
        d.text((x, y), ch, font=font, fill=255)
        x += d.textlength(ch, font=font) + spacing
    blur = layer.filter(ImageFilter.GaussianBlur(radius))
    img.paste(Image.new('RGB', (w, h), (255, 220, 140)), (0, 0), blur.point(lambda p: int(p * 0.45)))
    spaced(ImageDraw.Draw(img), xy, text, font, fill, spacing)

def plaque(draw, x, y, text):
    f = ImageFont.truetype(MONO, 24)
    tw = width(draw, text, f, 4)
    draw.rectangle([x, y, x + tw + 36, y + 44], fill=(181, 151, 74), outline=(92, 72, 28), width=3)
    draw.rectangle([x + 6, y + 6, x + tw + 30, y + 38], outline=(120, 96, 40), width=1)
    spaced(draw, (x + 18, y + 7), text, f, (40, 30, 10), 4, shadow=False)

def section(name, floor, title, sub, shot, fx=0.5, fy=0.5, zoom=1.0, bright=1.0):
    W, H = 1600, 300
    img = grade(cover(Image.open(f'{D}/shots/{shot}.png').convert('RGB'), W, H, fx, fy, zoom), bright=bright)
    d = ImageDraw.Draw(img)
    plaque(d, 70, 44, f'FLOOR {floor:02d}')
    tf = ImageFont.truetype(TITLE, 118)
    glow_text(img, (66, 92), title, tf, CREAM, 6)
    d = ImageDraw.Draw(img)
    d.rectangle([70, 222, 70 + 90, 228], fill=ACCENT)
    sf = ImageFont.truetype(MONO, 27)
    spaced(d, (180, 208), sub.upper(), sf, ACCENT, 3)
    # flickering tube at the very top
    d.rectangle([0, 0, W, 5], fill=(233, 205, 110))
    img.save(f'{D}/out/{name}.png', optimize=True)

def header():
    W, H = 1600, 620
    img = grade(cover(Image.open(f'{D}/shots/level0.png').convert('RGB'), W, H, 0.5, 0.45, 1.05), side='center')
    d = ImageDraw.Draw(img)
    tf = ImageFont.truetype(TITLE, 250)
    t = 'OFFICE ROOMS'
    tw = width(d, t, tf, 14)
    glow_text(img, ((W - tw) / 2, 140), t, tf, CREAM, 14, radius=18)
    d = ImageDraw.Draw(img)
    sf = ImageFont.truetype(MONO, 34)
    s = 'A LIMINAL OFFICE DIMENSION FOR MINECRAFT 1.20.1'
    sw = width(d, s, sf, 4)
    spaced(d, ((W - sw) / 2, 412), s, sf, ACCENT, 4)
    d.rectangle([W / 2 - 70, 395, W / 2 + 70, 401], fill=ACCENT)
    f2 = ImageFont.truetype(MONO, 24)
    s2 = 'FORGE  ·  90+ FLOORS  ·  7 MEGASTRUCTURES  ·  120 QUESTS'
    s2w = width(d, s2, f2, 3)
    spaced(d, ((W - s2w) / 2, 470), s2, f2, CREAM, 3)
    plaque(d, W / 2 - 118, 70, 'YOU NOCLIPPED')
    d.rectangle([0, 0, W, 6], fill=ACCENT); d.rectangle([0, H - 6, W, H], fill=ACCENT)
    img.save(f'{D}/out/00_header.png', optimize=True)

header()
S0 = [('01_office_rooms', 'The Office Rooms', 'Endless floors. Fluorescent hum. No way out.', 'office0', 0.5, 0.55, 1.0),
     ('02_megastructures', 'Megastructures', 'Poolrooms, Hotel, Maze, Cinema and more', 'pool1', 0.5, 0.5, 1.0),
     ('03_creatures', 'Creatures', 'Something walks the halls. Do not run.', 'creatures', 0.62, 0.45, 1.25),
     ('04_research_outposts', 'Research Outposts', 'Pull the lever. Wait. Press the red button.', 'rig', 0.55, 0.45, 1.15),
     ('05_quest_book', 'Quest Book', '120 quests, from your first day to the Underside', 'cinema', 0.5, 0.35, 1.1),
     ('06_level0_underside', 'Level 0 & The Underside', 'Above the top floor. Beneath the bedrock.', 'maze', 0.5, 0.5, 1.0),
     ('07_installation', 'Installation', 'Forge 1.20.1  ·  GeckoLib required', 'hotel', 0.6, 0.5, 1.0),
     ('08_compatibility', 'Compatibility', 'OfficeCraft, Refurbished Furniture, FTB Quests (all optional)', 'industrial', 0.6, 0.5, 1.0),
     ('09_credits', 'Credits', 'Thank you for wandering the halls', 'levelfun', 0.6, 0.5, 1.0)]
S = [('01_office_rooms', 'The Office Rooms', 'Endless floors. Fluorescent hum. No way out.', 'office0', 0.5, 0.55, 1.0, 1.25),
     ('02_megastructures', 'Megastructures', 'Poolrooms, Hotel, Maze, Cinema and more', 'pool1', 0.5, 0.5, 1.0, 1.1),
     ('03_creatures', 'Creatures', 'Something walks the halls. Do not run.', 'creatures', 0.62, 0.42, 1.25, 1.9),
     ('04_research_outposts', 'Research Outposts', 'Pull the lever. Wait. Press the red button.', 'rig', 0.55, 0.45, 1.15, 1.05),
     ('05_quest_book', 'Quest Book', '120 quests, from your first day to the Underside', 'levelfun', 0.6, 0.55, 1.0, 1.15),
     ('06_level0_underside', 'Level 0 & The Underside', 'Above the top floor. Beneath the bedrock.', 'maze', 0.5, 0.5, 1.0, 1.9),
     ('07_installation', 'Installation', 'Forge 1.20.1  ·  GeckoLib required', 'hotel', 0.15, 0.55, 1.1, 1.15),
     ('08_compatibility', 'Compatibility', 'OfficeCraft, Refurbished Furniture, FTB Quests (all optional)', 'industrial', 0.6, 0.5, 1.0, 1.25),
     ('09_credits', 'Credits', 'Thank you for wandering the halls', 'corridor', 0.6, 0.5, 1.0, 1.15)]
for i, (n, t, s, shot, fx, fy, z, br) in enumerate(S):
    section(n, i + 1, t, s, shot, fx, fy, z, br)
print('ok')
