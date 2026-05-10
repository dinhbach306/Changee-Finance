package org.dinhb.microservice.core.auth.web.dto;

import java.util.List;
import java.util.UUID;

public record TokenResponse(
        String accessToken,
        UUID userId,
        String username,
        List<String> roles
) {}
