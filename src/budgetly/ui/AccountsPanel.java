package budgetly.ui;

import budgetlyDB.BudgetService;
import budgetlyDB.SessionManager;
import budgetlyDB.model.FinancialAccount;
import budgetlyDB.ui.BrandTheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
 * Screen for managing the authenticated user's Cash, Bank, E-Wallet, and Other accounts.
 */
public class AccountsPanel extends JPanel {

    private final BudgetService budgetService = new BudgetService();
    private final Runnable onDataChanged;
    private final SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd");

    private JTextField nameField;
    private JComboBox<String> typeCombo;
    private JTextField initialBalanceField;
    private JTextField notesField;
    private JButton addButton;

    private DefaultTableModel tableModel;
    private JTable accountTable;
    private JLabel totalBalanceLabel;
    private JLabel headingLabel;
    private JLabel subLabel;
    private JPanel formCard;
    private JPanel tableCard;

    public AccountsPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        headingLabel = new JLabel("Accounts");
        headingLabel.setFont(BrandTheme.FONT_TITLE);
        subLabel = new JLabel("Manually track cash, bank, e-wallet, and other personal accounts.");
        subLabel.setFont(BrandTheme.FONT_SUBTITLE);
        totalBalanceLabel = new JLabel("Total Balance: PHP 0.00");
        totalBalanceLabel.setFont(BrandTheme.FONT_SECTION);

        JPanel titles = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        titles.setOpaque(false);
        titles.add(headingLabel);
        titles.add(subLabel);
        header.add(titles, BorderLayout.WEST);
        header.add(totalBalanceLabel, BorderLayout.EAST);

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

        nameField = new JTextField(14);
        nameField.setFont(BrandTheme.FONT_BODY);
        typeCombo = new JComboBox<>(new String[]{"CASH", "BANK", "EWALLET", "OTHER"});
        budgetlyDB.ui.UiLabels.install(typeCombo);
        typeCombo.setFont(BrandTheme.FONT_BODY);
        initialBalanceField = new JTextField("0.00", 10);
        initialBalanceField.setFont(BrandTheme.FONT_BODY);
        notesField = new JTextField(16);
        notesField.setFont(BrandTheme.FONT_BODY);
        addButton = BrandTheme.createPrimaryButton("+ Add Account");
        addButton.addActionListener(e -> addAccount());

        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0.25; formCard.add(new JLabel("Account Name"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.15; formCard.add(new JLabel("Type"), gbc);
        gbc.gridx = 2; gbc.weightx = 0.20; formCard.add(new JLabel("Initial Balance"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.30; formCard.add(new JLabel("Notes (Optional)"), gbc);
        gbc.gridx = 4; gbc.weightx = 0.10; formCard.add(new JLabel(" "), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0; formCard.add(nameField, gbc);
        gbc.gridx = 1; formCard.add(typeCombo, gbc);
        gbc.gridx = 2; formCard.add(initialBalanceField, gbc);
        gbc.gridx = 3; formCard.add(notesField, gbc);
        gbc.gridx = 4; formCard.add(addButton, gbc);

        tableCard = BrandTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Account Name", "Type", "Current Balance", "Notes", "Created"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        accountTable = new JTable(tableModel);
        budgetlyDB.ui.UiLabels.installTableColumn(accountTable, 2);
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        JButton editBtn = BrandTheme.createSecondaryButton("Edit Selected");
        editBtn.addActionListener(e -> editSelectedAccount());
        JButton deleteBtn = BrandTheme.createDangerButton("Delete Selected");
        deleteBtn.addActionListener(e -> deleteSelectedAccount());
        JButton refreshBtn = BrandTheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());

        btnRow.add(editBtn);
        btnRow.add(deleteBtn);
        btnRow.add(refreshBtn);

        tableCard.add(new JScrollPane(accountTable), BorderLayout.CENTER);
        tableCard.add(btnRow, BorderLayout.SOUTH);

        body.add(formCard, BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        return body;
    }

    public final void applyTheme() {
        setBackground(BrandTheme.getContentBackground());
        headingLabel.setForeground(BrandTheme.getTextPrimary());
        subLabel.setForeground(BrandTheme.getTextMuted());
        totalBalanceLabel.setForeground(BrandTheme.getAccentEmerald());
        formCard.setBackground(BrandTheme.getCardBackground());
        formCard.setBorder(BrandTheme.createCardBorder());
        tableCard.setBackground(BrandTheme.getCardBackground());
        tableCard.setBorder(BrandTheme.createCardBorder());
        BrandTheme.styleTable(accountTable);
    }

    public final void refreshData() {
        applyTheme();
        SwingWorker<List<FinancialAccount>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<FinancialAccount> doInBackground() throws Exception {
                return budgetService.getUserAccounts();
            }

            @Override
            protected void done() {
                if (!SessionManager.isLoggedIn()) {
                    return;
                }
                try {
                    List<FinancialAccount> accounts = get();
                    tableModel.setRowCount(0);
                    BigDecimal total = BigDecimal.ZERO;
                    for (FinancialAccount a : accounts) {
                        total = total.add(a.getBalance());
                        tableModel.addRow(new Object[]{
                                a.getAccountId(),
                                a.getAccountName(),
                                a.getAccountType(),
                                BudgetService.formatMoney(a.getBalance()),
                                a.getNotes(),
                                a.getDateCreated() == null ? "-" : dtFormat.format(a.getDateCreated())
                        });
                    }
                    totalBalanceLabel.setText("Total Balance: " + BudgetService.formatMoney(total));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AccountsPanel.this,
                            "Unable to load accounts: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void addAccount() {
        String name = nameField.getText().trim();
        String type = String.valueOf(typeCombo.getSelectedItem());
        String balText = initialBalanceField.getText().trim().replace(",", "");
        String notes = notesField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an account name.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        BigDecimal bal;
        try {
            bal = new BigDecimal(balText.isEmpty() ? "0.00" : balText);
            if (bal.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid non-negative initial balance.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        addButton.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                budgetService.createAccount(name, type, bal, notes);
                return null;
            }

            @Override
            protected void done() {
                addButton.setEnabled(true);
                try {
                    get();
                    nameField.setText("");
                    initialBalanceField.setText("0.00");
                    notesField.setText("");
                    refreshData();
                    if (onDataChanged != null) {
                        onDataChanged.run();
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AccountsPanel.this,
                            "Could not create account (ensure account name is unique): " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void editSelectedAccount() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an account to edit.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        JTextField nameTf = new JTextField(String.valueOf(tableModel.getValueAt(row, 1)));
        JComboBox<String> typeCb = new JComboBox<>(new String[]{"CASH", "BANK", "EWALLET", "OTHER"});
        budgetlyDB.ui.UiLabels.install(typeCb);
        typeCb.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 2)));
        JTextField notesTf = new JTextField(String.valueOf(tableModel.getValueAt(row, 4)));

        JPanel form = new JPanel(new java.awt.GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Account Name:")); form.add(nameTf);
        form.add(new JLabel("Account Type:")); form.add(typeCb);
        form.add(new JLabel("Notes:")); form.add(notesTf);

        int res = JOptionPane.showConfirmDialog(this, form, "Edit Account", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            budgetService.updateAccount(id, nameTf.getText(), String.valueOf(typeCb.getSelectedItem()), notesTf.getText());
            refreshData();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not update account: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedAccount() {
        int row = accountTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an account to delete.", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        String name = String.valueOf(tableModel.getValueAt(row, 1));
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete account '" + name + "'? All transactions associated with this account will also be deleted.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            budgetService.deleteAccount(id);
            refreshData();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not delete account: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
