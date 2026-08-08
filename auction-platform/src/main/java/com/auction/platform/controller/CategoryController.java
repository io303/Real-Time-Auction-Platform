package com.auction.platform.controller;

import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.CategoryResponse;
import com.auction.platform.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Public category browsing")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Categories fetched", categoryService.listAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by id")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Category fetched", categoryService.getById(id)));
    }
}
