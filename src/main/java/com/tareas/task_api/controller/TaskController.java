package com.tareas.task_api.controller;

import com.tareas.task_api.model.Task;
import com.tareas.task_api.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController = @Controller + @ResponseBody
// Todos los métodos devuelven JSON automáticamente
@RestController
// @RequestMapping: prefijo para todos los endpoints de este controller
@RequestMapping("/api/tasks")
// @RequiredArgsConstructor: Lombok genera el constructor (inyección por constructor)
@RequiredArgsConstructor
public class TaskController {

    // El controller SOLO habla con el servicio, NUNCA con el repositorio
    private final TaskService taskService;

    // GET /api/tasks
    // GET /api/tasks?completed=true
    // GET /api/tasks?completed=false
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(
            // @RequestParam: parámetro opcional de la URL (?completed=true)
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) Task.Priority priority,
            @RequestParam(required = false) String search) {

        if (completed != null) {
            return ResponseEntity.ok(taskService.getTasksByCompleted(completed));
        }
        if (priority != null) {
            return ResponseEntity.ok(taskService.getTasksByPriority(priority));
        }
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(taskService.searchByTitle(search));
        }
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    // GET /api/tasks/1
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            // @PathVariable: extrae el {id} de la URL
            @PathVariable Long id) {
        // Si no existe, TaskService lanza TaskNotFoundException
        // El GlobalExceptionHandler la captura y devuelve 404
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    // POST /api/tasks
    @PostMapping
    public ResponseEntity<Task> createTask(
            // @RequestBody: deserializa el JSON del body a un objeto Task
            @RequestBody Task task) {
        Task created = taskService.createTask(task);
        // ResponseEntity.status(201).body(created) = HTTP 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // PUT /api/tasks/1
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody Task task) {
        return ResponseEntity.ok(taskService.updateTask(id, task));
    }

    // PATCH /api/tasks/1/complete
    // Endpoint específico para marcar como completada sin enviar todo el body
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Task> markAsComplete(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.markAsComplete(id));
    }

    // DELETE /api/tasks/1
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        // 204 No Content: operación exitosa sin cuerpo de respuesta
        return ResponseEntity.noContent().build();
    }
}