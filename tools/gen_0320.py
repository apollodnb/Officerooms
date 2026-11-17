"""0.32.0: Aranea Membri - sounds (from the author's Ableton recordings plus synthesis), ceiling hole block, egg, lang, loot.
Usage: python3 gen_0320.py <dir with flayed.wav hound.wav colony.wav>"""
import json, os, random, subprocess, sys
from PIL import Image
SRC = sys.argv[1]
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources')
A = f'{R}/assets/office_rooms'; D = f'{R}/data/office_rooms'; O = 'office_rooms:'
S = f'{A}/sounds/entity/aranea_membri'; os.makedirs(S, exist_ok=True)
rnd = random.Random(320)
LOUD = ["compand", "0.01,0.25", "6:-60,-60,-36,-14,-20,-6,-8,-2,0,-1", "-3", "-90", "0.02"]

def sox(args):
    subprocess.run(['sox', '-V1'] + args, check=True)

def cut(src, out, start, dur, *fx, fade=(0.01, 0.2)):
    sox([os.path.join(SRC, src), '-C', '5', '-c', '1', f'{S}/{out}.ogg', 'trim', str(start), str(dur), 'remix', '-', 'gain', '-4',
         'fade', 't', str(fade[0]), '-0', str(fade[1])] + LOUD + list(fx) + ['norm', '-0.3', 'rate', '44100'])

# screech: the Flayed's scream dropped low, layered with a Colony call
for i, (fa, ca) in enumerate(((0.4, 0.8), (6.5, 9.6)), 1):
    a, b = f'/tmp/ar_a{i}.wav', f'/tmp/ar_b{i}.wav'
    sox([os.path.join(SRC, 'flayed.wav'), '-c', '1', a, 'trim', str(fa), '2.6', 'remix', '-', 'pitch', '-450', 'overdrive', '8', 'gain', '-6'])
    sox([os.path.join(SRC, 'colony.wav'), '-c', '1', b, 'trim', str(ca), '2.6', 'remix', '-', 'pitch', '-250', 'gain', '-8'])
    sox(['-m', a, b, '-C', '5', f'{S}/screech{i}.ogg', 'fade', 't', '0.01', '-0', '0.6'] + LOUD + ['reverb', '35', 'norm', '-0.3', 'rate', '44100'])
# hurt: short choked Colony slices, lowered
for i, t in enumerate((1.1, 5.6, 9.9), 1):
    cut('colony.wav', f'hurt{i}', t, 0.5, 'pitch', '-350', 'overdrive', '6', fade=(0.005, 0.22))
# death: the short Flayed shriek, very low, long tail
cut('flayed.wav', 'death', 16.2, 1.0, 'pitch', '-800', 'overdrive', '5', 'reverb', '70', 'pad', '0', '1.2', fade=(0.01, 0.4))
# ambient while awake: the Hound's panting, slowed into a wet rattle
for i, t in enumerate((10.2, 12.4), 1):
    cut('hound.wav', f'ambient{i}', t, 1.6, 'pitch', '-650', 'lowpass', '1800', fade=(0.1, 0.5))
# scratching in the ceiling (synthesised): irregular scraping bursts, dull through the tiles
for i in range(1, 4):
    parts = []
    for k in range(rnd.randint(3, 5)):
        p = f'/tmp/ar_s{i}_{k}.wav'
        dur = 0.08 + rnd.random() * 0.22
        sox(['-n', '-r', '44100', '-c', '1', p, 'synth', f'{dur:.3f}', 'whitenoise', 'bandpass', str(rnd.randint(1800, 3800)), '1200',
             'tremolo', str(rnd.randint(30, 70)), '90', 'fade', 't', '0.01', '-0', '0.04', 'pad', '0', f'{0.05 + rnd.random() * 0.25:.3f}'])
        parts.append(p)
    sox(parts + ['-C', '5', f'{S}/scratch{i}.ogg', 'lowpass', '2600', 'reverb', '20', 'norm', '-1'])
# heavy, sharp steps
for i in range(1, 3):
    a, b = f'/tmp/ar_t{i}a.wav', f'/tmp/ar_t{i}b.wav'
    sox(['-n', '-r', '44100', '-c', '1', a, 'synth', '0.18', 'sine', str(55 + i * 8), 'fade', 'q', '0', '-0', '0.16'])
    sox(['-n', '-r', '44100', '-c', '1', b, 'synth', '0.05', 'whitenoise', 'highpass', '2500', 'pad', '0', '0.13', 'gain', '-10'])
    sox(['-m', a, b, '-C', '5', f'{S}/step{i}.ogg', 'norm', '-2'])

