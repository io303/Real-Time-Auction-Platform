package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends ApiException {
    public EmailNotVerifiedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
