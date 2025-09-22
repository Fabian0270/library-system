package com.example.library_system.service;

import com.example.library_system.entity.SecurityLog;
import com.example.library_system.entity.User;
import com.example.library_system.repository.SecurityLogRepository;
import com.example.library_system.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SecurityLogService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityLogService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 24; // 24 timmar

    @Autowired
    private SecurityLogRepository securityLogRepository;

    @Autowired
    private UserRepository userRepository;

    // Logga säkerhetshändelse
    @Transactional
    public void logSecurityEvent(String eventType, String username, HttpServletRequest request,
                                 boolean success, String failureReason) {
        SecurityLog log = new SecurityLog();
        log.setEventType(eventType);
        log.setUsername(username);
        log.setSuccess(success);
        log.setFailureReason(failureReason);

        if (request != null) {
            log.setIpAddress(getClientIP(request));
            log.setUserAgent(request.getHeader("User-Agent"));
        }

        securityLogRepository.save(log);

        // Logga även till fil
        if (success) {
            logger.info("Security Event: {} - User: {} - Success", eventType, username);
        } else {
            logger.warn("Security Event: {} - User: {} - Failure: {}", eventType, username, failureReason);
        }
    }

    // Logga lyckad inloggning
    @Transactional
    public void logSuccessfulLogin(String username, HttpServletRequest request) {
        logSecurityEvent(SecurityLog.LOGIN_SUCCESS, username, request, true, null);

        // Uppdatera användarens senaste inloggning och återställ misslyckade försök
        userRepository.findByEmail(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        });
    }

    // Logga misslyckad inloggning
    @Transactional
    public void logFailedLogin(String username, HttpServletRequest request, String reason) {
        logSecurityEvent(SecurityLog.LOGIN_FAILURE, username, request, false, reason);

        // Uppdatera antal misslyckade försök
        userRepository.findByEmail(username).ifPresent(user -> {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);

            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // Lås kontot
                user.setAccountNonLocked(false);
                user.setLockTime(LocalDateTime.now());
                logSecurityEvent(SecurityLog.ACCOUNT_LOCKED, username, request, true,
                        "Account locked due to " + failedAttempts + " failed attempts");
                logger.error("Account locked for user: {} after {} failed attempts", username, failedAttempts);
            }

            userRepository.save(user);
        });
    }

    // Logga utloggning
    public void logLogout(String username, HttpServletRequest request) {
        logSecurityEvent(SecurityLog.LOGOUT, username, request, true, null);
    }

    // Logga registrering
    public void logRegistration(String username, HttpServletRequest request, boolean success, String reason) {
        logSecurityEvent(SecurityLog.REGISTRATION, username, request, success, reason);
    }

    // Logga åtkomst nekad
    public void logAccessDenied(String username, HttpServletRequest request, String resource) {
        logSecurityEvent(SecurityLog.ACCESS_DENIED, username, request, false,
                "Attempted to access: " + resource);
    }

    // Kontrollera om konto ska låsas upp
    @Transactional
    public boolean unlockAccountIfTimeExpired(User user) {
        if (user.getLockTime() == null) {
            return false;
        }

        LocalDateTime unlockTime = user.getLockTime().plusHours(LOCK_TIME_DURATION);

        if (LocalDateTime.now().isAfter(unlockTime)) {
            user.setAccountNonLocked(true);
            user.setFailedLoginAttempts(0);
            user.setLockTime(null);
            userRepository.save(user);

            logSecurityEvent(SecurityLog.ACCOUNT_UNLOCKED, user.getEmail(), null, true,
                    "Account automatically unlocked after timeout");
            return true;
        }

        return false;
    }

    // Hämta IP-adress från request
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    // Hämta säkerhetsloggar för en användare
    public List<SecurityLog> getUserSecurityLogs(String username) {
        return securityLogRepository.findByUsernameOrderByEventTimeDesc(username);
    }

    // Hämta alla säkerhetsloggar inom en tidsperiod
    public List<SecurityLog> getSecurityLogsBetween(LocalDateTime start, LocalDateTime end) {
        return securityLogRepository.findByEventTimeBetweenOrderByEventTimeDesc(start, end);
    }

    // Räkna misslyckade inloggningsförsök
    public long countRecentFailedAttempts(String username, int hours) {
        LocalDateTime sinceTime = LocalDateTime.now().minusHours(hours);
        return securityLogRepository.countFailedAttempts(username, SecurityLog.LOGIN_FAILURE, sinceTime);
    }
}