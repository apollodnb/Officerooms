# Test report 0.5.0 (tests actually run)

Environment: Forge 1.20.1-47.4.28, Java 17, dev server + dev client under Xvfb (software rendering, OpenAL wave-file output for audio), plus a production server (Forge installer, mods: OfficeRooms 0.5.0, OfficeCraft 1.0.3, Refurbished 1.0.20, Framework 0.8.0) with a connecting client. Date: 2026-11-09.

| Area | Test | Result |
|---|---|---|
| Build | `gradlew build` | OK |
| Workplace alignment | Close-up screenshots: open office, standard office (desk at wall), cubicle workstation, before/after fix | Fixed: monitor and keyboard now face the chair |
| Portal (entry) | Sculk sensor + falling sand (dev and production) | Rift appears at the sensor position, sand item consumed; walking in teleports into the dimension; return point saved |
| Portal (exit) | Exit room on storey 0, walk in (dev and production) | Back to the saved point in the Overworld; frame without sign |
| World generation | Server log after visiting all special rooms | 0 decoration failures (bug found and fixed: campfire food/sign text crashed during generation) |
| Carpets | `/officerooms verify` at spawn, abandoned camp, oasis, villager outpost, lush cave (dev) and at spawn, outpost, camp, exit room (production) | 330 000–356 000 carpets per check, 0 faulty, result OK |
| The Gaunt | Summoned; natural spawning in dark storeys (survival, 2.5 min) | Attacks immediately, very fast and deadly; natural spawns on dark storeys only, ≥24 blocks away, max. 1 |
| The Observer | `/officerooms observer` | Appears at the end of a corridor; vanished on staring and on approach (log reasons "stare", "near"); vanishes when no non-spectator player is near |
| Animals | Entity counts | Outpost: 1 cat, 1 iron golem, 4 villagers; lush cave: 2 frogs |
| Outposts | Screenshots | Stair ring, survival decor, dining set, office chairs at the fire |
| Abandoned camp | Screenshots, lectern | Tents, furniture, overgrowth; lectern has an English story book |
| Oasis | Screenshots, floor scan | Fully decorated (after generation fix), pools, palms, loungers, beach bars |
| Loot | 200 rolls each | `abandoned_camp` 0/200 empty, `outpost` 3/200 empty, `oasis` 0/200 empty; oasis has diamonds, emeralds, enchanted books |
| Music | Interval set to 2 min, audio log + wave levels | Starts on entering; repeats after exactly 2:00 (real time); continued through teleports into old and new chunks; restarted correctly after a quick dimension hop; one unexplained cut-off ~50 s after joining (see open features) |
| Ambient sounds | Gaps 8–15 s | Different sounds at varying positions; none while music plays; no lamp-hum restarts on chunk changes |
| Wolfspider | Volume values in code/sounds.json | Reduced (not measured by ear) |

Not tested: multiplayer with several players; very long sessions (30-minute music interval in real time); every furniture colour/orientation variant.
