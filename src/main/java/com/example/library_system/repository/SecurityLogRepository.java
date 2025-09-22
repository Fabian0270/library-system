package com.example.library_system.repository;

import com.example.library_system.entity.SecurityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    // Hitta loggar för en specifik användare
    List<SecurityLog> findByUsernameOrderByEventTimeDesc(String username);

    // Hitta loggar mellan två tidpunkter
    List<SecurityLog> findByEventTimeBetweenOrderByEventTimeDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    // Hitta misslyckade inloggningsförsök
    List<SecurityLog> findByEventTypeAndSuccessFalse(String eventType);

    // Räkna misslyckade inloggningsförsök för en användare inom en tidsperiod
    @Query("SELECT COUNT(s) FROM SecurityLog s WHERE s.username = :username " +
            "AND s.eventType = :eventType AND s.success = false " +
            "AND s.eventTime > :sinceTime")
    long countFailedAttempts(@Param("username") String username,
                             @Param("eventType") String eventType,
                             @Param("sinceTime") LocalDateTime sinceTime);

    // Hitta senaste inloggningen för en användare
    @Query("SELECT s FROM SecurityLog s WHERE s.username = :username " +
            "AND s.eventType = 'LOGIN_SUCCESS' ORDER BY s.eventTime DESC")
    List<SecurityLog> findLastLogin(@Param("username") String username);
}