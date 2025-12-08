package mars.mips.instructions.customlangs;

import mars.*;
import mars.util.*;
import mars.mips.hardware.*;
import mars.mips.instructions.*;

/**
 * Demon Slayer Assembly
 * Custom instruction set themed around breathing styles:
 * Water, Sun, Thunder, Beast, plus some Nezuko box instructions.
 *
 * All instructions operate on the normal MARS register file and memory.
 */
public class DemonSlayerAssembly extends CustomAssembly {

    private static boolean breathOn = false;

    @Override
    public String getName() {
        return "Demon Slayer Assembly";
    }

    @Override
    public String getDescription() {
        return "Anime-flavored MIPS-like instructions: Water/Sun/Thunder/Beast breathing styles.";
    }

    @Override
    protected void populate() {

        // ============================================================
        //  1–4: Core Breathing Control
        // ============================================================

        // 1) BREATH.ON  — enable breathing mode
        instructionList.add(new BasicInstruction(
            "breath.on $t1,$t2",
            "Enable breathing: ($t1) = 2*($t2), breath flag set.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss 00000 00000 010001",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int src  = RegisterFile.getValue(op[1]);
                    RegisterFile.updateRegister(op[0], src << 1);
                    breathOn = true;
                }
            }
        ));

        // 2) BREATH.OFF — disable breathing mode
        instructionList.add(new BasicInstruction(
            "breath.off",
            "Disable breathing mode (breath flag cleared).",
            BasicInstructionFormat.R_FORMAT,
            "000000 00000 00000 00000 00000 010010",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    breathOn = false;
                }
            }
        ));

        // 3) BREATH.FOCUS — if breathing on, boost register
        instructionList.add(new BasicInstruction(
            "breath.focus $t1",
            "If breathing is on, ($t1) = 3*($t1); otherwise unchanged.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff 00000 00000 00000 010011",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int val = RegisterFile.getValue(op[0]);
                    if (breathOn) {
                        RegisterFile.updateRegister(op[0], val * 3);
                    }
                }
            }
        ));

        // 4) BREATH.RESET — clear a breathing register
        instructionList.add(new BasicInstruction(
            "breath.reset $t1",
            "Reset ($t1) to 0.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff 00000 00000 00000 010100",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    RegisterFile.updateRegister(op[0], 0);
                }
            }
        ));

        // ============================================================
        //  5–10: Water Breathing techniques
        // ============================================================

        // 5) WATER.FIRST  — flowing slash: add
        instructionList.add(new BasicInstruction(
            "water.first $t1,$t2,$t3",
            "Water First Form: ($t1) = ($t2) + ($t3).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 010101",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], a + b);
                }
            }
        ));

        // 6) WATER.SECOND — water surface slash: subtract
        instructionList.add(new BasicInstruction(
            "water.second $t1,$t2,$t3",
            "Water Second Form: ($t1) = ($t2) - ($t3).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 010110",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], a - b);
                }
            }
        ));

        // 7) WATER.THIRD — water wheel: multiply
        instructionList.add(new BasicInstruction(
            "water.third $t1,$t2,$t3",
            "Water Third Form: ($t1) = ($t2) * ($t3).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 010111",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], a * b);
                }
            }
        ));

        // 8) WATER.FOURTH — calm stream: average
        instructionList.add(new BasicInstruction(
            "water.fourth $t1,$t2,$t3",
            "Water Fourth Form: ($t1) = (($t2)+($t3))/2.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 011000",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], (a + b) / 2);
                }
            }
        ));

        // 9) WATER.WHEEL — rotate-left word in memory
        instructionList.add(new BasicInstruction(
            "water.wheel $t1,0($t2),$t3",
            "Rotate-left MEM[($t2)+0] by ($t3 & 7) bits into ($t1).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 011001",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int base = RegisterFile.getValue(op[1]);
                    int shift = RegisterFile.getValue(op[2]) & 7;
                    try {
                        int w = Globals.memory.getWord(base);
                        int rl = (w << shift) | (w >>> (32 - shift));
                        RegisterFile.updateRegister(op[0], rl);
                    } catch (AddressErrorException e) {
                        throw new ProcessingException(st, e);
                    }
                }
            }
        ));

        // 10) WATER.RIPPLE — add half immediate
      instructionList.add(new BasicInstruction(
    "water.ripple $t1,100",
    "Water Ripple: ($t1) = ($t1) + (imm / 2).",
    BasicInstructionFormat.I_FORMAT,
    "001000 fffff 00000 ssssssssssssssss",
    new SimulationCode() {
        public void simulate(ProgramStatement st) throws ProcessingException {
            int[] op = st.getOperands();
            int src = RegisterFile.getValue(op[0]);
            int imm = op[1] << 16 >> 16;
            RegisterFile.updateRegister(op[0], src + imm / 2);
        }
    }
));

        // ============================================================
        //  11–16: Sun Breathing techniques
        // ============================================================

        // 11) SUN.FIRST — blazing slash: OR
        instructionList.add(new BasicInstruction(
            "sun.first $t1,$t2,$t3",
            "Sun First Form: ($t1) = ($t2) | ($t3).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 011010",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], a | b);
                }
            }
        ));

        // 12) SUN.SECOND — harsh light: AND
        instructionList.add(new BasicInstruction(
            "sun.second $t1,$t2,$t3",
            "Sun Second Form: ($t1) = ($t2) & ($t3).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 011011",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], a & b);
                }
            }
        ));

        // 13) SUN.THIRD — solar spark: XOR
        instructionList.add(new BasicInstruction(
            "sun.third $t1,$t2,$t3",
            "Sun Third Form: ($t1) = ($t2) ^ ($t3).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 011100",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], a ^ b);
                }
            }
        ));

        // 14) SUN.HEAL — clamp to non-negative
        instructionList.add(new BasicInstruction(
            "sun.heal $t1",
            "Sun Heal: if ($t1) < 0 then set to 0.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff 00000 00000 00000 011101",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int v = RegisterFile.getValue(op[0]);
                    if (v < 0) v = 0;
                    RegisterFile.updateRegister(op[0], v);
                }
            }
        ));

