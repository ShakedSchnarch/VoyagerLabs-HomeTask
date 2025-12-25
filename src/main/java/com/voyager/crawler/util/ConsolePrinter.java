package com.voyager.crawler.util;

/**
 * Lightweight console output helper for consistent CLI messaging.
 * Adds severity prefixes and truncates long warning/error messages for readability.
 */
public final class ConsolePrinter {
    private static final String INFO_PREFIX = "[INFO] ";
    private static final String WARN_PREFIX = "[WARN] ";
    private static final String ERROR_PREFIX = "[ERROR] ";
    private static final int KEY_WIDTH = 18;
    private static final int MAX_MESSAGE_LENGTH = 200;

    private ConsolePrinter() {
    }

    /**
     * Prints an info-level message to standard output.
     *
     * @param message the message to print.
     */
    public static void info(String message) {
        System.out.println(INFO_PREFIX + safe(message));
    }

    /**
     * Prints a warning-level message to standard error, truncating if needed.
     *
     * @param message the message to print.
     */
    public static void warn(String message) {
        System.err.println(WARN_PREFIX + truncate(safe(message)));
    }

    /**
     * Prints an error-level message to standard error, truncating if needed.
     *
     * @param message the message to print.
     */
    public static void error(String message) {
        System.err.println(ERROR_PREFIX + truncate(safe(message)));
    }

    /**
     * Prints a formatted info-level key/value line.
     *
     * @param key   the label to display.
     * @param value the value to display.
     */
    public static void infoKeyValue(String key, Object value) {
        System.out.printf(INFO_PREFIX + "%-" + KEY_WIDTH + "s %s%n", safe(key), safeValue(value));
    }

    /**
     * Prints a blank line to standard output.
     */
    public static void blankLine() {
        System.out.println();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeValue(Object value) {
        return value == null ? "null" : value.toString();
    }

    private static String truncate(String value) {
        if (value.length() <= MAX_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
    }
}
