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
        super("Upload…");
        putValue(SHORT_DESCRIPTION, "Commit and push a local file to a GitHub repository");
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        // 0) Must be signed in (session holds token + login)
        GitHubAuthSession session = GitHubAuthSession.CURRENT;
        if (session == null) {
            JOptionPane.showMessageDialog(null,
                    "You are not signed in.\nUse GitHub ▸ Sign in first.",
                    "GitHub", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 1) Ask what to upload / where (only file + repo name + branch + message)
        UploadDialog d = new UploadDialog(session.login);
        d.setVisible(true);
        if (!d.ok) return;

        String repoName = d.getRepoName().trim();
        if (repoName.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Enter a repository name.",
                    "GitHub", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Allow "MyRepo.git" or "/MyRepo"
        if (repoName.startsWith("/")) repoName = repoName.substring(1);
        if (repoName.endsWith(".git")) repoName = repoName.substring(0, repoName.length() - 4);

        // Build full HTTPS URL: https://github.com/<login>/<repo>.git
        String login = (session.login == null || session.login.isBlank())
                ? "YourUser"
                : session.login;
        final String repoUrl = "https://github.com/" + login + "/" + repoName + ".git";

        final String branch = (d.getBranch().trim().isEmpty() ? "main" : d.getBranch().trim());

        Path local = Paths.get(d.getLocalFile());
        if (!Files.isRegularFile(local)) {
            JOptionPane.showMessageDialog(null,
                    "Local file not found:\n" + local,
                    "GitHub", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Path in repo = just the filename (no subfolders)
        final String pathInRepo = local.getFileName().toString();

        var creds = new UsernamePasswordCredentialsProvider("oauth2", session.accessToken);

        Path temp = null;
        Git git = null;
        try {
            temp = Files.createTempDirectory("mars-git-" + UUID.randomUUID());

            // 2) Try to CLONE repo. If that fails (empty repo / no commits / no such repo),
            //    we INIT a new local repo and still commit.
            boolean cloned = false;
            try {
                git = Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(temp.toFile())
                        .setCloneAllBranches(false)
                        .setCredentialsProvider(creds)
                        .call();
                cloned = true;
            } catch (Exception cloneFail) {
                // clone failed – we'll create a new repo and push the first commit later
                git = Git.init()
                        .setDirectory(temp.toFile())
                        .setInitialBranch(branch)   // <-- important: avoid unborn HEAD + checkout
                        .call();
                git.remoteAdd().setName("origin").setUri(new URIish(repoUrl)).call();
            }

            // 3) Copy file into repo root
            Path target = temp.resolve(pathInRepo);
            if (target.getParent() != null) Files.createDirectories(target.getParent());
            Files.copy(local, target, StandardCopyOption.REPLACE_EXISTING);

            // 4) Branch handling
            boolean unborn = (git.getRepository().resolve("HEAD^{commit}") == null);

            if (!unborn) {
                // Repo already has commits (either cloned or previously initialized+committed).
                // Now make sure we're on the requested branch.
                try {
                    String current = git.getRepository().getBranch();
                    if (!branch.equals(current)) {
                        try {
                            // branch might already exist
                            git.checkout().setName(branch).call();
                        } catch (Exception ex) {
                            // create new branch from current HEAD
                            git.checkout().setCreateBranch(true).setName(branch).call();
                        }
                    }
                } catch (Exception any) {
                    // if anything weird happens, just stay on whatever branch we're on
                    System.err.println("Branch switch failed, staying on current branch: " + any);
                }
            }
            // If unborn == true, we do NOT touch branches here.
            // For the init case we already set initialBranch(branch), so the first commit
            // will create refs/heads/<branch>. No checkout necessary.

            // 5) Stage the file
            git.add().addFilepattern(pathInRepo).call();

            // 6) Commit identity based on signed-in GitHub user
            StoredConfig cfg = git.getRepository().getConfig();
            if (cfg.getString("user", null, "name") == null) {
                cfg.setString("user", null, "name", login);
                String email = login + "@users.noreply.github.com";
                cfg.setString("user", null, "email", email);
                cfg.save();
            }

            // 7) Commit
            git.commit()
               .setMessage(d.getCommitMessage())
               .call();

            // 8) Pull only if we actually cloned (repo exists with commits).
            //    For first-time init/push there is nothing to pull.
            if (cloned) {
                try {
                    git.pull()
                       .setRemote("origin")
                       .setCredentialsProvider(creds)
                       .call();
                } catch (Exception pullEx) {
                    System.err.println("Git pull failed (continuing to push): " + pullEx);
                }
            }

            // 9) Push – this will create the branch remotely if it doesn’t exist yet.
            git.push()
               .setRemote("origin")
               .setRefSpecs(new RefSpec("refs/heads/" + branch + ":refs/heads/" + branch))
               .setCredentialsProvider(creds)
               .setForce(false)
               .call();

            JOptionPane.showMessageDialog(null,
                    "Upload complete.\n\n" +
                    "Repo:   " + repoUrl + "\n" +
                    "Branch: " + branch + "\n" +
                    "File:   " + pathInRepo + "\n" +
                    "User:   " + login,
                    "GitHub", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            Throwable root = ex;
            while (root.getCause() != null) root = root.getCause();
            JOptionPane.showMessageDialog(null,
                    "Upload failed:\n" + root.getMessage() + "\n\nRepo URL used:\n" + repoUrl,
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        } finally {
            if (git != null) try { git.close(); } catch (Exception ignore) {}
            if (temp != null) try { deleteRecursively(temp); } catch (Exception ignore) {}
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) return;
        Files.walk(root)
             .sorted((a, b) -> b.getNameCount() - a.getNameCount())
             .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignore) {} });
    }

   // ===== Dialog: file, repo name, branch, message =====
static class UploadDialog extends JDialog {
    JTextField tfLocal    = new JTextField();
    JTextField tfRepoName = new JTextField();
    JTextField tfBranch   = new JTextField("main");
    JTextField tfMsg      = new JTextField("Add file from MARS");
    boolean ok = false;

    UploadDialog(String githubLogin) {
        setTitle("Upload to GitHub");
        setModal(true);
        setSize(640, 280);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JButton browseFile = new JButton("Browse…");
        browseFile.addActionListener(ev -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                tfLocal.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });

        JButton browseRepos = new JButton("Repos…");
        browseRepos.addActionListener(ev -> {
            GitHubAuthSession s = GitHubAuthSession.CURRENT;
            if (s == null) {
                JOptionPane.showMessageDialog(this,
                        "You are not signed in.\nUse GitHub ▸ Sign in first.",
                        "GitHub", JOptionPane.WARNING_MESSAGE);
                return;
            }
            GitHubRepoChooserDialog chooser =
                    new GitHubRepoChooserDialog(this, s);
            chooser.setVisible(true);
            String chosen = chooser.getSelectedRepoName();
            if (chosen != null && !chosen.isBlank()) {
                tfRepoName.setText(chosen);
            }
        });

        String userLabel = (githubLogin == null || githubLogin.isBlank())
                ? "your GitHub username"
                : githubLogin;

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(row("Local file", tfLocal, browseFile));
        form.add(row("Repository (under https://github.com/" + userLabel + "/)", tfRepoName, browseRepos));
        form.add(row("Branch", tfBranch));
        form.add(row("Commit message", tfMsg));

        add(form, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton okBtn  = new JButton("Upload");
        cancel.addActionListener(e -> dispose());
        okBtn.addActionListener(e -> { ok = true; dispose(); });
        actions.add(cancel);
        actions.add(okBtn);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel row(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel row(String label, JComponent field, JButton extra) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.add(new JLabel(label), BorderLayout.NORTH);
        JPanel inner = new JPanel(new BorderLayout(5, 5));
        inner.add(field, BorderLayout.CENTER);
        inner.add(extra, BorderLayout.EAST);
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    public String getLocalFile()     { return tfLocal.getText().trim(); }
    public String getRepoName()      { return tfRepoName.getText().trim(); }
    public String getBranch()        { return tfBranch.getText().trim(); }
    public String getCommitMessage() { return tfMsg.getText().trim(); }
}
}
