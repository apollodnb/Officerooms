# Test report 0.21.0

Build OK, unit tests pass. Tested with the production server and client.

| Test | Result |
|---|---|
| Entry | Clicked the top ceiling (y 543) at 200/200 → player in `office_rooms:level_0` at y 19 (floor 3). The ceiling block stayed. |
| Generation | Corridors, slits, pillar halls, Threshold Room with holes, Manila Room (lining, chest, table, hole), passage, chamber and exit all present (screenshots, block checks). |
| Puzzle | Survival with a water bucket: right-click into the hole → water in both shaft blocks and in the passage. Sprint into the passage → player swam through and arrived in `office_rooms:office_rooms` at 2082/239/−697. |
| Reward | 6 Glitched Ingots in the inventory. |
| Protection | Holding left click for 3 s with a diamond pickaxe in survival: the floor stayed. Placing stone: cancelled. |
| Immortal Flayed | Released 30 s after arrival; `/damage … 500` → health stays 80. Only one Flayed. No other entities in Level 0. |
| Pursuit | Player teleported 80 blocks away: the Flayed followed through the maze, and when it got stuck it reappeared nearby (position log). |
| Saved Flayed | After a server restart the saved Flayed is adopted again, so there are never two (fixed during the test). |

## Not tested
- Average search time for a Manila Room (designed for about 15 minutes, not measured).
- Multiplayer with several players in Level 0.
- Natural spawning of the new books (only the files and the build were checked).
- Fall damage through the Threshold holes, and the helper for jumping into the dry shaft (only implemented).
- Sounds were not heard in game (no audio device).
