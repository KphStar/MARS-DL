# DMT.asm
# LANGUAGE: Demon Slayer Assembly  (comment only – select in GUI)

        .text
        .globl main

main:
        # -------------------------------
        # 1) Base MIPS setup
        # -------------------------------
        li      $t1,10          # t1 = 10
        li      $t2,5           # t2 = 5

        # -------------------------------
        # 2) Water Breathing
        # -------------------------------
        # water.first  $t0,$t1,$t2  => t0 = 10 + 5 = 15
        water.first  $t0,$t1,$t2

        # water.second $t3,$t0,$t1 => t3 = 15 - 10 = 5
        water.second $t3,$t0,$t1

        # -------------------------------
        # 3) Sun Breathing (in-place imm)
        # -------------------------------
        # you defined: sun.blaze $t1,100
        # semantics:  t1 = sat(t1 + imm)
        move    $t4,$t3          # t4 = 5
        sun.blaze  $t4,5         # t4 = sat(5 + 5) = 10

        # -------------------------------
        # 4) Beast Breathing
        # -------------------------------
        # beast.fang $t5,$t4,$t1 => t5 = abs(10 - 10) = 0
        beast.fang $t5,$t4,$t1

        # -------------------------------
        # 5) Print results with syscalls
        # -------------------------------
        # print t0 (15)
        li      $v0,1
        move    $a0,$t0
        syscall

        # newline
        li      $v0,11
        li      $a0,'\n'
        syscall

        # print t3 (5)
        li      $v0,1
        move    $a0,$t3
        syscall

        # newline
        li      $v0,11
        li      $a0,'\n'
        syscall

        # print t4 (10)
        li      $v0,1
        move    $a0,$t4
        syscall

        # newline
        li      $v0,11
        li      $a0,'\n'
        syscall

        # print t5 (0)
        li      $v0,1
        move    $a0,$t5
        syscall

        # exit
        li      $v0,10
        syscall
