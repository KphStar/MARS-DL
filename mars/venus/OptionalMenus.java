package mars.venus;

import javax.swing.*;
import java.lang.reflect.Constructor;

public final class OptionalMenus {
    private OptionalMenus() {}

    /** If the given class exists and has a no-arg constructor and extends AbstractAction,
     *  add it as a JMenuItem. Returns true if added. */
    public static boolean addActionIfPresent(JMenu menu, String fqcn) {
        try {
            Class<?> c = Class.forName(fqcn);
            if (!javax.swing.AbstractAction.class.isAssignableFrom(c)) return false;
            @SuppressWarnings("unchecked")
            Class<? extends AbstractAction> actionClass = (Class<? extends AbstractAction>) c;
            Constructor<? extends AbstractAction> ctor = actionClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            AbstractAction action = ctor.newInstance();
            menu.add(new JMenuItem(action));
            return true;
        } catch (Throwable ignore) {
            // Class missing or bad ctor — just skip silently
            return false;
        }
    }
}
