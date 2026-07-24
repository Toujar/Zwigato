package com.fooddelivery.controller;

import com.fooddelivery.dto.request.CategoryRequest;
import com.fooddelivery.dto.response.ApiResponse;
import com.fooddelivery.dto.response.CategoryResponse;
import com.fooddelivery.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 *  Controller : CategoryController
 *  Base path  : /api/categories
 * ============================================================
 *
 *  Role matrix:
 *  ┌────────────────────────┬─────────────────────────────────┐
 *  │ Endpoint               │ Allowed roles                   │
 *  ├────────────────────────┼─────────────────────────────────┤
 *  │ GET  /                 │ PUBLIC (no token required)      │
 *  │ GET  /{id}             │ PUBLIC (no token required)      │
 *  ├────────────────────────┼─────────────────────────────────┤
 *  │ POST /                 │ ADMIN only                      │
 *  │ PUT  /{id}             │ ADMIN only                      │
 *  │ DELETE /{id}           │ ADMIN only                      │
 *  └────────────────────────┴─────────────────────────────────┘
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "3. Categories", description = "Food category management")
public class CategoryController {

    private final CategoryService categoryService;

    // ----------------------------------------------------------------
    // GET /api/categories  — PUBLIC
    // ----------------------------------------------------------------
    @GetMapping
    @Operation(
        summary     = "Get all active categories",
        description = "Returns all active food categories sorted alphabetically. "
                    + "Used to render filter chips on the home page. "
                    + "No authentication required."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories returned")
    })
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.getAllActiveCategories(),
                "Categories retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // GET /api/categories/{id}  — PUBLIC
    // ----------------------------------------------------------------
    @GetMapping("/{id}")
    @Operation(
        summary     = "Get a category by ID",
        description = "Returns a single active or inactive category. Returns 404 if not found."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.getCategoryById(id),
                "Category retrieved successfully"));
    }

    // ----------------------------------------------------------------
    // POST /api/categories  — ADMIN only
    // ----------------------------------------------------------------
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Create a new category (ADMIN only)",
        description = "Creates a new food category. Name must be unique (case-insensitive). "
                    + "Returns 400 if name already exists."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Duplicate name / validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        categoryService.createCategory(request),
                        "Category created successfully"));
    }

    // ----------------------------------------------------------------
    // PUT /api/categories/{id}  — ADMIN only
    // ----------------------------------------------------------------
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Update a category (ADMIN only)",
        description = "Updates name, description, and image URL. "
                    + "Returns 400 if the new name conflicts with another category. "
                    + "Returns 404 if not found."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category updated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Duplicate name / validation error"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                categoryService.updateCategory(id, request),
                "Category updated successfully"));
    }

    // ----------------------------------------------------------------
    // DELETE /api/categories/{id}  — ADMIN only
    // Soft-delete: sets is_active = false
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary     = "Deactivate a category (ADMIN only)",
        description = "Sets is_active = false. The category is hidden from the public API "
                    + "but its food items and historical order data are preserved. "
                    + "Returns 400 if already deactivated. Returns 404 if not found."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Category deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Already deactivated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "Category ID", required = true)
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deactivated successfully"));
    }
}
