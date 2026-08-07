package com.auction.platform.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private String message;

    public static MessageResponse of(String message) {
        return MessageResponse.builder().message(message).build();
    }
}
