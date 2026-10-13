package com.boutique.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.boutique.category.Category;
import com.boutique.category.CategoryService;
import com.boutique.category.dto.CategoryRequest;
import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.product.dto.ProductRequest;

class ProductServiceErrorTests {

    private ProductRepository repository;
    private ProductMapper mapper;
    private CategoryService categoryService;
    private ProductService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        mapper = mock(ProductMapper.class);
        categoryService = mock(CategoryService.class);
        service = new ProductService(repository, mapper, categoryService);
    }

    @Test
    void findByIdForMissingProductThrowsNotFoundWithoutMapping() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(404L));

        verify(repository).findById(404L);
        verifyNoInteractions(mapper, categoryService);
    }

    @Test
    void createWithMissingCategoryStopsBeforeSavingOrMapping() {
        Long categoryId = 404L;
        ProductRequest request = new ProductRequest("Desk", null, BigDecimal.TEN, 3, categoryId);
        ResourceNotFoundException failure = new ResourceNotFoundException("Catégorie", categoryId);
        when(categoryService.getEntity(categoryId)).thenThrow(failure);

        assertSame(failure, assertThrows(ResourceNotFoundException.class, () -> service.create(request)));

        verifyNoInteractions(repository, mapper);
    }

    @Test
    void updateForMissingProductStopsBeforeLoadingCategoryOrMapping() {
        Long productId = 404L;
        ProductRequest request = new ProductRequest("Desk", null, BigDecimal.TEN, 3, 1L);
        when(repository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(productId, request));

        verifyNoInteractions(categoryService, mapper);
    }

    @Test
    void reserveStockInsufficientLeavesProductStockUnchanged() {
        Product product = productWithStock(2);
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> service.reserveStock(1L, 3));

        assertEquals(2, product.getStock());
        verify(repository).findById(1L);
    }

    @Test
    void releaseStockRejectsNonPositiveQuantityWithoutChangingProduct() {
        Product product = productWithStock(2);
        when(repository.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BusinessException.class, () -> service.releaseStock(1L, 0));

        assertEquals(2, product.getStock());
        verify(repository).findById(1L);
    }

    @Test
    void deletePropagatesIntegrityViolationAfterAttemptingFlush() {
        Product product = productWithStock(2);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("product is referenced");
        when(repository.findById(1L)).thenReturn(Optional.of(product));
        org.mockito.Mockito.doThrow(failure).when(repository).flush();

        assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> service.delete(1L)));

        verify(repository).delete(product);
        verify(repository).flush();
    }

    private Product productWithStock(int stock) {
        return new Product("Desk", null, BigDecimal.TEN, stock, new Category("Furniture", null));
    }
}