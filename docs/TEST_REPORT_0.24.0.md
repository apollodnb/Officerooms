# Test report 0.24.0 (full test pass)

## Static checks
- Build OK; unit tests pass.
- `tools/validate_data.py` passes.
- New consistency check: every blockstate → model, model → texture and item → lang entry, plus loot table references. 0 problems.
- Client log: no missing models or textures.

## Rooms and structures
`/officerooms goto` reached every room type and every megastructure:
- all 10 standard room types;
- atrium;
- all 7 special rooms;
- Poolrooms (+ maze entrance), Neighbourhood, Maze (+ vault), Hotel (+ vault), Industrial, Level Fun, hideout, elevator and secure storage.

No errors in the log.

## Events
- **Hazard Zone:** warn, start, status and stop all work.
- **Blackout:** started in survival, fuse box on floor 51, 3 Gaunts spawned, stop removes them.

## Dimensions
- **Level 0:** entry and Manila locate work.
- **Underside:** `underside now` → [Unknown] spawned (head + 11 parts).
- **Research outpost (lab):** built.

## Mobs
Each mob was summoned 5 blocks from the player:
- **Attacked:** Gaunt, Flayed (killed a player with Resistance II), Partygoer, Wolfspider, Hound (very high damage, as intended).
- **Approached:** Smiler, Colony.
- **Observer:** vanishes when you are close (as designed).
- **Not conclusive:** the Hunter. The player stood in water, and Resistance V hid any damage.

## Deathmoth
- Natural population: 3 Deathmoths on walls around the player within 40 s on a fresh world.
- Sits flat on the wall (screenshot) and is barely visible in the dark.
- Ambush tested several times: trigger → flight → face → screen effect and camera shake → drain (20 → 14.3 health with Resistance I) → leaves.
- Hidden behind furniture it does not trigger from 4.6 blocks (no line of sight), as intended.

## Partygoer
- Hit one: within 6 s all 6 Partygoers nearby (10–37 blocks away) were at the player.

## Performance
- Sampling profiler (jstack) on the server thread while exploring new areas:
  - before the fix, OfficeCraft placement waited for neighbouring chunks (54 of about 80 busy samples);
  - after the fix, 3 samples.
- Standing still: 4.4 ms per tick in the Office Rooms, with no Office Rooms code in the samples.
- The remaining stalls in the test come from the vanilla `/tp` into ungenerated areas.
- Client: the only notable Office Rooms frame is OfficeCraft's own furniture drawing. The software-rendered test client cannot give meaningful FPS, so this is unchanged.

## Not tested
- Sounds (no audio device).
- Multiplayer.
- The Deathmoth encounter rate over a long real play session.
- The new atrium density, which only applies to new worlds; it was set, not measured.
- `/officerooms locate all` still takes about 19 s on the server thread. It is an operator test command and is listed as a known issue.
