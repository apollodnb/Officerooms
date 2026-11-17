# Test report 0.14.0

Environment:
- Forge 1.20.1-47.4.28 dedicated server (production JAR + GeckoLib 4.8.4), fresh world, seed 4711.
- Dev client (Xvfb).

## Unit tests

18 of 18 passed. New test `hotel` found 45 hotels in 576 regions. For each hotel it checks:
- none of the regions has elevators or hidden rooms;
- every staircase has one step per height with two blocks of headroom;
- the vault has exactly one doorway;
- the lobby has a marble floor;
- the ring wall opens only at office doors.

## In game

| Check | Result |
|---|---|
| `locate hotel` / `locate hotel_vault` | Hotel at 232 245 228; vault door (level 3) at 303 239 308 |
| Lobby, guest corridor, guest room, vault | Rendered as planned (screenshots): wallpaper, wainscot, carpet, room signs, beds, nightstands, TV, vault door with sign |
| Hounds in the hotel (player on a guest floor, 45 s) | 3 Hounds spawned, on the guest-floor walk level |
| The Maze (player inside, 45 s) | 1 Flayed, 0 Hunters |
| Server log | No exceptions |

During testing I found that the item model of the Flayed spawn egg was missing in 0.13.0. It is fixed in this version.

## Not tested

- Walking every staircase in game (the geometry is covered by the unit test).
- Opening the vault and the hotel chests in game.
- Multiplayer, shaderpacks.
