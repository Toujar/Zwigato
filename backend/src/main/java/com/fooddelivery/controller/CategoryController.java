package com.fooddelivery.controller;

import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.CategoryResponse;
import com.fooddelivery.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Manages food categories.
 * Base path: /api/categories
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Manage food categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all active categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PostMapping
    @Operation(summary = "Create a category (ADMIN)")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category (ADMIN)")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(null, "Endpoint ready — implementation pending"));
    }
}
