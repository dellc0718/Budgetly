package budgetly.model;

/**
 * Type of a money transaction. Each type also owns its list of categories,
 * so the category combo box always shows the right choices.
 */
public enum TransactionType {
    INCOME("Income", "Salary", "Allowance", "Business", "Freelance", "Gift", "Other"),
    EXPENSE("Expense", "Food", "Transportation", "Bills & Utilities", "Shopping",
            "Education", "Health", "Entertainment", "Other");

    private final String label;
    private final String[] categories;

    TransactionType(String label, String... categories) {
        this.label = label;
        this.categories = categories;
    }

    public String getLabel() {
        return label;
    }

    public String[] getCategories() {
        return categories.clone();
    }
}
