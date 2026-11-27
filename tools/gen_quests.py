"""0.25.0: FTB Quests progression for Office Rooms (FTB Quests 2001.4.x, file version 13).

Writes config/ftbquests/quests/ into ../quests/ (copy that folder into the modpack's config/ftbquests/).
Branches are unlocked by items (or kills); a few events use hidden milestone advancements
(office_rooms:milestones/...) granted by the mod itself. All text is English.
"""
import hashlib, os, shutil

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'quests', 'config', 'ftbquests', 'quests')
RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'office_rooms_quests')
PREFIX = 'office_rooms_'   # 0.40.0: unique file names, so they never collide with a modpack's own chapters
O = 'office_rooms:'


def hid(key):
    # FTB Quests parses ids as signed longs: keep the top bit clear (and never 0)
    v = int(hashlib.sha1(('office_rooms/' + key).encode()).hexdigest()[:16], 16) & 0x7FFFFFFFFFFFFFFF
    return format(v or 1, '016X')


def s(v):
    return '"' + v.replace('\\', '\\\\').replace('"', '\\"') + '"'


def snbt(v, ind=0):
    t = '\t' * ind
    if isinstance(v, bool):
        return 'true' if v else 'false'
    if isinstance(v, dict):
        if not v:
            return '{ }'
        return '{\n' + ''.join(f'{t}\t{k}: {snbt(x, ind + 1)}\n' for k, x in v.items()) + t + '}'
    if isinstance(v, list):
        if not v:
            return '[ ]'
        if all(isinstance(x, str) for x in v):
            return '[\n' + ''.join(f'{t}\t{s(x)}\n' for x in v) + t + ']'
        return '[\n' + ''.join(f'{t}\t{snbt(x, ind + 1)}\n' for x in v) + t + ']'
    if isinstance(v, Raw):
        return v.v
    if isinstance(v, str):
        return s(v)
    return str(v)


class Raw:
    def __init__(self, v):
        self.v = v


def L(n):
    return Raw(f'{n}L')


def D(n):
    return Raw(f'{float(n)}d')


# ---------------------------------------------------------------- task / reward helpers
def item(i, n=1, consume=False):
    d = {'type': 'item', 'item': i if ':' in i else O + i}
    if n != 1:
        d['count'] = L(n)
    if consume:
        d['consume_items'] = True
    return d


def kill(e, n=1):
    return {'type': 'kill', 'entity': e if ':' in e else O + e, 'value': L(n)}


def adv(path):
    return {'type': 'advancement', 'advancement': O + 'milestones/' + path, 'criterion': ''}


def dim(d):
    return {'type': 'dimension', 'dimension': d if ':' in d else O + d}


def observe(e, ticks=40):
    return {'type': 'observation', 'observe_type': 5, 'to_observe': e if ':' in e else O + e, 'timer': L(ticks)}


def check():
    return {'type': 'checkmark'}


def give(i, n=1):
    d = {'type': 'item', 'item': i if ':' in i else O + i}
    if n != 1:
        d['count'] = n
    return d


def xp(n):
    return {'type': 'xp', 'xp': n}


def tid(name):
    return int(hid('table/' + name), 16)


def table(name):
    return {'type': 'random', 'table_id': L(tid(name))}


# ---------------------------------------------------------------- the progression
CHAPTERS = []   # (key, title, subtitle, icon, quests)
cur = None


def chapter(key, title, subtitle, icon):
    global cur
    cur = []
    CHAPTERS.append((key, title, subtitle, icon, cur))


def quest(key, title, sub, desc, tasks, rewards=(), deps=(), x=0, y=0, shape=None, optional=False, icon=None, size=None, repeat=False, any_dep=False):
    q = {'_key': key, 'title': title, 'subtitle': sub, 'description': desc, 'tasks': list(tasks), 'rewards': list(rewards),
         'deps': list(deps), 'x': x, 'y': y}
    if shape: q['shape'] = shape
    if optional: q['optional'] = True
    if icon: q['icon'] = icon if ':' in icon else O + icon
    if size: q['size'] = size
    if repeat: q['can_repeat'] = True
    if any_dep: q['dependency_requirement'] = 'one_completed'
    cur.append(q)


# 1 --------------------------------------------------------------------------------------------
chapter('threshold', 'I. The Threshold', 'How to get in. You should not want to.', 'minecraft:sculk_sensor')
quest('memo', 'Memo #001', 'Read me first',
      ['&7FROM: Facilities', '&7TO: New staff', '',
       'Somewhere in the world there is an office that should not exist.',
       'Some people find a way in. Most of them do not find a way out.',
       '', 'This book will follow you. It does not judge. It only keeps track.'],
      [check()], [give('bandage', 2), xp(10)], x=0, y=0, shape='gear', size=1.5)
quest('shrine', 'The Shrine', 'An office in the wilderness',
      ['Very rarely, a piece of an office stands alone in the wild: overgrown, lit by candles, with a lectern.',
       'Its chest holds what you need to open the way.', '',
       '&8Hint: it is an Abandoned Office Shrine. With cheats: /locate structure office_rooms:abandoned_office_shrine'],
      [item('minecraft:sculk_sensor')], [give('glowstick', 4), xp(25)], deps=['memo'], x=2, y=-1.5, optional=True)
quest('sand', 'Something That Falls', '',
      ['The rift opens where sand falls onto a sculk sensor.', 'Any sand will do. It always does.'],
      [item('minecraft:sand', 4)], [xp(10)], deps=['memo'], x=2, y=0, optional=True)
quest('facility', 'A Door Someone Built', 'Or: start at a research outpost',
      ['There is a second way in. Research teams built outposts under ancient cities, in jungle temples, deep in caves and in the Nether.',
       'Pull the lever, wait out the warnings, press the red button. The gateway leads into the Office Rooms.'],
      [adv('outpost_gateway')], [give('electronic_component', 2), xp(60)], deps=['memo'], x=2, y=1.5, optional=True)
quest('rift', 'The Office Rooms', 'However you got here',
      ['Through a Liminal Rift (sand dropped onto a sculk sensor), through an outpost gateway, or simply because you woke up here.',
       'You will arrive in a room. The lights will hum. That is normal.', '',
       '&8Everything in the Overworld is optional: you never have to go back.'],
      [dim('office_rooms')], [give('almond_water', 3), give('chalk'), xp(50)], deps=['memo'], x=4, y=0, shape='hexagon', size=1.5)

# 2 --------------------------------------------------------------------------------------------
chapter('first_day', 'II. First Day at the Office', 'Supplies, habits and the first rules', O + 'almond_water')
quest('almond', 'Almond Water', 'Drink it. Trust it.',
      ['You will find bottles of Almond Water in desks, cabinets and break rooms.',
       'Nobody knows who leaves them there. Drink it when you feel wrong.'],
      [item('almond_water')], [give('almond_water', 2), xp(20)], deps=['rift'], x=0, y=0, shape='hexagon', size=1.25)
