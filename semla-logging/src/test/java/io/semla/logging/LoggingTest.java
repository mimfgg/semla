package io.semla.logging;

import ch.qos.logback.classic.Level;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class LoggingTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void staticHelpers() {
        Logging.setup();
        assertThat(Logging.root().getLevel(), is(Level.INFO));
        Logging.setTo(Level.ERROR);
        assertThat(Logging.logger("ROOT").getLevel(), is(Level.ERROR));
        Logging.withLogLevel(Level.ALL).setup();
        Logging.withAppenderLevel("io.semla", Level.ALL).setup();
        assertThat(Logging.logger(Logging.class).getEffectiveLevel(), is(Level.ALL));
        Logging.withAppenderLevel("io.semla", Level.ALL).setup();
        assertThat(Logging.logger(Logging.class).getEffectiveLevel(), is(Level.ALL));
        Logging.withPattern("%-5p [%t]: %m%n").setup();
        ListAppender listAppender = new ListAppender();
        Logging.withAppender(listAppender).noConsole().withPattern("%-5p [%t]: %m%n").setup();
        logger.info("test");
        assertThat(listAppender.logLines().get(0), is("INFO  [main]: test"));
    }

    @Test
    public void withFileAppender() throws Exception {
        Logging.configure()
            .noConsole()
            .withFileAppender()
            .withPattern("%-5p [%t]: %m%n")
            .withLogFilename("./target/test.log")
            .setup();
        logger.info("test");
        TimeUnit.MILLISECONDS.sleep(15);
        String line = Files.lines(new File("./target/test.log").toPath()).findFirst().orElseThrow(AssertionError::new);
        assertThat(line, is("INFO  [main]: test"));
    }

    @Test
    public void withRollingFileAppender() throws Exception {
        Logging.configure()
            .noConsole()
            .withFileAppender()
            .withPattern("%-5p [%t]: %m%n")
            .withLogFilename("./target/test.log")
            .withLogFilenamePattern("./target/test-%d.log.gz")
            .keep(30)
            .setup();
        logger.info("test");
        TimeUnit.MILLISECONDS.sleep(15);
        String line = Files.lines(new File("./target/test.log").toPath()).findFirst().orElseThrow(AssertionError::new);
        assertThat(line, is("INFO  [main]: test"));
    }
}