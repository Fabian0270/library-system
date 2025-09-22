package com.example.library_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "E-post är obligatorisk")
    @Email(message = "E-post måste vara giltig")
    private String email;

    @NotBlank(message = "Lösenord är obligatoriskt")
    private String password;

    // Default constructor
    public LoginRequest() {}

    // Constructor
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters och Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}