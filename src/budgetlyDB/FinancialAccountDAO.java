package budgetlyDB;

import budgetlyDB.model.FinancialAccount;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user-isolated FINANCIAL_ACCOUNTS (Cash, Bank, E-Wallet, Other).
 */
public class FinancialAccountDAO {

    public List<FinancialAccount> findByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM FINANCIAL_ACCOUNTS WHERE user_id = ? ORDER BY account_name ASC";
        List<FinancialAccount> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public FinancialAccount findByIdForUser(Connection con, int accountId, int userId) throws SQLException {
        String sql = "SELECT * FROM FINANCIAL_ACCOUNTS WHERE account_id = ? AND user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public int insertAccount(Connection con, FinancialAccount account) throws SQLException {
        String sql = "INSERT INTO FINANCIAL_ACCOUNTS (user_id, account_name, account_type, balance, notes) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, account.getUserId());
            ps.setString(2, account.getAccountName().trim());
            ps.setString(3, account.getAccountType().trim().toUpperCase());
            ps.setBigDecimal(4, account.getBalance());
            ps.setString(5, account.getNotes() == null ? "" : account.getNotes().trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    account.setAccountId(id);
                    return id;
                }
                throw new SQLException("Inserting financial account did not return a generated account_id.");
            }
        }
    }

    public int insertAccount(FinancialAccount account) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection()) {
            return insertAccount(con, account);
        }
    }

    public boolean updateAccountDetails(int accountId, int userId, String accountName,
                                        String accountType, String notes) throws SQLException {
        String sql = "UPDATE FINANCIAL_ACCOUNTS SET account_name = ?, account_type = ?, notes = ? "
                + "WHERE account_id = ? AND user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountName.trim());
            ps.setString(2, accountType.trim().toUpperCase());
            ps.setString(3, notes == null ? "" : notes.trim());
            ps.setInt(4, accountId);
            ps.setInt(5, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean adjustBalance(Connection con, int accountId, int userId, BigDecimal delta) throws SQLException {
        String sql = "UPDATE FINANCIAL_ACCOUNTS SET balance = balance + ? WHERE account_id = ? AND user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, delta);
            ps.setInt(2, accountId);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteAccount(int accountId, int userId) throws SQLException {
        String sql = "DELETE FROM FINANCIAL_ACCOUNTS WHERE account_id = ? AND user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public BigDecimal getTotalBalance(int userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(balance), 0.00) FROM FINANCIAL_ACCOUNTS WHERE user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    private FinancialAccount mapRow(ResultSet rs) throws SQLException {
        return new FinancialAccount(
                rs.getInt("account_id"),
                rs.getInt("user_id"),
                rs.getString("account_name"),
                rs.getString("account_type"),
                rs.getBigDecimal("balance"),
                rs.getString("notes"),
                rs.getTimestamp("date_created")
        );
    }
}
