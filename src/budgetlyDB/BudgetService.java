package budgetlyDB;

import budgetlyDB.model.BudgetTransaction;
import budgetlyDB.model.DashboardSummary;
import budgetlyDB.model.FinancialAccount;
import budgetlyDB.model.MonthlyBudget;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * Service layer coordinating all user-isolated financial operations (accounts, income,
 * expenses, monthly budgets, and transaction history) using atomic JDBC transactions.
 */
public class BudgetService {

    private final FinancialAccountDAO accountDAO = new FinancialAccountDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final MonthlyBudgetDAO budgetDAO = new MonthlyBudgetDAO();
    private final ActivityLogDAO activityLogDAO = new ActivityLogDAO();

    public DashboardSummary getDashboardSummary() throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        YearMonth now = YearMonth.now();

        BigDecimal income = transactionDAO.getMonthlyTotal(userId, "INCOME", now.getYear(), now.getMonthValue());
        BigDecimal expenses = transactionDAO.getMonthlyTotal(userId, "EXPENSE", now.getYear(), now.getMonthValue());
        BigDecimal balance = accountDAO.getTotalBalance(userId);
        List<FinancialAccount> accounts = accountDAO.findByUserId(userId);
        BigDecimal overallLimit = budgetDAO.findOverallBudgetLimit(userId, now.getYear(), now.getMonthValue());

        DashboardSummary summary = new DashboardSummary();
        summary.setMonthlyIncome(income);
        summary.setMonthlyExpenses(expenses);
        summary.setTotalAccountBalance(balance);
        summary.setAccountCount(accounts.size());
        summary.setOverallBudgetLimit(overallLimit);

