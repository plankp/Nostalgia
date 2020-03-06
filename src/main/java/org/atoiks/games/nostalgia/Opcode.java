package org.atoiks.games.nostalgia;

public final class Opcode {

    private Opcode() {
        /* static helper classs */
    }

    // All opcodes must be masked to 0x3F since the instruction only has 6 bits
    // to encode this information.

    public static final int OP_MOV_I    = 0 & 0x3F;
    public static final int OP_ADD_R    = 1 & 0x3F;
    public static final int OP_SUB_R    = 2 & 0x3F;
    public static final int OP_AND_R    = 3 & 0x3F;
    public static final int OP_OR_R     = 4 & 0x3F;
    public static final int OP_XOR_R    = 5 & 0x3F;
    public static final int OP_NAND_R   = 6 & 0x3F;
    public static final int OP_NOR_R    = 7 & 0x3F;
    public static final int OP_NXOR_R   = 8 & 0x3F;
    public static final int OP_ADD_I    = 9 & 0x3F;
    public static final int OP_SUB_I    = 10 & 0x3F;
    public static final int OP_RSUB_I   = 11 & 0x3F;
    public static final int OP_JABS_Z   = 12 & 0x3F;
    public static final int OP_JABS_NZ  = 13 & 0x3F;
    public static final int OP_JABS_GE  = 14 & 0x3F;
    public static final int OP_JABS_GT  = 15 & 0x3F;
    public static final int OP_JABS_LE  = 16 & 0x3F;
    public static final int OP_JABS_LT  = 17 & 0x3F;
    public static final int OP_JREL_Z   = 18 & 0x3F;
    public static final int OP_JREL_NZ  = 19 & 0x3F;
    public static final int OP_JREL_GE  = 20 & 0x3F;
    public static final int OP_JREL_GT  = 21 & 0x3F;
    public static final int OP_JREL_LE  = 22 & 0x3F;
    public static final int OP_JREL_LT  = 23 & 0x3F;
    public static final int OP_PUSH     = 24 & 0x3F;
    public static final int OP_POP      = 25 & 0x3F;
    public static final int OP_MTSP     = 26 & 0x3F;
    public static final int OP_MTBP     = 27 & 0x3F;
    public static final int OP_MSPT     = 28 & 0x3F;
    public static final int OP_MBPT     = 29 & 0x3F;
    public static final int OP_CALL     = 30 & 0x3F;
    public static final int OP_RET      = 31 & 0x3F;
    public static final int OP_ENTER    = 32 & 0x3F;
    public static final int OP_LEAVE    = 33 & 0x3F;
    public static final int OP_INNER    = 34 & 0x3F;
    public static final int OP_OUTER    = 35 & 0x3F;
    public static final int OP_LD_W     = 36 & 0x3F;
    public static final int OP_ST_W     = 37 & 0x3F;
    public static final int OP_LD_B     = 38 & 0x3F;
    public static final int OP_ST_B     = 39 & 0x3F;
    public static final int OP_SHL_R    = 40 & 0x3F;
    public static final int OP_SHR_R    = 41 & 0x3F;
    public static final int OP_SAR_R    = 42 & 0x3F;
    public static final int OP_SFT_I    = 43 & 0x3F;
}