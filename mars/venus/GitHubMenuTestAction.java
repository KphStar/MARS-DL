package mars.venus;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class GitHubMenuTestAction extends AbstractAction {
    public GitHubMenuTestAction() {
        super("Test Upload…");                    // Menu item label
        putValue(SHORT_DESCRIPTION, "GitHub test hook");
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl shift U")); // optional shortcut
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(
                null,
                "Hello from the GitHub menu! Wiring works",
                "MARS → GitHub (Test)",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}