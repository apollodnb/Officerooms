# Office Rooms 0.8.3 (beta) – Megastructures much more common

Everything from 0.8.2 is unchanged except this.

## More megastructures

- Default `poolrooms_region_chance` is now **0.07** (0.8.1: 0.024, 0.8.0: 0.012). It applies to all three megastructures, so each one is in about **7 % of regions**, and about **21 % of all regions** hold one of the three.
- Rough estimate for a player who keeps moving and changes storeys now and then: **about one megastructure every 45 minutes**. The structures still sit on one random storey each (The Maze 1, Neighbourhood 4, Poolrooms 5 storeys), so you only find one if you are on the right height in that region. Moving between storeys raises the odds.
- Only **new worlds** use the new value.

## Fixes found by the denser test

- **The Maze:** with several megastructures next to each other, a maze could end up with **no exit** (exits are only cut towards the west and north neighbours, and a neighbouring megastructure blocked the whole side). Now only the rooms inside a neighbour's storey band are excluded. Result: all 211 mazes in 3600 test regions have 3–5 exits.
- **Poolrooms:** in very rare layouts (1 of 114 sites) there was no hall of 28–68 blocks, so the hidden underwater labyrinth and its vault chests were missing. Such Poolrooms now use the hall closest to 64 × 64 that fits a labyrinth. Poolrooms that already had a labyrinth are unchanged.

## Tests (actually run)

- Build OK, 15 unit tests, 0 failures. 3600 regions: Poolrooms 254, Neighbourhood 257, Maze 211; all 211 mazes have 3–5 exits (3: 76, 4: 64, 5: 71); 114 of 114 Poolrooms have their labyrinth vault; 12 mazes fully reachable from the first exit.
- Fresh world, seed 4711: `goto poolrooms` (348 431 −187), `goto neighbourhood` (499 167 −308), `goto maze` (484 359 −331, 4 exits), `goto maze_vault` – all within about 600 blocks of the world centre. `verify 2` at all three: `RESULT: OK`. No exceptions in the server log.
