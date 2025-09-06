package com.asusoftware.SafeDocs_api.utils;

public final class FileNameSanitizer {
    private FileNameSanitizer() {}
    public static String sanitize(String original) {
        if (original == null || original.isBlank()) return "file";
        String clean = original.replace("\\", "_").replace("/", "_");
        clean = clean.replaceAll("[^a-zA-Z0-9._-]", "_");
        return clean.length() > 120 ? clean.substring(0, 120) : clean;
    }
}
