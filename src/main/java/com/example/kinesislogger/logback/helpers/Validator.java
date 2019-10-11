package com.example.kinesislogger.logback.helpers;

public class Validator {

    /**
     * Tests if input is null or empty.
     *
     * @param input test string
     * @return true if input is null or empty; false otherwise
     */
    public static boolean isBlank(String input) {
        return input == null || input.trim().isEmpty();
    }

    /**
     * For validating conditions and throwing error messages if the condition
     * doesn't hold true
     *
     * @param trueCondition boolean condition to be validated
     * @param exceptionMsg  error message to throw as {@code IllegalArgumentException} if
     *                      condition doesn't hold true
     */
    public static void validate(boolean trueCondition, String exceptionMsg) {
        if (!trueCondition) {
            throw new IllegalArgumentException(exceptionMsg);
        }
    }
}
