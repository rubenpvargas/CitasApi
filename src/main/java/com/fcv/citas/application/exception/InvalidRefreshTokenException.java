package com.fcv.citas.application.exception;

public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Invalid refresh token");
    }
}
