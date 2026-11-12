package budgetly.ui;

import budgetlyDB.BudgetService;
import budgetlyDB.ui.BrandTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.FontMetrics;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JPanel;

/** Native Java2D charts, drawn only from the current user's database aggregates. */
public final class FinancialChartsPanel extends JPanel {
    private static final Color INCOME = new Color(34, 132, 82);
    private static final Color EXPENSE = new Color(199, 70, 67);
    private static final Color[] SLICES = {INCOME, EXPENSE, new Color(59, 120, 174),
        new Color(219, 151, 55), new Color(126, 91, 171), new Color(55, 165, 168)};
    private Map<YearMonth, BigDecimal[]> trend = Collections.emptyMap();
    private Map<String, BigDecimal> categories = Collections.emptyMap();

    public FinancialChartsPanel() {
        setPreferredSize(new Dimension(700, 340));
        setBackground(BrandTheme.getCardBackground());
        setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    public void setData(Map<YearMonth, BigDecimal[]> monthly, Map<String, BigDecimal> breakdown) {
        trend = new LinkedHashMap<>(monthly);
        categories = new LinkedHashMap<>(breakdown);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() / 3;
            g.setColor(BrandTheme.getCardBorderColor());
            g.drawLine(w, 16, w, getHeight() - 16);
            g.drawLine(w * 2, 16, w * 2, getHeight() - 16);
            drawComparison(g, 0, w);
            drawTrend(g, w, w);
            drawBreakdown(g, w * 2, getWidth() - w * 2);
        } finally {
            g.dispose();
        }
    }

    private void title(Graphics2D g, String text, int x) {
        g.setColor(BrandTheme.getTextPrimary());
        g.setFont(getFont().deriveFont(Font.BOLD, 15f));
        g.drawString(text, x + 16, 28);
        g.setFont(getFont());
    }

    private double maxValue() {
        double max = 1;
        for (BigDecimal[] pair : trend.values()) {
            max = Math.max(max, Math.max(pair[0].doubleValue(), pair[1].doubleValue()));
        }
        return max;
    }

    private void drawComparison(Graphics2D g, int x, int w) {
        title(g, "Income vs. Expenses", x);
        int left = x + 22, top = 78, bottom = getHeight() - 62, right = x + w - 18;
        if (right <= left || bottom <= top) return;
        axis(g, left, top, right, bottom, maxValue());
        int i = 0, count = Math.max(1, trend.size());
        for (Map.Entry<YearMonth, BigDecimal[]> entry : trend.entrySet()) {
            int center = left + (right - left) * (2 * i + 1) / (2 * count);
            int width = Math.max(5, (right - left) / (count * 5));
            int inH = (int) ((bottom - top) * entry.getValue()[0].doubleValue() / maxValue());
            int outH = (int) ((bottom - top) * entry.getValue()[1].doubleValue() / maxValue());
            g.setColor(INCOME); g.fillRoundRect(center - width - 2, bottom - inH, width, inH, 3, 3);
            g.setColor(EXPENSE); g.fillRoundRect(center + 2, bottom - outH, width, outH, 3, 3);
            month(g, entry.getKey(), center, bottom + 20);
            i++;
        }
        legend(g, x + 22, getHeight() - 18);
    }

    private void drawTrend(Graphics2D g, int x, int w) {
        title(g, "Six Month Trend", x);
        int left = x + 22, top = 78, bottom = getHeight() - 62, right = x + w - 18;
        if (right <= left || bottom <= top) return;
        axis(g, left, top, right, bottom, maxValue());
        for (int series = 0; series < 2; series++) {
            g.setColor(series == 0 ? INCOME : EXPENSE);
            g.setStroke(new BasicStroke(2.5f));
            int px = -1, py = -1, i = 0, count = Math.max(1, trend.size());
            for (Map.Entry<YearMonth, BigDecimal[]> entry : trend.entrySet()) {
                int cx = left + (right - left) * i / Math.max(1, count - 1);
                int cy = bottom - (int) ((bottom - top) * entry.getValue()[series].doubleValue() / maxValue());
                if (px >= 0) g.drawLine(px, py, cx, cy);
                g.fillOval(cx - 3, cy - 3, 7, 7);
                if (series == 0) month(g, entry.getKey(), cx, bottom + 20);
                px = cx; py = cy; i++;
            }
        }
        legend(g, x + 22, getHeight() - 18);
    }

