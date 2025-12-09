package mars.venus;

import javax.swing.*;

import mars.venus.VenusUI;

import java.awt.*;

/**
 * Provides a "Theme" menu that lets the user change
 * the main MARS DL window colors.
 */
public final class ColorThemeMenu {

    private ColorThemeMenu() {}

    public static JMenu create(VenusUI ui) {
        JMenu theme = new JMenu("Theme");

        // --- Default theme ---
        JMenuItem defaultItem = new JMenuItem("Default");
        defaultItem.addActionListener(e -> applyDefaultTheme(ui));
        theme.add(defaultItem);

        // --- Custom color chooser ---
        JMenuItem customItem = new JMenuItem("Custom…");
        customItem.addActionListener(e -> {
            Color current = ui.getContentPane().getBackground();
            Color chosen = JColorChooser.showDialog(ui, "Choose theme color", current);
            if (chosen != null) {
                applySolidColorTheme(ui, chosen);
            }
        });
        theme.add(customItem);

        return theme;
    }

    // ----- Theme helpers -----

    private static void applyDefaultTheme(VenusUI ui) {
        // Reset to standard Swing colors
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        if (bg == null) bg = Color.LIGHT_GRAY;
        if (fg == null) fg = Color.BLACK;

        applyColors(ui, bg, fg);
    }

    private static void applyDarkTheme(VenusUI ui) {
        Color bg = new Color(0x212121);
        Color fg = new Color(0xEEEEEE);
        applyColors(ui, bg, fg);
    }

    private static void applySolidColorTheme(VenusUI ui, Color bg) {
        // Choose a readable foreground based on background brightness
        int luminance = (int)(0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue());
        Color fg = luminance < 128 ? Color.WHITE : Color.BLACK;
        applyColors(ui, bg, fg);
    }

     private static void applyColors(VenusUI ui, Color bg, Color fg) {
      if (ui == null) return;

      // Main window content
      ui.getContentPane().setBackground(bg);
      ui.getContentPane().setForeground(fg);

      // Theme all editor tabs (code area + line numbers + status)
      try {
         if (ui.editor != null) {
            ui.editor.applyTheme(bg, fg);
         }
      } catch (Throwable ignore) {
         // If anything goes wrong, just don't crash; window bg is still themed.
      }

      ui.repaint();
   }

}
