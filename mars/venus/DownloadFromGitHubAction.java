package mars.venus;

import mars.Globals;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class DownloadFromGitHubAction extends AbstractAction {

    public DownloadFromGitHubAction() {
        super("Download file…");
        putValue(SHORT_DESCRIPTION, "Download a file from one of your GitHub repositories");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Reuse existing session
        GitHubAuthSession session = GitHubAuthSession.CURRENT;
        if (session == null) {
            JOptionPane.showMessageDialog(null,
                    "You must sign in once through GitHub ▸ Sign in.\n" +
                    "After that you can use Download / Upload without signing in again.",
                    "GitHub", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor((Component) e.getSource());

        // 1) Choose repo
        GitHubRepoChooserDialog repoDlg = new GitHubRepoChooserDialog(owner, session);
        repoDlg.setVisible(true);
        String repoName = repoDlg.getSelectedRepoName();
        if (repoName == null || repoName.isBlank()) return;

        // 2) Branch (default "main")
        String branch = (String) JOptionPane.showInputDialog(
                owner, "Branch name:", "Select branch",
                JOptionPane.PLAIN_MESSAGE, null, null, "main");
        if (branch == null) return;
        branch = branch.trim().isEmpty() ? "main" : branch.trim();

        // 3) Choose file path inside repo
        GitHubFileChooserDialog fileDlg =
                new GitHubFileChooserDialog(owner, session, repoName, branch);
        fileDlg.setVisible(true);
        String pathInRepo = fileDlg.getSelectedPath();
        if (pathInRepo == null || pathInRepo.isBlank()) return;

        try {
            // 4) Download raw file
            String login = session.login;
            String url = "https://raw.githubusercontent.com/" +
                         login + "/" + repoName + "/" + branch + "/" + pathInRepo;

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<byte[]> res =
                    http.send(req, HttpResponse.BodyHandlers.ofByteArray());

            if (res.statusCode() != 200) {
                JOptionPane.showMessageDialog(owner,
                        "Download failed (" + res.statusCode() + ")\n" + url,
                        "GitHub", JOptionPane.ERROR_MESSAGE);
                return;
            }

            byte[] data = res.body();

            // 5) Save locally (default filename = last segment)
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File(
                    Path.of(pathInRepo).getFileName().toString()));
            if (fc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;

            Path dest = fc.getSelectedFile().toPath();
            Files.write(dest, data);

            // 6) Open in MARS as NEW TAB
            boolean openedInMars = false;
            try {
                VenusUI ui = Globals.getGui();      // active GUI
                if (ui != null) {
                    Editor editor = ui.getEditor(); // Editor instance
                    if (editor != null) {
                        openedInMars = editor.openFile(dest.toFile());
                    }
                }
            } catch (Throwable t) {
                // fall through to notification; we don't want to crash anything
            }

            // 7) Notify user
            if (openedInMars) {
                JOptionPane.showMessageDialog(owner,
                        "Downloaded and opened in a new tab:\n" + dest.toString(),
                        "GitHub", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(owner,
                        "Downloaded to:\n" + dest.toString() +
                        "\n\nCould not auto-open in MARS.\n" +
                        "Use File ▸ Open to open it manually.",
                        "GitHub", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(owner,
                    "Download failed:\n" + ex.getMessage(),
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        }
    }
}
