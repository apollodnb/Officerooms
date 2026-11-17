# Office Rooms – Projektstatus (Übergabe zwischen Chats)

Stand: 2026-11-17, ca. 14:45 (Phase 2). Nutzer achtet auf sein Wochenlimit: sparsam arbeiten, wenige Tool-Aufrufe.

## Offen / Ideen des Nutzers
- Screenshots mit Helligkeit 0 % (`run/options.txt` gamma 0.0). Für Overlay-Bilder F1 aus (prodclient.sh drückt F1 = HUD aus) und `sendCommandFeedback false`.
- Overworld: (noch) keine Office-Rooms-Mobs (Nutzer, 2026-11-12).
- Bekannt: `/officerooms locate all` blockiert den Server-Thread etwa 19 s (Testbefehl).
- Nutzer: Mod läuft im kompletten Better-Minecraft-Modpack (2026-11-14).
- **Build-Kontrolle:** immer `grep -E "FAILED|error:"` prüfen; 0.26.0–0.26.3 hatten unbemerkt 2 rote Planer-Tests (JAR wird trotzdem gebaut).
- Quests: Nutzer will „ganz einfach anfangen und extrem weit ausbauen“, Gating über Items und Kills, Level 0 optional, nur Office-Rooms-Inhalte. Nächste Ausbaustufen offen (mehr Kapitel und Quests, Loot-Crates, Sonderräume, Hotel- und Industrie-Details, Neighbourhood-Häuser …).
- Flooded Hallways und freundliche Mobs: vorerst weggelassen (Nutzer, 2026-11-11).
- Framework-Check (Modliste fehlt). Testumgebung enthält Refurbished Furniture + Framework. **KEIN Parkhaus.** Keine fremden Designs 1:1 nachbauen (Wandbilder aus Level-Fun-Fotos nicht kopieren).

## Plan (Nutzer, 2026-11-14) – Reihenfolge
1. ✅ Hotel-Fix und Rework (0.28.0).
2. ✅ (0.28.1) Poolrooms immersiver: Wassertiefen, Stufen und Bänke, abgelassene Becken, Unterwasserlichter, Dunst und Ringe, Bademeisterstuhl, Kinderbecken, Bögen ins Nichts. **Keine** Deckenlichter oder Oberlichter, **keine** Handtücher.
3. ✅ (0.29.0) Münzen (in Standard-Loot, 1–3, mittlere Chance) und Kantine mit Automaten (vor allem Vorräte).
4. ✅ (0.29.0) Spezialräume Kühlraum, Das letzte Meeting, Poststelle, Regenraum.
5. ✅ (0.29.0) Arcade-Raum (selten, ohne Melodie) und Quest-Kapitel „Curiosities“; Config-Chance pro Raum.
- Nutzer (22:33): Fokus immer auf neue Welten – noch nichts veröffentlicht, alte Welten werden gelöscht; Rücksicht auf bestehende Welten ist nicht nötig.
- Alle neuen Inhalte nur bei `content_version` ≥ 1 (neue Welten). Neue Räume haben normale Eingänge, keine Audio-Hinweise.

## Offen (Nutzer 23:11, Antwort „Mehr Stockwerke“): alle Megastrukturen mehrstöckig auf gleicher Fläche
- Erledigt: Maze (0.34.0), Poolrooms (0.35.0), Hotel/Industrie/Level Fun/Neighbourhood (0.36.0), Kino (0.37.0). Alle Megastrukturen sind mehrstöckig.

## Phase 2 (ab 2026-11-16) – Plan mit dem Nutzer
1. ✅ Kino mit mehr Stockwerken (0.37.0).
2. ~~Aranea-Männchen mit Geweih~~ – gestrichen (Nutzer, 17.11.).
3. ✅ Offene Testpunkte von 0.37.0 getestet (0.37.1).
- **Aufzüge bleiben absichtlich wahllos** (Nutzer, 17.11.): man soll nie dort ankommen, wo man hinwill. Nicht "verbessern".
4. ✅ Quests für neue Inhalte + Ticket im Schlüsselrezept (0.38.0). Offen dazu: Quests für neue Level-Fun-/Industrie-Räume (bräuchten Raum-Erkennung).
5. ✅ Nie getestete Altpunkte getestet (0.38.1): Rutsche, Rampen, Hotel-Treppen, Spielplatz, Umkleide, Bankett, Loot. Mehrere echte Fehler behoben.
6. ✅ Oberflächen-Forschungsstationen + Bohrinseln (0.39.0, Nutzerwunsch: selten!).
7. ✅ Großer Gesamttest (17.11., `docs/TEST_REPORT_full_0.39.0.md`): alle 7 Megastrukturen + Büro mittel/hoch, mit allen Mods und nur mit GeckoLib, je 45 s Überleben; keine Fehler, alle typischen Kreaturen da, Tick ≤ 30 ms; Feldstation komplett durchgespielt. Skript `scratch/fulltest039.sh` (in `tools/dev/`).
8. ✅ Balance: Nutzer findet die Schwierigkeit gut (17.11.), keine Änderung.
9. ✅ CurseForge-Banner + Beschreibung (`tools/promo/make_banners.py`, Ausgabe `/home/claude/banners/out`, Screenshots `/home/claude/banners/shots`).
10. ✅ Aufräumen + Release-Paket (0.39.1). Lizenz All Rights Reserved, Autor **Apollo**, Credits „Developed with Claude (Anthropic)“.
11. ✅ Quests installieren sich selbst (0.40.0), Release-Paket `outputs/OfficeRooms-0.40.0-release.zip`.
12. Offen: Release selbst (Nutzer lädt hoch), kleinere Punkte siehe PHASE2_START Abschnitt 6.

## Testumgebung (neu aufgesetzt 2026-11-16)
- Quellen aus `OfficeRooms-0.36.1-src.zip` nach `/home/claude/src0/OfficeRooms`. Java 17 per apt (`openjdk-17-jdk-headless`, `xdotool`).
- Proxy: Proxy-CA in `/etc/ssl/certs/java/cacerts` importieren (`keytool -importcert -alias ccr-agent-proxy -file /root/.ccr/agent-proxy-ca.crt`). Forge-Maven liefert über das Gateway sporadisch 404/Timeouts: `~/.gradle/init.d/mirror.gradle` hängt `https://maven.creeperhost.net/` als Spiegel an (Plugin- und Projekt-Repos), `~/.gradle/gradle.properties` mit längeren HTTP-Timeouts und Retries.
- Test-Mods: GeckoLib + Architectury von Modrinth (cdn.modrinth.com), FTB Quests/Library/Teams von maven.ftb.dev, Refurbished/Framework/OfficeCraft vom Nutzer hochgeladen (CurseForge ist gesperrt).
- prodtest: Forge-Installer **nur mit Temurin 17** (`prodtest/temurin`, von api.adoptium.net; Proxy-CA auch in dessen `lib/security/cacerts`). Ubuntu-OpenJDK 17 und 21 scheitern an der Prüfsumme der slim/extra-JARs. `server.properties`: online-mode=false, RCON 25575, Passwort `test`.
- Bei Lauftests mit xdotool kurze Tastendrücke (0,2–0,5 s pro Block), sonst läuft der Spieler über Podeste hinaus. Für Lauftests `gamerule fallDamage false`.

## Version 0.40.0 (Beta) – Questbuch installiert sich selbst (aktuelle Version)
- Auslieferung `/home/claude/outputs/v0400/`, Release `/home/claude/outputs/OfficeRooms-0.40.0-release.zip`. Nutzer: Quests sollen bei einem CurseForge-Download mitkommen.
- `quest/QuestInstaller` (aus `OfficeRooms.onCommonSetup`): nur wenn ftbquests geladen und Config `[quests] installQuests` (Standard true). Quelle `/office_rooms_quests/` im JAR (index.txt). Ziel `config/ftbquests/quests`. Ohne Kapitel → alles kopieren; sonst nur `office_rooms_*`-Dateien schreiben (alte `office_rooms_*` löschen) und Gruppe per Textmerge **hinter** die Pack-Gruppen in `chapter_groups.snbt` setzen; `data.snbt` und fremde Kapitel nie anfassen. Marker `.office_rooms_quests` = Version → nur einmal pro Version.
- `gen_quests.py`: PREFIX `office_rooms_` für Kapitel- und Tabellendateien (IDs unverändert), kopiert zusätzlich nach `src/main/resources/office_rooms_quests` + index.txt. Kapitel X/XI getauscht (Curiosities = X, Requisitions = XI).
- `OfficeRoomsIds.MOD_VERSION` (stand auf 0.18.1) ersetzt durch `version()` aus der ModList.
- Test `QuestInstallerTest`; im Spiel frische Installation und Merge mit nachgebautem Pack-Questbuch geprüft.

## Version 0.39.1 (Beta) – Release-Vorbereitung
- Auslieferung `/home/claude/outputs/v0391/`, Release-Paket `/home/claude/outputs/OfficeRooms-0.39.1-release.zip` (JAR, Quest-ZIP, README für Spieler, CHANGELOG aus allen NEW_IN, CONFIG.md mit Standard-Configs, LICENSE.txt, Ordner curseforge mit Bannern + Beschreibung).
- mods.toml: authors Apollo, neue Beschreibung (gradle.properties `mod_description`), `logoFile=office_rooms_logo.png` (1200×400, 128 Farben), credits. Achtung: Sonderzeichen wie „·“ übersteht processResources nicht (→ „??“), nur ASCII verwenden.
- `LICENSE` (All Rights Reserved, Modpacks über CurseForge/Modrinth erlaubt). `CHANGELOG.md` im Projekt-Root (aus docs/NEW_IN_*, neueste zuerst).
- Entfernt: Code-Modelle + Renderer für Gaunt, Colony, Headless Warden, Observer, Hound, Smiler, Hunter (alle GeckoLib); `MobRenderers` enthält nur noch die beiden Wolfspider-Renderer; `MobSpawner.hunter()` und `keepOneHunter()` entfernt.
- Deprecation-Warnungen (100) bewusst belassen: Block-Overrides in 1.20.1 üblich, `new ResourceLocation`/`FMLJavaModLoadingContext.get()` erst für einen 1.21-Port relevant.

