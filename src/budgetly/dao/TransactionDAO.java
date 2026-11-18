package budgetly.dao;

import budgetly.database.DatabaseConnection;
import budgetly.model.Transaction;
import budgetly.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the TRANSACTIONS table.
 * IMPORTANT: every query includes "user_id = ?" so a user can only
 * read, change or delete their OWN records.
 */
public class TransactionDAO {

    public void insert(Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (user_id, description, amount, category, "
                + "transaction_type, transaction_date) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, t.getUserId());
            ps.setString(2, t.getDescription());
            ps.setBigDecimal(3, t.getAmount());
            ps.setString(4, t.getCategory());
            ps.setString(5, t.getType().name());
            ps.setDate(6, Date.valueOf(t.getDate()));
            ps.executeUpdate();
        }
    }

    /** @return false if the record does not exist or belongs to another user */
    public boolean update(Transaction t) throws SQLException {
        String sql = "UPDATE transactions SET description = ?, amount = ?, category = ?, "
                + "transaction_date = ? WHERE transaction_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getDescription());
            ps.setBigDecimal(2, t.getAmount());
            ps.setString(3, t.getCategory());
            ps.setDate(4, Date.valueOf(t.getDate()));
            ps.setInt(5, t.getTransactionId());
            ps.setInt(6, t.getUserId());
            return ps.executeUpdate() == 1;
        }
    }

    /** @return false if the record does not exist or belongs to another user */
    public boolean delete(int transactionId, int userId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE transaction_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Loads all income and expense records of one user, newest first. */
    public List<Transaction> findByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM transactions WHERE user_id = ? "
                + "ORDER BY transaction_date DESC, transaction_id DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** Sum of the user's expenses in one month (0 when there are none). */
    public BigDecimal getMonthlyExpenses(int userId, int month, int year) throws SQLException {
        String sql = "SELECT SUM(amount) FROM transactions WHERE user_id = ? "
                + "AND transaction_type = 'EXPENSE' "
                + "AND MONTH(transaction_date) = ? AND YEAR(transaction_date) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                BigDecimal total = rs.next() ? rs.getBigDecimal(1) : null;
                return total == null ? BigDecimal.ZERO : total;
            }
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        return Transaction.create(
                TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getInt("transaction_id"),
                rs.getInt("user_id"),
                rs.getString("description"),
                rs.getBigDecimal("amount"),
                rs.getString("category"),
                rs.getDate("transaction_date").toLocalDate());
    }
}
