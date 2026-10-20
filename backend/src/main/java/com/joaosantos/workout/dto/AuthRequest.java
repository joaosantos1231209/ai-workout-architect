package com.joaosantos.workout.dto;

/**
 * Pedido de autenticação (Login).
 */
public record AuthRequest(
        String email,
        String password
) {
}
