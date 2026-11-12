# Java Programming Modules in Budgetly

This guide connects Modules 1–8 to code in the Budgetly project. All excerpts below are from the named project files. Some modules cover concepts that Budgetly does not use directly; those gaps are stated rather than filled with unrelated coursework examples. The application uses the JDK, Swing/Java2D, JDBC, and the project's bundled Apache Derby jars. No charting package was added.

## Module 1: Introduction to Java

**Concept.** A Java application begins in `main`, compiles to JVM bytecode, and can use platform libraries. NetBeans runs the same entry point as `java`.

**Budgetly implementation.** `budgetly.Budgetly` (`src/budgetly/Budgetly.java`) initializes Derby before showing the login window on Swing's event dispatch thread:

```java
public static void main(String[] args) {
    Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown, "Budgetly-DB-Shutdown"));

    try {
        DatabaseInitializer.initialize();
    } catch (SQLException ex) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                null,
                "Budgetly could not initialize the Apache Derby database.\n\nDetails: " + ex.getMessage(),
                "Database Startup Error",
                JOptionPane.ERROR_MESSAGE
        ));
        return;
    }

    SwingUtilities.invokeLater(() -> {
        LoginFrame loginFrame = new LoginFrame();
        loginFrame.setVisible(true);
    });
}
```

The JVM calls `main`. The startup code creates database tables and then creates the first window. The shutdown hook closes database resources.

## Module 2: Data Types, Logical Operations, Input and Output

**Concept.** Java primitives, reference types, Boolean operators, and comparisons represent and validate data. Swing fields provide user input and labels/dialogs provide output.

**Budgetly implementation.** `budgetlyDB.AuthenticationService` (`src/budgetlyDB/AuthenticationService.java`) validates registration input before any SQL write:

```java
if (u.isEmpty() || e.isEmpty() || fn.isEmpty() || ln.isEmpty()
        || password == null || password.length == 0
        || confirmPassword == null || confirmPassword.length == 0) {
    return new AuthResult(false, "All required registration fields must be filled out.", null);
}
if (!USERNAME_PATTERN.matcher(u).matches()) {
    return new AuthResult(false,
            "Username must be 3-30 characters (letters, numbers, dot, underscore, or hyphen).", null);
}
```

`String` and `char[]` are reference values, `password.length` is an `int`, and `||` stops once a required field is missing. `budgetly.ui.TransactionEntryPanel` (`src/budgetly/ui/TransactionEntryPanel.java`) converts text input to a financial amount:

```java
String amtText = amountField.getText().trim().replace(",", "");
BigDecimal amt;
try {
    amt = new BigDecimal(amtText);
    if (amt.compareTo(BigDecimal.ZERO) <= 0) {
        throw new IllegalArgumentException();
    }
} catch (Exception ex) {
    feedbackLabel.setForeground(BrandTheme.DANGER_RED);
    feedbackLabel.setText("Please enter a valid positive amount.");
    return;
}
```

`BigDecimal` avoids floating point rounding in money calculations. This GUI does not read normal user input with `Scanner`.

## Module 3: Classes, Objects and Encapsulation

**Concept.** Classes group fields and methods. Private fields and public methods control access to state.

**Budgetly implementation.** `budgetlyDB.model.UserProfile` (`src/budgetlyDB/model/UserProfile.java`) stores profile data in private fields:

```java
public class UserProfile {
    private int profileId;
    private int userId;
    private String profilePicture;
    private String displayName;
    private String contactNumber;
    private String address;
    private String bio;
    private String occupation;
    private Timestamp updatedAt;

    public UserProfile() {
        this.profilePicture = "default_croc";
    }
```

The rest of the class supplies getters and setters. `budgetlyDB.AuthenticationService` creates and populates a `UserProfile` object during registration, then `ProfileDAO` persists it. The fields are hidden from other classes, although these setters do not themselves enforce validation.

## Module 4: Inheritance, Polymorphism and Abstraction

**Concept.** A subclass inherits a parent class and can override behavior. An interface declares operations a class implements. A parent type can refer to different concrete implementations.

**Budgetly implementation.** `budgetly.ui.DashboardPanel` (`src/budgetly/ui/DashboardPanel.java`) extends Swing's `JPanel`, and `budgetly.ui.MainFrame` (`src/budgetly/ui/MainFrame.java`) implements `ActionListener`:

