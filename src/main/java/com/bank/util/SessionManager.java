package com.bank.util;

import com.bank.model.User;

public class SessionManager {
    private static User currentUser = null;
    private static long lastActivityTime = 0;
    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    public static synchronized void startSession(User user) {
        currentUser = user;
        updateActivity();
    }

    public static synchronized User getCurrentUser() {
        if (currentUser == null) {
            return null;
        }
        if (isSessionExpired()) {
            logout();
            return null;
        }
        updateActivity();
        return currentUser;
    }

    public static synchronized void updateActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    public static synchronized boolean isSessionExpired() {
        if (currentUser == null) return false;
        return (System.currentTimeMillis() - lastActivityTime) > SESSION_TIMEOUT_MS;
    }

    public static synchronized void logout() {
        currentUser = null;
        lastActivityTime = 0;
        System.out.println("Session cleared. User logged out.");
    }
}
