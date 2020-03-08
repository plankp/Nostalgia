package org.atoiks.games.nostalgia;

public final class Opcode {

    private Opcode() {
        /* static helper classs */
    }

    public static final int MASK_OP0 = 0x3F;
    public static final int MASK_OP1 = 0x07;

    // All OP0 opcodes must be masked to MASK_OP0!
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

    public static final int OP0_MOV_I   = 0 & MASK_OP0;
    public static final int OP0_ADD_R   = 1 & MASK_OP0;
    public static final int OP0_SUB_R   = 2 & MASK_OP0;
    public static final int OP0_AND_R   = 3 & MASK_OP0;
    public static final int OP0_OR_R    = 4 & MASK_OP0;
    public static final int OP0_XOR_R   = 5 & MASK_OP0;
    public static final int OP0_NAND_R  = 6 & MASK_OP0;
    public static final int OP0_NOR_R   = 7 & MASK_OP0;
    public static final int OP0_NXOR_R  = 8 & MASK_OP0;
    public static final int OP0_ADD_I   = 9 & MASK_OP0;
    public static final int OP0_SUB_I   = 10 & MASK_OP0;
    public static final int OP0_RSUB_I  = 11 & MASK_OP0;
    public static final int OP0_JABS_Z  = 12 & MASK_OP0;
    public static final int OP0_JABS_NZ = 13 & MASK_OP0;
    public static final int OP0_JABS_GE = 14 & MASK_OP0;
    public static final int OP0_JABS_GT = 15 & MASK_OP0;
    public static final int OP0_JABS_LE = 16 & MASK_OP0;
    public static final int OP0_JABS_LT = 17 & MASK_OP0;
    public static final int OP0_JREL_Z  = 18 & MASK_OP0;
    public static final int OP0_JREL_NZ = 19 & MASK_OP0;
    public static final int OP0_JREL_GE = 20 & MASK_OP0;
    public static final int OP0_JREL_GT = 21 & MASK_OP0;
    public static final int OP0_JREL_LE = 22 & MASK_OP0;
    public static final int OP0_JREL_LT = 23 & MASK_OP0;
    public static final int OP0_PUSH    = 24 & MASK_OP0;
    public static final int OP0_POP     = 25 & MASK_OP0;
    // Skipped 4 slots:
    //   26 & MASK_OP0;
    //   27 & MASK_OP0;
    //   28 & MASK_OP0;
    //   29 & MASK_OP0;
    public static final int OP0_CALL    = 30 & MASK_OP0;
    public static final int OP0_RET     = 31 & MASK_OP0;
    public static final int OP0_ENTER   = 32 & MASK_OP0;
    public static final int OP0_LEAVE   = 33 & MASK_OP0;
    // Skipped 2 slots:
    //   34 & MASK_OP0;
    //   35 & MASK_OP0;
    public static final int OP0_LD_W    = 36 & MASK_OP0;
    public static final int OP0_ST_W    = 37 & MASK_OP0;
    public static final int OP0_LD_B    = 38 & MASK_OP0;
    public static final int OP0_ST_B    = 39 & MASK_OP0;
    public static final int OP0_SHL_R   = 40 & MASK_OP0;
    public static final int OP0_SHR_R   = 41 & MASK_OP0;
    public static final int OP0_SAR_R   = 42 & MASK_OP0;
    public static final int OP0_SFT_I   = 43 & MASK_OP0;
    public static final int OP0_PUSH3   = 44 & MASK_OP0;
    public static final int OP0_POP3    = 45 & MASK_OP0;
    public static final int OP0_CMOV_I  = 46 & MASK_OP0;
    public static final int OP0_CMOV_R  = 47 & MASK_OP0;

    // All OP1 opcodes must be masked to MASK_OP1!
    //
    // Each OP1 opcode must have one of the following formats:
    //
    // - Lower immediates:
    //   1xxx iiii iiii iiii
    //
    // - Four registers:
    //   1xxx dddc ccbb baaa
    //
    // (more to come!)

    public static final int OP1_IEX     = 0 & MASK_OP1;
    public static final int OP1_MUL     = 1 & MASK_OP1;
    public static final int OP1_DIV     = 2 & MASK_OP1;
    public static final int OP1_REX     = 3 & MASK_OP1;
}