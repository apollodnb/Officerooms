# Office Rooms 0.8.4 (beta) – /officerooms locate

- New command `/officerooms locate <type>` lists the nearest places of a type with coordinates, storey and distance, without teleporting (5 nearest plus how many more are in the search area).
- `/officerooms locate all` shows the nearest one of every type.
- Types: `poolrooms`, `neighbourhood`, `maze`, `maze_vault`, `villager_outpost`, `lush_cave`, `water_office`, `sculk_office`, `abandoned_camp`, `oasis`, `exit_room`. Megastructures are searched within 30 regions (12 for `all`), special rooms within ±2 regions on all storeys.
- Works from the console too: `execute in office_rooms:office_rooms positioned <x> <y> <z> run officerooms locate all`.

Tested: server, seed 4711, `locate all` and `locate maze` from 0 300 0 – all 11 types found, no exceptions.
