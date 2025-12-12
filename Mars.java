
/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining 
a copy of this software and associated documentation files (the 
"Software"), to deal in the Software without restriction, including 
without limitation the rights to use, copy, modify, merge, publish, 
distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject 
to the following conditions:

The above copyright notice and this permission notice shall be 
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Portal to Mars
 * 
 * @author Pete Sanderson
 * @version March 2006
 ***/


import mars.MarsLaunch;

import javax.swing.*;
import java.awt.*;

/**
 * Entry point for MARS with a simple password gate.
 */
public class Mars {

    private static final String DEFAULT_PASSWORD = "CS240";

    public static void main(String[] args) {
        // 1) Check password before starting MARS GUI
        if (!checkPassword()) {
            // User cancelled or failed authentication
            System.out.println("MARS launch aborted: password check failed.");
            System.exit(0);
        }

        // 2) Start the normal MARS launch logic
        try {
            new MarsLaunch(args);
        } catch (Throwable t) {
            t.printStackTrace();
            // Optional: show an error dialog if startup fails
            JOptionPane.showMessageDialog(null,
                    "MARS failed to start:\n" + t.getMessage(),
                    "MARS Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    /**
     * Returns the password.
     * If the environment variable MARS_PASSWORD is set and non-empty,
     * Otherwise fall back to DEFAULT_PASSWORD above.
     */
    private static String getExpectedPassword() {
        String fromEnv = System.getenv("MARS_PASSWORD");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_PASSWORD;
    }

    /**
     * Shows a Swing dialog asking for the password.
     * Returns true if the password is correct, false otherwise.
     */
    private static boolean checkPassword() {
        final String expected = getExpectedPassword();

        // If for some reason the expected password is empty, skip check.
        if (expected == null || expected.isEmpty()) {
            return true;
        }

        // Optional: number of attempts
        final int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            JPasswordField pf = new JPasswordField();
            pf.setEchoChar('•'); // nicer bullet char

            JPanel panel = new JPanel(new BorderLayout(8, 8));
            panel.add(new JLabel("Enter password to open MARS:"), BorderLayout.NORTH);
            panel.add(pf, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(
                    null,
                    panel,
                    "MARS Password",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.OK_OPTION) {
                // Cancel / close
                return false;
            }

            char[] entered = pf.getPassword();
            String enteredStr = new String(entered);

            // Clear the char[] for a tiny bit of hygiene
            java.util.Arrays.fill(entered, '\0');

            if (expected.equals(enteredStr)) {
                return true; // success
            } else {
                if (attempt < maxAttempts) {
                    JOptionPane.showMessageDialog(
                            null,
                            "Incorrect password. Attempts remaining: " + (maxAttempts - attempt),
                            "MARS Password",
                            JOptionPane.WARNING_MESSAGE
                    );
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            "Too many failed attempts. Exiting.",
                            "MARS Password",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }

        return false; // failed all attempts
    }
}


