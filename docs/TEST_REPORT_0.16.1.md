# Test report 0.16.1

Environment:
- Forge 1.20.1-47.4.28 dedicated server (production JAR + GeckoLib 4.8.4), fresh world, seed 4711.
- Dev client; player in survival mode with Resistance.

## Unit tests

20 of 20 passed. `levelFun` covers all seven cell types, including the new bouncy castle (608 cells in 35 sites).

## Natural spawning

The spawn chances of the rare roaming creatures (Headless Warden, Colony, Flayed, Observer) were set to 1.0 **in the test configuration only**, so they could show up within minutes. The spawn code and the default values in the mod are unchanged. The configuration was restored afterwards.

Counts within 90 blocks of the player:

| Place (time there) | Creatures spawned |
|---|---|
| Office storey 299 (50 s) | Wolfspider 5, Hound 1, Smiler 1, Headless Warden 2, Colony 1, Flayed 1, Observer 1 |
| Poolrooms (45 s) | Hunter 1, Wolfspider 2, Hound 2, Smiler 1, Headless Warden 1, Colony 1, Flayed 1 |
| Level Fun (45 s) | Partygoer 5, plus the creatures following the player |
| Dark lowest storeys, y −49 (55 s) | Gaunt 6, Wolfspider 4, Hound 1, Smiler 2 |

Earlier versions already checked the Hotel (Hounds), the Industrial Complex (Flayed, Hounds) and the Maze (Flayed).

There were no exceptions in the server log.

## Not tested

- Default spawn rates over a long play session.
- Multiplayer, shaderpacks.