## Version 0.39.0 (Beta) – Feldstationen und Bohrinseln
- Auslieferung `/home/claude/outputs/v0390/` (inkl. Quest-ZIP). Nutzer (11:09/11:10): Außenposten auch an der Oberfläche, **selten**; zusätzlich als Bohrinsel in großen Ozeanen.
- `OutpostBuilder.Theme` + SURFACE, OCEAN (angehängt). SURFACE: Fundament Steinziegel (wie Nether), `exterior(true)`: Fenster, Antenne, Vorplatz Kies, Schild (SignBlockEntity.setText), Laterne, Maschendrahtzaun (Eisengitter auf Heightmap, Lücke vorn). OCEAN: `rig()` vor `shell()` (Deck u −10..10, v −6..15 bei y −1, Geländer, 7 Beine bis Grund, Leiter ab sea−8 an u 0 v −7, Bohrturm, Lampe), `exterior(false)` = Fenster/Antenne/Schild.
- `OutpostPlacer`: SURFACE 1/2500 (Salz 5) → `surface()`: Höhen 19×21-Raster, Spanne ≤ 5, über Meeresspiegel, kein Wasser/Eis, `structureManager().hasAnyStructureAt`; Tür bei max. Höhe. OCEAN 1/900 (Salz 6) → `ocean()`: Biom IS_OCEAN, überall OCEAN_FLOOR ≤ sea−15, Tür sea+9. `tryAt()` + `reason` für den Testbefehl `/officerooms outpost try_surface|try_ocean`.
- `OutpostNotes.SURFACE/OCEAN` (je ein Logbuch). Quests: out_start-Text und HOW facility ergänzt.
- Testorte (Seed 4711): Feldstation Hebel 277 71 418; Bohrinsel Hebel −98 73 −1659 (Knopf −108 72 −1660, Gateway z −1666), zweite 107 73 −1714 (Leiter 97 55..73 −1720, facing west).
- Testhinweise: Hebel nur **einmal** klicken (Doppelklick schaltet wieder aus). Zielen mit `tp … facing X (Y−1.62) Z`.

## Version 0.38.1 (Beta) – Fixes aus den Lauftests
- Auslieferung `/home/claude/outputs/v0381/` (inkl. Quest-ZIP).
- `Poolrooms.Builder.hall`: Becken nur, wenn x+w ≤ l[2]−3 und z+d ≤ l[3]−3 (sonst AIOOBE 25600 → ganze Site kaputt, `locate poolrooms` Fehler). Test `PoolroomsBuildTest`.
- `Hotel.lStair`: 6. Stufe bei o == 6, ca 5..6, cb 6. `dir()` immer mit `_0`-Basis: `dir(STAIR_1, 3)` war BED_0 → jetzt `dir(STAIR_0, rot + 1)`. Test `HotelStairTest`.
- Rampen: Eckstufe (5,1) nach Osten (`i < 5` bzw. `up < 5`), Kopfraum ab Stufe 2 bis w+i+3, Decke neben (3..5, 0/2) offen. Level 0 `Level0ChunkGenerator.rampUp`, Maze `mazePart`.
- **Dev-Dump `SpotsDump`** (Test) → `build/spots.txt`: Planer-Seed wie im Spiel = `new XoroshiroRandomSource(4711).forkPositional().fromHashOf(office_rooms:office_layout).forkPositional().at(0,0,0).nextLong()` (= 7657905887809284474), Einstellungen aus `dimension/office_rooms.json` über `GenSettingsCodec`. Immer `Megastructures.kindOf` filtern (site() liefert auch für fremde Regionen etwas).
- Testorte (Seed 4711, frisch): Hotel-L-Treppe 182.9 246.5 217.5 (Süd, dann West), Level-0-Rampe 61.5 9 37.5 (Ost, dann Süd), Maze-Rampe −178.5 241 389.5, Rutschenöffnung 35 107 −23 (facing south, Rechtsklick), Spielplatz −399 239 28, Umkleide −296 197 −296, Bankett −295 455 35, dunkle Zelle −305 455 35.
- Testhinweise: prodclient setzt 5 s nach dem Join Zuschauermodus → erst danach `gamemode survival`. `pkill -x java` vor jedem Gradle-Lauf beendet auch Server/Client. Lauftests in 0,35-s-Schritten mit Positionslog.

## Version 0.38.0 (Beta) – Ticket im Schlüssel, Quests für neue Inhalte
- Auslieferung `/home/claude/outputs/v0380/` (inkl. Quest-ZIP). Nutzer (08:40): Kino-Ticket soll für den Schlüssel nötig sein.
- `tools/gen_0380.py`: Rezept `underside_key` mit 7 Zutaten (+ ticket_stub), Meilenstein-Advancements `slide_ride`, `cinema_top`, `hotel_top`. `gen_0190.py` ebenfalls angepasst (nicht neu laufen lassen).
- `Milestones.onTick`: cinema_top (y ≥ yBase + 2·FH), hotel_top (modern, storeyOf ≥ s0 + storeys − 1). `SlideTravel`: grant slide_ride nach dem Teleport.
- `gen_quests.py`: mega-Liste + cinema (find/art), Quests cinema_top, hotel_top, slide (Kapitel V, optional, mit Icons), key = 7 Artefakte, Bestiarium + aranea_membri; HOW-Einträge. 120 Quests. `stories.py`: okafor_six „The Seven Things“.
- Test: Quests nach `prodtest/config/ftbquests/quests` kopieren, `ftbquests reload`; Buch per `execute as Dev run ftbquests open_book`, Kapitelliste Pfeil (233,360), Kapitel V (349,317), VII (349,373). Ziehen/Mausrad im Questbuch per xdotool wirkungslos.
- Xvfb kann wegsterben → `rm -f /tmp/.X99-lock`, Xvfb neu starten.

## Version 0.37.1 (Beta) – Kino-Fixes nach Gesamttest
- Auslieferung `/home/claude/outputs/v0371/`.
- `Cinema.flight()`: Podeste oben an jeder Treppe (lx 42–45 bzw. 115–118, 3 Reihen inkl. Außenreihe lz 70 / 90) und am Fuß der oberen Treppen (lx 27–29 / 131–133) mit Geländer an Außenreihe und Stirnseite (`landing()`); Geländerlücke oben bis lx 44 / 116.
- `MobSpawner.cinema`: Aranea-Spawn auf der Etage des Spielers (`spawnInBand` sucht nicht nach unten!).
- `HazardEvents.NAMES` ohne Kino → ArrayIndexOutOfBounds beim Zonenstart seit 0.31 (jetzt "Abandoned Cinema" + Fallback).
- `ElevatorRides.megaArrival`: n = aktuelle Stockwerke (Hotel/Industrie/LevelFun/Neighbourhood `site.storeys`, Poolrooms/Maze `STOREYS`). Auswahl weiter nach waagerechter Nähe (Nutzer will Zufall).
- Test ohne optionale Mods: Jars nach `/home/claude/mods_off` verschieben (prodtest/mods und libs; build.gradle nimmt libs-Jars nur, wenn vorhanden).
- Testhinweise: rcon.py gibt pro Befehl 3 Zeilen aus (`> cmd`, Antwort, leer). `data get block` per RCON läuft in der Overworld → `execute in office_rooms:office_rooms run …`. Kein `E` im Kreativmodus (Dev-Client stürzt an GeckoLib-Beispiel-Items ab). Escape nur, wenn eine GUI offen ist. Aufzugspanel: tp -65.5 143 -82.5 -35 3 (Seed 4711, Aufzug nördlich vom Kino), Tasten 4 (574,369), 0 (638,473), GO (702,473).

## Version 0.37.0 (Beta) – Kino-Multiplex
- Auslieferung `/home/claude/outputs/v0370/`. Nutzerwunsch: Kino mit mehr Stockwerken auf gleicher Fläche (letzte Megastruktur ohne Erweiterung).
- `Cinema`: `FLOORS = 3`, `FH = 12`, `STOREYS = 6`, `H = 36`, `HALLS = 12`. `part()` rechnet pro Etage `f = off / FH`, `o = off % FH`; Säle, Saalwände, Gänge und Lobbywände pro Etage mit `o`. `hallKind(f * 12 + i)`, `hallAt(lx, off, lz)` liefert 0–35.
- Lobby = Atrium über alle Etagen: `gallery(lx, lz)` (Streifen 4 breit an den Lobbywänden lz ≤ 67 / ≥ 93, lx ≤ 6 / ≥ 154), auf Etage 1 und 2 Fliesenboden, `rail()` an der Innenkante (Parts `RAIL_X/Z/ES/WS/EN/WN` = Dunkeleichenzaun mit festen Verbindungen), unter den Galerien Decke mit Lichtern. Der Rest des Atriums ist bis `H - 1` offen.
- Treppen `flight()`: Etage 0 → 1 auf lz 68–69 (lx 30–41 steigt nach Osten, lx 130–119 nach Westen), Etage 1 → 2 auf lz 91–92, mit Podesten auf Galeriehöhe (lx 27–29 und 131–133). Unter den Stufen `RISER`. Geländerlücken über `railGap(f, lx, lz)`.
- `CinemaBlocks.decorate`: Saalnummern 1–36, Plakate pro Etage, Laufschrift „SCREENS 13 TO 24“ / „SCREENS 25 TO 36“, Fundsachen-Truhen in allen 36 Sälen.
- Generator: Ringtüren auf jeder Etage (`off % FH` in 1..3). `ElevatorRides`: n = `Cinema.STOREYS`. HazardEvents und MobSpawner nutzen STOREYS/H automatisch.
- Test: `CinemaTest.floorsAreConnectedByStairs` und Hallentest über alle Etagen. Im Spiel: Treppe A und B zu Fuß bestiegen (siehe TEST_REPORT_0.37.0).

## Version 0.36.1 (Beta) – Refurbished-Möbel in Hotel und Neighbourhood
- Auslieferung `/home/claude/outputs/v0361/`. Nutzer (18:09/18:10): Vanilla-Möbel in Hotel und Neighbourhood gegen Refurbished Furniture tauschen (nicht Level Fun, nicht Industrie).
- `megastructure/MegaFurniture`: im FEATURES-Schritt pro Chunk Part neu berechnen und ersetzen (`swap`, Mapper `hotel()`/`hood()`), nur neue Welten und nur wenn der Mod geladen ist; Richtung über `FurnitureBuilder.refurbished(id, front)`; Bad zweiteilig (type head bei u = 3); Zäune/Tische per `updateFromNeighbourShapes`. Neighbourhood.Site: `inPark`, `mailboxPost`.

