# New in Office Rooms 0.26.0 (beta)

## Real elevators
- New cabin look in the generated shafts:
  - brushed steel walls;
  - a clean tiled floor;
  - a ceiling light (it goes out during blackouts);
  - a button panel on the wall beside the door.
- **Right-click the panel** (or, in cabins generated before 0.26.0, the old floor plate) to open the floor panel, then type a floor with the buttons or the number keys and press GO / Enter.
- How a ride goes:
  - Ding; the steel doors slide shut from the left and right (centre seam).
  - The cabin jolts (camera shake), hums and creaks for 4–10 seconds.
  - The display counts floors and sometimes shows nonsense.
- **You never arrive on the floor you typed.** The cabin stops on a random floor in the direction you chose, but the display claims it is the one you asked for.
- **If a megastructure lies within the floors you pass** (in a region next to the shaft), the doors always open inside it: Poolrooms, Neighbourhood, Maze, Hotel, Industrial Complex or Level Fun. There is no cabin there. You walk back.
- Everything standing in the cabin rides along, mobs included.
- **5 minutes cooldown** per elevator after a ride ("The panel stays dark").
- Rare disturbances:
  - **Blackout:** the cabin stalls and the doors stay shut until the power returns (at most 3 minutes). The doors can be broken.
  - **Visitor (8 %):** a Hound or a Gaunt is waiting in front of the doors.
  - **Knocking (15 %):** something knocks on the cabin roof during the ride.
- Jumping and sneaking no longer ride the generated shafts. Elevator plates you stack yourself elsewhere work as before (jump / sneak / right-click = floors per ride).

## Beds in the Office Rooms
- You can sleep in a bed in the Office Rooms. Using it sets your respawn point there.
- Monsters nearby still prevent sleeping.
- The dimension stays as it was otherwise: compasses still spin, clocks still don't work, no new vanilla mob spawns, no weather, same lighting rules.

## Atmosphere
- The whole Office Rooms dimension is about 30 % darker.
- The Flayed's scream now shakes the camera of players within 32 blocks (stronger up close).

## Notes
- The new cabins (steel, light, panel) only appear in chunks generated with 0.26.0. Older cabins keep their concrete walls and glowing plate; the plate opens the panel there.
- Elevator cooldowns are not saved: a server restart resets them.
