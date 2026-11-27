# Office Rooms – Changelog
Newest version first. Versions before 0.6.0 were early development builds.

---

## 0.43.0

**A new megastructure in the Office Rooms: a three-floor shopping centre that nobody has shopped in for a very long time.**

## The mall
- As rare as the other megastructures; worlds created with 0.43.0 or later only (see below).
- Two concourses cross in the middle. Above them the floors are open: atriums under grey skylights, galleries with
  glass railings and columns, a dry fountain with coins under the crossing.
- **Escalators** (not running) climb across the east–west atrium: floor 0 → 1 and floor 1 → 2.
- **Shops** line the concourses on every floor: shuttered, papered over "for lease", dark, or still stocked —
  clothes (with mannequins), shoes, televisions showing static, books, toys, pets (murky aquariums), a hair salon.
  Every shop has a stockroom with a back door.
- **Food court** under the north atrium: stalls with counters, fryers and glowing menu boards, rows of tables.
- **Service corridors** behind the shops (low, cinder block, flickering caged lights), reached through STAFF ONLY doors.
  They lead to the mall office, the security room and four **closed department stores** that have no entrance from the mall.
- The main entrance doors at the south end. It is night outside. It has been night for some time.
- Ceiling speakers play a few bars of tired music now and then, or the chime of an announcement nobody makes.
- **Aranea Membri** nests above the ceiling tiles (at most one per mall floor; config `[aranea_membri] mallChance`).
- About 57 new blocks, all drawn for the mod. Food court tables and chairs, kitchens, desks and chairs come from
  **Refurbished Furniture** and **OfficeCraft** when installed, own blocks or vanilla otherwise.
- Loot: tills, stockrooms, electronics, toys, food stalls, kiosks, the mall office (with a memo to all tenants),
  security room, department stores.

## Other
- `/officerooms locate mall`, `/officerooms goto mall`, `/locate structure office_rooms:dead_mall`.
- Quests (chapter V, optional): "The Dead Mall" and "Closed for Inventory". Milestones `discover_mall`, `mall_dept`.
- **Fix: new content stays in new worlds.** The content version of a world is now stored with the world when it is
  created. Before, the version came from the dimension file of the mod every time a world loaded, so an update could
  switch new content on in old worlds. Worlds from before 0.43.0 keep content version 2 (departure gates, no mall).


## 0.42.0

**A new dimension: an endless, foggy sea with lone cruise ships — and something that lives in their ceilings.**

## Getting there
- **Departure gates** are very rare special rooms in the Office Rooms (new worlds only): terminal carpet, rows of seats,
  a departure board ("SS FUN=) BOARDING") and a boarding gate. Use the gate to board.
- Every gate leads to a ship in **The Sea**, a dimension of nothing but water and thick fog. Ships lie 400+ blocks apart.

## The ship
- 300 blocks long, 12 decks: crew decks with the engine hall, catwalks, laundry, galley, mess, medical bay and cold room;
  passenger decks with endless carpeted corridors and cabins; embarkation lobby, atrium, shops, purser, theatre,
  casino, bar, dining room, lounge, kids' club, library, games room, buffet and spa.
- The **promenade deck** with the lifeboats is hard to reach: only one of the three stair cores continues above deck 8.
  The others end at barricades. Fire doors close off parts of the corridors.
- Bridge, captain's cabin, suites, a sun deck with pool, the funnel and the mast.
- About 70 new blocks: carpets, wainscot, handrails, cabin doors, portholes, life rings, fire hoses, vents, lamps that can go out,
  exit signs, departure board, boarding gate, lifeboat release …
- Furniture from **MrCrayfish's Furniture Mod: Refurbished** and **OfficeCraft** when installed, vanilla otherwise.
- Loot in cabins, crew rooms, stores, purser, casino, galley, kids' club, captain's cabin and medical bay. Logbooks and notices on lecterns.

## The Mascot
- Hides in ceiling vents. It drips. If you walk under it, it drops down behind you.
- Sometimes it stands at the end of a corridor and waves. It only comes closer when you are not looking.
- It hugs. While it hugs you, you are slowed and squeezed — **hit it to make it let go**.
- It turns off the lights it passes. If it loses you, it crawls back into a vent.

## Getting back
- Pull the **lifeboat release** on the promenade: the lifeboat drops you back into the Office Rooms, far away from the gate.
  The first escape gives 2 Research Files.

## Other
- Quest chapter **"XI. The SS Fun =) (optional)"** (FTB Quests), milestones `ship_boarded`, `ship_escaped`, `room_departure_gate`, `mascot_kill`.
- `/locate structure office_rooms:ss_fun`, `/officerooms locate ship|departure_gate`, `/officerooms goto ship|departure_gate`,
  `/officerooms ship board|lifeboat|info|mascot`.
- Config `[ss_fun]`: `gateChance`, `mascotHealth`, `mascotHugDamage`, `mascotChance`, `mascotMax`. Mascots follow `[spawning] enabled` and Peaceful.
- Departure gates only generate in worlds created with 0.42.0 or later (existing worlds are not changed).


## 0.41.2

- **Fix: `[spawning] enabled = false` now really turns creature spawns off** (reported by players). Before, it only stopped the
  regular spawn cycle; these still spawned creatures and now follow the switch (and Peaceful):
  - residents of the Neighbourhood and the Headless Warden in sculk offices (placed when chunks generate),
  - visitors waiting in front of elevator doors,
  - Observers,
  - Gaunts during blackouts and creatures in hazard zones.
- Creatures that already exist stay in the world. Story encounters (the Flayed in Level 0, the boss of the Underside) are not affected.


## 0.41.1

- **A bit brighter:** the Office Rooms are now 20 % darker than vanilla light instead of 30 %. Dark storeys, blackouts and night vision work as before.
- **Aranea Membri** lurks in cinema ceilings more often: chance per spawn check 0.04 → 0.12, and one per cinema floor instead of one per cinema.
  Configs that still have the old default are updated automatically (`balanceVersion` 6).
- **Commands:** `/officerooms locate` now also knows the curiosities: `cold_storage`, `last_meeting`, `mail_room`, `rain_room`, `canteen`, `arcade`
  (`goto` already had them).
- **Faster search for special rooms** in `locate` and `goto` (villager outpost, lush cave, oasis, exit room, curiosities …): only the nearest
  candidates are furnished. Before, one search could freeze the server for up to 20 seconds; now it takes 1–4 seconds.


## 0.41.0

**Megastructures work with `/locate` and structure-finding mods.**

- The seven megastructures of the Office Rooms are now known to Minecraft's structure system, so the vanilla
  `/locate structure` command and mods such as **Explorer's Compass** can find them:
  - `office_rooms:poolrooms`, `office_rooms:neighbourhood`, `office_rooms:the_maze`, `office_rooms:abandoned_hotel`,
    `office_rooms:industrial_complex`, `office_rooms:level_fun`, `office_rooms:abandoned_cinema`
  - The shrine in the Overworld (`office_rooms:abandoned_office_shrine`) already worked.
  - Search inside the Office Rooms dimension. The result points to the centre of the 160-block region of the
    megastructure; the height is up to you (they sit on different storeys) - `/officerooms locate` gives the exact
    entrance with height.
  - These markers are invisible and place no blocks. No other structures are added to the Office Rooms.
  - In worlds from before 0.41.0, areas that were already explored are not found this way (their chunks carry no
    markers); `/officerooms locate` still finds everything.
- Research outposts are placed when a chunk is generated (they need the terrain around them), so they cannot be
  predicted by `/locate`. New: `/officerooms locate outpost` lists all outposts generated so far, in all dimensions.
- New: `/officerooms locate shrine` (same search as the vanilla command, from anywhere).
- Fix: `/officerooms locate all` in a far, unexplored area could freeze the server for more than 60 seconds and make
  it shut down (it planned hundreds of regions at once). It now searches a smaller area for the small special rooms.


## New in Office Rooms 0.40.0 (beta)

**The quest book installs itself.** A mod download on CurseForge or Modrinth only brings the JAR, so the quest files now travel inside it.

- When **FTB Quests** is installed, Office Rooms adds its quest book on start (`config/ftbquests/quests`), before FTB Quests loads it.
  - **No quest book yet:** everything is copied, and the book is called "Office Rooms".
  - **A modpack with its own quest book:** only the Office Rooms chapters (files `office_rooms_*`) and the chapter group "Office Rooms" are added, below the pack's own groups. The pack's chapters and settings are never changed.
  - It runs once per version. When an update of Office Rooms brings new quests, only its own chapters are renewed.
  - To manage the quests yourself, set `installQuests = false` in `config/office_rooms-common.toml` (section `[quests]`).
- The quest files are now called `office_rooms_*.snbt`, so they never collide with a pack's files. Quest IDs are unchanged; progress is kept.
- Chapter order: "X. Curiosities" now comes before "XI. Requisitions".
- The version in the log at start is correct again (it said 0.18.1).

---

## New in Office Rooms 0.39.1 (beta)

Release preparation. Nothing changes in your worlds.

- **Mod list:** Office Rooms now has a logo, a description, credits and its license in the Forge mod list.
  - Author: Apollo. Developed with Claude (Anthropic).
  - License: All Rights Reserved (see `LICENSE`).
- **Cleanup:** removed seven unused old creature models and renderers (all creatures except the wolfspiders have used GeckoLib models for a long time) and an unused spawner routine.

---

## New in Office Rooms 0.39.0 (beta)

**Two new, rare kinds of research outpost in the Overworld.** Inside, they work like the others: power lever, warnings, monitors, red emergency button, gateway into the Office Rooms, a chest and a logbook with the procedure.

- **Field Station** (on land): a weathered station building standing alone on flat, dry ground.
  - Stone-brick foundation, barred windows, a radio antenna on the roof.
  - A gravel apron in front of the door, a sign "RESEARCH STATION / AUTHORIZED PERSONNEL ONLY" and a lantern.
  - A chain-link fence around it, open in front of the door, with a few panels missing.
  - Never on water or ice, never inside a village or another structure, only where the ground is fairly flat.
  - Rare: a chance of 1 in 2500 per new chunk, and many of those spots are refused.
- **Drilling Rig** (on the ocean): a station on a rig over large, deep ocean (at least 15 blocks of water under the whole rig).
  - Legs down to the sea floor, a railed steel deck, a derrick with a lamp on top, barrels and lanterns.
  - A ladder from 8 blocks below the waterline up to the deck.
  - Rare: 1 in 900 per new ocean chunk, only where the water is deep everywhere.
- New logbooks: "Field Station - Logbook" and "Rig 4 - Shift Log".
- The quest "Abandoned Outposts" and its hint mention the new stations.
- Test commands: `/officerooms outpost surface|ocean` builds one in front of you. `/officerooms outpost try_surface|try_ocean` runs the real placement check for the chunk you are in and tells you why it refuses.

---

## New in Office Rooms 0.38.1 (beta)

Fixes from walking the parts that had never been tested on foot.

- **Poolrooms:** some Poolrooms could not be built at all (a pool in a narrow hall ran past the region). With seed 4711 that was 7 Poolrooms in the area tested. `/officerooms locate poolrooms` failed with an error. Pools now always fit inside their hall.
- **Hotel L-stairs:** they had five steps for a six-block storey, so you had to jump at the top. The sixth step now lies in the floor above. In one of four rotations the second flight was even made of **beds** instead of steps (since 0.36.0); fixed.
- **Level 0 and Maze ramps:**
  - the corner step faced the wrong way (a full block edge when you came from below);
  - the ceiling was cut open one step too late, so you hit your head;
  - the ceiling beside the steep part is open now too, so you no longer get stuck when you drift sideways.
  - Both ramps can now be walked all the way up.
- **Quest "Down the Pipe":** says to use (right-click) the slide opening.

---

## New in Office Rooms 0.38.0 (beta)

**The cinema joins the way down, and the quest book catches up with the new content.**

- **Underside Key:** it now takes **seven** artifacts. The **Ticket Stub** from the Abandoned Cinema is the seventh (box office and lost-and-found chests). Okafor's survivor book "The Seven Things" lists it too.
- **Quests** (120 instead of 114, update the quest files from `quests/`):
  - Chapter V, Megastructures:
    - **The Abandoned Cinema** (discover it);
    - **Ticket Stub** (artifact);
    - **Screens 25 to 36** (reach the top cinema floor, optional);
    - **Top Floor, No Vacancy** (reach the highest guest floor of the hotel labyrinth, optional);
    - **Down the Pipe** (ride a slide from an office wall, optional).
  - Chapter VII: **Underside Key** needs all seven artifacts.
  - Chapter IX, Bestiary: **Aranea Membri**.
- New hidden milestones for the quests: `cinema_top`, `hotel_top`, `slide_ride`.

---

## New in Office Rooms 0.37.1 (beta)

Fixes from the full test of the cinema multiplex.

- **Cinema stairs:** every flight now ends on a landing with railings. Walking straight on at the top no longer drops you 12 blocks into the atrium. The landing at the foot of the upper flights also has railings on its open sides.
- **Aranea Membri in the cinema:** it now nests on the cinema floor you are on. Before, it could only appear on the ground floor.
- **Hazard Zone:** starting a zone could fail with an error when the Abandoned Cinema was picked (it had no name in the list). This bug existed since 0.31.0. The zone now covers all three cinema floors.
- **Elevators:** the check that lets a megastructure catch a ride uses the current number of storeys of the Hotel, Industrial Complex, Level Fun, Neighbourhood, Poolrooms and Maze. Rides still stop on a random floor in the direction you go.

---

## New in Office Rooms 0.37.0 (beta)

**The Abandoned Cinema is now a three-floor multiplex** (new worlds), on the same 160 × 160 footprint as before.

- **Three cinema floors**, each 12 blocks high (six office storeys in total). Every floor has its own two corridors and twelve auditoriums, so the cinema now has **36 screens**, numbered 1–36 over the corridor doors. Every hall rolls its own kind (empty screen, static, dark, wrecked, figure on the screen).
- **The lobby is an atrium** over all three floors: the box office, queues and concession stands stay on the ground floor, and above them **galleries** run along the lobby walls on floors 2 and 3, with dark wooden railings and dim lights underneath. The ceiling is high above, about 35 blocks up.
- **Freestanding stairs** in the atrium:
  - two flights from the lobby floor up to the first gallery (along the north side);
  - two flights from the first gallery up to the second (along the south side). They start from a small landing at gallery level.
  - The railings are open only where a flight begins or ends. Everywhere else it is a long way down.
- Posters on every floor. On the upper floors a marquee on the north wall says **"SCREENS 13 TO 24"** or **"SCREENS 25 TO 36"**.
- Lost-and-found chests can turn up in all 36 halls.
- **Office doors** now open into all three cinema floors through the outer wall.
- Elevator rides and hazard events treat the cinema as six storeys.

---

## New in Office Rooms 0.36.1 (beta)

When Refurbished Furniture is installed, the Hotel and the Neighbourhood (new worlds) use its furniture instead of vanilla stand-ins. Without the mod, nothing changes.

- **Hotel:**
  - lounge and lobby sofas → red sofas (light blue on the teal-carpet floors);
  - armchairs → red sofa pieces;
  - dining tables and chairs → dark oak tables and chairs;
  - bathrooms → white toilet, basin and a real two-block bath;
  - wardrobes and linen-closet shelves → dark oak storage cabinets;
  - luggage and basement crates → spruce crates.
