package com.auction.platform.mapper;

import com.auction.platform.dto.request.AddressRequest;
import com.auction.platform.dto.response.AddressResponse;
import com.auction.platform.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressResponse toResponse(Address address);

    Address toEntity(AddressRequest request);

    /** Updates an existing entity in place from a request — used for PUT /addresses/{id}. */
    void updateEntityFromRequest(AddressRequest request, @MappingTarget Address address);
}
