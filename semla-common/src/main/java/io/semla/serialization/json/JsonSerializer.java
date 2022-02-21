package io.semla.serialization.json;

import io.semla.serialization.Serializer;
import io.semla.serialization.io.CharacterWriter;

import java.util.Set;

public class JsonSerializer extends Serializer<JsonSerializer.Context> {

    public JsonSerializer() {
        write(String.class).with((context, value) -> {
            CharacterWriter writer = context.writer();
            writer.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '\n':
                        writer.append('\\').append('n');
                        break;
                    case '\t':
                        writer.append('\\').append('t');
                        break;
                    case '\b':
                        writer.append('\\').append('b');
                        break;
                    case '\r':
                        writer.append('\\').append('r');
                        break;
                    case '\f':
                        writer.append('\\').append('f');
                        break;
                    case '\\':
                    case '"':
                        writer.append('\\');
                    default:
                        writer.append(c);
                        break;
                }
            }
            writer.append('"');
        });
    }

    @Override
    protected Context newContext(CharacterWriter writer, Set<Option> options) {
        return new Context(writer, options);
    }

    @Override
    public void next(Context context) {
        context.writer().append(',');
    }

    @Override
    public void startObject(Context context) {
        if (context.isPretty()) {
            context.increaseDepth();
        }
        context.writer().append('{');
    }

    @Override
    public void endObject(Context context) {
        if (context.isPretty()) {
            context.decreaseDepth();
        }
        context.writer().append('}');
    }

    @Override
    public void writeKey(Context context, Object key) {
        if (context.isPretty()) {
            context.writer().newLine();
            context.writeIndentation();
            context.writer().append('\"').append(key).append("\":");
            context.writer().append(" ");
        } else {
            context.writer().append('\"').append(key).append("\":");
        }
    }

    @Override
    public void startArray(Context context) {
        if (context.isPretty()) {
            context.increaseDepth();
        }
        context.writer().append('[');
    }

    @Override
    public void endArray(Context context) {
        if (context.isPretty()) {
            context.decreaseDepth();
        }
        context.writer().append(']');
    }

    @Override
    public void writeArrayComponent(Context context, Object value) {
        if (context.isPretty()) {
            context.writer().newLine();
            context.writeIndentation();
        }
        getWriterFor(value).accept(context);
    }

    public class Context extends Serializer<?>.Context {

        private int depth;
        private boolean pretty;

        public Context(CharacterWriter writer, Set<Option> options) {
            super(writer, options);
            if (options.contains(PRETTY)) {
                this.pretty = true;
            }
        }

        public boolean isPretty() {
            return pretty;
        }

        private void increaseDepth() {
            depth++;
        }

        private void decreaseDepth() {
            depth--;
            writer().newLine();
            writeIndentation();
        }

        private void writeIndentation() {
            for (int i = 0; i < depth * 2; i++) {
                writer().append(' ');
            }
        }
    }

    public static final Option PRETTY = new Option();

}
