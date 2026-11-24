package com.gharbaar.bl.domain;

import java.time.LocalDateTime;

/**
 * [BL - DOMAIN] Notification
 * In-app message produced by InAppNotifier (e.g. "Your listing was verified").
 */
public class Notification {

    private final int notificationID;
    private final String message;
    private final boolean read;
    private final LocalDateTime createdAt;

    public Notification(int id, String message, boolean read, LocalDateTime createdAt) {
        this.notificationID = id;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public int getNotificationID() { return notificationID; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
