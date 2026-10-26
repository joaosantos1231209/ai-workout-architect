package com.joaosantos.workout.controller;

import com.joaosantos.workout.dto.AuthRequest;
import com.joaosantos.workout.dto.RegisterRequest;
import com.joaosantos.workout.dto.UserProfileResponse;
import com.joaosantos.workout.model.UserProfile;
import com.joaosantos.workout.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller REST para registo e login de atletas por email e password.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"}, originPatterns = "*")
@Slf4j
public class AuthController {

    private final UserProfileRepository userProfileRepository;

    public AuthController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Endpoint leve de ping / health check para manter o servidor ativo e acordar instâncias na nuvem.
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        try {
            long count = userProfileRepository.count();
            return ResponseEntity.ok("pong (" + count + " users)");
        } catch (Exception e) {
            log.warn("Ping DB check warning: {}", e.getMessage());
            return ResponseEntity.ok("pong");
        }
    }

    /**
     * Registo de nova conta de atleta.
     */
    @PostMapping("/register")
    public ResponseEntity<UserProfileResponse> register(@RequestBody RegisterRequest request) {
        log.info("Pedido de registo para o email: {}", request.email());

        if (userProfileRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email já se encontra registado.");
        }

        UserProfile user = UserProfile.builder()
                .name(request.name())
                .email(request.email().trim().toLowerCase())
                .password(request.password())
                .age(request.age() != null ? request.age() : 25)
                .fitnessLevel(request.fitnessLevel() != null ? request.fitnessLevel() : "Intermédio")
                .primaryGoal(request.primaryGoal() != null ? request.primaryGoal() : "Hipertrofia & Ganho Muscular")
                .build();

        UserProfile saved = userProfileRepository.save(user);
        log.info("Utilizador registado com sucesso com ID: {}", saved.getId());

        return ResponseEntity.ok(toResponse(saved));
    }

    /**
     * Autenticação de conta existente (Login).
     */
    @PostMapping("/login")
    public ResponseEntity<UserProfileResponse> login(@RequestBody AuthRequest request) {
        log.info("Tentativa de login para o email: {}", request.email());

        UserProfile user = userProfileRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou palavra-passe incorretos."));

        if (!user.getPassword().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email ou palavra-passe incorretos.");
        }

        log.info("Login bem-sucedido para o utilizador: {}", user.getName());
        return ResponseEntity.ok(toResponse(user));
    }

    private UserProfileResponse toResponse(UserProfile user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getFitnessLevel(),
                user.getPrimaryGoal()
        );
    }
}
