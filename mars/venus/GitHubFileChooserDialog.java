package mars.venus;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Dialog that lists all files (blobs) in a given repo + branch
 * and lets the user pick one path.
 */
public class GitHubFileChooserDialog extends JDialog {

    private final GitHubAuthSession session;
    private final String repoName;
    private final String branch;

    private final DefaultListModel<String> fileModel = new DefaultListModel<>();
    private final JList<String> fileList = new JList<>(fileModel);

    private String selectedPath = null;

    public GitHubFileChooserDialog(Window owner,
                                   GitHubAuthSession session,
                                   String repoName,
                                   String branch) {
        super(owner,
              "Choose file from " + repoName + " (" + branch + ")",
              ModalityType.APPLICATION_MODAL);

        this.session = session;
        this.repoName = repoName;
        this.branch = branch;

        setSize(600, 400);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(fileList);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Select a file from " + repoName + " (" + branch + ")"));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton select = new JButton("Select");
        cancel.addActionListener(e -> {
            selectedPath = null;
            dispose();
        });
        select.addActionListener(e -> {
            String sel = fileList.getSelectedValue();
            if (sel != null && !sel.isBlank()) {
                selectedPath = sel;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select a file.",
                        "GitHub", JOptionPane.WARNING_MESSAGE);
            }
        });
        bottom.add(cancel);
        bottom.add(select);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        loadFiles();
    }

    /** Returns the selected path in the repo (e.g. "src/Main.asm"), or null if cancelled. */
    public String getSelectedPath() {
        return selectedPath;
    }

    private void loadFiles() {
        fileModel.clear();
        try {
            HttpClient http = HttpClient.newHttpClient();
            String owner = session.login;

            String url = "https://api.github.com/repos/" + owner + "/" +
                         repoName + "/git/trees/" + branch + "?recursive=1";

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + session.accessToken)
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                JOptionPane.showMessageDialog(this,
                        "Failed to load files:\n" + res.body(),
                        "GitHub", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JSONObject root = new JSONObject(res.body());
            JSONArray tree = root.getJSONArray("tree");

            for (int i = 0; i < tree.length(); i++) {
                JSONObject entry = tree.getJSONObject(i);
                String type = entry.optString("type");
                if (!"blob".equals(type)) continue; // only files, skip directories
                String path = entry.optString("path");
                if (path != null && !path.isBlank()) {
                    fileModel.addElement(path);
                }
            }

            if (fileModel.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "No files found in this branch.",
                        "GitHub", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading files:\n" + ex.getMessage(),
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        }
    }
}
