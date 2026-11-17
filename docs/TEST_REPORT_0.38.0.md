# Test report 0.38.0

Build and all unit tests pass. Quests generated with `tools/gen_quests.py` (11 chapters, 120 quests) and loaded by the test server with FTB Quests (no load errors, `/ftbquests reload` ok).

In-game test: production server with all test mods, dev client, fresh world with seed 4711.

| Check | Result |
|---|---|
| Underside Key recipe with seven ingredients loads (`/recipe give`) | ok |
| Milestone `cinema_top`: not in the cinema lobby, granted on the third floor | ok |
| Milestone `hotel_top`: not in the hotel lobby, granted on the highest storey | ok |
| `discover_cinema` / `discover_hotel` | ok |
| Quest book: chapters V and VII open; new quests with their icons visible in chapter V | ok (photographed) |
| Quest files contain the cinema, ticket stub, slide, hotel and Aranea quests | ok |

Not tested:
- crafting the key in a crafting table (only the recipe was checked);
- milestone `slide_ride` in game (no slide opening was searched for; the code grants it right after the slide teleport);
- completing the new quests in the book;
- the cinema row of chapter V on screen (the test script could not scroll the quest view);
- multiplayer.
