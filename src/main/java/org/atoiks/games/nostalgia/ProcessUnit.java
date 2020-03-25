package org.atoiks.games.nostalgia;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public final class ProcessUnit implements Decoder.InstrStream, InstrVisitor {

    // Keep this in sync with Assembler.java!
    private static final int REG_SLOT_SP = 7;   // R8
    private static final int REG_SLOT_BP = 8;   // R9

    // R0 is always 0 (writing to it causes the value to be discarded)
    private final int[] regs = new int[15];

    private int ip;

    // These are secret registers (it doesn't even show up in toString!)
    private short iexImm;
    private byte rexRA;
    private byte rexRB;
    private byte rexRC;
    private byte rexRD;

    private final InstrTiming timing = new InstrTiming();
    private int quanta; // just models instruction timing

    // Super random, but can we get a counter register lulz!?

    private final MemoryUnit memory;
    private final Decoder decoder;

    public ProcessUnit(MemoryUnit memory) {
        this.memory = Objects.requireNonNull(memory);
        this.decoder = new Decoder(this);
    }

    private void adjustQuanta() {
        // It is tempting to just reset the quanta. DO NOT DO THAT! We need to
        // take the remaining time from the last operation into account.

        this.quanta = (this.quanta % 6) + 6;
    }

    public void executeNextQuanta() {
        this.adjustQuanta();

        while (true) {
            // Perform a double decoding:
            final int save = this.ip;

            // Check if we have enough quanta to execute the next instruction
            int cost = 0;
            while (cost == 0) {
                // bypass the instruction prefixes
                this.decoder.decode(this.timing);
                cost = this.timing.getTiming();
            }

            // Restore the ip (modified by the first decode)
            this.ip = save;

            // If it's too costly, then we do not execute the instruction.
            if (cost > this.quanta) {
                break;
            }

            // Adjust the quanta
            this.quanta -= cost;

            // Then execute the actual instruction
            this.executeNext();
        }
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

    public void reset() {
        Arrays.fill(this.regs, (short) 0);

        this.ip = 0;

        this.iexImm = 0;
        this.resetREX();

        this.quanta = 0;
    }

    private void resetREX() {
        this.rexRA = 0;
        this.rexRB = 0;
        this.rexRC = 0;
        this.rexRD = 0;
    }

    public void setIP(int ip) {
        this.ip = ip;
    }

    public void pushDword(int val) {
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(val).flip();
        this.push(buf);
    }

    public void pushWord(int val) {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort((short) val).flip();
        this.push(buf);
    }

    private void push(final ByteBuffer buf) {
        this.regs[REG_SLOT_SP] -= buf.capacity();
        memory.write(this.regs[REG_SLOT_SP], buf);
    }

    public int popDword() {
        final ByteBuffer buf = ByteBuffer.allocate(4);
        this.pop(buf);
        return buf.flip().getInt();
    }

    public short popWord() {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        this.pop(buf);
        return buf.flip().getShort();
    }

    private void pop(final ByteBuffer buf) {
        memory.read(this.regs[REG_SLOT_SP], buf);
        this.regs[REG_SLOT_SP] += buf.capacity();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("R0: 0x00000000");
        for (int i = 0; i < this.regs.length; ++i) {
            sb.append(' ').append('R').append(i + 1).append(": 0x")
                    .append(String.format("%08x", this.regs[i]));
        }

        sb.append('\n').append("ip: 0x").append(String.format("%08x", this.ip));

        return sb.toString();
    }

    @Override
    public short nextWord() {
        final ByteBuffer buf = ByteBuffer.allocate(2);
        memory.read(this.ip, buf);
        this.ip += 2;
        return buf.flip().getShort();
    }

    @Override
    public void illegalOp(int fullWord) {
        throw new RuntimeException("Process Unit: Illegal opcode: " + fullWord);
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

    public void writeRegDword(int slot, int val) {
        if (slot == 0) {
            return;
        }

        this.regs[slot - 1] = val;
    }

    public void writeRegWord(int slot, int val) {
        if (slot == 0) {
            return;
        }

        this.regs[slot - 1] &= 0xFFFF0000;
        this.regs[slot - 1] |= 0x0000FFFF & val;
    }

    public void writeRegHighByte(int slot, int val) {
        if (slot == 0) {
            return;
        }

        this.regs[slot - 1] &= (0xFFFF00FF);
        this.regs[slot - 1] |= (0x000000FF & val) << 8;
    }

    public void writeRegLowByte(int slot, int val) {
        if (slot == 0) {
            return;
        }

        this.regs[slot - 1] &= 0xFFFFFF00;
        this.regs[slot - 1] |= 0x000000FF & val;
    }

    public int readRegDword(int slot) {
        if (slot == 0) {
            return 0;
        }

        return this.regs[slot - 1];
    }

    public short readRegWord(int slot) {
        if (slot == 0) {
            return 0;
        }

        return (short) this.regs[slot - 1];
    }

    public byte readRegHighByte(int slot) {
        if (slot == 0) {
            return 0;
        }

        // Must be logical right shift!
        return (byte) (this.regs[slot - 1] >>> 8);
    }

    public byte readRegLowByte(int slot) {
        if (slot == 0) {
            return 0;
        }

        return (byte) this.regs[slot - 1];
    }

    private void rexWrite(int slot, int rex, int val) {
        switch ((rex >> 1) & 0x3) {
            case 0:     this.writeRegWord(slot, val); break;
            case 1:     this.writeRegLowByte(slot, val); break;
            case 2:     this.writeRegHighByte(slot, val); break;
            case 3:     this.writeRegDword(slot, val); break;
            default:    throw new AssertionError("Wtf illegal math mask with 3 produced a value out of 1..3?");
        }
    }

    private int rexReadSigned(int slot, int rex) {
        // Java does sign extension by default, so just return the values.
        switch ((rex >> 1) & 0x3) {
            case 0:     return this.readRegWord(slot);
            case 1:     return this.readRegLowByte(slot);
            case 2:     return this.readRegHighByte(slot);
            case 3:     return this.readRegDword(slot);
            default:    throw new AssertionError("Wtf illegal math mask with 3 produced a value out of 1..3?");
        }
    }

    private int rexReadUnsigned(int slot, int rex) {
        switch ((rex >> 1) & 0x3) {
            case 0:     return Short.toUnsignedInt(this.readRegWord(slot));
            case 1:     return Byte.toUnsignedInt(this.readRegLowByte(slot));
            case 2:     return Byte.toUnsignedInt(this.readRegHighByte(slot));
            case 3:     return this.readRegDword(slot);
            default:    throw new AssertionError("Wtf illegal math mask with 3 produced a value out of 1..3?");
        }
    }

    @Override
    public void movI(int imm6, int rA) {
        // Since immediates are at most 16 bits, it undergoes sign extension
        // (not zero extension!) when destination is through a dword access.

        final int imm  = this.loadImm6(imm6);
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        this.rexWrite(rdst, this.rexRA, imm);
        this.resetREX();
    }

    @Override
    public void movLO(int imm6, int rA) {
        final int imm  = this.loadImm6(imm6);
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        int old = this.readRegDword(rdst);
        switch ((this.rexRA >> 1) & 0x3) {
            case 0:
                old &= 0x0000FF00;
                old |= imm & 0xFF;
                break;
            case 1:
                old &= 0x000000F0;
                old |= imm & 0xF;
                break;
            case 2:
                old &= 0x0000F000;
                old |= (imm & 0xF) << 8;
                break;
            case 3:
                old &= 0xFFFF0000;
                old |= imm & 0xFFFF;
                break;
        }
        this.writeRegDword(rdst, old);
        this.resetREX();
    }

    @Override
    public void movHI(int imm6, int rA) {
        final int imm  = this.loadImm6(imm6);
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        int old = this.readRegDword(rdst);
        switch ((this.rexRA >> 1) & 0x3) {
            case 0:
                old &= 0x000000FF;
                old |= (imm & 0xFF) << 8;
                break;
            case 1:
                old &= 0x0000000F;
                old |= (imm & 0xF) << 4;
                break;
            case 2:
                old &= 0x00000F00;
                old |= (imm & 0xF) << 12;
                break;
            case 3:
                old &= 0x0000FFFF;
                old |= (imm & 0xFFFF) << 16;
                break;
        }
        this.writeRegDword(rdst, old);
        this.resetREX();
    }

    @Override
    public void addR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadSigned(rlhs, this.rexRC);
        final int rhs = this.rexReadSigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs + rhs);
        this.resetREX();
    }

    @Override
    public void subR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadSigned(rlhs, this.rexRC);
        final int rhs = this.rexReadSigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs - rhs);
        this.resetREX();
    }

    @Override
    public void andR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadSigned(rlhs, this.rexRC);
        final int rhs = this.rexReadSigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs & rhs);
        this.resetREX();
    }

    @Override
    public void orR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs | rhs);
        this.resetREX();
    }

    @Override
    public void xorR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs ^ rhs);
        this.resetREX();
    }

    @Override
    public void nandR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, ~(lhs & rhs));
        this.resetREX();
    }

    @Override
    public void norR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, ~(lhs | rhs));
        this.resetREX();
    }

    @Override
    public void nxorR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadSigned(rlhs, this.rexRC);
        final int rhs = this.rexReadSigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, ~(lhs ^ rhs));
        this.resetREX();
    }

    @Override
    public void addI(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);
        final int rsd = ((this.rexRA & 0x1) << 3) | rA;

        final int out = this.rexReadSigned(rsd, this.rexRA) + imm;
        this.rexWrite(rsd, this.rexRA, out);
        this.resetREX();
    }

    @Override
    public void subI(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);
        final int rsd = ((this.rexRA & 0x1) << 3) | rA;

        final int out = this.rexReadSigned(rsd, this.rexRA) - imm;
        this.rexWrite(rsd, this.rexRA, out);
        this.resetREX();
    }

    @Override
    public void rsubI(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);
        final int rsd = ((this.rexRA & 0x1) << 3) | rA;

        final int out = imm - this.rexReadSigned(rsd, this.rexRA);
        this.rexWrite(rsd, this.rexRA, out);
        this.resetREX();
    }

    @Override
    public void jabsZ(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) == 0) {
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void jabsNZ(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) != 0) {
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void jabsGE(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) >= 0) {
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void jabsGT(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) > 0) {
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void jabsLE(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) <= 0) {
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void jabsLT(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) < 0) {
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void jrelZ(int imm6, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm6(imm6);
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) == 0) {
            this.ip += imm;
        }
        this.resetREX();
    }

    @Override
    public void jrelNZ(int imm6, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm6(imm6);
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) != 0) {
            this.ip += imm;
        }
        this.resetREX();
    }

    @Override
    public void jrelGE(int imm6, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm6(imm6);
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) >= 0) {
            this.ip += imm;
        }
        this.resetREX();
    }

    @Override
    public void jrelGT(int imm6, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm6(imm6);
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) > 0) {
            this.ip += imm;
        }
        this.resetREX();
    }

    @Override
    public void jrelLE(int imm6, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm6(imm6);
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) <= 0) {
            this.ip += imm;
        }
        this.resetREX();
    }

    @Override
    public void jrelLT(int imm6, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm6(imm6);
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) < 0) {
            this.ip += imm;
        }
        this.resetREX();
    }

    @Override
    public void push(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);

        this.internalPush(this.rexRA, rA, imm);
        this.resetREX();
    }

    private void internalPush(int rex, int reg, int immediate) {
        // Note: byte values are promoted to word before the push. In other
        // words, each push consumes either a word size or a dword size.

        final int rsrc = ((rex & 0x1) << 3) | reg;
        final int value = this.rexReadSigned(rsrc, rex) + immediate;

        if (((rex >> 1) & 0x3) == 3) {
            this.pushDword(value);
        } else {
            this.pushWord((short) value);
        }
    }

    @Override
    public void pop(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);

        this.internalPop(this.rexRA, rA, imm);
        this.resetREX();
    }

    private void internalPop(int rex, int reg, int immediate) {
        // Note: the amount of stack space restored is dependent on the REX
        // prefix. Also, like the push instruction, byte pops are actually word
        // pops.

        final int rdst = ((rex & 0x1) << 3) | reg;

        final int value;
        if (((rex >> 1) & 0x3) == 3) {
            value = this.popDword();
        } else {
            value = this.popWord();
        }

        this.rexWrite(rdst, rex, value - immediate);
    }

    @Override
    public void ret() {
        this.ip = this.popDword();
    }

    @Override
    public void enter(int imm9) {
        this.pushDword(this.regs[REG_SLOT_BP]);
        this.regs[REG_SLOT_BP] = this.regs[REG_SLOT_SP];
        this.regs[REG_SLOT_SP] -= this.loadImm9(imm9);
    }

    @Override
    public void leave() {
        this.regs[REG_SLOT_SP] = this.regs[REG_SLOT_BP];
        this.regs[REG_SLOT_BP] = this.popDword();
    }

    @Override
    public void ldD(int imm3, int rB, int rA) {
        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int address = imm + this.rexReadSigned(radj, this.rexRB);
        final ByteBuffer buf = ByteBuffer.allocate(4);
        this.memory.read(address, buf);

        this.rexWrite(rdst, this.rexRA, buf.flip().getInt());
        this.resetREX();
    }

    @Override
    public void stD(int imm3, int rB, int rA) {
        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rsrc = ((this.rexRA & 0x1) << 3) | rA;

        final int address = imm + this.rexReadSigned(radj, this.rexRB);
        final ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(this.rexReadSigned(rsrc, this.rexRA)).flip();

        this.memory.write(address, buf);
        this.resetREX();
    }

    @Override
    public void ldW(int imm3, int rB, int rA) {
        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int address = imm + this.rexReadSigned(radj, this.rexRB);
        final ByteBuffer buf = ByteBuffer.allocate(2);
        this.memory.read(address, buf);

        this.rexWrite(rdst, this.rexRA, buf.flip().getShort());
        this.resetREX();
    }

    @Override
    public void stW(int imm3, int rB, int rA) {
        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rsrc = ((this.rexRA & 0x1) << 3) | rA;

        final int address = imm + this.rexReadSigned(radj, this.rexRB);
        final ByteBuffer buf = ByteBuffer.allocate(2);
        buf.putShort((short) this.rexReadSigned(rsrc, this.rexRA)).flip();

        this.memory.write(address, buf);
        this.resetREX();
    }

    @Override
    public void ldB(int imm3, int rB, int rA) {
        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int address = imm + this.rexReadSigned(radj, this.rexRB);
        final byte out = this.memory.read(address);

        this.rexWrite(rdst, this.rexRA, out);
        this.resetREX();
    }

    @Override
    public void stB(int imm3, int rB, int rA) {
        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rsrc = ((this.rexRA & 0x1) << 3) | rA;

        final int address = imm + this.rexReadSigned(radj, this.rexRB);
        final byte out = (byte) this.rexReadSigned(rsrc, this.rexRA);

        this.memory.write(address, out);
        this.resetREX();
    }

    @Override
    public void ldmD(int imm6, int rA) {
        final int mask = this.loadImm6(imm6);
        final int rbase = ((this.rexRA & 0x1) << 3) | rA;

        final int base = this.rexReadUnsigned(rbase, this.rexRA);
        final ByteBuffer buf = ByteBuffer.allocate(4);

        for (int index = 0, addr = base; index < 16; ++index) {
            if ((mask & (1 << index)) != 0) {
                this.memory.read(addr, buf);
                this.writeRegDword(index, buf.flip().getInt());

                addr += 4;
                buf.flip();
            }
        }

        this.resetREX();
    }

    @Override
    public void stmD(int imm6, int rA) {
        final int mask = this.loadImm6(imm6);
        final int rbase = ((this.rexRA & 0x1) << 3) | rA;

        final int base = this.rexReadUnsigned(rbase, this.rexRA);
        final ByteBuffer buf = ByteBuffer.allocate(4);

        for (int index = 15, addr = base; index >= 0; --index) {
            if ((mask & (1 << index)) != 0) {
                addr -= 4;

                buf.putInt(this.readRegDword(index));
                this.memory.write(addr, buf.flip());

                buf.flip();
            }
        }

        this.resetREX();
    }

    @Override
    public void ldmW(int imm6, int rA) {
        final int mask = this.loadImm6(imm6);
        final int rbase = ((this.rexRA & 0x1) << 3) | rA;

        final int base = this.rexReadUnsigned(rbase, this.rexRA);
        final ByteBuffer buf = ByteBuffer.allocate(2);

        for (int index = 0, addr = base; index < 16; ++index) {
            if ((mask & (1 << index)) != 0) {
                this.memory.read(addr, buf);
                this.writeRegWord(index, buf.flip().getInt());

                addr += 2;
                buf.flip();
            }
        }

        this.resetREX();
    }

    @Override
    public void stmW(int imm6, int rA) {
        final int mask = this.loadImm6(imm6);
        final int rbase = ((this.rexRA & 0x1) << 3) | rA;

        final int base = this.rexReadUnsigned(rbase, this.rexRA);
        final ByteBuffer buf = ByteBuffer.allocate(2);

        for (int index = 15, addr = base; index >= 0; --index) {
            if ((mask & (1 << index)) != 0) {
                addr -= 2;

                buf.putInt(this.readRegWord(index));
                this.memory.write(addr, buf.flip());

                buf.flip();
            }
        }

        this.resetREX();
    }

    @Override
    public void ldmB(int imm6, int rA) {
        final int mask = this.loadImm6(imm6);
        final int rbase = ((this.rexRA & 0x1) << 3) | rA;

        final int base = this.rexReadUnsigned(rbase, this.rexRA);

        for (int index = 0, addr = base; index < 16; ++index) {
            if ((mask & (1 << index)) != 0) {
                this.writeRegLowByte(index, this.memory.read(addr));
                addr++;
            }
        }

        this.resetREX();
    }

    @Override
    public void stmB(int imm6, int rA) {
        final int mask = this.loadImm6(imm6);
        final int rbase = ((this.rexRA & 0x1) << 3) | rA;

        final int base = this.rexReadUnsigned(rbase, this.rexRA);

        for (int index = 15, addr = base; index >= 0; --index) {
            if ((mask & (1 << index)) != 0) {
                addr--;
                this.memory.write(addr, this.readRegLowByte(index));
            }
        }

        this.resetREX();
    }

    @Override
    public void shlR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs << rhs);
        this.resetREX();
    }

    @Override
    public void shrR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs >>> rhs);
        this.resetREX();
    }

    @Override
    public void sarR(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        this.rexWrite(rdst, this.rexRA, lhs >> rhs);
        this.resetREX();
    }

    @Override
    public void shlI(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);
        final int rsd = ((this.rexRA & 0x1) << 3) | rA;

        final int out = this.rexReadUnsigned(rsd, this.rexRA) << imm;
        this.rexWrite(rsd, this.rexRA, out);
        this.resetREX();
    }

    @Override
    public void shrI(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);
        final int rsd = ((this.rexRA & 0x1) << 3) | rA;

        final int out = this.rexReadUnsigned(rsd, this.rexRA) >>> imm;
        this.rexWrite(rsd, this.rexRA, out);
        this.resetREX();
    }

    @Override
    public void sarI(int imm6, int rA) {
        final int imm = this.loadImm6(imm6);
        final int rsd = ((this.rexRA & 0x1) << 3) | rA;

        final int out = this.rexReadUnsigned(rsd, this.rexRA) >> imm;
        this.rexWrite(rsd, this.rexRA, out);
        this.resetREX();
    }

    @Override
    public void push3(int rC, int rB, int rA) {
        this.internalPush(this.rexRC, rC, 0);
        this.internalPush(this.rexRB, rB, 0);
        this.internalPush(this.rexRA, rA, 0);

        this.resetREX();
    }

    @Override
    public void pop3(int rC, int rB, int rA) {
        this.internalPop(this.rexRA, rA, 0);
        this.internalPop(this.rexRB, rB, 0);
        this.internalPop(this.rexRC, rC, 0);

        this.resetREX();
    }

    @Override
    public void cmovI(int imm3, int rB, int rA) {
        // The immediate slot is consumed regardless of the flag!

        final int imm = this.loadImm3(imm3);
        final int rflag = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRB) != 0) {
            this.rexWrite(rdst, this.rexRA, imm);
        }
        this.resetREX();
    }

    @Override
    public void cmovR(int rC, int rB, int rA) {
        // The value is read from the source register regardless of the flag!

        final int rsrc = ((this.rexRC & 0x1) << 3) | rC;
        final int rflag = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        final int out = this.rexReadSigned(rsrc, this.rexRC);
        if (this.rexReadSigned(rflag, this.rexRB) != 0) {
            this.rexWrite(rdst, this.rexRA, out);
        }
        this.resetREX();
    }

    @Override
    public void paddW(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        // Read the values as is
        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        // Split them into words
        final short lh = (short) (lhs >>> 16);
        final short ll = (short) lhs;
        final short rh = (short) (rhs >>> 16);
        final short rl = (short) rhs;

        // Parallel add them
        this.rexWrite(rdst, this.rexRA, 0
                | (((lh + rh) & 0xFFFF) << 16)
                | (((ll + rl) & 0xFFFF)));
        this.resetREX();
    }

    @Override
    public void psubW(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        // Read the values as is
        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        // Split them into words
        final short lh = (short) (lhs >>> 16);
        final short ll = (short) lhs;
        final short rh = (short) (rhs >>> 16);
        final short rl = (short) rhs;

        // Parallel sub them
        this.rexWrite(rdst, this.rexRA, 0
                | (((lh - rh) & 0xFFFF) << 16)
                | (((ll - rl) & 0xFFFF)));
        this.resetREX();
    }

    @Override
    public void paddB(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        // Read the values as is
        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        // Split them into bytes
        final byte lhh = (byte) (lhs >>> 24);
        final byte lhl = (byte) (lhs >>> 16);
        final byte llh = (byte) (lhs >>> 8);
        final byte lll = (byte) lhs;
        final byte rhh = (byte) (rhs >>> 24);
        final byte rhl = (byte) (rhs >>> 16);
        final byte rlh = (byte) (rhs >>> 8);
        final byte rll = (byte) rhs;

        // Parallel add them
        this.rexWrite(rdst, this.rexRA, 0 
                | (((lhh + rhh) & 0xFF) << 24)
                | (((lhl + rhl) & 0xFF) << 16)
                | (((llh + rlh) & 0xFF) << 8)
                | (((lll + rll) & 0xFF)));
        this.resetREX();
    }

    @Override
    public void psubB(int rC, int rB, int rA) {
        final int rlhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rrhs = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        // Read the values as is
        final int lhs = this.rexReadUnsigned(rlhs, this.rexRC);
        final int rhs = this.rexReadUnsigned(rrhs, this.rexRB);

        // Split them into bytes
        final byte lhh = (byte) (lhs >>> 24);
        final byte lhl = (byte) (lhs >>> 16);
        final byte llh = (byte) (lhs >>> 8);
        final byte lll = (byte) lhs;
        final byte rhh = (byte) (rhs >>> 24);
        final byte rhl = (byte) (rhs >>> 16);
        final byte rlh = (byte) (rhs >>> 8);
        final byte rll = (byte) rhs;

        // Parallel add them
        this.rexWrite(rdst, this.rexRA, 0 
                | (((lhh - rhh) & 0xFF) << 24)
                | (((lhl - rhl) & 0xFF) << 16)
                | (((llh - rlh) & 0xFF) << 8)
                | (((lll - rll) & 0xFF)));
        this.resetREX();
    }

    @Override
    public void callZ(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) == 0) {
            this.pushDword(this.ip);
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void callNZ(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) != 0) {
            this.pushDword(this.ip);
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void callGE(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) >= 0) {
            this.pushDword(this.ip);
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void callGT(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) > 0) {
            this.pushDword(this.ip);
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void callLE(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) <= 0) {
            this.pushDword(this.ip);
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void callLT(int imm3, int rB, int rA) {
        // The immediate is loaded regardless of the branch!

        final int imm = this.loadImm3(imm3);
        final int radj = ((this.rexRB & 0x1) << 3) | rB;
        final int rflag = ((this.rexRA & 0x1) << 3) | rA;

        if (this.rexReadSigned(rflag, this.rexRA) < 0) {
            this.pushDword(this.ip);
            this.ip = imm + this.rexReadSigned(radj, this.rexRB);
        }
        this.resetREX();
    }

    @Override
    public void iex(int imm12) {
        // Note: we save all 12 bits, but not all 12 bits are used.
        // See loadImm3/6/9 or the instruction format in Opcode.java for more.

        this.iexImm = (short) imm12;
    }

    @Override
    public void rex(int rD, int rC, int rB, int rA) {
        // Note: REX prefixes are each 3 bits long.
        //
        // The bit ranges:
        // 0    => Prepended to the register slot for registers R8 to R15.
        // 1, 2 => Defines the register width. Can be either 0, 1, 2, or 3.
        //         0 - word access (legacy reasons), bit widths [0, 15]
        //         1 - low byte access             , bit widths [0,  7]
        //         2 - high byte access            , bit widths [8, 15]
        //         3 - dword access                , bit widths [0, 31]
        //     31 --------- 15 ----- 7 ----- 0
        //         Anything out of the bit widths is not touched!

        this.rexRA = (byte) rA;
        this.rexRB = (byte) rB;
        this.rexRC = (byte) rC;
        this.rexRD = (byte) rD;
    }

    @Override
    public void imul(int rD, int rC, int rB, int rA) {
        // Note: if two destination registers are the same, then the value it
        // contains is undefined. Except if the registers were both R0, in
        // which case both high and low parts of the product are discarded.

        final int rlhs = ((this.rexRD & 0x1) << 3) | rD;
        final int rrhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rdlo = ((this.rexRB & 0x1) << 3) | rB;
        final int rdhi = ((this.rexRA & 0x1) << 3) | rA;

        // Sign extend to 64 bits
        final long lhs = this.rexReadSigned(rlhs, this.rexRD);
        final long rhs = this.rexReadSigned(rrhs, this.rexRC);

        // Note: tmp is long type because i32 * i32 -> i64
        final long tmp = lhs * rhs;

        // Now we need to partition tmp. Here is how it works:
        //
        // for rdlo:
        //   byte  => take [ 0,  7]
        //   word  => take [ 0, 15]
        //   dword => take [ 0, 31]
        //
        // for rdhi:
        //   byte  => take [ 8, 15]
        //   word  => take [16, 31]
        //   dword => take [32, 64]
        //
        // As you can see, it's highly recommended to use the same REX prefix
        // for both destinations registers. (It was either this or UB!)

        // rdlo
        switch ((this.rexRB >> 1) & 0x3) {
            case 0:
                this.writeRegWord(rdlo, (int) tmp);
                break;
            case 1:
                this.writeRegLowByte(rdlo, (int) tmp);
                break;
            case 2:
                this.writeRegHighByte(rdlo, (int) tmp);
                break;
            case 3:
                this.writeRegDword(rdlo, (int) tmp);
                break;
        }

        // rdhi
        switch ((this.rexRA >> 1) & 0x3) {
            case 0:
                this.writeRegWord(rdhi, (int) (tmp >>> 16));
                break;
            case 1:
                this.writeRegLowByte(rdhi, (int) (tmp >>> 8));
                break;
            case 2:
                this.writeRegHighByte(rdhi, (int) (tmp >>> 8));
                break;
            case 3:
                this.writeRegDword(rdhi, (int) (tmp >>> 32));
                break;
        }

        this.resetREX();
    }

    @Override
    public void idiv(int rD, int rC, int rB, int rA) {
        // Note: if two destination registers are the same, then the value it
        // contains is undefined. Except if the registers were both R0, in
        // which case both quotient and remainder are discarded.

        final int rlhs  = ((this.rexRD & 0x1) << 3) | rD;
        final int rrhs  = ((this.rexRC & 0x1) << 3) | rC;
        final int rdrem = ((this.rexRB & 0x1) << 3) | rB;
        final int rdquo = ((this.rexRA & 0x1) << 3) | rA;

        // Sign extend to 64 bits
        final long lhs = this.rexReadSigned(rlhs, this.rexRD);
        final long rhs = this.rexReadSigned(rrhs, this.rexRC);

        // let the JVM handle crash on division by zero...
        final long quo = lhs / rhs;
        final long rem = lhs % rhs;

        this.rexWrite(rdrem, this.rexRB, (int) rem);
        this.rexWrite(rdquo, this.rexRA, (int) quo);
        this.resetREX();
    }

    @Override
    public void mul(int rD, int rC, int rB, int rA) {
        // Note: if two destination registers are the same, then the value it
        // contains is undefined. Except if the registers were both R0, in
        // which case both high and low parts of the product are discarded.

        final int rlhs = ((this.rexRD & 0x1) << 3) | rD;
        final int rrhs = ((this.rexRC & 0x1) << 3) | rC;
        final int rdlo = ((this.rexRB & 0x1) << 3) | rB;
        final int rdhi = ((this.rexRA & 0x1) << 3) | rA;

        // Zero extend to 64 bits
        final long lhs = Integer.toUnsignedLong(this.rexReadUnsigned(rlhs, this.rexRD));
        final long rhs = Integer.toUnsignedLong(this.rexReadUnsigned(rrhs, this.rexRC));

        // Note: tmp is long type because i32 * i32 -> i64
        final long tmp = lhs * rhs;

        // Now we need to partition tmp. Here is how it works:
        //
        // for rdlo:
        //   byte  => take [ 0,  7]
        //   word  => take [ 0, 15]
        //   dword => take [ 0, 31]
        //
        // for rdhi:
        //   byte  => take [ 8, 15]
        //   word  => take [16, 31]
        //   dword => take [32, 64]
        //
        // As you can see, it's highly recommended to use the same REX prefix
        // for both destinations registers. (It was either this or UB!)

        // rdlo
        switch ((this.rexRB >> 1) & 0x3) {
            case 0:
                this.writeRegWord(rdlo, (int) tmp);
                break;
            case 1:
                this.writeRegLowByte(rdlo, (int) tmp);
                break;
            case 2:
                this.writeRegHighByte(rdlo, (int) tmp);
                break;
            case 3:
                this.writeRegDword(rdlo, (int) tmp);
                break;
        }

        // rdhi
        switch ((this.rexRA >> 1) & 0x3) {
            case 0:
                this.writeRegWord(rdhi, (int) (tmp >>> 16));
                break;
            case 1:
                this.writeRegLowByte(rdhi, (int) (tmp >>> 8));
                break;
            case 2:
                this.writeRegHighByte(rdhi, (int) (tmp >>> 8));
                break;
            case 3:
                this.writeRegDword(rdhi, (int) (tmp >>> 32));
                break;
        }

        this.resetREX();
    }

    @Override
    public void div(int rD, int rC, int rB, int rA) {
        // Note: if two destination registers are the same, then the value it
        // contains is undefined. Except if the registers were both R0, in
        // which case both quotient and remainder are discarded.

        final int rlhs  = ((this.rexRD & 0x1) << 3) | rD;
        final int rrhs  = ((this.rexRC & 0x1) << 3) | rC;
        final int rdrem = ((this.rexRB & 0x1) << 3) | rB;
        final int rdquo = ((this.rexRA & 0x1) << 3) | rA;

        final long lhs = Integer.toUnsignedLong(this.rexReadUnsigned(rlhs, this.rexRD));
        final long rhs = Integer.toUnsignedLong(this.rexReadUnsigned(rrhs, this.rexRC));

        // let the JVM handle crash on division by zero...
        final long quo = lhs / rhs;
        final long rem = lhs % rhs;

        this.rexWrite(rdrem, this.rexRB, (int) rem);
        this.rexWrite(rdquo, this.rexRA, (int) quo);
        this.resetREX();
    }

    @Override
    public void imac(int rD, int rC, int rB, int rA) {
        final int rlhs = ((this.rexRD & 0x1) << 3) | rD;
        final int rrhs = ((this.rexRC & 0x1) << 3) | rC;
        final int racc = ((this.rexRB & 0x1) << 3) | rB;
        final int rdst = ((this.rexRA & 0x1) << 3) | rA;

        // Sign extend to 64 bits
        final long lhs = this.rexReadSigned(rlhs, this.rexRD);
        final long rhs = this.rexReadSigned(rrhs, this.rexRC);

        this.rexWrite(rdst, this.rexRA, (int) (lhs * rhs + this.rexReadSigned(racc, this.rexRB)));
        this.resetREX();
    }
}

