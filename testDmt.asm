.data
prompt1:    .asciiz "Enter first number: "
prompt2:    .asciiz "Enter second number: "
msgResult:  .asciiz "Result after water.first + sun.blaze: "
newline:    .asciiz "\n"

.text
.globl main

main:
    # ----- read first integer into $t1 -----
    li      $v0, 4
    la      $a0, prompt1
    syscall

    li      $v0, 5          # read int
    syscall
    move    $t1, $v0

    # ----- read second integer into $t2 -----
    li      $v0, 4
    la      $a0, prompt2
    syscall

    li      $v0, 5
    syscall
    move    $t2, $v0

    # ----- Demon Slayer ops -----
    water.first $t3, $t1, $t2     # $t3 = $t1 + $t2
    sun.blaze  $t3, 5             # $t3 = $t3 + 5   (in-place add imm)

    # ----- print message -----
    li      $v0, 4
    la      $a0, msgResult
    syscall

    # print result in $t3
    move    $a0, $t3
    li      $v0, 1
    syscall

    # newline
    li      $v0, 4
    la      $a0, newline
    syscall

    # exit
    li      $v0, 10
    syscall
