package com.se.dhubbw.shared.infrastructure.logger;

import com.se.dhubbw.shared.testing.UnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@UnitTest
public class ColorLoggerTest {

    private final ColorLogger colorLogger = new ColorLogger(ColorLoggerTest.class);

    @Test
    public void givenMessage_whenColorDebug_thenNoExceptionThrown() {
        assertDoesNotThrow(() -> colorLogger.colorDebug("debug message"));
    }

    @Test
    public void givenMessageWithArgs_whenColorDebug_thenNoExceptionThrown() {
        assertDoesNotThrow(() ->
            colorLogger.colorDebug("debug {} with {}", "message", "args"));
    }

    @Test
    public void givenMessage_whenColorError_thenNoExceptionThrown() {
        assertDoesNotThrow(() -> colorLogger.colorError("error message"));
    }

    @Test
    public void givenMessageWithArgs_whenColorError_thenNoExceptionThrown() {
        assertDoesNotThrow(() ->
            colorLogger.colorError("error {} code {}", "test", 42));
    }

    @Test
    public void givenMessage_whenColorInfo_thenNoExceptionThrown() {
        assertDoesNotThrow(() -> colorLogger.colorInfo("info message"));
    }

    @Test
    public void givenMessageWithArgs_whenColorInfo_thenNoExceptionThrown() {
        assertDoesNotThrow(() ->
            colorLogger.colorInfo("synced {} items across {} days", 5, 3));
    }
}
