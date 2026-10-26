package com.joaosantos.workout.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Resposta com o plano de treino completo de um dia, incluindo status e lista de exercícios.
 */
public record WorkoutPlanResponse(
        Long id,
        String title,
        String focusArea,
        String warmupRoutine,
        String cooldownRoutine,
        String explanation,
        LocalDate workoutDate,
        boolean completed,
        String perceivedDifficulty,
        List<ExerciseDto> exercises
) {
}