quest('chalk', 'Mark the Walls', 'Left, right, cross',
      ['Rooms repeat. You will walk in circles. Chalk on the walls is the oldest trick that still works.'],
      [item('chalk')], [give('chalk'), xp(15)], deps=['almond'], x=2, y=-1.5)
quest('bandage', 'First Aid', 'Paper, paper, wool',
      ['Craft a bandage. Applying it takes two seconds and you cannot run while you do.', 'Plan your two seconds.'],
      [item('bandage')], [give('bandage', 2), xp(15)], deps=['almond'], x=2, y=0)
quest('glowstick', 'A Little Green Light', '',
      ['Throw a glowstick down a dark corridor before you walk it.'],
      [item('glowstick')], [give('glowstick', 3), xp(15)], deps=['almond'], x=2, y=1.5)
quest('flashlight', 'Industrial Flashlight', 'A strong beam, a short life',
      ['Right-click to switch it on. The charge runs out faster than you think.'],
      [item('industrial_flashlight')], [xp(40)], deps=['glowstick'], x=4, y=1.5)
quest('battery', 'Portable Battery', 'Steal light from the ceiling',
      ['Hold the battery in your other hand to charge the flashlight.',
       'Empty? Drain a lit ceiling light. The room will not thank you.'],
      [item('portable_battery')], [give('portable_battery'), xp(40)], deps=['flashlight'], x=6, y=1.5)
quest('flare', 'Emergency Flare', 'Red means back off',
      ['Most creatures back away from a burning flare. Most.'],
      [item('emergency_flare')], [give('emergency_flare'), xp(30)], deps=['glowstick'], x=4, y=3)
quest('noise', 'Noise Maker', 'Somewhere else, please',
      ['Throw it and run the other way. Whatever hears it will go and look.'],
      [item('noise_maker')], [xp(30)], deps=['flare'], x=6, y=3)
quest('card1', 'Level 1 Access Card', 'Security doors',
      ['Some doors in the Office Rooms are locked. Cards are found in special rooms and in the houses of the Neighbourhood.'],
      [item('level_1_access_card')], [xp(60), table('supplies')], deps=['chalk'], x=4, y=-1.5, shape='diamond')
quest('totem', 'Totem of Mercy', 'One way out',
      ['If you would die in the Office Rooms, it carries you back to the Overworld instead.', 'Keep one. Always.'],
      [item('totem_of_mercy')], [xp(80)], deps=['bandage'], x=4, y=0, shape='diamond')
quest('first_day_done', 'End of the First Day', 'You are still here',
      ['You have the basics. From here the building opens up in every direction.', '',
       '&eNext: Facilities (power and air), Research and the Megastructures.'],
      [item('almond_water', 4)], [table('supplies'), xp(100)], deps=['chalk', 'bandage', 'glowstick'], x=6, y=-0.5, shape='hexagon', size=1.5)

# 3 --------------------------------------------------------------------------------------------
chapter('facilities', 'III. Facilities', 'Power, air and what lives in the dark', O + 'gas_mask')
quest('monitor', 'The Monitors Are Always Right', 'Read the screens',
      ['HAZARD ZONE and POWER warnings appear on the monitors in offices and corridors long before anything happens.', '',
       'Craft a Hazard Monitor for yourself (or just keep reading the ones on the walls).'],
      [item('hazard_monitor')], [xp(60)], deps=['first_day_done'], x=0, y=0, shape='hexagon', size=1.25)
quest('fusebox', 'Lights Out', 'Repair the fuse box',
      ['Blackouts are announced on the monitors. When the lights die, a fuse box on your floor breaks.',
       'Find it in the dark and repair it. Repairs are rewarded.'],
      [adv('fuse_box_repaired')], [give('research_file'), give('portable_battery'), xp(120)], deps=['monitor'], x=2, y=-1.5, shape='diamond')
quest('component', 'Electronic Components', 'Salvage',
      ['Server rooms and the industrial complex are the best places to look.'],
      [item('electronic_component', 4)], [xp(40)], deps=['monitor'], x=2, y=0)
quest('goggles', 'Night Vision Goggles', 'See without being seen',
      ['They run on battery. A portable battery in your hand charges goggles you are wearing.'],
      [item('night_vision_goggles')], [give('portable_battery'), xp(80)], deps=['component'], x=4, y=0)
quest('filter', 'Gas Filter', 'Charcoal, paper, a nugget',
      ['A gas mask is worthless without a fresh filter.'],
      [item('gas_filter', 2)], [give('gas_filter'), xp(30)], deps=['monitor'], x=2, y=1.5)
quest('mask', 'Gas Mask', 'Breathe',
      ['Hazard Zones poison the air. A mask with a filter keeps you alive in them for ten minutes.'],
      [item('gas_mask')], [give('gas_filter', 2), xp(80)], deps=['filter'], x=4, y=1.5)
quest('detector', 'Special Event Detector', 'Where is it this time?',
      ['Hold it to find an announced or active Hazard Zone.'],
      [item('special_event_detector')], [xp(60)], deps=['mask'], x=6, y=1.5)
quest('crystal', 'Toxic Crystal', 'Only inside the zone',
      ['Toxic Crystals grow only inside an active Hazard Zone, guarded by what the gas brings with it.'],
      [item('toxic_crystal')], [give('gas_filter', 2), xp(150)], deps=['detector'], x=8, y=1.5, shape='diamond')
quest('visor', 'Hazard Visor', 'Mask and goggles, fused',
      ['Combine a gas mask, night vision goggles and a toxic crystal.'],
      [item('hazard_visor')], [give('research_file', 2), xp(250)], deps=['crystal', 'goggles'], x=10, y=0.75, shape='hexagon', size=1.5)
quest('anomaly', 'Anomaly Detector', 'It never says what it is',
      ['The closer a creature, the stronger the signal.'],
      [item('anomaly_detector')], [xp(80)], deps=['goggles'], x=6, y=0)
quest('elevator', 'Going Up', 'Build an elevator',
      ['Floors are endless in both directions. Elevators make them shorter.'],
      [item('elevator')], [xp(60)], deps=['component'], x=4, y=-1.5)

# 4 --------------------------------------------------------------------------------------------
chapter('research', 'IV. Research Division', 'Classified findings and what they buy', O + 'research_file')
quest('file', 'Classified', 'Your first Research File',
      ['Research Files are rare. Labs, security offices, vaults and repaired fuse boxes are good places to look.'],
      [item('research_file')], [xp(80)], deps=['first_day_done'], x=0, y=0, shape='hexagon', size=1.25)
