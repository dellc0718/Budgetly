package budgetlyDB.ui;

import budgetlyDB.SessionManager;
import budgetlyDB.model.UserSettings;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;

/**
 * Shared visual theme, typography, logo loader, and Swing styling utilities
 * aligned with Budgetly's original forest-green and clean light-card aesthetic.
 */
public final class BrandTheme {

    public static final Color SIDEBAR_GREEN = new Color(30, 55, 45);
    public static final Color SIDEBAR_FOREST = new Color(20, 42, 34);
    public static final Color EMERALD = new Color(30, 100, 65);
    public static final Color EMERALD_LIGHT = new Color(232, 245, 238);
    public static final Color CREAM_ACCENT = new Color(252, 248, 224);

    public static final Color CONTENT_BG = new Color(245, 247, 250);
    public static final Color CARD_BG = Color.WHITE;
    public static final Color CARD_BORDER = new Color(220, 228, 224);
    public static final Color TEXT_DARK = new Color(26, 40, 34);
    public static final Color TEXT_MUTED = new Color(96, 112, 105);

    public static final Color DARK_CONTENT_BG = new Color(24, 32, 29);
    public static final Color DARK_CARD_BG = new Color(34, 46, 41);
    public static final Color DARK_BORDER = new Color(56, 74, 67);
    public static final Color DARK_TEXT = new Color(238, 244, 240);
    public static final Color DARK_TEXT_MUTED = new Color(172, 188, 180);

    public static final Color DANGER_RED = new Color(185, 45, 45);
    public static final Color WARN_AMBER = new Color(180, 110, 15);
    public static final Color SUCCESS_GREEN = new Color(30, 120, 72);

    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SECTION = new Font("SansSerif", Font.BOLD, 16);
    public static final Font FONT_KPI = new Font("SansSerif", Font.BOLD, 24);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 12);
    public static final Font FONT_MONO = new Font("Monospaced", Font.PLAIN, 13);

    private static volatile BufferedImage cachedLogo;
    private static volatile BufferedImage cachedIcon;

    private BrandTheme() {
    }

    public static boolean isDarkTheme() {
        UserSettings s = SessionManager.getCurrentSettings();
        return s != null && "DARK".equalsIgnoreCase(s.getTheme());
    }

    public static boolean isForestTheme() {
        UserSettings s = SessionManager.getCurrentSettings();
        return s != null && "FOREST".equalsIgnoreCase(s.getTheme());
    }

    public static Color getSidebarBackground() {
        return isForestTheme() ? SIDEBAR_FOREST : SIDEBAR_GREEN;
    }

    public static Color getContentBackground() {
        if (isDarkTheme()) {
            return DARK_CONTENT_BG;
        }
        if (isForestTheme()) {
            return new Color(237, 244, 240);
        }
        return CONTENT_BG;
    }

    public static Color getCardBackground() {
        return isDarkTheme() ? DARK_CARD_BG : CARD_BG;
    }

    public static Color getCardBorderColor() {
        return isDarkTheme() ? DARK_BORDER : CARD_BORDER;
    }

    public static Color getTextPrimary() {
        return isDarkTheme() ? DARK_TEXT : TEXT_DARK;
    }

    public static Color getTextMuted() {
        return isDarkTheme() ? DARK_TEXT_MUTED : TEXT_MUTED;
    }

    public static Color getAccentEmerald() {
        return isDarkTheme() ? new Color(92, 198, 140) : EMERALD;
    }

    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getCardBorderColor(), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        );
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(getCardBackground());
        panel.setBorder(createCardBorder());
        return panel;
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(SIDEBAR_GREEN);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY);
        btn.setBackground(Color.WHITE);
        btn.setForeground(SIDEBAR_GREEN);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SIDEBAR_GREEN, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        return btn;
    }

    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BODY_BOLD);
        btn.setBackground(DANGER_RED);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(9, 16, 9, 16));
        return btn;
    }

    public static void styleTable(JTable table) {
        table.setRowHeight(30);
        table.setFont(FONT_BODY);
        table.setForeground(getTextPrimary());
        table.setBackground(getCardBackground());
        table.setGridColor(getCardBorderColor());
        table.setSelectionBackground(isDarkTheme() ? new Color(46, 84, 68) : EMERALD_LIGHT);
        table.setSelectionForeground(getTextPrimary());
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBackground(SIDEBAR_GREEN);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(0, 34));
        header.setReorderingAllowed(false);
    }

    public static void applyAppIcons(JFrame frame) {
        if (frame == null) {
            return;
        }
        BufferedImage iconImg = getMascotIconImage();
        if (iconImg == null) {
            return;
        }
        int[] sizes = {16, 32, 64, 128};
        List<Image> icons = new ArrayList<>();
        for (int s : sizes) {
            icons.add(scaleImage(iconImg, s, s));
        }
        frame.setIconImages(icons);
    }

    public static ImageIcon getLogoImageIcon(int width, int height) {
        BufferedImage img = getFullLogoImage();
        if (img == null) {
            return null;
        }
        return new ImageIcon(scaleImage(img, width, height));
    }

    public static ImageIcon getMascotBadgeIcon(int size) {
        BufferedImage img = getMascotIconImage();
        if (img == null) {
            return null;
        }
        return new ImageIcon(scaleImage(img, size, size));
    }

    private static BufferedImage getFullLogoImage() {
        if (cachedLogo == null) {
            cachedLogo = loadResourceImage("/budgetly/resources/budgetly-logo.png");
        }
        return cachedLogo;
    }

    private static BufferedImage getMascotIconImage() {
        if (cachedIcon == null) {
            cachedIcon = loadResourceImage("/budgetly/resources/budgetly-icon.png");
            if (cachedIcon == null) {
                cachedIcon = getFullLogoImage();
            }
        }
        return cachedIcon;
    }

    private static BufferedImage loadResourceImage(String resourcePath) {
        try (InputStream is = BrandTheme.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static BufferedImage scaleImage(BufferedImage src, int targetW, int targetH) {
        BufferedImage out = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, targetW, targetH, null);
        g2.dispose();
        return out;
    }

    public static void styleSidebarButton(JButton button, boolean highlighted) {
        button.setFont(highlighted ? FONT_BODY_BOLD : FONT_BODY);
        button.setBackground(highlighted ? CREAM_ACCENT : Color.WHITE);
        button.setForeground(SIDEBAR_GREEN);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
