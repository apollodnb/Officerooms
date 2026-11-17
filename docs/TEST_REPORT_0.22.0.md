# Test report 0.22.0

Build OK, unit tests pass. Tested with the production server and client (screenshots in `docs/bilder/0220_blackout.jpg`).

| Test | Result |
|---|---|
| Announcement | The monitor switched from "FACILITY STATUS NOMINAL" to "! POWER GRID ! UNSTABLE, BLACKOUT IN SECTOR E-6, FLOORS 58-64, 02:58". |
| Start | Very dark, monitor in the area off. Fuse box placed on floor 63 (found at 321/312/262, on the wall). |
| Gaunts | After about 25 s in survival there was 1 blackout Gaunt in the area. |
| Repair | Survival, 3 Electronic Components in hand, right-click → components consumed, phase back to 0, light back, the Gaunt was removed. |

## Not tested
- Monitors outside the area showing the fuse-box floor: the test spot was dark, so the screenshot shows nothing. The code path is the same as the warning text.
- Automatic cycle over real time (only started by command).
- Backup power after 20 minutes.
- Multiplayer.
- Hearing the hum fall silent (no audio device).
