package org.atoiks.games.nostalgia;

public final class Opcode {

    private Opcode() {
        /* static helper classs */
    }

    public static final int OP_MOV_I = 0;
    public static final int OP_ADD_R = 1;
    public static final int OP_SUB_R = 2;
    public static final int OP_AND_R = 3;
    public static final int OP_OR_R = 4;
    public static final int OP_XOR_R = 5;
    public static final int OP_NAND_R = 6;
    public static final int OP_NOR_R = 7;
    public static final int OP_NXOR_R = 8;
    public static final int OP_ADD_I = 9;
    public static final int OP_SUB_I = 10;
    public static final int OP_RSUB_I = 11;
    public static final int OP_JABS_Z = 12;
    public static final int OP_JABS_NZ = 13;
    public static final int OP_JABS_GE = 14;
    public static final int OP_JABS_GT = 15;
    public static final int OP_JABS_LE = 16;
    public static final int OP_JABS_LT = 17;
    public static final int OP_JREL_Z = 18;
    public static final int OP_JREL_NZ = 19;
    public static final int OP_JREL_GE = 20;
    public static final int OP_JREL_GT = 21;
    public static final int OP_JREL_LE = 22;
    public static final int OP_JREL_LT = 23;
    public static final int OP_PUSH = 24;
    public static final int OP_POP = 25;
    public static final int OP_MTSP = 26;
    public static final int OP_MTBP = 27;
    public static final int OP_MSPT = 28;
    public static final int OP_MBPT = 29;
    public static final int OP_CALL = 30;
    public static final int OP_RET = 31;
    public static final int OP_ENTER = 32;
    public static final int OP_LEAVE = 33;
    public static final int OP_INNER = 34;
    public static final int OP_OUTER = 35;
    public static final int OP_LD_W = 36;
    public static final int OP_ST_W = 37;
    public static final int OP_LD_B = 38;
    public static final int OP_ST_B = 39;
    public static final int OP_SHL_R = 40;
    public static final int OP_SHR_R = 41;
    public static final int OP_SAR_R = 42;
}