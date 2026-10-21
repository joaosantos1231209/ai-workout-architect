package com.joaosantos.workout.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade JPA que representa um plano de treino diário gerado por IA,
 * associado a um dia específico do calendário e ao atleta.
 */
@Entity
@Table(name = "workout_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "focus_area", nullable = false)
    private String focusArea;

    @Column(name = "warmup_routine", length = 2000)
    private String warmupRoutine;

    @Column(length = 2000)
    private String explanation;

    @Column(name = "workout_date", nullable = false)
    @Builder.Default
    private LocalDate workoutDate = LocalDate.now();

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(name = "perceived_difficulty")
    private String perceivedDifficulty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserProfile userProfile;

    @OneToMany(mappedBy = "workoutPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Exercise> exercises = new ArrayList<>();

    /**
     * Adiciona um exercício mantendo a integridade da relação bidirecional.
     */
    public void addExercise(Exercise exercise) {
        if (exercises == null) {
            exercises = new ArrayList<>();
        }
        exercises.add(exercise);
        exercise.setWorkoutPlan(this);
    }
}
