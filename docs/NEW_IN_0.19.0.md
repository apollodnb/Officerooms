# New in Office Rooms 0.19.0 – The Underside

## The way down
- **Six artifacts.** Each megastructure holds exactly one artifact in a fixed chest. Every item has its own texture and a short line of lore.

  | Artifact | Megastructure | Chest |
  |---|---|---|
  | Chlorine Pearl | Poolrooms | first chest at the end of the drowned maze |
  | Faded Photograph | Neighbourhood | first house with a chest |
  | Thread of the Maze | Maze | vault |
  | Room Key 000 | Abandoned Hotel | first chest in the cellar vault |
  | Humming Fuse | Industrial Complex | chest in generator hall 1 |
  | Party Smile Badge | Level Fun | first present |

- **Underside Key.** Crafted (shapeless) from all six artifacts. Use it on the bedrock beneath the lowest floor of the Office Rooms: the bedrock opens 3 × 3, the ground shakes, and the key is used up.
- **The fall.** Anyone who falls through the hole into the void lands in the Underside instead of dying. You arrive just below the upside-down meadow and drift down slowly onto the grass.

## The Underside (new dimension `office_rooms:underside`)
- An endless meadow with 1–2 blocks of relief, trees and flowers.
- About 11–14 blocks above it hangs the upside-down meadow: its own grass block, dirt, hanging trees and roots.
- Soft wind ambience, a pale fog and no sun. No creatures spawn here.

## Boss: [Unknown]
- **Look.** Our own design, a mix of sketches A and B:
  - a 20-block centipede made of wire-thin black lines;
  - on every third segment, a skinned ribcage with long human-like arms;
  - the Flayed's scribble head with an eye and a jaw of lines.
- **When it appears.** 20 seconds after you arrive:
  - the last 4.5 s the ground shakes (camera shake, rumbling, earth crumbling at the spot where it will break out);
  - then it bursts out vertically, roars and dives into the ceiling;
  - a boss bar shows "[Unknown]".
- **How it fights.**
  - It swims through the meadow and the ceiling.
  - Before every surfacing, dust and a short quake warn you, 1–1.5 s ahead.
  - It lunges across the gap, only about 40 % of the time directly at you; otherwise somewhere nearby.
  - After some lunges from the ceiling it crawls on the meadow for a few seconds and then dives again.
  - Touching it deals 14 damage.
  - It has 600 health and armour 10. All segments can be hit.
- **When it dies.** The segments burst one after another from the tail. Then:
  - the **Noclip Sword** drops, glowing;
  - **two portals** open: a yellow one in a wallpaper frame ("Back to the Office") and a green one in mossy stone ("Overworld", to your spawn point).
- **It comes back.** It appears again on every new visit. When nobody is left in the Underside, it disappears.

## Noclip Sword
- A netherite-class sword.
- Right-click: for 10 s you walk through walls and furniture, but never through floors. Then there is a 30 s cooldown.
- An actionbar countdown shows the time left. If the effect ends while you are inside a wall, you are pushed to the nearest free spot.

## Books
- 12 new survivor books with Okafor, Abara, Ellison and Lindqvist from the earlier logs, plus new voices: hotel guest book, generator hall shift log, Level Fun invitation, Neighbourhood watch, emergency broadcast (HAZARD ZONE), research table notes, the dark floors.
- They describe today's Office Rooms and hint, piece by piece, at the six artifacts, the bedrock, the 20 seconds, the dust warning, the two doors and the sword. They stay disturbing.

## Command
- `/officerooms underside [now]` (op): sends you to the Underside. With `now`, the quake starts after about 1 s.
