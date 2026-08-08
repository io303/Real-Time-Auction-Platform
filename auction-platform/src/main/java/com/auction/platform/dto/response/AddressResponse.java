package com.auction.platform.dto.response;

import com.auction.platform.entity.enums.AddressType;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private AddressType addressType;
    private boolean isDefault;
}
