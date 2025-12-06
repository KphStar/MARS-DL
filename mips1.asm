.data
msg:    .asciiz "Hello from MARS!\n"

.text
.globl main
main:
    li $v0, 4          # print string
    la $a0, msg
    syscall

    li $v0, 10         # exit
    syscall
