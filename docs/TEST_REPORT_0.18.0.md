# Test report 0.18.0

- Unit tests: 20 of 20 passed.
- `sounds.json`: 77 events; every referenced file exists.
- The Smiler sound files are unchanged (last modified 9 November).
- Production client and server start; no Office Rooms sound errors in the client log (only missing vanilla sounds of the dev environment); no exceptions on the server.
- Hurt sounds measured (spectral centroid, see NEW_IN): all deeper than before.
- A listening preview of 19 sounds (`OfficeRooms_0.18.0_sound_preview.mp3`) was produced from the generated files.

## Not tested

- **Listening in game.** The test client has no audio device, so I could not hear the sounds in game: volumes, reverb, distance falloff and how the area soundscapes are chosen while walking have not been checked by ear. Please listen to the preview and tell me what to change.
- Multiplayer.
