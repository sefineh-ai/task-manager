package com.safaricom.taskmanager.service;

import com.safaricom.taskmanager.dto.TaskRequest;
import com.safaricom.taskmanager.dto.TaskResponse;
import com.safaricom.taskmanager.exception.TaskNotFoundException;
import com.safaricom.taskmanager.model.Task;
import com.safaricom.taskmanager.model.TaskStatus;
import com.safaricom.taskmanager.repository.TaskRepository;
import com.safaricom.taskmanager.util.HtmlSanitizer;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAll(Sort.by("id")).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return TaskResponse.from(getTask(id));
    }

    public TaskResponse create(TaskRequest request) {
        TaskStatus status = request.status() != null ? request.status() : TaskStatus.TODO;
        Task task = new Task(request.title(), HtmlSanitizer.sanitize(request.description()), status);
        return TaskResponse.from(taskRepository.save(task));
    }

    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getTask(id);
        task.setTitle(request.title());
        task.setDescription(HtmlSanitizer.sanitize(request.description()));
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        return TaskResponse.from(task);
    }

    public void delete(Long id) {
        Task task = getTask(id);
        taskRepository.delete(task);
    }

    private Task getTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }
}
