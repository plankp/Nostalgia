package org.atoiks.games.nostalgia;

import java.util.Objects;

public final class Decoder {

    public static interface InstrStream {

        public short nextWord();
    }

    private static final int INSTR_WIDTH = 2;

    private InstrStream stream;

    public Decoder(InstrStream stream) {
        this.setInstrStream(stream);
    }

    public void setInstrStream(InstrStream stream) {
        this.stream = Objects.requireNonNull(stream);
    }

    public void decode(InstrVisitor vis) {
        // Types of instruction formats:
        // - Immediate-widening instruction:
        //   1iii iiii iiii iiii
        //   Note: even though it shows 15 significant bits, the actual amount
        //         depends on the next instruction.
        //
        // - Lower immediates:
        //   0xxx xxxi iiii iiii
        //   Note: only the last 6 bits of widening is used
        //
        // - One register:
        //   0xxx xxxi iiii iaaa
        //   Note: only the last 9 bits of widening is used
        //
        // - Two registers:
        //   0xxx xxxi iibb baaa
        //   Note: only the last 12 bits of widening is used
        //
        // - Three registers:
        //   0xxx xxxc ccbb baaa

        int widen = 0;

        short word = this.stream.nextWord();

        // If the highest bit is set, then is a special instruction prefix to
        // store an extended immediate value.
        if ((word & (1 << 15)) != 0) {
            widen = Short.toUnsignedInt(word) & ~(1 << 15);

            // Need to read the next word to know the actual instruction
            word = this.stream.nextWord();
        }

        // Sanity check: cannot have two prefixes in a row
        if ((word & (1 << 15)) != 0) {
            // Probably want a custom exception
            throw new RuntimeException("Decoder: cannot have two immediate extension prefixes in a row");
        }

        // Not the best way, but since all the operations are cheap, we will
        // compute all the possible parameters.

        final int rA = getInstrRegisterA(word);
        final int rB = getInstrRegisterB(word);
        final int rC = getInstrRegisterC(word);

        final int immLo = getInstrLowerImmediate(word, widen);
        final int immMi = getInstrMiddleImmediate(word, widen);
        final int immHi = getInstrUpperImmediate(word, widen);

        // Actual operation is masked in 0x7E00.
        // Note: No need to mask the highest bit because that is the immediate
        // extension prefix that we were just testing for.
        switch (Short.toUnsignedInt(word) >> 9) {
            case Opcode.OP_MOV_I:
                vis.movI(immMi, rA);
                break;
            case Opcode.OP_ADD_R:
                vis.addR(rC, rB, rA);
                break;
            case Opcode.OP_SUB_R:
                vis.subR(rC, rB, rA);
                break;
            case Opcode.OP_AND_R:
                vis.andR(rC, rB, rA);
                break;
            case Opcode.OP_OR_R:
                vis.orR(rC, rB, rA);
                break;
            case Opcode.OP_XOR_R:
                vis.xorR(rC, rB, rA);
                break;
            case Opcode.OP_NAND_R:
                vis.nandR(rC, rB, rA);
                break;
            case Opcode.OP_NOR_R:
                vis.norR(rC, rB, rA);
                break;
            case Opcode.OP_NXOR_R:
                vis.nxorR(rC, rB, rA);
                break;
            case Opcode.OP_ADD_I:
                vis.addI(immMi, rA);
                break;
            case Opcode.OP_SUB_I:
                vis.subI(immMi, rA);
                break;
            case Opcode.OP_RSUB_I:
                vis.rsubI(immMi, rA);
                break;
            case Opcode.OP_JABS_Z:
                vis.jabsZ(immMi, rA);
                break;
            case Opcode.OP_JABS_NZ:
                vis.jabsNZ(immMi, rA);
                break;
            case Opcode.OP_JABS_GE:
                vis.jabsGE(immMi, rA);
                break;
            case Opcode.OP_JABS_GT:
                vis.jabsGT(immMi, rA);
                break;
            case Opcode.OP_JABS_LE:
                vis.jabsLE(immMi, rA);
                break;
            case Opcode.OP_JABS_LT:
                vis.jabsLT(immMi, rA);
                break;
            case Opcode.OP_JREL_Z:
                vis.jrelZ(immMi, rA);
                break;
            case Opcode.OP_JREL_NZ:
                vis.jrelNZ(immMi, rA);
                break;
            case Opcode.OP_JREL_GE:
                vis.jrelGE(immMi, rA);
                break;
            case Opcode.OP_JREL_GT:
                vis.jrelGT(immMi, rA);
                break;
            case Opcode.OP_JREL_LE:
                vis.jrelLE(immMi, rA);
                break;
            case Opcode.OP_JREL_LT:
                vis.jrelLT(immMi, rA);
                break;
            case Opcode.OP_PUSH:
                vis.push(immMi, rA);
                break;
            case Opcode.OP_POP:
                vis.pop(immMi, rA);
                break;
            case Opcode.OP_MTSP:
                vis.mtsp(immMi, rA);
                break;
            case Opcode.OP_MTBP:
                vis.mtbp(immMi, rA);
                break;
            case Opcode.OP_MSPT:
                vis.mspt(immMi, rA);
                break;
            case Opcode.OP_MBPT:
                vis.mbpt(immMi, rA);
                break;
            case Opcode.OP_CALL:
                vis.call(immLo);
                break;
            case Opcode.OP_RET:
                vis.ret();
                break;
            case Opcode.OP_ENTER:
                vis.enter(immLo);
                break;
            case Opcode.OP_LEAVE:
                vis.leave();
                break;
            case Opcode.OP_INNER:
                vis.inner(rC, rB, rA);
                break;
            case Opcode.OP_OUTER:
                vis.outer(rC, rB, rA);
                break;
            case Opcode.OP_LD_W:
                vis.ldW(immHi, rB, rA);
                break;
            case Opcode.OP_ST_W:
                vis.stW(immHi, rB, rA);
                break;
            case Opcode.OP_LD_B:
                vis.ldB(immHi, rB, rA);
                break;
            case Opcode.OP_ST_B:
                vis.stB(immHi, rB, rA);
                break;
            case Opcode.OP_SHL_R:
                vis.shlR(rC, rB, rA);
                break;
            case Opcode.OP_SHR_R:
                vis.shrR(rC, rB, rA);
                break;
            case Opcode.OP_SAR_R:
                vis.sarR(rC, rB, rA);
                break;
        }
    }

