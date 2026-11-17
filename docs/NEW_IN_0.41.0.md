# New in Office Rooms 0.41.0 (beta)

**Megastructures work with `/locate` and structure-finding mods.**

- The seven megastructures of the Office Rooms are now known to Minecraft's structure system, so the vanilla
  `/locate structure` command and mods such as **Explorer's Compass** can find them:
  - `office_rooms:poolrooms`, `office_rooms:neighbourhood`, `office_rooms:the_maze`, `office_rooms:abandoned_hotel`,
    `office_rooms:industrial_complex`, `office_rooms:level_fun`, `office_rooms:abandoned_cinema`
  - The shrine in the Overworld (`office_rooms:abandoned_office_shrine`) already worked.
  - Search inside the Office Rooms dimension. The result points to the centre of the 160-block region of the
    megastructure; the height is up to you (they sit on different storeys) - `/officerooms locate` gives the exact
    entrance with height.
  - These markers are invisible and place no blocks. No other structures are added to the Office Rooms.
  - In worlds from before 0.41.0, areas that were already explored are not found this way (their chunks carry no
    markers); `/officerooms locate` still finds everything.
- Research outposts are placed when a chunk is generated (they need the terrain around them), so they cannot be
  predicted by `/locate`. New: `/officerooms locate outpost` lists all outposts generated so far, in all dimensions.
- New: `/officerooms locate shrine` (same search as the vanilla command, from anywhere).
- Fix: `/officerooms locate all` in a far, unexplored area could freeze the server for more than 60 seconds and make
  it shut down (it planned hundreds of regions at once). It now searches a smaller area for the small special rooms.
