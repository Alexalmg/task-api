package com.tareas.task_api.exception;

// Extendemos RuntimeException (no checked exception)
// Las unchecked exceptions no obligan al llamador a manejarlas con try-catch
// Spring las captura y las convierte en respuestas HTTP
public class TaskNotFoundException extends RuntimeException {

    private final Long taskId;

    public TaskNotFoundException(Long id) {
        super("Tarea no encontrada con id: " + id);
        this.taskId = id;
    }

    public Long getTaskId() {
        return taskId;
    }
}