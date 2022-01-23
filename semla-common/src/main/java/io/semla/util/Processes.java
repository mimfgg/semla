package io.semla.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static io.semla.util.Unchecked.unchecked;
import static java.time.Instant.now;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Processes {

    public static ProcessHandler execute(String command) {
        return unchecked(() -> new ProcessHandler(command));
    }

    public static class ProcessHandler {

        public static final int DEFAULT_TIMEOUT_IN_MS = 1000;
        private final String command;
        private final Process process;

        private ProcessHandler(String command) throws IOException {
            this.command = command;
            ProcessBuilder processBuilder = new ProcessBuilder(parse(command));
            this.process = processBuilder.start();
        }

        public String andReturn() {
            InputStream inputStream = process.getInputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            unchecked(() -> IOUtils.copy(inputStream, byteArrayOutputStream));
            return byteArrayOutputStream.toString().trim();
        }

        public boolean andWaitUntil(Predicate<String> outputMatches) {
            return andWaitUntil(outputMatches, DEFAULT_TIMEOUT_IN_MS);
        }

        public boolean andWaitUntil(Predicate<String> outputMatches, long timeoutInMs) {
            Instant deadline = now().plusMillis(timeoutInMs);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            Thread thread = new Thread(() -> {
                while (process.isAlive()) {
                    String line = unchecked(bufferedReader::readLine);
                    if (line != null && outputMatches.test(line)) {
                        break;
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
            while (thread.isAlive()) {
                unchecked(() -> TimeUnit.MILLISECONDS.sleep(1));
                if (now().isAfter(deadline)) {
                    thread.interrupt();
                    log.error("'" + command + "' didn't output the expect content within " + timeoutInMs + "ms");
                    return false;
                }
            }
            if (!process.isAlive() && process.exitValue() != 0) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                unchecked(() -> IOUtils.copy(process.getErrorStream(), outputStream));
                log.error("'" + command + "' returned error code " + process.exitValue() + " and:\n\t" + outputStream);
                return false;
            }
            return true;
        }
    }

    private static List<String> parse(String command) {
        if (!command.matches("^(\\w*) -c '(.+)'")) {
            command = "bash -c '" + command + "'";
        }
        List<String> commands = new ArrayList<>();
        StringBuilder currentCommand = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            switch (c) {
                case '\'':
                    inQuotes ^= true;
                    break;
                case ' ':
                    if (!inQuotes && currentCommand.length() > 0) {
                        commands.add(currentCommand.toString());
                        currentCommand = new StringBuilder();
                        break;
                    }
                default:
                    currentCommand.append(c);
            }
        }
        if (currentCommand.length() > 0) {
            commands.add(currentCommand.toString());
        }
        return commands;
    }
}
