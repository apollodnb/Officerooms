# Möbel, Licht, Fenster und Loot (Phase 2)

> **Stand 0.3.0.** Seit 0.4.0 geändert (siehe [PHASE4.md](PHASE4.md)): Deckenleuchten sind eigene Blöcke `office_rooms:ceiling_panel` / `ceiling_strip` statt OfficeCraft-Leuchten; Loot hat fünf Qualitätsstufen mit zentralen Chancen in `office_rooms-common.toml`; der Vanilla-Ersatz nutzt Spender statt Fässer und Steintreppen statt Birkentreppen; die untersten fünf Ebenen sind unbeleuchtet.

## 1. Möbelabhängigkeiten

Beide Möbel-Mods sind optional (`mandatory=false` in `mods.toml`). Office Rooms hat keine Kompilierabhängigkeit auf sie. Blöcke werden zur Laufzeit über `ForgeRegistries.BLOCKS` per Registry-ID geholt. OfficeCraft-Mehrblockmöbel werden über Reflexion an die öffentliche Katalog-API von OfficeCraft (`Catalog.get(id).cells(...)`, `PartEntity.configure(...)`) übergeben, damit die Teilblöcke wie beim Setzen durch einen Spieler entstehen.

| Mod | Mod-ID | Getestet | Pflicht? |
|---|---|---|---|
| OfficeCraft | `gamefantom_officecraft` | 1.0.3 | nein |
| Refurbished Furniture | `refurbished_furniture` | 1.0.20 (+ Framework) | nein |

Fehlt ein Block (anderer Versionsstand, Mod fehlt), wird einmalig eine Warnung geloggt und der Vanilla-Ersatz gesetzt.

## 2. Registry-ID-Liste

| Möbel (intern) | Größe B×T×H | OfficeCraft / Refurbished | Vanilla-Ersatz |
|---|---|---|---|
| Schreibtisch `DESK` | 3×1×2 | `gamefantom_officecraft:office_desk` | glatte Steinstufen (oben) |
| Cubicle-Arbeitsplatz `WORKSTATION` | 3×3×2 | `gamefantom_officecraft:cubicle_workstation` | Stufen in Tischform |
| Trennwand `PARTITION` | 3×1×2 | `gamefantom_officecraft:cubicle_partitions` | – (entfällt) |
| Bürostuhl `CHAIR` | 1×1×2 | `gamefantom_officecraft:chair_gray` / `chair_black` | glatte Quarztreppe |
| Konferenztisch groß / mittel | 7×3 / 5×3 | `gamefantom_officecraft:conference_large` / `conference_medium` | Stufen |
| Präsentationstafel `BOARD` | 3×1×2 | `gamefantom_officecraft:presentation_board` | – |
| Wasserspender | 1×1×2 | `gamefantom_officecraft:water_cooler` | Kessel mit Wasser |
| Automat | 1×1×2 | `gamefantom_officecraft:vending_machine` | – |
| Drucker | 1×1 | `gamefantom_officecraft:printer` | Werfer |
| Kaffeemaschine | 1×1 | `gamefantom_officecraft:coffee_machine` | – |
| Deckenleuchte / LED-Leiste | 1×1 / 3×1 | `gamefantom_officecraft:ceiling_light` / `ceiling_led_bar` + unsichtbarer `minecraft:light[level=15]` | Seelaterne bzw. ausgeschaltete Redstone-Lampe |
| Schublade `DRAWER` (Container) | 1×1 | `refurbished_furniture:white_kitchen_drawer` / `light_gray_kitchen_drawer` | Fass |
| Schubladenstapel `DRAWER_STACK` (Container) | 1×1×2 | Schublade + `white_/light_gray_kitchen_storage_cabinet` | 2 Fässer |
| Küchenzeile `COUNTER` | 1×1 | `refurbished_furniture:white_kitchen_cabinetry` | glatter Stein |
| Spüle | 1×1 | `refurbished_furniture:white_kitchen_sink` | Kessel |
| Kühlschrank (Container) | 1×1×2 | `refurbished_furniture:light_fridge` + `light_freezer` | 2 Eisenblöcke (kein Container) |
| Mikrowelle | 1×1 | `refurbished_furniture:light_microwave` | Räucherofen |
| Tisch / Stuhl (Pausenraum) | 1×1 | `refurbished_furniture:birch_table` / `birch_chair` | Stufe / Birkentreppe |
| Serverschrank `RACK` | 1×1×2 | – (Vanilla) | 2 Beobachter |

