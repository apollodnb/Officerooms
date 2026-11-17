# Test report 0.25.0

## Build and tests
- Build OK; unit tests pass.
- Test client: FTB Library 2001.2.13, FTB Teams 2001.3.2, FTB Quests 2001.4.22, Architectury 9.2.14.

## Quest book
- The server log shows "Loaded 2 chapter groups, 10 chapters, 106 quests, 1 reward tables". The 2nd group is FTB's default group.
- The quest book opens and all 10 chapters are listed. Screenshots of chapters I, III and IV checked.
- ID bug found and fixed: FTB parses IDs as signed numbers, so IDs now always have the top bit clear.
- Detection tested:
  - an advancement task: the fuse_box_repaired milestone completed the quest;
  - a kill task: a Gaunt killed by the player completed the quest. (A `/kill` does not count, as expected.)
- Not tested:
  - every single quest in a real playthrough;
  - the item tasks one by one (these are standard FTB Quests);
  - the observation tasks;
  - the reward table drop;
  - repeatable trades.

## Goto / locate
- **Found and teleported:** research_lab, concealed_room, security_office, maintenance_room, shrine (-768 75 112). The other hidden rooms were found in the first pass.
- **Correct message:** fuse_box and hazard_zone without an active event; outpost before any was generated.
- **Not tested:** goto outpost with a naturally generated outpost.

## Blackout
- Started next to a ceiling panel: the panel was lit=false during the blackout and lit=true after stop. Screenshot after power returned.
- Not tested with a shader pack in the test setup.

## Flayed in Level 0
- **Before the fix:** the client entity count showed 0 rendered and the Flayed was invisible right in front of the player.
- **After the fix:** the client knows the entity, and its body and hitbox are drawn (screenshot).

## [Unknown]
- The movement logic was changed. The fight itself was not replayed after the change.
