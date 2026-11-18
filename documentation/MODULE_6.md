# MODULE 6 – Java Collections Framework and Core Collection Types

## 1. Module Title
**Module 6: Java Collections Framework and Core Collection Types**

## 2. Concepts Covered
- **`List<E>` and `ArrayList<E>`**: Dynamic, ordered collections used to hold `List<Transaction>` and `List<User>` loaded from Apache Derby.
- **`Map<K, V>` and `LinkedHashMap<K, V>`**: Key-value pairs (`Map<String, Long>`) used to store named system statistics in insertion order for the Administrator Panel.
- **Iteration (`for-each` loop)**: Iterating through `List<Transaction>` and `List<User>` to calculate totals, filter records, and populate Swing `JTable` rows.
- **Java Generics (`<T>` and `<T extends Manageable>`)**: Writing type-safe, reusable `search` and `sort` methods that work on any model type (`User`, `Transaction`, `Income`, `Expense`).
- **`Comparator<T>` and `Collections.sort(...)`**: Sorting transactions dynamically by date (newest/oldest) or amount (highest/lowest).

---

## 3. How These Concepts Are Applied in BUDGETLY
- **Why `ArrayList<Transaction>` is chosen for Transaction History**:
  `ArrayList` preserves the database order, supports fast indexed access (`allTransactions.get(i)` for the top 10 recent transactions on the Dashboard), and allows fast iteration when filtering, searching, and sorting in memory without re-querying the database on every keystroke.
- **Why `LinkedHashMap<String, Long>` is chosen for Admin Statistics**:
  Each statistic has a label key (`"Registered Users"`, `"Active Users"`, `"Inactive Users"`, `"Total Transactions"`) mapped to a `Long` count while preserving insertion order.
- **Generic `search` and `sort` in `BudgetService`**:
  `BudgetService.search(List<T> items, String keyword)` accepts any `List<T>` where `T extends Manageable`. It is used both in `MainFrame` (to search `List<Transaction>`) and in `AdminDashboard` (to search `List<User>`).

---

## 4. Actual Source Code from BUDGETLY

### Example A – Creating, Populating, Searching, and Sorting `ArrayList` with Generics
- **Source File**: `src/budgetly/service/BudgetService.java`
- **Class & Methods**: `BudgetService.filterByType(...)`, `BudgetService.search(...)`, `BudgetService.sort(...)`, and `BudgetService.sortTransactions(...)`

```java
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
```

### Example B – Building an `ArrayList<Transaction>` from JDBC `ResultSet`
- **Source File**: `src/budgetly/dao/TransactionDAO.java`
- **Class & Method**: `TransactionDAO.findByUser(int userId)`

```java
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
```

---

## 5. Explanation of Important Lines of Code
- `List<Transaction> list = new ArrayList<>();`: Creates a resizable `ArrayList` referenced via the `List` interface.
- `list.add(mapRow(rs));`: Adds each `Income` or `Expense` object constructed from the database row into the list.
- `public static <T extends Manageable> List<T> search(List<T> items, String keyword)`: Uses a bounded type parameter `<T extends Manageable>` so `item.matches(clean)` can be called safely on any element type `T`.
- `Collections.sort(sorted, comparator);`: Sorts the copied `ArrayList` in place using the supplied `Comparator<T>`.

## 6. How It Works Within the Application
When the user logs in or clicks **Refresh Data**, `TransactionDAO.findByUser(...)` loads the user's records into `allTransactions` (`List<Transaction>`). Whenever the user searches, filters by type, or changes the sort dropdown on the Income, Expense, or Transaction History screens, `MainFrame` passes `allTransactions` through `filterByType`, `BudgetService.search`, and `sortTransactions` and updates the `DefaultTableModel` rows.
