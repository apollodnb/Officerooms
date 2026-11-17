# Test report 0.30.1

## Done
- Build and all unit tests: passed.
- New/louder files checked with sox: mono, RMS around −8 to −14 dBFS (0.30.0: around −20 dBFS); loops 17.55 s (Flayed) and 15.36 s (Hound).
- In game: summoned a Flayed and a Hound next to the player in survival and left them for 25 s. No exceptions on client or server.

## Not tested
- **Audible playback is not tested.** The test machine has no audio device, so the sound engine is off. This covers the loops following the mob, the loops stopping, and the loudness in game.
