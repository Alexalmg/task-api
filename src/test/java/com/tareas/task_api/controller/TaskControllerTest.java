package com.tareas.task_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tareas.task_api.exception.GlobalExceptionHandler;
import com.tareas.task_api.exception.TaskNotFoundException;
import com.tareas.task_api.model.Task;
import com.tareas.task_api.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest: solo carga la capa web (controller + filtros), muy rápido
// Sin @Import, el GlobalExceptionHandler no se cargaría automáticamente
@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("Tests del TaskController con MockMvc")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Simula requests HTTP sin servidor real

    @Autowired
    private ObjectMapper objectMapper;  // Serializa/deserializa JSON

    // @MockBean: crea un mock y lo registra en el contexto de Spring
    // Es diferente de @Mock de Mockito: @MockBean funciona con @WebMvcTest
    @MockBean
    private TaskService taskService;

    private Task task1;
    private Task task2;

    @BeforeEach
    void setUp() {
        task1 = new Task();
        task1.setId(1L);
        task1.setTitle("Aprender Spring Boot");
        task1.setCompleted(false);
        task1.setPriority(Task.Priority.HIGH);

        task2 = new Task();
        task2.setId(2L);
        task2.setTitle("Configurar Docker");
        task2.setCompleted(true);
        task2.setPriority(Task.Priority.MEDIUM);
    }

    @Nested
    @DisplayName("GET /api/tasks")
    class GetAllTasks {

        @Test
        @DisplayName("devuelve 200 con lista de tareas")
        void getAllTasks_Returns200WithList() throws Exception {
            // Given
            when(taskService.getAllTasks()).thenReturn(List.of(task1, task2));

            // When / Then
            mockMvc.perform(get("/api/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].title").value("Aprender Spring Boot"))
                    .andExpect(jsonPath("$[1].title").value("Configurar Docker"));

            verify(taskService, times(1)).getAllTasks();
        }

        @Test
        @DisplayName("GET /api/tasks?completed=true filtra por completadas")
        void getAllTasks_WithCompletedFilter_Filters() throws Exception {
            // Given
            when(taskService.getTasksByCompleted(true)).thenReturn(List.of(task2));

            // When / Then
            mockMvc.perform(get("/api/tasks").param("completed", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].completed").value(true));

            verify(taskService, times(1)).getTasksByCompleted(true);
            verify(taskService, never()).getAllTasks();  // no se llama al método sin filtro
        }
    }

    @Nested
    @DisplayName("GET /api/tasks/{id}")
    class GetTaskById {

        @Test
        @DisplayName("devuelve 200 con la tarea cuando existe")
        void getTaskById_WhenExists_Returns200() throws Exception {
            // Given
            when(taskService.getTaskById(1L)).thenReturn(task1);

            // When / Then
            mockMvc.perform(get("/api/tasks/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Aprender Spring Boot"))
                    .andExpect(jsonPath("$.priority").value("HIGH"));
        }

        @Test
        @DisplayName("devuelve 404 con JSON de error cuando no existe")
        void getTaskById_WhenNotFound_Returns404WithJson() throws Exception {
            // Given
            when(taskService.getTaskById(99L)).thenThrow(new TaskNotFoundException(99L));

            // When / Then
            mockMvc.perform(get("/api/tasks/99"))
                    .andExpect(status().isNotFound())   // HTTP 404
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Tarea no encontrada con id: 99"));
        }
    }

    @Nested
    @DisplayName("POST /api/tasks")
    class CreateTask {

        @Test
        @DisplayName("devuelve 201 con la tarea creada")
        void createTask_ValidBody_Returns201() throws Exception {
            // Given
            when(taskService.createTask(any(Task.class))).thenReturn(task1);
            String json = objectMapper.writeValueAsString(task1);

            // When / Then
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())   // HTTP 201
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Aprender Spring Boot"));

            verify(taskService, times(1)).createTask(any(Task.class));
        }

        @Test
        @DisplayName("devuelve 400 si el body no es JSON válido")
        void createTask_InvalidJson_Returns400() throws Exception {
            mockMvc.perform(post("/api/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("\"esto no es json\""))
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("PUT /api/tasks/{id}")
    class UpdateTask {

        @Test
        @DisplayName("devuelve 200 con la tarea actualizada")
        void updateTask_WhenExists_Returns200() throws Exception {
            // Given
            when(taskService.updateTask(eq(1L), any(Task.class))).thenReturn(task1);
            String json = objectMapper.writeValueAsString(task1);

            // When / Then
            mockMvc.perform(put("/api/tasks/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("devuelve 404 si la tarea a actualizar no existe")
        void updateTask_WhenNotFound_Returns404() throws Exception {
            // Given
            when(taskService.updateTask(eq(99L), any(Task.class)))
                    .thenThrow(new TaskNotFoundException(99L));

            // When / Then
            mockMvc.perform(put("/api/tasks/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new Task())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    @Nested
    @DisplayName("PATCH /api/tasks/{id}/complete")
    class MarkAsComplete {

        @Test
        @DisplayName("devuelve 200 con la tarea marcada como completada")
        void markAsComplete_Returns200WithCompletedTrue() throws Exception {
            // Given: preparamos la tarea ya completada
            task1.setCompleted(true);
            when(taskService.markAsComplete(1L)).thenReturn(task1);

            // When / Then
            mockMvc.perform(patch("/api/tasks/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completed").value(true));
        }

        @Test
        @DisplayName("devuelve 404 si la tarea no existe")
        void markAsComplete_WhenNotFound_Returns404() throws Exception {
            // Given
            when(taskService.markAsComplete(99L)).thenThrow(new TaskNotFoundException(99L));

            // When / Then
            mockMvc.perform(patch("/api/tasks/99/complete"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("devuelve 204 sin body cuando existe")
        void deleteTask_WhenExists_Returns204() throws Exception {
            // Given: doNothing porque deleteTask() devuelve void
            doNothing().when(taskService).deleteTask(1L);

            // When / Then
            mockMvc.perform(delete("/api/tasks/1"))
                    .andExpect(status().isNoContent())  // HTTP 204
                    .andExpect(content().string(""));    // sin body

            verify(taskService, times(1)).deleteTask(1L);
        }

        @Test
        @DisplayName("devuelve 404 si la tarea no existe")
        void deleteTask_WhenNotFound_Returns404() throws Exception {
            // Given
            doThrow(new TaskNotFoundException(99L)).when(taskService).deleteTask(99L);

            // When / Then
            mockMvc.perform(delete("/api/tasks/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Tarea no encontrada con id: 99"));
        }
    }
}