package budgetly.service;

import budgetly.dao.BudgetDAO;
import budgetly.dao.TransactionDAO;
import budgetly.model.Budget;
import budgetly.model.Manageable;
import budgetly.model.Transaction;
import budgetly.model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Business logic for income, expenses, budgets, searching and sorting.
 * All money math uses BigDecimal so centavos are never lost to rounding errors.
 */
public class BudgetService {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final BudgetDAO budgetDAO = new BudgetDAO();

    /**
     * Validates the form fields and inserts or updates a transaction.
     * @param transactionId null when adding a new record, or the existing ID when editing
     * @return validation error message, or null when saved successfully
     */
    public String saveTransaction(Integer transactionId, int userId, TransactionType type,
                                  String description, String amountText,
                                  String category, String dateText) throws SQLException {
        if (description == null || description.trim().isEmpty()) {
            return "Description is required.";
        }
        if (description.trim().length() > 150) {
            return "Description must not exceed 150 characters.";
        }
        if (category == null || category.trim().isEmpty()) {
            return "Please select a category.";
        }
        BigDecimal amount = parsePositiveAmount(amountText);
        if (amount == null) {
            return "Amount must be a valid number greater than zero (example: 1250.50).";
        }
        LocalDate date = parseDate(dateText);
        if (date == null) {
            return "Date must use the format YYYY-MM-DD (example: " + LocalDate.now() + ").";
        }

        Transaction tx = Transaction.create(
                type, transactionId, userId, description.trim(), amount, category.trim(), date);
        if (transactionId == null) {
            transactionDAO.insert(tx);
        } else if (!transactionDAO.update(tx)) {
            return "Could not update record. Make sure it belongs to your account.";
        }
        return null;
    }

    public boolean deleteTransaction(int transactionId, int userId) throws SQLException {
        return transactionDAO.delete(transactionId, userId);
    }

    public List<Transaction> getUserTransactions(int userId) throws SQLException {
        return transactionDAO.findByUser(userId);
    }

    /**
     * Filters a transaction list by type (or returns all when typeFilter is null).
     */
    public List<Transaction> filterByType(List<Transaction> transactions, TransactionType typeFilter) {
        List<Transaction> filtered = new ArrayList<>();
        for (Transaction tx : transactions) {
            if (typeFilter == null || tx.getType() == typeFilter) {
                filtered.add(tx);
            }
        }
        return filtered;
    }

    /**
     * Generic search method: works on List<User>, List<Transaction>, or any
     * class that implements the Manageable interface.
     */
    public static <T extends Manageable> List<T> search(List<T> items, String keyword) {
        String clean = keyword == null ? "" : keyword.trim().toLowerCase();
        if (clean.isEmpty()) {
            return new ArrayList<>(items);
        }
        List<T> matches = new ArrayList<>();
        for (T item : items) {
            if (item.matches(clean)) {
                matches.add(item);
            }
        }
        return matches;
    }

    /**
     * Generic sorting method using Collections.sort and a Comparator<T>.
     */
    public static <T> List<T> sort(List<T> items, Comparator<T> comparator) {
        List<T> sorted = new ArrayList<>(items);
        Collections.sort(sorted, comparator);
        return sorted;
    }

    /**
     * Sorts transactions according to the option chosen in the sort combo box.
     */
    public List<Transaction> sortTransactions(List<Transaction> items, String sortOption) {
        Comparator<Transaction> byDateDesc = Comparator
                .comparing(Transaction::getDate)
                .thenComparing(Transaction::getTransactionId)
                .reversed();
        if ("Date (Oldest First)".equals(sortOption)) {
            return sort(items, byDateDesc.reversed());
        }
        if ("Amount (High to Low)".equals(sortOption)) {
            return sort(items, Comparator.comparing(Transaction::getAmount).reversed());
        }
        if ("Amount (Low to High)".equals(sortOption)) {
            return sort(items, Comparator.comparing(Transaction::getAmount));
        }
        return sort(items, byDateDesc);
    }

    /** Sum of all INCOME or all EXPENSE records in the given list. */
    public BigDecimal calculateTotal(List<Transaction> transactions, TransactionType type) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            if (tx.getType() == type) {
                total = total.add(tx.getAmount());
            }
        }
        return total;
    }

    /**
     * Current Balance = Total Income - Total Expenses.
     * Also demonstrates polymorphism through Transaction.getSignedAmount().
     */
    public BigDecimal calculateBalance(List<Transaction> transactions) {
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction tx : transactions) {
            balance = balance.add(tx.getSignedAmount());
        }
        return balance;
    }

    // ---------------- Monthly Budget operations ----------------

    public Budget getBudget(int userId, int month, int year) throws SQLException {
        return budgetDAO.findBudget(userId, month, year);
    }

    public BigDecimal getMonthlyExpenses(int userId, int month, int year) throws SQLException {
        return transactionDAO.getMonthlyExpenses(userId, month, year);
    }

    /**
     * Creates or updates the user's monthly budget for the selected month and year.
     */
    public String saveBudget(int userId, String limitText, int month, int year) throws SQLException {
        BigDecimal limit = parsePositiveAmount(limitText);
        if (limit == null) {
            return "Monthly budget limit must be a valid number greater than zero.";
        }
        if (month < 1 || month > 12) {
            return "Month must be between 1 and 12.";
        }
        Budget existing = budgetDAO.findBudget(userId, month, year);
        if (existing == null) {
            budgetDAO.insert(new Budget(null, userId, limit, month, year));
        } else {
            existing.setMonthlyLimit(limit);
            budgetDAO.update(existing);
        }
        return null;
    }

    public static String formatMoney(BigDecimal amount) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount;
        return String.format("PHP %,.2f", safe.setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal parsePositiveAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(",", ""));
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(MAX_AMOUNT) > 0) {
                return null;
            }
            return value.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
