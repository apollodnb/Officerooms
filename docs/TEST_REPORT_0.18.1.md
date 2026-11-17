# Test report 0.18.1

- Build OK; unit tests pass (20 of 20).
- Production server and client: each of the 7 new `goto` targets was run twice, starting from different positions.
  - Every run reported the target and teleported the player.
  - In the second run every landing spot was checked block by block: solid block below, air at feet and head height.
- Not tested: multiplayer; running the command from the Overworld.
