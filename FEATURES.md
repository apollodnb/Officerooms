# Office Rooms – Feature list (0.9.0 beta)

## Implemented in 0.1.0 (phase 1, architecture)

### Dimension and rendering
- Dimension `office_rooms:office_rooms` (since 0.6.1: Y −64 to 543, 608 blocks, 101 storeys; originally 1024 blocks)
- Own chunk generator `office_rooms:office_rooms` with fully configurable settings in the datapack
- Empty biome without vanilla mob spawns, no structures, features or carvers
- Own client dimension effects: no sky (dark haze since 0.3.0)

### Room system
- Deterministic 3D grid: units (4 blocks), modules (16 × 16), regions (160 × 160 × full height)
- Each region is planned in advance, chunks only query the plan: no overlaps, no duplicated or cut-off rooms
- Room sizes in 9 weighted classes, from 8 × 8 to 80 × 64 blocks
- Large rooms over several modules as real connected rooms (no partition walls)
- L-, T- and offset floor plans (attached wings, appended leftover areas)
- Room heights 90 % / 7 % / 2 % / 1 % (4 / 5–8 / 9–16 / 17–28), independent of the storey grid, with fallbacks when space conflicts
- Corridor networks: narrow (3) and wide (7), straight, long, L, T, crossings, multiple branches
- Straight staircases between all storeys (≈ 3 per storey and region)

### Doors and passages
- Normal (1), double (2), wide (3) door openings, fully open passages across several wall sections
- Positions left, centre, right, random offset; several openings per wall
- Openings only where both sides are walkable (never into walls, solid areas or the void, never half-height)
- Spanning tree + loops, preferably via corridors; unreachable rooms are filled solid (≈ 0.1 %)
- Doors across region borders, global reachability tested

### Atriums and windows
- 7 atrium classes from 12 to 300 blocks tall, including shafts, halls and mega atriums (up to 120 × 120)
- Fully planned in advance, always enclosed (margins at the sides, top and bottom)
- Galleries all around or on two sides, bridges, parapets
- Windows only between a reachable room and an atrium, 2×2 / 2×3 / 3×2 / 3×3, not on every wall or room

### Tools
- `/officerooms enter | window [minHeight] | info | stats | verify [radius]`
- F3 debug screen: region, storey, room
- `PlanVerifier`: checks real world blocks against the plan and the rules
- JUnit tests of the planner, datapack validator

## Implemented in 0.2.0/0.3.0 (phase 2: liminal office)

- More atriums (density 0.075), several separate atriums per region
- Rare windows between two neighbouring offices (2 % per room pair)
- Grey carpet floor (grey wool since 0.6.1), white walls and ceilings
- Very dark dimension (`ambient_light` 0.07), light only from ceiling lights; variants normal, dimmed, partly broken, dark, bright corridor, dark corridor, lit and dark atriums
- Room types: empty, standard office, open-plan office (3 layouts), meeting room, break room with kitchen, server room, archive; corridors with water coolers
- Furniture from OfficeCraft and Refurbished, optional, with vanilla fallback
- Placement rules: inside the room only, no overlaps, doors and windows kept free, paths stay connected, never across chunk borders, deterministic, no double placement
- Lazy furnishing near players (memory)
- Loot: configurable per room type
- All values in the datapack (`furnishing`, `windows.office_pair_chance`, `atria.density`)
- Commands `goto <type>`; `verify` also checks furniture and office windows

## Implemented in 0.3.0 (phase 3: creatures and audio)

- Wolfspider: own model and black texture, attack with Wither, distorted reversed howl
- Headless Warden: headless model with its own animations, blind, pure sound AI (also through walls), sneaking is quiet, scream when the hunt begins, heartbeat nearby, very rare
- Spawns based on the room plan, never inside walls or furniture, despawn at long distance
- Music: three own tracks, only in the dimension (one at random on entering, then about one per half hour, otherwise only hum and ambience); Level Fun has its own looping music; seamless without restarting on chunk changes
- Quiet lamp hum as a central loop
- All values in `office_rooms-common.toml` / `office_rooms-client.toml`, spawn eggs, `/officerooms mobs`, `/officerooms spawning`

