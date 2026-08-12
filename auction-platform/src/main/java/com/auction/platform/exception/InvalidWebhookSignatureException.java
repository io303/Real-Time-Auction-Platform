package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class InvalidWebhookSignatureException extends ApiException {
    public InvalidWebhookSignatureException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
