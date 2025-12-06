package mars.venus;

import javax.swing.*;

public final class GitHubMenuFactory {
    private GitHubMenuFactory() {}

    public static JMenu createEmptyGitHubMenu() {
        return new JMenu("GitHub");
    }

    public static void addSignInIfPresent(JMenu m) {
        OptionalMenus.addActionIfPresent(m, "mars.venus.SignInWithGitHubAction");
    }

    public static void addSignOutIfPresent(JMenu m) {
        OptionalMenus.addActionIfPresent(m, "mars.venus.SignOutOfGitHubAction");
    }

    public static void addUploadIfPresent(JMenu m) {
        OptionalMenus.addActionIfPresent(m, "mars.venus.UploadToGitHubAction");
    }
}
