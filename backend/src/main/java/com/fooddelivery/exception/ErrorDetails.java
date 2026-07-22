package com.fooddelivery.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error payload returned by {@link GlobalExceptionHandler}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetails {

    @Builder.Default
    private LocalDateTime        timestamp  = LocalDateTime.now();
    private int                  status;
    private String               error;
    private String               message;
    private String               path;

    /** Populated only for validation errors — maps field name → error message. */
    private Map<String, String>  fieldErrors;
}
