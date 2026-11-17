# Office Rooms – Übersicht (Stand 0.29.1)

Alles, was es im Mod gibt: Räume, versteckte Räume, Spezialräume, Megastrukturen, weitere Strukturen und Kreaturen. Spielinterne Namen auf Englisch, Erklärungen auf Deutsch. **„Neue Welten“** heißt: nur in Welten, die mit 0.28.0 oder neuer erstellt wurden (`content_version` 1).

---

## 1. Normale Räume
Die Office Rooms sind ein endloses Bürogebäude mit über 90 Geschossen. Jeder Raum bekommt einen dieser Einrichtungstypen:

| Raum | Kurz |
|---|---|
| Standard Office | Schreibtische, Stühle, Aktenschränke |
| Open Office | Großraumbüro mit Arbeitsplatzreihen |
| Meeting Room | Konferenztisch, Bürostühle, Meeting-Board |
| Break Room | Pausenraum mit Tischen, Kaffee, Wasserspender |
| Server Room | Serverschränke, Kabel, Brummen |
| Archive | Regale und Aktenschränke |
| Corridor | Flure, teils mit Kisten und Wasserspendern |
| Atrium | Hohe, offene Hallen über mehrere Geschosse, teils mit Galerien und Brücken |
| Training Room | Stuhlreihen, Leinwand, Projektor, Flipchart |
| Executive Office | Chefbüro mit Teppich, Ledersessel, Schreibtischlampe |
| Reception | Empfang mit Theke, Klingel, Wartestühlen, Ticketautomat |
| Restroom (selten) | Fliesen, Kabinen, Spiegel mit tropfendem Hahn |

Die untersten 5 Geschosse haben kein künstliches Licht.

---

## 2. Versteckte Räume (Hidden Rooms)
Hinter Wänden normaler Räume, mit eigenen Türen. In neuen Welten 12 Versuche pro Region (früher 6).

| Raum | Zugang | Inhalt |
|---|---|---|
| Maintenance Room | normale Tür | Werkzeug, Vorräte |
| Supply Closet | normale Tür | viele Vorräte |
| Forgotten Break Room | Tür einen Spalt offen | alte Vorräte |
| Security Office | Eisentür, nur von innen zu öffnen | Monitore, Keycards Stufe 1–2 |
| Research Lab (groß) | Sicherheitstür Stufe 1–2 | Forschungstisch (50 %), Research Files |
| Concealed Room (groß) | verborgen | gute Beute, selten Keycard Stufe 3 |

Etwa 15 % der versteckten Räume sind leer, nur Staub und Spinnweben.

---

## 3. Spezialräume
Ersetzen einen normalen Raum. Chance pro passendem Raum:

| Raum | Chance | Besonderheit |
|---|---|---|
| Lush Cave | 15 % (nur hohe Räume) | Höhlenpflanzen, Leuchtbeeren |
| Villager Outpost | 1,2 % | Dorfbewohner haben sich eingerichtet, Handel |
| Abandoned Camp | 0,4 % | Zelte, Lagerfeuer, Tagebuch |
| Water Office | 0,4 % | geflutetes Büro |
| Sculk Office | 0,08 % | Sculk und ein Warden |
| Oasis | 6 % der Regionen, max. 1 | tropischer Wasserpark, sehr gute Beute |
| Exit Room | 8 % (nur unterstes Geschoss) | Rückkehrportal in die Overworld |
| **Neu (0.29, neue Welten):** | | |
| The Last Meeting | 1 % | Meeting-Möbel, Projektor an, Protokoll, Truhe |
| Cold Storage | 0,8 % | Eis und Schnee, Gefrierfässer voller Essen |
| Mail Room | 0,8 % | Paketregale (viele zu öffnen), klingelndes Telefon |
| Canteen | 0,8 % | Automaten (nehmen Old Coins), Theke, Tische |
| Rain Room | 0,6 % | Regen von der Decke, Pfützen, trockener Schreibtisch |
| Arcade | 0,3 % | leuchtende Automaten, Preistruhe, keine Musik |

Die neuen Chancen lassen sich in der Config unter `[curiosities]` einstellen.

---

## 4. Megastrukturen
Belegen eine ganze Region (160 × 160 Blöcke) über mehrere Geschosse. Jede hat ein Artefakt.

