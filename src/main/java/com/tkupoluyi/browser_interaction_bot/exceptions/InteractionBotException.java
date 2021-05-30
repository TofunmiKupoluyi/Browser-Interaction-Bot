package com.tkupoluyi.browser_interaction_bot.exceptions;

public class InteractionBotException extends Exception {
    int exceptionType;
    String exceptionMessage;

    public static final int UNREACHEABLE_BROWSER = 1;
    public static final int ELEMENT_NOT_FOUND = 2;
    public static final int UNSPECIFIED_EXCEPTION = 3;
    public static final int INTERACTION_EXCEPTION = 4;
    public static final int INTERACTION_NOT_SUPPORTED = 5;

    public InteractionBotException(int exceptionType) {
        this.exceptionType = exceptionType;
    }

    public InteractionBotException(int exceptionType, String exceptionMessage) {
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
    }

    public int getExceptionType() {
        return exceptionType;
    }
}
