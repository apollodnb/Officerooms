# Test report 0.15.0

Environment:
- Forge 1.20.1-47.4.28 dedicated server (production JAR + GeckoLib 4.8.4), fresh world, seed 4711.
- Dev client (Xvfb).

## Unit tests

19 of 19 passed.

New test `industrial` found 30 complexes in 576 regions. For each complex it checks:
- no hotels, mazes, elevators or hidden rooms in the same region;
- exactly 3 staircases per level, each with one step per height, headroom and an opening above;
- the generator hall is open from the floor through the catwalk level, with catwalks along the walls.

`SpecialRoomsTest` now samples 25 regions instead of 9. With another megastructure, the rare Oasis no longer showed up in the small sample.

## In game

| Check | Result |
|---|---|
| `locate industrial` | Complexes at 180 305 −300 and −268 389 −268 |
| Generator hall (from the catwalk and the floor) | Generators with glowing cores, pipe columns, catwalks, railings (screenshots) |
| Upper levels | Rooms with pipes, a storeroom with crates, a grated ceiling |
| After the first test: floor stripes repeated on every block, too few pipes | Floor texture simplified; pipes along the wall and corner risers added; photographed again |
| Spawns (player in the complex, 60 s) | 1 Flayed, 4 Hounds |
| Holes | The player fell from level 2 into a generator hall (the halls are open, as intended) |
| Server log | No exceptions |

## Not tested

- Opening grate doors by hand.
- Walking the staircases (geometry covered by the unit test).
- Opening the loot chests.
- Multiplayer, shaderpacks.
