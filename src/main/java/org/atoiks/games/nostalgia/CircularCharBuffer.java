package org.atoiks.games.nostalgia;

public final class CircularCharBuffer {

    private final char[] buffer;
    private int head;
    private int tail;
    private int alive; // overwrite, but don't read too far.

    public CircularCharBuffer(int size) {
        this.buffer = new char[size];
    }

    public int capacity() {
        return this.buffer.length;
    }

    public int size() {
        return this.alive;
    }

    public boolean isEmpty() {
        return this.alive == 0;
    }

    public void clear() {
        this.alive = 0;
    }

    public void offer(char ch) {
        this.buffer[this.head++] = ch;
        this.head %= this.buffer.length;

        this.alive = Math.min(this.buffer.length, this.alive + 1);
    }

    public char take() {
        if (this.isEmpty()) {
            // Not an error, but return null-char
            return '\0';
        }

        final char ch = this.buffer[this.tail++];
        this.tail %= this.buffer.length;

        this.alive = Math.max(0, this.alive - 1);
        return ch;
    }
}