final class InstrTiming implements InstrVisitor {

    private int timingBank;

    public int getTiming() {
        return this.timingBank;
    }

    @Override
    public void illegalOp(int fullWord) {
        // Operation is illegal anyway, timing is irrelevant. We give it 0
        // meaning if it did something, it took no time!
        this.timingBank = 0;
    }

    @Override
    public void movI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void movLO(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void movHI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void addR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void subR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void andR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void orR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void xorR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void nandR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void norR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void nxorR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void addI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void subI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void rsubI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void jabsZ(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jabsNZ(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jabsGE(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jabsGT(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jabsLE(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jabsLT(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jrelZ(int imm6, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jrelNZ(int imm6, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jrelGE(int imm6, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jrelGT(int imm6, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jrelLE(int imm6, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void jrelLT(int imm6, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void push(int imm6, int rsrc) {
        // Treat like an ordinary load store
        this.timingBank = 4;
    }

    @Override
    public void pop(int imm6, int rdst) {
        // Treat like an ordinary load store
        this.timingBank = 4;
    }

    @Override
    public void ret() {
        // Most of the cost comes from the implicit pop
        this.timingBank = 5;
    }

    @Override
    public void enter(int imm9) {
        this.timingBank = 5;
    }

    @Override
    public void leave() {
        this.timingBank = 5;
    }

    @Override
    public void ldD(int imm3, int radj, int rdst) {
        this.timingBank = 4;
    }

    @Override
    public void stD(int imm3, int radj, int rsrc) {
        this.timingBank = 4;
    }

    @Override
    public void ldW(int imm3, int radj, int rdst) {
        this.timingBank = 4;
    }

    @Override
    public void stW(int imm3, int radj, int rsrc) {
        this.timingBank = 4;
    }

    @Override
    public void ldB(int imm3, int radj, int rdst) {
        this.timingBank = 4;
    }

    @Override
    public void stB(int imm3, int radj, int rsrc) {
        this.timingBank = 4;
    }

    @Override
    public void ldmD(int imm3, int rbase) {
        this.timingBank = 6;
    }

    @Override
    public void stmD(int imm3, int rbase) {
        this.timingBank = 6;
    }

    @Override
    public void ldmW(int imm3, int rbase) {
        this.timingBank = 6;
    }

    @Override
    public void stmW(int imm3, int rbase) {
        this.timingBank = 6;
    }

    @Override
    public void ldmB(int imm3, int rbase) {
        this.timingBank = 6;
    }

    @Override
    public void stmB(int imm3, int rbase) {
        this.timingBank = 6;
    }

    @Override
    public void shlR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void shrR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void sarR(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void shlI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void shrI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void sarI(int imm6, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void push3(int rC, int rB, int rA) {
        this.timingBank = 6;
    }

    @Override
    public void pop3(int rC, int rB, int rA) {
        this.timingBank = 6;
    }

    @Override
    public void cmovI(int imm3, int rflag, int rdst) {
        // Faster than using a branch
        this.timingBank = 1;
    }

    @Override
    public void cmovR(int rsrc, int rflag, int rdst) {
        // Faster than using a branch
        this.timingBank = 1;
    }

    @Override
    public void paddW(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void psubW(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void paddB(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void psubB(int rlhs, int rrhs, int rdst) {
        this.timingBank = 1;
    }

    @Override
    public void callZ(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void callNZ(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void callGE(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void callGT(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void callLE(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void callLT(int imm3, int radj, int rflag) {
        // Cost is like super high because it needs to fetch the value, check
        // the condition, then branch!
        this.timingBank = 6;
    }

    @Override
    public void iex(int imm12) {
        // Instruction prefixes: assume they take no time to decode.
        this.timingBank = 0;
    }

    @Override
    public void rex(int rD, int rC, int rB, int rA) {
        // Instruction prefixes: assume they take no time to decode.
        this.timingBank = 0;
    }

    @Override
    public void imul(int rlhs, int rrhs, int rdlo, int rdhi) {
        this.timingBank = 2;
    }

    @Override
    public void idiv(int rlhs, int rrhs, int rdrem, int rdquo) {
        this.timingBank = 3;
    }

    @Override
    public void mul(int rlhs, int rrhs, int rdlo, int rdhi) {
        this.timingBank = 2;
    }

    @Override
    public void div(int rlhs, int rrhs, int rdrem, int rdquo) {
        this.timingBank = 3;
    }

    @Override
    public void imac(int rlhs, int rrhs, int racc, int rdst) {
        this.timingBank = 2;
    }
}