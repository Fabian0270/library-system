package com.example.library_system.service;

import com.example.library_system.entity.Loan;
import com.example.library_system.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    // Hämta alla lån
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    // Hämta lån med ID
    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }

    // Hämta användarens alla lån
    public List<Loan> getUserLoans(Long userId) {
        return loanRepository.findByUserId(userId);
    }

    // Hämta användarens aktiva lån
    public List<Loan> getUserActiveLoans(Long userId) {
        return loanRepository.findByUserIdAndReturnedDateIsNull(userId);
    }

    // Skapa nytt lån
    @Transactional
    public Loan createLoan(Long userId, Long bookId) {
        // Kontrollera att användaren existerar
        if (userService.getUserById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        // Kontrollera att boken existerar och är tillgänglig
        if (!bookService.isBookAvailable(bookId)) {
            throw new IllegalArgumentException("Book is not available");
        }

        // Kontrollera att användaren inte redan har lånat denna bok
        Optional<Loan> existingLoan = loanRepository.findActiveLoadByUserIdAndBookId(userId, bookId);
        if (existingLoan.isPresent()) {
            throw new IllegalArgumentException("User already has an active loan for this book");
        }

        // Minska tillgängliga kopior
        if (!bookService.decreaseAvailableCopies(bookId)) {
            throw new IllegalArgumentException("Failed to decrease available copies");
        }

        // Skapa lånet
        Loan loan = new Loan();
        loan.setUserId(userId);
        loan.setBookId(bookId);
        loan.setBorrowedDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14)); // 14 dagars lånetid

        return loanRepository.save(loan);
    }

    // Returnera bok
    @Transactional
    public Loan returnBook(Long loanId) {
        Optional<Loan> optionalLoan = loanRepository.findById(loanId);
        if (optionalLoan.isEmpty()) {
            throw new IllegalArgumentException("Loan not found");
        }

        Loan loan = optionalLoan.get();

        // Kontrollera att lånet inte redan är återlämnat
        if (loan.getReturnedDate() != null) {
            throw new IllegalArgumentException("Book already returned");
        }

        // Sätt återlämningsdatum
        loan.setReturnedDate(LocalDate.now());

        // Öka tillgängliga kopior
        if (!bookService.increaseAvailableCopies(loan.getBookId())) {
            throw new IllegalArgumentException("Failed to increase available copies");
        }

        return loanRepository.save(loan);
    }

    // Förläng lån
    @Transactional
    public Loan extendLoan(Long loanId) {
        Optional<Loan> optionalLoan = loanRepository.findById(loanId);
        if (optionalLoan.isEmpty()) {
            throw new IllegalArgumentException("Loan not found");
        }

        Loan loan = optionalLoan.get();

        // Kontrollera att lånet inte är återlämnat
        if (loan.getReturnedDate() != null) {
            throw new IllegalArgumentException("Cannot extend returned loan");
        }

        // Kontrollera att lånet inte är försenat
        if (loan.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot extend overdue loan");
        }

        // Förläng med 14 dagar
        loan.setDueDate(loan.getDueDate().plusDays(14));

        return loanRepository.save(loan);
    }

    // Hämta försenade lån
    public List<Loan> getOverdueLoans() {
        return loanRepository.findOverdueLoans();
    }
}