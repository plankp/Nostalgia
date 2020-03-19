package org.atoiks.games.nostalgia;

import java.io.*;
import java.util.*;
import java.util.function.UnaryOperator;

public final class Assembler {

    private static interface StringTransformer extends UnaryOperator<String> {

        // That way there's no generic parameter
    }

    private final HashMap<Integer, String> patchtbl = new HashMap<>();

    private final HashMap<String, Integer> symtbl = new HashMap<>();
    private final HashMap<String, String> subtbl = new HashMap<>();
    private final Encoder encoder = new Encoder();

    private final HashSet<String> imports = new HashSet<>();
    private final ArrayDeque<String> lines = new ArrayDeque<>();

    private int origin;

    // We do not provide a loadBuffer(String) method. Just use a StringReader.

    public void loadSource(String path) throws IOException {
        try (final FileReader fr = new FileReader(path)) {
            this.loadSource(fr);
        }
    }

    public void loadSource(Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);
        final ArrayDeque<String> buffer = new ArrayDeque<>();

        String line;
        while ((line = readNextLogicalLine(br)) != null) {
            buffer.addLast(line);
        }

        // Think of the `#include` preprocessor macro/directive in C. It would
        // expand the file in place. We do the same

        while ((line = buffer.pollLast()) != null) {
            this.lines.addFirst(line);
        }
    }

    private static String readNextLogicalLine(BufferedReader br) throws IOException {
        final StringBuilder sb = new StringBuilder();
        while (true) {
            final String line = br.readLine();
            if (line == null) {
                if (sb.length() == 0) {
                    return null;
                }
                break;
            }

            if (line.isEmpty()) {
                break;
            }

            sb.append(line);

            final int far = sb.length() - 1;
            if (sb.charAt(far) != '\\') {
                break;
            }

            sb.deleteCharAt(far);
        }

        return sb.toString();
    }

    private String nextLine() {
        return this.lines.pollFirst();
    }

    private boolean assembleNext() {
        final String line = this.nextLine();
        if (line == null) {
            // No more lines
            return false;
        }

        final StringTransformer[] ops = {
            this::processComment,
            this::processLabel,
            this::processInstr
        };

        String acc = line;
        for (final StringTransformer op : ops) {
            acc = acc.trim();
            if (acc.isEmpty()) {
                break;
            }

            acc = op.apply(acc);
        }
        return true;
    }

    public byte[] assembleAll() {
        while (this.assembleNext()) {
            // do nothing
        }

        final byte[] bytes = this.encoder.getBytes();

        for (final Map.Entry<Integer, String> entry : this.patchtbl.entrySet()) {
            final String label = entry.getValue();
            final Integer repl = this.symtbl.get(label);
            if (repl == null) {
                throw new BadConstantException(label);
            }

            // Perform the back-patch!
            //
            // Two possibilities:
            // Either we have IEX+opcode
            // Or we have IEX+REX+opcode.
            // Note: It's never REX+IEX+opcode because the encoder doesn't emit that way!

            final int addrIEX = entry.getKey();
            final int addrOpc;
            if ((Byte.toUnsignedInt(bytes[addrIEX + 2]) >> 4) == ((1 << 3) | Opcode.OP1_REX)) {
                // Need to skip the REX prefix.
                addrOpc = addrIEX + 4;
            } else {
                addrOpc = addrIEX + 2;
            }

            // Note: When back-patching, you are not allowed to delete any dead
            // IEX's!

            // Note: This only works with OP0 class opcodes (OP1 class only has
            // IEX carrying immediates anyway so we are good for now...)
            int opcode = Byte.toUnsignedInt(bytes[addrOpc]) >> 1;
            switch (opcode) {
                case Opcode.OP0_CALL:       // OP0 I class opcodes
                case Opcode.OP0_ENTER:
                {
                    final int lower = repl & 0b0000_0001_1111_1111;
                    final int widen = repl & 0b1111_1110_0000_0000;

                    // Re-emit the value corrected IEX
                    final int fixedIEX = (widen >> 9) & 0x1FFF;
                    bytes[addrIEX + 0] = (byte) ((1 << 7) | (Opcode.OP1_IEX_0 << 4) | (fixedIEX >> 8));
                    bytes[addrIEX + 1] = (byte) (fixedIEX);

                    // Fix the remaining immediate encoded by the opcode itself
                    bytes[addrOpc + 0] = (byte) ((bytes[addrOpc + 0] & 0xFE) | (lower >> 8));
                    bytes[addrOpc + 1] = (byte) (lower & 0xFF);

                    break;
                }
                case Opcode.OP0_MOV_I:      // OP0 IR class opcodes
                case Opcode.OP0_ADD_I:
                case Opcode.OP0_SUB_I:
                case Opcode.OP0_RSUB_I:
                case Opcode.OP0_JABS_Z:
                case Opcode.OP0_JABS_NZ:
                case Opcode.OP0_JABS_GE:
                case Opcode.OP0_JABS_GT:
                case Opcode.OP0_JABS_LE:
                case Opcode.OP0_JABS_LT:
                case Opcode.OP0_JREL_Z:
                case Opcode.OP0_JREL_NZ:
                case Opcode.OP0_JREL_GE:
                case Opcode.OP0_JREL_GT:
                case Opcode.OP0_JREL_LE:
                case Opcode.OP0_JREL_LT:
                case Opcode.OP0_PUSH:
                case Opcode.OP0_POP:
                case Opcode.OP0_SHL_I:
                case Opcode.OP0_SHR_I:
                case Opcode.OP0_SAR_I:
                {
                    final int lower = repl & 0b0000_0000_0011_1111;
                    final int widen = repl & 0b1111_1111_1100_0000;

                    // Re-emit the value corrected IEX
                    final int fixedIEX = (widen >> 6) & 0x1FFF;
                    bytes[addrIEX + 0] = (byte) ((1 << 7) | (Opcode.OP1_IEX_0 << 4) | (fixedIEX >> 8));
                    bytes[addrIEX + 1] = (byte) (fixedIEX);

                    // Fix the remaining immediate encoded by the opcode itself
                    bytes[addrOpc + 0] = (byte) ((bytes[addrOpc + 0] & 0xFE) | (lower >> 5));
                    bytes[addrOpc + 1] = (byte) ((bytes[addrOpc + 1] & 0x07) | ((lower & 0x1F) << 3));

                    break;
                }
                case Opcode.OP0_LD_D:       // OP0 IRR class opcodes
                case Opcode.OP0_ST_D:
                case Opcode.OP0_LD_W:
                case Opcode.OP0_ST_W:
                case Opcode.OP0_LD_B:
                case Opcode.OP0_ST_B:
                case Opcode.OP0_CMOV_I:
                {
                    final int lower = repl & 0b0000_0000_0000_0111;
                    final int widen = repl & 0b1111_1111_1111_1000;

                    // Re-emit the value corrected IEX
                    final int fixedIEX = (widen >> 3) & 0x1FFF;
                    bytes[addrIEX + 0] = (byte) ((1 << 7) | (Opcode.OP1_IEX_0 << 4) | (fixedIEX >> 8));
                    bytes[addrIEX + 1] = (byte) (fixedIEX);

                    // Fix the remaining immediate encoded by the opcode itself
                    bytes[addrOpc + 0] = (byte) ((bytes[addrOpc + 0] & 0xFE) | (lower >> 2));
                    bytes[addrOpc + 1] = (byte) ((bytes[addrOpc + 1] & 0x3F) | ((lower & 0x3) << 6));

                    break;
                }
                default:
                    throw new AssertionError("Assembler: Illegal back-patch on opcode: " + opcode);
            }
        }
        this.patchtbl.clear();

        return bytes;
    }

    private String processComment(String line) {
        final int index = line.indexOf(';');
        if (index < 0) {
            // ';' was not found, not a comment, the whole line should be
            // processed.
            return line;
        }

        // Comments run until the end of the line, so we only process the
        // things before the start of the comment.
        return line.substring(0, index);
    }

    private String processLabel(String line) {
        final int index = line.indexOf(':');
        if (index < 0) {
            // ':' was not found, not a label, the whole line should be
            // processed.
            return line;
        }

        // there is a symbol (a jump label)
        final String label = line.substring(0, index).trim();
        if (!isValidLabelName(label)) {
            throw new RuntimeException("Assembler: Illegal symbol: '" + label + "'");
        }

        final Integer prev;
        if ((prev = this.symtbl.putIfAbsent(label, this.origin + this.encoder.size())) != null) {
            throw new RuntimeException("Assembler: Redefinition of symbol: '" + label + "' previously bound at " + prev);
        }

        // Everything after the label should be processed
        return line.substring(index + 1);
    }

    private String processInstr(String line) {
        final String op;
        final String[] operands;

        {
            final int firstSplit = findFirstWhitespace(line);
            if (firstSplit < 0) {
                // nullary instructions can be like this:
                //      RET
                // (and that's all, no arguments and therefore no whitespace)
                // In that case, the whole line is the op
                op = line;
                operands = new String[0];
            } else {
                // most instructions are like this:
                //      ADD.R r1, r2, r3
                op = line.substring(0, firstSplit);

                // no need +1 because trim will handle it
                String rem = line.substring(firstSplit).trim();
                operands = rem.split(",");
                for (int i = 0; i < operands.length; ++i) {
                    operands[i] = operands[i].trim();
                }
            }
        }

        // mov.i and MOV.I (and everything in between is the same)
        final String mne = op.toUpperCase();
        if (mne.charAt(0) == '.') {
            this.matchDirective(mne, operands);
        } else {
            this.matchInstruction(mne, operands);
        }

        // Everything is used up. Nothing left to process
        return "";
    }

    private void matchDirective(String opUpcase, String[] operands) {
        String tmp;
        switch (opUpcase) {
            case ".SET":
                checkOperandCount(operands, 2);
                if ((tmp = this.subtbl.putIfAbsent(operands[0], operands[1])) != null) {
                    throw new RuntimeException("Assembler: Redefinition of '" + operands[0] + "' previously bound to '" + tmp + "'");
                }
                break;
            case ".UNSET":
                checkOperandCount(operands, 1);
                this.subtbl.remove(operands[0]);
                break;
            case ".ALIGN": {
                checkOperandCount(operands, 1);
                final int value = getConstant(operands[0]);
                this.encoder.emit((byte) 0, value - this.encoder.size() % value);
                break;
            }
            case ".ZERO":
                checkOperandCount(operands, 1);
                this.encoder.emit((byte) 0, getConstant(operands[0]) - this.encoder.size());
                break;
            case ".EMIT":
                for (final String op : operands) {
                    this.encoder.emit((byte) getConstant(op), 1);
                }
                break;
            case ".ORG":
                checkOperandCount(operands, 1);
                this.origin = getConstant(operands[0]);
                break;
            case ".INCLUDE":
                // Note: due to how operands are splitted, current path names
                // cannot contain commas (that should be ok for most cases?)
                checkOperandCount(operands, 1);
                tmp = this.macroExpand(operands[0]);
                try {
                    tmp = new File(tmp).getCanonicalPath();
                    this.loadSource(tmp);
                } catch (IOException ex) {
                    throw new RuntimeException("Assembler: Cannot load file: " + tmp);
                }
                break;
            case ".IMPORT":
                // Note: due to how operands are splitted, current path names
                // cannot contain commas (that should be ok for most cases?)
                checkOperandCount(operands, 1);
                tmp = this.macroExpand(operands[0]);
                try {
                    tmp = new File(tmp).getCanonicalPath();
                    if (this.imports.add(tmp)) {
                        this.loadSource(tmp);
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("Assembler: Cannot load file: " + tmp);
                }
                break;
            default:
                throw new RuntimeException("Assembler: Illegal directive: '" + opUpcase + "'");
        }
    }

    private void matchInstruction(String opUpcase, String[] operands) {
        int[] buf = null;
        switch (opUpcase) {
            case "MOV.I":
                buf = checkInstrClassIR(operands);
                this.encoder.movI(buf[0], buf[1]);
                break;
            case "ADD.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.addR(buf[0], buf[1], buf[2]);
                break;
            case "SUB.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.subR(buf[0], buf[1], buf[2]);
                break;
            case "AND.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.andR(buf[0], buf[1], buf[2]);
                break;
            case "OR.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.orR(buf[0], buf[1], buf[2]);
                break;
            case "XOR.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.xorR(buf[0], buf[1], buf[2]);
                break;
            case "NAND.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.nandR(buf[0], buf[1], buf[2]);
                break;
            case "NOR.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.norR(buf[0], buf[1], buf[2]);
                break;
            case "NXOR.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.nxorR(buf[0], buf[1], buf[2]);
                break;
            case "ADD.I":
                buf = checkInstrClassIR(operands);
                this.encoder.addI(buf[0], buf[1]);
                break;
            case "SUB.I":
                buf = checkInstrClassIR(operands);
                this.encoder.subI(buf[0], buf[1]);
                break;
            case "RSUB.I":
                buf = checkInstrClassIR(operands);
                this.encoder.rsubI(buf[0], buf[1]);
                break;
            case "JABS.Z":
                buf = checkInstrClassIR(operands);
                this.encoder.jabsZ(buf[0], buf[1]);
                break;
            case "JABS.NZ":
                buf = checkInstrClassIR(operands);
                this.encoder.jabsNZ(buf[0], buf[1]);
                break;
            case "JABS.GE":
                buf = checkInstrClassIR(operands);
                this.encoder.jabsGE(buf[0], buf[1]);
                break;
            case "JABS.GT":
                buf = checkInstrClassIR(operands);
                this.encoder.jabsGT(buf[0], buf[1]);
                break;
            case "JABS.LE":
                buf = checkInstrClassIR(operands);
                this.encoder.jabsLE(buf[0], buf[1]);
                break;
            case "JABS.LT":
                buf = checkInstrClassIR(operands);
                this.encoder.jabsLT(buf[0], buf[1]);
                break;
            case "JREL.Z":
                buf = checkInstrClassIR(operands);
                this.encoder.jrelZ(buf[0], buf[1]);
                break;
            case "JREL.NZ":
                buf = checkInstrClassIR(operands);
                this.encoder.jrelNZ(buf[0], buf[1]);
                break;
            case "JREL.GE":
                buf = checkInstrClassIR(operands);
                this.encoder.jrelGE(buf[0], buf[1]);
                break;
            case "JREL.GT":
                buf = checkInstrClassIR(operands);
                this.encoder.jrelGT(buf[0], buf[1]);
                break;
            case "JREL.LE":
                buf = checkInstrClassIR(operands);
                this.encoder.jrelLE(buf[0], buf[1]);
                break;
            case "JREL.LT":
                buf = checkInstrClassIR(operands);
                this.encoder.jrelLT(buf[0], buf[1]);
                break;
            case "PUSH":
                buf = checkInstrClassIR(operands);
                this.encoder.push(buf[0], buf[1]);
                break;
            case "POP":
                buf = checkInstrClassIR(operands);
                this.encoder.pop(buf[0], buf[1]);
                break;
            case "CALL":
                buf = checkInstrClassI(operands);
                this.encoder.call(buf[0]);
                break;
            case "RET":
                checkOperandCount(operands, 0);
                this.encoder.ret();
                break;
            case "ENTER":
                buf = checkInstrClassI(operands);
                this.encoder.enter(buf[0]);
                break;
            case "LEAVE":
                checkOperandCount(operands, 0);
                this.encoder.leave();
                break;
            case "LD.D":
                buf = checkInstrClassIRR(operands);
                this.encoder.ldD(buf[0], buf[1], buf[2]);
                break;
            case "ST.D":
                buf = checkInstrClassIRR(operands);
                this.encoder.stD(buf[0], buf[1], buf[2]);
                break;
            case "LD.W":
                buf = checkInstrClassIRR(operands);
                this.encoder.ldW(buf[0], buf[1], buf[2]);
                break;
            case "ST.W":
                buf = checkInstrClassIRR(operands);
                this.encoder.stW(buf[0], buf[1], buf[2]);
                break;
            case "LD.B":
                buf = checkInstrClassIRR(operands);
                this.encoder.ldB(buf[0], buf[1], buf[2]);
                break;
            case "ST.B":
                buf = checkInstrClassIRR(operands);
                this.encoder.stB(buf[0], buf[1], buf[2]);
                break;
            case "SHL.R":
            case "SAL.R":
                // no distinction between arithmetic and logical left shift
                buf = checkInstrClassRRR(operands);
                this.encoder.shlR(buf[0], buf[1], buf[2]);
                break;
            case "SHR.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.shrR(buf[0], buf[1], buf[2]);
                break;
            case "SAR.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.sarR(buf[0], buf[1], buf[2]);
                break;
            case "SHL.I":
            case "SAL.I":
                // semantically no distinction between arithmetic and logical left shift
                buf = checkInstrClassIR(operands);
                if (Integer.toUnsignedLong(buf[0]) > 0x0F) {
                    System.err.println("Assembler: Warning: " + opUpcase + " with immediate greater than 15: " + Integer.toUnsignedLong(buf[0]));
                }
                this.encoder.shlI(buf[0], buf[1]);
                break;
            case "SHR.I":
                buf = checkInstrClassIR(operands);
                if (Integer.toUnsignedLong(buf[0]) > 0x0F) {
                    System.err.println("Assembler: Warning: " + opUpcase + " with immediate greater than 15: " + Integer.toUnsignedLong(buf[0]));
                }
                this.encoder.shrI(buf[0], buf[1]);
                break;
            case "SAR.I":
                buf = checkInstrClassIR(operands);
                if (Integer.toUnsignedLong(buf[0]) > 0x0F) {
                    System.err.println("Assembler: Warning: " + opUpcase + " with immediate greater than 15: " + Integer.toUnsignedLong(buf[0]));
                }
                this.encoder.sarI(buf[0], buf[1]);
                break;
            case "PUSH.3":
                buf = checkInstrClassRRR(operands);
                this.encoder.push3(buf[0], buf[1], buf[2]);
                break;
            case "POP.3":
                buf = checkInstrClassRRR(operands);
                this.encoder.pop3(buf[0], buf[1], buf[2]);
                break;
            case "CMOV.I":
                buf = checkInstrClassIRR(operands);
                this.encoder.cmovI(buf[0], buf[1], buf[2]);
                break;
            case "CMOV.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.cmovR(buf[0], buf[1], buf[2]);
                break;
            case "PADD.W":
                buf = checkInstrClassRRR(operands);
                this.encoder.paddW(buf[0], buf[1], buf[2]);
                break;
            case "PSUB.W":
                buf = checkInstrClassRRR(operands);
                this.encoder.psubW(buf[0], buf[1], buf[2]);
                break;
            case "PADD.B":
                buf = checkInstrClassRRR(operands);
                this.encoder.paddB(buf[0], buf[1], buf[2]);
                break;
            case "PSUB.B":
                buf = checkInstrClassRRR(operands);
                this.encoder.psubB(buf[0], buf[1], buf[2]);
                break;
            case "MUL":
                buf = checkInstrClassRRRR(operands);
                this.encoder.mul(buf[0], buf[1], buf[2], buf[3]);
                break;
            case "DIV":
                buf = checkInstrClassRRRR(operands);
                this.encoder.div(buf[0], buf[1], buf[2], buf[3]);
                break;
            default:
                throw new RuntimeException("Assembler: Illegal instruction mnemonic: '" + opUpcase + "'");
        }
    }

    private int[] checkInstrClassI(String[] operands) {
        // Class I:     encoded as [imm]
        //    OP IMM
        checkOperandCount(operands, 1);

        try {
            final int imm = getConstant(operands[0]);
            return new int[] { imm };
        } catch (BadConstantException ex) {
            this.patchtbl.put(this.encoder.size(), ex.constant);

            // Force an IEX (and -1 is the largest unsigned int)
            return new int[] { -1 };
        }
    }

    private int[] checkInstrClassIR(String[] operands) {
        // Class IR:    encoded as [imm, rx]
        //    OP %RX, IMM
        checkOperandCount(operands, 2);

        final int rx  = getRegisterIndex(operands[0]);
        try {
            final int imm = getConstant(operands[1]);
            return new int[] { imm, rx };
        } catch (BadConstantException ex) {
            this.patchtbl.put(this.encoder.size(), ex.constant);

            // Force an IEX (and -1 is the largest unsigned int)
            return new int[] { -1, rx };
        }
    }

    private int[] checkInstrClassIRR(String[] operands) {
        // Class IRR:   encoded as [imm, rk, rx]
        //    OP %RX, IMM, %RK
        checkOperandCount(operands, 3);

        final int rx  = getRegisterIndex(operands[0]);
        final int rk  = getRegisterIndex(operands[2]);
        try {
            final int imm = getConstant(operands[1]);
            return new int[] { imm, rk, rx };
        } catch (BadConstantException ex) {
            this.patchtbl.put(this.encoder.size(), ex.constant);

            // Force an IEX (and -1 is the largest unsigned int)
            return new int[] { -1, rk, rx };
        }
    }

    private int[] checkInstrClassRRR(String[] operands) {
        // Class RRR:   encoded as [ru, rv, rx]
        //    OP %RX, %RU, %RV
        checkOperandCount(operands, 3);

        final int rx  = getRegisterIndex(operands[0]);
        final int ru  = getRegisterIndex(operands[1]);
        final int rv  = getRegisterIndex(operands[2]);
        return new int[] { ru, rv, rx };
    }

    private int[] checkInstrClassRRRR(String[] operands) {
        // Class RRR:   encoded as [rs1, rs2, rd2, rd1]
        //    OP %RD1, %RD2, %RS1, %RS2
        checkOperandCount(operands, 4);

        final int rd1 = getRegisterIndex(operands[0]);
        final int rd2 = getRegisterIndex(operands[1]);
        final int rs1 = getRegisterIndex(operands[2]);
        final int rs2 = getRegisterIndex(operands[3]);
        return new int[] { rs1, rs2, rd2, rd1 };
    }

    private static void checkOperandCount(String[] operands, int count) {
        if (operands.length != count) {
            throw new RuntimeException("Assembler: Illegal operands count != " + count + ": " + Arrays.toString(operands));
        }
    }

    public String macroExpand(String str) {
        // if $str is macro-defined, expand it, then try to expand it again.
        // otherwise, return it directly.

        String acc = str;
        while (true) {
            final String other = this.subtbl.get(acc);
            if (other == null) {
                break;
            }

            acc = other;
        }

        return acc;
    }

    public int getConstant(String str) {
        final String exp = this.macroExpand(str);
        try {
            return parseConstant(exp);
        } catch (BadConstantException ex) {
            // Try to see if it is a already existing label!
            final Integer addr = this.symtbl.get(exp);
            if (addr != null) {
                return addr;
            }

            // Reaching here means it's not a label we know, rethrow!
            throw ex;
        }
    }

    public static int parseConstant(String str) {
        if (str.length() >= 3) {
            if (str.charAt(0) == '0') {
                switch (str.charAt(1)) {
                    case 'b':   return Integer.parseInt(str.substring(2), 2);
                    case 'c':   return Integer.parseInt(str.substring(2), 8);
                    case 'd':   return Integer.parseInt(str.substring(2), 10);
                    case 'x':   return Integer.parseInt(str.substring(2), 16);
                    default:    throw new BadConstantException(str);
                }
            }
        }

        try {
            // parse as base 10
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            throw new BadConstantException(str);
        }
    }

    public int getRegisterIndex(String str) {
        return parseRegisterIndex(this.macroExpand(str));
    }

    public static int parseRegisterIndex(String str) {
        // See ProcessUnit.java for how REX extensions work.
        // But in short, we have 3 bits from the REX and 3 bits encoded by the
        // instruction itself. Due to the way it is encoded, we can instead of
        // doing:
        //      (0bxyz << 3) | 0babc
        //      where xy  is the register width
        //            z   is the high bit (allows R8 to R15 access)
        //            abc is the part encoded in the instruction (not the REX)
        //
        // Do this directly:
        //      (0bxy << 4) | 0bzabc

        switch (str.toUpperCase()) {
            case "%R0":
            case "%R0W":
                return 0;
            case "%R0L":
                return (0b01 << 4) | 0;
            case "%R0H":
                return (0b10 << 4) | 0;
            case "%R0D":
                return (0b11 << 4) | 0;

            case "%R1":
            case "%R1W":
                return 1;
            case "%R1L":
                return (0b01 << 4) | 1;
            case "%R1H":
                return (0b10 << 4) | 1;
            case "%R1D":
                return (0b11 << 4) | 1;

            case "%R2":
            case "%R2W":
                return 2;
            case "%R2L":
                return (0b01 << 4) | 2;
            case "%R2H":
                return (0b10 << 4) | 2;
            case "%R2D":
                return (0b11 << 4) | 2;

            case "%R3":
            case "%R3W":
                return 3;
            case "%R3L":
                return (0b01 << 4) | 3;
            case "%R3H":
                return (0b10 << 4) | 3;
            case "%R3D":
                return (0b11 << 4) | 3;

            case "%R4":
            case "%R4W":
                return 4;
            case "%R4L":
                return (0b01 << 4) | 4;
            case "%R4H":
                return (0b10 << 4) | 4;
            case "%R4D":
                return (0b11 << 4) | 4;

            case "%R5":
            case "%R5W":
                return 5;
            case "%R5L":
                return (0b01 << 4) | 5;
            case "%R5H":
                return (0b10 << 4) | 5;
            case "%R5D":
                return (0b11 << 4) | 5;

            case "%R6":
            case "%R6W":
                return 6;
            case "%R6L":
                return (0b01 << 4) | 6;
            case "%R6H":
                return (0b10 << 4) | 6;
            case "%R6D":
                return (0b11 << 4) | 6;

            case "%R7":
            case "%R7W":
                return 7;
            case "%R7L":
                return (0b01 << 4) | 7;
            case "%R7H":
                return (0b10 << 4) | 7;
            case "%R7D":
                return (0b11 << 4) | 7;

            case "%R8":
            case "%R8W":
                return 8;
            case "%R8L":
                return (0b01 << 4) | 8;
            case "%R8H":
                return (0b10 << 4) | 8;
            case "%R8D":
            case "%SP": // Note: SP is always dword access
                return (0b11 << 4) | 8;

            case "%R9":
            case "%R9W":
                return 9;
            case "%R9L":
                return (0b01 << 4) | 9;
            case "%R9H":
                return (0b10 << 4) | 9;
            case "%R9D":
            case "%BP": // Note: BP is always dword access
                return (0b11 << 4) | 9;

            case "%R10":
            case "%R10W":
                return 10;
            case "%R10L":
                return (0b01 << 4) | 10;
            case "%R10H":
                return (0b10 << 4) | 10;
            case "%R10D":
                return (0b11 << 4) | 10;

            case "%R11":
            case "%R11W":
                return 11;
            case "%R11L":
                return (0b01 << 4) | 11;
            case "%R11H":
                return (0b10 << 4) | 11;
            case "%R11D":
                return (0b11 << 4) | 11;

            case "%R12":
            case "%R12W":
                return 12;
            case "%R12L":
                return (0b01 << 4) | 12;
            case "%R12H":
                return (0b10 << 4) | 12;
            case "%R12D":
                return (0b11 << 4) | 12;

            case "%R13":
            case "%R13W":
                return 13;
            case "%R13L":
                return (0b01 << 4) | 13;
            case "%R13H":
                return (0b10 << 4) | 13;
            case "%R13D":
                return (0b11 << 4) | 13;

            case "%R14":
            case "%R14W":
                return 14;
            case "%R14L":
                return (0b01 << 4) | 14;
            case "%R14H":
                return (0b10 << 4) | 14;
            case "%R14D":
                return (0b11 << 4) | 14;

            case "%R15":
            case "%R15W":
                return 15;
            case "%R15L":
                return (0b01 << 4) | 15;
            case "%R15H":
                return (0b10 << 4) | 15;
            case "%R15D":
                return (0b11 << 4) | 15;

            default:
                throw new IllegalArgumentException("Assembler: Illegal register name: '" + str + "'");
        }
    }

    public static boolean isValidLabelName(String str) {
        final int limit = str == null ? 0 : str.length();
        if (limit == 0) {
            return false;
        }

        // Length cannot be negative, string at least has one char
        if (!Character.isUnicodeIdentifierStart(str.charAt(0))) {
            return false;
        }

        for (int i = 1; i < limit; ++i) {
            if (!Character.isUnicodeIdentifierPart(str.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    public static int findFirstWhitespace(String str) {
        final int limit = str == null ? 0 : str.length();
        for (int i = 0; i < limit; ++i) {
            if (Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }

        // Not found, return -1 like every other Java search method.
        return -1;
    }
}

class BadConstantException extends IllegalArgumentException {

    public final String constant;

    public BadConstantException(String constant) {
        super("Assembler: Illegal constant: '" + constant + "'");
        this.constant = constant;
    }
}