package com.boutique.category;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.category.dto.CategoryRequest;
import com.boutique.category.dto.CategoryResponse;
import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryService(CategoryRepository repository, CategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessException("Une catégorie nommée '" + request.name() + "' existe déjà");
        }
        return mapper.toResponse(repository.save(mapper.toEntity(request)));
    }

    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new BusinessException("Une categorie nommee '" + request.name() + "' existe deja");
        }
        mapper.update(category, request);
        return mapper.toResponse(category);
    }

    public void delete(Long id) {
        repository.delete(getEntity(id));
        repository.flush();
    }

    /** Utilisé par les autres services (ex. ProductService) pour obtenir l'entité. */
    @Transactional(readOnly = true)
    public Category getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
    }
}
