package com.safaricom.taskmanager.dto;

import com.safaricom.taskmanager.model.Task;
import com.safaricom.taskmanager.model.TaskStatus;

public record TaskResponse(Long id, String title, String description, TaskStatus status) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus());
    }
}
