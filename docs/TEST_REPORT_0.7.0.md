# Office Rooms 0.7.0 – Test report

Only tests that were actually run. Production Forge server with `OfficeRooms-0.7.0.jar`, OfficeCraft 1.0.3, Refurbished 1.0.20, Framework 0.8.0, dev client (software rendering).

## Build
| Test | Result |
|---|---|
| `gradlew build`, 11 JUnit tests | ✅ |

## Bug hunt
| Test | Result |
|---|---|
| All special room types visited on the new storey grid (outpost, lush cave, water office, sculk office, camp, oasis, exit room) | ✅ look correct |
| Door control fallback, first version (relaxed rule) | ⚠️ placed a button in a neighbouring doorway (verify: 1 error) → reworked |
| Door control fallback, final version, same spot in a fresh world | ✅ verify 0 errors there, in the Poolrooms and at 0/0 |
| `goto atrium` | fixed (now uses the atrium window search) – re-run in 0.7.1: ✅ |
| Server/client logs during all tests | ✅ no exceptions from Office Rooms |

## Performance
| Test | Result |
|---|---|
| JFR profile, player in an open office: 66 % of the dimension tick in entity ticks (villager/warden brains), ~13 % vanilla spawner | analysis |
| Same benchmark before/after (fresh world, 12 teleports, 20 s idle, `/forge tps`) | ✅ dimension 31.2 ms → 5.8 ms, overall 33.6 ms → 7.9 ms |
| Villagers near a survival player still move | ✅ |
| Office Rooms spawns still work (2 min census) | ✅ Wolfspiders, Hounds, Smilers present |

## Multidimensional gear, Glitched Ingot, Rod
| Test | Result |
|---|---|
| Items, worn armor rendering (screenshot) | ✅ |
| Loot rolls (400 per table): see NEW_IN_0.7.0.md table | ✅ all three drop |

## New items
| Test | Result |
|---|---|
| All new items render in the inventory (screenshot `bilder/41_items_070.png`) | ✅ |
| Witherfang hit on a zombie | ✅ Wither (id 20) |
| Leaden Edge hit on a zombie | ✅ Slowness (id 2) |
| Venomtongue hit on a pig | ✅ Poison II (id 19), 7.5 damage; on a zombie no effect (undead, vanilla) |
| Totem of Mercy: 200 damage in the Office Rooms, totem in inventory | ✅ player alive in the Overworld at world spawn (no bed), totem consumed |
| Liquid Pain: drunk (right-click hold) with a Totem of Mercy in the inventory | ✅ instant death, message "Dev drank Liquid Pain", totem not used |
| Loot: Liquid Pain, swords, Totem in rolls | ✅ (e.g. vault: Totem 11.5 %, sculk special: Totem 0.75 %) |

## The Hunter
| Test | Result |
|---|---|
| Natural spawn on entering the Poolrooms | ✅ one Hunter ~57 blocks away |
| Chase | ✅ 30 blocks in ~8 s toward the player; attacks at close range |
| Look | ✅ dark line figure (screenshot `bilder/40_hunter.png`); first glowing white version was invisible against the bright tiles → changed to dark lines |
| Sense range 96 | set after the first test (48 was too small for the 160-block hall); not re-measured from 90+ blocks |

## Not tested
Totem of Mercy with a bed set in the Overworld; Hunter fights over longer time; Hunter sounds heard in game; multiplayer; shader packs.
