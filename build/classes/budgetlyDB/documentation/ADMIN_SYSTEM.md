# Administrator System (`AdminDashboard`, `UserManagementFrame`, `DatabaseManagementFrame`)

Budgetly includes a role-protected administrator suite allowing authorized administrators to monitor users, inspect the Apache Derby database, review audit logs, and manage administrative security settings.

---

## 1. Administrator Authorization & Access Control

Administrator privileges are strictly isolated from regular user accounts:
- **Signup Guard**: `AuthenticationService.registerUser(...)` always sets `account_type = 'USER'`. Users cannot choose `ADMIN` during registration.
- **UI Guard**: `MainFrame` only adds the **Admin Dashboard** navigation button when `SessionManager.isAdmin()` returns `true`.
- **Service & DAO Guard**: Every method in `AdminDAO` and `DatabaseManager`, plus the constructors of `AdminDashboard`, `UserManagementFrame`, and `DatabaseManagementFrame`, immediately invokes:
```java
SessionManager.requireAdmin(); // throws SecurityException if not logged in as ADMIN
```
- **Last-Admin Protection**: `AdminDAO` blocks any action that would delete, demote, or deactivate the last active administrator account, or deactivate/delete the currently signed-in administrator.

---

## 2. Administrator Dashboard (`AdminDashboard.java`)

When an administrator opens **Admin Dashboard**, it displays:
1. **KPI Summary Cards**:
   - **Total Registered Users** (`COUNT(*) FROM USERS`)
   - **Active Users** (`account_status = 'ACTIVE'`)
   - **Inactive Users** (`account_status = 'INACTIVE'`)
   - **Total Administrators** (`account_type = 'ADMIN'`)
2. **Overview Tables**:
   - **Recently Registered Users** ordered by `date_created DESC`
   - **Recent Login Activity** ordered by `last_login DESC`
3. **Activity Logs Tab (`USER_ACTIVITY_LOGS`)**:
   - Displays audit events including logins, failed logins, blocked inactive logins, signups, password changes, profile updates, database maintenance, and admin actions.
4. **Admin Settings & Maintenance Tab**:
   - Change administrator password (requires current password verification + confirmation dialog).
   - Test Apache Derby connection (`DatabaseConnection.testConnection()`).
   - Verify & repair schema integrity (`DatabaseManager.verifySchemaIntegrity()`).

---

## 3. User Management (`UserManagementFrame.java`)

Administrators can manage all registered accounts inside a Swing `JTable`:
- **Search**: Filter users by username, email, first name, or last name (`AdminDAO.searchUsers(...)`).
- **View User Details**: Displays account info, registration/login timestamps, saved `USER_PROFILES` details, and recent `USER_ACTIVITY_LOGS` for that user.
- **Edit User Information**: Update email, first name, last name, account type, or status with uniqueness and last-admin validation.
- **Activate / Deactivate Accounts**: Toggles `account_status` between `ACTIVE` and `INACTIVE` with confirmation. Inactive accounts are immediately blocked from signing in.
- **Delete User Accounts**: Prompts with a warning confirmation dialog before cascading removal of the user and their dependent records.

---

## 4. Database Monitoring & Management (`DatabaseManagementFrame.java`)

Provides safe database administration without exposing raw passwords or unrestricted SQL execution:
- **Database Status Cards**: Displays connection health, Derby product & driver versions, endpoint (`jdbc:derby://localhost:1527/AdminDB`), schema (`ADMINUSER`), and connection pool counts.
- **Read-Only Table Explorer**: Lists all tables via `DatabaseMetaData.getTables(...)` and previews up to 200 rows from an allowlisted table. Sensitive `PASSWORD_HASH` and `PASSWORD_SALT` columns are automatically masked as `[PROTECTED]`.
- **Error Monitor**: Lists sanitized `DatabaseErrorRecord` entries recorded by `DatabaseConnection`, with options to verify schema integrity or clear the monitor buffer.
