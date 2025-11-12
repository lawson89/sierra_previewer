package com.sierra.previewer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Manages the list of recent files using the Preferences API for persistence.
 */
public class RecentFilesManager {

    private static final int MAX_RECENT_FILES = 5;
    private static final String PREF_KEY_RECENT_FILES = "recent_files_list";
    private static final String PATH_SEPARATOR = ";"; // Used to delimit paths in the stored string

    private final Preferences prefs;
    private final List<Path> recentFiles = new ArrayList<>();

    public RecentFilesManager(Class<?> applicationClass) {
        // Get the preferences node for the application
        this.prefs = Preferences.userNodeForPackage(applicationClass);
        loadRecentFiles();
    }

    /**
     * Loads the recent files list from the Preferences store.
     */
    private void loadRecentFiles() {
        String savedList = prefs.get(PREF_KEY_RECENT_FILES, "");

        if (!savedList.isEmpty()) {
            // Split the string by the separator, convert to Path objects, and collect.
            recentFiles.addAll(Arrays.stream(savedList.split(PATH_SEPARATOR))
                    .filter(s -> !s.trim().isEmpty())
                    .map(Paths::get)
                    .collect(Collectors.toList()));
        }
    }

    /**
     * Saves the current recent files list to the Preferences store.
     */
    private void saveRecentFiles() {
        // Convert the list of Paths to a single semicolon-separated string
        String listToSave = recentFiles.stream()
                .map(Path::toString)
                .collect(Collectors.joining(PATH_SEPARATOR));

        prefs.put(PREF_KEY_RECENT_FILES, listToSave);
    }

    /**
     * Adds a file path to the recent files list, moving it to the top.
     * The list is capped at MAX_RECENT_FILES.
     *
     * @param path The path of the file to add.
     */
    public void addFile(Path path) {
        // 1. Remove existing entry to ensure it moves to the top
        recentFiles.remove(path);

        // 2. Add to the front (most recent)
        recentFiles.add(0, path);

        // 3. Trim the list if it exceeds the max size
        if (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(MAX_RECENT_FILES);
        }

        // 4. Save the updated list
        saveRecentFiles();
    }

    /**
     * Returns a copy of the recent files list, sorted by most recent first.
     *
     * @return An unmodifiable list of recent file paths.
     */
    public List<Path> getRecentFiles() {
        return recentFiles;
    }

    /**
     * Gets the maximum number of files stored.
     *
     * @return The maximum size of the list.
     */
    public int getMaxFiles() {
        return MAX_RECENT_FILES;
    }
}