## Version 0.36.0 (Beta) – Hotel-Labyrinth, höhere Megastrukturen
- Auslieferung `/home/claude/outputs/v0360/`. Nutzer (17:00/17:01): Hotel aus vielen kleinen Räumen mit Nutzen, verwinkelte Gänge, viele Stockwerke, verwinkelte Treppen, heimisch-gruselig; dazu mehr Stockwerke + Diversität für Industrie, Level Fun, Neighbourhood.
- Storey-Zahl pro Site (`site.storeys`/`site.h`; alte Welten behalten alte Werte). Generator-Schleifen, Atmosphere, HazardEvents, overlaps nutzen die Site-Werte.
- `HotelLayout` (neu, rein Java): 15×15 Zellen à 10 (ORG 5), Gang-Labyrinth über 7×7 Makrozellen (DFS + 12 % Schleifen), Rest Räume (Kind GUEST/TWIN/BATH/LINEN/LOUNGE/DINING/TRASHED/STATIC/PHONE/BARRICADED/EMPTY), Türen per BFS (auch Raum-zu-Raum), `arms()` = Gangverbindungen + Türstummel, 2 Treppen pro Etage `{cx,cz,rot}`. Test `HotelLayoutTest` (Erreichbarkeit), Dev-Dump `HotelMapDump` → `build/hotel_map.txt`.
- `Hotel`: STOREYS 7 (LEGACY 4), `labyrinth()`, `room()` mit (u,v)-Raumkoordinaten (v=1 an der Tür), `lStair()` (L-Treppe kanonisch Ost dann Süd, Rotation), Knicke in geraden Gängen (60 %), neue Parts inkl. Vierer-Gruppen (`dirBase`/`dir`, E,S,W,N). Lobby: 2 L-Treppen hoch; alte Treppenlanes nur noch Keller→Lobby. Decorator: Zimmerschilder, Truhen, Geländer per `updateFromNeighbourShapes`.
- `Industrial`: STOREYS 8 (LEGACY 5), Hallen 4 Ebenen, neue Typen TANKS/FOUNDRY/LOCKERS. `LevelFun`: 4 Etagen (LEGACY 1), Typen STAIRS/BALL_PIT/DISCO/BANQUET/DARK, Hash-Salz `sl(lv)`. `Neighbourhood`: 6 (LEGACY 4), zweistöckige Häuser (`house2`), Parks (`park()`), Straßenlaternen, Briefkästen, Varianten per `hh()`.

## Version 0.35.2 (Beta) – Kino-Tapete
- Auslieferung `/home/claude/outputs/v0352/`. `cinema_wallpaper.png` neu (Bordeaux, goldene Art-déco-Rauten und Fächer), direkt gezeichnet; in `gen_0310.py` ist die alte Funktion auskommentiert.

## Version 0.35.1 (Beta) – Rutschen als Rohre
- Auslieferung `/home/claude/outputs/v0351/`. Nutzer: Rutschen wie Rohre aus der Wand, ohne Stufen; Öffnungen im Büro nur an der Wand.
- `trySlide`: slideRole 3 (Rohr) / 4 (Mündung), Höhe FLOOR+2, b 4..(5–6), Rohre im Abstand 2, Becken b 4–14 Tiefe 5; Blöcke `pool_slide_pipe` / `pool_slide_pipe_end` (Achteck-Ring-Modell, Mündung schräg, FALLING_WATER-Partikel). Rollen 1/2 (alte Stufen-Rutsche) werden nicht mehr erzeugt. `slide_entrance`-Modell flach (z 13–16).

## Version 0.35.0 (Beta) – Poolrooms-Rework
- Auslieferung `/home/claude/outputs/v0350/`. Nutzer (Referenzbilder 12:09): möglichst liminal, riesige Räume, tiefe Pools, Treppen beibehalten, Rutschen und Bögen im Wasser.
- `Poolrooms`: STOREYS 8, H 46, FLOOR 11, GALLERY 23, CEIL 44, LOW_CEIL FLOOR+6, MAZE0 = FLOOR−5 (Labyrinth-Offsets mo = off − MAZE0; Eingangsschacht Wasser bis FLOOR; Vault-Truhen MAZE0+1). BSP: canX/canZ ≥ 56, force > 110. Unsichtbare Lichter FLOOR+3/9/17/25/31.
- modernize (neue Welten): Wasserbögen (archList, 35 %, Kanal ±6, Tiefe 2), Tiefe per BFS-Distanz (1 + 2·(d−1), Halle max 3–5 oder 6–10), Arkaden in tiefen Pools (Reihe ≥ 9, Säule alle 4, D_LINTEL), Poolleitern D_LADDER (9–12), Rutschen `trySlide` (Schacht b 7–8, Röhrenwände b 6/9, Beine bei h%4==2, Landebecken Tiefe 5), runde Bögen in `wallLine`. hall(): Pools bis 3/5 der Hallengröße, geflutet 22 %.
- Blöcke `megastructure/PoolBlocks` (pool_slide StairBlock friction 0,989 STRAIGHT, pool_slide_tube, pool_ladder Turned). Assets `tools/gen_0350.py` (Mosaik pool_tile/pool_floor_tile neu).
- Rutschen (Nutzer 12:33): ragen aus der Außenwand (`trySlide(side, a0, tubes)`, 1–3 Röhren nebeneinander, b 4–10 Höhe 7→1, Landebecken b 11–18 Tiefe 5), `slideEnds`. Handläufe `pool_handrail` (D_RAIL 13/14: an Beckenrändern in 5er-Läufen, freistehend 50 % pro Halle).
- Rutschenöffnung im Büro (Nutzer 12:29/12:39/12:41): Decor-Piece `slide_entrance` (Decor 28, Fx.SLIDE), nur an Wänden (`onWall`), Gänge 0,6 %, Räume 0,4 %, nur neue Welten. `world/SlideTravel`: Blindheit + Langsamkeit + Quake 60 Ticks, Sound `block.slide.rush/splash`, dann Teleport ans Ende einer Rutsche der nächsten Poolrooms (Ziel hängt nur von der Öffnungsposition ab, Ausgangsrohr zufällig).
- Bekannt: Lightmap färbt Fliesen warm.

## Version 0.34.0 (Beta) – Rampen, Grubenfelder, Maze mit 3 Ebenen
- Auslieferung `/home/claude/outputs/v0340/`. Block `level0_ramp` (StairBlock, Form immer STRAIGHT, Modell: 45°-Teppichfläche + Stufenkörper, `gen_0330.py` Abschnitt 0.34.0). Level-0-Rampe: letzte Stufe (5,3) liegt im oberen Boden.
- Level 0: THRESH = Grubenfeld (ganze Zellinnenfläche Loch, Laufstege auf den Zelllinien), UNDER darunter = Schächte mit Wandlinien und 2 hohen Durchgängen bei lx/lz 3, ceil(k) an den Linien massiv.
- Maze komplett neu (`Maze.java`): LEVELS 3, LP 7, STOREYS 4, H 24 (21–23 FILL). Pro Ebene eigenes Labyrinth (`buildLevel`, Rng-Salt 0x9210+lv, Wand-/Lichtarrays [lv]); `pits[lv]` 4×4-Zellen-Grubenfeld (lv ≥ 1, 80 %), darunter Kammerraster (Linien alle 4, Türen bei 2); `ramps` je 3 pro Ebene (RAMP-Pfad wie Level 0); Vault zufällige Ebene `vault.floor`, `walkOff()`. Decorator/Befehle/MobSpawner (Ebene des Spielers) angepasst. Test `MegastructuresTest` prüft je Ebene Zusammenhang und Vault-Tür.

## Version 0.33.0 (Beta) – Level 0 und Maze mit mehr Level-0-Features
- Auslieferung `/home/claude/outputs/v0330/`. Nutzer: gleiche Farbpalette wie Level 0, Löcher im Boden, sinnlose Architektur, Decke im Schnitt 1 höher, klassische Rampen zu anderen Stockwerken.
- Texturen `tools/gen_0330.py` (level0_wallpaper/worn/arrow, carpet/stained/wet, ceiling/stained, light/light_off; maze_* = Kopien der Level-0-Texturen). Neuer Block `level0_carpet_wet` (animateTick Splash + Tropfen).
- Level 0: `Level0Layout.PITCH` 7 (floorY 6+7k, walk 7+7k, ceil 12+7k). `Level0Layout.ramp()` (65 % der ladderUp-Zellen): 7 Stufen (1,1)…(5,1),(5,2),(5,3), Ausschnitt in ceil(k)/floorY(k+1) (oberes Geschoss: rampBelow → Luft an (4,1),(5,1),(5,2)). Neue odd-Arten 10–16 (Säule, Loch, Stufe, versiegelte Kammer, nasser Teppich, Lichtschalter, Wand kurz unter der Decke); Wände: Licht in der Wand, Glasfenster.
- Maze: STOREYS 2, H 12, CEIL 6 (Lichter in der Decke, off 7–11 FILL). `Site.odd()` pro Zelle (PILLAR, LOW, HANG, STEPS, DOOR, SEALED, PIT, WET, RAISED, PLATFORM), Wandvarianten, Deckenvarianten, Steckdosen. HOLE schneidet zusätzlich die Platte darunter (Generator). Exit-Kandidaten auch Räume mit bottom ≤ s0 ≤ top und Atrien; Fallback ohne Kandidaten: 3 einfache Öffnungen.
- Testorte (Seed 4711): Level-0-Rampe bei 63 7 39 (Geschoss 0); Maze bei −171 317 324.

