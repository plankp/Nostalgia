package org.atoiks.games.nostalgia;

public interface MemoryHandler {

    public int getCapacity();

    public byte readOffset(int offset);
    public void writeOffset(int offset, byte b);
}