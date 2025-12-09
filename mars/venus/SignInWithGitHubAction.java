package mars.venus;

import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** GitHub ▸ Sign in */
public class SignInWithGitHubAction extends AbstractAction {

    // Replace with your OAuth App client ID
    private static final String GITHUB_CLIENT_ID = "Ov23lipng1s1BQoW8bCT";

    public SignInWithGitHubAction() {
        super("Sign in…");
        putValue(SHORT_DESCRIPTION, "Sign in to GitHub using Device Login");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // 1) Device flow -> token
            GitHubDeviceLogin login = new GitHubDeviceLogin(GITHUB_CLIENT_ID, "repo");
            String token = login.authorizeBlocking();
            if (token == null || token.isBlank()) {
                JOptionPane.showMessageDialog(null,
                        "GitHub sign-in did not return an access token.",
                        "GitHub", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2) Use token to get the GitHub username (login)
            HttpClient http = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                JOptionPane.showMessageDialog(null,
                        "Failed to fetch GitHub user info:\n" + res.body(),
                        "GitHub", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JSONObject json = new JSONObject(res.body());
            String loginName = json.optString("login", "unknown");
            String avatarUrl = json.optString("avatar_url", null);

            // 3) Store session
            GitHubAuthSession.CURRENT = new GitHubAuthSession(token, loginName, avatarUrl);

            JOptionPane.showMessageDialog(null,
                    "Signed in to GitHub as: " + loginName,
                    "GitHub", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "GitHub sign-in failed:\n" + ex.getMessage(),
                    "GitHub", JOptionPane.ERROR_MESSAGE);
        }
        GitHubMenuStatus.update();
    }
}