## Version 0.32.0 (Beta) – Aranea Membri
- Auslieferung `/home/claude/outputs/v0320/`. Nutzer: möglichst exakt nach seinen Referenzbildern (dicke schwarze Beine, Knie hoch → M-Silhouette, kleiner Rumpf tief, bleicher Raubtierschädel hängt vorne). Selbst modelliert, keine Bilder übernommen. Selten in Office Rooms und in Kinos (Nutzer 21:22).
- Modell `gen_mobs_geo.aranea()` (density 1, Beine je in eigener Ebene, per Bone-Yaw ±38° gedreht; leere Glowmask wird erzeugt, sonst Magenta). Animationen idle/walk/run/screech/attack.
- `entity/AraneaMembri`: DATA_LURK (sync) → unsichtbar, NoAI, NoGravity, noPhysics, nicht treffbar; `anchor()` hebt bis Hitbox an die Decke (Scan 12), setzt 1–2 `ceiling_hole` (mobGriefing); Kratzen + FALLING_DUST; Drop bei Spieler < 2,4 horizontal darunter → Decke/Lampen zerstören, hinter Spieler landen, Screech + Quake; Jagd: CeilingLightBlock LIT → false (Radius 1, 2–6 hoch); nach 200 Ticks ohne Ziel und ungesehen → wieder Lurk, dropCooldown 600. Spawn-Ei: wach; summon `{Lurking:0b}` für wach. Attribute in createAttributes fest, Config im Konstruktor (sonst Absturz: Config noch nicht geladen).
- Spawning: MobSpawner Office (ARANEA_SPAWN_CHANCE 0,002, nicht in Megastrukturen, wardenRooms, max 1) und `cinema()` (ARANEA_CINEMA_CHANCE 0,04, max 1 pro Kino). Sounds `tools/gen_0320.py <wav-dir>` (Screech aus flayed+colony, Hurt colony, Death flayed, Ambient hound, Kratzen/Schritte synthetisch). Hitbox 1,2 × 1,9.
- Offen: Männchen-Variante mit Geweih.

## Version 0.31.0 (Beta) – Raum-Hall und Kino (aktuelle Version)
- Auslieferung `/home/claude/outputs/v0310/`.
- `client/RoomReverb`: 26 Strahlen (6 pro Tick, `level.clip`), mittlere freie Distanz + Anteil weicher Flächen (SoundType WOOL/MOSS) → EAX-Reverb (Fallback Standard-Reverb) auf einem Aux-Slot; `PlaySoundSourceEvent`/`PlayStreamingSourceEvent` (Soundthread), Kanal-Quelle per `ObfuscationReflectionHelper` Feld `f_83642_`; Musik/relative/Attenuation-NONE trocken; bei Fehlern dauerhaft aus. Client-Config `roomReverb`, `roomReverbStrength`. Nicht hörbar getestet (kein OpenAL im Testrechner).
- Megastruktur `Cinema` (Kind CINEMA: u < 6c **und** `cfg.modern()`), STOREYS 2, H 12. Layout lokal: Ring lx/lz ≤ 2 bzw. ≥ 158; Säle Nord lz 3–56 (Leinwand bei lz 56), Wand 57 mit Türen ax 1–2 / w-1..w, Flur 58–62 (Decke off 6), Wand 63 mit Öffnungen (m 12–15), Lobby 64–96, Wand 97, Flur 98–102, Wand 103, Säle Süd 104–157. Säle je 26 breit (x0 = 3 + 26i), Stufen `level(t)`; Sitzreihen (t-10)%3==1. Hallenarten NORMAL/STATIC/DARK/WRECKED/FIGURE per Hash 0x9C01.
- `CinemaBlocks`: 32 Blöcke (Turned-Basisklasse mit gedrehter Form, Letter GLYPH 0–35, Poster VARIANT/UPPER, Projector mit Partikelstrahl + Sound `block.cinema.projector`), `decorate()` (Schriftzüge TICKETS/NOW SHOWING/CONCESSIONS, Saalnummern, Poster, Truhen cinema_box_office/concessions/lost_found). Item `ticket_stub`. Assets `tools/gen_0310.py`. Integration: OfficePlanner.cinema/megaContains, SpecialType.CINEMA, SpecialRooms, RoomFurnisher, Generator `cinema()`, MegaDecorator, HazardEvents, ElevatorRides, Befehle goto/locate. Test `CinemaTest`.
- Testort (Seed der prodtest-Welt, frisch): Kino bei 240 365 −76 (yBase 365; baseX 160, baseZ −160). RCON-tp immer mit `execute in office_rooms:office_rooms`.

## Version 0.30.1 (Beta) – Lauter, Jagd-Loops
- Auslieferung `/home/claude/outputs/v0301/`. gen_sounds_0300.py: Mob-Sounds `remix -` + `compand` vor den Effekten, `norm -0.3`; neue Dateien `entity/flayed/hunt_loop`, `entity/hound/hunt_loop` (komplette Aufnahmen).
- `client/MobLoops`: AbstractTickableSoundInstance pro Mob (WeakHashMap), looping, folgt dem Mob, stoppt, wenn er aufhört zu jagen. Flayed: neues synchronisiertes `DATA_HUNT`, Einzelschreie entfernt (screamShake bleibt). Hound: `isAngry()` → Loop, Bark-Sound entfernt, Growl während der Jagd stumm. Aufruf per `DistExecutor.unsafeRunWhenOn`.
- Musik: sounds.json volume 1,0; Config-Schlüssel `musicVolume` (Standard 0,8) ersetzt `musicLevel` 0,2.

## Version 0.30.0 (Beta) – Sound-Überarbeitung
- Auslieferung `/home/claude/outputs/v0300/`. Quelle: Ableton-WAVs des Nutzers (flayed, hound, colony, office_rooms_music_2_/3, level_fun_background_music); Generator `tools/gen_sounds_0300.py <dir>` (sox, Mob-Sounds mono, Musik stereo).
- Trigger: flayed/scream1–3 (scream4 entfernt), hound/bark1–4, colony/scream1–7. Hurt: flayed/hound/colony/wolfspider je 3, warden/gaunt/hunter/partygoer/unknown je 2, deathmoth 1 – alle aus den Aufnahmen (pitch/overdrive/reverb/reverse). Idle/Ambient unverändert.
- Musik: `music.office_rooms` mit 3 Varianten (office_rooms, office_rooms2, office_rooms3); ClientAudio: beim Betreten 1 zufälliger Track, danach Intervall × 0,7–1,3, nur wenn kein Track läuft. Neues Event `music.level_fun` (ModSounds.MUSIC_LEVEL_FUN), Schleife solange `AmbientSounds.funNearby` (≥4 Fun-Tapeten/Spielteppich, alle 2 s, 4 Fehlchecks zum Verlassen); Office-Musik dann gestoppt, Timer ≥ 60 s nach Fun. Spieluhr aus FUN-Pool entfernt.
- Testrechner hat kein OpenAL-Gerät → Wiedergabe im Spiel nicht getestet.

## Version 0.29.1 (Beta) – Letztes Meeting mit Meetingraum-Möbeln
- Auslieferung `/home/claude/outputs/v0291/`. RoomFurnisher.specialRoom: LAST_MEETING → `meetingRoom()` (OfficeCraft-Konferenztisch, Stühle, Board); SpecialDecorator.lastMeeting nur noch Projektor, Leinwand, Lesepult mit MINUTES, Truhe.

## Version 0.29.0 (Beta) – Curiosities, Münzen, Automaten
- Auslieferung `/home/claude/outputs/v0290/`.
- SpecialType/RoomType + COLD_STORAGE, LAST_MEETING, MAIL_ROOM, RAIN_ROOM, CANTEEN, ARCADE (am Ende angehängt). SpecialRooms.of: nur `cfg.modern()`, einstöckig, nicht in dunklen Geschossen; `SpecialRooms.CURIO` (arcade 0,003, rain 0,006, cold 0,008, mail 0,008, canteen 0,008, meeting 0,01; Mindestzellen 12/8/6/8/16/10), Config `[curiosities]` → `applyCurio()` beim Laden.
- SpecialDecorator: coldStorage, lastMeeting (Buch MINUTES), mailRoom, rainRoom, canteen (diningSet oder Vanilla-Tische), arcade; Helfer `wallPairs`, `piece`, `into`. RoomFurnisher-Licht: Kantine normal, Kühlraum/Post/Regen dim, Meeting partial, Arcade keins.
- Blöcke `block/CuriosityBlocks`: vending_machine (+top, Münze → `gameplay/vending`), arcade_cabinet (+top), parcel_stack, arcade_carpet, rain_emitter (unsichtbar, FALLING_WATER-Partikel). Item `office_coin`. Assets `tools/gen_0290.py`.
- Loot: chests/cold_storage, last_meeting, mail_parcel, rain_room, canteen, arcade_prizes; gameplay/vending; Münz-Pool (0,3; 1–3) in 29 normalen Truhentabellen.
- Milestones `room_<id>` per Tick (SpecialRooms.at an Spielerposition); Quests Kapitel XI „Curiosities“ (114 Quests).
- Test `SpecialRoomsTest.curiositiesInNewWorlds`.

## Version 0.28.1 (Beta) – Poolrooms immersiver
- Auslieferung `/home/claude/outputs/v0281/`. Poolrooms.Site: `modern`, `deco[]` (D_BENCH 1, D_POST 2, D_SEAT 3–6, D_ARCH 7, D_LINTEL 8), `drained[]`; `Builder.modernize()` mit eigenem Rng (alte Layouts unverändert): Kinderbecken, abgelassene Hallen 18 %, Bademeisterstuhl 55 %, Bogen 40 %, Tiefe nach Randabstand (1/2/3–4), Bänke an K_WALL. Part.POST = Birkenzaun. Client `PoolFx` (WHITE_ASH-Dunst, FISHING-Ringe, auch in alten Welten).

## Version 0.28.0 (Beta) – Hotel-Fix und Rework
- Auslieferung `/home/claude/outputs/v0280/`.
- GenSettings: neues Feld `contentVersion` (Codec `content_version`, Standard 0; JSON 1), `cfg.modern()`.
- Hotel: Wand bei lx = AX0 in Zimmerbändern (alle Welten). Site.modern; `RoomKind` (STANDARD, TWIN, TRASHED, BARRICADED, STATIC, PHONE, EMPTY) per Hash 0x9481; `modernRoom()`; Flur: SCONCE an ax 0 (r 2), ICE_S/CART_N 7 % an ax 5; CARPET2 (teal) auf lv 3; Lampen auf den Gästeetagen zu 55 % aus. BARRICADED: Eisentür, Barrikade innen, Schild „DO NOT DISTURB“, immer Truhe.
- Neue Deko-Pieces (Decor 23–27): hotel_phone (Fx PHONE), static_tv (STATIC), ice_machine (HUM), housekeeping_cart, hotel_sconce (Licht 9); Block hotel_carpet_teal. Assets `tools/gen_0280.py`.

