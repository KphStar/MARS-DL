package mars.venus;

import java.util.prefs.Preferences;

public final class GitHubAuth {
    private static final Preferences P = Preferences.userRoot().node("mars/github");
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_USER  = "username";

    public static void save(String username, String token) {
        if (username != null) P.put(KEY_USER, username);
        if (token != null) P.put(KEY_TOKEN, token);
    }

    public static String token()   { return P.get(KEY_TOKEN, ""); }
    public static String username(){ return P.get(KEY_USER,  ""); }
    public static boolean isSignedIn() { return !token().isEmpty(); }

    public static void signOut() {
        P.remove(KEY_TOKEN);
        P.remove(KEY_USER);
    }
}
