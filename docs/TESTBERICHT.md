# Testbericht (Phase 1: 0.1.0, Phasen 2 und 3: 0.3.0)

Stand: 2026-11-08. Alle Tests liefen in einer Linux-Umgebung mit Netzzugang (Gradle, Forge-Maven, Mojang).

## 1. Build

| Schritt | Ergebnis |
|---|---|
| `./gradlew build` der 0.0.1-Basis (ForgeGradle 6, Forge 47.4.28, JDK 17) | ✅ erfolgreich |
| `./gradlew build` von 0.1.0 inkl. `reobfJar` und Unit-Tests | ✅ erfolgreich, 8/8 Tests grün |
| `python3 tools/validate_data.py` | ✅ alle Prüfungen bestanden |
| Compiler-Warnungen | 3× `deprecated for removal` (Forge markiert `ResourceLocation(String,String)` und `FMLJavaModLoadingContext.get()`). In Forge 47 / 1.20.1 sind das die vorgesehenen APIs. Keine Funktionsauswirkung. |

## 2. Unit-Tests (`PlannerTest`, reines Java, ohne Spielstart)

| Test | Prüft | Ergebnis |
|---|---|---|
| `verticalGridFitsDimension` | 204 Geschosse passen in Y −64…959, Abschlussdecke innerhalb der Bauhöhe | ✅ |
| `deterministicAcrossInstancesAndOrder` | zwei Planer mit gleichem Seed liefern identische Spalten, auch wenn vorher andere Regionen berechnet wurden; anderer Seed liefert andere Welt | ✅ |
| `blockRulesHoldAcrossRegionBoundaries` | 3 Seeds × 144 × 144 × 1024 Blöcke über eine Ecke von 4 Regionen: Fenster nur Raum↔Atrium, jede Wandöffnung führt beidseitig in Luft eines erreichbaren Raums, keine Luft in massiven/versiegelten Zellen oder der Abschlussdecke | ✅ 63,7 Mio. Blöcke, 0 Fehler, 8 505 Glasblöcke, ~500 000 Türluftblöcke |
| `windowsOnlyIntoAtriaWholeRegion` | dasselbe für eine ganze Region (26,2 Mio. Blöcke) | ✅ 0 Fehler, 4 670 Glasblöcke |
| `roomHeightDistributionMatchesTarget` | Höhenverteilung über 9 Regionen | ✅ 90,18 / 6,91 / 1,94 / 0,97 % bei 157 898 Räumen |
| `atriaAreEnclosedAndVaried` | Abstand zum Regionsrand, Geschoss darüber/darunter, Höhe passt zur Geschosszahl; Vielfalt | ✅ 164 Atrien in 16 Regionen, alle 7 Klassen, max. 214 Blöcke, 144 Galerieebenen |
| `globalConnectivityAcrossRegions` | alle offenen Räume, Korridore und Treppen in 4 × 4 Regionen über interne und Grenztüren verbunden | ✅ 315 526 Räume in **einer** Komponente, 304 versiegelt (0,096 %) |
| `floorAndCapAreSolid` | Bedrock unten, keine Luft in der Abschlussdecke | ✅ |

## 3. Dev-Server (`./gradlew runServer`, headless, Befehle über RCON)

| Test | Ergebnis |
|---|---|
| Serverstart, Mod lädt, Dimension mit eigenem Generator und vollständigen JSON-Einstellungen wird gelesen (Codec) | ✅ |
| `/officerooms verify 2` bei 0/0: 25 Chunks, **echte Weltblöcke** gegen Planung + Regeln | ✅ 6,55 Mio. Blöcke, 0 Abweichungen, 0 Fehler |
| `/officerooms verify 2` bei 160/160 (Ecke von 4 Regionen) | ✅ 0 Abweichungen, 0 Fehler |
| `/officerooms verify 6` bei 600/−400: 169 Chunks | ✅ 44,3 Mio. Blöcke, 0 Abweichungen, 0 Fehler, 6 729 Glasblöcke. Generierung 5,8 s (≈ 34 ms/Chunk) |
| `/officerooms stats` | ✅ plausible Kennzahlen (siehe RAUMHOEHEN.md, Abschnitt 6) |

