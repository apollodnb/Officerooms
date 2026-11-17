package de.officerooms.furnish;

/**
 * Ein geplanter Einrichtungsgegenstand in Weltkoordinaten.
 *
 * @param item    Gegenstand
 * @param x       Hauptblock X
 * @param y       Hauptblock Y (Laufebene, bei Deckenleuchten der oberste Innenraumblock)
 * @param z       Hauptblock Z
 * @param facing  Blickrichtung der Vorderseite: 0 = Ost (+x), 1 = Süd (+z), 2 = West, 3 = Nord
 * @param variant Variante (z. B. Farbe); bei Leuchten 1 = an, 0 = aus
 * @param loot    Loot-Tabelle für Container (Pfad unter office_rooms:chests/) oder null
 */
public record Placement(Item item, int x, int y, int z, int facing, int variant, String loot) {

    public boolean lit() {
        return variant != 0;
    }
}
