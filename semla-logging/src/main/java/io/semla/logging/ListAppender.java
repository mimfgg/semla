package io.semla.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The original ListAppender doesn't support output patterns
 */
public class ListAppender extends OutputStreamAppender<ILoggingEvent> {

    private final List<String> logLines = Collections.synchronizedList(new ArrayList<>());

    public List<String> logLines() {
        return logLines;
    }

    public void start() {
        started = true;
    }

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        logLines.add(new String(encoder.encode(iLoggingEvent)).trim());
    }
}
