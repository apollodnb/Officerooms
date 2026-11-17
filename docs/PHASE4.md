# Phase 4 (0.4.0): Spezialräume, Items, Mechaniken

## Installation (kurz)

1. Forge 1.20.1-47.4.28 installieren (Java 17).
2. `OfficeRooms-0.4.0.jar` in `mods/` legen, optional OfficeCraft 1.0.3 und Refurbished 1.0.20 (+ Framework) dazu.
3. Neue Welt anlegen. In alten Welten bleiben die bereits generierten Chunks, wie sie sind; Spezialräume entstehen nur in neuen Chunks.
4. `/officerooms enter`, Spezialräume finden mit `/officerooms goto villager_outpost | lush_cave | water_office | sculk_office`.

## Neue Räume

| Raum | Häufigkeit (Standard) | Was drin ist |
|---|---|---|
| **Villager Outpost** | ~1 pro 330 Büros (Räume ab 24 Zellen, 1 Geschoss) | 3–6 Dorfbewohner mit verschiedenen Berufen (immer ein Bibliothekar, ein Bauer, wenn Platz für das Feld ist), jedes mit eigenem Bett und passendem Arbeitsblock; Feld mit Wasser und Feldfrüchten, Glocke, Eisengolem. Alle Durchgänge bis auf einen sind zugemauert, im letzten steht eine **Eisentür mit Knöpfen innen und außen**. Dorfbewohner können sie nicht öffnen. |
| **Lush Cave Office** | ~1 pro 560 Büros (nur hohe Räume ab 9 Blöcken lichter Höhe) | Moosboden und -wände, Moosteppiche, Gras, Farne, Azaleen, Leuchtbeeren-Ranken, Sporenblüten, Ranken, kleine Azaleenbäume (Eichenstämme), überwucherte Schreibtische, eine Truhe mit Lush-Loot (u. a. Holz). Licht nur von den Leuchtbeeren. |
| **Water Office** | ~1 pro 310 Büros (1 Geschoss, 6–40 Zellen) | Normales Büro (Schreibtische, Schubladen) vollständig unter Wasser, kein Teppich. Durchgänge zugemauert bis auf eine **Eisentür mit Hebel außen**. Wasser kann durch Türen nicht fließen. Leuchten stehen wassergeflutet. |
| **Sculk Office** | ~1 pro 2 000 Büros | Sculk auf Boden, Wänden und Decke, Sculk-Adern, Sensoren, ein Katalysator, verlassene Schreibtische, völlig dunkel. **Genau ein Headless Warden** (dauerhaft, verschwindet nicht) und eine **Spezialtruhe** mit dem besten Loot. Keine Sculk-Kreischer, also kein Vanilla-Warden. |

Holz: Bäume (Eichenstämme, Azaleenlaub) werden ausschließlich in Lush Cave Offices generiert. Holz-Loot gibt es nur in der Lush-Loot-Tabelle. Der Vanilla-Ersatz für Möbel nutzt kein Holz mehr (Spender statt Fässer, Steintreppen statt Birkentreppen).

**Unterste fünf Ebenen** (Geschoss 0–4, Y −63 bis −39): keine Deckenleuchten in Räumen, Korridoren und Atrien. Spezialräume entstehen dort weiterhin; Leuchtbeeren in Lush-Räumen bleiben als deren Eigenschaft erhalten.

Alle Raten und die Zahl dunkler Ebenen stehen im Datapack unter `generator.settings.special_rooms` (`data/office_rooms/dimension/office_rooms.json`).

## Kreaturen

- **Wolfspider:** jetzt im Schnitt 1 pro **40** Räume (`wolfspiderRoomsPerMob`), zufällig verteilt.
- **Friendly Wolfspider** (`friendlyWolfspiderChance`, 5 % aller gespawnten Wolfspider):
  - Aussehen: schwarz mit Wolfskopf, **weiße, leuchtende Augen**.
  - Verhalten: greift nie Spieler an, kein Wither, normale Wolfsgeräusche.
  - Zähmen: mit **Pilzsuppe** (Rechtsklick, Chance 1/3 je Suppe, die Schüssel kommt zurück).
  - Gezähmt: folgt dem Besitzer, Rechtsklick lässt sie sitzen oder aufstehen, sie verteidigt den Besitzer und verschwindet nicht.
- **Headless Warden:** neues, kopfloses Modell mit fünf Armen und vier Beinen, schief und asymmetrisch, dunkel eingefärbt, mit ruckartigen Glitch-Bewegungen. KI, Gehör, Tempo und Schaden sind unverändert.
  - Beim Tod: **genau ein Wurf mit 50 %** (`glitchedIngotChance`) auf einen Glitched Ingot.

## Items und Crafting

