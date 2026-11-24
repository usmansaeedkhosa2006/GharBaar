package com.gharbaar.bl.service;

import com.gharbaar.bl.domain.User;

/**
 * =====================================================================
 * [BL - SERVICE] Notifier  «interface»
 * GRASP Protected Variations: controllers depend on this interface, so
 * the notification channel (in-app, e-mail, SMS) can change without
 * touching any controller. Current implementation: InAppNotifier.
 * =====================================================================
 */
public interface Notifier {
    void notify(User recipient, String message);
    void notify(int recipientId, String message);
    void notifyAdmins(String message);
}
