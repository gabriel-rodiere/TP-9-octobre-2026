package com.boutique.order;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.boutique.category.Category;
import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.customer.Customer;
import com.boutique.customer.CustomerService;
import com.boutique.customer.Role;
import com.boutique.order.dto.OrderItemRequest;
import com.boutique.order.dto.OrderRequest;
import com.boutique.product.Product;
import com.boutique.product.ProductService;
import com.boutique.security.CurrentUserService;

class OrderServiceErrorTests {

    private OrderRepository repository;
    private OrderMapper mapper;
    private CustomerService customerService;
    private ProductService productService;
    private CurrentUserService currentUser;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        mapper = mock(OrderMapper.class);
        customerService = mock(CustomerService.class);
        productService = mock(ProductService.class);
        currentUser = mock(CurrentUserService.class);
        service = new OrderService(repository, mapper, customerService, productService, currentUser);
    }

    @Test
    void createForAnotherCustomerStopsBeforeLoadingCustomerOrReservingStock() {
        Long customerId = 9L;
        OrderRequest request = new OrderRequest(customerId, List.of(new OrderItemRequest(3L, 1)));
        AccessDeniedException failure = new AccessDeniedException("forbidden");
        org.mockito.Mockito.doThrow(failure).when(currentUser).checkAccessTo(customerId);

        assertSame(failure, assertThrows(AccessDeniedException.class, () -> service.create(request)));

        verify(currentUser).checkAccessTo(customerId);
        verifyNoInteractions(customerService, productService, repository, mapper);
    }

    @Test
    void productReservationFailurePreventsOrderSaveAndResponseMapping() {
        Long customerId = 8L;
        Customer customer = new Customer("Ava", "Martin", "ava@example.com", null, null,
                "encoded-password", Role.CUSTOMER);
        Product firstProduct = product(3L);
        OrderRequest request = new OrderRequest(customerId, List.of(
                new OrderItemRequest(3L, 1), new OrderItemRequest(4L, 1)));
        BusinessException failure = new BusinessException("insufficient stock");
        when(customerService.getEntity(customerId)).thenReturn(customer);
        when(productService.reserveStock(3L, 1)).thenReturn(firstProduct);
        when(productService.reserveStock(4L, 1)).thenThrow(failure);

        assertSame(failure, assertThrows(BusinessException.class, () -> service.create(request)));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any(Order.class));
        verifyNoInteractions(mapper);
    }

    @Test
    void findByIdForMissingOrderThrowsNotFoundWithoutMapping() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(404L));

        verify(repository).findById(404L);
        verifyNoInteractions(mapper, currentUser);
    }

    @Test
    void cancelDeniedForNonOwnerDoesNotReleaseStockOrMapResponse() {
        Long orderId = 2L;
        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn(5L);
        Order order = new Order(customer);
        AccessDeniedException failure = new AccessDeniedException("forbidden");
        when(repository.findById(orderId)).thenReturn(Optional.of(order));
        org.mockito.Mockito.doThrow(failure).when(currentUser).checkAccessTo(5L);

        assertSame(failure, assertThrows(AccessDeniedException.class, () -> service.cancel(orderId)));

        verify(currentUser).checkAccessTo(5L);
        verifyNoInteractions(productService, mapper);
    }

    @Test
    void invalidStatusTransitionDoesNotReleaseStockOrMapResponse() {
        Long orderId = 2L;
        Order order = new Order(new Customer("Ava", "Martin", "ava@example.com", null, null,
                "encoded-password", Role.CUSTOMER));
        when(repository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(BusinessException.class, () -> service.changeStatus(orderId, OrderStatus.DELIVERED));

        verifyNoInteractions(productService, mapper);
    }

    private Product product(Long id) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(id);
        when(product.getPrice()).thenReturn(BigDecimal.TEN);
        when(product.getName()).thenReturn("Desk");
        when(product.getCategory()).thenReturn(new Category("Furniture", null));
        return product;
    }
}