package com.joaosantos.workout.repository;

import com.joaosantos.workout.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositório Spring Data JPA para operações na entidade UserProfile.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Localiza um utilizador pelo seu endereço de email.
     */
    Optional<UserProfile> findByEmail(String email);

    /**
     * Verifica se já existe uma conta registada com o email fornecido.
     */
    boolean existsByEmail(String email);
}
