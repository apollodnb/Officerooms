# New in Office Rooms 0.21.0 – Level 0

## Getting in
Break any block of the **topmost ceiling of the Office Rooms** (the ceiling above the highest storey; y 543 with the default settings).
- The ceiling block stays intact; you are pulled through into the new dimension **Level 0** (`office_rooms:level_0`).
- You land on a free spot on a random one of its four floors.

## Level 0
- **Endless and procedural.** Four stacked floors of yellow wallpaper, damp carpet and fluorescent ceiling panels (about one in six is dead).
- **The layout:**
  - Cells of 6 × 6 blocks with 4-block-high rooms.
  - Wide openings, doorways and 1-block-wide slits.
  - Dead ends, loops and pillar halls.
  - Oversized tables (you walk underneath them) and giant chairs.
  - Ladders between the floors.
- **Threshold Rooms** (rare, floors 2–4): huge halls with rows of square holes that drop you to the floor below.
- **Always reachable.** Every floor of every 96 × 96 area is a spanning tree plus extra loops, joined to its neighbours on every border. Every free cell can therefore be reached without mining.
- **No mining, no building.**
  - Breaking and placing blocks is cancelled, and Mining Fatigue V is applied.
  - Explosions do not destroy blocks.
  - Ender pearls and chorus fruit do not teleport.
  - The Noclip Sword does not work.
- **The immortal Flayed.**
  - Exactly one Flayed hunts here, with the existing model, sounds and behaviour (scream, chase, falter).
  - It cannot be damaged.
  - When it gets stuck in the maze, falls far behind or is on another floor, it reappears out of sight close to you.
  - No other creatures spawn in Level 0.

## The way out: Manila Rooms
- **Rarity.** About one per five 96 × 96 areas, on a random floor. They are meant to take around 15 minutes of searching; this is an estimate and was not measured.
- **Hidden.** The room is lined with manila wallpaper on the inside only. Its single entrance is a 1-block-wide slit.
- **The puzzle.**
  1. The chest holds exactly one Water Bucket.
  2. Under the big table in the corner is a 1 × 1 hole: too narrow to walk on and too low to crawl.
  3. Empty the bucket into the hole. The whole shaft fills with water.
  4. Jump in and sprint-swim through the 1 × 1 passage to the light.
  - If you jump in dry, you are helped back out after 8 seconds.
- **Where you come out.** The way out sends you to a random office room in the Office Rooms, 800–3,200 blocks from where you broke in.
- **Reward.** Exactly one of these, chosen at random:
  - 3 Research Files;
  - 1 Multidimensional Sword;
  - 6 Glitched Ingots (the existing ingot for Multidimensional gear; there is no item called "Multidimensional Ingot").
  - You get a reward only once per trip into Level 0 (multiplayer-safe, per player).

## Books
- 7 new rare books in the Office Rooms: case file 0-117, survivor J. Marsh (two books), the Project LIMINAL ceiling study, missing posters, the Threshold note and the top-floor security log.
- Research outposts can now contain "Membrane Hypothesis".
- They hint at the top ceiling, the four floors, the immortal one, the manila rooms, the water and the hole.

## Commands (op)
- `/officerooms level0`: enter Level 0.
- `/officerooms level0 manila`: go to the nearest Manila Room.
- `/officerooms level0 threshold`: go to the nearest Threshold Room.
