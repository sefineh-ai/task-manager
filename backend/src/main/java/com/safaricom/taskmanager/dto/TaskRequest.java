package com.safaricom.taskmanager.dto;

import com.safaricom.taskmanager.dto.validation.MaxTextLength;
import com.safaricom.taskmanager.model.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/tasks and PUT /api/tasks/{id}.
 * The title is trimmed before validation. The description is rich text (HTML from the editor):
 * its visible text is limited to 500 characters, and the service sanitizes it before saving.
 * A null status means "not provided": defaults to TODO on create, keeps the current status on update.
 */
public record TaskRequest(
        // Trimmed below, so a blank title becomes "" and fails the size check with a single message.
        @NotNull(message = "title is required")
        @Size(min = 3, max = 100, message = "title must be between 3 and 100 characters")
        String title,

        @MaxTextLength(value = 500, message = "description must be at most 500 characters")
        @Size(max = 5000, message = "description formatting is too long")
        String description,

        TaskStatus status
) {

    public TaskRequest {
        title = title == null ? null : title.trim();
    }
}