        if (overallLimit == null || overallLimit.compareTo(BigDecimal.ZERO) <= 0) {
            summary.setAlertMessage("No monthly budget set yet.");
            summary.setAlertSeverity("NONE");
        } else {
            BigDecimal pct = expenses.multiply(BigDecimal.valueOf(100))
                    .divide(overallLimit, 1, RoundingMode.HALF_UP);
            int alertThreshold = SessionManager.getCurrentSettings() != null
                    ? SessionManager.getCurrentSettings().getDefaultBudgetAlertPercent()
                    : 80;
            boolean alertsOn = SessionManager.getCurrentSettings() == null
                    || SessionManager.getCurrentSettings().isBudgetAlertsEnabled();

            if (!alertsOn) {
                summary.setAlertMessage("Monthly limit: " + formatMoney(overallLimit)
                        + " (" + pct + "% used — alerts muted in Settings).");
                summary.setAlertSeverity("INFO");
            } else if (expenses.compareTo(overallLimit) > 0) {
                BigDecimal over = expenses.subtract(overallLimit);
                summary.setAlertMessage("OVER BUDGET! Spent " + formatMoney(expenses)
                        + " of " + formatMoney(overallLimit) + " (" + pct + "%, +" + formatMoney(over) + " over limit).");
                summary.setAlertSeverity("EXCEEDED");
            } else if (pct.compareTo(BigDecimal.valueOf(alertThreshold)) >= 0) {
                BigDecimal remaining = overallLimit.subtract(expenses);
                summary.setAlertMessage("Approaching monthly limit (" + pct + "% used). Remaining: "
                        + formatMoney(remaining) + " of " + formatMoney(overallLimit) + ".");
                summary.setAlertSeverity("WARNING");
            } else {
                BigDecimal remaining = overallLimit.subtract(expenses);
                summary.setAlertMessage("On track: " + formatMoney(expenses) + " spent (" + pct
                        + "% of " + formatMoney(overallLimit) + "). Remaining: " + formatMoney(remaining) + ".");
                summary.setAlertSeverity("OK");
            }
        }
        return summary;
    }

    public List<FinancialAccount> getUserAccounts() throws SQLException {
        return accountDAO.findByUserId(SessionManager.getCurrentUserId());
    }

    public int createAccount(String accountName, String accountType, BigDecimal initialBalance, String notes)
            throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name is required.");
        }
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        FinancialAccount account = new FinancialAccount();
        account.setUserId(userId);
        account.setAccountName(accountName.trim());
        account.setAccountType(accountType == null ? "CASH" : accountType.trim().toUpperCase());
        account.setBalance(initialBalance);
        account.setNotes(notes == null ? "" : notes.trim());

        int id = accountDAO.insertAccount(account);
        activityLogDAO.logActivity(userId, SessionManager.getCurrentUsername(), "ACCOUNT_CREATED",
                "Created " + account.getAccountType() + " account '" + account.getAccountName() + "'.");
        return id;
    }

    public boolean updateAccount(int accountId, String accountName, String accountType, String notes)
            throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name is required.");
        }
        return accountDAO.updateAccountDetails(accountId, userId, accountName, accountType, notes);
    }

    public boolean deleteAccount(int accountId) throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        boolean deleted = accountDAO.deleteAccount(accountId, userId);
        if (deleted) {
            activityLogDAO.logActivity(userId, SessionManager.getCurrentUsername(), "ACCOUNT_DELETED",
                    "Deleted financial account ID " + accountId + ".");
        }
        return deleted;
    }

    /**
     * Records an INCOME or EXPENSE transaction and atomically updates the selected
     * account balance inside a single JDBC transaction.
     */
    public int recordTransaction(int accountId, String type, String category,
                                 BigDecimal amount, String description, LocalDate date) throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Please select a category.");
        }
        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        if (!"INCOME".equals(normalizedType) && !"EXPENSE".equals(normalizedType)) {
            throw new IllegalArgumentException("Transaction type must be INCOME or EXPENSE.");
        }
        LocalDate txDate = date != null ? date : LocalDate.now();

        try (Connection con = DatabaseConnection.getConnection()) {
            boolean prevAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                FinancialAccount account = accountDAO.findByIdForUser(con, accountId, userId);
                if (account == null) {
                    throw new SecurityException("Selected financial account was not found for your user profile.");
                }

                BudgetTransaction tx = new BudgetTransaction();
                tx.setUserId(userId);
                tx.setAccountId(accountId);
                tx.setTransactionType(normalizedType);
                tx.setCategory(category.trim());
                tx.setAmount(amount);
                tx.setDescription(description == null || description.trim().isEmpty()
                        ? category.trim() : description.trim());
                tx.setTransactionDate(txDate);

                int txId = transactionDAO.insertTransaction(con, tx);
                BigDecimal delta = "INCOME".equals(normalizedType) ? amount : amount.negate();
                accountDAO.adjustBalance(con, accountId, userId, delta);

                activityLogDAO.logActivity(con, userId, SessionManager.getCurrentUsername(), "TRANSACTION",
                        normalizedType + " of " + formatMoney(amount) + " (" + tx.getCategory()
                        + ") on account '" + account.getAccountName() + "'.");

                con.commit();
                return txId;
            } catch (Exception ex) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                }
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }
                throw new SQLException(ex);
            } finally {
                con.setAutoCommit(prevAuto);
            }
        }
    }

    /**
     * Deletes a transaction belonging to the active user and atomically reverses
     * its effect on the associated account's balance.
     */
    public boolean deleteTransaction(int transactionId) throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        try (Connection con = DatabaseConnection.getConnection()) {
            boolean prevAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                BudgetTransaction tx = transactionDAO.findByIdForUser(con, transactionId, userId);
                if (tx == null) {
                    con.rollback();
                    return false;
                }
                BigDecimal reversal = tx.isIncome() ? tx.getAmount().negate() : tx.getAmount();
                accountDAO.adjustBalance(con, tx.getAccountId(), userId, reversal);
                boolean deleted = transactionDAO.deleteByIdForUser(con, transactionId, userId);
                if (deleted) {
                    activityLogDAO.logActivity(con, userId, SessionManager.getCurrentUsername(), "TRANSACTION_DELETED",
                            "Deleted " + tx.getTransactionType() + " transaction #" + transactionId
                            + " (" + formatMoney(tx.getAmount()) + ").");
                }
                con.commit();
                return deleted;
            } catch (SQLException ex) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                throw ex;
            } finally {
                con.setAutoCommit(prevAuto);
            }
        }
    }

    /** Updates a user-owned transaction and both affected account balances atomically. */
    public boolean updateTransaction(int transactionId, int accountId, String type, String category,
                                     BigDecimal amount, String description, LocalDate date) throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        if (!"INCOME".equals(normalizedType) && !"EXPENSE".equals(normalizedType)) {
            throw new IllegalArgumentException("Select Income or Expense.");
        }
        if (category == null || category.trim().isEmpty() || amount == null
                || amount.signum() <= 0 || date == null) {
            throw new IllegalArgumentException("Category, positive amount, and date are required.");
        }
        try (Connection con = DatabaseConnection.getConnection()) {
            boolean previousAuto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                BudgetTransaction old = transactionDAO.findByIdForUser(con, transactionId, userId);
                FinancialAccount account = accountDAO.findByIdForUser(con, accountId, userId);
                if (old == null || account == null) {
                    con.rollback();
                    return false;
                }
                BigDecimal reversal = old.isIncome() ? old.getAmount().negate() : old.getAmount();
                BigDecimal replacement = "INCOME".equals(normalizedType) ? amount : amount.negate();
                accountDAO.adjustBalance(con, old.getAccountId(), userId, reversal);
                accountDAO.adjustBalance(con, accountId, userId, replacement);
                old.setAccountId(accountId);
                old.setTransactionType(normalizedType);
                old.setCategory(category.trim());
                old.setAmount(amount);
                old.setDescription(description == null || description.trim().isEmpty()
                        ? category.trim() : description.trim());
                old.setTransactionDate(date);
                if (!transactionDAO.updateForUser(con, old)) {
                    con.rollback();
                    return false;
                }
                activityLogDAO.logActivity(con, userId, SessionManager.getCurrentUsername(),
                        "TRANSACTION_UPDATED", "Updated transaction #" + transactionId + ".");
                con.commit();
                return true;
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(previousAuto);
            }
        }
    }

    public List<BudgetTransaction> searchTransactions(String typeFilter, String categoryFilter,
                                                      Integer accountIdFilter, String keyword) throws SQLException {
        return transactionDAO.searchTransactions(
                SessionManager.getCurrentUserId(),
                typeFilter,
                categoryFilter,
                accountIdFilter,
                keyword
        );
    }

    public List<BudgetTransaction> getRecentTransactions(int limit) throws SQLException {
        return transactionDAO.findRecentByUser(SessionManager.getCurrentUserId(), limit);
    }

    public Map<String, BigDecimal> getMonthlyExpensesByCategory(int year, int month) throws SQLException {
        return transactionDAO.getMonthlyExpensesByCategory(SessionManager.getCurrentUserId(), year, month);
    }

    public Map<YearMonth, BigDecimal[]> getMonthlyTrend() throws SQLException {
        return transactionDAO.getMonthlyTrend(SessionManager.getCurrentUserId(), YearMonth.now());
    }

    public List<MonthlyBudget> getMonthlyBudgets(int year, int month) throws SQLException {
        return budgetDAO.findBudgetsWithSpent(SessionManager.getCurrentUserId(), year, month);
    }

    public void saveMonthlyBudget(int year, int month, String category, BigDecimal limitAmount) throws SQLException {
        int userId = SessionManager.getCurrentUserId();
        if (limitAmount == null || limitAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Budget limit must be greater than zero.");
        }
        String cat = (category == null || category.trim().isEmpty()) ? "OVERALL" : category.trim();
        budgetDAO.saveOrUpdateBudget(userId, year, month, cat, limitAmount);
        activityLogDAO.logActivity(userId, SessionManager.getCurrentUsername(), "BUDGET_UPDATED",
                "Set " + cat + " budget for " + year + "-" + String.format("%02d", month)
                + " to " + formatMoney(limitAmount) + ".");
    }

    public boolean deleteMonthlyBudget(int budgetId) throws SQLException {
        return budgetDAO.deleteBudget(budgetId, SessionManager.getCurrentUserId());
    }

    public static String formatMoney(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String prefix = SessionManager.getCurrencyPrefix();
        if (safe.compareTo(BigDecimal.ZERO) < 0) {
            return "-" + prefix + df.format(safe.abs());
        }
        return prefix + df.format(safe);
    }
}
