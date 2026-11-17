# Test report 0.16.0

Environment:
- Forge 1.20.1-47.4.28 dedicated server (production JAR + GeckoLib 4.8.4), fresh world, seed 4711.
- Dev client (Xvfb).

## Unit tests

20 of 20 passed. The new test `levelFun` found 35 Level Fun sites in 576 regions. It checks that:
- all six cell types occur;
- no other megastructure, elevator or hidden room sits in the same region;
- at least 70 % of the floor is walkable.

## In game

| Check | Result |
|---|---|
| `locate level_fun` | −80 251 −240 |
| Look of the level | Yellow walls, streamers, balloons, gift boxes, cake table, confetti; clearly darker overall (screenshot) |
| Partygoer model | Yellow, smiley face, party hat (screenshot) |
| Partygoer approaching (player in a cleared lane) | At 16 blocks it does not react yet. At 10.5 blocks it walks closer and stops at about 7.5 blocks. |
| Player closer than 6 blocks | The Partygoer moves in at once and hits; the player was knocked back (HurtByTimestamp set) |
| Server log | No exceptions |

## Not tested

- Natural Partygoer spawns (only summoned for the test).
- Waving and sounds (the test client has no sound device).
- How the darker light curve looks with shaderpacks (shaders replace the lightmap; it may have no effect there).
- Multiplayer.
