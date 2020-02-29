package org.atoiks.games.nostalgia;

import java.nio.ByteBuffer;

public class App {
    public static void main(String[] args) {
        System.out.println("Atoiks Games - Nostalgia");

        final MemoryUnit mem = new MemoryUnit();
        final ProcessUnit proc = new ProcessUnit(mem);

        final Encoder encoder = new Encoder();
        encoder.movI(0xFFFF, 7);
        encoder.orR(0, 0, 1);
        encoder.addI(1, 1);
        encoder.jrelZ(-3, 0);

        final ByteBuffer buffer = ByteBuffer.wrap(encoder.getBytes());

        mem.mapHandler(0, new GenericMemory(ByteBuffer.allocate(0x2000)));
        mem.mapHandler(0x2000, new GenericMemory(ByteBuffer.allocate(80 * 25 * 2))); // video memory!
        mem.mapHandler(0x4000, new GenericMemory(buffer.duplicate()));

        // Really just for human validation
        final Disassembler dis = new Disassembler(System.out, buffer.duplicate());
        dis.disassemble();

        // instruction pointer is word aligned
        proc.setIP(0x4000 / 2);

        for (int i = 0; i < 10; ++i) {
            proc.executeNext();
            System.out.println(proc.toString());
        }
    }
}
