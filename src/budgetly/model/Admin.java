package budgetly.model;

import java.time.LocalDateTime;

/**
 * An administrator account. Administrators manage registered users.
 */
public class Admin extends User {

    public Admin(int userId, String fullName, String username, String email,
                 boolean active, LocalDateTime createdAt) {
        super(userId, fullName, username, email, active, createdAt);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }

    @Override
    public boolean canManageUsers() {
        return true;
    }

    @Override
    public String getDashboardTitle() {
        return "Administrator Panel";
    }
}
