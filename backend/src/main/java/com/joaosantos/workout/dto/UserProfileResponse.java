package com.joaosantos.workout.dto;

/**
 * Resposta com os dados do perfil do atleta (sem expor a password).
 */
public record UserProfileResponse(
        Long id,
        String name,
        String email,
        Integer age,
        String fitnessLevel,
        String primaryGoal
) {
}
