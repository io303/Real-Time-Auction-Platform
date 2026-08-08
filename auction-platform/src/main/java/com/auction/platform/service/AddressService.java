package com.auction.platform.service;

import com.auction.platform.dto.request.AddressRequest;
import com.auction.platform.dto.response.AddressResponse;
import com.auction.platform.entity.User;

import java.util.List;

public interface AddressService {
    List<AddressResponse> listAddresses(User user);
    AddressResponse addAddress(User user, AddressRequest request);
    AddressResponse updateAddress(User user, Long addressId, AddressRequest request);
    void deleteAddress(User user, Long addressId);
    AddressResponse setDefaultAddress(User user, Long addressId);
}
