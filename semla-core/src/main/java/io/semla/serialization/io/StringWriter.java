package io.semla.serialization.io;


public class StringWriter implements CharacterWriter {

    private final StringBuilder builder = new StringBuilder(1024);

    @Override
    public CharacterWriter append(String value) {
        builder.append(value);
        return this;
    }

    @Override
    public CharacterWriter append(boolean b) {
        builder.append(b);
        return this;
    }

    @Override
    public CharacterWriter append(char c) {
        builder.append(c);
        return this;
    }

    @Override
    public CharacterWriter append(int i) {
        builder.append(i);
        return this;
    }

    @Override
    public CharacterWriter append(long l) {
        builder.append(l);
        return this;
    }

    @Override
    public CharacterWriter append(float f) {
        builder.append(f);
        return this;
    }

    @Override
    public CharacterWriter append(double d) {
        builder.append(d);
        return this;
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    @Override
    public boolean isEmpty() {
        return builder.length() == 0;
    }
}
