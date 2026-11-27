package de.officerooms.special;

/** Spezialraumtypen (Phase 4). */
public enum SpecialType {
    NONE("none"),
    VILLAGER_OUTPOST("villager_outpost"),
    LUSH_CAVE("lush_cave"),
    WATER_OFFICE("water_office"),
    SCULK_OFFICE("sculk_office"),
    /** Verlassenes Überlebenslager (Liminal-World-Update). */
    ABANDONED_CAMP("abandoned_camp"),
    /** Extrem seltener tropischer Wasserpark, höchstens einer pro Region. */
    OASIS("oasis"),
    /** Raum liegt (ganz oder teilweise) im Band einer Poolrooms-Megastruktur und wird nicht gebaut. */
    POOLROOMS("poolrooms"),
    /** 0.8.0: room lies in the band of a Neighbourhood megastructure and is not built. */
    NEIGHBOURHOOD("neighbourhood"),
    /** 0.8.0: room lies in the band of The Maze and is not built. */
    MAZE("maze"),
    HOTEL("hotel"),
    INDUSTRIAL("industrial"),
    LEVEL_FUN("level_fun"),
    /** Raum auf der untersten Ebene mit einem Rückkehrportal in die Overworld. */
    EXIT_ROOM("exit_room"),
    /** 0.29.0 curiosities (new worlds only, normal entrances). */
    COLD_STORAGE("cold_storage"),
    LAST_MEETING("last_meeting"),
    MAIL_ROOM("mail_room"),
    RAIN_ROOM("rain_room"),
    CANTEEN("canteen"),
    ARCADE("arcade"),
    /** 0.31.0: room lies in the band of the abandoned cinema. */
    CINEMA("cinema"),
    /** 0.42.0: departure gate to the SS Fun =) (content version 2). */
    DEPARTURE_GATE("departure_gate"),
    /** 0.43.0: room lies in the band of the Dead Mall. */
    MALL("mall");

    public final String id;

    SpecialType(String id) {
        this.id = id;
    }

    /** Spezialräume schließen alle Öffnungen bis auf eine Tür (Villager, Wasser). */
    /** Megastructure placeholder: the room is replaced by the megastructure's own architecture. */
    public boolean isMegastructure() {
        return this == POOLROOMS || this == NEIGHBOURHOOD || this == MAZE || this == HOTEL || this == INDUSTRIAL || this == LEVEL_FUN || this == CINEMA || this == MALL;
    }

    public boolean sealsOpenings() {
        return this == VILLAGER_OUTPOST || this == WATER_OFFICE;
    }
}
