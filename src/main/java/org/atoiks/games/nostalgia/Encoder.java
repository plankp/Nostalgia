package org.atoiks.games.nostalgia;

import java.io.IOException;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

public final class Encoder implements InstrVisitor {

    /* see Opcode.java for the instruction layout */

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

    private void emitOp0I(int op, int imm) {
        final int lower = imm & 0b0000_0001_1111_1111;
        final int widen = imm & 0b1111_1110_0000_0000;

        if (widen != 0) {
            // emit iex
            this.iex(widen >> 9);
        }

        this.emitShort((short) (0
                | ((op & Opcode.MASK_OP0) << 9)
                | lower));
    }

    private void emitOp0IR(int op, int imm, int rA) {
        final int lower = imm & 0b0000_0000_0011_1111;
        final int widen = imm & 0b1111_1111_1100_0000;

        if (widen != 0) {
            // emit iex
            this.iex(widen >> 6);
        }

        final int rexRA = (rA & 0b11_1000) >> 3;
        if (rexRA != 0) {
            // emit rex
            this.rex(0, 0, 0, rexRA);
        }

        this.emitShort((short) (0
                | ((op & Opcode.MASK_OP0) << 9)
                | (lower << 3)
                | (rA & 0x07)));
    }

    private void emitOp0IRR(int op, int imm, int rB, int rA) {
        final int lower = imm & 0b0000_0000_0000_0111;
        final int widen = imm & 0b1111_1111_1111_1000;

        if (widen != 0) {
            // emit iex
            this.iex(widen >> 3);
        }

        final int rexRA = (rA & 0b11_1000) >> 3;
        final int rexRB = (rA & 0b11_1000) >> 3;
        if (rexRA != 0 || rexRB != 0) {
            // emit rex
            this.rex(0, 0, rexRB, rexRA);
        }

        this.emitShort((short) (0
                | ((op & Opcode.MASK_OP0) << 9)
                | (lower << 6)
                | ((rB & 0x07) << 3)
                | (rA & 0x07)));
    }

    private void emitOp0RRR(int op, int rC, int rB, int rA) {
        final int rexRA = (rA & 0b11_1000) >> 3;
        final int rexRB = (rA & 0b11_1000) >> 3;
        final int rexRC = (rA & 0b11_1000) >> 3;
        if (rexRA != 0 || rexRB != 0 || rexRC != 0) {
            // emit rex
            this.rex(0, rexRC, rexRB, rexRA);
        }

        this.emitShort((short) (0
                | ((op & Opcode.MASK_OP0) << 9)
                | ((rC & 0x07) << 6)
                | ((rB & 0x07) << 3)
                | (rA & 0x07)));
    }

    private void emitOp1RRRR(int op, int rD, int rC, int rB, int rA) {
        final int rexRA = (rA & 0b11_1000) >> 3;
        final int rexRB = (rA & 0b11_1000) >> 3;
        final int rexRC = (rA & 0b11_1000) >> 3;
        final int rexRD = (rA & 0b11_1000) >> 3;
        if (rexRA != 0 || rexRB != 0 || rexRC != 0 || rexRD != 0) {
            // emit rex
            this.rex(rexRD, rexRC, rexRB, rexRA);
        }

        this.emitShort((short) ((1 << 15)
                | ((op & Opcode.MASK_OP1) << 12)
                | ((rD & 0x07) << 9)
                | ((rC & 0x07) << 6)
                | ((rB & 0x07) << 3)
                | ((rA & 0x07) << 0)));
    }

    @Override
    public void illegalOp(int op) {
        throw new RuntimeException("Encoder: Attempt to force an illegal opcode!");
    }

