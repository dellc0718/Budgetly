package budgetly.service;

import budgetly.dao.UserDAO;
import budgetly.model.User;
import budgetly.model.UserRole;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Business rules for accounts: registration, login, password hashing,
 * profile updates and the administrator's user management.
 *
 * Validation methods return an error message, or null when everything is OK.
 */
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int HASH_ITERATIONS = 65536;
    private static final int SALT_BYTES = 16;
    private static final String USERNAME_PATTERN = "[A-Za-z0-9_]{3,20}";
    private static final String EMAIL_PATTERN = "^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$";

    private final UserDAO userDAO = new UserDAO();

    /**
     * Registers a new REGULAR user. Public registration can never create an admin.
     * @return an error message, or null if the account was created
     */
    public String register(String fullName, String username, String password,
                           String confirmPassword) throws SQLException {
        return createAccount(fullName, username, password, confirmPassword, UserRole.USER);
    }

    /**
     * Database setup step: creates the initial administrator account on first launch
     * if no ADMIN record exists in the database yet.
     */
    public void initializeDefaultAdmin() throws SQLException {
        if (!userDAO.adminExists()) {
            userDAO.insertUser("System Administrator", "admin",
                    hashPassword("admin123"), UserRole.ADMIN);
        }
    }

    /** @return true when the database has no administrator yet (first start-up) */
    public boolean isAdminSetupRequired() throws SQLException {
        return !userDAO.adminExists();
    }

    /**
     * Creates the first administrator. Only allowed while no admin exists.
     */
    public String createInitialAdmin(String fullName, String username, String password,
                                     String confirmPassword) throws SQLException {
        if (!isAdminSetupRequired()) {
            return "An administrator account already exists.";
        }
        return createAccount(fullName, username, password, confirmPassword, UserRole.ADMIN);
    }

    /**
     * Checks the username and password.
     * @return the logged-in user (Admin or RegularUser), or null if the credentials are wrong
     */
    public User login(String username, String password) throws SQLException {
        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            return null;
        }
        String storedHash = userDAO.getPasswordHash(user.getUserId());
        return verifyPassword(password, storedHash) ? user : null;
    }

    /**
     * Updates the logged-in user's own profile. The user id always comes from
     * the logged-in User object, so nobody can edit another person's profile.
     */
    public String updateProfile(User user, String fullName, String username,
                                String email) throws SQLException {
        String error = validateNameAndUsername(fullName, username);
        if (error == null && !email.trim().isEmpty() && !email.trim().matches(EMAIL_PATTERN)) {
            error = "Please enter a valid email address (example: juan@gmail.com).";
        }
        if (error == null && userDAO.usernameExists(username.trim(), user.getUserId())) {
            error = "The username \"" + username.trim() + "\" is already taken.";
        }
        if (error != null) {
            return error;
        }
        String cleanEmail = email.trim().isEmpty() ? null : email.trim();
        userDAO.updateProfile(user.getUserId(), fullName.trim(), username.trim(), cleanEmail);
        user.setFullName(fullName.trim());
        user.setUsername(username.trim());
        user.setEmail(cleanEmail);
        return null;
    }

    public String changePassword(User user, String currentPassword, String newPassword,
                                 String confirmPassword) throws SQLException {
        if (!verifyPassword(currentPassword, userDAO.getPasswordHash(user.getUserId()))) {
            return "Your current password is incorrect.";
        }
        String error = validatePassword(newPassword, confirmPassword);
        if (error != null) {
            return error;
        }
        userDAO.updatePassword(user.getUserId(), hashPassword(newPassword));
        return null;
    }

    // ---------------- Administrator features ----------------

    public List<User> getAllUsers(User admin) throws SQLException {
        requireAdmin(admin);
        return userDAO.findAll();
    }

    public Map<String, Long> getStatistics(User admin) throws SQLException {
        requireAdmin(admin);
        return userDAO.getStatistics();
    }

    public boolean setUserActive(User admin, int userId, boolean active) throws SQLException {
        requireAdmin(admin);
        return userDAO.setActive(userId, active);
    }

    public boolean deleteUser(User admin, int userId) throws SQLException {
        requireAdmin(admin);
        return userDAO.deleteUser(userId);
    }

    /** Polymorphism: Admin returns true, RegularUser returns false. */
    private void requireAdmin(User user) {
        if (user == null || !user.canManageUsers()) {
            throw new SecurityException("Access denied: administrator privileges are required.");
        }
    }

    // ---------------- Validation helpers ----------------

    private String createAccount(String fullName, String username, String password,
                                 String confirmPassword, UserRole role) throws SQLException {
        String error = validateNameAndUsername(fullName, username);
        if (error == null) {
            error = validatePassword(password, confirmPassword);
        }
        if (error == null && userDAO.usernameExists(username.trim(), 0)) {
            error = "The username \"" + username.trim() + "\" is already taken.";
        }
        if (error != null) {
            return error;
        }
        userDAO.insertUser(fullName.trim(), username.trim(), hashPassword(password), role);
        return null;
    }

    private String validateNameAndUsername(String fullName, String username) {
        if (fullName.trim().isEmpty() || username.trim().isEmpty()) {
            return "Full name and username are required.";
        }
        if (fullName.trim().length() > 100) {
            return "Full name must not exceed 100 characters.";
        }
        if (!username.trim().matches(USERNAME_PATTERN)) {
            return "Username must be 3-20 characters (letters, numbers or underscore only).";
        }
        return null;
    }

    private String validatePassword(String password, String confirmPassword) {
        if (password.isEmpty()) {
            return "Password is required.";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        }
        if (!password.equals(confirmPassword)) {
            return "Password and confirm password do not match.";
        }
        return null;
    }

    // ---------------- Password hashing (PBKDF2 + random salt) ----------------

    /**
     * Hashes a password with a NEW random salt.
     * Stored format: "base64(salt):base64(hash)".
     */
    private String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password, salt);
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(salt) + ":" + encoder.encodeToString(hash);
    }

    private boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) {
            return false;
        }
        String[] parts = storedHash.split(":");
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        // MessageDigest.isEqual compares in constant time (safer than Arrays.equals)
        return MessageDigest.isEqual(expected, pbkdf2(password, salt));
    }

    private byte[] pbkdf2(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, HASH_ITERATIONS, 256);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            // Every standard JDK supports PBKDF2WithHmacSHA256, so this should never happen.
            throw new IllegalStateException("Password hashing is not available.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
