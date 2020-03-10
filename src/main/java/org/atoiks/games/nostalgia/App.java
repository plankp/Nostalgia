package org.atoiks.games.nostalgia;

import java.io.*;
import java.nio.ByteBuffer;

public class App {
    public static void main(String[] args) throws IOException {
        final Screen screen = new Screen();
        screen.setVisible(true);

        final MemoryUnit mem = new MemoryUnit();
        final ProcessUnit proc = new ProcessUnit(mem);

        System.out.println("===== Assembling loader =====");
        final ByteBuffer loader = assembleProgram(System.out, new InputStreamReader(App.class.getResourceAsStream("/bootloader.nos")));

        System.out.println("===== Assembling kernel =====");

        ByteBuffer kernel = null;
        String errMsg =
                "Hmm... Looks like you haven't loaded a kernel yet!\n" +
                "(You should do that) \1"; // \1 is the smiley face
        if (args.length == 1) {
            try {
                kernel = assembleProgram(System.out, new FileReader(args[0]));
            } catch (RuntimeException ex) {
                errMsg = ex.getMessage();
            }
        }

        if (kernel == null) {
            kernel = assembleProgram(System.out, new InputStreamReader(App.class.getResourceAsStream("/dummy_kernel.nos")));

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
                proc.executeNext();
                Thread.sleep(1);
            }
        } catch (RuntimeException | InterruptedException ex) {
            System.out.println(ex.getMessage());
            System.out.println(proc);
        }
    }

    // Will close the stream after read!
    public static ByteBuffer assembleProgram(PrintStream debugDis, Reader src) throws IOException {
        try (final Assembler asm = new Assembler(src)) {
            final ByteBuffer buffer = ByteBuffer.wrap(asm.assembleAll());

            if (debugDis != null) {
                new Disassembler(debugDis, buffer.duplicate()).disassembleAll();
            }

            return buffer;
        }
    }
}
