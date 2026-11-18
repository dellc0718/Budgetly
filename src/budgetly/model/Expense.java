package budgetly.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money spent by the user (food, bills, ...).
 */
public class Expense extends Transaction {

    public Expense(Integer transactionId, int userId, String description,
                   BigDecimal amount, String category, LocalDate date) {
        super(transactionId, userId, description, amount, category, date);
    }

    @Override
    public TransactionType getType() {
        return TransactionType.EXPENSE;
    }

    @Override
    public BigDecimal getSignedAmount() {
        return getAmount().negate();
    }
}
