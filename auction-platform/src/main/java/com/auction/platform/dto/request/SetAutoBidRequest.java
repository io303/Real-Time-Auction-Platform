package com.auction.platform.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetAutoBidRequest {

    @NotNull(message = "Max bid is required")
    @DecimalMin(value = "0.01", message = "Max bid must be greater than 0")
    private BigDecimal maxBid;
}
