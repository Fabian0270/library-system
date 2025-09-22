package com.example.library_system.listener;

import com.example.library_system.service.SecurityLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthenticationEventListener {

    @Autowired
    private SecurityLogService securityLogService;

    // Lyssna på lyckade inloggningar
    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        HttpServletRequest request = getHttpServletRequest();

        // Loggning sker i AuthController för att undvika dubbelloggar
    }

    // Lyssna på misslyckade inloggningar
    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getMessage();
        HttpServletRequest request = getHttpServletRequest();

        securityLogService.logFailedLogin(username, request, reason);
    }

    // Lyssna på utloggningar
    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        if (event.getAuthentication() != null) {
            String username = event.getAuthentication().getName();
            HttpServletRequest request = getHttpServletRequest();

            securityLogService.logLogout(username, request);
        }
    }

    // Hjälpmetod för att hämta HttpServletRequest
    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}