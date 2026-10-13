package com.boutique.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.boutique.category.Category;
import com.boutique.common.exception.BusinessException;

class ProductStockRulesTests {

    @Test
    void decreasesStockWhenQuantityIsAvailable() {
        Product product = productWithStock(5);
        product.decreaseStock(2);
        assertEquals(3, product.getStock());
    }

    @Test
    void allowsDecreasingStockToExactlyZero() {
        Product product = productWithStock(5);
        product.decreaseStock(5);
        assertEquals(0, product.getStock());
    }

    @Test
    void rejectsDecreasingStockBelowZeroWithoutChangingStock() {
        Product product = productWithStock(5);
        assertThrows(BusinessException.class, () -> product.decreaseStock(6));
        assertEquals(5, product.getStock());
    }

    @Test
    void rejectsNonPositiveDecrementWithoutChangingStock() {
        for (int quantity : new int[] {0, -1}) {
            Product product = productWithStock(5);
            assertThrows(BusinessException.class, () -> product.decreaseStock(quantity));
            assertEquals(5, product.getStock());
        }
    }

    @Test
    void increasesStockForPositiveQuantity() {
        Product product = productWithStock(5);
        product.increaseStock(3);
        assertEquals(8, product.getStock());
    }

    @Test
    void rejectsNonPositiveIncrementWithoutChangingStock() {
        for (int quantity : new int[] {0, -1}) {
            Product product = productWithStock(5);
            assertThrows(BusinessException.class, () -> product.increaseStock(quantity));
            assertEquals(5, product.getStock());
        }
    }

    private Product productWithStock(int stock) {
        return new Product("Keyboard", null, BigDecimal.TEN, stock, new Category("Computers", null));
    }
}