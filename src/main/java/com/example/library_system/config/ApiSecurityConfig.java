package com.example.library_system.config;

import com.example.library_system.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ApiSecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF-skydd med cookie-baserad token
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Undanta vissa endpoints från CSRF om nödvändigt
                        .ignoringRequestMatchers(
                                "/api/auth/check",  // GET-anrop behöver inte CSRF
                                "/api/auth/debug/**"  // Debug endpoints
                        )
                )

                // CORS för JavaScript
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Auktoriseringsregler
                .authorizeHttpRequests(authz -> authz
                        // Statiska resurser och publika endpoints
                        .requestMatchers("/", "/index.html", "/login.html", "/register.html",
                                "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/403.html", "/404.html", "/500.html").permitAll()

                        // Debug endpoints - TILLÅT ALLA FÖR FELSÖKNING
                        .requestMatchers("/api/auth/debug/**").permitAll()

                        // Auth endpoints - publika
                        .requestMatchers("/api/auth/login", "/api/auth/register",
                                "/api/auth/check", "/api/auth/csrf").permitAll()

                        // API endpoints med rollkrav
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/delete/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/edit/**").hasRole("ADMIN")

                        // Endpoints för inloggade användare
                        .requestMatchers("/api/books/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/loans/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/api/profile/**").authenticated()

                        // Befintliga endpoints
                        .requestMatchers("/books/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/loans/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/users/**").hasAnyRole("USER", "ADMIN")

                        // Alla andra kräver autentisering
                        .anyRequest().authenticated()
                )

                // Form-baserad inloggning för HTML-sidor
                .formLogin(form -> form
                        .loginPage("/login.html")
                        .loginProcessingUrl("/api/auth/form-login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard.html", true)
                        .failureUrl("/login.html?error=true")
                        .permitAll()
                )

                // Logout
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/login.html?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll()
                )

                // Exception handling - VIKTIGT FÖR API
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // För API-anrop, returnera 403 JSON
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(403);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Access denied\",\"message\":\"" +
                                        accessDeniedException.getMessage() + "\"}");
                            } else {
                                // För vanliga requests, redirect till 403.html
                                response.sendRedirect("/403.html");
                            }
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            // För API-anrop, returnera 401 JSON
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(401);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" +
                                        authException.getMessage() + "\"}");
                            } else {
                                // För vanliga requests, redirect till login
                                response.sendRedirect("/login.html");
                            }
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        // VIKTIGT: Tillåt CSRF-header
        configuration.addExposedHeader("X-CSRF-TOKEN");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}