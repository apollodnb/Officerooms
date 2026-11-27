package de.officerooms.furnish;

/** Einrichtungstypen (Phase 2). Die Namen entsprechen den Typen in {@code generator.settings.furnishing}. */
public enum RoomType {
    EMPTY("empty"),
    STANDARD_OFFICE("standard_office"),
    OPEN_OFFICE("open_office"),
    MEETING_ROOM("meeting_room"),
    BREAK_ROOM("break_room"),
    SERVER_ROOM("server_room"),
    ARCHIVE("archive"),
    CORRIDOR("corridor"),
    ATRIUM("atrium"),
    /** 0.23.0: new standard rooms (before the special types, see isSpecial()). */
    TRAINING_ROOM("training_room"),
    EXECUTIVE_OFFICE("executive_office"),
    RECEPTION("reception"),
    RESTROOM("restroom"),
    VILLAGER_OUTPOST("villager_outpost"),
    LUSH_CAVE("lush_cave"),
    WATER_OFFICE("water_office"),
    SCULK_OFFICE("sculk_office"),
    ABANDONED_CAMP("abandoned_camp"),
    OASIS("oasis"),
    EXIT_ROOM("exit_room"),
    COLD_STORAGE("cold_storage"),
    LAST_MEETING("last_meeting"),
    MAIL_ROOM("mail_room"),
    RAIN_ROOM("rain_room"),
    CANTEEN("canteen"),
    ARCADE("arcade"),
    DEPARTURE_GATE("departure_gate");

    public final String id;

    RoomType(String id) {
        this.id = id;
    }

    /** Einrichtungstyp eines Spezialraums. */
    public static RoomType of(de.officerooms.special.SpecialType t) {
        return switch (t) {
            case VILLAGER_OUTPOST -> VILLAGER_OUTPOST;
            case LUSH_CAVE -> LUSH_CAVE;
            case WATER_OFFICE -> WATER_OFFICE;
            case SCULK_OFFICE -> SCULK_OFFICE;
            case ABANDONED_CAMP -> ABANDONED_CAMP;
            case OASIS -> OASIS;
            case EXIT_ROOM -> EXIT_ROOM;
            case COLD_STORAGE -> COLD_STORAGE;
            case LAST_MEETING -> LAST_MEETING;
            case MAIL_ROOM -> MAIL_ROOM;
            case RAIN_ROOM -> RAIN_ROOM;
            case CANTEEN -> CANTEEN;
            case ARCADE -> ARCADE;
            case DEPARTURE_GATE -> DEPARTURE_GATE;
            default -> EMPTY;
        };
    }

    public boolean isSpecial() {
        return ordinal() >= VILLAGER_OUTPOST.ordinal();
    }

    public static RoomType byId(String id) {
        for (RoomType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return EMPTY;
    }
}