- **Neighbourhood:**
  - sofas → gray sofas;
  - tables → oak tables;
  - table lamps → white lamps (switched on where a lantern used to burn);
  - TVs → televisions on oak drawers;
  - wardrobes → oak storage cabinets;
  - mailboxes → oak mail boxes;
  - garden fences → spruce lattice fences.
  - The park benches stay as they are.

---

## New in Office Rooms 0.36.0 (beta)

All changes apply to new worlds only. Old worlds keep their megastructures exactly as they were.

### The Hotel becomes a labyrinth
- Seven storeys instead of four: the basement with the vault, the lobby, and five guest floors.
- Each guest floor is a grid of small 9 × 9 rooms. Narrow corridors three blocks wide wind between them like a maze, with dead ends, loops and kinks where the corridor swerves around a solid block. There are no big halls.
- Every room has a purpose:
  - guest rooms (bed, nightstands, wardrobe, armchair, dresser with TV, rug, luggage);
  - twin rooms;
  - bathrooms (tiled floor, tub, toilet, sink, towels);
  - linen closets (shelves, piles of laundry, crates, a housekeeping cart);
  - lounges (bookshelves, sofas around a rug, plants);
  - dining rooms (tables with chairs);
  - trashed rooms, rooms with a static TV, rooms with a ringing phone, barricaded rooms behind iron doors ("DO NOT DISTURB"), and a few empty ones.
- Some rooms can only be reached through another room via a connecting door.
- Signs above the doors show room numbers (e.g. "ROOM 214"), or LINEN, BATH, LOUNGE or DINING.
- Warm sconces sit on the blind sides of corridor bends. Ice machines and housekeeping carts stand in dead ends.
- Odd guest floors have teal carpet, even ones red.
- L-shaped staircases (two per floor, randomly placed and turned) climb one floor at a time. The upper floor has an opening with a railing. The lobby has two of these staircases up to the first guest floor.
- The basement-to-lobby staircase and the vault are unchanged.

### Industrial Complex
- Eight levels instead of five. The generator halls now reach through four levels.
- New rooms:
  - tank rooms (two round weathered copper tanks);
  - foundries (a row of lit blast furnaces, glowing lava vats, coal heaps);
  - locker rooms (rows of lockers and benches).

### Level Fun
- Four storeys instead of one, linked by straight birch staircases. Each storey has its own layout.
- New cells:
  - ball pits (inflatable rim, coloured balls);
  - disco floors (magenta and cyan checkerboard, jukebox, streamers);
  - banquet tables with cakes and gifts;
  - dark cells (lights off, "=)" painted on the walls).
- Gift chests on every storey. The artifact stays on the first storey.

### Neighbourhood
- The hall is six storeys high. The houses now have two floors: stairs at the back lead up to a bedroom floor with two beds, a wardrobe and a table lamp.
- Small differences between the identical houses:
  - boarded-up windows;
  - doors left open;
  - a lantern still burning in some windows;
  - mailboxes;
  - street lamps along the sidewalks (a few still lit).
- Some of the houses with a resident are now an empty playground (sandbox, swing, bench, tree). The resident waits there instead.
- Half of the houses without a resident have a second chest upstairs.

---

## New in Office Rooms 0.35.2 (beta)

- The cinema lobby wallpaper is now a classic cinema wallpaper: deep burgundy with faded gold art-deco diamonds and small sunburst fans, panel seams and some grime. It replaces the teal-and-gold fan pattern.

---

## New in Office Rooms 0.35.1 (beta) – Slide pipes

- **Water slides in the Poolrooms are now real pipes:**
  - 1–3 round, cream-coloured tubes side by side stick straight out of the outer wall, two blocks above the water.
  - Each is two to three blocks long, with an open, slanted mouth facing into the hall.
  - Water dribbles out of the mouth into a deep pool below.
  - The stepped tubes on legs are gone.
- The slide openings in the offices lead to the water in front of these pipes.
- **Slide Opening in the offices:** a flatter model that sits against the wall like a round hole with a white collar. It only ever spawns on walls.

New blocks: Slide Pipe, Slide Pipe End.

---

## New in Office Rooms 0.35.0 (beta) – Poolrooms rework

The Poolrooms now take up **eight storeys** (was five).

- **Huge halls:** far fewer dividing walls, so most halls are 60–150 blocks across, 32 blocks high, with the gallery 12 blocks up. Pools are sized to the halls, and whole halls are flooded more often.
- **Deep pools:** a shallow step at every edge, then deeper toward the middle.
  - Every hall has its own maximum; diving halls reach **ten blocks** deep.
  - Sea lanterns light the bottom.
  - The underwater labyrinth lies just below the hall floor.
- **Arches in the water:**
  - The arches in the walls are now round.
  - About a third of them stand in water, with a channel running through them.
  - Deep pools get an arcade of pillars rising from the bottom, with lintels over the water.
- **Water slides:** one to three tubes side by side stick out of the outer walls a few blocks up, bend down on legs and end over a deep landing pool. The chute is slippery like ice.
- **Slide openings in the offices (rare, new worlds):**
  - A **Slide Opening** sits on a wall now and then: in about 1 of 170 corridors and 1 of 250 rooms.
  - Right-click it and you climb in. The screen goes dark, the tube rumbles and shakes, and after three seconds you shoot out of a water slide into a landing pool of the nearest Poolrooms.
  - Everyone who uses the same opening ends up in the same Poolrooms; which slide exit is random.
- **Stainless handrails** in the classic Poolrooms style: in runs along pool edges, and some just standing on the tiles for no reason.
- **Pool ladders:** steel ladders at the edges of deep water.
- **Small square mosaic tiles** everywhere, and more light near the floor.
- **The stairs to the gallery stay as they are.**

New blocks: Water Slide, Slide Tube, Pool Ladder, Pool Handrail, Slide Opening.

---

## New in Office Rooms 0.34.0 (beta) – Ramps, pit fields, a three-level Maze

### Carpeted ramps
- New block **Carpeted Ramp**: one continuous slope of the same carpet as the floor. It walks like stairs.
- All ramps between Level 0 floors and all split levels in The Maze use it. The last step now lies in the upper floor, so you walk straight up without a jump.

### Pit fields
- **Level 0:** the old Threshold rooms are now pit fields (on floors 2–4).
  - A wide room whose floor is a grid of deep square pits with narrow carpet walkways between them.
  - Below each pit is a seven-block shaft. At the bottom the shafts are joined by low openings, so you can walk out of the floor below.
- **The Maze:** the same pit fields, on its upper levels.

### The Maze: three levels
- The Maze now has **three levels** on top of each other (four office storeys high). Each level is its own labyrinth with all the Level 0 features of 0.33.0.
- **Between the levels:**
  - three ramps up per level;
  - pit fields (a 24 × 24 hall of pits) dropping into a grid of chambers on the level below;
  - holes through the floor.
- **Exits:** 3–5 exits lead from the lowest level back into the offices.
- **The vault:** it stands on a **random level at a random spot**. `/officerooms goto maze_vault` shows which maze floor.

---

## New in Office Rooms 0.33.0 (beta) – More Level 0 in Level 0 and The Maze

The classic Level 0 features: mono-yellow wallpaper, damp carpet, a drop ceiling, inconsistently placed fluorescent lights, scattered outlets, a disorienting layout, arrows on the walls, lights that are out, and the Manila Room.

### One palette
- Level 0 and The Maze now use exactly the same textures, all redrawn:
  - worn mono-yellow wallpaper with a faint chevron stripe;
  - damp beige-yellow carpet with stains;
  - off-white mineral-fibre ceiling tiles on a T-bar grid;
  - fluorescent panels with two tubes.
- The Maze is now built from Level 0's own blocks: worn wallpaper, spray-painted arrows, stained tiles, vents and missing tiles.

### More room above your head
- **Level 0:** the floors are 7 apart instead of 6, so there are five blocks of space instead of four.
- **The Maze:** it now spans two storeys, with five blocks of space. The lights are set into the drop ceiling instead of hanging into the corridor.

### Ramps to another floor
- About two thirds of the connections between Level 0 floors are now **carpeted stairs**. They run along the wall of a room and up through the ceiling into the floor above. The rest are still ladders.
- In The Maze: **ramps up to a split level** that leads nowhere.

### New features
| Feature | Level 0 | The Maze |
|---|---|---|
| Holes in the floor (into the floor below, or a pit with no bottom on the lowest floor; in The Maze into the offices below) | ✔ | ✔ |
| Soaked carpet (new block: it splashes and drips) | ✔ | ✔ |
| Lone pillars | ✔ | ✔ |
| Sealed closets without a door | ✔ | ✔ |
| Half the room one step higher | ✔ | ✔ |
| A wall that stops one block short of the ceiling | ✔ | |
| A light switch that switches nothing | ✔ | |
| Fluorescent panels set into walls | ✔ | ✔ |
| Small windows into the next room | ✔ | ✔ |
| Low ceilings, hanging walls, stairs into the ceiling, doors to nowhere | (already there, now taller) | ✔ new |
| Outlets low on the walls | (already there) | ✔ new |
| Ceiling: stained and missing tiles, vents, flickering and dead lights | (already there) | ✔ new |

Existing worlds: Level 0 and The Maze change shape in chunks that have not been generated yet. As agreed, please use a new world.

---

## New in Office Rooms 0.32.0 (beta) – Aranea Membri

**Aranea Membri** is a new creature, after the community Backrooms entity, built for this mod to the author's reference pictures:
- four thick black legs whose knees rise high above the body (an "M" from the front);
- a small dark body slung low between them;
- a pale carnivore skull hanging down at the front.

**Where it lives**
- Rarely in the normal Office Rooms, never in the other megastructures.
- In the Abandoned Cinema, at most one per cinema.
- Config `[aranea_membri]`: `spawnChance` 0.002 and `cinemaChance` 0.04 per spawn check, plus `health` 70 and `damage` 9.

**How it hunts**
1. **Lurking:**
   - It nests invisibly up under the ceiling and cannot be hit there.
   - You hear scratching above you, more often the closer you are, and dust trickles down.
   - One or two **broken ceiling tiles** with dark holes appear near its nest.
2. **The drop:** walk underneath it and the ceiling bursts, the lamps right there come down, it lands (behind you if there is room) and screeches. The camera shakes.
3. **The hunt:**
   - It is fast and heavy.
   - Every ceiling light it passes under dies with a crack of glass.
   - It screeches now and then.
4. **Retreat:** lose it for about 10 seconds and, when nobody is looking, it climbs back into the ceiling where it stands. It waits 30 seconds before it can drop again.

**Details**
- From a spawn egg it is awake right away; natural spawns and `/summon` start in the ceiling. Use `{Lurking:0b}` to summon it awake.
- Sounds were made from the author's recordings plus synthesis: screech, scratching, a wet rattle, hurt, death and heavy steps.
- Drops: bones, string, sometimes a Research File.
- New block: **Broken Ceiling Tile**.
- Ceiling damage follows the `mobGriefing` gamerule.

---

## New in Office Rooms 0.31.0 (beta) – Room reverb and the Abandoned Cinema

### Reverb by room size
- In the Office Rooms dimensions every world sound now gets reverb that matches the space around you. The client measures the room around you several times per second (26 directions, up to 48 blocks).
  - Small furnished offices with carpet stay dry and muffled.
  - Long corridors, atriums, the Poolrooms, the tiled cinema lobby and big halls ring out with a long, bright tail.
  - Soft surfaces (wool, carpet, moss) dampen it.
- Footsteps, mob screams, doors and ambience all get it. Music, the lamp hum and UI sounds stay dry.
- Client config `[audio]`: `roomReverb` (on/off) and `roomReverbStrength` (0–2). The reverb uses OpenAL EFX and switches itself off quietly if your sound system does not support it.

### The Abandoned Cinema (new worlds only)
A new megastructure, as rare as the others. Two storeys of a region become a multiplex nobody has visited in decades. You enter it through office doors on its lower storey.
- **Lobby**, double height, with checkered tiles that echo:
  - a box office with ticket windows under a lit "TICKETS" marquee;
  - velvet-rope queues;
  - two concession stands with popcorn machines, soda fountains, candy cases, glowing menu boards and "CONCESSIONS" marquees;
  - faded posters ("HALL", "STAY", "OPEN", "EXIT", "LV0", "LOOK UP"), spilled popcorn and a "NOW SHOWING" sign.
- **Two low corridors** with numbered doors (1–12).
- **Twelve auditoriums**:
  - Each has stadium seating rising away from the screen, aisle steps, dim step lights, exit signs over the doors and a projector port in the back wall. The projector port throws a dusty beam and still rattles.
  - Every hall is one of five kinds:
    - an empty screen glowing faintly;
    - a screen of static;
    - a completely dark hall;
    - a wrecked hall with a torn screen and broken seats;
    - a hall where a thin figure stands on the screen.
- **Loot:**
  - the box office till (coins, Ticket Stubs);
  - the concession stock (snacks, Almond Water);
  - lost-and-found chests at the back of some halls.
- **New blocks and item:** 32 new blocks, all drawn for the mod, and a new item, the Ticket Stub.
- `/officerooms goto cinema` and `locate` support it. Elevators can arrive there, hazard zones can happen there, and there is a discovery milestone.
- No audio hint (as agreed for new rooms) and no own mobs yet.

---

## New in Office Rooms 0.30.1 (beta) – Louder, hunting voices

- **Louder:** all new mob screams and hurt sounds are compressed and brought close to full scale (roughly 2–3× as loud as in 0.30.0). Music now plays at full file volume. The default music level is 0.8 instead of 0.2, under a new config key `musicVolume`, so existing configs pick up the louder default automatically.
- **Flayed:** while it hunts, your full Flayed recording (17.5 s) loops and follows the Flayed. It stops as soon as it gives up or dies. The one-shot screams are gone, so nothing overlaps. Camera shake as before.
- **Hound:** while it hunts, your full Hound recording (15.4 s) loops instead of the cut-up barks. Its idle growl is muted while it hunts, so it no longer plays several sounds on top of each other.
- Idle sounds are unchanged; the Colony is unchanged.

---

## New in Office Rooms 0.30.0 (beta) – Sound overhaul

All new recordings are by the mod author (Ableton). Idle and ambient mob sounds are unchanged.

### Trigger screams
- **Flayed:** three new screams (5.6 s, 9.5 s and a short 1 s shriek) when it spots you. Camera shake as before.
- **Hound:** four new bursts when it turns on a new target.
- **Colony:** seven new calls when it starts hunting.
- All mob sounds are mono so they fade with distance and you can hear which direction they come from.

### Hurt sounds (when you hit a mob)
- **Flayed, Hound, Colony:** three hurt variants each, cut from the harshest moment of their own recordings, choked and distorted.
- **Every other mob** now has creepier hurt sounds built from the same recordings:
  - Wolfspider: high, dry clicking with a reversed tail.
  - Headless Warden: the Flayed scream an octave down, hollow and huge.
  - Gaunt: thin breathy shriek with a short echo.
  - Hunter: a guttural, low Hound burst.
  - Partygoer: a strangled giggle-shriek.
  - Deathmoth: a tiny insect flutter-scream.
  - The burrowing Unknown: a reversed, low scream that swells into the hit.

