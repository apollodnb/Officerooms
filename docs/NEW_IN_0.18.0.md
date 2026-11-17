# New in Office Rooms 0.18.0 – Sound Overhaul

All new sounds are synthesised for the mod (`tools/gen_sounds_0180.py`); no foreign audio material is used. Mob mechanics, damage and spawn rates are unchanged.

## Creatures

Every hostile creature has new, darker and deeper sounds, layered from growls, distorted breathing, wet organic noises and reverb. Most sounds come in 2–4 variations, and the idle sounds also play at a second pitch, so they repeat less.

| Creature | New sound identity |
|---|---|
| **The Flayed** | Four long signature screams, 4–6 s each: an agonised human-like voice, a guttural voice two octaves below and a shrill layer on top. Each begins with tearing muscle and ends in a long reverb tail. Audible up to 160 blocks. It now also screams in about half the cases when it resumes a chase after faltering. New: wet breathing, muscle tearing when it hits, heavy wet footsteps, a deeper hurt sound and a long collapsing death. |
| Wolfspider | Low growls with clicking legs, new clicking footsteps, a hiss-and-snap attack, a dark snarl when hurt. |
| Hound | Deep, rattling growls with heavy breathing, monstrous barks with a chest thump, a low pained growl instead of a yelp. |
| Hunter | Heavy distorted breathing over a low hum, sometimes with a distant scream inside. A distorted roar when it notices you, a metallic groan when hurt, heavy wiry footsteps. |
| Colony | Screams from four throats, out of tune with each other. Deep groans with wet clicks, heavy wet footsteps. |
| Gaunt | Dry, rasping breath over an almost inhuman growl, sudden distorted outbursts, dry bony footsteps. |
| Headless Warden, Partygoer | Deeper hurt and death sounds (the Partygoer no longer squeaks). |
| **Smiler** | Unchanged, as requested. |

Hurt sounds are clearly deeper. Measured spectral centroid, old → new:
- Hunter: 4023 → 552 Hz
- Flayed: 2839 → 1269 Hz
- Wolfspider: 2412 → 1589 Hz
- Gaunt: 1915 → 734 Hz
- Headless Warden: 1772 → 445 Hz
- Colony: 1896 → 1390 Hz
- Partygoer: 1426 → 927 Hz
- Hound: 1308 → 914 Hz

## Ambience

- **Everywhere:**
  - deep electrical buzzing with flicker and crackle;
  - air moving through vents;
  - pipes knocking and groaning;
  - something very far away (a muffled scream, a growl, a roar).
- **Area soundscapes** (chosen from the blocks around you):
  - Industrial Complex: heavy machine hum, hissing steam, pipes and metal. Running generator cores also hum loudly on their own.
  - Labs and security rooms: server fans, beeps and relays.
  - Hotel: distant doors, an elevator chime, a muffled TV, creaking.
  - Level Fun: a cheerful tune from a broken speaker that slowly goes out of tune, an off-key music box, distant giggles and party horns.
  - Poolrooms: a deep groan across the water, added to the water sounds.
- The lamp hum is louder and the music quieter (since 0.17.0).
