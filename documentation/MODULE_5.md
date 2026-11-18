# MODULE 5 – Java Errors and Exception Handling

## 1. Module Title
**Module 5: Java Errors and Exception Handling**

## 2. Concepts Covered
- **Compile-Time (Syntax) vs. Runtime vs. Logical Errors**: Preventing syntax errors during compilation, catching runtime exceptions (`NumberFormatException`, `DateTimeParseException`, `SecurityException`), and avoiding logical errors through `BigDecimal` math and `user_id` query checks.
- **Checked Exceptions (`SQLException`, `IOException`, `GeneralSecurityException`)**: Propagating database errors with `throws SQLException` from DAO and Service methods to the GUI layer.
- **`try-catch-finally` & `try-with-resources`**: Automatically closing JDBC `Connection`, `PreparedStatement`, and `ResultSet` objects and clearing sensitive password characters in a `finally` block.
- **`throw` and `throws` Keywords**: Throwing `SecurityException` when an unauthorized user attempts an administrator action.
- **Input Validation vs. Exceptions**: Validating empty fields, username format, password length, duplicate usernames, and positive amounts before executing SQL queries, and displaying clear `JOptionPane` messages.

---

## 3. How These Concepts Are Applied in BUDGETLY
- Every DAO method (`UserDAO`, `TransactionDAO`, `BudgetDAO`) uses `try-with-resources` so database connections and statements are always closed even if an exception occurs.
- `UserDAO.deleteUser(int userId)` uses an explicit `try-catch-finally` block around a multi-table transaction (`conn.setAutoCommit(false)`), calling `conn.rollback()` in the `catch` block if any `DELETE` fails and restoring `conn.setAutoCommit(true)` in `finally`.
- `AuthService` and `BudgetService` validate all user inputs (empty fields, duplicate usernames, password confirmation, positive numeric amounts, valid `YYYY-MM-DD` dates) and return clear human-readable messages displayed with `JOptionPane.showMessageDialog`.

---

## 4. Actual Source Code from BUDGETLY

### Example A – Transaction Rollback with `try-catch-finally` in `UserDAO.java`
- **Source File**: `src/budgetly/dao/UserDAO.java`
- **Class & Method**: `UserDAO.deleteUser(int userId)`

```java
public boolean deleteUser(int userId) throws SQLException {
    try (Connection conn = DatabaseConnection.getConnection()) {
        conn.setAutoCommit(false);
        try {
            executeDelete(conn, "DELETE FROM transactions WHERE user_id = ?", userId);
            executeDelete(conn, "DELETE FROM budgets WHERE user_id = ?", userId);
            int deleted = executeDelete(conn,
                    "DELETE FROM users WHERE user_id = ? AND role = 'USER'", userId);
            if (deleted != 1) {
                conn.rollback();
                return false;
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
```

### Example B – Input Validation & `throw new SecurityException` in `AuthService.java`
- **Source File**: `src/budgetly/service/AuthService.java`
- **Class & Methods**: `AuthService.requireAdmin(User user)`, `AuthService.validateNameAndUsername(...)`, and `AuthService.validatePassword(...)`

```java
/** Polymorphism: Admin returns true, RegularUser returns false. */
private void requireAdmin(User user) {
    if (user == null || !user.canManageUsers()) {
        throw new SecurityException("Access denied: administrator privileges are required.");
    }
}

private String validateNameAndUsername(String fullName, String username) {
    if (fullName.trim().isEmpty() || username.trim().isEmpty()) {
        return "Full name and username are required.";
    }
    if (fullName.trim().length() > 100) {
        return "Full name must not exceed 100 characters.";
    }
    if (!username.trim().matches(USERNAME_PATTERN)) {
        return "Username must be 3-20 characters (letters, numbers or underscore only).";
    }
    return null;
}

private String validatePassword(String password, String confirmPassword) {
    if (password.isEmpty()) {
        return "Password is required.";
    }
    if (password.length() < MIN_PASSWORD_LENGTH) {
        return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
    }
    if (!password.equals(confirmPassword)) {
        return "Password and confirm password do not match.";
    }
    return null;
}
```

### Example C – Handling `SQLException` & Showing `JOptionPane` Dialogs in `LoginFrame.java`
- **Source File**: `src/budgetly/gui/LoginFrame.java`
- **Class & Method**: `LoginFrame.handleLogin()`

```java
try {
    User user = authService.login(username, password);
    if (user == null) {
        JOptionPane.showMessageDialog(this,
                "Invalid username or password. Please try again.",
                "Login Failed", JOptionPane.ERROR_MESSAGE);
        return;
    }
    if (!Boolean.TRUE.equals(user.isActive())) {
        JOptionPane.showMessageDialog(this,
                "Your account has been deactivated by an administrator.",
                "Account Inactive", JOptionPane.WARNING_MESSAGE);
        return;
    }
    // ...
} catch (SQLException ex) {
    JOptionPane.showMessageDialog(this,
            "Database error during login: " + ex.getMessage(),
            "Database Error", JOptionPane.ERROR_MESSAGE);
}
```

---

## 5. Explanation of Important Lines of Code
- `try (Connection conn = DatabaseConnection.getConnection())`: Uses Java's try-with-resources statement so `conn.close()` is guaranteed to run.
- `conn.rollback(); throw e;`: Undoes partial deletions if an error occurs mid-transaction and rethrows the `SQLException` so the caller is notified.
- `finally { conn.setAutoCommit(true); }`: Restores default auto-commit mode before the connection closes.
- `throw new SecurityException(...)`: Immediately blocks unauthorized callers from executing admin methods.

## 6. How It Works Within the Application
Normal user mistakes (blank inputs, mismatched passwords, negative or non-numeric amounts, invalid dates, duplicate usernames) are caught by validation methods before reaching the database. Unexpected database failures (`SQLException`) are caught in the GUI event handlers and shown in friendly `JOptionPane` dialogs so the application never crashes.
