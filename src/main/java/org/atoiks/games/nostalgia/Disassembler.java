package org.atoiks.games.nostalgia;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class Disassembler implements Decoder.InstrStream, InstrVisitor {

    private final PrintStream out;
    private final ByteBuffer buf;
    private final Decoder decoder;

    // Basically do what ProcessUnit.java is doing: Reconstruct the intended
    // code. Actually not sure about IEX, but REX we should totally do this!
    private short iexImm;
    private byte rexRA;
    private byte rexRB;
    private byte rexRC;
    private byte rexRD;

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

    private void resetREX() {
        this.rexRA = 0;
        this.rexRB = 0;
        this.rexRC = 0;
        this.rexRD = 0;
    }

    private String rexSynthRegister(int rex, int inst) {
        // Only need a 5 char buffer...
        final StringBuilder sb = new StringBuilder(5);
        sb.append("%R").append(((rex & 0x1) << 3) | inst);

        switch ((rex >> 1) & 0x3) {
            case 0:     sb.append('W'); break;
            case 1:     sb.append('L'); break;
            case 2:     sb.append('H'); break;
            case 3:     sb.append('D'); break;
            default:    throw new AssertionError("Wtf illegal math mask with 3 produced a value out of 1..3?");
        }

        return sb.toString();
    }

    private short loadImm3(int imm3) {
        final int full = (this.iexImm << 3) | imm3;
        this.iexImm = 0;
        return (short) full;
    }

    private short loadImm6(int imm6) {
        final int full = (this.iexImm << 6) | imm6;
        this.iexImm = 0;
        return (short) full;
    }

    private short loadImm9(int imm9) {
        final int full = (this.iexImm << 9) | imm9;
        this.iexImm = 0;
        return (short) full;
    }

    @Override
    public void illegalOp(int fullWord) {
        this.out.printf("0x%04x     ??", fullWord);
    }

    @Override
    public void movI(int imm, int rdst) {
        this.out.printf("MOV.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void addR(int rlhs, int rrhs, int rdst) {
        this.out.printf("ADD.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void subR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SUB.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void andR(int rlhs, int rrhs, int rdst) {
        this.out.printf("AND.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void orR(int rlhs, int rrhs, int rdst) {
        this.out.printf("OR.R       %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void xorR(int rlhs, int rrhs, int rdst) {
        this.out.printf("XOR.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void nandR(int rlhs, int rrhs, int rdst) {
        this.out.printf("NAND.R     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void norR(int rlhs, int rrhs, int rdst) {
        this.out.printf("NOR.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void nxorR(int rlhs, int rrhs, int rdst) {
        this.out.printf("NXOR.R     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void addI(int imm, int rdst) {
        this.out.printf("ADD.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void subI(int imm, int rdst) {
        this.out.printf("SUB.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void rsubI(int imm, int rdst) {
        this.out.printf("RSUB.I     %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jabsZ(int imm, int rflag) {
        this.out.printf("JABS.Z     %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jabsNZ(int imm, int rflag) {
        this.out.printf("JABS.NZ    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jabsGE(int imm, int rflag) {
        this.out.printf("JABS.GE    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jabsGT(int imm, int rflag) {
        this.out.printf("JABS.GT    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jabsLE(int imm, int rflag) {
        this.out.printf("JABS.LE    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jabsLT(int imm, int rflag) {
        this.out.printf("JABS.LT    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jrelZ(int imm, int rflag) {
        this.out.printf("JREL.Z     %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jrelNZ(int imm, int rflag) {
        this.out.printf("JREL.NZ    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jrelGE(int imm, int rflag) {
        this.out.printf("JREL.GE    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jrelGT(int imm, int rflag) {
        this.out.printf("JREL.GT    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jrelLE(int imm, int rflag) {
        this.out.printf("JREL.LE    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void jrelLT(int imm, int rflag) {
        this.out.printf("JREL.LT    %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void push(int imm, int rsrc) {
        this.out.printf("PUSH       %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rsrc),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void pop(int imm, int rdst) {
        this.out.printf("POP        %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void call(int imm) {
        this.out.printf("CALL       0x%x", this.loadImm9(imm));
    }

    @Override
    public void ret() {
        this.out.printf("RET");
    }

    @Override
    public void enter(int imm) {
        this.out.printf("ENTER      0x%x", this.loadImm9(imm));
    }

    @Override
    public void leave() {
        this.out.printf("LEAVE");
    }

    @Override
    public void ldD(int imm, int radj, int rdst) {
        this.out.printf("LD.D       %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void stD(int imm, int radj, int rsrc) {
        this.out.printf("ST.D       %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rsrc),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void ldW(int imm, int radj, int rdst) {
        this.out.printf("LD.W       %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void stW(int imm, int radj, int rsrc) {
        this.out.printf("ST.W       %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rsrc),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void ldB(int imm, int radj, int rdst) {
        this.out.printf("LD.B       %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void stB(int imm, int radj, int rsrc) {
        this.out.printf("ST.B       %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rsrc),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void shlR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SHL.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void shrR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SHR.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void sarR(int rlhs, int rrhs, int rdst) {
        this.out.printf("SAR.R      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rlhs),
                this.rexSynthRegister(this.rexRB, rrhs));
        this.resetREX();
    }

    @Override
    public void shlI(int imm, int rdst) {
        this.out.printf("SHL.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                imm);
        this.resetREX();
        this.iexImm = 0; // techinically we have used up the immediate slot
    }

    @Override
    public void shrI(int imm, int rdst) {
        this.out.printf("SHR.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                imm);
        this.resetREX();
        this.iexImm = 0; // techinically we have used up the immediate slot
    }

    @Override
    public void sarI(int imm, int rdst) {
        this.out.printf("SAR.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                imm);
        this.resetREX();
        this.iexImm = 0; // techinically we have used up the immediate slot
    }

    @Override
    public void push3(int rC, int rB, int rA) {
        this.out.printf("PUSH.3     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rA),
                this.rexSynthRegister(this.rexRC, rC),
                this.rexSynthRegister(this.rexRB, rB));
        this.resetREX();
    }

    @Override
    public void pop3(int rC, int rB, int rA) {
        this.out.printf("POP.3      %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rA),
                this.rexSynthRegister(this.rexRC, rC),
                this.rexSynthRegister(this.rexRB, rB));
        this.resetREX();
    }

    @Override
    public void cmovI(int imm3, int rflag, int rdst) {
        this.out.printf("CMOV.I     %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm3(imm3),
                this.rexSynthRegister(this.rexRB, rflag));
        this.resetREX();
    }

    @Override
    public void cmovR(int rsrc, int rflag, int rdst) {
        this.out.printf("CMOV.R     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rsrc),
                this.rexSynthRegister(this.rexRB, rflag));
        this.resetREX();
    }

    @Override
    public void iex(int imm) {
        // This is honestly a pretty strange opcode...

        this.out.printf("<<IEX>>");

        this.iexImm = (short) imm;
    }

    @Override
    public void rex(int rD, int rC, int rB, int rA) {
        // This is honestly a pretty strange opcode...
        // ProcessUnit.java gives an explaination of how this extension prefix
        // works.

        this.out.printf("<<REX>>");

        this.rexRA = (byte) rA;
        this.rexRB = (byte) rB;
        this.rexRC = (byte) rC;
        this.rexRD = (byte) rD;
    }

    @Override
    public void mul(int rlhs, int rrhs, int rdlo, int rdhi) {
        this.out.printf("MUL        %s, %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdhi),
                this.rexSynthRegister(this.rexRB, rdlo),
                this.rexSynthRegister(this.rexRD, rlhs),
                this.rexSynthRegister(this.rexRC, rrhs));
        this.resetREX();
    }

    @Override
    public void div(int rlhs, int rrhs, int rdrem, int rdquo) {
        this.out.printf("DIV        %s, %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdquo),
                this.rexSynthRegister(this.rexRB, rdrem),
                this.rexSynthRegister(this.rexRD, rlhs),
                this.rexSynthRegister(this.rexRC, rrhs));
        this.resetREX();
    }
}