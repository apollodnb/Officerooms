# New in Office Rooms 0.22.0 – Blackouts

Now and then the power grid of the Office Rooms fails.

- **Announcement.** About 30 minutes after the first start, and then about an hour after each blackout, every Hazard Monitor shows:
  - "! POWER GRID ! UNSTABLE";
  - "BLACKOUT IN" with the sector and floors (for example "SECTOR E-6, FLOORS 58-64");
  - a countdown (3 minutes).
  - If a Hazard Zone is running at the same time, the screens take turns.
- **Blackout.** An area of about 97 × 97 blocks over 7 storeys around a player loses power:
  - the lights flicker for a moment and die, so it becomes very dark (night vision goggles still help);
  - the lamp hum falls silent;
  - the monitors inside the area go dark;
  - monitors outside show "BLACKOUT, FUSE BOX: FLOOR n, REPAIR REQUIRED" and the time until backup power.
- **Gaunts.** They come up out of the dark: at most 3 at a time, out of sight, 14–26 blocks away. When the power returns they go back.
- **Fuse box.** A sparking, crackling fuse box hangs on a wall at eye level somewhere in the area, and the floor is shown on screen when the blackout starts.
  - Right-click it with **3 Electronic Components** in your hand to restore the power for everyone.
  - Otherwise the backup power comes on after 20 minutes.
  - The repaired box stays as a wall decoration and can also be placed in creative mode.
- **Config** `[blackout]`: enabled, firstWarningAfter 30, cycleMinutes 60, warningMinutes 3, maxMinutes 20, radius 48, storeys 3, gaunts 3.
- **Command** (op): `/officerooms blackout status|warn|start|stop`.
