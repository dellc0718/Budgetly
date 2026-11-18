package budgetly.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Abstract parent of Income and Expense. It holds the shared fields;
 * each subclass decides its own type and how it affects the balance.
 */
public abstract class Transaction implements Manageable {

    // Integer (wrapper) instead of int: the id is null until the record is saved.
    private Integer transactionId;
    private int userId;
    private String description;
    private BigDecimal amount;
    private String category;
    private LocalDate date;

    public Transaction(Integer transactionId, int userId, String description,
                       BigDecimal amount, String category, LocalDate date) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
    }

    /**
     * Creates the correct subclass for the given type.
     */
    public static Transaction create(TransactionType type, Integer transactionId, int userId,
                                     String description, BigDecimal amount,
                                     String category, LocalDate date) {
        if (type == TransactionType.INCOME) {
            return new Income(transactionId, userId, description, amount, category, date);
        }
        return new Expense(transactionId, userId, description, amount, category, date);
    }

    /** @return INCOME or EXPENSE */
    public abstract TransactionType getType();

    /** @return the amount with a sign: positive adds to the balance, negative subtracts */
    public abstract BigDecimal getSignedAmount();

    @Override
    public boolean matches(String keyword) {
        return description.toLowerCase().contains(keyword)
                || category.toLowerCase().contains(keyword)
                || getType().getLabel().toLowerCase().contains(keyword);
    }

    @Override
    public String getSummary() {
        return getType().getLabel() + ": " + description + " (PHP " + amount.toPlainString() + ")";
    }

    public Integer getTransactionId() {
        return transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
