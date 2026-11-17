# Office Rooms 0.6.0 – Test report

Only tests that were actually run are listed. Environment: Forge 47.4.28, Java 17. "Dev" = `gradlew runServer` + `runClient` (Xvfb, software rendering, OpenAL output captured to a WAV file). "Production" = real Forge server install with the built `OfficeRooms-0.6.0.jar`, OfficeCraft 1.0.3, Refurbished Furniture 1.0.20 and Framework 0.8.0, `level-type=normal`, seed 4711, plus the dev client. Test machine: 2 CPU cores.

## 1. Build and unit tests

| Test | Result |
|---|---|
| `gradlew build` (compile, 11 JUnit tests, reobf JAR) | ✅ 11/11 passed |
| New test `poolroomsAreRareAndComplete`: 1600 regions, seed 4711 | ✅ 28 Poolrooms regions (1.75 %), all deterministic, every one has its maze vault |
| `specialRoomsAppearRegularly` | ✅ after excluding POOLROOMS (region-level band, cannot appear in its 9-region sample) |

## 2. Abandoned Office Shrine

| Test | Result |
|---|---|
| `/locate structure office_rooms:abandoned_office_shrine` (dev world switched to normal terrain, and production) | ✅ found at −768 / 112 |
| Generated naturally, looked at in game (dev and production) | ✅ walls, ceiling remnant, moss, vines, sculk, candles, skull, debris; sits naturally in the terrain |
| Chest loot table | ✅ `office_rooms:chests/office_shrine` (production, checked by NBT) |
| Lectern book | ✅ "Prayer" (production, checked by NBT) |
| Sign texts | ✅ "Those who dare will never leave." / "Do not combine them. You won't survive." (dev) |

## 3. Poolrooms

| Test | Result |
|---|---|
| `/officerooms goto poolrooms` / `goto poolrooms_maze` | ✅ dev and production |
| Visual check: halls, arches, gallery, stairs, pools with lanterns, lighting | ✅ dev (several screenshots), production |
| `/officerooms verify` in the Poolrooms area (dev) | ✅ 106,121 special blocks, 0 deviations, 0 faulty carpets |
| `/officerooms verify` in the Poolrooms area (production, 504/541) | ✅ 110,192 special blocks, 0 deviations, 0 errors, carpets 0 faulty |
| Maze: flooded corridors, path length, vault chest at the far end | ✅ dev (vault at 588, 8, 396) |
| Own ambient sounds | ✅ dev: inside the Poolrooms the subtitle "Pool pump hums" appeared (POOL sound set chosen) |

## 4. Entities

| Test | Result |
|---|---|
| Hound holding Mushroom Stew | ✅ no damage over 9 s |
| Hound without stew | ✅ player killed in 2 hits (20 → 8.8 → 0) |
| Hound: switching to stew mid-attack | ✅ no further damage, it walks away |
| Smiler: look, stalking | ✅ |
| Smiler: touch kill | ✅ "Dev was slain by The Smiler" |
| Smiler: torch pointed at it | ✅ stays while you look away with a torch or face it without one; gone when you face it with a torch |
| Smiler: bright light (light blocks level 15 placed over it) | ✅ gone after ~3 s |
| Colony chase on a straight 120-block track, player moving 3 blocks/s | ✅ follows at ~4.6 blocks/s; falters twice (t ≈ 8–10 s and 17–19 s), then resumes |
| Colony contact damage (Easy, no Resistance) | ✅ damage applied (20 → 17.8 → 14.5 → 13.3 → 9.8 while Regeneration was active) |
| Natural spawns, dev, before the spawn-chance fix (3 min) | ⚠️ Hound, Smiler and Colony all spawned, but a Colony was nearly always present, which is not "extremely rare" |
| Natural spawns, dev, after the fix (6 min, moving) | ✅ Hound seen 2×, Smiler 1×, Colony 0× |
| Natural spawns, production, survival (4.5 min, moving) | ✅ Smiler appeared; no Hound or Colony in that window (expected with the new rarity) |

## 5. Production server

| Test | Result |
|---|---|
| Start with 0.6.0 + furniture mods, fresh world | ✅ no errors in the server log |
| Shrine, Poolrooms, verify, spawns | ✅ see above |
| New recipes (6) known to the server | ✅ `/recipe give` accepts all six ids (an unknown id is rejected) |

## 6. Performance

| Test | Result |
|---|---|
| "Can't keep up" warnings while teleporting every 45 s into ungenerated areas with server **and** client on the 2-core machine | ⚠️ 15 warnings in ~5 min |
| Profiling (Java Flight Recorder) | Found: room lookups called the Poolrooms cache under a lock for every region, and the plan caches suffered hash collisions between neighbouring regions. Both fixed. |
| Server-only A/B, `verify 3` (49 fresh chunks) at 5 identical positions, fresh world each | ✅ 0.5.0: 12.5 (first, JIT warm-up) / 8.2 / 8.2 / 6.8 / 7.2 s; 0.6.0: 10.7 (first) / 7.4 / 7.9 / 7.2 / 7.7 s → no regression |
| With the client running on the same 2 cores, the same measurement was ~60 % slower for 0.6.0 | Most likely CPU competition from the software-rendered client: the comparison client always ran 0.6.0 code, so with a 0.5.0 server it probably did less rendering work (not verified). The server-only A/B above is the reliable comparison. |

## 7. Not tested

- Colony falter on Normal/Hard and with armor; Colony scream subtitle seen on screen.
- Hound calming after being hit while holding stew (implemented, not tried).
- Natural Hound or Colony spawns inside the Poolrooms specifically (spawn code shared with the offices; Hound/Colony natural spawns were only observed in the offices).
- Smiler `blink()` behind the player after being stuck (only seen to get stuck before the fix).
- Multiplayer, real GPU, long sessions, single-player world created via the menu.
- Existing 0.5.0 worlds updated to 0.6.0.
