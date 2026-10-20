package com.joaosantos.workout.dto;

/**
 * Immutable Java Record representing an individual exercise item
 * within an AI-generated structured workout plan.
 *
 * @param name            the movement name (e.g., "Barbell Incline Bench Press")
 * @param sets            number of prescribed sets
 * @param reps            prescribed repetition range (e.g., "8-10 reps")
 * @param equipmentNeeded necessary gym equipment or alternatives (e.g., "Barbell, Incline Bench")
 * @param instructions    technical cues, tempo notes, and execution guidance
 */
public record ExerciseRecord(
        String name,
        int sets,
        String reps,
        String equipmentNeeded,
        String instructions
) {
}
