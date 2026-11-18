package budgetly.model;

import java.time.LocalDateTime;

/**
 * A normal account that tracks its own income, expenses and budget.
 */
public class RegularUser extends User {

    public RegularUser(int userId, String fullName, String username, String email,
                       boolean active, LocalDateTime createdAt) {
        super(userId, fullName, username, email, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.USER;
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }

    @Override
    public String getDashboardTitle() {
        return "My Budget Dashboard";
    }
}
