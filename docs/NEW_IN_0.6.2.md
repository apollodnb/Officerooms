# Office Rooms 0.6.2 – Spawn balance

Works with 0.6.1 worlds (no new world needed).

## More Hounds and Smilers

| | before | now |
|---|---|---|
| Hound: spawn chance per check (every 3 s) | 0.02 | **0.15** |
| Hound: at most near one player | 1 | **2** |
| Hound: one per … rooms | 160 | **60** |
| Smiler: spawn chance per check | 0.025 | **0.15** |
| Smiler: at most near one player | 1 | **2** |
| Smiler: one per … rooms | 140 | **60** |
| Smiler: max. block light at the spawn spot | 3 | **7** (it still fades away in light 10+) |

## Gaunts rule the dark exit levels (lowest 5 storeys)

| | before | now |
|---|---|---|
| One Gaunt per … rooms | 250 | **30** |
| At most near one player | 1 | **6** |
| Senses players through walls | no | **within 24 blocks** (`senseRange`) |
| Scream calls the pack | no | **every Gaunt within 40 blocks joins the hunt** (`alertRadius`) |
| Keeps chasing up to | 48 blocks | **64 blocks** (`followRange`) |

Speed and damage are unchanged. Gaunts still only appear when you are in the five dark bottom storeys.

## Roaming Headless Warden less constant

A roaming Headless Warden used to be almost always near you (its limit was refilled every 3 seconds). It now has its own spawn chance (`wardenSpawnChance` = 0.01, roughly every 5 minutes). The guardians locked in Sculk Offices are unchanged.

## Your existing config is updated once

Old `office_rooms-common.toml` files kept their old spawn values. 0.6.2 writes the new values into the file once (marker `balanceVersion = 2` in `[spawning]`). Afterwards your own changes stay. Set `balanceVersion = 0` to reset all spawn values to the defaults again.

## Fix

The Hound's `spawnChance` was in the `[gaunt]` section of the config by mistake (0.6.0/0.6.1). It is now in `[hound]`.
