# Test report 0.39.0

Build and all unit tests pass.

In-game test: production server with all test mods, dev client, world with seed 4711. Screenshots at gamma 0 with night vision.

| Check | Result |
|---|---|
| Natural placement check on land (`try_surface`) in 7 chunks | refused 6 times with a sensible reason (village, water, uneven ground); placed once at 277 71 418 |
| Natural placement check on the ocean (`try_ocean`) in 12 chunks | refused in shallow ocean; placed over deep ocean at -98 73 -1659 and 107 73 -1714 |
| Field station: building, foundation on a slope, fence, apron, antenna (photographed) | ok |
| Drilling rig: legs, deck, railing, derrick, station (photographed) | ok |
| Rig: climb the ladder out of the water onto the deck (survival, key presses) | ok after making the ladder longer (before it started only 3 blocks below the surface and you sank past it) |
| Rig: lever → monitors "PORTAL SYSTEM READY" → red button → gateway opens → walk in → arrive in the Office Rooms | ok |

Flying 50 times over plains and ocean in spectator mode produced no outpost of any kind. That fits the rarity; the server was also lagging behind during that flight.

Not tested:
- the full sequence in a field station (same code as the rig);
- how often stations really appear over a large explored world;
- the sign text up close;
- multiplayer.
