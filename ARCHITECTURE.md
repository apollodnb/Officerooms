# Office Rooms – Architektur

Stand: Version 0.1.0 (Phase 1). Das Dokument erklärt, welche Klassen die Dimension erzeugen und wie die Daten fließen. Fachliche Details stehen in `docs/RAUMPLANUNG.md`, `docs/RAUMHOEHEN.md` und `docs/FENSTER_UND_ATRIEN.md`.

---

## 1. Überblick

```
 Forge lädt JAR ─► OfficeRooms (@Mod)
                     ├─ OfficeRoomsDimension.register(modBus) ─► OfficeWorldgen: Codec "office_rooms:office_rooms"
                     ├─ OfficeRoomsCommands.register()        ─► /officerooms … (Forge-Bus)
                     └─ client.ClientSetup (nur Client)       ─► DimensionSpecialEffects "office_rooms:office_rooms"

 Welt lädt Datapack-Registries:
   dimension/office_rooms.json ─► LevelStem: dimension_type + OfficeChunkGenerator(biome_source, settings)
                                                                   │  settings = GenSettings (GenSettingsCodec)
 Chunk-Erzeugung (beliebige Threads/Reihenfolge):                  ▼
   OfficeChunkGenerator.fillFromNoise ─► BlockSampler.column(x,z) ─► OfficePlanner.plan(rx,rz)  [Cache]
                                                                       ├─ layout(rx,rz)   = LayoutBuilder.build(...)
                                                                       │     ├─ AtriumPlanner.plan
                                                                       │     ├─ Treppen, Korridore, Räume, Höhen, Restzellen
                                                                       │     └─ ConnectionPlanner.planInternal (Spannbaum, Versiegelung)
                                                                       └─ OpeningPlanner.build(self, west, north)
                                                                             ├─ Türen/Durchgänge der Verbindungen
                                                                             ├─ Grenztüren West/Nord (liest Nachbar-Layouts)
                                                                             └─ WindowPlanner.plan (nur Raum↔Atrium)
                               BlockKind[] ─► BlockState ─► LevelChunkSection.setBlockState
```

## 2. Verantwortlichkeiten

| Klasse / Datei | Aufgabe |
|---|---|
| `OfficeRooms` | Einstieg, verbindet Module mit Mod- und Forge-Bus |
| `core.OfficeRoomsIds` | Mod-ID, Version, `id()` |
| `core.Hashing` | SplitMix64-Hash und `Rng`: einzige Zufallsquelle der Generierung |
| `dimension.OfficeRoomsDimension` | IDs, `LEVEL_KEY`, Höheneckdaten, registriert den Generator |
| `worldgen.OfficeWorldgen` | `DeferredRegister<Codec<? extends ChunkGenerator>>`, Eintrag `office_rooms` |
| `worldgen.OfficeChunkGenerator` | ChunkGenerator: Blöcke schreiben, Höhen-/Spaltenabfragen, F3-Info; leitet den Planer-Seed aus dem `RandomState` der Welt ab |
| `worldgen.GenSettingsCodec` | Codec der Einstellungen, alle Felder optional |
| `room.GenSettings` | Konfiguration als Java-Records mit Standardwerten (ohne Minecraft-Typen) |
| `room.OfficePlanner` | Raster, Geschosse, Caches für Layouts und Pläne |
| `room.LayoutBuilder` | Grundrisse einer Region: Atrien, Treppen, Korridore, Räume, Höhen, Restzellen |
| `room.RegionLayout` | Zellbelegung (`short` pro Zelle und Geschoss), Räume, Atrien, interne Verbindungen |
| `room.RegionPlan` | Layout + Öffnungscode pro Zellkante (1 Byte) |
| `room.Room` | Raum: Art, Bodengeschoss, Geschosszahl, lichte Höhe, Treppen-/Atriumdaten |
| `room.connection.*` | Kandidatengraph, Spannbaum, Versiegelung (`ConnectionPlanner`); Türtypen, offene Durchgänge, Grenztüren (`OpeningPlanner`); Kodierung (`Openings`) |
| `atrium.*` | Atrium-Modell und Vorabplanung inkl. Galerien und Brücken |
| `window.WindowPlanner` | Fenster nur auf Raum↔Atrium-Kanten |
| `room.structure.BlockSampler` / `BlockKind` | Rasterer Plan → abstrakte Blockart pro Koordinate |
| `room.PlanVerifier` | Regelprüfung auf Blockebene (Unit-Tests und `/officerooms verify`) |
| `room.Navigator` | Ankunftsraum und nächstes Atriumfenster für Befehle |
| `command.OfficeRoomsCommands` | `/officerooms enter, window, info, stats, verify` |
| `client.OfficeRoomsSkyEffects`, `ClientSetup` | kein Himmel, heller Dunst, normale Flächenschattierung |
| `data/…/dimension/office_rooms.json` | Dimension, Generator, **alle Generierungsparameter** |
| `data/…/dimension_type/office_rooms.json` | Höhe, `ambient_light 1.0`, Effekte `office_rooms:office_rooms` |
| `data/…/worldgen/biome/office_void.json` | leeres Biom, Nebel-/Himmelsfarbe = Dunst |

## 3. Pakete und Abhängigkeitsrichtung

