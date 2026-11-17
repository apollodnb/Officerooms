# New in Office Rooms 0.17.0

## 1. HAZARD ZONE (recurring world event)

### Cycle

Every 100 minutes a toxic zone appears in a random megastructure near the players (Poolrooms, Neighbourhood, Maze, Hotel, Industrial Complex or Level Fun).

| Time | What happens |
|---|---|
| T − 15 min | Monitors show the warning with a countdown. The detector starts working. |
| T | The zone activates. |
| T + 45 min | The zone disappears. |
| 40 min | Cooldown. Events never overlap. |

- The first warning comes 20 minutes after the mod is first loaded in a world.
- The schedule is stored with the world, survives restarts and is synchronised to every player.
- All times are in the config section `[hazard_zone]`.

### Monitors (no chat messages)

- **Where they stand:** Hazard Monitors stand in some offices (about one in seven) and corridors, Security Offices, labs, secure storage rooms, industrial control rooms and the hotel reception.
- **What they show:**
  - Normally: "FACILITY STATUS NOMINAL" in green.
  - During the warning: "WARNING – TOXIC CONTAMINATION DETECTED – HAZARD ZONE IN mm:ss", blinking red and yellow.
  - While the zone is active: "HAZARD ZONE ACTIVE", the place, "GAS MASK REQUIRED" and the time left.
- They flicker now and then and beep while an event is running.
- About one in six monitors is broken: switched off or showing corrupted data.
- You can craft one for your base (iron, glass pane, Electronic Component).

### The zone

- **Size and look:** about 90 × 90 blocks across the megastructure's full height. Inside it hangs a green haze of drifting gas, also visible from outside, with dense green fog while you are inside and the occasional hiss of gas.
- **Loot:** 8 Hazard chests in random places. Chests near the centre use the better `hazard_core` table: Research Files 90 %, Toxic Crystals, Electronic Components, Glitched Ingots, Totem, L3 card, Master Key, diamonds. The outer ones use `hazard_cache`.
- **Creatures:** Flayed, Colony, Headless Wardens and Hounds appear around players inside the zone.
- **When it ends:** gas, chests and event creatures are removed.

## 2. Equipment

- **Gas Mask** (head slot):
  - Protects completely while its filter lasts: 10 minutes inside the gas.
  - Replace the filter with a **Gas Filter** (right-click; crafted from iron nugget, charcoal and paper, gives 2).
  - Rare loot in offices, labs, security offices, storage rooms and industrial rooms.
- **Without a mask:**
  - Exposure builds up (shown in the action bar).
  - From 20 % nausea, from 40 % weakness and rising damage, which ends in death.
  - Outside the zone the exposure drops again.
  - Death or a dimension change resets it.
- **Night Vision Goggles** (head slot):
  - Night vision for 20 minutes per charge.
  - Recharge them by right-clicking a Portable Battery while wearing them.
  - Recipe: iron ingot, Electronic Component, iron ingot / glass pane, leather, glass pane.
- **Hazard Visor:**
  - Gas mask and night vision in one.
  - Crafted from Gas Mask + Night Vision Goggles + **Toxic Crystal**. The Toxic Crystal only drops in Hazard Zones.
- **Special Event Detector** (rare loot):
  - While held it shows an arrow towards the announced or active zone, the countdown, the place, the horizontal distance and how far above or below you it is.

## 3. Sound

- The lamp hum is louder (0.85 instead of 0.35).
- The music is much quieter (20 %).
- Both can be set in the client config (`lampHumLevel`, `musicLevel`).

## 4. Commands (operators)

`/officerooms hazard status | warn | start | stop`
