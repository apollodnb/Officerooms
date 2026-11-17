# Test report 0.20.0

Build OK, unit tests pass. Tested with the production server and client.

| Test | Result |
|---|---|
| Natural placement at an Ancient City (x 4256, z −1360) | Outpost placed at 4380, −48, −1353. Sculk interior; the tunnel leads into the city cavern (screenshots). |
| Natural placement at a Jungle Temple (368, 1568) | Outpost placed 9 blocks deep beside the temple. Ladder shaft with iron trapdoor at y 89. |
| Natural placement in the Nether (three spots around 6000/6000) | One outpost placed at 6043, 65, 5897 with Nether materials (screenshots). |
| Lever without power | The room is dark (fixed in 0.20.0: the ceiling tiles do not glow). |
| Full sequence | Lever → boot → warnings (monitors cycle and flicker, warning lights flash) → READY → red button → activation with % counter → frame lights up → gateway open. Screenshots at every stage. |
| Gateway | The player was teleported into the Office Rooms (`office_rooms:office_rooms`). |
| Shrines | Shrine classes and structure files were not changed (file dates unchanged). |

## Not tested
- Natural cave outposts (1 in 1,800 chunks): only the builder was tested, through the command. The search for a cave passage was not tested in the world.
- Button before power and during the warnings: the messages exist in the code but were not triggered in game.
- Multiplayer with several players at one gateway.
- Sounds were not heard in game (no audio device).
- The screenshots of the Ancient City, Jungle and Nether outposts were taken before the ceiling fix and still show the old glowing ceiling.
