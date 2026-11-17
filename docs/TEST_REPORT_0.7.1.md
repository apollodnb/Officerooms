# Office Rooms 0.7.1 – Test report

Only tests that were actually run. Production Forge server with `OfficeRooms-0.7.1.jar` and the furniture mods, dev client.

| Test | Result |
|---|---|
| `gradlew build`, 11 JUnit tests | ✅ |
| Armor worn in game, third-person front view (screenshot `bilder/42_multidimensional_armor.png`) | ✅ face visible through the helmet opening, shoulder plates, bracers, belt, thigh plates, boots; arms and belly visible between the plates |
| Command output in English: `info`, `stats`, `goto meeting_room`, `goto atrium`, `mobs`, `verify 1`, `spawning true` | ✅ (all English, `goto atrium` now finds an atrium window, verify `RESULT: OK`) |
| Existing config file with German comments | ✅ comments replaced by English ones on the next start |
| Source scan for German strings in code (excluding comments) | ✅ none left |
| Server log during the test | ✅ no exceptions |

Not tested: armor seen from behind and in first person; the German game language setting in the client.
