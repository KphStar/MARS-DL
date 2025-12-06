package mars.venus;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public final class GitHubApi {
    public static String fetchLogin(String token) throws IOException, InterruptedException {
        var http = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder(URI.create("https://api.github.com/user"))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + token)
                .timeout(Duration.ofSeconds(15))
                .GET().build();
        var res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) return "";
        var json = new org.json.JSONObject(res.body());
        return json.optString("login", "");
    }
}
