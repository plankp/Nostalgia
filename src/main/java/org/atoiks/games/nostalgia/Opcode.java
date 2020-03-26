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
    //
    // Note: All possible OP0 opcodes have been filled!
    // (OP0_RESV reserved to allow future extensions)
    // (and I guess if you decide to kill-off confusing ones...)

    public static final int OP0_MOV_I   = 0 & MASK_OP0;
    public static final int OP0_MOV_LO  = 1 & MASK_OP0;
    public static final int OP0_MOV_HI  = 2 & MASK_OP0;

    public static final int OP0_ADD_R   = 3 & MASK_OP0;
    public static final int OP0_SUB_R   = 4 & MASK_OP0;
    public static final int OP0_AND_R   = 5 & MASK_OP0;
    public static final int OP0_OR_R    = 6 & MASK_OP0;
    public static final int OP0_XOR_R   = 7 & MASK_OP0;
    public static final int OP0_NAND_R  = 8 & MASK_OP0;
    public static final int OP0_NOR_R   = 9 & MASK_OP0;
    public static final int OP0_NXOR_R  = 10 & MASK_OP0;

    public static final int OP0_ADD_I   = 11 & MASK_OP0;
    public static final int OP0_SUB_I   = 12 & MASK_OP0;
    public static final int OP0_RSUB_I  = 13 & MASK_OP0;

    public static final int OP0_JABS_Z  = 14 & MASK_OP0;
    public static final int OP0_JABS_NZ = 15 & MASK_OP0;
    public static final int OP0_JABS_GE = 16 & MASK_OP0;
    public static final int OP0_JABS_GT = 17 & MASK_OP0;
    public static final int OP0_JABS_LE = 18 & MASK_OP0;
    public static final int OP0_JABS_LT = 19 & MASK_OP0;

    public static final int OP0_JREL_Z  = 20 & MASK_OP0;
    public static final int OP0_JREL_NZ = 21 & MASK_OP0;
    public static final int OP0_JREL_GE = 22 & MASK_OP0;
    public static final int OP0_JREL_GT = 23 & MASK_OP0;
    public static final int OP0_JREL_LE = 24 & MASK_OP0;
    public static final int OP0_JREL_LT = 25 & MASK_OP0;

    public static final int OP0_PUSH    = 26 & MASK_OP0;
    public static final int OP0_POP     = 27 & MASK_OP0;

    public static final int OP0_RET     = 28 & MASK_OP0;
    public static final int OP0_ENTER   = 29 & MASK_OP0;
    public static final int OP0_LEAVE   = 30 & MASK_OP0;

    public static final int OP0_LD_D    = 31 & MASK_OP0;
    public static final int OP0_ST_D    = 32 & MASK_OP0;
    public static final int OP0_LD_W    = 33 & MASK_OP0;
    public static final int OP0_ST_W    = 34 & MASK_OP0;
    public static final int OP0_LD_B    = 35 & MASK_OP0;
    public static final int OP0_ST_B    = 36 & MASK_OP0;

    public static final int OP0_SHL_R   = 37 & MASK_OP0;
    public static final int OP0_SHR_R   = 38 & MASK_OP0;
    public static final int OP0_SAR_R   = 39 & MASK_OP0;
    public static final int OP0_SHL_I   = 40 & MASK_OP0;
    public static final int OP0_SHR_I   = 41 & MASK_OP0;
    public static final int OP0_SAR_I   = 42 & MASK_OP0;
    public static final int OP0_CMOV_I  = 43 & MASK_OP0;
    public static final int OP0_CMOV_R  = 44 & MASK_OP0;

    public static final int OP0_PADD_W  = 45 & MASK_OP0;
    public static final int OP0_PADD_B  = 46 & MASK_OP0;
    public static final int OP0_PSUB_W  = 47 & MASK_OP0;
    public static final int OP0_PSUB_B  = 48 & MASK_OP0;

    public static final int OP0_CALL_Z  = 49 & MASK_OP0;
    public static final int OP0_CALL_NZ = 50 & MASK_OP0;
    public static final int OP0_CALL_GE = 51 & MASK_OP0;
    public static final int OP0_CALL_GT = 52 & MASK_OP0;
    public static final int OP0_CALL_LE = 53 & MASK_OP0;
    public static final int OP0_CALL_LT = 54 & MASK_OP0;

    public static final int OP0_LDM_D   = 55 & MASK_OP0;
    public static final int OP0_STM_D   = 56 & MASK_OP0;
    public static final int OP0_LDM_W   = 57 & MASK_OP0;
    public static final int OP0_STM_W   = 58 & MASK_OP0;
    public static final int OP0_LDM_HB  = 59 & MASK_OP0;
    public static final int OP0_STM_HB  = 60 & MASK_OP0;
    public static final int OP0_LDM_LB  = 61 & MASK_OP0;
    public static final int OP0_STM_LB  = 62 & MASK_OP0;

    public static final int OP0_RESV    = 63 & MASK_OP0;

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
    // Note: All possible OP1 opcodes have been filled!

    public static final int OP1_REX     = 0 & MASK_OP1;
    public static final int OP1_IMUL    = 1 & MASK_OP1;
    public static final int OP1_IDIV    = 2 & MASK_OP1;
    public static final int OP1_MUL     = 3 & MASK_OP1;
    public static final int OP1_DIV     = 4 & MASK_OP1;
    public static final int OP1_IMAC    = 5 & MASK_OP1;

    // If you are wondering why there is IEX.0 and IEX.1, it's because I messed
    // up the bit calculation: you actually need 13 bits (not 12) to encode a
    // 16 bit immediate value... (oops...)
    public static final int OP1_IEX_0   = 6 & MASK_OP1;
    public static final int OP1_IEX_1   = 7 & MASK_OP1;
}