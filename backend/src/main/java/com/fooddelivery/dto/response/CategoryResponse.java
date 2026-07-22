package com.fooddelivery.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response payload representing a Category.
 */
@Data
@Builder
public class CategoryResponse {
    private Long          id;
    private String        name;
    private String        description;
    private String        imageUrl;
    private Boolean       isActive;
    private LocalDateTime createdAt;
}
