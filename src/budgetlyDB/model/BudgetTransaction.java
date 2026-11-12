package budgetlyDB.model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Domain model representing an income or expense record in TRANSACTIONS.
 */
public class BudgetTransaction {
    private int transactionId;
    private int userId;
    private int accountId;
    private String accountName;
    private String transactionType;
    private String category;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;
    private Timestamp createdAt;

    public BudgetTransaction() {
        this.amount = BigDecimal.ZERO;
        this.transactionDate = LocalDate.now();
    }

    public BudgetTransaction(int transactionId, int userId, int accountId, String accountName,
                             String transactionType, String category, BigDecimal amount,
                             String description, LocalDate transactionDate, Timestamp createdAt) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.accountId = accountId;
        this.accountName = accountName;
        this.transactionType = transactionType;
        this.category = category;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.description = description;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isIncome() {
        return "INCOME".equalsIgnoreCase(transactionType);
    }
}