## Implemented in 0.4.0 (phase 4)

- Performance: own ceiling lights (block model instead of OfficeCraft furniture), empty workplaces in open-plan offices, faster generation (room rectangles per region), fewer server checks, no unnecessary sound loops
- Villager Outpost: working village (professions, beds, workstations, field, bell, iron golem, trading), closed with an iron door
- Lush Cave Office with trees; the only source of wood in the dimension
- Water Office: flooded office, sealed, iron door with lever
- Sculk Office: exactly one Headless Warden, special chest
- Lowest five storeys without artificial light
- Wolfspider 1/40 rooms, friendly variant (5 %), tameable with mushroom stew
- New Headless Warden model (more limbs, glitched)
- Loot in five tiers plus wood, chances central in the TOML config
- Items: Almond Water, Glitched Ingot (50 % Warden drop), Multidimensional Rod, Multidimensional tools and armor (+20 % over Netherite)

Details (German): [docs/PHASE4.md](docs/PHASE4.md)

## Implemented in 0.5.0 (Liminal World Expansion)

- Sculk-sensor + sand portal (Liminal Rift), exit rooms on the lowest storey with return rifts
- The Gaunt (dark storeys), The Observer (extremely rare, harmless)
- Abandoned Outposts, Oasis / Tropical Waterpark, improved Villager Outposts (stair ring, survival decor, cat), frogs in lush rooms
- Furniture from Refurbished Furniture and OfficeCraft in the new structures (vanilla fallback)
- Survivor Stories (24 English books), new loot tables
- Music timer (real time, random track on entry, then about every 30 min ±30 %), Level Fun music, ambient sound pools
- 0.40.0: the quest book installs itself into FTB Quests and merges into a modpack's own quest book (config `installQuests`)
- 0.39.1: logo, description, credits and license in the Forge mod list; old unused creature models removed
- 0.39.0: rare research outposts in the Overworld: fenced field stations on flat land and drilling rigs over deep ocean (same gateway procedure)
- 0.38.1: fixes: Poolrooms that could not be built, hotel L-stairs (sixth step, beds in one rotation), Level 0 and Maze ramps walkable to the top
- 0.38.0: Underside Key needs the cinema Ticket Stub (seven artifacts); quests for the cinema, ticket, top floors of cinema and hotel, slides and Aranea Membri (120 quests)
- 0.37.1: landings with railings at the top of the cinema stairs; Aranea Membri on every cinema floor; Hazard Zone fix for the cinema
- 0.37.0: **Cinema multiplex** (new worlds): three cinema floors with 36 auditoriums, lobby atrium with galleries and railings, freestanding stairs with landings
- 0.35.0: Poolrooms 8 storeys, huge halls, pools up to 10 deep, round arches (some in water), arcades in deep pools, water slides with tube, pool ladders, mosaic tiles
- 0.34.0: Carpeted Ramp block; pit fields (Level 0 floors 2–4, Maze upper levels); The Maze has three levels (ramps, pits, holes between them), vault on a random level
- 0.33.0: Level 0 and The Maze: shared Level 0 palette, floors one block higher (Maze two storeys), carpeted ramps between Level 0 floors, split levels in the Maze, holes in the floor, soaked carpet, pillars, sealed closets, half-height rooms, lights in walls, windows, light switches
- 0.32.0: **Aranea Membri** (rare in the offices and cinemas): lurks invisibly in the ceiling (scratching, dust, broken ceiling tiles), drops when you walk underneath, kills the lights while hunting, climbs back up
- 0.31.0: reverb by room size (OpenAL EFX, client config `roomReverb`)
- 0.31.0: **The Abandoned Cinema** (new worlds): lobby with box office and concessions, two corridors, twelve auditoriums in five kinds, 32 new blocks, Ticket Stub, own loot
- 0.30.1: Flayed and Hound loop their full hunting recording while they chase; louder sounds and music
- 0.30.0: new Flayed/Hound/Colony screams and hurt sounds from the author's recordings; creepier hurt sounds for all other mobs
- Workplace alignment fix, carpet check in `/officerooms verify`, world-generation crash fix

