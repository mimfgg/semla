package io.semla.serialization.io;


import io.semla.exception.DeserializationException;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamReader extends CharacterReader {

    private final InputStream inputStream;

    public InputStreamReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public boolean isNull() {
        return inputStream == null;
    }

    @Override
    public int length() {
        try {
            int available = inputStream.available();
            return available > 0 ? current() + available : Integer.MAX_VALUE;
        } catch (IOException e) {
            throw new DeserializationException(e.getMessage(), e);
        }
    }

    @Override
    protected char read() {
        try {
            return (char) inputStream.read();
        } catch (IOException e) {
            throw new DeserializationException(e.getMessage(), e);
        }
    }
}
