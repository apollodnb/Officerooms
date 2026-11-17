# New in Office Rooms 0.20.0 – Abandoned Research Outposts

Project LIMINAL: unknown researchers investigated the Office Rooms from several dimensions. Their abandoned stations are now scattered through the Overworld and the Nether.

## Where they are
Outposts are only placed in chunks that are generated for the first time, so worlds you have already explored stay as they are.

| Location | How often | Integration |
|---|---|---|
| Ancient Cities | 60 % of cities | A sculk-grown annex just outside the city, with a 3 × 3 tunnel to the first walkable spot inside the city cavern. |
| Jungle Temples | 40 % of temples | Buried 9 blocks below the temple floor next to it. A ladder shaft with an iron trapdoor leads down from the jungle floor. |
| Caves below y 0 | about 1 in 1,800 new chunks (very rare) | Dug into the rock, opening onto a cave passage. Cobwebs, dripstone, collapsed walls. |
| Nether | about 1 in 500 new chunks | Built on solid ground on a blackstone foundation. Blackstone bricks, basalt pillars, barred windows, soul lanterns, magma vents. |

- The Ancient Cities, Jungle Temples and Nether terrain keep their structure: only the outpost and its access tunnel or shaft are added.
- The Office shrines are not affected; their code and data were not changed.

## What is inside
- A 13 × 11 laboratory with its own blocks: lab wall panels, floor tiles, ceiling tiles and a hazard floor.
- Furniture: control consoles with three gateway monitors, lab cabinets, specimen jar, Research Table, CCTV bank, server racks, containment tank, experiment machine, microscope and a fallen CRT.
- Two research chests (`chests/research_outpost`): research files, electronic components, batteries, filters, glitched ingots, redstone, copper, sometimes diamonds or rare loot.
- A lectern with a research note. Each kind of outpost has its own logs (sculk annex, temple station, deep bore site, Station Ash, Project LIMINAL summary); every log ends with the laminated gateway procedure.

## Activating the gateway
1. **Main power.** Pull the large industrial lever on the side wall ("MAIN POWER").
   - The facility boots: lights come on one after another, and the monitors show "PROJECT LIMINAL – BOOTING".
2. **Warning sequence (11 s).** The gateway stays locked while:
   - the monitors cycle through emergency warnings ("DIMENSIONAL INSTABILITY DETECTED", "CONTAINMENT FAILURE", "UNKNOWN DESTINATION", "STAFF STATUS: MISSING", …) and flicker;
   - the red warning lights flash and an alarm sounds;
   - sparks fly from the frame and the lights drop out.
   - The monitors end on "PORTAL SYSTEM READY – PRESS THE RED EMERGENCY BUTTON".
3. **Emergency button.** Press the large red button on its yellow-black pedestal.
   - Activation takes 8.5 s: a charging whine, arcs across the frame, the frame coils light up cyan one by one, the lights flicker, the ground shakes, and the screens count up to 100 %.
   - The gateway fills in from the bottom, with a flash and a roar.
- **Safety locks.**
  - Without power the button only reports "NO POWER".
  - During the warnings it reports "SAFETY LOCK".
  - Pulling the lever back shuts everything down and closes the gateway.
- **The gateway** takes you into an office room of the Office Rooms. It is server-side and works for each player separately (multiplayer).

## Test command
`/officerooms outpost <lab|ancient|jungle|cave|nether>` (op): builds an outpost in front of you.
