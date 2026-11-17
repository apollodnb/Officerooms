# Test report 0.40.0

Build and all unit tests pass. New test `QuestInstallerTest`:
- a fresh install copies everything;
- a merge keeps a modpack's `data.snbt` and chapters byte for byte, adds the group after the pack's groups and removes `office_rooms_*` files from older versions;
- the same version a second time changes nothing (even edited files stay);
- the group is never added twice.

In-game test: production server with FTB Quests, Library, Teams and Architectury, dev client.

| Check | Result |
|---|---|
| No quest folder: files installed at mod loading, a minute before "FTB Quests: Loading quests" | ok, 14 files, marker 0.40.0 |
| Modpack quest book ("Better Pack" with its own group and chapter): merged | ok: `data.snbt` and the pack's chapter unchanged (checksums), both groups present, Office Rooms below the pack |
| Quest book in game: "Better Pack / Getting Started" above "Office Rooms" with all 11 chapters | ok (photographed) |
| FTB Quests loads the merged book without errors | ok |

Not tested:
- a real large modpack quest book (for example Better Minecraft's);
- `installQuests = false` in game (only in code: it returns before touching anything);
- a dedicated server with separate clients (the files are only read on the server; FTB Quests sends them to the clients);
- multiplayer.
