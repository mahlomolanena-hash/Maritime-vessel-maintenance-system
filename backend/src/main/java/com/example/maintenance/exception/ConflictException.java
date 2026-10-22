package com.example.maintenance.exception;

/** The request is valid but conflicts with the current state (e.g. deleting something still in use). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
