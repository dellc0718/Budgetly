package budgetlyDB;

import budgetlyDB.model.UserSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object for USER_SETTINGS.
 */
public class SettingsDAO {

    public UserSettings findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM USER_SETTINGS WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public int insertSettings(Connection con, UserSettings settings) throws SQLException {
        String sql = "INSERT INTO USER_SETTINGS "
                + "(user_id, theme, notification_preferences, budget_alerts_enabled, email_summary_enabled, currency_preference, default_budget_alert_percent) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, settings.getUserId());
            ps.setString(2, safe(settings.getTheme(), "LIGHT"));
            ps.setString(3, safe(settings.getNotificationPreferences(), "BUDGET_AND_SECURITY"));
            ps.setBoolean(4, settings.isBudgetAlertsEnabled());
            ps.setBoolean(5, settings.isEmailSummaryEnabled());
            ps.setString(6, safe(settings.getCurrencyPreference(), "PHP"));
            ps.setInt(7, clampPercent(settings.getDefaultBudgetAlertPercent()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    settings.setSettingId(id);
                    return id;
                }
                throw new SQLException("Inserting settings did not return a generated setting_id.");
            }
        }
    }

    public boolean upsertSettings(UserSettings settings) throws SQLException {
        UserSettings existing = findByUserId(settings.getUserId());
        if (existing == null) {
            try (Connection con = DatabaseConnection.getConnection()) {
                insertSettings(con, settings);
                return true;
            }
        }
        String sql = "UPDATE USER_SETTINGS SET "
                + "theme = ?, notification_preferences = ?, budget_alerts_enabled = ?, "
                + "email_summary_enabled = ?, currency_preference = ?, default_budget_alert_percent = ?, "
                + "updated_at = CURRENT_TIMESTAMP "
                + "WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, safe(settings.getTheme(), "LIGHT"));
            ps.setString(2, safe(settings.getNotificationPreferences(), "BUDGET_AND_SECURITY"));
            ps.setBoolean(3, settings.isBudgetAlertsEnabled());
            ps.setBoolean(4, settings.isEmailSummaryEnabled());
            ps.setString(5, safe(settings.getCurrencyPreference(), "PHP"));
            ps.setInt(6, clampPercent(settings.getDefaultBudgetAlertPercent()));
            ps.setInt(7, settings.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    private UserSettings mapRow(ResultSet rs) throws SQLException {
        return new UserSettings(
                rs.getInt("setting_id"),
                rs.getInt("user_id"),
                rs.getString("theme"),
                rs.getString("notification_preferences"),
                rs.getBoolean("budget_alerts_enabled"),
                rs.getBoolean("email_summary_enabled"),
                rs.getString("currency_preference"),
                rs.getInt("default_budget_alert_percent"),
                rs.getTimestamp("updated_at")
        );
    }

    private static int clampPercent(int pct) {
        if (pct < 10) {
            return 10;
        }
        if (pct > 100) {
            return 100;
        }
        return pct;
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }
}
