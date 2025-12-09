package mars.venus;

public final class GitHubAuthSession {
    public static volatile GitHubAuthSession CURRENT;

    public final String accessToken;
    public final String login;   // GitHub username (login)
    public final String avatarUrl;  

    public GitHubAuthSession(String accessToken, String login, String avatarUrl) {
        this.accessToken = accessToken;
        this.login = login;
        this.avatarUrl = avatarUrl;
    }

    public static void clear() {
        CURRENT = null;
    }
}
