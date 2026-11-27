# New in Office Rooms 0.43.0 (beta): the Dead Mall

**A new megastructure in the Office Rooms: a three-floor shopping centre that nobody has shopped in for a very long time.**

## The mall
- As rare as the other megastructures; worlds created with 0.43.0 or later only (see below).
- Two concourses cross in the middle. Above them the floors are open: atriums under grey skylights, galleries with
  glass railings and columns, a dry fountain with coins under the crossing.
- **Escalators** (not running) climb across the east–west atrium: floor 0 → 1 and floor 1 → 2.
- **Shops** line the concourses on every floor: shuttered, papered over "for lease", dark, or still stocked —
  clothes (with mannequins), shoes, televisions showing static, books, toys, pets (murky aquariums), a hair salon.
  Every shop has a stockroom with a back door.
- **Food court** under the north atrium: stalls with counters, fryers and glowing menu boards, rows of tables.
- **Service corridors** behind the shops (low, cinder block, flickering caged lights), reached through STAFF ONLY doors.
  They lead to the mall office, the security room and four **closed department stores** that have no entrance from the mall.
- The main entrance doors at the south end. It is night outside. It has been night for some time.
- Ceiling speakers play a few bars of tired music now and then, or the chime of an announcement nobody makes.
- **Aranea Membri** nests above the ceiling tiles (at most one per mall floor; config `[aranea_membri] mallChance`).
- About 57 new blocks, all drawn for the mod. Food court tables and chairs, kitchens, desks and chairs come from
  **Refurbished Furniture** and **OfficeCraft** when installed, own blocks or vanilla otherwise.
- Loot: tills, stockrooms, electronics, toys, food stalls, kiosks, the mall office (with a memo to all tenants),
  security room, department stores.

## Other
- `/officerooms locate mall`, `/officerooms goto mall`, `/locate structure office_rooms:dead_mall`.
- Quests (chapter V, optional): "The Dead Mall" and "Closed for Inventory". Milestones `discover_mall`, `mall_dept`.
- **Fix: new content stays in new worlds.** The content version of a world is now stored with the world when it is
  created. Before, the version came from the dimension file of the mod every time a world loaded, so an update could
  switch new content on in old worlds. Worlds from before 0.43.0 keep content version 2 (departure gates, no mall).
