package org.atoiks.games.nostalgia;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class GenericMemory implements MemoryHandler {

    public final ByteBuffer buffer;

    public GenericMemory(ByteBuffer buffer) {
        this.buffer = Objects.requireNonNull(buffer);
    }

    public GenericMemory(byte[] array) {
        this.buffer = ByteBuffer.wrap(array);
    }

    @Override
    public int getCapacity() {
        return this.buffer.capacity();
    }

    @Override
    public byte readOffset(int offset) {
        return this.buffer.get(offset);
    }

    @Override
    public void writeOffset(int offset, byte b) {
        this.buffer.put(offset, b);
    }
}