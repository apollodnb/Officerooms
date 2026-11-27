package de.officerooms.quest;

import de.officerooms.OfficeRooms;
import de.officerooms.core.OfficeRoomsConfig;
import de.officerooms.core.OfficeRoomsIds;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 0.40.0: puts the Office Rooms quest book into FTB Quests automatically. A mod download on CurseForge only brings the JAR,
 * so the quest files travel inside it ({@code /office_rooms_quests/}) and are merged into {@code config/ftbquests/quests}:
 * <ul>
 *   <li>no quest book yet (no chapters): everything is copied, including {@code data.snbt};</li>
 *   <li>a modpack's own quest book: only the {@code office_rooms_*} chapter and reward table files are written and the
 *       Office Rooms chapter group is added to {@code chapter_groups.snbt}; everything else stays untouched;</li>
 *   <li>a marker file remembers the installed version, so nothing happens again until the mod is updated.</li>
 * </ul>
 */
public final class QuestInstaller {

    private static final String RES = "/office_rooms_quests/";
    private static final String PREFIX = "office_rooms_";
    private static final String MARK = ".office_rooms_quests";
    private static final Pattern GROUP_ID = Pattern.compile("id:\\s*\"([0-9A-F]+)\"");

    private QuestInstaller() {
    }

    public static void install() {
        if (!ModList.get().isLoaded("ftbquests")) {
            return;
        }
        if (!OfficeRoomsConfig.INSTALL_QUESTS.get()) {
            OfficeRooms.LOGGER.info("Office Rooms quests: automatic install is switched off (installQuests = false).");
            return;
        }
        try {
            install(FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests"), OfficeRoomsIds.version());
        } catch (IOException | RuntimeException e) {
            OfficeRooms.LOGGER.warn("Office Rooms quests could not be installed", e);
        }
    }

    static void install(Path dir, String version) throws IOException {
        Path mark = dir.resolve(MARK);
        if (Files.exists(mark) && Files.readString(mark, StandardCharsets.UTF_8).trim().equals(version)) {
            return;
        }
        List<String> files = index();
        Path chapters = dir.resolve("chapters");
        boolean fresh = !hasSnbt(chapters);
        Files.createDirectories(chapters);
        Files.createDirectories(dir.resolve("reward_tables"));
        // our files from an older version that are gone now
        for (String sub : new String[]{"chapters", "reward_tables"}) {
            try (Stream<Path> s = Files.list(dir.resolve(sub))) {
                for (Path p : s.toList()) {
                    String n = p.getFileName().toString();
                    if (n.startsWith(PREFIX) && !files.contains(sub + "/" + n)) {
                        Files.delete(p);
                    }
                }
            }
        }
        int written = 0;
        for (String f : files) {
            if (f.equals("index.txt")) {
                continue;
            }
            Path target = dir.resolve(f);
            if (f.equals("data.snbt")) {
                if (fresh || !Files.exists(target)) {
                    copy(f, target);
                    written++;
                }
            } else if (f.equals("chapter_groups.snbt")) {
                if (fresh || !Files.exists(target)) {
                    copy(f, target);
                } else {
                    mergeGroup(target, read(f));
                }
                written++;
            } else {
                copy(f, target);
                written++;
            }
        }
        Files.writeString(mark, version + "\n", StandardCharsets.UTF_8);
        OfficeRooms.LOGGER.info("Office Rooms quests {} into {} ({} files, {}).", fresh ? "installed" : "merged", dir, written, version);
    }

    /** Adds the Office Rooms chapter group to a modpack's chapter_groups.snbt, unless it is there already. */
    static void mergeGroup(Path target, String ours) throws IOException {
        Matcher m = GROUP_ID.matcher(ours);
        if (!m.find()) {
            return;
        }
        String id = m.group(1);
        String theirs = Files.readString(target, StandardCharsets.UTF_8);
        if (theirs.contains("\"" + id + "\"")) {
            return;
        }
        String entry = "\n\t\t{\n\t\t\tid: \"" + id + "\"\n\t\t\ttitle: \"Office Rooms\"\n\t\t}";
        int at = theirs.indexOf("chapter_groups: [");
        int end = theirs.lastIndexOf(']');
        String merged;
        if (at >= 0 && end > at) {
            // after the modpack's own groups, so its chapters stay on top of the quest book
            String before = theirs.substring(0, end).replaceAll("\\s+$", "");
            merged = before + entry + "\n\t" + theirs.substring(end);
        } else {
            merged = ours;                                              // unreadable file: use ours (a copy of theirs stays next to it)
            Files.copy(target, target.resolveSibling("chapter_groups.snbt.before_office_rooms"), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(target, merged, StandardCharsets.UTF_8);
    }

    private static boolean hasSnbt(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> s = Files.list(dir)) {
            return s.anyMatch(p -> p.getFileName().toString().endsWith(".snbt"));
        }
    }

    private static List<String> index() throws IOException {
        List<String> out = new ArrayList<>();
        for (String line : read("index.txt").split("\n")) {
            if (!line.isBlank()) {
                out.add(line.trim());
            }
        }
        return out;
    }

    private static String read(String f) throws IOException {
        try (InputStream in = QuestInstaller.class.getResourceAsStream(RES + f)) {
            if (in == null) {
                throw new IOException("missing " + RES + f);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void copy(String f, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        try (InputStream in = QuestInstaller.class.getResourceAsStream(RES + f)) {
            if (in == null) {
                throw new IOException("missing " + RES + f);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
