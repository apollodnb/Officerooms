# Test report 0.8.0 (beta)

Only tests that were actually run are listed. Environment: production server (Forge 47.4.28, Temurin 17, seed 4711, fresh world) with OfficeCraft and Refurbished, dev client on Xvfb (render distance 4).

## Build and unit tests

- `./gradlew build`: OK, `OfficeRooms-0.8.0.jar`.
- 14 unit tests, 0 failures:
  - `PlannerTest` (8), `SpecialRoomsTest` (3, megastructure types excluded from the per-room sample).
  - New `MegastructuresTest`:
    - 3600 regions: Poolrooms 54, Neighbourhood 56, Maze 48; never two megastructures in one region.
    - Neighbourhood: 20 houses, 6 with residents, chest and creature spots free, no light part.
    - 12 mazes: 3–5 exits each; the vault has exactly one opening; vault floor and ceiling are security plates; flood fill from the first exit reaches every other exit, the vault door and 100 % of the open floor.

## In game

| Test | Result |
|---|---|
| `goto neighbourhood` | Neighbourhood at storeys 75–78 (Y 388), houses, streets and lawns generated as planned (screenshots with night vision) |
| Darkness | without night vision the hall is completely black |
| Residents | 6 persistent residents (tag `office_rooms.neighbourhood`), e.g. a Hound exactly at the planned spot of house 6 |
| House chests | chests carry `office_rooms:chests/neighbourhood_house` (two houses checked) |
| Ring passages | west ring wall: two 3-wide passages at walk level where neighbouring office doors meet it |
| `goto maze` | Maze at storey 89, 5 exits; standing in an exit you look into an office room |
| Mining Fatigue | survival player in the maze: Mining Fatigue III (amplifier 2), refreshed |
| Creatures in the maze | after ~45 s within 80 blocks: 1 Hunter, 2 Hounds, 2 Smilers, 4 Wolfspiders, 1 Gaunt, 1 Headless Warden (part of them from the normal office spawns on nearby storeys) |
| `goto maze_vault` | security door level 2 with sign "SECURITY / LEVEL 2 / Access card / required" |
| Door without card | "ACCESS DENIED - Level 2 clearance required", door stays closed |
| Door with Level 1 card | stays closed |
| Door with Level 2 card | opens |
| Vault | closed steel room with a lit ceiling strip and the chest `office_rooms:chests/maze_vault` |
| Security wall | held left-click 6 s with a Netherite pickaxe in survival (under Mining Fatigue): block unchanged |
| Anomaly Detector | signal 40 % (unknown creature ~19 blocks away) → 66 % (Wolfspider 8 blocks) → 91 % (3 blocks), colour yellow → gold → red |
| Temporal Watch | message "Time slows for 2 creatures."; Hound and Wolfspider got Slowness (amplifier 5) and Weakness, 54 ticks left after ~1.3 s (80 ticks total) |
| `verify 2` | maze exit area, maze vault area, Neighbourhood west and east edges: 0 deviations, 0 errors, `RESULT: OK` each |
| Logs | no exceptions on server or client, no missing models or textures |

## Loot statistics (`loot insert`)

**maze_vault, 200 chests:** never empty; share of chests containing the item (stack appearances / chests): Totem of Mercy 36 %, Temporal Watch 25 %, Level 3 card 12 %, effect swords 62 %, Multidimensional pieces 1.5 per chest, Glitched Ingot 43 %, Multidimensional Rod 49 %, enchanted golden apple 47 %, netherite ingots 54 %.

**neighbourhood_house, 300 chests:** never empty; Anomaly Detector 11 %, Level 1 card 3 %, diamonds 10 %, Liquid Pain 3 %.

## Not tested

- Multiplayer.
- The Temporal Watch cooldown (vanilla item cooldown, not checked by hand).
- A level 1 or level 3 vault door in game (only level 2 was visited; the level logic is the same).
- Performance comparison with 0.7.1 (generation of the new bands is per column like the Poolrooms).
- Shader compatibility (still waiting for feedback from 0.6.3).
