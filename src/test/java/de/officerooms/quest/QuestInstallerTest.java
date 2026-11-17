package de.officerooms.quest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 0.40.0: the quest files are merged into a modpack's quest book without touching its own chapters. */
class QuestInstallerTest {

    private static final String THEIR_GROUPS = "{\n\tchapter_groups: [\n\t\t{\n\t\t\tid: \"1111111111111111\"\n\t\t\ttitle: \"Better Pack\"\n\t\t}\n\t]\n}\n";

    @Test
    void freshInstallCopiesEverything(@TempDir Path dir) throws Exception {
        QuestInstaller.install(dir, "1.0");
        assertTrue(Files.exists(dir.resolve("data.snbt")));
        assertTrue(Files.exists(dir.resolve("chapters/office_rooms_threshold.snbt")));
        assertTrue(Files.exists(dir.resolve("reward_tables/office_rooms_supplies.snbt")));
        assertTrue(Files.readString(dir.resolve("chapter_groups.snbt")).contains("Office Rooms"));
    }

    @Test
    void mergeKeepsTheModpack(@TempDir Path dir) throws Exception {
        Files.createDirectories(dir.resolve("chapters"));
        Files.writeString(dir.resolve("data.snbt"), "{ title: \"Better Pack\" }\n");
        Files.writeString(dir.resolve("chapter_groups.snbt"), THEIR_GROUPS);
        Files.writeString(dir.resolve("chapters/getting_started.snbt"), "{ id: \"2222222222222222\" }\n");
        Files.writeString(dir.resolve("chapters/office_rooms_old_chapter.snbt"), "{ }\n");   // from an older version: removed
        QuestInstaller.install(dir, "1.0");
        assertEquals("{ title: \"Better Pack\" }\n", Files.readString(dir.resolve("data.snbt")));
        assertEquals("{ id: \"2222222222222222\" }\n", Files.readString(dir.resolve("chapters/getting_started.snbt")));
        assertTrue(Files.exists(dir.resolve("chapters/office_rooms_threshold.snbt")));
        assertFalse(Files.exists(dir.resolve("chapters/office_rooms_old_chapter.snbt")));
        String groups = Files.readString(dir.resolve("chapter_groups.snbt"));
        assertTrue(groups.contains("Better Pack") && groups.contains("Office Rooms"), groups);
        assertTrue(groups.indexOf("Better Pack") < groups.indexOf("Office Rooms"), groups);   // the pack's groups stay first
        // a second start with the same version changes nothing; the group is never added twice
        Files.writeString(dir.resolve("chapters/office_rooms_threshold.snbt"), "edited by the pack author");
        QuestInstaller.install(dir, "1.0");
        assertEquals("edited by the pack author", Files.readString(dir.resolve("chapters/office_rooms_threshold.snbt")));
        QuestInstaller.install(dir, "1.1");
        String again = Files.readString(dir.resolve("chapter_groups.snbt"));
        assertEquals(again.indexOf("Office Rooms"), again.lastIndexOf("Office Rooms"));
    }
}
