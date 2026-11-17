# Test report 0.32.0

## Done
- Build and all unit tests: passed.
- In game, in the cinema lobby and corridor, with night vision for the photos:
  - model and textures render (no missing texture);
  - leg thickness increased after the first look;
  - an awake Aranea Membri went back into the ceiling on its own after losing interest, and broken ceiling tiles appeared near the spot;
  - walking under the lurking one in survival made it drop: `Lurking` changed 1 → 0 and it landed next to the player;
  - no exceptions on client or server.

## Not tested
- **Sounds and the camera shake are not tested:** there is no audio device, and the shake was not checked visually.
- Lights dying during the hunt, natural spawning in the offices and cinemas, and the ceiling bursting were not verified visually.
- No male/antler variant yet.
