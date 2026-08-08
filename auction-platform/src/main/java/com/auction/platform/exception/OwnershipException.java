package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class OwnershipException extends ApiException {
    public OwnershipException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
