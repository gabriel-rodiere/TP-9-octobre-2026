package com.boutique.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.boutique.category.Category;
import com.boutique.common.exception.BusinessException;
import com.boutique.customer.Customer;
import com.boutique.customer.Role;
import com.boutique.product.Product;

class OrderDomainTests {

    @Test
    void exposesOnlyTheConfiguredStatusTransitions() {
        Map<OrderStatus, Set<OrderStatus>> expected = new EnumMap<>(OrderStatus.class);
        expected.put(OrderStatus.PENDING, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        expected.put(OrderStatus.PAID, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        expected.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        expected.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        expected.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        for (OrderStatus current : OrderStatus.values()) {
            assertEquals(expected.get(current), current.allowedNext());
            for (OrderStatus next : OrderStatus.values()) {
                assertEquals(expected.get(current).contains(next), current.canTransitionTo(next));
            }
        }
    }

    @Test
    void changesStatusForAllowedTransitionAndRejectsInvalidTransition() {
        Order order = newOrder();
        order.changeStatus(OrderStatus.PAID);
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertThrows(BusinessException.class, () -> order.changeStatus(OrderStatus.DELIVERED));
        assertEquals(OrderStatus.PAID, order.getStatus());
    }

    @Test
    void calculatesOrderTotalAndPreservesItemPriceSnapshot() {
        Order order = newOrder();
        Product product = new Product("Book", null, new BigDecimal("12.50"), 4,
                new Category("Books", null));
        OrderItem item = new OrderItem(product, 2);
        order.addItem(item);
        product.setPrice(new BigDecimal("15.00"));
        assertEquals(new BigDecimal("25.00"), item.getSubtotal());
        assertEquals(new BigDecimal("25.00"), order.getTotal());
        assertEquals(new BigDecimal("12.50"), item.getUnitPrice());
    }

    private Order newOrder() {
        Customer customer = new Customer("Ava", "Martin", "ava@example.com", null, null,
                "encoded-password", Role.CUSTOMER);
        return new Order(customer);
    }
}