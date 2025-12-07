package mars.mips.instructions;

import mars.*;
import java.util.*;

/**
 * Handles all user-defined instruction sets.
 * For your setup: built-in MIPS + DemonSlayerAssembly.
 */
public class LanguageLoader {
    private static ArrayList<BasicInstruction> finalInstructionList = new ArrayList<BasicInstruction>();

    public static ArrayList<CustomAssembly> assemblyList = new ArrayList<CustomAssembly>();

    static {
        // ---- 1) Base MIPS ISA ----
        MipsAssembly mips = new MipsAssembly();
        mips.enabled = true;          // default: MIPS ON
        assemblyList.add(mips);

        // ---- 2) Demon Slayer ISA ----
        CustomAssembly demon = new mars.mips.instructions.customlangs.DemonSlayerAssembly();
        demon.enabled = false;        // OFF until user chooses it
        assemblyList.add(demon);

        // (Optional) remove jar auto-loading for now to keep things simple
        // If you want to re-add JAR loading later, we can bolt it back on.
    }

    /**
     * Merges all enabled custom instruction sets into the main instruction set.
     */
    public static void mergeCustomInstructions(ArrayList<BasicInstruction> instrList) {
        boolean mipsEnabled = false;
        finalInstructionList.clear();

        for (CustomAssembly c : assemblyList) {
            if (c.enabled) {
                c.addCustomInstructions(finalInstructionList);
                if (c instanceof MipsAssembly) {
                    mipsEnabled = true;
                }
            }
        }

        instrList.addAll(finalInstructionList);

        // If MIPS is enabled, also add pseudo-instructions (li, move, syscall, etc.)
        if (mipsEnabled) {
            Globals.instructionSet.addPseudoInstructions();
        }
    }
}