## Implemented in 0.6.0 (Shrines, Poolrooms & Horror Entities)

- **Abandoned Office Shrine** (Overworld, very rare): overgrown office fragment with a lectern book, warning signs, candles, a skull and a chest with the portal ingredients (sculk sensor, sand) plus supplies and a warning book. Natural biomes only, away from villages. `/locate structure office_rooms:abandoned_office_shrine`.
- **The Poolrooms** (Office Rooms dimension, ~1.2 % of regions): five storeys replaced by a 160 × 160 tiled pool complex with halls, arches, corridors, pools of depth 1–3, a perimeter gallery with stairs, dense lighting, chests and logbooks. A tiny 1 × 2 pool hides a flooded labyrinth with air pockets and two vault chests at its far end. Own ambient sound set (lapping, splashes, drips, pump, echo).
- **The Hound**: very high damage; leaves you alone while you hold Mushroom Stew (either hand), even mid-attack.
- **The Smiler**: spawns in the dark, stalks, kills on touch; dissolves when a held torch is pointed at it or when bright light reaches it.
- **The Colony** (original creature): extremely rare, screams and hunts without line of sight, falters after ~10 s of pursuit for 3 s.
- New blocks Pool Tile, Pool Floor Tile, Pool Tile Stairs, Pool Tile Slab (with crafting and stonecutter recipes); 9 new English books; spawn eggs; English subtitles for all new sounds.
- Performance: Poolrooms lookup has a lock-free fast path; the plan caches now mix their region keys so neighbouring regions no longer share hash buckets.

## Implemented in 0.6.1

- Two-block storey slab: smooth quartz below (ceiling), grey wool on top (floor); carpet layer removed. Room height 4, furniture, doors, windows, atriums and special rooms unchanged; stairs physically 6 blocks per storey.
- Dimension height 608 (Y −64..543), 101 storeys.
- Poolrooms band realigned to the 6-block grid (28 blocks tall).
- Fix: "Entity is already tracked!" crash on world exit (PortalIgnition reads blocks only for sand that really landed).
- World check: office doors closed by the Poolrooms tile ring are counted as intended.

## Implemented in 0.6.2

- Spawn balance: Hound and Smiler spawn chance 0.15, up to 2 each near a player; Smilers spawn up to block light 7.
- Gaunts in the 5 dark bottom storeys: 1 per 30 rooms, up to 6 per player, sense players through walls within 24 blocks, a scream calls every Gaunt within 40 blocks, follow range 64.
- Roaming Headless Warden gets its own spawn chance (0.01); Sculk Office guardians unchanged.
- One-time config migration (`balanceVersion`), Hound `spawnChance` moved to `[hound]`.

## Implemented in 0.6.3

- Experimental OfficeCraft shader compatibility (`client/OfficeCraftShaderCompat`): wraps OfficeCraft's block entity renderer; with an active Oculus/Iris shader pack the furniture meshes are read from the OfficeCraft JAR at runtime and drawn via entity render types. Client config `[compat] officeCraftShaderCompat = AUTO/ALWAYS/OFF`.

## Implemented in 0.7.0

- Items: Totem of Mercy (death in the Office Rooms → Overworld), Liquid Pain (instant death, own damage type), Witherfang / Leaden Edge / Venomtongue (enchantable swords with Wither / Slowness / Poison on hit). All in the loot tiers.
- The Hunter: 3.6-block line figure in the Poolrooms, always one per visit, senses players up to 96 blocks.
- Fix: sealed special rooms never stay locked (door control search over the whole wall, wooden door as last resort).
- Fix: `goto atrium`.
- Performance: far, idle creatures pause (`freezeFarMobs`), vanilla spawner off in the dimension; dimension tick 31 → 5.8 ms in the test.

## Implemented in 0.7.1

