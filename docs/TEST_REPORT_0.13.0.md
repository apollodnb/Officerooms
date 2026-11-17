# Test report 0.13.0

Environment:
- Forge 1.20.1-47.4.28 dedicated server (production JAR + GeckoLib 4.8.4), seed 4711.
- Dev client (Xvfb).

## Unit tests

17 of 17 passed.

## In game

| Check | Result |
|---|---|
| Model renders (black, thin strands, wide scribbled head, jaw) | OK (screenshots in an office and in a test corridor) |
| Scream phase on sight | Speed 0 for about 1.6 s |
| Chase speed (test corridor, player 28 blocks away) | Attribute 0.525, about 7.6 blocks/s |
| First build at 0.555 | Measured at well over 10 blocks/s, too fast. Corrected in a second attempt: 1.05 gave only about 4.3 blocks/s, so the default is now 1.75. |
| Falter after continuous pursuit | After about 10 s: 0.075 for 3 s, then chasing again |
| Server log | No exceptions |

The Flayed got stuck once in the test corridor where I had opened the ceiling into the storey above for a photo. After the ceiling was closed it ran normally. In the generated office world there are no such openings.

## Not tested

- Natural spawning (spawn chance is very low; only summoned for tests).
- Damage against an unprotected player (player had Resistance V).
- Research File drop.
- Research Tables in labs and secure storage rooms were not visited in game. The code paths are the same as the lab furniture tested in 0.12.0.
- New paper and book loot entries (JSON only).
- Multiplayer, shaderpacks.
