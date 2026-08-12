package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotAllowedException extends ApiException {
    public PaymentNotAllowedException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
