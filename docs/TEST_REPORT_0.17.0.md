# Test report 0.17.0

Environment:
- Forge 1.20.1-47.4.28 dedicated server (production JAR + GeckoLib 4.8.4), fresh world, seed 4711.
- Dev client (Xvfb).

## Unit tests

20 of 20 passed.

## In game

| Check | Result |
|---|---|
| Monitor in a hidden Security Office | "FACILITY STATUS NOMINAL" in green → after `hazard warn` "WARNING … HAZARD ZONE IN 14:58" in red (screenshots) |
| Monitors in normal offices | Several found within 48 blocks of the elevator. One was broken (cracked, off); a working one showed "HAZARD ZONE ACTIVE – THE MAZE – GAS MASK REQUIRED – ENDS IN 44:05". |
| Zone choice | The Maze, x −938..−850, y 286..291, z 351..439 |
| Hazard chests | 8 of 8 placed once the player came near |
| Event creatures | 2 spawned around the player inside the zone |
| Without a gas mask | Health 20 for about 12 s, then falling (17 after 16 s); the player died shortly after. Green fog and gas particles visible. |
| With a gas mask | No damage in 15 s; the filter used 19 s |
| Special Event Detector (128 blocks away) | Action bar: "↖ HAZARD ZONE ACTIVE – The Maze 128 blocks, level with the zone" |
| Night Vision Goggles | Battery is used while worn (7 s after 7 s) |
| `hazard stop` | Status idle, next warning in 97:30, no event creatures left; no chest inside the former zone |
| Server log | No exceptions |

After the test, the monitor frequency in normal rooms was lowered at the user's request: 15 % of offices, 6 % of corridors (previously 45 % and 25 %). The build was repeated and the unit tests passed.

## Not tested

- A full natural 100-minute cycle; only the commands were used.
- Multiplayer synchronisation with two clients.
- Opening hazard chests.
- Crafting the Hazard Visor in game.
- Recharging the goggles with a battery.
- The alarm beeps (the test client has no audio device).
- The louder lamp hum and quieter music (not heard).
- Shaderpacks.
