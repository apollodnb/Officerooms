# Test report 0.29.0

- Build and all unit tests green.
  - New test `curiositiesInNewWorlds`: in 7 × 7 regions every new room appears (cold storage 2699, mail room 2138, last meeting 2018, rain room 1582, canteen 754, arcade 520).
  - Old worlds get none of them (checked in the test).
- Fresh world: `/officerooms goto <type>` found all six rooms. Shots **with night vision** (otherwise too dark), partly from awkward camera positions:
  - arcade: carpet and cabinets;
  - rain room: falling water;
  - last meeting: table, chairs, chest;
  - mail room: barrels with parcels;
  - cold storage: ice floor;
  - canteen: vending machine and counter.
- Loot, 10 rolls of `gameplay/vending`: 10 items (food, tools, supplies, one Research File by chance).
- Coins: 40 rolls of `chests/standard_office` gave coins.

**Not tested:**
- using a vending machine in game with a coin (only the loot table);
- the quest chapter in game;
- milestone detection when walking into a room;
- the rain sound.
