package io.semla.serialization.io;

import io.semla.exception.SerializationException;

import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamWriter implements CharacterWriter {

    private final OutputStream outputStream;
    private int length;

    public OutputStreamWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    private void write(byte[] bytes) {
        try {
            length += bytes.length;
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new SerializationException(e.getMessage(), e);
        }
    }

    @Override
    public CharacterWriter append(String value) {
        write(value.getBytes());
        return this;
    }

    @Override
    public CharacterWriter append(boolean b) {
        append(Boolean.toString(b));
        return this;
    }

    @Override
    public CharacterWriter append(char c) {
        append(Character.toString(c));
        return this;
    }

    @Override
    public CharacterWriter append(int i) {
        append(Integer.toString(i));
        return this;
    }

    @Override
    public CharacterWriter append(long l) {
        append(Long.toString(l));
        return this;
    }

    @Override
    public CharacterWriter append(float f) {
        append(Float.toString(f));
        return this;
    }

    @Override
    public CharacterWriter append(double d) {
        append(Double.toString(d));
        return this;
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }
}
