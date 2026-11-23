package kz.uib.kitaphana.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

final class UiTheme {
    static final Color NAVY = new Color(27, 48, 72);
    static final Color TEAL = new Color(15, 121, 109);
    static final Color TEAL_DARK = new Color(10, 91, 85);
    static final Color BG = new Color(246, 248, 251);
    static final Color TEXT = new Color(34, 48, 66);
    static final Color MUTED = new Color(100, 115, 130);
    static final Color BORDER = new Color(221, 228, 236);
    static final Color DANGER = new Color(175, 54, 58);
    static final Font NORMAL = new Font("Dialog", Font.PLAIN, 14);
    static final Font STRONG = new Font("Dialog", Font.BOLD, 14);

    private UiTheme() { }

    static void install() {
        UIManager.put("Label.font", NORMAL);
        UIManager.put("Button.font", STRONG);
        UIManager.put("TextField.font", NORMAL);
        UIManager.put("ComboBox.font", NORMAL);
        UIManager.put("Table.font", NORMAL);
        UIManager.put("TableHeader.font", STRONG);
        UIManager.put("OptionPane.messageFont", NORMAL);
        UIManager.put("OptionPane.buttonFont", STRONG);
    }

    static Border inset(int t, int l, int b, int r) {
        return BorderFactory.createEmptyBorder(t, l, b, r);
    }

    static JButton button(String label, Color background, Color foreground) {
        JButton button = new JButton(label);
        button.setFont(STRONG);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.equals(Color.WHITE) ? BORDER : background),
                inset(8, 13, 8, 13)));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    static void focusBorder(JComponent comp) {
        comp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), inset(7, 10, 7, 10)));
    }
}
