"""Converts the data pack of Office Rooms from the Minecraft 1.20.1 layout (branch main) to 1.21.1 (branch neoforge-1.21.1).
Idempotent: run it after copying generated resources over from the 1.20.1 branch.
  python3 tools/to_1211.py
- folder names: advancements -> advancement, loot_tables -> loot_table, recipes -> recipe, functions -> function,
  tags/blocks -> tags/block, tags/items -> tags/item, tags/entity_types -> tags/entity_type, tags/fluids -> tags/fluid
- recipes: result {"item"} -> {"id"}, stonecutting result string + count -> {"id", "count"}
- loot tables: set_nbt (written books, potions) -> set_components
- advancements: display.icon.item -> id
"""
import json, os, re, shutil

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'data')
RENAMES = [('advancements', 'advancement'), ('loot_tables', 'loot_table'), ('recipes', 'recipe'), ('functions', 'function')]
TAGS = [('blocks', 'block'), ('items', 'item'), ('entity_types', 'entity_type'), ('fluids', 'fluid'), ('functions', 'function')]


def merge_move(src, dst):
    if not os.path.isdir(src):
        return
    for dirpath, _, files in os.walk(src):
        rel = os.path.relpath(dirpath, src)
        os.makedirs(os.path.join(dst, rel), exist_ok=True)
        for f in files:
            shutil.move(os.path.join(dirpath, f), os.path.join(dst, rel, f))
    shutil.rmtree(src)


def snbt_pages(s):
    """the single-quoted JSON strings of pages:['...','...']"""
    i = s.index('pages:[') + 7
    out = []
    while i < len(s) and s[i] != ']':
        if s[i] == "'":
            j = i + 1; buf = []
            while s[j] != "'":
                if s[j] == '\\' and s[j + 1] in "'\\":
                    buf.append(s[j + 1]); j += 2
                else:
                    buf.append(s[j]); j += 1
            out.append(json.loads(''.join(buf)))
            i = j + 1
        else:
            i += 1
    return out


def convert_function(fn):
    if fn.get('function') != 'minecraft:set_nbt':
        return fn
    tag = fn['tag']
    if tag.startswith('{Potion:'):
        potion = re.search(r'Potion:"([^"]+)"', tag).group(1)
        return {"function": "minecraft:set_components", "components": {"minecraft:potion_contents": {"potion": potion}}}
    if tag.startswith('{title:'):
        title = re.search(r'title:"((?:[^"\\]|\\.)*)"', tag).group(1).replace('\\"', '"')
        author = re.search(r'author:"((?:[^"\\]|\\.)*)"', tag).group(1).replace('\\"', '"')
        gen = int(re.search(r'generation:(\d+)', tag).group(1)) if 'generation:' in tag else 0
        return {"function": "minecraft:set_components", "components": {"minecraft:written_book_content": {
            "title": title, "author": author, "generation": gen, "resolved": True, "pages": snbt_pages(tag)}}}
    raise SystemExit('unknown set_nbt tag: ' + tag[:80])


def walk_loot(o):
    if isinstance(o, dict):
        if 'functions' in o:
            o['functions'] = [convert_function(f) for f in o['functions']]
        for v in o.values():
            walk_loot(v)
    elif isinstance(o, list):
        for v in o:
            walk_loot(v)


def fix_recipe(r):
    res = r.get('result')
    if isinstance(res, str):
        r['result'] = {"id": res, "count": r.pop('count', 1)}
    elif isinstance(res, dict) and 'item' in res:
        res['id'] = res.pop('item')


def each_json(folder):
    for dirpath, _, files in os.walk(folder):
        for f in files:
            if f.endswith('.json'):
                yield os.path.join(dirpath, f)


for ns in os.listdir(ROOT):
    base = os.path.join(ROOT, ns)
    for a, b in RENAMES:
        merge_move(os.path.join(base, a), os.path.join(base, b))
    for a, b in TAGS:
        merge_move(os.path.join(base, 'tags', a), os.path.join(base, 'tags', b))
    for p in each_json(os.path.join(base, 'recipe')):
        r = json.load(open(p)); fix_recipe(r); json.dump(r, open(p, 'w'), indent=1)
    for p in each_json(os.path.join(base, 'loot_table')):
        t = json.load(open(p)); walk_loot(t); json.dump(t, open(p, 'w'), indent=1)
    for p in each_json(os.path.join(base, 'advancement')):
        a = json.load(open(p))
        icon = a.get('display', {}).get('icon')
        if icon and 'item' in icon:
            icon['id'] = icon.pop('item')
            if 'nbt' in icon:
                icon.pop('nbt')
        json.dump(a, open(p, 'w'), indent=1)
print('data converted to 1.21.1')