quest('table', 'Research Table', 'Paper, books, iron',
      ['Exchange Research Files at a Research Table: more hearts, or a reforged weapon.'],
      [item('research_table')], [xp(60)], deps=['file'], x=2, y=0)
quest('heart', 'Heart Research', '+1 heart, for good',
      ['Three files buy one more heart. It survives death.'],
      [adv('heart_research')], [give('research_file'), xp(150)], deps=['table'], x=4, y=-1)
quest('heart_max', 'Five Hearts Later', 'Maximum Heart Research',
      ['Complete all five stages of Heart Research.'],
      [adv('heart_research_max')], [give('glitched_ingot', 2), xp(500)], deps=['heart'], x=6, y=-1, shape='diamond', size=1.25)
quest('reforge', 'Reforged', 'Sharper than before',
      ['Reforge a weapon at the Research Table. Enchantments stay.'],
      [adv('reforge')], [give('research_file'), xp(150)], deps=['table'], x=4, y=1)
quest('reforge_max', 'Masterwork', 'Reforge level 3',
      ['Take one weapon all the way.'],
      [adv('reforge_max')], [give('glitched_ingot', 2), xp(500)], deps=['reforge'], x=6, y=1, shape='diamond', size=1.25)
for i, (b, t) in enumerate([('microscope', 'Microscope'), ('specimen_jar', 'Specimen Jar'), ('lab_cabinet', 'Lab Cabinet'),
                            ('containment_tank', 'Containment Tank'), ('experiment_machine', 'Experiment Machine')]):
    quest('lab_' + b, t, 'Equip a lab', [f'Build a {t} for your own laboratory.'], [item(b)], [xp(40)],
          deps=['table' if i == 0 else 'lab_' + ['microscope', 'specimen_jar', 'lab_cabinet', 'containment_tank'][i - 1]],
          x=2 + i * 1.5, y=3, optional=True)
for i, (b, t) in enumerate([('crt_monitor', 'CRT Monitor'), ('cctv_bank', 'CCTV Bank'), ('control_panel', 'Control Panel'), ('server_rack', 'Server Rack')]):
    quest('sec_' + b, t, 'Equip a security office', [f'Build a {t}.'], [item(b)], [xp(40)],
          deps=['table' if i == 0 else 'sec_' + ['crt_monitor', 'cctv_bank', 'control_panel'][i - 1]],
          x=2 + i * 1.5, y=4.5, optional=True)
quest('ingot', 'Glitched Ingot', 'Metal that is not quite here',
      ['Found in vaults and deep loot. It flickers when you are not looking.'],
      [item('glitched_ingot')], [xp(120)], deps=['file'], x=2, y=-2.5, shape='diamond')
quest('rod', 'Multidimensional Rod', '',
      ['The handle for everything that follows.'],
      [item('multidimensional_rod')], [xp(120)], deps=['ingot'], x=4, y=-2.5)
gear = [('sword', 'Sword'), ('pickaxe', 'Pickaxe'), ('axe', 'Axe'), ('shovel', 'Shovel'), ('hoe', 'Hoe'),
        ('helmet', 'Helmet'), ('chestplate', 'Chestplate'), ('leggings', 'Leggings'), ('boots', 'Boots')]
