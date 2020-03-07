package org.atoiks.games.nostalgia;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class Disassembler implements Decoder.InstrStream, InstrVisitor {

    private final PrintStream out;
    private final ByteBuffer buf;
    private final Decoder decoder;

    public Disassembler(PrintStream out, ByteBuffer buf) {
        this.out = Objects.requireNonNull(out);
        this.buf = Objects.requireNonNull(buf);
        this.decoder = new Decoder(this);
    }

    public void disassembleNext() {
        this.out.print(String.format("+%04x  ", this.buf.position()));
        this.decoder.decode(this);
        this.out.println();
    }

    public void disassemble(int limit) {
        for (int i = 0; i < limit && this.buf.hasRemaining(); ++i) {
            this.disassembleNext();
        }
    }

    public void disassembleAll() {
        while (this.buf.hasRemaining()) {
            this.disassembleNext();
        }
    }

    public void rewindBuffer() {
        this.buf.rewind();
    }

    public void markBuffer() {
        this.buf.mark();
    }

    public void rewindBufferToMark() {
        this.buf.reset();
    }

    public void setBufferPosition(int pos) {
        this.buf.position(pos);
    }

    @Override
    public short nextWord() {
        return buf.getShort();
    }

    @Override
    public void illegalOp(int fullWord) {
        this.out.printf("0x%04x     ??", fullWord);
    }

    @Override
    public void movI(int imm, int rdst) {
        this.out.printf("MOV.I      %%r%d, 0x%x", rdst, imm);
    }

    @Override
    public void addR(int rlhs, int rrhs, int rdst) {
        this.out.printf("ADD.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void subR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SUB.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void andR(int rlhs, int rrhs, int rdst) {
        this.out.printf("AND.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void orR(int rlhs, int rrhs, int rdst) {
        this.out.printf("OR.R       %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void xorR(int rlhs, int rrhs, int rdst) {
        this.out.printf("XOR.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void nandR(int rlhs, int rrhs, int rdst) {
        this.out.printf("NAND.R     %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void norR(int rlhs, int rrhs, int rdst) {
        this.out.printf("NOR.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void nxorR(int rlhs, int rrhs, int rdst) {
        this.out.printf("NXOR.R     %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void addI(int imm, int rdst) {
        this.out.printf("ADD.I      %%r%d, 0x%x", rdst, imm);
    }

    @Override
    public void subI(int imm, int rdst) {
        this.out.printf("SUB.I      %%r%d, 0x%x", rdst, imm);
    }

    @Override
    public void rsubI(int imm, int rdst) {
        this.out.printf("RSUB.I     %%r%d, 0x%x", rdst, imm);
    }

    @Override
    public void jabsZ(int imm, int rflag) {
        this.out.printf("JABS.Z     %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jabsNZ(int imm, int rflag) {
        this.out.printf("JABS.NZ    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jabsGE(int imm, int rflag) {
        this.out.printf("JABS.GE    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jabsGT(int imm, int rflag) {
        this.out.printf("JABS.GT    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jabsLE(int imm, int rflag) {
        this.out.printf("JABS.LE    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jabsLT(int imm, int rflag) {
        this.out.printf("JABS.LT    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jrelZ(int imm, int rflag) {
        this.out.printf("JREL.Z     %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jrelNZ(int imm, int rflag) {
        this.out.printf("JREL.NZ    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jrelGE(int imm, int rflag) {
        this.out.printf("JREL.GE    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jrelGT(int imm, int rflag) {
        this.out.printf("JREL.GT    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jrelLE(int imm, int rflag) {
        this.out.printf("JREL.LE    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void jrelLT(int imm, int rflag) {
        this.out.printf("JREL.LT    %%r%d, 0x%x", rflag, imm);
    }

    @Override
    public void push(int imm, int rsrc) {
        this.out.printf("PUSH       %%r%d, 0x%x", rsrc, imm);
    }

    @Override
    public void pop(int imm, int rdst) {
        this.out.printf("POP        %%r%d, 0x%x", rdst, imm);
    }

    @Override
    public void mtsp(int imm, int rsrc) {
        this.out.printf("MTSP       %%r%d, 0x%x", rsrc, imm);
    }

    @Override
    public void mtbp(int imm, int rsrc) {
        this.out.printf("MTBP       %%r%d, 0x%x", rsrc, imm);
    }

    @Override
    public void mspt(int imm, int rdst) {
        this.out.printf("MSPT       %%r%d, 0x%x", rdst, imm);
    }

    @Override
    public void mbpt(int imm, int rdst) {
        this.out.printf("MBPT       %%r%d, 0x%x", rdst, imm);
    }

    @Override
    public void call(int imm) {
        this.out.printf("CALL       0x%x", imm);
    }

    @Override
    public void ret() {
        this.out.printf("RET");
    }

    @Override
    public void enter(int imm) {
        this.out.printf("ENTER      0x%x", imm);
    }

    @Override
    public void leave() {
        this.out.printf("LEAVE");
    }

    @Override
    public void inner(int rlhs, int rrhs, int rdst) {
        this.out.printf("INNER      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void outer(int rlhs, int rrhs, int rdst) {
        this.out.printf("OUTER      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void ldW(int imm, int radj, int rdst) {
        this.out.printf("LD.W       %%r%d, 0x%x, %%r%d", rdst, imm, radj);
    }

    @Override
    public void stW(int imm, int radj, int rsrc) {
        this.out.printf("ST.W       %%r%d, 0x%x, %%r%d", rsrc, imm, radj);
    }

    @Override
    public void ldB(int imm, int radj, int rdst) {
        this.out.printf("LD.B       %%r%d, 0x%x, %%r%d", rdst, imm, radj);
    }

    @Override
    public void stB(int imm, int radj, int rsrc) {
        this.out.printf("ST.B       %%r%d, 0x%x, %%r%d", rsrc, imm, radj);
    }

    @Override
    public void shlR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SHL.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void shrR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SHR.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void sarR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SAR.R      %%r%d, %%r%d, %%r%d", rdst, rlhs, rrhs);
    }

    @Override
    public void shlI(int imm, int rdst) {
        this.out.printf("SHL.I      %%r%d, 0x%x", imm);
    }

    @Override
    public void shrI(int imm, int rdst) {
        this.out.printf("SHR.I      %%r%d, 0x%x", imm);
    }

    @Override
    public void sarI(int imm, int rdst) {
        this.out.printf("SAR.I      %%r%d, 0x%x", imm);
    }

    @Override
    public void push3(int rC, int rB, int rA) {
        this.out.printf("PUSH.3     %%r%d, %%r%d, %%r%d", rA, rC, rB);
    }

    @Override
    public void pop3(int rC, int rB, int rA) {
        this.out.printf("POP.3      %%r%d, %%r%d, %%r%d", rA, rC, rB);
    }

    @Override
    public void cmovI(int imm3, int rflag, int rdst) {
        this.out.printf("CMOV.I     %%r%d, 0x%x, %%r%d", rdst, imm3, rflag);
    }

    @Override
    public void cmovR(int rsrc, int rflag, int rdst) {
        this.out.printf("CMOV.R     %%r%d, %%r%d, %%r%d", rdst, rsrc, rflag);
    }

    @Override
    public void hi12(int imm) {
        // This is honestly a pretty strange opcode...
        this.out.printf("HI12       0x%x", imm);
    }

    @Override
    public void mul(int rlhs, int rrhs, int rdlo, int rdhi) {
        this.out.printf("MUL        %%r%d, %%r%d, %%r%d, %%r%d", rdhi, rdlo, rlhs, rrhs);
    }
}