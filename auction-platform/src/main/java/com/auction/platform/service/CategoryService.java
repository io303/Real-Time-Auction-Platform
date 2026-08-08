package com.auction.platform.service;

import com.auction.platform.dto.request.CategoryRequest;
import com.auction.platform.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> listAll();
    CategoryResponse getById(Long id);
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
}