def ev(name, files, att, sub):
    return {"category": "hostile", "subtitle": f"subtitles.office_rooms.aranea_membri.{sub}",
            "sounds": [{"name": O + "entity/aranea_membri/" + f, "attenuation_distance": att} for f in files]}
SJ = f'{A}/sounds.json'; sj = json.load(open(SJ))
sj['entity.aranea_membri.screech'] = ev('screech', ['screech1', 'screech2'], 64, 'screech')
sj['entity.aranea_membri.scratch'] = ev('scratch', ['scratch1', 'scratch2', 'scratch3'], 20, 'scratch')
sj['entity.aranea_membri.ambient'] = ev('ambient', ['ambient1', 'ambient2'], 16, 'ambient')
sj['entity.aranea_membri.hurt'] = ev('hurt', ['hurt1', 'hurt2', 'hurt3'], 16, 'hurt')
sj['entity.aranea_membri.death'] = ev('death', ['death'], 32, 'death')
sj['entity.aranea_membri.step'] = ev('step', ['step1', 'step2'], 16, 'step')
json.dump(sj, open(SJ, 'w'), indent=2)

# ceiling hole: off-white acoustic tile, ragged black hole, something pale deep inside
im = Image.new('RGBA', (16, 16)); px = im.load()
for y in range(16):
    for x in range(16):
        c = (206 + rnd.randint(-6, 6),) * 3
        if x % 8 == 0 or y % 8 == 0:
            c = (176, 174, 170)
        r = ((x - 7.5) ** 2 + (y - 8) ** 2) ** 0.5 + rnd.random() * 1.6
        if r < 5.2:
            c = (8, 7, 7)
        elif r < 6.2:
            c = (120, 110, 96)
        px[x, y] = c + (255,)
px[6, 8] = (150, 142, 120, 255); px[9, 8] = (150, 142, 120, 255)
im.save(f'{A}/textures/block/ceiling_hole.png')
json.dump({"parent": "minecraft:block/cube_all", "textures": {"all": O + "block/ceiling_hole"}}, open(f'{A}/models/block/ceiling_hole.json', 'w'))
json.dump({"variants": {"": {"model": O + "block/ceiling_hole"}}}, open(f'{A}/blockstates/ceiling_hole.json', 'w'))
json.dump({"parent": O + "block/ceiling_hole"}, open(f'{A}/models/item/ceiling_hole.json', 'w'))
json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(f'{A}/models/item/aranea_membri_spawn_egg.json', 'w'))
os.makedirs(f'{D}/loot_tables/entities', exist_ok=True)
json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": O + "ceiling_hole"}],
           "conditions": [{"condition": "minecraft:survives_explosion"}]}]}, open(f'{D}/loot_tables/blocks/ceiling_hole.json', 'w'))
json.dump({"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:bone", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 5}}]}]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:string", "functions": [
        {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 4}}]}]},
    {"rolls": 1, "conditions": [{"condition": "minecraft:random_chance", "chance": 0.25}],
     "entries": [{"type": "minecraft:item", "name": O + "research_file"}]}]},
    open(f'{D}/loot_tables/entities/aranea_membri.json', 'w'))
L = f'{A}/lang/en_us.json'; lang = json.load(open(L))
lang.update({'entity.office_rooms.aranea_membri': 'Aranea Membri', 'item.office_rooms.aranea_membri_spawn_egg': 'Aranea Membri Spawn Egg',
             'block.office_rooms.ceiling_hole': 'Broken Ceiling Tile',
             'subtitles.office_rooms.aranea_membri.screech': 'Something screeches', 'subtitles.office_rooms.aranea_membri.scratch': 'Scratching above',
             'subtitles.office_rooms.aranea_membri.ambient': 'Wet rattling', 'subtitles.office_rooms.aranea_membri.hurt': 'Aranea Membri hurts',
             'subtitles.office_rooms.aranea_membri.death': 'Aranea Membri dies', 'subtitles.office_rooms.aranea_membri.step': 'Heavy steps'})
json.dump(lang, open(L, 'w'), indent=1, ensure_ascii=False)
print('ok')
