package budgetlyDB;

import budgetlyDB.model.ActivityLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for USER_ACTIVITY_LOGS.
 */
public class ActivityLogDAO {

    public void logActivity(Integer userId, String usernameSnapshot, String activityType, String description) {
        try (Connection con = DatabaseConnection.getConnection()) {
            logActivity(con, userId, usernameSnapshot, activityType, description);
        } catch (SQLException ex) {
            DatabaseConnection.recordError("Activity Logging", ex);
        }
    }

    public void logActivity(Connection con, Integer userId, String usernameSnapshot,
                            String activityType, String description) throws SQLException {
        String sql = "INSERT INTO USER_ACTIVITY_LOGS "
                + "(user_id, username_snapshot, activity_type, activity_description) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, usernameSnapshot == null ? "system" : trimTo(usernameSnapshot, 50));
            ps.setString(3, trimTo(activityType == null ? "INFO" : activityType, 40));
            ps.setString(4, trimTo(description == null ? "" : description, 500));
            ps.executeUpdate();
        }
    }

    public List<ActivityLog> findRecentLogs(int limit) throws SQLException {
        String sql = "SELECT * FROM USER_ACTIVITY_LOGS ORDER BY activity_date DESC, log_id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setMaxRows(Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }

    public List<ActivityLog> findLogsForUser(int userId, int limit) throws SQLException {
        String sql = "SELECT * FROM USER_ACTIVITY_LOGS WHERE user_id = ? ORDER BY activity_date DESC, log_id DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setMaxRows(Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        int rawUid = rs.getInt("user_id");
        Integer uid = rs.wasNull() ? null : rawUid;
        return new ActivityLog(
                rs.getInt("log_id"),
                uid,
                rs.getString("username_snapshot"),
                rs.getString("activity_type"),
                rs.getString("activity_description"),
                rs.getTimestamp("activity_date")
        );
    }

    private static String trimTo(String s, int maxLen) {
        String t = s.trim();
        return t.length() <= maxLen ? t : t.substring(0, maxLen);
    }
}
