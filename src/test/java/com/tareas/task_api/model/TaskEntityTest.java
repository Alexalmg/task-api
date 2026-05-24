package com.tareas.task_api.model; // O la ruta correcta de tu test

import com.tareas.task_api.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;

// @DataJpaTest: carga solo la capa JPA, usa H2 automáticamente, muy rápido
@DataJpaTest
@DisplayName("Tests de la entidad Task y TaskRepository con JPA")
class TaskEntityTest {

    // TestEntityManager: herramienta de test para manipular la BD directamente
    // Más potente que el repositorio porque permite flush/clear de la caché
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TaskRepository taskRepository;

    // Variables de test reutilizables
    private Task taskAlta;
    private Task taskMedia;
    private Task taskBaja;

    @BeforeEach
        // Se ejecuta antes de CADA test, reinicia el estado
    void setUp() {
        taskAlta = new Task();
        taskAlta.setTitle("Tarea de alta prioridad");
        taskAlta.setDescription("Esta tarea es urgente");
        taskAlta.setPriority(Task.Priority.HIGH);
        taskAlta.setCompleted(false);

        taskMedia = new Task();
        taskMedia.setTitle("Aprender Spring Boot");
        taskMedia.setPriority(Task.Priority.MEDIUM);
        taskMedia.setCompleted(false);

        taskBaja = new Task();
        taskBaja.setTitle("Leer documentación");
        taskBaja.setPriority(Task.Priority.LOW);
        taskBaja.setCompleted(true);  // esta está completada
    }

    @Test
    @DisplayName("save() asigna ID y timestamps automáticamente")
    void save_AssignsIdAndTimestamps() {
        // Given: una tarea nueva sin id
        Task nuevaTarea = new Task();
        nuevaTarea.setTitle("Mi tarea");
        nuevaTarea.setPriority(Task.Priority.MEDIUM);
        // id, createdAt, updatedAt son null en este momento

        // When: guardamos la tarea
        Task guardada = taskRepository.save(nuevaTarea);

        // Then: verificamos que la BD asignó los valores automáticamente
        assertThat(guardada.getId()).isNotNull();          // la BD asignó un id
        assertThat(guardada.getId()).isPositive();          // el id es positivo
        assertThat(guardada.getCreatedAt()).isNotNull();   // @PrePersist lo asignó
        assertThat(guardada.getUpdatedAt()).isNotNull();   // @PrePersist lo asignó
        assertThat(guardada.isCompleted()).isFalse();      // valor por defecto
        assertThat(guardada.getPriority()).isEqualTo(Task.Priority.MEDIUM);
    }

