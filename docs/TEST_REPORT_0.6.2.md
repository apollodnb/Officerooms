# Office Rooms 0.6.2 – Test report

Only tests that were actually run. Production Forge server with `OfficeRooms-0.6.2.jar` and the furniture mods, fresh world, dev client, survival, difficulty Normal.

| Test | Result |
|---|---|
| `gradlew build` incl. 11 JUnit tests | ✅ |
| Config migration: existing 0.6.1 config with old values | ✅ log "spawn balance 2 applied"; file afterwards: hound 60 / 2 / 0.15, smiler 60 / 2 / 7 / 0.15, gaunt 30 / 6 / alert 40 / follow 64; stray `spawnChance` removed from `[gaunt]`; colony unchanged |
| Normal storeys, 3 min census (moving once) | ✅ Hounds 1–3 and Smilers 1–2 present at every sample (before: mostly 0), Wolfspiders 4–6, Colony 0 |
| Headless Wardens in the census | 8 of 9 were persistent Sculk Office guardians, 1 roaming (limit held). Roaming spawn chance added afterwards (not separately measured). |
| Dark exit levels (storey 0), 2 min census | ✅ 3–4 Gaunts within range constantly (plus Hounds/Smilers) |
| Before `senseRange`: Gaunts stood still behind walls | ⚠️ → fixed |
| After `senseRange`: player without Resistance in an Exit Room | ✅ Gaunts closed in; player 20 → 12.5 → 4.3 → dead within ~10 s |

Not tested: Gaunt pack call on its own (scream → other Gaunts join), running escapes over longer distances, multiplayer.
