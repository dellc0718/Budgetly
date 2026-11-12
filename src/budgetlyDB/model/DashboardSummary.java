package budgetlyDB.model;

import java.math.BigDecimal;

/**
 * Aggregated dashboard metrics for the currently authenticated user.
 */
public class DashboardSummary {
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal totalAccountBalance;
    private BigDecimal overallBudgetLimit;
    private int accountCount;
    private String alertMessage;
    private String alertSeverity;

    public DashboardSummary() {
        this.monthlyIncome = BigDecimal.ZERO;
        this.monthlyExpenses = BigDecimal.ZERO;
        this.totalAccountBalance = BigDecimal.ZERO;
        this.alertMessage = "No monthly budget set yet.";
        this.alertSeverity = "NONE";
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome == null ? BigDecimal.ZERO : monthlyIncome;
    }

    public BigDecimal getMonthlyExpenses() {
        return monthlyExpenses;
    }

    public void setMonthlyExpenses(BigDecimal monthlyExpenses) {
        this.monthlyExpenses = monthlyExpenses == null ? BigDecimal.ZERO : monthlyExpenses;
    }

    public BigDecimal getTotalAccountBalance() {
        return totalAccountBalance;
    }

    public void setTotalAccountBalance(BigDecimal totalAccountBalance) {
        this.totalAccountBalance = totalAccountBalance == null ? BigDecimal.ZERO : totalAccountBalance;
    }

    public BigDecimal getOverallBudgetLimit() {
        return overallBudgetLimit;
    }

    public void setOverallBudgetLimit(BigDecimal overallBudgetLimit) {
        this.overallBudgetLimit = overallBudgetLimit;
    }

    public int getAccountCount() {
        return accountCount;
    }

    public void setAccountCount(int accountCount) {
        this.accountCount = accountCount;
    }

    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public String getAlertSeverity() {
        return alertSeverity;
    }

    public void setAlertSeverity(String alertSeverity) {
        this.alertSeverity = alertSeverity;
    }
}
