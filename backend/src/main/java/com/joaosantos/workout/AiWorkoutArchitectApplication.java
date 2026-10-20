package com.joaosantos.workout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the AI Workout Architect enterprise backend service.
 * Orchestrates AI-powered fitness routine generation, user biometric tracking,
 * and state persistence.
 */
@SpringBootApplication
public class AiWorkoutArchitectApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiWorkoutArchitectApplication.class, args);
    }
}
