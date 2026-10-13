package com.boutique.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.boutique.customer.dto.CustomerRequest;
import com.boutique.customer.dto.CustomerResponse;

class CustomerMapperTests {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    void normalizesEmailAndTrimsNamesWhenMappingToEntity() {
        CustomerRequest request = new CustomerRequest(" Ava ", " Martin ", " Ava@Example.COM ", null, null);
        Customer customer = mapper.toEntity(request, "encoded-password", Role.CUSTOMER);
        assertEquals("Ava", customer.getFirstName());
        assertEquals("Martin", customer.getLastName());
        assertEquals("ava@example.com", customer.getEmail());
        assertEquals("encoded-password", customer.getPassword());
        assertEquals(Role.CUSTOMER, customer.getRole());
    }

    @Test
    void normalizesEmailAndPreservesNull() {
        assertEquals("ava@example.com", CustomerMapper.normalizeEmail(" Ava@Example.COM "));
        assertNull(CustomerMapper.normalizeEmail(null));
    }

    @Test
    void updatesMutableProfileFieldsWithoutChangingPasswordOrRole() {
        Customer customer = new Customer("Ava", "Martin", "ava@example.com", "0601020304", "Old address",
                "encoded-password", Role.CUSTOMER);
        CustomerRequest request = new CustomerRequest(" Grace ", " Hopper ", " GRACE@EXAMPLE.COM ",
                null, "New address");
        mapper.update(customer, request);
        assertEquals("Grace", customer.getFirstName());
        assertEquals("Hopper", customer.getLastName());
        assertEquals("grace@example.com", customer.getEmail());
        assertNull(customer.getPhoneNumber());
        assertEquals("New address", customer.getAddress());
        assertEquals("encoded-password", customer.getPassword());
        assertEquals(Role.CUSTOMER, customer.getRole());
    }

    @Test
    void responseDoesNotExposePassword() {
        Customer customer = new Customer("Ava", "Martin", "ava@example.com", null, null,
                "encoded-password", Role.CUSTOMER);
        CustomerResponse response = mapper.toResponse(customer);
        assertEquals("ava@example.com", response.email());
        assertEquals(Role.CUSTOMER, response.role());
    }
}