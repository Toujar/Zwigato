package com.fooddelivery.util;

/**
 * Application-wide constants.
 * Use as static imports wherever needed.
 */
public final class AppConstants {

    private AppConstants() {}  // Prevent instantiation

    // ---- Pagination defaults ----
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE   = "10";
    public static final String DEFAULT_SORT_BY     = "id";
    public static final String DEFAULT_SORT_DIR    = "asc";
    public static final int    MAX_PAGE_SIZE       = 100;

    // ---- JWT / Security ----
    public static final String TOKEN_PREFIX    = "Bearer ";
    public static final String HEADER_STRING   = "Authorization";

    // ---- API paths ----
    public static final String API_BASE        = "/api";
    public static final String AUTH_PATH       = "/auth/**";
    public static final String SWAGGER_PATH    = "/swagger-ui/**";
    public static final String API_DOCS_PATH   = "/v3/api-docs/**";
}
