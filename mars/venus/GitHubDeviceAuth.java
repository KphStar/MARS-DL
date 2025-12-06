package mars.venus;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.LinkedHashMap;

public final class GitHubDeviceAuth {

    // ====== Session ======
    public static final class Session {
        public final String accessToken;
        public final String tokenType;
        public final String scope;

        public Session(String accessToken, String tokenType, String scope) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.scope = scope;
        }
    }

    // Keep the current session in memory
    public static volatile Session CURRENT = null;

    // ====== Device code response ======
    public static final class DeviceCode {
        public final String device_code;
        public final String user_code;
        public final String verification_uri;
        public final int expires_in;
        public final int interval;

        public DeviceCode(String device_code, String user_code, String verification_uri, int expires_in, int interval) {
            this.device_code = device_code;
            this.user_code = user_code;
            this.verification_uri = verification_uri;
            this.expires_in = expires_in;
            this.interval = interval;
        }
    }

    // ====== Small JSON helper (no external libs) ======
    private static String jsonGet(String json, String key) {
        // super tiny parser for flat JSON strings: "key":"value"
        // works because GitHub responses here are simple
        String q = "\"" + key + "\"";
        int i = json.indexOf(q);
        if (i < 0) return null;
        int c = json.indexOf(':', i + q.length());
        if (c < 0) return null;
        // skip spaces and quotes
        int s = c + 1;
        while (s < json.length() && (json.charAt(s) == ' ' || json.charAt(s) == '\"')) s++;
        // read until quote/comma/brace
        int e = s;
        boolean inStr = json.charAt(s - 1) == '\"';
        if (inStr) {
            // ended by quote
            e = json.indexOf('"', s);
            if (e < 0) e = json.length();
            return json.substring(s, e);
        } else {
            while (e < json.length() && json.charAt(e) != ',' && json.charAt(e) != '}' && json.charAt(e) != '\n') e++;
            return json.substring(s, e).trim();
        }
    }

    private static HttpClient http() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private static HttpRequest formPost(String url, Map<String,String> body) {
        StringBuilder sb = new StringBuilder();
        for (var e : body.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                .build();
    }

    private static String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ====== Public API ======

    /** Start device flow: returns device_code + user_code + verification_uri. */
    public static DeviceCode startDeviceFlow(String clientId, String scope) throws Exception {
        var req = formPost("https://github.com/login/device/code",
                Map.of("client_id", clientId, "scope", scope));
        var res = http().send(req, BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new RuntimeException("Device code HTTP " + res.statusCode() + ": " + res.body());

        String json = res.body();
        var device_code = jsonGet(json, "device_code");
        var user_code = jsonGet(json, "user_code");
        var verification_uri = jsonGet(json, "verification_uri");
        int expires_in = Integer.parseInt(jsonGet(json, "expires_in"));
        int interval = Integer.parseInt(jsonGet(json, "interval"));

        if (device_code == null || user_code == null || verification_uri == null) {
            throw new RuntimeException("Malformed device code response: " + json);
        }
        return new DeviceCode(device_code, user_code, verification_uri, expires_in, interval);
    }

    /** Polls for access_token until authorized or expired. */

public static Session pollForToken(String clientId, DeviceCode dc) throws Exception {
    long deadline = System.currentTimeMillis() + (long) dc.expires_in * 1000L;
    int wait = Math.max(dc.interval, 5);

    // read once; OK if null/blank
    final String clientSecret = System.getenv("GITHUB_OAUTH_CLIENT_SECRET");
    final HttpClient client = http();

    while (System.currentTimeMillis() < deadline) {
        // Build form body; include client_secret if available
        var body = new LinkedHashMap<String, String>();
        body.put("client_id", clientId);
        if (clientSecret != null && !clientSecret.isBlank()) {
            body.put("client_secret", clientSecret);
        }
        body.put("device_code", dc.device_code);
        body.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

        HttpRequest req = formPost("https://github.com/login/oauth/access_token", body);
        HttpResponse<String> res = client.send(req, BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            throw new RuntimeException("Token HTTP " + res.statusCode() + ": " + res.body());
        }

        String json = res.body();
        String error = jsonGet(json, "error");
        if (error == null) {
            String access = jsonGet(json, "access_token");
            String type   = jsonGet(json, "token_type");
            String scope  = jsonGet(json, "scope");
            if (access == null) throw new RuntimeException("No access_token in response: " + json);
            return new Session(access, type, scope);
        }

        switch (error) {
            case "authorization_pending":
                Thread.sleep(wait * 1000L);
                break;
            case "slow_down":
                wait += 5;
                Thread.sleep(wait * 1000L);
                break;
            case "expired_token":
                throw new RuntimeException("Device code expired.");
            case "access_denied":
                throw new RuntimeException("User denied access.");
            case "incorrect_client_credentials":
                throw new RuntimeException("Incorrect client_id/client_secret or device flow not enabled.");
            default:
                throw new RuntimeException("OAuth error: " + json);
        }
    }
    throw new RuntimeException("Timed out waiting for authorization.");
}


    // ====== Swing Actions ======

    /** Menu item: GitHub ▸ Sign In */
    public static final class SignInWithGitHubAction extends AbstractAction {
        public SignInWithGitHubAction() { super("Sign in…"); }

        @Override public void actionPerformed(java.awt.event.ActionEvent e) {
            String clientId = System.getenv("GITHUB_OAUTH_CLIENT_ID");
            if (clientId == null || clientId.isBlank()) {
                clientId = JOptionPane.showInputDialog(null,
                        "Enter your GitHub OAuth App Client ID\n(Device Flow must be enabled):",
                        "GitHub", JOptionPane.QUESTION_MESSAGE);
                if (clientId == null || clientId.isBlank()) {
                    JOptionPane.showMessageDialog(null, "Sign-in cancelled.", "GitHub", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }

            final String scope = "repo"; // adjust if you need more
            try {
                DeviceCode dc = startDeviceFlow(clientId, scope);

                // Show code & open browser
                JTextArea ta = new JTextArea(
                        "1) Visit: " + dc.verification_uri + "\n" +
                        "2) Enter code: " + dc.user_code + "\n\n" +
                        "Waiting for authorization…");
                ta.setEditable(false);
                ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

                var pane = new JScrollPane(ta);
                var dlg = new JDialog((Frame)null, "GitHub Sign in", true);
                dlg.getContentPane().add(pane);
                dlg.setSize(520, 220);
                dlg.setLocationRelativeTo(null);

                // Try opening the browser automatically
                try { Desktop.getDesktop().browse(new URI(dc.verification_uri)); } catch (Exception ignore) {}

                AtomicBoolean done = new AtomicBoolean(false);


                final String cid = clientId;
                final DeviceCode code = dc;

                // Poll in background
                new SwingWorker<Void,Void>() {
                    @Override protected Void doInBackground() {
                        try {
                           Session s = pollForToken(cid, code);
                            CURRENT = s;
                            ta.append("\n\n Authorized. Scope: " + s.scope);
                            done.set(true);
                        } catch (Exception ex) {
                            ta.append("\n\n Failed: " + ex.getMessage());
                        }
                        return null;
                    }
                    @Override protected void done() {
                        JButton ok = new JButton("Close");
                        ok.addActionListener(ev -> dlg.dispose());
                        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        south.add(ok);
                        dlg.getContentPane().add(south, BorderLayout.SOUTH);
                        dlg.revalidate(); dlg.repaint();
                    }
                }.execute();

                dlg.setVisible(true);

                if (done.get()) {
                    JOptionPane.showMessageDialog(null, "Signed in to GitHub.", "GitHub", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Sign-in failed:\n" + ex.getMessage(),
                        "GitHub", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Menu item: GitHub ▸ Sign Out */
    public static final class SignOutOfGitHubAction extends AbstractAction {
        public SignOutOfGitHubAction() { super("Sign out"); }
        @Override public void actionPerformed(java.awt.event.ActionEvent e) {
            CURRENT = null;
            JOptionPane.showMessageDialog(null, "Signed out.", "GitHub", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