Die „Clean Drawers“ aus dem Auftrag sind die Refurbished-Küchenschubladen und -schränke (Macaw's Furniture lag nicht vor).

## 3. Einrichtungsvarianten

Der Raumtyp wird pro Raum deterministisch aus dem Weltseed gewählt, nach Raumgröße (klein ≤ 9 Zellen, mittel ≤ 30, groß darüber). Gewichte in `furnishing.small_rooms / medium_rooms / large_rooms`.

| Typ | Einrichtung |
|---|---|
| `empty` | nur Licht |
| `standard_office` | 1–2 Schreibtische mit Stuhl an der Wand (der zweite bevorzugt gegenüber), teils mit Schublade, gelegentlich ein Drucker |
| `open_office` | Cubicle-Raster, Schreibtischreihen oder Rücken-an-Rücken-Gruppen mit Stühlen, dazu Drucker, Wasserspender, Automat, Schubladen |
| `meeting_room` | großer oder mittlerer Konferenztisch mit Stühlen, Tafel; in kleinen Räumen ein kleiner Tisch |
| `break_room` | Küchenzeile mit Spüle, Mikrowelle und Kaffeemaschine, Kühlschrank, Tisch mit Stühlen, teils Automat und Wasserspender |
| `server_room` | Reihen von Serverschränken, immer dunkel oder gedimmt |
| `archive` | Schubladenstapel an den Wänden, optional eine Mittelreihe |
| Korridor | gelegentlich ein Wasserspender (`water_cooler_chance` 0,12) |

### Platzierungsregeln

- Möbel stehen nur auf Innenfeldern des Raums, nie außerhalb.
- Vor jeder Tür bleiben 2 Felder frei, vor Fenstern 1 Feld.
- Möbel überlappen sich nicht; jedes Möbel liegt vollständig im Chunk seines Hauptblocks.
- Nach jeder Platzierung prüft eine Breitensuche, ob alle Türen des Raums weiter verbunden sind. Sonst wird die Platzierung zurückgenommen.
- Möbel werden nur in Luft oder Teppich gesetzt. Unter Möbeln liegt grauer Beton statt Teppich.

### Lazy Placement

Die Weltgenerierung setzt nur die Lichtblöcke. Möbel werden erst in Spielernähe gesetzt (Radius 3 Chunks, ±3 Geschosse, 10 Einheiten alle 4 Ticks). Grund: Bei 204 Geschossen pro Chunk-Säule wären es sonst über 4000 Block-Entities pro Säule, was in Tests zu Speicherüberlauf führte. Bereits eingerichtete Einheiten merkt sich die Welt (`office_rooms_furnished`), es gibt also keine Doppelplatzierung.

## 4. Beleuchtung

Die Dimension ist sehr dunkel (`ambient_light` 0,07, Nebel und Himmel fast schwarz). Licht kommt nur von Deckenleuchten.

| Variante | Gewicht | Verhalten |
|---|---|---|
| Raum `normal` | 45 | Leuchten im Raster `light_spacing` (7) |
| Raum `dim` | 25 | Raster `dim_light_spacing` (11) |
| Raum `partial` | 20 | Raster 7, etwa 40 % ausgefallen (ausgeschaltete Lampe) |
| Raum `dark` | 10 | höchstens eine Leuchte |
| Korridor `bright` | 40 | jede 2. Zelle eine LED-Leiste in Laufrichtung |
| Korridor `dark` | 60 | jede 3. Zelle, davon 55 % an |
| Atrium | – | mit Chance `atrium_lit_chance` (0,55) beleuchtet: Leuchten an der Decke und unter Galerien |

Mit OfficeCraft ist der Leuchtkörper das OfficeCraft-Deckenpanel, das Licht stammt von einem unsichtbaren Vanilla-Lichtblock. Ohne OfficeCraft wird eine Seelaterne in die Decke gesetzt.

## 5. Fenster und Atrien (Änderungen gegenüber Phase 1)

- Atriendichte erhöht (`atria.density` jetzt 0,075); mehrere getrennte Atrien pro Region (gemessen: 13 in Region 0,0).
- Neu: seltene Fenster zwischen zwei benachbarten Büros (`windows.office_pair_chance` 0,02 pro Raumpaar). Nur zwischen zwei normalen, erreichbaren Räumen mit gleichem Bodengeschoss und nur an Wandstücken ohne Tür, höchstens eins pro Paar. Dadurch blickt kein Fenster ins Leere oder ins Massiv.
- Materialien: Boden grauer Teppich auf Quarz, Wände und Decken weißer Quarz.

## 6. Loot

Einziger Gegenstand: **Pilzsuppe**. Jede Container-Tabelle verweist auf die gemeinsame Tabelle:

```
data/office_rooms/loot_tables/chests/common.json    1 Pilzsuppe mit Chance 0,08
data/office_rooms/loot_tables/chests/<raumtyp>.json  verweist auf common
```

Raumtypen mit eigener Tabelle: `standard_office`, `open_office`, `meeting_room`, `break_room`, `server_room`, `archive`, `corridor`.

- **Zentral ändern:** `common.json` (Chance oder Gegenstand).
- **Pro Raumtyp erweitern:** in `<raumtyp>.json` einen weiteren Pool hinzufügen.

Beides geht per Datapack ohne Neubau. Die Tabelle wird beim ersten Öffnen gewürfelt (Vanilla-Verhalten von `RandomizableContainerBlockEntity`). Container im Vanilla-Ersatz sind Fässer; der Ersatz-Kühlschrank (Eisenblöcke) hat kein Loot.
