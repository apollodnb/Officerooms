# Test report 0.27.0

- Build and **all unit tests green**.
  - Note: since 0.26.0 two planner tests had been failing (elevator panel in front of an opening); this went unnoticed because only the JAR was checked. Fixed now.
- Config migration on the test server: "balance 5 applied", Gaunt health 60, Colony health 80.
- Maze, after 45 s inside:
  - exactly one Flayed in the Maze;
  - no other creature on the Maze level inside the Maze region; one Wolfspider stood just outside it, in the neighbouring region.
- Partygoers:
  - 4 Partygoers kept exactly the same position over 10 s (they stand);
  - after stepping 1.5 blocks next to one, all four ran at the player within 4 s, including ones 6+ blocks away.
- Loot tables: no load errors. The changed chances are checked in the JSON files.

**Not tested:**
- the drop rates in game (my sampling with `/loot spawn` gave no usable output);
- generation of new worlds (more hidden rooms, fewer outposts);
- the elevator floor fallback;
- the wave animation as seen on screen.
