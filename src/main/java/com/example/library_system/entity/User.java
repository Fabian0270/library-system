package com.example.library_system.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @NotBlank(message = "Förnamn är obligatoriskt")
    @Size(min = 2, max = 50, message = "Förnamn måste vara mellan 2 och 50 tecken")
    @Column(name = "first_name", nullable = false)
    private String firstName;

    @NotBlank(message = "Efternamn är obligatoriskt")
    @Size(min = 2, max = 50, message = "Efternamn måste vara mellan 2 och 50 tecken")
    @Column(name = "last_name", nullable = false)
    private String lastName;

    @NotBlank(message = "E-post är obligatorisk")
    @Email(message = "E-postadressen måste vara giltig")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Lösenord är obligatoriskt")
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{8,}$",
            message = "Lösenordet måste vara minst 8 tecken långt och innehålla både bokstäver och siffror"
    )
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    // Ändrat till wrapper Boolean för Hibernate
    @Column(name = "enabled", nullable = false, columnDefinition = "BOOLEAN DEFAULT 1")
    private Boolean enabled = true;

    @Column(name = "account_non_locked", nullable = false, columnDefinition = "BOOLEAN DEFAULT 1")
    private Boolean accountNonLocked = true;

    // Ändrat till wrapper Integer
    @Column(name = "failed_login_attempts", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer failedLoginAttempts = 0;

    @Column(name = "lock_time")
    private LocalDateTime lockTime;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Default constructor
    public User() {}

    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.registrationDate = LocalDate.now();
        this.enabled = true;
        this.accountNonLocked = true;
        this.failedLoginAttempts = 0;
    }

    // Hjälpmetoder för roller
    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(Boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLockTime() {
        return lockTime;
    }

    public void setLockTime(LocalDateTime lockTime) {
        this.lockTime = lockTime;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ==============================
    // Alias-metoder för Spring Security
    // ==============================
    public boolean isEnabledPrimitive() {
        return enabled != null ? enabled : true;
    }

    public boolean isAccountNonLockedPrimitive() {
        return accountNonLocked != null ? accountNonLocked : true;
    }

    // Behåller gamla metodnamn för kompatibilitet med Spring Security
    public boolean isEnabled() {
        return isEnabledPrimitive();
    }

    public boolean isAccountNonLocked() {
        return isAccountNonLockedPrimitive();
    }

    public int getFailedLoginAttemptsPrimitive() {
        return failedLoginAttempts != null ? failedLoginAttempts : 0;
    }
}