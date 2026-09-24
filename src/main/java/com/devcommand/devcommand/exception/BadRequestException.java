package com.devcommand.devcommand.exception;

/** Thrown for well-formed but semantically invalid requests (e.g. duplicate email). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