### Music
- **Office Rooms:** three tracks now (the original plus two new variants). On joining or entering the dimension, one of them plays at random. After that, roughly one track per half hour (randomised 21–39 min). The rest of the time you only hear the hum and the backrooms ambience.
- **Level Fun** has its own looping music while you are in it. It pauses the office music (no overlap) and stops a few seconds after you leave. The party ambience keeps playing underneath. The old music box ambience was dropped.
- Config `musicIntervalMinutes` (default 30) still sets the average gap; `musicLevel` still sets the volume.

Generator: `tools/gen_sounds_0300.py` (sox), reproducible from the WAV masters.

---

## New in Office Rooms 0.29.1 (beta)

- **The Last Meeting** now uses the same furniture as the ordinary meeting rooms: conference table, office chairs and the meeting board (OfficeCraft, placed like everywhere else as you come near).
- On top of that: the projector on the ceiling and the screen on the short wall, the minutes on a lectern, and a chest.

---

## New in Office Rooms 0.29.0 (beta)

### Six new special rooms (new worlds only, ordinary doors)
- **Cold Storage:** packed ice and snow on the floor, ice stacked along the walls, freezer barrels full of food.
- **The Last Meeting:** a long table, chairs pushed back (one knocked over), cups and papers, the projector and screen still on. On a lectern: the minutes. Plus a chest.
- **Mail Room:** shelves of barrels with parcels along the walls (many can be opened), a sorting table, a phone that rings for nobody.
- **Rain Room:** it rains from the ceiling; puddles, mud and moss. Only the desk in the middle stays dry, with a chest beside it.
- **Canteen:** 2–3 vending machines, a serving counter with a bell, tables and chairs, a chest with food and coins.
- **Arcade (rare):** patterned carpet, rows of humming cabinets whose screens are the only light, a prize chest. No music.
- How common they are (per suitable room): last meeting 1 %, cold storage, mail room and canteen 0.8 %, rain room 0.6 %, arcade 0.3 %. Adjustable in the common config, section `[curiosities]`.

### Old Coins
- New item **Old Coin**: about 30 % of ordinary containers hold 1–3.
- **Vending machines** take one coin per use and drop supplies, food, sometimes something better and very rarely a Research File or an Electronic Component.

### Quest book
- New optional chapter **XI. Curiosities**: find each of the six rooms (walking in counts), plus a coin hand-in. 11 chapters, 114 quests.

---

## New in Office Rooms 0.28.1 (beta)

### Poolrooms, more liminal (new worlds only)
- **Water depths:** every pool now has a shallow step at its edge and gets deeper towards the middle (up to 4 blocks), with more lights on the bottom of deep water.
- **Drained pools:** whole halls where the water is gone. The stepped basins are dry, the lights on the bottom still glow.
- **Children's pools:** small, shallow square pools.
- **Tiled benches** along the hall walls.
- **Lifeguard chairs** beside the water, facing it.
- **Freestanding arches** in the middle of a hall that lead nowhere.
- **Haze and ripples (client, all worlds):** a faint haze drifts over the water and now and then a ring spreads on the still surface.
- The ceiling lights stay as they were; no towels (as discussed).

---

## New in Office Rooms 0.28.0 (beta)

### Hotel
- **Fix (all worlds):** the guest rooms beside the central staircase aisle had no wall on that side. They now have one. In existing worlds this only applies to hotel parts that are not generated yet.
- **Rework (new worlds only):**
  - Room kinds:
    - standard double rooms;
    - twin rooms with two single beds;
    - trashed rooms (mattress gone, smashed TV, old magazines on the floor, cobwebs);
    - rooms with a hissing TV showing static;
    - rooms where an old phone on the nightstand rings for nobody;
    - empty rooms;
    - "DO NOT DISTURB" rooms: a locked iron door, something pushed against it from inside, and always a loot chest.
  - Warm, dim wall sconces between the doors. More ceiling lights are dead on the guest floors.
  - Ice machines (they hum) and housekeeping carts in some corridors.
  - The upper guest floor has its own faded teal carpet.
- New decoration blocks (also obtainable): Hotel Phone, Old TV, Ice Machine, Housekeeping Cart, Wall Sconce, Hotel Carpet (Teal).

### World settings
- New setting `content_version` (1 in new worlds, missing = 0 in old ones). It switches on the reworked content so that explored areas of existing worlds stay consistent. The poolrooms rework and the new special rooms will use it too.

---

## New in Office Rooms 0.27.1 (beta)

- **The Hound, more disturbing:**
  - starved body with a ridge of vertebrae and spurs along the back;
  - a neck of bare vertebrae;
  - flesh hanging off the face and the jaw, torn skin under the belly;
  - thinner legs with bony knees;
  - a tail of vertebrae.
- **The Hound is faster:** base speed 0.30 → 0.33, and it charges at 1.3× instead of 1.2× (about 19 % faster in an attack).

---

## New in Office Rooms 0.27.0 (beta)

### The Maze
- **Only the Flayed hunts in the Maze.** There is always one Flayed inside the Maze while you are in it; other creatures that wander in are removed.
- The Flayed no longer spawns in the offices directly around a Maze, so it does not suddenly show up just outside it.
- The "neon hum" heard on the storeys above and below a Maze is gone. It sounded like 8-bit music.

### Partygoers (Level Fun)
- They no longer follow you. They stand on their spot, turn towards you and wave.
- Come closer than **2 blocks**, or hit one, and it alerts every Partygoer nearby and they all attack.

### Tougher creatures
- Gaunt: 40 → **60** health.
- Colony: 50 → **80** health.
- Existing configs are updated automatically if they still use the old values (balance 5).

### Loot
- **Research Files** are much rarer in ordinary containers (offices, archive, reception, restrooms, hotel rooms, houses, gifts, control rooms…), about 40 % of before. Vaults, labs, research outposts, oases and hazard caches stay as they were.
- **Keycards** are rare in ordinary containers:
  - about 1/5 of before in the random "rare" and "special" loot;
  - fewer at the hotel reception and in industrial control rooms.
  - Security hideouts and vaults stay the main source.

### World generation (new worlds only)
- **Fewer Villager Outposts** (3 % → 1.2 % of suitable rooms).
- **More hidden rooms** (12 attempts per region instead of 6).
- Existing worlds keep their settings so already explored areas stay consistent.

### Fixes
- Elevator panel: never placed in a cabin corner that a doorway of a neighbouring room opens onto. If a cabin has no free corner, right-clicking the cabin floor opens the panel.

---

## New in Office Rooms 0.26.3 (beta)

