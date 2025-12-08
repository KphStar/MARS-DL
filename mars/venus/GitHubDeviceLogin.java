package mars.venus;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.json.JSONObject;

/**
 * Handles GitHub OAuth Device Authorization Flow.
 * No client secret needed. You only need your GitHub OAuth App CLIENT_ID.
 */
public final class GitHubDeviceLogin {
    private static final String DEVICE_CODE_URL = "https://github.com/login/device/code";
    private static final String TOKEN_URL       = "https://github.com/login/oauth/access_token";

    private final String clientId;
    private final String scope; // e.g. "public_repo" or "repo"

    public GitHubDeviceLogin(String clientId, String scope) {
        this.clientId = clientId;
        this.scope = scope;
    }

    /** Launch browser, wait for user to authorize, then poll until approved and return the access token. */
    public String authorizeBlocking() throws Exception {
        HttpClient http = HttpClient.newHttpClient();

        // 1) Get device_code & user_code
        String form = "client_id=" + enc(clientId) + "&scope=" + enc(scope);
        HttpRequest req = HttpRequest.newBuilder(URI.create(DEVICE_CODE_URL))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw new IOException("GitHub device start failed: " + res.body());
        }

        JSONObject json = new JSONObject(res.body());
        String deviceCode = json.getString("device_code");
        String userCode   = json.getString("user_code");
        String verifyUri  = json.getString("verification_uri");
        int intervalSec   = json.optInt("interval", 5);

        // 2) Show instructions + open browser
        try {
            Desktop.getDesktop().browse(URI.create(verifyUri));
        } catch (Exception ignore) {}

        JOptionPane.showMessageDialog(null,
                "Authorize this app to use your GitHub account:\n\n" +
                "1) Your browser opened: " + verifyUri + "\n" +
                "2) Enter this code:  " + userCode + "\n" +
                "3) Click Authorize.\n\n" +
                "Return here after approving.",
                "GitHub Sign-In", JOptionPane.INFORMATION_MESSAGE);

        // 3) Poll for token
        while (true) {
            Thread.sleep(intervalSec * 1000L);

            String poll = "client_id=" + enc(clientId) +
                          "&device_code=" + enc(deviceCode) +
                          "&grant_type=urn:ietf:params:oauth:grant-type:device_code";

            HttpRequest tokenReq = HttpRequest.newBuilder(URI.create(TOKEN_URL))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(poll))
                    .build();

            HttpResponse<String> tokenRes = http.send(tokenReq, HttpResponse.BodyHandlers.ofString());
            if (tokenRes.statusCode() != 200) {
                throw new IOException("GitHub token error: " + tokenRes.body());
            }

            JSONObject tjson = new JSONObject(tokenRes.body());

            if (tjson.has("access_token")) {
                return tjson.getString("access_token");
            }

            String err = tjson.optString("error", "");
            if ("authorization_pending".equals(err)) {
                continue;
            }
            if ("slow_down".equals(err)) {
                intervalSec += 2;
                continue;
            }
            if (!err.isEmpty()) {
                throw new IOException("GitHub authorization failed: " + err);
            }
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