    private void drawBreakdown(Graphics2D g, int x, int w) {
        title(g, "Expenses by Category", x);
        double total = categories.values().stream().mapToDouble(BigDecimal::doubleValue).sum();
        if (total <= 0) {
            g.setColor(BrandTheme.getTextMuted());
            g.drawString("No expenses this month", x + 20, getHeight() / 2);
            return;
        }
        int diameter = Math.min(142, Math.max(70, w / 2 - 16));
        int centerX = x + w / 2, centerY = 146;
        int angle = 90, i = 0;
        for (Map.Entry<String, BigDecimal> entry : categories.entrySet()) {
            int arc = (int) Math.round(entry.getValue().doubleValue() * 360 / total);
            if (i == categories.size() - 1) arc = 450 - angle;
            g.setColor(SLICES[i % SLICES.length]);
            g.fillArc(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter, angle, arc);
            angle += arc; i++;
        }
        g.setColor(BrandTheme.getCardBackground());
        g.fillOval(centerX - diameter / 4, centerY - diameter / 4, diameter / 2, diameter / 2);
        g.setColor(BrandTheme.getTextPrimary());
        String totalText = "Total: " + BudgetService.formatMoney(BigDecimal.valueOf(total));
        g.drawString(totalText, centerX - g.getFontMetrics().stringWidth(totalText) / 2, 242);
        int legendColumnWidth = Math.min(160, w / 2 - 12);
        int legendStart = centerX - (categories.size() == 1 ? legendColumnWidth : legendColumnWidth * 2) / 2;
        int line = 0;
        for (Map.Entry<String, BigDecimal> entry : categories.entrySet()) {
            if (line >= 8) break;
            int column = line % 2;
            int lx = legendStart + column * legendColumnWidth;
            int ly = 267 + (line / 2) * 21;
            g.setColor(SLICES[line % SLICES.length]);
            g.fillRect(lx, ly - 9, 10, 10);
            String name = entry.getKey();
            String pct = String.format(" %.0f%%", entry.getValue().doubleValue() * 100 / total);
            int available = Math.max(30, legendColumnWidth - 25 - g.getFontMetrics().stringWidth(pct));
            while (name.length() > 1 && g.getFontMetrics().stringWidth(name) > available) {
                name = name.substring(0, name.length() - 1);
            }
            if (!name.equals(entry.getKey())) name += "…";
            g.setColor(BrandTheme.getTextPrimary());
            g.drawString(name + pct, lx + 15, ly);
            line++;
        }
    }

    private void axis(Graphics2D g, int left, int top, int right, int bottom, double max) {
        g.setColor(BrandTheme.getCardBorderColor());
        g.drawLine(left, bottom, right, bottom);
        g.drawLine(left, top, left, bottom);
        g.setColor(BrandTheme.getTextMuted());
        g.drawString(BudgetService.formatMoney(BigDecimal.valueOf(max)), left, top - 10);
        g.drawString("0", left + 3, bottom - 5);
    }

    private void month(Graphics2D g, YearMonth month, int x, int y) {
        g.setColor(BrandTheme.getTextMuted());
        String label = month.format(DateTimeFormatter.ofPattern("MMM"));
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(label, x - metrics.stringWidth(label) / 2, y);
    }

    private void legend(Graphics2D g, int x, int y) {
        g.setColor(INCOME); g.fillRect(x, y - 9, 10, 10);
        g.setColor(BrandTheme.getTextMuted()); g.drawString("Income", x + 15, y);
        g.setColor(EXPENSE); g.fillRect(x + 83, y - 9, 10, 10);
        g.setColor(BrandTheme.getTextMuted()); g.drawString("Expenses", x + 98, y);
    }
}
