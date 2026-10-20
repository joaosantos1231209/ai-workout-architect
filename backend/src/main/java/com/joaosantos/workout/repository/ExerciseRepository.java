package com.joaosantos.workout.repository;

import com.joaosantos.workout.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositório Spring Data JPA para operações com a entidade Exercise.
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
}
