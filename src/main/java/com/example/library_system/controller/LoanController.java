package com.example.library_system.controller;

import com.example.library_system.entity.Loan;
import com.example.library_system.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping
public class LoanController {

    @Autowired
    private LoanService loanService;

    // GET /loans - Lista alla lån
    @GetMapping("/loans")
    public List<Loan> getAllLoans() {
        return loanService.getAllLoans();
    }

    // GET /loans/{id} - Hämta specifikt lån
    @GetMapping("/loans/{id}")
    public ResponseEntity<Loan> getLoanById(@PathVariable Long id) {
        Optional<Loan> loan = loanService.getLoanById(id);
        if (loan.isPresent()) {
            return ResponseEntity.ok(loan.get());
        }
        return ResponseEntity.notFound().build();
    }

    // GET /users/{userId}/loans - Hämta användarens lån
    @GetMapping("/users/{userId}/loans")
    public ResponseEntity<List<Loan>> getUserLoans(@PathVariable Long userId) {
        List<Loan> loans = loanService.getUserLoans(userId);
        return ResponseEntity.ok(loans);
    }

    // POST /loans - Låna bok (kräver userId och bookId)
    @PostMapping("/loans")
    public ResponseEntity<?> createLoan(@RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            Long bookId = request.get("bookId");

            if (userId == null || bookId == null) {
                return ResponseEntity.badRequest().body("userId and bookId are required");
            }

            Loan loan = loanService.createLoan(userId, bookId);
            return ResponseEntity.status(HttpStatus.CREATED).body(loan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /loans/{id}/return - Returnera bok
    @PutMapping("/loans/{id}/return")
    public ResponseEntity<?> returnBook(@PathVariable Long id) {
        try {
            Loan loan = loanService.returnBook(id);
            return ResponseEntity.ok(loan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /loans/{id}/extend - Förläng lån
    @PutMapping("/loans/{id}/extend")
    public ResponseEntity<?> extendLoan(@PathVariable Long id) {
        try {
            Loan loan = loanService.extendLoan(id);
            return ResponseEntity.ok(loan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /loans/overdue - Hämta försenade lån
    @GetMapping("/loans/overdue")
    public List<Loan> getOverdueLoans() {
        return loanService.getOverdueLoans();
    }
}