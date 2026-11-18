package budgetly.model;

import java.time.LocalDateTime;

/**
 * Base class for every account. Admin and RegularUser extend it and
 * override the abstract methods (inheritance + polymorphism).
 * The password hash is intentionally NOT stored here, so it can never
 * be shown on screen by accident.
 */
public abstract class User implements Manageable {

    private int userId;
    private String fullName;
    private String username;
    private String email;
    private Boolean active;
    private LocalDateTime createdAt;

    public User(int userId, String fullName, String username, String email,
                Boolean active, LocalDateTime createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.active = active;
        this.createdAt = createdAt;
    }

    /** @return the role saved in the database for this account */
    public abstract UserRole getRole();

    /** @return true if this account may open the user-management screen */
    public abstract boolean canManageUsers();

    /** @return the heading shown at the top of the main window */
    public abstract String getDashboardTitle();

    public String getStatusText() {
        return Boolean.TRUE.equals(active) ? "Active" : "Inactive";
    }

    @Override
    public boolean matches(String keyword) {
        return username.toLowerCase().contains(keyword)
                || fullName.toLowerCase().contains(keyword);
    }

    @Override
    public String getSummary() {
        return fullName + " (@" + username + " - " + getRole().getLabel() + ")";
    }

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
