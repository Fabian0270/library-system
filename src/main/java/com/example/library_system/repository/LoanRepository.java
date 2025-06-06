package com.example.library_system.repository;

import com.example.library_system.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Hitta alla lån för en specifik användare
    List<Loan> findByUserId(Long userId);

    // Hitta alla aktiva lån för en användare (ej återlämnade)
    List<Loan> findByUserIdAndReturnedDateIsNull(Long userId);

    // Hitta alla lån för en specifik bok
    List<Loan> findByBookId(Long bookId);

    // Hitta aktivt lån för en specifik bok (ej återlämnad)
    List<Loan> findByBookIdAndReturnedDateIsNull(Long bookId);

    // Kontrollera om en användare har ett aktivt lån för en specifik bok
    @Query("SELECT l FROM Loan l WHERE l.userId = :userId AND l.bookId = :bookId AND l.returnedDate IS NULL")
    Optional<Loan> findActiveLoadByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    // Hitta alla försenade lån
    @Query("SELECT l FROM Loan l WHERE l.returnedDate IS NULL AND l.dueDate < CURRENT_DATE")
    List<Loan> findOverdueLoans();
}