package budgetlyDB.ui;

import budgetlyDB.AdminDAO;
import budgetlyDB.DatabaseConnection;
import budgetlyDB.DatabaseManager;
import budgetlyDB.SessionManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/**
 * Administrator Database Management & Monitoring window using Java Swing (JFrame).
 * Provides connection status, metadata info, safe read-only table browsing, and error monitoring.
 */
public class DatabaseManagementFrame extends JFrame {

    private final AdminDAO adminDAO = new AdminDAO();
    private final DatabaseManager dbManager = new DatabaseManager();
    private final SimpleDateFormat tsFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private JLabel statusValueLbl;
    private JLabel engineValueLbl;
    private JLabel urlValueLbl;
    private JLabel poolValueLbl;

    private DefaultListModel<String> tableListModel;
    private JList<String> tableList;
    private DefaultTableModel recordTableModel;
    private JTable recordTable;
    private JLabel tableSummaryLabel;

    private DefaultTableModel errorTableModel;

    public DatabaseManagementFrame() {
        super("Budgetly Admin - Database Management");
        SessionManager.requireAdmin();
        BrandTheme.applyAppIcons(this);
        setSize(1100, 690);
        setMinimumSize(new Dimension(920, 580));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(createHeaderBar(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);

        refreshAllAsync();
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BrandTheme.SIDEBAR_GREEN);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        ImageIcon icon = BrandTheme.getMascotBadgeIcon(34);
        if (icon != null) {
            left.add(new JLabel(icon));
        }
        JLabel title = new JLabel("Apache Derby Database Management");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        left.add(title);

        JButton closeBtn = BrandTheme.createSecondaryButton("Close");
        closeBtn.addActionListener(e -> dispose());

        header.add(left, BorderLayout.WEST);
        header.add(closeBtn, BorderLayout.EAST);
        return header;
    }

    private JPanel createBody() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BrandTheme.CONTENT_BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        // Top KPI cards
        JPanel infoCards = new JPanel(new GridLayout(1, 4, 14, 0));
        infoCards.setOpaque(false);
        statusValueLbl = new JLabel("Checking...");
        engineValueLbl = new JLabel("-");
        urlValueLbl = new JLabel("-");
        poolValueLbl = new JLabel("-");

        infoCards.add(createInfoCard("Connection Status", statusValueLbl));
        infoCards.add(createInfoCard("Database Engine", engineValueLbl));
        infoCards.add(createInfoCard("Derby Endpoint & Schema", urlValueLbl));
        infoCards.add(createInfoCard("Connection Pool", poolValueLbl));

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(BrandTheme.FONT_BODY_BOLD);
        tabs.addTab("Tables & Records (Read-Only)", createTablesTab());
        tabs.addTab("Database Errors & Maintenance", createErrorsTab());

        root.add(infoCards, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel createInfoCard(String title, JLabel valLabel) {
        JPanel card = BrandTheme.createCardPanel();
        card.setLayout(new GridLayout(2, 1, 0, 6));
        JLabel t = new JLabel(title);
        t.setFont(BrandTheme.FONT_SMALL);
        t.setForeground(BrandTheme.TEXT_MUTED);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        valLabel.setForeground(BrandTheme.EMERALD);
        card.add(t);
        card.add(valLabel);
        return card;
    }

    private JPanel createTablesTab() {
        JPanel tab = new JPanel(new BorderLayout(12, 12));
        tab.setBackground(BrandTheme.CONTENT_BG);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));

