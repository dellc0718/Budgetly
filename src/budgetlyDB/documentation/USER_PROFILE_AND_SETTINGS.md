# User Profile and Settings Management

Every Budgetly user (including administrators) has dedicated personal profile and application preference records linked to their `user_id`.

---

## 1. Automatic Creation During Registration

During signup (`AuthenticationService.registerUser(...)`), Budgetly automatically inserts default rows inside the same atomic database transaction:
- **`USER_PROFILES`**: Sets `display_name` to `FirstName LastName`, `profile_picture = 'default_croc'`, and empty optional contact/address/bio fields.
- **`USER_SETTINGS`**: Sets `theme = 'LIGHT'`, `currency_preference = 'PHP'`, `notification_preferences = 'BUDGET_AND_SECURITY'`, `budget_alerts_enabled = TRUE`, `email_summary_enabled = FALSE`, and `default_budget_alert_percent = 80`.

---

## 2. Profile & Preferences Editing (`ProfileSettingsFrame` & `ProfilePanel`)

Users can access their profile and preferences either directly inside `MainFrame`'s **Profile** tab (`ProfilePanel`) or as a standalone window (`ProfileSettingsFrame`).

Editable fields saved to Derby:
- **Account Information (`USERS`)**: Email address (validated for uniqueness), First Name, Last Name.
- **Personal Profile (`USER_PROFILES`)**: Display Name, Contact Number, Occupation, Address, Avatar Style, Short Bio.
- **Application Preferences (`USER_SETTINGS`)**:
  - **Preferred Currency**: `PHP`, `USD`, `EUR`, `GBP`, `JPY` — dynamically updates currency formatting across `DashboardPanel`, `AccountsPanel`, `IncomePanel`, `ExpensesPanel`, `MonthlyBudgetPanel`, and `TransactionHistoryPanel` via `BudgetService.formatMoney(...)`.
  - **Interface Theme**: `LIGHT`, `FOREST`, `DARK` — immediately refreshes `BrandTheme` background, card, text, and sidebar colors across `MainFrame` when saved.
  - **Notifications & Budget Alert Threshold**: `budgetAlertsEnabled`, `emailSummaryEnabled`, `notificationPreferences`, and `defaultBudgetAlertPercent` (10%–100%).
- **Password Change**: Requires verifying the user's current password via `PasswordHasher.verifyPassword(...)` before hashing the new password with a fresh salt.

---

## 3. Restoration Upon Re-Login

When a user logs out and signs back in later:
1. `AuthenticationService.login(...)` queries `ProfileDAO.findByUserId(userId)` and `SettingsDAO.findByUserId(userId)`.
2. `SessionManager.startSession(user, profile, settings)` loads those exact saved objects into the active session.
3. `MainFrame` and `DashboardPanel` initialize immediately with the user's saved display name, theme, currency prefix, and alert preferences.

---

## 4. Code Example from `AuthenticationService.java`

```java
userDAO.updateBasicAccountInfo(userId, e, fn, ln);
profileDAO.upsertProfile(profile);
settingsDAO.upsertSettings(settings);

User updatedUser = userDAO.findById(userId);
UserProfile updatedProfile = profileDAO.findByUserId(userId);
UserSettings updatedSettings = settingsDAO.findByUserId(userId);
SessionManager.setCurrentUser(updatedUser);
SessionManager.setCurrentProfile(updatedProfile);
SessionManager.setCurrentSettings(updatedSettings);
```
