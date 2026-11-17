# New in Office Rooms 0.30.0 (beta) – Sound overhaul

All new recordings are by the mod author (Ableton). Idle and ambient mob sounds are unchanged.

## Trigger screams
- **Flayed:** three new screams (5.6 s, 9.5 s and a short 1 s shriek) when it spots you. Camera shake as before.
- **Hound:** four new bursts when it turns on a new target.
- **Colony:** seven new calls when it starts hunting.
- All mob sounds are mono so they fade with distance and you can hear which direction they come from.

## Hurt sounds (when you hit a mob)
- **Flayed, Hound, Colony:** three hurt variants each, cut from the harshest moment of their own recordings, choked and distorted.
- **Every other mob** now has creepier hurt sounds built from the same recordings:
  - Wolfspider: high, dry clicking with a reversed tail.
  - Headless Warden: the Flayed scream an octave down, hollow and huge.
  - Gaunt: thin breathy shriek with a short echo.
  - Hunter: a guttural, low Hound burst.
  - Partygoer: a strangled giggle-shriek.
  - Deathmoth: a tiny insect flutter-scream.
  - The burrowing Unknown: a reversed, low scream that swells into the hit.

## Music
- **Office Rooms:** three tracks now (the original plus two new variants). On joining or entering the dimension, one of them plays at random. After that, roughly one track per half hour (randomised 21–39 min). The rest of the time you only hear the hum and the backrooms ambience.
- **Level Fun** has its own looping music while you are in it. It pauses the office music (no overlap) and stops a few seconds after you leave. The party ambience keeps playing underneath. The old music box ambience was dropped.
- Config `musicIntervalMinutes` (default 30) still sets the average gap; `musicLevel` still sets the volume.

Generator: `tools/gen_sounds_0300.py` (sox), reproducible from the WAV masters.