        tableListModel = new DefaultListModel<>();
        tableList = new JList<>(tableListModel);
        tableList.setFont(BrandTheme.FONT_BODY_BOLD);
        tableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableList.setFixedCellHeight(36);
        tableList.setSelectionBackground(BrandTheme.SIDEBAR_GREEN);
        tableList.setSelectionForeground(Color.WHITE);
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = tableList.getSelectedValue();
                if (sel != null) {
                    loadTablePreviewAsync(sel);
                }
            }
        });

        JPanel leftPanel = BrandTheme.createCardPanel();
        leftPanel.setLayout(new BorderLayout(0, 8));
        leftPanel.setPreferredSize(new Dimension(240, 0));
        JLabel listTitle = new JLabel("Schema Tables");
        listTitle.setFont(BrandTheme.FONT_SECTION);
        leftPanel.add(listTitle, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(tableList), BorderLayout.CENTER);

        JPanel rightPanel = BrandTheme.createCardPanel();
        rightPanel.setLayout(new BorderLayout(0, 10));

        JPanel topRight = new JPanel(new BorderLayout());
        topRight.setOpaque(false);
        tableSummaryLabel = new JLabel("Select a table to inspect up to 200 records.");
        tableSummaryLabel.setFont(BrandTheme.FONT_BODY_BOLD);
        JButton refreshBtn = BrandTheme.createPrimaryButton("Refresh Database Info");
        refreshBtn.addActionListener(e -> refreshAllAsync());
        topRight.add(tableSummaryLabel, BorderLayout.WEST);
        topRight.add(refreshBtn, BorderLayout.EAST);

        recordTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recordTable = new JTable(recordTableModel);
        BrandTheme.styleTable(recordTable);
        recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        rightPanel.add(topRight, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(recordTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setDividerLocation(245);
        split.setBorder(null);
        tab.add(split, BorderLayout.CENTER);
        return tab;
    }

    private JPanel createErrorsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setBackground(BrandTheme.CONTENT_BG);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 8, 8, 8));

        JPanel card = BrandTheme.createCardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        JButton refreshErrBtn = BrandTheme.createPrimaryButton("Refresh Monitor");
        refreshErrBtn.addActionListener(e -> refreshAllAsync());

        JButton verifyBtn = BrandTheme.createSecondaryButton("Verify Schema Integrity");
        verifyBtn.addActionListener(e -> verifySchema());

        JButton clearErrBtn = BrandTheme.createDangerButton("Clear Error Log");
        clearErrBtn.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                    "Clear the recorded database error monitor log?",
                    "Confirm Clear", JOptionPane.YES_NO_OPTION);
            if (c == JOptionPane.YES_OPTION) {
                dbManager.clearErrorLog();
                refreshAllAsync();
            }
        });

        toolbar.add(refreshErrBtn);
        toolbar.add(verifyBtn);
        toolbar.add(clearErrBtn);

        errorTableModel = new DefaultTableModel(new String[]{"Timestamp", "Context", "SQLState", "Message"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable errTable = new JTable(errorTableModel);
        BrandTheme.styleTable(errTable);
        errTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        errTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        errTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        errTable.getColumnModel().getColumn(3).setPreferredWidth(540);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(new JScrollPane(errTable), BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void refreshAllAsync() {
        SessionManager.requireAdmin();
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private DatabaseManager.DatabaseStatusInfo status;
            private List<String> tables;
            private List<DatabaseConnection.DatabaseErrorRecord> errors;

            @Override
            protected Void doInBackground() throws Exception {
                status = dbManager.getDatabaseStatus();
                tables = adminDAO.listApplicationTables();
                errors = dbManager.getRecentErrors();
                return null;
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    get();
                    if (status.isConnected()) {
                        statusValueLbl.setText("CONNECTED (" + status.getTableCount() + " tables)");
                        statusValueLbl.setForeground(BrandTheme.EMERALD);
                    } else {
                        statusValueLbl.setText("DISCONNECTED");
                        statusValueLbl.setForeground(BrandTheme.DANGER_RED);
                    }
                    engineValueLbl.setText(status.getProductName() + " " + status.getProductVersion());
                    urlValueLbl.setText("<html>" + status.getJdbcUrl() + "<br>Schema: " + status.getSchemaName()
                            + " (User: " + status.getDatabaseUser() + ")</html>");
                    poolValueLbl.setText("Idle: " + status.getPooledConnections()
                            + " / Open: " + status.getTotalCreatedConnections());

                    String prevSelected = tableList.getSelectedValue();
                    tableListModel.clear();
                    for (String t : tables) {
                        tableListModel.addElement(t);
                    }
                    if (prevSelected != null && tables.contains(prevSelected)) {
                        tableList.setSelectedValue(prevSelected, true);
                    } else if (!tables.isEmpty()) {
                        tableList.setSelectedIndex(0);
                    }

                    errorTableModel.setRowCount(0);
                    for (int i = errors.size() - 1; i >= 0; i--) {
                        DatabaseConnection.DatabaseErrorRecord rec = errors.get(i);
                        errorTableModel.addRow(new Object[]{
                                tsFormat.format(rec.getTimestamp()),
                                rec.getContext(),
                                rec.getSqlState(),
                                rec.getMessage()
                        });
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(DatabaseManagementFrame.this,
                            "Error refreshing database metadata: " + ex.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void loadTablePreviewAsync(String tableName) {
        tableSummaryLabel.setText("Loading " + tableName + "...");
        SwingWorker<AdminDAO.TablePreview, Void> worker = new SwingWorker<>() {
            private int totalRows;

            @Override
            protected AdminDAO.TablePreview doInBackground() throws Exception {
                totalRows = adminDAO.getTableRowCount(tableName);
                return adminDAO.previewTable(tableName, 200);
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    AdminDAO.TablePreview preview = get();
                    recordTableModel.setDataVector(
                            preview.getRows().toArray(new Object[0][]),
                            preview.getColumns().toArray()
                    );
                    for (int i = 0; i < recordTable.getColumnCount(); i++) {
                        recordTable.getColumnModel().getColumn(i).setPreferredWidth(150);
                    }
                    tableSummaryLabel.setText("Table: " + tableName + " | Total Rows: " + totalRows
                            + " (Showing " + preview.getRows().size() + ")");
                } catch (Exception ex) {
                    tableSummaryLabel.setText("Error reading table " + tableName);
                }
            }
        };
        worker.execute();
    }

    private void verifySchema() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Run schema integrity check to verify all required Derby tables and admin account exist?",
                "Verify Schema", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            String msg = dbManager.verifySchemaIntegrity();
            refreshAllAsync();
            JOptionPane.showMessageDialog(this, msg, "Schema Verified", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Schema check failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
