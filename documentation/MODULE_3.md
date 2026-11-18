# MODULE 3 – Classes, Objects and Encapsulation

## 1. Module Title
**Module 3: Classes, Objects and Encapsulation**

## 2. Concepts Covered
- **Classes and Objects**: Designing blueprints (`User`, `Transaction`, `Income`, `Expense`, `Budget`) and instantiating objects to represent real database records in memory.
- **Attributes (Fields)**: Storing state inside each object (`budgetId`, `userId`, `monthlyLimit`, `month`, `year`).
- **Constructors**: Initializing all required attributes when an object is created with `new`.
- **Encapsulation & Access Modifiers**: Declaring attributes `private` so external classes cannot corrupt object state directly, and exposing controlled access through `public` getters and setters.
- **Security through Encapsulation**: Intentionally omitting `passwordHash` from the `User` model class so password hashes can never be accidentally displayed in any GUI table or dialog.

---

## 3. How These Concepts Are Applied in BUDGETLY
Every core entity in BUDGETLY is modeled as a Java class in the `budgetly.model` package:
- `User.java` (with subclasses `Admin.java` and `RegularUser.java`) encapsulates account information (`userId`, `fullName`, `username`, `email`, `active`, `createdAt`).
- `Transaction.java` (with subclasses `Income.java` and `Expense.java`) encapsulates transaction details (`transactionId`, `userId`, `description`, `amount`, `category`, `date`).
- `Budget.java` encapsulates a user's monthly spending limit (`budgetId`, `userId`, `monthlyLimit`, `month`, `year`).

All fields in these classes are marked `private` and accessed via getters and setters.

---

## 4. Actual Source Code from BUDGETLY

### Example A – Encapsulated Model Class (`Budget.java`)
- **Source File**: `src/budgetly/model/Budget.java`
- **Class**: `budgetly.model.Budget`

```java
package budgetly.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The spending limit a user sets for one month.
 */
public class Budget {

    private Integer budgetId;
    private int userId;
    private BigDecimal monthlyLimit;
    private int month;
    private int year;

    public Budget(Integer budgetId, int userId, BigDecimal monthlyLimit, int month, int year) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.monthlyLimit = monthlyLimit;
        this.month = month;
        this.year = year;
    }

    /** @return how much money is left (negative when over budget) */
    public BigDecimal getRemaining(BigDecimal spent) {
        return monthlyLimit.subtract(spent);
    }

    /** @return true when the spending reached or passed the limit */
    public boolean isReached(BigDecimal spent) {
        return spent.compareTo(monthlyLimit) >= 0;
    }

    public Integer getBudgetId() {
        return budgetId;
    }

    public int getUserId() {
        return userId;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(BigDecimal monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }
}
```

### Example B – Creating and Updating `Budget` Objects in `BudgetService`
- **Source File**: `src/budgetly/service/BudgetService.java`
- **Class & Method**: `BudgetService.saveBudget(int userId, String limitText, int month, int year)`

```java
Budget existing = budgetDAO.findBudget(userId, month, year);
if (existing == null) {
    budgetDAO.insert(new Budget(null, userId, limit, month, year));
} else {
    existing.setMonthlyLimit(limit);
    budgetDAO.update(existing);
}
```

---

## 5. Explanation of Important Lines of Code
- `private Integer budgetId; private int userId; private BigDecimal monthlyLimit;`: All fields are `private`, enforcing encapsulation.
- `public Budget(Integer budgetId, int userId, BigDecimal monthlyLimit, int month, int year)`: Parameterized constructor that initializes a `Budget` instance.
- `public BigDecimal getMonthlyLimit()` / `public void setMonthlyLimit(BigDecimal monthlyLimit)`: Getter and setter for reading and updating the monthly limit while keeping `budgetId`, `userId`, `month`, and `year` read-only after construction.
- `new Budget(null, userId, limit, month, year)`: Instantiates a new `Budget` object when the user sets a budget for a month for the first time.

## 6. How It Works Within the Application
When a user enters a monthly limit on the Budget screen or adds an income/expense record, `BudgetService` creates `Budget`, `Income`, or `Expense` objects and passes them to `BudgetDAO` or `TransactionDAO`. When records are loaded from Apache Derby, the DAOs construct model objects from each `ResultSet` row and return them to the GUI for display.