    private static int getInstrLowerImmediate(short op, int widen) {
        // 0xxx xxxi iiii iiii
        return Short.toUnsignedInt((short) (widen << 9)) | (Short.toUnsignedInt(op) & 0x01FF);
    }

    private static int getInstrMiddleImmediate(short op, int widen) {
        // 0xxx xxxi iiii iaaa ==> down shift the I fields
        return Short.toUnsignedInt((short) (widen << 6)) | ((Short.toUnsignedInt(op) & 0x01F8) >> 3);
    }

    private static int getInstrUpperImmediate(short op, int widen) {
        // 0xxx xxxi iibb baaa ==> down shift the I fields
        return Short.toUnsignedInt((short) (widen << 3)) | ((Short.toUnsignedInt(op) & 0x01C0) >> 6);
    }

    private static int getInstrRegisterA(short op) {
        // 0xxx xxxc ccbb baaa
        return Short.toUnsignedInt(op) & 0x0007;
    }

    private static int getInstrRegisterB(short op) {
        // 0xxx xxxc ccbb baaa
        return (Short.toUnsignedInt(op) & 0x0038) >> 3;
    }

    private static int getInstrRegisterC(short op) {
        // 0xxx xxxc ccbb baaa
        return (Short.toUnsignedInt(op) & 0x01C0) >> 6;
    }
}