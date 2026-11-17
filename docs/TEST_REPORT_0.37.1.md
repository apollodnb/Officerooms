# Test report 0.37.1

Build and all unit tests pass (`CinemaTest` also checks the new landings and railings).

In-game test: production server, dev client, fresh worlds with seed 4711. Cinema at -80 137 84. Screenshots at gamma 0 with night vision. For the spawn and hazard checks the test config was changed temporarily (Aranea `cinemaChance` 1.0, hazard `searchRegions` 1) and reset afterwards.

| Check | Result |
|---|---|
| All four flights walked up in survival with key presses (west and east, lower and upper) | ok, each ends on its gallery |
| Walking straight on at the top of a flight | stops at the landing railing (before: fell into the atrium, fixed) |
| Walking forward from the foot landing of an upper flight | stops at the railing (before: fell, fixed) |
| Office doors in the outer wall (block scan along the west and north ring) | doors on all three floors; photo of a floor-3 door with the office corridor behind it |
| Lost-and-found chest on floor 3, opened by the player | ok: battery, ticket stubs, glowstick, coins |
| Aranea Membri while the player is on floor 3 | spawned on floor 3 (y 165, in the ceiling). Before the fix it could only spawn on the ground floor |
| Hazard Zone picks the cinema | box y 136–171 = all three floors. Before: "An unexpected error occurred" (fixed) |
| Elevator next to the cinema (storey 34, inside the cinema band) | the ride ended in the cinema lobby |
| Server and client with only GeckoLib (no OfficeCraft, Refurbished, Framework, FTB, Architectury) | cinema, hotel lobby and offices generate normally with vanilla stand-ins; no errors in the server or client log |

Found and fixed: missing landings, Aranea only on the ground floor, the hazard start error.

Note: pressing E in creative crashes the **dev** client because of GeckoLib's example items (`geckolib:jack_in_the_box`). This only happens in the development environment, not with the mod.

Not tested:
- the inside of Neighbourhood houses without Refurbished (only the street was photographed);
- quests without FTB Quests (the mod simply has no quest book then);
- multiplayer;
- sound, music and reverb (no audio device).