- Multidimensional armor with a real shape: Netherite-style helmet with open face, chestplate with shoulder plates and bracers, leggings with belt, thigh plates and knee guards, boots only around the feet (`tools/gen_armor_070.py`).
- Everything English: command output, `/officerooms verify`, config comments, log messages, function files, mod description; the `de_de` language file shows the English texts.

## Implemented in 0.8.0 (beta)

- Megastructure framework: at most one megastructure per region. The Poolrooms keep their placement; the Neighbourhood and The Maze use the same chance (`poolrooms_region_chance`, default 0.012), so all three are equally rare.
- The Neighbourhood: 4 storeys tall, unlit hall (160 × 160) with 20 identical houses on identical lots, asphalt streets, sidewalks, lawns and fences. Every third house holds a persistent Smiler, Headless Warden, Hound or Colony; the other 14 houses hold a chest (`chests/neighbourhood_house`). Up to 3 creatures (Wolfspider, Hound, Smiler) roam the streets. Doors of the neighbouring offices on the entrance storey lead in.
- The Maze (Level 0): one storey, 26 × 26 cells of yellow wallpaper, damp carpet, ceiling tiles and fluorescent strips (12 % dead), with loops and open pillar halls. Mining Fatigue III inside. 3–5 exits cut through the west/north office wall into ordinary rooms or corridors. Always one Hunter, plus Wolfspiders, Hounds, Smilers, Gaunts and rarely a Headless Warden. Hidden security vault (unbreakable walls, floor and ceiling) behind a security door of level 1 (60 %), 2 (30 %) or 3 (10 %) with a sign; chest `chests/maze_vault` (the rarest loot).
- Blocks: Maze Wallpaper, Damp Carpet, Maze Ceiling Tile (obtainable), Security Wall and Security Door (unbreakable, door opens only with a card of sufficient level, ignores redstone).
- Items: The Temporal Watch (Slowness VI + Weakness II for 4 s on every creature within 8 blocks, 2 min cooldown; rare tier and maze vault), Anomaly Detector (signal bar and beeping get stronger the closer a creature is, within 32 blocks, never names it; uncommon tier, Neighbourhood houses), Level 1/2/3 Access Cards (special tier: L1/L2; Neighbourhood houses: L1/L2; Poolrooms vault: L2/L3; maze vault: L3).
- Commands: `goto neighbourhood`, `goto maze`, `goto maze_vault`.

## Implemented in 0.8.1 (beta)

- `poolrooms_region_chance` default doubled to 0.024: Poolrooms, Neighbourhood and The Maze each in about 2.4 % of regions (new worlds only).

## Implemented in 0.8.2 (beta)

- Spawns only in entity-ticking chunks; at most one Hunter per band also with several players (extra ones are removed). Tested with two clients at once.

## Implemented in 0.8.3 (beta)

- `poolrooms_region_chance` default 0.07 (each megastructure in ~7 % of regions, new worlds only).
- Fix: mazes next to other megastructures always get 3–5 exits; Poolrooms layouts without a suitable hall still get their labyrinth and vault.

## Implemented in 0.9.0 (beta)

