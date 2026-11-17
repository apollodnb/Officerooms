package de.officerooms.outpost;

import de.officerooms.special.Stories;
import net.minecraft.util.RandomSource;

/** 0.20.0: research notes left in the outposts (one on the lectern of each outpost). */
public final class OutpostNotes {

    private static final String PROCEDURE = "GATEWAY PROCEDURE (laminated, stained):\n1. MAIN POWER lever.\n2. WAIT. Do not touch anything while the screens are red.\n3. Only when the system reports READY: the red button.";

    static final Stories.Book[] ANCIENT = {
            new Stories.Book("outpost_sculk", "Sculk Annex - Log", "Dr. H. Moreau", new String[]{
                    "We built the annex beside the city because the city was already listening. The great frame in its centre hums at exactly the frequency of the lamps on the other side.",
                    "60 Hz. Always 60 Hz. The sculk repeats it back to us, a little wrong, like a child learning a word.",
                    "Gateway test 4: the field opened onto carpet. Damp, yellow, endless. Corporal Wren stepped through. The sculk was silent for a full day afterwards.",
                    "We do not shout here. Something under the city wakes when we shout. Something on the other side wakes when the gateway is open. We cannot afford both.",
                    PROCEDURE}),
            new Stories.Book("outpost_sculk2", "Wren - Debrief", "Cpl. J. Wren", new String[]{
                    "Q: What did you see?\nA: Offices. Like ours, but nobody had ever worked in them. The chairs were warm.",
                    "Q: Were you alone?\nA: No.",
                    "Q: Who was with you?\nA: I kept walking around corners and finding the corner I had just walked around.",
                    "Q: Why did you come back?\nA: I didn't. (Interviewer's note: subject insists this is not him. Subject is him.)"})};
    static final Stories.Book[] JUNGLE = {
            new Stories.Book("outpost_temple", "Temple Station - Field Notes", "R. Okonkwo", new String[]{
                    "The builders of the temple carved a corridor into the lowest stone. It has no end. It is painted yellow under the moss.",
                    "The traps upstairs were never meant to keep thieves out. They were meant to keep something in.",
                    "We dug our station beside the temple, not under it. The vines grow toward our gateway. Even plants want to go there.",
                    "Supply drop missed again. Morale low. Somebody keeps humming. Nobody admits it.",
                    PROCEDURE})};
    static final Stories.Book[] CAVE = {
            new Stories.Book("outpost_bore", "Deep Bore Site 7", "Drill Team C", new String[]{
                    "Depth -40: the drill core came up wet and smelled of old carpet.",
                    "Depth -48: the core was carpet.",
                    "We stopped drilling and started building. Command says the gateway will 'stabilise the anomaly'. Command has never been down here.",
                    "Our lights flicker at the same time as the lights on the other side. We know because we can hear their ballasts through the rock.",
                    PROCEDURE})};
    static final Stories.Book[] NETHER = {
            new Stories.Book("outpost_ash", "Station Ash - Report", "Lt. S. Varga", new String[]{
                    "We came in through the Nether because the heat seems to drown out the hum. It does not. It only makes it sound further away.",
                    "Distances do not shrink to an eighth here when you step through. The Office does not care about our rules. One room is one room everywhere.",
                    "The piglins will not come near the frame. They kneel to it, at a distance. We have stopped laughing about that.",
                    "The basalt sweats when the gateway is open. The droplets are cold.",
                    PROCEDURE})};
    static final Stories.Book[] LEVEL0 = {
            new Stories.Book("outpost_ceiling", "Membrane Hypothesis", "Dr. H. Moreau", new String[]{
                    "Our gateways reach the Office Rooms. The Office Rooms reach further. Its topmost ceiling is thin; break it and you are somewhere else.",
                    "We call it Level 0. Larger, yellower, four floors deep. One of the skinless ones walks it, and it cannot be killed.",
                    "Survivors return through rooms the colour of manila folders, wet to the skin. They never return to where they left. The building chooses.",
                    "This outpost has a standing order: if anyone hears humming above the ceiling of the Office Rooms, log the time and do not follow."})};
    static final Stories.Book[] LAB = {
            new Stories.Book("outpost_lab", "Project LIMINAL - Summary", "Office of Research", new String[]{
                    "CLASSIFIED. Project LIMINAL operates gateway outposts in deep caves, in ancient cities, under temples and in the Nether.",
                    "All gateways open into the same place: endless offices, a constant 60 Hz hum, no exits we can map.",
                    "Every outpost was abandoned within weeks. In every case the last log entry mentions a sound of footsteps on carpet.",
                    PROCEDURE})};

    /** 0.39.0: the surface field stations. */
    static final Stories.Book[] SURFACE = {
            new Stories.Book("outpost_field", "Field Station - Logbook", "Site Manager E. Lindqvist", new String[]{
                    "Day 1. They gave us a hut in an empty field and a fence nobody will ever climb. Nearest town: two days on foot. Good.",
                    "Day 9. The frame arrived in crates marked FURNITURE. The drivers did not ask. The drivers did not stay for dinner.",
                    "Day 23. First test. Through the frame: a corridor, yellow, lit, humming. The grass around the hut went quiet. Not the wind. The grass.",
                    "Day 24. Somebody stood in the field all night, facing the hut. In the morning there were no footprints. Just a square of flattened grass, the size of an office carpet tile.",
                    "Day 31. We are leaving the lights on and the fence open. If you came across the field to find this: the gateway works. That is the problem.",
                    PROCEDURE})};

    /** 0.39.0: the drilling rig stations over the deep ocean. */
    static final Stories.Book[] OCEAN = {
            new Stories.Book("outpost_rig", "Rig 4 - Shift Log", "Toolpusher M. Abiodun", new String[]{
                    "We were told we were drilling for oil. At 900 metres the bit came up dry and smelling of photocopier toner.",
                    "At 1100 the drill string came up wrapped in carpet. Wet. Yellow. Someone laughed. Nobody laughed after that.",
                    "Command sent a frame instead of a new bit. We bolted it into the accommodation block. It hums at 60 Hz, same as the lamps below the sea floor.",
                    "At night the sea is flat as a floor. Last night it was a floor. Carpet to the horizon, ceiling tiles where the stars were. By morning it was water again.",
                    "If the supply boat finds this: we did not abandon the rig. We went down the frame, one by one, to see where the drill went.",
                    PROCEDURE})};

    private OutpostNotes() {
    }

    static Stories.Book forTheme(OutpostBuilder.Theme th, RandomSource r) {
        Stories.Book[] b = switch (th) {
            case ANCIENT -> ANCIENT;
            case JUNGLE -> JUNGLE;
            case CAVE -> CAVE;
            case NETHER -> NETHER;
            case SURFACE -> SURFACE;
            case OCEAN -> OCEAN;
            default -> LAB;
        };
        int roll = r.nextInt(10);
        return roll == 0 ? LAB[0] : roll == 1 ? LEVEL0[0] : b[r.nextInt(b.length)];
    }
}
