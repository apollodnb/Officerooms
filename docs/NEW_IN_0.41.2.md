# New in Office Rooms 0.41.2 (beta)

- **Fix: `[spawning] enabled = false` now really turns creature spawns off** (reported by players). Before, it only stopped the
  regular spawn cycle; these still spawned creatures and now follow the switch (and Peaceful):
  - residents of the Neighbourhood and the Headless Warden in sculk offices (placed when chunks generate),
  - visitors waiting in front of elevator doors,
  - Observers,
  - Gaunts during blackouts and creatures in hazard zones.
- Creatures that already exist stay in the world. Story encounters (the Flayed in Level 0, the boss of the Underside) are not affected.
