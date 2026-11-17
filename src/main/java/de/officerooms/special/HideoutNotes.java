package de.officerooms.special;

/** 0.11.0: English notes found in hidden rooms. */
public final class HideoutNotes {

    private HideoutNotes() {
    }

    private static Stories.Book b(String id, String title, String author, String... pages) {
        return new Stories.Book(id, title, author, pages);
    }

    public static final Stories.Book[][] BY_TYPE = {
        { // maintenance room
            b("maint_1", "Work Order 4471", "Facilities",
                "WORK ORDER 4471\nPriority: LOW\n\nLight panels on this floor buzz at the wrong pitch. Replaced ballast twice. Still wrong.\n\nNote: the pitch changes when nobody is in the room.",
                "Follow-up: there is no breaker for this section. There is no section. The plans show a wall where I am standing.\n\nClosing this ticket."),
            b("maint_2", "Maintenance Log", "R. Okonkwo",
                "Day 9. Pipes in the ceiling are warm. Nothing runs through them. I put my ear to one and heard footsteps going the other way.",
                "Day 14. I keep the door panel loose so I can get out fast. If you found this room, put the panel back. They check the walls.")
        },
        { // supply closet
            b("supply_1", "Inventory Sheet", "Unknown",
                "Batteries: 14\nBandages: 9\nGlowsticks: 30\nAlmond water: 11\n\nCounted twice. Counted again in the morning. Counted again.",
                "Batteries: 14\nBandages: 9\nGlowsticks: 30\nAlmond water: 11\n\nI have been taking from this shelf for a month.")
        },
        { // forgotten break room
            b("break_1", "Happy Birthday Dave", "Everyone",
                "Happy birthday Dave!!\n\nCake is in the fridge. Don't let Marcus near it.\n\n- Priya, Tom, Marcus, Ellen, Sam",
                "(on the back, in pencil)\n\nWe waited three hours. Dave never came up. The elevator kept stopping on this floor with nobody in it."),
            b("break_2", "Notice", "Management",
                "REMINDER\n\nThe break room is closed until further notice.\n\nDo not use the coffee machine.\nDo not answer the phone.\nDo not count the chairs.")
        },
        { // abandoned security office
            b("sec_1", "Night Shift Report", "Officer D. Hale",
                "0214 - Camera 6 shows a man standing in corridor C. Camera 7, same man, same pose. Camera 8, same.\n\nThere are only five cameras.",
                "0230 - All feeds show this office. From behind me.\n\n0231 - I am not turning around.\n\nLevel 1 card left in the drawer for whoever relieves me."),
            b("sec_2", "Access Policy", "Security",
                "Level 1: storage rooms, labs on lower floors.\nLevel 2: secure storage, medical.\nLevel 3: vaults. Do not ask what is in the vaults.\n\nLost cards must be reported. Found cards must be reported twice.")
        },
        { // research lab
            b("lab_1", "Specimen 3", "Dr. A. Voss",
                "Specimen 3 does not age. It does not eat. It reacts only to light, and only by growing toward the dark.",
                "Tried the almond water. The specimen went still for an hour. So did the lights.\n\nRecommendation: the water is not water."),
            b("lab_2", "Experiment Notes", "Dr. A. Voss",
                "Sound test. Played a recording of footsteps in the empty corridor.\n\nThe recording now has two sets of footsteps.\n\nDestroyed the tape. The second set continues.")
        },
        { // concealed room
            b("hidden_1", "Don't", "?",
                "dont count them\ndont count them\ndont count them\n\nthere were nine marks on the wall when i got here\nthere are ten now and i only made one",
                "if you are reading this there are eleven"),
            b("hidden_2", "Last Entry", "M.",
                "The smile was in the dark at the end of the hall again. It doesn't move when I look. It moves when I blink.\n\nI am going to stop blinking.")
        }
    };
}
