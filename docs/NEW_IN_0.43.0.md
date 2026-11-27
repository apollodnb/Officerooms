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

## The Underside Key needs nine artifacts now
- **You Are Here** (new): the red pin of a mall directory, put away in a stockroom chest of a department store on the
  top floor of the Dead Mall (one per mall). The memo in the mall office hints at it. Worlds from before 0.43.0 have no
  malls: there one lost-and-found chest of each Abandoned Cinema keeps the pin instead.
- **Captain's Compass** (new): in the captain's chest of every SS Fun =), deck 10, behind the bridge.
- Recipe: the seven megastructure artifacts + the two new ones (shapeless, a full crafting grid).
- Quests: the mall joins chapter V like the other megastructures ("The Dead Mall", artifact quest "You Are Here", plus
  optional "Out of Order" (top floor), "Closed for Inventory" (department store) and "To All Tenants" (the memo)).
  Chapter XI "The SS Fun =)" is no longer optional: the departure gate, boarding and the new artifact quest
  "Captain's Compass" lead to the Underside Key. Okafor's notes now list nine things.

## Other
- `/officerooms locate mall`, `/officerooms goto mall`, `/locate structure office_rooms:dead_mall`.
- Milestones `discover_mall`, `mall_top`, `mall_dept`. Test command `/officerooms ship mascot wave`.
- **Fix: new content stays in new worlds.** The content version of a world is now stored with the world when it is
  created. Before, the version came from the dimension file of the mod every time a world loaded, so an update could
  switch new content on in old worlds. Worlds from before 0.43.0 keep content version 2 (departure gates, no mall).
