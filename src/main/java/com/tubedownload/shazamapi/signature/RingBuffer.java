package com.tubedownload.shazamapi.signature;

import java.util.function.Supplier;

final class RingBuffer<T> {
    private final Object[] values;
    private int position;
    private int numWritten;

    RingBuffer(int size, Supplier<T> defaultValue) {
        values = new Object[size];
        for (int i = 0; i < size; i++) {
            values[i] = defaultValue.get();
        }
    }

    int size() {
        return values.length;
    }

    int position() {
        return position;
    }

    int numWritten() {
        return numWritten;
    }

    void append(T value) {
        values[position] = value;
        position = (position + 1) % values.length;
        numWritten++;
    }

    void writeSequential(short[] batch) {
        for (short sample : batch) {
            values[position] = sample;
            position = (position + 1) % values.length;
            numWritten++;
        }
    }

    @SuppressWarnings("unchecked")
    T get(int index) {
        return (T) values[Math.floorMod(index, values.length)];
    }
}
