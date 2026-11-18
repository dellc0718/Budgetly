# MODULE 7 – Multithreading and Java GUI Programming with Swing

## 1. Module Title
**Module 7: Multithreading and Java GUI Programming with Swing**

## 2. Concepts Covered
- **Java Swing Components**: `JFrame`, `JPanel`, `JButton`, `JLabel`, `JTextField`, `JPasswordField`, `JComboBox`, `JTable`, `JScrollPane`, `JTabbedPane`, `JProgressBar`, `JTextArea`, and `JOptionPane`.
- **Layout Managers**: `BorderLayout`, `CardLayout`, `GridLayout`, `FlowLayout`, and `BoxLayout` for responsive, organized screen design.
- **Event-Driven Programming & Event Listeners**: Using `ActionListener` (`addActionListener(e -> ...)`) and `ListSelectionListener` to respond to button clicks, form submissions, and table row selections.
- **Event Dispatch Thread (EDT)**: Launching and updating all Swing components on the EDT using `SwingUtilities.invokeLater(...)`.
- **Multithreading with `SwingWorker<T, V>`**: Running database queries on a background worker thread (`doInBackground()`) and updating Swing labels and tables on the EDT (`done()`) so the GUI never freezes.

---

## 3. How These Concepts Are Applied in BUDGETLY
- `Main.java` starts the application inside `SwingUtilities.invokeLater(...)` so the GUI is created on the Event Dispatch Thread.
- `LoginFrame.java` uses `CardLayout` to switch between the Login and Registration views.
- `MainFrame.java` uses a left sidebar + `CardLayout` to switch between Dashboard, Income, Expenses, Budget, Transaction History, and Settings & Documentation.
- Both `MainFrame.refreshAllData()` and `AdminDashboard.refreshUsersAndStats()` use `SwingWorker` to fetch records from Apache Derby in the background without blocking the user interface.

---

## 4. Actual Source Code from BUDGETLY

### Example A – Background Database Loading with `SwingWorker` in `MainFrame.java`
- **Source File**: `src/budgetly/gui/MainFrame.java`
- **Class & Method**: `MainFrame.refreshAllData()`

```java
/**
 * Uses SwingWorker so database queries run on a background thread
 * and Swing components are updated safely on the Event Dispatch Thread (EDT).
 */
public final void refreshAllData() {
    new SwingWorker<List<Transaction>, Void>() {
        @Override
        protected List<Transaction> doInBackground() throws Exception {
            return budgetService.getUserTransactions(currentUser.getUserId());
        }

        @Override
        protected void done() {
            try {
                allTransactions = get();
                updateDashboardSection();
                updateIncomeTable();
                updateExpenseTable();
                updateHistoryTable();
                refreshBudgetSection(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Failed to load records: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}
```

### Example B – Background User & Statistics Loading in `AdminDashboard.java`
- **Source File**: `src/budgetly/gui/AdminDashboard.java`
- **Class & Method**: `AdminDashboard.refreshUsersAndStats()`

```java
public final void refreshUsersAndStats() {
    new SwingWorker<Map<String, Long>, Void>() {
        private List<User> loadedUsers;

        @Override
        protected Map<String, Long> doInBackground() throws Exception {
            loadedUsers = authService.getAllUsers(adminUser);
            return authService.getStatistics(adminUser);
        }

        @Override
        protected void done() {
            try {
                Map<String, Long> stats = get();
                allUsers = loadedUsers;
                statRegisteredLabel.setText(String.valueOf(stats.getOrDefault("Registered Users", 0L)));
                statActiveLabel.setText(String.valueOf(stats.getOrDefault("Active Users", 0L)));
                statInactiveLabel.setText(String.valueOf(stats.getOrDefault("Inactive Users", 0L)));
                statTransactionsLabel.setText(String.valueOf(stats.getOrDefault("Total Transactions", 0L)));
                populateUserTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(AdminDashboard.this,
                        "Error loading users: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}
```

### Example C – Sidebar Navigation & `ActionListener` Events in `MainFrame.java`
- **Source File**: `src/budgetly/gui/MainFrame.java`
- **Class & Method**: `MainFrame.createNavButton(String text, String cardName)`

```java
private JButton createNavButton(String text, String cardName) {
    JButton btn = createSidebarButton(text, new Color(34, 76, 66));
    btn.addActionListener(e -> contentLayout.show(contentPanel, cardName));
    return btn;
}
```

---

## 5. Explanation of Important Lines of Code
- `new SwingWorker<List<Transaction>, Void>() { ... }.execute();`: Creates and starts a background worker task.
- `protected List<Transaction> doInBackground() throws Exception`: Runs on a worker thread, calling `budgetService.getUserTransactions(...)` over JDBC without freezing the window.
- `protected void done()`: Automatically runs back on the Swing Event Dispatch Thread (EDT) once `doInBackground()` finishes.
- `allTransactions = get();`: Retrieves the result from `doInBackground()` (or rethrows any exception caught inside the `try-catch` block) and refreshes the Swing labels and tables.

## 6. How It Works Within the Application
Whenever a user clicks a button in BUDGETLY, Swing fires an `ActionEvent` on the EDT. For quick UI navigation (`CardLayout.show`), the event listener switches panels immediately. For database-heavy operations like loading dashboard statistics, transaction history, or the administrator's user list, `SwingWorker` performs the SQL queries on a background thread and updates the `JLabel` cards and `JTable` models on the EDT when finished.
