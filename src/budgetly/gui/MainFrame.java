package budgetly.gui;

import budgetly.model.Budget;
import budgetly.model.Transaction;
import budgetly.model.TransactionType;
import budgetly.model.User;
import budgetly.service.AuthService;
import budgetly.service.BudgetService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Main window for a Regular User.
 * Uses a sidebar and a CardLayout to switch between:
 * Dashboard, Income, Expenses, Budget, Transaction History, and Settings & Documentation.
 */
public class MainFrame extends JFrame {

    private static final String[] SORT_OPTIONS = {
            "Date (Newest First)", "Date (Oldest First)",
            "Amount (High to Low)", "Amount (Low to High)"
    };
    private static final String[] MODULE_TITLES = {
            "Module 1 - Java Compiler, JDK, IDE & Development Environment",
            "Module 2 - Data Types, Variables, Input & Output",
            "Module 3 - Classes, Objects & Encapsulation",
            "Module 4 - Inheritance, Polymorphism, Abstraction & Interfaces",
            "Module 5 - Java Errors & Exception Handling",
            "Module 6 - Java Collections Framework & Core Collection Types",
            "Module 7 - Multithreading & Java GUI Programming with Swing",
            "Module 8 - Database Programming with JDBC & Application Integration"
    };

    private final User currentUser;
    private final AuthService authService;
    private final BudgetService budgetService;

    private List<Transaction> allTransactions = new ArrayList<>();
    private Integer selectedIncomeId = null;
    private Integer selectedExpenseId = null;

    // Navigation
    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(contentLayout);
    private final JLabel topWelcomeLabel = new JLabel();

    // Dashboard components
    private final JLabel dashIncomeLabel = createCardValueLabel();
    private final JLabel dashExpenseLabel = createCardValueLabel();
    private final JLabel dashBalanceLabel = createCardValueLabel();
    private final JLabel dashCountLabel = createCardValueLabel();
    private final DefaultTableModel recentTableModel = createReadOnlyModel(
            new String[]{"ID", "Type", "Description", "Category", "Amount", "Date"});

    // Income components
    private final JTextField incDescField = new JTextField();
    private final JTextField incAmountField = new JTextField();
    private final JComboBox<String> incCategoryBox = new JComboBox<>(TransactionType.INCOME.getCategories());
    private final JTextField incDateField = new JTextField(LocalDate.now().toString());
    private final JTextField incSearchField = new JTextField(14);
    private final JComboBox<String> incSortBox = new JComboBox<>(SORT_OPTIONS);
    private final DefaultTableModel incTableModel = createReadOnlyModel(
            new String[]{"Income ID", "Description", "Amount", "Category", "Date"});

    // Expense components
    private final JTextField expDescField = new JTextField();
    private final JTextField expAmountField = new JTextField();
    private final JComboBox<String> expCategoryBox = new JComboBox<>(TransactionType.EXPENSE.getCategories());
    private final JTextField expDateField = new JTextField(LocalDate.now().toString());
    private final JTextField expSearchField = new JTextField(14);
    private final JComboBox<String> expSortBox = new JComboBox<>(SORT_OPTIONS);
    private final DefaultTableModel expTableModel = createReadOnlyModel(
            new String[]{"Expense ID", "Description", "Amount", "Category", "Date"});

    // Budget components
    private final JComboBox<Integer> budgetMonthBox = new JComboBox<>();
    private final JComboBox<Integer> budgetYearBox = new JComboBox<>();
    private final JTextField budgetLimitField = new JTextField(12);
    private final JLabel budgetLimitLabel = createCardValueLabel();
    private final JLabel budgetSpentLabel = createCardValueLabel();
    private final JLabel budgetRemainingLabel = createCardValueLabel();
    private final JProgressBar budgetProgressBar = new JProgressBar(0, 100);
    private final JLabel budgetWarningLabel = new JLabel(" ");

    // Transaction History components
    private final JTextField histSearchField = new JTextField(14);
    private final JComboBox<String> histTypeBox = new JComboBox<>(
            new String[]{"All Types", "INCOME", "EXPENSE"});
    private final JComboBox<String> histSortBox = new JComboBox<>(SORT_OPTIONS);
    private final DefaultTableModel histTableModel = createReadOnlyModel(
            new String[]{"Transaction ID", "Transaction Type", "Description", "Category", "Amount", "Date"});

