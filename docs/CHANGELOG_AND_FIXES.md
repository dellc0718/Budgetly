# Budgetly changes and verification

## 2026-11-12

### Registration and database startup

- **Problem:** The reported registration failure with existing users did not reproduce in the integration suite. Existing duplicate prechecks worked, but a simultaneous signup could reach a Derby unique constraint and show raw SQL text. A later test run could not restart the in-process Derby server after shutdown.
- **Cause:** Registration returned `SQLException.getMessage()` on constraint failures. The bundled Derby Network Server needed `derbytools.jar`, which was missing from the runtime folder and NetBeans classpath.
- **Files:** `src/budgetlyDB/AuthenticationService.java`, `nbproject/project.properties`, `lib/derbytools.jar`, `dist/lib/derbytools.jar`, `test/budgetlyDB/BudgetlyIntegrationVerify.java`.
- **Fix:** Registration still validates required fields, checks case-insensitive username and email, hashes passwords with the existing PBKDF2 implementation, and commits user/profile/settings/wallet/log in one transaction. A `23505` conflict now returns a clear duplicate message after rollback; other SQL failures are logged and shown as a safe generic message. Added the matching Derby runtime jar for automatic server startup. No table was dropped or recreated and existing account hashes were untouched.
- **Verification:** New signup, duplicate username/email including different case, invalid password, admin and user login, logout, password change, account preservation and data isolation passed against Derby. The original issue may depend on a particular database state that was not present during this run.

### Dashboard

- **Problem:** The dashboard had only three totals and a large empty transaction area.
- **Cause:** No chart data query or chart component was implemented.
- **Files:** `src/budgetly/ui/DashboardPanel.java`, `src/budgetly/ui/FinancialChartsPanel.java`, `src/budgetlyDB/BudgetService.java`, `src/budgetlyDB/TransactionDAO.java`, `test/budgetlyDB/BudgetlyIntegrationVerify.java`.
- **Fix:** Added Java2D income/expense bars, a six-month trend, an expense-category doughnut, and budget progress. Existing cards, alerts and recent transactions remain. Chart SQL filters on the signed-in user and includes zero-valued months. Refreshes use the existing `SwingWorker` flow after transaction changes.
- **Verification:** Integration checks confirmed monthly totals, category amounts, six-month aggregation, account balances, and data isolation. Swing frame construction passed. Manual visual review at several window sizes is still advisable.

### Transaction editing

- **Problem:** History could delete transactions but had no edit action.
- **Cause:** The service and DAO only implemented insert and delete operations.
- **Files:** `src/budgetly/ui/TransactionHistoryPanel.java`, `src/budgetlyDB/BudgetService.java`, `src/budgetlyDB/TransactionDAO.java`, `test/budgetlyDB/BudgetlyIntegrationVerify.java`.
- **Fix:** Added an edit dialog for type, account, category, amount, description and date. The service verifies ownership, reverses the previous balance effect, applies the new effect, updates the transaction, and commits together. Failure rolls back the entire change.
- **Verification:** The integration suite edits an expense, checks the recalculated account balance, restores it, and confirms another user cannot edit the first user's transaction.

### User-facing labels

- **Problem:** Internal codes such as `BUDGET_AND_SECURITY` and `default_croc` appeared in controls.
- **Cause:** Swing combo boxes and tables rendered stored strings directly.
- **Files:** `src/budgetlyDB/ui/UiLabels.java`, `src/budgetlyDB/ui/ProfileSettingsFrame.java`, `src/budgetlyDB/ui/UserManagementFrame.java`, `src/budgetlyDB/ui/AdminDashboard.java`, `src/budgetlyDB/model/FinancialAccount.java`, `src/budgetly/ui/AccountsPanel.java`, `src/budgetly/ui/TransactionHistoryPanel.java`, `src/budgetly/ui/MonthlyBudgetPanel.java`, `src/budgetly/ui/DashboardPanel.java`.
- **Fix:** Added explicit label rendering for persisted codes, with a controlled fallback for uppercase identifiers. Combo selection and table model values remain the original database codes, so saves, filters, and edits keep working.
- **Verification:** Compile and Swing construction passed; integration tested profile/settings persistence and financial workflows.

### Java Modules documentation

- **Problem:** The source contained a large embedded viewer with unrelated coursework examples, including a calculator example.
- **Cause:** Explanations and snippets were stored as Java string literals in `JavaModulesFrame`.
- **Files:** `docs/JAVA_MODULES.md`, `src/budgetlyDB/documentation/JAVA_MODULES.md`, `src/budgetlyDB/ui/JavaModulesFrame.java`, `src/budgetly/ui/MainFrame.java`, `test/budgetlyDB/BudgetlyIntegrationVerify.java`.
- **Fix:** Moved all eight modules into an external Markdown guide with actual Budgetly source excerpts and file/package references. The former documentation location now points to the guide.
- **Verification:** Documentation existence and frame construction passed. Source search found no calculator example in the new guide or viewer.

## Build and test

Java 21, native Swing/Java2D/JDBC, and Apache Derby 10.17.1.0 were used. No chart library was introduced. Sources and integration tests compiled with `javac -encoding UTF-8 -cp 'dist/lib/*'`. Running `budgetlyDB.BudgetlyIntegrationVerify` passed **54 checks** against the existing Derby database. Temporary test users were deleted by the test; the application database was not reset.

## Remaining scope

The integration suite covers transaction creation, editing and deletion, budgets, account creation, profile and password updates, permissions, and persistence across login, but does not automate every GUI interaction or theme appearance. Currency preferences still change the displayed prefix; they do not convert amounts with exchange rates.

## Dashboard layout follow-up

- Removed the Java Modules sidebar tab and its embedded content card from `src/budgetly/ui/MainFrame.java`. The Markdown guide remains in `docs/JAVA_MODULES.md` for evaluation.
- Set stable heights for the dashboard summary cards, alert, budget progress, and charts in `src/budgetly/ui/DashboardPanel.java`, and reset the dashboard scroll position to the top when created.
- Aligned chart titles, axis labels, month labels, legends, category labels, and the doughnut total in `src/budgetly/ui/FinancialChartsPanel.java`. The total now sits below the ring so it cannot overflow the center hole.
- Updated the old setup documentation to describe opening the external Markdown file. Recompiled the project, reran all 54 integration checks, and inspected a rendered chart preview at 1200 × 340 pixels.
- Centered the expense doughnut, total, and legend as a group in `src/budgetly/ui/FinancialChartsPanel.java`; visually checked both one-category and two-category previews and rebuilt `dist/Budgetly.jar`.
