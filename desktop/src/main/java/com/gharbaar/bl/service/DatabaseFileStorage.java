package com.gharbaar.bl.service;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.db.FileRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * [BL - SERVICE] DatabaseFileStorage  implements FileStorage
 * Saves file bytes in the stored_files table. Path format: "db:<file_id>".
 */
public class DatabaseFileStorage implements FileStorage {

    private final FileRepository repo;
    private final Session session = Session.getInstance();

    public DatabaseFileStorage(FileRepository repo) { this.repo = repo; }

    @Override
    public boolean isSupportedImage(List<File> files) {
        if (files == null || files.isEmpty()) return false;
        for (File f : files)
            if (!IMAGE_TYPES.contains(FileStorage.extension(f)) || f.length() > MAX_BYTES) return false;
        return true;
    }

    @Override
    public boolean isSupportedDocument(File file) {
        return file != null && DOCUMENT_TYPES.contains(FileStorage.extension(file)) && file.length() <= MAX_BYTES;
    }

    @Override
    public String store(List<File> files) {
        List<String> paths = new ArrayList<>();
        for (File f : files) paths.add(save(f, false));
        return String.join(";", paths);
    }

    @Override
    public String store(File file) { return save(file, true); }

    private String save(File f, boolean adminOnly) {
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            Integer uploader = session.getCurrentUser() == null ? null : session.getCurrentUser().getUserID();
            int id = repo.insert(f.getName(), FileStorage.contentType(FileStorage.extension(f)), bytes, adminOnly, uploader);
            return "db:" + id;
        } catch (IOException e) {
            throw new BusinessException("Could not read file " + f.getName());
        }
    }

    @Override
    public StoredFile load(String path) {
        if (path == null || !path.startsWith("db:")) return null;
        return repo.findById(Integer.parseInt(path.substring(3)));
    }
}
