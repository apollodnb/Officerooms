# New in Office Rooms 0.31.0 (beta) – Room reverb and the Abandoned Cinema

## Reverb by room size
- In the Office Rooms dimensions every world sound now gets reverb that matches the space around you. The client measures the room around you several times per second (26 directions, up to 48 blocks).
  - Small furnished offices with carpet stay dry and muffled.
  - Long corridors, atriums, the Poolrooms, the tiled cinema lobby and big halls ring out with a long, bright tail.
  - Soft surfaces (wool, carpet, moss) dampen it.
- Footsteps, mob screams, doors and ambience all get it. Music, the lamp hum and UI sounds stay dry.
- Client config `[audio]`: `roomReverb` (on/off) and `roomReverbStrength` (0–2). The reverb uses OpenAL EFX and switches itself off quietly if your sound system does not support it.

## The Abandoned Cinema (new worlds only)
A new megastructure, as rare as the others. Two storeys of a region become a multiplex nobody has visited in decades. You enter it through office doors on its lower storey.
- **Lobby**, double height, with checkered tiles that echo:
  - a box office with ticket windows under a lit "TICKETS" marquee;
  - velvet-rope queues;
  - two concession stands with popcorn machines, soda fountains, candy cases, glowing menu boards and "CONCESSIONS" marquees;
  - faded posters ("HALL", "STAY", "OPEN", "EXIT", "LV0", "LOOK UP"), spilled popcorn and a "NOW SHOWING" sign.
- **Two low corridors** with numbered doors (1–12).
- **Twelve auditoriums**:
  - Each has stadium seating rising away from the screen, aisle steps, dim step lights, exit signs over the doors and a projector port in the back wall. The projector port throws a dusty beam and still rattles.
  - Every hall is one of five kinds:
    - an empty screen glowing faintly;
    - a screen of static;
    - a completely dark hall;
    - a wrecked hall with a torn screen and broken seats;
    - a hall where a thin figure stands on the screen.
- **Loot:**
  - the box office till (coins, Ticket Stubs);
  - the concession stock (snacks, Almond Water);
  - lost-and-found chests at the back of some halls.
- **New blocks and item:** 32 new blocks, all drawn for the mod, and a new item, the Ticket Stub.
- `/officerooms goto cinema` and `locate` support it. Elevators can arrive there, hazard zones can happen there, and there is a discovery milestone.
- No audio hint (as agreed for new rooms) and no own mobs yet.
