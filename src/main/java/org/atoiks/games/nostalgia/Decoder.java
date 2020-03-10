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
        /* see Opcode.java for the instruction layout */

        // Check if the opcode is class OP0 or OP1 by checking the highest bit.
        final int word = Short.toUnsignedInt(this.stream.nextWord());
        if ((word & (1 << 15)) == 0) {
            // It's a OP0 opcode
            this.decodeOP0((word >> 9) & Opcode.MASK_OP0, word & 0x1FF, vis);
        } else {
            // It's a OP1 opcode
            this.decodeOP1((word >> 12) & Opcode.MASK_OP1, word & 0xFFF, vis);
        }
    }

    public void decodeOP0(int op, int lo9, InstrVisitor vis) {
        // Decode all the possible OP0 class formats:

        final int rA = (lo9 >> 0) & 0x7; // dummy shr 0 just for fmt
        final int rB = (lo9 >> 3) & 0x7;
        final int rC = (lo9 >> 6) & 0x7;

        final int immLo = (lo9 >> 0) & 0x1FF; // dummy shr 0 just for fmt
        final int immMi = (lo9 >> 3) & 0x3F;
        final int immHi = (lo9 >> 6) & 0x7; // yes, it's the same as rC

        switch (op) {
            case Opcode.OP0_MOV_I:
                vis.movI(immMi, rA);
                break;
            case Opcode.OP0_ADD_R:
                vis.addR(rC, rB, rA);
                break;
            case Opcode.OP0_SUB_R:
                vis.subR(rC, rB, rA);
                break;
            case Opcode.OP0_AND_R:
                vis.andR(rC, rB, rA);
                break;
            case Opcode.OP0_OR_R:
                vis.orR(rC, rB, rA);
                break;
            case Opcode.OP0_XOR_R:
                vis.xorR(rC, rB, rA);
                break;
            case Opcode.OP0_NAND_R:
                vis.nandR(rC, rB, rA);
                break;
            case Opcode.OP0_NOR_R:
                vis.norR(rC, rB, rA);
                break;
            case Opcode.OP0_NXOR_R:
                vis.nxorR(rC, rB, rA);
                break;
            case Opcode.OP0_ADD_I:
                vis.addI(immMi, rA);
                break;
            case Opcode.OP0_SUB_I:
                vis.subI(immMi, rA);
                break;
            case Opcode.OP0_RSUB_I:
                vis.rsubI(immMi, rA);
                break;
            case Opcode.OP0_JABS_Z:
                vis.jabsZ(immMi, rA);
                break;
            case Opcode.OP0_JABS_NZ:
                vis.jabsNZ(immMi, rA);
                break;
            case Opcode.OP0_JABS_GE:
                vis.jabsGE(immMi, rA);
                break;
            case Opcode.OP0_JABS_GT:
                vis.jabsGT(immMi, rA);
                break;
            case Opcode.OP0_JABS_LE:
                vis.jabsLE(immMi, rA);
                break;
            case Opcode.OP0_JABS_LT:
                vis.jabsLT(immMi, rA);
                break;
            case Opcode.OP0_JREL_Z:
                vis.jrelZ(immMi, rA);
                break;
            case Opcode.OP0_JREL_NZ:
                vis.jrelNZ(immMi, rA);
                break;
            case Opcode.OP0_JREL_GE:
                vis.jrelGE(immMi, rA);
                break;
            case Opcode.OP0_JREL_GT:
                vis.jrelGT(immMi, rA);
                break;
            case Opcode.OP0_JREL_LE:
                vis.jrelLE(immMi, rA);
                break;
            case Opcode.OP0_JREL_LT:
                vis.jrelLT(immMi, rA);
                break;
            case Opcode.OP0_PUSH:
                vis.push(immMi, rA);
                break;
            case Opcode.OP0_POP:
                vis.pop(immMi, rA);
                break;
            case Opcode.OP0_CALL:
                vis.call(immLo);
                break;
            case Opcode.OP0_RET:
                vis.ret();
                break;
            case Opcode.OP0_ENTER:
                vis.enter(immLo);
                break;
            case Opcode.OP0_LEAVE:
                vis.leave();
                break;
            case Opcode.OP0_LD_D:
                vis.ldD(immHi, rB, rA);
                break;
            case Opcode.OP0_ST_D:
                vis.stD(immHi, rB, rA);
                break;
            case Opcode.OP0_LD_W:
                vis.ldW(immHi, rB, rA);
                break;
            case Opcode.OP0_ST_W:
                vis.stW(immHi, rB, rA);
                break;
            case Opcode.OP0_LD_B:
                vis.ldB(immHi, rB, rA);
                break;
            case Opcode.OP0_ST_B:
                vis.stB(immHi, rB, rA);
                break;
            case Opcode.OP0_SHL_R:
                vis.shlR(rC, rB, rA);
                break;
            case Opcode.OP0_SHR_R:
                vis.shrR(rC, rB, rA);
                break;
            case Opcode.OP0_SAR_R:
                vis.sarR(rC, rB, rA);
                break;
            case Opcode.OP0_SHL_I:
                vis.shlI(immMi, rA);
                break;
            case Opcode.OP0_SHR_I:
                vis.shrI(immMi, rA);
                break;
            case Opcode.OP0_SAR_I:
                vis.sarI(immMi, rA);
                break;
            case Opcode.OP0_PUSH3:
                vis.push3(rC, rB, rA);
                break;
            case Opcode.OP0_POP3:
                vis.pop3(rC, rB, rA);
                break;
            case Opcode.OP0_CMOV_I:
                vis.cmovI(immHi, rB, rA);
                break;
            case Opcode.OP0_CMOV_R:
                vis.cmovR(rC, rB, rA);
                break;
            default:
                // Reconstruct the whole opcode
                vis.illegalOp((op << 9) | lo9);
                break;
        }
    }

    public void decodeOP1(int op, int lo12, InstrVisitor vis) {
        // Decode all the possible OP1 class formats:

        final int rA = (lo12 >> 0) & 0x7; // dummy shr 0 just for fmt
        final int rB = (lo12 >> 3) & 0x7;
        final int rC = (lo12 >> 6) & 0x7;
        final int rD = (lo12 >> 9) & 0x7;

        switch (op) {
            case Opcode.OP1_REX:
                vis.rex(rD, rC, rB, rA);
                break;
            case Opcode.OP1_MUL:
                vis.mul(rD, rC, rB, rA);
                break;
            case Opcode.OP1_DIV:
                vis.div(rD, rC, rB, rA);
                break;
            case Opcode.OP1_IEX_0:
                vis.iex(lo12);
                break;
            case Opcode.OP1_IEX_1:
                // hack around my mistake (see Opcode.java): we add the high bit back to the extended immediate
                vis.iex((1 << 12) | lo12);
                break;
            default:
                // Reconstruct the whole opcode
                vis.illegalOp((1 << 15) | (op << 12) | lo12);
                break;
        }
    }
}