```java
public class DashboardPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final BudgetService budgetService = new BudgetService();
```

```java
public class MainFrame extends JFrame implements ActionListener {

    private final AuthenticationService authService = new AuthenticationService();
```

`DashboardPanel` inherits Swing container behavior. `MainFrame` is a `JFrame` and supplies an `ActionListener` implementation for sidebar actions. Budgetly uses Swing abstractions; it does not define a custom abstract finance class hierarchy.

## Module 5: Exception Handling

**Concept.** `try`/`catch` handles failures and `finally` releases sensitive or shared resources. Try with resources closes JDBC handles.

**Budgetly implementation.** `budgetlyDB.AuthenticationService` (`src/budgetlyDB/AuthenticationService.java`) rolls back a failed registration and clears password arrays:

```java
} catch (SQLException ex) {
    try {
        con.rollback();
    } catch (SQLException ignored) {
    }
    DatabaseConnection.recordError("User Registration", ex);
    return new AuthResult(false, registrationError(ex, con, u, e), null);
} finally {
    con.setAutoCommit(prevAuto);
}
```

The outer `finally` in `registerUser` clears both password arrays. `budgetlyDB.UserDAO` (`src/budgetlyDB/UserDAO.java`) closes its connection, statement and result set automatically:

```java
try (Connection con = DatabaseConnection.getConnection();
     PreparedStatement ps = con.prepareStatement(sql)) {
    ps.setInt(1, userId);
    try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? mapRow(rs) : null;
    }
}
```

## Module 6: Java Collection Framework

**Concept.** Generic collections hold typed groups of objects; `List` preserves a sequence and `Map` associates keys and values.

**Budgetly implementation.** `budgetlyDB.TransactionDAO` (`src/budgetlyDB/TransactionDAO.java`) accumulates result rows for the current user:

```java
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
```

The dashboard iterates this list to populate recent transactions. The same DAO returns `Map<String, BigDecimal>` for expense categories and `Map<YearMonth, BigDecimal[]>` for chart months. A `LinkedHashMap` keeps the month order stable.

## Module 7: Multithreading, Threads and Java GUI with Swing

**Concept.** Swing changes belong on the event dispatch thread. `SwingWorker` runs database work in the background, then updates components in `done()`.

**Budgetly implementation.** `budgetly.ui.DashboardPanel` (`src/budgetly/ui/DashboardPanel.java`) loads data without blocking painting or navigation:

```java
SwingWorker<Void, Void> worker = new SwingWorker<>() {
    private DashboardSummary summary;
    private List<BudgetTransaction> recent;

    @Override
    protected Void doInBackground() throws Exception {
        summary = budgetService.getDashboardSummary();
        recent = budgetService.getRecentTransactions(8);
        trend = budgetService.getMonthlyTrend();
        YearMonth month = YearMonth.now();
        categories = budgetService.getMonthlyExpensesByCategory(month.getYear(), month.getMonthValue());
        return null;
    }
```

The `done()` method applies the returned data to cards, tables, and Java2D charts. `budgetly.ui.FinancialChartsPanel` overrides `paintComponent` for charts. `budgetly.Budgetly` uses `SwingUtilities.invokeLater` for the initial login window.

## Module 8: JDBC and Database Programming

**Concept.** JDBC connects to Derby, executes parameterized SQL, and converts `ResultSet` rows to models. Transactions keep related writes atomic.

**Budgetly implementation.** `budgetlyDB.DatabaseConnection` (`src/budgetlyDB/DatabaseConnection.java`) uses the existing Derby Network Server configuration:

```java
public static final String HOST = "localhost";
public static final int PORT = 1527;
public static final String DATABASE_NAME = "AdminDB";
public static final String SCHEMA_NAME = "ADMINUSER";
public static final String DB_URL = "jdbc:derby://" + HOST + ":" + PORT + "/" + DATABASE_NAME;
```

`budgetlyDB.TransactionDAO` (`src/budgetlyDB/TransactionDAO.java`) queries only the signed in user's monthly transactions with a prepared statement:

```java
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
```

The parameters protect SQL syntax and ensure a user's dashboard stays scoped to their `user_id`. `BudgetService.recordTransaction` starts a JDBC transaction, inserts the transaction, adjusts account balance, then commits; on failure it rolls back. `DatabaseInitializer` creates tables only if absent and preserves existing records.
