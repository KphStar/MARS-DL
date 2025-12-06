############################################################
# Demon Slayer Assembly – test program
# Assumes custom instructions are integrated into InstructionSet.
#
# Watch registers ($t0–$t7) and memory at label `buf` in the MARS GUI.
############################################################

.data
buf:    .word   0x12345678      # test buffer for water.wheel and nezuko.* ops

.text
.globl main

main:
    ########################################################
    # 1) BREATH.ON / BREATH.OFF
    # breath.on $t1,$t2  => $t1 = 2 * $t2; breathOn = true
    ########################################################
    li      $t2, 10
    breath.on $t1,$t2          # expect $t1 = 20
    # breath flag now true (internal)

    breath.off                 # just clears the flag (no register change)

    ########################################################
    # 2) THUNDER.CLAP
    # thunder.clap $t1,$t2,$t3
    #   $t1 = $t2 + $t3;
    #   if result == 0, skip next instruction.
    ########################################################

    # Case 1: result == 0, so skip the next addi
    li      $t2, 5
    li      $t3, -5
    thunder.clap $t1,$t2,$t3   # $t1 = 0, skip the next instruction

    addi    $t0,$zero,111      # SHOULD BE SKIPPED
    addi    $t0,$zero,222      # SHOULD EXECUTE -> expect $t0 = 222

    # Case 2: result != 0, so do NOT skip next instruction
    li      $t2, 7
    li      $t3, 1
    thunder.clap $t1,$t2,$t3   # $t1 = 8, do not skip

    addi    $t0,$t0,1          # executes -> $t0 = 223

    ########################################################
    # 3) WATER.WHEEL
    # water.wheel $t1,0($t2),$t3
    #   $t1 = ROTL( word at [$t2], ($t3 & 7) bits )
    ########################################################

    la      $t2, buf           # base address
    li      $t3, 4             # rotate by 4 bits
    water.wheel $t1,0($t2),$t3 # read 0x12345678, rotate-left by 4 into $t1

    ########################################################
    # 4) SUN.BLAZE – saturating add
    # sun.blaze $t1,$t2,imm
    #   $t1 = sat( $t2 + imm )
    ########################################################

    # Normal case
    li      $t2, 1000
    sun.blaze $t1,$t2,100      # expect 1100

    # Overflow case – should clamp to +2^31-1
    li      $t2, 0x7FFFFFF0    # very close to max int
    sun.blaze $t1,$t2,100      # should saturate to 0x7FFFFFFF

    ########################################################
    # 5) NEZUKO.BOX / NEZUKO.UNBOX
    # nezuko.box   $t1,0($t2)  -> store $t1 ^ 0xA5A5A5A5
    # nezuko.unbox $t1,0($t2)  -> load and XOR again (original value)
    ########################################################

    li      $t1, 0xCAFEBABE    # secret payload
    la      $t2, buf

    nezuko.box   $t1,0($t2)    # store masked value into [buf]
    li      $t1, 0             # clear to prove load restores value
    nezuko.unbox $t1,0($t2)    # load -> expect $t1 = 0xCAFEBABE

    ########################################################
    # 6) DEMON.MARK / MARK.STEP
    # demon.mark imm    -> markCountdown = imm
    # mark.step $t1     -> decrement if >0; $t1 = remaining
    ########################################################

    demon.mark 5               # start countdown at 5

mark_loop:
    mark.step $t4              # $t4 = current countdown
    # after 5 times: $t4 should reach 0 and stay 0
    bgtz    $t4, mark_loop

    ########################################################
    # 7) TOTAL.CONCENTRATION
    # total.concentration $t1 -> sleep ($t1 & 0xFFFF) ms
    ########################################################

    li      $t1, 200           # 200 ms sleep
    total.concentration $t1    # cooperative pause; you'll see an execution delay

    ########################################################
    # 8) KATANA.SWAP
    # katana.swap $t1,$t2 -> swap values
    ########################################################

    li      $t1, 123
    li      $t2, 999
    katana.swap $t1,$t2        # after this: $t1 = 999, $t2 = 123

    ########################################################
    # Exit program
    ########################################################
    li      $v0, 10
    syscall
