package com.auction.platform.service;

import com.auction.platform.dto.request.AddressRequest;
import com.auction.platform.entity.Address;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AddressType;
import com.auction.platform.mapper.AddressMapper;
import com.auction.platform.repository.AddressRepository;
import com.auction.platform.service.impl.AddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Phase 3 default-address logic — the first address is always default,
 * and setting a new default clears the previous one atomically.
 */
class AddressServiceImplTest {

    @Mock private AddressRepository addressRepository;
    @Mock private AddressMapper addressMapper;

    private AddressServiceImpl addressService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        addressService = new AddressServiceImpl(addressRepository, addressMapper);
        user = User.builder().id(1L).fullName("Test User").build();
    }

    @Test
    void addAddress_firstAddressForUser_isAutomaticallyDefault() {
        AddressRequest request = new AddressRequest();
        request.setAddressLine1("123 Main St");
        request.setCity("Delhi");
        request.setState("Delhi");
        request.setPostalCode("110001");
        request.setCountry("India");
        request.setAddressType(AddressType.HOME);
        request.setDefault(false); // explicitly false — should still become default as the first one

        when(addressRepository.countByUser(user)).thenReturn(0L);
        when(addressMapper.toEntity(request)).thenReturn(new Address());
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addressMapper.toResponse(any())).thenReturn(
                com.auction.platform.dto.response.AddressResponse.builder().build());

        addressService.addAddress(user, request);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().isDefault()).isTrue();
    }

    @Test
    void addAddress_secondAddress_isNotDefaultUnlessRequested() {
        AddressRequest request = new AddressRequest();
        request.setAddressLine1("456 Second St");
        request.setCity("Mumbai");
        request.setState("Maharashtra");
        request.setPostalCode("400001");
        request.setCountry("India");
        request.setAddressType(AddressType.WORK);
        request.setDefault(false);

        when(addressRepository.countByUser(user)).thenReturn(1L); // already has one address
        when(addressMapper.toEntity(request)).thenReturn(new Address());
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addressMapper.toResponse(any())).thenReturn(
                com.auction.platform.dto.response.AddressResponse.builder().build());

        addressService.addAddress(user, request);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().isDefault()).isFalse();
        verify(addressRepository, never()).clearDefaultForUser(user);
    }

    @Test
    void setDefaultAddress_clearsExistingDefaultBeforeSettingNew() {
        Address existingAddress = Address.builder().id(5L).user(user).isDefault(false).build();

        when(addressRepository.findByIdAndUser(5L, user)).thenReturn(java.util.Optional.of(existingAddress));
        when(addressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(addressMapper.toResponse(any())).thenReturn(
                com.auction.platform.dto.response.AddressResponse.builder().build());

        addressService.setDefaultAddress(user, 5L);

        // Order matters: clear-existing-default must happen BEFORE marking the new one default,
        // within the same transaction — this test documents that expectation.
        var inOrder = inOrder(addressRepository);
        inOrder.verify(addressRepository).clearDefaultForUser(user);
        inOrder.verify(addressRepository).save(existingAddress);

        assertThat(existingAddress.isDefault()).isTrue();
    }
}