## 4. Produktionsserver (echte Forge-Installation mit der gebauten JAR)

Forge 47.4.28 mit dem offiziellen Installer installiert (Eclipse Temurin 17), `OfficeRooms-0.1.0.jar` (reobfuskiert) in `mods/`.

| Test | Ergebnis |
|---|---|
| Start, Mod lädt, Dimension registriert | ✅ |
| `verify 3` bei 0/0 (49 Chunks) und `verify 2` bei −150/330 (25 Chunks) | ✅ 19,4 Mio. Blöcke, 0 Abweichungen, 0 Fehler |
| `save-all`, Neustart, erneut `verify 3`: Chunks werden aus den Regionsdateien geladen, nicht neu generiert | ✅ identisch, 0 Fehler |

Hinweis: Der Forge-Installer schlug mit dem Ubuntu-OpenJDK 17 an einer Prüfsumme fehl (System-zlib). Mit Temurin 17 lief er fehlerfrei. Das betrifft nur die Installation von Forge, nicht den Mod.

## 5. Client im Spiel (`./gradlew runClient`, Xvfb + Mesa llvmpipe, mit dem Dev-Server verbunden)

| Test | Ergebnis |
|---|---|
| Client startet, tritt dem Server bei, Dimension wird betreten (`/officerooms enter`) | ✅ Büroraum, 4 Blöcke hoch, Türöffnungen (Bild 01) |
| `/officerooms window`: Blick durch 2×2-Fenster in kleines Atrium | ✅ Gegenseite mit Fenstern und Durchgängen sichtbar (Bild 02) |
| `/officerooms window 150`: 3×3-Fenster auf Galerie eines 155 Blöcke hohen Atriums | ✅ Galerie, Brüstung, Gegenwand mit vielen Fenstern (Bild 03) |
| Standpunkt auf der Galerie | ✅ Brücke, Galerieebenen, Fenster auf allen Höhen (Bild 04) |
| Mega-Atrium 112 × 100 × 221 Blöcke, Blick nach oben und zur Gegenseite | ⚠️ → ✅ Im ersten Durchlauf war außerhalb der Server-Sichtweite (6 Chunks) **schwarzer End-Hintergrund** zu sehen (Bild 07). Behoben durch eigene Dimensionseffekte mit hellem Dunst; erneut getestet (Bilder 05, 06). |
| Abschluss oben bei 155-Block-Atrium | ✅ Decke geschlossen, sobald alle Chunk-Abschnitte gerendert sind. Kurzzeitig sichtbare Lücken waren nur noch nicht kompilierte Geometrie (Software-Renderer). |

Einschränkung der Testumgebung: Bei Sichtweite 12 wurde der Client vom Speicherlimit der Testumgebung beendet (8 GB für Server, Client und Software-Renderer zusammen). Mit Sichtweite 10 lief er stabil. Das sagt wenig über echte Hardware aus, zeigt aber: Die Dimension ist auf ganzer Höhe bebaut (64 Sektionen pro Chunk) und damit für den Client schwerer als die Overworld.

## 6. Phase 1: noch ausstehend / nicht getestet

| Punkt | Warum offen |
|---|---|
| **Einzelspielerwelt über das Menü anlegen** (integrierter Server) | Getestet wurde Client + dedizierter Server. Der Generator ist identisch, der Weg über das Menü wurde aber nicht durchgeklickt. |
| **Produktions-Client** (Minecraft-Launcher mit Forge-Profil und reobf-JAR) | Der Client wurde nur in der Entwicklungsumgebung getestet. Die Client-Klassen sind klein (Dimensionseffekte) und werden wie der Serverteil reobfuskiert, der in Produktion getestet ist. |
| **Echte GPU, längere Spielsitzung, Überlebensmodus** | Nur Software-Rendering. Bildrate, Chunk-Ladegefühl beim Laufen und Speicherverhalten über Stunden sind nicht gemessen. |
| **Mehrere Spieler gleichzeitig** | nicht getestet |
| **Andere Konfigurationswerte** als die ausgelieferten (z. B. `unit_size` 5, andere `base_height`, höhere Dimension) | Codec und Validator decken sie ab, generiert und geprüft wurde aber nur die Standardkonfiguration. |
| **Bestehende Welt aus 0.0.1** | nicht getestet; Verhalten laut Minecraft-Logik siehe README (alte Chunks bleiben flach) |
| **Kompatibilität mit anderen Mods** | nicht getestet |


