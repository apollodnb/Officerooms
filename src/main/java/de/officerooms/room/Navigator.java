package de.officerooms.room;

import de.officerooms.room.connection.Openings;

/**
 * Sucht geeignete Zielpunkte in der Planung (für Befehle): einen sicheren Ankunftsraum und das
 * nächstgelegene Atriumfenster. Rein planbasiert, lädt keine Chunks.
 */
public final class Navigator {

    /** Zielpunkt: Blockmitte x/z, Laufhöhe y, Blickrichtung yaw (Minecraft-Konvention). */
    public record Target(double x, double y, double z, float yaw, String description) {
    }

    private Navigator() {
    }

    /** Raum im Bodengeschoss eines erreichbaren Büroraums nahe (x, z) und Geschoss s. */
    public static Target findRoom(OfficePlanner p, int x, int z, int preferredStorey) {
        int rx = p.regionOf(x);
        int rz = p.regionOf(z);
        RegionLayout l = p.layout(rx, rz);
        int cx = Math.max(0, Math.min(p.ru - 1, (x - rx * p.regionBlocks) / p.unit));
        int cz = Math.max(0, Math.min(p.ru - 1, (z - rz * p.regionBlocks) / p.unit));
        for (int ds = 0; ds < p.storeys; ds++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int s = preferredStorey + ds * sign;
                if (s < 0 || s >= p.storeys) {
                    continue;
                }
                for (int r = 0; r < p.ru; r++) {
                    for (int uz = cz - r; uz <= cz + r; uz++) {
                        for (int ux = cx - r; ux <= cx + r; ux++) {
                            if (Math.max(Math.abs(ux - cx), Math.abs(uz - cz)) != r || ux < 0 || uz < 0
                                    || ux >= p.ru || uz >= p.ru) {
                                continue;
                            }
                            Room room = l.room(s, ux, uz);
                            if (room != null && room.kind == Room.Kind.ROOM && room.bottom == s) {
                                return new Target(rx * p.regionBlocks + ux * p.unit + p.unit / 2 + 0.5, p.floorY(s) + 1,
                                        rz * p.regionBlocks + uz * p.unit + p.unit / 2 + 0.5, 0f, room.toString());
                            }
                        }
                    }
                }
                if (ds == 0) {
                    break;
                }
            }
        }
        return null;
    }

    /**
     * Nächstes Fenster zu einem Atrium mit mindestens {@code minClear} Blöcken lichter Höhe
     * (Suchradius in Regionen). Standpunkt im Raum, Blick zum Fenster.
     */
    public static Target findWindow(OfficePlanner p, int x, int y, int z, int regionRadius, int minClear) {
        int prx = p.regionOf(x);
        int prz = p.regionOf(z);
        Target best = null;
        double bestD = Double.MAX_VALUE;
        for (int rx = prx - regionRadius; rx <= prx + regionRadius; rx++) {
            for (int rz = prz - regionRadius; rz <= prz + regionRadius; rz++) {
                RegionPlan plan = p.plan(rx, rz);
                RegionLayout l = plan.layout;
                int n = plan.edgeCount();
                for (int key = 0; key < n; key++) {
                    int code = plan.edgeByKey(key);
                    if (Openings.type(code) != Openings.WINDOW) {
                        continue;
                    }
                    int axis = key & 1;
                    int cell = key >> 1;
                    int s = cell / (p.ru * p.ru);
                    int c = cell % (p.ru * p.ru);
                    int ux = c % p.ru;
                    int uz = c / p.ru;
                    Room b = l.room(s, ux, uz);
                    boolean roomIsB = b != null && b.isWalkableRoom();
                    Room atriumRoom = roomIsB
                            ? (axis == 0 ? l.room(s, ux - 1, uz) : l.room(s, ux, uz - 1)) : b;
                    if (atriumRoom == null || atriumRoom.atrium == null || atriumRoom.atrium.clear < minClear) {
                        continue;
                    }
                    int mid = Openings.start(code) + Openings.width(code) / 2;
                    double wx;
                    double wz;
                    double sx;
                    double sz;
                    float yaw;
                    if (axis == 0) {
                        wx = rx * p.regionBlocks + ux * p.unit;
                        wz = rz * p.regionBlocks + uz * p.unit + mid + 0.5;
                        sx = roomIsB ? wx + 2.5 : wx - 1.5;
                        sz = wz;
                        yaw = roomIsB ? 90f : -90f;
                    } else {
                        wz = rz * p.regionBlocks + uz * p.unit;
                        wx = rx * p.regionBlocks + ux * p.unit + mid + 0.5;
                        sz = roomIsB ? wz + 2.5 : wz - 1.5;
                        sx = wx;
                        yaw = roomIsB ? 180f : 0f;
                    }
                    double sy = p.floorY(s) + 1;
                    double d = (sx - x) * (sx - x) + (sz - z) * (sz - z) + 4 * (sy - y) * (sy - y);
                    if (d < bestD) {
                        bestD = d;
                        best = new Target(sx, sy, sz, yaw, atriumRoom.atrium.toString());
                    }
                }
            }
        }
        return best;
    }
}
