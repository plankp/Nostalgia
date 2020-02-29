package org.atoiks.games.nostalgia;

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
        screen.flush();

        final MemoryUnit mem = new MemoryUnit();
        final ProcessUnit proc = new ProcessUnit(mem);

        final Encoder encoder = new Encoder();
        encoder.movI(0xFFFF, 7);
        encoder.orR(0, 0, 1);
        encoder.addI(1, 1);
        encoder.jrelZ(-3, 0);

        final ByteBuffer buffer = ByteBuffer.wrap(encoder.getBytes());

        mem.mapHandler(0, new GenericMemory(ByteBuffer.allocate(0x1000)));
        mem.mapHandler(0x4000, new GenericMemory(buffer.duplicate()));
        screen.setupMemory(mem);

        // Really just for human validation
        final Disassembler dis = new Disassembler(System.out, buffer.duplicate());
        dis.disassemble();

        // instruction pointer is word aligned
        proc.setIP(0x4000 / 2);

        while (true) {
            proc.executeNext();
        }
    }
}
