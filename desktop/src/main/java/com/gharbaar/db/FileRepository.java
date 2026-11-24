package com.gharbaar.db;

import com.gharbaar.bl.service.FileStorage.StoredFile;

/**
 * [DB LAYER] FileRepository   -> table: stored_files
 * Used by DatabaseFileStorage to keep photos / proof documents.
 */
public class FileRepository extends BaseRepository {

    public int insert(String name, String contentType, byte[] data, boolean adminOnly, Integer uploadedBy) {
        return insert("INSERT INTO stored_files (file_name, content_type, data, admin_only, uploaded_by) VALUES (?,?,?,?,?)",
                name, contentType, data, adminOnly, uploadedBy);
    }

    public StoredFile findById(int id) {
        return queryOne("SELECT file_name, content_type, data FROM stored_files WHERE file_id=?",
                rs -> new StoredFile(rs.getString("file_name"), rs.getString("content_type"), rs.getBytes("data")), id);
    }
}
