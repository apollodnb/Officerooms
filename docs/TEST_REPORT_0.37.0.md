# Test report 0.37.0

Build and all unit tests pass. `CinemaTest` was extended: doors, screens, seats and walkways on all three floors, every step of the four flights with two blocks of head room, the top step level with the gallery floor, open railings at the flights, closed railings elsewhere, landings, and the open atrium up to the ceiling.

In-game test: production server with all test mods (GeckoLib, OfficeCraft, Refurbished Furniture + Framework, FTB Quests/Library/Teams, Architectury), dev client, fresh world with seed 4711. Cinema at -80 137 84 (`/officerooms goto cinema`). Screenshots at gamma 0 with night vision. Render distance 4, so the far parts of the 160-block lobby appear as bright fog in the pictures.

| Check | Result |
|---|---|
| Three floors with galleries, railings, under-gallery ceilings with lights | ok (photographed) |
| "SCREENS 13 TO 24" marquee on floor 2 | ok |
| Auditorium on floor 3: seats, screen, carpet, ceiling | ok |
| Corridor on floor 1 with doors | ok |
| Walking up flight A (lobby → gallery 1) in survival with real key presses, then stepping sideways onto the gallery | ok (y 137 → 148.5 → 149) |
| Walking up flight B from its landing (gallery 1 → gallery 2), then onto the gallery | ok (y 149 → 161) |
| Walking from gallery 1 through the railing gap towards the landing | ok (no fall) |
| Atrium ceiling at the top (block test) | ok |
| Falling off the end of a flight in survival | possible and deadly. A 12-block fall killed the player once during testing (fall damage was off for the later walking tests) |

Found and fixed during the test: the upper flights could not be climbed from the gallery side (the first step is one block higher than the gallery), so they got a landing at gallery level in front of the first step.

Not tested:
- the two east flights on foot (same layout, mirrored; covered by the unit test);
- office doors into the upper cinema floors in game;
- elevator rides to the cinema and hazard events;
- loot of the lost-and-found chests on the upper floors;
- spawns (Aranea Membri) in the larger cinema;
- sound, music and reverb (no audio device on the test machine);
- a world without the optional mods.
