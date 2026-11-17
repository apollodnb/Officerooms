# New in Office Rooms 0.38.1 (beta)

Fixes from walking the parts that had never been tested on foot.

- **Poolrooms:** some Poolrooms could not be built at all (a pool in a narrow hall ran past the region). With seed 4711 that was 7 Poolrooms in the area tested. `/officerooms locate poolrooms` failed with an error. Pools now always fit inside their hall.
- **Hotel L-stairs:** they had five steps for a six-block storey, so you had to jump at the top. The sixth step now lies in the floor above. In one of four rotations the second flight was even made of **beds** instead of steps (since 0.36.0); fixed.
- **Level 0 and Maze ramps:**
  - the corner step faced the wrong way (a full block edge when you came from below);
  - the ceiling was cut open one step too late, so you hit your head;
  - the ceiling beside the steep part is open now too, so you no longer get stuck when you drift sideways.
  - Both ramps can now be walked all the way up.
- **Quest "Down the Pipe":** says to use (right-click) the slide opening.