| Item | Wirkung / Herkunft |
|---|---|
| **Mandelwasser** | Getränk (Trinkanimation), füllt die Hungerleiste ganz (20), gibt die Glasflasche zurück, keine Effekte. Häufiger Loot. |
| **Glitched Ingot** | 50 % Drop des Headless Warden, extrem selten in Spezialraum-Truhen. |
| **Multidimensional Rod** | sehr selten, nur in Spezialraum-Truhen (Stufe „special“). |

Rezepte (I = Glitched Ingot, R = Multidimensional Rod, E = Eisenbarren):

```
Schwert    Spitzhacke   Axt      Schaufel   Hacke
 .I.        III          II.      .I.        II.
 .I.        .R.          IR.      .R.        .R.
 .R.        .E.          .E.      .E.        .E.

Helm       Brustpanzer  Beinschutz  Stiefel
 IRI        I.I          IRI         I.I
 I.I        IRI          I.I         IRI
            III          I.I
```

Werte (im Spiel mit `/attribute` gemessen):

| | Netherite | Multidimensional |
|---|---|---|
| Schwert | 8 Schaden | **9,6** |
| Spitzhacke / Axt / Schaufel | 6 / 10 / 6,5 | **7,6 / 12 / 7,8** |
| Rüstung gesamt | 20 | **24** |
| Härte gesamt | 12 | **14,4** |
| Rückstoßresistenz | 0,4 | **0,48** |
| Haltbarkeit Werkzeug | 2031 | **2437** |
| Abbautempo | 9 | **10,8** |
| Verzauberbarkeit | 15 | **18** |

Die Teile sind normal verzauberbar und amboss-tauglich; repariert werden sie mit Glitched Ingots. Feuerfest wie Netherite.

## Loot

Inhalt der Stufen: `data/office_rooms/loot_tables/tiers/`

| Stufe | Inhalt |
|---|---|
| common | Mandelwasser, Eisenbarren, Brot, Wolle, Pilzsuppe |
| uncommon | Diamanten, Netheritplatten, Netherit-Schmiedevorlage, Schmiedetisch, Amboss |
| rare | Netheritbarren, verzauberte Bücher, verzauberte Diamantwerkzeuge |
| special | verzaubertes Netherit-Schwert, -Spitzhacke, -Rüstungsteile, hochwertige Bücher (Stufe 30–39 inkl. Schatzverzauberungen), verzauberter goldener Apfel, Multidimensional Rod, Glitched Ingot (sehr selten) |
| multidimensional | fertige Multidimensional-Werkzeuge und -Rüstungsteile (nur Sculk-Spezialtruhe) |
| wood | Eichenstämme, Setzlinge, Azaleen, Stöcke (nur Lush) |

**Zentrale Wahrscheinlichkeiten:** `config/office_rooms-common.toml`, Abschnitt `[loot]`. Pro Raumtyp eine Liste mit der Chance je Stufe in dieser Reihenfolge: `[common, uncommon, rare, special, multidimensional, wood]`. Beispiel:

```
standard_office = [0.25, 0.04, 0.008, 0.0, 0.0, 0.0]
```

Normale Räume haben `special` und `multidimensional` auf 0. Die besten Gegenstände gibt es nur in Spezialräumen, `multidimensional` nur in der Sculk-Spezialtruhe.

Loot wird wie in Vanilla beim ersten Öffnen eines Containers einmal gewürfelt und dann entfernt.

## Performance

- **Deckenleuchten sind eigene, flache Blöcke**, keine OfficeCraft-Möbel mehr. OfficeCraft zeichnet jedes Möbelstück in jedem Bild einzeln (mit eigenem Zeichenaufruf). Leuchten waren in der Planung 27 % aller OfficeCraft-Objekte (251 229 von 940 187 in 4 Regionen). Jetzt werden sie mit dem Chunk gerendert, ohne Kosten pro Bild.
- Im Großraumbüro bleiben ~20 % der Arbeitsplätze leer. Das sind weniger Möbel-Objekte, und es wirkt verlassener.
- Server:
  - Die Einrichtung in Spielernähe prüft nichts mehr, solange der Spieler im selben Chunk und Geschoss bleibt.
  - Die Geräusch-KI steigt bei Ereignissen außerhalb der Dimension sofort aus.
  - Die Raumliste für Spawns wird einmal statt zweimal pro Spieler berechnet.
- Generierung:
  - Raum-Rechtecke werden einmal pro Region berechnet, statt für jeden Raum die ganze Regionsebene abzusuchen.
  - Spezialräume sind pro Region vorab gesammelt.
  - Gemessen in schon geplanten Regionen: ca. 15 ms Füllen + 9 ms Licht + 7,5 ms Spezialräume pro Chunk (CPU-Zeit).
- Audio:
  - Bei Lautstärke 0 wird keine Schleife mehr alle 2 s neu angelegt.
  - Die Herzschlag-Suche läuft nur, wenn ein Schlag fällig ist.