## Version 0.27.1 (Beta) – Hound gruseliger und schneller
- Auslieferung `/home/claude/outputs/v0271/`. gen_mobs_geo.hound(): schmaler Rumpf, 12 Wirbel und Querfortsätze, Wirbelhals, Fleischfetzen an face/jaw/Bauch, dünnere Beine, Wirbelschwanz (Bone-Namen unverändert). Hound.java: Speed 0,33, MeleeAttackGoal 1,3.

## Version 0.27.0 (Beta) – Maze, Partygoer, Loot, Balance
- Auslieferung `/home/claude/outputs/v0270/`.
- MobSpawner.maze: nur Flayed (Zählung im Band ohne inflate, bis 4 Spawnversuche 24–72), andere nicht persistente Mobs im Band werden entfernt; `nearMaze()` sperrt den generischen Flayed-Spawn in Maze-Nachbarregionen (±3 Geschosse).
- Atmosphere.hint: Maze ohne Sound (AMB_NEON klang wie 8-Bit).
- Partygoer: Approach und Stroll entfernt, LookAtPlayer 24, TRIGGER 2,0, Winken alle 3–6 s bis 16 Blöcke; Alarm-Logik von 0.24.0 bleibt.
- Config: Gaunt-Leben 60, Colony 80, CURRENT_BALANCE 5 (Migration 4 → 5, 3 → 4 → 5).
- Loot: research_file in normalen Truhen etwa ×0,4; tiers/rare und special: alle Nicht-Karten-Gewichte ×5 (L1 1,2 % bzw. 0,57 %, L2 0,28 %); hotel_reception L1 0,12 / L2 0,04; industrial_control 0,1 / 0,03.
- GenSettings.Special neu `hideoutAttempts` (Codec `hideout_attempts`, Standard 6 für alte Welten; JSON 12); `outpost_chance` 0,012 in der JSON. Gilt nur für neue Welten.
- Extras.panelSide: Ecke muss an beiden Wänden frei von fremden Öffnungen sein; sonst keine Wandkonsole, dann öffnet `elevator_floor` per Rechtsklick das Panel.

## Version 0.26.3 (Beta) – Headless Warden wieder alt
- Auslieferung `/home/claude/outputs/v0263/`. Nutzer: Warden-Umbau zurück, Rest bleibt. `warden()` in gen_mobs_geo.py und die Warden-Assets entsprechen 0.26.1 (byte-gleich).

## Version 0.26.2 (Beta) – Spindeldürre Mobs, Colony jagt in den Poolrooms
- Auslieferung `/home/claude/outputs/v0262/` (JAR, Quellen, Quests).
- **Wichtig:** Gaunt, Colony, Headless Warden, Hound, Smiler, Hunter und Observer sind **GeckoLib-Modelle** aus `tools/gen_mobs_geo.py` (Renderer `GeoMobRenderer` in ClientSetup). Die Java-Klassen `GauntModel`, `ColonyModel` und `HeadlessWardenModel` sind ungenutzte Altlasten. Neu erzeugen nur gezielt: `python3 -c "import gen_mobs_geo as m; m.gaunt()"` im Ordner tools.
- gen_mobs_geo.py:
  - Gaunt: dünnere Beine, Arme und Krallen;
  - Warden: dünn umgebaut, in 0.26.3 auf Nutzerwunsch zurückgesetzt;
  - Colony: Gliedmaßen 0,6/0,45 mit `_s` 60° und `lo` −80° (Idle/Crawl-Basis 60), Fäden, gestufter Crawl.
- MobSpawner: `poolHunter()` (mindestens eine Colony im Poolrooms-Band; nicht persistente Hunter dort entfernen). Der alte `hunter()` ist ungenutzt. Quest `kill_hunter` entfernt (106 Quests).
- Fototipp: Fläche freiräumen, `light`-Blöcke an der Decke, Kamera per `tp X Y Z facing X (Zielhöhe − 1,62) Z` (`facing` zielt von den Füßen).

## Version 0.26.1 (Beta) – Böse Wolfspider neu
- Auslieferung `/home/claude/outputs/v0261/`. `client/DreadWolfspiderModel` (Code-ModelParts, Textur `wolfspider_dread.png` 64×64 + Augen-Layer `wolfspider_dread_eyes.png` per EyesLayer). Layer `DREAD_WOLFSPIDER_LAYER`, Renderer ohne Skalierung. Hitbox 0,95 × 0,95. Freundliche Wolfspider unverändert (alter `WolfspiderModel`).
- Animation: Beine Hüfte(yaw) → Coxa/Femur/Tibia (zRot), gestufter Gang, Kopf-Kippen per Zeit-Slot-Rauschen `rnd(id, slot, salt)`.
- Testhinweis: Monster despawnen sofort, wenn Dev im Zuschauermodus ist → für Fotos Kreativ + `PersistenceRequired:1b`.

## Version 0.26.0 (Beta) – Echte Aufzüge, Betten, dunkler
- Auslieferung `/home/claude/outputs/v0260/`.
- Aufzüge: `world/ElevatorRides.java` (Fahrt-Zustandsmaschine, Ziel `pickTarget`: zufälliger Halt in Fahrtrichtung, nie die Wunschetage; `megaArrival`: Megastruktur in Nachbarregionen (Radius 1), deren Band zwischen Start und Ziel liegt → Ankunft dort per safeSpot; Cooldown 5 min pro Schacht, statische Map, nicht gespeichert; Blackout-Stau max. 3 min; Besucher 8 % Hound/Gaunt; Klopfen 15 %).
  - Blöcke: `ElevatorDoorBlock` (AXIS, STAGE 0–2, wird von der Fahrt gesetzt und entfernt; zerbrechbar), `ElevatorPanelBlock` (FACING, Klickfläche größer als Modell), `elevator_wall`, `elevator_floor`.
  - BlockKind: ELEVATOR → elevator_floor, ELEV_WALL → elevator_wall, neu ELEV_LIGHT (19, ceiling_panel), ELEV_PANEL_E..N (20–23), COUNT 24. Extras: Panel neben der ersten Tür (`panelSide`, PANEL_FX/FZ), Licht Mitte pl5.
  - Netzwerk: HazardNet VERSION 2, `ElevatorPanel` (S2C, id 3), `ElevatorCall` (C2S, id 4); `client/ElevatorPanelScreen`.
  - Springen/Schleichen in generierten Schächten deaktiviert; selbst gestapelte Platten wie bisher.
- Betten: `world/OfficeSleep.java` (SleepingTimeCheck ALLOW + eigener PlayerSleepInBedEvent mit Vanilla-Prüfungen). Dimension bleibt `natural: false` (natural wirkt in 1.20.1 nur auf Betten, Kompass, Uhr, Portal-Piglins; Nutzer will alles sonst unverändert).
- Dimension 30 % dunkler: `OfficeRoomsSkyEffects.adjustLightmapColors` × 0,7.
- Flayed: `screamShake()` (HazardNet.Quake an Spieler ≤ 32 Blöcke).
- Testhinweis: Panel per `tp … yaw pitch` anvisieren (nicht `facing`, das zielt von den Füßen); erster xdotool-Klick nach tp geht oft verloren → `mousemove 640 360 click 3`.

## Version 0.25.1 (Beta) – Quest-Hinweise und Atrium-Hintergrund
- Auslieferung `/home/claude/outputs/v0251/` (JAR, Quests, Paket).
- `gen_quests.py`:
  - HOW-Tabelle (Präfix-Schlüssel wie `lab_`, `md_`, `kill_` …), an jede Beschreibung angehängt als „&6How:“;
  - ICONS für Quests mit Ereignis-Aufgaben;
  - Kapitelbild `images` (atrium_1..3, alpha 150, order −1, deckt die Quest-Bounding-Box im Format 16:9 ab).
- Texturen: `assets/office_rooms/textures/gui/quests/atrium_*.png`, Atrium-Fotos mit Gamma 0,55, warmem Ton und Vignette.
- Overworld optional (Nutzerwunsch):
  - rift hängt nur von memo ab;
  - shrine und sand sind optional;
  - neue Quest `facility` (outpost_gateway, optional);
  - Kapitel VI optional, `out_start` per Checkmark ab memo.
- Level 0: furn 1 und 2 (übergroße Tische aus Stämmen und Bretterblöcken) entfernt (Nutzerwunsch). Gilt nur für neue Chunks.
- Electronic Components (Nachtrag, gleiche Version) als zusätzlicher Pool mit random_chance:
  - chests/server_room 0,4 (1–2);
  - hideout_maintenance und hideout_security 0,35 (1–2);
  - tiers/tools 0,08 (1).
  - Gemessen: 50 Server-Truhen → 39 Stück; 100 Würfe tools → 10.

