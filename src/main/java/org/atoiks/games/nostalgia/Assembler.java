package org.atoiks.games.nostalgia;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.util.function.UnaryOperator;

public final class Assembler {

    private static interface StringTransformer extends UnaryOperator<String> {

        // That way there's no generic parameter
    }

    private static final Map<String, Integer> GPREGS;
    private static final Map<String, Integer> FPREGS;

    static {
        final HashMap<String, Integer> fpregs = new HashMap<>();
        for (int i = 0; i < 32; ++i) {
            fpregs.put("%FP" + i, i);
            fpregs.put("%FP" + i + "D", i);
            fpregs.put("%FP" + i + "Q", (1 << 5) | i);
        }

        final HashMap<String, Integer> gpregs = new HashMap<>();
        for (int i = 0; i < 16; ++i) {
            gpregs.put("%R" + i, i);
            gpregs.put("%R" + i + "W", i);
            gpregs.put("%R" + i + "L", (0b01 << 4) | i);
            gpregs.put("%R" + i + "H", (0b10 << 4) | i);
            gpregs.put("%R" + i + "D", (0b11 << 4) | i);
        }
        // %SP is %R8D, %BP is %R9D
        gpregs.put("%SP", (0b11 << 4) | 8);
        gpregs.put("%BP", (0b11 << 4) | 9);

        FPREGS = fpregs;
        GPREGS = gpregs;
    }

    private final HashMap<Integer, String> patchtbl = new HashMap<>();

    private final HashMap<String, Integer> symtbl = new HashMap<>();
    private final HashMap<String, String> subtbl = new HashMap<>();
    private final Encoder encoder = new Encoder();

    private final LinkedHashSet<Path> searchPaths = new LinkedHashSet<>();
    private final HashSet<String> imports = new HashSet<>();
    private final ArrayDeque<String> lines = new ArrayDeque<>();

    private int origin;

    public void addSearchDir(String path) {
        this.searchPaths.add(Paths.get(path).normalize().toAbsolutePath());
    }

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
                case Opcode.OP0_RET:        // OP0 I class opcodes
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
                case Opcode.OP0_JREL_Z:
                case Opcode.OP0_JREL_NZ:
                case Opcode.OP0_JREL_GE:
                case Opcode.OP0_JREL_GT:
                case Opcode.OP0_JREL_LE:
                case Opcode.OP0_JREL_LT:
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
                case Opcode.OP0_JABS_Z:     // OP0 IRR class opcodes
                case Opcode.OP0_JABS_NZ:
                case Opcode.OP0_JABS_GE:
                case Opcode.OP0_JABS_GT:
                case Opcode.OP0_JABS_LE:
                case Opcode.OP0_JABS_LT:
                case Opcode.OP0_LD_D:
                case Opcode.OP0_ST_D:
                case Opcode.OP0_LD_W:
                case Opcode.OP0_ST_W:
                case Opcode.OP0_LD_B:
                case Opcode.OP0_ST_B:
                case Opcode.OP0_CMOV_I:
                case Opcode.OP0_CALL_Z:
                case Opcode.OP0_CALL_NZ:
                case Opcode.OP0_CALL_GE:
                case Opcode.OP0_CALL_GT:
                case Opcode.OP0_CALL_LE:
                case Opcode.OP0_CALL_LT:
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
                    tmp = this.expandFile(tmp);
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
                    tmp = this.expandFile(tmp);
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

    private String expandFile(String path) {
        final Path raw = Paths.get(path);
        for (final Path dir : this.searchPaths) {
            final Path resolved = dir.resolve(raw);
            if (Files.exists(resolved)) {
                return resolved.normalize().toAbsolutePath().toString();
            }
        }

        return raw.normalize().toAbsolutePath().toString();
    }

