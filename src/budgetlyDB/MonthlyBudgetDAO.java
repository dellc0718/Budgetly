package budgetlyDB;

import budgetlyDB.model.MonthlyBudget;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for user-isolated MONTHLY_BUDGETS.
 */
public class MonthlyBudgetDAO {

    private final TransactionDAO transactionDAO = new TransactionDAO();

    public void saveOrUpdateBudget(int userId, int year, int month, String category, BigDecimal limitAmount)
            throws SQLException {
        String cat = (category == null || category.trim().isEmpty()) ? "OVERALL" : category.trim();
        String checkSql = "SELECT budget_id FROM MONTHLY_BUDGETS "
                + "WHERE user_id = ? AND budget_year = ? AND budget_month = ? AND category = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement checkPs = con.prepareStatement(checkSql)) {
            checkPs.setInt(1, userId);
            checkPs.setInt(2, year);
            checkPs.setInt(3, month);
            checkPs.setString(4, cat);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    int budgetId = rs.getInt("budget_id");
                    String updateSql = "UPDATE MONTHLY_BUDGETS SET limit_amount = ?, updated_at = CURRENT_TIMESTAMP "
                            + "WHERE budget_id = ? AND user_id = ?";
                    try (PreparedStatement upd = con.prepareStatement(updateSql)) {
                        upd.setBigDecimal(1, limitAmount);
                        upd.setInt(2, budgetId);
                        upd.setInt(3, userId);
                        upd.executeUpdate();
                    }
                } else {
                    String insertSql = "INSERT INTO MONTHLY_BUDGETS "
                            + "(user_id, budget_year, budget_month, category, limit_amount) "
                            + "VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ins = con.prepareStatement(insertSql)) {
                        ins.setInt(1, userId);
                        ins.setInt(2, year);
                        ins.setInt(3, month);
                        ins.setString(4, cat);
                        ins.setBigDecimal(5, limitAmount);
                        ins.executeUpdate();
                    }
                }
            }
        }
    }

    public List<MonthlyBudget> findBudgetsWithSpent(int userId, int year, int month) throws SQLException {
        Map<String, BigDecimal> spentByCategory = transactionDAO.getMonthlyExpensesByCategory(userId, year, month);
        BigDecimal totalSpent = transactionDAO.getMonthlyTotal(userId, "EXPENSE", year, month);

        String sql = "SELECT * FROM MONTHLY_BUDGETS "
                + "WHERE user_id = ? AND budget_year = ? AND budget_month = ? "
                + "ORDER BY CASE WHEN category = 'OVERALL' THEN 0 ELSE 1 END, category ASC";
        List<MonthlyBudget> budgets = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cat = rs.getString("category");
                    BigDecimal spent = "OVERALL".equalsIgnoreCase(cat)
                            ? totalSpent
                            : spentByCategory.getOrDefault(cat, BigDecimal.ZERO);
                    budgets.add(new MonthlyBudget(
                            rs.getInt("budget_id"),
                            rs.getInt("user_id"),
                            rs.getInt("budget_year"),
                            rs.getInt("budget_month"),
                            cat,
                            rs.getBigDecimal("limit_amount"),
                            spent,
                            rs.getTimestamp("updated_at")
                    ));
                }
            }
        }
        return budgets;
    }

    public BigDecimal findOverallBudgetLimit(int userId, int year, int month) throws SQLException {
        String sql = "SELECT limit_amount FROM MONTHLY_BUDGETS "
                + "WHERE user_id = ? AND budget_year = ? AND budget_month = ? AND category = 'OVERALL'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal("limit_amount") : null;
            }
        }
    }

    public boolean deleteBudget(int budgetId, int userId) throws SQLException {
        String sql = "DELETE FROM MONTHLY_BUDGETS WHERE budget_id = ? AND user_id = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
