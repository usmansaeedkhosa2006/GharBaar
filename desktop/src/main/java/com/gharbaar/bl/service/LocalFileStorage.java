package com.gharbaar.bl.service;

import com.gharbaar.bl.BusinessException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * [BL - SERVICE] LocalFileStorage  implements FileStorage   (from the DCD)
 * Alternate implementation that copies files into a local folder.
 * Not used by default because the web version cannot read local files,
 * but it shows Protected Variations: swap it in AppContext with one line.
 */
public class LocalFileStorage implements FileStorage {

    private final String baseDir;

    public LocalFileStorage(String baseDir) { this.baseDir = baseDir; }

    @Override
    public boolean isSupportedImage(List<File> files) {
        return files != null && !files.isEmpty()
                && files.stream().allMatch(f -> IMAGE_TYPES.contains(FileStorage.extension(f)) && f.length() <= MAX_BYTES);
    }

    @Override
    public boolean isSupportedDocument(File file) {
        return file != null && DOCUMENT_TYPES.contains(FileStorage.extension(file)) && file.length() <= MAX_BYTES;
    }

    @Override
    public String store(List<File> files) {
        List<String> out = new ArrayList<>();
        for (File f : files) out.add(store(f));
        return String.join(";", out);
    }

    @Override
    public String store(File file) {
        try {
            Path dir = Files.createDirectories(Path.of(baseDir));
            Path target = dir.resolve(UUID.randomUUID() + "." + FileStorage.extension(file));
            Files.copy(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return "file:" + target.toAbsolutePath();
        } catch (IOException e) {
            throw new BusinessException("Could not save file " + file.getName());
        }
    }

    @Override
    public StoredFile load(String path) {
        try {
            Path p = Path.of(path.substring(5));
            return new StoredFile(p.getFileName().toString(),
                    FileStorage.contentType(FileStorage.extension(p.toFile())), Files.readAllBytes(p));
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
