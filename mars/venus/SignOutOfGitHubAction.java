package mars.venus;

import javax.swing.*;
import java.awt.event.ActionEvent;

/** GitHub ▸ Sign out */
public class SignOutOfGitHubAction extends AbstractAction {

    public SignOutOfGitHubAction() {
        super("Sign out");
        putValue(SHORT_DESCRIPTION, "Sign out of GitHub");
     
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GitHubAuthSession.CURRENT = null;
        JOptionPane.showMessageDialog(null,
                "Signed out of GitHub.",
                "GitHub", JOptionPane.INFORMATION_MESSAGE);
        //GitHubStatusIndicator.updateStatus();
        GitHubMenuStatus.update();
    }
    
}
