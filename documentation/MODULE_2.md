# MODULE 2 – Data Types, Variables, Input and Output

## 1. Module Title
**Module 2: Data Types, Variables, Input and Output**

## 2. Concepts Covered
- **Primitive Data Types**: `int` (IDs, month, year), `boolean` (flags, validation checks), `byte` (cryptographic salt bytes).
- **Reference Data Types**: `String` (names, descriptions, categories), `BigDecimal` (exact monetary amounts), `LocalDate` and `LocalDateTime` (dates and timestamps).
- **Wrapper Classes**: `Integer` (nullable transaction/budget IDs), `Boolean` (user active status), `Double` (budget usage percentage), `Long` (system statistic counts).
- **Constants (`static final`)**: Fixed configuration values such as `MAX_AMOUNT`, `MIN_PASSWORD_LENGTH`, and database URLs.
- **Type Conversion & Parsing**: Converting user text from `JTextField` into `BigDecimal` and `LocalDate`.
- **User Input & Program Output**: Accepting input via `JTextField`, `JPasswordField`, and `JComboBox`, and displaying output via `JLabel`, `JTable`, `JProgressBar`, and `JOptionPane`.
- **Arithmetic Operations**: Monetary addition, subtraction, and percentage calculation using `BigDecimal`.

---

## 3. How These Concepts Are Applied in BUDGETLY
- Financial values (`amount`, `monthlyLimit`, `totalIncome`, `totalExpense`, `balance`) use `java.math.BigDecimal` instead of `double` so centavos are never lost to floating-point rounding.
- Wrapper classes are used where object references or null values are required:
  - `Integer transactionId` in `Transaction.java` (`null` before a new record is inserted into the database).
  - `Boolean active` in `User.java` for account activation status.
  - `Double getUsagePercentage(BigDecimal spent)` in `Budget.java` to compute the percentage of the monthly budget spent.
  - `Map<String, Long>` in `UserDAO.getStatistics()` to hold row counts for the Administrator Panel.

---

## 4. Actual Source Code from BUDGETLY

### Example A – Parsing User Input & Monetary Arithmetic
- **Source File**: `src/budgetly/service/BudgetService.java`
- **Class & Methods**: `BudgetService.calculateTotal(...)`, `BudgetService.calculateBalance(...)`, `BudgetService.parsePositiveAmount(...)`, and `BudgetService.parseDate(...)`

```java
private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");

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
```

### Example B – Wrapper Class `Double` and Budget Arithmetic
- **Source File**: `src/budgetly/model/Budget.java`
- **Class & Methods**: `Budget.getRemaining(BigDecimal spent)` and `Budget.getUsagePercentage(BigDecimal spent)`

```java
/** @return how much money is left (negative when over budget) */
public BigDecimal getRemaining(BigDecimal spent) {
    return monthlyLimit.subtract(spent);
}

/** @return percentage of the monthly budget used (0.0 to 100.0+) as a Double wrapper */
public Double getUsagePercentage(BigDecimal spent) {
    if (monthlyLimit == null || monthlyLimit.compareTo(BigDecimal.ZERO) <= 0) {
        return 0.0;
    }
    return spent.multiply(BigDecimal.valueOf(100))
            .divide(monthlyLimit, 1, RoundingMode.HALF_UP)
            .doubleValue();
}
```

### Example C – Reading Swing Input & Displaying Output
- **Source File**: `src/budgetly/gui/MainFrame.java`
- **Class & Method**: `MainFrame.updateDashboardSection()`

```java
private void updateDashboardSection() {
    BigDecimal totalIncome = budgetService.calculateTotal(allTransactions, TransactionType.INCOME);
    BigDecimal totalExpense = budgetService.calculateTotal(allTransactions, TransactionType.EXPENSE);
    BigDecimal balance = budgetService.calculateBalance(allTransactions);

    dashIncomeLabel.setText(BudgetService.formatMoney(totalIncome));
    dashExpenseLabel.setText(BudgetService.formatMoney(totalExpense));
    dashBalanceLabel.setText(BudgetService.formatMoney(balance));
    dashCountLabel.setText(String.valueOf(allTransactions.size()));
```

---

## 5. Explanation of Important Lines of Code
- `private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999.99");`: Declares a constant matching the database column precision `DECIMAL(12,2)`.
- `new BigDecimal(text.trim().replace(",", ""))`: Converts the user's `String` input from `incAmountField` or `expAmountField` into an exact decimal number.
- `value.setScale(2, RoundingMode.HALF_UP)`: Rounds monetary values to 2 decimal places (centavos).
- `LocalDate.parse(text.trim())`: Converts a `YYYY-MM-DD` string into a `java.time.LocalDate` object.
- `dashBalanceLabel.setText(BudgetService.formatMoney(balance))`: Formats the calculated `BigDecimal` as `"PHP 1,250.00"` and displays it in a Swing `JLabel`.

## 6. How It Works Within the Application
When a user types a description, amount, and date into the Income or Expense form and clicks **Add**, `MainFrame` reads the `String` values from the `JTextField` components and passes them to `BudgetService.saveTransaction(...)`. `BudgetService` converts the strings into `BigDecimal` and `LocalDate`, saves the record to Derby, recalculates `Total Income`, `Total Expenses`, `Current Balance`, and `Remaining Budget`, and updates the dashboard labels and tables.
