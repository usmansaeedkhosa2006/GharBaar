package com.gharbaar.db;

import com.gharbaar.bl.domain.Notification;

import java.util.List;

/**
 * [DB LAYER] NotificationRepository   -> table: notifications
 */
public class NotificationRepository extends BaseRepository {

    public void save(int recipientId, String message) {
        insert("INSERT INTO notifications (recipient_id, message) VALUES (?,?)", recipientId, message);
    }

    public void saveForRole(String role, String message) {
        execute("INSERT INTO notifications (recipient_id, message) SELECT user_id, ? FROM users WHERE role=?", message, role);
    }

    public List<Notification> findByRecipient(int userId) {
        return query("SELECT * FROM notifications WHERE recipient_id=? ORDER BY created_at DESC LIMIT 50",
                rs -> new Notification(rs.getInt("notification_id"), rs.getString("message"),
                        rs.getBoolean("is_read"), ts(rs, "created_at")), userId);
    }

    public int countUnread(int userId) {
        return count("SELECT COUNT(*) FROM notifications WHERE recipient_id=? AND is_read=FALSE", userId);
    }

    public void markAllRead(int userId) {
        execute("UPDATE notifications SET is_read=TRUE WHERE recipient_id=?", userId);
    }
}
