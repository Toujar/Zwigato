package com.fooddelivery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload representing a Category.
 *
 * @NoArgsConstructor + @AllArgsConstructor required for Jackson
 * deserialization from Redis cache.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long          id;
    private String        name;
    private String        description;
    private String        imageUrl;
    private Boolean       isActive;
    private LocalDateTime createdAt;
}
