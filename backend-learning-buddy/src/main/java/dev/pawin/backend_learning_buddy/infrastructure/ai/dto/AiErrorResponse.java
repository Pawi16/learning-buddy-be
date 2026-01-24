package dev.pawin.backend_learning_buddy.infrastructure.ai.dto;

import java.util.List;

public record AiErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {}