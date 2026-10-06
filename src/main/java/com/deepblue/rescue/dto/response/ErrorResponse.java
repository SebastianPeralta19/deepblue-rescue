package com.deepblue.rescue.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/** Contrato único de error para toda la API. */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
}
