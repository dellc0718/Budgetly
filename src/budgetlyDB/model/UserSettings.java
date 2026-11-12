package budgetlyDB.model;

import java.sql.Timestamp;

/**
 * Domain model representing per-user application settings stored in USER_SETTINGS.
 */
public class UserSettings {
    private int settingId;
    private int userId;
    private String theme;
    private String notificationPreferences;
    private boolean budgetAlertsEnabled;
    private boolean emailSummaryEnabled;
    private String currencyPreference;
    private int defaultBudgetAlertPercent;
    private Timestamp updatedAt;

    public UserSettings() {
        this.theme = "LIGHT";
        this.notificationPreferences = "BUDGET_AND_SECURITY";
        this.budgetAlertsEnabled = true;
        this.emailSummaryEnabled = false;
        this.currencyPreference = "PHP";
        this.defaultBudgetAlertPercent = 80;
    }

    public UserSettings(int settingId, int userId, String theme, String notificationPreferences,
                        boolean budgetAlertsEnabled, boolean emailSummaryEnabled,
                        String currencyPreference, int defaultBudgetAlertPercent, Timestamp updatedAt) {
        this.settingId = settingId;
        this.userId = userId;
        this.theme = theme;
        this.notificationPreferences = notificationPreferences;
        this.budgetAlertsEnabled = budgetAlertsEnabled;
        this.emailSummaryEnabled = emailSummaryEnabled;
        this.currencyPreference = currencyPreference;
        this.defaultBudgetAlertPercent = defaultBudgetAlertPercent;
        this.updatedAt = updatedAt;
    }

    public int getSettingId() {
        return settingId;
    }

    public void setSettingId(int settingId) {
        this.settingId = settingId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getNotificationPreferences() {
        return notificationPreferences;
    }

    public void setNotificationPreferences(String notificationPreferences) {
        this.notificationPreferences = notificationPreferences;
    }

    public boolean isBudgetAlertsEnabled() {
        return budgetAlertsEnabled;
    }

    public void setBudgetAlertsEnabled(boolean budgetAlertsEnabled) {
        this.budgetAlertsEnabled = budgetAlertsEnabled;
    }

    public boolean isEmailSummaryEnabled() {
        return emailSummaryEnabled;
    }

    public void setEmailSummaryEnabled(boolean emailSummaryEnabled) {
        this.emailSummaryEnabled = emailSummaryEnabled;
    }

    public String getCurrencyPreference() {
        return currencyPreference;
    }

    public void setCurrencyPreference(String currencyPreference) {
        this.currencyPreference = currencyPreference;
    }

    public int getDefaultBudgetAlertPercent() {
        return defaultBudgetAlertPercent;
    }

    public void setDefaultBudgetAlertPercent(int defaultBudgetAlertPercent) {
        this.defaultBudgetAlertPercent = defaultBudgetAlertPercent;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns the display prefix corresponding to the saved currency code.
     */
    public String getCurrencyPrefix() {
        if (currencyPreference == null) {
            return "PHP ";
        }
        switch (currencyPreference.toUpperCase()) {
            case "USD":
                return "USD ";
            case "EUR":
                return "EUR ";
            case "JPY":
                return "JPY ";
            case "GBP":
                return "GBP ";
            case "PHP":
            default:
                return "PHP ";
        }
    }
}
