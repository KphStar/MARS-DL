package mars.venus;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GitHubRepoChooserDialog extends JDialog {

    private final GitHubAuthSession session;

    private final DefaultListModel<String> repoModel = new DefaultListModel<>();
    private final JList<String> repoList = new JList<>(repoModel);

    private String selectedRepoName = null;

    public GitHubRepoChooserDialog(Window owner, GitHubAuthSession session) {
        super(owner, "Choose GitHub Repository", ModalityType.APPLICATION_MODAL);
        this.session = session;

        setSize(500, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        repoList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(repoList);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadRepos());

        JButton newRepoBtn = new JButton("+ New Repo");
        newRepoBtn.addActionListener(e -> createNewRepo());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(refreshBtn);
        top.add(newRepoBtn);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton select = new JButton("Select");
        cancel.addActionListener(e -> {
            selectedRepoName = null;
            dispose();
        });
        select.addActionListener(e -> {
            String sel = repoList.getSelectedValue();
            if (sel != null && !sel.isBlank()) {
                selectedRepoName = sel;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select a repository.",
                        "GitHub", JOptionPane.WARNING_MESSAGE);
            }
        });
        bottom.add(cancel);
        bottom.add(select);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        // Load on open
        loadRepos();
    }

    public String getSelectedRepoName() {
        return selectedRepoName;
    }

    // ---- HTTP helpers ----

    private void loadRepos() {
        repoModel.clear();
        try {
            HttpClient http = HttpClient.newHttpClient();
            // list user repos, first 100 is usually enough
            HttpRequest req = HttpRequest.newBuilder(
                            URI.create("https://api.github.com/user/repos?per_page=100"))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + session.accessToken)
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                JOptionPane.showMessageDialog(this,
                        "Failed to load repositories:\n" + res.body(),
                        "GitHub", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JSONArray arr = new JSONArray(res.body());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                // we only care about the simple repo name, not full_name
                String name = o.optString("name");
                if (name != null && !name.isBlank()) {
                    repoModel.addElement(name);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading repos:\n" + ex.getMessage(),
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createNewRepo() {
        String name = JOptionPane.showInputDialog(this,
                "New repository name:",
                "Create GitHub Repository",
                JOptionPane.PLAIN_MESSAGE);

        if (name == null) return; // cancelled
        name = name.trim();
        if (name.isEmpty()) return;

        try {
            HttpClient http = HttpClient.newHttpClient();

            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("auto_init", true); // create initial commit so clone works
            // body.put("private", false); // set to true if you want private

            HttpRequest req = HttpRequest.newBuilder(
                            URI.create("https://api.github.com/user/repos"))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + session.accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 201) {
                JOptionPane.showMessageDialog(this,
                        "Failed to create repository:\n" + res.body(),
                        "GitHub", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // On success, add to list and select it
            if (!repoModel.contains(name)) {
                repoModel.addElement(name);
            }
            repoList.setSelectedValue(name, true);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error creating repo:\n" + ex.getMessage(),
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        }
    }
}