---

# Phasen 2 und 3 (Version 0.3.0)

Stand 2026-11-08. Umgebung wie oben: Dev-Server und Dev-Client (Xvfb, Software-Rendering) mit OfficeCraft 1.0.3, Refurbished 1.0.20 und Framework 0.7.15 aus dem Quellcode, sowie ein Produktionsserver (Forge 47.4.28 Installer, Temurin 17). Eingaben im Client erfolgten über echte Tastendrücke (xdotool), Befehle über RCON. Ton wurde über den OpenAL-Soft-Dateiausgang aufgezeichnet und ausgewertet.

## 7. Build und Unit-Tests

| Test | Ergebnis |
|---|---|
| `./gradlew build` 0.3.0 inkl. reobf | ✅ |
| `PlannerTest` (inkl. Bürofenster: Glas nur Raum↔Atrium oder Raum↔Raum) | ✅ 8/8 |
| Inhalt der JAR | ✅ nur `de/officerooms/**`, eigene `assets/office_rooms`, `data/office_rooms`, `META-INF`, `pack.mcmeta`; keine fremden Klassen, Texturen, Modelle oder Sounds |

## 8. Weltgenerierung mit Möbeln und Licht

| Test | Ergebnis |
|---|---|
| `verify` auf dem Produktionsserver (9 Chunks, 2,36 Mio. Blöcke) mit allen drei Möbel-Mods | ✅ 0 Abweichungen, 0 Fehler, 322 Glasblöcke in Bürofenstern |
| `verify` im Dev-Client nach Lazy-Einrichtung (Möbel tatsächlich gesetzt) | ✅ 0 Abweichungen, 0 Fehler. Möbelblöcke stehen nur dort, wo der Plan Luft vorsieht, und nicht in Türen oder vor Fenstern. |
| `stats` Region 0,0 | 13 Atrien (mehrere getrennt, 13–129 Blöcke hoch), 17 594 Räume, 1 316 Fenster |
| Ohne Möbel-Mods (Vanilla-Ersatz), 5 Raumtypen besucht, je `verify` | ⚠️ → ✅ Erster Lauf: 5 Fehler. Ersatzstühle (Quarztreppen) hatten exakt den Blockzustand der Architekturtreppen, und der Prüfer hielt sie für Treppen. Ersatzstuhl auf glatte Quarztreppe umgestellt, neu getestet: 5 × 0 Abweichungen, 0 Fehler. |
| Sichtprüfung Großraum, Büro, Besprechung, Pausenraum, Serverraum, Archiv, Atrium, Bürofenster | ✅ Bilder 10–14. Refurbished-Küchenzeile, Mikrowelle und Kühlschrank zeigen in den Raum, OfficeCraft-Stühle stehen an den Tischen. |
| Speicher | ⚠️ → ✅ Möbel bei der Generierung: Server-OOM (über 4 000 Block-Entities pro Chunk-Säule). Seither werden Möbel lazy in Spielernähe gesetzt, ohne OOM. |
| OfficeCraft-Absturz beim Chunk-Meshing (`ConcurrentModificationException` in `Catalog$Entry.shape`, Fehler in OfficeCraft) | ⚠️ → ✅ Umgehung: Cache wird beim Start auf dem Hauptthread vorbefüllt. Danach kein Absturz mehr. |

## 9. Loot

