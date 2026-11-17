package de.officerooms.room.connection;

/**
 * Eine geplante Verbindung zwischen zwei Knoten (Räume, Korridore, Treppenhäuser, Atrium-Ebenen).
 *
 * @param nodeA  Knoten A
 * @param nodeB  Knoten B
 * @param edges  alle gemeinsamen Zellkanten (Kantenschlüssel), auf denen eine Öffnung möglich ist
 * @param stairs true für die festen Treppenhaus-Anschlüsse
 */
public record Connection(int nodeA, int nodeB, int[] edges, boolean stairs) {
}