- Elevators (one shaft per region without megastructure). 0.26.0: steel cabin with sliding doors, a ceiling light and a wall panel; type a floor, the doors close and the cabin stops on a random floor (never the one typed), or inside a megastructure within the floors passed; everything in the cabin rides along; 5 minutes cooldown; rare stalls during blackouts, visitors and knocking. Self-built stacked Elevator Plates: jump up, sneak down, right-click = floors per ride.
- 0.29.0: Curiosity rooms (new worlds): Cold Storage, The Last Meeting, Mail Room, Rain Room, Canteen (vending machines), Arcade (rare); Old Coins in ~30 % of ordinary containers; config `[curiosities]`; quest chapter XI.
- 0.28.1: Poolrooms (new worlds): stepped depths up to 4, drained pools, children's pools, benches, lifeguard chairs, arches to nowhere; client haze and ripples.
- 0.28.0: Hotel: wall fix beside the aisle; new worlds: twin/trashed/static-TV/phone/"do not disturb" rooms, wall sconces, ice machines, housekeeping carts, teal carpet upstairs; `content_version` world setting.
- 0.27.1: Hound redesigned (vertebrae ridge and neck, flesh hanging from the face, thin legs) and faster (0.33, charge 1.3×).
- 0.27.0: Maze is the Flayed's alone (no other creatures, no generic Flayed spawns next to it, no hint hum); Partygoers stand and wave and attack together within 2 blocks; Gaunt 60 / Colony 80 health; Research Files and Keycards rarer in ordinary containers; new worlds: more hidden rooms (12 attempts), fewer Villager Outposts (1.2 %).
- 0.26.2/0.26.3: spindly Gaunt (thinner legs and claws) and Colony (hair-thin stilt limbs); the Colony hunts the Poolrooms instead of the Hunter (always at least one there); the Hunter no longer spawns naturally.
- 0.26.1: hostile Wolfspider redesigned (gaunt skull, hair-thin three-segment stilt legs, glowing eyes, jerky twitching animation; hitbox 0.95 × 0.95).
- 0.26.0: beds work in the Office Rooms (sleep, respawn point); the dimension is 30 % darker; the Flayed's scream shakes the camera.
- Megastructure sound hints, flickering lights near creatures (`[atmosphere]` config).
- Secure storage rooms of level 1-3 with tiered loot; `/officerooms locate elevator|secure_storage`.
- Shader compatibility: simplified furniture shapes by distance, render distance, no furniture shadows by default.

## Known limitations

- Furniture only appears near players (3 chunks, ±3 storeys); seen from far away, for example across an atrium, distant rooms are still empty.
- OfficeCraft 1.0.3 has a threading bug during chunk meshing. Office Rooms avoids it by pre-filling the cache; other mods that place OfficeCraft furniture en masse could still trigger it.
- The Headless Warden hears through walls without damping and only reacts to player sounds; creative and spectator players are ignored.
- The dimension is built over its full height. It is heavier for the client than the Overworld, furniture block entities add to that.
- The vanilla fallback is simple: partitions, boards, vending and coffee machines are missing, the fallback fridge has no loot.
- Water offices can only be opened from outside with the lever. Whoever is inside while someone outside flips the lever has to break the door.
- Glow berries in lush rooms of the lowest storeys give light (property of the room type).
- Villagers stay in their room. An iron golem is only created once at generation; vanilla golem spawns were not tested.
- The friendly Wolfspider has no baby form and cannot be bred.
- (0.6.0) The Poolrooms and the shrine only appear in newly generated chunks. The Poolrooms need the default region size (160 blocks).
- (0.6.1) Worlds from 0.6.0 or older need a new world (or a reset Office Rooms dimension): grid and height changed.
- (0.6.3) OfficeCraft furniture with shader packs: experimental compatibility drawing, not yet tested with a real shader pack.
- (0.6.0) The Smiler can only be removed with a torch, bright light or commands; ordinary weapons do not hurt it.
- (0.7.0) Creatures far away from every player pause (performance); they continue as soon as a player comes near (`freezeFarMobs`).
- (0.8.0) The Neighbourhood and The Maze only appear in newly generated chunks and need the default region size (160 blocks).
- (0.8.0) A maze exit cuts through the office wall; furniture of the neighbouring room may stand right behind it (it can be removed).
- (0.8.2) If the server is killed instead of stopped, creatures of recently unloaded chunks (e.g. Neighbourhood residents) can be lost.
- (0.8.0) The security door does not close by itself; access cards currently only exist for the maze vault.
- The design documents of versions 0.1–0.4 in `docs/` are written in German.

## Open / later phases

### Architecture
- Non-rectangular atriums (L-shaped, composite)
- Atriums across region borders (deliberately excluded to keep planning local)
- Spiral or landing staircases, stairs over several storeys, elevator shafts
- Rooms with mezzanines or galleries in tall offices
- Variable wall thickness, columns in large rooms
- Diagonal corridors
- Stepped suspended ceilings (quartz slabs) instead of full blocks

### Further ideas
- More room designs and materials
- Respawn inside the dimension
- Optional sound damping through walls and doors
- More creatures and room types

## Hidden rooms (0.11.0)

