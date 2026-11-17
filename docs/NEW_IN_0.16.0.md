# New in Office Rooms 0.16.0

## 1. Much darker

- The dimension's base brightness (`ambient_light`) is now 0 instead of 0.07.
- The light curve of the dimension is much steeper:
  - directly under a lamp it is as bright as before;
  - block light 8 is only about 60 % as bright;
  - block light 3 is about 40 %;
  - unlit areas are close to black.
- This runs on the client (`OfficeRoomsSkyEffects.adjustLightmapColors`) and applies only in the Office Rooms dimension.

## 2. Level Fun =) (new megastructure)

Level Fun is as rare as the other megastructures. One storey of a region (160 × 160 blocks) becomes an endless party in yellow.

- Walls are only partly built, Level-0 style: pillars at every grid point and many wall pieces with gaps.
- Furnishings:
  - party tables with cake and gift boxes;
  - clusters of floating balloons in five colours;
  - colourful streamers under the ceiling;
  - confetti on the carpet;
  - piles of gift boxes, with a real chest among them (`level_fun_gift`: cake, cookies, fireworks, almond water, glowsticks, golden apples, emeralds, Research Files 15 %);
  - "=)" painted in red on the walls, dripping.
- Flickering fluorescent strips; some are dead.
- New blocks: Party Wallpaper, five balloons, Party Streamer, Confetti, Gift Box, Party Table, "=)".
- `/officerooms locate level_fun`.

## 3. The Partygoer

An own design: a tall, slender, glossy yellow figure with a painted smiley face and a striped party hat.

- It looks friendly. It watches you, waves, giggles and blows a party horn.
- It slowly follows you until it stands about **7 blocks** away, then just stands there smiling.
- Come closer than **6 blocks**, or hit it, and it laughs and sprints at you (6 damage). A sprinting player can still get away.
- Up to six Partygoers roam near a player in Level Fun.
- Drops cookies, and a Research File with a 5 % chance.
- All sounds are synthesised for the mod (`tools/gen_sounds_0160.py`): giggles, party horn, laughter, squeak, deflating balloon.
