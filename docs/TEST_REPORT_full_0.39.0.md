# Full test 0.39.0 (before balance, cleanup and release)

Version 0.39.0, production server and dev client, fresh worlds with seed 4711, difficulty normal. Two runs: with all test mods (GeckoLib, OfficeCraft, Refurbished Furniture + Framework, FTB Quests/Library/Teams, Architectury) and with **GeckoLib only**. At every place the player stood in survival for 45 s; creatures were counted within 80 blocks (this also counts offices above, below and around the place). Tick time from `/forge tps` for the Office Rooms dimension.

| Place | Creatures (all mods / GeckoLib only) | Mean tick time | Errors in log |
|---|---|---|---|
| Office, storey 27 | wolfspider, smiler, deathmoth, hound, headless warden / same | 29.6 / 10.2 ms (first load) | 0 / 0 |
| Office, storey 85 | wolfspider, smiler, deathmoth, hound / + headless warden | 7.5 / 8.1 ms | 0 / 0 |
| Poolrooms | **colony**, wolfspider, hound / + smiler | 27.2 / 23.6 ms | 0 / 0 |
| Neighbourhood | hounds (4), deathmoth, wolfspider, smiler, headless warden | 10.5 / 8.3 ms | 0 / 0 |
| Maze | **flayed**, wolfspider, smiler / + hound | 12.7 / 8.8 ms | 0 / 0 |
| Hotel | **hounds**, wolfspider, smiler, deathmoth | 12.5 / 5.8 ms | 0 / 0 |
| Industrial Complex | **hounds (5), flayed**, wolfspider, smiler, deathmoth, headless warden | 7.7 / 8.3 ms | 0 / 0 |
| Level Fun | **partygoers (6)**, wolfspider, smiler, deathmoth, hound | 11.9 / 6.0 ms | 0 / 0 |
| Abandoned Cinema | **Aranea Membri**, wolfspider, smiler, deathmoth, hound | 10.0 / 8.1 ms | 0 / 0 |

- "Can't keep up" warnings: 0–2 per place while new chunks were generated (2-core test machine). The highest mean tick time was 30 ms, under the 50 ms limit.
- Client log: no errors from Office Rooms in either run (only the known missing audio device).
- **Field station** at 277 71 418 played through in survival: power lever → monitors "PORTAL SYSTEM READY" → red button → gateway opens → walk in → arrive in the Office Rooms. ok.
- Already tested in 0.37.1–0.39.0 and not repeated: cinema stairs and floors, elevator into the cinema, hazard zone over the cinema, slide, hotel L-stairs, Level 0 and Maze ramps, loot tables, drilling rig with ladder.

No new bugs found in this run.

Not tested:
- multiplayer;
- sound, music and reverb (no audio device);
- long sessions over hours (memory, saving and loading many explored regions);
- the drilling rig and field station in a world without the optional mods (they use only vanilla and Office Rooms blocks);
- the darkness of the dark storeys and dark Level Fun cells without night vision.