Six rare room types behind loose panels or doors: maintenance room, supply closet, forgotten break room, abandoned security office, research lab, concealed room. English notes, own loot tables, `/officerooms locate hideout`.

## Creature models (0.11.0)

GeckoLib models and animations for Gaunt, Headless Warden, Hound, Smiler, Colony, Hunter and Observer. The Wolfspider keeps its classic look.

## Research and progression (0.12.0)

Rare Research Files; Research Table: Heart Research (+1 heart, max +5, 3 files each) and Weapon Reforging (+3 damage per level, max 3 levels, 2/3/4 files; all vanilla weapons incl. bows, crossbows, tridents; enchantments kept). Ten craftable lab/security blocks. Labs and security offices are 11 x 8 rooms.

## The Flayed (0.13.0)

Extremely rare, wire-thin black figure (about 3.5 blocks tall). On sight: scream (standing still for 1.6 s), then a sprint at about 7.5 blocks/s, 18 damage; after 10 s of pursuit it falters for 3 s. 50% Research File drop. Config section [flayed].

## The Abandoned Hotel (0.14.0)

Megastructure (as rare as the Poolrooms): basement with vault (security level 2/3), lobby with reception, two guest floors with numbered rooms, beds, nightstands, TVs; loot chests; Hounds. 13 own blocks. The Flayed replaces the Hunter in the Maze.

## The Industrial Complex (0.15.0)

Megastructure: five levels of 16 x 16 concrete rooms full of pipes (pipe galleries with grate floors, control rooms, storerooms, generator rooms, staircases), grate doors, two generator halls through three levels with catwalks. One Flayed, Hounds. 10 own blocks, Electronic Component.

## Level Fun and the Partygoer (0.16.0)

Megastructure: a yellow party level with half-built walls, balloons, streamers, confetti, cake tables, gift boxes and =) on the walls. Partygoers follow you to about 7 blocks and attack if you come closer than 6. Much darker lighting in the whole dimension.

0.16.1: Level Fun has a red play carpet (roads and little cars) and decorative bouncy castles.

## HAZARD ZONE (0.17.0)

Every 100 min: 15 min warning on Hazard Monitors, 45 min toxic zone in a random megastructure (green gas, 8 hazard chests, rare creatures), 40 min cooldown. Gas Mask with filters, Night Vision Goggles, Hazard Visor (Toxic Crystal), Special Event Detector. Louder lamp hum, quieter music.

## Sound overhaul (0.18.0)

New dark, layered sounds for all hostile creatures except the Smiler, with variations; the Flayed has four long signature screams. New ambience (buzz, vents, pipes, distant creatures) and area soundscapes for the Industrial Complex, labs/security rooms, Hotel, Level Fun and Poolrooms; running generators hum.
- 0.18.1: `goto hotel | hotel_vault | industrial | level_fun | hideout | elevator | secure_storage` (nearest one, safe landing spot).

## The Underside (0.19.0)

Six artifacts (one per megastructure), the Underside Key, the bedrock opening, the Underside dimension (meadow under an upside-down meadow), the boss [Unknown] (20 s after arrival, quake warning, burrows in floor and ceiling, lunges, crawls; respawns on every visit), the Noclip Sword (10 s through walls, 30 s cooldown) and the two exit portals. 12 new survivor books with hints.
- 0.19.1: [Unknown] has black body hair (bristles on the body and legs, strands on the head).

## Research outposts (0.20.0)

Rare abandoned Project LIMINAL laboratories beside Ancient Cities (60 %), beside Jungle Temples (40 %, ladder shaft), in deep caves below y 0 (very rare) and in the Nether (rare), placed only in new chunks. Each has a gateway into the Office Rooms with a three-step activation: Main Power lever → warning sequence → red emergency button. New blocks: lab walls, floor and ceiling, hazard floor, power lever, emergency button, gateway monitors, control consoles, warning lights, facility lights, gateway frame, dimensional gateway. Research chests and notes.

## Level 0 (0.21.0)

