package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class InvalidAutoBidException extends ApiException {
    public InvalidAutoBidException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
