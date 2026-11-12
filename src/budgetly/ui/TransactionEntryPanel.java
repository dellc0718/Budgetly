package budgetly.ui;

import budgetlyDB.BudgetService;
import budgetlyDB.SessionManager;
import budgetlyDB.model.BudgetTransaction;
import budgetlyDB.model.FinancialAccount;
import budgetlyDB.ui.BrandTheme;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Reusable transaction entry screen for IncomePanel and ExpensesPanel.
 */
public class TransactionEntryPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BudgetService budgetService = new BudgetService();
    private final String transactionType;
    private final String[] categories;
    private final Runnable onDataChanged;

    private JLabel headingLabel;
    private JLabel subLabel;
    private JPanel formCard;
    private JPanel tableCard;

    private JComboBox<FinancialAccount> accountCombo;
    private JComboBox<String> categoryCombo;
    private JTextField amountField;
    private JTextField dateField;
    private JTextField descriptionField;
    private JButton submitButton;
    private JLabel feedbackLabel;

    private DefaultTableModel tableModel;
    private JTable recentTable;

    public TransactionEntryPanel(String title, String subtitle, String transactionType,
                                 String[] categories, Runnable onDataChanged) {
        this.transactionType = transactionType;
        this.categories = categories;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel header = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        headingLabel = new JLabel(title);
        headingLabel.setFont(BrandTheme.FONT_TITLE);
        subLabel = new JLabel(subtitle);
        subLabel.setFont(BrandTheme.FONT_SUBTITLE);
        header.add(headingLabel);
        header.add(subLabel);

        add(header, BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        applyTheme();
        refreshData();
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        formCard = BrandTheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        accountCombo = new JComboBox<>();
        accountCombo.setFont(BrandTheme.FONT_BODY);
        categoryCombo = new JComboBox<>(categories);
        categoryCombo.setFont(BrandTheme.FONT_BODY);
        amountField = new JTextField(10);
        amountField.setFont(BrandTheme.FONT_BODY);
        dateField = new JTextField(LocalDate.now().format(DATE_FMT), 10);
        dateField.setFont(BrandTheme.FONT_BODY);
        descriptionField = new JTextField(18);
        descriptionField.setFont(BrandTheme.FONT_BODY);

        submitButton = "INCOME".equalsIgnoreCase(transactionType)
                ? BrandTheme.createPrimaryButton("+ Record Income")
                : BrandTheme.createDangerButton("- Record Expense");
        submitButton.addActionListener(e -> submitEntry());

        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0.25; formCard.add(new JLabel("Account"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.18; formCard.add(new JLabel("Category"), gbc);
        gbc.gridx = 2; gbc.weightx = 0.15; formCard.add(new JLabel("Amount"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.14; formCard.add(new JLabel("Date (YYYY-MM-DD)"), gbc);
        gbc.gridx = 4; gbc.weightx = 0.28; formCard.add(new JLabel("Description / Notes"), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0; formCard.add(accountCombo, gbc);
        gbc.gridx = 1; formCard.add(categoryCombo, gbc);
        gbc.gridx = 2; formCard.add(amountField, gbc);
        gbc.gridx = 3; formCard.add(dateField, gbc);
        gbc.gridx = 4; formCard.add(descriptionField, gbc);

        feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(BrandTheme.FONT_SMALL);
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        formCard.add(feedbackLabel, gbc);

        gbc.gridx = 4;
        gbc.gridwidth = 1;
        formCard.add(submitButton, gbc);

        tableCard = BrandTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 8));
        JLabel listTitle = new JLabel("INCOME".equalsIgnoreCase(transactionType)
                ? "Recent Income Records" : "Recent Expense Records");
        listTitle.setFont(BrandTheme.FONT_SECTION);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Date", "Account", "Category", "Description", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        recentTable = new JTable(tableModel);
        tableCard.add(listTitle, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(recentTable), BorderLayout.CENTER);

        body.add(formCard, BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        return body;
    }

    public final void applyTheme() {
        setBackground(BrandTheme.getContentBackground());
        headingLabel.setForeground(BrandTheme.getTextPrimary());
        subLabel.setForeground(BrandTheme.getTextMuted());
        formCard.setBackground(BrandTheme.getCardBackground());
        formCard.setBorder(BrandTheme.createCardBorder());
        tableCard.setBackground(BrandTheme.getCardBackground());
        tableCard.setBorder(BrandTheme.createCardBorder());
        BrandTheme.styleTable(recentTable);
    }

    public final void refreshData() {
        applyTheme();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private List<FinancialAccount> accounts;
            private List<BudgetTransaction> transactions;

            @Override
            protected Void doInBackground() throws Exception {
                accounts = budgetService.getUserAccounts();
                transactions = budgetService.searchTransactions(transactionType, "ALL", null, "");
                return null;
            }

            @Override
            protected void done() {
                if (!SessionManager.isLoggedIn()) {
                    return;
                }
                try {
                    get();
                    accountCombo.removeAllItems();
                    for (FinancialAccount a : accounts) {
                        accountCombo.addItem(a);
                    }
                    tableModel.setRowCount(0);
                    for (BudgetTransaction t : transactions) {
                        tableModel.addRow(new Object[]{
                                t.getTransactionId(),
                                t.getTransactionDate().format(DATE_FMT),
                                t.getAccountName(),
                                t.getCategory(),
                                t.getDescription(),
                                (t.isIncome() ? "+ " : "- ") + BudgetService.formatMoney(t.getAmount())
                        });
                    }
                } catch (Exception ex) {
                    feedbackLabel.setForeground(BrandTheme.DANGER_RED);
                    feedbackLabel.setText("Error loading records: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void submitEntry() {
        FinancialAccount account = (FinancialAccount) accountCombo.getSelectedItem();
        if (account == null) {
            JOptionPane.showMessageDialog(this,
                    "Please create an account first in the Accounts tab.",
                    "No Account Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String amtText = amountField.getText().trim().replace(",", "");
        BigDecimal amt;
        try {
            amt = new BigDecimal(amtText);
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            feedbackLabel.setForeground(BrandTheme.DANGER_RED);
            feedbackLabel.setText("Please enter a valid positive amount.");
            return;
        }

        LocalDate txDate;
        try {
            txDate = LocalDate.parse(dateField.getText().trim(), DATE_FMT);
        } catch (Exception ex) {
            feedbackLabel.setForeground(BrandTheme.DANGER_RED);
            feedbackLabel.setText("Invalid date. Use YYYY-MM-DD (e.g. " + LocalDate.now() + ").");
            return;
        }

        if ("EXPENSE".equalsIgnoreCase(transactionType) && amt.compareTo(account.getBalance()) > 0) {
            int proceed = JOptionPane.showConfirmDialog(this,
                    "This expense (" + BudgetService.formatMoney(amt) + ") exceeds the current balance of '"
                    + account.getAccountName() + "' (" + BudgetService.formatMoney(account.getBalance()) + ").\n"
                    + "Record this expense anyway?",
                    "Low Account Balance", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (proceed != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String category = String.valueOf(categoryCombo.getSelectedItem());
        String desc = descriptionField.getText().trim();

        submitButton.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                budgetService.recordTransaction(
                        account.getAccountId(), transactionType, category, amt, desc, txDate);
                return null;
            }

            @Override
            protected void done() {
                submitButton.setEnabled(true);
                try {
                    get();
                    amountField.setText("");
                    descriptionField.setText("");
                    feedbackLabel.setForeground(BrandTheme.EMERALD);
                    feedbackLabel.setText("Saved " + transactionType.toLowerCase() + " of "
                            + BudgetService.formatMoney(amt) + " to '" + account.getAccountName() + "'.");
                    refreshData();
                    if (onDataChanged != null) {
                        onDataChanged.run();
                    }
                } catch (Exception ex) {
                    feedbackLabel.setForeground(BrandTheme.DANGER_RED);
                    feedbackLabel.setText("Error saving transaction: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
