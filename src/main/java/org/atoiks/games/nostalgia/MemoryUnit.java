package org.atoiks.games.nostalgia;

import java.nio.ByteBuffer;
import java.util.*;

public final class MemoryUnit {

    private final TreeMap<Integer, MemoryHandler> regions = new TreeMap<>();

    private Optional<Map.Entry<Integer, MemoryHandler>> loadHandler(final int address) {
        final Map.Entry<Integer, MemoryHandler> entry = regions.floorEntry(address);
        if (entry != null) {
            final int start = entry.getKey();
            final MemoryHandler handler = entry.getValue();
            if (start <= address && address - start < handler.getCapacity()) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    public void mapHandler(int address, MemoryHandler handler) {
        if (this.loadHandler(address).isPresent()) {
            throw new RuntimeException("Memory Unit: handler has overlapping regions at 0x" + Integer.toString(address, 16));
        }

        regions.put(address, Objects.requireNonNull(handler));
    }

    public byte read(final int address) {
        final Map.Entry<Integer, MemoryHandler> entry = this.loadHandler(address)
                .orElseThrow(() -> new IndexOutOfBoundsException("Memory Unit: bad memory access at 0x" + Integer.toString(address, 16)));
        return entry.getValue().readOffset(address - entry.getKey());
    }

    public void write(final int address, byte b) {
        final Map.Entry<Integer, MemoryHandler> entry = this.loadHandler(address)
                .orElseThrow(() -> new IndexOutOfBoundsException("Memory Unit: bad memory access at 0x" + Integer.toString(address, 16)));
        entry.getValue().writeOffset(address - entry.getKey(), b);
    }

    public void read(final int start, ByteBuffer buf) {
        int address = start;
        while (buf.hasRemaining()) {
            buf.put(this.read(address++));
        }
    }

    public void write(final int start, ByteBuffer buf) {
        int address = start;
        while (buf.hasRemaining()) {
            this.write(address++, buf.get());
        }
    }
}