# Test report 0.8.2 (beta) – multiplayer

Only tests that were actually run. Production server (Forge 47.4.28, seed 4711, world from 0.8.1) with **two real clients at the same time** ("Dev" and "Dev2"), simulation distance 4, on a 2-core / 8 GB test machine.

| Test | Result |
|---|---|
| Build, 14 unit tests | OK, 0 failures |
| Code review for single-player assumptions (first player, client classes in server code) | none found |
| Security door, level 1: Dev2 without card, Dev with Level 1 card | Dev2: "ACCESS DENIED", door stays closed; Dev: door opens; Dev2 sees it open |
| Mining Fatigue, both players in the maze | both Mining Fatigue III |
| Hunter count, both players in the maze ~70 blocks apart | **0.8.1: 2 Hunters (bug)**. 0.8.2: exactly 1 Hunter, checked 6 × over 2 minutes |
| Hunter chases | player teleported 8 blocks away: Hunter follows at once |
| Totem of Mercy on Dev2 (100 damage) | Dev2 survives in the Overworld with 5 HP, totem used up; Dev unaffected in the Office Rooms |
| Relog in the maze (Dev2 client closed and restarted) | Dev2 back at the same spot in the dimension, Mining Fatigue III again, still 1 Hunter |
| Neighbourhood residents after unload + clean restart | fresh Neighbourhood: 6 residents → all players away (unloaded) → `/stop` → restart → 6 residents |
| Load: 5 spread areas of 11 × 11 chunks (= 5 players, view distance 5) generated at once, server only | 605 new chunks in 60 s; afterwards dimension tick 9.3 ms (TPS 20), server memory 2.4 GB |
| 0.8.2 server start | OK, no exceptions |

## Observed, not a mod bug

- The test machine itself ran out of memory twice with server + two clients and killed the server. Residents of recently unloaded Neighbourhood chunks were missing afterwards (4 of 6). With a clean `/stop` nothing was lost (see above). Advice: enough RAM and always `/stop`.

## Not tested

- More than 2 players at once (machine limit).
- A dedicated server on other hardware, other mod packs.
