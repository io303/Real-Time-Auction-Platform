package com.auction.platform.service.impl;

import com.auction.platform.dto.request.AddressRequest;
import com.auction.platform.dto.response.AddressResponse;
import com.auction.platform.entity.Address;
import com.auction.platform.entity.User;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.mapper.AddressMapper;
import com.auction.platform.repository.AddressRepository;
import com.auction.platform.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;

    @Override
    public List<AddressResponse> listAddresses(User user) {
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user).stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse addAddress(User user, AddressRequest request) {
        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        boolean isFirstAddress = addressRepository.countByUser(user) == 0;
        boolean shouldBeDefault = isFirstAddress || request.isDefault();

        if (shouldBeDefault) {
            addressRepository.clearDefaultForUser(user);
            address.setDefault(true);
        } else {
            address.setDefault(false);
        }

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(User user, Long addressId, AddressRequest request) {
        Address address = findOwnedAddressOrThrow(user, addressId);

        addressMapper.updateEntityFromRequest(request, address);

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultForUser(user);
            address.setDefault(true);
        }

        Address saved = addressRepository.save(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(User user, Long addressId) {
        Address address = findOwnedAddressOrThrow(user, addressId);
        boolean wasDefault = address.isDefault();

        addressRepository.delete(address);

        if (wasDefault) {
            // Promote the oldest remaining address to default, if any exist, so the user always
            // has a sensible default rather than silently ending up with none.
            addressRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(User user, Long addressId) {
        Address address = findOwnedAddressOrThrow(user, addressId);

        addressRepository.clearDefaultForUser(user);
        address.setDefault(true);
        Address saved = addressRepository.save(address);

        return addressMapper.toResponse(saved);
    }

    private Address findOwnedAddressOrThrow(User user, Long addressId) {
        return addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
