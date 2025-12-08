############################################################
# demon_game_vs_cpu.asm - Demon Slayer Battle (Player vs CPU)
# Requires: DemonSlayerAssembly custom instruction set
#
# Player HP:
#   $s0 = P1_HP (human)
#   $s1 = CPU_HP
#
# P1 character choice: $s2
# CPU character choice: $s3 (random 1..4)
############################################################

.data
newline:            .asciiz "\n"
menu_title:         .asciiz "=== Demon Slayer Battle (Player vs Computer) ===\n"

char_menu:          .asciiz "Choose your character:\n1) Tanjiro\n2) Nezuko\n3) Zenitsu\n4) Inosuke\n> "
p1_prompt_txt:      .asciiz "Player, select your character.\n"
cpu_char_txt:       .asciiz "Computer's character (1-4): "
p1_turn_txt:        .asciiz "\n--- Your Turn ---\n"
cpu_turn_txt:       .asciiz "\n--- Computer's Turn ---\n"

hp_status_txt:      .asciiz "\nHP: You="
hp_mid_txt:         .asciiz "  CPU="
tech_used_txt:      .asciiz "Uses technique: "

tech_choice_prompt: .asciiz "Choose technique slot (0-9): "

p1_win_txt:         .asciiz "\n*** You WIN! ***\n"
cpu_win_txt:        .asciiz "\n*** Computer WINS! ***\n"

# Technique name table for 10 techniques
tech0_name: .asciiz "Breath On"
tech1_name: .asciiz "Breath Focus"
tech2_name: .asciiz "Water First"
tech3_name: .asciiz "Water Second"
tech4_name: .asciiz "Water Third"
tech5_name: .asciiz "Sun First"
tech6_name: .asciiz "Sun Third"
tech7_name: .asciiz "Thunder Clap"
tech8_name: .asciiz "Beast Fang"
tech9_name: .asciiz "Beast Rampage"

tech_name_table:
    .word tech0_name
    .word tech1_name
    .word tech2_name
    .word tech3_name
    .word tech4_name
    .word tech5_name
    .word tech6_name
    .word tech7_name
    .word tech8_name
    .word tech9_name

# 10-slot move decks (can be changed/shuffled)
p1_moves:
    .word 0,1,2,3,4,5,6,7,8,9
p2_moves:
    .word 9,8,7,6,5,4,3,2,1,0

seed:       .word 123456789   # PRNG seed

.text
.globl main

############################################################
# main
############################################################
main:
    # Print title
    li   $v0, 4
    la   $a0, menu_title
    syscall

    # ---- Player character selection ----
    la   $a0, p1_prompt_txt
    li   $v0, 4
    syscall

    la   $a0, char_menu
    li   $v0, 4
    syscall

    li   $v0, 5          # read int
    syscall
    move $s2, $v0        # P1 character choice (1..4)

    # ---- Computer character selection (random 1..4) ----
    jal  rand_1_to_100   # random 1..100 in $v0
    move $t0, $v0
    li   $t1, 4
    div  $t0, $t1
    mfhi $t0             # remainder 0..3
    addi $s3, $t0, 1     # CPU character 1..4

    la   $a0, cpu_char_txt
    li   $v0, 4
    syscall

    move $a0, $s3        # print CPU char number
    li   $v0, 1
    syscall

    la   $a0, newline
    li   $v0, 4
    syscall

    # Initialize HP
    li   $s0, 100        # Player HP
    li   $s1, 100        # CPU HP

    # Initial breathing buff example
    li   $t0, 5
    li   $t1, 10
    breath.on $t0, $t1   # enable breathing mode

game_loop:
    # Print HP status
    jal  print_hp

    ####################################################
    # Player turn (choose technique slot 0-9)
    ####################################################
    la   $a0, p1_turn_txt
    li   $v0, 4
    syscall

    la   $a0, tech_choice_prompt
    li   $v0, 4
    syscall

    li   $v0, 5          # read int
    syscall
    move $t3, $v0        # raw slot
    li   $t4, 10
    div  $t3, $t4
    mfhi $t3             # t3 = slot 0..9 (mod 10)

    # Get moveID = p1_moves[slot]
    la   $t0, p1_moves
    sll  $t1, $t3, 2
    add  $t0, $t0, $t1
    lw   $t2, 0($t0)     # t2 = moveID 0..9

    move $a0, $t2
    jal  print_tech_name

    move $a0, $t2
    jal  perform_tech_p1 # damage in $v0

    # Apply damage to CPU
    sub  $s1, $s1, $v0
    bltz $s1, p1_wins
    beq  $s1, $zero, p1_wins

    ####################################################
    # Computer turn (random slot 0-9)
    ####################################################
    la   $a0, cpu_turn_txt
    li   $v0, 4
    syscall

    # random slot 0..9 from rand_1_to_100
    jal  rand_1_to_100
    move $t3, $v0
    li   $t4, 10
    div  $t3, $t4
    mfhi $t3             # t3 = slot 0..9

    # Get moveID = p2_moves[slot]
    la   $t0, p2_moves
    sll  $t1, $t3, 2
    add  $t0, $t0, $t1
    lw   $t2, 0($t0)     # t2 = moveID

    move $a0, $t2
    jal  print_tech_name

    move $a0, $t2
    jal  perform_tech_p2 # damage in $v0

    # Apply damage to Player
    sub  $s0, $s0, $v0
    bltz $s0, cpu_wins
    beq  $s0, $zero, cpu_wins

    # Next round
    j    game_loop

