package io.semla.serialization.yaml;

import io.semla.exception.DeserializationException;
import io.semla.serialization.Deserializer;
import io.semla.serialization.Token;
import io.semla.serialization.io.CharacterReader;
import io.semla.util.Arrays;
import io.semla.util.Pair;
import io.semla.util.Strings;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

import static io.semla.serialization.io.CharacterReader.EOF;

public class YamlDeserializer extends Deserializer<YamlDeserializer.Context> {

    private static final Pattern NUMBER = Pattern.compile("[0-9]+(?:\\.[0-9]+)?");
    private static final Pattern BOOLEAN = Pattern.compile("(?i)y|yes|true|on|n|no|false|off");
    private static final Pattern TRUE = Pattern.compile("(?i)y|yes|true|on");
    private static final Pattern DOCUMENT_END = Pattern.compile("\\.\\.\\.");

    public YamlDeserializer() {
        read(Boolean.class).as(Token.BOOLEAN, value -> TRUE.matcher(value).matches());
    }

    @Override
    protected Context newContext(CharacterReader reader, Set<Option> options) {
        return new Context(reader, options);
    }

    @Override
    protected String read(Context context) {
        if (context.buffer().charAt(0) == '"' && context.buffer().charAt(context.buffer().length() - 1) == '"') {
            context.buffer().deleteCharAt(0).deleteCharAt(context.buffer().length() - 1);
        }

        String value = context.buffer().toString();
        if (logger.isTraceEnabled()) {
            logger.trace("returning buffer: '{}'", value);
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
                if (logger.isDebugEnabled()) {
                    logger.debug("storing value of anchor '" + context.saveAsAnchor.key() + "': " + Strings.toString(value));
                }
                context.anchors.put(context.saveAsAnchor.key(), Pair.of(context.lastPop, value));
                context.saveAsAnchor = null;
            }
        }
        return value;
    }

    public class Context extends Deserializer<Context>.Context {

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
                if (logger.isTraceEnabled()) {
                    logger.trace("evaluating '" + (c == '\n' ? "\\n" : c) + "' isValue: " + isValue + " buffered: '" + buffer + "'");
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
                        if (logger.isTraceEnabled()) {
                            logger.trace("sequence as key!");
                        }
                        previousWasCompositeKey = true;
                        if (structure().isEmpty() || last() != Token.OBJECT) {
                            return Token.OBJECT;
                        }
                        return evaluateNextToken();
                    case '!':
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
                                    if (logger.isTraceEnabled()) {
                                        logger.trace("including file " + file.getAbsolutePath());
                                    }
                                    try {
                                        byte[] content = Files.readAllBytes(file.toPath());
                                        currentAnchorValue = read(new String(content));
                                        explicitToken = Token.fromType(currentAnchorValue.getClass());
                                    } catch (IOException e) {
                                        throw new DeserializationException("while including " + file + " @" + reader(), e);
                                    }
                                } else {
                                    try {
                                        Class<?> clazz = Class.forName(tag);
                                        explicitToken = Token.fromType(clazz);
                                    } catch (ClassNotFoundException e) {
                                        throw new DeserializationException("unknown class: " + tag + " @" + reader(), e);
                                    }
                                }
                            }
                            if (logger.isTraceEnabled()) {
                                logger.trace("explicit type is: " + explicitToken);
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
                        if (style == FlowStyle.INDENTATION && buffer.length() > 1 && last() == Token.PROPERTY) {
                            bufferFlowBlock();
                            return evaluateBuffer();
                        } else {
                            buffer.append(c);
                        }
                        break;
                    case ':':
                        if (previousWasCompositeKey) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("end of sequence as key...");
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
                        } else {
                            buffer.append(c);
                        }
                        break;
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
                            if (logger.isTraceEnabled()) {
                                logger.trace("will save next as anchor " + this.saveAsAnchor);
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

            if (logger.isTraceEnabled()) {
                logger.trace("reached EOF");
            }

            if (buffer.length() > 0) {
                return evaluateBuffer();
            }

            for (int i = structure().size() - 1; i >= 0; i--) {
                switch (structure().get(i)) {
                    case OBJECT:
                        enqueue(Token.OBJECT_END);
                        break;
                    case ARRAY:
                        enqueue(Token.ARRAY_END);
                        break;
                }
            }

            enqueue(Token.END);

            return next();
        }

        private void setStyle(FlowStyle style) {
            if (logger.isTraceEnabled()) {
                logger.trace("setting style to " + style);
            }
            this.style = style;
        }

        private void bufferFlowBlock() {
            boolean read = true, escaped = false, backslashed = false;
            int initialColumn = columns.isEmpty() ? 0 : columns.getLast();
            if (quoting == Quoting.PLAIN) {
                initialColumn += Math.max(indentation, 1);
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
                            for (int i = 1; i < delta; i++) {
                                buffer.append(System.lineSeparator());
                            }
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
                            if (buffer.length() == 0) {
                                quoting = Quoting.SINGLE_QUOTED;
                            } else {
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
                            }
                            break;
                        case '"':
                            if (buffer.length() == 0) {
                                quoting = Quoting.DOUBLE_QUOTED;
                            } else {
                                switch (quoting) {
                                    case DOUBLE_QUOTED:
                                        if (!backslashed) {
                                            read = false;
                                            break;
                                        }
                                        buffer.append(backslashed(s));
                                        backslashed = false;
                                        break;
                                    case SINGLE_QUOTED:
                                    case PLAIN:
                                        buffer.append(backslashed ? backslashed(s) : s);
                                        backslashed = false;
                                        break;
                                }
                            }
                            break;
                        case '#':
                            consumeComment();
                            read = false;
                            break;
                        default:
                            buffer.append(backslashed ? backslashed(s) : s);
                            backslashed = false;
                            break;
                    }
                    reader().next();
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("block buffer: '" + buffer + "'");
                }
            } while (read && reader().current() != EOF && reader().column() >= initialColumn);
        }

        private char backslashed(char c) {
            switch (c) {
                case 't':
                    return '\t';
                case 'b':
                    return '\b';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 'f':
                    return '\f';
                case '"':
                    return '\"';
                default:
                    return c;
            }
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
                                for (int i = 0; i <= delta; i++) {
                                    buffer.append(System.lineSeparator());
                                }
                                break;
                        }
                    } else {
                        switch (blockStyle) {
                            case FOLDED:
                                if (delta > 1) {
                                    for (int i = 1; i < delta; i++) {
                                        buffer.append(System.lineSeparator());
                                    }
                                } else {
                                    buffer.append(" ");
                                }
                                break;
                            case LITERAL:
                                for (int i = 0; i < delta; i++) {
                                    buffer.append(System.lineSeparator());
                                }
                                break;
                        }
                    }
                } else {
                    buffer.append(s);
                    reader().next();
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("block buffer: '" + buffer + "'");
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
                if (logger.isTraceEnabled()) {
                    logger.trace("resetting buffer: '" + buffer + "'");
                }
                buffer = new StringBuilder();
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
            if (logger.isTraceEnabled()) {
                logger.trace("consuming comment...");
            }
            //noinspection StatementWithEmptyBody
            while (reader().next() != '\n' && reader().current() != EOF) {
                // this is a comment, let's just consume it (っ˘ڡ˘ς)
            }
        }

        @Override
        public Token evaluateNextToken() {
            if (!resume) {
                if (logger.isTraceEnabled()) {
                    logger.trace("next token...");
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
                    if (logger.isTraceEnabled()) {
                        logger.trace("file offset is " + fileOffset);
                    }
                } else if (style == FlowStyle.INDENTATION) {
                    int lastIdent = indent;
                    indent = reader().column() - fileOffset;
                    if (reader().line() > previousLine) {
                        // new line
                        int delta = indent - lastIdent;
                        if (logger.isTraceEnabled()) {
                            logger.trace("new line with ident: {} delta: {} ", indent, delta);
                        }
                        if (delta > 0) {
                            isNewObject = true;
                            if (indentation == -1) {
                                indentation = delta;
                                if (logger.isTraceEnabled()) {
                                    logger.trace("indentation is " + indentation);
                                }
                            }
                        } else if (delta < 0) {
                            List<Token> pops = gatherStructureToPop(indent);
                            if (!pops.isEmpty()) {
                                pops.forEach(this::enqueue);
                                resume = true;
                                return queued.removeFirst();
                            } else if (logger.isTraceEnabled()) {
                                logger.trace("nothing to pop ...");
                            }
                        }
                    }
                }
            } else if (logger.isTraceEnabled()) {
                logger.trace("resuming...");
            }

            resume = false;

            Token token = evaluateCurrent(isNewObject);

            List<Token> pops = new LinkedList<>();
            if (token == Token.END) {
                pops.addAll(gatherStructureToPop(-1));
            }

            if (logger.isTraceEnabled()) {
                if (!pops.isEmpty()) {
                    logger.trace("to pop: {}", pops);
                }
                logger.trace("current: '{}' -> {} (buffered: '{}')", reader().current() == '\n' ? "\\n" : reader().current(), token, buffer);
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
                    case OBJECT:
                        pops.add(Token.OBJECT_END);
                        break;
                    case ARRAY:
                        pops.add(Token.ARRAY_END);
                        break;
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
        switch (tag) {
            case "!str":
                return Token.STRING;
            case "!int":
            case "!float":
                return Token.NUMBER;
            case "!bool":
                return Token.BOOLEAN;
            case "!map":
                return Token.OBJECT;
            case "!seq":
                return Token.ARRAY;
            default:
                return null;
        }
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
            switch (indicator) {
                case '\n':
                    return CLIP;
                case '-':
                    return STRIP;
                case '+':
                    return KEEP;
            }
            throw new DeserializationException("unsupported BlockChomping: " + indicator);
        }
    }
}
