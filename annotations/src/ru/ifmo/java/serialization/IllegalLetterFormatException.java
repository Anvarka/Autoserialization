package ru.ifmo.java.serialization;

public class IllegalLetterFormatException extends RuntimeException {

    public IllegalLetterFormatException() {
        super();
    }

    public IllegalLetterFormatException(String message) {
        super(message);
    }

    public IllegalLetterFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalLetterFormatException(Throwable cause) {
        super(cause);
    }

    protected IllegalLetterFormatException(String message, Throwable cause, boolean enableSuppression,
                                           boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);

    }
}

