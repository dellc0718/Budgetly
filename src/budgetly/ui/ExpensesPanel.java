package budgetly.ui;

/**
 * Screen for recording user expenses deducted from a chosen account.
 */
public class ExpensesPanel extends TransactionEntryPanel {

    private static final String[] EXPENSE_CATEGORIES = {
        "Food", "Transport", "Bills", "Shopping", "Health", "Education", "Entertainment", "Other"
    };

    public ExpensesPanel(Runnable onDataChanged) {
        super(
                "Expenses",
                "Record expenses and select the account used.",
                "EXPENSE",
                EXPENSE_CATEGORIES,
                onDataChanged
        );
    }
}
