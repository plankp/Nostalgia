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
        final ByteBuffer kernel = assembleProgram(System.out, new InputStreamReader(App.class.getResourceAsStream("/moving_dot.nos")));

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
            asm.assembleAll();

            final ByteBuffer buffer = ByteBuffer.wrap(asm.getEncoder().getBytes());

            if (debugDis != null) {
                new Disassembler(debugDis, buffer.duplicate()).disassembleAll();
            }

            return buffer;
        }
    }
}
