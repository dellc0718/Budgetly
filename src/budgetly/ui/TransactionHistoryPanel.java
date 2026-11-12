package budgetly.ui;

import budgetlyDB.BudgetService;
import budgetlyDB.SessionManager;
import budgetlyDB.model.BudgetTransaction;
import budgetlyDB.model.FinancialAccount;
import budgetlyDB.ui.BrandTheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Screen for viewing, filtering, searching, and managing transaction history.
 */
public class TransactionHistoryPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BudgetService budgetService = new BudgetService();
    private final Runnable onDataChanged;

    private JLabel headingLabel;
    private JLabel subLabel;
    private JPanel filterCard;
    private JPanel tableCard;

    private JComboBox<String> typeCombo;
    private JComboBox<String> categoryCombo;
    private JComboBox<AccountFilterItem> accountCombo;
    private JTextField searchField;

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel countLabel;
    private List<BudgetTransaction> displayedTransactions = new ArrayList<>();

    private static final class AccountFilterItem {
        private final Integer accountId;
        private final String label;

        AccountFilterItem(Integer accountId, String label) {
            this.accountId = accountId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public TransactionHistoryPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel header = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        headingLabel = new JLabel("Transaction History");
        headingLabel.setFont(BrandTheme.FONT_TITLE);
        subLabel = new JLabel("View, search, and filter your income and expense records.");
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

        filterCard = BrandTheme.createCardPanel();
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));

        typeCombo = new JComboBox<>(new String[]{"ALL", "INCOME", "EXPENSE"});
        budgetlyDB.ui.UiLabels.install(typeCombo);
        categoryCombo = new JComboBox<>(new String[]{
                "ALL", "Salary", "Business", "Freelance", "Allowance", "Investment",
                "Food", "Transport", "Bills", "Shopping", "Health", "Education", "Entertainment", "Gift", "Other"
        });
        accountCombo = new JComboBox<>();
        searchField = new JTextField(15);
        searchField.addActionListener(e -> loadTransactionsAsync());

        JButton filterBtn = BrandTheme.createPrimaryButton("Apply Filter");
        filterBtn.addActionListener(e -> loadTransactionsAsync());
        JButton resetBtn = BrandTheme.createSecondaryButton("Reset");
        resetBtn.addActionListener(e -> {
            typeCombo.setSelectedIndex(0);
            categoryCombo.setSelectedIndex(0);
            if (accountCombo.getItemCount() > 0) {
                accountCombo.setSelectedIndex(0);
            }
            searchField.setText("");
            loadTransactionsAsync();
        });

        filterCard.add(new JLabel("Type:"));
        filterCard.add(typeCombo);
        filterCard.add(new JLabel("Category:"));
        filterCard.add(categoryCombo);
        filterCard.add(new JLabel("Account:"));
        filterCard.add(accountCombo);
        filterCard.add(new JLabel("Search:"));
        filterCard.add(searchField);
        filterCard.add(filterBtn);
        filterCard.add(resetBtn);

        tableCard = BrandTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Date", "Type", "Account", "Category", "Description", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        budgetlyDB.ui.UiLabels.installTableColumn(table, 2);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        countLabel = new JLabel("0 records");
        countLabel.setFont(BrandTheme.FONT_SMALL);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        JButton deleteBtn = BrandTheme.createDangerButton("Delete Selected (Reverse Balance)");
        deleteBtn.addActionListener(e -> deleteSelectedTransaction());
        JButton editBtn = BrandTheme.createSecondaryButton("Edit Selected");
        editBtn.addActionListener(e -> editSelectedTransaction());
        JButton refreshBtn = BrandTheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        btns.add(editBtn);
        btns.add(deleteBtn);
        btns.add(refreshBtn);

        bottom.add(countLabel, BorderLayout.WEST);
        bottom.add(btns, BorderLayout.EAST);

        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        tableCard.add(bottom, BorderLayout.SOUTH);

        body.add(filterCard, BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        return body;
    }

    public final void applyTheme() {
        setBackground(BrandTheme.getContentBackground());
        headingLabel.setForeground(BrandTheme.getTextPrimary());
        subLabel.setForeground(BrandTheme.getTextMuted());
        filterCard.setBackground(BrandTheme.getCardBackground());
        filterCard.setBorder(BrandTheme.createCardBorder());
        tableCard.setBackground(BrandTheme.getCardBackground());
        tableCard.setBorder(BrandTheme.createCardBorder());
        countLabel.setForeground(BrandTheme.getTextMuted());
        BrandTheme.styleTable(table);
    }

    public final void refreshData() {
        applyTheme();
        SwingWorker<List<FinancialAccount>, Void> accWorker = new SwingWorker<>() {
            @Override
            protected List<FinancialAccount> doInBackground() throws Exception {
                return budgetService.getUserAccounts();
            }

            @Override
            protected void done() {
                try {
                    List<FinancialAccount> accounts = get();
                    accountCombo.removeAllItems();
                    accountCombo.addItem(new AccountFilterItem(null, "All Accounts"));
                    for (FinancialAccount a : accounts) {
                        accountCombo.addItem(new AccountFilterItem(a.getAccountId(), a.getAccountName()));
                    }
                } catch (Exception ignored) {
                }
                loadTransactionsAsync();
            }
        };
        accWorker.execute();
    }

    private void loadTransactionsAsync() {
        String type = String.valueOf(typeCombo.getSelectedItem());
        String cat = String.valueOf(categoryCombo.getSelectedItem());
        AccountFilterItem accItem = (AccountFilterItem) accountCombo.getSelectedItem();
        Integer accId = accItem == null ? null : accItem.accountId;
        String kw = searchField.getText().trim();

        SwingWorker<List<BudgetTransaction>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BudgetTransaction> doInBackground() throws Exception {
                return budgetService.searchTransactions(type, cat, accId, kw);
            }

            @Override
            protected void done() {
                if (!SessionManager.isLoggedIn()) {
                    return;
                }
                try {
                    List<BudgetTransaction> list = get();
                    displayedTransactions = list;
                    tableModel.setRowCount(0);
                    for (BudgetTransaction t : list) {
                        tableModel.addRow(new Object[]{
                                t.getTransactionId(),
                                t.getTransactionDate().format(DATE_FMT),
                                t.getTransactionType(),
                                t.getAccountName(),
                                t.getCategory(),
                                t.getDescription(),
                                (t.isIncome() ? "+ " : "- ") + BudgetService.formatMoney(t.getAmount())
                        });
                    }
                    countLabel.setText("Showing " + list.size() + " transaction(s)");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TransactionHistoryPanel.this,
                            "Error loading transaction history: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void deleteSelectedTransaction() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a transaction row to delete.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int txId = (Integer) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete transaction #" + txId + " and reverse its amount on the linked account balance?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            budgetService.deleteTransaction(txId);
            loadTransactionsAsync();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete transaction: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void editSelectedTransaction() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= displayedTransactions.size()) {
            JOptionPane.showMessageDialog(this, "Select a transaction row to edit.");
            return;
        }
        BudgetTransaction original = displayedTransactions.get(table.convertRowIndexToModel(row));
        JComboBox<String> type = new JComboBox<>(new String[]{"INCOME", "EXPENSE"});
        budgetlyDB.ui.UiLabels.install(type);
        type.setSelectedItem(original.getTransactionType());
        JComboBox<FinancialAccount> account = new JComboBox<>();
        try {
            for (FinancialAccount item : budgetService.getUserAccounts()) {
                account.addItem(item);
                if (item.getAccountId() == original.getAccountId()) account.setSelectedItem(item);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to load accounts: " + ex.getMessage());
            return;
        }
        JTextField category = new JTextField(original.getCategory());
        JTextField amount = new JTextField(original.getAmount().toPlainString());
        JTextField description = new JTextField(original.getDescription());
        JTextField date = new JTextField(original.getTransactionDate().format(DATE_FMT));
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Type")); form.add(type);
        form.add(new JLabel("Account")); form.add(account);
        form.add(new JLabel("Category")); form.add(category);
        form.add(new JLabel("Amount")); form.add(amount);
        form.add(new JLabel("Description")); form.add(description);
        form.add(new JLabel("Date (YYYY-MM-DD)")); form.add(date);
        if (JOptionPane.showConfirmDialog(this, form, "Edit Transaction #" + original.getTransactionId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            BigDecimal revisedAmount = new BigDecimal(amount.getText().trim().replace(",", ""));
            LocalDate revisedDate = LocalDate.parse(date.getText().trim(), DATE_FMT);
            FinancialAccount chosen = (FinancialAccount) account.getSelectedItem();
            if (chosen == null || !budgetService.updateTransaction(original.getTransactionId(),
                    chosen.getAccountId(), String.valueOf(type.getSelectedItem()), category.getText(),
                    revisedAmount, description.getText(), revisedDate)) {
                throw new IllegalArgumentException("Transaction or account is unavailable.");
            }
            loadTransactionsAsync();
            if (onDataChanged != null) onDataChanged.run();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Unable to update transaction: " + ex.getMessage(),
                    "Edit Transaction", JOptionPane.ERROR_MESSAGE);
        }
    }
}
