package mars.venus;

import javax.imageio.ImageIO;
import javax.swing.*;

import mars.venus.GitHubAuthSession;

import java.awt.*;
import java.net.URL;

/**
 * Decorates the existing "GitHub" JMenu with icon + username.
 * After login, the menu text becomes e.g. "GitHub (KphStar)" and
 * the icon becomes the user's avatar. When logged out, it shows
 * "GitHub (Not signed in)" with a generic GitHub icon.
 */
public final class GitHubMenuStatus {

    private static JMenu githubMenu;
    private static ImageIcon defaultIcon;

    private GitHubMenuStatus() {}

    /** Call once after you've created the GitHub JMenu. */
    public static void attach(JMenu menu) {
        githubMenu = menu;
        loadDefaultIcon();
        update();   // set initial text/icon
    }

    /** Load fallback GitHub icon from disk once. */
    private static void loadDefaultIcon() {
        if (defaultIcon != null) return;
        try {
            // relative to where you run `java` (MARS-DL root)
            ImageIcon raw = new ImageIcon("mars/images/MarsSurfacePathfinder.jpg");
            Image scaled = raw.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            defaultIcon = new ImageIcon(scaled);
        } catch (Exception ex) {
            System.err.println("GitHub default icon load failed: " + ex);
            defaultIcon = null;
        }
    }

    /** Update menu text + icon based on current auth session. */
    public static void update() {
        if (githubMenu == null) return;

        GitHubAuthSession s = GitHubAuthSession.CURRENT;

        if (s == null) {
            githubMenu.setText("GitHub (Not signed in)");
            githubMenu.setIcon(defaultIcon);
            return;
        }

        githubMenu.setText("GitHub (" + s.login + ")");

        if (s.avatarUrl == null || s.avatarUrl.isBlank()) {
            githubMenu.setIcon(defaultIcon);
            return;
        }

        // Load avatar in a background thread so UI doesn't freeze
        new Thread(() -> {
            try {
                URL url = new URL(s.avatarUrl);
                Image avatarImg = ImageIO.read(url);
                if (avatarImg != null) {
                    Image scaled = avatarImg.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                    ImageIcon avatarIcon = new ImageIcon(scaled);
                    SwingUtilities.invokeLater(() -> githubMenu.setIcon(avatarIcon));
                    return;
                }
            } catch (Exception ignore) {
                // fall through to default icon
            }
            SwingUtilities.invokeLater(() -> githubMenu.setIcon(defaultIcon));
        }, "GitHubAvatarLoader").start();
    }
}
