package com.tareas.task_api.repository;

import com.tareas.task_api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

// JpaRepository<Task, Long>:
// - Task: la entidad que gestiona este repositorio
// - Long: el tipo de la clave primaria
// Spring genera la implementación automáticamente en tiempo de arranque
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // ===== Consultas derivadas del nombre del método =====
    // Spring Data JPA lee el nombre y genera el SQL automáticamente:

    // findByCompleted(true) -> SELECT * FROM tasks WHERE completed = true
    List<Task> findByCompleted(boolean completed);

    // findByPriority(HIGH) -> SELECT * FROM tasks WHERE priority = 'HIGH'
    List<Task> findByPriority(Task.Priority priority);

    // findByTitleContainingIgnoreCase("spring") ->
    // SELECT * FROM tasks WHERE LOWER(title) LIKE '%spring%'
    List<Task> findByTitleContainingIgnoreCase(String keyword);

    // Combinaciones:
    // findByCompletedAndPriority(false, HIGH) ->
    // SELECT * FROM tasks WHERE completed = false AND priority = 'HIGH'
    List<Task> findByCompletedAndPriority(boolean completed, Task.Priority priority);

    // ===== Consulta JPQL personalizada =====
    // JPQL usa nombres de clases y campos Java, no nombres de tabla SQL
    @Query("SELECT t FROM Task t WHERE t.completed = false ORDER BY t.createdAt DESC")
    List<Task> findPendingTasksOrderedByDate();

    // ===== Métodos heredados de JpaRepository (sin necesidad de definirlos) =====
    // findAll()         -> SELECT * FROM tasks
    // findById(id)      -> SELECT * FROM tasks WHERE id = ?
    // save(task)        -> INSERT o UPDATE según si el id existe
    // deleteById(id)    -> DELETE FROM tasks WHERE id = ?
    // count()           -> SELECT COUNT(*) FROM tasks
    // existsById(id)    -> SELECT EXISTS(SELECT * FROM tasks WHERE id = ?)
}