    @Override
    public void movI(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_MOV_I, imm, rdst);
    }

    @Override
    public void addR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_ADD_R, rlhs, rrhs, rdst);
    }

    @Override
    public void subR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_SUB_R, rlhs, rrhs, rdst);
    }

    @Override
    public void andR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_AND_R, rlhs, rrhs, rdst);
    }

    @Override
    public void orR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_OR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void xorR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_XOR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void nandR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_NAND_R, rlhs, rrhs, rdst);
    }

    @Override
    public void norR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_NOR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void nxorR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_NXOR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void addI(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_ADD_I, imm, rdst);
    }

    @Override
    public void subI(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_SUB_I, imm, rdst);
    }

    @Override
    public void rsubI(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_RSUB_I, imm, rdst);
    }

    @Override
    public void jabsZ(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JABS_Z, imm, rflag);
    }

    @Override
    public void jabsNZ(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JABS_NZ, imm, rflag);
    }

    @Override
    public void jabsGE(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JABS_GE, imm, rflag);
    }

    @Override
    public void jabsGT(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JABS_GT, imm, rflag);
    }

    @Override
    public void jabsLE(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JABS_LE, imm, rflag);
    }

    @Override
    public void jabsLT(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JABS_LT, imm, rflag);
    }

    @Override
    public void jrelZ(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JREL_Z, imm, rflag);
    }

    @Override
    public void jrelNZ(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JREL_NZ, imm, rflag);
    }

    @Override
    public void jrelGE(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JREL_GE, imm, rflag);
    }

    @Override
    public void jrelGT(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JREL_GT, imm, rflag);
    }

    @Override
    public void jrelLE(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JREL_LE, imm, rflag);
    }

    @Override
    public void jrelLT(int imm, int rflag) {
        this.emitOp0IR(Opcode.OP0_JREL_LT, imm, rflag);
    }

    @Override
    public void push(int imm, int rsrc) {
        this.emitOp0IR(Opcode.OP0_PUSH, imm, rsrc);
    }

    @Override
    public void pop(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_POP, imm, rdst);
    }

    @Override
    public void call(int imm) {
        this.emitOp0I(Opcode.OP0_CALL, imm);
    }

    @Override
    public void ret() {
        this.emitOp0I(Opcode.OP0_RET, 0);
    }

    @Override
    public void enter(int imm) {
        this.emitOp0I(Opcode.OP0_ENTER, imm);
    }

    @Override
    public void leave() {
        this.emitOp0I(Opcode.OP0_LEAVE, 0);
    }

    @Override
    public void ldD(int imm, int radj, int rdst) {
        this.emitOp0IRR(Opcode.OP0_LD_D, imm, radj, rdst);
    }

    @Override
    public void stD(int imm, int radj, int rsrc) {
        this.emitOp0IRR(Opcode.OP0_ST_D, imm, radj, rsrc);
    }

    @Override
    public void ldW(int imm, int radj, int rdst) {
        this.emitOp0IRR(Opcode.OP0_LD_W, imm, radj, rdst);
    }

    @Override
    public void stW(int imm, int radj, int rsrc) {
        this.emitOp0IRR(Opcode.OP0_ST_W, imm, radj, rsrc);
    }

    @Override
    public void ldB(int imm, int radj, int rdst) {
        this.emitOp0IRR(Opcode.OP0_LD_B, imm, radj, rdst);
    }

    @Override
    public void stB(int imm, int radj, int rsrc) {
        this.emitOp0IRR(Opcode.OP0_ST_B, imm, radj, rsrc);
    }

    @Override
    public void shlR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_SHL_R, rlhs, rrhs, rdst);
    }

    @Override
    public void shrR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_SHR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void sarR(int rlhs, int rrhs, int rdst) {
        this.emitOp0RRR(Opcode.OP0_SAR_R, rlhs, rrhs, rdst);
    }

    @Override
    public void shlI(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_SFT_I, imm & 0x0F, rdst);
    }

    @Override
    public void shrI(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_SFT_I, (imm & 0x0F) | 0x10, rdst);
    }

    @Override
    public void sarI(int imm, int rdst) {
        this.emitOp0IR(Opcode.OP0_SFT_I, (imm & 0x0F) | 0x30, rdst);
    }

    @Override
    public void push3(int rC, int rB, int rA) {
        this.emitOp0RRR(Opcode.OP0_PUSH3, rC, rB, rA);
    }

    @Override
    public void pop3(int rC, int rB, int rA) {
        this.emitOp0RRR(Opcode.OP0_POP3, rC, rB, rA);
    }

    @Override
    public void cmovI(int imm3, int rflag, int rdst) {
        this.emitOp0IRR(Opcode.OP0_CMOV_I, imm3, rflag, rdst);
    }

    @Override
    public void cmovR(int rsrc, int rflag, int rdst) {
        this.emitOp0RRR(Opcode.OP0_CMOV_R, rsrc, rflag, rdst);
    }

    @Override
    public void iex(int imm12) {
        // This opcode is special... The other instructions that use immediates
        // can subject to auto immediate extension. This one must not because
        // it is the instruction to perform that extension!

        this.emitShort((short) ((1 << 15) | (Opcode.OP1_IEX << 12) | (imm12 & 0xFFF)));
    }

    @Override
    public void rex(int rD, int rC, int rB, int rA) {
        // This opcode is special... these are not actually registers, but
        // instead are register extensions (similar to IEX which is an
        // immediate extension, not the actual immediate value).

        this.emitOp1RRRR(Opcode.OP1_REX, rD & 0x07, rC & 0x07, rB & 0x07, rA & 0x07);
    }

    @Override
    public void mul(int rlhs, int rrhs, int rdlo, int rdhi) {
        this.emitOp1RRRR(Opcode.OP1_MUL, rlhs, rrhs, rdlo, rdhi);
    }

    @Override
    public void div(int rlhs, int rrhs, int rdrem, int rdquo) {
        this.emitOp1RRRR(Opcode.OP1_DIV, rlhs, rrhs, rdrem, rdquo);
    }
}