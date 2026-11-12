package budgetly.ui;

import budgetlyDB.BudgetService;
import budgetlyDB.SessionManager;
import budgetlyDB.model.BudgetTransaction;
import budgetlyDB.model.DashboardSummary;
import budgetlyDB.ui.AdminDashboard;
import budgetlyDB.ui.BrandTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JProgressBar;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class DashboardPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final BudgetService budgetService = new BudgetService();

    private JLabel headingLabel;
    private JLabel welcomeUserLabel;
    private JLabel incomeLabel;
    private JLabel expensesLabel;
    private JLabel balanceLabel;
    private JLabel alertLabel;
    private JLabel previewLabel;
    private FinancialChartsPanel chartsPanel;
    private JProgressBar budgetProgress;
    private JLabel budgetProgressLabel;

    private JPanel alertPanel;
    private JPanel recentCard;
    private JPanel incomeCard;
    private JPanel expensesCard;
    private JPanel balanceCard;
    private JLabel incomeTitleLabel;
    private JLabel expensesTitleLabel;
    private JLabel balanceTitleLabel;

    private DefaultTableModel recentModel;
    private JTable recentTable;

    public DashboardPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(BrandTheme.getContentBackground());
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);

        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 0, 2));
        titleGroup.setOpaque(false);
        headingLabel = new JLabel("Dashboard");
        headingLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        welcomeUserLabel = new JLabel("Signed in as " + SessionManager.getDisplayName());
        welcomeUserLabel.setFont(BrandTheme.FONT_SUBTITLE);
        titleGroup.add(headingLabel);
        titleGroup.add(welcomeUserLabel);
        topHeader.add(titleGroup, BorderLayout.WEST);

        if (SessionManager.isAdmin()) {
            JPanel adminActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            adminActions.setOpaque(false);
            JButton adminBtn = BrandTheme.createPrimaryButton("Open Admin Dashboard");
            adminBtn.addActionListener(e -> new AdminDashboard().setVisible(true));
            adminActions.add(adminBtn);
            topHeader.add(adminActions, BorderLayout.EAST);
        }

        add(topHeader, BorderLayout.NORTH);

        incomeLabel = new JLabel("PHP 0.00");
        expensesLabel = new JLabel("PHP 0.00");
        balanceLabel = new JLabel("PHP 0.00");

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        summaryPanel.setOpaque(false);

        incomeTitleLabel = new JLabel("Income This Month");
        expensesTitleLabel = new JLabel("Expenses This Month");
        balanceTitleLabel = new JLabel("Total Account Balance");

        incomeCard = createSummaryCard(incomeTitleLabel, incomeLabel);
        expensesCard = createSummaryCard(expensesTitleLabel, expensesLabel);
        balanceCard = createSummaryCard(balanceTitleLabel, balanceLabel);

        summaryPanel.add(incomeCard);
        summaryPanel.add(expensesCard);
        summaryPanel.add(balanceCard);

        alertLabel = new JLabel("No monthly budget set yet.");
        alertLabel.setFont(BrandTheme.FONT_BODY);

        alertPanel = new JPanel(new BorderLayout());
        alertPanel.setBackground(BrandTheme.getCardBackground());
        alertPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Budget Alerts"),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        alertPanel.add(alertLabel, BorderLayout.CENTER);

        budgetProgress = new JProgressBar(0, 100);
        budgetProgress.setStringPainted(true);
        budgetProgressLabel = new JLabel("Monthly budget progress");
        budgetProgressLabel.setFont(BrandTheme.FONT_BODY);
        JPanel progressCard = BrandTheme.createCardPanel();
        progressCard.setLayout(new BorderLayout(0, 8));
        progressCard.add(budgetProgressLabel, BorderLayout.NORTH);
        progressCard.add(budgetProgress, BorderLayout.CENTER);
        progressCard.setPreferredSize(new Dimension(700, 82));
        progressCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));

        chartsPanel = new FinancialChartsPanel();
        chartsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));
        chartsPanel.setBorder(BrandTheme.createCardBorder());

        recentCard = BrandTheme.createCardPanel();
        recentCard.setLayout(new BorderLayout(0, 10));
        JLabel recentTitle = new JLabel("Recent Transactions");
        recentTitle.setFont(BrandTheme.FONT_SECTION);
        recentTitle.setForeground(BrandTheme.getTextPrimary());
        recentModel = new DefaultTableModel(
                new String[]{"Date", "Type", "Account", "Category", "Description", "Amount"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        recentTable = new JTable(recentModel);
        recentCard.add(recentTitle, BorderLayout.NORTH);
        recentCard.add(new JScrollPane(recentTable), BorderLayout.CENTER);

        JPanel bodyPanel = new JPanel();
        bodyPanel.setLayout(new BoxLayout(bodyPanel, BoxLayout.Y_AXIS));
        bodyPanel.setOpaque(false);
        summaryPanel.setPreferredSize(new Dimension(700, 130));
        summaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        alertPanel.setPreferredSize(new Dimension(700, 80));
        alertPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        recentCard.setPreferredSize(new Dimension(700, 260));
        recentCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        bodyPanel.add(summaryPanel);
        bodyPanel.add(Box.createVerticalStrut(14));
        bodyPanel.add(alertPanel);
        bodyPanel.add(Box.createVerticalStrut(14));
        bodyPanel.add(progressCard);
        bodyPanel.add(Box.createVerticalStrut(14));
        bodyPanel.add(chartsPanel);
        bodyPanel.add(Box.createVerticalStrut(14));
        bodyPanel.add(recentCard);
        JScrollPane bodyScroll = new JScrollPane(bodyPanel);
        bodyScroll.setBorder(null);
        bodyScroll.getVerticalScrollBar().setUnitIncrement(20);
        add(bodyScroll, BorderLayout.CENTER);
        SwingUtilities.invokeLater(() -> bodyScroll.getViewport().setViewPosition(new Point(0, 0)));

        previewLabel = new JLabel("Group 5 | Budget Tracking System.");
        previewLabel.setForeground(Color.DARK_GRAY);
        add(previewLabel, BorderLayout.SOUTH);

        applyTheme();
        refreshData();
    }

    private JPanel createSummaryCard(JLabel titleLabel, JLabel valueLabel) {
        JPanel card = new JPanel(new GridLayout(2, 1, 0, 10));
        card.setBackground(BrandTheme.getCardBackground());
        card.setBorder(BrandTheme.createCardBorder());

        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        valueLabel.setForeground(BrandTheme.getAccentEmerald());

        card.add(titleLabel);
        card.add(valueLabel);
        return card;
    }

    public final void applyTheme() {
        setBackground(BrandTheme.getContentBackground());
        headingLabel.setForeground(BrandTheme.getTextPrimary());
        welcomeUserLabel.setForeground(BrandTheme.getTextMuted());
        previewLabel.setForeground(BrandTheme.getTextMuted());

        Color cardBg = BrandTheme.getCardBackground();
        incomeCard.setBackground(cardBg);
        incomeCard.setBorder(BrandTheme.createCardBorder());
        expensesCard.setBackground(cardBg);
        expensesCard.setBorder(BrandTheme.createCardBorder());
        balanceCard.setBackground(cardBg);
        balanceCard.setBorder(BrandTheme.createCardBorder());

        incomeTitleLabel.setForeground(BrandTheme.getTextMuted());
        expensesTitleLabel.setForeground(BrandTheme.getTextMuted());
        balanceTitleLabel.setForeground(BrandTheme.getTextMuted());

        incomeLabel.setForeground(BrandTheme.getAccentEmerald());
        expensesLabel.setForeground(BrandTheme.DANGER_RED);
        balanceLabel.setForeground(BrandTheme.getAccentEmerald());

        alertPanel.setBackground(cardBg);
        recentCard.setBackground(cardBg);
        recentCard.setBorder(BrandTheme.createCardBorder());
        chartsPanel.setBackground(cardBg);
        chartsPanel.setBorder(BrandTheme.createCardBorder());
        budgetProgressLabel.setForeground(BrandTheme.getTextPrimary());
        BrandTheme.styleTable(recentTable);
    }

    public final void refreshData() {
        applyTheme();
        welcomeUserLabel.setText("Signed in as " + SessionManager.getDisplayName()
                + (SessionManager.isAdmin() ? " (Administrator)" : ""));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private DashboardSummary summary;
            private List<BudgetTransaction> recent;
            private Map<YearMonth, BigDecimal[]> trend;
            private Map<String, BigDecimal> categories;

            @Override
            protected Void doInBackground() throws Exception {
                summary = budgetService.getDashboardSummary();
                recent = budgetService.getRecentTransactions(8);
                trend = budgetService.getMonthlyTrend();
                YearMonth month = YearMonth.now();
                categories = budgetService.getMonthlyExpensesByCategory(month.getYear(), month.getMonthValue());
                return null;
            }

            @Override
            protected void done() {
                if (!SessionManager.isLoggedIn()) {
                    return;
                }
                try {
                    get();
                    incomeLabel.setText(BudgetService.formatMoney(summary.getMonthlyIncome()));
                    expensesLabel.setText(BudgetService.formatMoney(summary.getMonthlyExpenses()));
                    balanceLabel.setText(BudgetService.formatMoney(summary.getTotalAccountBalance()));
                    alertLabel.setText(summary.getAlertMessage());
                    chartsPanel.setData(trend, categories);
                    BigDecimal limit = summary.getOverallBudgetLimit();
                    if (limit == null || limit.signum() <= 0) {
                        budgetProgress.setValue(0);
                        budgetProgress.setString("No budget set");
                        budgetProgressLabel.setText("Set a monthly budget to track progress");
                    } else {
                        int percent = summary.getMonthlyExpenses().multiply(BigDecimal.valueOf(100))
                                .divide(limit, 0, RoundingMode.HALF_UP).intValue();
                        budgetProgress.setValue(Math.max(0, Math.min(100, percent)));
                        budgetProgress.setString(percent + "% used");
                        budgetProgress.setForeground(percent > 100 ? BrandTheme.DANGER_RED
                                : percent >= 80 ? BrandTheme.WARN_AMBER : BrandTheme.SUCCESS_GREEN);
                        budgetProgressLabel.setText("Spent " + BudgetService.formatMoney(summary.getMonthlyExpenses())
                                + " of " + BudgetService.formatMoney(limit) + " • Remaining "
                                + BudgetService.formatMoney(limit.subtract(summary.getMonthlyExpenses())));
                    }

                    String sev = summary.getAlertSeverity();
                    if ("EXCEEDED".equals(sev)) {
                        alertLabel.setForeground(BrandTheme.DANGER_RED);
                    } else if ("WARNING".equals(sev)) {
                        alertLabel.setForeground(BrandTheme.WARN_AMBER);
                    } else if ("OK".equals(sev)) {
                        alertLabel.setForeground(BrandTheme.getAccentEmerald());
                    } else {
                        alertLabel.setForeground(BrandTheme.getTextPrimary());
                    }

                    recentModel.setRowCount(0);
                    for (BudgetTransaction t : recent) {
                        recentModel.addRow(new Object[]{
                                t.getTransactionDate().format(DATE_FMT),
                                budgetlyDB.ui.UiLabels.label(t.getTransactionType()),
                                t.getAccountName(),
                                t.getCategory(),
                                t.getDescription(),
                                (t.isIncome() ? "+ " : "- ") + BudgetService.formatMoney(t.getAmount())
                        });
                    }
                } catch (Exception ex) {
                    alertLabel.setForeground(BrandTheme.DANGER_RED);
                    alertLabel.setText("Unable to load dashboard summary: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
