package io.semla.serialization.io;

public interface CharacterWriter {

    default CharacterWriter append(Object object) {
        return append(String.valueOf(object));
    }

    default CharacterWriter newLine() {
        append(System.lineSeparator());
        return this;
    }

    CharacterWriter append(String value);

    CharacterWriter append(boolean b);

    CharacterWriter append(char c);

    CharacterWriter append(int i);

    CharacterWriter append(long l);

    CharacterWriter append(float f);

    CharacterWriter append(double d);

    boolean isEmpty();
}
