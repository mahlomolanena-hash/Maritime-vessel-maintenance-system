package com.example.maintenance.exception;

public class InvalidWorkOrderStateException extends RuntimeException {
    public InvalidWorkOrderStateException(String message) {
        super(message);
    }
}
