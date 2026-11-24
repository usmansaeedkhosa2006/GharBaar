package com.gharbaar.bl.controller;

import com.gharbaar.bl.domain.Notification;
import com.gharbaar.bl.domain.User;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.NotificationRepository;

import java.util.List;

/**
 * [BL - CONTROLLER] NotificationController
 * Shows the messages produced by the Notifier (bell icon).
 */
public class NotificationController {

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) { this.repo = repo; }

    public List<Notification> getMyNotifications() {
        return repo.findByRecipient(Session.getInstance().require(User.class).getUserID());
    }

    public int getUnreadCount() {
        User u = Session.getInstance().getCurrentUser();
        return u == null ? 0 : repo.countUnread(u.getUserID());
    }

    public void markAllRead() {
        repo.markAllRead(Session.getInstance().require(User.class).getUserID());
    }
}
