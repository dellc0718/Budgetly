package budgetlyDB.model;

import java.sql.Timestamp;

/**
 * Specialized User model representing an Administrator account with elevated privileges.
 */
public class Admin extends User {
    private String roleTitle;
    private boolean canManageUsers;
    private boolean canManageDatabase;

    public Admin() {
        super();
        setAccountType("ADMIN");
        this.roleTitle = "System Administrator";
        this.canManageUsers = true;
        this.canManageDatabase = true;
    }

    public Admin(int userId, String username, String passwordHash, String passwordSalt,
                 String email, String firstName, String lastName,
                 String accountStatus, Timestamp dateCreated, Timestamp lastLogin) {
        super(userId, username, passwordHash, passwordSalt, email, firstName, lastName,
                "ADMIN", accountStatus, dateCreated, lastLogin);
        this.roleTitle = "System Administrator";
        this.canManageUsers = true;
        this.canManageDatabase = true;
    }

    public static Admin fromUser(User user) {
        if (user == null) {
            return null;
        }
        return new Admin(
                user.getUserId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getPasswordSalt(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getAccountStatus(),
                user.getDateCreated(),
                user.getLastLogin()
        );
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public boolean isCanManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }

    public boolean isCanManageDatabase() {
        return canManageDatabase;
    }

    public void setCanManageDatabase(boolean canManageDatabase) {
        this.canManageDatabase = canManageDatabase;
    }
}
