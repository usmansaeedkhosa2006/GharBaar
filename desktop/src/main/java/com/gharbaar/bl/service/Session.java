package com.gharbaar.bl.service;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.User;

/**
 * =====================================================================
 * [BL - SERVICE] Session  «pure fabrication, singleton»  (GoF: Singleton)
 * Holds the currently logged-in user for the whole desktop app.
 * =====================================================================
 */
public final class Session {

    private static Session instance;
    private User currentUser;

    private Session() { }

    public static synchronized Session getInstance() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public void setCurrentUser(User u) { this.currentUser = u; }
    public User getCurrentUser() { return currentUser; }
    public void clear() { this.currentUser = null; }

    /** Returns the current user cast to the required role or throws. */
    public <T extends User> T require(Class<T> type) {
        if (currentUser == null) throw new BusinessException("Please log in first.");
        if (!type.isInstance(currentUser))
            throw new BusinessException("Only a " + type.getSimpleName() + " can do this.");
        return type.cast(currentUser);
    }
}
