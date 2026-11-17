# Office Rooms 0.6.1 – Test report

Only tests that were actually run are listed. Environment as in `TEST_REPORT_0.6.0.md`: real Forge 47.4.28 server with `OfficeRooms-0.6.1.jar`, OfficeCraft 1.0.3, Refurbished Furniture 1.0.20, Framework 0.8.0, fresh world (seed 4711), plus the dev client (software rendering, no shaders).

## 1. Build and unit tests

| Test | Result |
|---|---|
| `gradlew build`, 11 JUnit tests | ✅ 11/11 |
| Vertical grid (height 608) | ✅ 101 storeys, slab of storey 0 at Y −63 (quartz), floor at −62 (wool), top slab inside the build height |
| Dark bottom storeys | ✅ 1,938 rooms in storeys 0–4 without lights, 33,438 lit rooms above |
| Poolrooms rarity (1600 regions) | ✅ 28 sites, all with maze vault |

## 2. In game (production server)

| Test | Result |
|---|---|
| Open-plan office: floor, ceiling, furniture | ✅ grey wool floor, quartz ceiling with panels, OfficeCraft desks/chairs/monitors stand on the wool (screenshot) |
| Block layers at the player | ✅ walk level air, below wool, below that quartz, below that the room underneath; above: 4 blocks air, quartz ceiling, wool floor of the next storey |
| Staircase cross-section | ✅ lower landing wool, 5 quartz stair steps (one more than before), upper landing wool on quartz, open stairwell above |
| Atrium cross-section (mega atrium, gallery) | ✅ atrium walls, gallery edges and slabs under parapets/walls are quartz; wool only on walkable gallery and room floors; atrium ceiling quartz |
| `/officerooms verify 2` in 3 office areas + start area | ✅ 0 deviations, 0 errors each |
| Poolrooms (two sites) | ✅ hall renders; verify 0 deviations, 0 errors |
| Poolrooms ring passages | ✅ straight passages from office doors into the hall at the hall level (Y 419) and the gallery level (Y 431) |
| Office doors on the Poolrooms edge at other storeys | ⚠️ → ✅ they open onto the tile ring (existed in 0.6.0 too); now counted as intended by verify (first run: 9 errors, after change: 0) |
| Hound on wool | ✅ chased and attacked normally (player killed in 2 hits on Normal); stands at full-block height |
| Zombie on wool | ✅ walks normally |
| Natural spawns (survival, 2 min) | ✅ 4 Wolfspiders, 1 Hound, 1 Smiler; spawned exactly on walk levels |
| Portal ignition (sand onto sculk sensor, Overworld) | ✅ Liminal Rift appears |
| Server stop afterwards | ✅ no "Entity is already tracked!", no exceptions |

## 3. User-reported problems

| Problem | Finding |
|---|---|
| OfficeCraft furniture invisible (outlines only), very low FPS | From the user's log: Oculus 1.8.0 with the BSL v8.4 shader pack on Embeddium. OfficeCraft draws furniture with its own renderer, which does not show with that shader setup. Not reproducible here without shaders (furniture visible). Documented workaround: turn shaders off. **Not tested with shaders.** |
| Crash "Entity is already tracked!" on world exit (`PortalIgnition.onLeave`) | Fixed in code (only landed sand is handled, unloaded chunks are never read). My reproduction attempt (falling sand unloaded at shutdown) did **not** trigger the crash with either version, so the fix is verified only by code review and the clean shutdown above. |

## 4. Not tested

- Shaders (Oculus/Iris) in combination with OfficeCraft.
- Upgrading an existing 0.6.0 world (not supported: needs a new world).
- Shrine in the Overworld (unchanged code, not re-checked in 0.6.1).
- Multiplayer, real GPU.
