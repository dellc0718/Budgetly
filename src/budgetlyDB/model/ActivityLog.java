package budgetlyDB.model;

import java.sql.Timestamp;

/**
 * Domain model representing an entry in USER_ACTIVITY_LOGS.
 */
public class ActivityLog {
    private int logId;
    private Integer userId;
    private String usernameSnapshot;
    private String activityType;
    private String activityDescription;
    private Timestamp activityDate;

    public ActivityLog() {
    }

    public ActivityLog(int logId, Integer userId, String usernameSnapshot,
                       String activityType, String activityDescription, Timestamp activityDate) {
        this.logId = logId;
        this.userId = userId;
        this.usernameSnapshot = usernameSnapshot;
        this.activityType = activityType;
        this.activityDescription = activityDescription;
        this.activityDate = activityDate;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsernameSnapshot() {
        return usernameSnapshot;
    }

    public void setUsernameSnapshot(String usernameSnapshot) {
        this.usernameSnapshot = usernameSnapshot;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }

    public Timestamp getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(Timestamp activityDate) {
        this.activityDate = activityDate;
    }
}
