package io.semla.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class ProcessesTest {

    @Test
    public void executeAndReturn() {
        assertThat(Processes.execute("echo 10").andReturn()).isEqualTo("10");
    }

    @Test
    public void executeUntil() {
        assertThat(Processes.execute("echo 10").andWaitUntil(output -> output.contains("10"))).isEqualTo(true);
        assertThat(Processes.execute("sleep 0.01 && echo 10").andWaitUntil(output -> output.contains("10"))).isEqualTo(true);
        assertThat(Processes.execute("sleep 0.01 && echo 10").andWaitUntil(output -> output.contains("10"), 1)).isEqualTo(false);
        assertThat(Processes.execute("sdfasfd").andWaitUntil(output -> output.contains("10"), 1)).isEqualTo(false);
    }
}
