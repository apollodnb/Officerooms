# New in Office Rooms 0.40.0 (beta)

**The quest book installs itself.** A mod download on CurseForge or Modrinth only brings the JAR, so the quest files now travel inside it.

- When **FTB Quests** is installed, Office Rooms adds its quest book on start (`config/ftbquests/quests`), before FTB Quests loads it.
  - **No quest book yet:** everything is copied, and the book is called "Office Rooms".
  - **A modpack with its own quest book:** only the Office Rooms chapters (files `office_rooms_*`) and the chapter group "Office Rooms" are added, below the pack's own groups. The pack's chapters and settings are never changed.
  - It runs once per version. When an update of Office Rooms brings new quests, only its own chapters are renewed.
  - To manage the quests yourself, set `installQuests = false` in `config/office_rooms-common.toml` (section `[quests]`).
- The quest files are now called `office_rooms_*.snbt`, so they never collide with a pack's files. Quest IDs are unchanged; progress is kept.
- Chapter order: "X. Curiosities" now comes before "XI. Requisitions".
- The version in the log at start is correct again (it said 0.18.1).
