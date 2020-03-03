package io.semla.serialization.io;


public class StringReader extends CharacterReader {

    private final String content;

    public StringReader(String content) {
        this.content = content;
    }

    @Override
    public boolean isNull() {
        return content == null;
    }

    @Override
    public int length() {
        return content.length();
    }

    @Override
    protected char read() {
        return content.charAt(index());
    }
}
