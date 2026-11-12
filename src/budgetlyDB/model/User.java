package budgetlyDB.model;

import java.sql.Timestamp;

/**
 * Domain model representing a registered Budgetly user account in the USERS table.
 */
public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private String email;
    private String firstName;
    private String lastName;
    private String accountType;
    private String accountStatus;
    private Timestamp dateCreated;
    private Timestamp lastLogin;

    public User() {
        this.accountType = "USER";
        this.accountStatus = "ACTIVE";
    }

    public User(int userId, String username, String passwordHash, String passwordSalt,
                String email, String firstName, String lastName,
                String accountType, String accountStatus,
                Timestamp dateCreated, Timestamp lastLogin) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.accountType = accountType;
        this.accountStatus = accountStatus;
        this.dateCreated = dateCreated;
        this.lastLogin = lastLogin;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getFullName() {
        String fn = firstName == null ? "" : firstName.trim();
        String ln = lastName == null ? "" : lastName.trim();
        return (fn + " " + ln).trim();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(accountType);
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(accountStatus);
    }

    @Override
    public String toString() {
        return "User{userId=" + userId
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", accountType='" + accountType + '\''
                + ", accountStatus='" + accountStatus + '\''
                + '}';
    }
}
