# New in Office Rooms 0.41.1 (beta)

- **A bit brighter:** the Office Rooms are now 20 % darker than vanilla light instead of 30 %. Dark storeys, blackouts and night vision work as before.
- **Aranea Membri** lurks in cinema ceilings more often: chance per spawn check 0.04 → 0.12, and one per cinema floor instead of one per cinema.
  Configs that still have the old default are updated automatically (`balanceVersion` 6).
- **Commands:** `/officerooms locate` now also knows the curiosities: `cold_storage`, `last_meeting`, `mail_room`, `rain_room`, `canteen`, `arcade`
  (`goto` already had them).
- **Faster search for special rooms** in `locate` and `goto` (villager outpost, lush cave, oasis, exit room, curiosities …): only the nearest
  candidates are furnished. Before, one search could freeze the server for up to 20 seconds; now it takes 1–4 seconds.
