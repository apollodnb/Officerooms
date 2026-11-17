# Test report 0.26.0

Production server (Forge 47.4.28, seed 4711, fresh world) + dev client, brightness 0, no night vision.

| Area | Result |
|---|---|
| Build | `gradlew build` without errors |
| New cabin (fresh chunks) | steel walls, floor, ceiling light and panel placed at the expected positions (checked with `execute if block`); panel sits beside the door |
| Panel GUI | opens on right-click; number keys, GO / Enter work |
| Doors | close in stages (screenshot burst: open → leaves sliding → shut with centre seam) |
| Ride | storey 10 → 29 (asked for 30), storey 10 → 51 (asked for 20): never the requested floor |
| Megastructure | rides ended in Level Fun and the Hotel (before the radius was reduced to the neighbouring regions); a pig in the cabin rode along |
| Cooldown | "The panel stays dark (4:32)" right after a ride; the elevator did not respond until the restart |
| Visitor | one ride ended with a Hound inside the cabin |
| Beds | sleeping works, "Respawn point set", Sweet Dreams advancement, woken after a full sleep; died far away → respawned at the bed |
| `natural` | checked in the 1.20.1 code: only used for beds, compass, clock and Nether-portal piglins. It stays `false`; beds are handled by the mod instead |
| Flayed scream | camera visibly shifts in a screenshot burst while the camera was fixed; no errors |
| Darker dimension | screenshots of offices, cabin and Level Fun clearly darker |

**Not tested:**
- the blackout stall;
- the knocking;
- old cabins in already generated chunks (plate → panel);
- mobs riding along into a megastructure;
- multiplayer with several riders;
- a full night with several players.
