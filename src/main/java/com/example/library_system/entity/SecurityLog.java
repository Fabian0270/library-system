package com.example.library_system.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_logs")
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "username")
    private String username;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "success")
    private boolean success;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    // Event types konstanter
    public static final String LOGIN_ATTEMPT = "LOGIN_ATTEMPT";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String LOGOUT = "LOGOUT";
    public static final String REGISTRATION = "REGISTRATION";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String ACCOUNT_UNLOCKED = "ACCOUNT_UNLOCKED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String ROLE_CHANGE = "ROLE_CHANGE";

    // Default constructor
    public SecurityLog() {
        this.eventTime = LocalDateTime.now();
    }

    // Constructor för enkla loggningar
    public SecurityLog(String eventType, String username, boolean success) {
        this.eventType = eventType;
        this.username = username;
        this.success = success;
        this.eventTime = LocalDateTime.now();
    }

    // Constructor med full information
    public SecurityLog(String eventType, String username, String ipAddress,
                       String userAgent, boolean success, String failureReason) {
        this.eventType = eventType;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.failureReason = failureReason;
        this.eventTime = LocalDateTime.now();
    }

    // Getters och Setters
    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }
}