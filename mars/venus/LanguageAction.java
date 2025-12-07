package mars.venus;

import mars.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import mars.mips.instructions.CustomAssembly;
import mars.mips.instructions.MipsAssembly;

/**
 * Action class for the Instruction Set dropdown to toggle instruction sets on and off.
 */
public class LanguageAction extends GuiAction {
    private CustomAssembly assembly;
    private ArrayList<CustomAssembly> assemblyList;
    private JMenu dropdown;
    private EditTabbedPane editPane;
    private VenusUI g;
    private JFrame window;

    public LanguageAction(String name, Icon icon, String descrip,
                          Integer mnemonic, KeyStroke accel,
                          VenusUI gui, CustomAssembly asm,
                          ArrayList<CustomAssembly> asmList,
                          JMenu dd, EditTabbedPane ep, JFrame win) {
        super(name, icon, descrip, mnemonic, accel, gui);
        g = gui;
        assembly = asm;
        assemblyList = asmList;
        dropdown = dd;
        editPane = ep;
        window = win;
    }

    public void actionPerformed(ActionEvent e) {

    boolean userClickedMips = (assembly instanceof MipsAssembly);

        if (userClickedMips) {
            for (CustomAssembly c : assemblyList) {
                c.enabled = (c instanceof MipsAssembly);
            }
        } else {
            for (CustomAssembly c : assemblyList) {
                if (c instanceof MipsAssembly) {
                    c.enabled = true;              
                } else {
                    c.enabled = (c == assembly);   
                }
            }
        }

        // UI coloring
        for (Component c : dropdown.getMenuComponents()) {
            ((JMenuItem) c).setBackground(Color.WHITE);
        }
        ((JMenuItem) e.getSource()).setBackground(new Color(200, 221, 242));

        // Regenerate the instruction set
        Globals.instructionSet.populate();

        // Optional syntax highlighting fix if you pass editPane in
        if (editPane != null && editPane.getCurrentEditTab() != null) {
            String currentFilename = editPane.getCurrentEditTab().getFilename();
            editPane.closeCurrentFile();
            editPane.openFile(new File(currentFilename));
        }

        if (window != null) {
            window.setVisible(false);
        }
    }
}
