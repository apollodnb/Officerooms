# Raumhöhen und Generierungswahrscheinlichkeiten

Alle Werte stehen in `data/office_rooms/dimension/office_rooms.json` unter `generator.settings` und sind dort änderbar. Die hier genannten Zahlen sind die ausgelieferten Standardwerte.

## 1. Raumhöhen (normale Räume)

**Lichte Höhe** bedeutet den freien Raum zwischen Bodenoberkante und Deckenunterkante. Boden und Decke kommen jeweils als 1 Block dazu.

| Band | Lichte Höhe | Gewicht | Ziel | Gemessen (157 898 Räume, 9 Regionen) |
|---|---|---|---|---|
| Standard | exakt 4 | 90 | 90 % | **90,18 %** |
| erhöht | 5–8 (gleichverteilt) | 7 | 7 % | **6,91 %** |
| hoch | 9–16 (gleichverteilt) | 2 | 2 % | **1,94 %** |
| sehr hoch | 17–28 (gleichverteilt) | 1 | 1 % | **0,97 %** |

Die Verteilung gilt für alle Büroräume, vom kleinsten Büro bis zum Großraum. Korridore sind immer 4 Blöcke hoch, Treppenhäuser 9 (zwei Geschosse). Atrien haben eigene Höhen (Abschnitt 4).

```json
"room_heights": [
  { "min": 4,  "max": 4,  "weight": 90 },
  { "min": 5,  "max": 8,  "weight": 7 },
  { "min": 9,  "max": 16, "weight": 2 },
  { "min": 17, "max": 28, "weight": 1 }
]
```

### Wie das Geschossraster damit umgeht

Ein Raum mit lichter Höhe *h* belegt *k* = ⌈(h + 1) / 5⌉ Geschosse und hat damit 5k − 1 Blöcke Platz. Die überzähligen Blöcke werden unter der nächsten Geschossdecke als **abgehängte Decke** (glatter Quarz) gefüllt:

| h | Geschosse | Platz | abgehängte Decke |
|---|---|---|---|
| 4 | 1 | 4 | 0 |
| 6 | 2 | 9 | 3 |
| 9 | 2 | 9 | 0 |
| 13 | 3 | 14 | 1 |
| 24 | 5 | 24 | 0 |

So ist jede ganzzahlige Höhe ab 3 möglich, unabhängig vom Raster.

### Wie die Verteilung exakt bleibt

Ein hoher Raum braucht die Zellen über sich frei. Sind sie belegt (Atrium, Treppe, anderer hoher Raum, Treppenzugang), wird der Raum mit 4 Blöcken gebaut, und die gewürfelte Höhe wird **an den nächsten Raum weitergereicht**. Ohne diesen Ausgleich läge der 4-Block-Anteil spürbar über 90 %.

## 2. Raumgrößen

Größen in Einheiten zu 4 Blöcken. Breite und Tiefe werden zufällig vertauscht. Passt die Wunschgröße nicht, wird der Raum so groß wie möglich gesetzt (mindestens 2 × 2 Einheiten).

| Klasse | Gewicht | Anteil | Einheiten | Blöcke (Raster) |
|---|---|---|---|---|
| small_square | 20 | 20 % | 2–3 × 2–3 | 8–12 × 8–12 |
| small_rect | 18 | 18 % | 2–3 × 3–5 | 8–12 × 12–20 |
| office | 20 | 20 % | 4 × 4 | 16 × 16 |
| office_varied | 14 | 14 % | 3–5 × 3–6 | 12–20 × 12–24 |
| rect_office | 10 | 10 % | 8 × 4 | 32 × 16 |
| open_plan | 8 | 8 % | 8 × 8 | 32 × 32 |
| large_open | 6 | 6 % | 12 × 8 | 48 × 32 |
| huge_open | 3 | 3 % | 16 × 12 | 64 × 48 |
| vast_open | 1 | 1 % | 16–20 × 12–16 | 64–80 × 48–64 |

Die Anteile sind Wahrscheinlichkeiten pro Raum-*Versuch*. Große Klassen werden häufiger durch Platzmangel verkleinert, deshalb liegen sie in der fertigen Welt etwas darunter.

