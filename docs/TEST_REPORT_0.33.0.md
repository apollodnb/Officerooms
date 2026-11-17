# Test report 0.33.0

## Done
- Build and all unit tests: passed, including the Maze tests (connectivity, 3–5 exits).
  - The two-storey Maze needed a fallback: one site in 211 had no neighbouring room for an exit. Rooms that reach the storey from below now count too, and if there is still no room, three plain openings are cut.
- In game on a fresh world (Level 0 at brightness 0 is lit; the Maze photos are at brightness 0, plus two with night vision):
  - Level 0: a carpeted ramp up through the ceiling; five-block-high rooms; the new wallpaper, carpet and ceiling; a stain; worn wallpaper.
  - The Maze: same palette; pillars; stained carpet; lights in the ceiling; holes in the ceiling tiles; a staircase into the ceiling.

## Not tested
- Every individual new feature in game: holes, light switch, sealed closet, windows, split level, soaked carpet sound and splash.
- Whether a ramp can be walked all the way up (geometry checked in the photo; not walked).
- Whether the Flayed in Level 0 follows you up a ramp.
