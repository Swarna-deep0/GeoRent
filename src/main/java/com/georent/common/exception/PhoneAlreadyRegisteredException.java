package com.georent.common.exception;

public class PhoneAlreadyRegisteredException
        extends RuntimeException {

    public PhoneAlreadyRegisteredException(String message) {
        super(message);
    }
}