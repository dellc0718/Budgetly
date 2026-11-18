package budgetly.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Money received by the user (salary, allowance, ...).
 */
public class Income extends Transaction {

    public Income(Integer transactionId, int userId, String description,
                  BigDecimal amount, String category, LocalDate date) {
        super(transactionId, userId, description, amount, category, date);
    }

    @Override
    public TransactionType getType() {
        return TransactionType.INCOME;
    }

    @Override
    public BigDecimal getSignedAmount() {
        return getAmount();
    }
}
