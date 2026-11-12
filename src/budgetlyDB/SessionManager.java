package budgetlyDB;

import budgetlyDB.model.User;
import budgetlyDB.model.UserProfile;
import budgetlyDB.model.UserSettings;
import java.sql.Timestamp;

/**
 * Holds the active authenticated user session for the Budgetly desktop application.
 */
public final class SessionManager {

    private static User currentUser;
    private static UserProfile currentProfile;
    private static UserSettings currentSettings;
    private static Timestamp loginTime;

    private SessionManager() {
    }

    public static synchronized void startSession(User user, UserProfile profile, UserSettings settings) {
        if (user == null) {
            throw new IllegalArgumentException("Authenticated user cannot be null.");
        }
        currentUser = user;
        currentProfile = profile;
        currentSettings = settings != null ? settings : new UserSettings();
        loginTime = new Timestamp(System.currentTimeMillis());
    }

    public static synchronized void clearSession() {
        currentUser = null;
        currentProfile = null;
        currentSettings = null;
        loginTime = null;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    public static synchronized void requireLoggedIn() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("Authentication is required. Please log in first.");
        }
    }

    public static synchronized void requireAdmin() {
        requireLoggedIn();
        if (!isAdmin()) {
            throw new SecurityException("Access denied. Administrator privileges are required.");
        }
    }

    public static synchronized int getCurrentUserId() {
        requireLoggedIn();
        return currentUser.getUserId();
    }

    public static synchronized String getCurrentUsername() {
        return currentUser == null ? null : currentUser.getUsername();
    }

    public static synchronized String getCurrentAccountType() {
        return currentUser == null ? null : currentUser.getAccountType();
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void setCurrentUser(User user) {
        if (user != null) {
            currentUser = user;
        }
    }

    public static synchronized UserProfile getCurrentProfile() {
        return currentProfile;
    }

    public static synchronized void setCurrentProfile(UserProfile profile) {
        currentProfile = profile;
    }

    public static synchronized UserSettings getCurrentSettings() {
        return currentSettings;
    }

    public static synchronized void setCurrentSettings(UserSettings settings) {
        currentSettings = settings;
    }

    public static synchronized Timestamp getLoginTime() {
        return loginTime;
    }

    public static synchronized String getCurrencyPrefix() {
        if (currentSettings == null) {
            return "PHP ";
        }
        return currentSettings.getCurrencyPrefix();
    }

    public static synchronized String getDisplayName() {
        if (currentProfile != null && currentProfile.getDisplayName() != null
                && !currentProfile.getDisplayName().trim().isEmpty()) {
            return currentProfile.getDisplayName().trim();
        }
        if (currentUser != null) {
            String full = currentUser.getFullName();
            return full.isEmpty() ? currentUser.getUsername() : full;
        }
        return "Guest";
    }
}
