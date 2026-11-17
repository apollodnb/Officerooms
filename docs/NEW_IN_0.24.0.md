# New in Office Rooms 0.24.0 – Deathmoth, full test pass, fixes

## The Deathmoth (new creature)
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

## Other changes
- **Hounds** appear a little more often:
  - roomsPerMob 60 → 45;
  - spawnChance 0.15 → 0.2.
- **Wolfspiders** are rarer: wolfspiderRoomsPerMob 40 → 70.
- Existing config files are updated only where they still hold the old default values.
- **Atria** are a third more frequent (density 0.075 → 0.1), but only in **newly created worlds**. Existing worlds keep their layout, so no seams appear at already explored places.
- **Level Fun**: when one Partygoer turns aggressive, it immediately alerts every Partygoer within 48 blocks, and they all come for you.

## Fixes from the full test pass
- **Performance (big):**
  - When new areas were furnished, OfficeCraft furniture at a chunk edge made the server wait for the neighbouring chunk to generate. In one pass that took up to several seconds.
  - Furniture is now placed only once all neighbouring chunks are loaded.
  - The profile while exploring: our share of the server thread dropped from about 66 to 6 of 300 samples. Standing still, the mod no longer appears in the profile at all (4.4 ms per tick).
- **Sculk offices:** in small or crowded rooms the Headless Warden got no spot. It now uses any free tile.
- **`/officerooms blackout warn|start`** now explains why no blackout is possible: no player outside spectator mode in the Office Rooms.
