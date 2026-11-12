package budgetlyDB;

import budgetlyDB.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for CRUD and authentication queries against the USERS table.
 */
public class UserDAO {

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM USERS WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public User findByUsername(String username) throws SQLException {
        if (username == null) {
            return null;
        }
        String sql = "SELECT * FROM USERS WHERE LOWER(username) = LOWER(?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean existsByUsername(Connection con, String username) throws SQLException {
        String sql = "SELECT 1 FROM USERS WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByEmail(Connection con, String email) throws SQLException {
        String sql = "SELECT 1 FROM USERS WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByEmailExcludingUser(String email, int excludeUserId) throws SQLException {
        String sql = "SELECT 1 FROM USERS WHERE LOWER(email) = LOWER(?) AND user_id <> ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setInt(2, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertUser(Connection con, User user) throws SQLException {
        String sql = "INSERT INTO USERS "
                + "(username, password_hash, password_salt, email, first_name, last_name, account_type, account_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getPasswordSalt());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getFirstName());
            ps.setString(6, user.getLastName());
            ps.setString(7, user.getAccountType());
            ps.setString(8, user.getAccountStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    user.setUserId(id);
                    return id;
                }
                throw new SQLException("Inserting user did not return a generated user_id.");
            }
        }
    }

    public void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE USERS SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean updatePassword(int userId, String passwordHash, String passwordSalt) throws SQLException {
        String sql = "UPDATE USERS SET password_hash = ?, password_salt = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, passwordSalt);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateBasicAccountInfo(int userId, String email, String firstName, String lastName) throws SQLException {
        String sql = "UPDATE USERS SET email = ?, first_name = ?, last_name = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, firstName.trim());
            ps.setString(3, lastName.trim());
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<User> findAllUsers() throws SQLException {
        String sql = "SELECT * FROM USERS ORDER BY date_created DESC, user_id DESC";
        List<User> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<User> searchUsers(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return findAllUsers();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        String sql = "SELECT * FROM USERS "
                + "WHERE LOWER(username) LIKE ? OR LOWER(email) LIKE ? "
                + "OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? "
                + "ORDER BY date_created DESC, user_id DESC";
        List<User> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public boolean updateStatus(int userId, String newStatus) throws SQLException {
        String sql = "UPDATE USERS SET account_status = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateUserByAdmin(int userId, String email, String firstName, String lastName,
                                     String accountType, String accountStatus) throws SQLException {
        String sql = "UPDATE USERS SET email = ?, first_name = ?, last_name = ?, "
                + "account_type = ?, account_status = ? WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setString(2, firstName.trim());
            ps.setString(3, lastName.trim());
            ps.setString(4, accountType);
            ps.setString(5, accountStatus);
            ps.setInt(6, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM USERS WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("date_created");
        Timestamp lastLogin = rs.getTimestamp("last_login");
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("password_salt"),
                rs.getString("email"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("account_type"),
                rs.getString("account_status"),
                created,
                lastLogin
        );
    }
}