for i, (g, t) in enumerate(gear):
    quest('md_' + g, 'Multidimensional ' + t, 'Glitched gear', [f'Craft the Multidimensional {t}.'], [item('multidimensional_' + g)],
          [xp(100)], deps=['rod'], x=6 + (i % 3) * 1.25, y=-4 + (i // 3) * 1.25, optional=g in ('hoe', 'shovel'))
quest('md_set', 'Out of Phase', 'The full multidimensional armour',
      ['Wear what does not quite exist.'],
      [item('multidimensional_helmet'), item('multidimensional_chestplate'), item('multidimensional_leggings'), item('multidimensional_boots')],
      [give('research_file', 3), xp(800)], deps=['md_helmet', 'md_chestplate', 'md_leggings', 'md_boots'], x=10.5, y=-2.75, shape='hexagon', size=1.5)

# 5 --------------------------------------------------------------------------------------------
chapter('megastructures', 'V. Megastructures', 'Places the floor plan does not show', O + 'pool_tile')
mega = [('poolrooms', 'The Poolrooms', 'Warm water, white tiles, no exits',
         'Tiles and lukewarm water as far as you can see. Somewhere below the surface there is a labyrinth.', 'chlorine_pearl', 'Chlorine Pearl',
         'From the very end of the underwater labyrinth.'),
        ('maze', 'The Maze', 'Walls that move when you blink',
         'Yellowed wallpaper, endless turns. There is a vault at the heart of it.', 'thread_of_the_maze', 'Thread of the Maze',
         'Unwound in the vault of the Maze.'),
        ('hotel', 'The Abandoned Hotel', 'No vacancy',
         'Lobby, corridors, rooms that were never checked out. The vault is in the basement.', 'room_key_000', 'Room Key 000',
         'From the hotel vault. There is no room 000.'),
        ('industrial', 'The Industrial Complex', 'Steam, grates and generator halls',
         'Five levels of pipes and machines. Something still hums in the generator halls.', 'humming_fuse', 'Humming Fuse',
         'From a generator hall.'),
        ('neighbourhood', 'The Neighbourhood', 'Houses inside an office',
         'A street of houses under fluorescent light. Their families are in the photographs.', 'faded_photograph', 'Faded Photograph',
         'From one of the houses.'),
        ('level_fun', 'Level Fun', '=)',
         'Balloons, confetti, presents. The Partygoers are very happy to see you.', 'party_smile_badge', 'Party Smile Badge',
         'A present from Level Fun.'),
        ('cinema', 'The Abandoned Cinema', 'Now showing: nothing',
         'Three floors of dark auditoriums around a lobby nobody cleaned. Some screens still flicker. Only in worlds made with 0.31.0 or newer.',
         'ticket_stub', 'Ticket Stub', 'Admit one. From the box office or the lost-and-found chests of the cinema.'),
        ('mall', 'The Dead Mall', 'Always open',
         'Three floors of shops around atriums under grey skylights. The escalators stopped long ago, the fountain is dry, the music still plays. '
         'Something lives above the ceiling tiles. Only in worlds made with 0.43.0 or newer.',
         'you_are_here_pin', 'You Are Here',
         'The red pin of a mall directory. The memo in the mall office says where it went: a stockroom on the top floor, behind the shops. '
         'In older worlds without malls, one lost-and-found chest of the cinema keeps it.')]
quest('mega_start', 'Beyond the Floor Plan', 'Megastructures',
      ['Between the endless offices lie whole structures that do not belong there.',
       'Each one keeps an artifact. The Underside Key needs all eight, and one more from the sea.'],
      [item('level_1_access_card')], [give('glowstick', 4), xp(60)], deps=['first_day_done'], x=0, y=3.75, shape='hexagon', size=1.5)
for i, (k, t, sub, d, art, at, ad) in enumerate(mega):
    quest('find_' + k, t, sub, [d], [adv('discover_' + k)], [table('supplies'), xp(150)], deps=['mega_start'], x=2.5, y=i * 1.5, shape='circle')
    quest('art_' + k, at, 'Artifact', [ad, '', '&8Needed for the Underside Key.'], [item(art)], [give('research_file'), xp(300)],
          deps=['find_' + k], x=4.5, y=i * 1.5, shape='diamond')
quest('card2', 'Level 2 Access Card', 'Deeper doors',
      ['Found in Neighbourhood houses, special rooms and the Poolrooms vault.'],
      [item('level_2_access_card')], [xp(150)], deps=['find_neighbourhood', 'find_poolrooms'], any_dep=True, x=6.5, y=6)
quest('card3', 'Level 3 Access Card', 'The deepest doors',
      ['Only in the Poolrooms vault and the vault of the Maze.'],
      [item('level_3_access_card')], [give('research_file'), xp(300)], deps=['card2'], x=8, y=6, shape='diamond')
quest('master', 'Master Key', 'Every door, for a while',
      ['Opens any security door. It will not last forever.'],
      [item('master_key')], [xp(150)], deps=['card2'], x=8, y=7.5)
quest('cinema_top', 'Screens 25 to 36', 'The top floor',
      ['The stairs in the cinema lobby climb past two galleries. Reach the third floor.', 'Mind the railings. They are not everywhere.'],
      [adv('cinema_top')], [give('office_coin', 4), xp(120)], deps=['find_cinema'], x=6.5, y=9, optional=True, icon='cinema_seat')
quest('mall_top', 'Out of Order', 'Up the escalators',
      ['The escalators of the mall stopped long ago. Climb them anyway, to the top floor under the skylights.'],
      [adv('mall_top')], [give('office_coin', 4), xp(120)], deps=['find_mall'], x=6.5, y=10.5, optional=True, icon='mall_escalator')
quest('mall_dept', 'Closed for Inventory', 'Behind the shops',
      ['The department stores of the mall have no entrance from the concourse. Find the door marked STAFF ONLY and follow the service corridor.'],
      [adv('mall_dept')], [give('office_coin', 4), xp(120)], deps=['find_mall'], x=6.5, y=12, optional=True, icon='mall_clothes_rack')
quest('mall_memo', 'To All Tenants', 'Mall management',
      ['The mall office is on the first floor, next to the food court side. A memo lies on a lectern. Read it.'],
      [check()], [xp(80)], deps=['find_mall'], x=8, y=10.5, optional=True, icon='minecraft:writable_book')
quest('hotel_top', 'Top Floor, No Vacancy', 'Through the labyrinth',
      ['The hotel corridors twist and the staircases turn. Find your way to the highest guest floor.', 'Only in worlds made with 0.36.0 or newer.'],
      [adv('hotel_top')], [table('supplies'), xp(150)], deps=['find_hotel'], x=6.5, y=4.5, optional=True, icon='hotel_bed')
quest('slide', 'Down the Pipe', 'Wheee',
      ['Some office walls have a round opening with a draught coming out of it. Climb in (use it).', 'You will come out somewhere wet.'],
      [adv('slide_ride')], [give('almond_water', 2), xp(80)], deps=['find_poolrooms'], x=6.5, y=0, optional=True, icon='slide_entrance')
quest('watch', 'The Temporal Watch', 'Four slow seconds',
      ['Slows every creature around you. Rare tiers and the maze vault.'],
      [item('temporal_watch')], [xp(200)], deps=['find_maze'], x=6.5, y=1.5, optional=True)
for i, (w, t, d) in enumerate([('witherfang', 'Witherfang', 'Its cuts wither whatever they touch.'),
                               ('leaden_edge', 'Leaden Edge', 'Every hit drags the target down.'),
                               ('venomtongue', 'Venomtongue', 'Every hit leaves poison behind.'),
                               ('liquid_pain', 'Liquid Pain', 'Do not drink.')]):
    quest('weapon_' + w, t, 'Vault weapon', [d], [item(w)], [xp(200)], deps=['mega_start'], x=6.5 + (i % 2) * 1.5, y=-1.5 - (i // 2) * 1.5, optional=True)

# 6 --------------------------------------------------------------------------------------------
chapter('outposts', 'VI. Research Outposts (optional)', 'Someone tried to build a door', 'minecraft:lever')
quest('out_start', 'Abandoned Outposts', 'Lever, warning, button',
      ['Optional: everything here is in the Overworld or the Nether.',
       'In ancient cities, jungle temples, deep caves and the Nether, a research team built small outposts. A few rare stations stand alone in open country, and a drilling rig or two out on the deep ocean.',
       'Each one holds a gateway into the Office Rooms. The notes inside explain the procedure.'],
      [check()], [xp(20)], deps=['memo'], x=0, y=0, shape='hexagon', size=1.25, optional=True)
quest('out_gate', 'Gateway Online', 'Pull. Wait. Press.',
      ['Pull the power lever, wait out the warnings, then press the red emergency button.', 'Stand back.'],
      [adv('outpost_gateway')], [give('research_file', 2), give('electronic_component', 3), xp(300)], deps=['out_start'], x=2, y=0, shape='diamond', size=1.25, optional=True)
quest('out_ancient', 'Under the Ancient City', '',
      ['The quietest outpost. Sculk all around it.'], [item('minecraft:echo_shard')], [xp(100)], deps=['out_start'], x=2, y=-1.5, optional=True)
quest('out_nether', 'Through the Heat', '',
      ['One team went to the Nether. Their outpost is still standing.'], [dim('minecraft:the_nether')], [xp(60)], deps=['out_start'], x=2, y=1.5, optional=True)

# 7 --------------------------------------------------------------------------------------------
chapter('underside', 'VII. The Underside', 'Beneath the bedrock', O + 'underside_key')
quest('key', 'Underside Key', 'Nine artifacts, one key',
      ['Combine the nine artifacts: one from each megastructure (the cinema ticket and the mall pin count) and the Captain\'s Compass from the SS Fun =).',
       'Use the key on the bedrock beneath the lowest floor of the Office Rooms.'],
      [item('underside_key')], [xp(500)], deps=['art_' + m[0] for m in mega] + ['art_ship'], x=0, y=0, shape='hexagon', size=1.5)
quest('fall', 'Upside Down', 'Fall through',
      ['Below the bedrock there is only void. Fall into it.', 'You land somewhere with grass on the ceiling.'],
      [dim('underside')], [give('almond_water', 3), give('bandage', 3), xp(200)], deps=['key'], x=2, y=0)
quest('unknown', '[Unknown]', 'It burrows',
      ['Twenty seconds after you arrive, the ground starts to shake.', 'Defeat it.'],
      [kill('unknown')], [xp(1500)], deps=['fall'], x=4, y=0, shape='gear', size=2)
quest('noclip', 'Noclip', 'Walls are a suggestion',
      ['The Noclip Sword lets you walk through walls (not floors) for ten seconds.', 'Two portals appear: one back to the office, one to the Overworld.'],
      [item('noclip_sword')], [give('research_file', 5), xp(1000)], deps=['unknown'], x=6, y=0, shape='hexagon', size=1.5)

# 8 --------------------------------------------------------------------------------------------
chapter('level0', 'VIII. Level 0 (optional)', 'Above the top floor', 'minecraft:yellow_wool')
quest('l0_enter', 'Above the Top Floor', 'Break through the ceiling',
      ['Above the highest storey of the Office Rooms there is a ceiling that should not be there.',
       'Something on the other side is waiting. Only one thing.'],
      [dim('level_0')], [give('almond_water', 4), xp(200)], deps=['first_day_done'], x=0, y=0, shape='hexagon', size=1.25, optional=True)
quest('l0_flayed', 'It Does Not Die', 'Do not fight it',
      ['There is a Flayed in Level 0. It cannot be killed. Look at it once, then run.'],
      [observe('flayed', 20)], [xp(150)], deps=['l0_enter'], x=2, y=-1.5, optional=True)
quest('l0_manila', 'Manila', 'The room that is the wrong colour',
      ['Somewhere among the yellow rooms is a room the colour of an envelope.', 'Expect to search for a while.'],
      [adv('level0_manila')], [xp(300)], deps=['l0_enter'], x=2, y=0, optional=True)
quest('l0_bucket', 'A Bucket of Water', 'You will understand',
      ['Bring a water bucket.'],
      [item('minecraft:water_bucket')], [xp(30)], deps=['l0_manila'], x=4, y=1.5, optional=True)
quest('l0_escape', 'Out', 'Through the narrow passage',
      ['Find the way out of the Manila Room. You will bring something back with you.'],
      [adv('level0_escaped')], [give('research_file', 2), xp(800)], deps=['l0_manila', 'l0_bucket'], x=6, y=0, shape='diamond', size=1.5, optional=True)

# 9 --------------------------------------------------------------------------------------------
chapter('bestiary', 'IX. Bestiary', 'Everything that walks the halls', 'minecraft:skeleton_skull')
beasts = [('gaunt', 'The Gaunt', 'Too tall, too thin. Comes with the dark.', 1, 'first_day_done'),
          ('wolfspider', 'Wolfspider', 'Hunts in the offices. Hates light.', 1, 'first_day_done'),
          ('smiler', 'The Smiler', 'Only a smile in the dark.', 1, 'first_day_done'),
          ('colony', 'The Colony', 'Many small things acting as one. It hunts in the warm water of the Poolrooms.', 1, 'first_day_done'),
          ('deathmoth', 'Deathmoth', 'It waits on dark walls. Do not stand too close to a wall you have not looked at.', 1, 'first_day_done'),
          ('hound', 'The Hound', 'Hotel corridors. Do not run, do not stop.', 1, 'find_hotel'),
          ('partygoer', 'Partygoer', 'Hit one and the whole party comes.', 3, 'find_level_fun'),
          ('headless_warden', 'Headless Warden', 'Guards the Sculk Offices. Blind. Listening.', 1, 'first_day_done'),
          ('flayed', 'The Flayed', 'Wire-thin and very, very fast.', 1, 'file'),
          ('aranea_membri', 'Aranea Membri', 'It nests in the ceiling. If you hear scratching above you, do not stand still.', 1, 'first_day_done')]
quest('bestiary', 'Field Notes', 'Know what hunts you',
      ['Every creature of the Office Rooms. Some are worth avoiding more than others.'],
      [check()], [xp(20)], deps=['first_day_done'], x=0, y=3, shape='hexagon', size=1.25)
for i, (e, t, d, n, dep) in enumerate(beasts):
    quest('kill_' + e, t, 'Kill ' + (f'{n}' if n > 1 else 'one'), [d], [kill(e, n)], [xp(100 + 40 * i), table('supplies')],
          deps=['bestiary', dep] if dep != 'first_day_done' else ['bestiary'], x=2 + (i % 5) * 1.5, y=1.5 + (i // 5) * 3)
quest('observer', 'The Observer', 'It is gone when you get close',
      ['You cannot catch it. Just see it.'], [observe('observer', 10)], [xp(150)], deps=['bestiary'], x=2, y=4.5)
quest('slayer', 'Exterminator', 'Ten of each',
      ['Gaunts, wolfspiders and smilers. Ten each.'],
      [kill('gaunt', 10), kill('wolfspider', 10), kill('smiler', 10)], [give('research_file', 2), give('glitched_ingot'), xp(1000)],
      deps=['kill_gaunt', 'kill_wolfspider', 'kill_smiler'], x=10, y=6, shape='diamond', size=1.5, optional=True)

# 10 -------------------------------------------------------------------------------------------
chapter('curiosities', 'X. Curiosities (optional)', 'Rooms that should not be here', O + 'office_coin')
curio = [('cold_storage', 'Cold Storage', 'Frost on the carpet',
          'An office kept freezing cold. The barrels along the walls are freezers. Someone stocked them for a long stay.', 'minecraft:packed_ice'),
         ('last_meeting', 'The Last Meeting', 'Nobody adjourned it',
          'Chairs pushed back, cups still on the table, the projector still on. Read the minutes.', 'minecraft:lectern'),
         ('mail_room', 'The Mail Room', 'Undeliverable',
          'Shelves of parcels nobody collected. Some of them can be opened. The phone rings for nobody.', O + 'parcel_stack'),
         ('rain_room', 'The Rain Room', 'Indoors, it rains',
          'Water falls from the ceiling of an ordinary office. Only the desk in the middle stays dry.', 'minecraft:water_bucket'),
         ('canteen', 'The Canteen', 'Exact change only',
          'Tables, a serving counter and vending machines. They take Old Coins.', O + 'vending_machine'),
         ('arcade', 'The Arcade', 'Insert coin',
          'A dark room full of humming cabinets. Their screens are the only light. Very rare.', O + 'arcade_cabinet')]
quest('curio_start', 'Curiosities', 'Keep your eyes open',
      ['Some ordinary-looking doors lead into rooms that are anything but.',
       'They are rare and appear only in worlds created with version 0.29.0 or newer. Old Coins buy things from vending machines.'],
      [item('office_coin')], [xp(30)], deps=['first_day_done'], x=0, y=3.75, shape='hexagon', size=1.5, optional=True)
for i, (k, t, sub, d, ic) in enumerate(curio):
    quest('room_' + k, t, sub, [d], [adv('room_' + k)], [table('supplies'), give('office_coin', 3), xp(120)],
          deps=['curio_start'], x=2.5, y=i * 1.5, shape='circle', optional=True, icon=ic)
quest('vend', 'Exact Change', 'Feed the machine',
      ['Hand in five Old Coins. The vending machines in a canteen take them one at a time, too.'],
      [item('office_coin', 5, True)], [give('almond_water', 2), xp(40)], deps=['room_canteen'], x=4.5, y=4 * 1.5, optional=True)

# 0.42.0 --------------------------------------------------------------------------------------
chapter('ss_fun', 'XI. The SS Fun =)', 'Now boarding at gate 26', O + 'ship_lifeboat_release')
quest('ss_start', 'Now Boarding', 'Gate 26',
      ['Somewhere in the offices there is a departure lounge that should not be there: rows of seats, a board that says BOARDING, and fog in the windows.',
       'Departure gates are rare and only appear in worlds made with 0.42.0 or newer.'],
      [adv('room_departure_gate')], [xp(80)], deps=['first_day_done'], x=0, y=3, shape='hexagon', size=1.5, icon='boarding_gate')
quest('ss_board', 'All Aboard', 'Welcome to the SS Fun =)',
      ['Right-click the gate. Endless corridors of cabins, a theatre, a casino, a kids\' club. Nobody else is on board.',
       'At least, nobody you can see.'],
      [dim('the_sea')], [table('supplies'), give('office_coin', 4), xp(150)], deps=['ss_start'], x=2.5, y=3, icon='ship_carpet')
quest('art_ship', "Captain's Compass", 'Artifact',
      ["The captain's cabin is on deck 10, behind the bridge, where most staircases do not go. The compass is in the captain's chest.", '',
       '&8Needed for the Underside Key.'],
      [item('captains_compass')], [give('research_file'), xp(300)], deps=['ss_board'], x=4.5, y=6, shape='diamond')
quest('ss_log', 'The Captain\'s Log', 'Day ?',
      ['The captain kept a log. You will find it on a lectern: in the library or in the captain\'s cabin on deck 10.'],
      [check()], [xp(100)], deps=['ss_board'], x=4.5, y=1.5, optional=True, icon='minecraft:writable_book')
quest('ss_mascot', 'Hugs Are Free', 'Do not take it with you',
      ['The kids\' club mascot is still on board. It lives in the vents. It waves. It wants a hug.',
       'Hit it to make it let go.'],
      [kill('mascot')], [give('research_file'), table('supplies'), xp(300)], deps=['ss_board'], x=4.5, y=4.5, optional=True, icon='ship_poster')
quest('ss_escape', 'Abandon Ship', 'Pull the red handle',
      ['The lifeboats hang along the promenade on deck 10. Most staircases stop at deck 8. One does not.',
       'Pull the red handle beside a lifeboat and it drops into the fog.'],
      [adv('ship_escaped')], [give('research_file', 2), give('glitched_ingot'), xp(500)], deps=['ss_board'], x=6.5, y=3, shape='diamond', size=1.5,
      optional=True, icon='ship_lifeboat')

chapter('requisitions', 'XII. Requisitions', 'Facilities takes trades (repeatable)', O + 'electronic_component')
quest('req_start', 'Requisition Desk', 'It is always open',
      ['Facilities trades. Hand in what you have too much of.'],
      [check()], [], deps=['first_day_done'], x=0, y=0, shape='hexagon')
trades = [('req_battery', 'Batteries', [item('electronic_component', 4, True)], [give('portable_battery')]),
          ('req_filter', 'Filters', [item('minecraft:charcoal', 8, True), item('minecraft:paper', 8, True)], [give('gas_filter', 2)]),
          ('req_water', 'Almond Water', [item('minecraft:glass_bottle', 4, True), item('minecraft:sugar', 4, True)], [give('almond_water', 2)]),
          ('req_light', 'Light Sources', [item('minecraft:glowstone_dust', 8, True)], [give('glowstick', 4), give('emergency_flare')]),
          ('req_file', 'Bureaucracy', [item('minecraft:paper', 64, True), item('minecraft:book', 8, True), item('electronic_component', 8, True)],
           [give('research_file')])]
for i, (k, t, tasks, rew) in enumerate(trades):
    quest(k, t, 'Repeatable trade', ['Hand in the items (they are taken) and receive supplies.'], tasks, rew + [xp(20)],
          deps=['req_start'], x=2, y=(i - 2) * 1.5, repeat=True)


# ---------------------------------------------------------------- how to reach each goal (shown in every quest)
HOW = {
    'memo': 'Just tick the checkmark.',
    'shrine': 'Find an Abandoned Office Shrine in the Overworld (rare, natural biomes, away from villages). Its chest holds a sculk sensor - or mine one in a Deep Dark.',
    'sand': 'Any sand from a beach or desert.',
    'facility': 'Find a research outpost (under an Ancient City, beside a Jungle Temple, deep in a cave, in the Nether, rarely as a fenced station on flat land or as a drilling rig over deep ocean), pull the power lever, wait for the warnings to end and press the red button.',
    'rift': 'Enter the Office Rooms any way you like: drop sand onto a placed sculk sensor and walk into the rift, or use an outpost gateway.',
    'almond': 'Desks, cabinets and break rooms in the offices, hidden rooms, hotel rooms and Level Fun presents.',
    'chalk': 'Office loot (tool drawers, cabinets).',
    'bandage': 'Craft: 2 paper + 1 white wool (shapeless). Also in supply closets.',
    'glowstick': 'Office loot, supply closets, hotel rooms, Level Fun presents.',
    'flashlight': 'Security offices, maintenance rooms, industrial control rooms and office tool loot.',
    'battery': 'Maintenance rooms, industrial control rooms and generator halls, research outposts, repaired fuse boxes.',
    'flare': 'Security offices, industrial storage, office tool loot.',
    'noise': 'Security offices, Level Fun presents, office tool loot.',
    'card1': 'Rare office loot, security offices, hotel reception, Neighbourhood houses, industrial control rooms.',
    'totem': 'Very rare: vaults (Maze, Hotel, Poolrooms), secure storage level 3, concealed hidden rooms.',
    'first_day_done': 'Carry 4 Almond Water (nothing is taken).',
    'monitor': 'Craft: iron, glass panes and an electronic component.',
    'fusebox': 'Wait for a POWER warning on the monitors. When the lights die, find the sparking fuse box on the floor the message names and right-click it holding 3 Electronic Components.',
    'component': 'Server rooms, hidden maintenance rooms and security offices, now and then office drawers; plenty in the Industrial Complex, research outposts and Hazard Zone caches.',
    'goggles': 'Craft: iron, an electronic component, glass panes and leather. Also from research outposts and fuse box rewards.',
    'filter': 'Craft: iron nugget, charcoal and paper (vertical). Also maintenance rooms, supply closets, hotel rooms.',
    'mask': 'Office desks and cabinets of almost every room type, hidden rooms, secure storage.',
    'detector': 'Security offices, labs, industrial control rooms, vaults, Hazard Zone caches.',
    'crystal': 'Only inside an active Hazard Zone (follow the monitors and the Special Event Detector). Wear a mask with a filter.',
    'visor': 'Craft: gas mask + night vision goggles + toxic crystal (shapeless).',
    'anomaly': 'Hidden labs, Neighbourhood houses, research outposts, uncommon office loot.',
    'elevator': 'Craft: iron, a piston, an ender pearl and redstone.',
    'file': 'Labs, security offices, vaults, executive offices, hotel safes, fuse box rewards. Flayed and Partygoers sometimes drop one.',
    'table': 'Craft: paper, a book, iron and a crafting table.',
    'heart': 'Open a Research Table with 3 Research Files and choose Heart Research.',
    'heart_max': 'Repeat Heart Research until all five hearts are bought (15 files).',
    'reforge': 'Put a weapon into the Research Table and reforge it (2 / 3 / 4 files per level).',
    'reforge_max': 'Reforge one weapon three times.',
    'lab_': 'Craft it (recipe in JEI or the recipe book). Hidden labs show how it looks.',
    'sec_': 'Craft it (recipe in JEI or the recipe book).',
    'ingot': 'Vaults (Maze, Hotel, Poolrooms), hidden labs, generator halls, research outposts, fuse box rewards, Hazard Zone cores.',
    'rod': 'The vault of the Maze, or very rare special loot.',
    'md_': 'Craft from Glitched Ingots and the Multidimensional Rod (tool and armour shapes).',
    'md_set': 'Carry all four armour pieces.',
    'mega_start': 'Carry a Level 1 Access Card.',
    'curio_start': 'Find an Old Coin: they are common in ordinary containers.',
    'room_': 'Explore the offices: these rooms look like any other room from the corridor. Walking in counts. Only in worlds made with 0.29.0+.',
    'vend': 'Hand in five Old Coins (they are taken).',
    'ss_start': 'Explore the Office Rooms: a departure gate looks like an ordinary room from the corridor. Walking in counts. Worlds made with 0.42.0+.',
    'ss_board': 'Right-click the boarding gate (the door with the 26 above it) in a departure gate room.',
    'ss_log': 'Read the Captain\'s Log on a lectern aboard (library on deck 8, captain\'s cabin on deck 10), then tick the box.',
    'ss_mascot': 'Aboard the SS Fun =): it hides in the ceiling vents. Walk under a dripping vent, or stare down a corridor.',
    'ss_escape': 'Find the one staircase that goes past deck 8, reach the promenade on deck 10 and right-click a red lifeboat handle.',
    'cinema_top': 'Find an Abandoned Cinema and climb the stairs in its lobby atrium to the third floor (screens 25 to 36).',
    'mall_top': 'Climb the escalators of a Dead Mall to the top floor.',
    'mall_memo': 'Read the memo on the lectern in the mall office (floor 1), then tick the box.',
    'art_ship': 'Captain\'s cabin on deck 10 of the SS Fun =): the chest next to the desk. Reach deck 10 through the one staircase that goes past deck 8.',
    'mall_dept': 'Enter one of the four closed department stores of a Dead Mall through the service corridors (STAFF ONLY doors).',
    'hotel_top': 'Climb the staircases of an Abandoned Hotel up to its highest guest floor.',
    'slide': 'Find a round slide opening in an office wall (rare, worlds made with 0.35.0+) and right-click it.',
    'find_': 'Explore the Office Rooms: megastructures fill whole regions on certain storeys. Walking into one counts.',
    'art_': 'In the structure\'s vault or special chest (see the description).',
    'card2': 'Neighbourhood houses, hotel reception, hidden labs and security offices, Poolrooms vault, secure storage level 1.',
    'card3': 'Poolrooms and Maze vaults, hotel vault, concealed hidden rooms, secure storage level 2, Hazard Zone cores.',
    'master': 'Vaults, maintenance and concealed hidden rooms, secure storage level 3.',
    'watch': 'Maze vault, concealed hidden rooms, secure storage level 2 and 3, rare office loot.',
    'weapon_': 'Maze vault, rare and special office loot (Liquid Pain: hidden labs, uncommon loot).',
    'out_start': 'Just tick the checkmark.',
    'out_gate': 'Pull the power lever in an outpost, wait out the warnings, press the red button (be within 32 blocks).',
    'out_ancient': 'Echo shards from Ancient City chests.',
    'out_nether': 'Enter the Nether.',
    'key': 'Craft from the nine artifacts (shapeless): seven megastructures, the mall pin and the Captain\'s Compass.',
    'fall': 'Use the key on the bedrock under the lowest floor of the Office Rooms and fall into the void.',
    'unknown': 'It appears 20 seconds after you arrive in the Underside. Hit it when it surfaces.',
    'noclip': 'Dropped by [Unknown].',
    'l0_enter': 'Break the ceiling above the highest storey of the Office Rooms.',
    'l0_flayed': 'Look at the Flayed in Level 0 for one second.',
    'l0_manila': 'Find and step onto the manila carpet of a Manila Room.',
    'l0_bucket': 'Fill a bucket with water.',
    'l0_escape': 'Pour water into the narrow shaft of the Manila Room and swim out.',
    'bestiary': 'Just tick the checkmark.',
    'kill_': 'Kill it yourself (where it lives is in the description).',
    'observer': 'Look at an Observer for half a second before it vanishes. It watches from far away in the offices.',
    'slayer': 'Kill them yourself, ten each.',
    'req_start': 'Just tick the checkmark.',
    'req_': 'Hand in the items (they are taken). Repeatable.',
}


ICONS = {'find_cinema': O + 'cinema_projector', 'memo': 'minecraft:paper', 'facility': O + 'power_lever', 'rift': 'minecraft:sculk_sensor', 'fusebox': O + 'fuse_box',
         'heart': 'minecraft:golden_apple', 'heart_max': 'minecraft:enchanted_golden_apple', 'reforge': 'minecraft:anvil',
         'reforge_max': 'minecraft:smithing_table', 'out_start': O + 'power_lever', 'out_gate': O + 'emergency_button',
         'out_nether': 'minecraft:netherrack', 'fall': 'minecraft:bedrock', 'l0_enter': O + 'level0_wallpaper',
         'l0_flayed': O + 'flayed_spawn_egg', 'l0_manila': O + 'manila_carpet', 'l0_escape': 'minecraft:water_bucket',
         'bestiary': 'minecraft:writable_book', 'observer': 'minecraft:ender_eye', 'req_start': 'minecraft:writable_book',
         'find_poolrooms': O + 'pool_tile', 'find_maze': O + 'maze_wallpaper', 'find_hotel': O + 'hotel_carpet',
         'find_industrial': O + 'industrial_wall', 'find_neighbourhood': 'minecraft:oak_door', 'find_level_fun': O + 'balloon_red', 'find_mall': O + 'mall_escalator'}


def how(key):
    for k in sorted(HOW, key=len, reverse=True):
        if key == k or (k.endswith('_') and key.startswith(k)):
            return HOW[k]
    raise KeyError(key)

# ---------------------------------------------------------------- reward table + writing
TABLE = {'supplies': ('Office Supplies', [('almond_water', 2, 30), ('bandage', 2, 25), ('glowstick', 3, 25), ('chalk', 1, 15),
                                          ('emergency_flare', 1, 10), ('portable_battery', 1, 8), ('gas_filter', 1, 8),
                                          ('electronic_component', 2, 10), ('research_file', 1, 2)])}


def write():
    root = os.path.normpath(os.path.join(OUT, '..'))
    if os.path.isdir(root):
        shutil.rmtree(root)
    os.makedirs(os.path.join(OUT, 'chapters'))
    os.makedirs(os.path.join(OUT, 'reward_tables'))
    data = {'default_autoclaim_rewards': 'disabled', 'default_consume_items': False, 'default_quest_disable_jei': False,
            'default_quest_shape': 'circle', 'default_reward_team': False, 'detection_delay': 20, 'disable_gui': False,
            'drop_loot_crates': False, 'emergency_items_cooldown': 300, 'grid_scale': D(0.5), 'icon': O + 'almond_water',
            'id': hid('file'), 'lock_message': '', 'pause_game': False, 'progression_mode': 'linear',
            'title': 'Office Rooms', 'version': 13}
    open(os.path.join(OUT, 'data.snbt'), 'w').write(snbt(data) + '\n')
    open(os.path.join(OUT, 'chapter_groups.snbt'), 'w').write(snbt({'chapter_groups': [{'id': hid('group'), 'title': 'Office Rooms'}]}) + '\n')
    for name, (title, entries) in TABLE.items():
        t = {'id': format(tid(name), '016X'), 'title': title, 'loot_size': 1, 'hide_tooltip': False, 'use_title': True,
             'rewards': [{'item': O + i, 'count': c, 'weight': D(w)} if c != 1 else {'item': O + i, 'weight': D(w)} for i, c, w in entries]}
        open(os.path.join(OUT, 'reward_tables', PREFIX + name + '.snbt'), 'w').write(snbt(t) + '\n')
    keys = {}
    for ci, (ck, title, sub, icon, qs) in enumerate(CHAPTERS):
        for q in qs:
            assert q['_key'] not in keys, q['_key']
            keys[q['_key']] = ck
    n = 0
    for ci, (ck, title, sub, icon, qs) in enumerate(CHAPTERS):
        out = []
        for q in qs:
            for d in q['deps']:
                assert d in keys, (q['_key'], d)
            e = {'id': hid('q/' + q['_key']), 'title': q['title']}
            if q['subtitle']:
                e['subtitle'] = q['subtitle']
            e['description'] = list(q['description']) + ['', '&6How: &r' + how(q['_key'])]
            if q['deps']:
                e['dependencies'] = [hid('q/' + d) for d in q['deps']]
            if 'icon' not in q and any(t['type'] in ('advancement', 'checkmark', 'dimension', 'observation') for t in q['tasks']):
                q['icon'] = ICONS.get(q['_key'], O + 'research_file')
            for k in ('shape', 'optional', 'icon', 'size', 'can_repeat', 'dependency_requirement'):
                if k in q:
                    e[k] = D(q[k]) if k == 'size' else q[k]
            e['tasks'] = [dict({'id': hid(f't/{q["_key"]}/{i}')}, **t) for i, t in enumerate(q['tasks'])]
            if q['rewards']:
                e['rewards'] = [dict({'id': hid(f'r/{q["_key"]}/{i}')}, **r) for i, r in enumerate(q['rewards'])]
            e['x'] = D(q['x'])
            e['y'] = D(q['y'])
            out.append(e)
            n += 1
        # 0.25.0: a dark atrium photograph behind the quest map (quest book only)
        xs = [q['x'] for q in qs]
        ys = [q['y'] for q in qs]
        cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
        w = max(max(xs) - min(xs) + 8, (max(ys) - min(ys) + 6) * 16 / 9)
        bg = {'x': D(round(cx, 2)), 'y': D(round(cy, 2)), 'width': D(round(w, 2)), 'height': D(round(w * 9 / 16, 2)),
              'rotation': D(0), 'image': f'office_rooms:textures/gui/quests/atrium_{ci % 3 + 1}.png', 'color': 16777215,
              'alpha': 150, 'order': -1, 'hover': [], 'click': '', 'dev': False, 'corner': False}
        ch = {'images': [bg], 'default_hide_dependency_lines': False, 'default_quest_shape': '', 'filename': PREFIX + ck, 'group': hid('group'),
              'icon': icon, 'id': hid('c/' + ck), 'order_index': ci, 'quest_links': [], 'quests': out,
              'subtitle': [sub], 'title': title}
        open(os.path.join(OUT, 'chapters', PREFIX + ck + '.snbt'), 'w').write(snbt(ch) + '\n')
    print(f'{len(CHAPTERS)} chapters, {n} quests ->', os.path.normpath(OUT))
    # 0.40.0: the same files inside the JAR; the mod merges them into config/ftbquests/quests on first start
    if os.path.isdir(RES):
        shutil.rmtree(RES)
    shutil.copytree(OUT, RES)
    files = sorted(os.path.relpath(os.path.join(dp, f), RES).replace(os.sep, '/') for dp, _, fs in os.walk(RES) for f in fs)
    open(os.path.join(RES, 'index.txt'), 'w').write('\n'.join(files) + '\n')
    print(len(files), 'files ->', os.path.normpath(RES))


if __name__ == '__main__':
    write()
