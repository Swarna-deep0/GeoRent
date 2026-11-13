package com.onlyrooms.common.exception;

public class PhoneAlreadyRegisteredException
        extends RuntimeException {

    public PhoneAlreadyRegisteredException(String message) {
        super(message);
    }
}