| Test | Ergebnis |
|---|---|
| Container bekommen beim Setzen ihre Tabelle (`data get block`) | ✅ 12 Container im Umkreis, je `office_rooms:chests/<raumtyp>` |
| 1 000 Würfe (`/loot spawn`) über 4 Raumtabellen | ✅ 76 × 1 Pilzsuppe, 924 × leer (7,6 % bei eingestellten 8 %), kein anderer Gegenstand |
| Refurbished-Kühlschrank im Spiel geöffnet | ✅ Tabelle wird beim Öffnen gewürfelt und entfernt (Ergebnis: leer, Bild 18) |

## 10. Kreaturen

| Test | Ergebnis |
|---|---|
| Natürliches Spawnen Wolfspider | ✅ gemessen 1 pro 18,6 Räume (Ziel 20). Kreaturen erscheinen auf dem Teppich, nicht in Wänden oder Möbeln. |
| Wolfspider greift an | ✅ ohne Schutz Spieler getötet („Dev was slain by Wolfspider“) |
| Wither | ✅ nach Treffer `minecraft:wither`, Stufe I, 6 s (119 Ticks Rest gemessen) |
| Heulen | ✅ `entity.wolfspider.howl` im Client-Log |
| Modelle | ✅ Wolfspider schwarz mit Wolfskopf (Bild 16), Headless Warden ohne Kopf (Bild 15) |
| Warden, Spieler steht still, Warden 4–10 Blöcke entfernt, auch in direkter Sicht | ✅ keine Reaktion |
| Warden, Spieler schleicht 2 s | ⚠️ → ✅ erster Lauf: erkannt. Vanilla meldet auch beim Schleichen Schritte; Filter über `ignore_vibrations_sneaking` ergänzt. Erneut getestet: keine Reaktion. |
| Warden, Spieler springt, Warden 9 Blöcke entfernt **hinter einer Wand** | ✅ Geräusch registriert, Warden läuft zur Geräuschquelle (bis an die Wand) |
| Warden, Spieler springt, Warden ca. 9 Blöcke entfernt, gleicher Raum | ✅ Warden läuft hin und beginnt die Jagd |
| Warden, Spieler geht 1,2 s | ✅ beide Warden (einer davon hinter der Wand) jagen den Spieler |
| Schrei beim Jagdbeginn, Herzschlag in der Nähe | ✅ beide im Client-Log (`[Audio]`) |
| Produktionsserver: beide Kreaturen per `summon` | ✅ |

Hinweis: Ein erster Sprungtest war ungültig, weil der simulierte Tastendruck zu kurz war und der Spieler gar nicht sprang. Er wurde mit gehaltener Taste und gemessener Spielerhöhe wiederholt (Ergebnis oben).

## 11. Audio (aufgezeichnet)

| Test | Ergebnis |
|---|---|
| Musik in der Dimension | ✅ ca. −22 dB RMS, in der Overworld Stille |
| Musik nach Verlassen und erneutem Betreten | ✅ startet wieder (anfangs fehlerhaft: Minecraft stoppt beim Dimensionswechsel alle Sounds; behoben) |
| Kein Neustart bei Chunk- und Geschosswechsel | ✅ 4 Teleports über bis zu 400 Blöcke und 3 Geschosse: kein neuer Musikstart im Log |
| Lampensummen hörbar | ✅ Musik per `/stopsound` stumm: −34 dB RMS, deutliche Spitzen bei 100/200/300 Hz (+45 dB gegenüber Nachbarfrequenzen) |
| Musik im Kreativmodus | ⚠️ → ✅ Vanilla ersetzt dort die Biom-Musik; jetzt eigene Schleife mit unterdrückter Vanilla-Musik |

## 12. Nicht getestet (0.3.0)

