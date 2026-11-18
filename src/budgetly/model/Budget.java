package budgetly.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The spending limit a user sets for one month.
 */
public class Budget {

    private Integer budgetId;
    private int userId;
    private BigDecimal monthlyLimit;
    private int month;
    private int year;

    public Budget(Integer budgetId, int userId, BigDecimal monthlyLimit, int month, int year) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.monthlyLimit = monthlyLimit;
        this.month = month;
        this.year = year;
    }

    /** @return how much money is left (negative when over budget) */
    public BigDecimal getRemaining(BigDecimal spent) {
        return monthlyLimit.subtract(spent);
    }

    /** @return true when the spending reached or passed the limit */
    public boolean isReached(BigDecimal spent) {
        return spent.compareTo(monthlyLimit) >= 0;
    }

    /** @return percentage of the monthly budget used (0.0 to 100.0+) as a Double wrapper */
    public Double getUsagePercentage(BigDecimal spent) {
        if (monthlyLimit == null || monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(monthlyLimit, 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public Integer getBudgetId() {
        return budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }
}