| Megastruktur | Was dich erwartet | Jäger | Artefakt |
|---|---|---|---|
| **The Poolrooms** | Fliesen, warmes Wasser, Galerien, Unterwasser-Labyrinth. Neue Welten: Stufenbecken, abgelassene Becken, Kinderbecken, Bademeisterstühle, Bögen ins Nichts, Dunst | **Colony** | Chlorine Pearl |
| **The Maze** | Vergilbte Tapete, endlose Gänge, Tresor in der Mitte | nur der **Flayed** | Thread of the Maze |
| **The Abandoned Hotel** | Lobby, 2 Gästeetagen, Keller-Tresor. Neue Welten: Zwei-Bett-, verwüstete, „Do not disturb“-Zimmer, Wandleuchten, Eismaschinen | Hounds | Room Key 000 |
| **The Industrial Complex** | 5 Ebenen mit Rohren, Gittertüren, Generatorhallen | Flayed, Hounds | Humming Fuse |
| **The Neighbourhood** | Straße mit Häusern unter Neonlicht | Hounds | Faded Photograph |
| **Level Fun** | Ballons, Konfetti, Geschenke, Hüpfburgen | Partygoer | Party Smile Badge |

Die Artefakte öffnen am Ende den Weg in **The Underside**.

---

## 5. Weitere Strukturen und Orte
- **Aufzüge:** in jeder Region ohne Megastruktur. Panel wählen, die Türen schließen sich, man landet **nie** auf der gewählten Etage und manchmal in einer Megastruktur. Danach 5 Minuten Cooldown. Selten: Stillstand bei Stromausfall, Besucher vor der Tür, Klopfen aufs Dach.
- **Secure Storage:** Tresore der Stufen 1–3 mit Sicherheitstüren, zu öffnen mit der passenden Keycard.
- **Fuse Box (Stromausfall):** Bei einem Blackout gehen alle Lichter im Umkreis aus, Gaunts kommen. Wer den Sicherungskasten repariert, bekommt eine Belohnung.
- **Hazard Zone:** zeitweise giftige Zone in einer Megastruktur. Gasmaske nötig, dafür Spezialkisten.
- **Betten:** Schlafen und Spawnpunkt setzen geht auch in den Office Rooms.
- **Overworld:**
  - **Office Shrine:** Eingang in die Office Rooms.
  - **Research Outposts:** verlassene Labore neben Ancient Citys (60 %), unter Dschungeltempeln (40 %), selten in tiefen Höhlen und im Nether. Gateway in die Office Rooms.
- **Level 0:** über der obersten Decke der Office Rooms. Gelbe Endlosräume, ein unsterblicher Flayed, das Manila-Rätsel mit Belohnung.
- **The Underside:** unter dem Bedrock. Wiese unter einer Decke aus Bäumen, Bosskampf gegen **[Unknown]**, danach Schwert und Portale.

---

## 6. Kreaturen

| Kreatur | Wo | Verhalten |
|---|---|---|
| **Wolfspider** | Büros | dürre Stelzenbeine, glühende Augen, zuckt; jagt |
| **Friendly Wolfspider** | Büros (selten) | zähmbar, folgt dir |
| **Hound** | überall, v. a. Hotel, Neighbourhood | schnell, Wirbelsäule und hängendes Fleisch |
| **Smiler** | dunkle Räume | lauert im Dunkeln, nur das Lächeln ist zu sehen |
| **Gaunt** | dunkle Geschosse, Blackouts | 60 Leben, alarmiert andere |
| **Headless Warden** | selten, Sculk Office | groß, langsam, gefährlich |
| **Colony** | sehr selten in Büros; **jagt in den Poolrooms** | 80 Leben, viele dünne Beine |
| **Deathmoth** | dunkle Wände in Büros | sitzt reglos, fliegt dich an und beißt sich fest |
| **Flayed** | **Maze** (immer einer), Industrial, Level 0 (unsterblich) | extrem schnell, Schrei schüttelt die Kamera |
| **Partygoer** | Level Fun | steht und winkt; ab 2 Blöcken greift die ganze Party an |
| **Observer** | selten | beobachtet nur |
| **Hunter** | nur noch per Spawn-Ei | früher in den Poolrooms |
| **[Unknown]** | The Underside | Endboss, gräbt sich durch den Boden |

In der Overworld gibt es keine Office-Rooms-Kreaturen.

---

## 7. Fortschritt in Kürze
Questbuch mit 11 Kapiteln und 114 Quests (FTB Quests, optional): Eingang → erster Tag → Einrichtungen → Forschung → Megastrukturen → (Außenposten) → Underside → (Level 0) → Bestiarium → Requisitionen → Curiosities. Wichtige Gegenstände sind Research Files, Keycards 1–3, Electronic Components und Old Coins.
