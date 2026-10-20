package com.joaosantos.workout.controller;

import com.joaosantos.workout.dto.ExerciseDto;
import com.joaosantos.workout.dto.ExerciseRecord;
import com.joaosantos.workout.dto.WorkoutPlanRecord;
import com.joaosantos.workout.dto.WorkoutPlanResponse;
import com.joaosantos.workout.model.Exercise;
import com.joaosantos.workout.model.UserProfile;
import com.joaosantos.workout.model.WorkoutPlan;
import com.joaosantos.workout.repository.ExerciseRepository;
import com.joaosantos.workout.repository.UserProfileRepository;
import com.joaosantos.workout.repository.WorkoutPlanRepository;
import com.joaosantos.workout.service.AiWorkoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Controller REST para geração, consulta e gestão diária de planos de treino em casa.
 */
@RestController
@RequestMapping("/workouts")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, originPatterns = "*")
@Slf4j
public class WorkoutController {

    private final AiWorkoutService aiWorkoutService;
    private final UserProfileRepository userProfileRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final ExerciseRepository exerciseRepository;

    public WorkoutController(
            AiWorkoutService aiWorkoutService,
            UserProfileRepository userProfileRepository,
            WorkoutPlanRepository workoutPlanRepository,
            ExerciseRepository exerciseRepository
    ) {
        this.aiWorkoutService = aiWorkoutService;
        this.userProfileRepository = userProfileRepository;
        this.workoutPlanRepository = workoutPlanRepository;
        this.exerciseRepository = exerciseRepository;
    }

    /**
     * Gera ou substitui o treino de um dia específico para um atleta autenticado.
     */
    @PostMapping("/generate")
    @Transactional
    public ResponseEntity<WorkoutPlanResponse> generateWorkout(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String fitnessLevel,
            @RequestParam(required = false) String focusArea,
            @RequestBody(required = false) UserProfile incomingProfile
    ) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        UserProfile profile;
        if (userId != null) {
            profile = userProfileRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atleta não encontrado."));
        } else if (incomingProfile != null) {
            profile = userProfileRepository.save(incomingProfile);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É obrigatório fornecer o ID do utilizador.");
        }

        log.info("A gerar treino em casa para {} | Data: {} | Nível: {} | Foco: {}",
                profile.getName(), targetDate, fitnessLevel, focusArea);

        // Se já existe treino para esta data, podemos reutilizá-lo ou substituí-lo
        Optional<WorkoutPlan> existingPlanOpt = workoutPlanRepository.findByUserProfileIdAndWorkoutDate(profile.getId(), targetDate);
        if (existingPlanOpt.isPresent()) {
            workoutPlanRepository.delete(existingPlanOpt.get());
            workoutPlanRepository.flush();
        }

        // Sintetizar com IA
        WorkoutPlanRecord planRecord = aiWorkoutService.generateWorkoutForUser(profile, fitnessLevel, focusArea);

        // Mapear para entidade JPA
        WorkoutPlan workoutPlan = WorkoutPlan.builder()
                .title(planRecord.title())
                .focusArea(planRecord.focusArea())
                .explanation(planRecord.explanation())
                .workoutDate(targetDate)
                .completed(false)
                .userProfile(profile)
                .build();

        if (planRecord.exercises() != null) {
            for (ExerciseRecord er : planRecord.exercises()) {
                Exercise ex = Exercise.builder()
                        .name(er.name())
                        .sets(er.sets())
                        .reps(er.reps())
                        .equipmentNeeded(er.equipmentNeeded())
                        .instructions(er.instructions())
                        .completed(false)
                        .build();
                workoutPlan.addExercise(ex);
            }
        }

        WorkoutPlan savedPlan = workoutPlanRepository.save(workoutPlan);
        log.info("Plano de treino guardado com ID: {} para a data: {}", savedPlan.getId(), targetDate);

        return ResponseEntity.ok(toResponse(savedPlan));
    }

    /**
     * Consulta todos os planos de treino de um utilizador.
     */
    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<WorkoutPlanResponse>> getUserWorkouts(@PathVariable Long userId) {
        List<WorkoutPlan> plans = workoutPlanRepository.findByUserProfileIdOrderByWorkoutDateDesc(userId);
        List<WorkoutPlanResponse> responseList = plans.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responseList);
    }

    /**
     * Consulta o plano de treino de um dia específico.
     */
    @GetMapping("/user/{userId}/date/{date}")
    @Transactional(readOnly = true)
    public ResponseEntity<WorkoutPlanResponse> getWorkoutByDate(
            @PathVariable Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return workoutPlanRepository.findByUserProfileIdAndWorkoutDate(userId, date)
                .map(plan -> ResponseEntity.ok(toResponse(plan)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Assinala um exercício como concluído ou pendente (toggle).
     * Se todos os exercícios forem concluídos, atualiza o dia como cumprido!
     */
    @PatchMapping("/{workoutId}/exercises/{exerciseId}/toggle")
    @Transactional
    public ResponseEntity<WorkoutPlanResponse> toggleExercise(
            @PathVariable Long workoutId,
            @PathVariable Long exerciseId
    ) {
        WorkoutPlan plan = workoutPlanRepository.findById(workoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de treino não encontrado."));

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercício não encontrado."));

        exercise.setCompleted(!exercise.isCompleted());
        exerciseRepository.save(exercise);

        // Verificar se todos os exercícios do treino estão concluídos
        boolean allCompleted = plan.getExercises().stream().allMatch(Exercise::isCompleted);
        plan.setCompleted(allCompleted);
        workoutPlanRepository.save(plan);

        log.info("Exercício ID: {} assinalado como: {}. Plano do dia concluído: {}",
                exercise.getId(), exercise.isCompleted() ? "FEITO" : "PENDENTE", allCompleted);

        return ResponseEntity.ok(toResponse(plan));
    }

    private WorkoutPlanResponse toResponse(WorkoutPlan plan) {
        List<ExerciseDto> exerciseDtos = plan.getExercises().stream()
                .map(ex -> new ExerciseDto(
                        ex.getId(),
                        ex.getName(),
                        ex.getSets(),
                        ex.getReps(),
                        ex.getEquipmentNeeded(),
                        ex.getInstructions(),
                        ex.isCompleted()
                ))
                .toList();

        return new WorkoutPlanResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getFocusArea(),
                plan.getExplanation(),
                plan.getWorkoutDate(),
                plan.isCompleted(),
                exerciseDtos
        );
    }
}
