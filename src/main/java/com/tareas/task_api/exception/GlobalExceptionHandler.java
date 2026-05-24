package com.tareas.task_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// @RestControllerAdvice: intercepta excepciones de TODOS los controllers
// y devuelve JSON automáticamente
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Método auxiliar para construir el body de error de forma consistente
    private Map<String, Object> buildErrorBody(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    // Captura TaskNotFoundException -> HTTP 404
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTaskNotFound(
            TaskNotFoundException ex, WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildErrorBody(404, "Not Found", ex.getMessage()));
    }

    // Captura cualquier otra excepción no esperada -> HTTP 500
    // IMPORTANTE: En producción no expongas el mensaje real (podría dar pistas a atacantes)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, WebRequest request) {
        // Logueamos el error real para debugging, pero no lo exponemos al cliente
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorBody(500, "Internal Server Error",
                        "Ha ocurrido un error inesperado. Contacta con el administrador."));
    }
}