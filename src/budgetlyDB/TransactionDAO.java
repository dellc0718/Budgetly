package budgetlyDB;

import budgetlyDB.model.BudgetTransaction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for user-isolated TRANSACTIONS (Income and Expenses).
 */
public class TransactionDAO {

    public int insertTransaction(Connection con, BudgetTransaction tx) throws SQLException {
        String sql = "INSERT INTO TRANSACTIONS "
                + "(user_id, account_id, transaction_type, category, amount, description, transaction_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getUserId());
            ps.setInt(2, tx.getAccountId());
            ps.setString(3, tx.getTransactionType().trim().toUpperCase());
            ps.setString(4, tx.getCategory().trim());
            ps.setBigDecimal(5, tx.getAmount());
            ps.setString(6, tx.getDescription() == null ? "" : tx.getDescription().trim());
            ps.setDate(7, Date.valueOf(tx.getTransactionDate()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    tx.setTransactionId(id);
                    return id;
                }
                throw new SQLException("Inserting transaction did not return a generated transaction_id.");
            }
        }
    }

    public BudgetTransaction findByIdForUser(Connection con, int transactionId, int userId) throws SQLException {
        String sql = "SELECT t.*, a.account_name FROM TRANSACTIONS t "
                + "JOIN FINANCIAL_ACCOUNTS a ON t.account_id = a.account_id "
                + "WHERE t.transaction_id = ? AND t.user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean deleteByIdForUser(Connection con, int transactionId, int userId) throws SQLException {
        String sql = "DELETE FROM TRANSACTIONS WHERE transaction_id = ? AND user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateForUser(Connection con, BudgetTransaction tx) throws SQLException {
        String sql = "UPDATE TRANSACTIONS SET account_id = ?, transaction_type = ?, category = ?, "
                + "amount = ?, description = ?, transaction_date = ? "
                + "WHERE transaction_id = ? AND user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, tx.getAccountId());
            ps.setString(2, tx.getTransactionType());
            ps.setString(3, tx.getCategory());
            ps.setBigDecimal(4, tx.getAmount());
            ps.setString(5, tx.getDescription());
            ps.setDate(6, Date.valueOf(tx.getTransactionDate()));
            ps.setInt(7, tx.getTransactionId());
            ps.setInt(8, tx.getUserId());
            return ps.executeUpdate() == 1;
        }
    }

    public List<BudgetTransaction> findRecentByUser(int userId, int limit) throws SQLException {
        String sql = "SELECT t.*, a.account_name FROM TRANSACTIONS t "
                + "JOIN FINANCIAL_ACCOUNTS a ON t.account_id = a.account_id "
                + "WHERE t.user_id = ? "
                + "ORDER BY t.transaction_date DESC, t.transaction_id DESC";
        List<BudgetTransaction> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setMaxRows(Math.max(1, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<BudgetTransaction> searchTransactions(int userId, String typeFilter,
                                                      String categoryFilter, Integer accountIdFilter,
                                                      String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, a.account_name FROM TRANSACTIONS t "
                + "JOIN FINANCIAL_ACCOUNTS a ON t.account_id = a.account_id "
                + "WHERE t.user_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (typeFilter != null && !"ALL".equalsIgnoreCase(typeFilter)) {
            sql.append("AND t.transaction_type = ? ");
            params.add(typeFilter.toUpperCase());
        }
        if (categoryFilter != null && !"ALL".equalsIgnoreCase(categoryFilter)) {
            sql.append("AND t.category = ? ");
            params.add(categoryFilter);
        }
        if (accountIdFilter != null && accountIdFilter > 0) {
            sql.append("AND t.account_id = ? ");
            params.add(accountIdFilter);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (LOWER(t.description) LIKE ? OR LOWER(t.category) LIKE ? OR LOWER(a.account_name) LIKE ?) ");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        sql.append("ORDER BY t.transaction_date DESC, t.transaction_id DESC");

        List<BudgetTransaction> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public BigDecimal getMonthlyTotal(int userId, String type, int year, int month) throws SQLException {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        String sql = "SELECT COALESCE(SUM(amount), 0.00) FROM TRANSACTIONS "
                + "WHERE user_id = ? AND transaction_type = ? "
                + "AND transaction_date >= ? AND transaction_date < ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type.toUpperCase());
            ps.setDate(3, Date.valueOf(start));
            ps.setDate(4, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /** Six month user-scoped totals; empty months remain present with zero values. */
    public Map<YearMonth, BigDecimal[]> getMonthlyTrend(int userId, YearMonth lastMonth) throws SQLException {
        Map<YearMonth, BigDecimal[]> totals = new LinkedHashMap<>();
        YearMonth first = lastMonth.minusMonths(5);
        for (int i = 0; i < 6; i++) {
            totals.put(first.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        String sql = "SELECT YEAR(transaction_date) AS y, MONTH(transaction_date) AS m, "
                + "transaction_type, SUM(amount) AS total FROM TRANSACTIONS "
                + "WHERE user_id = ? AND transaction_date >= ? AND transaction_date < ? "
                + "GROUP BY YEAR(transaction_date), MONTH(transaction_date), transaction_type";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(first.atDay(1)));
            ps.setDate(3, Date.valueOf(lastMonth.plusMonths(1).atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal[] pair = totals.get(YearMonth.of(rs.getInt("y"), rs.getInt("m")));
                    if (pair != null) {
                        pair["INCOME".equals(rs.getString("transaction_type")) ? 0 : 1]
                                = rs.getBigDecimal("total");
                    }
                }
            }
        }
        return totals;
    }

    public Map<String, BigDecimal> getMonthlyExpensesByCategory(int userId, int year, int month) throws SQLException {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        String sql = "SELECT category, COALESCE(SUM(amount), 0.00) AS total_spent FROM TRANSACTIONS "
                + "WHERE user_id = ? AND transaction_type = 'EXPENSE' "
                + "AND transaction_date >= ? AND transaction_date < ? "
                + "GROUP BY category ORDER BY total_spent DESC";
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getBigDecimal("total_spent"));
                }
            }
        }
        return map;
    }

    private BudgetTransaction mapRow(ResultSet rs) throws SQLException {
        Date d = rs.getDate("transaction_date");
        return new BudgetTransaction(
                rs.getInt("transaction_id"),
                rs.getInt("user_id"),
                rs.getInt("account_id"),
                rs.getString("account_name"),
                rs.getString("transaction_type"),
                rs.getString("category"),
                rs.getBigDecimal("amount"),
                rs.getString("description"),
                d == null ? LocalDate.now() : d.toLocalDate(),
                rs.getTimestamp("created_at")
        );
    }
}
