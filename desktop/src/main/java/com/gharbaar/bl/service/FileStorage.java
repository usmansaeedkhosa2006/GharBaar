package com.gharbaar.bl.service;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * =====================================================================
 * [BL - SERVICE] FileStorage  «interface»
 * GRASP Protected Variations: where photos/proof documents are stored
 * is hidden behind this interface.
 *   - DatabaseFileStorage (used)  : stores bytes in table stored_files,
 *                                   so the web app can show them too
 *   - LocalFileStorage (alternate): copies files to a local folder
 * =====================================================================
 */
public interface FileStorage {

    List<String> IMAGE_TYPES = List.of("jpg", "jpeg", "png", "webp");
    List<String> DOCUMENT_TYPES = List.of("pdf", "jpg", "jpeg", "png");
    long MAX_BYTES = 4L * 1024 * 1024;   // 4 MB per file

    boolean isSupportedImage(List<File> files);
    boolean isSupportedDocument(File file);

    /** Stores listing photos, returns their paths joined by ';' */
    String store(List<File> files);

    /** Stores one proof document (admin-only), returns its path */
    String store(File file);

    /** Reads a stored file back (for showing photos / proof). */
    StoredFile load(String path);

    record StoredFile(String fileName, String contentType, byte[] data) { }

    // ---- shared helpers ----
    static String extension(File f) {
        String n = f.getName();
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    static String contentType(String ext) {
        return switch (ext) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            default -> "image/jpeg";
        };
    }
}
