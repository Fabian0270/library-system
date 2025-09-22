package com.example.library_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationDTO {

    @NotBlank(message = "Förnamn är obligatoriskt")
    @Size(min = 2, max = 50, message = "Förnamn måste vara mellan 2 och 50 tecken")
    private String firstName;

    @NotBlank(message = "Efternamn är obligatoriskt")
    @Size(min = 2, max = 50, message = "Efternamn måste vara mellan 2 och 50 tecken")
    private String lastName;

    @NotBlank(message = "E-post är obligatorisk")
    @Email(message = "E-postadressen måste vara giltig")
    private String email;

    @NotBlank(message = "Lösenord är obligatoriskt")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,}$",
            message = "Lösenordet måste vara minst 8 tecken långt och innehålla både bokstäver och siffror"
    )
    private String password;

    @NotBlank(message = "Bekräfta lösenord är obligatoriskt")
    private String confirmPassword;

    // Default constructor
    public RegistrationDTO() {}

    // Getters och Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}