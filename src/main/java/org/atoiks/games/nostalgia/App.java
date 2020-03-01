package org.atoiks.games.nostalgia;

import java.io.*;
import java.nio.ByteBuffer;

public class App {
    public static void main(String[] args) {
        final Screen screen = new Screen();
        screen.setVisible(true);

        // Print the welcoming message
        final String st = "Atoiks Games - Nostalgia...";
        final byte fast = Screen.makeCellAttr(7, 0);
        for (int i = 0; i < st.length(); ++i) {
            screen.setEntry(i, 0, (byte) st.charAt(i), fast);
        }
        for (int i = st.length(); i < 80 * 25; ++i) {
            screen.setEntry(i, 0, (byte) 0, fast);
        }
        screen.flush();

        final MemoryUnit mem = new MemoryUnit();
        final ProcessUnit proc = new ProcessUnit(mem);

        final ByteBuffer buffer;
        try (final Assembler asm = new Assembler(new InputStreamReader(App.class.getResourceAsStream("/check_kc_a.nos")))) {
            asm.assembleAll();
            buffer = ByteBuffer.wrap(asm.getEncoder().getBytes());
        } catch (IOException ex) {
            System.err.println("IOExceptioned: " + ex.getMessage());
            return;
        }

        mem.mapHandler(0, new GenericMemory(ByteBuffer.allocate(0x1000)));
        mem.mapHandler(0x4000, new GenericMemory(buffer.duplicate()));
        screen.setupMemory(mem);

        // Really just for human validation
        final Disassembler dis = new Disassembler(System.out, buffer.duplicate());
        dis.disassemble();

        // instruction pointer is word aligned
        proc.setIP(0x4000 / 2);

        try {
            while (true) {
                proc.executeNext();
            }
        } catch (RuntimeException ex) {
            System.out.println(ex.getMessage());
            System.out.println(proc);
        }
    }
}
