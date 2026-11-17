# Test report 0.31.0

## Done
- Build and all unit tests: passed, including the new `CinemaTest`:
  - the cinema exists only in new worlds and in about 3–10 % of regions;
  - it never overlaps another megastructure;
  - every hall has doors, a screen, at least 8 seat rows and a free aisle;
  - the lobby connects to the corridors.
- In game (fresh world, `goto cinema`, screenshots with **night vision** because at brightness 0 the cinema is almost black):
  - lobby with box office, TICKETS marquee, queues, posters and the concession stand with all machines;
  - corridors;
  - six auditoriums: dark, empty screen and wrecked (torn screen, broken seats);
  - seats, exit signs, aisle steps.
  - No missing models or textures in the log.

## Not tested
- **Room reverb is not tested:** the test machine has no audio device, so OpenAL never starts. The code switches itself off on any error.
- Static and figure halls were not visited (they exist in the south wing or other cinemas).
- The projector sound, lost-and-found chests and the hazard zone/elevator arrival in the cinema were not tested.
- Some distant chunks showed sky in screenshots because they were still loading.
