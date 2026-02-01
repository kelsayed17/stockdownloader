package com.stockdownloader.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized file I/O operations for reading, writing, appending, and deleting files.
 */
public final class FileHelper {

    private static final Logger LOGGER = Logger.getLogger(FileHelper.class.getName());

    private FileHelper() {}

    public static void writeLines(Set<String> lines, String filename) {
        try {
            Files.writeString(Path.of(filename), String.join(System.lineSeparator(), lines),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
    }

    public static void writeContent(String content, String filename) {
        try {
            String cleaned = content.replace("[", "").replace("]", "");
            Files.writeString(Path.of(filename), cleaned, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error writing file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
    }

    public static TreeSet<String> readLines(String filename) {
        var list = new TreeSet<String>();
        try {
            Path path = Path.of(filename);
            if (Files.exists(path)) {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    if (!line.isBlank()) {
                        list.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
        return list;
    }

    public static TreeSet<String> readCsvLines(String filename) {
        var list = new TreeSet<String>();
        try {
            for (String line : Files.readAllLines(Path.of(filename), StandardCharsets.UTF_8)) {
                Collections.addAll(list, line.split("\\s*,\\s*"));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading CSV file {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
        return list;
    }

    public static void appendLine(String line, String filename) {
        try {
            Files.writeString(Path.of(filename), line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error appending to {0}: {1}",
                    new Object[]{filename, e.getMessage()});
        }
    }

    public static boolean deleteFile(String filename) {
        try {
            return Files.deleteIfExists(Path.of(filename));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error deleting {0}: {1}",
                    new Object[]{filename, e.getMessage()});
            return false;
        }
    }
}
