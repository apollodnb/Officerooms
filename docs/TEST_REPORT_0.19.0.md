# Test report 0.19.0

Build OK, unit tests pass. Tested with the production server and client on the existing test world: the new dimension is added to an old world without problems.

| Test | Result |
|---|---|
| `/officerooms underside now` | Arrival under the ceiling, slow fall onto the meadow. Terrain, trees, upside-down meadow and hanging roots generate (screenshots). |
| Boss timer | After a dimension change without `now`, [Unknown] appeared after 19 s. With `now`, after the quake. |
| Boss behaviour | It moves between floor depth and ceiling depth (y 33–61), surfaces with dust, lunges and crawls on the meadow toward the player (screenshot series of 40 frames). |
| Boss gone when nobody is there | It was removed about 5 s after the player left. |
| Boss death | Segments burst. Noclip Sword drops. Both portals are built with frames and signs. 0.19.0 fix: the area around the portals is now cleared (trees). |
| Overworld portal | Teleport to the world spawn. |
| Office portal | Teleport into an office room. |
| Noclip Sword | Right-click via the client; 21 blocks walked in a straight line through furniture and walls with the height unchanged (y 239). At the end the player was pushed out of the wall. |
| Underside Key | Used on the bedrock beneath the lowest storey: bedrock opened, player fell through the void and landed in the Underside. |
| Artifact loot tables | All six `*_artifact` tables give their artifact. |
| Books | 40 books, all pages ≤ 256 characters. A sample from the loot table contained a new book ("Yellow Maze Notes"). |

## Not tested
- Fair boss fight without Resistance; balance of 600 health against normal gear.
- Multiplayer: several players, boss bar and quake for everyone.
- That the artifact chests appear in newly generated megastructures. Only the loot tables and the code paths were checked.
- Crafting the key in the crafting grid. Only checked that the recipe loads without errors.
- Sounds were not heard in game (no audio device).
