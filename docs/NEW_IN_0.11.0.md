# New in Office Rooms 0.11.0

## 1. Creature overhaul (GeckoLib)

GeckoLib **4.4 or newer** is now a required dependency (tested with `geckolib-forge-1.20.1-4.8.4`). It is **not** bundled; install it next to the mod.

Seven creatures have new animated models. Their behaviour, sounds and spawn rules are unchanged:

| Creature | New look | Animations |
|---|---|---|
| Gaunt | Arthropod centaur: segmented lower body with stinger, 6 legs, upright torso with clawed arms, red glowing eyes | idle, walk (tripod gait), run, scream |
| Headless Warden | Hunched sculk body, rib cage with a cyan glowing heart, glowing neck stump, tendrils | idle, walk, run, attack, scream |
| Hound | Hairless dog with a human head | idle, walk, run, bark, attack |
| Smiler | No body. A black, smoky shroud around a face with glowing white eyes and grin | idle, walk, run, giggle, attack |
| Colony | Fused mass with four heads and eight limbs | idle, walk, run, scream |
| Hunter | Wireframe figure made of thin rods, tiny glowing eyes, always full-bright | idle, walk, run, notice |
| Observer | Tall, thin, dark figure with faint eyes and sudden head snaps | idle, walk, run |

The **Wolfspider** keeps its original look.

Glowing parts use a full-bright layer, so they also glow in the dark office dimension, which has no sky light.

## 2. Hidden rooms

Small 7 × 6 rooms are built into larger ordinary office rooms. There are about 0.5 per region, spread over all storeys. They are never placed in megastructures, elevators or secure storage rooms.

| Room | Entrance | Contents |
|---|---|---|
| Maintenance room | Loose panel (looks like the wall) | Pipes, chains, dead fuse box, anvil, chest and barrel |
| Supply closet | Loose panel | A row of barrels on shelves, chest; good supplies |
| Forgotten break room | Spruce door (sometimes ajar) | Table and chairs, fridge, coffee machine, dead plant, candle, "HAPPY BIRTHDAY DAVE" sign |
| Abandoned security office | Iron door, left open | Desk with a wall of dead monitors, camera, chest with a chance of access cards |
| Research lab | Security door, level 1 (60%) or level 2 (40%), with a warning sign | Lab bench, brewing stand, cauldron, sculk specimen tank, cage with a skull, slime |
| Concealed room | Loose panel | Walls covered in chalk crosses, a chair facing them, stains, chest with rare loot |

- Each furnished room has a lectern with one of 11 English notes.
- About 15% of rooms are empty, holding only dust and cobwebs. Concealed rooms are never empty.
- Seven new loot tables (`chests/hideout_*`):
  - Security office: level 1 card 25%, level 2 card 10%.
  - Concealed room: Totem of Mercy, Master Key, Temporal Watch, level 3 card.
- **Loose Panel**: a new block that looks like the quartz office wall but breaks quickly by hand or with an axe.
- `/officerooms locate hideout` lists the nearest hidden rooms, by type, at their entrance.
