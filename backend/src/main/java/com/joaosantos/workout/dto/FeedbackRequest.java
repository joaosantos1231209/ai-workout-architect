package com.joaosantos.workout.dto;

/**
 * Pedido de feedback de dificuldade percebida no final do treino ("Muito Fácil", "Ideal", "Muito Difícil").
 */
public record FeedbackRequest(
        String perceivedDifficulty
) {
}
