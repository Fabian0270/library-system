package com.example.library_system.service;

import com.example.library_system.entity.Role;
import com.example.library_system.entity.User;
import com.example.library_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityLogService securityLogService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Användare med email " + email + " hittades inte"));

        // Kontrollera om kontot är låst
        if (!user.isAccountNonLocked()) {
            throw new UsernameNotFoundException("Kontot är låst på grund av för många misslyckade inloggningsförsök");
        }

        return new CustomUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                mapRolesToAuthorities(user.getRoles()),
                user
        );
    }

    private Collection<? extends GrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }

    // Custom UserDetails implementation
    public static class CustomUserDetails implements UserDetails {
        private String username;
        private String password;
        private boolean enabled;
        private boolean accountNonLocked;
        private Collection<? extends GrantedAuthority> authorities;
        private User user;

        public CustomUserDetails(String username, String password, boolean enabled,
                                 boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities,
                                 User user) {
            this.username = username;
            this.password = password;
            this.enabled = enabled;
            this.accountNonLocked = accountNonLocked;
            this.authorities = authorities;
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        public User getUser() {
            return user;
        }
    }
}