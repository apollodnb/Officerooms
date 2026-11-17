# Test report 0.35.0

## Done
- Build and all unit tests: passed.
- In game on a fresh world with `goto poolrooms`, one shot at brightness 0 and the rest with night vision:
  - huge flooded halls and deep pools;
  - a pool ladder;
  - slide tubes sticking out of the wall into a landing pool;
  - handrails along a pool;
  - round arches.

- Slide opening: placed in an office and right-clicked in game. The screen went dark with the opening still in view, and three seconds later the player was in the Poolrooms (433 71 −203 region, landing at 428 66 −167).
  - The rule that the same opening always leads to the same Poolrooms was changed after this test. It is built and unit-tested, but not re-tried in game.

## Not tested
- Sounds of the ride and the splash (no audio device).
- **Riding a slide:** whether the slippery chute carries a player down nicely was not tried.
- The arcades in the water and the water arches were not looked at individually.
- The underwater labyrinth, its vault chests and the Colony hunter in the taller band were not checked.

## Known
- The Office Rooms lightmap tints the white tiles warm (cream/orange), unlike the cool white of the reference picture.
- In the photos, parts of the far hall show the sky because they were beyond the render distance.
