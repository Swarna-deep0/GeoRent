package com.onlyrooms.common.exception;

public class AdminRegistrationNotAllowedException
        extends RuntimeException {

    public AdminRegistrationNotAllowedException(String message) {
        super(message);
    }
}