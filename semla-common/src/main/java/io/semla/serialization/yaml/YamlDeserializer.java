package io.semla.serialization.yaml;

import io.semla.exception.DeserializationException;
import io.semla.reflect.Types;
import io.semla.serialization.Deserializer;
import io.semla.serialization.Token;
import io.semla.serialization.io.CharacterReader;
import io.semla.util.Arrays;
import io.semla.util.Pair;
import io.semla.util.Strings;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.semla.serialization.io.CharacterReader.EOF;

@Slf4j
public class YamlDeserializer extends Deserializer<YamlDeserializer.Context> {

    private static final Pattern NUMBER = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");
    private static final Pattern BOOLEAN = Pattern.compile("(?i)y|yes|true|on|n|no|false|off");
    private static final Pattern TRUE = Pattern.compile("(?i)y|yes|true|on");
    private static final Pattern DOCUMENT_END = Pattern.compile("\\.\\.\\.");
    private static final Pattern NAMED_TYPE = Pattern.compile("<(.*)>");

    public YamlDeserializer() {
        read(Boolean.class).as(Token.BOOLEAN, value -> TRUE.matcher(value).matches());
    }

    @Override
    protected Context newContext(CharacterReader reader, Set<Option> options) {
        return new Context(reader, options);
    }

    @Override
    protected String read(Context context) {
        if (!context.buffer().isEmpty()) {
            if (context.buffer().charAt(0) == '"' && context.buffer().charAt(context.buffer().length() - 1) == '"') {
                context.buffer().deleteCharAt(0).deleteCharAt(context.buffer().length() - 1);
            }
        }

        String value = context.buffer().toString();
        if (log.isTraceEnabled()) {
            log.trace("returning buffer: '{}'", value);
        }
        context.resetBuffer();
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <E> E read(Context context, Type type) {
        E value;
        if (context.currentAnchorValue != null) {
            value = (E) context.currentAnchorValue;
            context.currentAnchorValue = null;
        } else {
            value = super.read(context, type);
            if (context.saveAsAnchor != null && context.columns.getLast() <= context.saveAsAnchor.value()) {
                if (log.isDebugEnabled()) {
                    log.debug("storing value of anchor '" + context.saveAsAnchor.key() + "': " + Strings.toString(value));
                }
                context.anchors.put(context.saveAsAnchor.key(), Pair.of(context.lastPop, value));
                context.saveAsAnchor = null;
            }
        }
        return value;
    }

    public class Context extends Deserializer<Context>.Context {

        private final LinkedList<String> stashedBuffers = new LinkedList<>();
        private final LinkedList<Integer> columns = new LinkedList<>();
        private final Map<String, Pair<Token, Object>> anchors = new LinkedHashMap<>();
        private Token lastPop;

        private StringBuilder buffer = new StringBuilder();

        private Pair<String, Integer> saveAsAnchor;
        private Object currentAnchorValue;

        private boolean previousWasName = false;
        private boolean previousWasCompositeKey = false;
        private FlowStyle style = FlowStyle.INDENTATION;
        private int indent;
        private int indentation = -1;
        private int fileOffset = 0;

        private boolean resume;
        private boolean isNewObject;
        private Quoting quoting;

        public Context(CharacterReader reader, Set<Option> options) {
            super(reader, options);
        }

        public StringBuilder buffer() {
            return buffer;
        }

        private Token evaluateCurrent(boolean isNewObject) {
            boolean escaped = false, isValue = previousWasName && !isNewObject && style == FlowStyle.INDENTATION;
            quoting = Quoting.PLAIN;
            previousWasName = false;
            do {
                char c = reader().current();
                if (log.isTraceEnabled()) {
                    log.trace("evaluating '" + (c == '\n' ? "\\n" : c) + "' isValue: " + isValue + " buffered: '" + buffer + "'");
                }
                String currentBuffer = buffer.toString();
                switch (c) {
                    case '\\':
                        switch (quoting) {
                            case DOUBLE_QUOTED:
                                if (!escaped) {
                                    escaped = true;
                                    break;
                                } else {
                                    escaped = false;
                                }
                            case PLAIN:
                            case SINGLE_QUOTED:
                                buffer.append(c);
                        }
                        break;
                    case '\'':
                        if (buffer.length() == 0) {
                            quoting = Quoting.SINGLE_QUOTED;
                        } else {
                            if (quoting == Quoting.SINGLE_QUOTED) {
                                return Token.STRING;
                            }
                            buffer.append(c);
                        }
                        break;
                    case '"':
                        if (buffer.length() == 0) {
                            quoting = Quoting.DOUBLE_QUOTED;
                        } else {
                            switch (quoting) {
                                case DOUBLE_QUOTED:
                                    if (!escaped) {
                                        return Token.STRING;
                                    }
                                case SINGLE_QUOTED:
                                case PLAIN:
                                    buffer.append(c);
                                    break;
                            }
                        }
                        break;
                    case '{':
                        if (quoting == Quoting.PLAIN) {
                            if (buffer.length() == 0) {
                                setStyle(FlowStyle.EXPLICIT);
                                return Token.OBJECT;
                            } else {
                                throw new DeserializationException("unexpected character '" + c + "' while buffer already contains " + buffer + " @" + reader());
                            }
                        }
                        buffer.append(c);
                        break;
                    case '}':
                        if (quoting == Quoting.PLAIN) {
                            if (!Arrays.in(last(), Token.PROPERTY, Token.OBJECT) || style != FlowStyle.EXPLICIT) {
                                throw new DeserializationException("unexpected character '" + c + "' while no object has been created @" + reader());
                            }
                            setStyle(FlowStyle.INDENTATION);
                            return evaluateBufferOrReturn(Token.OBJECT_END);
                        }
                        buffer.append(c);
                        break;
                    case '[':
                        if (quoting == Quoting.PLAIN) {
                            if (buffer.length() == 0) {
                                setStyle(FlowStyle.EXPLICIT);
                                return Token.ARRAY;
                            } else {
                                throw new DeserializationException("unexpected character '" + c + "' while buffer already contains " + buffer + " @" + reader());
                            }
                        }
                        buffer.append(c);
                        break;
                    case ']':
                        if (quoting == Quoting.PLAIN) {
                            if (last() != Token.ARRAY) {
                                throw new DeserializationException("unexpected character '" + c + "' while no array has been created @" + reader());
                            }
                            setStyle(FlowStyle.INDENTATION);
                            return evaluateBufferOrReturn(Token.ARRAY_END);
                        }
                        buffer.append(c);
                        break;
                    case '?':
                        if (buffer.length() == 0) {
                            if (log.isTraceEnabled()) {
                                log.trace("sequence as key!");
                            }
                            previousWasCompositeKey = true;
                            if (structure().isEmpty() || last() != Token.OBJECT) {
                                return Token.OBJECT;
                            }
                            return evaluateNextToken();
                        }
                        buffer.append(c);
                        break;
                    case '!':
                        if (currentBuffer.startsWith("--- ")) {
                            resetBuffer();
                        }
                        if (buffer.length() == 0) {
                            char next = reader().next();
                            while (!Character.isWhitespace(next) && next != EOF) {
                                buffer.append(next);
                                next = reader().next();
                            }
                            String tag = buffer.toString();
                            Token explicitToken = explicitTokenOf(tag);
                            resetBuffer();
                            evaluateNextToken();
                            if (explicitToken == null) {
                                if (tag.equals("include")) {
                                    File file = new File(buffer.toString());
                                    if (log.isTraceEnabled()) {
                                        log.trace("including file " + file.getAbsolutePath());
                                    }
                                    try {
                                        byte[] content = Files.readAllBytes(file.toPath());
                                        currentAnchorValue = YamlDeserializer.this.read(new String(content));
                                        explicitToken = Token.fromType(currentAnchorValue.getClass());
                                    } catch (IOException e) {
                                        throw new DeserializationException("while including " + file + " @" + reader(), e);
                                    }
                                } else {
                                    Matcher namedType = NAMED_TYPE.matcher(tag);
                                    if (namedType.matches()) {
                                        buffer.append("!type");
                                        stashedBuffers.add(namedType.group(1));
                                        enqueue(Token.PROPERTY);
                                        enqueue(Token.STRING);
                                        return Token.OBJECT;
                                    } else {
                                        try {
                                            Class<?> clazz = Class.forName(tag);
                                            explicitToken = Token.fromType(clazz);
                                        } catch (ClassNotFoundException e) {
                                            throw new DeserializationException("unknown class: " + tag + " @" + reader(), e);
                                        }
                                    }
                                }
                            }
                            if (log.isTraceEnabled()) {
                                log.trace("explicit type is: " + explicitToken);
                            }
                            return explicitToken;
                        } else {
                            buffer.append(c);
                            break;
                        }
                    case ' ':
                        if (buffer.length() == 1 && buffer.charAt(0) == '-') {
                            resetBuffer();
                            if (structure().isEmpty() || (indent < reader().column() && last() != Token.ARRAY)) {
                                return Token.ARRAY;
                            }
                            return Token.SKIP;
                        }
                        buffer.append(c);
                        break;
                    case ':':
                        if (previousWasCompositeKey) {
                            if (log.isTraceEnabled()) {
                                log.trace("end of sequence as key...");
                            }
                            previousWasCompositeKey = false;
                            return Token.SKIP;
                        }

                        if (quoting == Quoting.PLAIN && !isValue) {
                            if (buffer.length() > 0) {
                                previousWasName = true;
                                if (last() != Token.OBJECT || (style == FlowStyle.INDENTATION && isNewObject)) {
                                    trimBuffer();
                                    enqueue(Token.PROPERTY);
                                    return Token.OBJECT;
                                }
                                return Token.PROPERTY;
                            } else {
                                throw new DeserializationException("unexpected character '" + c + "' while buffer already contains " + buffer + " @" + reader());
                            }
                        }
                        buffer.append(c);
                        break;
                    case '\n':
                        if (style == FlowStyle.EXPLICIT) {
                            return evaluateBufferOrReturn(Token.SKIP);
                        }
                        if (currentBuffer.equals("---") || DOCUMENT_END.matcher(currentBuffer).matches()) {
                            return Token.SKIP;
                        } else if (currentBuffer.equals("-")) {
                            if (structure().isEmpty() || (indent < reader().column() - 1 && last() != Token.ARRAY)) {
                                return Token.ARRAY;
                            }
                            return Token.SKIP;
                        }
                        bufferFlowBlock();
                        return evaluateBuffer();
                    case ',':
                        if (buffer.length() == 0 && last() == Token.ARRAY) {
                            return Token.SKIP;
                        }
                        if (quoting == Quoting.PLAIN && style == FlowStyle.EXPLICIT) {
                            // terminate the line
                            return evaluateBuffer();
                        }
                        buffer.append(c);
                        break;
                    case '#':
                        if (quoting == Quoting.PLAIN && buffer.length() == 0) {
                            consumeComment();
                            return Token.SKIP;
                        }
                        bufferFlowBlock();
                        return evaluateBuffer();
                    case EOF:
                        break;
                    case '&':
                        if (quoting == Quoting.PLAIN && buffer.length() == 0) {
                            // anchor
                            StringBuilder anchor = new StringBuilder();
                            while (!Character.isWhitespace(reader().next()) && reader().current() != EOF) {
                                anchor.append(reader().current());
                            }
                            this.saveAsAnchor = Pair.of(anchor.toString(), columns.getLast());
                            if (log.isTraceEnabled()) {
                                log.trace("will save next as anchor " + this.saveAsAnchor);
                            }
                            return evaluateNextToken();
                        }
                        buffer.append(c);
                        break;
                    case '*':
                        if (quoting == Quoting.PLAIN && buffer.length() == 0) {
                            // anchor reference
                            StringBuilder anchor = new StringBuilder();
                            while (!Character.isWhitespace(reader().next()) && reader().current() != EOF) {
                                anchor.append(reader().current());
                            }
                            Pair<Token, Object> anchorValue = anchors.get(anchor.toString());
                            if (anchorValue == null) {
                                throw new DeserializationException("didn't have stored value for anchor '" + anchor + "' @" + reader());
                            }
                            this.currentAnchorValue = anchorValue.value();
                            return anchorValue.key();
                        }
                        buffer.append(c);
                        break;
                    case '|':
                    case '>':
                        if (quoting == Quoting.PLAIN) {
                            if (currentBuffer.startsWith("--- ")) {
                                resetBuffer();
                            }
                            char s = reader().next();
                            // block
                            reader().nextNonWhiteSpaceCharacter();
                            bufferBlock(BlockStyle.from(c), BlockChomping.from(s));
                            return Token.STRING;
                        }
                        buffer.append(c);
                        break;
                    default:
                        escaped = false;
                        if (c != '-' && style == FlowStyle.INDENTATION && quoting == Quoting.PLAIN && last() == Token.PROPERTY && columns.getLast() == indent) {
                            throw new DeserializationException("Plain flow scalars cannot be placed at the same indentation level than their property @" + reader());
                        }
                        buffer.append(c);
                        break;
                }
            } while (reader().next() != EOF);

            if (log.isTraceEnabled()) {
                log.trace("reached EOF");
            }

            if (buffer.length() > 0) {
                return evaluateBuffer();
            }

            for (int i = structure().size() - 1; i >= 0; i--) {
                switch (structure().get(i)) {
                    case OBJECT -> enqueue(Token.OBJECT_END);
                    case ARRAY -> enqueue(Token.ARRAY_END);
                }
            }

            enqueue(Token.END);

            return next();
        }

        private void setStyle(FlowStyle style) {
            if (log.isTraceEnabled()) {
                log.trace("setting style to " + style);
            }
            this.style = style;
        }

        private void bufferFlowBlock() {
            boolean read = true, escaped = false, backslashed = false;
            int initialColumn = columns.isEmpty() ? 0 : columns.getLast();
            if (quoting == Quoting.PLAIN) {
                initialColumn += Math.max(indentation, 1);
            }
            if (structure().getLast().equals(Token.ARRAY)) {
                initialColumn += 2;
            }
            do {
                char s = reader().current();
                if (s == '\n') {
                    int previousLine = reader().line();
                    reader().nextNonWhiteSpaceCharacter();
                    int currentLine = reader().line();
                    int delta = currentLine - previousLine;
                    if (reader().column() < initialColumn) {
                        reader().stashCurrent();
                    } else {
                        if (delta > 1) {
                            buffer.append(String.valueOf(System.lineSeparator()).repeat(delta - 1));
                        } else {
                            if (!backslashed) {
                                buffer.append(" ");
                            }
                            backslashed = false;
                        }
                    }
                } else {
                    switch (s) {
                        case '\\':
                            switch (quoting) {
                                case DOUBLE_QUOTED:
                                    if (!backslashed) {
                                        backslashed = true;
                                        break;
                                    } else {
                                        backslashed = false;
                                    }
                                case PLAIN:
                                case SINGLE_QUOTED:
                                    buffer.append(backslashed ? backslashed(s) : s);
                                    backslashed = false;
                            }
                            break;
                        case '\'':
                            switch (quoting) {
                                case SINGLE_QUOTED:
                                    if (!escaped) {
                                        escaped = true;
                                        break;
                                    } else {
                                        escaped = false;
                                    }
                                case DOUBLE_QUOTED:
                                case PLAIN:
                                    buffer.append(backslashed ? backslashed(s) : s);
                                    backslashed = false;
                                    break;
                            }
                            break;
                        case '"':
                            switch (quoting) {
                                case DOUBLE_QUOTED -> {
                                    if (!backslashed) {
                                        read = false;
                                        break;
                                    }
                                    buffer.append(backslashed(s));
                                    backslashed = false;
                                }
                                case SINGLE_QUOTED, PLAIN -> {
                                    buffer.append(backslashed ? backslashed(s) : s);
                                    backslashed = false;
                                }
                            }
                            break;
                        case '#':
                            if (buffer.charAt(buffer.length() - 1) == ' ') {
                                consumeComment();
                                break;
                            }
                        default:
                            buffer.append(backslashed ? backslashed(s) : s);
                            backslashed = false;
                            break;
                    }
                    reader().next();
                }
                if (log.isTraceEnabled()) {
                    log.trace("block buffer: '" + buffer + "'");
                }
            } while (read && reader().current() != EOF && reader().column() >= initialColumn);
            if (reader().current() != EOF) {
                reader().stashCurrent();
            }
        }

        private char backslashed(char c) {
            return switch (c) {
                case 't' -> '\t';
                case 'b' -> '\b';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 'f' -> '\f';
                case '"' -> '\"';
                default -> c;
            };
        }

        private void bufferBlock(BlockStyle blockStyle, BlockChomping chomping) {
            int initialColumn = reader().column();
            do {
                char s = reader().current();
                if (s == '\n') {
                    int previousLine = reader().line();
                    reader().nextNonWhiteSpaceCharacter();
                    int currentLine = reader().line();
                    int delta = currentLine - previousLine;
                    if (reader().column() < initialColumn) {
                        reader().stashCurrent();
                        switch (chomping) {
                            case CLIP:
                                buffer.append(System.lineSeparator());
                                break;
                            case STRIP:
                                break;
                            case KEEP:
                                buffer.append(String.valueOf(System.lineSeparator()).repeat(Math.max(0, delta + 1)));
                                break;
                        }
                    } else {
                        switch (blockStyle) {
                            case FOLDED:
                                if (delta > 1) {
                                    buffer.append(String.valueOf(System.lineSeparator()).repeat(delta - 1));
                                } else {
                                    buffer.append(" ");
                                }
                                break;
                            case LITERAL:
                                buffer.append(String.valueOf(System.lineSeparator()).repeat(Math.max(0, delta)));
                                break;
                        }
                    }
                } else {
                    buffer.append(s);
                    reader().next();
                }
                if (log.isTraceEnabled()) {
                    log.trace("block buffer: '" + buffer + "'");
                }
            } while (reader().current() != EOF && reader().column() >= initialColumn);
        }

        private Token evaluateBufferOrReturn(Token token) {
            if (buffer.length() == 0) {
                return token;
            } else {
                enqueue(token);
                return evaluateBuffer();
            }
        }

        private void resetBuffer() {
            if (buffer.length() > 0) {
                if (log.isTraceEnabled()) {
                    log.trace("resetting buffer: '" + buffer + "'");
                }
                buffer = new StringBuilder();
            }
            if (!stashedBuffers.isEmpty()) {
                buffer.append(stashedBuffers.removeFirst());
            }
        }

        private Token evaluateBuffer() {
            trimBuffer();
            String value = buffer.toString();
            if (NUMBER.matcher(value).matches()) {
                return Token.NUMBER;
            } else if (BOOLEAN.matcher(value).matches()) {
                return Token.BOOLEAN;
            } else if (DOCUMENT_END.matcher(value).matches()) {
                return Token.END;
            } else if (value.equals("null") || value.equals("~")) {
                if (quoting == Quoting.DOUBLE_QUOTED) {
                    return Token.STRING;
                }
                return Token.NULL;
            } else {
                return Token.STRING;
            }
        }

        private void trimBuffer() {
            // if there is some whitespaces at the end rewind until the last character
            while (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == ' ') {
                buffer.deleteCharAt(buffer.length() - 1);
            }
        }

        private void consumeComment() {
            if (log.isTraceEnabled()) {
                log.trace("consuming comment...");
            }
            //noinspection StatementWithEmptyBody
            while (reader().next() != '\n' && reader().current() != EOF) {
                // this is a comment, let's just consume it (っ˘ڡ˘ς)
            }
        }

        @Override
        public Token evaluateNextToken() {
            if (!resume) {
                if (log.isTraceEnabled()) {
                    log.trace("next token...");
                }
                resume = false;
                isNewObject = false;
                resetBuffer();
                int previousLine = reader().line();
                reader().nextNonWhiteSpaceCharacter();

                if (structure().isEmpty()) {
                    // new file
                    isNewObject = true;
                    fileOffset = reader().column();
                    if (log.isTraceEnabled()) {
                        log.trace("file offset is " + fileOffset);
                    }
                } else if (style == FlowStyle.INDENTATION) {
                    int lastIdent = indent;
                    indent = reader().column() - fileOffset;
                    if (reader().line() > previousLine) {
                        // new line
                        int delta = indent - lastIdent;
                        if (log.isTraceEnabled()) {
                            log.trace("new line with ident: {} delta: {} ", indent, delta);
                        }
                        if (delta > 0) {
                            isNewObject = true;
                            if (indentation == -1) {
                                indentation = delta;
                                if (log.isTraceEnabled()) {
                                    log.trace("indentation is " + indentation);
                                }
                            }
                        } else if (delta < 0) {
                            List<Token> pops = gatherStructureToPop(indent);
                            if (!pops.isEmpty()) {
                                pops.forEach(this::enqueue);
                                resume = true;
                                return queued.removeFirst();
                            } else if (log.isTraceEnabled()) {
                                log.trace("nothing to pop ...");
                            }
                        }
                    }
                }
            } else if (log.isTraceEnabled()) {
                log.trace("resuming...");
            }

            resume = false;

            Token token = evaluateCurrent(isNewObject);

            List<Token> pops = new LinkedList<>();
            if (token == Token.END) {
                pops.addAll(gatherStructureToPop(-1));
            }

            if (log.isTraceEnabled()) {
                if (!pops.isEmpty()) {
                    log.trace("to pop: {}", pops);
                }
                log.trace("current: '{}' -> {} (buffered: '{}')", reader().current() == '\n' ? "\\n" : reader().current(), token, buffer);
            }

            if (pops.isEmpty()) {
                return token;
            }
            doNext(token);
            pops.forEach(this::doNext);
            return queued.removeFirst();
        }

        public LinkedList<Token> gatherStructureToPop(int column) {
            LinkedList<Token> pops = new LinkedList<>();
            for (int i = structure().size() - 1; i >= 0 && columns.get(i) > column; i--) {
                switch (structure().get(i)) {
                    case OBJECT -> pops.add(Token.OBJECT_END);
                    case ARRAY -> pops.add(Token.ARRAY_END);
                }
            }
            return pops;
        }

        @Override
        protected void push(Token token) {
            super.push(token);
            columns.add(indent);
        }

        @Override
        protected void pop(Token token) {
            super.pop(token);
            lastPop = token;
            columns.removeLast();
        }
    }

    private Token explicitTokenOf(String tag) {
        return switch (tag) {
            case "!str" -> Token.STRING;
            case "!int", "!float" -> Token.NUMBER;
            case "!bool" -> Token.BOOLEAN;
            case "!map" -> Token.OBJECT;
            case "!seq" -> Token.ARRAY;
            default -> null;
        };
    }

    private enum FlowStyle {
        INDENTATION, EXPLICIT
    }

    private enum Quoting {
        PLAIN, SINGLE_QUOTED, DOUBLE_QUOTED
    }

    private enum BlockStyle {

        FOLDED, LITERAL;

        static BlockStyle from(char indicator) {
            switch (indicator) {
                case '>':
                    return FOLDED;
                case '|':
                    return LITERAL;
            }
            throw new DeserializationException("unsupported BlockStyle: " + indicator);
        }
    }

    private enum BlockChomping {

        CLIP, STRIP, KEEP;

        static BlockChomping from(char indicator) {
            return switch (indicator) {
                case '\n' -> CLIP;
                case '-' -> STRIP;
                case '+' -> KEEP;
                default -> {
                    throw new DeserializationException("unsupported BlockChomping: " + indicator);
                }
            };
        }
    }
}
