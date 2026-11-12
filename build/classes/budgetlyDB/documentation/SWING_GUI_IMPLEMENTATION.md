# Java Swing GUI Implementation

Budgetly's graphical desktop interface is built **100% with Java Swing** (`javax.swing.*` and `java.awt.*`), with zero external UI frameworks or web/JavaFX dependencies.

---

## 1. Visual Consistency & Branding (`BrandTheme.java`)

Before creating any new frames, we inspected `MainFrame.java` and `DashboardPanel.java` and preserved Budgetly's original visual identity:
- **Sidebar Primary Green**: `new Color(30, 55, 45)`
- **Emerald Accent**: `new Color(30, 100, 65)`
- **Content Background**: `new Color(245, 247, 250)`
- **White Surface Cards**: `Color.WHITE` with `1px` soft border `new Color(220, 228, 224)`
- **Warm Cream Mascot Accent**: `new Color(252, 248, 224)` matching the alligator mascot logo
- **Typography**: `SansSerif` (`26pt Bold` titles, `16pt Bold` sections, `24pt Bold` KPI metrics, `14pt Plain` inputs/tables, `Monospaced` for Java code snippets)

### Desktop App Logo Integration
`BrandTheme.applyAppIcons(JFrame)` loads `/budgetly/resources/budgetly-icon.png` and sets multi-resolution icons (`16x16`, `32x32`, `64x64`, `128x128`) via `frame.setIconImages(...)` on every `JFrame`. `LoginFrame` displays the full `/budgetly/resources/budgetly-logo.png` in its hero brand panel, while `MainFrame`, `AdminDashboard`, `SignupFrame`, `UserManagementFrame`, `DatabaseManagementFrame`, `ProfileSettingsFrame`, and `JavaModulesFrame` display the mascot badge in their headers.

---

## 2. Main `JFrame` Windows & Swing Components

| Window (`JFrame`) | Layout Managers Used | Key Components |
| :--- | :--- | :--- |
| `LoginFrame` | `BorderLayout`, `BoxLayout`, `GridBagLayout` | Logo card, `JTextField`, `JPasswordField`, `JCheckBox`, `JButton` |
| `SignupFrame` | `BorderLayout`, `BoxLayout`, `GridBagLayout` | Multi-column registration fields, `JPasswordField`, validation feedback |
| `MainFrame` | `BorderLayout`, `CardLayout`, `GridLayout` | Forest-green sidebar, mascot header, 8 financial/docs panels, Logout & Exit |
| `AdminDashboard` | `BorderLayout`, `GridLayout`, `FlowLayout` | KPI cards, `JTabbedPane`, 3 `JTable`s inside `JScrollPane`s |
| `UserManagementFrame` | `BorderLayout`, `FlowLayout`, `GridLayout` | Search bar, user `JTable`, `JOptionPane` dialogs |
| `DatabaseManagementFrame` | `BorderLayout`, `JSplitPane`, `JTabbedPane` | Status cards, `JList` of tables, read-only `JTable`, error monitor |
| `ProfileSettingsFrame` | `BorderLayout`, `GridLayout`, `GridBagLayout` | Profile fields, `JComboBox`, `JSpinner`, `JCheckBox`, password fields |
| `JavaModulesFrame` | `BorderLayout`, `JSplitPane` | Module `JList`, HTML `JEditorPane`, monospace `JTextArea`, Copy button |

---

## 3. Event Handling, EDT & `SwingWorker`

In Java Swing, all UI creation and updates must run on the **Event Dispatch Thread (EDT)** (`SwingUtilities.invokeLater(...)`). Running slow JDBC network queries or 120,000-iteration PBKDF2 hashing directly on the EDT would freeze buttons and window repainting.

Budgetly avoids UI freezing by running database and cryptographic operations inside **`SwingWorker<T, Void>`**:
- `doInBackground()` executes JDBC queries / `PasswordHasher` off the EDT on a worker thread.
- `done()` safely returns to the EDT to update `DefaultTableModel`, `JLabel` KPIs, or display `JOptionPane` notifications.

```java
SwingWorker<AuthenticationService.AuthResult, Void> worker = new SwingWorker<>() {
    @Override
    protected AuthenticationService.AuthResult doInBackground() {
        return authService.login(username, password); // Runs off EDT
    }

    @Override
    protected void done() {
        // Safely updates Swing UI back on the EDT
        AuthenticationService.AuthResult result = get();
        if (result.isSuccess()) {
            dispose();
            new MainFrame().setVisible(true);
        }
    }
};
worker.execute();
```
