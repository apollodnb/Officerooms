# Test report 0.34.0

## Done
- Build and all unit tests: passed. The Maze test was extended:
  - every level is one connected labyrinth (at least 97 % of the open floor reachable);
  - the vault shell has exactly one door on its level, and the door can be reached on that level;
  - 3–5 exits.
- In game, Level 0 on a fresh world:
  - a pit field seen from above and from the side, looking like the reference picture;
  - the ramp renders as a continuous carpet slope.
  - After the photos a light ceiling seam inside the shafts was removed. That fix is not checked in game.

## Not tested
- **The Maze in game:** the levels, its ramps, pit fields, chamber grid and the vault on an upper level.
- Walking a ramp all the way up, and getting out of a pit at the bottom.
- Mob spawning on upper maze levels.