- The Headless Warden is back to its previous model (user's choice).
- Everything else from 0.26.2 stays: spindly Gaunt and Colony, and the Colony hunts the Poolrooms.

---

## New in Office Rooms 0.26.2 (beta)

### Spindly redesigns (same style as the new Wolfspider)
- **Headless Warden:**
  - all limbs hair-thin, with knee and elbow knobs;
  - narrower torso and back;
  - longer forearms ending in long thin claws;
  - three thin toes per foot;
  - thinner tendrils.
  - Hitbox unchanged (0.9 × 2.2), so it still fits through 3-block doorways.
- **Gaunt** (only a light touch): much thinner legs with knee knobs, thinner arms, longer and thinner claws.
- **Colony:** eight hair-thin stilt limbs with high knees, long thin fingers and toes, hair-thin filaments on the mass, jerky stepped crawling.
- All animations are kept.

### Poolrooms
- The **Colony** now hunts the Poolrooms instead of the Hunter. While you are in the Poolrooms there is always at least one Colony, 24–70 blocks away.
- Leftover Hunters in the Poolrooms are removed. The Hunter no longer spawns naturally anywhere; its spawn egg still works.
- Quest book: the "The Hunter" bestiary quest is removed. The Colony quest now says it hunts in the Poolrooms (106 quests).

---

## New in Office Rooms 0.26.1 (beta)

### The hostile Wolfspider, redesigned
- New model (own design):
  - a gaunt wolf skull with a hanging jaw, long thin ears and six glowing red eyes;
  - a narrow thorax with a long, drooping, segmented abdomen;
  - eight hair-thin stilt legs of three segments each, with high knees and dark joint knobs.
- Bigger than before but still fits through one block: hitbox 0.95 × 0.95 (was 0.9 × 0.7). The legs reach well beyond it visually.
- Wrong movement:
  - jerky, stepped gait;
  - single legs tapping while it stands;
  - the head snapping to a sideways tilt (sometimes a full 90°) and back;
  - shivers and micro twitches;
  - the abdomen swaying;
  - the jaw chattering while it hunts.
- The eyes glow in the dark.
- The friendly Wolfspider keeps its old look.

---

## New in Office Rooms 0.26.0 (beta)

### Real elevators
- New cabin look in the generated shafts:
  - brushed steel walls;
  - a clean tiled floor;
  - a ceiling light (it goes out during blackouts);
  - a button panel on the wall beside the door.
- **Right-click the panel** (or, in cabins generated before 0.26.0, the old floor plate) to open the floor panel, then type a floor with the buttons or the number keys and press GO / Enter.
- How a ride goes:
  - Ding; the steel doors slide shut from the left and right (centre seam).
  - The cabin jolts (camera shake), hums and creaks for 4–10 seconds.
  - The display counts floors and sometimes shows nonsense.
- **You never arrive on the floor you typed.** The cabin stops on a random floor in the direction you chose, but the display claims it is the one you asked for.
- **If a megastructure lies within the floors you pass** (in a region next to the shaft), the doors always open inside it: Poolrooms, Neighbourhood, Maze, Hotel, Industrial Complex or Level Fun. There is no cabin there. You walk back.
- Everything standing in the cabin rides along, mobs included.
- **5 minutes cooldown** per elevator after a ride ("The panel stays dark").
- Rare disturbances:
  - **Blackout:** the cabin stalls and the doors stay shut until the power returns (at most 3 minutes). The doors can be broken.
  - **Visitor (8 %):** a Hound or a Gaunt is waiting in front of the doors.
  - **Knocking (15 %):** something knocks on the cabin roof during the ride.
- Jumping and sneaking no longer ride the generated shafts. Elevator plates you stack yourself elsewhere work as before (jump / sneak / right-click = floors per ride).

### Beds in the Office Rooms
- You can sleep in a bed in the Office Rooms. Using it sets your respawn point there.
- Monsters nearby still prevent sleeping.
- The dimension stays as it was otherwise: compasses still spin, clocks still don't work, no new vanilla mob spawns, no weather, same lighting rules.

### Atmosphere
- The whole Office Rooms dimension is about 30 % darker.
- The Flayed's scream now shakes the camera of players within 32 blocks (stronger up close).

### Notes
- The new cabins (steel, light, panel) only appear in chunks generated with 0.26.0. Older cabins keep their concrete walls and glowing plate; the plate opens the panel there.
- Elevator cooldowns are not saved: a server restart resets them.

---

## New in Office Rooms 0.25.1

### Quest book
- Every quest now ends with a **How:** line explaining where to find the item, the creature or the event.
  - It covers loot locations, recipes and steps.
  - For the fuse box it also says that you need 3 Electronic Components in your hand.
- **Atrium background**: every chapter shows a dark atrium photograph behind the quest map. It is a chapter image inside the quest book only; nothing in the world changes. The textures ship in the mod JAR (`office_rooms:textures/gui/quests/atrium_1..3.png`).
- Event quests (advancement, dimension, observation, checkmark) have proper icons, for example the fuse box, the power lever or the manila carpet.
- **Nothing forces you back to the Overworld:**
  - The shrine and the sand quests are optional.
  - Entering the Office Rooms by any route (rift, outpost gateway, …) completes "The Office Rooms".
  - A new optional quest "A Door Someone Built" covers the outpost route.
  - Chapter VI "Research Outposts" is now entirely optional and open from the start.
- 107 quests in total.

### Level 0
- The oversized log-and-plank tables are gone; they looked stacked rather than liminal. This only applies to newly generated parts of Level 0.

### Electronic Components easier to find (update to 0.25.1)
- **Server rooms:** 40 % chance per chest, 1–2 each. Tested: 50 chests gave 39 components.
- **Hidden maintenance rooms and security offices:** 35 %, 1–2.
- **Office tool loot** (drawers and cabinets): 8 %, 1. Tested: 100 rolls gave 10 components.
- The Industrial Complex, research outposts and Hazard Zones stay the big sources.
- The quest "How:" text is updated to match.

---

## New in Office Rooms 0.25.0 – Quest book, fixes

### FTB Quests progression (Office Rooms content only)
- Folder `quests/config/ftbquests/` in the package: copy it into the modpack (`config/ftbquests/`).
- Generated by `tools/gen_quests.py`: 10 chapters, 106 quests, 1 reward table. FTB Quests 2001.4.x, file version 13.
- Branches unlock with items (or kills). It starts very simple and keeps going.
- The mod works without FTB Quests: nothing of it is inside the JAR except the hidden milestones below.
- Chapters:
  1. The Threshold
  2. First Day at the Office
  3. Facilities
  4. Research Division
  5. Megastructures
  6. Research Outposts
  7. The Underside
  8. Level 0 (optional)
  9. Bestiary
  10. Requisitions (repeatable trades)
- **Hidden milestones**: advancements `office_rooms:milestones/...` that do not show in the advancement screen. The mod grants them for events that are not an item or a kill:
  - fuse box repaired;
  - Heart Research (first and max);
  - reforge (first and max);
  - outpost gateway online;
  - Level 0: Manila Room found and escaped;
  - each megastructure discovered (Poolrooms, Maze, Hotel, Industrial Complex, Neighbourhood, Level Fun).

### New goto / locate targets
- **Hidden rooms one by one**: `maintenance_room`, `supply_closet`, `forgotten_break_room`, `security_office`, `research_lab`, `concealed_room`. These are searched up to 10 regions away.
- `fuse_box`: the broken fuse box of the current blackout.
- `hazard_zone`: the centre of the announced or active zone.
- `outpost`: the nearest research outpost generated since 0.25.0 (also in the Nether).
- `shrine`: the nearest Abandoned Office Shrine in the Overworld.

### Blackouts: real darkness
- During a blackout the ceiling lights in the area are actually switched off on the server: about 6 chunks per quarter second, until the whole area is dark.
- They switch back on when the power returns. Chunks that were not loaded at that moment are restored when they load.
- So the blackout is now properly dark without shaders and with shader packs (which ignore the old screen-only darkening).
- The area is bigger: radius 48 → 80 blocks, storeys ±3 → ±4. Existing configs are updated only if they still hold the old values.

### Fixes
- **The Flayed in Level 0 was invisible**: entity tags are not sent to the client, so the client treated it as an intruder and dropped it. Fixed.
- **[Unknown] seemed stuck**: near its target it could circle the aim point. It now turns straight at the target when close; the burrow phase ends sooner, and the lunge has a time limit.

---

## New in Office Rooms 0.24.0 – Deathmoth, full test pass, fixes

### The Deathmoth (new creature)
- **Appearance**: a large moth with a wingspan of about 2 blocks.
  - It has dusty brown-grey wings with dull eye spots, a hairy body and feathered antennae.
  - Nothing on it glows: it is lit like its surroundings and almost disappears in dark corners.
- **Spawning**:
  - It spawns only in the Office Rooms dimension. The Overworld has no Office Rooms mobs.
  - Like the other mobs it spawns as a population around players, not on a timer aimed at you. That is about one per 35 rooms, at most 3 near a player.
  - It sits spread out on dark walls: in offices, the hotel, the industrial complex, the neighbourhood and Level Fun, preferably in dark places and out of sight.
  - It never spawns in a Maze or Poolrooms region (the whole region, every storey), and never in Level 0 or the Underside.
- **Waiting**:
  - It clings flat to the wall, head up, with its wings spread or folded back into a delta shape.
  - It does not move, apart from a very rare twitch. Now and then a quiet rustle can be heard nearby.
- **Ambush**:
  - It wakes when a player comes within about 6 blocks with a clear view, or within 2.5 blocks in any case, or when it is hit. It bursts off the wall with a loud flapping sound and flies straight at your face.
  - It **clings to your face**. Wings and body cover the screen and flutter, the view jerks, and it screeches.
  - It drains health: a 2 damage bite, then 1.5 damage every 0.6 s for 3.5 s.
  - Then it lets go and flies off into the dark. Hitting it knocks it off early.
  - It gives up after 10 seconds of chasing and never follows you for long.
- **Stats**: 16 health. The settings are in the config under `[deathmoth]` (roomsPerMob, maxNearPlayer, health, biteDamage, drainDamage, attachSeconds, chaseSeconds, detectionRange).
- **Extras**: own sounds (idle rustle, trigger, flapping, attack screech, hurt, death), a spawn egg, and the test command `/officerooms deathmoth`.

### Other changes
- **Hounds** appear a little more often:
  - roomsPerMob 60 → 45;
  - spawnChance 0.15 → 0.2.
- **Wolfspiders** are rarer: wolfspiderRoomsPerMob 40 → 70.
- Existing config files are updated only where they still hold the old default values.
- **Atria** are a third more frequent (density 0.075 → 0.1), but only in **newly created worlds**. Existing worlds keep their layout, so no seams appear at already explored places.
- **Level Fun**: when one Partygoer turns aggressive, it immediately alerts every Partygoer within 48 blocks, and they all come for you.

### Fixes from the full test pass
- **Performance (big):**
  - When new areas were furnished, OfficeCraft furniture at a chunk edge made the server wait for the neighbouring chunk to generate. In one pass that took up to several seconds.
  - Furniture is now placed only once all neighbouring chunks are loaded.
  - The profile while exploring: our share of the server thread dropped from about 66 to 6 of 300 samples. Standing still, the mod no longer appears in the profile at all (4.4 ms per tick).
- **Sculk offices:** in small or crowded rooms the Headless Warden got no spot. It now uses any free tile.
- **`/officerooms blackout warn|start`** now explains why no blackout is possible: no player outside spectator mode in the Office Rooms.

---

## New in Office Rooms 0.23.0 – New standard rooms

Four new everyday room types now appear between the ordinary offices. They are not special rooms. They use furniture from Refurbished Furniture where it is installed, plus 26 new Office Rooms blocks, and they are meant to feel empty, still and slightly wrong.

- **Training room** (medium and large rooms, about 6–8 %):
  - a glowing projection screen on a short wall showing a blank slide, with a ceiling projector aimed at it;
  - a lectern holding an "Orientation Handout", a book with English notes about seats, blackouts, fuse boxes and the top floor;
  - a flipchart;
  - rows of stacking chairs, perfectly aligned with two aisles. Sometimes one chair faces the wrong way;
  - a wall clock that ticks while its hands never move.
- **Executive office** (small and medium rooms, about 5–6 %):
  - a burgundy rug and a dark oak desk with a green banker's lamp;
  - a leather executive chair, which sometimes faces the wall;
  - dark oak visitor chairs, bookshelves (one of them a chiseled shelf), a sofa and an office plant;
  - storage cabinets holding the best loot of the standard rooms (loot table `chests/executive_office`).
- **Reception** (medium and large rooms, about 5–6 %):
  - a reception desk with nobody behind it and a service bell (right-click to ring it; once in a long while it rings by itself);
  - a red "NOW SERVING 00" display and a ticket dispenser;
  - rows of linked waiting chairs, a table with old magazines, a sofa, plants, and doormats at the doors.
- **Restroom** (rare: about 2–2.5 %):
  - tiled floor and tiled walls. The wall tiles show their tiles only on the inside, so the room next door still sees plain quartz;
  - up to three stalls with partitions and Refurbished toilets. Often one stall is locked, with a red OCCUPIED indicator;
  - wash basins with mirrors. One tap always drips (particles and sound);
  - a hand dryer or paper towel dispenser;
  - light that is always dim or partly failed.
- **New blocks**:
  - Restroom Floor Tiles and Restroom Wall Tiles;
  - Stall Partition (+ Top) and Locked Stall Door (+ Top);
  - Restroom Mirror, Hand Dryer and Paper Towel Dispenser;
  - Projection Screen (+ Top), Ceiling Projector, Stacking Chair and Flipchart;
  - Executive Chair, Desk Lamp, Wall Clock and Executive Rug;
  - Reception Desk, Service Bell, Waiting Room Chair, Old Magazines, Office Plant, Now Serving Display, Ticket Dispenser and Doormat.
  - All of them are in the Office Rooms creative tab and drop themselves when mined.
- **Without Refurbished Furniture**: toilets, basins, desks, sofas and cabinets fall back to vanilla blocks.
- **Existing worlds**: the new rooms are rolled with their own random stream, so every other room keeps its layout. They only appear in chunks that have not been furnished yet.
- **Config** `[loot]`: new entries `executive_office`, `training_room`, `reception`, `restroom` (only executive offices have containers by default).

---

## New in Office Rooms 0.22.1

- **Maintenance kit.** Repairing the fuse box during a blackout makes a small kit pop out of the box towards the player who fixed it. It contains:
  - always 1 Research File;
  - one of: a Portable Battery, 2–3 Glitched Ingots, a Gas Filter, or (rarely) Night Vision Goggles;
  - 15–25 experience.
- No reward when the backup power comes on by itself. Loot table: `office_rooms:gameplay/fuse_box_reward`.

---

## New in Office Rooms 0.22.0 – Blackouts

Now and then the power grid of the Office Rooms fails.

- **Announcement.** About 30 minutes after the first start, and then about an hour after each blackout, every Hazard Monitor shows:
  - "! POWER GRID ! UNSTABLE";
  - "BLACKOUT IN" with the sector and floors (for example "SECTOR E-6, FLOORS 58-64");
  - a countdown (3 minutes).
  - If a Hazard Zone is running at the same time, the screens take turns.
- **Blackout.** An area of about 97 × 97 blocks over 7 storeys around a player loses power:
  - the lights flicker for a moment and die, so it becomes very dark (night vision goggles still help);
  - the lamp hum falls silent;
  - the monitors inside the area go dark;
  - monitors outside show "BLACKOUT, FUSE BOX: FLOOR n, REPAIR REQUIRED" and the time until backup power.
- **Gaunts.** They come up out of the dark: at most 3 at a time, out of sight, 14–26 blocks away. When the power returns they go back.
- **Fuse box.** A sparking, crackling fuse box hangs on a wall at eye level somewhere in the area, and the floor is shown on screen when the blackout starts.
  - Right-click it with **3 Electronic Components** in your hand to restore the power for everyone.
  - Otherwise the backup power comes on after 20 minutes.
  - The repaired box stays as a wall decoration and can also be placed in creative mode.
- **Config** `[blackout]`: enabled, firstWarningAfter 30, cycleMinutes 60, warningMinutes 3, maxMinutes 20, radius 48, storeys 3, gaunts 3.
- **Command** (op): `/officerooms blackout status|warn|start|stop`.

---

## New in Office Rooms 0.21.2

- **Refurbished Furniture in Level 0.** If MrCrayfish's Refurbished Furniture is installed (declared as an optional dependency; nothing of it is shipped), newly generated parts of Level 0 use its furniture instead of the vanilla stand-ins:
  - oak chairs facing the wall;
  - a lonely oak table;
  - a yellow or light grey sofa alone in the middle of a room;
  - a chair in front of a drawer and a storage cabinet.
  - Without the mod, or if a block id is missing, the vanilla stand-ins remain.
- **Ceiling chairs removed** (your request). In their place there is now a single chair, or a sofa with Refurbished, in the middle of the room, turned in a random direction.

---

## New in Office Rooms 0.21.1 – More Level 0 atmosphere

Only newly generated parts of Level 0 change; areas you have already visited stay as they are.

- **Darker and more uneven light:**
  - less ambient light;
  - about a third of the fluorescent panels are dead;
  - about 6 % flicker in short bursts with a buzz;
  - whole dark zones of about 18 × 18 blocks;
  - lights are placed inconsistently: centre, offset, pairs or short rows.
- **Ceiling:** missing tiles (black behind them), water-stained tiles and vents.
- **Walls and floor:**
  - electrical outlets low on the walls;
  - peeling wallpaper patches;
  - rare red spray-painted arrows (they do not necessarily point anywhere useful);
  - soaked dark patches in the carpet.
- **Oddly placed wooden furniture:**
  - chairs facing the wall;
  - chairs hanging upside down from the ceiling;
  - a lone small table in a corner;
  - a single bookshelf with a chair in front of it.
- **Paradoxical architecture:**
  - staircases that end in the ceiling;
  - free-standing doors in a frame that lead nowhere;
  - walls hanging from the ceiling that stop two blocks above the floor;
  - rooms where the ceiling suddenly drops to two blocks.
- None of these blocks the way through a cell, so every cell stays reachable.
- The windows set too high in walls, added during development, were removed at your request.

---

## New in Office Rooms 0.21.0 – Level 0

### Getting in
Break any block of the **topmost ceiling of the Office Rooms** (the ceiling above the highest storey; y 543 with the default settings).
- The ceiling block stays intact; you are pulled through into the new dimension **Level 0** (`office_rooms:level_0`).
- You land on a free spot on a random one of its four floors.

### Level 0
- **Endless and procedural.** Four stacked floors of yellow wallpaper, damp carpet and fluorescent ceiling panels (about one in six is dead).
- **The layout:**
  - Cells of 6 × 6 blocks with 4-block-high rooms.
  - Wide openings, doorways and 1-block-wide slits.
  - Dead ends, loops and pillar halls.
  - Oversized tables (you walk underneath them) and giant chairs.
  - Ladders between the floors.
- **Threshold Rooms** (rare, floors 2–4): huge halls with rows of square holes that drop you to the floor below.
- **Always reachable.** Every floor of every 96 × 96 area is a spanning tree plus extra loops, joined to its neighbours on every border. Every free cell can therefore be reached without mining.
- **No mining, no building.**
  - Breaking and placing blocks is cancelled, and Mining Fatigue V is applied.
  - Explosions do not destroy blocks.
  - Ender pearls and chorus fruit do not teleport.
  - The Noclip Sword does not work.
- **The immortal Flayed.**
  - Exactly one Flayed hunts here, with the existing model, sounds and behaviour (scream, chase, falter).
  - It cannot be damaged.
  - When it gets stuck in the maze, falls far behind or is on another floor, it reappears out of sight close to you.
  - No other creatures spawn in Level 0.

### The way out: Manila Rooms
- **Rarity.** About one per five 96 × 96 areas, on a random floor. They are meant to take around 15 minutes of searching; this is an estimate and was not measured.
- **Hidden.** The room is lined with manila wallpaper on the inside only. Its single entrance is a 1-block-wide slit.
- **The puzzle.**
  1. The chest holds exactly one Water Bucket.
  2. Under the big table in the corner is a 1 × 1 hole: too narrow to walk on and too low to crawl.
  3. Empty the bucket into the hole. The whole shaft fills with water.
  4. Jump in and sprint-swim through the 1 × 1 passage to the light.
  - If you jump in dry, you are helped back out after 8 seconds.
- **Where you come out.** The way out sends you to a random office room in the Office Rooms, 800–3,200 blocks from where you broke in.
- **Reward.** Exactly one of these, chosen at random:
  - 3 Research Files;
  - 1 Multidimensional Sword;
  - 6 Glitched Ingots (the existing ingot for Multidimensional gear; there is no item called "Multidimensional Ingot").
  - You get a reward only once per trip into Level 0 (multiplayer-safe, per player).

### Books
- 7 new rare books in the Office Rooms: case file 0-117, survivor J. Marsh (two books), the Project LIMINAL ceiling study, missing posters, the Threshold note and the top-floor security log.
- Research outposts can now contain "Membrane Hypothesis".
- They hint at the top ceiling, the four floors, the immortal one, the manila rooms, the water and the hole.

### Commands (op)
- `/officerooms level0`: enter Level 0.
- `/officerooms level0 manila`: go to the nearest Manila Room.
- `/officerooms level0 threshold`: go to the nearest Threshold Room.

---

## New in Office Rooms 0.20.0 – Abandoned Research Outposts

Project LIMINAL: unknown researchers investigated the Office Rooms from several dimensions. Their abandoned stations are now scattered through the Overworld and the Nether.

### Where they are
Outposts are only placed in chunks that are generated for the first time, so worlds you have already explored stay as they are.

| Location | How often | Integration |
|---|---|---|
| Ancient Cities | 60 % of cities | A sculk-grown annex just outside the city, with a 3 × 3 tunnel to the first walkable spot inside the city cavern. |
| Jungle Temples | 40 % of temples | Buried 9 blocks below the temple floor next to it. A ladder shaft with an iron trapdoor leads down from the jungle floor. |
| Caves below y 0 | about 1 in 1,800 new chunks (very rare) | Dug into the rock, opening onto a cave passage. Cobwebs, dripstone, collapsed walls. |
| Nether | about 1 in 500 new chunks | Built on solid ground on a blackstone foundation. Blackstone bricks, basalt pillars, barred windows, soul lanterns, magma vents. |

- The Ancient Cities, Jungle Temples and Nether terrain keep their structure: only the outpost and its access tunnel or shaft are added.
- The Office shrines are not affected; their code and data were not changed.

### What is inside
- A 13 × 11 laboratory with its own blocks: lab wall panels, floor tiles, ceiling tiles and a hazard floor.
- Furniture: control consoles with three gateway monitors, lab cabinets, specimen jar, Research Table, CCTV bank, server racks, containment tank, experiment machine, microscope and a fallen CRT.
- Two research chests (`chests/research_outpost`): research files, electronic components, batteries, filters, glitched ingots, redstone, copper, sometimes diamonds or rare loot.
- A lectern with a research note. Each kind of outpost has its own logs (sculk annex, temple station, deep bore site, Station Ash, Project LIMINAL summary); every log ends with the laminated gateway procedure.

### Activating the gateway
1. **Main power.** Pull the large industrial lever on the side wall ("MAIN POWER").
   - The facility boots: lights come on one after another, and the monitors show "PROJECT LIMINAL – BOOTING".
2. **Warning sequence (11 s).** The gateway stays locked while:
   - the monitors cycle through emergency warnings ("DIMENSIONAL INSTABILITY DETECTED", "CONTAINMENT FAILURE", "UNKNOWN DESTINATION", "STAFF STATUS: MISSING", …) and flicker;
   - the red warning lights flash and an alarm sounds;
   - sparks fly from the frame and the lights drop out.
   - The monitors end on "PORTAL SYSTEM READY – PRESS THE RED EMERGENCY BUTTON".
3. **Emergency button.** Press the large red button on its yellow-black pedestal.
   - Activation takes 8.5 s: a charging whine, arcs across the frame, the frame coils light up cyan one by one, the lights flicker, the ground shakes, and the screens count up to 100 %.
   - The gateway fills in from the bottom, with a flash and a roar.
- **Safety locks.**
  - Without power the button only reports "NO POWER".
  - During the warnings it reports "SAFETY LOCK".
  - Pulling the lever back shuts everything down and closes the gateway.
- **The gateway** takes you into an office room of the Office Rooms. It is server-side and works for each player separately (multiplayer).

### Test command
`/officerooms outpost <lab|ancient|jungle|cave|nether>` (op): builds an outpost in front of you.

---

## New in Office Rooms 0.19.1

[Unknown] now has black body hair:
- about 60 thin bristles of different lengths along every body segment;
- bristles on all legs and arms;
- 16 long strands trailing back from the scribble head.

Its behaviour, health and damage are unchanged.

---

## New in Office Rooms 0.19.0 – The Underside

### The way down
- **Six artifacts.** Each megastructure holds exactly one artifact in a fixed chest. Every item has its own texture and a short line of lore.

  | Artifact | Megastructure | Chest |
  |---|---|---|
  | Chlorine Pearl | Poolrooms | first chest at the end of the drowned maze |
  | Faded Photograph | Neighbourhood | first house with a chest |
  | Thread of the Maze | Maze | vault |
  | Room Key 000 | Abandoned Hotel | first chest in the cellar vault |
  | Humming Fuse | Industrial Complex | chest in generator hall 1 |
  | Party Smile Badge | Level Fun | first present |

- **Underside Key.** Crafted (shapeless) from all six artifacts. Use it on the bedrock beneath the lowest floor of the Office Rooms: the bedrock opens 3 × 3, the ground shakes, and the key is used up.
- **The fall.** Anyone who falls through the hole into the void lands in the Underside instead of dying. You arrive just below the upside-down meadow and drift down slowly onto the grass.

### The Underside (new dimension `office_rooms:underside`)
- An endless meadow with 1–2 blocks of relief, trees and flowers.
- About 11–14 blocks above it hangs the upside-down meadow: its own grass block, dirt, hanging trees and roots.
- Soft wind ambience, a pale fog and no sun. No creatures spawn here.

### Boss: [Unknown]
- **Look.** Our own design, a mix of sketches A and B:
  - a 20-block centipede made of wire-thin black lines;
  - on every third segment, a skinned ribcage with long human-like arms;
  - the Flayed's scribble head with an eye and a jaw of lines.
- **When it appears.** 20 seconds after you arrive:
  - the last 4.5 s the ground shakes (camera shake, rumbling, earth crumbling at the spot where it will break out);
  - then it bursts out vertically, roars and dives into the ceiling;
  - a boss bar shows "[Unknown]".
- **How it fights.**
  - It swims through the meadow and the ceiling.
  - Before every surfacing, dust and a short quake warn you, 1–1.5 s ahead.
  - It lunges across the gap, only about 40 % of the time directly at you; otherwise somewhere nearby.
  - After some lunges from the ceiling it crawls on the meadow for a few seconds and then dives again.
  - Touching it deals 14 damage.
  - It has 600 health and armour 10. All segments can be hit.
- **When it dies.** The segments burst one after another from the tail. Then:
  - the **Noclip Sword** drops, glowing;
  - **two portals** open: a yellow one in a wallpaper frame ("Back to the Office") and a green one in mossy stone ("Overworld", to your spawn point).
- **It comes back.** It appears again on every new visit. When nobody is left in the Underside, it disappears.

### Noclip Sword
- A netherite-class sword.
- Right-click: for 10 s you walk through walls and furniture, but never through floors. Then there is a 30 s cooldown.
- An actionbar countdown shows the time left. If the effect ends while you are inside a wall, you are pushed to the nearest free spot.

### Books
- 12 new survivor books with Okafor, Abara, Ellison and Lindqvist from the earlier logs, plus new voices: hotel guest book, generator hall shift log, Level Fun invitation, Neighbourhood watch, emergency broadcast (HAZARD ZONE), research table notes, the dark floors.
- They describe today's Office Rooms and hint, piece by piece, at the six artifacts, the bedrock, the 20 seconds, the dust warning, the two doors and the sword. They stay disturbing.

### Command
- `/officerooms underside [now]` (op): sends you to the Underside. With `now`, the quake starts after about 1 s.

---

## New in Office Rooms 0.18.1

`/officerooms goto` now also reaches the newer structures:
- `hotel`: the Abandoned Hotel lobby;
- `hotel_vault`: the vault door in the hotel basement;
- `industrial`: a generator hall of the Industrial Complex;
- `level_fun`: Level Fun;
- `hideout`: the nearest hidden room;
- `elevator`;
- `secure_storage`.

All of them can be tab-completed. The command picks the nearest target (the same positions as `/officerooms locate`) and lands you on a free spot with solid ground, searching up to 6 blocks around. Everything from 0.18.0 is unchanged.

---

## New in Office Rooms 0.18.0 – Sound Overhaul

All new sounds are synthesised for the mod (`tools/gen_sounds_0180.py`); no foreign audio material is used. Mob mechanics, damage and spawn rates are unchanged.

### Creatures

Every hostile creature has new, darker and deeper sounds, layered from growls, distorted breathing, wet organic noises and reverb. Most sounds come in 2–4 variations, and the idle sounds also play at a second pitch, so they repeat less.

| Creature | New sound identity |
|---|---|
| **The Flayed** | Four long signature screams, 4–6 s each: an agonised human-like voice, a guttural voice two octaves below and a shrill layer on top. Each begins with tearing muscle and ends in a long reverb tail. Audible up to 160 blocks. It now also screams in about half the cases when it resumes a chase after faltering. New: wet breathing, muscle tearing when it hits, heavy wet footsteps, a deeper hurt sound and a long collapsing death. |
| Wolfspider | Low growls with clicking legs, new clicking footsteps, a hiss-and-snap attack, a dark snarl when hurt. |
| Hound | Deep, rattling growls with heavy breathing, monstrous barks with a chest thump, a low pained growl instead of a yelp. |
| Hunter | Heavy distorted breathing over a low hum, sometimes with a distant scream inside. A distorted roar when it notices you, a metallic groan when hurt, heavy wiry footsteps. |
| Colony | Screams from four throats, out of tune with each other. Deep groans with wet clicks, heavy wet footsteps. |
| Gaunt | Dry, rasping breath over an almost inhuman growl, sudden distorted outbursts, dry bony footsteps. |
| Headless Warden, Partygoer | Deeper hurt and death sounds (the Partygoer no longer squeaks). |
| **Smiler** | Unchanged, as requested. |

Hurt sounds are clearly deeper. Measured spectral centroid, old → new:
- Hunter: 4023 → 552 Hz
- Flayed: 2839 → 1269 Hz
- Wolfspider: 2412 → 1589 Hz
- Gaunt: 1915 → 734 Hz
- Headless Warden: 1772 → 445 Hz
- Colony: 1896 → 1390 Hz
- Partygoer: 1426 → 927 Hz
- Hound: 1308 → 914 Hz

### Ambience

- **Everywhere:**
  - deep electrical buzzing with flicker and crackle;
  - air moving through vents;
  - pipes knocking and groaning;
  - something very far away (a muffled scream, a growl, a roar).
- **Area soundscapes** (chosen from the blocks around you):
  - Industrial Complex: heavy machine hum, hissing steam, pipes and metal. Running generator cores also hum loudly on their own.
  - Labs and security rooms: server fans, beeps and relays.
  - Hotel: distant doors, an elevator chime, a muffled TV, creaking.
  - Level Fun: a cheerful tune from a broken speaker that slowly goes out of tune, an off-key music box, distant giggles and party horns.
  - Poolrooms: a deep groan across the water, added to the water sounds.
- The lamp hum is louder and the music quieter (since 0.17.0).

---

## New in Office Rooms 0.17.1

- **Hazard Zone safety:** a zone is never placed in a megastructure where a player is standing at that moment. "In it" means the same region, from 12 blocks below to 12 blocks above it. This way nobody is gassed out of nowhere; there is always the 15-minute warning first.
- Test: the player stood in the Maze and a warning was started six times. The Maze was never chosen (picks: three hotels, one Industrial Complex, two Neighbourhoods). All 20 unit tests passed.

---

## New in Office Rooms 0.17.0

### 1. HAZARD ZONE (recurring world event)

#### Cycle

Every 100 minutes a toxic zone appears in a random megastructure near the players (Poolrooms, Neighbourhood, Maze, Hotel, Industrial Complex or Level Fun).

| Time | What happens |
|---|---|
| T − 15 min | Monitors show the warning with a countdown. The detector starts working. |
| T | The zone activates. |
| T + 45 min | The zone disappears. |
| 40 min | Cooldown. Events never overlap. |

- The first warning comes 20 minutes after the mod is first loaded in a world.
- The schedule is stored with the world, survives restarts and is synchronised to every player.
- All times are in the config section `[hazard_zone]`.

#### Monitors (no chat messages)

- **Where they stand:** Hazard Monitors stand in some offices (about one in seven) and corridors, Security Offices, labs, secure storage rooms, industrial control rooms and the hotel reception.
- **What they show:**
  - Normally: "FACILITY STATUS NOMINAL" in green.
  - During the warning: "WARNING – TOXIC CONTAMINATION DETECTED – HAZARD ZONE IN mm:ss", blinking red and yellow.
  - While the zone is active: "HAZARD ZONE ACTIVE", the place, "GAS MASK REQUIRED" and the time left.
- They flicker now and then and beep while an event is running.
- About one in six monitors is broken: switched off or showing corrupted data.
- You can craft one for your base (iron, glass pane, Electronic Component).

#### The zone

- **Size and look:** about 90 × 90 blocks across the megastructure's full height. Inside it hangs a green haze of drifting gas, also visible from outside, with dense green fog while you are inside and the occasional hiss of gas.
- **Loot:** 8 Hazard chests in random places. Chests near the centre use the better `hazard_core` table: Research Files 90 %, Toxic Crystals, Electronic Components, Glitched Ingots, Totem, L3 card, Master Key, diamonds. The outer ones use `hazard_cache`.
- **Creatures:** Flayed, Colony, Headless Wardens and Hounds appear around players inside the zone.
- **When it ends:** gas, chests and event creatures are removed.

### 2. Equipment

- **Gas Mask** (head slot):
  - Protects completely while its filter lasts: 10 minutes inside the gas.
  - Replace the filter with a **Gas Filter** (right-click; crafted from iron nugget, charcoal and paper, gives 2).
  - Rare loot in offices, labs, security offices, storage rooms and industrial rooms.
- **Without a mask:**
  - Exposure builds up (shown in the action bar).
  - From 20 % nausea, from 40 % weakness and rising damage, which ends in death.
  - Outside the zone the exposure drops again.
  - Death or a dimension change resets it.
- **Night Vision Goggles** (head slot):
  - Night vision for 20 minutes per charge.
  - Recharge them by right-clicking a Portable Battery while wearing them.
  - Recipe: iron ingot, Electronic Component, iron ingot / glass pane, leather, glass pane.
- **Hazard Visor:**
  - Gas mask and night vision in one.
  - Crafted from Gas Mask + Night Vision Goggles + **Toxic Crystal**. The Toxic Crystal only drops in Hazard Zones.
- **Special Event Detector** (rare loot):
  - While held it shows an arrow towards the announced or active zone, the countdown, the place, the horizontal distance and how far above or below you it is.

### 3. Sound

- The lamp hum is louder (0.85 instead of 0.35).
- The music is much quieter (20 %).
- Both can be set in the client config (`lampHumLevel`, `musicLevel`).

### 4. Commands (operators)

`/officerooms hazard status | warn | start | stop`

---

## New in Office Rooms 0.16.1

### Level Fun

- **Play Carpet:** the floor is now a deep red kids' play carpet with darker swirls, curving roads with yellow dashed lines and little cars. The texture is 32 × 32 so the pattern stays readable.
- **Bouncy castles** (not bouncy, decoration only) in about 7 % of the cells:
  - 6 × 6 inflated yellow floor;
  - red and blue puffy walls with an opening at the front;
  - four blue towers with yellow tops.
- New blocks: Play Carpet, Red/Blue/Yellow Inflatable.

### Natural spawning checked

Every creature was checked for natural spawning in a fresh world (see the test report).

---

## New in Office Rooms 0.16.0

### 1. Much darker

- The dimension's base brightness (`ambient_light`) is now 0 instead of 0.07.
- The light curve of the dimension is much steeper:
  - directly under a lamp it is as bright as before;
  - block light 8 is only about 60 % as bright;
  - block light 3 is about 40 %;
  - unlit areas are close to black.
- This runs on the client (`OfficeRoomsSkyEffects.adjustLightmapColors`) and applies only in the Office Rooms dimension.

### 2. Level Fun =) (new megastructure)

Level Fun is as rare as the other megastructures. One storey of a region (160 × 160 blocks) becomes an endless party in yellow.

- Walls are only partly built, Level-0 style: pillars at every grid point and many wall pieces with gaps.
- Furnishings:
  - party tables with cake and gift boxes;
  - clusters of floating balloons in five colours;
  - colourful streamers under the ceiling;
  - confetti on the carpet;
  - piles of gift boxes, with a real chest among them (`level_fun_gift`: cake, cookies, fireworks, almond water, glowsticks, golden apples, emeralds, Research Files 15 %);
  - "=)" painted in red on the walls, dripping.
- Flickering fluorescent strips; some are dead.
- New blocks: Party Wallpaper, five balloons, Party Streamer, Confetti, Gift Box, Party Table, "=)".
- `/officerooms locate level_fun`.

### 3. The Partygoer

An own design: a tall, slender, glossy yellow figure with a painted smiley face and a striped party hat.

- It looks friendly. It watches you, waves, giggles and blows a party horn.
- It slowly follows you until it stands about **7 blocks** away, then just stands there smiling.
- Come closer than **6 blocks**, or hit it, and it laughs and sprints at you (6 damage). A sprinting player can still get away.
- Up to six Partygoers roam near a player in Level Fun.
- Drops cookies, and a Research File with a 5 % chance.
- All sounds are synthesised for the mod (`tools/gen_sounds_0160.py`): giggles, party horn, laughter, squeak, deflating balloon.

---

## New in Office Rooms 0.15.0

### The Industrial Complex (new megastructure)

The complex is as rare as the other megastructures. One region (160 × 160 blocks) replaces five storeys with a liminal factory of five levels.

- **Rooms:** every level is a grid of 16 × 16 concrete rooms. Pipes are everywhere: two main lines under every ceiling, one along a wall, risers in the corners.
  - Most walls have a **grate door** in the middle (some open, some closed, some just a gap).
  - Lighting: cage lamps, many of them broken.
- **Room types:**
  - Pipe galleries with grated floors you can see through to the level below.
  - Control rooms with consoles, CCTV banks, chairs, lockers and a chest.
  - Storerooms with stacked crates and often a chest.
  - Small generator rooms.
  - Empty rooms.
  - Three staircases per level.
- **Two generator halls** (32 × 32) reach through the three lowest levels.
  - Four huge generators each; some still run with a glowing core.
  - Pipes lead up into the ceiling.
  - Grated catwalks with railings line the walls on levels 1 and 2.
  - A sign reads "GENERATOR HALL A/B", and a chest stands on the floor.
- Office doors open into the complex on **every** level.
- **Creatures:** one **Flayed** per complex (it takes a while to show up) and roaming **Hounds**.
- **New blocks:** Industrial Wall, Industrial Floor, Industrial Ceiling, Metal Grate (see-through), Grate Door (opens by hand), Generator Casing, Generator Core (glowing, animated), Dead Generator Core, Cage Lamp (and a broken one).
- **New item:** Electronic Component (rare loot).
- **Loot tables:** `industrial_control` (cards, Research Files 10 %), `industrial_storage` (iron, copper, redstone, supplies) and `industrial_generator` (Electronic Components 70 %, Research Files 25 %).
- **Locate:** `/officerooms locate industrial`.

### Note for existing worlds

As with the hotel, regions that were never generated can now become industrial complexes.

---

## New in Office Rooms 0.14.0

### 1. The Abandoned Hotel (new megastructure)

The hotel is as rare as the Poolrooms, the Neighbourhood and the Maze. One region (160 × 160 blocks) replaces four storeys with four hotel levels:

| Level | What is there |
|---|---|
| Basement | Bare concrete, pillars, rusty pipes under the ceiling, crates and laundry, only a few working lights. A **hotel vault** (security door level 2 or 3) holds two vault chests, three safe-deposit boxes and, in half of the hotels, a Research Table. |
| Lobby | One huge open hall: marble floor, quartz columns, a long rug, sofa groups with dead plants, brass ceiling lamps (some broken) and the **reception** with bell, sign and a chest. Office doors open into the lobby. |
| Guest floors 1 and 2 | Long carpeted corridors with numbered rooms on both sides (ROOM 1001 and up). Each room has a double bed, two nightstands with lamps, a dresser and an old TV. Some doors stand open, some rooms were stripped bare, and about one in six holds a loot chest. |

- A wide central aisle with staircases links all levels.
- **Hounds** roam the corridors and the lobby, with an occasional Smiler.
- New blocks with their own textures, all obtainable and placeable: Hotel Wallpaper, Hotel Wainscot, Hotel Carpet, Hotel Marble, Hotel Ceiling, Hotel Ceiling Lamp (and a broken one), Hotel Bed (head and foot), Hotel Nightstand (with a glowing lamp), Hotel Dresser, Reception Desk, Rusty Pipe.
- New loot tables:
  - `hotel_room`
  - `hotel_reception`: access cards L1 35 % and L2 12 %
  - `hotel_vault`: Research Files 80 % (1–3), L3 card, Glitched Ingots, gold, diamonds, Totem of Mercy, Master Key
  - `hotel_safe_box`
- `/officerooms locate hotel` and `/officerooms locate hotel_vault`.

### 2. The Maze

The Flayed now stalks the Maze instead of the Hunter (exactly one per Maze). The Hunter still lives in the Poolrooms.

### Note for existing worlds

Regions that were never generated can now become hotels. If such a region borders terrain you have already explored, the hotel may end abruptly there. The same applied when earlier megastructures were added.

---

## New in Office Rooms 0.13.0

### 1. The Flayed

An extremely rare hostile creature.

**Look**
- A stretched, skinless figure built entirely from thin strands of black muscle, far taller than a player (model about 3.5 blocks; hitbox 0.7 × 2.9, so it still fits through office doors).
- It has no solid torso: a cage of fibres hangs around the spine.
- Its arms almost reach the floor and end in hair-thin fingers.
- Its wide, flat head is covered in scribbled scratches; the front is a tall, ragged mouth with a hanging jaw.
- There are no glowing parts.

**Behaviour**
- Idle: drifts slowly and twitches (sudden head and finger jerks).
- When it sees a player (line of sight needed), it rears up and screams for 1.6 s, standing still. The scream is enormous and distorted and can be heard about 128 blocks away.
- It then sprints after you at about 7.5 blocks per second; a sprinting player does 5.6.
- It deals very high damage: 18 by default.
- After 10 s of continuous pursuit it falters for 3 s, slumped and panting at a crawl. This is your chance to break line of sight.
- Spawns roughly once per 40 minutes of exploring, at most one near a player and never closer than 40 blocks.
- Drops a Research File with a 50% chance when killed by a player.
- Config section `[flayed]`: health, damage, chaseMultiplier, chaseSeconds, falterSeconds, spawnChance.
- All sounds are synthesised for the mod (`tools/gen_sounds_0130.py`). The model is generated by `tools/gen_flayed_geo.py`.

### 2. Research Table: ingredients and finds

- Paper and books now appear in office loot (common, uncommon and supply tiers). Wood and iron were already there, so the Research Table can be crafted from Office Rooms finds alone.
- Half of the hidden research labs and 40% of the secure storage rooms (restricted access) contain a Research Table.

---

## New in Office Rooms 0.12.0

### 1. Research Files and the Research Table

**Research File** (rare): found in labs, security offices, concealed rooms, vaults (Maze, Poolrooms, secure storage), the Oasis, sculk offices and, very rarely, ordinary office desks and archives.

**Research Table**
- Recipe: paper, book, paper / iron ingot, crafting table, iron ingot.
- Right-click to open it. It spends Research Files from your inventory:

| Research | Cost | Effect |
|---|---|---|
| Heart Research | 3 files | +1 heart of maximum health, permanently (max. +5 hearts). Kept after death and restarts. |
| Reforge Weapon | 2 / 3 / 4 files | +3 damage per level, up to 3 levels (+9). |

Reforging details:
- Works on all swords, axes, tridents, bows and crossbows, including every vanilla weapon. Modpacks can add more items through the item tag `office_rooms:reforgeable`.
- Enchantments and existing attributes are kept, and the bonus stacks independently of Sharpness/Power.
- Bows, crossbows and thrown tridents add the bonus to every projectile.
- The tooltip shows "Reforged I–III".

A full set (5 hearts + one weapon at III) costs 24 Research Files.

### 2. New lab and security equipment

Ten new blocks. They can be crafted and placed for your own base:
- Research Table
- CRT Monitor
- CCTV Monitor Bank (animated static)
- Control Panel
- Server Rack (blinking LEDs)
- Lab Cabinet
- Microscope
- Specimen Jar
- Containment Tank
- Resonance Apparatus (glowing, animated core)

The models are kept simple (few cuboids) so they stay fast with shaderpacks.

### 3. Bigger, believable labs and security offices

Hidden research labs and abandoned security offices are now 11 × 8 rooms (previously 7 × 6).

- **Security office:** a CCTV wall over a row of control panels, CRT monitors, server racks, a supervisor desk, a key cabinet, a chest with access cards, a camera in the corner and a dead ceiling light.
- **Research lab:** an instrument bench (microscope, specimen jars, brewing stand), three containment tanks with one broken and sculk leaking out, a Resonance Apparatus fenced in with chains, a computer corner, cabinets, a dim ceiling strip and a soul lantern.

---

## New in Office Rooms 0.11.0

### 1. Creature overhaul (GeckoLib)

GeckoLib **4.4 or newer** is now a required dependency (tested with `geckolib-forge-1.20.1-4.8.4`). It is **not** bundled; install it next to the mod.

Seven creatures have new animated models. Their behaviour, sounds and spawn rules are unchanged:

| Creature | New look | Animations |
|---|---|---|
| Gaunt | Arthropod centaur: segmented lower body with stinger, 6 legs, upright torso with clawed arms, red glowing eyes | idle, walk (tripod gait), run, scream |
| Headless Warden | Hunched sculk body, rib cage with a cyan glowing heart, glowing neck stump, tendrils | idle, walk, run, attack, scream |
| Hound | Hairless dog with a human head | idle, walk, run, bark, attack |
| Smiler | No body. A black, smoky shroud around a face with glowing white eyes and grin | idle, walk, run, giggle, attack |
| Colony | Fused mass with four heads and eight limbs | idle, walk, run, scream |
| Hunter | Wireframe figure made of thin rods, tiny glowing eyes, always full-bright | idle, walk, run, notice |
| Observer | Tall, thin, dark figure with faint eyes and sudden head snaps | idle, walk, run |

The **Wolfspider** keeps its original look.

Glowing parts use a full-bright layer, so they also glow in the dark office dimension, which has no sky light.

### 2. Hidden rooms

Small 7 × 6 rooms are built into larger ordinary office rooms. There are about 0.5 per region, spread over all storeys. They are never placed in megastructures, elevators or secure storage rooms.

| Room | Entrance | Contents |
|---|---|---|
| Maintenance room | Loose panel (looks like the wall) | Pipes, chains, dead fuse box, anvil, chest and barrel |
| Supply closet | Loose panel | A row of barrels on shelves, chest; good supplies |
| Forgotten break room | Spruce door (sometimes ajar) | Table and chairs, fridge, coffee machine, dead plant, candle, "HAPPY BIRTHDAY DAVE" sign |
| Abandoned security office | Iron door, left open | Desk with a wall of dead monitors, camera, chest with a chance of access cards |
| Research lab | Security door, level 1 (60%) or level 2 (40%), with a warning sign | Lab bench, brewing stand, cauldron, sculk specimen tank, cage with a skull, slime |
| Concealed room | Loose panel | Walls covered in chalk crosses, a chair facing them, stains, chest with rare loot |

- Each furnished room has a lectern with one of 11 English notes.
- About 15% of rooms are empty, holding only dust and cobwebs. Concealed rooms are never empty.
- Seven new loot tables (`chests/hideout_*`):
  - Security office: level 1 card 25%, level 2 card 10%.
  - Concealed room: Totem of Mercy, Master Key, Temporal Watch, level 3 card.
- **Loose Panel**: a new block that looks like the quartz office wall but breaks quickly by hand or with an axe.
- `/officerooms locate hideout` lists the nearest hidden rooms, by type, at their entrance.

---

## Office Rooms 0.10.1 (beta) – QoL tools

Eight small tools to find in the Office Rooms. Requires GeckoLib like 0.10.0. Everything else is unchanged.

![Items and chalk marks](bilder/0101_tools.png)

| Item | What it does | Where |
|---|---|---|
| **Glowstick** | Throw it: a dim green light (level 12) with a soft glow where it lands, for about a minute. Stacks to 16. | Office loot (supplies, common) |
| **Bandage** | Takes 2 seconds to apply (you move slowly and cannot run meanwhile), then Regeneration I for 12 s (about 2.5 hearts). Only usable when hurt. | Office loot; craftable: 2 paper + white wool → 3 |
| **Chalk** | Right-click a wall: click its left, right, top or bottom part for an arrow in that direction, the middle for a cross. 64 marks. Marks need a solid wall behind them. | Office loot |
| **Industrial Flashlight** | Right-click to switch on: a bright beam (light 15) where you point, up to 28 blocks, and light right in front of you. About 2.8 minutes of charge. A Smiler treats a lit flashlight like a torch. | Rare office loot only |
| **Emergency Flare** | Throw it: blinding red light with flames and smoke for 20 s. Creatures within ~9 blocks back away from it (not the Hunter and not the blind Headless Warden). | Office loot only |
| **Noise Maker** | Throw it: rings for 10 s; creatures within 24 blocks drop their target and go to the noise (not the Hunter). | Office loot only |
| **Portable Battery** | Hold a flashlight in the other hand and right-click: the flashlight is fully charged. 3 charges. Empty? Right-click a lit ceiling light: the battery is full again, but that light goes dark for good. | Office loot only |
| **Master Key** | Opens every security door (all levels). Wears out after 3 doors. | Extremely rare: special-room loot, maze vault (8 %), level 3 secure storage (6 %), Poolrooms vault (3 %) |

Loot: the new table `tiers/tools` holds glowsticks, bandages, chalk, flares, noise makers, batteries and flashlights; it is linked from the supplies, common, uncommon and rare tiers. The lights of glowsticks, flashlights and flares are invisible temporary blocks that remove themselves.

### Tests (actually run)

- Build OK, 16 unit tests, 0 failures. No exceptions in the server log.
- In game (production server and client, GeckoLib 4.8.4):
  - Glowstick thrown: one light block where it landed.
  - Flashlight switched on: light blocks along the beam.
  - Chalk on a wall, middle: cross mark, chalk wear 1.
  - Bandage while hurt: Regeneration applied, one bandage used; refused at full health.
  - Battery with a half-empty flashlight in the off hand: flashlight back to full, battery down one charge.
  - Master key on a closed level 3 door: door opens, key wear 1.
  - Flare thrown next to a Hound 3.5 blocks away: the Hound moved to 10 blocks away within 4 s.
  - Noise maker thrown 20 blocks from a Hound: the Hound walked to the noise.

### Not tested

- Drawing over an existing chalk mark to change it (the test click missed the wall).
- Arrow marks (only the cross was tested in game), recharging the battery from a ceiling light, the flashlight running empty, multiplayer, shader packs.

---

## Office Rooms 0.10.0 (beta) – GeckoLib and the new Gaunt

### New required dependency: GeckoLib

Office Rooms now needs **GeckoLib for Forge 1.20.1 (4.4 or newer, tested with 4.8.4)** on the server and on every client. Download it from CurseForge or Modrinth and put it into the `mods` folder next to Office Rooms. It is not bundled.

GeckoLib lets creatures have many bones and keyframe animations instead of simple box models.

### The Gaunt, redesigned

First creature in the new style (dark, wrong anatomy, unnatural movement). Mechanics are unchanged.

- Hunched, emaciated body with a visible rib cage, spine spikes and a glowing heart; a long neck and a pallid skull with deep, uneven eye sockets, glowing pupils and long uneven teeth on a separate jaw.
- Floor-length arms in two segments with bone claws, two small vestigial arms on the chest, legs that bend the wrong way.
- Animations: idle (slow breathing, the skull snaps sideways in sudden jerks, claws curl, the small arms twitch), a lurching walk, a fast run thrown forward with the jaw open and the head shaking, and a scream (rears up, jaw unhinged, arms spread, whole body shaking) when it calls its pack.
- Eyes and heart glow in the dark.

![The new Gaunt](bilder/0100_gaunt_geckolib.png)

Model, animations and textures are generated by `tools/gen_gaunt_geo.py` (no foreign assets).

### Tests (actually run)

- Build OK, 16 unit tests, 0 failures.
- Production server and client with GeckoLib 4.8.4, OfficeCraft and Refurbished: no errors from GeckoLib or Office Rooms.
- Gaunt seen from front, side and three-quarter view; idle twitch and the scream pose captured; a Gaunt summoned 18 blocks away reached the player within about 2 seconds (chase AI unchanged).

### Not tested

- Shader packs with the GeckoLib model, multiplayer.

---

## Office Rooms 0.9.0 (beta) – Elevators, hints, flickering lights, secure storage, faster shader furniture

Works with 0.8.x worlds. Elevators and secure storage rooms only appear in newly generated chunks. Everything else is unchanged.

### Faster OfficeCraft furniture with shader packs

The compatibility renderer now draws simplified shapes at a distance. The largest triangles of a mesh carry almost all of its visible surface; the thousands of tiny ones are rounded edges and screws. Kept are 90 % of the surface (about one eighth of the triangles) at medium range and 70 % far away. Screens and lamps stay complete.

Client config `office_rooms-client.toml`, section `[compat]`:

| Option | Default | Meaning |
|---|---|---|
| `officeCraftShaderDetail` | `BALANCED` | `FULL` = every triangle (as in 0.8), `BALANCED` = full detail only close to the camera, `SIMPLE` = always simplified (fastest) |
| `officeCraftShaderFullDetailDistance` | 8 | BALANCED: full detail within this many blocks |
| `officeCraftShaderRenderDistance` | 48 | furniture further away is not drawn |
| `officeCraftShaderShadows` | false | furniture casts no shadows with shader packs (the shadow pass drew everything a second time) |

If it is still too slow: set `SIMPLE` and lower the render distance to 32. Drawing the furniture "without shaders" while a shader pack is active is not possible, that is exactly the path that made it invisible.

![Simplified (left) and original (right)](bilder/090_shader_lod.png)

### Megastructure hints

Up to 8 storeys above and below a megastructure its sounds seep through the floors: lapping water (Poolrooms), buzzing neon (The Maze), distant barking (Neighbourhood). Louder and more frequent the closer you are, and it comes from above or below, wherever the structure is. Config `[atmosphere] megastructureHints`, `megastructureHintStoreys`.

### Elevators

- Every region without a megastructure has one elevator shaft: a light grey 5 × 5 column with a 3 × 3 cabin, at the same spot on all storeys. A storey is a stop wherever a side opens onto an ordinary room or corridor (on average 97 of 101 storeys).
- Stand on the **Elevator Plate** and **jump** to ride up, **sneak** to ride down. **Right-click** the plate to choose 1, 5, 10 or 25 floors per ride. The action bar shows the floor.
- The plate is also craftable (iron, piston, ender pearl, redstone → 2) and works anywhere between plates stacked above each other (up to 64 blocks apart).
- Find one: `/officerooms locate elevator`.

### Flickering lights

Ceiling lights within 5 blocks of a creature flicker when that creature is within 14 blocks of a player. Config `[atmosphere] lightFlicker`, `lightFlickerRadius`.

### Secure storage rooms (Level Access Cards)

- Rare strongrooms of unbreakable plating inside ordinary offices (about one per two regions), closed by a security door of level 1 (60 %), 2 (30 %) or 3 (10 %), with a sign.
- Inside: a chest (`chests/secure_storage_1/2/3`, better loot per level) and a supply barrel.
- Progression: Level 1 cards are now also in rare office loot; level 1 rooms can hold a Level 2 card (20 %), level 2 rooms a Level 3 card (20 %); level 3 rooms hold Multidimensional gear and can hold a Totem of Mercy.
- Find one: `/officerooms locate secure_storage`.

![Cabin, storage door, inside](bilder/090_elevator_storage.png)

### Tests (actually run)

- Build OK, 16 unit tests, 0 failures (new: elevators and storage rooms never in megastructure regions, every elevator has at least 2 stops; the block rules of the planner hold with the new installations).
- In game (production server, fresh world, seed 4711): elevator ride up 1 floor, 10 floors after two right-clicks, down by sneaking; storage door stays closed without a card and opens with a Level 1 card; chest and barrel carry the right loot tables; `verify 2` around elevator and storage: 0 errors.
- Flicker: a light above a creature was off in 39 of 120 samples, never without a creature, and lit again after the creature was gone. Dimension tick 4.1 ms.
- Shader renderer forced on without a shader pack: furniture looks the same as the original at room distance, no errors.

### Not tested

- Frame rate with a real shader pack (please report), shadows option.
- The hint sounds were not checked by ear; the server ran 70 s above a Poolrooms without errors.
- Multiplayer for the new features.

---

## Office Rooms 0.8.4 (beta) – /officerooms locate

- New command `/officerooms locate <type>` lists the nearest places of a type with coordinates, storey and distance, without teleporting (5 nearest plus how many more are in the search area).
- `/officerooms locate all` shows the nearest one of every type.
- Types: `poolrooms`, `neighbourhood`, `maze`, `maze_vault`, `villager_outpost`, `lush_cave`, `water_office`, `sculk_office`, `abandoned_camp`, `oasis`, `exit_room`. Megastructures are searched within 30 regions (12 for `all`), special rooms within ±2 regions on all storeys.
- Works from the console too: `execute in office_rooms:office_rooms positioned <x> <y> <z> run officerooms locate all`.

Tested: server, seed 4711, `locate all` and `locate maze` from 0 300 0 – all 11 types found, no exceptions.

---

## Office Rooms 0.8.3 (beta) – Megastructures much more common

Everything from 0.8.2 is unchanged except this.

### More megastructures

- Default `poolrooms_region_chance` is now **0.07** (0.8.1: 0.024, 0.8.0: 0.012). It applies to all three megastructures, so each one is in about **7 % of regions**, and about **21 % of all regions** hold one of the three.
- Rough estimate for a player who keeps moving and changes storeys now and then: **about one megastructure every 45 minutes**. The structures still sit on one random storey each (The Maze 1, Neighbourhood 4, Poolrooms 5 storeys), so you only find one if you are on the right height in that region. Moving between storeys raises the odds.
- Only **new worlds** use the new value.

### Fixes found by the denser test

- **The Maze:** with several megastructures next to each other, a maze could end up with **no exit** (exits are only cut towards the west and north neighbours, and a neighbouring megastructure blocked the whole side). Now only the rooms inside a neighbour's storey band are excluded. Result: all 211 mazes in 3600 test regions have 3–5 exits.
- **Poolrooms:** in very rare layouts (1 of 114 sites) there was no hall of 28–68 blocks, so the hidden underwater labyrinth and its vault chests were missing. Such Poolrooms now use the hall closest to 64 × 64 that fits a labyrinth. Poolrooms that already had a labyrinth are unchanged.

### Tests (actually run)

- Build OK, 15 unit tests, 0 failures. 3600 regions: Poolrooms 254, Neighbourhood 257, Maze 211; all 211 mazes have 3–5 exits (3: 76, 4: 64, 5: 71); 114 of 114 Poolrooms have their labyrinth vault; 12 mazes fully reachable from the first exit.
- Fresh world, seed 4711: `goto poolrooms` (348 431 −187), `goto neighbourhood` (499 167 −308), `goto maze` (484 359 −331, 4 exits), `goto maze_vault` – all within about 600 blocks of the world centre. `verify 2` at all three: `RESULT: OK`. No exceptions in the server log.

---

## Office Rooms 0.8.2 (beta) – Multiplayer check before the first group playtest

Everything from 0.8.1 is unchanged except these fixes.

### Fixes

- **Two Hunters at once (multiplayer):** a Hunter could appear in a chunk that was loaded but outside the simulation distance. There it was invisible to the "exactly one Hunter" check, so a second one appeared. Now creatures only spawn in fully active chunks, and if two Hunters ever meet in one Poolrooms/Maze band, only the one closest to a player stays.
- **Creatures piling up out of reach:** the same rule now applies to all Office Rooms spawns (office creatures, Poolrooms, Neighbourhood, Maze). Before, creatures could be placed in inactive chunks where they were neither counted nor cleaned up.

### Recommendations for a group playtest (4–5 players)

- Same mod list on server and all clients (OfficeCraft / Refurbished on the server means on every client too).
- Server RAM 6 GB, `view-distance` 6–8, `simulation-distance` 6 or more (creatures and the Hunter appear up to ~64 blocks away and need to be inside the simulation distance to move).
- Back up the world first, and always stop the server with `/stop`. A server that is killed (crash, out of memory, power loss) can lose the creatures of recently unloaded chunks, e.g. Neighbourhood residents.
- Optional: pre-generate the area with a mod like Chunky. On the 2-core test machine the dimension generates about 10 chunks per second.
- After the test: send `logs/latest.log` of the server and any `crash-reports`.

---

## Office Rooms 0.8.1 (beta) – Megastructures twice as common

Everything from 0.8.0 is unchanged except this:

- The default of `poolrooms_region_chance` is now **0.024** (was 0.012). It applies to all three megastructures (Poolrooms, Neighbourhood, The Maze), so each one now appears in about **2.4 % of regions**: on average one per 42 regions, about one per 1,030 × 1,030 blocks per type, one of the three per ~600 × 600 blocks.
- Within ±3 regions (1,120 × 1,120 blocks) there is a given type with about 70 % chance (before: 45 %), within ±5 regions about 95 %.
- Each structure still sits on one random storey (between about storey 9 and 90), so you also need the right height.
- The value is part of the dimension's generator settings. It only takes effect in **new worlds**; already generated chunks are never changed.

### Tests (actually run)

- Build OK, 14 unit tests, 0 failures. Frequencies in 3600 regions: Poolrooms 90, Neighbourhood 103, Maze 80 (0.8.0: 54 / 56 / 48). Poolrooms test: 44 of 1600 regions (0.8.0: 28).
- Production server, fresh world seed 4711: `goto poolrooms`, `goto neighbourhood`, `goto maze` and `goto maze_vault` all found a site; the nearest Maze is now at −919 179 −796 (0.8.0: −1799 473 −156). `verify 2` at the maze vault: 0 errors, `RESULT: OK`. No exceptions in the server log.

---

## Office Rooms 0.8.0 (beta) – The Neighbourhood, The Maze and new tools

Works with 0.6.1+ worlds. The new megastructures only appear in newly generated chunks. Everything from earlier versions is unchanged unless listed here.

### Two new megastructures

Each region (160 × 160 blocks) holds at most one megastructure. The Poolrooms keep their placement from 0.6.0. Regions without Poolrooms roll once more: with the same chance as the Poolrooms (`poolrooms_region_chance`, default 0.012) they become a Neighbourhood, with the same chance again they become The Maze. All three megastructures are therefore equally rare (unit test on 3600 regions: Poolrooms 54, Neighbourhood 56, Maze 48).

Both bands sit somewhere between storey 8 and the upper storeys, never in the dark exit storeys. Teleport: `/officerooms goto neighbourhood`, `goto maze`, `goto maze_vault`.

#### The Neighbourhood

![Neighbourhood](bilder/080_neighbourhood.png)

- A huge hall, 4 storeys tall, **without any light**. You need torches, or night vision.
- 20 identical suburban houses on identical lots in 4 rows of 5: asphalt streets with lane marks, sidewalks, lawns, fences, gravel paths. Every house is the same: white walls, dark oak corners, windows, a spruce door, a gable roof of deepslate tiles with a brick chimney, an attic, and the same furniture inside (bookshelf, TV, sofa, carpet, side table, a dead plant).
- **Every third house** (houses 3, 6, 9, 12, 15, 18) has a resident: a Smiler (35 %), Hound (35 %), Headless Warden (20 %) or even a Colony (10 %). Residents are persistent and wait inside.
- **Every other house** has a chest (`chests/neighbourhood_house`): office supplies, food, almond water, stories, sometimes diamonds or netherite scrap, an Anomaly Detector (~10 %) or an access card (~4 %).
- Up to 3 creatures (Wolfspider, Hound, Smiler) roam the streets around you.
- You get in through the doors of the neighbouring offices on the entrance storey; the hall is closed off by a thick concrete ring wall.

![House inside](bilder/080_neighbourhood_house.png)

#### The Maze (Level 0)

![The Maze](bilder/080_maze.png)

- One storey: yellow wallpaper, damp carpet, acoustic ceiling tiles and buzzing fluorescent strips (about one in eight is dead). 26 × 26 cells of corridors with loops and several open halls full of pillars.
- **Mining Fatigue III** as long as you are inside. Digging your way out is not an option.
- **3–5 exits** lead back into the office rooms: openings through the west and north office wall into ordinary rooms or corridors of the neighbouring region. They are only 3 blocks tall, so the Hunter cannot follow.
- **The Hunter** is always there (exactly one), plus Wolfspiders, Hounds, Smilers, Gaunts and rarely a Headless Warden (up to 5 near you).
- **Security vault:** a hidden room of unbreakable steel plates (walls, floor and ceiling) with a **Security Door**. The sign next to it shows the level: 1 (60 %), 2 (30 %) or 3 (10 %). Inside is one chest with the rarest loot in the game: always Multidimensional gear (often two pieces), Totem of Mercy (~35 %), an effect sword (~60 %), The Temporal Watch (~25–30 %), a Level 3 Access Card (~12–15 %), netherite ingots, enchanted golden apples, Glitched Ingots, Multidimensional Rods, enchanted books and diamonds.

![Vault door](bilder/080_maze_vault_door.png)
![Vault](bilder/080_maze_vault.png)

### New items

| Item | What it does | Where |
|---|---|---|
| **The Temporal Watch** | Right-click: every creature within 8 blocks gets Slowness VI and Weakness II for 4 seconds (almost frozen). Cooldown 2 minutes. | Only the rare office loot tier (~4 % of rare rolls), the maze vault |
| **Anomaly Detector** | Hold it (main or off hand): the action bar shows a signal bar and percentage, and it beeps faster and higher the closer the nearest creature is (range 32 blocks). It never says which creature it is. | Uncommon loot tier, Neighbourhood houses |
| **Level 1 / 2 / 3 Access Card** | Opens Security Doors up to its level (main or off hand, right-click the door). Currently the only security door is the maze vault. | Very rare in special-room loot (L1, L2), Neighbourhood houses (L1, L2), Poolrooms vault (L2, L3), maze vault (L3) |

### New blocks

- **Maze Wallpaper**, **Damp Carpet**, **Maze Ceiling Tile**: can be mined and used for building (creative tab).
- **Security Wall** and **Security Door**: unbreakable, immovable, no drop. The door ignores redstone and stays open until you close it with the card again.

### Config

No new options. `poolrooms_region_chance` in the generator settings now controls all three megastructures. A new loot entry `neighbourhood_house` is added to `office_rooms-common.toml` `[loot]` automatically.

---

## Office Rooms 0.7.1 – Shaped armor, everything in English

Works with 0.6.1+ worlds.

### Multidimensional armor with a real shape

The armor used to cover every face of the player model like a box, including the face. It is now drawn like vanilla armor, with open areas:

- **Helmet:** Netherite-style silhouette – brow band, cheek and jaw guards, open face, crest on top.
- **Chestplate:** chest plate with a centre ridge, shoulder plates and bracers; upper arms and lower belly stay visible.
- **Leggings:** belt with buckle, thigh plates and knee guards.
- **Boots:** only around the feet, with cuffs and toe caps.

Own drawing in the existing Multidimensional colours (`tools/gen_armor_070.py`). Stats and recipes are unchanged.

![Multidimensional armor](bilder/42_multidimensional_armor.png)

### Everything in English

- All command output (`info`, `stats`, `goto`, `mobs`, `spawning`, `verify` and its error messages, `furniture`), arrival and window messages.
- Config file comments in `office_rooms-common.toml` / `office_rooms-client.toml` – existing files are updated automatically on the next start.
- Log messages, the `enter`/`leave` functions, the mod description.
- The German language file (`de_de`) now shows the same English texts, so players with a German game language see English too.
- README, FEATURES and BUILD_INSTRUCTIONS are in English. The older design notes of versions 0.1–0.4 in `docs/` and `ARCHITECTURE.md` are still in German.

Note for scripts: `/officerooms verify` now ends with `RESULT: OK` or `RESULT: ERRORS`.

---

## Office Rooms 0.7.0 – Bug fixes, performance, new items and The Hunter

Works with 0.6.1+ worlds. Everything from earlier versions is unchanged unless listed here.

### New items (all in the loot system)

| Item | What it does | Where |
|---|---|---|
| **Totem of Mercy** (extremely rare) | If you would die in the Office Rooms, it is used up, you survive and are brought to your bed/respawn anchor in the Overworld (or the world spawn), with Regeneration, Fire Resistance and Slow Falling. Works from anywhere in your inventory. Does not protect against Liquid Pain. | Poolrooms maze vault (~10 %), very rarely in the best Office Rooms chests |
| **Liquid Pain** | Looks like a drink. Kills you instantly when you drink it. Nothing saves you (no armor, totem or effect). Death message: "… drank Liquid Pain". | Uncommon loot |
| **Witherfang** | Sword (7.5 damage, enchantable like any sword). Every hit gives Wither II for 5 s. | Rare and special loot |
| **Leaden Edge** | Sword. Every hit gives Slowness III for 4 s. | Rare and special loot |
| **Venomtongue** | Sword. Every hit gives Poison II for 5 s (undead are immune to poison, as in vanilla). | Rare and special loot |

The three swords repair with diamonds in an anvil.

### The Hunter (Poolrooms only)

A huge (3.6 blocks), gaunt figure made only of thin dark lines – wireframe head with eye slits, wireframe torso, very long arms with line fingers. Whenever a player is in the Poolrooms, exactly one Hunter roams them; it appears 24–70 blocks away on the hall floor.

- Senses players up to 96 blocks away, even through walls, and follows relentlessly (a bit slower than sprinting).
- 120 health, 6 armor, 11 damage, no knockback. It hums like electric wire and screams when it finds you.
- Too tall for the 3-block office doors, so it stays in the Poolrooms.
- Spawn egg in the creative tab.

### Checked: Multidimensional gear, Glitched Ingot, Multidimensional Rod

All registered, with textures, worn-armor textures, recipes (ingots + rod) and loot sources. Measured drop rates per chest (400 rolls each):

| Chest | Multidimensional gear | Glitched Ingot | Rod |
|---|---|---|---|
| Sculk Office special chest | ~6.5 % | 1 % | 3 % |
| Oasis | – | 2.75 % | 5.5 % |
| Poolrooms maze vault | – | 15.5 % | – |

### Bug fixes

- Sealed special rooms (water/sculk offices) could get an iron door **without** a lever or button and stay locked forever. The door control now searches the whole door wall; if there is still no spot, the room gets a hand-operated wooden door instead.
- `/officerooms goto atrium` did nothing; it now leads to an atrium window.

### Performance

- Creatures far away from every player (more than 64 blocks horizontally or 16 blocks / ~3 storeys vertically) and not hunting now pause until a player comes near. The Office Rooms are built over the full height, so villagers, golems, cats, frogs and guardians on dozens of storeys above and below you used to tick constantly. Measured on the test server with the same 86 villagers and 56 golems loaded: **dimension tick 31 ms → 5.8 ms**.
- The vanilla mob spawner is switched off in the Office Rooms dimension (it had nothing to spawn there but still searched every chunk). Office Rooms' own spawns are unaffected.
- Settings in `office_rooms-common.toml`, `[spawning]`: `freezeFarMobs`, `freezeHorizontal`, `freezeVertical`.

---

## Office Rooms 0.6.3 – OfficeCraft furniture with shader packs (experimental)

Works with 0.6.1/0.6.2 worlds.

### The problem

OfficeCraft draws its furniture with its own vertex buffers and its own shader, outside Minecraft's normal render types. Shader loaders (Oculus/Iris) replace Minecraft's rendering with the shader pack and never see those draws, so OfficeCraft furniture is invisible (only the block outlines remain).

### What Office Rooms does now

Office Rooms puts a thin wrapper around OfficeCraft's furniture renderer:

- **No shader pack active:** OfficeCraft draws exactly as before (unchanged).
- **Shader pack active (Oculus/Iris):** Office Rooms reads the same furniture meshes from your installed OfficeCraft JAR and draws them through Minecraft's normal entity render types, which the shader pack processes. Desks, chairs, monitors (on/off), lamps (lit/unlit), glass and water bottles (translucent), and props on desks are supported.

Nothing from OfficeCraft is included in Office Rooms; the meshes and textures are read at runtime from OfficeCraft itself. Tested against OfficeCraft 1.0.3.

### Setting (client config `office_rooms-client.toml`, section `[compat]`)

| `officeCraftShaderCompat` | Effect |
|---|---|
| `"AUTO"` (default) | shader-friendly drawing only while a shader pack is active |
| `"ALWAYS"` | always use the shader-friendly drawing |
| `"OFF"` | always OfficeCraft's own drawing |

The file is reloaded while the game runs. If anything goes wrong while drawing, Office Rooms logs an error and falls back to OfficeCraft's own renderer.

### Notes

- Lighting comes from the shader pack (or Minecraft's entity lighting), so furniture looks slightly different from OfficeCraft's own flat lighting.
- Furniture held in the hand or shown as an item still uses OfficeCraft's item renderer.
- Many furniture pieces are drawn every frame in both modes; with shaders, a lower render distance in the Office Rooms helps FPS.

---

## Office Rooms 0.6.2 – Spawn balance

Works with 0.6.1 worlds (no new world needed).

### More Hounds and Smilers

| | before | now |
|---|---|---|
| Hound: spawn chance per check (every 3 s) | 0.02 | **0.15** |
| Hound: at most near one player | 1 | **2** |
| Hound: one per … rooms | 160 | **60** |
| Smiler: spawn chance per check | 0.025 | **0.15** |
| Smiler: at most near one player | 1 | **2** |
| Smiler: one per … rooms | 140 | **60** |
| Smiler: max. block light at the spawn spot | 3 | **7** (it still fades away in light 10+) |

### Gaunts rule the dark exit levels (lowest 5 storeys)

| | before | now |
|---|---|---|
| One Gaunt per … rooms | 250 | **30** |
| At most near one player | 1 | **6** |
| Senses players through walls | no | **within 24 blocks** (`senseRange`) |
| Scream calls the pack | no | **every Gaunt within 40 blocks joins the hunt** (`alertRadius`) |
| Keeps chasing up to | 48 blocks | **64 blocks** (`followRange`) |

Speed and damage are unchanged. Gaunts still only appear when you are in the five dark bottom storeys.

### Roaming Headless Warden less constant

A roaming Headless Warden used to be almost always near you (its limit was refilled every 3 seconds). It now has its own spawn chance (`wardenSpawnChance` = 0.01, roughly every 5 minutes). The guardians locked in Sculk Offices are unchanged.

### Your existing config is updated once

Old `office_rooms-common.toml` files kept their old spawn values. 0.6.2 writes the new values into the file once (marker `balanceVersion = 2` in `[spawning]`). Afterwards your own changes stay. Set `balanceVersion = 0` to reset all spawn values to the defaults again.

### Fix

The Hound's `spawnChance` was in the `[gaunt]` section of the config by mistake (0.6.0/0.6.1). It is now in `[hound]`.

---

## Office Rooms 0.6.1 – Wool floors, lower building, crash fix

**Important: start a new world (or delete the folder `dimensions/office_rooms/office_rooms` inside an old world's save).** The building grid and the dimension height changed. Chunks generated by 0.6.0 or older would no longer line up with new ones, and everything above Y 543 in an old world is cut off.

- **Grey wool floors instead of carpet.** Every storey slab is now two blocks thick: smooth quartz underneath (the ceiling of the room below, unchanged) and grey wool on top (the floor). No more carpet layer, so no more seams between carpet and ceiling and no more mobs stepping on 1/16-block carpets. Walls, ceilings and every edge seen from an atrium stay quartz; wool only appears where you can walk. Rooms are still 4 blocks tall, furniture, doors, windows, atriums and special rooms are unchanged. Stairs got one more step.
- **Lower building for better performance.** The dimension is now 608 blocks tall (Y −64 to 543, 60 % of before) with 101 storeys (previously 204).
- **Poolrooms** adapted to the new storey grid (hall and gallery still connect to the office doors).
- **Crash fix:** leaving a world could crash with "Entity is already tracked!" when falling sand was unloaded near the portal code. The portal check now only reacts to sand that actually lands.
- Office doors on the edge of the Poolrooms that open onto the tile ring are intended (a doorway filled with pool tiles); the world check no longer reports them as errors.

**Shaders:** OfficeCraft draws its furniture with its own renderer. With Oculus/Iris shader packs (for example BSL) active, that furniture can turn invisible (only outlines) and FPS drop sharply. Turn the shader pack off (Oculus: `K`, or Video Settings → Shader Packs) while you are in the Office Rooms.

### Screenshots

![Open-plan office with grey wool floor](bilder/37_wool_floor_0.6.1.png)

![Poolrooms on the new grid](bilder/38_poolrooms_0.6.1.png)

---

## Office Rooms 0.6.0 – Shrines, Poolrooms and new entities

Everything from earlier versions is still in the mod. This update only adds new content.

### Install (short)

1. Forge 1.20.1-47.4.28, Java 17.
2. Put `OfficeRooms-0.6.0.jar` into `mods/` on the server **and** every client. Optional (recommended): OfficeCraft 1.0.3 and Refurbished Furniture 1.0.20 (+ Framework), downloaded from their official pages.
3. Create a new world (or explore new chunks). The shrine and the Poolrooms only generate in chunks that did not exist before.
4. Updating from 0.5.0: the config file `office_rooms-common.toml` gets the new sections `[hound]`, `[smiler]` and `[colony]` added automatically.

### 1. Abandoned Office Shrine (Overworld, very rare)

- **What it is:** a fragment of the Office Rooms lying half-buried and overgrown in the Overworld.
  - broken quartz walls, a corner pillar and a piece of ceiling with a dead light panel
  - grey carpet, moss, vines, cobwebs and creeping sculk
  - fallen ceiling slabs and debris scattered around it
- **The shrine:**
  - a lectern with a survivor's warning book
  - candles and a skull
  - a chest with **everything needed to open the portal**: a sculk sensor, sand, another warning book and a few supplies
- **Warning signs:** "Those who dare will never leave." and "Do not combine them. You won't survive."
- **Where:** only on dry, fairly flat ground in plains, meadows, forests, taiga, savanna, swamp and cherry groves.
  - It keeps its distance from villages.
  - Placement grid: 48 chunks spacing, 20 chunks separation.
  - Find one with `/locate structure office_rooms:abandoned_office_shrine`.

### 2. The Poolrooms (Office Rooms, extremely rare)

- **Where:** in about 1.2 % of regions (`poolrooms_region_chance`), five storeys (25 blocks) of the office building are replaced by a gigantic indoor pool complex covering the whole region (160 × 160 blocks).
- **Look:** cream tiled walls, floors and ceilings (new Pool Tile blocks), bright and evenly lit, completely empty.
- **Halls and corridors:**
  - huge halls linked by tiled arches
  - pillar halls and flooded halls
  - long, low corridors, some with a water channel
- **Water:** pools of different sizes and depths (1–3 blocks) with glowing tiles in the deeper ones.
- **Gallery:** an upper level runs around the whole complex, with railings and staircases.
- **Way in:** office doors on two storey levels lead straight in.
- **Hidden underwater labyrinth:**
  - The entrance is a tiny 1 × 2 pool in a corner of a dim hall, easy to overlook.
  - Below it is a flooded maze of tiled corridors with occasional sea lanterns and a few air pockets in the ceiling.
  - At the far end of the path are two treasure chests: diamonds, emeralds, gold, Heart of the Sea, trident, enchanted books, golden apples, a conduit, netherite scrap and more.
- **Also found:** lifeguard corners with a chest and sometimes a lectern with a Poolrooms logbook.
- **Ambient sounds of its own:** water lapping, distant splashes, echoing drips, the hum of a pool pump and rare distant calls.
- **Commands:** `/officerooms goto poolrooms` and `/officerooms goto poolrooms_maze`.

### 3. New entities (whole Office Rooms dimension, uncommon)

| Entity | Look | Behaviour |
|---|---|---|
| **The Hound** | Gaunt wolf body with a bloody human head | Aggressive, very high damage (12; kills in 2 hits on Normal). **Hold Mushroom Stew** (either hand) and it will not attack you: it stops mid-attack, sniffs and walks away, even if you hit it. Spawns anywhere in the dimension, at most one near you; on average one shows up every few minutes of exploring. |
| **The Smiler** | A tall veil of shadow and smoke, only a glowing smile and two eyes | Spawns only in dark spots (block light ≤ 3). Stalks slowly, faster when you look away. It giggles. Touching it kills instantly. If it gets stuck it reappears in the dark behind you. **Hold a torch and point it straight at it** (within 10 blocks, clear line of sight) and it dissolves. Bright light drives it off too. Ordinary weapons do not hurt it. |
| **The Colony** | An original creature: a crawling, many-legged mass of pale, diseased growth with glowing spots | Extremely rare (roughly one encounter per 20 minutes of exploring, never more than one near you). Screams when it picks up your trail and hunts you without needing to see you. It is faster than a walking player but slower than a sprinting one (about 4.6 blocks/s) and deals 14 damage (vanilla difficulty scaling applies). **After about 10 seconds of continuous pursuit it falters**: it shakes and nearly stops for 3 seconds. That is your chance to get away. |

All three also spawn inside the Poolrooms. Settings are in `office_rooms-common.toml`, sections `[hound]`, `[smiler]` and `[colony]` (health, damage, speed, `spawnChance`, `maxNearPlayer`, Colony chase and falter seconds, Smiler max light). Spawn eggs are in the creative tab.

### 4. Books

New English books:
- three warning books for the shrine
- two Poolrooms logbooks
- four survivor notes that hint at the Hound (mushroom stew), the Smiler (torch), the Colony (it falters after ten seconds) and the Poolrooms

### New blocks and crafting

Pool Tile, Pool Floor Tile, Pool Tile Stairs and Pool Tile Slab. Mine them in the Poolrooms with a pickaxe (they drop themselves).

| Result | Recipe |
|---|---|
| 4 Pool Tile Stairs | 6 Pool Tile in a stair shape (crafting table) |
| 6 Pool Tile Slabs | 3 Pool Tile in a row |
| 1 Pool Tile Stairs / 2 Pool Tile Slabs | 1 Pool Tile in the stonecutter |
| Pool Floor Tile ↔ Pool Tile | 1:1 in the stonecutter |

Opening the portal is unchanged from 0.5.0: drop sand onto a sculk sensor to tear open a Liminal Rift. The shrine chest contains both.

### Performance

- Regions without Poolrooms are rejected by a cheap hash check before any cache is touched.
- The internal plan caches now spread neighbouring regions across hash buckets (this also speeds up the older parts of the generator slightly).
- The new mobs are only spawned near players, capped per player and despawn beyond 96 blocks like the other Office Rooms mobs.
- Measured on a 2-core test machine, server only: fresh-chunk generation of 49 chunks took about the same time as 0.5.0 (0.5.0: 6.8–8.2 s, 0.6.0: 7.2–7.9 s).

### Screenshots

| | |
|---|---|
| ![Shrine](bilder/31_office_shrine.png) Abandoned Office Shrine | ![Poolrooms hall](bilder/32_poolrooms_hall.png) Poolrooms hall with gallery |
| ![Pool](bilder/33_poolrooms_pool.png) Pool with glowing tiles | ![Entities](bilder/34_hound_smiler_colony.png) Hound, Smiler and Colony (spawned for the picture) |
| ![Smiler](bilder/35_smiler.png) The Smiler in an office | ![Colony](bilder/36_colony.png) The Colony |
