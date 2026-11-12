package budgetly.ui;

/**
 * Screen for recording user income into a chosen account.
 */
public class IncomePanel extends TransactionEntryPanel {

    private static final String[] INCOME_CATEGORIES = {
        "Salary", "Business", "Freelance", "Allowance", "Gift", "Investment", "Other"
    };

    public IncomePanel(Runnable onDataChanged) {
        super(
                "Income",
                "Record income and select the account receiving it.",
                "INCOME",
                INCOME_CATEGORIES,
                onDataChanged
        );
    }
}
