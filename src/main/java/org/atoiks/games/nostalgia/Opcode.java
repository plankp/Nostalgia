package org.atoiks.games.nostalgia;

public final class Opcode {

    private Opcode() {
        /* static helper classs */
    }

    // All OP0 opcodes must be masked to 0x3F since the instruction only has 6
    // bits to encode this information:
    //
    // Each OP0 opcode must have one of the following formats:
    //
    // - Lower immediates:
    //   0xxx xxxi iiii iiii
    //   Note: only the last 6 bits of HI12 opcode (see below) is used
    //
    // - One register:
    //   0xxx xxxi iiii iaaa
    //   Note: only the last 9 bits of HI12 opcode (see below) is used
    //
    // - Two registers:
    //   0xxx xxxi iibb baaa
    //   Note: only the last 12 bits of HI12 opcode (see below) is used
    //
    // - Three registers:
    //   0xxx xxxc ccbb baaa

    public static final int OP0_MOV_I   = 0 & 0x3F;
    public static final int OP0_ADD_R   = 1 & 0x3F;
    public static final int OP0_SUB_R   = 2 & 0x3F;
    public static final int OP0_AND_R   = 3 & 0x3F;
    public static final int OP0_OR_R    = 4 & 0x3F;
    public static final int OP0_XOR_R   = 5 & 0x3F;
    public static final int OP0_NAND_R  = 6 & 0x3F;
    public static final int OP0_NOR_R   = 7 & 0x3F;
    public static final int OP0_NXOR_R  = 8 & 0x3F;
    public static final int OP0_ADD_I   = 9 & 0x3F;
    public static final int OP0_SUB_I   = 10 & 0x3F;
    public static final int OP0_RSUB_I  = 11 & 0x3F;
    public static final int OP0_JABS_Z  = 12 & 0x3F;
    public static final int OP0_JABS_NZ = 13 & 0x3F;
    public static final int OP0_JABS_GE = 14 & 0x3F;
    public static final int OP0_JABS_GT = 15 & 0x3F;
    public static final int OP0_JABS_LE = 16 & 0x3F;
    public static final int OP0_JABS_LT = 17 & 0x3F;
    public static final int OP0_JREL_Z  = 18 & 0x3F;
    public static final int OP0_JREL_NZ = 19 & 0x3F;
    public static final int OP0_JREL_GE = 20 & 0x3F;
    public static final int OP0_JREL_GT = 21 & 0x3F;
    public static final int OP0_JREL_LE = 22 & 0x3F;
    public static final int OP0_JREL_LT = 23 & 0x3F;
    public static final int OP0_PUSH    = 24 & 0x3F;
    public static final int OP0_POP     = 25 & 0x3F;
    public static final int OP0_MTSP    = 26 & 0x3F;
    public static final int OP0_MTBP    = 27 & 0x3F;
    public static final int OP0_MSPT    = 28 & 0x3F;
    public static final int OP0_MBPT    = 29 & 0x3F;
    public static final int OP0_CALL    = 30 & 0x3F;
    public static final int OP0_RET     = 31 & 0x3F;
    public static final int OP0_ENTER   = 32 & 0x3F;
    public static final int OP0_LEAVE   = 33 & 0x3F;
    public static final int OP0_INNER   = 34 & 0x3F;
    public static final int OP0_OUTER   = 35 & 0x3F;
    public static final int OP0_LD_W    = 36 & 0x3F;
    public static final int OP0_ST_W    = 37 & 0x3F;
    public static final int OP0_LD_B    = 38 & 0x3F;
    public static final int OP0_ST_B    = 39 & 0x3F;
    public static final int OP0_SHL_R   = 40 & 0x3F;
    public static final int OP0_SHR_R   = 41 & 0x3F;
    public static final int OP0_SAR_R   = 42 & 0x3F;
    public static final int OP0_SFT_I   = 43 & 0x3F;
    public static final int OP0_PUSH3   = 44 & 0x3F;
    public static final int OP0_POP3    = 45 & 0x3F;
    public static final int OP0_CMOV_I  = 46 & 0x3F;
    public static final int OP0_CMOV_R  = 47 & 0x3F;

    // All OP1 opcodes must be masked to 0x07 since the instruction only has 3
    // bits to encode this information.
    //
    // Each OP1 opcode must have one of the following formats:
    //
    // - Lower immediates:
    //   1xxx iiii iiii iiii
    //
    // (more to come!)

    public static final int OP1_HI12    = 0 & 0x07;
}