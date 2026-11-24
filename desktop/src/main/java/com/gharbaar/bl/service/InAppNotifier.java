package com.gharbaar.bl.service;

import com.gharbaar.bl.domain.User;
import com.gharbaar.db.NotificationRepository;

/**
 * [BL - SERVICE] InAppNotifier  implements Notifier
 * Saves notifications in the "notifications" table; the user sees them
 * under the bell icon (desktop and web).
 */
public class InAppNotifier implements Notifier {

    private final NotificationRepository repo;

    public InAppNotifier(NotificationRepository repo) { this.repo = repo; }

    @Override public void notify(User recipient, String message) { notify(recipient.getUserID(), message); }

    @Override public void notify(int recipientId, String message) { repo.save(recipientId, message); }

    @Override public void notifyAdmins(String message) { repo.saveForRole("ADMIN", message); }
}