// 15) SUN.BLAZE — saturating add immediate (in-place)
instructionList.add(new BasicInstruction(
    "sun.blaze $t1,100",
    "Sun Blaze: ($t1) = sat( ($t1) + imm ).",
    BasicInstructionFormat.I_FORMAT,
    "001101 fffff 00000 ssssssssssssssss",
    new SimulationCode() {
        public void simulate(ProgramStatement st) throws ProcessingException {
            int[] op = st.getOperands();
            int src = RegisterFile.getValue(op[0]);       // same register as dest
            int imm = op[1] << 16 >> 16;                  // sign-extended imm
            long sum = (long) src + (long) imm;
            if (sum > Integer.MAX_VALUE) sum = Integer.MAX_VALUE;
            if (sum < Integer.MIN_VALUE) sum = Integer.MIN_VALUE;
            RegisterFile.updateRegister(op[0], (int) sum);
        }
    }
));

        // 16) SUN.RISING — shift-left by other reg
        instructionList.add(new BasicInstruction(
            "sun.rising $t1,$t2,$t3",
            "Sun Rising: ($t1) = ($t2) << ( ($t3) & 31 ).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 011110",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int val = RegisterFile.getValue(op[1]);
                    int sh  = RegisterFile.getValue(op[2]) & 31;
                    RegisterFile.updateRegister(op[0], val << sh);
                }
            }
        ));

        // ============================================================
        //  17–22: Thunder Breathing techniques
        // ============================================================

        // 17) THUNDER.CLAP — add, skip next if zero
        instructionList.add(new BasicInstruction(
            "thunder.clap $t1,$t2,$t3",
            "Thunder Clap: ($t1) = ($t2)+($t3); if result==0, skip next instr.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 011111",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    int r = a + b;
                    RegisterFile.updateRegister(op[0], r);
                    if (r == 0) {
                        Globals.instructionSet.processBranch(1); // skip 1 instruction
                    }
                }
            }
        ));

        // 18) THUNDER.DASH — branch if positive
        instructionList.add(new BasicInstruction(
            "thunder.dash $t1,label",
            "Thunder Dash: if ($t1) > 0, branch to label.",
            BasicInstructionFormat.I_BRANCH_FORMAT,
            "000100 fffff 00000 ssssssssssssssss",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    if (RegisterFile.getValue(op[0]) > 0) {
                        Globals.instructionSet.processBranch(op[1]);
                    }
                }
            }
        ));

        // 19) THUNDER.CHARGE — double-immediate add
       instructionList.add(new BasicInstruction(
    "thunder.charge $t1,100",
    "Thunder Charge: ($t1) = ($t1) + 2*imm.",
    BasicInstructionFormat.I_FORMAT,
    "001001 fffff 00000 ssssssssssssssss",
    new SimulationCode() {
        public void simulate(ProgramStatement st) throws ProcessingException {
            int[] op = st.getOperands();
            int src = RegisterFile.getValue(op[0]);
            int imm = op[1] << 16 >> 16;
            RegisterFile.updateRegister(op[0], src + 2 * imm);
        }
    }
));
        // 20) THUNDER.DISCHARGE — zero out
        instructionList.add(new BasicInstruction(
            "thunder.discharge $t1",
            "Thunder Discharge: set ($t1) = 0.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff 00000 00000 00000 100000",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    RegisterFile.updateRegister(op[0], 0);
                }
            }
        ));

        // 21) THUNDER.CHAIN — double-sum
        instructionList.add(new BasicInstruction(
            "thunder.chain $t1,$t2,$t3",
            "Thunder Chain: ($t1) = 2 * ( ($t2)+($t3) ).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 100001",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    RegisterFile.updateRegister(op[0], 2 * (a + b));
                }
            }
        ));

        // 22) THUNDER.FLASH — logical shift right
        instructionList.add(new BasicInstruction(
            "thunder.flash $t1,$t2,$t3",
            "Thunder Flash: ($t1) = ($t2) >>> ( ($t3) & 31 ).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 100010",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int val = RegisterFile.getValue(op[1]);
                    int sh  = RegisterFile.getValue(op[2]) & 31;
                    RegisterFile.updateRegister(op[0], val >>> sh);
                }
            }
        ));

        // ============================================================
        //  23–28: Beast Breathing techniques
        // ============================================================

        // 23) BEAST.FANG — absolute difference
        instructionList.add(new BasicInstruction(
            "beast.fang $t1,$t2,$t3",
            "Beast Fang: ($t1) = abs( ($t2) - ($t3) ).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ttttt 00000 100011",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    int diff = a - b;
                    if (diff < 0) diff = -diff;
                    RegisterFile.updateRegister(op[0], diff);
                }
            }
        ));

        // 24) BEAST.RIP — XOR with immediate
     instructionList.add(new BasicInstruction(
    "beast.rip $t1,100",
    "Beast Rip: ($t1) = ($t1) ^ imm.",
    BasicInstructionFormat.I_FORMAT,
    "001010 fffff 00000 ssssssssssssssss",
    new SimulationCode() {
        public void simulate(ProgramStatement st) throws ProcessingException {
            int[] op = st.getOperands();
            int src = RegisterFile.getValue(op[0]);
            int imm = op[1] << 16 >> 16;
            RegisterFile.updateRegister(op[0], src ^ imm);
        }
    }
));

        // 25) BEAST.DUAL — swap two registers
        instructionList.add(new BasicInstruction(
            "beast.dual $t1,$t2",
            "Beast Dual: swap ($t1) and ($t2).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss 00000 00000 100100",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[0]);
                    int b = RegisterFile.getValue(op[1]);
                    RegisterFile.updateRegister(op[0], b);
                    RegisterFile.updateRegister(op[1], a);
                }
            }
        ));

        // 26) BEAST.SENSE — sign detection
        instructionList.add(new BasicInstruction(
            "beast.sense $t1,$t2",
            "Beast Sense: ($t1) = sign($t2) (-1,0,1).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss 00000 00000 100101",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int v = RegisterFile.getValue(op[1]);
                    int sign = 0;
                    if (v > 0) sign = 1;
                    else if (v < 0) sign = -1;
                    RegisterFile.updateRegister(op[0], sign);
                }
            }
        ));

        // 27) BEAST.RAMPAGE — triple strength
        instructionList.add(new BasicInstruction(
            "beast.rampage $t1",
            "Beast Rampage: ($t1) = 3*($t1).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff 00000 00000 00000 100110",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int v = RegisterFile.getValue(op[0]);
                    RegisterFile.updateRegister(op[0], v * 3);
                }
            }
        ));

        // 28) BEAST.GUARD — clamp negative to 0
        instructionList.add(new BasicInstruction(
            "beast.guard $t1",
            "Beast Guard: if ($t1) < 0 then set to 0 (like a guard stance).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff 00000 00000 00000 100111",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int v = RegisterFile.getValue(op[0]);
                    if (v < 0) v = 0;
                    RegisterFile.updateRegister(op[0], v);
                }
            }
        ));

        // ============================================================
        //  29–30: Nezuko Box (secure memory)
        // ============================================================

        // 29) NEZUKO.BOX — store XOR-masked
        instructionList.add(new BasicInstruction(
            "nezuko.box $t1,0($t2)",
            "Nezuko Box: MEM[($t2)+0] = ($t1) ^ 0xA5A5A5A5.",
            BasicInstructionFormat.I_FORMAT,
            "101011 fffff sssss ssssssssssssssss",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int rt   = RegisterFile.getValue(op[0]);
                    int base = RegisterFile.getValue(op[1]);
                    int imm  = op[2] << 16 >> 16;
                    int addr = base + imm;
                    int masked = rt ^ 0xA5A5A5A5;
                    try {
                        Globals.memory.setWord(addr, masked);
                    } catch (AddressErrorException e) {
                        throw new ProcessingException(st, e);
                    }
                }
            }
        ));

        // 30) NEZUKO.UNBOX — load XOR-unmasked
        instructionList.add(new BasicInstruction(
            "nezuko.unbox $t1,0($t2)",
            "Nezuko Unbox: ($t1) = MEM[($t2)+0] ^ 0xA5A5A5A5.",
            BasicInstructionFormat.I_FORMAT,
            "100011 fffff sssss ssssssssssssssss",
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int base = RegisterFile.getValue(op[1]);
                    int imm  = op[2] << 16 >> 16;
                    int addr = base + imm;
                    try {
                        int w = Globals.memory.getWord(addr);
                        RegisterFile.updateRegister(op[0], w ^ 0xA5A5A5A5);
                    } catch (AddressErrorException e) {
                        throw new ProcessingException(st, e);
                    }
                }
            }
        ));

        // =========================
        // End of custom instructions
        // =========================
    }
}
