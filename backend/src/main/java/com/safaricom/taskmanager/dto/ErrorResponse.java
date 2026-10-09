package com.safaricom.taskmanager.dto;

import java.time.Instant;

/** Consistent JSON body returned for every error. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
