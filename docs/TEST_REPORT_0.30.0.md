# Test report 0.30.0

## Done
- Build and all unit tests: passed (no FAILED, no compile errors).
- All 213 sound files referenced in sounds.json exist (script check).
- New mob sounds checked with `soxi`: mono, 44.1 kHz Ogg Vorbis. Music: stereo Ogg Vorbis, streamed.
- Production server plus dev client started and entered the dimension: no "Missing sound" warning for any `office_rooms` sound event; `music.level_fun` registered.

## Not tested
- **Audible playback in game is not tested.** The test machine has no audio device ("Failed to open OpenAL device"), so Minecraft's sound engine is off and the client plays nothing.
  - This covers the scream/hurt sounds in game, the track picked on join, the half-hour timer, and Level Fun music start/stop.
  - The sounds were only checked as files. `docs/sound_preview_0300.mp3` (in the delivery) contains every new scream and hurt sound in a row, so you can listen to them outside the game.
- Loudness balance against the other sounds still needs to be judged by ear.
