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
import com.joaosantos.workout.dto.FeedbackRequest;
import com.joaosantos.workout.dto.UserStatsResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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

        // Obter último feedback de dificuldade do atleta para adaptação inteligente da IA
        String lastFeedback = workoutPlanRepository.findFirstByUserProfileIdAndCompletedTrueOrderByWorkoutDateDesc(profile.getId())
                .map(WorkoutPlan::getPerceivedDifficulty)
                .orElse(null);

        // Sintetizar com IA considerando feedback adaptativo
        WorkoutPlanRecord planRecord = aiWorkoutService.generateWorkoutForUser(profile, fitnessLevel, focusArea, lastFeedback);

        // Mapear para entidade JPA
        WorkoutPlan workoutPlan = WorkoutPlan.builder()
                .title(planRecord.title())
                .focusArea(planRecord.focusArea())
                .warmupRoutine(planRecord.warmupRoutine())
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

    /**
     * Substitui um exercício específico por uma alternativa gerada pela IA do Google Gemini.
     */
    @PostMapping("/{workoutId}/exercises/{exerciseId}/replace")
    @Transactional
    public ResponseEntity<WorkoutPlanResponse> replaceExercise(
            @PathVariable Long workoutId,
            @PathVariable Long exerciseId
    ) {
        WorkoutPlan plan = workoutPlanRepository.findById(workoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de treino não encontrado."));

        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercício não encontrado."));

        UserProfile profile = plan.getUserProfile();
        ExerciseRecord replacement = aiWorkoutService.replaceExercise(exercise, plan, profile);

        exercise.setName(replacement.name());
        exercise.setSets(replacement.sets());
        exercise.setReps(replacement.reps());
        exercise.setEquipmentNeeded(replacement.equipmentNeeded());
        exercise.setInstructions(replacement.instructions());
        exercise.setCompleted(false);

        exerciseRepository.save(exercise);
        log.info("Exercício ID: {} substituído por: '{}'", exerciseId, replacement.name());

        return ResponseEntity.ok(toResponse(plan));
    }

    /**
     * Regista a percepção de dificuldade do atleta no final do treino ("Muito Fácil", "Ideal", "Muito Difícil").
     */
    @PostMapping("/{workoutId}/feedback")
    @Transactional
    public ResponseEntity<WorkoutPlanResponse> submitFeedback(
            @PathVariable Long workoutId,
            @RequestBody FeedbackRequest request
    ) {
        WorkoutPlan plan = workoutPlanRepository.findById(workoutId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano de treino não encontrado."));

        plan.setPerceivedDifficulty(request.perceivedDifficulty());
        workoutPlanRepository.save(plan);
        log.info("Feedback de dificuldade '{}' guardado para o treino ID: {}", request.perceivedDifficulty(), workoutId);

        return ResponseEntity.ok(toResponse(plan));
    }

    /**
     * Consulta as estatísticas de gamificação do atleta (streak de dias seguidos e conquistas).
     */
    @GetMapping("/user/{userId}/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        List<WorkoutPlan> completedPlans = workoutPlanRepository.findByUserProfileIdAndCompletedTrueOrderByWorkoutDateDesc(userId);
        int totalCompleted = completedPlans.size();

        // Calcular streak de dias com tolerância de 1 dia de recuperação fisiológica
        int streak = 0;
        LocalDate today = LocalDate.now();

        List<LocalDate> distinctDates = completedPlans.stream()
                .map(WorkoutPlan::getWorkoutDate)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (!distinctDates.isEmpty()) {
            LocalDate mostRecent = distinctDates.get(0);
            long daysSinceLast = ChronoUnit.DAYS.between(mostRecent, today);
            if (daysSinceLast <= 2) {
                streak = 1;
                LocalDate previous = mostRecent;
                for (int i = 1; i < distinctDates.size(); i++) {
                    LocalDate current = distinctDates.get(i);
                    long gap = ChronoUnit.DAYS.between(current, previous);
                    if (gap <= 2) {
                        streak++;
                        previous = current;
                    } else {
                        break;
                    }
                }
            }
        }

        // Conquistas desbloqueadas
        List<String> badges = new ArrayList<>();
        if (totalCompleted >= 1) {
            badges.add("🥉 Primeiro Passo: Completaste o teu 1º treino em casa!");
        }
        if (streak >= 3) {
            badges.add("🔥 Chama Acesa: 3 dias consecutivos a treinar!");
        }
        if (streak >= 7) {
            badges.add("⚡ Semana de Ferro: 7 dias seguidos sem falhar!");
        }
        if (totalCompleted >= 5) {
            badges.add("💪 Guerreiro do Lar: 5 treinos em casa completados!");
        }
        if (totalCompleted >= 10) {
            badges.add("🏆 Mestre da Consistência: 10 treinos no historial!");
        }

        return ResponseEntity.ok(new UserStatsResponse(streak, totalCompleted, badges));
    }

    private WorkoutPlanResponse toResponse(WorkoutPlan plan) {
        String effectiveWarmup = plan.getWarmupRoutine();
        if (effectiveWarmup == null || effectiveWarmup.isBlank()) {
            effectiveWarmup = getDynamicWarmupFallback(plan.getFocusArea());
        }

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
                effectiveWarmup,
                plan.getExplanation(),
                plan.getWorkoutDate(),
                plan.isCompleted(),
                plan.getPerceivedDifficulty(),
                exerciseDtos
        );
    }

    private String getDynamicWarmupFallback(String focusArea) {
        String focus = focusArea != null ? focusArea.toLowerCase() : "";
        if (focus.contains("superior") || focus.contains("peito") || focus.contains("braço") || focus.contains("costa")) {
            return "1. Rotação de Ombros e Pulsos (2x 30s) • 2. Círculos com os Braços frente/trás (2x 20 reps) • 3. Flexões inclinadas suaves na parede para ativação do peitoral e escápulas (2x 10 reps) • 4. Alongamento dinâmico de peitoral no batente da porta (30s por lado).";
        } else if (focus.contains("inferior") || focus.contains("perna") || focus.contains("glúteo")) {
            return "1. Rotação Articular de Tornozelos e Joelhos (2x 30s) • 2. Balanço Dinâmico de Pernas (2x 10 reps por perna) • 3. Agachamento Profundo com pausa de 2 segundos no fundo (2x 8 reps) • 4. Ponte de Glúteos no chão para ativação neuromuscular (2x 12 reps).";
        } else if (focus.contains("core") || focus.contains("abdominal") || focus.contains("abdómen")) {
            return "1. Postura Gato-Vaca para mobilidade da coluna vertebral (2x 10 ciclos) • 2. Bird-Dog (Cão-Pássaro) para ativação do core e lombar (2x 8 reps por lado) • 3. Dead Bug controlado (2x 10 reps) • 4. Prancha isométrica de joelhos (2x 20s).";
        } else if (focus.contains("cardio") || focus.contains("hiit") || focus.contains("caloria")) {
            return "1. Marcha no lugar com elevação suave dos joelhos (60s) • 2. Rotações Dinâmicas da Anca e Tronco (30s) • 3. Polichinelos de ritmo leve a moderado (45s) • 4. Agachamentos livres suaves de aquecimento (12 reps).";
        } else if (focus.contains("mobilidade") || focus.contains("alongamento") || focus.contains("recuperação")) {
            return "1. Respiração diafragmática profunda em decúbito dorsal (1 min) • 2. Rotações lentas do pescoço e ombros (2x 30s) • 3. Postura da Criança com respiração relaxada (60s) • 4. Rotações torácicas em quatro apoios (8 reps por lado).";
        } else {
            return "1. Marcha com elevação de joelhos e rotação de braços (60s) • 2. Inchworm (caminhada das mãos à prancha e retorno) (2x 5 reps) • 3. Agachamentos com peso corporal com rotação do tronco (2x 8 reps) • 4. Rotação articular completa de ombros e anca (30s).";
        }
    }
}
