package io.semla.serialization.json;

import io.semla.exception.DeserializationException;
import io.semla.serialization.Deserializer;
import io.semla.serialization.Token;
import io.semla.serialization.io.CharacterReader;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

import static io.semla.serialization.io.CharacterReader.EOF;

@Slf4j
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

        if (log.isTraceEnabled()) {
            log.trace("read: {}", content.toString());
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
            if (log.isTraceEnabled()) {
                log.trace("evaluating: " + c);
            }
            return switch (c) {
                case '"' -> {
                    if (last() == Token.OBJECT) {
                        yield Token.PROPERTY;
                    }
                    yield Token.STRING;
                }
                case 't', 'f' -> Token.BOOLEAN;
                case 'n' -> {
                    reader().assertNextCharactersAre("ull");
                    yield Token.NULL;
                }
                case '{' -> Token.OBJECT;
                case '[' -> Token.ARRAY;
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.' -> Token.NUMBER;
                case ':', ',', '\n', '\r' -> evaluateNextToken();
                case EOF -> Token.END;
                case '}' -> Token.OBJECT_END;
                case ']' -> Token.ARRAY_END;
                default -> throw new DeserializationException("unexpected character '" + c + "' at " + reader().index() + "/" + reader().length());
            };
        }
    }
}
