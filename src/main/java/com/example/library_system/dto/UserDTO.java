package com.example.library_system.dto;

import java.time.LocalDate;

public class UserDTO {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate registrationDate;

    // Default constructor
    public UserDTO() {}

    // Constructor för att konvertera från User entity
    public UserDTO(Long userId, String firstName, String lastName, String email, LocalDate registrationDate) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.registrationDate = registrationDate;
    }

    // Getters och Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }
}