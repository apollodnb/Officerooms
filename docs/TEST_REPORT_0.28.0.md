# Test report 0.28.0

- Build and all unit tests green.
- Old world (before the fix): blocks checked with `execute if block`. The rooms beside the aisle were open (x = 73 air). The fix is in the code; that old hotel was already generated, so the wall does not appear there.
- Fresh world (content_version 1), hotel guest floors:
  - corridor at brightness 0: sconces glow between the doors, the upper floor has the teal carpet;
  - 8 rooms in a row, shot **with night vision** for detail: twin rooms, a trashed room, standard rooms and an empty room;
  - a "DO NOT DISTURB" room, shot **with night vision**: sign, iron door, barricade inside, chest.
- Wall sconce light raised from 7 to 9 after the screenshots (they were taken with 7).

**Not tested:**
- the ringing phone and the TV hiss (no audio device);
- ice machines and carts not seen in the sampled corridors;
- multiplayer.
