# Test report 0.36.0

Build: `./gradlew build` with all unit tests passing. The new test `HotelLayoutTest` covers 40 seeds × 6 floors and checks that every cell of every guest floor can be reached from the staircase arriving from below (through corridors, room doors and connecting doors), that there are two staircases per floor (none on the top floor), and that every staircase cell is a corridor.

In-game test: production client with Forge 47.4.28, fresh world, seed 4711. Screenshots at gamma 0 with night vision on.

| Check | Result |
|---|---|
| Hotel lobby loads, `/officerooms goto hotel` works | ok |
| Guest floor: narrow corridors with sconces, iron door with "ROOM … DO NOT DISTURB" sign | ok |
| Rooms: dining room, bedroom (bed, nightstands, rug), bathroom (tile floor), empty room | ok |
| L-shaped staircase level 2 → 3, opening and railings on level 3 | ok; railings connect into a fence (fixed and re-checked in the final build) |
| ASCII floor plan from a dev dump: labyrinth corridors, kinks, room doors, stairs on lobby and guest floors | ok |
| Level Fun: straight staircase between storeys, opening with railing above, disco floor, ball pit | ok |
| Neighbourhood: two-storey houses, mailbox, street lamp, open door, stairs inside with chest, upstairs bedroom | ok |
| Industrial: copper tank room, foundry (blast furnaces, lava vat) | ok |

Not tested in game:
- the railing connection fix for Level Fun (same approach as the hotel, which was checked);
- the lobby staircases (shown only in the floor-plan dump);
- the Neighbourhood parks;
- the new Industrial locker room;
- the Level Fun banquet and dark cells;
- loot in the new chests;
- mob spawning on the upper storeys;
- old worlds (the code keeps the old storey counts and layouts when `content_version` is 0).