    private void matchInstruction(String opUpcase, String[] operands) {
        int[] buf = null;
        switch (opUpcase) {
            case "MOV.I":
                buf = checkInstrClassIR(operands);
                this.encoder.movI(buf[0], buf[1]);
                break;
            case "MOV.LO":
                buf = checkInstrClassIR(operands);
                this.encoder.movLO(buf[0], buf[1]);
                break;
            case "MOV.HI":
                buf = checkInstrClassIR(operands);
                this.encoder.movHI(buf[0], buf[1]);
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
            case "ANDN.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.andnR(buf[0], buf[1], buf[2]);
                break;
            case "ORN.R":
                buf = checkInstrClassRRR(operands);
                this.encoder.ornR(buf[0], buf[1], buf[2]);
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
                buf = checkInstrClassIRR(operands);
                this.encoder.jabsZ(buf[0], buf[1], buf[2]);
                break;
            case "JABS.NZ":
                buf = checkInstrClassIRR(operands);
                this.encoder.jabsNZ(buf[0], buf[1], buf[2]);
                break;
            case "JABS.GE":
                buf = checkInstrClassIRR(operands);
                this.encoder.jabsGE(buf[0], buf[1], buf[2]);
                break;
            case "JABS.GT":
                buf = checkInstrClassIRR(operands);
                this.encoder.jabsGT(buf[0], buf[1], buf[2]);
                break;
            case "JABS.LE":
                buf = checkInstrClassIRR(operands);
                this.encoder.jabsLE(buf[0], buf[1], buf[2]);
                break;
            case "JABS.LT":
                buf = checkInstrClassIRR(operands);
                this.encoder.jabsLT(buf[0], buf[1], buf[2]);
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
            case "PUSH.D":
                buf = checkInstrPushPop(operands, 0b11);
                this.encoder.push(buf[0] | 1);
                break;
            case "POP.D":
                buf = checkInstrPushPop(operands, 0b11);
                this.encoder.pop(buf[0] | 1);
                break;
            case "PUSH.W":
                buf = checkInstrPushPop(operands, 0b00);
                this.encoder.push(buf[0]);
                break;
            case "POP.W":
                buf = checkInstrPushPop(operands, 0b00);
                this.encoder.pop(buf[0]);
                break;
            case "RET":
                buf = checkInstrClassI(operands);
                this.encoder.ret(buf[0]);
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
            case "LDM.D":
                buf = checkInstrMultipleLDST(operands, 0b11);
                this.encoder.ldmD(buf[0], buf[1]);
                break;
            case "STM.D":
                buf = checkInstrMultipleLDST(operands, 0b11);
                this.encoder.stmD(buf[0], buf[1]);
                break;
            case "LDM.W":
                buf = checkInstrMultipleLDST(operands, 0b00);
                this.encoder.ldmW(buf[0], buf[1]);
                break;
            case "STM.W":
                buf = checkInstrMultipleLDST(operands, 0b00);
                this.encoder.stmW(buf[0], buf[1]);
                break;
            case "LDM.L":
                buf = checkInstrMultipleLDST(operands, 0b01);
                this.encoder.ldmLB(buf[0], buf[1]);
                break;
            case "STM.L":
                buf = checkInstrMultipleLDST(operands, 0b01);
                this.encoder.stmLB(buf[0], buf[1]);
                break;
            case "LDM.H":
                buf = checkInstrMultipleLDST(operands, 0b10);
                this.encoder.ldmHB(buf[0], buf[1]);
                break;
            case "STM.H":
                buf = checkInstrMultipleLDST(operands, 0b10);
                this.encoder.stmHB(buf[0], buf[1]);
                break;
            case "LDM.DS":
                buf = checkInstrMultipleLDST(operands, 0b11);
                this.encoder.ldmD(buf[0] | 1, buf[1]);
                break;
            case "STM.DS":
                buf = checkInstrMultipleLDST(operands, 0b11);
                this.encoder.stmD(buf[0] | 1, buf[1]);
                break;
            case "LDM.WS":
                buf = checkInstrMultipleLDST(operands, 0b00);
                this.encoder.ldmW(buf[0] | 1, buf[1]);
                break;
            case "STM.WS":
                buf = checkInstrMultipleLDST(operands, 0b00);
                this.encoder.stmW(buf[0] | 1, buf[1]);
                break;
            case "LDM.LS":
                buf = checkInstrMultipleLDST(operands, 0b01);
                this.encoder.ldmLB(buf[0] | 1, buf[1]);
                break;
            case "STM.LS":
                buf = checkInstrMultipleLDST(operands, 0b01);
                this.encoder.stmLB(buf[0] | 1, buf[1]);
                break;
            case "LDM.HS":
                buf = checkInstrMultipleLDST(operands, 0b10);
                this.encoder.ldmHB(buf[0] | 1, buf[1]);
                break;
            case "STM.HS":
                buf = checkInstrMultipleLDST(operands, 0b10);
                this.encoder.stmHB(buf[0] | 1, buf[1]);
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
                if (Integer.toUnsignedLong(buf[0]) > 0x1F) {
                    System.err.println("Assembler: Warning: " + opUpcase + " with immediate greater than 31: " + Integer.toUnsignedLong(buf[0]));
                }
                this.encoder.shlI(buf[0], buf[1]);
                break;
            case "SHR.I":
                buf = checkInstrClassIR(operands);
                if (Integer.toUnsignedLong(buf[0]) > 0x1F) {
                    System.err.println("Assembler: Warning: " + opUpcase + " with immediate greater than 31: " + Integer.toUnsignedLong(buf[0]));
                }
                this.encoder.shrI(buf[0], buf[1]);
                break;
            case "SAR.I":
                buf = checkInstrClassIR(operands);
                if (Integer.toUnsignedLong(buf[0]) > 0x1F) {
                    System.err.println("Assembler: Warning: " + opUpcase + " with immediate greater than 31: " + Integer.toUnsignedLong(buf[0]));
                }
                this.encoder.sarI(buf[0], buf[1]);
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
            case "CALL.Z":
                buf = checkInstrClassIRR(operands);
                this.encoder.callZ(buf[0], buf[1], buf[2]);
                break;
            case "CALL.NZ":
                buf = checkInstrClassIRR(operands);
                this.encoder.callNZ(buf[0], buf[1], buf[2]);
                break;
            case "CALL.GE":
                buf = checkInstrClassIRR(operands);
                this.encoder.callGE(buf[0], buf[1], buf[2]);
                break;
            case "CALL.GT":
                buf = checkInstrClassIRR(operands);
                this.encoder.callGT(buf[0], buf[1], buf[2]);
                break;
            case "CALL.LE":
                buf = checkInstrClassIRR(operands);
                this.encoder.callLE(buf[0], buf[1], buf[2]);
                break;
            case "CALL.LT":
                buf = checkInstrClassIRR(operands);
                this.encoder.callLT(buf[0], buf[1], buf[2]);
                break;
            case "IMUL":
                buf = checkInstrClassRRRR(operands);
                this.encoder.imul(buf[0], buf[1], buf[2], buf[3]);
                break;
            case "IDIV":
                buf = checkInstrClassRRRR(operands);
                this.encoder.idiv(buf[0], buf[1], buf[2], buf[3]);
                break;
            case "MUL":
                buf = checkInstrClassRRRR(operands);
                this.encoder.mul(buf[0], buf[1], buf[2], buf[3]);
                break;
            case "DIV":
                buf = checkInstrClassRRRR(operands);
                this.encoder.div(buf[0], buf[1], buf[2], buf[3]);
                break;
            case "IMAC":
                buf = checkInstrClassRRRR(operands);
                this.encoder.imac(buf[0], buf[1], buf[2], buf[3]);
                break;
            case "MOV.F":
                buf = checkInstrClassRFp(operands);
                this.encoder.fpext(Opcode.FPEXT_MOV_F, buf[0], buf[1]);
                break;
            case "MOV.R":
                buf = checkInstrClassFpR(operands);
                this.encoder.fpext(Opcode.FPEXT_MOV_R, buf[0], buf[1]);
                break;
            case "CVT.F":
                buf = checkInstrClassRFp(operands);
                this.encoder.fpext(Opcode.FPEXT_CVT_F, buf[0], buf[1]);
                break;
            case "CVT.R":
                buf = checkInstrClassFpR(operands);
                this.encoder.fpext(Opcode.FPEXT_CVT_R, buf[0], buf[1]);
                break;
            case "FADD":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FADD , buf[0], buf[1]);
                break;
            case "FSUB":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FSUB , buf[0], buf[1]);
                break;
            case "FMUL":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FMUL , buf[0], buf[1]);
                break;
            case "FDIV":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FDIV , buf[0], buf[1]);
                break;
            case "FMOD":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FMOD , buf[0], buf[1]);
                break;
            case "FREM":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FREM, buf[0], buf[1]);
                break;
            case "FMOV":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FMOV, buf[0], buf[1]);
                break;
            case "FRSUB":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FRSUB, buf[0], buf[1]);
                break;
            case "FRDIV":
                buf = checkInstrClassFpFp(operands);
                this.encoder.fpext(Opcode.FPEXT_FRDIV, buf[0], buf[1]);
                break;
            default:
                throw new RuntimeException("Assembler: Illegal instruction mnemonic: '" + opUpcase + "'");
        }
    }

    private int[] checkInstrClassFpR(String[] operands) {
        // Class FPEXT's Fp R: encoded as [fp, rx]
        //    OP %RX, %FP
        checkOperandCount(operands, 2);

        final int rx = getRegisterIndex(operands[0]);
        final int fp = getFpRegIndex(operands[1]);
        return new int[] { fp, rx };
    }

    private int[] checkInstrClassRFp(String[] operands) {
        // Class FPEXT's R Fp: encoded as [rx, fp]
        //    OP %FP, %RX
        checkOperandCount(operands, 2);

        final int fp = getFpRegIndex(operands[0]);
        final int rx = getRegisterIndex(operands[1]);
        return new int[] { rx, fp };
    }

    private int[] checkInstrClassFpFp(String[] operands) {
        // Class FPEXT's Fp Fp: encoded as [fps, fpd]
        //    OP %FPd, %FPs
        checkOperandCount(operands, 2);

        final int fpd = getFpRegIndex(operands[0]);
        final int fps = getFpRegIndex(operands[1]);
        return new int[] { fps, fpd };
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

    private int getRegmask(String[] operands, int start, int end, int validRexDataWidth) {
        // actually 16 bits, but use int to prevent accidental sign extension
        int regmask = 0;
        for (int i = start; i < end; ++i) {
            final int rex = getRegisterIndex(operands[i]);
            if (((rex >> 4) & 0x3) != (validRexDataWidth & 0x3)) {
                throw new RuntimeException("Assembler: Illegal register width for register mask: " + operands[i]);
            }

            final int slot = rex & 0xF;
            if (slot == 0) {
                throw new RuntimeException("Assembler: Illegal %R0 use in register mask");
            }

            regmask |= 1 << slot;
        }

        if (regmask == 0) {
            throw new RuntimeException("Assembler: Illegal register mask of 0: " + Arrays.toString(operands) + " from " + start + " to " + end);
        }

        return regmask;
    }

    private int[] checkInstrMultipleLDST(String[] operands, int validRexDataWidth) {
        // This is encoded the same as class IR, but has a very different
        // syntax (as far as the assembler is concerned)

        if (operands.length < 2) {
            throw new RuntimeException("Assembler: Illegal operands count < 2: " + Arrays.toString(operands));
        }

        final int regmask = getRegmask(operands, 0, operands.length - 1, validRexDataWidth);
        final int rx = getRegisterIndex(operands[operands.length - 1]);

        return new int[] { regmask, rx };
    }

    private int[] checkInstrPushPop(String[] operands, int validRexDataWidth) {
        // This is encoded the same as class I, but has a very different syntax
        // (as far as the assembler is concered)

        if (operands.length < 1) {
            throw new RuntimeException("Assembler: Illegal operands count < 2: " + Arrays.toString(operands));
        }

        final int regmask = getRegmask(operands, 0, operands.length, validRexDataWidth);

        return new int[] { regmask };
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

        final Integer slot = GPREGS.get(str.toUpperCase());
        if (slot == null) {
            throw new IllegalArgumentException("Assembler: Illegal register name: '" + str + "'");
        }

        return slot.intValue();
    }

    public int getFpRegIndex(String str) {
        return parseFpRegIndex(this.macroExpand(str));
    }

    public static int parseFpRegIndex(String str) {
        // See ProcessUnit.java for how REX extensions work.
        // This is different from REX on general purpose registers (%r0
        // example).

        final Integer slot = FPREGS.get(str.toUpperCase());
        if (slot == null) {
            throw new IllegalArgumentException("Assembler: Illegal float-point register name: '" + str + "'");
        }

        return slot.intValue();
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