package com.auction.platform.service.impl;

import com.auction.platform.dto.request.CategoryRequest;
import com.auction.platform.dto.response.CategoryResponse;
import com.auction.platform.entity.Category;
import com.auction.platform.exception.ApiException;
import com.auction.platform.exception.DuplicateResourceException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.mapper.CategoryMapper;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.CategoryRepository;
import com.auction.platform.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuctionRepository auctionRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category already exists: " + request.getName());
        }

        Category parent = request.getParentCategoryId() != null
                ? findOrThrow(request.getParentCategoryId())
                : null;

        Category category = Category.builder()
                .name(request.getName())
                .slug(toSlug(request.getName()))
                .description(request.getDescription())
                .parentCategory(parent)
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findOrThrow(id);

        category.setName(request.getName());
        category.setSlug(toSlug(request.getName()));
        category.setDescription(request.getDescription());

        if (request.getParentCategoryId() != null) {
            category.setParentCategory(findOrThrow(request.getParentCategoryId()));
        } else {
            category.setParentCategory(null);
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);

        if (auctionRepository.existsByCategory(category)) {
            throw new ApiException("Cannot delete a category that has auctions assigned to it", HttpStatus.CONFLICT);
        }

        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private String toSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = Pattern.compile("[^\\w\\s-]").matcher(normalized).replaceAll("");
        return slug.trim().toLowerCase().replaceAll("[\\s]+", "-");
    }
}
