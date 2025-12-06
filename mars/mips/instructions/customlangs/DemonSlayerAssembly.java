package mars.mips.instructions.customlangs;

import mars.*;
import mars.util.*;
import mars.mips.hardware.*;
import mars.mips.instructions.*;

/**
 * Demon Slayer Assembly – playful custom ISA ops for MARS.
 * All instructions are purely software-side effects on registers/memory.
 * No hidden performance effects; any "mode" is tracked by static flags here.
 */


public class DemonSlayerAssembly extends CustomAssembly {

      // 🔹 NEW: enable this language by default
    public DemonSlayerAssembly() {
        this.enabled = true;
    }


    // lightweight “status” for a couple of ops
    private static boolean breathOn = false;
    private static int     markCountdown = 0; // decremented by an explicit helper op

    @Override
    public String getName() {
        return "Demon Slayer Assembly";
    }

    @Override
    public String getDescription() {
        return "Anime-flavored MIPS-style ops: breath, mark, thunder-skip, secure box/unbox, etc.";
    }

    @Override
    protected void populate() {

        // 1) BREATH.ON  — rd <- rs*2, set breath flag
        instructionList.add(new BasicInstruction(
            "breath.on $t1,$t2",
            "Breathing style on: set flag; ($t1) = 2*($t2).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss 00000 00000 010001", // op=0, funct=0x11 (arbitrary here)
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int rs   = RegisterFile.getValue(op[1]);
                    RegisterFile.updateRegister(op[0], rs << 1);
                    breathOn = true;
                }
            }
        ));

        // 2) BREATH.OFF — clear flag
        instructionList.add(new BasicInstruction(
            "breath.off",
            "Breathing style off: clear flag.",
            BasicInstructionFormat.R_FORMAT,
            "000000 00000 00000 00000 00000 010010", // funct=0x12
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    breathOn = false;
                }
            }
        ));

        // 3) THUNDER.CLAP — rd = rs+rt; if result==0, skip next instruction (branch +4)
        instructionList.add(new BasicInstruction(
            "thunder.clap $t1,$t2,$t3",
            "Fused add-and-dash: ($t1)=($t2)+($t3); if zero then skip next instr.",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ddddd 00000 010011", // funct=0x13
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int a = RegisterFile.getValue(op[1]);
                    int b = RegisterFile.getValue(op[2]);
                    int r = a + b;
                    RegisterFile.updateRegister(op[0], r);
                    if (r == 0) {
                        // skip one instruction (branch relative +1)
                        Globals.instructionSet.processBranch(1);
                    }
                }
            }
        ));

        // 4) WATER.WHEEL — rotate left the 32-bit word at MEM[base] by (len & 7) and put into rd
        instructionList.add(new BasicInstruction(
            "water.wheel $t1,0($t2),$t3",
            "Rotate-left the word at memory[($t2)+0] by ($t3 & 7) bits into ($t1).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss ddddd 00000 010100", // funct=0x14, rs=base, rt=len, rd=dest
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int base = RegisterFile.getValue(op[1]);
                    int len  = RegisterFile.getValue(op[2]) & 7;
                    try {
                        int w = Globals.memory.getWord(base);
                        int rl = (w << len) | (w >>> (32 - len));
                        RegisterFile.updateRegister(op[0], rl);
                    } catch (AddressErrorException e) {
                        throw new ProcessingException(st, e);
                    }
                }
            }
        ));

        // 5) SUN.BLAZE — saturating add immediate (I-format shape)
        instructionList.add(new BasicInstruction(
            "sun.blaze $t1,$t2,100",
            "Saturating addi: ($t1) = sat( ($t2) + imm ).",
            BasicInstructionFormat.I_FORMAT,
            "001101 fffff sssss ssssssssssssssss", // opcode 0x0D here for demo
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int rs = RegisterFile.getValue(op[1]);
                    int imm = op[2] << 16 >> 16; // sign-extend 16-bit
                    long sum = (long)rs + (long)imm;
                    if (sum > Integer.MAX_VALUE) sum = Integer.MAX_VALUE;
                    if (sum < Integer.MIN_VALUE) sum = Integer.MIN_VALUE;
                    RegisterFile.updateRegister(op[0], (int)sum);
                }
            }
        ));

        // 6) NEZUKO.BOX — store XOR-masked
        instructionList.add(new BasicInstruction(
            "nezuko.box $t1,0($t2)",
            "Secure store: MEM[($t2)+0] = ($t1) ^ 0xA5A5A5A5.",
            BasicInstructionFormat.I_FORMAT,
            "101011 fffff sssss ssssssssssssssss", // like SW (0x2B), we’ll just simulate special behavior
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int rt = RegisterFile.getValue(op[0]);
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

        // 7) NEZUKO.UNBOX — load and XOR-unmask
        instructionList.add(new BasicInstruction(
            "nezuko.unbox $t1,0($t2)",
            "Secure load: ($t1) = MEM[($t2)+0] ^ 0xA5A5A5A5.",
            BasicInstructionFormat.I_FORMAT,
            "100011 fffff sssss ssssssssssssssss", // like LW (0x23)
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

        // 8) DEMON.MARK — start a countdown (explicit)
        instructionList.add(new BasicInstruction(
            "demon.mark 1000",
            "Enable mark countdown by imm (decrement via mark.step).",
            BasicInstructionFormat.I_FORMAT,
            "001000 00000 00000 ssssssssssssssss", // like ADDI $zero,$zero,imm (we just use imm)
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    markCountdown = (op[2] << 16 >> 16);
                }
            }
        ));

        // 9) MARK.STEP — decrement the mark (lets you model timeboxed bursts)
        instructionList.add(new BasicInstruction(
            "mark.step $t1",
            "Decrement mark; ($t1) = remaining (>=0).",
            BasicInstructionFormat.R_FORMAT,
            "000000 00000 00000 ddddd 00000 010101", // funct=0x15
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    if (markCountdown > 0) markCountdown--;
                    RegisterFile.updateRegister(op[0], markCountdown);
                }
            }
        ));

        // 10) TOTAL.CONCENTRATION — cooperative wait N ms from rs&0xFFFF
        instructionList.add(new BasicInstruction(
            "total.concentration $t1",
            "Sleep for ($t1 & 0xFFFF) milliseconds (cooperative wait).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff 00000 00000 00000 010110", // funct=0x16
            new SimulationCode() {
                public void simulate(ProgramStatement st) throws ProcessingException {
                    int[] op = st.getOperands();
                    int ms = RegisterFile.getValue(op[0]) & 0xFFFF;
                    try {
                        Thread.sleep(ms);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        ));

        // Bonus) KATANA.SWAP — swap rs and rt (handy utility)
        instructionList.add(new BasicInstruction(
            "katana.swap $t1,$t2",
            "Swap ($t1) and ($t2).",
            BasicInstructionFormat.R_FORMAT,
            "000000 fffff sssss 00000 00000 010111", // funct=0x17
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
    }
}