package com.tareas.task_api.service;

import com.tareas.task_api.exception.TaskNotFoundException;
import com.tareas.task_api.model.Task;
import com.tareas.task_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @Service: marca como bean de Spring y comunica que es la capa de negocio
@Service
// @RequiredArgsConstructor: Lombok genera el constructor con todos los campos 'final'
// Esto es INYECCIÓN POR CONSTRUCTOR (mejor que @Autowired en campo)
@RequiredArgsConstructor
// @Slf4j: Lombok genera 'log' como campo estático
// Equivale a: private static final Logger log = LoggerFactory.getLogger(TaskService.class);
@Slf4j
public class TaskService {

    // 'final' + @RequiredArgsConstructor = Spring inyecta esto por constructor
    private final TaskRepository taskRepository;

    // Los métodos de solo lectura NO necesitan @Transactional
    // (Spring crea la transacción automáticamente para JPA si es necesario)
    public List<Task> getAllTasks() {
        log.info("Obteniendo todas las tareas");
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        log.debug("Buscando tarea con id: {}", id);  // {} es el placeholder de SLF4J
        // findById devuelve Optional<Task>
        // orElseThrow: si el Optional está vacío, lanza la excepción
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    public List<Task> getTasksByCompleted(boolean completed) {
        return taskRepository.findByCompleted(completed);
    }

    public List<Task> getTasksByPriority(Task.Priority priority) {
        return taskRepository.findByPriority(priority);
    }

    public List<Task> searchByTitle(String keyword) {
        return taskRepository.findByTitleContainingIgnoreCase(keyword);
    }

    // @Transactional: Spring gestiona la transacción
    // Si el método lanza una RuntimeException, se hace ROLLBACK automáticamente
    @Transactional
    public Task createTask(Task task) {
        log.info("Creando tarea: '{}'", task.getTitle());
        Task saved = taskRepository.save(task);
        log.info("Tarea creada con id: {}", saved.getId());
        return saved;
    }

    @Transactional
    public Task updateTask(Long id, Task updatedTask) {
        // Primero verificamos que existe (lanza 404 si no)
        Task existing = getTaskById(id);

        // Actualizamos solo los campos que acepta el API
        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setCompleted(updatedTask.isCompleted());
        existing.setPriority(updatedTask.getPriority());

        // @PreUpdate se disparará automáticamente (actualiza updatedAt)
        Task saved = taskRepository.save(existing);
        log.info("Tarea {} actualizada", id);
        return saved;
    }

    @Transactional
    public void deleteTask(Long id) {
        // Verificamos que existe antes de borrar (lanza 404 si no)
        Task task = getTaskById(id);
        taskRepository.delete(task);
        log.info("Tarea {} eliminada", id);
    }

    @Transactional
    public Task markAsComplete(Long id) {
        Task task = getTaskById(id);
        task.setCompleted(true);
        return taskRepository.save(task);
    }
}