package com.joaosantos.workout.dto;

import java.util.List;

/**
 * Estatísticas do atleta para gamificação (streak de dias seguidos e conquistas desbloqueadas).
 */
public record UserStatsResponse(
        int streakDays,
        int totalCompletedWorkouts,
        List<String> unlockedBadges
) {
}