    @Test
    @DisplayName("save() + findById() persiste todos los campos correctamente")
    void saveAndFindById_PersistsAllFields() {
        // Given: guardamos una tarea con datos completos
        entityManager.persistAndFlush(taskAlta);
        // clear() limpia la caché de primer nivel de Hibernate
        // Fuerza que el siguiente findById vaya a la BD, no a la caché
        entityManager.clear();

        // When: leemos la tarea de la BD
        Task encontrada = taskRepository.findById(taskAlta.getId()).orElseThrow();

        // Then: todos los campos deben coincidir exactamente
        assertThat(encontrada.getTitle()).isEqualTo("Tarea de alta prioridad");
        assertThat(encontrada.getDescription()).isEqualTo("Esta tarea es urgente");
        assertThat(encontrada.getPriority()).isEqualTo(Task.Priority.HIGH);
        assertThat(encontrada.isCompleted()).isFalse();
        assertThat(encontrada.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByCompleted() filtra tareas pendientes y completadas")
    void findByCompleted_FiltersByStatus() {
        // Given: guardamos 3 tareas: 2 pendientes, 1 completada
        entityManager.persist(taskAlta);    // pendiente
        entityManager.persist(taskMedia);   // pendiente
        entityManager.persistAndFlush(taskBaja);  // completada

        // When
        List<Task> pendientes  = taskRepository.findByCompleted(false);
        List<Task> completadas = taskRepository.findByCompleted(true);

        // Then
        assertThat(pendientes).hasSize(6);
        assertThat(completadas).hasSize(4);
        assertThat(pendientes).extracting(Task::getTitle)
                .containsExactlyInAnyOrder("Implementar TaskService",
                        "Implementar TaskController",
                        "Anadir tests unitarios",
                        "Subir a GitHub","Tarea de alta prioridad", "Aprender Spring Boot");
        assertThat(completadas).extracting(Task::getTitle)
                .containsExactly("Configurar Spring Initializr", "Crear entidad Task", "Implementar TaskRepository", "Leer documentación");
    }

    @Test
    @DisplayName("findByPriority() filtra por prioridad exacta")
    void findByPriority_FiltersByPriority() {
        // Given
        entityManager.persist(taskAlta);
        entityManager.persist(taskMedia);
        entityManager.persistAndFlush(taskBaja);

        // When
        List<Task> altas  = taskRepository.findByPriority(Task.Priority.HIGH);
        List<Task> medias = taskRepository.findByPriority(Task.Priority.MEDIUM);
        List<Task> bajas  = taskRepository.findByPriority(Task.Priority.LOW);

        // Then
        assertThat(altas).hasSize(4);
        assertThat(medias).hasSize(4);
        assertThat(bajas).hasSize(2);
        assertThat(altas.get(0).getTitle()).isEqualTo("Configurar Spring Initializr");
    }

    @Test
    @DisplayName("findByTitleContainingIgnoreCase() hace búsqueda case-insensitive")
    void findByTitle_IsCaseInsensitive() {
        // Given
        entityManager.persist(taskAlta);
        entityManager.persistAndFlush(taskMedia);

        // When: buscamos con mayúsculas, minúsculas y mezcla
        List<Task> resultadosMayusculas = taskRepository.findByTitleContainingIgnoreCase("APRENDER");
        List<Task> resultadosMinusculas = taskRepository.findByTitleContainingIgnoreCase("aprender");
        List<Task> resultadosMezcla    = taskRepository.findByTitleContainingIgnoreCase("Aprender");
        List<Task> sinResultados       = taskRepository.findByTitleContainingIgnoreCase("xyz");

        // Then: todos devuelven el mismo resultado
        assertThat(resultadosMayusculas).hasSize(1);
        assertThat(resultadosMinusculas).hasSize(1);
        assertThat(resultadosMezcla).hasSize(1);
        assertThat(sinResultados).isEmpty();
    }

    @Test
    @DisplayName("deleteById() elimina la tarea correctamente")
    void deleteById_RemovesTask() {
        // Given
        entityManager.persistAndFlush(taskAlta);
        Long id = taskAlta.getId();

        // When
        taskRepository.deleteById(id);
        entityManager.flush();

        // Then: la tarea ya no existe
        assertThat(taskRepository.findById(id)).isEmpty();
        assertThat(taskRepository.count()).isEqualTo(7);
    }

    @Test
    @DisplayName("count() devuelve el número correcto de tareas")
    void count_ReturnsCorrectNumber() {
        // Given: empezamos con 0 tareas
        assertThat(taskRepository.count()).isEqualTo(7);

        // When: añadimos 3 tareas
        entityManager.persist(taskAlta);
        entityManager.persist(taskMedia);
        entityManager.persistAndFlush(taskBaja);

        // Then
        assertThat(taskRepository.count()).isEqualTo(10);
    }

    @Test
    @DisplayName("findByCompletedAndPriority() combina dos filtros")
    void findByCompletedAndPriority_CombinesFilters() {
        // Given
        entityManager.persist(taskAlta);  // pendiente + HIGH
        entityManager.persist(taskMedia); // pendiente + MEDIUM
        entityManager.persistAndFlush(taskBaja);  // completada + LOW

        // When: buscamos tareas pendientes de alta prioridad
        List<Task> pendientesAltas = taskRepository.findByCompletedAndPriority(
                false, Task.Priority.HIGH);

        // Then: solo taskAlta cumple ambas condiciones
        assertThat(pendientesAltas).hasSize(2);
        assertThat(pendientesAltas.get(0).getTitle()).isEqualTo("Implementar TaskController");
    }
}