# Office Rooms 0.7.0 – Bug fixes, performance, new items and The Hunter

Works with 0.6.1+ worlds. Everything from earlier versions is unchanged unless listed here.

## New items (all in the loot system)

| Item | What it does | Where |
|---|---|---|
| **Totem of Mercy** (extremely rare) | If you would die in the Office Rooms, it is used up, you survive and are brought to your bed/respawn anchor in the Overworld (or the world spawn), with Regeneration, Fire Resistance and Slow Falling. Works from anywhere in your inventory. Does not protect against Liquid Pain. | Poolrooms maze vault (~10 %), very rarely in the best Office Rooms chests |
| **Liquid Pain** | Looks like a drink. Kills you instantly when you drink it. Nothing saves you (no armor, totem or effect). Death message: "… drank Liquid Pain". | Uncommon loot |
| **Witherfang** | Sword (7.5 damage, enchantable like any sword). Every hit gives Wither II for 5 s. | Rare and special loot |
| **Leaden Edge** | Sword. Every hit gives Slowness III for 4 s. | Rare and special loot |
| **Venomtongue** | Sword. Every hit gives Poison II for 5 s (undead are immune to poison, as in vanilla). | Rare and special loot |

The three swords repair with diamonds in an anvil.

## The Hunter (Poolrooms only)

A huge (3.6 blocks), gaunt figure made only of thin dark lines – wireframe head with eye slits, wireframe torso, very long arms with line fingers. Whenever a player is in the Poolrooms, exactly one Hunter roams them; it appears 24–70 blocks away on the hall floor.

- Senses players up to 96 blocks away, even through walls, and follows relentlessly (a bit slower than sprinting).
- 120 health, 6 armor, 11 damage, no knockback. It hums like electric wire and screams when it finds you.
- Too tall for the 3-block office doors, so it stays in the Poolrooms.
- Spawn egg in the creative tab.

## Checked: Multidimensional gear, Glitched Ingot, Multidimensional Rod

All registered, with textures, worn-armor textures, recipes (ingots + rod) and loot sources. Measured drop rates per chest (400 rolls each):

| Chest | Multidimensional gear | Glitched Ingot | Rod |
|---|---|---|---|
| Sculk Office special chest | ~6.5 % | 1 % | 3 % |
| Oasis | – | 2.75 % | 5.5 % |
| Poolrooms maze vault | – | 15.5 % | – |

## Bug fixes

- Sealed special rooms (water/sculk offices) could get an iron door **without** a lever or button and stay locked forever. The door control now searches the whole door wall; if there is still no spot, the room gets a hand-operated wooden door instead.
- `/officerooms goto atrium` did nothing; it now leads to an atrium window.

## Performance

- Creatures far away from every player (more than 64 blocks horizontally or 16 blocks / ~3 storeys vertically) and not hunting now pause until a player comes near. The Office Rooms are built over the full height, so villagers, golems, cats, frogs and guardians on dozens of storeys above and below you used to tick constantly. Measured on the test server with the same 86 villagers and 56 golems loaded: **dimension tick 31 ms → 5.8 ms**.
- The vanilla mob spawner is switched off in the Office Rooms dimension (it had nothing to spawn there but still searched every chunk). Office Rooms' own spawns are unaffected.
- Settings in `office_rooms-common.toml`, `[spawning]`: `freezeFarMobs`, `freezeHorizontal`, `freezeVertical`.
