package mars.venus;

public final class GitHubAuthSession {
    public static volatile GitHubAuthSession CURRENT;

    public final String accessToken;
    public final String login;   // GitHub username (login)

    public GitHubAuthSession(String accessToken, String login) {
        this.accessToken = accessToken;
        this.login = login;
    }

    public static void clear() {
        CURRENT = null;
    }
}
