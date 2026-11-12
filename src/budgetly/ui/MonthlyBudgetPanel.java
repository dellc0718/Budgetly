package budgetly.ui;

import budgetlyDB.BudgetService;
import budgetlyDB.SessionManager;
import budgetlyDB.model.MonthlyBudget;
import budgetlyDB.ui.BrandTheme;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Screen for setting and reviewing monthly overall and category spending limits.
 */
public class MonthlyBudgetPanel extends JPanel {

    private static final String[] BUDGET_CATEGORIES = {
        "OVERALL", "Food", "Transport", "Bills", "Shopping", "Health", "Education", "Entertainment", "Other"
    };

    private final BudgetService budgetService = new BudgetService();
    private final Runnable onDataChanged;

    private JLabel headingLabel;
    private JLabel subLabel;
    private JPanel formCard;
    private JPanel tableCard;

    private JComboBox<Integer> monthCombo;
    private JSpinner yearSpinner;
    private JComboBox<String> categoryCombo;
    private JTextField limitField;
    private JButton saveButton;

    private DefaultTableModel tableModel;
    private JTable budgetTable;

    public MonthlyBudgetPanel(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
        setLayout(new BorderLayout(0, 18));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel header = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        header.setOpaque(false);
        headingLabel = new JLabel("Monthly Budget");
        headingLabel.setFont(BrandTheme.FONT_TITLE);
        subLabel = new JLabel("Set and review your overall or per-category monthly spending limits.");
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

        YearMonth now = YearMonth.now();
        formCard = BrandTheme.createCardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        monthCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12});
        monthCombo.setSelectedItem(now.getMonthValue());
        monthCombo.addActionListener(e -> refreshData());

        yearSpinner = new JSpinner(new SpinnerNumberModel(now.getYear(), 2020, 2100, 1));
        yearSpinner.addChangeListener(e -> refreshData());

        categoryCombo = new JComboBox<>(BUDGET_CATEGORIES);
        limitField = new JTextField("15000.00", 12);
        saveButton = BrandTheme.createPrimaryButton("Save Monthly Limit");
        saveButton.addActionListener(e -> saveLimit());

        gbc.gridy = 0;
        gbc.gridx = 0; gbc.weightx = 0.15; formCard.add(new JLabel("Month"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.15; formCard.add(new JLabel("Year"), gbc);
        gbc.gridx = 2; gbc.weightx = 0.25; formCard.add(new JLabel("Scope / Category"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.25; formCard.add(new JLabel("Spending Limit"), gbc);
        gbc.gridx = 4; gbc.weightx = 0.20; formCard.add(new JLabel(" "), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0; formCard.add(monthCombo, gbc);
        gbc.gridx = 1; formCard.add(yearSpinner, gbc);
        gbc.gridx = 2; formCard.add(categoryCombo, gbc);
        gbc.gridx = 3; formCard.add(limitField, gbc);
        gbc.gridx = 4; formCard.add(saveButton, gbc);

        tableCard = BrandTheme.createCardPanel();
        tableCard.setLayout(new BorderLayout(0, 10));

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Period", "Category", "Monthly Limit", "Spent", "Remaining", "Usage", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        budgetTable = new JTable(tableModel);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        JButton deleteBtn = BrandTheme.createDangerButton("Delete Selected Budget");
        deleteBtn.addActionListener(e -> deleteSelectedBudget());
        JButton refreshBtn = BrandTheme.createSecondaryButton("Refresh");
        refreshBtn.addActionListener(e -> refreshData());
        btnRow.add(deleteBtn);
        btnRow.add(refreshBtn);

        tableCard.add(new JScrollPane(budgetTable), BorderLayout.CENTER);
        tableCard.add(btnRow, BorderLayout.SOUTH);

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
        BrandTheme.styleTable(budgetTable);
    }

    public final void refreshData() {
        applyTheme();
        int month = (Integer) monthCombo.getSelectedItem();
        int year = (Integer) yearSpinner.getValue();

        SwingWorker<List<MonthlyBudget>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<MonthlyBudget> doInBackground() throws Exception {
                return budgetService.getMonthlyBudgets(year, month);
            }

            @Override
            protected void done() {
                if (!SessionManager.isLoggedIn()) {
                    return;
                }
                try {
                    List<MonthlyBudget> list = get();
                    int threshold = SessionManager.getCurrentSettings() != null
                            ? SessionManager.getCurrentSettings().getDefaultBudgetAlertPercent()
                            : 80;
                    tableModel.setRowCount(0);
                    for (MonthlyBudget b : list) {
                        BigDecimal limit = b.getLimitAmount();
                        BigDecimal spent = b.getSpentAmount();
                        BigDecimal rem = limit.subtract(spent);
                        BigDecimal pct = limit.compareTo(BigDecimal.ZERO) > 0
                                ? spent.multiply(BigDecimal.valueOf(100)).divide(limit, 1, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;
                        String status = spent.compareTo(limit) > 0
                                ? "Over Budget"
                                : (pct.compareTo(BigDecimal.valueOf(threshold)) >= 0 ? "Warning" : "On Track");

                        tableModel.addRow(new Object[]{
                                b.getBudgetId(),
                                b.getBudgetYear() + "-" + String.format("%02d", b.getBudgetMonth()),
                                budgetlyDB.ui.UiLabels.label(b.getCategory()),
                                BudgetService.formatMoney(limit),
                                BudgetService.formatMoney(spent),
                                BudgetService.formatMoney(rem),
                                pct + "%",
                                status
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MonthlyBudgetPanel.this,
                            "Error loading budgets: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void saveLimit() {
        int month = (Integer) monthCombo.getSelectedItem();
        int year = (Integer) yearSpinner.getValue();
        String cat = String.valueOf(categoryCombo.getSelectedItem());
        BigDecimal limit;
        try {
            limit = new BigDecimal(limitField.getText().trim().replace(",", ""));
            if (limit.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Enter a positive spending limit.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveButton.setEnabled(false);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                budgetService.saveMonthlyBudget(year, month, cat, limit);
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                try {
                    get();
                    refreshData();
                    if (onDataChanged != null) {
                        onDataChanged.run();
                    }
                    JOptionPane.showMessageDialog(MonthlyBudgetPanel.this,
                            "Monthly budget saved for " + cat + ".",
                            "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MonthlyBudgetPanel.this,
                            "Could not save budget: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void deleteSelectedBudget() {
        int row = budgetTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a budget entry to delete.",
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int id = (Integer) tableModel.getValueAt(row, 0);
        int c = JOptionPane.showConfirmDialog(this,
                "Delete the selected monthly budget limit?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            budgetService.deleteMonthlyBudget(id);
            refreshData();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to delete budget: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
