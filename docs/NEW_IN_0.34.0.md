# New in Office Rooms 0.34.0 (beta) – Ramps, pit fields, a three-level Maze

## Carpeted ramps
- New block **Carpeted Ramp**: one continuous slope of the same carpet as the floor. It walks like stairs.
- All ramps between Level 0 floors and all split levels in The Maze use it. The last step now lies in the upper floor, so you walk straight up without a jump.

## Pit fields
- **Level 0:** the old Threshold rooms are now pit fields (on floors 2–4).
  - A wide room whose floor is a grid of deep square pits with narrow carpet walkways between them.
  - Below each pit is a seven-block shaft. At the bottom the shafts are joined by low openings, so you can walk out of the floor below.
- **The Maze:** the same pit fields, on its upper levels.

## The Maze: three levels
- The Maze now has **three levels** on top of each other (four office storeys high). Each level is its own labyrinth with all the Level 0 features of 0.33.0.
- **Between the levels:**
  - three ramps up per level;
  - pit fields (a 24 × 24 hall of pits) dropping into a grid of chambers on the level below;
  - holes through the floor.
- **Exits:** 3–5 exits lead from the lowest level back into the offices.
- **The vault:** it stands on a **random level at a random spot**. `/officerooms goto maze_vault` shows which maze floor.
