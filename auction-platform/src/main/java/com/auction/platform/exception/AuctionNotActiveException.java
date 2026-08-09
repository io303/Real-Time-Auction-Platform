package com.auction.platform.exception;

import org.springframework.http.HttpStatus;

public class AuctionNotActiveException extends ApiException {
    public AuctionNotActiveException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
