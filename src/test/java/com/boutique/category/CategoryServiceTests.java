package com.boutique.category;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.boutique.category.dto.CategoryRequest;
import com.boutique.category.dto.CategoryResponse;
import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;

class CategoryServiceTests {

    private CategoryRepository repository;
    private CategoryMapper mapper;
    private CategoryService service;

    @BeforeEach
    void setUp() {
        repository = mock(CategoryRepository.class);
        mapper = mock(CategoryMapper.class);
        service = new CategoryService(repository, mapper);
    }

    @Test
    void createsCategoryWhenNameIsAvailable() {
        CategoryRequest request = new CategoryRequest("Books", "Reading");
        Category entity = mock(Category.class);
        Category saved = mock(Category.class);
        CategoryResponse response = new CategoryResponse(1L, "Books", "Reading");
        when(repository.existsByNameIgnoreCase("Books")).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(saved);
        when(mapper.toResponse(saved)).thenReturn(response);
        assertEquals(response, service.create(request));
    }

    @Test
    void rejectsDuplicateCategoryOnCreate() {
        CategoryRequest request = new CategoryRequest("Books", null);
        when(repository.existsByNameIgnoreCase("Books")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(request));
        verifyNoInteractions(mapper);
    }

    @Test
    void updatesCategoryWhenItsNameIsNotUsedByAnotherCategory() {
        Long id = 7L;
        Category category = mock(Category.class);
        CategoryRequest request = new CategoryRequest("Books", "Updated");
        CategoryResponse response = new CategoryResponse(id, "Books", "Updated");
        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndIdNot("Books", id)).thenReturn(false);
        when(mapper.toResponse(category)).thenReturn(response);
        assertEquals(response, service.update(id, request));
        verify(mapper).update(category, request);
    }

    @Test
    void rejectsCategoryNameUsedByAnotherCategoryOnUpdate() {
        Long id = 7L;
        Category category = mock(Category.class);
        CategoryRequest request = new CategoryRequest("Books", "Updated");
        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(repository.existsByNameIgnoreCaseAndIdNot("Books", id)).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.update(id, request));
        verifyNoInteractions(mapper);
    }

    @Test
    void updateForMissingCategoryStopsBeforeCheckingNameOrMapping() {
        Long id = 404L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.update(id, new CategoryRequest("Books", "Reading")));

        verify(repository, never()).existsByNameIgnoreCaseAndIdNot("Books", id);
        verifyNoInteractions(mapper);
    }

    @Test
    void deleteForMissingCategoryDoesNotDeleteOrFlush() {
        Long id = 404L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.delete(id));

        verify(repository, never()).delete(org.mockito.ArgumentMatchers.any(Category.class));
        verify(repository, never()).flush();
    }

    @Test
    void propagatesIntegrityFailureWhenDeletingCategory() {
        Long id = 7L;
        Category category = mock(Category.class);
        DataIntegrityViolationException failure = new DataIntegrityViolationException("category is referenced");
        when(repository.findById(id)).thenReturn(Optional.of(category));
        org.mockito.Mockito.doThrow(failure).when(repository).flush();

        assertEquals(failure, assertThrows(DataIntegrityViolationException.class, () -> service.delete(id)));

        verify(repository).delete(category);
        verify(repository).flush();
    }
}