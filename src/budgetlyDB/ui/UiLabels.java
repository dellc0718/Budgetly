package budgetlyDB.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/** Maps persisted codes to display text while leaving combo box values unchanged. */
public final class UiLabels {
    private static final Map<String, String> LABELS;
    static {
        Map<String, String> labels = new HashMap<>();
        labels.put("default_croc", "Default Crocodile");
        labels.put("emerald_badge", "Emerald Badge");
        labels.put("classic_initials", "Classic Initials");
        labels.put("EWALLET", "E-Wallet");
        labels.put("PHP", "PHP");
        labels.put("USD", "USD");
        labels.put("EUR", "EUR");
        labels.put("GBP", "GBP");
        labels.put("JPY", "JPY");
        LABELS = Collections.unmodifiableMap(labels);
    }

    private UiLabels() { }

    public static String label(String code) {
        if (code == null) return "";
        String known = LABELS.get(code);
        if (known != null) return known;
        if (!code.matches("[A-Z][A-Z_]*")) return code;
        StringBuilder result = new StringBuilder();
        for (String word : code.split("_")) {
            if (result.length() > 0) result.append(' ');
            result.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    public static void install(JComboBox<String> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list,
                    Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list,
                        value == null ? "" : label(value.toString()), index, selected, focus);
            }
        });
    }

    public static void installTableColumn(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                super.setValue(value == null ? "" : label(value.toString()));
            }
        });
    }
}
