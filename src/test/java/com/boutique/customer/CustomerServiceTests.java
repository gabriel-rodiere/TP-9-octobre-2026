package com.boutique.customer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.customer.dto.CustomerRequest;
import com.boutique.security.CurrentUserService;

class CustomerServiceTests {

    private CustomerRepository repository;
    private CustomerMapper mapper;
    private PasswordEncoder passwordEncoder;
    private CurrentUserService currentUser;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        repository = mock(CustomerRepository.class);
        mapper = mock(CustomerMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        currentUser = mock(CurrentUserService.class);
        service = new CustomerService(repository, mapper, passwordEncoder, currentUser);
    }

    @Test
    void registersWithEncodedPasswordAndRequestedRole() {
        CustomerRequest request = new CustomerRequest("Ava", "Martin", "Ava@Example.com", null, null);
        Customer customer = new Customer("Ava", "Martin", "ava@example.com", null, null,
                "encoded-password", Role.CUSTOMER);
        when(repository.findByEmail("ava@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(mapper.toEntity(request, "encoded-password", Role.CUSTOMER)).thenReturn(customer);
        when(repository.save(customer)).thenReturn(customer);
        assertSame(customer, service.register(request, "raw-password", Role.CUSTOMER));
        verify(passwordEncoder).encode("raw-password");
        verify(mapper).toEntity(request, "encoded-password", Role.CUSTOMER);
        verify(repository).save(customer);
    }

    @Test
    void rejectsEmailDuplicateIgnoringCaseAndWhitespace() {
        CustomerRequest request = new CustomerRequest("Ava", "Martin", " Ava@Example.com ", null, null);
        Customer existing = mock(Customer.class);
        when(existing.getId()).thenReturn(4L);
        when(repository.findByEmail("ava@example.com")).thenReturn(Optional.of(existing));
        assertThrows(BusinessException.class, () -> service.register(request, "raw-password", Role.CUSTOMER));
        verify(passwordEncoder, never()).encode("raw-password");
        verifyNoInteractions(mapper);
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getEntityForMissingCustomerThrowsNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEntity(404L));

        verify(repository).findById(404L);
        verifyNoInteractions(mapper, passwordEncoder);
    }

    @Test
    void deniedProfileAccessDoesNotQueryRepository() {
        org.mockito.Mockito.doThrow(new AccessDeniedException("forbidden"))
                .when(currentUser).checkAccessTo(8L);

        assertThrows(AccessDeniedException.class, () -> service.findById(8L));

        verifyNoInteractions(repository, mapper);
    }

    @Test
    void updateWithAnotherCustomersEmailDoesNotInvokeMapper() {
        Long customerId = 8L;
        Customer customer = mock(Customer.class);
        Customer otherCustomer = mock(Customer.class);
        when(customer.getId()).thenReturn(customerId);
        when(otherCustomer.getId()).thenReturn(9L);
        when(repository.findById(customerId)).thenReturn(Optional.of(customer));
        when(repository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherCustomer));
        CustomerRequest request = new CustomerRequest("Ava", "Martin", "Taken@Example.com", null, null);

        assertThrows(BusinessException.class, () -> service.update(customerId, request));

        verify(currentUser).checkAccessTo(customerId);
        verify(mapper, never()).update(customer, request);
        verifyNoInteractions(passwordEncoder);
    }
}