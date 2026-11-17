# Office Rooms 0.8.1 (beta) – Megastructures twice as common

Everything from 0.8.0 is unchanged except this:

- The default of `poolrooms_region_chance` is now **0.024** (was 0.012). It applies to all three megastructures (Poolrooms, Neighbourhood, The Maze), so each one now appears in about **2.4 % of regions**: on average one per 42 regions, about one per 1,030 × 1,030 blocks per type, one of the three per ~600 × 600 blocks.
- Within ±3 regions (1,120 × 1,120 blocks) there is a given type with about 70 % chance (before: 45 %), within ±5 regions about 95 %.
- Each structure still sits on one random storey (between about storey 9 and 90), so you also need the right height.
- The value is part of the dimension's generator settings. It only takes effect in **new worlds**; already generated chunks are never changed.

## Tests (actually run)

- Build OK, 14 unit tests, 0 failures. Frequencies in 3600 regions: Poolrooms 90, Neighbourhood 103, Maze 80 (0.8.0: 54 / 56 / 48). Poolrooms test: 44 of 1600 regions (0.8.0: 28).
- Production server, fresh world seed 4711: `goto poolrooms`, `goto neighbourhood`, `goto maze` and `goto maze_vault` all found a site; the nearest Maze is now at −919 179 −796 (0.8.0: −1799 473 −156). `verify 2` at the maze vault: 0 errors, `RESULT: OK`. No exceptions in the server log.
