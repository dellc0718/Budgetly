# MODULE 4 – Inheritance, Polymorphism, Abstraction and Interfaces

## 1. Module Title
**Module 4: Inheritance, Polymorphism, Abstraction and Interfaces**

## 2. Concepts Covered
- **Inheritance (`extends`)**: Creating specialized subclasses (`Admin` and `RegularUser` extending `User`; `Income` and `Expense` extending `Transaction`) that inherit common attributes and methods from their parent class.
- **Abstraction (`abstract class` & `abstract` methods)**: Defining abstract base classes (`User` and `Transaction`) that cannot be instantiated directly and declare abstract methods that subclasses must implement.
- **Polymorphism (`@Override`)**: Calling an overridden method (`user.canManageUsers()`, `user.getDashboardTitle()`, `tx.getSignedAmount()`, `tx.getType()`) on a parent-class reference (`User` or `Transaction`) and executing the subclass's specific behavior at runtime.
- **Interfaces (`interface` & `implements`)**: Defining the `Manageable` contract implemented by both `User` and `Transaction` so a single generic search method can search both users and transactions.

---

## 3. How These Concepts Are Applied in BUDGETLY

### UML Relationship Overview
```
          <<interface>>
           Manageable
        + matches(keyword)
        + getSummary()
          ^            ^
          |            |
   +------+            +-------+
   |                           |
  User (abstract)         Transaction (abstract)
   ^                           ^
   |                           |
+--+---------+          +------+------+
|            |          |             |
Admin   RegularUser   Income       Expense
```

---

## 4. Actual Source Code from BUDGETLY

### Example A – The `Manageable` Interface
- **Source File**: `src/budgetly/model/Manageable.java`
- **Interface**: `budgetly.model.Manageable`

```java
package budgetly.model;

/**
 * Shared interface for application records (users and transactions)
 * that can be searched in tables and summarized in dialogs.
 */
public interface Manageable {

    /**
     * @param keyword search text in lower-case
     * @return true when this record matches the search keyword
     */
    boolean matches(String keyword);

    /**
     * @return short one-line description of this record for dialogs
     */
    String getSummary();
}
```

### Example B – Abstract Parent `User` and Subclasses `Admin` & `RegularUser`
- **Source Files**: `src/budgetly/model/User.java`, `src/budgetly/model/Admin.java`, `src/budgetly/model/RegularUser.java`

```java
// From User.java
public abstract class User implements Manageable {
    // ... private fields and constructor ...
    public abstract UserRole getRole();
    public abstract boolean canManageUsers();
    public abstract String getDashboardTitle();

    @Override
    public boolean matches(String keyword) {
        return username.toLowerCase().contains(keyword)
                || fullName.toLowerCase().contains(keyword);
    }
}

// From Admin.java
public class Admin extends User {
    public Admin(int userId, String fullName, String username, String email,
                 boolean active, LocalDateTime createdAt) {
        super(userId, fullName, username, email, active, createdAt);
    }

    @Override
    public UserRole getRole() { return UserRole.ADMIN; }

    @Override
    public boolean canManageUsers() { return true; }

    @Override
    public String getDashboardTitle() { return "Administrator Panel"; }
}

// From RegularUser.java
public class RegularUser extends User {
    public RegularUser(int userId, String fullName, String username, String email,
                       boolean active, LocalDateTime createdAt) {
        super(userId, fullName, username, email, active, createdAt);
    }

    @Override
    public UserRole getRole() { return UserRole.USER; }

    @Override
    public boolean canManageUsers() { return false; }

    @Override
    public String getDashboardTitle() { return "My Budget Dashboard"; }
}
```

### Example C – Abstract Parent `Transaction` and Subclasses `Income` & `Expense`
- **Source Files**: `src/budgetly/model/Transaction.java`, `src/budgetly/model/Income.java`, `src/budgetly/model/Expense.java`

```java
// From Transaction.java
public abstract class Transaction implements Manageable {
    public abstract TransactionType getType();
    public abstract BigDecimal getSignedAmount();
}

// From Income.java
public class Income extends Transaction {
    @Override
    public TransactionType getType() { return TransactionType.INCOME; }

    @Override
    public BigDecimal getSignedAmount() { return getAmount(); }
}

// From Expense.java
public class Expense extends Transaction {
    @Override
    public TransactionType getType() { return TransactionType.EXPENSE; }

    @Override
    public BigDecimal getSignedAmount() { return getAmount().negate(); }
}
```

---

## 5. Explanation of Important Lines of Code
- `public abstract class User implements Manageable`: Declares `User` as an abstract class that implements `Manageable`.
- `super(userId, fullName, username, email, active, createdAt);`: Calls the parent `User` constructor from `Admin` and `RegularUser`.
- `public abstract BigDecimal getSignedAmount();`: Forces every `Transaction` subclass to define how its amount affects the user's balance (`Income` returns `+amount`; `Expense` returns `-amount`).

## 6. How It Works Within the Application
- During login (`LoginFrame.handleLogin()`), `authService.login(...)` returns a `User` reference that points to either an `Admin` or a `RegularUser` instance. Calling `user.canManageUsers()` polymorphically routes `Admin` to `AdminDashboard` and `RegularUser` to `MainFrame`.
- When calculating the current balance (`BudgetService.calculateBalance()`), the loop calls `tx.getSignedAmount()` on each `Transaction` in `List<Transaction>`, automatically adding `Income` and subtracting `Expense` without needing `instanceof` checks.
