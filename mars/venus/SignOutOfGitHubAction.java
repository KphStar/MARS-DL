package mars.venus;

import javax.swing.*;

public class SignOutOfGitHubAction extends AbstractAction {
    public SignOutOfGitHubAction() {
        super("Sign out of GitHub");
        putValue(SHORT_DESCRIPTION, "Forget the current GitHub session");
    }

    @Override public void actionPerformed(java.awt.event.ActionEvent e) {
        GitHubDeviceAuth.CURRENT = null;
        JOptionPane.showMessageDialog(null, "Signed out.", "GitHub",
                JOptionPane.INFORMATION_MESSAGE);
    }
}