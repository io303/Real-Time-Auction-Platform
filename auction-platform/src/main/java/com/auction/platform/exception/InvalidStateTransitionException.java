package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class InvalidStateTransitionException extends ApiException {
    public InvalidStateTransitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
