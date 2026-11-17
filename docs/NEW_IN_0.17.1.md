# New in Office Rooms 0.17.1

- **Hazard Zone safety:** a zone is never placed in a megastructure where a player is standing at that moment. "In it" means the same region, from 12 blocks below to 12 blocks above it. This way nobody is gassed out of nowhere; there is always the 15-minute warning first.
- Test: the player stood in the Maze and a warning was started six times. The Maze was never chosen (picks: three hotels, one Industrial Complex, two Neighbourhoods). All 20 unit tests passed.
