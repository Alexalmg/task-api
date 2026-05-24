package com.tareas.task_api.model;

import jakarta.persistence.*;  // Jakarta EE (el nuevo nombre de Java EE)
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// @Entity: le dice a Hibernate que esta clase es una tabla de BD
@Entity
// @Table: nombre explícito de la tabla (buena práctica, aunque sin esto usaría 'task')
@Table(name = "tasks")
// @Data: Lombok genera getters, setters, equals, hashCode y toString
@Data
// @NoArgsConstructor: constructor sin parámetros (JPA lo requiere)
@NoArgsConstructor
// @AllArgsConstructor: constructor con todos los campos (conveniente para tests)
@AllArgsConstructor
public class Task {

    // @Id: este campo es la clave primaria
    @Id
    // @GeneratedValue: la BD genera el valor automáticamente
    // IDENTITY: usa la estrategia de autoincremento de la BD (SERIAL en PostgreSQL, AUTO_INCREMENT en MySQL)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(nullable = false): esta columna NO puede ser NULL en BD
    // length = 255: longitud máxima del VARCHAR
    @Column(nullable = false, length = 255)
    private String title;

    // Sin @Column: usa los valores por defecto (nullable = true, length = 255)
    // length = 1000 para descripción más larga
    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean completed = false;  // valor por defecto: no completada

    // @Enumerated(EnumType.STRING): guarda el enum como texto ('LOW', 'MEDIUM', 'HIGH')
    // Si usaras EnumType.ORDINAL guardaria 0, 1, 2 (peligroso: si cambias el orden se rompe todo)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    // updatable = false: una vez insertado, este campo no se actualiza
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // @PrePersist: se ejecuta justo antes de hacer INSERT en la BD
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // @PreUpdate: se ejecuta justo antes de hacer UPDATE en la BD
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enum anidado: las prioridades posibles
    public enum Priority {
        LOW,    // baja
        MEDIUM, // media (valor por defecto)
        HIGH    // alta
    }
}