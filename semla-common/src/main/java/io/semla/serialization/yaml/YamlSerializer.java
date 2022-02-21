package io.semla.serialization.yaml;

import io.semla.exception.SerializationException;
import io.semla.serialization.Serializer;
import io.semla.serialization.Token;
import io.semla.serialization.io.CharacterWriter;

import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.semla.reflect.Types.isAssignableTo;
import static io.semla.reflect.Types.isAssignableToOneOf;

public class YamlSerializer extends Serializer<YamlSerializer.Context> {

    private int indentation = 2;

    public YamlSerializer() {
        write(String.class).with((context, value) -> {
            if ("null".equals(value)) {
                writeWith(context, "\"null\"", (c, o) -> c.writer().append(o));
            } else if (context.noBreak()) {
                writeWith(context, value, (c, o) -> c.writer().append(o));
            } else {
                int column = context.depth * indentation;
                CharacterWriter writer = context.writer();
                if (column + value.length() > 80) {
                    writer.append(" >");
                    if (value.endsWith("\n\n")) {
                        writer.append('+');
                    } else if (value.endsWith("\n")) {
                        value = value.substring(0, value.length() - 1);
                    } else {
                        writer.append('-');
                    }
                    writer.newLine();
                    context.depth++;
                    int available = 80 - column - indentation;
                    int start = 0, current = 0, lastSpace = 0;
                    do {
                        char c = value.charAt(current);
                        if (c == ' ') {
                            lastSpace = current;
                        }
                        if (lastSpace > start && (c == '\n' || (current - start) >= available)) {
                            context.writeIndentation().append(value.substring(start, lastSpace).trim()).newLine();
                            start = lastSpace + 1;
                        }
                    } while (++current < value.length());
                    if (start != current) {
                        context.writeIndentation().append(value.substring(start));
                    }
                    context.depth--;
                } else if (value.contains("\n")) {
                    if (context.lastIs(Token.PROPERTY)) {
                        writer.append(' ');
                    }
                    writer.append("|").newLine();
                    context.depth++;
                    context.writeIndentation();
                    for (int i = 0; i < value.length(); i++) {
                        char s = value.charAt(i);
                        if (s == '\n') {
                            writer.append(s);
                            context.writeIndentation();
                        } else {
                            writer.append(s);
                        }
                    }
                    context.depth--;
                } else {
                    writeWith(context, value, (c, o) -> c.writer().append(o));
                }
            }
        });
    }

    @Override
    protected Context newContext(CharacterWriter writer, Set<Option> options) {
        return new Context(writer, options);
    }

    @Override
    public void writeKey(YamlSerializer.Context context, Object key) {
        context.writeIndentation();
        if (key.getClass().isArray() || isAssignableToOneOf(key.getClass(), Collection.class, Map.class)) { // add more?
            context.writer().append("? ");
            context.startIndentingAt = 4;
            getWriterFor(key).accept(context);
            context.writer().append('\n');
            context.writeIndentation();
            context.writer().append(": ");
            context.startIndentingAt = 4;
        } else {
            context.structure.add(Token.PROPERTY);
            context.writer().append(key);
            context.writer().append(':');
        }
    }

    @Override
    protected void writeWith(YamlSerializer.Context context, Object value, BiConsumer<YamlSerializer.Context, Object> writer) {
        if (context.lastIs(Token.PROPERTY)) {
            context.writer().append(' ');
        }
        super.writeWith(context, value, writer);
    }


    @Override
    public Consumer<Context> getWriterFor(Object value) {
        return super.getWriterFor(value).andThen(context -> {
            if (context.lastIs(Token.PROPERTY)) {
                context.structure.removeLast();
            }
        });
    }

    @Override
    protected void writeNull(YamlSerializer.Context context) {
        if (context.lastIs(Token.PROPERTY)) {
            context.writer().append(' ');
        }
        super.writeNull(context);
    }

    @Override
    public void next(YamlSerializer.Context context) {
        context.writer().newLine();
    }

    @Override
    public void startObject(YamlSerializer.Context context) {
        if (context.lastIs(Token.PROPERTY)) {
            context.writer().newLine();
        }
        if (!context.structure.isEmpty()) {
            context.depth++;
        }
        context.structure.add(Token.OBJECT);
    }

    @Override
    public void endObject(YamlSerializer.Context context) {
        Token last = context.structure.removeLast();
        if (last != Token.OBJECT) {
            throw new SerializationException("was expecting " + Token.OBJECT + " but was " + last);
        }
        if (!context.structure.isEmpty()) {
            context.depth--;
        }
    }

    @Override
    public void startArray(YamlSerializer.Context context) {
        if (context.lastIs(Token.PROPERTY) || context.lastIs(Token.ARRAY)) {
            context.writer().newLine();
        }
        if (!context.structure.isEmpty()) {
            context.depth++;
        }
        context.structure.add(Token.ARRAY);
    }

    @Override
    public void endArray(YamlSerializer.Context context) {
        Token last = context.structure.removeLast();
        if (last != Token.ARRAY) {
            throw new SerializationException("was expecting " + Token.ARRAY + " but was " + last);
        }
        if (!context.structure.isEmpty()) {
            context.depth--;
        }
    }

    @Override
    protected void writeArray(YamlSerializer.Context context, Collection<?> values) {
        if (values.isEmpty()) {
            context.writer().append(" []");
        } else {
            super.writeArray(context, values);
        }
    }

    @Override
    public void writeArrayComponent(YamlSerializer.Context context, Object value) {
        context.writeIndentation();
        context.writer().append("-");
        if (value == null) {
            context.writer().append(" ");
            writeNull(context);
        } else {
            if (!value.getClass().isArray() && !isAssignableTo(value.getClass(), Collection.class)) {
                context.writer().append(' ');
                if (isAssignableToOneOf(value.getClass(), Number.class, Boolean.class, String.class, Character.class,
                    Date.class, Temporal.class, Calendar.class, Optional.class)) {
                    context.startIndentingAt = (context.depth + 1) * indentation;
                } else {
                    context.startIndentingAt = (context.depth + 2) * indentation;
                }
            }
            getWriterFor(value).accept(context);
        }
        context.startIndentingAt = 0;
    }

    public class Context extends Serializer<?>.Context {

        private final boolean noBreak;
        private final LinkedList<Token> structure = new LinkedList<>();
        private int startIndentingAt = 0;
        private int depth = 0;

        public Context(CharacterWriter writer, Set<Option> options) {
            super(writer, options);
            this.noBreak = options.contains(NO_BREAK);
        }

        public boolean noBreak() {
            return noBreak;
        }

        private boolean lastIs(Token name) {
            return !structure.isEmpty() && structure.getLast().equals(name);
        }

        private CharacterWriter writeIndentation() {
            for (int i = startIndentingAt; i < depth * indentation; i++) {
                writer().append(' ');
            }
            startIndentingAt = 0;
            return writer();
        }
    }

    public final static Option NO_BREAK = new Option();

}