| Punkt | Bemerkung |
|---|---|
| Produktions-**Client** (Launcher + Forge-Profil) | nur Dev-Client getestet; Produktionsserver lief mit der gebauten JAR |
| Einzelspieler über das Menü | nicht durchgeklickt |
| Geräusche durch Türen, Blockabbau und -setzen, Rennen, Container | Code vorhanden (Tabelle in KREATUREN_UND_AUDIO.md), im Spiel einzeln nicht ausgelöst |
| Warden-Schaden gegen ungeschützten Spieler | Tests liefen mit Resistenz; Schadenswert nur aus der Konfiguration |
| Lautstärke von Schrei und Herzschlag in der Aufnahme | nur im Log nachgewiesen, nicht ausgemessen |
| Lange Spielsitzung, echte GPU, mehrere Spieler | nicht getestet |
| Andere Versionen der Möbel-Mods | nur die oben genannten |


---

# Phase 4 (Version 0.4.0)

Stand 2026-11-09. Gleiche Umgebung wie oben (Dev-Server und Dev-Client mit OfficeCraft, Refurbished und Framework; Produktionsserver Forge 47.4.28). Im Client waren Eingaben echte Tastendrücke und Mausklicks (xdotool), Befehle liefen über RCON.

## 13. Build und Unit-Tests

| Test | Ergebnis |
|---|---|
| `./gradlew build` 0.4.0 | ✅ |
| `PlannerTest` | ✅ 8/8 |
| `SpecialRoomsTest.specialRoomsAppearRegularly` (9 Regionen, 156 044 Büros) | ✅ Outposts 479, Lush 278, Wasser 509, Sculk 78; deterministisch |
| `SpecialRoomsTest.bottomFiveStoreysAreCompletelyDark` (4 Regionen) | ✅ 1 935 Räume in Geschoss 0–4 ohne Licht, keine Leuchte unter Y −38 |
| JAR-Inhalt | ✅ nur eigene Klassen und Ressourcen |

## 14. Weltgenerierung

| Test | Ergebnis |
|---|---|
| `verify` 7×7 Chunks (Dev) | ✅ 0 Abweichungen, 0 Fehler, 268 668 Spezialraum-Blöcke, 18 412 Wasserblöcke, alle in Wasser- oder Dorfräumen |
| `verify` 9×9 Chunks an zwei weiteren Stellen | ✅ 0 Fehler |
| Erster Lauf | ⚠️ → ✅ 1 Fehler: Ein Hebel eines Wasserraums stand vor dem Durchgang zweier anderer Räume. Bedienelemente werden jetzt nur an freien Stellen gesetzt. Neu getestet: 0 Fehler. |
| Produktionsserver mit Möbel-Mods, `verify` 7×7 | ✅ 0 Fehler |
| Produktionsserver ohne Möbel-Mods, `verify` 7×7 | ✅ 0 Fehler |
| Generierungszeit (CPU je Chunk, bereits geplante Regionen) | Füllen 15 ms, Licht 9 ms, Spezialräume 7,5 ms |

## 15. Spezialräume im Spiel

| Test | Ergebnis |
|---|---|
| Villager Outpost: Dorfbewohner, Golem | ✅ 4 Dorfbewohner (Bauer, Kartograf, Werkzeugschmied, Bibliothekar), 1 Eisengolem (Bild 20) |
| Betten und Arbeitsplätze | ⚠️ → ✅ Erster Lauf: Betten wurden nie gesetzt (Wegprüfung zählte einen Knopf als Hindernis). Behoben. Danach haben alle 4 `home` und `job_site` im Gedächtnis, plus Glocke als Treffpunkt. |
| Schlafen | ✅ Nachts (`time set 13500`) schlafen alle 4 in ihren Betten |
| Handel | ✅ Handelsfenster geöffnet, echter Tausch 24 Papier → 1 Smaragd, Erfahrung des Dorfbewohners steigt (Bild 28) |
| Bett durch Spieler benutzt | ✅ Keine Explosion (`bed_works` jetzt true), kein Spawnpunkt gesetzt |
| Water Office | ✅ Hebel geöffnet, Tür offen, ins Wasser geschwommen (Luft sinkt). `verify` danach: kein Wasser außerhalb (Bild 22). |
| Sculk Office | ✅ Genau 1 Headless Warden mit Markierung, `PersistenceRequired` = 1. Truhe mit Spezialtabelle vorhanden (Bild 23). |
| Lush Cave | ✅ Bäume, Moos, Ranken, überwucherte Schreibtische, Truhe (Bild 21) |
| Dunkle unterste Ebenen | ✅ Besprechungsraum auf Y −52 ohne Leuchte (Bild 29) |

