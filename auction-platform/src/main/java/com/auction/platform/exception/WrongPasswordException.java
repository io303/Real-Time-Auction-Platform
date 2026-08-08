package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class WrongPasswordException extends ApiException {
    public WrongPasswordException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
