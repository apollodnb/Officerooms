# Test report 0.38.1

Build and all unit tests pass. New tests:
- `PoolroomsBuildTest` builds every Poolrooms within 25 regions for four seeds, including the real planner seed of the test world;
- `HotelStairTest` checks six steps in every rotation of the L-stairs.

Dev tool `SpotsDump` (test) writes `build/spots.txt` with the positions of rare cells for the test world. It derives the planner seed like the game does.

In-game test: production server with all test mods, dev client, fresh worlds with seed 4711. Walking tests in survival with key presses in short steps, position logged after each step.

| Check | Result |
|---|---|
| Slide opening in an office (35 107 -23): right-click → arrive in the Poolrooms (421 72 -168), milestone `slide_ride` | ok |
| Hotel lobby L-stairs, walked up (y 245 → 251, onto the guest floor) | ok after the fix (before: stuck at y 250) |
| Level 0 ramp, walked up east and around the corner south (y 8 → 14, onto the next floor) | ok after the fix (before: stuck after two or four steps) |
| Maze ramp, walked up the same way (y 240 → 246) | ok after the fix |
| `/officerooms locate poolrooms` near a formerly broken Poolrooms | ok after the fix (before: "An unexpected error occurred") |
| Loot rolled into a test chest: hotel_room, hotel_safe_box, hotel_reception, industrial_storage, industrial_control, industrial_generator, level_fun_gift, neighbourhood_house, cinema_concessions | ok, sensible contents |
| Neighbourhood playground, Industrial lockers, Level Fun banquet (photographed) | ok |

Not tested:
- what the world generated in the formerly broken Poolrooms regions before the fix;
- the darkness of a Level Fun dark cell (night vision was on for the photos);
- a hotel L-stair in rotation 3 in game (only the unit test);
- riding through a whole Poolrooms slide pipe (the office opening teleports you; the pipes in the Poolrooms are scenery);
- multiplayer, sound.
