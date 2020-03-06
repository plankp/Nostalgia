package org.atoiks.games.nostalgia;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

public final class Encoder implements InstrVisitor {

    /* see Decoder#decode(InstrVisitor) on the instruction layout */

    private static final int MASK_INSTR_OP = 0x3F;

    private final ByteArrayOutputStream out;

    public Encoder() {
        this.out = new ByteArrayOutputStream();
    }

    public Encoder(int sizeHint) {
        this.out = new ByteArrayOutputStream(sizeHint);
    }

    public int size() {
        return this.out.size();
    }

    public byte[] getBytes() {
        return this.out.toByteArray();
    }

    public void writeTo(OutputStream stream) throws IOException {
        this.out.writeTo(stream);
    }

    public void emit(byte b, int times) {
        if (times < 0) {
            throw new IllegalArgumentException("Encoder: Cannot fill byte negative times: " + times);
        }

        for (int i = 0; i < times; ++i) {
            this.out.write(b);
        }
    }

    private void emitShort(short s) {
        final int ui = Short.toUnsignedInt(s);

        // no need high-mask because ui is converted from short
        this.out.write(ui >> 8);
        this.out.write(ui & 0xFF);
    }

    private void emitInstrI(int op, int imm) {
        final int lower = imm & 0b0000_0001_1111_1111;
        final int widen = imm & 0b1111_1110_0000_0000;

        if (widen != 0) {
            // emit an immediate extension prefix
            this.emitShort((short) ((1 << 15) | (widen >> 9)));
        }

        this.emitShort((short) (0
                | ((op & MASK_INSTR_OP) << 9)
                | lower));
    }

    private void emitInstrIR(int op, int imm, int rA) {
        final int lower = imm & 0b0000_0000_0011_1111;
        final int widen = imm & 0b1111_1111_1100_0000;

        if (widen != 0) {
            // emit an immediate extension prefix
            this.emitShort((short) ((1 << 15) | (widen >> 6)));
        }

        this.emitShort((short) (0
                | ((op & MASK_INSTR_OP) << 9)
                | (lower << 3)
                | (rA & 0x07)));
    }

    private void emitInstrIRR(int op, int imm, int rB, int rA) {
        final int lower = imm & 0b0000_0000_0000_0111;
        final int widen = imm & 0b1111_1111_1111_1000;

        if (widen != 0) {
            // emit an immediate extension prefix
            this.emitShort((short) ((1 << 15) | (widen >> 3)));
        }

        this.emitShort((short) (0
                | ((op & MASK_INSTR_OP) << 9)
                | (lower << 6)
                | ((rB & 0x07) << 3)
                | (rA & 0x07)));
    }

    private void emitInstrRRR(int op, int rC, int rB, int rA) {
        this.emitShort((short) (((op & MASK_INSTR_OP) << 9)
                | ((rC & 0x07) << 6)
                | ((rB & 0x07) << 3)
                | (rA & 0x07)));
    }

    @Override
    public void movI(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_MOV_I, imm, rdst);
    }

    @Override
    public void addR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_ADD_R, rlhs, rrhs, rdst);
    }

    @Override
    public void subR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_SUB_R, rlhs, rrhs, rdst);
    }

    @Override
    public void andR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_AND_R, rlhs, rrhs, rdst);
    }

    @Override
    public void orR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_OR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void xorR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_XOR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void nandR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_NAND_R, rlhs, rrhs, rdst);
    }

    @Override
    public void norR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_NOR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void nxorR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_NXOR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void addI(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_ADD_I, imm, rdst);
    }

    @Override
    public void subI(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_SUB_I, imm, rdst);
    }

    @Override
    public void rsubI(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_RSUB_I, imm, rdst);
    }

    @Override
    public void jabsZ(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JABS_Z, imm, rflag);
    }

    @Override
    public void jabsNZ(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JABS_NZ, imm, rflag);
    }

    @Override
    public void jabsGE(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JABS_GE, imm, rflag);
    }

    @Override
    public void jabsGT(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JABS_GT, imm, rflag);
    }

    @Override
    public void jabsLE(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JABS_LE, imm, rflag);
    }

    @Override
    public void jabsLT(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JABS_LT, imm, rflag);
    }

    @Override
    public void jrelZ(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JREL_Z, imm, rflag);
    }

    @Override
    public void jrelNZ(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JREL_NZ, imm, rflag);
    }

    @Override
    public void jrelGE(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JREL_GE, imm, rflag);
    }

    @Override
    public void jrelGT(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JREL_GT, imm, rflag);
    }

    @Override
    public void jrelLE(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JREL_LE, imm, rflag);
    }

    @Override
    public void jrelLT(int imm, int rflag) {
        this.emitInstrIR(Opcode.OP_JREL_LT, imm, rflag);
    }

    @Override
    public void push(int imm, int rsrc) {
        this.emitInstrIR(Opcode.OP_PUSH, imm, rsrc);
    }

    @Override
    public void pop(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_POP, imm, rdst);
    }

    @Override
    public void mtsp(int imm, int rsrc) {
        this.emitInstrIR(Opcode.OP_MTSP, imm, rsrc);
    }

    @Override
    public void mtbp(int imm, int rsrc) {
        this.emitInstrIR(Opcode.OP_MTBP, imm, rsrc);
    }

    @Override
    public void mspt(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_MSPT, imm, rdst);
    }

    @Override
    public void mbpt(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_MBPT, imm, rdst);
    }

    @Override
    public void call(int imm) {
        this.emitInstrI(Opcode.OP_CALL, imm);
    }

    @Override
    public void ret() {
        this.emitInstrI(Opcode.OP_RET, 0);
    }

    @Override
    public void enter(int imm) {
        this.emitInstrI(Opcode.OP_ENTER, imm);
    }

    @Override
    public void leave() {
        this.emitInstrI(Opcode.OP_LEAVE, 0);
    }

    @Override
    public void inner(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_INNER, rlhs, rrhs, rdst);
    }

    @Override
    public void outer(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_OUTER, rlhs, rrhs, rdst);
    }

    @Override
    public void ldW(int imm, int radj, int rdst) {
        this.emitInstrIRR(Opcode.OP_LD_W, imm, radj, rdst);
    }

    @Override
    public void stW(int imm, int radj, int rsrc) {
        this.emitInstrIRR(Opcode.OP_ST_W, imm, radj, rsrc);
    }

    @Override
    public void ldB(int imm, int radj, int rdst) {
        this.emitInstrIRR(Opcode.OP_LD_B, imm, radj, rdst);
    }

    @Override
    public void stB(int imm, int radj, int rsrc) {
        this.emitInstrIRR(Opcode.OP_ST_B, imm, radj, rsrc);
    }

    @Override
    public void shlR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_SHL_R, rlhs, rrhs, rdst);
    }

    @Override
    public void shrR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_SHR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void sarR(int rlhs, int rrhs, int rdst) {
        this.emitInstrRRR(Opcode.OP_SAR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void shlI(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_SFT_I, imm & 0x0F, rdst);
    }

    @Override
    public void shrI(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_SFT_I, (imm & 0x0F) | 0x10, rdst);
    }

    @Override
    public void sarI(int imm, int rdst) {
        this.emitInstrIR(Opcode.OP_SFT_I, (imm & 0x0F) | 0x30, rdst);
    }
}