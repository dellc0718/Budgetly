package budgetlyDB.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Domain model representing a monthly spending limit in MONTHLY_BUDGETS.
 */
public class MonthlyBudget {
    private int budgetId;
    private int userId;
    private int budgetYear;
    private int budgetMonth;
    private String category;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private Timestamp updatedAt;

    public MonthlyBudget() {
        this.category = "OVERALL";
        this.limitAmount = BigDecimal.ZERO;
        this.spentAmount = BigDecimal.ZERO;
    }

    public MonthlyBudget(int budgetId, int userId, int budgetYear, int budgetMonth,
                         String category, BigDecimal limitAmount, BigDecimal spentAmount,
                         Timestamp updatedAt) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.budgetYear = budgetYear;
        this.budgetMonth = budgetMonth;
        this.category = category;
        this.limitAmount = limitAmount == null ? BigDecimal.ZERO : limitAmount;
        this.spentAmount = spentAmount == null ? BigDecimal.ZERO : spentAmount;
        this.updatedAt = updatedAt;
    }

    public int getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(int budgetId) {
        this.budgetId = budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(int budgetYear) {
        this.budgetYear = budgetYear;
    }

    public int getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(int budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount == null ? BigDecimal.ZERO : limitAmount;
    }

    public BigDecimal getSpentAmount() {
        return spentAmount;
    }

    public void setSpentAmount(BigDecimal spentAmount) {
        this.spentAmount = spentAmount == null ? BigDecimal.ZERO : spentAmount;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
