package com.example.library_system.repository;

import com.example.library_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Hitta användare via email
    Optional<User> findByEmail(String email);

    // Kontrollera om email redan existerar
    boolean existsByEmail(String email);
}