package budgetly.model;

/**
 * The two kinds of accounts in BUDGETLY.
 */
public enum UserRole {
    ADMIN("Administrator"),
    USER("Regular User");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
