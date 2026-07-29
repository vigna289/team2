package com.dbtraining.reconx.exception;

/** 401 Unauthorized: the supplied login credentials do not identify an enabled user. */
public class InvalidCredentialsException extends ReconException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
