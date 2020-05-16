package io.semla.cucumber.steps;

import ch.qos.logback.classic.Level;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.semla.logging.Logging;

public class LoggerSteps {

    private static boolean shouldReinstall = false;
    private static final Level DEFAULT_LEVEL = Level.ERROR;
    private Level level = DEFAULT_LEVEL;

    @Given("^a root logger set to (ALL|TRACE|DEBUG|INFO|WARN|ERROR|OFF)$")
    public void a_root_logger_set_to_(String level) {
        shouldReinstall = true;
        this.level = Level.valueOf(level);
        reinstall();
    }

    @Before
    public void before() {
        if (shouldReinstall) {
            shouldReinstall = false;
            reinstall();
        }
    }

    private void reinstall() {
        Logging.withLogLevel(this.level).setup();
    }
}