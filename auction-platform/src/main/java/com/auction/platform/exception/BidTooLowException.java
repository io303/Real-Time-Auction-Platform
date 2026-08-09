package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class BidTooLowException extends ApiException {
    public BidTooLowException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
