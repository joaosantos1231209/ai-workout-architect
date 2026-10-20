package com.joaosantos.workout.dto;

/**
 * DTO para representar um exercício persistido e o seu estado de conclusão.
 */
public record ExerciseDto(
        Long id,
        String name,
        int sets,
        String reps,
        String equipmentNeeded,
        String instructions,
        boolean completed
) {
}
