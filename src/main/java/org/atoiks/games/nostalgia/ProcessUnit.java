package org.atoiks.games.nostalgia;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public final class ProcessUnit implements Decoder.InstrStream, InstrVisitor {

    private static final int DATA_WIDTH = 2;

    // See readRegister and writeRegister (r0 is always 0)
    private final short[] regs = new short[7];

    private short ip;
    private short sp;
    private short bp;

    // This is a secret register (it doesn't even show up in toString!)
    private short imm;

    // Super random, but can we get a counter register lulz!?

    private final MemoryUnit memory;
    private final Decoder decoder;

    public ProcessUnit(MemoryUnit memory) {
        this.memory = Objects.requireNonNull(memory);
        this.decoder = new Decoder(this);
    }

    public void executeNext() {
        // Then the eflags will be very easy to implement:
        // Just catch the exceptions and handle them. Done!
        this.decoder.decode(this);
    }

    public void execute() {
        while (true) {
            this.executeNext();
        }
    }

    public short readRegister(int offset) {
        if (offset == 0) {
            return 0;
        }

        return this.regs[offset - 1];
    }

    public void writeRegister(int offset, short value) {
        if (offset != 0) {
            this.regs[offset - 1] = value;
        }
    }

    public void reset() {
        Arrays.fill(this.regs, (short) 0);

        this.ip = 0;
        this.sp = 0;
        this.bp = 0;

        this.imm = 0;
    }

    public void setIP(int ip) {
        this.ip = (short) ip;
    }

    public void push(short val) {
        final ByteBuffer buf = ByteBuffer.allocate(DATA_WIDTH);
        buf.putShort(val).flip();
        this.sp -= 2;
        memory.write(Short.toUnsignedInt(this.sp), buf);
    }

    public short pop() {
        final ByteBuffer buf = ByteBuffer.allocate(DATA_WIDTH);
        memory.read(Short.toUnsignedInt(this.sp), buf);
        this.sp += 2;
        return buf.flip().getShort();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; ++i) {
            sb.append('R').append(i)
                    .append(": 0x").append(String.format("%04x", this.readRegister(i)))
                    .append(' ');
        }

        sb.append('\n')
                .append("ip: 0x").append(String.format("%04x", this.ip)).append('\n')
                .append("sp: 0x").append(String.format("%04x", this.sp)).append('\n')
                .append("bp: 0x").append(String.format("%04x", this.bp)).append('\n');

        return sb.toString();
    }

    @Override
    public short nextWord() {
        final ByteBuffer buf = ByteBuffer.allocate(DATA_WIDTH);
        memory.read(DATA_WIDTH * Short.toUnsignedInt(this.ip++), buf);
        return buf.flip().getShort();
    }

    @Override
    public void illegalOp(int fullWord) {
        throw new RuntimeException("Process Unit: Illegal opcode: " + fullWord);
    }

    private short loadImm3(int imm3) {
        final int full = (this.imm << 3) | imm3;
        this.imm = 0;
        return (short) full;
    }

    private short loadImm6(int imm6) {
        final int full = (this.imm << 6) | imm6;
        this.imm = 0;
        return (short) full;
    }

    private short loadImm9(int imm9) {
        final int full = (this.imm << 9) | imm9;
        this.imm = 0;
        return (short) full;
    }

    @Override
    public void movI(int imm6, int rdst) {
        this.writeRegister(rdst, this.loadImm6(imm6));
    }

    @Override
    public void addR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (this.readRegister(rlhs) + this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void subR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (this.readRegister(rlhs) - this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void andR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (this.readRegister(rlhs) & this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void orR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (this.readRegister(rlhs) | this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void xorR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (this.readRegister(rlhs) ^ this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void nandR(int rlhs, int rrhs, int rdst) {
        final short out = (short) ~(this.readRegister(rlhs) & this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void norR(int rlhs, int rrhs, int rdst) {
        final short out = (short) ~(this.readRegister(rlhs) | this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void nxorR(int rlhs, int rrhs, int rdst) {
        final short out = (short) ~(this.readRegister(rlhs) ^ this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void addI(int imm6, int rdst) {
        final short out = (short) (this.readRegister(rdst) + this.loadImm6(imm6));
        this.writeRegister(rdst, out);
    }

    @Override
    public void subI(int imm6, int rdst) {
        final short out = (short) (this.readRegister(rdst) - this.loadImm6(imm6));
        this.writeRegister(rdst, out);
    }

    @Override
    public void rsubI(int imm6, int rdst) {
        final short out = (short) (this.loadImm6(imm6) - this.readRegister(rdst));
        this.writeRegister(rdst, out);
    }

    @Override
    public void jabsZ(int imm6, int rflag) {
        if (this.readRegister(rflag) == 0) {
            this.ip = this.loadImm6(imm6);
        }
    }

    @Override
    public void jabsNZ(int imm6, int rflag) {
        if (this.readRegister(rflag) != 0) {
            this.ip = this.loadImm6(imm6);
        }
    }

    @Override
    public void jabsGE(int imm6, int rflag) {
        if (this.readRegister(rflag) >= 0) {
            this.ip = this.loadImm6(imm6);
        }
    }

    @Override
    public void jabsGT(int imm6, int rflag) {
        if (this.readRegister(rflag) > 0) {
            this.ip = this.loadImm6(imm6);
        }
    }

    @Override
    public void jabsLE(int imm6, int rflag) {
        if (this.readRegister(rflag) <= 0) {
            this.ip = this.loadImm6(imm6);
        }
    }

    @Override
    public void jabsLT(int imm6, int rflag) {
        if (this.readRegister(rflag) < 0) {
            this.ip = this.loadImm6(imm6);
        }
    }

    @Override
    public void jrelZ(int imm6, int rflag) {
        if (this.readRegister(rflag) == 0) {
            this.ip += this.loadImm6(imm6);
        }
    }

    @Override
    public void jrelNZ(int imm6, int rflag) {
        if (this.readRegister(rflag) != 0) {
            this.ip += this.loadImm6(imm6);
        }
    }

    @Override
    public void jrelGE(int imm6, int rflag) {
        if (this.readRegister(rflag) >= 0) {
            this.ip += this.loadImm6(imm6);
        }
    }

    @Override
    public void jrelGT(int imm6, int rflag) {
        if (this.readRegister(rflag) > 0) {
            this.ip += this.loadImm6(imm6);
        }
    }

    @Override
    public void jrelLE(int imm6, int rflag) {
        if (this.readRegister(rflag) <= 0) {
            this.ip += this.loadImm6(imm6);
        }
    }

    @Override
    public void jrelLT(int imm6, int rflag) {
        if (this.readRegister(rflag) < 0) {
            this.ip += this.loadImm6(imm6);
        }
    }

    @Override
    public void push(int imm6, int rsrc) {
        this.push((short) (this.readRegister(rsrc) + this.loadImm6(imm6)));
    }

    @Override
    public void pop(int imm6, int rdst) {
        this.writeRegister(rdst, (short) (this.pop() - this.loadImm6(imm6)));
    }

    @Override
    public void mtsp(int imm6, int rsrc) {
        this.sp = (short) (this.readRegister(rsrc) + this.loadImm6(imm6));
    }

    @Override
    public void mtbp(int imm6, int rsrc) {
        this.bp = (short) (this.readRegister(rsrc) + this.loadImm6(imm6));
    }

    @Override
    public void mspt(int imm6, int rdst) {
        this.writeRegister(rdst, (short) (this.sp - this.loadImm6(imm6)));
    }

    @Override
    public void mbpt(int imm6, int rdst) {
        this.writeRegister(rdst, (short) (this.bp - this.loadImm6(imm6)));
    }

    @Override
    public void call(int imm9) {
        // Due to how this#nextWord() is implemented, by the time this
        // instruction is actually handled here, ip would already contain the
        // address of the next instruction!
        this.push(this.ip);
        this.ip = this.loadImm9(imm9);
    }

    @Override
    public void ret() {
        this.ip = this.pop();
    }

    @Override
    public void enter(int imm9) {
        this.push(this.bp);
        this.bp = this.sp;
        this.sp -= this.loadImm9(imm);
    }

    @Override
    public void leave() {
        this.sp = this.bp;
        this.bp = this.pop();
    }

    @Override
    public void inner(int rlhs, int rrhs, int rdst) {
        final int hi = Short.toUnsignedInt(this.readRegister(rlhs)) & 0x00FF;
        final int lo = Short.toUnsignedInt(this.readRegister(rrhs)) & 0xFF00;
        final short out = (short) ((hi << 8) | (lo >> 8));
        this.writeRegister(rdst, out);
    }

    @Override
    public void outer(int rlhs, int rrhs, int rdst) {
        final int hi = Short.toUnsignedInt(this.readRegister(rlhs)) & 0xFF00;
        final int lo = Short.toUnsignedInt(this.readRegister(rrhs)) & 0x00FF;
        final short out = (short) (hi | lo);
        this.writeRegister(rdst, out);
    }

    @Override
    public void ldW(int imm3, int radj, int rdst) {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        this.memory.read(this.loadImm3(imm3) + this.readRegister(radj), buf);
        this.writeRegister(rdst, buf.flip().getShort());
    }

    @Override
    public void stW(int imm3, int radj, int rsrc) {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort(this.readRegister(rsrc)).flip();
        this.memory.write(this.loadImm3(imm3) + this.readRegister(radj), buf);
    }

    @Override
    public void ldB(int imm3, int radj, int rdst) {
        this.writeRegister(rdst, this.memory.read(this.loadImm3(imm3) + this.readRegister(radj)));
    }

    @Override
    public void stB(int imm3, int radj, int rsrc) {
        this.memory.write(this.loadImm3(imm3) + this.readRegister(radj), (byte) this.readRegister(rsrc));
    }

    @Override
    public void shlR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (this.readRegister(rlhs) << this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void shrR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (Short.toUnsignedInt(this.readRegister(rlhs)) >> this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void sarR(int rlhs, int rrhs, int rdst) {
        final short out = (short) (this.readRegister(rlhs) >> this.readRegister(rrhs));
        this.writeRegister(rdst, out);
    }

    @Override
    public void shlI(int imm4, int rdst) {
        final short out = (short) (this.readRegister(rdst) << imm4);
        this.writeRegister(rdst, out);
        this.imm = 0; // techinically we have used up the immediate slot
    }

    @Override
    public void shrI(int imm4, int rdst) {
        final short out = (short) (Short.toUnsignedInt(this.readRegister(rdst)) >> imm4);
        this.writeRegister(rdst, out);
        this.imm = 0; // techinically we have used up the immediate slot
    }

    @Override
    public void sarI(int imm4, int rdst) {
        final short out = (short) (this.readRegister(rdst) >> imm4);
        this.writeRegister(rdst, out);
        this.imm = 0; // techinically we have used up the immediate slot
    }

    @Override
    public void push3(int rC, int rB, int rA) {
        this.push(this.readRegister(rC));
        this.push(this.readRegister(rB));
        this.push(this.readRegister(rA));
    }

    @Override
    public void pop3(int rC, int rB, int rA) {
        this.writeRegister(rC, this.pop());
        this.writeRegister(rB, this.pop());
        this.writeRegister(rA, this.pop());
    }

    @Override
    public void cmovI(int imm3, int rflag, int rdst) {
        // The immediate slot is consumed regardless of the flag!

        final short value = this.loadImm3(imm3);
        if (this.readRegister(rflag) != 0) {
            this.writeRegister(rdst, value);
        }
    }

    @Override
    public void cmovR(int rsrc, int rflag, int rdst) {
        // The value is read from the source register regardless of the flag!

        final short value = this.readRegister(rsrc);
        if (this.readRegister(rflag) != 0) {
            this.writeRegister(rdst, value);
        }
    }

    @Override
    public void hi12(int imm12) {
        this.imm = (short) imm12;
    }

    @Override
    public void mul(int rlhs, int rrhs, int rdlo, int rdhi) {
        // How this instruction works:
        // i32 tmp            = i16 rlhs * i16 rrhs
        // i16 rdhi, i16 rdlo = unpack i32 tmp

        // Also this is signed multiplication
        final short lhs = this.readRegister(rlhs);
        final short rhs = this.readRegister(rrhs);
        final int tmp = lhs * rhs;

        this.writeRegister(rdlo, (short) (tmp >>> 0));
        this.writeRegister(rdhi, (short) (tmp >>> 16));
    }

    @Override
    public void div(int rlhs, int rrhs, int rdrem, int rdquo) {
        final short lhs = this.readRegister(rlhs);
        final short rhs = this.readRegister(rrhs);

        // let the JVM handle crash on division by zero...
        this.writeRegister(rdrem, (short) (lhs % rhs));
        this.writeRegister(rdquo, (short) (lhs / rhs));
    }
}