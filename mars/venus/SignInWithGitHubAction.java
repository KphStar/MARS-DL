package mars.venus;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Menu item: “Sign in with GitHub…”
 * Uses GitHubDeviceAuth (Device Flow). Requires env var GITHUB_OAUTH_CLIENT_ID.
 */
public class SignInWithGitHubAction extends AbstractAction {

    public SignInWithGitHubAction() {
        super("Sign in with GitHub…");
        putValue(SHORT_DESCRIPTION, "Authorize MARS to access your GitHub repositories");
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        try {
            // 1) Get client id (from env or prompt once)
            String clientId = System.getenv("GITHUB_OAUTH_CLIENT_ID");
            if (clientId == null || clientId.isBlank()) {
                clientId = JOptionPane.showInputDialog(
                        null,
                        "Enter your GitHub OAuth App Client ID (Device Flow must be enabled):",
                        "GitHub",
                        JOptionPane.QUESTION_MESSAGE
                );
                if (clientId == null || clientId.isBlank()) {
                    JOptionPane.showMessageDialog(null, "Sign-in cancelled.", "GitHub",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            // 2) Start device flow (scope 'repo' is enough for clone/pull/push in user repos)
            final String scope = "repo";
            GitHubDeviceAuth.DeviceCode dc = GitHubDeviceAuth.startDeviceFlow(clientId, scope);

            // 3) Show instructions and begin polling in background
            JTextArea ta = new JTextArea(
                    "1) Visit: " + dc.verification_uri + "\n" +
                    "2) Enter code: " + dc.user_code + "\n\n" +
                    "Waiting for authorization…"
            );
            ta.setEditable(false);
            ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

            JScrollPane pane = new JScrollPane(ta);
            JDialog dlg = new JDialog((Frame) null, "GitHub Sign in", true);
            dlg.getContentPane().setLayout(new BorderLayout());
            dlg.getContentPane().add(pane, BorderLayout.CENTER);
            dlg.setSize(560, 260);
            dlg.setLocationRelativeTo(null);

            // Try to open the browser automatically (optional)
            try {
                Desktop.getDesktop().browse(new URI(dc.verification_uri));
            } catch (Exception ignore) {}

            final String cid = clientId;                // effectively final for inner class
            final GitHubDeviceAuth.DeviceCode code = dc;
            AtomicBoolean ok = new AtomicBoolean(false);

            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() {
                    try {
                        GitHubDeviceAuth.Session s = GitHubDeviceAuth.pollForToken(cid, code);
                        GitHubDeviceAuth.CURRENT = s;
                        ta.append("\n\nAuthorized. Scope: " + s.scope);
                        ok.set(true);
                    } catch (Exception ex) {
                        ta.append("\n\nFailed: " + ex.getMessage());
                    }
                    return null;
                }

                @Override protected void done() {
                    JButton close = new JButton("Close");
                    close.addActionListener(ev -> dlg.dispose());
                    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                    south.add(close);
                    dlg.getContentPane().add(south, BorderLayout.SOUTH);
                    dlg.revalidate();
                    dlg.repaint();
                }
            }.execute();

            dlg.setVisible(true);

            if (ok.get()) {
                JOptionPane.showMessageDialog(null, "Signed in to GitHub ✓",
                        "GitHub", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Sign-in failed:\n" + ex.getMessage(),
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        }
    }
}
