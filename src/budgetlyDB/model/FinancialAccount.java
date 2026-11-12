package budgetlyDB.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Domain model representing a user's cash, bank, or e-wallet account in FINANCIAL_ACCOUNTS.
 */
public class FinancialAccount {
    private int accountId;
    private int userId;
    private String accountName;
    private String accountType;
    private BigDecimal balance;
    private String notes;
    private Timestamp dateCreated;

    public FinancialAccount() {
        this.balance = BigDecimal.ZERO;
    }

    public FinancialAccount(int accountId, int userId, String accountName, String accountType,
                            BigDecimal balance, String notes, Timestamp dateCreated) {
        this.accountId = accountId;
        this.userId = userId;
        this.accountName = accountName;
        this.accountType = accountType;
        this.balance = balance == null ? BigDecimal.ZERO : balance;
        this.notes = notes;
        this.dateCreated = dateCreated;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance == null ? BigDecimal.ZERO : balance;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }

    @Override
    public String toString() {
        return accountName + " (" + budgetlyDB.ui.UiLabels.label(accountType) + ")";
    }
}
