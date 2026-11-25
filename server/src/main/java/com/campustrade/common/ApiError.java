package com.campustrade.common;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(
        int status,
        String message,
        Map<String, String> fields,
        LocalDateTime timestamp
) {
}