## Version 0.25.0 (Beta) – Questbuch, Milestones, echte Stromausfälle
- Auslieferung `/home/claude/outputs/v0250/OfficeRooms-0.25.0.zip` und `OfficeRooms-0.25.0-quests.zip`.
- **FTB Quests**:
  - `tools/gen_quests.py` → `quests/config/ftbquests/quests/` (data.snbt version 13, chapter_groups, chapters/*.snbt, reward_tables/supplies.snbt).
  - IDs per SHA1 aus einem Schlüssel, **oberstes Bit 0** (FTB liest Long mit Vorzeichen).
  - Task-Typen: item (consume_items boolesch), kill (entity/value), advancement, dimension, observation (observe_type 5 = ENTITY_TYPE), checkmark.
  - Rewards: item, xp, random (table_id als Long).
  - 10 Kapitel, 106 Quests.
  - Testumgebung: FTB-JARs in `libs/` (`*-0.jar`) als runtimeOnly (optionalMods in build.gradle) und in `prodtest/mods`, inklusive Architectury 9.2.14 vom Nutzer.
  - Quests liegen im Server unter `prodtest/config/ftbquests/quests`. `/ftbquests open_book`, `/ftbquests reload`, `/ftbquests change_progress Dev complete <id>`.
- **`quest/Milestones`**: `grant(p, path)` → `office_rooms:milestones/<path>` (impossible-Trigger, ohne Anzeige). Gesetzt in:
  - FuseBoxBlock.reward;
  - ResearchMenu (heart_research(_max), reforge(_max));
  - Controller ONLINE (Spieler im Umkreis von 32 Blöcken);
  - Level0Events.escape;
  - Tick alle 2 s: `discover_<kind>` über megaContains + kindOf, `level0_manila` über den Block unter dem Spieler.
- **goto**:
  - HIDEOUT_IDS (6 Typen, Filter in megaHits, Suchradius 10 Regionen);
  - `gotoSpecial`: fuse_box, hazard_zone, outpost (`outpost/OutpostRegistry`, SavedData pro Dimension, Eintrag in OutpostPlacer), shrine (`findNearestMapStructure`).
  - Auch in LOCATE_TYPES.
- **Blackout**: BlackoutData off/restore/doneChunks (LongSets, gespeichert).
  - `BlackoutEvents.onLevelTick` alle 5 Ticks: `darken` (6 Chunks, `section.maybeHas(lit)`, LIT = false); `relight` bei end; `restoreLoaded` für nachgeladene Chunks.
  - Radius 80, Stockwerke 4; CURRENT_BALANCE 4 (Migration 3 → 4, 2 → 3 → 4 rekursiv).
- **Fixes**:
  - `Level0Events.onJoin` nur auf dem Server. Tags werden nicht synchronisiert, deshalb hat der Client den Flayed verworfen → er war unsichtbar.
  - Unknown: `steer` direkt bei Abstand < 6·speed; BURROW-Ende bei 2,0 bzw. 160 Ticks; LUNGE-Timeout 100.

## Version 0.24.0 (Beta) – Deathmoth, Gesamttest, Fixes
- Auslieferung `/home/claude/outputs/v0240/OfficeRooms-0.24.0.zip`, Bilder `docs/bilder/0240_*`.
- **Deathmoth**:
  - `entity/Deathmoth`: Monster, noGravity, Hitbox 0,7 × 0,5.
    - Zustände WALL/FLY/ATTACHED/LEAVE, synchronisiert über STATE, WALL_DIR (2D-Wert der Normalen), TARGET, FOLDED.
    - `attachTo(air, normal, idle)`: 0,14 zur Wand versetzt; der Renderer verschiebt zusätzlich um 0,27.
    - Auslöser: Reichweite 6 mit eigener Sichtprüfung `sees()` oder unter 2,5 Blöcken, oder bei Treffer.
    - FLY: Steuerung, nach 10 s Aufgabe. ATTACHED: noPhysics; Biss 2, alle 12 Ticks 1,5 (`invulnerableTime` = 0), 3,5 s; ein Treffer löst ihn. LEAVE: 60 Ticks, danach discard.
    - Gespeichert wird nur WALL oder LEAVE. `finalizeSpawn` hängt sich an eine Wand (0–2 höher).
  - `entity/DeathmothDirector`:
    - `populateOffice()` aus MobSpawner (Räume / roomsPerMob 35, höchstens 3; in Megastrukturen mindestens 70 Raumäquivalente).
    - `trySpawn(level, p, minD, maxD)`: 96 Proben, Wand mit sturdy face, Boden 1–4 tiefer, außer Sicht. Licht ≤ 7 bevorzugt, ≤ 12 zu 50 %.
    - `forbidden()`: Maze/Poolrooms per `Megastructures.kindOf` der Region plus `contains`. Nur Office-Dimension.
  - `client/DeathmothRenderer`: ModelParts in Code (Flügel als Flächen der Höhe 0, cutout). Wand: Y-Rotation zur Normalen, dann X +90°. Overlay `textures/gui/deathmoth_overlay.png` per RegisterGuiOverlaysEvent, abgedunkelt nach Licht (0,5 bis 1). Kamerazucken über `UndersideClientFx.onCamera`.
  - Assets: `tools/gen_0240.py` (Textur 128×64 und Overlay), `tools/gen_sounds_0240.py`. ModSounds DEATHMOTH_*, Spawn-Ei, Config `[deathmoth]`, Befehl `/officerooms deathmoth`.
  - Nutzerwünsche: nicht zeitgesteuert, sondern Population wie die anderen Mobs; nicht in der Overworld.
- **Balance**: CURRENT_BALANCE 3. Migration 2 → 3 ändert nur Werte, die noch auf dem alten Standard stehen:
  - Wolfspider roomsPerMob 40 → 70;
  - Hound roomsPerMob 60 → 45, spawnChance 0,15 → 0,2.
- **Atrien**: density 0,075 → 0,1 (JSON und GenSettings). Gilt nur für neue Welten, weil die Einstellungen in level.dat stehen.
- **Partygoer**: Wird einer feindlich, setzen alle im Umkreis von 48 Blöcken (±8 Höhe) ohne Ziel dasselbe Ziel. Das Feld `alerted` verhindert eine Weitergabe. FOLLOW_RANGE 48.
- **Fixes**:
  - `FurnishingTicker.loaded3x3`: Möbel erst, wenn alle Nachbar-Chunks geladen sind. OfficeCraft `PartEntity.configure` → setChanged → Nachbar-Update hat sonst synchron Chunks generiert.
  - Sculk-Warden-Fallback `anyFreeTile`.
  - Klarere Meldung bei `blackout warn/start`.
- **Testwerkzeuge**:
  - `/home/claude/scratch/fulltest.py [rooms|events|dims]`, `mobtest.py`;
  - `sample.sh N` (jstack-Sampler für den Server-Thread);
  - `/tmp/mothatk.py`, `/tmp/party.py`.

## Version 0.23.0 (Beta) – Neue Standardräume
- Auslieferung `/home/claude/outputs/v0230/OfficeRooms-0.23.0.zip`.
- RoomType: TRAINING_ROOM, EXECUTIVE_OFFICE, RECEPTION und RESTROOM, vor VILLAGER_OUTPOST eingefügt (wegen `isSpecial`).
- `RoomFurnisher.extraType()`: eigene `Rng(hash(seed, rx, rz, 0xFC, index))`, überschreibt den Tabellenwurf; alte Layouts bleiben gleich.
  - Klein (≤ 9 Zellen): WC 2,5 %, Chef 6 %.
  - Mittel (≤ 30): WC 2 %, Chef 5 %, Empfang 5 %, Schulung 6 %.
  - Groß: Empfang 6 %, Schulung 8 %.
- Raum-Methoden `restroom()`, `trainingRoom()` (ohne Leinwand → meetingRoom), `executiveOffice()`, `reception()`; Helfer `deco`, `wallY`, `floorCover`, `onWall`, `wallRow`.
- Neue Items:
  - TOILET, BASIN, EXEC_DESK, SOFA, CABINET (Container) als `rf`-Blöcke mit Vanilla-Fallback;
  - BOOKSHELF;
  - LECTERN mit Buch „Orientation Handout“;
  - DECOR (variant = Index in `furnish/Decor`);
  - FLOOR_COVER und WALL_COVER (`isSurface`, umgehen vacant):
    - Boden-Fliese ersetzt die GRAY_WOOL unter der Gehebene;
    - Teppich und Fußmatte liegen auf Luft über Wolle;
    - Wandfliese ersetzt QUARTZ_BLOCK, variant = Höhe.
- SIDE_CHAIR variant 1 = `dark_oak_chair`.
- `block/RoomDecorBlocks`:
  - 23 `Piece`-Blöcke, deren Form sich mit der Ausrichtung dreht. Fx: DRIP am Spiegel, TICK an der Uhr, BELL (Klingel per Rechtsklick und selten von selbst);
  - Teppiche `executive_rug` und `doormat`;
  - Block `restroom_floor_tile`;
  - Wandfliese: Fliesen nur auf der Nordseite des Modells, sonst Quarz.
- Assets `tools/gen_0230.py`: leuchtende Flächen per `forge_data` block_light, Pflanze mit cutout; Loot `chests/<raum>`; Config-Loot-Defaults.
- Getestet: alle 4 Räume im Spiel (Bilder im Chat), das Pultbuch und das Server-Log.
- Nicht getestet: Sounds, Fallbacks ohne Refurbished, Mehrspieler, Häufigkeiten über große Flächen.
- Hinweis: Die Räume haben **Wollboden** (GRAY_WOOL bei floorY − 1), keinen Teppich. `free()` kennt GRAY_CARPET nur aus älteren Generierungen.

## Version 0.22.1 (Beta) – Belohnung für die Reparatur
- `FuseBoxBlock.reward()`: Loot `gameplay/fuse_box_reward` (Research File + 1 aus Batterie / 2–3 Ingots / Filter / Brille) plus 15–25 XP. Nur wenn `BlackoutEvents.repaired()` true liefert. Getestet. Auslieferung `/home/claude/outputs/v0221/OfficeRooms-0.22.1.zip`.

## Version 0.22.0 (Beta) – Stromausfälle
- Auslieferung `/home/claude/outputs/v0220/OfficeRooms-0.22.0.zip`, Bild `docs/bilder/0220_blackout.jpg`.
- `hazard/`:
  - `BlackoutData` (SavedData „office_rooms_blackout“: nextCycle, phase, phaseEnd, box, name, fuse, fuseFloor).
  - `BlackoutEvents`:
    - `warn` (Box ±radius um einen Spieler, Stockwerke ±storeys), `start` (`placeFuseBox` an der Wand auf Augenhöhe), `end` (Gaunts mit Tag `office_rooms.blackout` entfernen, Kasten reparieren).
    - `gaunts()`: 8 % pro Sekunde, außer Sicht, max. Config-Wert.
    - Sync alle 5 s und beim Login.
  - `BlackoutState` (Client, `dim`).
  - `HazardNet.Blackout` (Paket-ID 2).
- Block `fuse_box` (`block/FuseBoxBlock`, broken + facing, 3 Komponenten).
- Client:
  - `BlackoutClient` (dim 1 → 0,1, Flackern bei Ein-/Austritt, Nachtsicht hebt auf).
  - `OfficeRoomsSkyEffects` multipliziert mit dim; das Brummen skaliert mit dim.
  - `HazardMonitorRenderer.blackoutLines()`: Warnung / im Bereich dunkel / außerhalb Kasten-Etage; wechselt sich mit Hazard alle 4 s ab.
- Config `[blackout]`, Befehl `/officerooms blackout status|warn|start|stop`. Assets `tools/gen_0220.py`.
- Getestet: Warnung, Start, Kasten, Gaunt, Reparatur. Nicht getestet: Außen-Monitor, automatischer Zyklus, Notstrom, Mehrspieler.

## Version 0.21.2 (Beta) – Refurbished-Möbel in Level 0
- Auslieferung `/home/claude/outputs/v0212/OfficeRooms-0.21.2.zip`.
- `Level0ChunkGenerator.refurbish()` in `applyBiomeDecoration`, nur wenn `ModList.isLoaded("refurbished_furniture")`.
- `rf(id, facing)`: Block per Registry, Property „facing“ wird gesetzt, null bei fehlender ID.
- Ersetzt furn 3 (Stühle + Tisch), 4 (Sofa yellow/light_gray, sonst Stuhl) und 9 (Stuhl, oak_drawer, oak_storage_cabinet).
- mods.toml: optionale Abhängigkeit `refurbished_furniture`. Laut Nutzer ist sie im Modpack ohnehin enthalten; die Testumgebung hat sie auch.
- Deckenstühle entfernt (Nutzerwunsch); furn 4 = ein Stuhl mitten im Raum.

## Version 0.21.1 (Beta) – Mehr Atmosphäre in Level 0
- Auslieferung `/home/claude/outputs/v0211/OfficeRooms-0.21.1.zip`.
- Licht: `ambient_light` 0,12; Leuchtenarten 66 % an / 6 % flackernd (`level0_light_flicker`: randomTick → kurze Schübe + AMB_BUZZ) / Rest tot.
- `ceilingAt()`: 6 Leuchtmuster pro Zelle, Dunkelzonen (3×3 Zellen, 9 %), void 2 %, vent 1,5 %, Flecken 3,5 %.
- Wände: worn 5 %, arrow 0,6 %. Teppich: stained 6 %. `outlet()` 3,5 % an Wandseiten.
- `odd()`, furn-Werte 3..9: Stühle zur Wand, Stühle an der Decke, Treppe in die Decke, Tür ins Nichts, hängende Wand, niedrige Decke, Bücherregal + Stuhl.
- Fenster auf Nutzerwunsch entfernt.

## Version 0.21.0 (Beta) – Level 0
- Auslieferung `/home/claude/outputs/v0210/OfficeRooms-0.21.0.zip`, Bild `docs/bilder/0210_level0.jpg`.
- Eingang: oberste Decke der Office Rooms (y ≥ y0 + phys·storeys, Standard 543). Nutzer hat korrigiert: nicht das Maze. Der Abbau wird abgebrochen, der Block bleibt.
- Paket `level0/`:
  - `Level0Layout`, reine Funktionen:
    - Zellen 6, Regionen 16×16 Zellen, 4 Ebenen: Boden 6 + 6k, Gehebene 7 + 6k, Decke 11 + 6k, Höhe 48.
    - Spannbaum + Schleifen; Grenzöffnungen garantiert (Randzellen nie blockiert); Hallen mit Säulen.
    - Threshold 5×4 (k ≥ 1, P 0,22, darunter UNDER offen).
    - Manila 3×3 (P 0,22 pro Region, zufällige Ebene, darunter BLOCKED für das Rätsel).
    - Leitern 3 pro Ebenenpaar und Region.
    - Möbel: Tische (unterlaufbar), Stühle nur in Hallen.
  - `Level0ChunkGenerator`: Blöcke in `buildSurface` (wegen des Seeds); Truhe per Loot-Tabelle in `applyBiomeDecoration`.
  - `Level0Blocks`: wallpaper, carpet, ceiling, light, light_off, manila_wallpaper, manila_carpet, exit (LiquidBlockContainer, damit das Wasser ihn nicht wegspült).
  - `Level0Events`:
    - Schutz (Break, Place, Explosion, Pearl, Chorus, Noclip), Mining Fatigue.
    - Nur der Flayed mit Tag `office_rooms.immortal` darf joinen; nach Neustart wird er übernommen.
    - `LivingAttackEvent` abgebrochen.
    - Jagd: FOLLOW_RANGE 96, `setTarget`; Versetzen bei > 72 Blöcken Abstand oder anderer Ebene (20 s) bzw. ohne Fortschritt (6 s).
    - FillBucket im Schacht füllt das obere Schachtfeld.
    - Rettung aus dem trockenen Schacht nach 8 s.
    - `escape()`: zufälliges Büro 800–3200 Blöcke entfernt; Ticket → 1 Belohnung.
    - `locate()` für den Befehl.
- Rätsel-Geometrie (Manila-lokal bx/bz): Schacht (3,3) W−1/W−2; Gang bz 3, bx 4..8, y W−2; Kammer bx 9..11, bz 2..4, y W−4..W−2; Ausgang bei (11,3).
- Belohnung „Multidimensional Ingots“ = `glitched_ingot` (vorhandenes Item), Schwert = `multidimensional_sword`.
- Assets `tools/gen_0210.py`; 7 Bücher in `stories.py` (47 Bücher); `OutpostNotes.LEVEL0`.
- Getestet: Eingang, Rätsel komplett, Belohnung, Schutz, Unsterblichkeit, Verfolgung.
- Nicht getestet: Suchdauer, Mehrspieler.

## Version 0.20.0 (Beta) – Verlassene Forschungsaußenposten
- Auslieferung `/home/claude/outputs/v0200/OfficeRooms-0.20.0.zip`, Bilder `docs/bilder/0200_*`.
- Paket `outpost/`:
  - `OutpostBlocks` mit allen Blöcken und 2 BE-Typen:
    - Wand, Boden, Decke, Gefahrenboden;
    - PowerLever = Controller-BE;
    - EmergencyButton (Zustand POWERED, 12 Ticks);
    - Monitor + MonitorEntity (phase/start, Sync);
    - Console, Lamp (warning_light, facility_light, LIT);
    - Frame (ACTIVE), Gateway (AXIS, → `UndersidePortalBlock.sendToOffice`).
  - `Controller`: Phasen OFF/BOOT(50)/WARNING(220)/READY/ACTIVATING(170)/ONLINE; Positionslisten im NBT; `near()` sucht ±16.
  - `OutpostBuilder`: Themes LAB/ANCIENT/JUNGLE/CAVE/NETHER; lokale Koordinaten u/v/y, Innenraum u −6..6, v 1..11, y 0..5, Gateway bei v = 10; `tunnel()`, `shaft()`.
  - `OutpostPlacer`: `ChunkEvent.Load` mit `isNewChunk` → Queue, 1 Auftrag pro Tick:
    - Ancient City 0,6 und Jungle 0,4 über `chunk.getAllStarts()`;
    - Höhle 1/1800, Nether 1/500 per Hash.
  - `OutpostNotes`: Bücher je Theme, alle mit Prozedur.
- Client: `OutpostMonitorRenderer`.
- Assets `tools/gen_0200.py`, Sounds `tools/gen_sounds_0200.py`.
- Befehl `/officerooms outpost <theme>`.
- Schreine unverändert.
- Getestet: natürliche Platzierung Ancient/Jungle/Nether, volle Sequenz, Teleport.
- Nicht getestet: natürliche Höhlen-Außenposten, Knopf ohne Strom bzw. während Warnung, Mehrspieler.

## Version 0.19.1 (Beta) – Körperbehaarung für [Unknown]
- `UnknownRenderer`: `hair()`-Sticks (0,4 px dick): 60 je Segment, 7 je Ober- und Unterschenkel, 16 Strähnen am Kopf. Auslieferung `/home/claude/outputs/v0191/OfficeRooms-0.19.1.zip`.

## Version 0.19.0 (Beta) – The Underside
- Auslieferung `/home/claude/outputs/v0190/OfficeRooms-0.19.0.zip`, Bilder `docs/bilder/0190_*`.
- Paket `underside/`:
  - `UndersideTerrain`: Boden y 40–42, Decke 53–55, Bäume und hängende Bäume, reine Funktionen.
  - `UndersideChunkGenerator` (Codec `office_rooms:underside`).
  - `UndersideEvents`:
    - Spieler unter y −72 im Office → `enter()`.
    - 20-s-Timer `ARRIVED`; Beben ab 15,5 s; Spawn des Bosses.
    - Boss weg, wenn 5 s kein Spieler unten ist.
    - `defeated()`: Schwert + 2 Portale.
    - Noclip: `LivingTick` setzt `noPhysics`, `floorClamp` auf dem Client, `unstick` auf dem Server.
  - `Unknown` (Monster, 11 `UnknownPart`, Trail; Modi INTRO/BURROW/TELEGRAPH/LUNGE/CRAWL/DIVE; `shouldBeSaved` = false).
  - `UndersideClientState` + `HazardNet.Quake` (Paket-ID 1).
- Client: `UnknownRenderer` (Vanilla-ModelParts in Code), `UndersideClientFx` (Kamerabeben), Grasfarbe in `ClientSetup`.
- Items: `item/UndersideItems` (6 Artefakte, Schlüssel, Noclip Sword).
- Blöcke: `underside_grass`, `UndersidePortalBlock` (`overworld` true/false).
- Artefakt-Truhen: Loot-Tabellen `*_artifact`, jeweils fest an einer Truhe:
  - Maze: Tresor;
  - Hotel: Tresor, Truhe c[0] = 2;
  - Industrie: Halle 0;
  - Poolrooms: erste Tresortruhe;
  - Neighbourhood: `firstChestHouse()`;
  - Level Fun: `artifactCell()`.
- Assets `tools/gen_0190.py`, Sounds `tools/gen_sounds_0190.py`.
- Befehl `/officerooms underside [now]`.
- 12 neue Bücher in `tools/stories.py` (40 Bücher) mit Hinweisen.
- Nutzerwünsche:
  - Boss erst nach 20 s;
  - Wechsel beim Fall durchs Bedrock ins Void;
  - Boss kommt bei jedem Besuch wieder;
  - Design: Mischung aus den Skizzen A und B;
  - Bücher spiegeln den aktuellen Stand wider und enthalten Hinweise.
- Nicht getestet: fairer Kampf ohne Resistenz, Mehrspieler, Artefakt-Truhen in neu erzeugten Strukturen, Crafting im Raster.

## Version 0.18.1 (Beta) – goto für neue Strukturen
- `/officerooms goto hotel|hotel_vault|industrial|level_fun|hideout|elevator|secure_storage` nutzt `megaHits` (wie locate), nächster Treffer, `safeSpot` (freie 2 Blöcke + fester Boden, ±6). Getestet: alle 7 Ziele, Landeblöcke geprüft. Auslieferung `/home/claude/outputs/v0181/OfficeRooms-0.18.1.zip`.

## Version 0.18.0 (Beta) – Sound-Overhaul
- Auslieferung `/home/claude/outputs/v0180/OfficeRooms-0.18.0.zip` + Hörprobe `/home/claude/outputs/OfficeRooms_0.18.0_sound_preview.mp3`. 20/20 Tests grün.
- `tools/gen_sounds_0180.py` (Bausteine voice/growl/breath/tear/verb; alle Mob-Sounds außer Smiler neu, Varianten; Flayed 4 Schreie 4–6 s, tear; Ambient buzz/vent/pipe/distant, industrial machine/steam, lab hum, hotel far, fun party/musicbox, pool distant). sounds.json 77 Events (Varianten, Pitch 0,9-Duplikate, attenuation_distance).
- Java: neue ModSounds (WOLFSPIDER_STEP/ATTACK, HUNTER_STEP, GAUNT_STEP, FLAYED_TEAR, AMB_*), Schritt-/Angriffssounds, Flayed Reißen bei Treffer + Schrei bei Jagd-Wiederaufnahme (50 %), `AmbientSounds.area()` (Blockscan → INDUSTRIAL/FUN/HOTEL/LAB), Generator-Kern brummt (animateTick).
- Hurt-Sounds tiefer (Spektralschwerpunkt gemessen). Nicht im Spiel gehört (kein Audiogerät) → Nutzer-Feedback zur Hörprobe abwarten.

## Version 0.17.1 (Beta) – Zone nie dort, wo ein Spieler ist
- `HazardEvents.occupied`: Megastruktur-Region wird bei der Zonenwahl übersprungen, wenn ein Spieler darin steht (Band ±12 Blöcke). Getestet: 6 Ziehungen mit Spieler im Maze, nie das Maze. Auslieferung `/home/claude/outputs/v0171/OfficeRooms-0.17.1.zip`.

## Version 0.17.0 (Beta) – HAZARD ZONE
- Auslieferung: `/home/claude/outputs/v0170/OfficeRooms-0.17.0.zip` (docs/Bilder 0.14–0.17). 20/20 Tests grün.
- Paket `hazard/`: `HazardData` (SavedData „office_rooms_hazard“ im Overworld: nextCycle, phase 0/1/2, phaseEnd, box[6], name, pending/placed/toRemove-Kisten), `HazardEvents` (ServerTick alle 20 Ticks: Zyklus über Overworld-GameTime, Zonenwahl = zufällige Megastruktur in ±8 Regionen um zufälligen Spieler, Box ±44 um Regionsmitte über volle Bandhöhe; Vergiftung in PersistentData „office_rooms_tox“ (+2/s, ab 20 Übelkeit, ab 40 Schwäche + Schaden, tödlich), Filter-/Akkuverbrauch, Detektor-Actionbar, 8 Kisten lazy beim Laden, Kreaturen mit Tag `office_rooms.hazard`, Aufräumen beim Ende), `HazardNet` (SimpleChannel office_rooms:main, Sync-Paket alle 5 s/Login/Dimensionswechsel), `HazardState` (Client-Kopie).
- Client: `client/HazardMonitorRenderer` (BER, Text je Phase, Flackern, kaputte Monitore), `client/HazardClientFx` (grüne Partikel + Nebel in der Zone, Zischen). Block `hazard_monitor` (`block/HazardMonitorBlock` + BE, Property broken, Piepen per animateTick), `ModBlocks.monitor(facing, broken)`.
- Monitore: Hideout Security/Labor, Secure-Storage-Raum (auf Fass), Industrie-Kontrollräume, Hotel-Rezeption, Büros 15 % / Gänge 6 % (furnish `Item.MONITOR`, variant 1 = kaputt; Nutzer: „nicht in jedem Büro, ausgewogen“).
- Items: `HazardHeadgear` (gas_mask Filter 600 s, night_vision_goggles Akku 1200 s, hazard_visor beides; NBT FilterUsed/ChargeUsed, unzerstörbar, eigene Armor-Layer), gas_filter (Rechtsklick), toxic_crystal (nur Event), special_event_detector. Akku lädt getragene Brille (ToolItems Batterie). Loot `hazard_cache`/`hazard_core` + Zusätze; Rezepte Brille, Filter, Monitor, Visor. Assets `tools/gen_0170.py`.
- Config `[hazard_zone]` (enabled, firstWarningAfter 20, cycleMinutes 100, warningMinutes 15, activeMinutes 45, radius 44, searchRegions 8, chests 8). Befehl `/officerooms hazard status|warn|start|stop`.
- Audio (Client-Config): `lampHumLevel` 0,85 (vorher lampHumVolume 0,35), `musicLevel` 0,2.
- Getestet: Monitore idle/Warnung/aktiv, Vergiftung tödlich, Maske schützt, 8 Kisten, Kreaturen, Detektor, Brillenakku, Aufräumen. Nicht: voller 100-min-Zyklus, Mehrspieler, Visor-Crafting, Akku-Laden, Sounds, Shader.

## Version 0.16.1 (Beta) – Spielteppich, Hüpfburgen, Spawn-Test
- Auslieferung: `/home/claude/outputs/v0161/OfficeRooms-0.16.1.zip` (docs/Bilder 0.14–0.16.1). 20/20 Tests grün.
- Level Fun: Boden `play_carpet` (32×32-Textur: rot, Straßen mit gelben Strichen, kleine Autos; Nutzer-Referenz). Zelltyp CASTLE (~7 %): Hüpfburg 6×6 aus `inflatable_red/blue/yellow` (nur Deko). Assets am Ende von `tools/gen_0160.py`.
- Natürlicher Spawn geprüft (Spawnchancen von Warden/Colony/Flayed/Observer nur in der Test-Config auf 1,0, danach zurückgesetzt): alle Kreaturen spawnen, Gaunts in den dunklen Geschossen (Gehebene y −49), Hunter in den Poolrooms, Partygoer in Level Fun.

## Version 0.16.0 (Beta) – Dunkler + Level Fun
- Dunkler: `dimension_type` `ambient_light` 0.0 (war 0.07); `client/OfficeRoomsSkyEffects.adjustLightmapColors` Faktor 0,35 + 0,65·(Licht/15)^1,6.
- `megastructure/LevelFun` (Kind LEVEL_FUN: u in [4c, 5c); 1 Geschoss, Zellen 10×10, Säulen an Ecken, Wände 55 % mit 2er-Lücke, Zelltypen PLAIN/TABLE/BALLOONS/GIFTS/CONFETTI/SMILE/CASTLE), `LevelFunBlocks` (States + Geschenk-Kiste `level_fun_gift`). Blöcke: fun_wallpaper, balloon_*, streamer, confetti, gift_box, party_table, smile_graffiti.
- `entity/Partygoer` (eigenes Design, `tools/gen_partygoer_geo.py`: gelb, Smiley, Partyhut). Approach bis 7,5 Blöcke, Angriff bei < 6 oder Treffer, Speed 0,36, Schaden 6. Sounds `tools/gen_sounds_0160.py`. Spawner `MobSpawner.levelFun`.

## Version 0.15.0 – Industrial Complex; 0.14.0 – Abandoned Hotel
- `megastructure/Industrial` + `IndustrialBlocks` (5 Ebenen, Zellen 16×16, Rohre, Gittertüren `grate_door`, 2 Generatorhallen, Flayed + Hounds).
- `megastructure/Hotel` + `HotelBlocks` (Keller mit Tresor, Lobby, 2 Gästeetagen, Hounds). Maze: Flayed statt Hunter.

## Version 0.13.0 – The Flayed; 0.12.0 – Research & Labore; 0.11.0 – GeckoLib-Mobs, versteckte Räume
- Flayed `entity/Flayed` (drahtdünn, schwarz, Kritzel-Kopf; Tempo-Multiplikator 1,75 ≈ 7,6 Bl./s; Config `[flayed]`).
- Research: `research/*`, Herzen in PlayerPersisted, Reforge-NBT `OfficeReforge`, Deko-Blöcke `block/DecorBlock`.
- GeckoLib Pflicht (`libs/geckolib-forge-1.20.1-4.8.4-0.jar`, nie ausliefern), `client/GeoMobRenderer`, `tools/geo_lib.py`.

## Hinweise für die Umgebung
- Projekt: `/home/claude/src0/OfficeRooms`. Produktion: `bash /home/claude/scratch/prodclient.sh [fresh]` (kopiert OfficeRooms-0.36.1.jar). Bildbereich (213,120)-(1067,600). `gamerule sendCommandFeedback false` vor Fotos. Raumansicht: `/officerooms goto <raumtyp>`, dann `/officerooms find <block> 48` (mit `execute as Dev at @s`); Skript `/tmp/around.sh` (4 Ansichten um einen Block).
- Test-Config `/home/claude/prodtest/config/office_rooms-common.toml` (`[spawning] enabled = true`, Standard-Chancen); neue Welt = Easy → `difficulty normal`. Common-Config nur per Neustart.
- Gehebenen: y0 = minY + 1 = −63; Geschoss s: Platte −63 + 6s, Wolle +1, Gehebene +2 (s = 2 → −49).
- Vor Neustart `save-all flush`. Startet der Dev-Client nicht: `run/config/fml.toml` löschen.
- Test-Orte (Seed 4711, frische Welt): Level Fun −80 251 −240 (Hüpfburg-Ecke −138 251 −258), Aufzug 58 299 −34, Poolrooms 348 431 −187.
- RCON-Zählung: `execute in office_rooms:office_rooms positioned X Y Z if entity @e[type=…,distance=..90]` → „count: n“.
- Neue Megastruktur: Kind in `Megastructures.kindOf` (nächstes: u < 6c), Site.part(), Generator-Füllung in `OfficeChunkGenerator`, `MegaDecorator.decorate`, Overlaps in RoomFurnisher/SpecialRooms/SpecialType, `OfficePlanner.megaContains`, Atmosphere, Locate, MobSpawner, Unit-Test. SpecialRoomsTest prüft 5×5 Regionen.
- Testrechner 2 Kerne / 8 GB; nie bauen, während Server/Client laufen (`pkill -9 -x java`).
