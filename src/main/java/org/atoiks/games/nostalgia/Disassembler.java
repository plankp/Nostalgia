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
        try {
            for (int i = 0; i < limit && this.buf.hasRemaining(); ++i) {
                this.disassembleNext();
            }
        } catch (RuntimeException ex) {
        }
    }

    public void disassembleAll() {
        try {
            while (this.buf.hasRemaining()) {
                this.disassembleNext();
            }
        } catch (RuntimeException ex) {
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

    private String getRegmask(int mask, char suffix) {
        final StringBuilder sb = new StringBuilder();

        // Skip index 0 (which does not mean %R0)
        for (int index = 1; index < 16; ++index) {
            if ((mask & (1 << index)) != 0) {
                sb.append("%R").append(index).append(suffix).append(", ");
            }
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
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
    public void movLO(int imm, int rdst) {
        this.out.printf("MOV.LO     %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void movHI(int imm, int rdst) {
        this.out.printf("MOV.HI     %s, 0x%x",
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
    public void jabsZ(int imm, int radj, int rflag) {
        this.out.printf("JABS.Z     %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void jabsNZ(int imm, int radj, int rflag) {
        this.out.printf("JABS.NZ    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void jabsGE(int imm, int radj, int rflag) {
        this.out.printf("JABS.GE    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void jabsGT(int imm, int radj, int rflag) {
        this.out.printf("JABS.GT    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void jabsLE(int imm, int radj, int rflag) {
        this.out.printf("JABS.LE    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void jabsLT(int imm, int radj, int rflag) {
        this.out.printf("JABS.LT    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
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
    public void push(int imm9) {
        final int mask = this.loadImm9(imm9);
        final char wdt = (mask & 1) != 0 ? 'D' : 'W';
        this.out.printf("PUSH.%c     %s",
                wdt,
                this.getRegmask(mask, wdt));
        this.resetREX();
    }

    @Override
    public void pop(int imm9) {
        final int mask = this.loadImm9(imm9);
        final char wdt = (mask & 1) != 0 ? 'D' : 'W';
        this.out.printf("POP.%c      %s",
                wdt,
                this.getRegmask(mask, wdt));
        this.resetREX();
    }

    @Override
    public void ret(int imm) {
        this.out.printf("RET        0x%x", this.loadImm9(imm));
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
    public void ldmD(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("LDM.D%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'D'),
                this.rexSynthRegister(this.rexRA, rbase));
        this.resetREX();
    }

    @Override
    public void stmD(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("STM.D%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'D'),
                this.rexSynthRegister(this.rexRA, rbase));
        this.resetREX();
    }

    @Override
    public void ldmW(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("LDM.W%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'W'),
                this.rexSynthRegister(this.rexRA, rbase));
        this.resetREX();
    }

    @Override
    public void stmW(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("STM.W%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'W'),
                this.rexSynthRegister(this.rexRA, rbase));
        this.resetREX();
    }

    @Override
    public void ldmLB(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("LDM.L%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'L'),
                this.rexSynthRegister(this.rexRA, rbase));
        this.resetREX();
    }

    @Override
    public void stmLB(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("STM.L%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'L'),
                this.rexSynthRegister(this.rexRA, rbase));
        this.resetREX();
    }

    @Override
    public void ldmHB(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("LDM.H%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'H'),
                this.rexSynthRegister(this.rexRA, rbase));
        this.resetREX();
    }

    @Override
    public void stmHB(int imm6, int rbase) {
        final int mask = this.loadImm6(imm6);
        this.out.printf("STM.H%c     %s, %s",
                (mask & 1) != 0 ? 'S' : ' ',
                this.getRegmask(mask, 'H'),
                this.rexSynthRegister(this.rexRA, rbase));
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
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void shrI(int imm, int rdst) {
        this.out.printf("SHR.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
        this.resetREX();
    }

    @Override
    public void sarI(int imm, int rdst) {
        this.out.printf("SAR.I      %s, 0x%x",
                this.rexSynthRegister(this.rexRA, rdst),
                this.loadImm6(imm));
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
    public void paddW(int rsrc, int rflag, int rdst) {
        this.out.printf("PADD.W     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rsrc),
                this.rexSynthRegister(this.rexRB, rflag));
        this.resetREX();
    }

    @Override
    public void paddB(int rsrc, int rflag, int rdst) {
        this.out.printf("PADD.B     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rsrc),
                this.rexSynthRegister(this.rexRB, rflag));
        this.resetREX();
    }

    @Override
    public void psubW(int rsrc, int rflag, int rdst) {
        this.out.printf("PSUB.W     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rsrc),
                this.rexSynthRegister(this.rexRB, rflag));
        this.resetREX();
    }

    @Override
    public void psubB(int rsrc, int rflag, int rdst) {
        this.out.printf("PSUB.B     %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRC, rsrc),
                this.rexSynthRegister(this.rexRB, rflag));
        this.resetREX();
    }

    @Override
    public void callZ(int imm, int radj, int rflag) {
        this.out.printf("CALL.Z     %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void callNZ(int imm, int radj, int rflag) {
        this.out.printf("CALL.NZ    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void callGE(int imm, int radj, int rflag) {
        this.out.printf("CALL.GE    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void callGT(int imm, int radj, int rflag) {
        this.out.printf("CALL.GT    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void callLE(int imm, int radj, int rflag) {
        this.out.printf("CALL.LE    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void callLT(int imm, int radj, int rflag) {
        this.out.printf("CALL.LT    %s, 0x%x, %s",
                this.rexSynthRegister(this.rexRA, rflag),
                this.loadImm3(imm),
                this.rexSynthRegister(this.rexRB, radj));
        this.resetREX();
    }

    @Override
    public void iex(int imm) {
        // This is a prefix. Decode the next word after setting some fields.

        this.iexImm = (short) imm;

        this.decoder.decode(this);
    }

    @Override
    public void rex(int rD, int rC, int rB, int rA) {
        // This is a prefix. Decode the next word after setting some fields.
        // ProcessUnit.java gives an explaination of how this extension prefix
        // works.

        this.rexRA = (byte) rA;
        this.rexRB = (byte) rB;
        this.rexRC = (byte) rC;
        this.rexRD = (byte) rD;

        this.decoder.decode(this);
    }

    @Override
    public void imul(int rlhs, int rrhs, int rdlo, int rdhi) {
        this.out.printf("IMUL       %s, %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdhi),
                this.rexSynthRegister(this.rexRB, rdlo),
                this.rexSynthRegister(this.rexRD, rlhs),
                this.rexSynthRegister(this.rexRC, rrhs));
        this.resetREX();
    }

    @Override
    public void idiv(int rlhs, int rrhs, int rdrem, int rdquo) {
        this.out.printf("IDIV       %s, %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdquo),
                this.rexSynthRegister(this.rexRB, rdrem),
                this.rexSynthRegister(this.rexRD, rlhs),
                this.rexSynthRegister(this.rexRC, rrhs));
        this.resetREX();
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

    @Override
    public void imac(int rlhs, int rrhs, int racc, int rdst) {
        this.out.printf("IMAC       %s, %s, %s, %s",
                this.rexSynthRegister(this.rexRA, rdst),
                this.rexSynthRegister(this.rexRB, racc),
                this.rexSynthRegister(this.rexRD, rlhs),
                this.rexSynthRegister(this.rexRC, rrhs));
        this.resetREX();
    }
}