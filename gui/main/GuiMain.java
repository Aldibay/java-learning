package kz.uib.kitaphana.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Графикалық терезені Swing Event Dispatch Thread ішінде іске қосамыз. */
public final class GuiMain {
    private GuiMain() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Жүйелік стиль қолжетімсіз болса, Swing стандартты стилін қолданады.
            }
            UiTheme.install();
            new Dashboard().setVisible(true);
        });
    }
}
