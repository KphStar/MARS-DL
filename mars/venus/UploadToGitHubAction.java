package mars.venus;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

public class UploadToGitHubAction extends AbstractAction {

    public UploadToGitHubAction() {
        super("Upload to GitHub…");
        putValue(SHORT_DESCRIPTION, "Commit and push a local file to a GitHub repository");
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        UploadDialog d = new UploadDialog();
        d.setVisible(true);
        if (!d.ok) return;

        final String repoUrl = normalizeRepoUrl(d.getRepoUrl());
        final String branch  = d.getBranch().trim();

        if (!repoUrl.startsWith("https://")) {
            JOptionPane.showMessageDialog(null,
                    "Use an HTTPS URL, e.g. https://github.com/<user>/<repo>.git",
                    "GitHub", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ---- Credentials selection ----
        String userForHttps = "oauth2";   // user is ignored for HTTPS+token; must be non-empty
        String tokenForHttps = null;

        // (1) Prefer OAuth Device Flow session
        if (GitHubDeviceAuth.CURRENT != null) {
            tokenForHttps = GitHubDeviceAuth.CURRENT.accessToken;
        }

        // (2) Fall back to PAT dialog/env
        if (tokenForHttps == null || tokenForHttps.isBlank()) {
            String envUser  = System.getenv("GH_USER");
            String envToken = System.getenv("GH_TOKEN");
            userForHttps  = (envUser != null && !envUser.isBlank()) ? envUser : d.getUsername();
            tokenForHttps = (envToken != null && !envToken.isBlank()) ? envToken : d.getToken();
        }

        if (tokenForHttps == null || tokenForHttps.isBlank()) {
            JOptionPane.showMessageDialog(null,
                    "No credentials. Sign in (GitHub ▸ Sign in) or provide a Personal Access Token.",
                    "GitHub", JOptionPane.WARNING_MESSAGE);
            return;
        }

        var creds = new UsernamePasswordCredentialsProvider(userForHttps, tokenForHttps);

        Path temp = null;
        Git git = null;
        try {
            temp = Files.createTempDirectory("mars-git-" + UUID.randomUUID());

            // Try clone, else init
            try {
                git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(temp.toFile())
                        .setCloneAllBranches(false)
                        .setCredentialsProvider(creds)
                        .call();
                try { git.checkout().setName(branch).call(); } catch (Exception ignore) {}
            } catch (Exception cloneFail) {
                git = Git.init().setDirectory(temp.toFile()).call();
                git.remoteAdd().setName("origin").setUri(new URIish(repoUrl)).call();
                try { git.checkout().setCreateBranch(true).setName(branch).call(); } catch (Exception ignore) {}
            }

            // Copy file
            Path local = Paths.get(d.getLocalFile());
            if (!Files.isRegularFile(local)) throw new IllegalArgumentException("Local file not found: " + local);
            String pathInRepo = d.getPathInRepo().replace("\\","/");
            Path target = temp.resolve(pathInRepo);
            if (target.getParent()!=null) Files.createDirectories(target.getParent());
            Files.copy(local, target, StandardCopyOption.REPLACE_EXISTING);

            // Ensure branch
            boolean unborn = (git.getRepository().resolve("HEAD^{commit}") == null);
            if (unborn) {
                git.checkout().setCreateBranch(true).setName(branch).call();
            } else {
                try {
                    String current = git.getRepository().getBranch();
                    if (!branch.equals(current)) {
                        try { git.checkout().setName(branch).call(); }
                        catch (Exception ex) { git.checkout().setCreateBranch(true).setName(branch).call(); }
                    }
                } catch (Exception any) {
                    git.checkout().setCreateBranch(true).setName(branch).call();
                }
            }

            // Stage + identity + commit
            git.add().addFilepattern(pathInRepo).call();

            StoredConfig cfg = git.getRepository().getConfig();
            if (cfg.getString("user", null, "name") == null) {
                // a friendly identity (not used by GitHub auth)
                cfg.setString("user", null, "name", "MARS");
                cfg.setString("user", null, "email", "noreply@example.com");
                cfg.save();
            }

            git.commit().setMessage(d.getCommitMessage()).call();

            // Push
            git.push()
               .setRemote("origin")
               .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch))
               .setCredentialsProvider(creds)
               .setForce(false)
               .call();

            JOptionPane.showMessageDialog(null, "Upload complete ✅", "GitHub", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            Throwable root = ex;
            while (root.getCause()!=null) root = root.getCause();
            JOptionPane.showMessageDialog(null,
                    "Upload failed:\n" + root.getMessage() + "\n\nRepo URL used:\n" + repoUrl,
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (git != null) try { git.close(); } catch (Exception ignore) {}
            if (temp != null) try { deleteRecursively(temp); } catch (Exception ignore) {}
        }
    }

    private static String normalizeRepoUrl(String url) {
        if (url == null) return "";
        url = url.trim();
        if (url.startsWith("git@github.com:"))
            url = "https://github.com/" + url.substring("git@github.com:".length());
        while (url.endsWith("/")) url = url.substring(0, url.length()-1);
        if (!url.isEmpty() && !url.endsWith(".git")) url += ".git";
        return url;
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        Files.walk(root)
             .sorted((a,b)->b.getNameCount()-a.getNameCount())
             .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignore) {} });
    }

    // ===== Dialog (unchanged except title text) =====
    static class UploadDialog extends JDialog {
        JTextField tfLocal  = new JTextField();
        JTextField tfRepo   = new JTextField("https://github.com/YourUser/YourRepo.git");
        JTextField tfBranch = new JTextField("main");
        JTextField tfPath   = new JTextField("mips/" + System.getProperty("user.name","user") + ".asm");
        JTextField tfMsg    = new JTextField("Add file from MARS");
        JTextField tfUser   = new JTextField(System.getenv().getOrDefault("GH_USER",""));
        JPasswordField pfToken = new JPasswordField(System.getenv().getOrDefault("GH_TOKEN",""));
        boolean ok = false;

        UploadDialog() {
            setTitle("Upload to GitHub");
            setModal(true);
            setSize(640, 380);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout(10,10));
            JButton browse = new JButton("Browse…");
            browse.addActionListener(ev -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    tfLocal.setText(fc.getSelectedFile().getAbsolutePath());
                }
            });
            JPanel form = new JPanel(new GridLayout(0,1,6,6));
            form.add(row("Local file", tfLocal, browse));
            form.add(row("Repo URL (HTTPS)", tfRepo));
            form.add(row("Branch", tfBranch));
            form.add(row("Path in repo", tfPath));
            form.add(row("Commit message", tfMsg));
            form.add(row("GitHub username (fallback if not signed in)", tfUser));
            form.add(row("Personal Access Token (fallback if not signed in)", pfToken));
            add(form, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton cancel = new JButton("Cancel");
            JButton okBtn  = new JButton("Upload");
            cancel.addActionListener(e -> dispose());
            okBtn.addActionListener(e -> { ok = true; dispose(); });
            actions.add(cancel); actions.add(okBtn);
            add(actions, BorderLayout.SOUTH);
        }
        private JPanel row(String label, JComponent field) {
            JPanel p = new JPanel(new BorderLayout(5,5));
            p.add(new JLabel(label), BorderLayout.NORTH);
            p.add(field, BorderLayout.CENTER);
            return p;
        }
        private JPanel row(String label, JComponent field, JButton extra) {
            JPanel p = new JPanel(new BorderLayout(5,5));
            p.add(new JLabel(label), BorderLayout.NORTH);
            JPanel inner = new JPanel(new BorderLayout(5,5));
            inner.add(field, BorderLayout.CENTER);
            inner.add(extra, BorderLayout.EAST);
            p.add(inner, BorderLayout.CENTER);
            return p;
        }
        public String getLocalFile()     { return tfLocal.getText().trim(); }
        public String getRepoUrl()       { return tfRepo.getText().trim(); }
        public String getBranch()        { return tfBranch.getText().trim(); }
        public String getPathInRepo()    { return tfPath.getText().trim().replace("\\","/"); }
        public String getCommitMessage() { return tfMsg.getText().trim(); }
        public String getUsername()      { return tfUser.getText().trim(); }
        public String getToken()         { return new String(pfToken.getPassword()); }
    }
}
