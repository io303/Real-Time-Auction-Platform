package com.auction.platform.controller;

import com.auction.platform.dto.request.CategoryRequest;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.CategoryResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Tag(name = "Admin - Categories", description = "Admin-only category management")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Create a category (admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Category created", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category (admin only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Category updated", categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category (admin only)")
    public ResponseEntity<ApiResponse<MessageResponse>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", MessageResponse.of("Category removed")));
    }
}
