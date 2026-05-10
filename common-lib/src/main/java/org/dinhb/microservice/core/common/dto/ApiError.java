package org.dinhb.microservice.core.common.dto;

import java.time.Instant;

public record ApiError(
        int status,
        String code,
        String message,
        String path,
        Instant timestamp
) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(status, code, message, path, Instant.now());
    }
}