Weitere Wahrscheinlichkeiten:

| Parameter | Wert | Bedeutung |
|---|---|---|
| `composite_chance` | 0,15 | Raum bekommt einen angesetzten Flügel (L-/T-Form) |
| Restzellen | – | werden an einstöckige Nachbarräume angehängt, sonst massiv |

## 3. Korridore

| Parameter | Wert |
|---|---|
| `seeds_min` / `seeds_max` | 1 / 3 zusätzliche Startpunkte pro Geschoss (plus alle Treppenzugänge) |
| `max_share` | höchstens 30 % der Geschossfläche |
| `wide_chance` | 20 % der Segmente 7 Blöcke breit (sonst 3) |
| `segment_min` / `segment_max` | 3–10 Einheiten (12–40 Blöcke) |
| `long_chance` / `long_max` | 12 % lange Segmente bis 24 Einheiten (96 Blöcke) |
| `turn_chance` | 45 % Abbiegen am Segmentende (L) |
| `branch_chance` | 25 % Verzweigung am Segmentende (T), davon 30 % mit Geradeausast (Kreuzung) |
| `side_branch_chance` | 5 % pro Schritt seitlicher Abzweig (T im Verlauf) |
| Rest | 12 % geradeaus weiter, sonst Ende |

## 4. Atrien

| Klasse | Gewicht | Grundfläche (Einheiten) | Grundfläche (Blöcke) | Lichte Höhe |
|---|---|---|---|---|
| small | 30 | 3–5 × 3–5 | 12–20 | 12–20 |
| medium | 25 | 7–9 × 7–9 | 28–36 | 20–40 |
| large | 15 | 11–13 × 11–13 | 44–52 | 40–80 |
| shaft (schmal, hoch) | 12 | 2–3 × 3–6 | 8–12 × 12–24 | 40–120 |
| hall (breit) | 8 | 12–20 × 8–12 | 48–80 × 32–48 | 20–40 |
| huge | 7 | 15–17 × 15–17 | 60–68 | 80–160 |
| mega | 3 | 24–30 × 24–30 | 96–120 | 160–300 |

`density` = 0,06 bedeutet 0,06 Platzierungsversuche pro Geschoss und Region, also 12 Versuche pro 160 × 160-Säule. Überlappende Versuche entfallen. Gemessen über 16 Regionen: **164 Atrien**, im Mittel etwa 10 pro Regionssäule bzw. eins pro 20 Geschosse. Klassen: small 56, medium 38, large 27, shaft 22, hall 12, huge 7, mega 2. Höchstes Atrium: 214 Blöcke.

Galerien: In Abständen von 2–4 Geschossen bekommt ein Atrium mit 60 % Wahrscheinlichkeit eine Galerie (nur ab 5 × 5 Einheiten). Brücken kommen mit 45 % dazu. Details in [FENSTER_UND_ATRIEN.md](FENSTER_UND_ATRIEN.md).

## 5. Türen und Fenster

| Parameter | Wert |
|---|---|
| Türtyp normal / doppelt / breit / offen | 45 / 25 / 15 / 15 |
| `extra_chance` | 25 % Grundwahrscheinlichkeit für Zusatzverbindungen (Schleifen), je nach Raumart ×0,6 bis ×2,5 |
| `second_opening_chance` | 15 % zweite Öffnung in derselben Wand |
| `boundary_chance` / `boundary_min_per_storey` | 12 % der Raumpaare an einer Regionsgrenze, mindestens 2 pro Geschoss |
| `windows.room_chance` | 60 % der Räume an einem Atrium bekommen Fenster |
| `windows.max_per_room` | 1–2 Fenster pro Raum |
| `stairs_per_storey` | 3 Treppenhäuser pro Geschoss und Region |

## 6. Ergebnis einer typischen Region (Messung im Spiel, `/officerooms stats`)

Region 0,0, Testserver: 204 Geschosse, 17 710 Räume, 1 476 Korridornetze, 609 Treppenhäuser, 10 Atrien, 8 versiegelte Räume. Raumhöhen 90,2 / 7,0 / 1,9 / 0,9 %. 26 092 Türen und Durchgänge, davon 947 an Regionsgrenzen, 764 Fenster.
