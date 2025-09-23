package com.example.library_system.controller;

import com.example.library_system.dto.LoginRequest;
import com.example.library_system.dto.RegistrationDTO;
import com.example.library_system.entity.Role;
import com.example.library_system.entity.User;
import com.example.library_system.repository.RoleRepository;
import com.example.library_system.repository.UserRepository;
import com.example.library_system.service.SecurityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger logger = LoggerFactory.getLogger(AuthApiController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityLogService securityLogService;

    // CSRF Token endpoint - VIKTIGT!
    @GetMapping("/csrf")
    public ResponseEntity<?> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        Map<String, Object> response = new HashMap<>();
        if (csrfToken != null) {
            response.put("token", csrfToken.getToken());
            response.put("headerName", csrfToken.getHeaderName());
            response.put("parameterName", csrfToken.getParameterName());
        } else {
            response.put("message", "CSRF token not available");
        }

        return ResponseEntity.ok(response);
    }

    // Login endpoint med CSRF-validering
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        try {
            // Använd email som username för Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Sätt authentication i SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Skapa session
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Logga lyckad inloggning
            securityLogService.logSuccessfulLogin(loginRequest.getEmail(), request);

            // Hämta användarinfo
            User user = userRepository.findByEmail(loginRequest.getEmail()).orElseThrow();

            // Skapa response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inloggning lyckades");
            response.put("sessionId", session.getId());
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("userId", user.getUserId());
            response.put("roles", user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // Logga misslyckad inloggning
            securityLogService.logFailedLogin(loginRequest.getEmail(), request, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Felaktigt användarnamn eller lösenord");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    // Registrering endpoint med CSRF-validering
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationDTO registrationDTO,
                                      HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Validera lösenord
        if (!registrationDTO.getPassword().matches("^(?=.*[a-zA-Z])(?=.*\\d).{8,}$")) {
            response.put("success", false);
            response.put("message", "Lösenordet måste vara minst 8 tecken och innehålla både bokstäver och siffror");
            return ResponseEntity.badRequest().body(response);
        }

        // Kontrollera att lösenorden matchar
        if (!registrationDTO.getPassword().equals(registrationDTO.getConfirmPassword())) {
            response.put("success", false);
            response.put("message", "Lösenorden matchar inte");
            return ResponseEntity.badRequest().body(response);
        }

        // Kontrollera om email redan finns
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            securityLogService.logRegistration(registrationDTO.getEmail(), request, false,
                    "Email already exists");
            response.put("success", false);
            response.put("message", "E-postadressen används redan");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Skapa ny användare
            User newUser = new User();
            newUser.setFirstName(registrationDTO.getFirstName());
            newUser.setLastName(registrationDTO.getLastName());
            newUser.setEmail(registrationDTO.getEmail());
            newUser.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));
            newUser.setRegistrationDate(LocalDate.now());
            newUser.setEnabled(true);
            newUser.setAccountNonLocked(true);
            newUser.setFailedLoginAttempts(0);

            // VIKTIGT: Säkerställ att USER-rollen finns och tilldelas
            Role userRole = roleRepository.findByName("USER")
                    .orElseGet(() -> {
                        logger.warn("USER-roll fanns inte, skapar ny");
                        Role newRole = new Role("USER", "Standard användare");
                        return roleRepository.save(newRole);
                    });

            newUser.addRole(userRole);
            User savedUser = userRepository.save(newUser);

            logger.info("Ny användare registrerad: {} med {} roller",
                    savedUser.getEmail(), savedUser.getRoles().size());

            // Logga registreringen
            securityLogService.logRegistration(registrationDTO.getEmail(), request, true, null);

            response.put("success", true);
            response.put("message", "Registrering lyckades! Du kan nu logga in.");
            response.put("userId", savedUser.getUserId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Fel vid registrering", e);
            securityLogService.logRegistration(registrationDTO.getEmail(), request, false,
                    e.getMessage());
            response.put("success", false);
            response.put("message", "Ett fel uppstod vid registreringen");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Logout endpoint
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            securityLogService.logLogout(auth.getName(), request);
        }

        // Invalidera sessionen
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        Map<String, String> response = new HashMap<>();
        response.put("message", "Utloggning lyckades");
        return ResponseEntity.ok(response);
    }

    // Kontrollera autentiseringsstatus
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            response.put("authenticated", true);
            response.put("username", auth.getName());
            response.put("roles", auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .collect(Collectors.toList()));
        } else {
            response.put("authenticated", false);
        }

        return ResponseEntity.ok(response);
    }

    // ============== DEBUG ENDPOINTS - TA BORT I PRODUKTION! ==============

    // Debug: Fixa användare som saknar roll
    @GetMapping("/debug/fix-user/{email}")
    public ResponseEntity<?> fixUser(@PathVariable String email) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (!userOpt.isPresent()) {
                result.put("error", "Användare finns inte");
                return ResponseEntity.ok(result);
            }

            User user = userOpt.get();
            result.put("userFound", true);
            result.put("email", user.getEmail());
            result.put("hadRoles", user.getRoles().size());

            if (user.getRoles().isEmpty()) {
                Role userRole = roleRepository.findByName("USER")
                        .orElseGet(() -> {
                            Role newRole = new Role("USER", "Standard användare");
                            return roleRepository.save(newRole);
                        });

                user.addRole(userRole);
                userRepository.save(user);

                result.put("roleAdded", true);
                result.put("message", "USER-roll har lagts till");
            } else {
                result.put("roleAdded", false);
                result.put("existingRoles", user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()));
            }

            if (!user.isEnabled()) {
                user.setEnabled(true);
                userRepository.save(user);
                result.put("enabled", "Kontot har aktiverats");
            }

            if (!user.isAccountNonLocked()) {
                user.setAccountNonLocked(true);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
                result.put("unlocked", "Kontot har låsts upp");
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    // Debug: Visa användarinfo
    @GetMapping("/debug/user/{email}")
    public ResponseEntity<?> debugUser(@PathVariable String email) {
        Map<String, Object> info = new HashMap<>();

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            info.put("exists", true);
            info.put("userId", user.getUserId());
            info.put("email", user.getEmail());
            info.put("firstName", user.getFirstName());
            info.put("lastName", user.getLastName());
            info.put("enabled", user.isEnabled());
            info.put("accountNonLocked", user.isAccountNonLocked());
            info.put("failedAttempts", user.getFailedLoginAttempts());
            info.put("roles", user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList()));
            info.put("hasPassword", user.getPassword() != null);
            info.put("passwordIsEncrypted", user.getPassword() != null &&
                    user.getPassword().startsWith("$2"));
        } else {
            info.put("exists", false);
        }

        info.put("allUsers", userRepository.findAll().stream()
                .map(User::getEmail)
                .collect(Collectors.toList()));

        return ResponseEntity.ok(info);
    }

    // Debug: Lista alla roller
    @GetMapping("/debug/roles")
    public ResponseEntity<?> debugRoles() {
        Map<String, Object> info = new HashMap<>();
        info.put("roles", roleRepository.findAll().stream()
                .map(role -> {
                    Map<String, Object> roleInfo = new HashMap<>();
                    roleInfo.put("id", role.getRoleId());
                    roleInfo.put("name", role.getName());
                    roleInfo.put("description", role.getDescription());
                    roleInfo.put("userCount", role.getUsers().size());
                    return roleInfo;
                })
                .collect(Collectors.toList()));
        return ResponseEntity.ok(info);
    }

    @GetMapping("/debug/fix-null-values")
    public ResponseEntity<?> fixNullValues() {
        Map<String, Object> result = new HashMap<>();
        int fixedCount = 0;

        try {
            List<User> allUsers = userRepository.findAll();

            for (User user : allUsers) {
                boolean needsUpdate = false;

                if (user.getFailedLoginAttempts() < 0) {
                    user.setFailedLoginAttempts(0);
                    needsUpdate = true;
                }

                user.setEnabled(true);
                user.setAccountNonLocked(true);
                needsUpdate = true;

                if (needsUpdate) {
                    userRepository.save(user);
                    fixedCount++;
                }
            }

            result.put("success", true);
            result.put("usersFixed", fixedCount);
            result.put("totalUsers", allUsers.size());
            result.put("message", "Alla användare har uppdaterats med korrekta värden");

            result.put("users", allUsers.stream()
                    .map(u -> {
                        Map<String, Object> userInfo = new HashMap<>();
                        userInfo.put("email", u.getEmail());
                        userInfo.put("enabled", u.isEnabled());
                        userInfo.put("accountNonLocked", u.isAccountNonLocked());
                        userInfo.put("failedAttempts", u.getFailedLoginAttempts());
                        return userInfo;
                    })
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}