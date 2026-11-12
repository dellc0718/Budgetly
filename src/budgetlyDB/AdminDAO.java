package budgetlyDB;

import budgetlyDB.model.ActivityLog;
import budgetlyDB.model.User;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object dedicated to administrator monitoring, user administration,
 * and safe read-only database inspection.
 */
public class AdminDAO {

    private final UserDAO userDAO = new UserDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public static final class AdminOverviewStats {
        private final int totalUsers;
        private final int activeUsers;
        private final int inactiveUsers;
        private final int totalAdmins;

        public AdminOverviewStats(int totalUsers, int activeUsers, int inactiveUsers, int totalAdmins) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.inactiveUsers = inactiveUsers;
            this.totalAdmins = totalAdmins;
        }

        public int getTotalUsers() {
            return totalUsers;
        }

        public int getActiveUsers() {
            return activeUsers;
        }

        public int getInactiveUsers() {
            return inactiveUsers;
        }

        public int getTotalAdmins() {
            return totalAdmins;
        }
    }

    public static final class TablePreview {
        private final List<String> columns;
        private final List<Object[]> rows;

        public TablePreview(List<String> columns, List<Object[]> rows) {
            this.columns = columns;
            this.rows = rows;
        }

        public List<String> getColumns() {
            return columns;
        }

        public List<Object[]> getRows() {
            return rows;
        }
    }

    public AdminOverviewStats getOverviewStats() throws SQLException {
        SessionManager.requireAdmin();
        String sql = "SELECT "
                + "COUNT(*) AS total_users, "
                + "SUM(CASE WHEN account_status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_users, "
                + "SUM(CASE WHEN account_status = 'INACTIVE' THEN 1 ELSE 0 END) AS inactive_users, "
                + "SUM(CASE WHEN account_type = 'ADMIN' THEN 1 ELSE 0 END) AS total_admins "
                + "FROM USERS";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new AdminOverviewStats(
                        rs.getInt("total_users"),
                        rs.getInt("active_users"),
                        rs.getInt("inactive_users"),
                        rs.getInt("total_admins")
                );
            }
            return new AdminOverviewStats(0, 0, 0, 0);
        }
    }

    public List<User> getRecentlyRegisteredUsers(int limit) throws SQLException {
        SessionManager.requireAdmin();
        String sql = "SELECT * FROM USERS ORDER BY date_created DESC, user_id DESC";
        List<User> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setMaxRows(Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUserRow(rs));
                }
            }
        }
        return list;
    }

    public List<User> getRecentLoginUsers(int limit) throws SQLException {
        SessionManager.requireAdmin();
        String sql = "SELECT * FROM USERS WHERE last_login IS NOT NULL ORDER BY last_login DESC";
        List<User> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setMaxRows(Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapUserRow(rs));
                }
            }
        }
        return list;
    }

    public List<User> getAllUsers() throws SQLException {
        SessionManager.requireAdmin();
        return userDAO.findAllUsers();
    }

    public List<User> searchUsers(String keyword) throws SQLException {
        SessionManager.requireAdmin();
        return userDAO.searchUsers(keyword);
    }

    public User getUserById(int userId) throws SQLException {
        SessionManager.requireAdmin();
        return userDAO.findById(userId);
    }

    public boolean setUserStatus(int targetUserId, String newStatus) throws SQLException {
        SessionManager.requireAdmin();
        if (!"ACTIVE".equals(newStatus) && !"INACTIVE".equals(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus);
        }
        User target = userDAO.findById(targetUserId);
        if (target == null) {
            return false;
        }
        if (targetUserId == SessionManager.getCurrentUserId() && "INACTIVE".equals(newStatus)) {
            throw new IllegalStateException("You cannot deactivate the currently signed-in administrator account.");
        }
        if (target.isAdmin() && "INACTIVE".equals(newStatus) && countActiveAdmins() <= 1) {
            throw new IllegalStateException("Cannot deactivate the only active administrator account.");
        }
        boolean ok = userDAO.updateStatus(targetUserId, newStatus);
        if (ok) {
            activityLogDAO.logActivity(
                    SessionManager.getCurrentUserId(),
                    SessionManager.getCurrentUsername(),
                    "ADMIN_ACTION",
                    "Changed status of user '" + target.getUsername() + "' to " + newStatus
            );
        }
        return ok;
    }

    public boolean updateUserDetails(int targetUserId, String email, String firstName, String lastName,
                                     String accountType, String accountStatus) throws SQLException {
        SessionManager.requireAdmin();
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        if (firstName == null || firstName.trim().isEmpty() || lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name and last name are required.");
        }
        if (!"ADMIN".equals(accountType) && !"USER".equals(accountType)) {
            throw new IllegalArgumentException("Invalid account type.");
        }
        if (!"ACTIVE".equals(accountStatus) && !"INACTIVE".equals(accountStatus)) {
            throw new IllegalArgumentException("Invalid account status.");
        }
        User existing = userDAO.findById(targetUserId);
        if (existing == null) {
            return false;
        }
        if (userDAO.existsByEmailExcludingUser(email, targetUserId)) {
            throw new IllegalArgumentException("Email address is already in use by another account.");
        }
        if (existing.isAdmin() && (!"ADMIN".equals(accountType) || "INACTIVE".equals(accountStatus))
                && countActiveAdmins() <= 1) {
            throw new IllegalStateException("Cannot demote or deactivate the last active administrator account.");
        }
        boolean ok = userDAO.updateUserByAdmin(targetUserId, email, firstName, lastName, accountType, accountStatus);
        if (ok) {
            activityLogDAO.logActivity(
                    SessionManager.getCurrentUserId(),
                    SessionManager.getCurrentUsername(),
                    "ADMIN_ACTION",
                    "Updated account details for user '" + existing.getUsername() + "' (ID " + targetUserId + ")"
            );
        }
        return ok;
    }

    public boolean deleteUser(int targetUserId) throws SQLException {
        SessionManager.requireAdmin();
        if (targetUserId == SessionManager.getCurrentUserId()) {
            throw new IllegalStateException("You cannot delete your own active administrator account.");
        }
        User target = userDAO.findById(targetUserId);
        if (target == null) {
            return false;
        }
        if (target.isAdmin() && countActiveAdmins() <= 1) {
            throw new IllegalStateException("Cannot delete the last active administrator account.");
        }
        boolean ok = userDAO.deleteUser(targetUserId);
        if (ok) {
            activityLogDAO.logActivity(
                    SessionManager.getCurrentUserId(),
                    SessionManager.getCurrentUsername(),
                    "ADMIN_ACTION",
                    "Deleted user account '" + target.getUsername() + "' (ID " + targetUserId + ")"
            );
        }
        return ok;
    }

    public List<ActivityLog> getRecentActivityLogs(int limit) throws SQLException {
        SessionManager.requireAdmin();
        return activityLogDAO.findRecentLogs(limit);
    }

    public List<String> listApplicationTables() throws SQLException {
        SessionManager.requireAdmin();
        List<String> tables = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();
            try (ResultSet rs = meta.getTables(null, DatabaseConnection.SCHEMA_NAME, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        }
        Collections.sort(tables);
        return tables;
    }

    /**
     * Safely reads rows from an allowlisted schema table. Never executes arbitrary user SQL.
     * Sensitive password hash and salt columns are masked before returning to the GUI.
     */
    public TablePreview previewTable(String tableName, int maxRows) throws SQLException {
        SessionManager.requireAdmin();
        List<String> validTables = listApplicationTables();
        Set<String> validSet = new HashSet<>(validTables);
        if (tableName == null || !validSet.contains(tableName.toUpperCase())) {
            throw new IllegalArgumentException("Unknown or unauthorized table name.");
        }
        String safeTable = tableName.toUpperCase();
        String sql = "SELECT * FROM " + safeTable;

        List<String> columns = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setMaxRows(Math.max(1, maxRows));
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData rsMeta = rs.getMetaData();
                int colCount = rsMeta.getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    columns.add(rsMeta.getColumnName(i));
                }
                while (rs.next()) {
                    Object[] row = new Object[colCount];
                    for (int i = 1; i <= colCount; i++) {
                        String colName = columns.get(i - 1);
                        Object val = rs.getObject(i);
                        if ("PASSWORD_HASH".equalsIgnoreCase(colName) || "PASSWORD_SALT".equalsIgnoreCase(colName)) {
                            row[i - 1] = "[PROTECTED]";
                        } else {
                            row[i - 1] = val;
                        }
                    }
                    rows.add(row);
                }
            }
        }
        return new TablePreview(columns, rows);
    }

    public int getTableRowCount(String tableName) throws SQLException {
        SessionManager.requireAdmin();
        List<String> validTables = listApplicationTables();
        if (tableName == null || !validTables.contains(tableName.toUpperCase())) {
            throw new IllegalArgumentException("Unknown table name.");
        }
        String sql = "SELECT COUNT(*) FROM " + tableName.toUpperCase();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int countActiveAdmins() throws SQLException {
        String sql = "SELECT COUNT(*) FROM USERS WHERE account_type = 'ADMIN' AND account_status = 'ACTIVE'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User mapUserRow(ResultSet rs) throws SQLException {
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
                rs.getTimestamp("date_created"),
                rs.getTimestamp("last_login")
        );
    }
}
