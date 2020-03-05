package org.atoiks.games.nostalgia;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public final class ProcessUnit implements Decoder.InstrStream, InstrVisitor {

    // TODO: ADD SHIFTS, PUSH3, POP3

    private static final int DATA_WIDTH = 2;

    // See readRegister and writeRegister (r0 is always 0)
    private final short[] regs = new short[7];

    private short ip;
    private short sp;
    private short bp;

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
    }

    public void setIP(int ip) {
        this.ip = (short) ip;
    }

    public void push(short val) {
        final ByteBuffer buf = ByteBuffer.allocate(DATA_WIDTH);
        buf.putShort(val).flip();
        memory.write(DATA_WIDTH * Short.toUnsignedInt(--this.sp), buf);
    }

    public short pop() {
        final ByteBuffer buf = ByteBuffer.allocate(DATA_WIDTH);
        memory.read(DATA_WIDTH * Short.toUnsignedInt(this.sp++), buf);
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
    public void movI(int imm, int rdst) {
        this.writeRegister(rdst, (short) imm);
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
    public void addI(int imm, int rdst) {
        final short out = (short) (this.readRegister(rdst) + imm);
        this.writeRegister(rdst, out);
    }

    @Override
    public void subI(int imm, int rdst) {
        final short out = (short) (this.readRegister(rdst) - imm);
        this.writeRegister(rdst, out);
    }

    @Override
    public void rsubI(int imm, int rdst) {
        final short out = (short) (imm - this.readRegister(rdst));
        this.writeRegister(rdst, out);
    }

    @Override
    public void jabsZ(int imm, int rflag) {
        if (this.readRegister(rflag) == 0) {
            this.ip = (short) imm;
        }
    }

    @Override
    public void jabsNZ(int imm, int rflag) {
        if (this.readRegister(rflag) != 0) {
            this.ip = (short) imm;
        }
    }

    @Override
    public void jabsGE(int imm, int rflag) {
        if (this.readRegister(rflag) >= 0) {
            this.ip = (short) imm;
        }
    }

    @Override
    public void jabsGT(int imm, int rflag) {
        if (this.readRegister(rflag) > 0) {
            this.ip = (short) imm;
        }
    }

    @Override
    public void jabsLE(int imm, int rflag) {
        if (this.readRegister(rflag) <= 0) {
            this.ip = (short) imm;
        }
    }

    @Override
    public void jabsLT(int imm, int rflag) {
        if (this.readRegister(rflag) < 0) {
            this.ip = (short) imm;
        }
    }

    @Override
    public void jrelZ(int imm, int rflag) {
        if (this.readRegister(rflag) == 0) {
            this.ip += (short) imm;
        }
    }

    @Override
    public void jrelNZ(int imm, int rflag) {
        if (this.readRegister(rflag) != 0) {
            this.ip += (short) imm;
        }
    }

    @Override
    public void jrelGE(int imm, int rflag) {
        if (this.readRegister(rflag) >= 0) {
            this.ip += (short) imm;
        }
    }

    @Override
    public void jrelGT(int imm, int rflag) {
        if (this.readRegister(rflag) > 0) {
            this.ip += (short) imm;
        }
    }

    @Override
    public void jrelLE(int imm, int rflag) {
        if (this.readRegister(rflag) <= 0) {
            this.ip += (short) imm;
        }
    }

    @Override
    public void jrelLT(int imm, int rflag) {
        if (this.readRegister(rflag) < 0) {
            this.ip += (short) imm;
        }
    }

    @Override
    public void push(int imm, int rsrc) {
        this.push((short) (this.readRegister(rsrc) + imm));
    }

    @Override
    public void pop(int imm, int rdst) {
        this.writeRegister(rdst, (short) (this.pop() - imm));
    }

    @Override
    public void mtsp(int imm, int rsrc) {
        this.sp = (short) (this.readRegister(rsrc) + imm);
    }

    @Override
    public void mtbp(int imm, int rsrc) {
        this.bp = (short) (this.readRegister(rsrc) + imm);
    }

    @Override
    public void mspt(int imm, int rdst) {
        this.writeRegister(rdst, (short) (this.sp - imm));
    }

    @Override
    public void mbpt(int imm, int rdst) {
        this.writeRegister(rdst, (short) (this.bp - imm));
    }

    @Override
    public void call(int imm) {
        // Due to how this#nextWord() is implemented, by the time this
        // instruction is actually handled here, ip would already contain the
        // address of the next instruction!
        this.push(this.ip);
        this.ip = (short) imm;
    }

    @Override
    public void ret() {
        this.ip = this.pop();
    }

    @Override
    public void enter(int imm) {
        this.push(this.bp);
        this.bp = this.sp;
        this.sp -= imm;
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
    public void ldW(int imm, int radj, int rdst) {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        this.memory.read(imm + this.readRegister(radj), buf);
        this.writeRegister(rdst, buf.flip().getShort());
    }

    @Override
    public void stW(int imm, int radj, int rsrc) {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort(this.readRegister(rsrc)).flip();
        this.memory.write(imm + this.readRegister(radj), buf);
    }

    @Override
    public void ldB(int imm, int radj, int rdst) {
        this.writeRegister(rdst, this.memory.read(imm + this.readRegister(radj)));
    }

    @Override
    public void stB(int imm, int radj, int rsrc) {
        this.memory.write(imm + this.readRegister(radj), (byte) this.readRegister(rsrc));
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
}