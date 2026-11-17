# Test report 0.12.0

Environment:
- Forge 1.20.1-47.4.28 dedicated server (production JAR + GeckoLib 4.8.4), seed 4711.
- Dev client (Xvfb).

## Unit tests

17 of 17 passed. `hiddenRooms` found 119 rooms in 256 regions; labs and security offices meet the larger size and distance rules.

## In game

| Check | Result |
|---|---|
| New security office 1079 221 30 | CCTV wall, consoles, racks, desk, cabinet, chest (screenshot) |
| New research lab −1161 233 22 | Bench, jars, tanks, apparatus, computer corner (screenshot) |
| Research Table GUI | Opens; buttons enabled or disabled correctly; tooltips |
| 16 files: reforge sword 3× and Heart Research 2× | 1 file left; max health 24; sword `OfficeReforge: 3`, Sharpness II kept |
| Attack damage with Iron Sword Reforged III | 15.0 (1 + 5 + 9; Sharpness on top) |
| Weapon in the slot when the GUI is closed | Returned to the inventory |
| Death and respawn | Max health still 24, respawned at full health |
| Server restart after `save-all` | Hearts persisted (`PlayerPersisted.office_rooms_hearts`) |
| Arrow from a Reforged III bow vs. a normal bow (test arrow on a zombie) | 11.8 vs. 2.9 damage |
| Logs | No exceptions; no missing models |

## Not tested

- Crossbow and trident in real combat (same code path as the arrow).
- Every recipe crafted in game.
- Multiplayer.
- Shaderpacks with the new blocks.
- Hearts are only saved with the normal player save. A server killed without saving loses the last minutes, as with all player data.
