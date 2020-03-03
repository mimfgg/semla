package io.semla.serialization.json;

import io.semla.exception.DeserializationException;
import io.semla.serialization.Deserializer;
import io.semla.serialization.Token;
import io.semla.serialization.io.CharacterReader;

import java.util.Set;

import static io.semla.serialization.io.CharacterReader.EOF;

public class JsonDeserializer extends Deserializer<JsonDeserializer.Context> {

    @Override
    protected Context newContext(CharacterReader reader, Set<Option> options) {
        return new Context(reader, options);
    }

    @Override
    protected String read(Context context) {
        StringBuilder content = new StringBuilder();
        boolean escaped = false, quoted = false, read = true;
        char current = context.reader().current();

        do {
            switch (current) {
                case EOF:
                    read = false;
                    break;
                case '\\':
                    escaped ^= true;
                    if (!escaped) {
                        content.append(current);
                    }
                    break;
                case ',':
                case ':':
                case '}':
                case ']':
                case '\r':
                case '\n':
                case ' ':
                    if (quoted) {
                        content.append(current);
                    } else {
                        if (current == '}') {
                            context.enqueue(Token.OBJECT_END);
                        } else if (current == ']') {
                            context.enqueue(Token.ARRAY_END);
                        }
                        read = false;
                    }
                    break;
                case '"':
                    if (!escaped) {
                        quoted ^= true;
                    }
                default:
                    escaped = false;
                    content.append(current);
                    break;
            }
            if (read) {
                current = context.reader().next();
            }
        } while (read);

        if (content.charAt(0) == '"' && content.charAt(content.length() - 1) == '"') {
            content.deleteCharAt(0).deleteCharAt(content.length() - 1);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("read: {}", content.toString());
        }
        return content.toString();
    }

    public class Context extends Deserializer<Context>.Context {

        public Context(CharacterReader reader, Set<Option> options) {
            super(reader, options);
        }

        @Override
        public Token evaluateNextToken() {
            char c = reader().nextNonWhiteSpaceCharacter();
            if (logger.isTraceEnabled()) {
                logger.trace("evaluating: " + c);
            }
            switch (c) {
                case '"':
                    if (last() == Token.OBJECT) {
                        return Token.PROPERTY;
                    }
                    return Token.STRING;
                case 't':
                case 'f':
                    return Token.BOOLEAN;
                case 'n':
                    reader().assertNextCharactersAre("ull");
                    return Token.NULL;
                case '{':
                    return Token.OBJECT;
                case '[':
                    return Token.ARRAY;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                case '.':
                    return Token.NUMBER;
                case ':':
                case ',':
                case '\n':
                case '\r':
                    return evaluateNextToken();
                case EOF:
                    return Token.END;
                case '}':
                    return Token.OBJECT_END;
                case ']':
                    return Token.ARRAY_END;
                default:
                    throw new DeserializationException("unexpected character '" + c + "' at " + reader().index() + "/" + reader().length());
            }
        }
    }
}
