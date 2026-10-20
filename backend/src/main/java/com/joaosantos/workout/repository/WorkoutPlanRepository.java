package com.joaosantos.workout.repository;

import com.joaosantos.workout.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações com WorkoutPlan.
 */
@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    /**
     * Devolve todos os planos de treino de um utilizador ordenados pela data (mais recente primeiro).
     */
    List<WorkoutPlan> findByUserProfileIdOrderByWorkoutDateDesc(Long userProfileId);

    /**
     * Localiza o plano de treino do utilizador para uma data específica (ex: hoje).
     */
    Optional<WorkoutPlan> findByUserProfileIdAndWorkoutDate(Long userProfileId, LocalDate workoutDate);

    /**
     * Devolve todos os planos de treino concluídos ordenados por data decrescente (para streak e stats).
     */
    List<WorkoutPlan> findByUserProfileIdAndCompletedTrueOrderByWorkoutDateDesc(Long userProfileId);

    /**
     * Devolve o último plano concluído do utilizador (para calibrar o próximo treino por IA).
     */
    Optional<WorkoutPlan> findFirstByUserProfileIdAndCompletedTrueOrderByWorkoutDateDesc(Long userProfileId);
}
