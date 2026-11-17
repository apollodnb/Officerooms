# Office Rooms 0.5.0 – Liminal World Expansion

Everything from 0.1.0–0.4.0 is still in the mod; this update only adds and improves.

## Install (short)

1. Install Forge 1.20.1-47.4.28 (Java 17).
2. Put `OfficeRooms-0.5.0.jar` into `mods/`. Optional, recommended: OfficeCraft 1.0.3 and MrCrayfish's Furniture Mod: Refurbished 1.0.20 (+ Framework). Download them from their official pages; Office Rooms contains none of their files.
3. Create a new world (new rooms and structures only appear in newly generated chunks).
4. Enter the Office Rooms:
   - **Survival:** place a **sculk sensor**, then drop **sand** onto it from above (e.g. place sand two blocks above it and remove the block below the sand). The sand breaks on the sensor, the vibration activates it, and a **Liminal Rift** appears exactly there. Walk into it.
   - **Command:** `/officerooms enter`.
5. Leave: find an **Exit Room** on the lowest storey (the rift in a crying-obsidian frame) and walk in. You return to where you entered; if no point is saved, to the world spawn.

## Crafting

No new recipes in 0.5.0. Existing recipes (Almond Water, Glitched Ingot gear, Multidimensional Rod) are unchanged – see `docs/PHASE4.md`. The rift itself is not an item: it is created only by the sand-on-sculk-sensor ritual and drops nothing when broken.

## New content

### Rooms and structures

| Structure | Rarity (default) | Contents |
|---|---|---|
| **Abandoned Outpost (camp)** | rare (`abandoned_camp_chance` 0.004) | 1–3 wool tents with beds and door mats, campfire (sometimes cold, sometimes with forgotten food), log benches, table and chairs (sometimes just stools), crates, coolers, storage jars, grill, barrels, hay, lectern with a survivor's book, overgrown floor (coarse dirt, moss, podzol, dead bushes, vines, cobwebs). Good loot (`chests/abandoned_camp`). Looks as if the occupants left in a hurry. |
| **Oasis / Tropical Waterpark** | extremely rare (max. one per region, `oasis_region_chance` 0.06), up to 32×32 | Beach sand, 2–3 sunken pools with sandstone rims, fountain, palms with cocoa, loungers with umbrellas, coolers, beach bars (table, stools, grill), bamboo planters, plank boardwalks. Exclusive loot (`chests/oasis`, incl. the oasis treasure pool). Never overwrites other special rooms. |
| **Exit Room** | lowest storey (`exit_room_chance` 0.08) | Return rift to the Overworld in a crying-obsidian frame. |
| **Villager Outpost (improved)** | as before | Organic, jittered layout; wooden stair ring around the field (corners open); survival decor and improvised storage (barrels, crates, coolers, storage jars, workbench, grill, recycle bin, OfficeCraft water cooler); dining table with chairs and dishes; some fire seats are scavenged OfficeCraft office chairs; one cat. |
| **Lush Cave Office (improved)** | as before | 1–2 frogs. |

Furniture uses Refurbished Furniture and OfficeCraft when installed; otherwise vanilla blocks are used.

### Creatures

- **The Gaunt** – an original creature for the dark bottom storeys: about player height, emaciated, long arms, glowing red eyes. Extremely fast and aggressive, screams when it locks on. Rare: only spawns while a player is in the dark storeys, at least 24 blocks away, at most 1 near a player (`[gaunt]` config: health 40, damage 9, speed 0.38, roomsPerMob 250, maxNearPlayer 1). Spawn egg available.
- **The Observer** – a tall, slim, dark figure seen far away at the end of corridors or behind windows, sometimes briefly between furniture or behind doors. Never spawns in front of the player, never attacks. It vanishes when you approach, stare at it, or hit it. In the deepest storeys a rarer variant brings brief darkness and a whisper behind you. Extremely rare (`[observer] chance` 0.02).
- **Wolfspider** is much quieter (volume 0.45, footsteps 0.08).

### Audio

- **Main music**: plays once when entering, then every 30 minutes (real time, `musicIntervalMinutes`). Leaving the dimension resets it; chunk changes never restart it; never more than one instance.
- **Ambient sounds**: steps, knocking, doors, neon buzz, metal, creaking, distant voices, rare whispers, wind, dripping, rustling, rumble and "something moving behind you". Separate pools for office, lush and dark storeys; positional (8–22 blocks away, or 2–4 blocks behind you); random pauses of 45–150 s (`ambientMinSeconds`/`ambientMaxSeconds`); paused while the music plays.

### Survivor Stories

24 English books (87 pages): multi-part diaries and notes (the Kestrel expedition, Reyes, June Albright, Tally, Vos, the Relay, Heron, camp notes, torn pages) with hints at real places – the oasis, the exit rift, water rooms, the sculk guardian, the Headless Warden, the Observer, the Gaunt, frogs, cats and almond water. Found on lecterns in camps and in loot of outposts, office rooms, labs/bunkers and rare structures.

### Fixes and performance

- Computers, monitors and keyboards now face their chairs in all office layouts.
- Fewer workplaces, removed only at row ends (no visible gaps).
- Carpets: `/officerooms verify` now checks that every carpet sits on a full block.

### Commands

- `/officerooms goto abandoned_camp | oasis | exit_room` (plus all earlier room types)
- `/officerooms observer` – makes the Observer appear near you (for testing)

## Open features / known limitations

- The Oasis is so rare that `/officerooms goto oasis` may report none nearby; move further away and try again.
- In one dev test the music stopped once by itself about 50 s after joining during heavy chunk loading (software-rendered test environment); it came back on the next interval. It could not be reproduced afterwards.
- Furniture orientation of coolers, grills and stools was checked only visually, not for every variant.
- Old worlds: existing chunks are not changed.
