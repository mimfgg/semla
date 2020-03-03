package io.semla.serialization.io;

import io.semla.exception.DeserializationException;

import java.util.function.Predicate;


public abstract class CharacterReader {

    public static final char EOF = ((char) -1);

    protected int index = -1;
    protected int column = -1;
    protected int line;
    protected boolean stashed = false;
    protected char current;

    public int index() {
        return index;
    }

    public int column() {
        return column;
    }

    public int line() {
        return line;
    }

    public abstract int length();

    public char current() {
        return current;
    }

    public boolean hasNext() {
        return index < length() - 1;
    }

    public abstract boolean isNull();

    protected abstract char read();

    public char next() {
        if (stashed) {
            stashed = false;
            return current;
        }
        if (hasNext()) {
            index++;
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            current = read();
            if (current == '\r') {
                current = next();
            }
        } else {
            current = EOF;
        }
        return current;
    }

    public void stashCurrent() {
        stashed = true;
    }

    public char nextNonWhiteSpaceCharacter() {
        return nextUntil(c -> !Character.isWhitespace(c));
    }

    public char nextUntil(Predicate<Character> until) {
        char character = next();
        while (character != EOF && !until.test(character)) {
            character = next();
        }
        return character;
    }

    public void assertCurrentCharacterIs(char c) {
        if (current != c) {
            throw new DeserializationException("expected the next non white space character to be '" + c + "' " +
                "but it was '" + current() + "' at index: " + index() + "/" + length());
        }
    }

    public void assertNextCharactersAre(String characters) {
        for (int i = 0; i < characters.length(); i++) {
            next();
            assertCurrentCharacterIs(characters.charAt(i));
        }
    }

    @Override
    public String toString() {
        return "column: " + column + " line: " + line + " character: '" + current + "' @" + index + "/" + length();
    }

}