Breaking the topmost ceiling of the Office Rooms leads into Level 0: an endless, fully connected four-floor yellow labyrinth (corridors, slits, pillar halls, oversized furniture, ladders, Threshold Rooms with holes). No mining, building, pearls or noclip. One immortal Flayed hunts the players. Rare hidden Manila Rooms hold a water bucket and a 1 × 1 passage that can only be swum through; the way out leads to a random office, with one reward per trip (3 Research Files, a Multidimensional Sword or 6 Glitched Ingots). 7 new books.
- 0.21.1: Level 0 is darker and has flickering, dead and irregular lights, dark zones, missing or stained ceiling tiles, vents, outlets, peeling wallpaper, spray-painted arrows and soaked carpet, plus oddly placed wooden furniture and paradoxical architecture (stairs into the ceiling, doors to nowhere, hanging walls, sudden low ceilings).
- 0.21.2: Level 0 uses Refurbished Furniture (chairs, table, sofas, drawer, cabinet) when that mod is installed; the ceiling chairs were removed.

## Blackouts (0.22.0)

A recurring event: the monitors announce a blackout (sector, floors, countdown), then an area of about 97 × 97 blocks over 7 storeys goes dark (client lightmap; the hum stops; the monitors inside die; monitors outside show the fuse-box floor). Gaunts come up in the dark. A sparking fuse box in the area is repaired with 3 Electronic Components; otherwise backup power returns after 20 minutes. Config `[blackout]`, command `/officerooms blackout`.
- 0.22.1: repairing the fuse box drops a maintenance kit (Research File + battery, ingots, filter or goggles) and XP.

## New standard rooms (0.23.0)

Training rooms (glowing screen, projector, lectern with a handout, aligned stacking chairs), executive offices (rug, dark oak desk, banker's lamp, leather chair, bookshelves, cabinets with better loot), receptions (desk with service bell, NOW SERVING 00, waiting chairs, magazines, doormats) and rare restrooms (tiled floor and walls, stalls with an occasionally locked door, basins with mirrors and a dripping tap). 26 new blocks; Refurbished Furniture is used where installed, with vanilla fallbacks. Rolled with a separate random stream, so existing layouts do not change.

## Deathmoth and full test pass (0.24.0)

The Deathmoth spawns only in the Office Rooms (never in Maze or Poolrooms regions, Level 0 or the Underside) as a population on dark walls. It sits motionless, bursts off when a player comes close, clings to the face (screen covered by wings, camera shake) and drains health for 3.5 s, then flies off. It has no glowing parts. Config `[deathmoth]`, command `/officerooms deathmoth`. Also: Partygoers alert the whole party, hounds more common, wolfspiders rarer, atria +1/3 in new worlds. Performance: furniture is only placed when the neighbouring chunks are loaded (no more waiting for chunk generation on the server thread).

## Quest book, milestones, real blackouts (0.25.0)

FTB Quests progression in `quests/config/ftbquests/` (10 chapters, 106 quests, item- and kill-gated, Level 0 optional, repeatable requisitions). Hidden advancements `office_rooms:milestones/*` for events (fuse box, research, outpost gateway, Level 0, megastructures discovered). `/officerooms goto` for each hidden room type, fuse_box, hazard_zone, outpost and shrine. Blackouts switch the ceiling lights off server-side (works with shaders), larger area. Fixes: invisible Level 0 Flayed, circling [Unknown].

## Labyrinth hotel and taller megastructures (0.36.0, new worlds)
- Hotel: 7 storeys; guest floors are a 15 × 15 grid of small rooms (guest, twin, bath, linen, lounge, dining, trashed, static TV, phone, barricaded, empty) with winding 3-wide corridors, kinks, connecting doors, room-number signs and L-shaped staircases in random spots (`HotelLayout`).
- Industrial Complex: 8 levels, generator halls through 4 levels, new tank rooms, foundries and locker rooms.
- Level Fun: 4 storeys with staircases, ball pits, disco floors, banquets and dark cells.
- Neighbourhood: 6 storeys high, two-storey houses with small differences (boarded windows, open doors, lanterns, mailboxes), street lamps and playground lots.