```
worldgen ──► room ──► room.connection, room.structure, atrium, window ──► core
command  ──► room, worldgen, dimension
client   ──► core                       (nie umgekehrt)
dimension ─► worldgen, core
```

`room`, `room.connection`, `room.structure`, `atrium`, `window` und `core.Hashing` importieren **keine** Minecraft-Klassen. Sie sind mit JUnit testbar und bilden die Grundlage für spätere Phasen (Möbel, Raumdesigns, Loot), die nur die Blockumsetzung erweitern müssen.

Reserviert für spätere Phasen: `megastructure` (Sondermodule), `room.template` (Raumvorlagen mit Einrichtung), `loot`, `compat`.

## 4. Datenmodell

- **Zelle** = (Geschoss s, Einheit ux, uz) einer Region → `owner`: 0 = massiv, sonst Raumindex + 1. Ein Raum über mehrere Geschosse steht in allen seinen Geschossen.
- **Kante** = West- (axis 0) oder Nordkante einer Zelle → Schlüssel `((s·ru + uz)·ru + ux)·2 + axis`, Öffnungscode in `RegionPlan.edges`.
- **Knoten** im Verbindungsgraphen = Raumindex; zusätzlich je Atrium ein Knoten für den Boden und jede Galerieebene.
- **Grenzlinie** x = 0 / z = 0 einer Region gehört dieser Region. Nur sie plant und zeichnet die Türen darauf.

## 5. Nebenläufigkeit und Determinismus

- Alle Zufälle: `Hashing.hash(seed, rx, rz, Zweck, …)`. Keine Abhängigkeit von Reihenfolge, Zeit oder Thread.
- `PlanCache`: LRU mit `CompletableFuture` pro Schlüssel. Jede Region wird genau einmal berechnet, gleichzeitige Anfragen warten auf dasselbe Ergebnis. Fertige Pläne sind unveränderlich.
- `layout()` hängt von nichts ab. `plan()` hängt von `layout(self)`, `layout(west)` und `layout(north)` ab. Keine weitere Rekursion, keine Zyklen.

## 6. Erweiterungspunkte

| Vorhaben | Ansatz |
|---|---|
| Möbel/Einrichtung | Nach `fillFromNoise` in `applyBiomeDecoration` pro Raum Vorlagen platzieren; `Room` liefert Art, Größe, Höhe, Türpositionen (aus `RegionPlan`). |
| Neue Materialien/Raumdesigns | `BlockKind` erweitern oder die Zuordnung `OfficeChunkGenerator.states()` pro Raumtyp variieren. |
| Neue Raumtypen | `Room.Kind` erweitern, in `LayoutBuilder` platzieren, `ConnectionPlanner.nodeAt` und `BlockSampler.interior` ergänzen. |
| Loot | Container in Vorlagen, `RandomizableContainerBlockEntity.setLootTable`. |
| Mod-Kompatibilität | `compat`, nur bei `ModList.isLoaded`, Zusatzvorlagen mit Tag `requires:<modid>`. |

## 7. Konventionen

- IDs nur über `OfficeRoomsIds` und `OfficeRoomsDimension`.
- Jedes Modul bekommt eine statische `register(...)`-Methode, `OfficeRooms` ruft sie auf.
- Weltgenerierung: deterministisch, ohne globalen veränderlichen Zustand, Zufall nur aus Hashes.
- Nur Vanilla-Blöcke im Basisinhalt.
- Nach Änderungen an `data/` immer `python3 tools/validate_data.py`, nach Änderungen am Planer `./gradlew test`.

## Ergänzungen 0.2.0 / 0.3.0

| Paket | Klassen | Aufgabe |
|---|---|---|
| `furnish` | `RoomFurnisher`, `FurnishingPlanner`, `Item`, `Placement`, `RoomType` | reiner Java-Einrichtungsplan pro Raum: Raumtyp, Licht, Möbel mit Regeln (Türen frei, Wege-BFS, Chunkgrenzen); eigener Zufallsstrom für Licht |
| `compat` | `FurnitureBuilder`, `OfficeCraftCompat` | setzt Plan-Einträge als Blöcke: OfficeCraft über Reflexion, Refurbished über Registry-IDs, sonst Vanilla-Ersatz; Loot-Tabellen |
| `worldgen` | `FurnishingTicker`, `FurnishedData` | Lazy-Einrichtung in Spielernähe, gespeicherte erledigte Einheiten |
| `entity` | `Wolfspider`, `HeadlessWarden`, `NoiseSystem`, `MobSpawner`, `ModEntities` | Kreaturen, Geräusch-KI, Spawns über den Raumplan |
| `core` | `ModSounds`, `OfficeRoomsConfig` | Sound-Events, Forge-Konfiguration |
| `client` | `WolfspiderModel`, `HeadlessWardenModel`, `MobRenderers`, `ClientAudio` | Modelle, Renderer, Musik-/Summen-Schleifen, Herzschlag |

Datenfluss Möbel: `OfficePlanner` (Räume) → `RoomFurnisher` (Plan, deterministisch) → bei der Generierung nur Lichter (`OfficeChunkGenerator.applyBiomeDecoration`) → in Spielernähe `FurnishingTicker` → `FurnitureBuilder`.
