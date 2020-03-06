package org.atoiks.games.nostalgia;

import java.io.*;
import java.util.*;
import java.util.function.UnaryOperator;

public final class Assembler implements Closeable {

    private static interface StringTransformer extends UnaryOperator<String> {

        // That way there's no generic parameter
    }

    private final HashMap<String, Integer> symtbl = new HashMap<>();
    private final HashMap<String, String> subtbl = new HashMap<>();
    private final Encoder encoder = new Encoder();

    private final BufferedReader reader;

    public Assembler(Reader reader) {
        this.reader = new BufferedReader(reader);
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    public Encoder getEncoder() {
        return this.encoder;
    }

    public boolean assembleNext() throws IOException {
        final String line = this.reader.readLine();
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

    public void assembleAll() throws IOException {
        while (this.assembleNext()) {
            // do nothing
        }
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
        if ((prev = this.symtbl.putIfAbsent(label, this.encoder.size())) != null) {
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
            case ".ZERO":
                checkOperandCount(operands, 1);
                this.encoder.emit((byte) 0, getConstant(operands[0]) - this.encoder.size());
                break;
            case ".EMIT":
                for (final String op : operands) {
                    this.encoder.emit((byte) getConstant(op), 1);
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
            case "MTSP":
                buf = checkInstrClassIR(operands);
                this.encoder.mtsp(buf[0], buf[1]);
                break;
            case "MTBP":
                buf = checkInstrClassIR(operands);
                this.encoder.mtbp(buf[0], buf[1]);
                break;
            case "MSPT":
                buf = checkInstrClassIR(operands);
                this.encoder.mspt(buf[0], buf[1]);
                break;
            case "MBPT":
                buf = checkInstrClassIR(operands);
                this.encoder.mbpt(buf[0], buf[1]);
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
            case "INNER":
                buf = checkInstrClassRRR(operands);
                this.encoder.inner(buf[0], buf[1], buf[2]);
                break;
            case "OUTER":
                buf = checkInstrClassRRR(operands);
                this.encoder.outer(buf[0], buf[1], buf[2]);
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
            default:
                throw new RuntimeException("Assembler: Illegal instruction mnemonic: '" + opUpcase + "'");
        }
    }

    private int[] checkInstrClassI(String[] operands) {
        // Class I:     encoded as [imm]
        //    OP IMM
        checkOperandCount(operands, 1);

        final int imm = getConstant(operands[0]);
        return new int[] { imm };
    }

    private int[] checkInstrClassIR(String[] operands) {
        // Class IR:    encoded as [imm, rx]
        //    OP %RX, IMM
        checkOperandCount(operands, 2);

        final int rx  = getRegisterIndex(operands[0]);
        final int imm = getConstant(operands[1]);
        return new int[] { imm, rx };
    }

    private int[] checkInstrClassIRR(String[] operands) {
        // Class IRR:   encoded as [imm, rk, rx]
        //    OP %RX, IMM, %RK
        checkOperandCount(operands, 3);

        final int rx  = getRegisterIndex(operands[0]);
        final int imm = getConstant(operands[1]);
        final int rk  = getRegisterIndex(operands[2]);
        return new int[] { imm, rk, rx };
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
        return parseConstant(this.macroExpand(str));
    }

    public static int parseConstant(String str) {
        if (str.length() >= 3) {
            if (str.charAt(0) == '0') {
                switch (str.charAt(1)) {
                    case 'b':   return Integer.parseInt(str.substring(2), 2);
                    case 'c':   return Integer.parseInt(str.substring(2), 8);
                    case 'd':   return Integer.parseInt(str.substring(2), 10);
                    case 'x':   return Integer.parseInt(str.substring(2), 16);
                    default:    throw new IllegalArgumentException("Assembler: Illegal constant: '" + str + "'");
                }
            }
        }

        try {
            // parse as base 10
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Assembler: Illegal constant: '" + str + "'");
        }
    }

    public int getRegisterIndex(String str) {
        return parseRegisterIndex(this.macroExpand(str));
    }

    public static int parseRegisterIndex(String str) {
        switch (str) {
            case "%r0":
            case "%R0":
                return 0;
            case "%r1":
            case "%R1":
                return 1;
            case "%r2":
            case "%R2":
                return 2;
            case "%r3":
            case "%R3":
                return 3;
            case "%r4":
            case "%R4":
                return 4;
            case "%r5":
            case "%R5":
                return 5;
            case "%r6":
            case "%R6":
                return 6;
            case "%r7":
            case "%R7":
                return 7;
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