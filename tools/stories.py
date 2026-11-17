#!/usr/bin/env python3
"""
Survivor Stories: the English books and diaries found in the Office Rooms.

This file is the single source. Running it writes
  - data/office_rooms/loot_tables/tiers/stories.json   (every book as a loot entry)
  - src/main/java/de/officerooms/special/Stories.java  (the same books for lecterns in camps)

A book is (id, title, author, [pages]). Pages are plain text, max ~230 characters each.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")

BOOKS = [
    # ---------------------------------------------------------------- Survey Team Kestrel (Dr. Mara Ellison)
    ("kestrel_1", "Kestrel Field Log I", "Dr. M. Ellison", [
        "Survey Team Kestrel. Field log, entry 1.\n\nWe reproduced the effect at the quarry: sand dropped onto the listening coral. The coral answered. Then the air folded.",
        "Members through: Ellison, Reyes, Okafor, Lindqvist, Abara. Equipment intact. Compasses spin slowly, clockwise, as if something below us is turning.",
        "Environment: offices. Endless. Grey carpet, white walls, ceiling panels at irregular intervals. No windows to the outside. The hum is constant, roughly 100 Hz.",
        "No dust. That bothers me more than anything. Rooms this old should have dust.",
        "Okafor counted floors using the stairwells. He stopped at 60 going up and 40 going down. He says the numbers stop meaning anything after a while.",
    ]),
    ("kestrel_2", "Kestrel Field Log II", "Dr. M. Ellison", [
        "Entry 9.\n\nWater is not a problem. We found sealed bottles in desk drawers. Clear, faintly sweet, smells of almonds. Abara refused it for two days. Then he drank three.",
        "One bottle is enough for a full day. I cannot explain the nutrition. I have stopped trying to explain things in this log. I only write them down.",
        "Lindqvist reports a figure at the end of corridor C (our designation). Tall, thin, completely still. When she walked toward it, it was simply not there.",
        "I told her it was fatigue. I have seen it twice since. I have not told her.",
        "We will descend. Reyes believes the structure has a bottom, and that bottoms have exits.",
    ]),
    ("kestrel_3", "Kestrel Field Log III", "Dr. M. Ellison", [
        "Entry 23.\n\nThe lights end around the fifth floor from the bottom. Not broken. Simply never installed. Our lamps feel small down here.",
        "Okafor heard screaming two rooms over. Not human. High, tearing, and then very fast footsteps. We put out the lamps and lay on the carpet for an hour.",
        "Something passed the door. I saw red light under it, pulsing like a heart. It did not stop.",
        "Reyes found a door that is not a door. A rift in the wall, the same colour as the quarry, framed in black stone that weeps purple. Nobody built it. It was waiting.",
        "Abara wants to go through now. I want to know where it leads before we",
    ]),

    # ---------------------------------------------------------------- Tomas Reyes (mechanic)
    ("reyes_1", "Notes for Whoever Finds the Camp", "Tomas Reyes", [
        "If you're reading this you made it to one of the green rooms or you found our camp. Either way: hi. Sit down. You look terrible.",
        "Practical stuff first. There's NO wood out in the offices. Desks, chairs, all of it is some kind of plastic or metal. Wood only grows in the rooms that went wild. Moss, vines, little trees.",
        "Take what you need but replant. I planted three saplings by the last tree I cut. Maybe someone after you needs them more.",
        "Iron turns up in drawers. Bread too, somehow always fresh. Don't ask. I asked. Nobody answered.",
        "The rooms with villagers in them are real. They trade. They have a cat. The cat does not care that we're lost. I find that comforting.",
    ]),
    ("reyes_2", "Reyes - Building Notes", "Tomas Reyes", [
        "Camp 2. Built a fence ring around the wheat because the golem kept stepping on it. Logs make good benches. Logs make good everything.",
        "The headless ones. Listen to me. They are BLIND. They hear. Crouch, walk slow, don't jump, don't break blocks near them. Lindqvist sneaked past one at arm's length.",
        "When one does find you, it screams first. That's your warning. You have maybe two seconds. Use them.",
        "Okafor killed one with a trap. It dropped a bar of blue metal that flickers when you don't look at it directly. Heavier than it should be.",
    ]),
    ("reyes_3", "Reyes - Last Page", "Tomas Reyes", [
        "We're splitting up. Ellison and Abara are going down to the rift. Okafor, Lindqvist and me stay up here and keep the camp.",
        "If they come back, they come back. If they don't, the camp is here, the bread is in the barrel, and the wheat needs water.",
        "Day whatever. Okafor went to look for them.",
        "Lindqvist says the tall man was standing in the corridor all night. Just standing. I didn't see it. I believe her.",
        "Packing up. If you find this, we didn't go far.",
    ]),

    # ---------------------------------------------------------------- June Albright (night cleaner)
    ("june_1", "June's Diary", "June Albright", [
        "I clean offices at night. That's my whole job. So when I woke up in an office I just thought I'd fallen asleep on shift.",
        "I walked for six hours before I admitted it. Every room is a little bit different and a little bit the same. Like someone described an office to someone who'd never seen one.",
        "I talk to myself now. Out loud. It helps with the hum.",
        "Found a room with a water cooler and cried for twenty minutes. Normal things are the worst things down here.",
    ]),
    ("june_2", "June's Diary (cont.)", "June Albright", [
        "There's a man at the end of the long hallways. Every time. Very tall. Too thin. I can't see his face, there isn't enough light.",
        "He doesn't move. I wave. He doesn't wave back. When I get close he isn't there anymore, and the light where he stood buzzes a bit louder.",
        "I've started looking at the floor when I walk the long hallways. It's easier.",
        "Sometimes I hear someone walking right behind me. I don't turn around anymore. If it wanted me it would have me.",
    ]),
    ("june_3", "June's Diary (last)", "June Albright", [
        "Met people! Real people! Tomas and Signe. They have a camp, a FIRE, actual cooked food. I'm going to cry again.",
        "Signe asked if I'd seen the tall man. I said yes. She just nodded and gave me another potato.",
        "They say there are rooms full of water. Whole offices, flooded to the ceiling, desks and all. There's a lever outside to open the door. Don't let anyone close it behind you.",
        "I think I'll stay with them.",
    ]),

    # ---------------------------------------------------------------- The counter (fragments)
    ("counter_1", "Tally", "?", [
        "Day 12. Floor 102.\nHum: B-flat, maybe.\nDoors counted: 1,404.\nPeople: 0.",
        "Day 19. Floor 97.\nThe stairs go down one floor at a time. Never two. Never skip.\nPeople: 0.",
        "Day 26. Floor 88.\nFound footprints in moss. Bare feet. Small. Going in circles.\nPeople: 0?",
        "Day 31.\nIs it day 31.",
    ]),
    ("counter_2", "Tally (2)", "?", [
        "Day 44. Floor 61.\nA room that is a beach. White sand. Warm. Palms. Turquoise water.\nI am writing this so I believe it later.",
        "There was a chest under the palms. Diamonds. A golden apple that glowed. I sat on the sand for a long time.",
        "Tall rooms. It only happens in the tall rooms. Look for ceilings you can't touch.",
        "Day 45.\nWent back. Couldn't find it. Floor 61 has no beach.\nPeople: 0.",
    ]),

    # ---------------------------------------------------------------- Hendrik Vos (sailor)
    ("vos_1", "Log of H. Vos", "Hendrik Vos", [
        "Thirty years at sea and I get lost in a building. My wife would laugh until she cried.",
        "I keep a heading anyway. North is the way the carpet pile leans. I don't know if that's true. It's something.",
        "Found the paradise room on what I call the eleventh day. Sand, palms, a pool clearer than the Caribbean. Wooden boardwalks. Deck chairs.",
        "Nobody built this for us. Nobody built this for anyone. It was simply waiting.",
        "I left my spare compass in the chest there. If you find it, it still points at the sea.",
    ]),
    ("vos_2", "Log of H. Vos, cont.", "Hendrik Vos", [
        "Went down. Shouldn't have. Below the fifth floor from the bottom there are no lights at all and something hunts there.",
        "Red. All I saw was red. Like coals breathing. It ran on two legs and then on four and it was FAST.",
        "I hid in a cupboard. A cupboard. Thirty years at sea.",
        "It screamed when it lost me. I will hear that until I die.",
    ]),

    # ---------------------------------------------------------------- Expedition / station notes
    ("relay_1", "Relay Station Notes", "Station Operator", [
        "RELAY 4 - operator notes.\nSignal from Kestrel lost after entry 23. Last coordinates: lowest level, sector unknown.",
        "Second team (Heron) requested. Request denied. Quarry sealed. Coral removed.",
        "Note to self: the coral grows back. Every time.",
        "Unofficial: if anyone reads this from the inside, the rifts go both ways. Same colour. Look for the weeping black stone on the lowest floor.",
    ]),
    ("heron_1", "Heron Team - Unsent", "Lt. P. Danjuma", [
        "We came anyway. Unofficially. Four of us, sand and a sensor in a backpack.",
        "Arrived floor 102 like they said. Same hum. Same carpet. Ellison's camp markings on the stairwell: KESTREL -> DOWN.",
        "Found a room overgrown with blue rot. Sculk, Abara would have called it. It ate the walls. A chest in the corner and one of the headless ones standing guard.",
        "There is always exactly one. I don't know who decided that.",
        "Kowalski went for the chest.",
    ]),
    ("heron_2", "Heron Team - Unsent (2)", "Lt. P. Danjuma", [
        "Kowalski is fine. Kowalski is better than fine. Enchanted netherite, books that hum, and two strips of that blue metal.",
        "He says if you get enough of the blue metal and one of those strange rods, you can make tools that cut better than anything at home.",
        "We'll need to kill more of the headless ones. Half of them carry a bar. Half don't. There is no pattern.",
        "I'd rather not.",
    ]),

    # ---------------------------------------------------------------- Abandoned camp fragments
    ("camp_note_1", "Left on the Bedroll", "S. Lindqvist", [
        "Went to get water. Back in ten minutes. Fire's still going. Don't eat my bread.",
    ]),
    ("camp_note_2", "Scratched Pages", "unknown", [
        "they took tomas they took tomas they took",
        "no. nobody took anybody. he walked away. i watched him walk to the end of the hall and stand next to the tall one.",
        "they were the same height.",
    ]),
    ("camp_note_3", "Inventory", "Camp 3", [
        "Bread x14\nIron x9\nAlmond water x6\nWool (grey) x20\nTorches x0 (DON'T USE TORCHES BELOW, IT SEES LIGHT)",
        "Saplings x3 (oak)\nBooks x2 (Ellison's)\nCompass x1 (broken? points down)",
        "Note: the cat came back.",
    ]),
    ("camp_note_4", "For Okafor", "M. E.", [
        "Okafor - we went through the rift on the lowest floor. It works. It goes HOME. Grass, sky, wind.",
        "I'm leaving this at the camp in case you come back here first. The rift is in a room on the very bottom floor, in a frame of crying stone. You can't miss the glow.",
        "Don't stop on the floors above it. Don't light anything. Just go down and through.",
        "Abara didn't",
    ]),

    # ---------------------------------------------------------------- Pages without context
    ("page_1", "Torn Page", "unknown", [
        "...the villagers don't seem to notice the walls at all. They sleep when the sun sets somewhere else. They wake when it rises somewhere else. They know something we don't.",
    ]),
    ("page_2", "Torn Page", "unknown", [
        "...and if you hear knocking, don't knock back.",
    ]),
    ("page_3", "Torn Page", "unknown", [
        "Floor 3. No lights. I can hear the hum but there are no lamps to make it. Something here is humming on its own.",
    ]),
    ("page_4", "Frogs", "June Albright", [
        "Found frogs in one of the green rooms! Two of them. They sat in the moss and looked at me like I was the strange one.",
        "I am the strange one. Fair.",
    ]),
    # ---------------------------------------------------------------- 0.6.0: Hounds, Smilers, Bacteria, Poolrooms hints
    ("hound_1", "Dog Notes", "Tally", [
        "There is a dog down here that is not a dog. Wolf body. Wrong head. A person's head, and it is wet with something.",
        "It took Marcus in two bites. Two. I watched from under a desk and I did not breathe.",
        "Then it found me. I had the soup in my hand, the mushroom soup from the camp, because I was too scared to put it down.",
        "It stopped. It sniffed the bowl. It looked at me with that face and then it just walked away.",
        "I tested it twice more. Bowl in hand, it leaves you alone. Bowl in the bag, it does not. Hold the soup. HOLD THE SOUP.",
    ]),
    ("smiler_1", "Do Not Look Away", "June Albright", [
        "In the dark rooms there is a smile. Only the smile. Everything around it is darker than the dark.",
        "It does not run. It does not need to. Every time I looked back it was one room closer.",
        "Reyes says it touched Okafor's sleeve and Okafor fell over and did not get up. No wound. Just gone.",
        "I lit a torch and held it right up at the thing. Right in its face. It came apart like smoke and I sat down and cried.",
        "Torch in your hand, pointed at it. Not on the wall. In your hand.",
    ]),
    ("bacteria_1", "It Gets Tired", "Vos", [
        "I have heard it three times now. A scream that sounds like it is being pulled out of a throat that is too long.",
        "It does not stalk. It runs. It knows where you are and it comes straight at you and it does not stop.",
        "Except it does. About ten seconds in, it falters. Shakes. Slows. Like whatever is inside it needs to catch up.",
        "That is your window. Do not look back. Do not hide in the first room. Get distance, get doors, get stairs.",
        "Rare, thank God. If you hear it, you have about two breaths to start moving.",
    ]),
    ("pool_hint_1", "The Wet Floors", "Heron", [
        "Somewhere far off the stairs the carpet turns to tile. Cream tile, yellow light, the smell of chlorine and warm water.",
        "Pools. Dozens of them. Halls of them. No one swimming. No one ever swimming.",
        "It is the most peaceful place I have found, and I could not stay longer than an hour. Too quiet. The water is too still.",
    ]),
    # ---------------------------------------------------------------- 0.19.0: the current Office Rooms + the way down
    ("okafor_six", "The Seven Things", "Okafor", [
        "I found Abara's pack. Not Abara. Inside: a list in his handwriting, and the word UNDER underlined until the pencil went through the page.",
        "1. a pearl, at the far end of the drowned maze under the pools\n2. a photograph, in the first empty house on the street that should not be indoors\n3. a thread, in the vault of the yellow maze",
        "4. a key to room 000, in the cellar vault of the hotel\n5. a fuse that hums, in a generator hall\n6. a badge, inside a present at the party that never ends\n7. a ticket stub, from the cinema where nothing is showing",
        "Under the list: 'seven things, one key. the key does not open a door. it opens the floor.'",
        "I don't know what floor he meant. There are so many floors. I am going to find out anyway, because he went first and he did not come back up.",
    ]),
    ("abara_below", "Below the Last Floor", "Abara", [
        "Under the lowest office there is stone that does not break. Picks bounce. Explosions sulk. I scraped the carpet away and lay on it and listened.",
        "Something under it was breathing. Long, slow, like a field breathes in wind.",
        "The key went into the stone like it was soft. The stone forgot it was stone. I fell. Dark for a long time. Then grass.",
        "Grass below me. Grass above me, upside down, with trees hanging from it like teeth. It is daytime there and there is no sun.",
        "I counted twenty breaths before the ground started to shake. If you ever go: count. Then run.",
    ]),
    ("abara_it", "It Is Long", "Abara", [
        "It is longer than a corridor. Black lines, like someone tried to draw a centipede from memory and could not stop drawing.",
        "Every few segments there is a ribcage. Arms instead of legs, with hands. The head is scribbled out. I think there is a face under the scribbles. I think I know it.",
        "It swims in the ground and in the ceiling. When the dirt jumps under you, MOVE. It comes straight up through where you stood.",
        "Sometimes it doesn't dive. Sometimes it crawls with you on the grass. That is when it can be hurt. That is also when it can hurt you.",
    ]),
    ("lindqvist_doors", "Two Doors", "L. Lindqvist", [
        "Abara came back. Not to camp. To the stairwell on 12, walking out of a wall like the wall was fog.",
        "He said it stopped. He said when it stops, it leaves two doors in the field: one yellow, one green. One goes back to the hum. One goes where the sky is.",
        "He chose the hum. He said the sky felt like a trick.",
        "He had a sword I had never seen. When he held it up, he could walk through walls for a few breaths. Never through floors. He said floors are 'the only honest thing down here.'",
        "In the morning he went back to the stone. He said it always comes back, and so does he.",
    ]),
    ("hotel_guests", "Guest Book, Grand Hotel", "various", [
        "Lovely stay! Lobby a bit dusty. - R. & M.\n\nThe dogs in the corridors are not the hotel's. Do not feed them. Do not run. - 214",
        "Asked reception for room 000. The clerk wasn't there. The bell rang anyway.\n\nThere are stairs down behind the lobby. STAFF ONLY. The vault needs a card.",
        "The vault is real. Two chests. One of them has a brass key with a red tag: 000.\n\nThere is no room 000. I checked every floor. Twice.",
        "Checking out. Can't find the exit. Checking out. Can't find the exit. Checking out.",
    ]),
    ("gen_log", "Generator Hall Shift Log", "Plant Crew 3", [
        "Grate doors stay CLOSED. Steam on level 2 is normal. The cores that still glow are fine, the dead ones are fine, the ones that hum without power are not fine.",
        "There is a fuse in hall one that hums when you hold it. Kept it in my pocket a week. Dreamed about grass growing on a ceiling.",
        "The skinless one walks the catwalks. It is very fast and very thin. Hit it and it stumbles, like it forgot what it was doing.",
        "Then it remembers. Louder. Run while it is still remembering.",
    ]),
    ("party_invite", "You Are Invited!", "THE PARTY", [
        "YOU ARE INVITED!\nThere will be cake!\nThere will be balloons!\nThere will be presents for EVERYONE!\n\nPlease do not leave the party.",
        "(in pencil) They come closer while you stand still. They stop at arm's length plus a bit and smile. If you step inside that, they stop smiling.",
        "(in pencil) One of the presents had a badge in it. A yellow smile. Too wide. I put it back. It was in my pocket again the next day.",
        "PLEASE DO NOT LEAVE THE PARTY.",
    ]),
    ("hazard_transcript", "Emergency Broadcast", "Facility System", [
        "TRANSCRIPT, monitors, all floors:\n\nCAUTION. HAZARD EVENT SCHEDULED. A SECTOR WILL BE SEALED. LOCATE AND AVOID THE MARKED STRUCTURE.",
        "(handwritten) When the screens go yellow, you have about a quarter of an hour. When they go red, the air in one of the big places turns green.",
        "(handwritten) Masks work. Filters run out. Goggles help in the dark if you keep them charged. The green crystals only grow while the air is poison.",
        "(handwritten) It never picks the place you are standing in. I think it wants witnesses.",
    ]),
    ("street_watch", "Neighbourhood Watch", "Mrs. P.", [
        "WEEKLY REPORT\n\nStreetlights fine. Lawns fine. Nobody has walked a dog. There are no dogs. There is no outside. The street is indoors and the ceiling is painted blue.",
        "Some houses have someone in them. Do not knock. They answer.",
        "The empty ones have things in drawers. The first empty house on the row had a photograph: a family on the porch. None of them have faces.",
        "I put it back. Next week it was facing the window.",
    ]),
    ("maze_thread", "Yellow Maze Notes", "Okafor", [
        "Yellow walls, damp carpet, no ceiling lights you can trust. The maze has exits on the sides. Mark them. Chalk works.",
        "There is a vault in it. Heavy door. I needed a card. Inside: a spool of thread that keeps unwinding toward the floor, like it wants to go down.",
        "Something without skin hunts these halls now. It used to be something else. Don't let it see your light.",
    ]),
    ("lab_tables", "Research Table Notes", "Dr. A. Voss", [
        "The tables in the labs and the restricted rooms accept what we find. Files. Components. Strange metal. They give back something better.",
        "Test 6: subject's heart beats twice where it beat once. Subject reports feeling 'more there'. Subject is me.",
        "Do not use the tables more than you must. Every upgrade is a little more of this place inside you.",
    ]),
    ("ellison_last", "Kestrel Field Log IV", "Dr. M. Ellison", [
        "Entry 61.\n\nThe bottom five floors have no light at all. Something thin breathes there. It does not like our lamps. It likes our silence.",
        "Abara says the structure has a bottom, and under the bottom a field. He says the field has a keeper.",
        "He showed me a drawing of it. Long. Black lines. A ribcage every few segments. The head is all scribble.",
        "In the scribble there is an eye, and I am almost certain the eye is his.",
        "We are going down together tomorrow. If this log is found, the seven things are in the seven big places. Do not follow us. Follow us.",
    ]),

    # ---------------------------------------------------------------- 0.21.0: the top ceiling and Level 0
    ("case_ceiling", "Case File 0-117", "Facility Records", [
        "CASE 0-117 (CLASSIFIED)\nSubject: maintenance worker, name withheld.\nLast seen: topmost floor, ladder against the ceiling, removing a tile.",
        "Colleague's statement: 'He pushed the tile up and there was no roof behind it. Just more ceiling, from the wrong side. Then he was not on the ladder any more.'",
        "Search of the top floor: no hole, no damage. The tile he removed was back in place. It was warm.",
        "Status: MISSING. File cross-referenced with 0-031, 0-064, 0-090, 0-102. Same floor. Same tile pattern. Do not repair the ceiling up there. Do not touch it.",
    ]),
    ("survivor_level0", "What Is Above", "J. Marsh (survivor)", [
        "I broke through the top ceiling because I thought there had to be a roof, and a roof means sky.",
        "There was no sky. There was a room exactly like ours, only yellower, quieter and much bigger. Four floors of it, stacked, all the same. The carpet was wet.",
        "Nothing breaks there. I tried. My hands were too tired to even scratch the wallpaper.",
        "I was not alone. The thin black one was there too. It does not die. I hit it with everything I had. It just kept walking.",
        "I got out. I am not going to write down how. I am going to write down this: the yellow walls are not your friends, but the paler ones might be.",
    ]),
    ("survivor_level0b", "The Paler Room", "J. Marsh (survivor)", [
        "Fifteen minutes. That is how long it took me to find it, and that was lucky. Paler walls than the rest, the colour of an old folder. You get in through a gap you would miss if you were running.",
        "There was a chest. One bucket of water. Nothing else. I nearly threw it at the thing chasing me.",
        "Under the big table, in the corner, there is a hole in the floor. Too narrow to walk. Too narrow to crawl. Not too narrow to swim, if it were full.",
        "Fill it. Jump in. Swim. Don't stop. There is a light at the other end.",
    ]),
    ("research_level0", "Project LIMINAL - Ceiling Study", "Office of Research", [
        "SUMMARY OF 23 YEARS OF OBSERVATIONS (heavily redacted)",
        "The topmost ceiling of the Office Rooms is not a boundary. It is a membrane. Breaking it transfers the person to a secondary space designated LEVEL 0.",
        "Level 0 reproduces our corridors at a larger scale and with fewer exits. Recovered survivors describe four stacked floors, holes in the floor of certain halls, and oversized furniture 'built for someone much taller'.",
        "One entity is present at all times. It matches the description of the skinless walker. All attempts to kill it failed.",
        "Exit: rare rooms in manila colour. Every survivor mentions water. Research continues. Volunteers continue to be needed.",
    ]),
    ("missing_poster", "MISSING", "a notice board", [
        "MISSING\n\nT. Albrecht, 34\nLast seen: top floor, east stairwell\n'Said he could hear lights humming above the ceiling.'",
        "MISSING\n\nR. Osei, 29\nLast seen: top floor, storage room\nLeft a crowbar and a note: 'it sounds hollow up there'.",
        "MISSING\n\nThe search party for T. Albrecht.\n(all six)",
        "If you hear humming above the ceiling, do not answer it.",
    ]),
    ("threshold_note", "Threshold", "unknown", [
        "There is a hall up there with square holes in the floor, rows of them, perfectly black.",
        "I dropped a coin into one. It landed on carpet, one floor down. I heard it.",
        "Then I heard something pick it up.",
    ]),
    ("security_log0", "Security Log - Top Floor", "Night Shift", [
        "02:14 Noise from above ceiling, sector 7. Like footsteps on wet carpet.\n02:31 Same.\n02:47 Guard Peters went up the maintenance ladder to check.",
        "03:05 Peters not responding.\n03:06 Ladder empty. Ceiling intact.\n03:40 Footsteps above ceiling. Two sets now.",
        "04:00 Recommendation: seal the maintenance ladders. Recommendation denied. Reason given: 'They come back sometimes. With things.'",
    ]),

]

SHRINE_BOOKS = [
    ("shrine_1", "Found Near The Ruin", "unknown", [
        "If you are reading this, you found the place where the office came through. It should not be here. It was not built here.",
        "Inside the chest: the listening coral and the sand. That is all it takes. That is the joke.",
        "Do not combine them. You won't survive.",
    ]),
    ("shrine_2", "Last Warning", "Dr. M. Ellison", [
        "We did it at the quarry. Sand on the coral. The air folded and we walked in, five of us.",
        "I am the only one who came back, and I am not sure I came back all the way. I still hear the hum at night.",
        "There are things in the halls. One with a wolf's body and a man's face. One that is only a smile. One that screams.",
        "Those who dare will never leave.",
        "Bury the chest. Burn this book. Go home.",
    ]),
    ("shrine_3", "Prayer", "unknown", [
        "Grey carpet, white walls, keep us.\nHumming light, keep us.\nLong corridor, keep us.\nDo not open, keep us.",
        "We left the offering. We did not open the door.",
        "Someone will. Someone always does.",
    ]),
]

POOL_BOOKS = [
    ("pool_1", "Lifeguard Log", "unknown", [
        "Shift 1. All pools clear. No swimmers. There are never swimmers.",
        "Shift 4. The small pool in the side hall is deeper than it looks. Much deeper. I dropped a lantern in and watched it sink out of sight.",
        "Shift 9. There are corridors under the water. I swam in two turns and came back with my lungs on fire.",
        "Shift 12. Someone hid something at the end of it. Chests. I saw the glint. Bring something to breathe with.",
    ]),
    ("pool_2", "Wet Pages", "Heron", [
        "Under the floor there is a maze full of water. The walls are the same cream tile. Every turn looks like the last one.",
        "Count your turns. Keep one hand on the left wall. There are pockets of air in the ceiling if you look up.",
        "I made it to the end. I could not carry it all back. Whoever you are: it is still there.",
    ]),
]


def page_json(text):
    return json.dumps({"text": text}, ensure_ascii=False)


def book_nbt(title, author, pages):
    # SNBT for set_nbt: {title:"..",author:"..",pages:['{"text":".."}'],resolved:1b}
    def q(s):
        return '"' + s.replace('\\', '\\\\').replace('"', '\\"') + '"'

    def sq(s):
        return "'" + s.replace('\\', '\\\\').replace("'", "\\'") + "'"
    return "{title:" + q(title) + ",author:" + q(author) + ",generation:2,resolved:1b,pages:[" + ",".join(sq(page_json(p)) for p in pages) + "]}"


def write_loot(books=None, name="stories"):
    books = BOOKS if books is None else books
    entries = []
    for (bid, title, author, pages) in books:
        assert len(title) <= 32, title
        for p in pages:
            assert len(p) <= 256, (bid, len(p))
        entries.append({"type": "minecraft:item", "name": "minecraft:written_book", "weight": 1,
                        "functions": [{"function": "minecraft:set_nbt", "tag": book_nbt(title, author, pages)}]})
    path = os.path.join(ROOT, "src/main/resources/data/office_rooms/loot_tables/tiers/" + name + ".json")
    json.dump({"type": "minecraft:chest", "pools": [{"rolls": 1, "entries": entries}]}, open(path, "w"), indent=1, ensure_ascii=False)


def jstr(s):
    return '"' + s.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n') + '"'


def write_java():
    lines = ["package de.officerooms.special;", "",
             "/** Generated by tools/stories.py - the English survivor books (do not edit by hand). */",
             "public final class Stories {", "",
             "    public record Book(String id, String title, String author, String[] pages) {", "    }", "",
             "    public static final Book[] ALL = {"]
    for (bid, title, author, pages) in BOOKS:
        lines.append(f"            new Book({jstr(bid)}, {jstr(title)}, {jstr(author)}, new String[]{{")
        for p in pages:
            lines.append(f"                    {jstr(p)},")
        lines.append("            }),")
    lines += ["    };", ""]
    for (arr, books) in (("SHRINE", SHRINE_BOOKS), ("POOL", POOL_BOOKS)):
        lines.append(f"    public static final Book[] {arr} = {{")
        for (bid, title, author, pages) in books:
            lines.append(f"            new Book({jstr(bid)}, {jstr(title)}, {jstr(author)}, new String[]{{")
            for p in pages:
                lines.append(f"                    {jstr(p)},")
            lines.append("            }),")
        lines += ["    };", ""]
    lines += ["    private Stories() {", "    }", "",
              "    public static Book byId(String id) {",
              "        for (Book b : ALL) {", "            if (b.id().equals(id)) {", "                return b;", "            }", "        }",
              "        return ALL[0];", "    }", "}", ""]
    open(os.path.join(ROOT, "src/main/java/de/officerooms/special/Stories.java"), "w").write("\n".join(lines))


if __name__ == "__main__":
    write_loot()
    write_loot(SHRINE_BOOKS, "shrine_stories")
    write_loot(POOL_BOOKS, "pool_stories")
    write_java()
    print(len(BOOKS), "books,", sum(len(b[3]) for b in BOOKS), "pages")
