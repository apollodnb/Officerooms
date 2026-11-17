# Office Rooms 0.6.3 – Test report

Only tests that were actually run. Production Forge server + dev client (software rendering, **no shader pack available in the test environment**), OfficeCraft 1.0.3, Refurbished 1.0.20.

| Test | Result |
|---|---|
| Build, 11 JUnit tests | ✅ |
| Wrapper installed (log "OfficeCraft shader compatibility renderer installed.") | ✅ |
| Mesh loading for all furniture in an open-plan office (desks, chairs, printers, coffee/vending machines, conference tables, boards) | ✅ after fixing the file header constant; no load errors |
| Same view `OFF` vs `ALWAYS` (open office, 3 positions) | ✅ geometry, textures, monitor images, plants, drawers, chairs identical; side faces slightly darker with entity lighting (screenshot `docs/bilder/39_officecraft_compat_vergleich.png`, top OFF, bottom ALWAYS) |
| Translucent parts (water cooler bottle) | ✅ translucent blue as in the original |
| Live switch of the setting while playing | ✅ (file reload) |
| `AUTO` without shader pack | ✅ OfficeCraft's own renderer used, furniture visible, no errors |
| JAR contains no OfficeCraft files | ✅ |

**Not tested:** the actual target case — Oculus/Iris with a shader pack (detection through the Iris API and drawing inside the shader pipeline). Needs a test on a real PC with shaders. Performance with shaders not measured.