    public MainFrame(User currentUser, AuthService authService, BudgetService budgetService) {
        this.currentUser = currentUser;
        this.authService = authService;
        this.budgetService = budgetService;

        setTitle("BUDGETLY - " + currentUser.getDashboardTitle());
        setSize(1150, 740);
        setMinimumSize(new Dimension(1020, 660));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        LoginFrame.applyWindowIcon(this);

        initBudgetMonthYearSelectors();
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(buildCenterArea(), BorderLayout.CENTER);

        refreshHeaderText();
        refreshAllData();
    }

    private void initBudgetMonthYearSelectors() {
        LocalDate today = LocalDate.now();
        for (int m = 1; m <= 12; m++) {
            budgetMonthBox.addItem(m);
        }
        for (int y = today.getYear() - 1; y <= today.getYear() + 2; y++) {
            budgetYearBox.addItem(y);
        }
        budgetMonthBox.setSelectedItem(today.getMonthValue());
        budgetYearBox.setSelectedItem(today.getYear());
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(LoginFrame.BRAND_DARK);
        sidebar.setPreferredSize(new Dimension(235, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 16, 18, 16));

        JLabel logo = new JLabel();
        ImageIcon icon = LoginFrame.loadLogoIcon(80, 80);
        if (icon != null) {
            logo.setIcon(icon);
        }
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel brandTitle = new JLabel("BUDGETLY");
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel roleSubtitle = new JLabel(currentUser.getRole().getLabel());
        roleSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        roleSubtitle.setForeground(new Color(180, 215, 195));
        roleSubtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(logo);
        sidebar.add(Box.createVerticalStrut(6));
        sidebar.add(brandTitle);
        sidebar.add(roleSubtitle);
        sidebar.add(Box.createVerticalStrut(22));

        sidebar.add(createNavButton("1. Dashboard", "DASHBOARD"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createNavButton("2. Income", "INCOME"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createNavButton("3. Expenses", "EXPENSES"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createNavButton("4. Budget", "BUDGET"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createNavButton("5. Transaction History", "HISTORY"));
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(createNavButton("6. Settings & Docs", "SETTINGS"));

        sidebar.add(Box.createVerticalGlue());
        JButton logoutBtn = createSidebarButton("7. Logout", new Color(180, 52, 52));
        logoutBtn.addActionListener(e -> handleLogout());
        sidebar.add(logoutBtn);
        return sidebar;
    }

    private JButton createNavButton(String text, String cardName) {
        JButton btn = createSidebarButton(text, new Color(36, 82, 70));
        btn.addActionListener(e -> contentLayout.show(contentPanel, cardName));
        return btn;
    }

    private JButton createSidebarButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(62, 115, 100), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return btn;
    }

    private JPanel buildCenterArea() {
        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(LoginFrame.BG_LIGHT);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(215, 225, 218)),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        topWelcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        topWelcomeLabel.setForeground(LoginFrame.BRAND_DARK);

        JButton refreshBtn = LoginFrame.createSecondaryButton("Refresh Data");
        refreshBtn.addActionListener(e -> refreshAllData());

        topBar.add(topWelcomeLabel, BorderLayout.WEST);
        topBar.add(refreshBtn, BorderLayout.EAST);

        contentPanel.setBackground(LoginFrame.BG_LIGHT);
        contentPanel.add(buildDashboardCard(), "DASHBOARD");
        contentPanel.add(buildIncomeCard(), "INCOME");
        contentPanel.add(buildExpenseCard(), "EXPENSES");
        contentPanel.add(buildBudgetCard(), "BUDGET");
        contentPanel.add(buildHistoryCard(), "HISTORY");
        contentPanel.add(createSettingsAndDocsPanel(
                currentUser, authService, this::refreshHeaderText), "SETTINGS");

        area.add(topBar, BorderLayout.NORTH);
        area.add(contentPanel, BorderLayout.CENTER);
        return area;
    }

    private void refreshHeaderText() {
        topWelcomeLabel.setText(currentUser.getDashboardTitle() + "  |  Signed in as "
                + currentUser.getFullName() + " (@" + currentUser.getUsername() + ")");
    }

    // ---------------- Card 1: Dashboard ----------------

    private JPanel buildDashboardCard() {
        JPanel panel = createPagePanel("Overview & Recent Activity");

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(createStatCard("Total Income", dashIncomeLabel, new Color(34, 139, 74)));
        cards.add(createStatCard("Total Expenses", dashExpenseLabel, new Color(192, 57, 43)));
        cards.add(createStatCard("Current Balance", dashBalanceLabel, new Color(24, 90, 157)));
        cards.add(createStatCard("Transactions", dashCountLabel, new Color(115, 81, 16)));

        JTable recentTable = styleTable(new JTable(recentTableModel));
        JPanel tableSection = createTitledBox("Recent Transactions (Latest 10)", new JScrollPane(recentTable));

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(cards, BorderLayout.NORTH);
        body.add(tableSection, BorderLayout.CENTER);

        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    // ---------------- Card 2 & 3: Income & Expense Management ----------------

    private JPanel buildIncomeCard() {
        return buildTransactionCard(TransactionType.INCOME,
                incDescField, incAmountField, incCategoryBox, incDateField,
                incSearchField, incSortBox, incTableModel);
    }

    private JPanel buildExpenseCard() {
        return buildTransactionCard(TransactionType.EXPENSE,
                expDescField, expAmountField, expCategoryBox, expDateField,
                expSearchField, expSortBox, expTableModel);
    }

    private JPanel buildTransactionCard(TransactionType type, JTextField descField,
                                        JTextField amountField, JComboBox<String> categoryBox,
                                        JTextField dateField, JTextField searchField,
                                        JComboBox<String> sortBox, DefaultTableModel model) {
        String label = type.getLabel();
        JPanel panel = createPagePanel(label + " Management");

        JPanel form = new JPanel(new GridLayout(2, 4, 10, 6));
        form.setBackground(Color.WHITE);
        form.add(new JLabel("Description:"));
        form.add(new JLabel("Amount (PHP):"));
        form.add(new JLabel("Category:"));
        form.add(new JLabel("Date (YYYY-MM-DD):"));
        form.add(descField);
        form.add(amountField);
        form.add(categoryBox);
        form.add(dateField);

        JButton addBtn = LoginFrame.createPrimaryButton("Add " + label);
        JButton updateBtn = LoginFrame.createSecondaryButton("Update Selected");
        JButton deleteBtn = LoginFrame.createSecondaryButton("Delete Selected");
        JButton clearBtn = LoginFrame.createSecondaryButton("Clear Form");

        addBtn.addActionListener(e -> saveTransactionForm(type, false));
        updateBtn.addActionListener(e -> saveTransactionForm(type, true));
        deleteBtn.addActionListener(e -> deleteSelectedTransaction(type));
        clearBtn.addActionListener(e -> clearTransactionForm(type));

        JPanel formCard = createFormAndButtonsBox("Add or Edit " + label, form, addBtn, updateBtn, deleteBtn, clearBtn);

        JTable table = styleTable(new JTable(model));
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                Integer id = (Integer) model.getValueAt(row, 0);
                if (type == TransactionType.INCOME) {
                    selectedIncomeId = id;
                } else {
                    selectedExpenseId = id;
                }
                descField.setText(String.valueOf(model.getValueAt(row, 1)));
                amountField.setText(String.valueOf(model.getValueAt(row, 2)).replace("PHP ", "").replace(",", ""));
                categoryBox.setSelectedItem(model.getValueAt(row, 3));
                dateField.setText(String.valueOf(model.getValueAt(row, 4)));
            }
        });

        Runnable refresher = (type == TransactionType.INCOME) ? this::updateIncomeTable : this::updateExpenseTable;
        JPanel filterBar = createSearchSortBar(searchField, sortBox, refresher);
        JPanel tableBox = createTableWithFilterBox(label + " History", filterBar, table);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(formCard, BorderLayout.NORTH);
        body.add(tableBox, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    // ---------------- Card 4: Budget Management ----------------

    private JPanel buildBudgetCard() {
        JPanel panel = createPagePanel("Monthly Budget Management");

        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        controlBar.setBackground(Color.WHITE);
        controlBar.setBorder(BorderFactory.createTitledBorder("Set or Edit Monthly Budget"));

        JButton loadMonthBtn = LoginFrame.createSecondaryButton("Load Month");
        JButton saveBudgetBtn = LoginFrame.createPrimaryButton("Save Monthly Budget");
        loadMonthBtn.addActionListener(e -> refreshBudgetSection(false));
        saveBudgetBtn.addActionListener(e -> handleSaveBudget());

        controlBar.add(new JLabel("Month:"));
        controlBar.add(budgetMonthBox);
        controlBar.add(new JLabel("Year:"));
        controlBar.add(budgetYearBox);
        controlBar.add(loadMonthBtn);
        controlBar.add(new JLabel("Monthly Limit (PHP):"));
        controlBar.add(budgetLimitField);
        controlBar.add(saveBudgetBtn);

        JPanel cards = new JPanel(new GridLayout(1, 3, 14, 0));
        cards.setOpaque(false);
        cards.add(createStatCard("Monthly Budget Limit", budgetLimitLabel, new Color(24, 90, 157)));
        cards.add(createStatCard("Total Monthly Expenses", budgetSpentLabel, new Color(192, 57, 43)));
        cards.add(createStatCard("Remaining Budget", budgetRemainingLabel, new Color(34, 139, 74)));

        JPanel monitorBox = new JPanel(new BorderLayout(0, 10));
        monitorBox.setBackground(Color.WHITE);
        monitorBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Budget Usage Monitor"),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        budgetProgressBar.setStringPainted(true);
        budgetProgressBar.setFont(new Font("SansSerif", Font.BOLD, 14));
        budgetProgressBar.setPreferredSize(new Dimension(400, 32));

        budgetWarningLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        monitorBox.add(budgetProgressBar, BorderLayout.NORTH);
        monitorBox.add(budgetWarningLabel, BorderLayout.CENTER);

        JPanel centerStack = new JPanel(new BorderLayout(0, 16));
        centerStack.setOpaque(false);
        centerStack.add(cards, BorderLayout.NORTH);
        centerStack.add(monitorBox, BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(controlBar, BorderLayout.NORTH);
        body.add(centerStack, BorderLayout.CENTER);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    // ---------------- Card 5: Transaction History ----------------

    private JPanel buildHistoryCard() {
        JPanel panel = createPagePanel("Complete Transaction History");

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        filterBar.setBackground(Color.WHITE);
        JButton applyBtn = LoginFrame.createPrimaryButton("Apply Filter");
        JButton resetBtn = LoginFrame.createSecondaryButton("Reset");
        applyBtn.addActionListener(e -> updateHistoryTable());
        histTypeBox.addActionListener(e -> updateHistoryTable());
        histSortBox.addActionListener(e -> updateHistoryTable());
        resetBtn.addActionListener(e -> {
            histSearchField.setText("");
            histTypeBox.setSelectedIndex(0);
            histSortBox.setSelectedIndex(0);
            updateHistoryTable();
        });

        filterBar.add(new JLabel("Search:"));
        filterBar.add(histSearchField);
        filterBar.add(new JLabel("Type:"));
        filterBar.add(histTypeBox);
        filterBar.add(new JLabel("Sort By:"));
        filterBar.add(histSortBox);
        filterBar.add(applyBtn);
        filterBar.add(resetBtn);

        JTable table = styleTable(new JTable(histTableModel));
        JPanel box = createTableWithFilterBox("All Income & Expense Records", filterBar, table);
        panel.add(box, BorderLayout.CENTER);
        return panel;
    }

    // ---------------- Card 6: Settings & Documentation (Reusable) ----------------

    /**
     * Builds the Settings & Documentation panel with two internal tabs:
     * 1. Profile Settings
     * 2. Documentation & Learning Guide (User Manual + Java Modules 1-8 Implementation Guide)
     */
    public static JPanel createSettingsAndDocsPanel(User user, AuthService authService,
                                                    Runnable onProfileUpdated) {
        JPanel page = createPagePanel("Settings & Documentation");
        JTabbedPane settingsTabs = new JTabbedPane();
        settingsTabs.setFont(new Font("SansSerif", Font.BOLD, 13));
        settingsTabs.addTab("1. Profile Settings",
                buildProfileSettingsTab(user, authService, onProfileUpdated));
        settingsTabs.addTab("2. Documentation & Learning Guide",
                buildDocumentationTab());
        page.add(settingsTabs, BorderLayout.CENTER);
        return page;
    }

    private static JPanel buildProfileSettingsTab(User user, AuthService authService,
                                                  Runnable onProfileUpdated) {
        JPanel container = new JPanel(new GridLayout(1, 2, 16, 0));
        container.setBackground(LoginFrame.BG_LIGHT);
        container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Left column: Personal Info + Read-Only Account Info
        JTextField fullNameField = new JTextField(user.getFullName());
        JTextField usernameField = new JTextField(user.getUsername());
        JTextField emailField = new JTextField(user.getEmail() == null ? "" : user.getEmail());

        JLabel roUsernameLabel = new JLabel(user.getUsername());
        JLabel roRoleLabel = new JLabel(user.getRole().getLabel() + " (Read-Only)");
        JLabel roStatusLabel = new JLabel(user.getStatusText() + " (Managed by Admin)");
        String createdDate = user.getCreatedAt() == null ? "N/A"
                : user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        JLabel roDateLabel = new JLabel(createdDate);

        JPanel personalPanel = new JPanel(new GridLayout(7, 2, 8, 8));
        personalPanel.setBackground(Color.WHITE);
        personalPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("A. Personal & Account Information"),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        personalPanel.add(new JLabel("Full Name:"));
        personalPanel.add(fullNameField);
        personalPanel.add(new JLabel("Username:"));
        personalPanel.add(usernameField);
        personalPanel.add(new JLabel("Email Address:"));
        personalPanel.add(emailField);
        personalPanel.add(new JLabel("Current Username:"));
        personalPanel.add(roUsernameLabel);
        personalPanel.add(new JLabel("Account Role:"));
        personalPanel.add(roRoleLabel);
        personalPanel.add(new JLabel("Account Status:"));
        personalPanel.add(roStatusLabel);
        personalPanel.add(new JLabel("Registration Date:"));
        personalPanel.add(roDateLabel);

        JButton saveProfileBtn = LoginFrame.createPrimaryButton("Save Personal Information");
        saveProfileBtn.addActionListener(e -> {
            try {
                String error = authService.updateProfile(user,
                        fullNameField.getText(), usernameField.getText(), emailField.getText());
                if (error != null) {
                    JOptionPane.showMessageDialog(container, error,
                            "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                roUsernameLabel.setText(user.getUsername());
                if (onProfileUpdated != null) {
                    onProfileUpdated.run();
                }
                JOptionPane.showMessageDialog(container,
                        "Your profile information was updated successfully.",
                        "Profile Updated", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(container,
                        "Database error while updating profile: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel leftBox = new JPanel(new BorderLayout(0, 10));
        leftBox.setOpaque(false);
        leftBox.add(personalPanel, BorderLayout.CENTER);
        leftBox.add(saveProfileBtn, BorderLayout.SOUTH);

        // Right column: Password Management
        JPasswordField currentPassField = new JPasswordField();
        JPasswordField newPassField = new JPasswordField();
        JPasswordField confirmPassField = new JPasswordField();

        JPanel passPanel = new JPanel(new GridLayout(6, 1, 6, 6));
        passPanel.setBackground(Color.WHITE);
        passPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("B. Change Password (PBKDF2 Salted Hash)"),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        passPanel.add(new JLabel("Current Password:"));
        passPanel.add(currentPassField);
        passPanel.add(new JLabel("New Password (minimum 6 characters):"));
        passPanel.add(newPassField);
        passPanel.add(new JLabel("Confirm New Password:"));
        passPanel.add(confirmPassField);

        JButton changePassBtn = LoginFrame.createPrimaryButton("Update Password");
        changePassBtn.addActionListener(e -> {
            try {
                String error = authService.changePassword(user,
                        new String(currentPassField.getPassword()),
                        new String(newPassField.getPassword()),
                        new String(confirmPassField.getPassword()));
                if (error != null) {
                    JOptionPane.showMessageDialog(container, error,
                            "Password Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                currentPassField.setText("");
                newPassField.setText("");
                confirmPassField.setText("");
                JOptionPane.showMessageDialog(container,
                        "Your password has been changed with a new cryptographic salt.",
                        "Password Updated", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(container,
                        "Database error while changing password: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel rightBox = new JPanel(new BorderLayout(0, 10));
        rightBox.setOpaque(false);
        rightBox.add(passPanel, BorderLayout.CENTER);
        rightBox.add(changePassBtn, BorderLayout.SOUTH);

        container.add(leftBox);
        container.add(rightBox);
        return container;
    }

    private static JPanel buildDocumentationTab() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);

        JTabbedPane docCategories = new JTabbedPane();
        docCategories.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Category A: User Manual
        JTextArea manualArea = createCodeFriendlyTextArea();
        manualArea.setText(loadDocumentationFile("USER_MANUAL.md"));
        manualArea.setCaretPosition(0);
        JPanel manualPanel = new JPanel(new BorderLayout(0, 8));
        manualPanel.setBackground(Color.WHITE);
        manualPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JLabel manualHeader = new JLabel("BUDGETLY Complete User Manual (Getting Started to Troubleshooting)");
        manualHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        manualPanel.add(manualHeader, BorderLayout.NORTH);
        manualPanel.add(new JScrollPane(manualArea), BorderLayout.CENTER);

        // Category B: Java Modules 1-8 Implementation Guide
        JPanel modulesPanel = new JPanel(new BorderLayout(0, 8));
        modulesPanel.setBackground(Color.WHITE);
        modulesPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JComboBox<String> moduleSelector = new JComboBox<>(MODULE_TITLES);
        JButton prevBtn = LoginFrame.createSecondaryButton("Previous Module");
        JButton nextBtn = LoginFrame.createSecondaryButton("Next Module");
        JTextArea moduleArea = createCodeFriendlyTextArea();

        Runnable loadSelectedModule = () -> {
            int idx = moduleSelector.getSelectedIndex();
            String fileName = "MODULE_" + (idx + 1) + ".md";
            moduleArea.setText(loadDocumentationFile(fileName));
            moduleArea.setCaretPosition(0);
        };

        moduleSelector.addActionListener(e -> loadSelectedModule.run());
        prevBtn.addActionListener(e -> {
            int i = moduleSelector.getSelectedIndex();
            if (i > 0) {
                moduleSelector.setSelectedIndex(i - 1);
            }
        });
        nextBtn.addActionListener(e -> {
            int i = moduleSelector.getSelectedIndex();
            if (i < MODULE_TITLES.length - 1) {
                moduleSelector.setSelectedIndex(i + 1);
            }
        });

        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        navBar.setBackground(Color.WHITE);
        navBar.add(new JLabel("JAVA MODULES IMPLEMENTATION GUIDE:"));
        navBar.add(moduleSelector);
        navBar.add(prevBtn);
        navBar.add(nextBtn);

        loadSelectedModule.run();
        modulesPanel.add(navBar, BorderLayout.NORTH);
        modulesPanel.add(new JScrollPane(moduleArea), BorderLayout.CENTER);

        docCategories.addTab("A. User Manual", manualPanel);
        docCategories.addTab("B. JAVA MODULES IMPLEMENTATION GUIDE (Modules 1-8)", modulesPanel);
        outer.add(docCategories, BorderLayout.CENTER);
        return outer;
    }

    private static JTextArea createCodeFriendlyTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new java.awt.Insets(12, 14, 12, 14));
        return area;
    }

    public static String loadDocumentationFile(String fileName) {
        String[] paths = {
                "documentation/" + fileName,
                "Budgetly/documentation/" + fileName
        };
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    return "Unable to read " + fileName + ": " + e.getMessage();
                }
            }
        }
        return "Documentation file not found: " + fileName;
    }

    // ---------------- Background Data Loading & Event Handlers ----------------

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

    private void updateDashboardSection() {
        BigDecimal totalIncome = budgetService.calculateTotal(allTransactions, TransactionType.INCOME);
        BigDecimal totalExpense = budgetService.calculateTotal(allTransactions, TransactionType.EXPENSE);
        BigDecimal balance = budgetService.calculateBalance(allTransactions);

        dashIncomeLabel.setText(BudgetService.formatMoney(totalIncome));
        dashExpenseLabel.setText(BudgetService.formatMoney(totalExpense));
        dashBalanceLabel.setText(BudgetService.formatMoney(balance));
        dashCountLabel.setText(String.valueOf(allTransactions.size()));

        recentTableModel.setRowCount(0);
        int limit = Math.min(10, allTransactions.size());
        for (int i = 0; i < limit; i++) {
            Transaction tx = allTransactions.get(i);
            recentTableModel.addRow(new Object[]{
                    tx.getTransactionId(), tx.getType().getLabel(), tx.getDescription(),
                    tx.getCategory(), BudgetService.formatMoney(tx.getAmount()), tx.getDate()
            });
        }
    }

    private void updateIncomeTable() {
        populateTypeTable(TransactionType.INCOME, incSearchField.getText(),
                (String) incSortBox.getSelectedItem(), incTableModel);
    }

    private void updateExpenseTable() {
        populateTypeTable(TransactionType.EXPENSE, expSearchField.getText(),
                (String) expSortBox.getSelectedItem(), expTableModel);
    }

    private void populateTypeTable(TransactionType type, String query,
                                   String sortOption, DefaultTableModel model) {
        List<Transaction> list = budgetService.filterByType(allTransactions, type);
        list = BudgetService.search(list, query);
        list = budgetService.sortTransactions(list, sortOption);

        model.setRowCount(0);
        for (Transaction tx : list) {
            model.addRow(new Object[]{
                    tx.getTransactionId(), tx.getDescription(),
                    BudgetService.formatMoney(tx.getAmount()), tx.getCategory(), tx.getDate()
            });
        }
    }

    private void updateHistoryTable() {
        String selectedType = (String) histTypeBox.getSelectedItem();
        TransactionType typeFilter = "INCOME".equals(selectedType) ? TransactionType.INCOME
                : ("EXPENSE".equals(selectedType) ? TransactionType.EXPENSE : null);

        List<Transaction> list = budgetService.filterByType(allTransactions, typeFilter);
        list = BudgetService.search(list, histSearchField.getText());
        list = budgetService.sortTransactions(list, (String) histSortBox.getSelectedItem());

        histTableModel.setRowCount(0);
        for (Transaction tx : list) {
            histTableModel.addRow(new Object[]{
                    tx.getTransactionId(), tx.getType().getLabel(), tx.getDescription(),
                    tx.getCategory(), BudgetService.formatMoney(tx.getAmount()), tx.getDate()
            });
        }
    }

    private void saveTransactionForm(TransactionType type, boolean isUpdate) {
        boolean isIncome = (type == TransactionType.INCOME);
        Integer targetId = isUpdate ? (isIncome ? selectedIncomeId : selectedExpenseId) : null;
        if (isUpdate && targetId == null) {
            JOptionPane.showMessageDialog(this,
                    "Please click a row in the table first before updating.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String desc = isIncome ? incDescField.getText() : expDescField.getText();
        String amount = isIncome ? incAmountField.getText() : expAmountField.getText();
        String category = (String) (isIncome ? incCategoryBox.getSelectedItem() : expCategoryBox.getSelectedItem());
        String date = isIncome ? incDateField.getText() : expDateField.getText();

        try {
            String error = budgetService.saveTransaction(
                    targetId, currentUser.getUserId(), type, desc, amount, category, date);
            if (error != null) {
                JOptionPane.showMessageDialog(this, error,
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            clearTransactionForm(type);
            refreshAllData();
            if (type == TransactionType.EXPENSE) {
                refreshBudgetSection(true);
            }
            JOptionPane.showMessageDialog(this,
                    type.getLabel() + (isUpdate ? " updated" : " added") + " successfully!",
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error while saving record: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedTransaction(TransactionType type) {
        boolean isIncome = (type == TransactionType.INCOME);
        Integer targetId = isIncome ? selectedIncomeId : selectedExpenseId;
        if (targetId == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a record in the table to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this " + type.getLabel().toLowerCase() + " record (ID #" + targetId + ")?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (budgetService.deleteTransaction(targetId, currentUser.getUserId())) {
                clearTransactionForm(type);
                refreshAllData();
                JOptionPane.showMessageDialog(this, "Record deleted successfully.",
                        "Deleted", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error while deleting record: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearTransactionForm(TransactionType type) {
        if (type == TransactionType.INCOME) {
            selectedIncomeId = null;
            incDescField.setText("");
            incAmountField.setText("");
            incCategoryBox.setSelectedIndex(0);
            incDateField.setText(LocalDate.now().toString());
        } else {
            selectedExpenseId = null;
            expDescField.setText("");
            expAmountField.setText("");
            expCategoryBox.setSelectedIndex(0);
            expDateField.setText(LocalDate.now().toString());
        }
    }

    private void handleSaveBudget() {
        int month = (Integer) budgetMonthBox.getSelectedItem();
        int year = (Integer) budgetYearBox.getSelectedItem();
        try {
            String error = budgetService.saveBudget(
                    currentUser.getUserId(), budgetLimitField.getText(), month, year);
            if (error != null) {
                JOptionPane.showMessageDialog(this, error,
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            refreshBudgetSection(true);
            JOptionPane.showMessageDialog(this,
                    "Monthly budget saved for " + month + "/" + year + ".",
                    "Budget Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Database error while saving budget: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshBudgetSection(boolean showPopupIfExceeded) {
        int month = (Integer) budgetMonthBox.getSelectedItem();
        int year = (Integer) budgetYearBox.getSelectedItem();
        try {
            Budget budget = budgetService.getBudget(currentUser.getUserId(), month, year);
            BigDecimal spent = budgetService.getMonthlyExpenses(currentUser.getUserId(), month, year);
            budgetSpentLabel.setText(BudgetService.formatMoney(spent));

            if (budget == null) {
                budgetLimitLabel.setText("Not Set");
                budgetRemainingLabel.setText("Not Set");
                budgetProgressBar.setValue(0);
                budgetProgressBar.setString("No budget set for " + month + "/" + year);
                budgetWarningLabel.setForeground(new Color(95, 110, 102));
                budgetWarningLabel.setText("Tip: Enter a monthly limit above to monitor your spending.");
                return;
            }

            budgetLimitField.setText(budget.getMonthlyLimit().toPlainString());
            budgetLimitLabel.setText(BudgetService.formatMoney(budget.getMonthlyLimit()));
            BigDecimal remaining = budget.getRemaining(spent);
            budgetRemainingLabel.setText(BudgetService.formatMoney(remaining));

            Double pct = budget.getUsagePercentage(spent);
            budgetProgressBar.setValue(Math.min(100, pct.intValue()));
            budgetProgressBar.setString(pct + "% of monthly budget used");

            if (budget.isReached(spent)) {
                budgetWarningLabel.setForeground(new Color(192, 57, 43));
                budgetWarningLabel.setText("WARNING: Your monthly expenses have reached or exceeded your budget limit!");
                if (showPopupIfExceeded) {
                    JOptionPane.showMessageDialog(this,
                            "Budget Warning: Your monthly expenses (" + BudgetService.formatMoney(spent)
                                    + ") have reached or exceeded your budget limit ("
                                    + BudgetService.formatMoney(budget.getMonthlyLimit()) + ")!",
                            "Budget Limit Warning", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                budgetWarningLabel.setForeground(LoginFrame.BRAND_GREEN);
                budgetWarningLabel.setText("You are within your monthly budget. Remaining: "
                        + BudgetService.formatMoney(remaining));
            }
        } catch (SQLException ex) {
            budgetWarningLabel.setText("Unable to load budget: " + ex.getMessage());
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Do you want to sign out of BUDGETLY?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new LoginFrame(authService).setVisible(true);
            dispose();
        }
    }

    // ---------------- Small Layout Helpers ----------------

    public static JPanel createPagePanel(String heading) {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBackground(LoginFrame.BG_LIGHT);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JLabel title = new JLabel(heading);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(LoginFrame.BRAND_DARK);
        panel.add(title, BorderLayout.NORTH);
        return panel;
    }

    public static JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(4, 0, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLbl.setForeground(new Color(95, 110, 102));
        valueLabel.setForeground(accentColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public static JLabel createCardValueLabel() {
        JLabel label = new JLabel("PHP 0.00");
        label.setFont(new Font("SansSerif", Font.BOLD, 20));
        return label;
    }

    public static DefaultTableModel createReadOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static JTable styleTable(JTable table) {
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(LoginFrame.BRAND_DARK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(212, 237, 222));
        table.setSelectionForeground(LoginFrame.BRAND_DARK);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        table.getTableHeader().setForeground(LoginFrame.BRAND_DARK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    private JPanel createFormAndButtonsBox(String title, JPanel formGrid, JButton... buttons) {
        JPanel box = new JPanel(new BorderLayout(0, 10));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setBackground(Color.WHITE);
        for (JButton btn : buttons) {
            btnRow.add(btn);
        }
        box.add(formGrid, BorderLayout.CENTER);
        box.add(btnRow, BorderLayout.SOUTH);
        return box;
    }

    private JPanel createSearchSortBar(JTextField searchField, JComboBox<String> sortBox, Runnable onUpdate) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        bar.setBackground(Color.WHITE);
        JButton searchBtn = LoginFrame.createPrimaryButton("Search");
        JButton resetBtn = LoginFrame.createSecondaryButton("Show All");

        searchBtn.addActionListener(e -> onUpdate.run());
        sortBox.addActionListener(e -> onUpdate.run());
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            sortBox.setSelectedIndex(0);
            onUpdate.run();
        });

        bar.add(new JLabel("Search:"));
        bar.add(searchField);
        bar.add(searchBtn);
        bar.add(resetBtn);
        bar.add(new JLabel("Sort By:"));
        bar.add(sortBox);
        return bar;
    }

    private JPanel createTableWithFilterBox(String title, JPanel filterBar, JTable table) {
        JPanel box = new JPanel(new BorderLayout(0, 8));
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(8, 12, 12, 12)));
        box.add(filterBar, BorderLayout.NORTH);
        box.add(new JScrollPane(table), BorderLayout.CENTER);
        return box;
    }

    private JPanel createTitledBox(String title, JComponent content) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(8, 12, 12, 12)));
        box.add(content, BorderLayout.CENTER);
        return box;
    }
}