## 16. Kreaturen

| Test | Ergebnis |
|---|---|
| Wolfspider-Rate (Ziel 1/40) | 5 Wolfspider auf 250 Räume über 5 Messungen im Minutenabstand, also 1 pro 50. Vorher (Ziel 1/20) 1 pro 18,6. Bei so kleinen Zahlen schwankt das stark. |
| Freundliche Wolfspider zähmen | ✅ Pilzsuppe gefüttert, Besitzer gesetzt, Fortschritt „Best Friends Forever“, Schüssel zurück |
| Sitzen / Aufstehen | ✅ Rechtsklick wechselt `Sitting` 1 → 0 → 1 → 0 |
| Folgen | ✅ Spieler 12 Blöcke weg teleportiert: Wolfspider folgt bzw. teleportiert zum Besitzer |
| Dauerhaft gezähmt | ✅ Nach einem (unsauberen) Server-Neustart noch da, Besitzer erhalten |
| Weiße Augen | ✅ (Bild 25) |
| Headless Warden neues Modell | ✅ kopflos, mehrere Arme/Beine, dunkel, Haltung wechselt sprunghaft (Bild 24) |
| Glitched Ingot | ✅ 100 Warden getötet: 47 Barren (erwartet 50) |

## 17. Items, Crafting, Loot

| Test | Ergebnis |
|---|---|
| Mandelwasser | ✅ Hunger 0 → 20, Glasflasche zurück, keine Effekte |
| Crafting im Spiel | ✅ Multidimensional Sword aus 2 Barren + Stab am Werkbank-GUI hergestellt (Bild 27) |
| Werte | ✅ gemessen: Schwert 9,6, Spitzhacke 7,6, Axt 12, Schaufel 7,8; volle Rüstung 24 Punkte, Härte 14,4, Rückstoß 0,48 |
| Rüstung sichtbar | ✅ eigene Textur am Spieler (Bild 30) |
| Sculk-Spezialtruhe, 300 Würfe | ✅ nie leer; Multidimensional-Teile in 16 von 300, Glitched Ingot 6×, Stab 14×, kein Holz |
| Standardbüro, 400 Würfe | ✅ 268 leer; nur common/uncommon/rare, kein Spezialloot, kein Holz |
| Lush, 200 Würfe | ✅ Holz (Stämme 49, Setzlinge 25, Stöcke 30), Spezialloot selten |
| Gegenstände nach Neustart | ✅ Inventar mit Multidimensional-Ausrüstung erhalten |

## 18. Nicht getestet (0.4.0)

| Punkt | Bemerkung |
|---|---|
| FPS-Messung | Nur Software-Rendering. Die Einsparung ist aus der Planung gezählt (27 % weniger OfficeCraft-Objekte durch eigene Leuchten, ~20 % weniger Arbeitsplätze), nicht mit einer GPU gemessen. |
| Anteil freundlicher Wolfspider (5 %) | nicht statistisch gemessen; die Auswahl ist ein Zufallswurf pro Spawn |
| Dorf: Golem-Spawn durch Dorfbewohner, Zucht, Raids | nicht geprüft (Raids sind in der Dimension aus) |
| Alle Rezepte im GUI | nur das Schwert im GUI gefertigt; die anderen Rezepte geladen (`/recipe give`), nicht einzeln gefertigt |
| Verzaubern am Tisch, Amboss-Reparatur | nicht im Spiel ausprobiert |
| Produktions-Client, lange Sitzung, Mehrspieler | nicht getestet |
| Testumgebung | Der Dev-Client wurde zweimal vom Speicherlimit der Sandbox beendet (Server + Client + Gradle zusammen). Danach lief er mit kleinerem Heap stabil. |
