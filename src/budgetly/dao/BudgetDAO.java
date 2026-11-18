package budgetly.dao;

import budgetly.database.DatabaseConnection;
import budgetly.model.Budget;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * All SQL for the BUDGETS table (one budget per user per month).
 */
public class BudgetDAO {

    /** @return the budget for that month, or null if none was set */
    public Budget findBudget(int userId, int month, int year) throws SQLException {
        String sql = "SELECT budget_id, monthly_limit FROM budgets "
                + "WHERE user_id = ? AND budget_month = ? AND budget_year = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, month);
            ps.setInt(3, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Budget(rs.getInt("budget_id"), userId,
                            rs.getBigDecimal("monthly_limit"), month, year);
                }
                return null;
            }
        }
    }

    public void insert(Budget budget) throws SQLException {
        String sql = "INSERT INTO budgets (user_id, monthly_limit, budget_month, budget_year) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, budget.getUserId());
            ps.setBigDecimal(2, budget.getMonthlyLimit());
            ps.setInt(3, budget.getMonth());
            ps.setInt(4, budget.getYear());
            ps.executeUpdate();
        }
    }

    public void update(Budget budget) throws SQLException {
        String sql = "UPDATE budgets SET monthly_limit = ? WHERE budget_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, budget.getMonthlyLimit());
            ps.setInt(2, budget.getBudgetId());
            ps.setInt(3, budget.getUserId());
            ps.executeUpdate();
        }
    }
}
