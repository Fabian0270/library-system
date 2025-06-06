package com.example.library_system.service;

import com.example.library_system.dto.UserDTO;
import com.example.library_system.entity.User;
import com.example.library_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Konvertera User till UserDTO
    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setRegistrationDate(user.getRegistrationDate());
        return dto;
    }

    // Hämta alla användare som DTOs
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Hämta användare via ID
    public Optional<UserDTO> getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::convertToDTO);
    }

    // Hämta användare via email
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDTO);
    }

    // Skapa ny användare
    public UserDTO createUser(User user) {
        // Sätt registreringsdatum till idag om det inte är satt
        if (user.getRegistrationDate() == null) {
            user.setRegistrationDate(LocalDate.now());
        }

        // Kontrollera att email inte redan finns
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    // Uppdatera användare
    public UserDTO updateUser(Long id, User userDetails) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setFirstName(userDetails.getFirstName());
            user.setLastName(userDetails.getLastName());

            // Om email ändras, kontrollera att den nya emailen inte redan finns
            if (!user.getEmail().equals(userDetails.getEmail())) {
                if (userRepository.existsByEmail(userDetails.getEmail())) {
                    throw new IllegalArgumentException("Email already exists");
                }
                user.setEmail(userDetails.getEmail());
            }

            // Uppdatera password om det är satt
            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                user.setPassword(userDetails.getPassword());
            }

            User savedUser = userRepository.save(user);
            return convertToDTO(savedUser);
        }
        return null;
    }

    // Ta bort användare
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}