############################################################
# print_hp: prints "HP: You=xx  CPU=yy"
############################################################
print_hp:
    la   $a0, hp_status_txt
    li   $v0, 4
    syscall

    move $a0, $s0
    li   $v0, 1
    syscall

    la   $a0, hp_mid_txt
    li   $v0, 4
    syscall

    move $a0, $s1
    li   $v0, 1
    syscall

    la   $a0, newline
    li   $v0, 4
    syscall

    jr   $ra

############################################################
# print_tech_name(moveID in $a0)
############################################################
print_tech_name:
    la   $a1, tech_name_table
    sll  $t0, $a0, 2
    add  $a1, $a1, $t0
    lw   $a1, 0($a1)      # a1 = string address

    la   $a0, tech_used_txt
    li   $v0, 4
    syscall

    move $a0, $a1
    li   $v0, 4
    syscall

    la   $a0, newline
    li   $v0, 4
    syscall
    jr   $ra

############################################################
# perform_tech_p1(moveID in $a0) -> $v0 = damage (1..100)
# Demonstrates your custom instructions; final damage randomized.
############################################################
perform_tech_p1:
    move $t9, $ra         # save return

    li   $t0, 5
    li   $t1, 7

    beq  $a0, 0, p1_m0
    beq  $a0, 1, p1_m1
    beq  $a0, 2, p1_m2
    beq  $a0, 3, p1_m3
    beq  $a0, 4, p1_m4
    beq  $a0, 5, p1_m5
    beq  $a0, 6, p1_m6
    beq  $a0, 7, p1_m7
    beq  $a0, 8, p1_m8
    beq  $a0, 9, p1_m9
    j    p1_after_tech

p1_m0:  # breath.on
    breath.on $t0, $t1
    j p1_after_tech
p1_m1:  # breath.focus
    breath.focus $t0
    j p1_after_tech
p1_m2:
    water.first $t0,$t0,$t1
    j p1_after_tech
p1_m3:
    water.second $t0,$t1,$t0
    j p1_after_tech
p1_m4:
    water.third $t0,$t0,$t1
    j p1_after_tech
p1_m5:
    sun.first $t0,$t0,$t1
    j p1_after_tech
p1_m6:
    sun.third $t0,$t0,$t1
    j p1_after_tech
p1_m7:
    thunder.clap $t0,$t0,$t1
    j p1_after_tech
p1_m8:
    beast.fang $t0,$t0,$t1
    j p1_after_tech
p1_m9:
    beast.rampage $t0
    j p1_after_tech

p1_after_tech:
    # Random damage 1..100
    jal  rand_1_to_100
    move $ra, $t9
    jr   $ra

############################################################
# perform_tech_p2(moveID in $a0) -> $v0 = damage (1..100)
# CPU side; you can later customize per move/character.
############################################################
perform_tech_p2:
    move $t9, $ra

    li   $t0, 4
    li   $t1, 9
    # could branch on $a0 like P1, but for now just random dmg
    jal  rand_1_to_100

    move $ra, $t9
    jr   $ra

############################################################
# rand_1_to_100: LCG PRNG
############################################################
rand_1_to_100:
    la   $t0, seed
    lw   $t1, 0($t0)

    li   $t2, 1103515245
    mult $t1, $t2
    mflo $t1
    addiu $t1, $t1, 12345

    li   $t2, 0x7FFFFFFF
    and  $t1, $t1, $t2
    sw   $t1, 0($t0)

    li   $t2, 100
    div  $t1, $t2
    mfhi $t3
    addi $v0, $t3, 1      # 1..100
    jr   $ra

############################################################
# Win handlers
############################################################
p1_wins:
    la   $a0, p1_win_txt
    li   $v0, 4
    syscall
    j    game_exit

cpu_wins:
    la   $a0, cpu_win_txt
    li   $v0, 4
    syscall
    j    game_exit

game_exit:
    li   $v0, 10
    syscall
