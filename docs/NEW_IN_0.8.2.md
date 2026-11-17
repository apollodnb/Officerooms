# Office Rooms 0.8.2 (beta) – Multiplayer check before the first group playtest

Everything from 0.8.1 is unchanged except these fixes.

## Fixes

- **Two Hunters at once (multiplayer):** a Hunter could appear in a chunk that was loaded but outside the simulation distance. There it was invisible to the "exactly one Hunter" check, so a second one appeared. Now creatures only spawn in fully active chunks, and if two Hunters ever meet in one Poolrooms/Maze band, only the one closest to a player stays.
- **Creatures piling up out of reach:** the same rule now applies to all Office Rooms spawns (office creatures, Poolrooms, Neighbourhood, Maze). Before, creatures could be placed in inactive chunks where they were neither counted nor cleaned up.

## Recommendations for a group playtest (4–5 players)

- Same mod list on server and all clients (OfficeCraft / Refurbished on the server means on every client too).
- Server RAM 6 GB, `view-distance` 6–8, `simulation-distance` 6 or more (creatures and the Hunter appear up to ~64 blocks away and need to be inside the simulation distance to move).
- Back up the world first, and always stop the server with `/stop`. A server that is killed (crash, out of memory, power loss) can lose the creatures of recently unloaded chunks, e.g. Neighbourhood residents.
- Optional: pre-generate the area with a mod like Chunky. On the 2-core test machine the dimension generates about 10 chunks per second.
- After the test: send `logs/latest.log` of the server and any `crash-reports`.
