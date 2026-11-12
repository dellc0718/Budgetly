package budgetlyDB;

import budgetlyDB.model.FinancialAccount;
import budgetlyDB.model.User;
import budgetlyDB.model.UserProfile;
import budgetlyDB.model.UserSettings;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Coordinates user registration, login authentication, password changes,
 * profile/settings updates, and logout lifecycle.
 */
public class AuthenticationService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserDAO userDAO = new UserDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();
    private final FinancialAccountDAO accountDAO = new FinancialAccountDAO();

    public static final class AuthResult {
        private final boolean success;
        private final String message;
        private final User user;

        public AuthResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }

    /**
     * Authenticates a user (or administrator) by username and password,
     * verifies account status, loads profile/settings, and starts the active session.
     */
    public AuthResult login(String username, char[] password) {
        String cleanUsername = username == null ? "" : username.trim();
        if (cleanUsername.isEmpty() || password == null || password.length == 0) {
            clearChars(password);
            return new AuthResult(false, "Please enter both your username and password.", null);
        }

        try {
            User user = userDAO.findByUsername(cleanUsername);
            if (user == null) {
                clearChars(password);
                activityLogDAO.logActivity(null, cleanUsername, "LOGIN_FAILED",
                        "Failed login attempt for unknown username '" + cleanUsername + "'.");
                return new AuthResult(false, "Invalid username or password.", null);
            }

            boolean passwordValid = PasswordHasher.verifyPassword(
                    password, user.getPasswordHash(), user.getPasswordSalt());
            if (!passwordValid) {
                activityLogDAO.logActivity(user.getUserId(), user.getUsername(), "LOGIN_FAILED",
                        "Invalid password entered for user '" + user.getUsername() + "'.");
                return new AuthResult(false, "Invalid username or password.", null);
            }

            if (!user.isActive()) {
                activityLogDAO.logActivity(user.getUserId(), user.getUsername(), "LOGIN_BLOCKED",
                        "Login blocked because account '" + user.getUsername() + "' is INACTIVE.");
                return new AuthResult(false,
                        "Your account is currently inactive. Please contact an administrator.", null);
            }

            userDAO.updateLastLogin(user.getUserId());
            User refreshed = userDAO.findById(user.getUserId());
            if (refreshed == null) {
                refreshed = user;
            }

            UserProfile profile = profileDAO.findByUserId(refreshed.getUserId());
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(refreshed.getUserId());
                profile.setDisplayName(refreshed.getFullName().isEmpty() ? refreshed.getUsername() : refreshed.getFullName());
                profileDAO.upsertProfile(profile);
            }

            UserSettings settings = settingsDAO.findByUserId(refreshed.getUserId());
            if (settings == null) {
                settings = new UserSettings();
                settings.setUserId(refreshed.getUserId());
                settingsDAO.upsertSettings(settings);
            }

            SessionManager.startSession(refreshed, profile, settings);
            activityLogDAO.logActivity(refreshed.getUserId(), refreshed.getUsername(), "LOGIN_SUCCESS",
                    "Signed in as " + refreshed.getAccountType() + ".");

            return new AuthResult(true, "Welcome back, " + SessionManager.getDisplayName() + "!", refreshed);
        } catch (SQLException ex) {
            DatabaseConnection.recordError("Login Authentication", ex);
            return new AuthResult(false, "Database error during login: " + ex.getMessage(), null);
        } finally {
            clearChars(password);
        }
    }

    /**
     * Registers a new regular user inside a single atomic database transaction.
     * Creates USERS, USER_PROFILES, USER_SETTINGS, a starter Cash Wallet account, and an activity log entry.
     */
    public AuthResult registerUser(String username, String email, String firstName, String lastName,
                                   char[] password, char[] confirmPassword) {
        try {
            String u = username == null ? "" : username.trim();
            String e = email == null ? "" : email.trim();
            String fn = firstName == null ? "" : firstName.trim();
            String ln = lastName == null ? "" : lastName.trim();

            if (u.isEmpty() || e.isEmpty() || fn.isEmpty() || ln.isEmpty()
                    || password == null || password.length == 0
                    || confirmPassword == null || confirmPassword.length == 0) {
                return new AuthResult(false, "All required registration fields must be filled out.", null);
            }
            if (!USERNAME_PATTERN.matcher(u).matches()) {
                return new AuthResult(false,
                        "Username must be 3-30 characters (letters, numbers, dot, underscore, or hyphen).", null);
            }
            if (!EMAIL_PATTERN.matcher(e).matches()) {
                return new AuthResult(false, "Please enter a valid email address.", null);
            }
            if (password.length < MIN_PASSWORD_LENGTH) {
                return new AuthResult(false,
                        "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.", null);
            }
            if (!Arrays.equals(password, confirmPassword)) {
                return new AuthResult(false, "Passwords do not match. Please re-enter them.", null);
            }

            try (Connection con = DatabaseConnection.getConnection()) {
                boolean prevAuto = con.getAutoCommit();
                con.setAutoCommit(false);
                try {
                    if (userDAO.existsByUsername(con, u)) {
                        con.rollback();
                        return new AuthResult(false, "Username already exists. Please choose another username.", null);
                    }
                    if (userDAO.existsByEmail(con, e)) {
                        con.rollback();
                        return new AuthResult(false, "Email address is already registered. Please use another email.", null);
                    }

                    PasswordHasher.HashedPassword hashed = PasswordHasher.hashPassword(Arrays.copyOf(password, password.length));

                    User user = new User();
                    user.setUsername(u);
                    user.setPasswordHash(hashed.getHash());
                    user.setPasswordSalt(hashed.getSalt());
                    user.setEmail(e);
                    user.setFirstName(fn);
                    user.setLastName(ln);
                    // Regular signup can NEVER grant ADMIN privileges.
                    user.setAccountType("USER");
                    user.setAccountStatus("ACTIVE");
                    int userId = userDAO.insertUser(con, user);

                    UserProfile profile = new UserProfile();
                    profile.setUserId(userId);
                    profile.setProfilePicture("default_croc");
                    profile.setDisplayName(fn + " " + ln);
                    profile.setContactNumber("");
                    profile.setAddress("");
                    profile.setBio("");
                    profile.setOccupation("");
                    profileDAO.insertProfile(con, profile);

                    UserSettings settings = new UserSettings();
                    settings.setUserId(userId);
                    settingsDAO.insertSettings(con, settings);

                    FinancialAccount starterWallet = new FinancialAccount();
                    starterWallet.setUserId(userId);
                    starterWallet.setAccountName("Cash Wallet");
                    starterWallet.setAccountType("CASH");
                    starterWallet.setBalance(BigDecimal.ZERO);
                    starterWallet.setNotes("Default starter account");
                    accountDAO.insertAccount(con, starterWallet);

                    activityLogDAO.logActivity(con, userId, u, "SIGNUP",
                            "Registered new USER account for " + fn + " " + ln + ".");

                    con.commit();
                    return new AuthResult(true, "Account created successfully! You can now sign in.", user);
                } catch (SQLException ex) {
                    try {
                        con.rollback();
                    } catch (SQLException ignored) {
                    }
                    DatabaseConnection.recordError("User Registration", ex);
                    return new AuthResult(false, registrationError(ex, con, u, e), null);
                } finally {
                    con.setAutoCommit(prevAuto);
                }
            } catch (SQLException ex) {
                DatabaseConnection.recordError("User Registration Connection", ex);
                return new AuthResult(false, "Unable to reach the database. Please try again later.", null);
            }
        } finally {
            clearChars(password);
            clearChars(confirmPassword);
        }
    }

    private String registrationError(SQLException ex, Connection con, String username, String email) {
        // A competing signup may pass both prechecks before Derby enforces a UNIQUE constraint.
        if ("23505".equals(ex.getSQLState())) {
            try {
                if (userDAO.existsByUsername(con, username)) {
                    return "Username already exists. Please choose another username.";
                }
                if (userDAO.existsByEmail(con, email)) {
                    return "Email address is already registered. Please use another email.";
                }
            } catch (SQLException lookupError) {
                DatabaseConnection.recordError("Registration Duplicate Lookup", lookupError);
            }
            return "Username or email already exists. Please use different details.";
        }
        return "Unable to create the account. Please try again later.";
    }

    /**
     * Changes the currently logged-in user's password after verifying their current password.
     */
    public AuthResult changePassword(char[] currentPassword, char[] newPassword, char[] confirmNewPassword) {
        try {
            SessionManager.requireLoggedIn();
            int userId = SessionManager.getCurrentUserId();

            if (currentPassword == null || currentPassword.length == 0
                    || newPassword == null || newPassword.length == 0
                    || confirmNewPassword == null || confirmNewPassword.length == 0) {
                return new AuthResult(false, "Please complete all password fields.", null);
            }
            if (newPassword.length < MIN_PASSWORD_LENGTH) {
                return new AuthResult(false,
                        "New password must be at least " + MIN_PASSWORD_LENGTH + " characters long.", null);
            }
            if (!Arrays.equals(newPassword, confirmNewPassword)) {
                return new AuthResult(false, "New passwords do not match.", null);
            }

            User fresh = userDAO.findById(userId);
            if (fresh == null) {
                return new AuthResult(false, "Active user account could not be found.", null);
            }

            boolean validCurrent = PasswordHasher.verifyPassword(
                    Arrays.copyOf(currentPassword, currentPassword.length),
                    fresh.getPasswordHash(),
                    fresh.getPasswordSalt()
            );
            if (!validCurrent) {
                activityLogDAO.logActivity(userId, fresh.getUsername(), "PASSWORD_CHANGE_FAILED",
                        "Failed password change attempt due to incorrect current password.");
                return new AuthResult(false, "Current password is incorrect.", null);
            }

            PasswordHasher.HashedPassword hashed = PasswordHasher.hashPassword(Arrays.copyOf(newPassword, newPassword.length));
            userDAO.updatePassword(userId, hashed.getHash(), hashed.getSalt());
            fresh.setPasswordHash(hashed.getHash());
            fresh.setPasswordSalt(hashed.getSalt());
            SessionManager.setCurrentUser(fresh);

            activityLogDAO.logActivity(userId, fresh.getUsername(), "PASSWORD_CHANGE",
                    "Password updated securely using PBKDF2WithHmacSHA256.");
            return new AuthResult(true, "Password updated successfully!", fresh);
        } catch (SQLException ex) {
            DatabaseConnection.recordError("Password Change", ex);
            return new AuthResult(false, "Failed to update password: " + ex.getMessage(), null);
        } finally {
            clearChars(currentPassword);
            clearChars(newPassword);
            clearChars(confirmNewPassword);
        }
    }

    /**
     * Updates the currently logged-in user's account fields, profile, and application settings.
     */
    public AuthResult saveCurrentUserProfileAndSettings(String email, String firstName, String lastName,
                                                        UserProfile profile, UserSettings settings) {
        SessionManager.requireLoggedIn();
        int userId = SessionManager.getCurrentUserId();

        String e = email == null ? "" : email.trim();
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();

        if (e.isEmpty() || !EMAIL_PATTERN.matcher(e).matches()) {
            return new AuthResult(false, "Please enter a valid email address.", null);
        }
        if (fn.isEmpty() || ln.isEmpty()) {
            return new AuthResult(false, "First name and last name are required.", null);
        }

        try {
            if (userDAO.existsByEmailExcludingUser(e, userId)) {
                return new AuthResult(false, "Email '" + e + "' is already used by another account.", null);
            }

            profile.setUserId(userId);
            if (profile.getDisplayName() == null || profile.getDisplayName().trim().isEmpty()) {
                profile.setDisplayName(fn + " " + ln);
            }
            settings.setUserId(userId);

            userDAO.updateBasicAccountInfo(userId, e, fn, ln);
            profileDAO.upsertProfile(profile);
            settingsDAO.upsertSettings(settings);

            User updatedUser = userDAO.findById(userId);
            UserProfile updatedProfile = profileDAO.findByUserId(userId);
            UserSettings updatedSettings = settingsDAO.findByUserId(userId);
            SessionManager.setCurrentUser(updatedUser);
            SessionManager.setCurrentProfile(updatedProfile);
            SessionManager.setCurrentSettings(updatedSettings);

            activityLogDAO.logActivity(userId, SessionManager.getCurrentUsername(), "PROFILE_UPDATE",
                    "Updated user profile and preferences (Currency: " + updatedSettings.getCurrencyPreference()
                    + ", Theme: " + updatedSettings.getTheme() + ").");

            return new AuthResult(true, "Profile and settings saved to database.", updatedUser);
        } catch (SQLException ex) {
            DatabaseConnection.recordError("Profile & Settings Update", ex);
            return new AuthResult(false, "Unable to save profile/settings: " + ex.getMessage(), null);
        }
    }

    public void logout() {
        if (SessionManager.isLoggedIn()) {
            int uid = SessionManager.getCurrentUserId();
            String uname = SessionManager.getCurrentUsername();
            activityLogDAO.logActivity(uid, uname, "LOGOUT", "User '" + uname + "' logged out.");
        }
        SessionManager.clearSession();
    }

    private static void clearChars(char[] arr) {
        if (arr != null) {
            Arrays.fill(arr, '\0');
        }
    }
}
