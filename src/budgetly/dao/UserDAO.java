package budgetly.dao;

import budgetly.database.DatabaseConnection;
import budgetly.model.Admin;
import budgetly.model.RegularUser;
import budgetly.model.User;
import budgetly.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All SQL for the USERS table. Every statement is a PreparedStatement,
 * which protects the application from SQL injection.
 */
public class UserDAO {

    private static final String USER_COLUMNS =
            "user_id, full_name, username, email, role, is_active, created_at";

    public void insertUser(String fullName, String username, String passwordHash,
                           UserRole role) throws SQLException {
        String sql = "INSERT INTO users (full_name, username, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.setString(4, role.name());
            ps.executeUpdate();
        }
    }

    /**
     * Checks if a username is already used by another account.
     * @param excludeUserId the account to ignore (use 0 for a new account)
     */
    public boolean usernameExists(String username, int excludeUserId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?) AND user_id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT " + USER_COLUMNS + " FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** Returns the stored salted hash; used only to verify passwords. */
    public String getPasswordHash(int userId) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT " + USER_COLUMNS + " FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public void updateProfile(int userId, String fullName, String username,
                              String email) throws SQLException {
        String sql = "UPDATE users SET full_name = ?, username = ?, email = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, email);
            ps.setInt(4, userId);
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Activates/deactivates a regular user. Admin accounts are never changed. */
    public boolean setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ? AND role = 'USER'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Deletes a regular user together with their transactions and budgets.
     * The three DELETE statements run in ONE database transaction:
     * either all of them succeed (commit) or none of them do (rollback).
     */
    public boolean deleteUser(int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                executeDelete(conn, "DELETE FROM transactions WHERE user_id = ?", userId);
                executeDelete(conn, "DELETE FROM budgets WHERE user_id = ?", userId);
                int deleted = executeDelete(conn,
                        "DELETE FROM users WHERE user_id = ? AND role = 'USER'", userId);
                if (deleted != 1) {
                    conn.rollback();
                    return false;
                }
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public boolean adminExists() throws SQLException {
        return countRows("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'") > 0;
    }

    /**
     * Basic numbers for the admin panel. A LinkedHashMap keeps the insertion
     * order, so the cards always appear in the same order.
     */
    public Map<String, Long> getStatistics() throws SQLException {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("Registered Users", countRows("SELECT COUNT(*) FROM users WHERE role = 'USER'"));
        stats.put("Active Users", countRows("SELECT COUNT(*) FROM users WHERE role = 'USER' AND is_active = TRUE"));
        stats.put("Inactive Users", countRows("SELECT COUNT(*) FROM users WHERE role = 'USER' AND is_active = FALSE"));
        stats.put("Total Transactions", countRows("SELECT COUNT(*) FROM transactions"));
        return stats;
    }

    private int executeDelete(Connection conn, String sql, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }

    private Long countRows(String sql) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /** Converts one database row into an Admin or RegularUser object. */
    private User mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String fullName = rs.getString("full_name");
        String username = rs.getString("username");
        String email = rs.getString("email");
        boolean active = rs.getBoolean("is_active");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        if (UserRole.valueOf(rs.getString("role")) == UserRole.ADMIN) {
            return new Admin(id, fullName, username, email, active, createdAt);
        }
        return new RegularUser(id, fullName, username, email, active, createdAt);
    }
}
