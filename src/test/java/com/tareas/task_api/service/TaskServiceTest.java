package com.tareas.task_api.service;

import com.tareas.task_api.exception.TaskNotFoundException;
import com.tareas.task_api.model.Task;
import com.tareas.task_api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class): activa Mockito para esta clase de test
// NO arranca Spring, no usa BD: es puro Java
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitarios de TaskService")
class TaskServiceTest {

    // @Mock: crea un mock del repositorio (objeto falso que simula el real)
    @Mock
    private TaskRepository taskRepository;

    // @InjectMocks: crea una instancia real de TaskService e inyecta los @Mock
    @InjectMocks
    private TaskService taskService;

    // Datos de prueba compartidos
    private Task taskExistente;
    private Task taskCompletada;

    @BeforeEach
    void setUp() {
        taskExistente = new Task();
        taskExistente.setId(1L);
        taskExistente.setTitle("Aprender JUnit 5");
        taskExistente.setDescription("Tests unitarios con Mockito");
        taskExistente.setCompleted(false);
        taskExistente.setPriority(Task.Priority.HIGH);

        taskCompletada = new Task();
        taskCompletada.setId(2L);
        taskCompletada.setTitle("Configurar IntelliJ");
        taskCompletada.setCompleted(true);
        taskCompletada.setPriority(Task.Priority.LOW);
    }

    // @Nested permite agrupar tests relacionados visualmente
    @Nested
    @DisplayName("getAllTasks()")
    class GetAllTasks {

        @Test
        @DisplayName("devuelve lista con todas las tareas")
        void getAllTasks_ReturnsList() {
            // Given
            when(taskRepository.findAll()).thenReturn(List.of(taskExistente, taskCompletada));

            // When
            List<Task> result = taskService.getAllTasks();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Task::getTitle)
                    .containsExactlyInAnyOrder("Aprender JUnit 5", "Configurar IntelliJ");
            verify(taskRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay tareas")
        void getAllTasks_EmptyList_ReturnsEmpty() {
            // Given
            when(taskRepository.findAll()).thenReturn(List.of());

            // When
            List<Task> result = taskService.getAllTasks();

            // Then
            assertThat(result).isEmpty();
            verify(taskRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("getTaskById()")
    class GetTaskById {

        @Test
        @DisplayName("devuelve la tarea cuando existe")
        void getTaskById_WhenExists_ReturnsTask() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(taskExistente));

            // When
            Task result = taskService.getTaskById(1L);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Aprender JUnit 5");
            assertThat(result.getPriority()).isEqualTo(Task.Priority.HIGH);
        }

        @Test
        @DisplayName("lanza TaskNotFoundException cuando no existe")
        void getTaskById_WhenNotFound_ThrowsException() {
            // Given
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // When / Then: verificamos la excepción y su mensaje
            assertThatThrownBy(() -> taskService.getTaskById(99L))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining("99");

            verify(taskRepository, times(1)).findById(99L);
        }
    }

    @Nested
    @DisplayName("createTask()")
    class CreateTask {

        @Test
        @DisplayName("guarda la tarea y devuelve el objeto con id")
        void createTask_SavesAndReturnsWithId() {
            // Given: simulamos que save() devuelve la tarea con id asignado
            when(taskRepository.save(any(Task.class))).thenReturn(taskExistente);

            Task nueva = new Task();
            nueva.setTitle("Nueva tarea");
            nueva.setPriority(Task.Priority.MEDIUM);

            // When
            Task result = taskService.createTask(nueva);

            // Then
            assertThat(result.getId()).isEqualTo(1L);  // el mock devuelve taskExistente
            assertThat(result.getTitle()).isEqualTo("Aprender JUnit 5");
            // Verificamos que save() fue llamado exactamente una vez con cualquier Task
            verify(taskRepository, times(1)).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("updateTask()")
    class UpdateTask {

        @Test
        @DisplayName("actualiza y devuelve la tarea modificada")
        void updateTask_UpdatesCorrectFields() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(taskExistente));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
            // inv.getArgument(0): el mock devuelve el mismo objeto que recibe

            Task datos = new Task();
            datos.setTitle("Título actualizado");
            datos.setDescription("Descripción nueva");
            datos.setCompleted(true);
            datos.setPriority(Task.Priority.LOW);

            // When
            Task result = taskService.updateTask(1L, datos);

            // Then
            assertThat(result.getTitle()).isEqualTo("Título actualizado");
            assertThat(result.isCompleted()).isTrue();
            assertThat(result.getPriority()).isEqualTo(Task.Priority.LOW);
            verify(taskRepository, times(1)).findById(1L);
            verify(taskRepository, times(1)).save(any(Task.class));
        }

        @Test
        @DisplayName("lanza excepción si la tarea a actualizar no existe")
        void updateTask_WhenNotFound_Throws() {
            // Given
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> taskService.updateTask(99L, new Task()))
                    .isInstanceOf(TaskNotFoundException.class);

            // IMPORTANTE: si no encuentra la tarea, nunca debe llamar a save()
            verify(taskRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteTask()")
    class DeleteTask {

        @Test
        @DisplayName("elimina la tarea cuando existe")
        void deleteTask_WhenExists_Deletes() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(taskExistente));

            // When
            taskService.deleteTask(1L);

            // Then: verificamos que se llamó a delete con la tarea correcta
            verify(taskRepository, times(1)).delete(taskExistente);
        }

        @Test
        @DisplayName("lanza excepción si la tarea a borrar no existe")
        void deleteTask_WhenNotFound_ThrowsAndNeverDeletes() {
            // Given
            when(taskRepository.findById(99L)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> taskService.deleteTask(99L))
                    .isInstanceOf(TaskNotFoundException.class);

            // NUNCA se debe llamar a delete() si la tarea no existe
            verify(taskRepository, never()).delete(any());
            verify(taskRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("markAsComplete()")
    class MarkAsComplete {

        @Test
        @DisplayName("marca la tarea como completada")
        void markAsComplete_SetsCompletedTrue() {
            // Given
            when(taskRepository.findById(1L)).thenReturn(Optional.of(taskExistente));
            when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Task result = taskService.markAsComplete(1L);

            // Then
            assertThat(result.isCompleted()).isTrue();
            verify(taskRepository, times(1)).save(any(Task.class));
        }
    }
}