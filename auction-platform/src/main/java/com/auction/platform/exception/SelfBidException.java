package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class SelfBidException extends ApiException {
    public SelfBidException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
