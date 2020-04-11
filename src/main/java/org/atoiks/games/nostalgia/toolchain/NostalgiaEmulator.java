package org.atoiks.games.nostalgia.toolchain;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.atoiks.games.nostalgia.*;

public final class NostalgiaEmulator {

    public static void main(String[] args) throws IOException, UnsupportedEncodingException {
        boolean errored = false;
        boolean dspHelp = false;
        boolean fastMode = false;

        String kernelFile = null;

        final int limit = args.length;
        for (int i = 0; i < limit; ++i) {
            final String el = args[i];

            if (el.isEmpty()) {
                continue;
            }

            try {
                if (el.charAt(0) == '-') {
                    switch (el) {
                        case "-h":
                        case "--help":
                            dspHelp = true;
                            continue;
                        case "--fast":
                            fastMode = true;
                            continue;
                        case "--slow":
                            fastMode = false;
                            continue;
                        default:
                            System.out.println("Error: Unsupported option: " + el);
                            errored = true;
                            continue;
                    }
                }

                kernelFile = el;
            } catch (IndexOutOfBoundsException ex) {
                System.out.println("Error: Option " + el + " missing value after");
                errored = true;
                continue;
            }
        }

        if (dspHelp) {
            System.out.println(""
                    + "Nostalgia Emulator\n"
                    + "\n"
                    + "Usage: nosemu [options] file\n"
                    + "\n"
                    + "Options:\n"
                    + "  -h | --help            Displays this help message\n"
                    + "  --fast                 Runs at (relatively) fast mode\n"
                    + "  --slow                 Runs at (relatively) slow mode [default]\n"
                    + "\n"
                    + "Note: the file will be loaded at 0x4000");
            return;
        }

        if (errored) {
            return;
        }

        final Screen screen = new Screen();
        screen.setVisible(true);

        final MemoryUnit mem = new MemoryUnit();
        final ProcessUnit proc = new ProcessUnit(mem);

        final ByteBuffer loader = assembleProgram(new InputStreamReader(App.class.getResourceAsStream("/bootloader.nos")));

        ByteBuffer kernel = null;
        String errMsg =
                "Hmm... Looks like you haven't loaded a kernel yet!\n" +
                "(You should do that) \1"; // \1 is the smiley face

        if (kernelFile != null) {
            try {
                final byte[] bytes = Files.readAllBytes(Paths.get(kernelFile));

                // Note: We don't disassemble this one (there is a disassembler
                // tool bundled now anyway!)
                kernel = ByteBuffer.wrap(bytes);
            } catch (IOException | RuntimeException ex) {
                errMsg = ex.getMessage();
            }
        }

        if (kernel == null) {
            kernel = assembleProgram(new InputStreamReader(App.class.getResourceAsStream("/dummy_kernel.nos")));

            // flash the error message into 0x4200
            final byte[] msgBytes = new StringBuilder()
                    .append("Atoiks Games - Nostalgia...\n\n")
                    .append(errMsg)
                    .append('\0') // null terminated strings (like C)!
                    .toString()
                    .getBytes("US-ASCII");
            mem.mapHandler(0x4200, new GenericMemory(msgBytes));
        }

        mem.mapHandler(0, new GenericMemory(loader));
        mem.mapHandler(0x4000, new GenericMemory(kernel.duplicate()));
        screen.setupMemory(mem);

        try {
            while (true) {
                proc.executeNextQuanta();
                if (fastMode) {
                    proc.executeNextQuanta();
                    proc.executeNextQuanta();
                    proc.executeNextQuanta();
                }

                Thread.sleep(1);
            }
        } catch (RuntimeException | InterruptedException ex) {
            System.out.println(ex.getMessage());
            System.out.println(proc);
        }
    }

    private static ByteBuffer assembleProgram(Reader src) throws IOException {
        final Assembler asm = new Assembler();
        try (final BufferedReader br = new BufferedReader(src)) {
            asm.loadSource(br);
        }

        final ByteBuffer buffer = ByteBuffer.wrap(asm.assembleAll());

        // // Uncomment to print out the disassembly...
        // new Disassembler(System.out, buffer.duplicate()).disassembleAll();

        return buffer;
    }
}