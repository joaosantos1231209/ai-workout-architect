package com.joaosantos.workout.dto;

/**
 * Pedido de registo de nova conta de atleta.
 */
public record RegisterRequest(
        String name,
        String email,
        String password,
        Integer age,
        String fitnessLevel,
        String primaryGoal
) {
}
