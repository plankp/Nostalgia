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
        for (int i = st.length(); i < 80 * 25; ++i) {
            screen.setEntry(i, 0, (byte) 0, fast);
        }
        screen.flush();

        final MemoryUnit mem = new MemoryUnit();
        final ProcessUnit proc = new ProcessUnit(mem);

        final Encoder encoder = new Encoder();

        // We look for keycode 'A', and if we do, display '!'.
        // Otherwise, we do not display anything!
        encoder.movI(0x2036, 1);    // graphics memory
        encoder.movI(0x1000, 2);    // keycode memory
        encoder.movI(65, 5);
        encoder.stB(3, 2, 5);       //  set keyboard to listen to 'A'
                                    //  {
        encoder.ldB(4, 2, 5);       //      r5 = whether or not 'A' is down
        encoder.movI(' ', 3);
        encoder.jrelZ(+1, 5);
        encoder.movI('!', 3);       //      r3 = if r5 then '!' else ' '
        encoder.stB(0, 1, 3);       //      display r3 on screen
        encoder.jabsZ(0x400e / 2, 0);   // } loop

        final ByteBuffer buffer = ByteBuffer.wrap(encoder.getBytes());

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
}
